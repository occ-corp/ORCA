package open.dolphin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import open.dolphin.delegater.ChartStateDelegater;
import open.dolphin.infomodel.ChartStateMsgModel;

/**
 * Chart状態の変化をまとめて管理する
 * @author masuda, Masuda Naika
 */
public class ChartStateListener {
    
    // このクライアントのUUID
    private String clientUUID;

    private int currentId;
    
    private List<IChartStateListener> listeners;
    
    // スレッド
    private ChartStateListenTask listenTask;
    private Thread thread;
    
    // ChartStateMsgListを取得して各listenerに通知するタスク
    private Executor exec;
    
    private static final ChartStateListener instance;

    static {
        instance = new ChartStateListener();
    }

    private ChartStateListener() {
        clientUUID = Dolphin.getInstance().getClientUUID();
    }

    public static ChartStateListener getInstance() {
        return instance;
    }

    
    public void addListener(IChartStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IChartStateListener listener) {
        listeners.remove(listener);
    }
    
    // 状態変更処理の共通入り口
    public void updateChartState(ChartStateMsgModel msg) {
        msg.setIssuerUUID(clientUUID);
        exec.execute(new UpdateStateTask(msg));
    }
    
    
    public void start() {
        listeners = new ArrayList<IChartStateListener>();
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
                    String str = ChartStateDelegater.getInstance().getCurrentId(currentId);
                    currentId = Integer.valueOf(str);
                    exec.execute(new OnMessageTask());
                } catch (Exception e) {
                    //System.out.println(e.toString());
                }
            }
        }
    }
    
    // 自クライアントの状態変更後、サーバーにも通知するタスク
    private class UpdateStateTask implements Runnable {
        
        private ChartStateMsgModel msg;
        
        private UpdateStateTask(ChartStateMsgModel msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            // まずは自クライアントを更新
            for (IChartStateListener listener : listeners) {
                listener.updateLocalState(msg);
            }
            // サーバーに更新を通知
            ChartStateDelegater del = ChartStateDelegater.getInstance();
            del.updateChartState(msg);
        }
        
    }
    
    // サーバーからの状態変化通知メッセージを処理するタスク
    private class OnMessageTask implements Runnable {

        @Override
        public void run() {
            // 状態変化モデルを取得しに行く
            ChartStateDelegater del = ChartStateDelegater.getInstance();
            List<ChartStateMsgModel> msgList = del.getChartStateMsgList(currentId);
            // 各リスナーで更新処理をする
            for (IChartStateListener listener : listeners) {
                listener.stateChanged(msgList);
            }
        }
    }
}
