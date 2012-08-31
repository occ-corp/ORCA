package open.dolphin.infomodel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

/**
 * 入院カルテモデル
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_admission")
public class AdmissionModel extends KarteEntryBean {
    
    @Embedded
    private DocInfoModel docInfo;
    
    @JsonDeserialize(contentAs=DocumentModel.class)
    @ElementCollection
    @CollectionTable(name="msd_admission_documents")
    @OneToMany(fetch=FetchType.LAZY)    // PostgresだとEAGERだめ。MySQLは大丈夫
    private List<DocumentModel> documents;
    
    private RoomModel roomModel;
    
    
    public AdmissionModel() {
        docInfo = new DocInfoModel();
        docInfo.setDocType(DOC_TYPE_ADMISSION);
    }

    public DocInfoModel getDocInfoModel() {
        return docInfo;
    }

    public void setDocInfoModel(DocInfoModel docInfo) {
        this.docInfo = docInfo;
    }
    
    public List<DocumentModel> getDocumentList() {
        return documents;
    }
    
    public void setDocumentList(List<DocumentModel> documents) {
        this.documents = documents;
    }

    public RoomModel getRoomModel() {
        return roomModel;
    }
    
    public void setRoomModel(RoomModel roomModel) {
        this.roomModel = roomModel;
    }
    
    public void toDetuch() {
        docInfo.setDocPk(getId());
        docInfo.setParentPk(getLinkId());
        docInfo.setConfirmDate(getConfirmed());
        docInfo.setFirstConfirmDate(getStarted());
        docInfo.setStatus(getStatus());
    }
    
    public void toPersist() {
        setLinkId(docInfo.getParentPk());
        setLinkRelation(docInfo.getParentIdRelation());
        setConfirmed(docInfo.getConfirmDate());
        setFirstConfirmed(docInfo.getFirstConfirmDate());
        setStatus(docInfo.getStatus());
    }
    
    public boolean isInHospital(Date d) {
        Date started = getStarted();
        Date ended = getEnded();
        if (started == null) {
            return false;
        }
        if (!started.before(d) && (ended == null || !ended.before(d))) {
            return true;
        }
        return false;
    }
}
