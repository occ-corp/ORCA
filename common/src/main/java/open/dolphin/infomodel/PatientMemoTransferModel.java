
package open.dolphin.infomodel;

import java.util.Date;

/**
 * 転送用のPatientMemoModel ネスト防止
 * 
 * @author masuda, Masuda nIka
 */
public class PatientMemoTransferModel {

    private long id;
    private Date confirmed;
    private Date started;
    private Date recorded;
    private long linkId;
    private String linkRelation;
    private String status;
    private UserModel creator;

    private String memo;
    
    public void setPatientMemoModel(PatientMemoModel model) {
        id = model.getId();
        confirmed = model.getConfirmed();
        started = model.getStarted();
        recorded = model.getRecorded();
        linkId = model.getLinkId();
        linkRelation = model.getLinkRelation();
        status = model.getStatus();
        creator = model.getCreator();
        memo = model.getMemo();
    }
    
    public PatientMemoModel getPatientMemoModel() {
        PatientMemoModel model = new PatientMemoModel();
        model.setId(id);
        model.setConfirmed(confirmed);
        model.setStarted(started);
        model.setRecorded(recorded);
        model.setLinkId(linkId);
        model.setLinkRelation(linkRelation);
        model.setStatus(status);
        model.setUserModel(creator);
        model.setMemo(memo);
        return model;
    }
}
