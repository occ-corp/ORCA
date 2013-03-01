package open.dolphin.client;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
//import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import open.dolphin.delegater.ChartEventDelegater;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.BeanUtils;
import open.dolphin.util.NamedThreadFactory;

/**
 * カルテオープンなどの状態の変化をまとめて管理する
 * @author masuda, Masuda Naika
 */
public class ChartEventHandler {
    
    // propName
    private static final String CHART_EVENT_PROP = "chartEvent";
    
     // このクライアントのパラメーター類
    private String clientUUID;
    private String orcaId;
    private String deptCode;
    private String departmentDesc;
    private String doctorName;
    private String userId;
    private String jmariCode;
    private String facilityId;

    private PropertyChangeSupport boundSupport;
    
    // ChartEvent監視タスク
    private EventListenThread listenThread;
    
    // 状態変化を各listenerに通知するタスク
    private ExecutorService onEventExec;
    
    private static final ChartEventHandler instance;

    static {
        instance = new ChartEventHandler();
    }

    private ChartEventHandler() {
        init();
     }

    public static ChartEventHandler getInstance() {
        return instance;
    }
    
    private void init() {
        clientUUID = Dolphin.getInstance().getClientUUID();
        orcaId = Project.getUserModel().getOrcaId();
        deptCode = Project.getUserModel().getDepartmentModel().getDepartment();
        departmentDesc = Project.getUserModel().getDepartmentModel().getDepartmentDesc();
        doctorName = Project.getUserModel().getCommonName();
        userId = Project.getUserModel().getUserId();
        jmariCode = Project.getString(Project.JMARI_CODE);
        facilityId = Project.getFacilityId();
    }
    
    public String getClientUUID() {
        return clientUUID;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(CHART_EVENT_PROP, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.removePropertyChangeListener(CHART_EVENT_PROP, l);
    }

    // 状態変更処理の共通入り口
    private void publish(ChartEventModel evt) {
        onEventExec.execute(new LocalOnEventTask(evt));
    }
    
    public void publishPvtDelete(PatientVisitModel pvt) {
        
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_DELETE);
        
        publish(evt);
    }
    
    public void publishPvtState(PatientVisitModel pvt) {
        
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_STATE);
        
        publish(evt);
    }
    
    public void publishKarteOpened(PatientVisitModel pvt) {
        
        // 閲覧のみの処理、ええい！面倒だ！
        if (!clientUUID.equals(pvt.getPatientModel().getOwnerUUID())) {
            return;
        }

        // PatientVisitModel.BIT_OPENを立てる
        pvt.setStateBit(PatientVisitModel.BIT_OPEN, true);
        // ChartStateListenerに通知する
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_STATE);
        
        publish(evt);
    }
    
    public void publishKarteClosed(PatientVisitModel pvt) {
        
        // 閲覧のみの処理、ええい！面倒だ！
        if (!clientUUID.equals(pvt.getPatientModel().getOwnerUUID())) {
            return;
        }
        
        // PatientVisitModel.BIT_OPENとownerUUIDをリセットする
        pvt.setStateBit(PatientVisitModel.BIT_OPEN, false);
        pvt.getPatientModel().setOwnerUUID(null);
        
        // ChartStateListenerに通知する
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_STATE);
        
        publish(evt);
    }

    public void start() {
        NamedThreadFactory factory = new NamedThreadFactory("ChartEvent Handle Task");
        onEventExec = Executors.newSingleThreadExecutor(factory);
        listenThread = new EventListenThread();
        listenThread.start();
    }

    public void stop() {
        listenThread.halt();
        shutdownExecutor();
    }

    private void shutdownExecutor() {

        try {
            onEventExec.shutdown();
            if (!onEventExec.awaitTermination(20, TimeUnit.MILLISECONDS)) {
                onEventExec.shutdownNow();
            }
        } catch (InterruptedException ex) {
            onEventExec.shutdownNow();
        } catch (NullPointerException ex) {
        }
        onEventExec = null;
    }

    // Commetでサーバーと同期するスレッド
    private class EventListenThread extends Thread {
        
        //private SubscribeTask subscribeTask;
        //private ExecutorService subscribeExec;
        //private Future<InputStream> future;
        private boolean isRunning;
        
        private EventListenThread() {
            super("ChartEvent Listen Thread");
            isRunning = true;
            //NamedThreadFactory factory = new NamedThreadFactory("ChartEvent Subscribe Task");
            //subscribeExec = Executors.newSingleThreadExecutor(factory);
            //subscribeTask = new SubscribeTask();
        }

        public void halt() {
            isRunning = false;
            interrupt();
            /*
            if (future != null) {
                future.cancel(true);
            }
            try {
                subscribeExec.shutdown();
                if (!subscribeExec.awaitTermination(20, TimeUnit.MILLISECONDS)) {
                    subscribeExec.shutdownNow();
                }
            } catch (InterruptedException ex) {
                subscribeExec.shutdownNow();
            } catch (NullPointerException ex) {
            }
            subscribeExec = null;
            */
        }
        
        @Override
        public void run() {
            
            while (isRunning) {
                try {
                    // Futureでなくてもいい気がするｗ
                    //future = subscribeExec.submit(subscribeTask);
                    //InputStream is = future.get();
                    InputStream is = ChartEventDelegater.getInstance().subscribe();
                    onEventExec.execute(new RemoteOnEventTask(is));
                } catch (Exception e) {
                }
            }
        }
    }
/*
    private class SubscribeTask implements Callable<InputStream> {
        
        @Override
        public InputStream call() throws Exception {
            return ChartEventDelegater.getInstance().subscribe();
        }
    }
*/
    // 自クライアントの状態変更後、サーバーに通知するタスク
    private class LocalOnEventTask implements Runnable {
        
        private ChartEventModel evt;
        
        private LocalOnEventTask(ChartEventModel evt) {
            this.evt = evt;
        }

        @Override
        public void run() {
            
            // まずは自クライアントを更新
            boundSupport.firePropertyChange(CHART_EVENT_PROP, null, evt);

            // サーバーに更新を通知
            ChartEventDelegater del = ChartEventDelegater.getInstance();
            try {
                del.putChartEvent(evt);
            } catch (Exception ex) {
            }
        }
    }
    
    // 状態変化通知メッセージをデシリアライズし各リスナに処理を分配する
    private class RemoteOnEventTask implements Runnable {
        
        private InputStream is;
        
        private RemoteOnEventTask(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            
            ChartEventModel evt = (ChartEventModel) 
                    JsonConverter.getInstance().fromJson(is, ChartEventModel.class);
            
            if (evt == null) {
                return;
            }
            
            // PatientModelが乗っかってきている場合は保険をデコード
            PatientModel pm = evt.getPatientModel();
            if (pm != null) {
                decodeHealthInsurance(pm);
            }
            PatientVisitModel pvt = evt.getPatientVisitModel();
            if (pvt != null) {
                decodeHealthInsurance(pvt.getPatientModel());
            }
            
            // 各リスナーで更新処理をする
            boundSupport.firePropertyChange(CHART_EVENT_PROP, null, evt);
        }
    }
    
    /**
     * バイナリの健康保険データをオブジェクトにデコードする。
     *
     * @param patient 患者モデル
     */
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
    
    // FakePatientVisitModelを作る
    public PatientVisitModel createFakePvt(PatientModel pm) {

        // 来院情報を生成する
        PatientVisitModel pvt = new PatientVisitModel();
        pvt.setId(0L);
        pvt.setPatientModel(pm);
        pvt.setFacilityId(facilityId);

        //--------------------------------------------------------
        // 受け付けを通していないのでログイン情報及び設定ファイルを使用する
        // 診療科名、診療科コード、医師名、医師コード、JMARI
        // 2.0
        //---------------------------------------------------------
        pvt.setDeptName(departmentDesc);
        pvt.setDeptCode(deptCode);
        pvt.setDoctorName(doctorName);
        if (orcaId != null) {
            pvt.setDoctorId(orcaId);
        } else {
            pvt.setDoctorId(userId);
        }
        pvt.setJmariNumber(jmariCode);
        
        // 来院日
        pvt.setPvtDate(ModelUtils.getDateTimeAsString(new Date()));
        return pvt;
    }
}
