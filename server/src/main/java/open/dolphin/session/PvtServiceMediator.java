package open.dolphin.session;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Timeout;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.AsyncContext;
import open.dolphin.infomodel.*;
import open.dolphin.mbean.AsyncContextHolder;

/**
 * PvtServiceMediator
 *
 * RemovtePvtServiceのメディエーター役のSingleton EJB
 *
 * @author masuda, Masuda Naika
 */
@Singleton
public class PvtServiceMediator {
    
    public static final int BIT_OPEN            = 0;
    public static final int BIT_SAVE_CLAIM      = 1;
    public static final int BIT_MODIFY_CLAIM    = 2;
    public static final int BIT_TREATMENT       = 3;
    public static final int BIT_HURRY           = 4;
    public static final int BIT_GO_OUT          = 5;
    public static final int BIT_CANCEL          = 6;
    public static final int BIT_UNFINISHED      = 8;

    // facilityIdとfaciltyContextのマップ
    private Map<String, FacilityContext> facilityContextMap 
            = new ConcurrentHashMap<String, FacilityContext>();

    // 今日と明日
    private GregorianCalendar today;
    private GregorianCalendar tomorrow;

    private static final Logger logger = Logger.getLogger(PvtServiceMediator.class.getSimpleName());
    
    @Inject
    private AsyncContextHolder contextHolder;
    
    @PersistenceContext
    private EntityManager em;
    
    
    private class FacilityContext {

        private final List<PvtMessageModel> pvtMessageList = new CopyOnWriteArrayList<PvtMessageModel>();
        private final List<PatientVisitModel> pvtList = new CopyOnWriteArrayList<PatientVisitModel>();
    }
    
   
    @PostConstruct
    public void init() {
        initializePvtList();
    }

    @PreDestroy
    public void stop() {

    }

    // 日付が変わったらpvtListをクリアしクライアントに伝える
    @Schedule(hour="0", minute="0", persistent=false)
    public void dayChange() {
        setToday();
        renewPvtList();
    }
    @Timeout
    public void timeout(Timer timer) {
        logger.warning("PvtServiceMediator: timeout occurred");
    }
    
    // 今日と明日を設定する
    private void setToday() {

        today= new GregorianCalendar();
        int year = today.get(GregorianCalendar.YEAR);
        int month = today.get(GregorianCalendar.MONTH);
        int date = today.get(GregorianCalendar.DAY_OF_MONTH);
        today.clear();
        today.set(year, month, date);

        tomorrow = new GregorianCalendar();
        tomorrow.setTime(today.getTime());
        tomorrow.add(GregorianCalendar.DAY_OF_MONTH, 1);
    }
    
    // pvtListをリニューアルする
    @SuppressWarnings("unchecked")
    private void renewPvtList() {

        for (Iterator itr = facilityContextMap.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            FacilityContext facilityContext = (FacilityContext) entry.getValue();
            List<PatientVisitModel> pvtList = facilityContext.pvtList;
            
            for (Iterator<PatientVisitModel> itr1 = pvtList.iterator(); itr.hasNext();) {
                PatientVisitModel pvt = itr1.next();
                // BIT_SAVE_CLAIMとBIT_MODIFY_CLAIMは削除する
                if (pvt.hasStateBit(BIT_SAVE_CLAIM) || pvt.hasStateBit(BIT_MODIFY_CLAIM)) {
                    itr1.remove();
                }
            }
            // 受付番号を振りなおす
            //int counter = 0;
            //for (PatientVisitModel pvt : facilityContext.pvtList) {
            //    pvt.setNumber(++counter);
            //}
            facilityContext.pvtMessageList.clear();
            // クライアントに伝える
            String fid = (String) entry.getKey();
            PvtMessageModel msg = new PvtMessageModel();
            msg.setCommand(PvtMessageModel.CMD_RENEW);
            notifyEvent(fid, msg);
        }
        logger.info("PvtServiceMediator: renew pvtList");
    }
    
    private void notifyEvent(final String fid, final PvtMessageModel msg) {

        FacilityContext context = getFacilityContext(fid);
        context.pvtMessageList.add(msg);
        String nextId = String.valueOf(context.pvtMessageList.size());

        List<AsyncContext> acList = contextHolder.getAsyncContextList();
        synchronized (acList) {
            for (Iterator<AsyncContext> itr = acList.iterator(); itr.hasNext();) {
                AsyncContext ac = itr.next();
                String acFid = (String) ac.getRequest().getAttribute("fid");
                if (fid != null && fid.equals(acFid)) {
                    itr.remove();
                    ac.getRequest().setAttribute("nextId", nextId);
                    ac.dispatch("/openSource/pvt2/nextId");
                }
            }
        }
    }
    
    private FacilityContext getFacilityContext(String fid) {
        FacilityContext context = facilityContextMap.get(fid);
        if (context == null) {
            context = new FacilityContext();
            facilityContextMap.put(fid, context);
        }
        return context;
    }

    public PvtListModel getPvtListModel(String fid) {
        FacilityContext context = getFacilityContext(fid);
        PvtListModel model = new PvtListModel();
        model.setNextId(context.pvtMessageList.size());
        model.setPvtList(context.pvtList);
        return model;
    }
    
    public List<PvtMessageModel> getPvtMessageList(String fid, int from) {
        FacilityContext context = getFacilityContext(fid);
        int to = context.pvtMessageList.size();
        if (to > 0 && to > from && from >= 0) {
            return context.pvtMessageList.subList(from, to);
        } else {
            return null;
        }
    }

    public GregorianCalendar getToday() {
        return today;
    }
    public GregorianCalendar getTomorrow() {
        return tomorrow;
    }

    public void notifyAdd(String fid, PvtMessageModel msg) {
        msg.setCommand(PvtMessageModel.CMD_ADD);
        notifyEvent(fid, msg);
    }
    public void notifyDelete(String fid, PvtMessageModel msg) {
        msg.setCommand(PvtMessageModel.CMD_DELETE);
        notifyEvent(fid, msg);
    }
    public void notifyState(String fid, PvtMessageModel msg) {
        msg.setCommand(PvtMessageModel.CMD_STATE);
        notifyEvent(fid, msg);
    }
    public void notifyMerge(String fid, PvtMessageModel msg) {
        msg.setCommand(PvtMessageModel.CMD_MERGE);
        notifyEvent(fid, msg);
    }
    
    
    // 起動後最初のPvtListを作る
    private void initializePvtList() {

        setToday();
        
        // サーバーの「今日」で管理する
        final SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        String fromDate = frmt.format(today.getTime());
        String toDate = frmt.format(tomorrow.getTime());

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
        int counter = 0;

        for (PatientVisitModel pvt : result) {
            
            String fid = pvt.getFacilityId();
            FacilityContext context = getFacilityContext(fid);
            context.pvtList.add(pvt);

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
                    .setParameter("date", today.getTime())
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
        @SuppressWarnings("unchecked")
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
}
