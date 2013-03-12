package open.dolphin.toucha.model;

import java.io.Serializable;

/**
 *
 * @author masuda, Masuda Naika
 */
public class PatientVisitModelS implements Serializable {
    
    private String pvtDate;
    private String patientId;
    private String patientName;
    private String patientAge;
    private String patientSex;
    private String department;
    
    public PatientVisitModelS() {
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
