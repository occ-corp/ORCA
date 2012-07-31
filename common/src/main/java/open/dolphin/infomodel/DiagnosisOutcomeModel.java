package open.dolphin.infomodel;

import javax.persistence.Embeddable;

/**
 * Diagnosis のカテゴリーモデル。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
@Embeddable
public class DiagnosisOutcomeModel extends InfoModel {
    
    private String outcome;
    private String outcomeDesc;
    private String outcomeCodeSys;
    
    public DiagnosisOutcomeModel() {
    }
    
    @Override
    public String toString() {
        return getOutcomeDesc();
    }
    
    /**
     * @param outcome The outcome to set.
     */
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
    
    /**
     * @return Returns the outcome.
     */
    public String getOutcome() {
        return outcome;
    }
    
    /**
     * @param outcomeDesc The outcomeDesc to set.
     */
    public void setOutcomeDesc(String outcomeDesc) {
        this.outcomeDesc = outcomeDesc;
    }
    
    /**
     * @return Returns the outcomeDesc.
     */
    public String getOutcomeDesc() {
        return outcomeDesc;
    }
    
    /**
     * @param outcomeCodeSys The outcomeCodeSys to set.
     */
    public void setOutcomeCodeSys(String outcomeCodeSys) {
        this.outcomeCodeSys = outcomeCodeSys;
    }
    
    /**
     * @return Returns the outcomeCodeSys.
     */
    public String getOutcomeCodeSys() {
        return outcomeCodeSys;
    }
}
