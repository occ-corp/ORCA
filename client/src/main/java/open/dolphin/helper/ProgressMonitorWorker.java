package open.dolphin.helper;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * ProgressMonitor付のSimpleWorker
 * 
 * @author masuda, Masuda Naika
 */
public abstract class ProgressMonitorWorker<T, Void> extends SwingWorker<T, Void> {

    private static final String STATE = "state";
    private static final String PROGRESS = "progress";
    private Timer taskTimer;
    private ProgressMonitor monitor;
    private int delayCount;
    private static final int DEFAULT_ESTIMATION = 120*1000;  // 120 秒
    private static final int DEFAULT_DELAY = 300;       // 300 mmsec
    private boolean timeout;
    private PropertyChangeListener pcl;

    public ProgressMonitorWorker(Component c, String message, String note) {
        this(c, message, note, DEFAULT_ESTIMATION, DEFAULT_DELAY);
    }

    public ProgressMonitorWorker(Component c, String message, String note, int maxEstimation, int delay) {

        pcl = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                if (STATE.equals(pce.getPropertyName())) {
                    if (SwingWorker.StateValue.STARTED == pce.getNewValue()) {
                        startProgress();
                    }
                    else if(SwingWorker.StateValue.DONE == pce.getNewValue()) {
                        stopProgress();
                    }
                } else if (PROGRESS.equals(pce.getPropertyName())) {
                    progress((Integer) pce.getNewValue());
                }
            }
        };
        
        addPropertyChangeListener(pcl);
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);
        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                ProgressMonitorWorker worker = ProgressMonitorWorker.this;
                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);
                } else if (delayCount >= monitor.getMaximum() && (!worker.isCancelled())) {
                    monitor.close();
                    timeout = true;
                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });
    }

    protected void progress(int value) {
    }

    protected void setMessage(String msg) {
        monitor.setNote(msg);
    }

    protected void startProgress() {
        delayCount = 0;
        taskTimer.start();
    }

    protected void stopProgress() {
        removePropertyChangeListener(pcl);
        taskTimer.stop();
        monitor.close();
        taskTimer = null;
        monitor = null;
    }

    @Override
    protected void done() {

        if (timeout) {
            timeout();
            return;
        } else if (isCancelled()) {
            cancelled();
            return;
        }

        try {
            succeeded(get());
        } catch (InterruptedException ex) {
            interrupted(ex);
        } catch (ExecutionException ex) {
            failed(ex);
        } catch (Exception ex) {
            failed(ex);
        }
    }
    protected void succeeded(T result) {
    }

    protected void cancelled() {
    }

    protected void failed(Throwable cause) {
    }

    protected void interrupted(Throwable cause) {
    }

    protected void timeout() {
    }
}
