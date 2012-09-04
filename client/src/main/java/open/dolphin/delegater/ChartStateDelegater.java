package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import open.dolphin.infomodel.*;
import open.dolphin.util.BeanUtils;

/**
 * ChartState変化関連のデレゲータ
 * @author masuda, Masuda Naika
 */
public class ChartStateDelegater extends BusinessDelegater {
    
    private static final String RES_CS = "chartState/";
    private static final String POLLING_PATH = RES_CS + "subscribe/";
    
    private static final boolean debug = false;
    private static final ChartStateDelegater instance;

    static {
        instance = new ChartStateDelegater();
    }
    
    private ChartStateDelegater() {
    }
    
    public static ChartStateDelegater getInstance() {
        return instance;
    }
    
    public int updateChartState(ChartStateMsgModel msg) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(RES_CS);
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
    
    public List<ChartStateMsgModel> getChartStateMsgList(int currentId) {

        StringBuilder sb = new StringBuilder();
        sb.append(RES_CS);
        sb.append("msgList/");
        sb.append(String.valueOf(currentId));
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
        
        TypeReference typeRef = new TypeReference<List<ChartStateMsgModel>>(){};
        List<ChartStateMsgModel> list = (List<ChartStateMsgModel>)
                getConverter().fromJson(entityStr, typeRef);

        // pvtがのっかて来てるときは保険をデコード
        if (list != null && !list.isEmpty()) {
            for (ChartStateMsgModel msg : list) {
                PatientVisitModel pvt = msg.getPatientVisitModel();
                if (pvt != null) {
                    PatientModel pm = pvt.getPatientModel();
                    decodeHealthInsurance(pm);
                }
            }
        }
        return list;
    }
    
    public String getCurrentId(int id) {
        
        String path = POLLING_PATH + String.valueOf(id);
        String ret = JerseyClient.getInstance()
                .getAsyncResource(path)
                .accept(MEDIATYPE_TEXT_UTF8)
                .get(String.class);
        
        return ret;
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

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
