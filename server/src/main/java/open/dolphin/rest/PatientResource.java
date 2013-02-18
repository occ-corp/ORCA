package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.session.PatientServiceBean;

/**
 * PatientResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("rest/patient")
public class PatientResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private PatientServiceBean patientServiceBean;
    
    
    public PatientResource() {
    }


    @GET
    @Path("name/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getPatientsByName(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String name = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByName(fid, name);

        String json = getConverter().toJson(patients);
        debug(json);
        
        return json;
    }


    @GET
    @Path("kana/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getPatientsByKana(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String kana = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByKana(fid, kana);

        String json = getConverter().toJson(patients);
        debug(json);
        
        return json;
    }
    

    @GET
    @Path("digit/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getPatientsByDigit(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String digit = param;
        debug(fid);
        debug(digit);

        List<PatientModel> patients = patientServiceBean.getPatientsByDigit(fid, digit);

        String json = getConverter().toJson(patients);
        debug(json);
        
        return json;
    }


    @GET
    @Path("id/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getPatientById(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String pid = param;

        PatientModel patient = patientServiceBean.getPatientById(fid, pid);

        String json = getConverter().toJson(patient);
        debug(json);
        
        return json;
    }

    @GET
    @Path("pvt/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getPatientsByPvt(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String pvtDate = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByPvtDate(fid, pvtDate);

        String json = getConverter().toJson(patients);
        debug(json);
        
        return json;
    }


    @POST
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String postPatient(String json) {

        String fid = getRemoteFacility();

        PatientModel patient = (PatientModel)
                getConverter().fromJson(json, PatientModel.class);
        patient.setFacilityId(fid);

        long pk = patientServiceBean.addPatient(patient);
        String pkStr = String.valueOf(pk);
        debug(pkStr);

        return pkStr;
    }


    @PUT
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putPatient(String json) {

        String fid = getRemoteFacility();

        PatientModel patient = (PatientModel)
                getConverter().fromJson(json, PatientModel.class);
        patient.setFacilityId(fid);

        int cnt = patientServiceBean.update(patient);
        String pkStr = String.valueOf(cnt);
        debug(pkStr);

        return pkStr;
    }
    
    @GET
    @Path("list")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getPatientList(@QueryParam("ids") String ids) {
        
        String fid = getRemoteFacility();
        List<String> idList = getConverter().toStrList(ids);
        
        List<PatientModel> list = patientServiceBean.getPatientList(fid, idList);
        
        String json = getConverter().toJson(list);
        
        return json;
    }

    @GET
    @Path("insurances/{id}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getHealthInsurances(@PathParam("id") Long pk) {
        
        List<HealthInsuranceModel> list = patientServiceBean.getHealthInsurances(pk);
        
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
