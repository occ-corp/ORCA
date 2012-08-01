
package open.dolphin.infomodel;

import java.util.Date;

/**
 * Karteを含まないKarteEntryBeanの抽象クラス
 * 
 * @author masuda, Masuda Naika
 */
public abstract class AbstractKarteEntryTransferModel {

    private long id;
    private Date confirmed;
    private Date started;
    private Date ended;
    private Date recorded;
    private long linkId;
    private String linkRelation;
    private String status;
    private UserModel userModel;
    
    public abstract KarteEntryBean getKarteEntryBean();
    public abstract void setKarteEntryBean(KarteEntryBean karteEntryBean);
    
    protected final void store(KarteEntryBean karteEntryBean) {
        id = karteEntryBean.getId();
        confirmed = karteEntryBean.getConfirmed();
        started = karteEntryBean.getStarted();
        ended = karteEntryBean.getEnded();
        recorded = karteEntryBean.getRecorded();
        linkId = karteEntryBean.getLinkId();
        linkRelation = karteEntryBean.getLinkRelation();
        status = karteEntryBean.getStatus();
        userModel = karteEntryBean.getUserModel();
    }
    
    protected final void restore(KarteEntryBean karteEntryBean) {
        karteEntryBean.setId(id);
        karteEntryBean.setConfirmed(confirmed);
        karteEntryBean.setStarted(started);
        karteEntryBean.setEnded(ended);
        karteEntryBean.setRecorded(recorded);
        karteEntryBean.setLinkId(linkId);
        karteEntryBean.setLinkRelation(linkRelation);
        karteEntryBean.setStatus(status);
        karteEntryBean.setUserModel(userModel);
    }

    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Date confirmed) {
        this.confirmed = confirmed;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getEnded() {
        return ended;
    }

    public void setEnded(Date ended) {
        this.ended = ended;
    }

    public Date getRecorded() {
        return recorded;
    }

    public void setRecorded(Date recorded) {
        this.recorded = recorded;
    }

    public long getLinkId() {
        return linkId;
    }

    public void setLinkId(long linkId) {
        this.linkId = linkId;
    }

    public String getLinkRelation() {
        return linkRelation;
    }

    public void setLinkRelation(String linkRelation) {
        this.linkRelation = linkRelation;
    }
        
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserModel getUserModel() {
        return userModel;
    }

    public void setUserModel(UserModel userModel) {
        this.userModel = userModel;
    }
}
