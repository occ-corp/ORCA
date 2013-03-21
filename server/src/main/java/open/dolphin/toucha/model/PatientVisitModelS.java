package open.dolphin.toucha.model;
import open.dolphin.infomodel.PatientVisitModel;

/**
 *
 * @author masuda, Masuda Naika
 */
public class PatientVisitModelS {
    
    private String pvtDate;
    private String patientId;
    private String patientName;
    private String patientAge;
    private String patientSex;
    private String department;
    
    public PatientVisitModelS() {
    }
    
    public PatientVisitModelS(PatientVisitModel pvt) {
        setModel(pvt);
    }

    public final void setModel(PatientVisitModel pvt) {
        pvtDate = pvt.getPvtDate();
        patientId = pvt.getPatientId();
        patientName = pvt.getPatientName();
        patientAge = pvt.getPatientModel().getAge();
        patientSex = pvt.getPatientModel().getGenderDesc();
        String dept = pvt.getDepartment();
        int pos = dept.indexOf(',');
        department = dept.substring(0, pos);
    }
}
