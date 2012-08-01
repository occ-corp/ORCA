
package open.dolphin.infomodel;

/**
 * 転送用のPatientMemoModel
 * 
 * @author masuda, Masuda nIka
 */
public class PatientMemoTransferModel extends AbstractKarteEntryTransferModel {

    private String memo;
    
    public PatientMemoTransferModel() {
    }
    
    @Override
    public PatientMemoModel getKarteEntryBean() {
        PatientMemoModel model = new PatientMemoModel();
        restore(model);
        model.setMemo(memo);
        return model;
    }

    @Override
    public void setKarteEntryBean(KarteEntryBean karteEntryBean) {
        store(karteEntryBean);
        PatientMemoModel model = (PatientMemoModel) karteEntryBean;
        memo = model.getMemo();
    }
    
    public String getMemo() {
        return memo;
    }
    
    public void setMemo(String memo) {
        this.memo = memo;
    }
}
