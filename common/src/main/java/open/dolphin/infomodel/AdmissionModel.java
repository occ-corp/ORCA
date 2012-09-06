package open.dolphin.infomodel;

import java.util.Date;
import javax.persistence.*;

/**
 * 入院モデル
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_admission")
public class AdmissionModel implements IInfoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    @Column(nullable=false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date started;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date ended;
    
    private String room;
    
    private String dept;
    
    @ManyToOne
    @JoinColumn(name="patient_id")
    private PatientModel patient;
    
    @Transient
    private String patientId;

    
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return id;
    }
    
    public void setStarted(Date started) {
        this.started = started;
    }
    public Date getStarted() {
        return started;
    }
    
    public void setEnded(Date ended) {
        this.ended = ended;
    }
    public Date getEnded() {
        return ended;
    }
    
    public void setRoom(String room) {
        this.room = room;
    }
    public String getRoom() {
        return room;
    }
    
    public void setDepartment(String dept) {
        this.dept = dept;
    }
    public String getDepartment() {
        return dept;
    }
    
    public void setPatientModel(PatientModel pm) {
        patient = pm;
        patientId = (patientId != null) ? pm.getPatientId() : null;
     }
    public PatientModel getPatientModel() {
        return patient;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    public String getPatientId() {
        return patientId;
    }
}
