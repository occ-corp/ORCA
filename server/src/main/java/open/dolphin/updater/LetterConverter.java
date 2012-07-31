
package open.dolphin.updater;

import java.util.List;
import open.dolphin.infomodel.*;

/**
 * LetterConverter
 * 
 * @author masuda, Masuda Naika
 */
public class LetterConverter extends AbstractUpdaterModule {
    
    private static final String VERSION_DATE    = "2012-03-10";
    private static final String UPDATE_MEMO     = "Old letters converted to new format.";
    private static final String NO_UPDATE_MEMO  = "Letters not updated.";
    
    private static final String ITEM_DISEASE = "disease";
    private static final String ITEM_PURPOSE = "purpose";
    private static final String TEXT_PAST_FAMILY = "pastFamily";
    private static final String TEXT_CLINICAL_COURSE = "clinicalCourse";
    private static final String TEXT_MEDICATION = "medication";
    private static final String ITEM_REMARKS = "remarks";
    private static final String ITEM_VISITED_DATE = "visitedDate";
    private static final String TEXT_INFORMED_CONTENT = "informedContent";

    private static final String TITLE__PREFIX_LETTER = "紹介状:";
    private static final String TITLE__PREFIX_REPLY = "ご報告:";
    
    private static final boolean fixPrimaryKey = false;  // 通常必要なし

    @Override
    public String getVersionDateStr() {
        return VERSION_DATE;
    }

    @Override
    public String getModuleName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public MsdUpdaterModel start() {
        
        boolean updated = true;
        
        // TouTouLetterを変換する
        List<LetterModel> letterList = em.createQuery("from TouTouLetter").getResultList();
        for (LetterModel model : letterList) {
            byte[] bytes = model.getBeanBytes();
            TouTouLetter letter = (TouTouLetter) ModelUtils.xmlDecode(bytes);
            decodeAndFixPrimaryKeys(letter);
            convertTouTouLetter(letter);
        }

        // TouTouReplyを変換する
        List<LetterModel> replyList = em.createQuery("from TouTouReply").getResultList();
        for (LetterModel model : replyList) {
            byte[] bytes = model.getBeanBytes();
            TouTouReply reply = (TouTouReply) ModelUtils.xmlDecode(bytes);
            decodeAndFixPrimaryKeys(reply);
            convertTouTouReply(reply);
        }
 
        return updated 
                ? getResult(UPDATE_MEMO) 
                : getResult(NO_UPDATE_MEMO);
    }
    
    // beanBytes内部のprimary keyを更新する 単純にMySQL->PSQLに変換したヤツの落とし穴ｗ
    private void decodeAndFixPrimaryKeys(LetterModel model) {
        
        if (!fixPrimaryKey) {
            return;
        }
        
        KarteBean karteBean = model.getKarteBean();
        UserModel creator = model.getUserModel();
        PatientModel patientModel = karteBean.getPatientModel();

        String fid = patientModel.getFacilityId();
        String patientId = patientModel.getPatientId();
        String userId = creator.getUserId();
        
        // PatientModelのprimary keyを修正
        long patientPk = (Long)
                em.createQuery("select p.id from PatientModel p where p.facilityId = :fid and p.patientId = :pid")
                .setParameter("fid", fid)
                .setParameter("pid", patientId)
                .getSingleResult();
        patientModel.setId(patientPk);
        
        if (patientModel.getHealthInsurances() != null) {
            for (HealthInsuranceModel insModel : patientModel.getHealthInsurances()) {
                insModel.getPatient().setId(patientPk);
            }
        }
        
        // KarteBeanのprimary keyを修正
        long kartePk= (Long)
                em.createQuery("select k.id from KarteBean k where k.patient.id = :id")
                .setParameter("id", patientPk)
                .getSingleResult();
        karteBean.setId(kartePk);
        
        // UserModelのprimary keyを修正
        long userPk = (Long)
                em.createQuery("select u.id from UserModel u where u.userId = :uid")
                .setParameter("uid", userId)
                .getSingleResult();
        creator.setId(userPk);
        
        long facilityPk = (Long)
                em.createQuery("select f.id from FacilityModel f where f.facilityId = :fid")
                .setParameter("fid", fid)
                .getSingleResult();
        creator.getFacilityModel().setId(facilityPk);
        
        if (creator.getRoles() != null) {
        for (RoleModel roleModel : creator.getRoles()) {
            roleModel.getUserModel().setId(userPk);
        }}
    }
    
    // TouTouLetterを変換する
    private void convertTouTouLetter(TouTouLetter letter) {
        
        LetterModule model = new LetterModule();
        
        // Handle Class
        model.setHandleClass("open.dolphin.letter.LetterViewer");
        model.setLetterType(IInfoModel.CLIENT);
        
        // 確定日等
        model.setConfirmed(letter.getConfirmed());
        model.setRecorded(letter.getRecorded());
        model.setStarted(letter.getStarted());
        model.setStatus(letter.getStatus());
        model.setKarteBean(letter.getKarteBean());
        model.setUserModel(letter.getUserModel());
        model.setLinkId(letter.getLinkId());    // all zero

        // 患者情報
        PatientModel patient = letter.getKarteBean().getPatientModel();
        model.setPatientId(patient.getPatientId());
        model.setPatientName(patient.getFullName());
        model.setPatientKana(patient.getKanaName());
        model.setPatientGender(patient.getGenderDesc());
        model.setPatientBirthday(patient.getBirthday());
        model.setPatientAge(ModelUtils.getAge(patient.getBirthday()));

        if (patient.getSimpleAddressModel() != null) {
            model.setPatientAddress(patient.getSimpleAddressModel().getAddress());
        }
        model.setPatientTelephone(patient.getTelephone());

        // 紹介元
        UserModel user = letter.getUserModel();
        model.setClientHospital(user.getFacilityModel().getFacilityName());
        model.setClientDoctor(user.getCommonName());
        model.setClientDept(user.getDepartmentModel().getDepartmentDesc());
        model.setClientTelephone(user.getFacilityModel().getTelephone());
        model.setClientFax(user.getFacilityModel().getFacsimile());
        model.setClientZipCode(user.getFacilityModel().getZipCode());
        model.setClientAddress(user.getFacilityModel().getAddress());
        
        // 紹介先（宛先）
        model.setConsultantHospital(letter.getConsultantHospital());
        model.setConsultantDept(letter.getConsultantDept());
        model.setConsultantDoctor(letter.getConsultantDoctor());
        
        // 傷病名
        String value = letter.getDisease();
        if (value != null) {
            LetterItem item = new LetterItem(ITEM_DISEASE, value);
            model.addLetterItem(item);
        }

        // 紹介目的
        value = letter.getPurpose();
        if (value != null) {
            LetterItem item = new LetterItem(ITEM_PURPOSE, value);
            model.addLetterItem(item);
        }

        // 既往歴、家族歴
        String text = letter.getPastFamily();
        if (text != null) {
            LetterText lt = new LetterText(TEXT_PAST_FAMILY, text);
            model.addLetterText(lt);
        }

        // 症状経過
        text = letter.getClinicalCourse();
        if (text != null) {
            LetterText lt = new LetterText(TEXT_CLINICAL_COURSE, text);
            model.addLetterText(lt);
        }

        // 現在の処方
        text = letter.getMedication();
        if (text != null) {
            LetterText lt = new LetterText(TEXT_MEDICATION, text);
            model.addLetterText(lt);
        }

        // 備考
        value = letter.getRemarks();
        if (value != null) {
            LetterItem item = new LetterItem(ITEM_REMARKS, value);
            model.addLetterItem(item);
        }

        // Title
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE__PREFIX_LETTER).append(model.getConsultantHospital());
        model.setTitle(sb.toString());
        
        saveLetter(model);
    }
    
   // TouTouReplyを変換する
    private void convertTouTouReply(TouTouReply reply) {

        LetterModule reply1 = createReplyModule(reply);
        // Handle Class
        reply1.setHandleClass("open.dolphin.letter.Reply1Viewer");
        // Title
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE__PREFIX_LETTER).append(reply1.getClientHospital());
        reply1.setTitle(sb.toString());

        saveLetter(reply1);


        LetterModule reply2 = createReplyModule(reply);
        // Handle Class
        reply2.setHandleClass("open.dolphin.letter.Reply2Viewer");
        // Title
        sb = new StringBuilder();
        sb.append(TITLE__PREFIX_REPLY).append(reply2.getClientHospital());
        reply2.setTitle(sb.toString());

        saveLetter(reply2);
    }
    
    private LetterModule createReplyModule(TouTouReply reply) {
        
        LetterModule model = new LetterModule();
        model.setLetterType(IInfoModel.CONSULTANT);

        // 確定日等
        model.setConfirmed(reply.getConfirmed());
        model.setRecorded(reply.getRecorded());
        model.setStarted(reply.getStarted());
        model.setStatus(reply.getStatus());
        model.setKarteBean(reply.getKarteBean());
        model.setUserModel(reply.getUserModel());

        // 患者情報
        PatientModel patient = reply.getKarteBean().getPatientModel();
        model.setPatientId(patient.getPatientId());
        model.setPatientName(patient.getFullName());
        model.setPatientKana(patient.getKanaName());
        model.setPatientGender(patient.getGenderDesc());
        model.setPatientBirthday(patient.getBirthday());
        model.setPatientAge(ModelUtils.getAge(patient.getBirthday()));
        
        if (patient.getSimpleAddressModel() != null) {
            model.setPatientAddress(patient.getSimpleAddressModel().getAddress());
        }
        model.setPatientTelephone(patient.getTelephone());

        // 紹介元
        UserModel user = reply.getUserModel();
        model.setConsultantHospital(user.getFacilityModel().getFacilityName());
        model.setConsultantDoctor(user.getCommonName());
        model.setConsultantDept(user.getDepartmentModel().getDepartmentDesc());
        model.setConsultantTelephone(user.getFacilityModel().getTelephone());
        model.setConsultantFax(user.getFacilityModel().getFacsimile());
        model.setConsultantZipCode(user.getFacilityModel().getZipCode());
        model.setConsultantAddress(user.getFacilityModel().getAddress());

        // 来院日
        String value = reply.getVisited();
        LetterItem item = new LetterItem(ITEM_VISITED_DATE, value);
        model.addLetterItem(item);
        
        // 紹介元（宛先）
        model.setClientHospital(reply.getClientHospital());
        model.setClientDept(reply.getClientDept());
        model.setClientDoctor(reply.getClientDoctor());

        // Informed
        String informed = reply.getInformedContent();
        if (informed != null) {
            LetterText text = new LetterText();
            text.setName(TEXT_INFORMED_CONTENT);
            text.setTextValue(informed);
            model.addLetterText(text);
        }

        return model;
    }
    
    // LetterModelを保存する
    private void saveLetter(LetterModule model) {

        // 保存
        em.persist(model);
        
        List<LetterItem> items = model.getLetterItems();
        if (items != null) {
            for (LetterItem item : items) {
                item.setModule(model);
                em.persist(item);
            }
        }
        List<LetterText> texts = model.getLetterTexts();
        if (texts != null) {
            for (LetterText txt : texts) {
                txt.setModule(model);
                em.persist(txt);
            }
        }
        List<LetterDate> dates = model.getLetterDates();
        if (dates != null) {
            for (LetterDate date : dates) {
                date.setModule(model);
                em.persist(date);
            }
        }
    }
    
    
}
