package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.dto.PatientSearchSpec;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.PVTHealthInsuranceModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.util.BeanUtils;

/**
 * 患者関連の Business Delegater　クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class  PatientDelegater extends BusinessDelegater {

    private static final String BASE_RESOURCE       = "patient/";
    private static final String NAME_RESOURCE       = "patient/name/";
    private static final String KANA_RESOURCE       = "patient/kana/";
    private static final String ID_RESOURCE         = "patient/id/";
    private static final String DIGIT_RESOURCE      = "patient/digit/";
    private static final String PVT_DATE_RESOURCE   = "patient/pvt/";

    private static final boolean debug = false;
    private static final PatientDelegater instance;

    static {
        instance = new PatientDelegater();
    }

    public static PatientDelegater getInstance() {
        return instance;
    }

    private PatientDelegater() {
    }
    
    /**
     * 患者を追加する。
     * @param patient 追加する患者
     * @return PK
     */
    public long addPatient(PatientModel patient) {
        
        String json = getConverter().toJson(patient);

        String path = BASE_RESOURCE;

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        return Long.valueOf(entityStr);
    }
    
    /**
     * 患者を検索する。
     * @param pid 患者ID
     * @return PatientModel
     */
    public PatientModel getPatientById(String pid) {
        
        String path = ID_RESOURCE;

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        PatientModel patient = (PatientModel)
                getConverter().fromJson(entityStr, PatientModel.class);
        
        return patient;
    }
    
    /**
     * 患者を検索する。
     * @param spec PatientSearchSpec 検索仕様
     * @return PatientModel の Collection
     */
    public List<PatientModel> getPatients(PatientSearchSpec spec) {

        StringBuilder sb = new StringBuilder();

        switch (spec.getCode()) {

            case PatientSearchSpec.NAME_SEARCH:
                sb.append(NAME_RESOURCE);
                sb.append(spec.getName());
                break;

            case PatientSearchSpec.KANA_SEARCH:
                sb.append(KANA_RESOURCE);
                sb.append(spec.getName());
                break;

            case PatientSearchSpec.DIGIT_SEARCH:
                sb.append(DIGIT_RESOURCE);
                sb.append(spec.getDigit());
                break;

           case PatientSearchSpec.DATE_SEARCH:
                sb.append(PVT_DATE_RESOURCE);
                sb.append(spec.getDigit());
                break;
        }

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
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        List<PatientModel> list = (List<PatientModel>)
                getConverter().fromJson(entityStr, typeRef);
        
        if (list != null && !list.isEmpty()) {
            for (PatientModel pm : list) {
                decodeHealthInsurance(pm);
            }
        }
        return list;
    }

    /**
     * 患者を更新する。
     * @param patient 更新する患者
     * @return 更新数
     */
    public int updatePatient(PatientModel patient) {

        String json = getConverter().toJson(patient);

        String path = BASE_RESOURCE;

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)    
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        return Integer.parseInt(entityStr);
    }
    
    // patientIDリストからPatienteModelのリストを取得する
    public List<PatientModel> getPatientList(Collection patientIdList) {
        
        String path = BASE_RESOURCE + "list";
        String ids = getConverter().fromList(patientIdList);
        
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("ids", ids);
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);
        
        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        List<PatientModel> list = (List<PatientModel>)
                getConverter().fromJson(entityStr, typeRef);
        
        // 忘れがちｗ
        if (list != null && !list.isEmpty()) {
            for (PatientModel pm : list) {
                decodeHealthInsurance(pm);
            }
        }
        
        return list;
    }


    /**
     * バイナリの健康保険データをオブジェクトにデコードする。
     */
    private void decodeHealthInsurance(PatientModel patient) {

        // Health Insurance を変換をする beanXML2PVT
        Collection<HealthInsuranceModel> c = patient.getHealthInsurances();

        if (c != null && c.size() > 0) {

            for (HealthInsuranceModel model : c) {
                try {
                    // byte[] を XMLDecord
                    PVTHealthInsuranceModel hModel = (PVTHealthInsuranceModel)BeanUtils.xmlDecode(model.getBeanBytes());
                    patient.addPvtHealthInsurance(hModel);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            c.clear();
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
