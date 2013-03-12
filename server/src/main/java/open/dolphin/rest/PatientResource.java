package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.session.PatientServiceBean;

/**
 * PatientResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("patient")
public class PatientResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private PatientServiceBean patientServiceBean;
    
    
    public PatientResource() {
    }


    @GET
    @Path("name/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientsByName(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String name = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByName(fid, name);
        
        StreamingOutput so = getJsonOutStream(patients);
        
        return Response.ok(so).build();
    }


    @GET
    @Path("kana/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientsByKana(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String kana = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByKana(fid, kana);
        
        StreamingOutput so = getJsonOutStream(patients);
        
        return Response.ok(so).build();
    }
    

    @GET
    @Path("digit/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientsByDigit(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String digit = param;
        debug(fid);
        debug(digit);

        List<PatientModel> patients = patientServiceBean.getPatientsByDigit(fid, digit);
        
        StreamingOutput so = getJsonOutStream(patients);
        
        return Response.ok(so).build();
    }


    @GET
    @Path("id/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientById(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String pid = param;

        PatientModel patient = patientServiceBean.getPatientById(fid, pid);
        
        StreamingOutput so = getJsonOutStream(patient);
        
        return Response.ok(so).build();
    }

    @GET
    @Path("pvt/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientsByPvt(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String pvtDate = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByPvtDate(fid, pvtDate);
        
        StreamingOutput so = getJsonOutStream(patients);
        
        return Response.ok(so).build();
    }


    @POST
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response postPatient(String json) {

        String fid = getRemoteFacility();

        PatientModel patient = (PatientModel)
                getConverter().fromJson(json, PatientModel.class);
        patient.setFacilityId(fid);

        long pk = patientServiceBean.addPatient(patient);
        String pkStr = String.valueOf(pk);
        debug(pkStr);

        return Response.ok(pkStr).build();
    }


    @PUT
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putPatient(String json) {

        String fid = getRemoteFacility();

        PatientModel patient = (PatientModel)
                getConverter().fromJson(json, PatientModel.class);
        patient.setFacilityId(fid);

        int cnt = patientServiceBean.update(patient);
        String pkStr = String.valueOf(cnt);
        debug(pkStr);

        return Response.ok(pkStr).build();
    }
    
    @GET
    @Path("list")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientList(@QueryParam("ids") String ids) {
        
        String fid = getRemoteFacility();
        List<String> idList = getConverter().toStrList(ids);
        
        List<PatientModel> list = patientServiceBean.getPatientList(fid, idList);
        
        StreamingOutput so = getJsonOutStream(list);
        
        return Response.ok(so).build();
    }

    @GET
    @Path("insurances/{id}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getHealthInsurances(@PathParam("id") Long pk) {
        
        List<HealthInsuranceModel> list = patientServiceBean.getHealthInsurances(pk);
        
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
