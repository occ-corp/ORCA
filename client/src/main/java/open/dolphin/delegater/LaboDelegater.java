package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.infomodel.LaboModuleValue;
import open.dolphin.infomodel.NLaboModule;
import open.dolphin.infomodel.PatientLiteModel;
import open.dolphin.infomodel.PatientModel;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

/**
 * Labo 関連の Delegater クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class LaboDelegater extends BusinessDelegater {
    
    private static final boolean debug = false;
    private static final LaboDelegater instance;

    static {
        instance = new LaboDelegater();
    }

    public static LaboDelegater getInstance() {
        return instance;
    }

    private LaboDelegater() {
    }

    public List<PatientLiteModel> getConstrainedPatients(List<String> idList) {
        
        try {
            String path = "lab/patient";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("ids", getConverter().fromList(idList));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<PatientLiteModel>>(){};
            List<PatientLiteModel> list = (List<PatientLiteModel>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    /**
     * 検査結果を追加する。
     * @param value 追加する検査モジュール
     * @return      患者オブジェクト
     */
    public PatientModel postNLaboModule(NLaboModule value) {
        
        try {
            String path = "lab/module/";
            
            String json = getConverter().toJson(value);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            PatientModel patient = (PatientModel)
                    getConverter().fromJson(entityStr, PatientModel.class);
            
            return patient;
            
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * ラボモジュールを検索する。
     * @param patientId     対象患者のID
     * @param firstResult   取得結果リストの最初の番号
     * @param maxResult     取得する件数の最大値
     * @return              ラボモジュールを採取日で降順に格納したリスト
     */
    public List<NLaboModule> getLaboTest(String patientId, int firstResult, int maxResult) {
        
        try {
            String path = "lab/module/" + patientId;
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("firstResult", String.valueOf(firstResult));
            qmap.add("maxResult", String.valueOf(maxResult));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<NLaboModule>>(){};
            List<NLaboModule> list = (List<NLaboModule>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
        } catch (Exception ex) {
            return null;
        }
    }

//masuda^   旧ラボ
    public PatientModel putMmlLaboModule(LaboModuleValue value) {
        
        try {
            String path = "lab/mmlModule";

            String json = getConverter().toJson(value);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            PatientModel patient = (PatientModel) 
                    getConverter().fromJson(entityStr, PatientModel.class);
            
            decodeHealthInsurance(patient);
            
            return patient;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    // 削除
    public int deleteNlaboModule(long id) {
        
        try {
            String path = "lab/module/id/" + String.valueOf(id);
            
            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .delete(ClientResponse.class);

            int status = response.getStatus();
            
            debug(status, "delete response");

            return 1;
        } catch (Exception ex) {
            return -1;
        }
    }
    
    public int deleteMmlLaboModule(long id) {
        
        try {
            String path = "lab/mmlModule/id/" + String.valueOf(id);
            
            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .delete(ClientResponse.class);

            int status = response.getStatus();
            
            debug(status, "delete response");

            return 1;
            
        } catch (Exception ex) {
            return -1;
        }
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
//masuda$
}
