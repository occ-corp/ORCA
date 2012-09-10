package open.dolphin.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import open.dolphin.delegater.ChartEventDelegater;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.BeanUtils;

/**
 * カルテオープンなどの状態の変化をまとめて管理する
 * @author masuda, Masuda Naika
 */
public class ChartEventListener {
    
     // このクライアントのパラメーター類
    private String clientUUID;
    private String orcaId;
    private String deptCode;
    private String departmentDesc;
    private String doctorName;
    private String userId;
    private String jmariCode;
    private String facilityId;

    private List<IChartEventListener> listeners;
    
    // スレッド
    private EventListenTask listenTask;
    private Thread thread;
    
    // 状態変化を各listenerに通知するタスク
    private Executor exec;
    
    private static final ChartEventListener instance;

    static {
        instance = new ChartEventListener();
    }

    private ChartEventListener() {
        init();
     }

    public static ChartEventListener getInstance() {
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
        listeners = new ArrayList<IChartEventListener>(); 
    }
    
   
    public void addListener(IChartEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IChartEventListener listener) {
        listeners.remove(listener);
    }
    
    // 状態変更処理の共通入り口
    private void publish(ChartEventModel evt) {
        exec.execute(new LocalOnEventTask(evt));
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

        // PatientVisitModel.BIT_OPENを立てる
        pvt.setStateBit(PatientVisitModel.BIT_OPEN, true);
        // ChartStateListenerに通知する
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_STATE);
        
        publish(evt);
    }
    
    public void publishKarteClosed(PatientVisitModel pvt) {
        
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

        exec = Executors.newSingleThreadExecutor();
        listenTask = new EventListenTask();
        thread = new Thread(listenTask, "ChartEvent Listen Task");
        thread.start();
    }

    public void stop() {
        
        listenTask.stop();
        try {
            thread.join(100);
        } catch (InterruptedException ex) {
        }
        thread.interrupt();
        thread = null;
    }

    // Commetでサーバーと同期するスレッド
    private class EventListenTask implements Runnable {
        
        private boolean isRunning;
        
        private EventListenTask() {
            isRunning = true;
        }

        private void stop() {
            isRunning = false;
        }
        
        @Override
        public void run() {
            
            //long t1 = System.currentTimeMillis();
            
            while (isRunning) {
                try {
                    //System.out.println("time = " + String.valueOf(System.currentTimeMillis() - t1));
                    String json = ChartEventDelegater.getInstance().subscribe();
                    if (json != null) {
                        exec.execute(new RemoteOnEventTask(json));
                    }
                    //System.out.println("ChartEvent= " + json);
                    //t1 = System.currentTimeMillis();
                } catch (Exception e) {
                    //System.out.println(e.toString());
                }
            }
        }
    }
    
    // 自クライアントの状態変更後、サーバーに通知するタスク
    private class LocalOnEventTask implements Runnable {
        
        private ChartEventModel evt;
        
        private LocalOnEventTask(ChartEventModel evt) {
            this.evt = evt;
        }

        @Override
        public void run() {
            // まずは自クライアントを更新
            for (IChartEventListener listener : listeners) {
                listener.onEvent(evt);
            }
            // サーバーに更新を通知
            ChartEventDelegater del = ChartEventDelegater.getInstance();
            del.putChartEvent(evt);
        }
        
    }
    
    // 状態変化通知メッセージをデシリアライズし各リスナに処理を分配する
    private class RemoteOnEventTask implements Runnable {
        
        private String json;
        
        private RemoteOnEventTask(String json) {
            this.json = json;
        }

        @Override
        public void run() {
        
            ChartEventModel evt = (ChartEventModel) 
                    JsonConverter.getInstance().fromJson(json, ChartEventModel.class);
            
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
            for (IChartEventListener listener : listeners) {
                listener.onEvent(evt);
            }
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
