package open.dolphin.util;

import java.util.List;
import java.util.concurrent.*;

/**
 * MultiTaskExecutor
 *
 * @author masuda, Masuda Naika
 */
public class MultiTaskExecutor<T> {

    private static final int DEFAULT_NUM_THREADS = 3;
    private static final int DEFAULT_SHUTDOWN_WAIT = 1000;
    
    private int numOfThreads = DEFAULT_NUM_THREADS;
    private int shutdownWait = DEFAULT_SHUTDOWN_WAIT;    
    
    private ExecutorService exec;
    private List<Callable<T>> taskList;

    public MultiTaskExecutor() {
    }

    public MultiTaskExecutor(List<Callable<T>> taskList) {
        this.taskList = taskList;
    }

    public void setTaskList(List<Callable<T>> taskList) {
        this.taskList = taskList;
    }
    
    public void setShutdownWait(int waitInMillisec) {
        this.shutdownWait = waitInMillisec;
    }
    
    public void setNumOfThreads(int num) {
        numOfThreads = num;
    }

    public List<Future<T>> execute() throws InterruptedException {

        if (taskList == null || taskList.isEmpty()) {
            return null;
        }
        
        if (exec == null) {
            exec = Executors.newFixedThreadPool(numOfThreads);
        }
        
        List<Future<T>> futures = exec.invokeAll(taskList);
        return futures;
    }
    
    public void executeWaitCompletion() throws InterruptedException, ExecutionException {
        
        List<Future<T>> futures = execute();
        if (futures != null) {
            for (Future<T> future : futures) {
                future.get();
            }
        }
    }

    public void dispose() {
        try {
            exec.shutdown();
            if (!exec.awaitTermination(shutdownWait, TimeUnit.MILLISECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException ex) {
            exec.shutdownNow();
        } catch (NullPointerException ex) {
        }
        exec = null;
        taskList = null;
    }
}
