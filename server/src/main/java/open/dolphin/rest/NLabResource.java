package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import open.dolphin.infomodel.*;
import open.dolphin.session.NLabServiceBean;

/**
 * MLabResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
@Path("lab")
public class NLabResource extends AbstractResource {

    private static final boolean debug = false;

    @Inject
    private NLabServiceBean nLabServiceBean;
    
    
    public NLabResource() {
    }

    @GET
    @Path("module/{ptId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getLaboTest(@PathParam("ptId") String pid,
            @QueryParam("firstResult") Integer firstResult,
            @QueryParam("maxResult") Integer maxResult) {

        StringBuilder sb = new StringBuilder();
        sb.append(getRemoteFacility());
        sb.append(IInfoModel.COMPOSITE_KEY_MAKER);
        sb.append(pid);
        String fidPid = sb.toString();

        List<NLaboModule> list = nLabServiceBean.getLaboTest(fidPid, firstResult, maxResult);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    @GET
    @Path("patient")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getConstrainedPatients(@QueryParam("ids") String ids) {

        String fid = getRemoteFacility();

        List<String> idList = getConverter().toStrList(ids);

        List<PatientLiteModel> list = nLabServiceBean.getConstrainedPatients(fid, idList);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    @POST
    @Path("module")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_JSON_UTF8)
    public String postNLaboTest(String json) {

        String fid = getRemoteFacility();


        NLaboModule module = (NLaboModule) 
                getConverter().fromJson(json, NLaboModule.class);
        
        PatientModel patient = nLabServiceBean.create(fid, module);

        String ret = getConverter().toJson(patient);
        debug(ret);

        return ret;
    }
    
//masuda^ 旧ラボ
    @POST
    @Path("mmlModule/")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_JSON_UTF8)
    public String postMmlLaboTest(String json) {

        String fid = getRemoteFacility();

        LaboModuleValue module = (LaboModuleValue)
                getConverter().fromJson(json, LaboModuleValue.class);
        
        // 関係を構築する ManyToOneのもの
        List<LaboSpecimenValue> specimens = module.getLaboSpecimens();
        if (specimens != null && !specimens.isEmpty()) {
            for (LaboSpecimenValue specimen : specimens) {
                specimen.setLaboModule(module);
                List<LaboItemValue> items = specimen.getLaboItems();
                if (items != null && !items.isEmpty()) {
                    for (LaboItemValue item : items) {
                        item.setLaboSpecimen(specimen);
                    }
                }
            }
        }
        
        PatientModel patient = nLabServiceBean.putLaboModule(fid, module);

        String ret = getConverter().toJson(patient);
        debug(ret);

        return ret;
    }
    
    @DELETE
    @Path("module/id/{id}")
    public void deleteNlaboModule(@PathParam("id") String idStr) {
        
        long id = Long.valueOf(idStr);

        int cnt = nLabServiceBean.deleteNlaboModule(id);
        String cntStr = String.valueOf(cnt);
        debug(cntStr);
    }
    
    @DELETE
    @Path("mmlModule/id/{id}")
    public void deleteMmlModule(@PathParam("id") String idStr) {
        
        long id = Long.valueOf(idStr);

        int cnt = nLabServiceBean.deleteMmlModule(id);
        String cntStr = String.valueOf(cnt);
        debug(cntStr);
    }
//masuda$
    
    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
