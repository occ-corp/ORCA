package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * 算定履歴を記録する
 * 
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_santeiHistory")
public class SanteiHistoryModel implements Serializable {
    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    @Column(nullable=false)
    private String srycd;
    
    private int itemCount;
    
    @JsonIgnore
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(nullable=false) // "moduleModel_id"
    private ModuleModel moduleModel; 
    
    private int itemIndex;
    
//transient^
    @Transient
    private Date santeiDate;
    
    public Date getSanteiDate() {
        return santeiDate;
    }
    public void setSanteiDate(Date date) {
        this.santeiDate = date;
    }
    
    @Transient
    private String itemName;
    
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    @JsonIgnore
    @Transient
    private ETensuModel1 eTensuModel1;
    
    public ETensuModel1 getETensuModel1() {
        return eTensuModel1;
    }
    public void setETensuModel1(ETensuModel1 model) {
        eTensuModel1 = model;
    }
//transient$
    
    public SanteiHistoryModel() {
    }
    
    public long getId() {
        return id;
    }

    public String getSrycd() {
        return srycd;
    }
    
    public int getItemCount() {
        return itemCount;
    }

    public ModuleModel getModuleModel() {
        return moduleModel;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public void setId(long id) {
        this.id = id;
    }
    
    public void setSrycd(String srycd) {
        this.srycd = srycd;
    }
    
    public void setItemCount(int count) {
        this.itemCount = count;
    }
    
    public void setModuleModel(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
        if (moduleModel != null) {
            santeiDate = moduleModel.getStarted();
            
        }
    }
    
    public void setItemIndex(int index) {
        this.itemIndex = index;
    }
}
