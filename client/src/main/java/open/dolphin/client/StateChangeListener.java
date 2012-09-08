package open.dolphin.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import open.dolphin.delegater.StateDelegater;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.infomodel.StateMsgModel;
import open.dolphin.project.Project;

/**
 * カルテオープンなどの状態の変化をまとめて管理する
 * @author masuda, Masuda Naika
 */
public class StateChangeListener {
    
     // このクライアントのUUID
    private String clientUUID;
    private String orcaId;
    private String deptCode;
    private String departmentDesc;
    private String doctorName;
    private String userId;
    private String jmariCode;

    private List<IStateChangeListener> listeners;
    
    // スレッド
    private StateListenTask listenTask;
    private Thread thread;
    
    // 状態変化を各listenerに通知するタスク
    private Executor exec;
    
    private static final StateChangeListener instance;

    static {
        instance = new StateChangeListener();
    }

    private StateChangeListener() {
        init();
     }

    public static StateChangeListener getInstance() {
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
        listeners = new ArrayList<IStateChangeListener>(); 
    }
    
   
    public void addListener(IStateChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IStateChangeListener listener) {
        listeners.remove(listener);
    }
    
    // 状態変更処理の共通入り口
    private void publish(StateMsgModel msg) {
        msg.setIssuerUUID(clientUUID);
        exec.execute(new UpdateStateTask(msg));
    }
    
    public void publishPvtDelete(PatientVisitModel pvt) {
        
        StateMsgModel msg = new StateMsgModel();
        msg.setParamFromPvt(pvt);
        msg.setIssuerUUID(clientUUID);
        msg.setCommand(StateMsgModel.CMD.PVT_DELETE);
        
        publish(msg);
    }
    
    public void publishPvtState(PatientVisitModel pvt) {
        
        StateMsgModel msg = new StateMsgModel();
        msg.setParamFromPvt(pvt);
        msg.setCommand(StateMsgModel.CMD.PVT_STATE);
        
        publish(msg);
    }
    
    public void publishKarteOpened(PatientVisitModel pvt) {
        
        // PatientVisitModel.BIT_OPENを立てる
        pvt.setStateBit(PatientVisitModel.BIT_OPEN, true);
        // ChartStateListenerに通知する
        StateMsgModel msg = new StateMsgModel();
        msg.setParamFromPvt(pvt);
        msg.setCommand(StateMsgModel.CMD.PVT_STATE);
        
        publish(msg);
    }
    
    public void publishKarteClosed(PatientVisitModel pvt) {
        
        // PatientVisitModel.BIT_OPENとownerUUIDをリセットする
        pvt.setStateBit(PatientVisitModel.BIT_OPEN, false);
        pvt.getPatientModel().setOwnerUUID(null);
        
        // ChartStateListenerに通知する
        StateMsgModel msg = new StateMsgModel();
        msg.setParamFromPvt(pvt);
        msg.setCommand(StateMsgModel.CMD.PVT_STATE);
        
        publish(msg);
    }

    public void start() {

        exec = Executors.newSingleThreadExecutor();
        listenTask = new StateListenTask();
        thread = new Thread(listenTask, "ChartState Listen Task");
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
    private class StateListenTask implements Runnable {
        
        private boolean isRunning;
        
        private StateListenTask() {
            isRunning = true;
        }

        private void stop() {
            isRunning = false;
        }
        
        @Override
        public void run() {
            
            while (isRunning) {
                try {
                    StateMsgModel msg = StateDelegater.getInstance().subscribe();
                    if (msg != null) {
                        exec.execute(new OnMessageTask(msg));
                    }
                } catch (Exception e) {
                    //System.out.println(e.toString());
                }
            }
        }
    }
    
    // 自クライアントの状態変更後、サーバーに通知するタスク
    private class UpdateStateTask implements Runnable {
        
        private StateMsgModel msg;
        
        private UpdateStateTask(StateMsgModel msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            // まずは自クライアントを更新
            for (IStateChangeListener listener : listeners) {
                listener.onMessage(msg);
            }
            // サーバーに更新を通知
            StateDelegater del = StateDelegater.getInstance();
            del.putStateMsgModel(msg);
        }
        
    }
    
    // サーバーからの状態変化通知メッセージを処理するタスク
    private class OnMessageTask implements Runnable {
        
        private StateMsgModel msg;
        
        private OnMessageTask(StateMsgModel msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            // 各リスナーで更新処理をする
            for (IStateChangeListener listener : listeners) {
                listener.onMessage(msg);
            }
        }
    }
    
    // FakePatientVisitModelを作る
    public PatientVisitModel createFakePvt(PatientModel pm) {

        // 来院情報を生成する
        PatientVisitModel pvt = new PatientVisitModel();
        pvt.setId(0L);
        pvt.setPatientModel(pm);

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
