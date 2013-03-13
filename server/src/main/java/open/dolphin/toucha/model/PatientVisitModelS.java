package open.dolphin.toucha.model;
import java.io.Serializable;
import open.dolphin.infomodel.PatientVisitModel;

/**
 *
 * @author masuda, Masuda Naika
 */
public class PatientVisitModelS implements Serializable {
    
    private String number;
    private String pvtDate;
    private String patientId;
    private String patientName;
    private String patientAge;
    private String patientSex;
    private String department;
    
    public PatientVisitModelS() {
    }
    
    public PatientVisitModelS(PatientVisitModel pvt) {
        pvtDate = pvt.getPvtDate();
        patientId = pvt.getPatientId();
        patientName = pvt.getPatientName();
        patientAge = pvt.getPatientAgeBirthday();
        patientSex = pvt.getPatientModel().getGenderDesc();
        String dept = pvt.getDepartment();
        int pos = dept.indexOf(',');
        department = dept.substring(0, pos);
    }
    
    public void setNumber(String number) {
        this.number = number;
    }
    public void setPvtDate(String pvtDate) {
        this.pvtDate = pvtDate;
    }
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }
    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }
    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getNumber() {
        return number;
    }
    public String getPvtDate() {
        return pvtDate;
    }
    public String getPatientId() {
        return patientId;
    }
    public String getPatientName() {
        return patientName;
    }
    public String getPatientAge() {
        return patientAge;
    }
    public String getPatientSex() {
        return patientSex;
    }
    public String getDepartment() {
        return department;
    }
}
