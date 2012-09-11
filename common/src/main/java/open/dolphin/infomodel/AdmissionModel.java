package open.dolphin.infomodel;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * 入院モデル
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_admission")
public class AdmissionModel implements Serializable {

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
    
    private String doctor;
    
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

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    public String getPatientId() {
        return patientId;
    }
    
    public void setDoctorName(String name) {
        doctor = name;
    }
    public String getDoctorName() {
        return doctor;
    }
    
    
    @Override
    public AdmissionModel clone() {
        
        AdmissionModel ret = new AdmissionModel();
        ret.setStarted(started);
        ret.setEnded(ended);
        ret.setRoom(room);
        ret.setDepartment(dept);
        ret.setDoctorName(doctor);
        
        return ret;
    }
}
