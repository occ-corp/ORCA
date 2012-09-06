package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.infomodel.*;
import open.dolphin.util.BeanUtils;

/**
 * MasudaDelegater
 *
 * @author masuda, Masuda Naika
 */
public class MasudaDelegater extends BusinessDelegater {
    

    private static final String RES_BASE = "masuda/";

    private static final boolean debug = false;
    private static final MasudaDelegater instance;

    static {
        instance = new MasudaDelegater();
    }

    public static MasudaDelegater getInstance() {
        return instance;
    }

    private MasudaDelegater() {
    }

    // 定期処方
    public List<RoutineMedModel> getRoutineMedModels(long karteId, int firstResult, int maxResults) {
        
        String path = RES_BASE + "routineMed/list/" + String.valueOf(karteId);
        
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("firstResult", String.valueOf(firstResult));
        qmap.add("maxResults", String.valueOf(maxResults));
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<RoutineMedModel>>(){};
        List<RoutineMedModel> list = (List<RoutineMedModel>) 
                getConverter().fromJson(entityStr, typeRef);

        // いつもデコード忘れるｗ
        for (RoutineMedModel model : list) {
            for (ModuleModel mm : model.getModuleList()) {
                mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
            }
        }
        
        return list;
    }
    
    public RoutineMedModel getRoutineMedModel(long id) {

        String path = RES_BASE + "routineMed/" + String.valueOf(id);
        
        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        RoutineMedModel model = (RoutineMedModel)
                getConverter().fromJson(entityStr, RoutineMedModel.class);
        
        if (model == null) {
            return null;
        }
        // いつもデコード忘れるｗ
        for (ModuleModel mm : model.getModuleList()) {
            mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
        }

        return model;
    }
    
    public void removeRoutineMedModel(RoutineMedModel model) {

        String path = RES_BASE + "routineMed";
        
        ClientResponse response = getResource(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .delete(ClientResponse.class);

        int status = response.getStatus();
        debug(status, "delete response");
    }
    
    public void addRoutineMedModel(RoutineMedModel model) {
        
        String path = RES_BASE + "routineMed";

        String json = getConverter().toJson(model);

        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);
    }
    
    public void updateRoutineMedModel(RoutineMedModel model) {
        
        String path = RES_BASE + "routineMed";

        String json = getConverter().toJson(model);

        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);
    }
    
    // 中止項目
    public List<DisconItemModel> getDisconItemModels() {

        String path = RES_BASE + "discon";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<DisconItemModel>>(){};
        List<DisconItemModel> list = (List<DisconItemModel>)
                getConverter().fromJson(entityStr, typeRef);

        return list;
    }

    public void addDisconItemModel(DisconItemModel model) {

        String path = RES_BASE + "discon";
        
        String json = getConverter().toJson(model);
        
        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);
    }

    public void removeDisconItemModel(DisconItemModel model) {

        String path = RES_BASE + "discon/" + String.valueOf(model.getId());

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .delete(ClientResponse.class);

        int status = response.getStatus();
        debug(status, "delete response");

    }

    public void updateDisconItemModel(DisconItemModel model) {
        
        String path = RES_BASE + "discon";
        
        String json = getConverter().toJson(model);

        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);
    }

    // 採用薬
    public List<UsingDrugModel> getUsingDrugModels() {
        
        String path = RES_BASE + "usingDrug";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        TypeReference typeRef = new TypeReference<List<UsingDrugModel>>(){};
        List<UsingDrugModel> list = (List<UsingDrugModel>)
                getConverter().fromJson(entityStr, typeRef);

        return list;
    }

    public void addUsingDrugModel(UsingDrugModel model) {

        String path = RES_BASE + "usingDrug";

        String json = getConverter().toJson(model);

        ClientResponse response = getResource(path, null)
                    .type(MEDIATYPE_JSON_UTF8)
                    .post(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);
    }

    public void removeUsingDrugModel(UsingDrugModel model) {

        String path = RES_BASE + "usingDrug/" + String.valueOf(model.getId());

        ClientResponse response = getResource(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .delete(ClientResponse.class);

        int status = response.getStatus();
        debug(status, "delete response");
    }

    public void updateUsingDrugModel(UsingDrugModel model) {
        
        String path = RES_BASE + "usingDrug";

        String json = getConverter().toJson(model);

        ClientResponse response = getResource(path, null)
                    .type(MEDIATYPE_JSON_UTF8)
                    .put(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);
    }

    // 指定したEntityのModuleModelをがさっと取ってくる
    public List<ModuleModel> getModulesEntitySearch(long karteId, Date fromDate, Date toDate, List<String> entities) {

        if (entities == null || entities.isEmpty()) {
            return null;
        }
        
        String path = RES_BASE + "moduleSearch/" + String.valueOf(karteId);
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fromDate", REST_DATE_FRMT.format(fromDate));
        qmap.add("toDate", REST_DATE_FRMT.format(toDate));
        qmap.add("entities", getConverter().fromList(entities));

        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);
        
        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<ModuleModel>>(){};
        List<ModuleModel> list = (List<ModuleModel>)
                getConverter().fromJson(entityStr, typeRef);
        
        for (ModuleModel module : list) {
            module.setModel((InfoModel) BeanUtils.xmlDecode(module.getBeanBytes()));
        }

        return list;
    }

    // FEV-70に患者情報を登録するときに使用する。PatientVisitを扱うが、ここに居候することにした
    public PatientVisitModel getLastPvtInThisMonth(PatientVisitModel pvt) {
        
        // long ptid は設定されていないのでだめ!
        //long ptId = pvt.getPatientModel().getId();
        String ptId = pvt.getPatientModel().getPatientId();

        String path = RES_BASE + "lastPvt/" + ptId;

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        PatientVisitModel model = (PatientVisitModel)
                getConverter().fromJson(entityStr, PatientVisitModel.class);

        return model;
    }

    // 指定したdocIdのDocinfoModelを取得する
    public List<DocInfoModel> getDocumentList(List<Long> docPkList) {

        if (docPkList == null || docPkList.isEmpty()) {
            return null;
        }

        String path = RES_BASE + "docList";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("ids", getConverter().fromList(docPkList));

        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<DocInfoModel>>(){};
        List<DocInfoModel> list = (List<DocInfoModel>)
                getConverter().fromJson(entityStr, typeRef);

        return list;
    }

    // Hibernate Searchの初期インデックスを作成する
    public String makeDocumentModelIndex(long fromDocPk, int maxResults) {

        String path = RES_BASE +"search/makeIndex";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fromDocPk", String.valueOf(fromDocPk));
        qmap.add("maxResults", String.valueOf(maxResults));

        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_TEXT_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        return entityStr;
    }

    // HibernteSearchによる全文検索
    public List<PatientModel> getKarteFullTextSearch(long karteId, String text) {

        String path = RES_BASE + "search/hibernate";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("karteId", String.valueOf(karteId));
        qmap.add("text", text);

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
        
        return list;
    }

    // grep方式の全文検索
    public SearchResultModel getSearchResult(String text, long fromId, int maxResult, boolean progressCourseOnly) {

        String path = RES_BASE + "search/grep";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("text", text);
        qmap.add("fromId", String.valueOf(fromId));
        qmap.add("maxResult", String.valueOf(maxResult));
        qmap.add("pcOnly", String.valueOf(progressCourseOnly));

        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        SearchResultModel model = (SearchResultModel)
                getConverter().fromJson(entityStr, SearchResultModel.class);
        
        return model;
    }

    // 検査履歴を取得する
    public List<ExamHistoryModel> getExamHistory(long karteId, Date fromDate, Date toDate) {
        
        String path = RES_BASE + "examHistory/" + String.valueOf(karteId);
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fromDate", REST_DATE_FRMT.format(fromDate));
        qmap.add("toDate", REST_DATE_FRMT.format(toDate));
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);
        
        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<ExamHistoryModel>>(){};
        List<ExamHistoryModel> list = (List<ExamHistoryModel>)
                getConverter().fromJson(entityStr, typeRef);
        
        return list;
    }
    
    // 処方切れ患者を検索する
    public List<PatientModel> getOutOfMedPatient(Date fromDate, Date toDate, int yoyuu) {
        
        String path = RES_BASE + "outOfMed";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fromDate", REST_DATE_FRMT.format(fromDate));
        qmap.add("toDate", REST_DATE_FRMT.format(toDate));
        qmap.add("yoyuu", String.valueOf(yoyuu));

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
        
        return list;
    }

    // 施設内検査
    public List<InFacilityLaboItem> getInFacilityLaboItemList() {
        
        String path = RES_BASE + "inFacilityLabo/list";
        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);
        
        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<InFacilityLaboItem>>(){};
        List<InFacilityLaboItem> list = (List<InFacilityLaboItem>)
                getConverter().fromJson(entityStr, typeRef);
        
        return list;
    }
    
    public void updateInFacilityLaboItemList(List<InFacilityLaboItem> list) {
        
        String path = RES_BASE + "inFacilityLabo/list";
        
        String json = getConverter().toJson(list);

        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);

    }
    
    // 電子点数表　未使用
    public String updateETensu1Table(List<ETensuModel1> list) {
        
        String path = RES_BASE + "etensu/update/";

        String json = getConverter().toJson(list);

        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);
        
        int status = response.getStatus();
        String ret = response.getEntity(String.class);
        debug(status, ret);

        if (status != HTTP200) {
            return null;
        }
        
        return ret;
    }

    public String initSanteiHistory(long fromId, int maxResults) {
        
        String path = RES_BASE + "santeiHistory/init";
        
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fromId", String.valueOf(fromId));
        qmap.add("maxResults", String.valueOf(maxResults));
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_TEXT_UTF8)
                .get(ClientResponse.class);
        
        int status = response.getStatus();
        String ret = response.getEntity(String.class);
        debug(status, ret);

        if (status != HTTP200) {
            return null;
        }
        
        return ret;
    }
    
    public List<SanteiHistoryModel> getSanteiHistory(long karteId, Date fromDate, Date toDate, List<String> srycds) {
        
        String path = RES_BASE + "santeiHistory/" + String.valueOf(karteId);
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fromDate", REST_DATE_FRMT.format(fromDate));
        qmap.add("toDate", REST_DATE_FRMT.format(toDate));
        if (srycds != null) {
            qmap.add("srycds", getConverter().fromList(srycds));
        }
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);
        
        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        TypeReference typeRef = new TypeReference<List<SanteiHistoryModel>>(){};
        List<SanteiHistoryModel> list = (List<SanteiHistoryModel>)
                getConverter().fromJson(entityStr, typeRef);
        
        return list;
    }
    
    
    public List<List<RpModel>> getRpHistory(long karteId, Date fromDate, Date toDate, boolean lastOnly) {
        
        String path = RES_BASE + "rpHistory/list/" + String.valueOf(karteId);
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fromDate", REST_DATE_FRMT.format(fromDate));
        qmap.add("toDate", REST_DATE_FRMT.format(toDate));
        qmap.add("lastOnly", String.valueOf(lastOnly));
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);
        
        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<List<RpModel>>>(){};
        List<List<RpModel>> list = (List<List<RpModel>>)
                getConverter().fromJson(entityStr, typeRef);
        
        return list;
    }
    
    public void postUserProperties(String userId, List<UserPropertyModel> list) {
        
        String path = RES_BASE + "userProperty/" + userId;
        
        String json = getConverter().toJson(list);
        
        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);
    }
    
    public List<UserPropertyModel> getUserProperties(String userId) {
        
        String path = RES_BASE + "userProperty/" + userId;
        
        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);
        
        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<UserPropertyModel>>(){};
        List<UserPropertyModel> list = (List<UserPropertyModel>) 
                getConverter().fromJson(entityStr, typeRef);
        
        return list;
    }
    
    // 入院患者を取得する
    public List<PatientModel> getAdmittedPatients(List<AdmissionModel> admissionList) {
        
        String path = RES_BASE + "admission/patients";
        
        String json = getConverter().toJson(admissionList);
        
        // ここは"get"だけど、admissionListを送りたいから"put"... いいのかなぁ
        ClientResponse response = getResource(path, null)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);
        
        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        List<PatientModel> list = (List<PatientModel>) 
                getConverter().fromJson(entityStr, typeRef);
        
        return list;
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
