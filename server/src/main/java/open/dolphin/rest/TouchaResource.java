package open.dolphin.rest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.session.KarteServiceBean;
import open.dolphin.session.PVTServiceBean;
import open.dolphin.toucha.model.PatientVisitModelS;


/**
 *
 * @author masuda, Masuda Naika
 */
@Path("toucha")
public class TouchaResource extends AbstractResource {
    
    private static final boolean debug = false;
    
    @Inject
    private PVTServiceBean pvtServiceBean;
    
    @Inject
    private KarteServiceBean karteServiceBean;
   
    @GET
    @Path("hello")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response helloDolphin() {
        return Response.ok("Hello Dolphin").build();
    }
    
    @GET
    @Path("pvt")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPvtList(@QueryParam("pvtDate") String pvtDate) {
        
        String fid = getRemoteFacility();
        
        if (pvtDate == null) {
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
            pvtDate = frmt.format(new Date());
        }
        
        List<PatientVisitModelS> pvtList = pvtServiceBean.getPvtList(fid, pvtDate);
        
        StreamingOutput so = getJsonOutStream(pvtList);

        return Response.ok(so).build();
    }
    
    @GET
    @Path("document")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getDocument(@QueryParam("ptId") String ptId, @QueryParam("docPk") Long docPk) {
        
        String fid = getRemoteFacility();
        String ret = karteServiceBean.getDocHtml(fid, ptId, docPk);
        
        return Response.ok(ret).build();
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
