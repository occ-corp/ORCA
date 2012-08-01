
package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

/**
 * 定期処方薬の延滞茶
 * 
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_routineMed")
public class RoutineMedModel implements Serializable, Comparable, Cloneable{
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
    
    @Id 
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    @Column(nullable=false)
    private long karteId;
    
    @Column(nullable=false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date registDate;
    
    private Boolean bookmark = false;
    
    private String memo;
    
    @JsonDeserialize(contentAs=ModuleModel.class)
    @ElementCollection
    @CollectionTable(name="msd_routineMed_moduleList")
    @OneToMany(fetch=FetchType.LAZY)    // PostgresだとEAGERだめ。MySQLは大丈夫
    private List<ModuleModel> moduleList;
    
    @JsonIgnore
    @Transient
    private String status;
    
    public RoutineMedModel() {
    }
    
    public long getId() {
        return id;
    }
    
    public long getKarteId() {
        return karteId;
    }
    
    public Date getRegistDate() {
        return registDate;
    }

    public boolean getBookmark() {
        return bookmark;
    }

    public String getMemo() {
        return memo;
    }
    
    public List<ModuleModel> getModuleList() {
        return moduleList;
    }
    
    public void setId(long id) {
        this.id = id;
    }

    public void setKarteId(long karteId) {
        this.karteId = karteId;
    }

    public void setRegistDate(Date registDate) {
        this.registDate = registDate;
    }

    public void setBookmark(boolean b) {
        bookmark = b;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setModuleList(List<ModuleModel> list) {
        moduleList = list;
    }
    
    // for display
    public String getRegistDateStr() {
        return sdf.format(registDate);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int compareTo(Object o) {
        RoutineMedModel target = (RoutineMedModel) o;
        return registDate.compareTo(target.getRegistDate());
    }

    @Override
    public RoutineMedModel clone() {
        try {
            return (RoutineMedModel) super.clone();
        } catch (Exception ex) {
        }
        return null;
    }
}
