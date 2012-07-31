
package open.dolphin.infomodel;

import java.io.Serializable;
import javax.persistence.*;

/**
 * 中止項目のモデル
 *
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_disconitem")
public class DisconItemModel implements Serializable, Comparable {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    @Column(nullable=true)
    private String disconDate;
    
    @Column(nullable=false)
    private String itemName;

    @Column(nullable=true)
    private String memo;

    @Column
    private String facilityId;
    
    public DisconItemModel() {
    }

    public long getId() {
        return id;
    }

    public String getDate() {
        return disconDate;
    }

    public String getItemName() {
        return itemName;
    }

    public String getMemo() {
        return memo;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDate(String date) {
        disconDate = date;
    }

    public void setItemName(String name) {
        itemName = name;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setFacilityId(String fid) {
        facilityId = fid;
    }

    @Override
    public int compareTo(Object o) {
        String objDate = ((DisconItemModel) o).getDate();
        return disconDate.compareTo(objDate);

    }
 
}

