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
import open.dolphin.mbean.FacilityContext;
import open.dolphin.mbean.ServletContextHolder;

/**
 * ChartStateServiceBean
 * @author masuda, Masuda Naika
 */
@Stateless
public class ChartStateServiceBean {
 
    private static final Logger logger = Logger.getLogger(ChartStateServiceBean.class.getSimpleName());
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @PersistenceContext
    private EntityManager em;
    
    
    // pvtListをリニューアルする
    public  void renewPvtList() {

        Map<String, FacilityContext> map = contextHolder.getFacilityContextMap();
        for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            FacilityContext facilityContext = (FacilityContext) entry.getValue();
            List<PatientVisitModel> toRemove = new ArrayList<PatientVisitModel>();
            List<PatientVisitModel> pvtList = facilityContext.getPvtList();
            for (PatientVisitModel pvt : pvtList) {
                // BIT_SAVE_CLAIMとBIT_MODIFY_CLAIMは削除する
                if (pvt.hasStateBit(PatientVisitModel.BIT_SAVE_CLAIM) 
                        || pvt.hasStateBit(PatientVisitModel.BIT_MODIFY_CLAIM)) {
                    toRemove.add(pvt);
                }
            }
            pvtList.removeAll(toRemove);
            // 受付番号を振りなおす
            //int counter = 0;
            //for (PatientVisitModel pvt : facilityContext.pvtList) {
            //    pvt.setNumber(++counter);
            //}
            facilityContext.getChartStateMsgList().clear();
            // クライアントに伝える
            String fid = (String) entry.getKey();
            ChartStateMsgModel msg = new ChartStateMsgModel();
            msg.setFacilityId(fid);
            msg.setCommand(ChartStateMsgModel.CMD.PVT_RENEW);
            notifyEvent(msg);
        }
        logger.info("PvtServiceMediator: renew pvtList");
    }
    
    private void notifyEvent(ChartStateMsgModel msg) {

        String fid = msg.getFacilityId();
        FacilityContext context = contextHolder.getFacilityContext(fid);
        context.getChartStateMsgList().add(msg);
        String nextId = String.valueOf(context.getChartStateMsgList().size());

        List<AsyncContext> acList = contextHolder.getAsyncContextList();
        synchronized (acList) {
            for (Iterator<AsyncContext> itr = acList.iterator(); itr.hasNext();) {
                AsyncContext ac = itr.next();
                String acFid = (String) ac.getRequest().getAttribute("fid");
                if (fid != null && fid.equals(acFid)) {
                    itr.remove();
                    try {
                        ac.getRequest().setAttribute("nextId", nextId);
                        ac.dispatch("/openSource/pvt2/nextId");
                    } catch (Exception ex) {
                        //logger.warning(ex.toString());
                    }
                }
            }
        }
    }

    public PvtListModel getPvtListModel(String fid) {
        FacilityContext context = contextHolder.getFacilityContext(fid);
        PvtListModel model = new PvtListModel();
        model.setNextId(context.getChartStateMsgList().size());
        model.setPvtList(context.getPvtList());
        return model;
    }
    

    public void notifyAdd(ChartStateMsgModel msg) {
        msg.setCommand(ChartStateMsgModel.CMD.PVT_ADD);
        notifyEvent(msg);
    }
    public void notifyDelete(ChartStateMsgModel msg) {
        msg.setCommand(ChartStateMsgModel.CMD.PVT_DELETE);
        notifyEvent(msg);
    }
    public void notifyState(ChartStateMsgModel msg) {
        msg.setCommand(ChartStateMsgModel.CMD.PVT_STATE);
        notifyEvent(msg);
    }
    public void notifyMerge(ChartStateMsgModel msg) {
        msg.setCommand(ChartStateMsgModel.CMD.PVT_MERGE);
        notifyEvent(msg);
    }
    
    // 起動後最初のPvtListを作る
    public void initializePvtList() {

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
        int counter = 0;

        for (PatientVisitModel pvt : result) {
            
            String fid = pvt.getFacilityId();
            FacilityContext context = contextHolder.getFacilityContext(fid);
            context.getPvtList().add(pvt);

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
