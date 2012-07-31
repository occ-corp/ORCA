
package open.dolphin.infomodel;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * MsdUpdaterModel
 * 
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_updater")
public class MsdUpdaterModel implements Serializable {
    
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    @Column
    @Temporal(value = TemporalType.DATE)
    private Date versionDate;
    
    private String moduleName;
    
    private String memo;
    
    @Column
    @Temporal(value = TemporalType.DATE)
    private Date updateDate;
    
    public MsdUpdaterModel() {
    }

    public long getId() {
        return id;
    }
    
    public void setVersionDate(Date d) {
        versionDate = d;
    }
    public Date getVersionDate() {
        return versionDate;
    }
    
    public void setModuleName(String name) {
        moduleName = name;
    }
    public String getModuleName() {
        return moduleName;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
    public String getMemo() {
        return memo;
    }
    
    public void setUpdateDate(Date d) {
        updateDate = d;
    }
    public Date getUpdateDate() {
        return updateDate;
    }
    
}
