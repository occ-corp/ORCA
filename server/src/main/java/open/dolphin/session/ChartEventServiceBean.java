package open.dolphin.session;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.AsyncContext;
import open.dolphin.infomodel.*;
import open.dolphin.mbean.ServletContextHolder;
import open.dolphin.rest.ChartEventResource;

/**
 * StateServiceBean
 * @author masuda, Masuda Naika
 */
@Stateless
public class ChartEventServiceBean {

    private static final Logger logger = Logger.getLogger(ChartEventServiceBean.class.getSimpleName());
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @PersistenceContext
    private EntityManager em;
    

    public void notifyEvent(ChartEvent evt) {

        String fid = evt.getFacilityId();
        if (fid == null) {
            logger.warning("Facility id is null.");
            return;
        }

        List<AsyncContext> acList = contextHolder.getAsyncContextList();
        synchronized (acList) {
            for (Iterator<AsyncContext> itr = acList.iterator(); itr.hasNext();) {
                
                AsyncContext ac = itr.next();
                String acFid = (String) ac.getRequest().getAttribute(ChartEventResource.FID);
                String acUUID = (String) ac.getRequest().getAttribute(ChartEventResource.CLIENT_UUID);
                String issuerUUID = evt.getIssuerUUID();
                
                // 同一施設かつStateMsgModelの発行者でないクライアントに通知する
                if (fid.equals(acFid) && !acUUID.equals(issuerUUID)) {
                    itr.remove();
                    try {
                        ac.getRequest().setAttribute(ChartEventResource.KEY_NAME, evt);
                        ac.dispatch(ChartEventResource.DISPATCH_URL);
                    } catch (Exception ex) {
                        logger.warning("Exception in ac.dispatch.");
                    }
                }
            }
        }
    }
    
    public String getServerUUID() {
        return contextHolder.getServerUUID();
    }

    public List<PatientVisitModel> getPvtList(String fid) {
        return contextHolder.getPvtList(fid);
    }
    /**
     * status情報を更新する
     */
    public int updateState(ChartEvent msg) {

        // msgからパラメーターを取得
        String fid = msg.getFacilityId();
        long pvtId = msg.getPvtPk();
        int state = msg.getState();
        int byomeiCount = msg.getByomeiCount();
        int byomeiCountToday = msg.getByomeiCountToday();
        String memo = msg.getMemo();
        String ownerUUID = msg.getOwnerUUID();
        long ptPk = msg.getPtPk();
        
        List<PatientVisitModel> pvtList = contextHolder.getPvtList(fid);

        // データベースのPatientVisitModelを更新
        PatientVisitModel pvt = em.find(PatientVisitModel.class, pvtId);
        if (pvt != null) {
            pvt.setState(state);
            pvt.setByomeiCount(byomeiCount);
            pvt.setByomeiCountToday(byomeiCountToday);
            pvt.setMemo(memo);
        }
        // データベースのPatientModelを更新
        PatientModel pm = em.find(PatientModel.class, ptPk);
        if (pm != null) {
            pm.setOwnerUUID(ownerUUID);
        }

        // pvtListを更新
        for (PatientVisitModel model : pvtList) {
            if (model.getId() == pvtId) {
                model.setState(state);
                model.setByomeiCount(byomeiCount);
                model.setByomeiCountToday(byomeiCountToday);
                model.setMemo(memo);
                model.getPatientModel().setOwnerUUID(ownerUUID);
                break;
            }
        }

        // クライアントに通知
        notifyEvent(msg);

        return 1;
    }

    public void start() {
        setupServerUUID();
        initializePvtList();
    }
    
    // serverUUIDを設定する
    private void setupServerUUID() {
        String uuid = UUID.randomUUID().toString();
        contextHolder.setServerUUID(uuid);
    }
    
    // 起動後最初のPvtListを作る
    private void initializePvtList() {

        contextHolder.setToday();
        
        // サーバーの「今日」で管理する
        final SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        String fromDate = frmt.format(contextHolder.getToday().getTime());
        String toDate = frmt.format(contextHolder.getTomorrow().getTime());

        // PatientVisitModelを施設IDで検索する
        final String sql =
                "from PatientVisitModel p " +
                "where p.pvtDate >= :fromDate and p.pvtDate < :toDate " +
                "order by p.id";
        @SuppressWarnings("unchecked")
        List<PatientVisitModel> result =
                em.createQuery(sql)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        // 患者の基本データを取得する
        // 来院情報と患者は ManyToOne の関係である
        //int counter = 0;

        for (PatientVisitModel pvt : result) {
            
            String fid = pvt.getFacilityId();
            contextHolder.getPvtList(fid).add(pvt);

            PatientModel patient = pvt.getPatientModel();

            // 患者の健康保険を取得する
            @SuppressWarnings("unchecked")
            List<HealthInsuranceModel> insurances =
                    em.createQuery("from HealthInsuranceModel h where h.patient.id = :pk")
                    .setParameter("pk", patient.getId())
                    .getResultList();
            patient.setHealthInsurances(insurances);

            KarteBean karte = (KarteBean)
                    em.createQuery("from KarteBean k where k.patient.id = :pk")
                    .setParameter("pk", patient.getId())
                    .getSingleResult();

            // カルテの PK を得る
            long karteId = karte.getId();

            // 予約を検索する
            @SuppressWarnings("unchecked")
             List<AppointmentModel> list =
                    em.createQuery("from AppointmentModel a where a.karte.id = :karteId and a.date = :date")
                    .setParameter("karteId", karteId)
                    .setParameter("date", contextHolder.getToday().getTime())
                    .getResultList();
            if (list != null && !list.isEmpty()) {
                AppointmentModel appo = list.get(0);
                pvt.setAppointment(appo.getName());
            }

            // 病名数をチェックする
            setByomeiCount(karteId, pvt);
            // 受付番号セット
            //pvt.setNumber(++counter);
        }
        
        logger.info("PvtServiceMediator: pvtList initialized");
    }
    
    // データベースを調べてpvtに病名数を設定する
    public void setByomeiCount(long karteId, PatientVisitModel pvt) {

        // byomeiCountがすでに0でないならば、byomeiCountは設定済みであろう
        //if (pvt.getByomeiCount() != 0) {
        //    return;
        //}

        int byomeiCount = 0;
        int byomeiCountToday = 0;
        Date pvtDate = ModelUtils.getCalendar(pvt.getPvtDate()).getTime();

        // データベースから検索
        final String sql = "from RegisteredDiagnosisModel r where r.karte.id = :karteId";
        List<RegisteredDiagnosisModel> rdList =
                em.createQuery(sql)
                .setParameter("karteId", karteId)
                .getResultList();
        for (RegisteredDiagnosisModel rd : rdList) {
            Date start = ModelUtils.getStartDate(rd.getStarted()).getTime();
            Date ended = ModelUtils.getEndedDate(rd.getEnded()).getTime();
            if (start.getTime() == pvtDate.getTime()) {
                byomeiCountToday++;
            }
            if (ModelUtils.isDateBetween(start, ended, pvtDate)) {
                byomeiCount++;
            }
        }
        pvt.setByomeiCount(byomeiCount);
        pvt.setByomeiCountToday(byomeiCountToday);
    }
    
    // ０時にpvtListをリニューアルする
    public void renewPvtList() {
        
        contextHolder.setToday();
        
        Map<String, List<PatientVisitModel>> map = contextHolder.getPvtListMap();
        
        for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            List<PatientVisitModel> pvtList = (List<PatientVisitModel>) entry.getValue();
            
            List<PatientVisitModel> toRemove = new ArrayList<PatientVisitModel>();
            for (PatientVisitModel pvt : pvtList) {
                // BIT_SAVE_CLAIMとBIT_MODIFY_CLAIMは削除する
                if (pvt.getStateBit(PatientVisitModel.BIT_SAVE_CLAIM) 
                        || pvt.getStateBit(PatientVisitModel.BIT_MODIFY_CLAIM)) {
                    toRemove.add(pvt);
                }
            }
            pvtList.removeAll(toRemove);
            
            // クライアントに伝える。
            String fid = (String) entry.getKey();
            String uuid = contextHolder.getServerUUID();
            ChartEvent msg = new ChartEvent(uuid);
            msg.setFacilityId(fid);
            msg.setEventType(ChartEvent.EVENT.PVT_RENEW);
            notifyEvent(msg);
        }
        logger.info("StateService: renew pvtList");
    }
}
