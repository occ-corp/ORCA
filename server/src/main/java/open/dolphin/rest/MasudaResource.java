
package open.dolphin.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import open.dolphin.infomodel.*;
import open.dolphin.session.MasudaServiceBean;

/**
 * MasudaResource
 * @author masuda, Masuda Naika
 */
@Path("masuda")
public class MasudaResource extends AbstractResource {
    
    private static final boolean debug = false;
    
    @Inject
    private MasudaServiceBean masudaServiceBean;
    
    @Context
    private HttpServletRequest servletReq;
    
    public MasudaResource() {
    }
    
    @GET
    @Path("routineMed/list/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getRoutineMedModels(@PathParam("karteId") Long karteId,
                @QueryParam("firstResult") Integer firstResult,
                @QueryParam("maxResults") Integer maxResults) {
        
        List<RoutineMedModel> list = masudaServiceBean.getRoutineMedModels(karteId, firstResult, maxResults);
        
        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }
    
    @GET
    @Path("routineMed/{id}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getRoutineMedModel(@PathParam("param") Long id) {
        
        RoutineMedModel model = masudaServiceBean.getRoutineMedModel(id);
        
        String json = getConverter().toJson(model);
        debug(json);
        
        return json;
    }
    
    @DELETE
    @Path("routineMed/{id}/")
    public void removeRoutineMedModel(@PathParam("id") Long id) {

        long ret = masudaServiceBean.removeRoutineMedModel(id);

        debug(String.valueOf(ret));
    }
    
    @POST
    @Path("routineMed/")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String addRoutineMedModel(String json) {
        
        RoutineMedModel model = (RoutineMedModel)
                getConverter().fromJson(json, RoutineMedModel.class);
        
        long id = masudaServiceBean.addRoutineMedModel(model);
        
        String ret = String.valueOf(id);
        debug(ret);
        return ret;
    }
    
    @PUT
    @Path("routineMed/")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String updateRoutineMedModel(String json) {
        
        RoutineMedModel model = (RoutineMedModel)
                getConverter().fromJson(json, RoutineMedModel.class);
        
        long id = masudaServiceBean.updateRoutineMedModel(model);
        
        String ret = String.valueOf(id);
        debug(ret);
        return ret;
    }
    
    // 中止項目
    @GET
    @Path("discon")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getDisconItemModels() {

        // 施設
        String fid = getRemoteFacility(servletReq.getRemoteUser());

        List<DisconItemModel> list = masudaServiceBean.getDisconItems(fid);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    @POST
    @Path("discon")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String addDisconItemModel(String json) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());

        DisconItemModel model = (DisconItemModel)
                getConverter().fromJson(json, DisconItemModel.class);
        model.setFacilityId(fid);

        long id = masudaServiceBean.addDisconItem(model);
        String ret = String.valueOf(id);
        debug(ret);
        return ret;
    }

    @PUT
    @Path("discon")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String updateDiconItemModel(String json) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());

        DisconItemModel model = (DisconItemModel)
                getConverter().fromJson(json, DisconItemModel.class);
        model.setFacilityId(fid);

        long id = masudaServiceBean.updateDisconItem(model);
        String ret = String.valueOf(id);
        debug(ret);
        return ret;
    }

    @DELETE
    @Path("discon/{param}")
    public void removeDisconItem(@PathParam("param") String param) {

        long id = Long.valueOf(param);

        long ret = masudaServiceBean.removeDisconItem(id);

        debug(String.valueOf(ret));
    }

    // 採用薬
    @GET
    @Path("usingDrug")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getUsingDrugModels() {

        // 施設
        String fid = getRemoteFacility(servletReq.getRemoteUser());

        List<UsingDrugModel> list = masudaServiceBean.getUsingDrugModels(fid);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    @POST
    @Path("usingDrug")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String addUsingDrugModel(String json) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());
        
        UsingDrugModel model = (UsingDrugModel)
                getConverter().fromJson(json, UsingDrugModel.class);
        model.setFacilityId(fid);

        long id = masudaServiceBean.addUsingDrugModel(model);
        String ret = String.valueOf(id);
        debug(ret);
        return ret;
    }

    @PUT
    @Path("usingDrug")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String updateUsingDrugModel(String json) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());

        UsingDrugModel model = (UsingDrugModel)
                getConverter().fromJson(json, UsingDrugModel.class);
        model.setFacilityId(fid);

        long id = masudaServiceBean.updateUsingDrugModel(model);
        String ret = String.valueOf(id);
        debug(ret);
        return ret;
    }

    @DELETE
    @Path("usingDrug/{param}")
    public void removeUsingDrugModel(@PathParam("param") String param) {

        long id = Long.parseLong(param);

        long ret = masudaServiceBean.removeUsingDrugModel(id);

        debug(String.valueOf(ret));
    }

    // 指定したEntityのModuleModelをまとめて取得
    @GET
    @Path("moduleSearch/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getModulesEntitySearch(@PathParam("karteId") Long karteId,
                @QueryParam("fromDate") String fromDateStr,
                @QueryParam("toDate") String toDateStr,
                @QueryParam("entities") String entitiesStr) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());

        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        
        List<String> entities = getConverter().toStrList(entitiesStr);

        List<ModuleModel> list = masudaServiceBean.getModulesEntitySearch(fid, karteId, fromDate, toDate, entities);
        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    // 今月の最後の受診日を調べる
    @GET
    @Path("lastPvt/{ptId}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getLastPvtInThisMonth(@PathParam("ptId") String ptId) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());
        
        PatientVisitModel model = masudaServiceBean.getLastPvtInThisMonth(fid, ptId);
        
        String json = getConverter().toJson(model);
        debug(json);
        
        return json;
    }

    // 指定したidのdocInfoをまとめて取得、検索結果で使用
    @GET
    @Path("docList")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getDocumentList(@QueryParam("ids") String ids) {

        List<Long> docPkList = getConverter().toLongList(ids);

        List<DocInfoModel> list = masudaServiceBean.getDocumentList(docPkList);
        
        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    @GET
    @Path("search/makeIndex")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String makeDocumentModelIndex(
            @QueryParam("fromDocPk") Long fromDocPk, 
            @QueryParam("maxResults") Integer maxResults) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());

        String ret = masudaServiceBean.makeDocumentModelIndex(fid, fromDocPk, maxResults);

        debug(ret);
        return ret;
    }

    @GET
    @Path("search/hibernate")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getKarteFullTextSearch(
            @QueryParam("karteId") Long karteId,
            @QueryParam("text") String text) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());

        List<PatientModel> list = masudaServiceBean.getKarteFullTextSearch(fid, karteId, text);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    @GET
    @Path("search/grep/{param}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getSearchResult(
            @QueryParam("text") String text,
            @QueryParam("fromId") Long fromId,
            @QueryParam("maxResult") Integer maxResult,
            @QueryParam("pcOnly") Boolean progressCourseOnly) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());

        SearchResultModel model = masudaServiceBean.getSearchResult(fid, text, fromId, maxResult, progressCourseOnly);

        String json = getConverter().toJson(model);
        debug(json);
        
        return json;
    }
    
    @GET
    @Path("examHistory/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getExamHistory(@PathParam("karteId") Long karteId,
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr) {
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        
        List<ExamHistoryModel> list = masudaServiceBean.getExamHistory(fid, karteId, fromDate, toDate);
        
        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }
    
    @GET
    @Path("outOfMed")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getOutOfMedStockPatient(
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("yoyuu") Integer yoyuu) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());
        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);

        List<PatientModel> list = masudaServiceBean.getOutOfMedStockPatient(fid, fromDate, toDate, yoyuu);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }
    
    @GET
    @Path("inFacilityLabo/list")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getInFacilityLaboItemList() {
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        List<InFacilityLaboItem> list = masudaServiceBean.getInFacilityLaboItemList(fid);
        
        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }
    
    @PUT
    @Path("inFacilityLabo/list")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String updateInFacilityLaboItem(String json) {
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        
        TypeReference typeRef = new TypeReference<List<InFacilityLaboItem>>(){};
        List<InFacilityLaboItem> list = (List<InFacilityLaboItem>)
                getConverter().fromJson(json, typeRef);
        
        long ret = masudaServiceBean.updateInFacilityLaboItem(fid, list);
        
        return String.valueOf(ret);
    }

    // 電子点数表　未使用
    @POST
    @Path("etensu/update")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String updateETensu1Table(String json) {
        
        TypeReference typeRef = new TypeReference<List<ETensuModel1>>(){};
        List<ETensuModel1> list = (List<ETensuModel1>)
                getConverter().fromJson(json, typeRef);
        
        String ret = masudaServiceBean.updateETensu1Table(list);
        
        return ret;
    }
    
    @GET
    @Path("santeiHistory/init")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String initSanteiHistory(
            @QueryParam("fromId") Long fromIndex,
            @QueryParam("maxResults") Integer maxResults) {
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        
        String ret = masudaServiceBean.initSanteiHistory(fid, fromIndex, maxResults);
        
        return ret;
    }
    
    @GET
    @Path("santeiHistory/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getSanteiHistory(@PathParam("karteId") Long karteId,
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("srycds") String srycds) {
    
        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        List<String> srycdList = null;
        if (srycds != null && !srycds.isEmpty()) {
            srycdList = getConverter().toStrList(srycds);
        }
        
        List<SanteiHistoryModel> list = masudaServiceBean.getSanteiHistory(karteId, fromDate, toDate, srycdList);
        
        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }
    
    @GET
    @Path("rpHistory/list/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getRpHistory(@PathParam("karteId") Long karteId,
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("lastOnly") Boolean lastOnly) {

        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        
        List<List<RpModel>> list = masudaServiceBean.getRpModelList(karteId, fromDate, toDate, lastOnly);
        
        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }
    
    @POST
    @Path("userProperty/{fid}")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String postUserProperties(@PathParam("fid") String fid, String json) {
        
        TypeReference typeRef = new TypeReference<List<UserPropertyModel>>(){};
        List<UserPropertyModel> list = (List<UserPropertyModel>) 
                getConverter().fromJson(json, typeRef);
        
        int cnt = masudaServiceBean.postUserProperties(fid, list);
        return String.valueOf(cnt);
    }
    
    @GET
    @Path("userProperty/{fid}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getUserProperties(@PathParam("fid") String fid) {
        
        List<UserPropertyModel> list = masudaServiceBean.getUserProperties(fid);
        String json = getConverter().toJson(list);
        
        return json;
    }
    
    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
    
    
}