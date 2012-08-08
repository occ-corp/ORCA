package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import open.dolphin.infomodel.*;
import open.dolphin.util.BeanUtils;

/**
 * PVT 関連の Business Delegater　クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class PVTDelegater extends BusinessDelegater {

    private static final String RES_PVT = "pvt2/";
    public static final String PVT_MESSAGE_EVENT = "pvtMessageEvent";
    
    // スレッド
    private PollingTask pollingTask;
    private Thread thread;
    
    // 束縛サポート
    private PropertyChangeSupport boundSupport;
    
    private static final boolean debug = false;
    private static final PVTDelegater instance;

    static {
        instance = new PVTDelegater();
    }

    public static PVTDelegater getInstance() {
        return instance;
    }

    private PVTDelegater() {
    }

    /**
     * 受付情報 PatientVisitModel をデータベースに登録する。
     *
     * @param pvtModel 受付情報 PatientVisitModel
     * @param principal UserId と FacilityId
     * @return 保存に成功した個数
     */
    public int addPvt(PatientVisitModel pvtModel) {

        // convert
        String json = getConverter().toJson(pvtModel);

        // resource post
        String path = RES_PVT;
        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);

        // result = count
        int cnt = Integer.parseInt(enityStr);
        return cnt;
    }

    public int removePvt(long id) {

        String path = RES_PVT + String.valueOf(id);

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .delete(ClientResponse.class);

        int status = response.getStatus();
        String enityStr = "delete response";
        debug(status, enityStr);

        return 1;
    }

    public int updatePvtState(PvtMessageModel msg) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(RES_PVT);
        sb.append("state");
        String path = sb.toString();

        String json = getConverter().toJson(msg);

        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);

        return Integer.parseInt(enityStr);

    }

    public PvtListModel getPvtListModel() {

        StringBuilder sb = new StringBuilder();
        sb.append(RES_PVT);
        sb.append("pvtListModel");
        String path = sb.toString();

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        PvtListModel model = (PvtListModel)
                getConverter().fromJson(entityStr, PvtListModel.class);

        // 保険をデコード
        List<PatientVisitModel> pvtList = model.getPvtList();
        if (pvtList != null && !pvtList.isEmpty()) {
            for (PatientVisitModel pvt : pvtList) {
                PatientModel pm = pvt.getPatientModel();
                decodeHealthInsurance(pm);
            }
        }

        return model;
    }

    public List<PvtMessageModel> getPvtMessageList(int nextId) {

        StringBuilder sb = new StringBuilder();
        sb.append(RES_PVT);
        sb.append("pvtMessage/");
        sb.append(String.valueOf(nextId));
        String path = sb.toString();

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<PvtMessageModel>>(){};
        List<PvtMessageModel> list = (List<PvtMessageModel>)
                getConverter().fromJsonTypeRef(entityStr, typeRef);

        // pvtがのっかて来てるときは保険をデコード
        if (list != null) {
            for (PvtMessageModel msg : list) {
                PatientVisitModel pvt = msg.getPatientVisitModel();
                if (pvt != null) {
                    PatientModel pm = pvt.getPatientModel();
                    decodeHealthInsurance(pm);
                }
            }
        }
        return list;
    }

    public void subscribePvt() {

        StringBuilder sb = new StringBuilder();
        sb.append(RES_PVT);
        sb.append("subscribe");
        final String path = sb.toString();

        pollingTask = new PollingTask();
        pollingTask.setPath(path);
        
        thread = new Thread(pollingTask, "PVT polling task");
        thread.start();
    }

    public void disposePollingTask() {
        pollingTask.stop();
        try {
            thread.join(100);
        } catch (InterruptedException ex) {
        }
        thread.interrupt();
        thread = null;
    }

    private class PollingTask implements Runnable {

        private String pollingPath;
        private boolean isRunning;

        private void setPath(String path) {
            this.pollingPath = path;
        }
        
        private void stop() {
            isRunning = false;
        }
        
        @Override
        public void run() {
            
            isRunning = true;
            
            while (isRunning) {
                try {
                    String str = JerseyClient.getInstance()
                            .getAsyncResource(pollingPath)
                            .accept(MEDIATYPE_TEXT_UTF8)
                            .get(String.class);
                    int nextId = Integer.valueOf(str);
                    boundSupport.firePropertyChange(PVT_MESSAGE_EVENT, 0, nextId);
                } catch (Exception e) {
                    //System.out.println(e.toString());
                }
            }
        }
    }

    /**
     * バイナリの健康保険データをオブジェクトにデコードする。
     *
     * @param patient 患者モデル
     */
    private void decodeHealthInsurance(PatientModel patient) {

        // Health Insurance を変換をする beanXML2PVT
        Collection<HealthInsuranceModel> c = patient.getHealthInsurances();

        if (c != null && c.size() > 0) {

            List<PVTHealthInsuranceModel> list = new ArrayList<PVTHealthInsuranceModel>(c.size());

            for (HealthInsuranceModel model : c) {
                try {
                    // byte[] を XMLDecord
                    PVTHealthInsuranceModel hModel = (PVTHealthInsuranceModel) BeanUtils.xmlDecode(model.getBeanBytes());
                    list.add(hModel);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            patient.setPvtHealthInsurances(list);
            patient.getHealthInsurances().clear();
            patient.setHealthInsurances(null);
        }
    }

    /**
     * 束縛プロパティリスナを登録する。
     *
     * @param propName プロパティ名
     * @param listener リスナ
     */
    public void addPropertyChangeListener(String propName, PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(propName, listener);
    }

    /**
     * 束縛プロパティを削除する。
     *
     * @param propName プロパティ名
     * @param listener リスナ
     */
    public void removePropertyChangeListener(String propName, PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.removePropertyChangeListener(propName, listener);
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
