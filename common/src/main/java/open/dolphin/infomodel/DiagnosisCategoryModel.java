package open.dolphin.infomodel;

import javax.persistence.Embeddable;

/**
 * Diagnosis のカテゴリーモデル。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
@Embeddable
public class DiagnosisCategoryModel extends InfoModel {
    
    private String diagnosisCategory;
    private String diagnosisCategoryDesc;
    private String diagnosisCategoryCodeSys;
    
    public DiagnosisCategoryModel() {
    }
    
    /**
     * @param category The category to set.
     */
    public void setDiagnosisCategory(String category) {
        this.diagnosisCategory = category;
    }
    /**
     * @return Returns the category.
     */
    public String getDiagnosisCategory() {
        return diagnosisCategory;
    }
    /**
     * @param categoryDesc The categoryDesc to set.
     */
    public void setDiagnosisCategoryDesc(String categoryDesc) {
        this.diagnosisCategoryDesc = categoryDesc;
    }
    /**
     * @return Returns the categoryDesc.
     */
    public String getDiagnosisCategoryDesc() {
        return diagnosisCategoryDesc;
    }
    /**
     * @param categoryCodeSys The categoryCodeSys to set.
     */
    public void setDiagnosisCategoryCodeSys(String categoryCodeSys) {
        this.diagnosisCategoryCodeSys = categoryCodeSys;
    }
    /**
     * @return Returns the categoryCodeSys.
     */
    public String getDiagnosisCategoryCodeSys() {
        return diagnosisCategoryCodeSys;
    }
    
    @Override
    public String toString() {
        return getDiagnosisCategoryDesc();
    }
}
