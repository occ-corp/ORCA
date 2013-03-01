package open.dolphin.rest;

import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.session.ChartEventServiceBean;
import open.dolphin.session.PVTServiceBean;

/**
 * PVTResource2
 *
 * @author masuda, Masuda Naika
 */

@Path("rest/pvt2")
public class PVTResource2 extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private PVTServiceBean pvtServiceBean;
    
    @Inject
    private ChartEventServiceBean eventServiceBean;

    
    public PVTResource2() {
    }

    @POST
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response postPvt(String json) {

        PatientVisitModel model = (PatientVisitModel)
                getConverter().fromJson(json, PatientVisitModel.class);

        // 関係構築
        String fid = getRemoteFacility();
        model.setFacilityId(fid);
        //model.getPatientModel().setFacilityId(fid);

        Collection<HealthInsuranceModel> c = model.getPatientModel().getHealthInsurances();
        if (c!= null && c.size() > 0) {
            for (HealthInsuranceModel hm : c) {
                hm.setPatient(model.getPatientModel());
            }
        }

        int result = pvtServiceBean.addPvt(model);
        String cntStr = String.valueOf(result);
        debug(cntStr);

        return Response.ok(cntStr).build();
    }
    

    @DELETE
    @Path("{pvtPK}")
    public void deletePvt(@PathParam("pvtPK") String pkStr) {

        long pvtPK = Long.parseLong(pkStr);
        String fid = getRemoteFacility();

        int cnt = pvtServiceBean.removePvt(pvtPK, fid);

        debug(String.valueOf(cnt));
    }
    

    @GET
    @Path("pvtList")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPvtList() {
        
        String fid = getRemoteFacility();
        List<PatientVisitModel> model = eventServiceBean.getPvtList(fid);
        
        //String json = getConverter().toJson(model);
        //debug(json);
        //return json;
        
        StreamingOutput so = getJsonOutStream(model);
        
        return Response.ok(so).build();
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
