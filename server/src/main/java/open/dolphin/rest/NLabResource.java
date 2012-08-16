package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
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
    
    @Context
    private HttpServletRequest servletReq;
    
    public NLabResource() {
    }

    @GET
    @Path("module/{ptId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getLaboTest(@PathParam("ptId") String pid,
            @QueryParam("firstResult") Integer firstResult,
            @QueryParam("maxResult") Integer maxResult) {

        String fidPid = getFidPid(servletReq.getRemoteUser(), pid);

        List<NLaboModule> list = nLabServiceBean.getLaboTest(fidPid, firstResult, maxResult);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    @GET
    @Path("patient")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getConstrainedPatients(@QueryParam("ids") String ids) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());

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

        String fid = getRemoteFacility(servletReq.getRemoteUser());


        NLaboModule module = (NLaboModule) 
                getConverter().fromJson(json, NLaboModule.class);
        
        List<NLaboItem> items = module.getItems();
        // 関係を構築する
        if (items!=null && items.size()>0) {
            for (NLaboItem item : items) {
                item.setLaboModule(module);
            }
        }
        
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

        String fid = getRemoteFacility(servletReq.getRemoteUser());

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
//masuda$
    
    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
