package open.dolphin.infomodel;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.*;

/**
 * 採用薬のモデル
 *
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_usingdrug")
public class UsingDrugModel implements Serializable, Comparable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    @Column(nullable = false)
    private int srycd;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = true)
    private String administration;
    
    @Column(nullable = true)
    private String usualDose;
    
    @Column(nullable = true)
    private String maxDose;
    
    @Column(nullable = false)
    private boolean valid;
    
    @Column
    @Temporal(value = TemporalType.DATE)
    private Date created;
    
    @Column
    private String facilityId;
    
    @Column(nullable = true)
    private Boolean hasLimit;
    
    
    public UsingDrugModel() {
    }

    public long getId() {
        return id;
    }

    public int getSrycd() {
        return srycd;
    }

    public String getName() {
        return name;
    }

    public String getAdmin() {
        return administration;
    }

    public String getUsualDose() {
        return usualDose;
    }

    public String getMaxDose() {
        return maxDose;
    }

    public boolean getValid() {
        return valid;
    }

    public Date getCreated() {
        return created;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public String getCreatedStr() {
        final SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        return frmt.format(created);
    }
    
    public boolean getHasLimit() {
        return hasLimit != null ? hasLimit : false;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setSrycd(int srycd) {
        this.srycd = srycd;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAdmin(String administration) {
        this.administration = administration;
    }

    public void setUsualDose(String dose) {
        usualDose = dose;
    }

    public void setMaxDose(String dose) {
        maxDose = dose;
    }

    public void setValid(boolean b) {
        valid = b;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setFacilityId(String fid) {
        facilityId = fid;
    }
    
    public void setHasLimit(boolean b) {
        hasLimit = b;
    }

    @Override
    public int compareTo(Object o) {
        String objName = ((UsingDrugModel) o).getName();
        return name.compareTo(objName);
    }
}
