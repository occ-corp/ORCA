package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;

/**
 * KarteBean
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika
 */
@Entity
@Table(name = "d_karte")
public class KarteBean extends InfoModel {
    
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    //@ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore     // lazy fetch?
    @ManyToOne
    @JoinColumn(name="patient_id", nullable=false)
    private PatientModel patient;
    
    @JsonIgnore // no use
    @Transient
    private Map<String, List> entries;
    
    @Column(nullable=false)
    @Temporal(value = TemporalType.DATE)
    private Date created;

    @JsonDeserialize(contentAs=AllergyModel.class)
    @Transient
    private List<AllergyModel> allergies;

    @JsonDeserialize(contentAs=PhysicalModel.class)
    @Transient
    private List<PhysicalModel> heights;

    @JsonDeserialize(contentAs=PhysicalModel.class)
    @Transient
    private List<PhysicalModel> weights;

    @JsonDeserialize(contentAs=String.class)
    @Transient
    private List<String> patientVisits;

    @JsonDeserialize(contentAs=DocInfoModel.class)
    @Transient
    private List<DocInfoModel> docInfoList;

    @JsonIgnore // bi-directional references
    @Transient
    private List<PatientMemoModel> memoList;
    
    // シリアライズ用
    @JsonDeserialize(contentAs=PatientMemoTransferModel.class)
    @Transient
    private List<PatientMemoTransferModel> memoTransList;
    
    @JsonIgnore // bi-directional references
    @Transient
    private List<AppointmentModel> appoList;
    
    // シリアライズ用
    @JsonDeserialize(contentAs=AppointmentTransferModel.class)
    @Transient
    private List<AppointmentTransferModel> appoTransList;
    
    @Transient
    private Date lastDocDate; 
    
    //==========================================================================
    public void setLastDocDate(Date d) {
        lastDocDate = d;
    }
    public Date getLastDocDate() {
        return lastDocDate;
    }
    public void setMemoTransList(List<PatientMemoTransferModel> list) {
        memoTransList = list;
    }
    
    public List<PatientMemoTransferModel> getMemoTransList() {
        return memoTransList;
    }
    
    public void setAppoTransList(List<AppointmentTransferModel> list) {
        appoTransList = list;
    }
    
    public List<AppointmentTransferModel> getAppoTransList() {
        return appoTransList;
    }
    
    public void setAppointmentList(List<AppointmentModel> list) {
        appoList = list;
    }
    public List<AppointmentModel> getAppointmentList() {
        return appoList;
    }
    //==========================================================================
    
    public KarteBean() {
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public PatientModel getPatientModel() {
        return patient;
    }
    
    public void setPatientModel(PatientModel patient) {
        this.patient = patient;
    }
    
    public Date getCreated() {
        return created;
    }
    
    public void setCreated(Date created) {
        this.created = created;
    }

    //-----------------------------------------------
    
    public List<AllergyModel> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<AllergyModel> allergies) {
        this.allergies = allergies;
    }

    public List<PhysicalModel> getHeights() {
        return heights;
    }

    public void setHeights(List<PhysicalModel> heights) {
        this.heights = heights;
    }

    public List<PhysicalModel> getWeights() {
        return weights;
    }

    public void setWeights(List<PhysicalModel> weights) {
        this.weights = weights;
    }

    public List<String> getPatientVisits() {
        return patientVisits;
    }

    public void setPatientVisits(List<String> patientVisits) {
        this.patientVisits = patientVisits;
    }

    public List<DocInfoModel> getDocInfoList() {
        return docInfoList;
    }

    public void setDocInfoList(List<DocInfoModel> docInfoList) {
        this.docInfoList = docInfoList;
    }

    public List<PatientMemoModel> getMemoList() {
        return memoList;
    }

    public void setMemoList(List<PatientMemoModel> memoList) {
        this.memoList = memoList;
    }
    
//    /**
//     * カルテのエントリを返す。
//     * @return カテゴリをKey、エントリのコレクションをValueにしたHashMap
//     */
//    public Map<String, List> getEntries() {
//        return entries;
//    }
//
//    /**
//     * カルテのエントリを設定する。
//     * param entries カテゴリをKey、エントリのコレクションをValueにしたHashMap
//     */
//    public void setEntries(Map<String, List> entries) {
//        this.entries = entries;
//    }
//
//    /**
//     * 指定したカテゴリのエントリコレクションを返す。
//     * @param category カテゴリ
//     * @return　エントリのコレクション
//     */
//    public List getEntryCollection(String category) {
//        return entries != null ? entries.get(category) : null;
//    }
//
//    /**
//     * カテゴリとそのエントリのコレクションを追加する。
//     * @param category カテゴリ
//     * @param entries カテゴリのエントリーのコレクション
//     */
//    public void addEntryCollection(String category, List entrs) {
//
//        if (entries == null) {
//            entries = new HashMap<String, List>();
//        }
//        entries.put(category, entrs);
//    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (id ^ (id >>> 32));
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final KarteBean other = (KarteBean) obj;
        if (id != other.id)
            return false;
        return true;
    }

    //-------------------------------------------------------------

    public Map<String, List> getEntries() {
        return entries;
    }
    public void setEntries(Map<String, List> entries) {
        this.entries = entries;
    }

    public PatientModel getPatient() {
        return getPatientModel();
    }

    public void setPatient(PatientModel patient) {
        setPatientModel(patient);
    }
    //-------------------------------------------------------------
}
