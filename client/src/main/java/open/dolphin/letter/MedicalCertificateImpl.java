package open.dolphin.letter;

import java.awt.BorderLayout;
import java.util.Date;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;
import open.dolphin.client.ClientContext;
import open.dolphin.client.Panel2;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import org.apache.log4j.Level;

/**
 * MedicalCertificateImpl
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class MedicalCertificateImpl extends AbstractLetterImpl {

    protected static final String TITLE = "診断書";
    protected static final String ITEM_DISEASE = "disease";
    protected static final String TEXT_INFORMED_CONTENT = "informedContent";

    protected MedicalCertificateView view;
    
    protected boolean DEBUG;

    /** Creates a new instance of LetterDocument */
    public MedicalCertificateImpl() {
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
            view = new MedicalCertificateView();
        }

        // 患者氏名
        LetterHelper.setModelValue(view.getPatientNameFld(), m.getPatientName());

        // 患者生年月日
        //String val = LetterHelper.getBirdayWithAge(m.getPatientBirthday(), m.getPatientAge());
        String val = LetterHelper.getDateString(m.getPatientBirthday());
        LetterHelper.setModelValue(view.getPatientBirthday(), val);

        // 患者性別
        LetterHelper.setModelValue(view.getSexFld(), m.getPatientGender());

        // 患者住所
        LetterHelper.setModelValue(view.getPatientAddress(), m.getPatientAddress());

        // 確定日
        String dateStr = LetterHelper.getDateAsString(m.getConfirmed());
        LetterHelper.setModelValue(view.getConfirmedFld(), dateStr);

        // 病院住所
        val = LetterHelper.getAddressWithZipCode(m.getConsultantAddress(), m.getConsultantZipCode());
        LetterHelper.setModelValue(view.getHospitalAddressFld(), val);

        // 病院名
        LetterHelper.setModelValue(view.getHospitalNameFld(), m.getConsultantHospital());

        // 病院電話
        LetterHelper.setModelValue(view.getHospitalTelephoneFld(), m.getConsultantTelephone());

        // 医師
        LetterHelper.setModelValue(view.getDoctorNameFld(), m.getConsultantDoctor());

        //----------------------------------------------------------------------

        // 病名
        String value = model.getItemValue(ITEM_DISEASE);
        if (value != null) {
            LetterHelper.setModelValue(view.getDiseaseFld(), value);
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

        // 患者情報、差し出し人側はtartでmodelに設定済

        // 傷病名
        String value = LetterHelper.getFieldValue(view.getDiseaseFld());
        model.addLetterItem(new LetterItem(ITEM_DISEASE, value));

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
        sb.append(TITLE).append(":").append(value);
        model.setTitle(sb.toString());
    }

    @Override
    public void start() {

        this.model = new LetterModule();

        // Handle Class
        this.model.setHandleClass(MedicalCertificateViewer.class.getName());
        this.model.setLetterType(IInfoModel.MEDICAL_CERTIFICATE);

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
        this.model.setPatientAge(ModelUtils.getAge(patient.getBirthday()));
        if (patient.getSimpleAddressModel()!=null) {
            this.model.setPatientAddress(patient.getSimpleAddressModel().getAddress());
//masuda^   zip codeも設定する
            this.model.setPatientZipCode(patient.getSimpleAddressModel().getZipCode());
//masuda$
        }
        this.model.setPatientTelephone(patient.getTelephone());

        // 病院
        UserModel user = Project.getUserModel();
        this.model.setConsultantHospital(user.getFacilityModel().getFacilityName());
        this.model.setConsultantDoctor(user.getCommonName());
        this.model.setConsultantDept(user.getDepartmentModel().getDepartmentDesc());
        this.model.setConsultantTelephone(user.getFacilityModel().getTelephone());
        this.model.setConsultantFax(user.getFacilityModel().getFacsimile());
        this.model.setConsultantZipCode(user.getFacilityModel().getZipCode());
        this.model.setConsultantAddress(user.getFacilityModel().getAddress());

        // view を生成
        this.view = new MedicalCertificateView();
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

        MedicalCertificatePDFMaker maker = new MedicalCertificatePDFMaker();
        maker.setModel(model);
        maker.setContext(getContext());
        maker.create();
    }

    @Override
    public void setEditables(boolean b) {
        view.getDiseaseFld().setEditable(b);
        view.getInformedContent().setEditable(b);
    }

    @Override
    public void setListeners() {
        
        if (listenerIsAdded) {
            return;
        }
        super.setListeners();

        JTextComponent[] jcs = new JTextComponent[]{
            view.getDiseaseFld(),       // 傷病名
            view.getInformedContent(),  // Informed
        };
        
        setComponentListeners(jcs);

        listenerIsAdded = true;
    }

    @Override
    public boolean letterIsDirty() {
        boolean dirty =  true;
        dirty = dirty && (LetterHelper.getFieldValue(view.getDiseaseFld()) != null);
        dirty = dirty && (LetterHelper.getAreaValue(view.getInformedContent()) != null);
        return dirty;
    }
}
