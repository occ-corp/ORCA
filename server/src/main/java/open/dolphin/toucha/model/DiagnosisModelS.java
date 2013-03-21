package open.dolphin.toucha.model;

import open.dolphin.infomodel.RegisteredDiagnosisModel;

/**
 *
 * @author masuda, Masuda Naika
 */
public class DiagnosisModelS {
    
    private String diagnosis;
    private String category;
    private String outcome;
    private String started;
    private String ended;
    
    public DiagnosisModelS() {
    }
    
    public DiagnosisModelS(RegisteredDiagnosisModel rd) {
        diagnosis = rd.getDiagnosis();
        category = rd.getCategoryDesc();
        outcome = rd.getOutcomeDesc();
        started = rd.getStartDate();
        ended = rd.getEndDate();
    }
}
