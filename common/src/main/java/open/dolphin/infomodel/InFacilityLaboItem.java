
package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import javax.persistence.*;

/**
 * 施設内検査項目
 * 
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name="msd_facilityLabo")
public class InFacilityLaboItem implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    private int itemIndex;
    
    // Labo コード
    private String laboCode;    // facilityId

    // グループコード
    private String groupCode;

    // グループ名称
    private String groupName;

    // 検査項目コード・親
    private String parentCode;

    // MEDIS コード
    private String medisCode;

    // 検査項目名
    private String itemName;

    // 基準値
    private String normalValue;

    // 単位
    private String unit;

    // 検査材料コード
    private String specimenCode;

    // 検査材料名
    private String specimenName;
    
    // 値・性別などTransientなもの
    @JsonIgnore
    @Transient
    private String itemValue;
    
    public void setItemValue(String value) {
        itemValue = value;
    }
    public String getItemValue() {
        return itemValue;
    }

    @Transient
    private String abnormalFlg;
    public void setAbnormalFlg(String flg) {
        abnormalFlg = flg;
    }
    public String getAbnormalFlg() {
        return abnormalFlg;
    }

    // coustructor
    public InFacilityLaboItem() {
    }
    
    public InFacilityLaboItem(
            String medisCode,
            String itemName,
            String parentCode,
            String groupCode,
            String groupName,
            String specimenCode,
            String specimenName,
            String normalValue,
            String unit) {
        this.medisCode = medisCode;
        this.itemName = itemName;
        this.parentCode = parentCode;
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.specimenCode = specimenCode;
        this.specimenName = specimenName;
        this.normalValue = normalValue;
        this.unit = unit;
    }
    
    // getter
    public long getId() {
        return id;
    }
    public int getItemIndex() {
        return itemIndex;
    }
    public String getLaboCode() {
        return laboCode;
    }
    public String getGroupCode() {
        return groupCode;
    }
    public String getGroupName() {
        return groupName;
    }
    public String getParentCode() {
        return parentCode;
    }
    public String getMedisCode() {
        return medisCode;
    }
    public String getItemName() {
        return itemName;
    }
    public String getNormalValue() {
        return normalValue;
    }
    public String getUnit() {
        return unit;
    }
    public String getSpecimenCode() {
        return specimenCode;
    }
    public String getSpecimenName() {
        return specimenName;
    }
    
    // setter
    public void setId(long id) {
        this.id = id;
    }
    public void setItemIndex(int index) {
        this.itemIndex = index;
    }
    public void setLaboCode(String laboCode) {
        this.laboCode = laboCode;
    }
    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }
    public void setMedisCode(String medisCode) {
        this.medisCode = medisCode;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public void setNormalValue(String normalValue) {
        this.normalValue = normalValue;
    }
    public void setUnit(String unit) {
        this.unit = unit;
    }
    public void setSpecimenCode(String specimenCode) {
        this.specimenCode = specimenCode;
    }
    public void setSpecimenName(String specimenName) {
        this.specimenName = specimenName;
    }
}
