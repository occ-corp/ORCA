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
                    String str = ChartStateDelegater.getInstance().getNextId(currentId);
                    int nextId = Integer.valueOf(str);
                    exec.execute(new OnMessageTask(nextId));
                } catch (Exception e) {
                    //System.out.println(e.toString());
                }
            }
        }
    }
    
    private class OnMessageTask implements Runnable {

        private int nextId;

        private OnMessageTask(int nextId) {
            this.nextId = nextId;
        }

        @Override
        public void run() {
            ChartStateDelegater del = ChartStateDelegater.getInstance();
            List<ChartStateMsgModel> msgList = del.getPvtMessageList(nextId);
            for (IChartStateListener listener : listeners) {
                listener.stateChanged(msgList);
            }
        }
    }
}
