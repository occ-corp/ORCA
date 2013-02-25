package open.dolphin.rest;

import javax.servlet.AsyncContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import open.dolphin.infomodel.ClaimMessageModel;
import open.dolphin.infomodel.OrcaSqlModel;
import open.dolphin.server.orca.OrcaService;

/**
 * OrcaResource
 * @author masuda, Masuda Naika
 */
@Path("rest/orca")
public class OrcaResource extends AbstractResource {
    
    public static final String CLAIMRES_URL = "/rest/orca/claimres";
    
    private static final boolean debug = false;
    
    public OrcaResource() {
    }
    
    @POST
    @Path("query")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_JSON_UTF8)
    public String executeQuery(String json) {
        
        OrcaSqlModel sqlModel = (OrcaSqlModel) 
                getConverter().fromJson(json, OrcaSqlModel.class);
        
        OrcaService.getInstance().executeSql(sqlModel);
        
        json = getConverter().toJson(sqlModel);
        debug(json);
        
        return json;
    }
    
    @POST
    @Path("claim")
    @Consumes(MEDIATYPE_JSON_UTF8)
    public void postClaim(String json) {

        ClaimMessageModel model = (ClaimMessageModel)
                getConverter().fromJson(json, ClaimMessageModel.class);
        
        AsyncContext ac = servletReq.startAsync();
        ac.getRequest().setAttribute(ClaimMessageModel.class.getSimpleName(), model);
        OrcaService.getInstance().sendClaim(ac);
    }
    
    @POST
    @Path("claimres")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_JSON_UTF8)
    public String dispachClaimResponse() {
        
        ClaimMessageModel model = (ClaimMessageModel) 
                servletReq.getAttribute(ClaimMessageModel.class.getSimpleName());
        model.setContent(null);
        String json = getConverter().toJson(model);
        
        return json;
    }
    
    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
