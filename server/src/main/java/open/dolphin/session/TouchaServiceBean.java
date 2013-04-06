package open.dolphin.session;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.NLaboModule;
import open.dolphin.infomodel.PVTHealthInsuranceModel;
import open.dolphin.infomodel.PatientMemoModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.toucha.BeanUtils;
import open.dolphin.toucha.KarteHtmlRenderer;
import open.dolphin.toucha.LaboHtmlRenderer;
import open.dolphin.toucha.StringTool;
import open.dolphin.toucha.model.DiagnosisModelS;
import open.dolphin.toucha.model.DocumentModelS;
import open.dolphin.toucha.model.PatientModelS;
import open.dolphin.toucha.model.PatientVisitModelList;
import open.dolphin.toucha.model.PatientVisitModelS;

/**
 *
 * @author masuda, Masuda Naika
 */
@Stateless
public class TouchaServiceBean {
    
    private static final String PK = "pk";
    private static final String FID = "fid";
    private static final String PID = "pid";
    private static final String ID = "id";
    private static final String DATE = "date";
    
    private static final String QUERY_DIAGNOSIS 
            = "from RegisteredDiagnosisModel r "
            + "where r.karte.patient.patientId =:pid and r.karte.patient.facilityId = :fid "
            + "and r.started <= :date and r.ended is NULL order by r.started desc";
    private static final String QUERY_INSURANCE_BY_PATIENT_PK 
            = "from HealthInsuranceModel h where h.patient.id=:pk";
    private static final String QUERY_PATIENT_MEMO 
            = "from PatientMemoModel p where p.karte.patient.id=:id";
    private static final String QUERY_PVT_BY_FID_PVTDATE
            = "from PatientVisitModel p where p.facilityId=:fid and p.pvtDate like :date";
    private static final String QUERY_DOC = "from DocumentModel d where d.karte.patient.patientId = :pid "
            + "and d.karte.patient.facilityId = :fid and (d.status='F' or d.status='T') ";
    private static final String QUERY_DOCUMENT_LATEST = QUERY_DOC + "order by d.started desc";
    private static final String QUERY_DOCUMENT_PREV = QUERY_DOC + "and d.started < :date order by d.started desc";
    private static final String QUERY_DOCUMENT_NEXT = QUERY_DOC + "and d.started > :date order by d.started asc";
    private static final String QUERY_DOCUMENT_DATE = QUERY_DOC + "and d.started >= :date order by d.started asc";
    
    private static final String QUERY_PATIENT
            = "from PatientModel p where p.facilityId=:fid and p.patientId = :pid";
    
    @PersistenceContext
    private EntityManager em;
    
    @Inject
    private PatientServiceBean patientServiceBean;
    
    @Inject
    private MasudaServiceBean masudaServiceBean;
    
    @Inject
    private NLabServiceBean nlaboServiceBean;
    
    
    public String getLaboHtml(String fid, String ptId, int firstResult, int maxResults) {
        StringBuilder sb = new StringBuilder();
        sb.append(fid).append(":").append(ptId);
        String fidPid = sb.toString();
        List<NLaboModule> modules = nlaboServiceBean.getLaboTest(fidPid, firstResult, maxResults);
        LaboHtmlRenderer renderer = LaboHtmlRenderer.getInstance();
        String html = renderer.render(modules);
        return html;
    }

    public List<PatientModelS> getSearchResults(String fid, String text, String type) {
        
        List<PatientModel> list;
        if ("karte".equals(type)) {
            list = masudaServiceBean.getKarteFullTextSearch(fid, 0, text);
        } else {
            if (StringTool.isDate(text)) {
                list = patientServiceBean.getPatientsByPvtDate(fid, text);
            } else if (StringTool.startsWithKatakana(text)) {
                list = patientServiceBean.getPatientsByKana(fid, text);
            } else if (StringTool.startsWithHiragana(text)) {
                text = StringTool.hiraganaToKatakana(text);
                list = patientServiceBean.getPatientsByKana(fid, text);
            } else if (StringTool.isNameAddress(text)) {
                list = patientServiceBean.getPatientsByName(fid, text);
            } else {
                list = patientServiceBean.getPatientsByDigit(fid, text);
            }
        }
        
        List<PatientModelS> sList = new ArrayList<PatientModelS>();
        for (PatientModel pm : list) {
            PatientModelS sModel = new PatientModelS();
            sModel.setLiteModel(pm);
            sList.add(sModel);
        }
        return sList;
    }
    
    public List<DiagnosisModelS> getDiagnosis(String fid, String ptId) {

        List<RegisteredDiagnosisModel> rdList = 
                em.createQuery(QUERY_DIAGNOSIS)
                .setParameter(FID, fid)
                .setParameter(PID, ptId)
                .setParameter(DATE, new Date())
                .getResultList();
        
        List<DiagnosisModelS> list = new ArrayList<DiagnosisModelS>();
        for (RegisteredDiagnosisModel rd : rdList) {
            DiagnosisModelS diag = new DiagnosisModelS(rd);
            list.add(diag);
        }
        
        return list;
    }
    
    public PatientModelS getPatientModel(String fid, String ptId) {
        
        try {
            // 患者モデルを取得する
            PatientModel pm = (PatientModel) 
                    em.createQuery(QUERY_PATIENT)
                    .setParameter(FID, fid)
                    .setParameter(PID, ptId)
                    .setMaxResults(1)
                    .getSingleResult();
            
            // 患者Memoを取得する
            PatientMemoModel memoModel = (PatientMemoModel)
                    em.createQuery(QUERY_PATIENT_MEMO)
                    .setParameter(ID, pm.getId())
                    .setMaxResults(1)
                    .getSingleResult();
            
            pm.setHealthInsurances(getHealthInsurances(pm.getId()));
            decodeHealthInsurance(pm);
            
            return new PatientModelS(pm, memoModel.getMemo());
            
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return new PatientModelS();
    }
    
    private List<HealthInsuranceModel> getHealthInsurances(long pk) {

        List<HealthInsuranceModel> ins =
                em.createQuery(QUERY_INSURANCE_BY_PATIENT_PK)
                .setParameter(PK, pk)
                .getResultList();

        return ins;
    }
    
    private void decodeHealthInsurance(PatientModel patient) {

        // Health Insurance を変換をする beanXML2PVT
        Collection<HealthInsuranceModel> c = patient.getHealthInsurances();

        if (c != null && !c.isEmpty()) {

            List<PVTHealthInsuranceModel> list = new ArrayList<PVTHealthInsuranceModel>(c.size());

            for (HealthInsuranceModel model : c) {
                try {
                    // byte[] を XMLDecord
                    PVTHealthInsuranceModel hModel = (PVTHealthInsuranceModel) 
                            BeanUtils.xmlDecode(model.getBeanBytes());
                    list.add(hModel);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            patient.setPvtHealthInsurances(list);
            patient.getHealthInsurances().clear();
            patient.setHealthInsurances(null);
        }
    }
    
    public PatientVisitModelList getPvtList(String fid, String pvtDate, String direction) {
        
        SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        
        Date d = new Date();
        try {
            d = frmt.parse(pvtDate);            
        } catch (ParseException ex) {
        }
        
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(d);
        if ("prev".equals(direction)) {
            gc.add(GregorianCalendar.DATE, -1);
        } else if ("next".equals(direction)) {
            gc.add(GregorianCalendar.DATE, 1);
        }
        pvtDate = frmt.format(gc.getTime());

        List<PatientVisitModel> pvtList =
                em.createQuery(QUERY_PVT_BY_FID_PVTDATE)
                .setParameter(FID, fid)
                .setParameter(DATE, pvtDate + "%")
                .getResultList();
        
        List<PatientVisitModelS> sList = new ArrayList<PatientVisitModelS>();
        for (PatientVisitModel pvt : pvtList) {
            sList.add(new PatientVisitModelS(pvt));
        }
        
        PatientVisitModelList list = new PatientVisitModelList();
        list.setPvtDate(pvtDate);
        list.setPvtList(sList);

        return list;
    }
    
    
    public DocumentModelS getDocHtml(String fid, String ptId, 
            String docPkStr, String docDateStr, String direction) {

        DocumentModel model = null;
        long docPk = -1;

        if (docDateStr != null) {
            // 日付指定の場合
            try {
                SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
                Date docDate = frmt.parse(docDateStr);
                model = (DocumentModel) em.createQuery(QUERY_DOCUMENT_DATE)
                        .setParameter(PID, ptId)
                        .setParameter(FID, fid)
                        .setParameter(DATE, docDate)
                        .setMaxResults(1)
                        .getSingleResult();
                docPk = model.getId();
            } catch (Exception ex) {
            }
        } else {
            if ("0".equals(docPkStr)) {
                // docPk未指定の場合
                try {
                    model = (DocumentModel) em.createQuery(QUERY_DOCUMENT_LATEST)
                            .setParameter(PID, ptId)
                            .setParameter(FID, fid)
                            .setMaxResults(1)
                            .getSingleResult();
                    docPk = model.getId();
                } catch (Exception ex) {
                }
            } else {
                long id = Long.valueOf(docPkStr);
                if ("prev".equals(direction)) {
                    try {
                        DocumentModel doc = em.find(DocumentModel.class, id);
                        model = (DocumentModel) em.createQuery(QUERY_DOCUMENT_PREV)
                                .setParameter(PID, ptId)
                                .setParameter(FID, fid)
                                .setParameter(DATE, doc.getStarted())
                                .setMaxResults(1)
                                .getSingleResult();
                        docPk = model.getId();
                    } catch (Exception ex) {
                    }
                } else if ("next".equals(direction)) {
                    try {
                        DocumentModel doc = em.find(DocumentModel.class, id);
                        model = (DocumentModel) em.createQuery(QUERY_DOCUMENT_NEXT)
                                .setParameter(PID, ptId)
                                .setParameter(FID, fid)
                                .setParameter(DATE, doc.getStarted()) 
                                .setMaxResults(1)
                                .getSingleResult();
                        docPk = model.getId();
                    } catch (Exception ex) {
                    }
                } else {
                    // docPk指定の場合
                    try {
                        model = em.find(DocumentModel.class, id);
                        docPk = model.getId();
                    } catch (Exception ex) {
                    }
                }
            }
        }

        String html = (model != null)
                ? KarteHtmlRenderer.getInstance().render(model)
                : "<div>No Document</div>";
        
        DocumentModelS smodel = new DocumentModelS();
        smodel.setDocPk(docPk);
        smodel.setHtml(html);

        return smodel;
    }
}
