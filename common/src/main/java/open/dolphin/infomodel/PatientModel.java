package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.*;

/**
 * PatientModel
 *
 * @author Minagawa,kazushi
 * @author modified by masuda, Masuda Naika
 */
@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
@Entity
@Table(name = "d_patient")
public class PatientModel extends InfoModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    /** 施設ID */
    @Column(nullable=false)
    private String facilityId;
    
    /** 施設内の患者ID */
    @Column(nullable=false)
    private String patientId;
    
    @JsonIgnore
    private String familyName;
    
    @JsonIgnore
    private String givenName;
    
    @Column(nullable=false)
    private String fullName;
    
    @JsonIgnore
    private String kanaFamilyName;
    
    @JsonIgnore
    private String kanaGivenName;
    
    private String kanaName;
    
    @JsonIgnore
    private String romanFamilyName;
    
    @JsonIgnore
    private String romanGivenName;
    
    private String romanName;
    
    @Column(nullable=false)
    private String gender;
    
    private String genderDesc;
    
    @JsonIgnore
    @Transient
    private String genderCodeSys;
    
    private String birthday;
    
    private String nationality;
    
    @Transient
    private String nationalityDesc;
    
    @JsonIgnore
    @Transient
    private String nationalityCodeSys;
    
    private String maritalStatus;
    
    @JsonIgnore
    @Transient
    private String maritalStatusDesc;
    
    @JsonIgnore
    @Transient
    private String maritalStatusCodeSys;
    
    @Lob
    private byte[] jpegPhoto;
    
    private String memo;
    
    @Embedded
    private SimpleAddressModel address;
    
    private String telephone;
    
    private String mobilePhone;
    
    private String email;
    
    @JsonDeserialize(contentAs=HealthInsuranceModel.class)
    @OneToMany(mappedBy="patient", cascade={CascadeType.PERSIST, CascadeType.REMOVE})
    private List<HealthInsuranceModel> healthInsurances;
    
    @JsonIgnore
    @Transient
    private List<PVTHealthInsuranceModel> pvtHealthInsurances;
    
    @JsonIgnore
    @Transient
    private Collection<AddressModel> addresses;
    
    @JsonIgnore
    @Transient
    private Collection<TelephoneModel> telephones;
    
//masuda^
    @JsonDeserialize(contentAs=Long.class)
    @Transient
    private List<Long> docPkList;
    @Transient
    private String searchText;

    public String getSearchText(){
        return searchText;
    }
    public void setSearchText(String searchText){
        this.searchText = searchText;
    }
    
    public List<Long> getDocPkList(){
        return docPkList;
    }
    public void setDocPkList(List<Long> docPkList) {
        this.docPkList = docPkList;
    }

    // 受診日。患者検索で使用
    @Transient
    private String pvtDate;

    public String getPvtDate() {
        return pvtDate;
    }
    public void setPvtDate(String pvtDate) {
        this.pvtDate = pvtDate;
    }
    public String getPvtDateTrimTime() {
        return ModelUtils.trimTime(pvtDate);
    }
    public String getPvtDateTrimDate() {
        return ModelUtils.trimDate(pvtDate);
    }
    
    // 算定情報
    @Embedded
    private SanteiInfoModel santeiInfo;

    public SanteiInfoModel getSanteiInfoModel() {
        return santeiInfo;
    }

    public void setSanteiInfoModel(SanteiInfoModel info) {
        santeiInfo = info;
    }
    
    // 排他処理関連
    private String ownerUUID;   // 現在の所有者UUID
    public void setOwnerUUID(String uuid) {
        ownerUUID = uuid;
    }
    public String getOwnerUUID() {
        return ownerUUID;
    }
    
    // 入院モデル
    // bi-directional references, @JsonIdentityInfo added to PatientModel
    @Transient
    private AdmissionModel admission;
    public void setAdmissionModel(AdmissionModel model) {
        admission = model;
    }
    public AdmissionModel getAdmissionModel() {
        return admission;
    }
//masuda$
    
    /**
     * 患者オブジェクトを生成する。
     */
    public PatientModel() {
//masuda^   SanteiInfoを初期化
        santeiInfo = new SanteiInfoModel();
//masuda$
    }
    
    /**
     * Database Pk を返す。
     *
     * @return Database Pk
     */
    public long getId() {
        return id;
    }
    
    /**
     * Database Pk を設定する。
     *
     * @param id
     *            Database Pk
     */
    public void setId(long id) {
        this.id = id;
    }
    
    /**
     * 施設IDを返す。
     * @return 施設ID
     */
    public String getFacilityId() {
        return facilityId;
    }
    
    /**
     * 施設IDを設定する。
     * @param facilityId 施設ID
     */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }
    
    /**
     * 患者IDを設定する。
     *
     * @param patientId
     *            患者ID
     */
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    /**
     * 患者IDを返す。
     *
     * @return 患者ID
     */
    public String getPatientId() {
        return patientId;
    }
    
    /**
     * 姓名を設定する。
     *
     * @param sirName
     *            姓名
     */
    public void setFamilyName(String sirName) {
        this.familyName = sirName;
    }
    
    /**
     * 姓名を返す。
     *
     * @return 姓名
     */
    public String getFamilyName() {
        return familyName;
    }
    
    /**
     * 名前を設定する。
     *
     * @param givenName
     *            名前
     */
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }
    
    /**
     * 名前を返す。
     *
     * @return 名前
     */
    public String getGivenName() {
        return givenName;
    }
    
    /**
     * フルネームを設定する。
     *
     * @param name
     *            フルネーム
     */
    public void setFullName(String name) {
        this.fullName = name;
    }
    
    /**
     * フルネームを返す。
     *
     * @return フルネーム
     */
    public String getFullName() {
        return fullName;
    }
    
    /**
     * カナ家族名を設定する。
     *
     * @param kanaSirName
     *            Tカナ家族名
     */
    public void setKanaFamilyName(String kanaSirName) {
        this.kanaFamilyName = kanaSirName;
    }
    
    /**
     * カナ家族名を返す。
     *
     * @return カナ家族名
     */
    public String getKanaFamilyName() {
        return kanaFamilyName;
    }
    
    /**
     * カナGivenName を設定する。
     *
     * @param kanaGivenName
     *            カナGivenName
     */
    public void setKanaGivenName(String kanaGivenName) {
        this.kanaGivenName = kanaGivenName;
    }
    
    /**
     * カナGivenName を返す。
     *
     * @return カナGivenName
     */
    public String getKanaGivenName() {
        return kanaGivenName;
    }
    
    /**
     * カナフルネームを設定する。
     *
     * @param kanaName
     *            カナフルネーム
     */
    public void setKanaName(String kanaName) {
        this.kanaName = kanaName;
    }
    
    /**
     * カナフルネームを返す。
     *
     * @return カナフルネーム
     */
    public String getKanaName() {
        return kanaName;
    }
    
    /**
     * ローマ字名字を設定する。
     *
     * @param romanSirName
     *            ローマ字名字
     */
    public void setRomanFamilyName(String romanSirName) {
        this.romanFamilyName = romanSirName;
    }
    
    /**
     * ローマ字名字を返す。
     *
     * @return ローマ字名
     */
    public String getRomanFamilyName() {
        return romanFamilyName;
    }
    
    /**
     * ローマ字名を設定する。
     *
     * @param romanGivenName
     *            ローマ字名
     */
    public void setRomanGivenName(String romanGivenName) {
        this.romanGivenName = romanGivenName;
    }
    
    /**
     * ローマ字名を返す。
     *
     * @return ローマ字名
     */
    public String getRomanGivenName() {
        return romanGivenName;
    }
    
    /**
     * ローマ字フルネームを設定する。
     *
     * @param romanName
     *            ローマ字フルネーム
     */
    public void setRomanName(String romanName) {
        this.romanName = romanName;
    }
    
    /**
     * ローマ字フルネームを返す。
     *
     * @return ローマ字フルネーム
     */
    public String getRomanName() {
        return romanName;
    }
    
    /**
     * 性別を設定する。
     *
     * @param gender
     *            性別
     */
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    /**
     * 性別を返す。
     *
     * @return 性別
     */
    public String getGender() {
        return gender;
    }
    
    /**
     * 性別説明を設定する。
     *
     * @param genderDesc
     *            性別説明
     */
    public void setGenderDesc(String genderDesc) {
        this.genderDesc = genderDesc;
    }
    
    /**
     * 性別説明を返す。
     *
     * @return 性別説明
     */
    public String getGenderDesc() {
        return genderDesc != null
                ? genderDesc
                : ModelUtils.getGenderDesc(gender);
    }
    
    /**
     * 性別説明体系を設定する。
     *
     * @param genderCodeSys
     *            性別説明体系
     */
    public void setGenderCodeSys(String genderCodeSys) {
        this.genderCodeSys = genderCodeSys;
    }
    
    /**
     * 性別説明体系を返す。
     *
     * @return 性別説明体系
     */
    public String getGenderCodeSys() {
        return genderCodeSys;
    }
    
    /**
     * 生年月日を設定する。
     *
     * @param birthday
     *            生年月日 yyyy-MM-dd
     */
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
    
    /**
     * 生年月日を返す。
     *
     * @return 生年月日 yyyy-MM-dd
     */
    public String getBirthday() {
        return birthday;
    }
    
    /**
     * 年齢と生年月日を返す。
     *
     * @return age(yyyy-MM-dd)
     */
    public String getAgeBirthday() {
        return ModelUtils.getAgeBirthday(birthday);
    }
    
    /**
     * 国籍を設定する。
     *
     * @param nationality
     *            国籍
     */
    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
    
    /**
     * 国籍を返す。
     *
     * @return 国籍
     */
    public String getNationality() {
        return nationality;
    }
    
    /**
     * 国籍説明を設定する。
     *
     * @param nationalityDesc
     *            国籍説明
     */
    public void setNationalityDesc(String nationalityDesc) {
        this.nationalityDesc = nationalityDesc;
    }
    
    /**
     * 国籍説明を返す。
     *
     * @return 国籍説明
     */
    public String getNationalityDesc() {
        return nationalityDesc;
    }
    
    /**
     * 国籍体系を設定する。
     *
     * @param nationalityCodeSys
     *            国籍体系
     */
    public void setNationalityCodeSys(String nationalityCodeSys) {
        this.nationalityCodeSys = nationalityCodeSys;
    }
    
    /**
     * 国籍体系を返す。
     *
     * @return 国籍体系
     */
    public String getNationalityCodeSys() {
        return nationalityCodeSys;
    }
    
    /**
     * 婚姻状況を設定する。
     *
     * @param maritalStatus
     *            婚姻状況
     */
    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }
    
    /**
     * 婚姻状況を返す。
     *
     * @return 婚姻状況
     */
    public String getMaritalStatus() {
        return maritalStatus;
    }
    
    /**
     * 婚姻状況説明を設定する。
     *
     * @param maritalStatusDesc
     *            婚姻状況説明を設定する。
     */
    public void setMaritalStatusDesc(String maritalStatusDesc) {
        this.maritalStatusDesc = maritalStatusDesc;
    }
    
    /**
     * 婚姻状況説明を返す。
     *
     * @return 婚姻状況説明
     */
    public String getMaritalStatusDesc() {
        return maritalStatusDesc;
    }
    
    /**
     * 婚姻状況体系を設定する。
     *
     * @param maritalStatusCodeSys
     *            婚姻状況体系
     */
    public void setMaritalStatusCodeSys(String maritalStatusCodeSys) {
        this.maritalStatusCodeSys = maritalStatusCodeSys;
    }
    
    /**
     * 婚姻状況体系を返す。
     *
     * @return 婚姻状況体系
     */
    public String getMaritalStatusCodeSys() {
        return maritalStatusCodeSys;
    }
    
    /**
     * 写真を設定する。
     *
     * @param jpegPhoto
     *            JPEG 画像のバイト配列
     */
    public void setJpegPhoto(byte[] jpegPhoto) {
        this.jpegPhoto = jpegPhoto;
    }
    
    /**
     * 写真を返す。
     *
     * @return JPEG 画像のバイト配列
     */
    public byte[] getJpegPhoto() {
        return jpegPhoto;
    }
    
    /**
     * 患者メモを設定する。
     *
     * @param memo
     *            患者メモ
     */
    public void setMemo(String memo) {
        this.memo = memo;
    }
    
    /**
     * 患者メモを返す。
     *
     * @return 患者メモ
     */
    public String getMemo() {
        return memo;
    }
    
    /**
     * 住所モデルを返す。
     * @return 住所モエル
     */
    public SimpleAddressModel getSimpleAddressModel() {
        return address;
    }
    
    /**
     * 住所モデルを設定する。
     * @param address 住所モデル
     */
    public void setSimpleAddressModel(SimpleAddressModel address) {
        this.address = address;
    }
    
    /**
     * 郵便番号を返す。
     * @return 郵便番号
     */
    public String contactZipCode() {
        return (address!=null) ? address.getZipCode() : null;
    }
    
    /**
     * 住所を返す。
     * @return 住所
     */
    public String contactAddress() {
        return (address!=null) ? address.getAddress() : null;
    }

    public String getHomeAddress() {
        return (address!=null) ? address.getAddress() : null;
    }
    
    /**
     * 電話番号を返す。
     * @return 電話番号
     */
    public String getTelephone() {
        return telephone;
    }
    
    /**
     * 電話番号を設定する。
     * @param telephone 電話番号
     */
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
    
    /**
     *携帯電話の番号を返す。
     * @return 携帯電話の番号
     */
    public String getMobilePhone() {
        return mobilePhone;
    }
    
    /**
     *携帯電話の番号を設定する。
     *@param mobilePhone 携帯電話の番号
     */
    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }
    
    /**
     * 電子メールアドレスを返す。
     * @return 電子メールアドレス
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * 電子メールアドレスを設定する。
     * @param email 電子メールアドレス
     */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /**
     * 健康保険を設定する。
     *
     * @param healthInsurances
     *            健康保険
     */
    public void setHealthInsurances(
            List<HealthInsuranceModel> healthInsurances) {
        this.healthInsurances = healthInsurances;
    }
    
    /**
     * 健康保険を返す。
     *
     * @return 健康保険
     */
    public List<HealthInsuranceModel> getHealthInsurances() {
        return healthInsurances;
    }
    
    /**
     * 健康保険を追加する。
     *
     * @param value
     *            健康保険
     */
    public void addHealthInsurance(HealthInsuranceModel value) {
        if (healthInsurances == null) {
            healthInsurances = new ArrayList<HealthInsuranceModel>(2);
        }
        healthInsurances.add(value);
    }
    
    public List<PVTHealthInsuranceModel> getPvtHealthInsurances() {
        return pvtHealthInsurances;
    }
    
    public void setPvtHealthInsurances(
            List<PVTHealthInsuranceModel> pvtHealthInsurances) {
        this.pvtHealthInsurances = pvtHealthInsurances;
    }
    
    public void addPvtHealthInsurance(PVTHealthInsuranceModel model) {
        if (pvtHealthInsurances == null) {
            pvtHealthInsurances =  new ArrayList<PVTHealthInsuranceModel>(2);
        }
        pvtHealthInsurances.add(model);
    }
    
    public Collection<AddressModel> getAddresses() {
        return addresses;
    }
    
    public void setAddresses(Collection<AddressModel> addresses) {
        this.addresses = addresses;
    }
    
    public void addAddress(AddressModel address) {
        if (addresses == null) {
            addresses = new ArrayList<AddressModel>(1);
        }
        addresses.add(address);
    }
    
    public Collection<TelephoneModel> getTelephones() {
        return telephones;
    }
    
    public void setTelephones(Collection<TelephoneModel> telephones) {
        this.telephones = telephones;
    }
    
    public void addTelephone(TelephoneModel telephone) {
        if (telephones == null) {
            telephones = new ArrayList<TelephoneModel>(1);
        }
        telephones.add(telephone);
    }
    
    /**
     * 患者簡易情報を返す。
     *
     * @return 患者簡易情報
     */
    public PatientLiteModel patientAsLiteModel() {
        PatientLiteModel model = new PatientLiteModel();
        model.setPatientId(getPatientId());
        model.setFullName(getFullName());
        model.setKanaName(getKanaName());
        model.setGender(getGender());
        model.setGenderDesc(getGenderDesc());
        model.setBirthday(getBirthday());
        return model;
    }
    
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
        final PatientModel other = (PatientModel) obj;
        if (id != other.id)
            return false;
        return true;
    }

    //-------------------------------------------------------------
    public SimpleAddressModel getAddress() {
        return getSimpleAddressModel();
    }

    public void setAddress(SimpleAddressModel address) {
        setSimpleAddressModel(address);
    }
    //-------------------------------------------------------------
    
//masuda^
    public String getAgeBirthday2() {
        return ModelUtils.getAgeBirthday2(birthday);
    }
    
    public String getRoom() {
        return (admission != null) ? admission.getRoom() : null;
    }
    public String getDeptName() {
        return (admission != null) ? admission.getDepartment() : null;
    }
    public String getAdmissionDate() {
        if (admission != null) {
            SimpleDateFormat frmt = new SimpleDateFormat(DATE_WITHOUT_TIME);
            return frmt.format(admission.getStarted());
        }
        return null;
    }
    public String getDoctorName() {
        return (admission != null) ? admission.getDoctorName() : null;
    }
    public boolean isOpened() {
        return ownerUUID != null;
    }

//masuda$
}
