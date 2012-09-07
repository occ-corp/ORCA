package open.dolphin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import open.dolphin.delegater.StateDelegater;
import open.dolphin.infomodel.StateMsgModel;

/**
 * カルテオープンなどの状態の変化をまとめて管理する
 * @author masuda, Masuda Naika
 */
public class StateChangeMediator {
    
    // このクライアントのUUID
    private String clientUUID;

    // このクライアントが更新済みのStateMsgModelの番号
    private int currentId;
    
    private List<AbstractStateListener> listeners;
    
    // スレッド
    private ChartStateListenTask listenTask;
    private Thread thread;
    
    // 状態変化を各listenerに通知するタスク
    private Executor exec;
    
    private static final StateChangeMediator instance;

    static {
        instance = new StateChangeMediator();
    }

    private StateChangeMediator() {
        clientUUID = Dolphin.getInstance().getClientUUID();
    }

    public static StateChangeMediator getInstance() {
        return instance;
    }

    
    public void addListener(AbstractStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AbstractStateListener listener) {
        listeners.remove(listener);
    }
    
    // 状態変更処理の共通入り口
    public void postStateMsg(StateMsgModel msg) {
        msg.setIssuerUUID(clientUUID);
        exec.execute(new UpdateStateTask(msg));
    }
    
    
    public void start() {
        listeners = new ArrayList<AbstractStateListener>();
        exec = Executors.newSingleThreadExecutor();
        listenTask = new ChartStateListenTask();
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
    private class ChartStateListenTask implements Runnable {

        private boolean isRunning;

        private void stop() {
            isRunning = false;
        }
        
        @Override
        public void run() {
            
            isRunning = true;
            
            while (isRunning) {
                try {
                    String str = StateDelegater.getInstance().getCurrentId(currentId);
                    currentId = Integer.valueOf(str);
                    exec.execute(new OnMessageTask(currentId));
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
            for (AbstractStateListener listener : listeners) {
                listener.processStateChange(msg);
            }
            // サーバーに更新を通知
            StateDelegater del = StateDelegater.getInstance();
            del.putStateMsgModel(msg);
        }
        
    }
    
    // サーバーからの状態変化通知メッセージを処理するタスク
    private class OnMessageTask implements Runnable {
        
        private int id;
        
        private OnMessageTask(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            // 状態変化モデルを取得しに行く
            StateDelegater del = StateDelegater.getInstance();
            List<StateMsgModel> msgList = del.getChartStateMsgList(id);
            
            // 各リスナーで更新処理をする
            for (AbstractStateListener listener : listeners) {
                listener.stateChanged(msgList);
            }
        }
    }
}
