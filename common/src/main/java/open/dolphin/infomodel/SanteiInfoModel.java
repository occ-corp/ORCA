
package open.dolphin.infomodel;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * 算定情報モデル
 *
 * @author masuda, Masuda Naika
 */
@Embeddable
public class SanteiInfoModel implements Serializable {

    // Ｃ管理
    private Boolean cancerCare = false;

    // 在宅時医学総合管理料・特定施設入居時医学総合管理料
    private Boolean zaitakuSougouKanri = false;

    // 在宅
    private Boolean homeMedicalCare = false;

    // 特定施設
    private Boolean nursingHomeMedicalCare = false;
    
    public SanteiInfoModel() {
    }
    
    public boolean isCancerCare() {
        return cancerCare;
    }
    public void setCancerCare(boolean b) {
        cancerCare = b;
    }
    public boolean isZaitakuSougouKanri() {
        return zaitakuSougouKanri;
    }
    public void setZaitakuSougouKanri(boolean b) {
        zaitakuSougouKanri = b;
    }
    public boolean isHomeMedicalCare() {
        return homeMedicalCare;
    }
    public void setHomeMedicalCare(boolean b) {
        homeMedicalCare = b;
    }
    public boolean isNursingHomeMedicalCare() {
        return nursingHomeMedicalCare;
    }
    public void setNursingHomeMedicalCare(boolean b) {
        nursingHomeMedicalCare = b;
    }
}
