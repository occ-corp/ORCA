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
 * Reply2Impl
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class Reply2Impl extends AbstractLetterImpl {

    protected static final String TITLE = "ご　報　告";
    protected static final String ITEM_VISITED_DATE = "visited";
    protected static final String TEXT_INFORMED_CONTENT = "informedContent";

    private static final String TITLE__PREFIX = "ご報告:";

    protected Reply2View view;

    protected boolean DEBUG;


    public Reply2Impl() {
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
            view = new Reply2View();
        }

        // 日付
        String dateStr = LetterHelper.getDateAsString(m.getConfirmed());
        LetterHelper.setModelValue(view.getConfirmed(), dateStr);

        // 紹介元（宛先）医療機関名
        LetterHelper.setModelValue(view.getClientHospital(), m.getClientHospital());

        // 紹介元（宛先）診療科
        LetterHelper.setModelValue(view.getClientDept(), m.getClientDept());

        // 紹介元紹介元（宛先）担当医
        LetterHelper.setModelValue(view.getClientDoctor(), m.getClientDoctor());

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
        String val = LetterHelper.getBirdayWithAge(m.getPatientBirthday(), m.getPatientAge());
        LetterHelper.setModelValue(view.getPatientBirthday(), val);

        // 紹介先（差出人）住所
        if (m.getConsultantAddress()==null) {
            m.setConsultantZipCode(Project.getUserModel().getFacilityModel().getZipCode());
            m.setConsultantAddress(Project.getUserModel().getFacilityModel().getAddress());
        }
        val = LetterHelper.getAddressWithZipCode(m.getConsultantAddress(), m.getConsultantZipCode());
        LetterHelper.setModelValue(view.getConsultantAddress(), val);

        // 紹介先（差出人）電話
        if (m.getConsultantTelephone()==null) {
            m.setConsultantTelephone(Project.getUserModel().getFacilityModel().getTelephone());
        }
        LetterHelper.setModelValue(view.getConsultantTelephone(), m.getConsultantTelephone());

        // 紹介先（差出人）病院名
        LetterHelper.setModelValue(view.getConsultantHospital(), m.getConsultantHospital());

        // 紹介（差出人）先医師
        LetterHelper.setModelValue(view.getConsultantDoctor(), m.getConsultantDoctor());

        //----------------------------------------------------------------------

        // 来院日
        String value = model.getItemValue(ITEM_VISITED_DATE);
        if (value != null) {
            LetterHelper.setModelValue(view.getVisited(), value);
        }

        // Informed
        String text = model.getTextValue(TEXT_INFORMED_CONTENT);
        if (text!=null) {
            LetterHelper.setModelValue(view.getInformedContent(), text);
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

        // 紹介元（宛先）
        model.setClientHospital(LetterHelper.getFieldValue(view.getClientHospital()));
        model.setClientDept(LetterHelper.getFieldValue(view.getClientDept()));
        model.setClientDoctor(LetterHelper.getFieldValue(view.getClientDoctor()));

        // 患者情報、差し出し人側はtartでmodelに設定済

        // 来院日
        String value = LetterHelper.getFieldValue(view.getVisited());
        if (value != null) {
            LetterItem item = new LetterItem(ITEM_VISITED_DATE, value);
            model.addLetterItem(item);
        }

        // Informed
        String informed = LetterHelper.getAreaValue(view.getInformedContent());
        if (informed!=null) {
            LetterText text = new LetterText();
            text.setName(TEXT_INFORMED_CONTENT);
            text.setTextValue(informed);
            model.addLetterText(text);
        }

        // Title
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE__PREFIX).append(model.getClientHospital());
        model.setTitle(sb.toString());
    }

    @Override
    public void start() {

        this.model = new LetterModule();

        // Handle Class
        this.model.setHandleClass(Reply2Viewer.class.getName());
        this.model.setLetterType(IInfoModel.CONSULTANT);

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
        this.model.setConsultantHospital(user.getFacilityModel().getFacilityName());
        this.model.setConsultantDoctor(user.getCommonName());
        this.model.setConsultantDept(user.getDepartmentModel().getDepartmentDesc());
        this.model.setConsultantTelephone(user.getFacilityModel().getTelephone());
        this.model.setConsultantFax(user.getFacilityModel().getFacsimile());
        this.model.setConsultantZipCode(user.getFacilityModel().getZipCode());
        this.model.setConsultantAddress(user.getFacilityModel().getAddress());

        // 来院日
        String value = LetterHelper.getDateAsString(new Date(),"yyyy-MM-dd");
        LetterItem item = new LetterItem(ITEM_VISITED_DATE, value);
        model.addLetterItem(item);

        // view を生成
        this.view = new Reply2View();
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

        Reply2PDFMaker maker = new Reply2PDFMaker();
        maker.setModel(model);
        maker.setContext(getContext());
        maker.create();
    }

    @Override
    public void setEditables(boolean b) {
        view.getClientHospital().setEditable(b);
        view.getClientDept().setEditable(b);
        view.getClientDoctor().setEditable(b);
        view.getInformedContent().setEditable(b);
    }

    @Override
    public void setListeners() {

        if (listenerIsAdded) {
            return;
        }
        super.setListeners();

        JTextComponent[] jcs = new JTextComponent[]{
            view.getClientHospital(),   // 紹介元（宛先）病院
            view.getClientDept(),       // 紹介元（宛先）診療科
            view.getClientDoctor(),     // 紹介元（宛先）医師
            view.getInformedContent(),  // Informed
        };
        
        setComponentListeners(jcs);
        
        // 来院日
        PopupCalendarListener pl = new PopupCalendarListener(view.getVisited());
        view.getVisited().getDocument().addDocumentListener(dl);

        listenerIsAdded = true;
    }

    @Override
    public boolean letterIsDirty() {
        boolean dirty =  true;
        dirty = dirty && (LetterHelper.getFieldValue(view.getClientHospital()) != null);
        dirty = dirty && (LetterHelper.getFieldValue(view.getVisited()) != null);
        dirty = dirty && (LetterHelper.getAreaValue(view.getInformedContent()) != null);
        return dirty;
    }
}
