package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.session.TouchaServiceBean;
import open.dolphin.toucha.model.DiagnosisModelS;
import open.dolphin.toucha.model.DocumentModelS;
import open.dolphin.toucha.model.PatientModelS;
import open.dolphin.toucha.model.PatientVisitModelList;


/**
 * TouchaResource
 * @author masuda, Masuda Naika
 */
@Path("toucha")
public class TouchaResource extends AbstractResource {
    
    private static final boolean debug = false;
    
    @Inject
    private TouchaServiceBean touchaServiceBean;
   
    
    @GET
    @Path("hello")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response helloDolphin() {
        return Response.ok("Hello Dolphin").build();
    }
    
    @GET
    @Path("diagnosis/{ptId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getDiagnosis(@PathParam("ptId") String ptId) {
        
        String fid = getRemoteFacility();
        
        List<DiagnosisModelS> sList = touchaServiceBean.getDiagnosis(fid, ptId);
        
        StreamingOutput so = getJsonOutStream(sList);

        return Response.ok(so).build();
    }
    
    @GET
    @Path("pvt/{pvtDate}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPvtList(
            @PathParam("pvtDate") String pvtDate, 
            @QueryParam("direction") String direction) {
        
        String fid = getRemoteFacility();
        PatientVisitModelList model= touchaServiceBean.getPvtList(fid, pvtDate, direction);

        StreamingOutput so = getJsonOutStream(model);

        return Response.ok(so).build();
    }
    
    @GET
    @Path("document/{docPk}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getDocument(@PathParam("docPk") String docPkStr,
            @QueryParam("patientId") String ptId,
            @QueryParam("docDate") String docDateStr,
            @QueryParam("direction") String direction) {
        
        String fid = getRemoteFacility();
        DocumentModelS model = touchaServiceBean.getDocHtml(fid, ptId, docPkStr, docDateStr, direction);
        
        StreamingOutput so = getJsonOutStream(model);
        
        return Response.ok(so).build();
    }
    
    @GET
    @Path("patient/{ptId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientModel(@PathParam("ptId") String ptId) {
        
        String fid = getRemoteFacility();
        PatientModelS model = touchaServiceBean.getPatientModel(fid, ptId);
        StreamingOutput so = getJsonOutStream(model);
        
        return Response.ok(so).build();
    }
    
    @GET
    @Path("search")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getSearchResults(@QueryParam("text") String text ,@QueryParam("type") String type) {
        
        String fid = getRemoteFacility();
        List<PatientModelS> list = touchaServiceBean.getSearchResults(fid, text, type);
        StreamingOutput so = getJsonOutStream(list);
        
        return Response.ok(so).build();
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
