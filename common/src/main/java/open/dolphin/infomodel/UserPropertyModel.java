
package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import javax.persistence.*;

/**
 * クライアントの設定保存項目
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_userProperty")
public class UserPropertyModel implements Serializable {
    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    @Column(name = "c_key")
    private String key;
    
    @Column(name = "c_value", length = 16384)   // MySqlではLONGVARCHARになっちゃう…
    private String value;
    
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="facility_id", nullable=false)
    private FacilityModel facilityModel;
    
    public UserPropertyModel() {
    }
    
    public long getId() {
        return id;
    }
    public String getKey() {
        return key;
    }
    public String getValue() {
        return value;
    }
    public FacilityModel getFacilityModel() {
        return facilityModel;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public void setValue(String value){
        this.value = value;
    }
    public void setFacilityModel(FacilityModel model) {
        facilityModel = model;
    }
}
