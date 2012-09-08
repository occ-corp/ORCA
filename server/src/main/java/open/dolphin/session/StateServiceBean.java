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
 * StateServiceBean
 * @author masuda, Masuda Naika
 */
@Stateless
public class StateServiceBean {
 
    private static final Logger logger = Logger.getLogger(StateServiceBean.class.getSimpleName());
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @PersistenceContext
    private EntityManager em;
    

    public void notifyEvent(StateMsgModel msg) {

        String fid = msg.getFacilityId();
        if (fid == null) {
            logger.warning("Facility id is null.");
            return;
        }
        
        FacilityContext context = contextHolder.getFacilityContext(fid);
        
        int nextId = context.getNextId();
        msg.setId(nextId);
        context.getStateMsgList().add(msg);

        List<AsyncContext> acList = contextHolder.getAsyncContextList();
        synchronized (acList) {
            int minId = Integer.MAX_VALUE;
            for (Iterator<AsyncContext> itr = acList.iterator(); itr.hasNext();) {
                
                AsyncContext ac = itr.next();
                int id = (Integer) ac.getRequest().getAttribute("id");
                minId = Math.min(minId, id);
                
                String acFid = (String) ac.getRequest().getAttribute("fid");
                String acUUID = (String) ac.getRequest().getAttribute("clientUUID");
                String issuerUUID = msg.getIssuerUUID();
                
                // 同一施設かつStateMsgModelの発行者でないクライアントに通知する
                if (fid.equals(acFid) && !acUUID.equals(issuerUUID)) {
                    itr.remove();
                    try {
                        ac.getRequest().setAttribute("nextId", String.valueOf(nextId));
                        ac.dispatch("/openSource/stateRes/nextId");
                    } catch (Exception ex) {
                        logger.warning("Exception in ac.dispatch.");
                    }
                }
            }
            // こまめにゴミ掃除
            cleanUpMsgList(context, minId);
        }
    }

    private void cleanUpMsgList(FacilityContext context, int minId) {
        
        List<StateMsgModel> stateMsgList = context.getStateMsgList();
        List<StateMsgModel> toRemove = new ArrayList<StateMsgModel>();
        for(StateMsgModel msg : stateMsgList) {
            if (msg.getId() <= minId) {
                toRemove.add(msg);
            }
        }
        stateMsgList.removeAll(toRemove);
    }
    
    public List<PatientVisitModel> getPvtList(String fid) {
        FacilityContext context = contextHolder.getFacilityContext(fid);
        return context.getPvtList();
    }
    
    public List<StateMsgModel> getStateMsgList(String fid, int fromMsgId) {
        FacilityContext context = contextHolder.getFacilityContext(fid);
        List<StateMsgModel> list = new ArrayList<StateMsgModel>();
        for (StateMsgModel msg : context.getStateMsgList()) {
            if (msg.getId() >= fromMsgId) {
                list.add(msg);
            }
        }
        return list;
    }
    
    public int getCurrentMsgId(String fid) {
        FacilityContext context = contextHolder.getFacilityContext(fid);
        return context.getCurrentId();
     }
    
    /**
     * Pvtの情報を更新する
     * 所有者、state、病名数、メモ
     */
    public int updateState(StateMsgModel msg) {
        
        int ret = 0;

        long pvtId = msg.getPvtPk();
        int state = msg.getState();
        int byomeiCount = msg.getByomeiCount();
        int byomeiCountToday = msg.getByomeiCountToday();
        String memo = msg.getMemo();

        // データベースから該当PVTを取得
        PatientVisitModel exist = em.find(PatientVisitModel.class, pvtId);
        // WatingListから開いていないとexist = nullなので
        if (exist != null) {
            // データベースのpvtStateを更新
            exist.setState(state);
            exist.setByomeiCount(byomeiCount);
            exist.setByomeiCountToday(byomeiCountToday);
            exist.setMemo(memo);
        }

        // pvtListのpvtStateとbyomei countとオーナーを更新
        String fid = msg.getFacilityId();
        FacilityContext context = contextHolder.getFacilityContext(fid);
        List<PatientVisitModel> pvtList = context.getPvtList();

        for (PatientVisitModel model : pvtList) {
            if (model.getId() == pvtId) {
                model.setState(state);
                model.setByomeiCount(byomeiCount);
                model.setByomeiCountToday(byomeiCountToday);
                model.setMemo(memo);
                model.getPatientModel().setOwnerUUID(msg.getOwnerUUID());
                // クライアントに通知
                notifyEvent(msg);
                ret = 1;
                break;
            }
        }
        
        return ret;
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
        
        Map<String, FacilityContext> map = contextHolder.getFacilityContextMap();
        for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            FacilityContext facilityContext = (FacilityContext) entry.getValue();
            List<PatientVisitModel> toRemove = new ArrayList<PatientVisitModel>();
            List<PatientVisitModel> pvtList = facilityContext.getPvtList();
            for (PatientVisitModel pvt : pvtList) {
                // BIT_SAVE_CLAIMとBIT_MODIFY_CLAIMは削除する
                if (pvt.getStateBit(PatientVisitModel.BIT_SAVE_CLAIM) 
                        || pvt.getStateBit(PatientVisitModel.BIT_MODIFY_CLAIM)) {
                    toRemove.add(pvt);
                }
            }
            pvtList.removeAll(toRemove);
            
            // StateMsgListを初期化する
            facilityContext.clearStateMsgList();
            
            // クライアントに伝える。サーバーで作るmsgはIssuerUUIDはnull
            String fid = (String) entry.getKey();
            StateMsgModel msg = new StateMsgModel();
            msg.setFacilityId(fid);
            msg.setIssuerUUID(null);
            msg.setCommand(StateMsgModel.CMD.PVT_RENEW);
            notifyEvent(msg);
        }
        logger.info("StateService: renew pvtList");
    }
}
