package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.infomodel.LaboModuleValue;
import open.dolphin.infomodel.NLaboModule;
import open.dolphin.infomodel.PatientLiteModel;
import open.dolphin.infomodel.PatientModel;

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

        String path = "lab/patient";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("ids", fromList(idList));

        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<PatientLiteModel>>(){};
        List<PatientLiteModel> list = (List<PatientLiteModel>)
                getConverter().fromJsonTypeRef(entityStr, typeRef);
        
        return list;
    }
    
    /**
     * 検査結果を追加する。
     * @param value 追加する検査モジュール
     * @return      患者オブジェクト
     */
    public PatientModel postNLaboModule(NLaboModule value) {

        String path = "lab/module/";
        
        String json = getConverter().toJson(value);

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

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
     * ラボモジュールを検索する。
     * @param patientId     対象患者のID
     * @param firstResult   取得結果リストの最初の番号
     * @param maxResult     取得する件数の最大値
     * @return              ラボモジュールを採取日で降順に格納したリスト
     */
    public List<NLaboModule> getLaboTest(String patientId, int firstResult, int maxResult) {

        String path = "lab/module/" + patientId;
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("firstResult", String.valueOf(firstResult));
        qmap.add("maxResult", String.valueOf(maxResult));

        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<NLaboModule>>(){};
        List<NLaboModule> list = (List<NLaboModule>)
                getConverter().fromJsonTypeRef(entityStr, typeRef);
        
        return list;
    }

//masuda^   旧ラボ
    public PatientModel putMmlLaboModule(LaboModuleValue value) {

        String path = "lab/mmlModule";

        String json = getConverter().toJson(value);

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

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
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
//masuda$
}
