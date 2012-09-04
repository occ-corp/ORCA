package open.dolphin.infomodel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

/**
 * 入院カルテモデル（仮）
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_admission")
public class AdmissionModel implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    @Column(nullable=false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date started;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date ended;
    
    private String room;
    
    @Embedded
    private DocInfoModel docInfo;
    
    @JsonDeserialize(contentAs=DocumentModel.class)
    @ElementCollection
    @CollectionTable(name="msd_admission_documents")
    @OneToMany(fetch=FetchType.LAZY)
    private List<DocumentModel> documents;
    
    
    public AdmissionModel() {
        docInfo = new DocInfoModel();
        docInfo.setDocType(IInfoModel.DOC_TYPE_ADMISSION);
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
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
    
    public String getRoom() {
        return room;
    }
    
    public void setRoom(String room) {
        this.room = room;
    }
    
    public void setChildDocInfoList() {
        List<DocInfoModel> list = new ArrayList<DocInfoModel>();
        for (DocumentModel model : documents) {
            list.add(model.getDocInfoModel());
        }
        docInfo.setChildDocInfoList(list);
        
    }
    public boolean isInHospital(Date d) {

        if (started == null) {
            return false;
        }
        if (!started.before(d) && (ended == null || !ended.before(d))) {
            return true;
        }
        return false;
    }
}
