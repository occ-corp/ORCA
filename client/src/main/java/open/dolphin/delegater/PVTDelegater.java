package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import java.util.List;
import open.dolphin.infomodel.PatientVisitModel;

/**
 * PVT 関連の Business Delegater　クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class PVTDelegater extends BusinessDelegater {

    private static final String RES_PVT = "pvt2/";
    
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

    public List<PatientVisitModel> getPvtList() {

        StringBuilder sb = new StringBuilder();
        sb.append(RES_PVT);
        sb.append("pvtList");
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

        TypeReference typeRef = new TypeReference<List<PatientVisitModel>>(){};
        List<PatientVisitModel> pvtList = (List<PatientVisitModel>)
                getConverter().fromJson(entityStr, typeRef);

        // 保険をデコード
        decodePvtHealthInsurance(pvtList);

        return pvtList;
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
