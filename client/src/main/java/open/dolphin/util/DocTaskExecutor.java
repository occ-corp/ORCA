package open.dolphin.util;

import java.util.List;
import java.util.concurrent.*;

/**
 * DocTaskExecutor
 * KarteDocumentViewerとDocumentDelegaterのタスクを一元管理して
 * スレッドを作りすぎないように
 * @author masuda, Masuda Naika
 */
public class DocTaskExecutor<T> {

    private static final int NUM_THREADS = 2;   // ３以上だと苦しそうｗ
    private static final int SHUTDOWN_WAIT = 1000;
    
    private ExecutorService exec;

    private static DocTaskExecutor instance;
    
    static {
        instance = new DocTaskExecutor();
    }
    
    private DocTaskExecutor() {
        exec = Executors.newFixedThreadPool(NUM_THREADS);
    }

    public static DocTaskExecutor getInstance() {
        return instance;
    }

    public List<Future<T>> execute(List<Callable<T>> taskList) throws InterruptedException {

        if (taskList == null || taskList.isEmpty()) {
            return null;
        }

        List<Future<T>> futures = exec.invokeAll(taskList);
        return futures;
    }

    public void dispose() {
        try {
            exec.shutdown();
            if (!exec.awaitTermination(SHUTDOWN_WAIT, TimeUnit.MILLISECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException ex) {
            exec.shutdownNow();
        } catch (NullPointerException ex) {
        }
        exec = null;
    }

    // CompletionService
    public CompletionService createCompletionService() {
        return new ExecutorCompletionService<T>(exec);
    }
}
