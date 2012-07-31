package open.dolphin.letter;

import java.awt.BorderLayout;
import java.util.Date;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;
import open.dolphin.client.ClientContext;
import open.dolphin.client.Panel2;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.AgeCalculator;
import org.apache.log4j.Level;

/**
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public class LetterImpl extends AbstractLetterImpl {

    protected static final String TITLE = "診療情報提供書";
    
    protected static final String ITEM_DISEASE = "disease";
    protected static final String ITEM_PURPOSE = "purpose";
    protected static final String TEXT_PAST_FAMILY = "pastFamily";
    protected static final String TEXT_CLINICAL_COURSE = "clinicalCourse";
    protected static final String TEXT_MEDICATION = "medication";
    protected static final String ITEM_REMARKS = "remarks";

    private static final String TITLE__PREFIX = "紹介状:";

    protected LetterView view;

    protected boolean DEBUG;

    public LetterImpl() {
        setTitle(TITLE);
        DEBUG = (ClientContext.getBootLogger().getLevel() == Level.DEBUG);
    }
    
    @Override
    protected Panel2 getView() {
        return view;
    };
    @Override
    protected String getFrameTitle() {
        return TITLE;
    };
    
    @Override
    public void modelToView(LetterModule m) {

        if (view == null) {
            view = new LetterView();
        }

        // 日付
        String dateStr = LetterHelper.getDateAsString(m.getConfirmed());
        LetterHelper.setModelValue(view.getConfirmed(), dateStr);

        // 紹介先（宛先）医療機関名
        LetterHelper.setModelValue(view.getConsultantHospital(), m.getConsultantHospital());

        // 紹介先（宛先）診療科
        LetterHelper.setModelValue(view.getConsultantDept(), m.getConsultantDept());

        // 紹介先（宛先）担当医
        LetterHelper.setModelValue(view.getConsultantDoctor(), m.getConsultantDoctor());

        // title
        StringBuilder sb = new StringBuilder();
        sb.append("先生　");
        String title = Project.getString("letter.atesaki.title");
        if (title!=null && (!title.equals("無し"))) {
            sb.append(title);
        }
        LetterHelper.setModelValue(view.getAtesakiLbl(), sb.toString());

        // 患者氏名
        LetterHelper.setModelValue(view.getPatientName(), m.getPatientName());

        // 患者生年月日
        LetterHelper.setModelValue(view.getPatientBirthday(), m.getPatientBirthday());

        // 年齢
        LetterHelper.setModelValue(view.getPatientAge(), m.getPatientAge());

        // 性別
        LetterHelper.setModelValue(view.getPatientGender(), m.getPatientGender());

        // 紹介先（差出人）住所
        //String val = LetterHelper.getAddressWithZipCode(m.getConsultantAddress(), m.getConsultantZipCode());
        //LetterHelper.setModelValue(view.getConsultantAddress(), val);

        // 紹介先（差出人）電話
        //LetterHelper.setModelValue(view.getConsultantTelephone(), m.getConsultantTelephone());

        // 紹介先（差出人）病院名
        //LetterHelper.setModelValue(view.getConsultantHospital(), m.getConsultantHospital());

        // 紹介（差出人）先医師
        //LetterHelper.setModelValue(view.getConsultantDoctor(), m.getConsultantDoctor());

        //----------------------------------------------------------------------

        // 病名
        String value = model.getItemValue(ITEM_DISEASE);
        if (value != null) {
            LetterHelper.setModelValue(view.getDisease(), value);
        }

        // 紹介目的
        value = model.getItemValue(ITEM_PURPOSE);
        if (value != null) {
            LetterHelper.setModelValue(view.getPurpose(), value);
        }

        // 既往歴、家族歴
        String text = model.getTextValue(TEXT_PAST_FAMILY);
        if (text!=null) {
            LetterHelper.setModelValue(view.getPastFamily(), text);
        }

        // 症状経過
        text = model.getTextValue(TEXT_CLINICAL_COURSE);
        if (text!=null) {
            LetterHelper.setModelValue(view.getClinicalCourse(), text);
        }

        // 現在の処方
        text = model.getTextValue(TEXT_MEDICATION);
        if (text!=null) {
            LetterHelper.setModelValue(view.getMedication(), text);
        }

        // 備考
        value = model.getItemValue(ITEM_REMARKS);
        if (value != null) {
            LetterHelper.setModelValue(view.getRemarks(), value);
        }
    }

    @Override
    public void viewToModel() {

        long savedId = model.getId();
        model.setId(0L);
        model.setLinkId(savedId);

        Date d = new Date();
        model.setConfirmed(d);
        model.setRecorded(d);
        model.setKarteBean(getContext().getKarte());
        model.setUserModel(Project.getUserModel());
        model.setStatus(IInfoModel.STATUS_FINAL);

        // 紹介先（宛先）
        model.setConsultantHospital(LetterHelper.getFieldValue(view.getConsultantHospital()));
        model.setConsultantDept(LetterHelper.getFieldValue(view.getConsultantDept()));
        model.setConsultantDoctor(LetterHelper.getFieldValue(view.getConsultantDoctor()));

        // 患者情報、差し出し人側はstartでmodelに設定済

        // 傷病名
        String value = LetterHelper.getFieldValue(view.getDisease());
        if (value != null) {
            LetterItem item = new LetterItem(ITEM_DISEASE, value);
            model.addLetterItem(item);
        }

        // 紹介目的
        value = LetterHelper.getFieldValue(view.getPurpose());
        if (value != null) {
            LetterItem item = new LetterItem(ITEM_PURPOSE, value);
            model.addLetterItem(item);
        }

        // 既往歴、家族歴
        String text = LetterHelper.getAreaValue(view.getPastFamily());
        if (text != null) {
            LetterText lt = new LetterText(TEXT_PAST_FAMILY, text);
            model.addLetterText(lt);
        }

        // 症状経過
        text = LetterHelper.getAreaValue(view.getClinicalCourse());
        if (text != null) {
            LetterText lt = new LetterText(TEXT_CLINICAL_COURSE, text);
            model.addLetterText(lt);
        }

        // 現在の処方
        text = LetterHelper.getAreaValue(view.getMedication());
        if (text != null) {
            LetterText lt = new LetterText(TEXT_MEDICATION, text);
            model.addLetterText(lt);
        }

        // 備考
        value = LetterHelper.getFieldValue(view.getRemarks());
        if (value != null) {
            LetterItem item = new LetterItem(ITEM_REMARKS, value);
            model.addLetterItem(item);
        }

        // Title
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE__PREFIX).append(model.getConsultantHospital());
        model.setTitle(sb.toString());
    }

    @Override
    public void start() {

        this.model = new LetterModule();

        // Handle Class
        this.model.setHandleClass(LetterViewer.class.getName());
        this.model.setLetterType(IInfoModel.CLIENT);

        // 確定日等
        Date d = new Date();
        this.model.setConfirmed(d);
        this.model.setRecorded(d);
        this.model.setStarted(d);
        this.model.setStatus(IInfoModel.STATUS_FINAL);
        this.model.setKarteBean(getContext().getKarte());
        this.model.setUserModel(Project.getUserModel());

        // 患者情報
        PatientModel patient = getContext().getPatient();
        this.model.setPatientId(patient.getPatientId());
        this.model.setPatientName(patient.getFullName());
        this.model.setPatientKana(patient.getKanaName());
        this.model.setPatientGender(patient.getGenderDesc());
        this.model.setPatientBirthday(patient.getBirthday());

        int showMonth = Project.getInt(Project.KARTE_AGE_TO_NEED_MONTH);
        String age = AgeCalculator.getAge(patient.getBirthday(), showMonth);
        this.model.setPatientAge(age);

        if (patient.getSimpleAddressModel()!=null) {
            this.model.setPatientAddress(patient.getSimpleAddressModel().getAddress());
//masuda^   zip codeも設定する
            this.model.setPatientZipCode(patient.getSimpleAddressModel().getZipCode());
//masuda$
        }
        this.model.setPatientTelephone(patient.getTelephone());

        // 紹介元
        UserModel user = Project.getUserModel();
        this.model.setClientHospital(user.getFacilityModel().getFacilityName());
        this.model.setClientDoctor(user.getCommonName());
        this.model.setClientDept(user.getDepartmentModel().getDepartmentDesc());
        this.model.setClientTelephone(user.getFacilityModel().getTelephone());
        this.model.setClientFax(user.getFacilityModel().getFacsimile());
        this.model.setClientZipCode(user.getFacilityModel().getZipCode());
        this.model.setClientAddress(user.getFacilityModel().getAddress());

        // view を生成
        this.view = new LetterView();
        JScrollPane scroller = new JScrollPane(this.view);
        getUI().setLayout(new BorderLayout());
        getUI().add(scroller);

        modelToView(this.model);
        setEditables(true);
        setListeners();
        
        stateMgr = new LetterStateMgr(this);
    }

    @Override
    public void makePDF() {

        if (this.model == null) {
            return;
        }

        LetterPDFMaker maker = new LetterPDFMaker();
        maker.setModel(model);
        maker.setContext(getContext());
        maker.create();
    }

    @Override
    public void setEditables(boolean b) {
        view.getConsultantHospital().setEditable(b);
        view.getConsultantDept().setEditable(b);
        view.getConsultantDoctor().setEditable(b);
        view.getDisease().setEditable(b);
        view.getPurpose().setEditable(b);
        view.getPastFamily().setEditable(b);
        view.getClinicalCourse().setEditable(b);
        view.getMedication().setEditable(b);
        view.getRemarks().setEditable(b);
    }

    @Override
    public void setListeners() {

        if (listenerIsAdded) {
            return;
        }
        super.setListeners();

        JTextComponent[] jcs = new JTextComponent[]{
            view.getConsultantHospital(),   // 紹介先（宛先）病院
            view.getConsultantDept(),       // 紹介先（宛先）診療科
            view.getConsultantDoctor(),     // 紹介先（宛先）医師
            view.getDisease(),              // 傷病名
            view.getPurpose(),              // 紹介目的
            view.getPastFamily(),           // 既往歴、家族歴
            view.getClinicalCourse(),       // 症状経過
            view.getMedication(),           // 現在の処方
            view.getRemarks()               // 備考    
        };
        
        setComponentListeners(jcs);

        listenerIsAdded = true;
    }

    @Override
    public boolean letterIsDirty() {
        boolean dirty =  true;
        dirty = dirty && (LetterHelper.getFieldValue(view.getConsultantHospital()) != null);
        dirty = dirty && (LetterHelper.getFieldValue(view.getDisease()) != null);
        dirty = dirty && (LetterHelper.getFieldValue(view.getPurpose()) != null);
        dirty = dirty && (LetterHelper.getAreaValue(view.getClinicalCourse()) != null);
        return dirty;
    }
}
