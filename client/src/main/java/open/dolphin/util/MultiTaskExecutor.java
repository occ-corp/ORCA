package open.dolphin.util;

import java.util.List;
import java.util.concurrent.*;

/**
 * MultiTaskExecutor
 * @author masuda, Masuda Naika
 */
public class MultiTaskExecutor<T> {

    private static final int NUM_THREADS = 2;   // ３以上だと苦しそうｗ
    private static final int SHUTDOWN_WAIT = 1000;
    
    private ExecutorService exec;

    public MultiTaskExecutor() {
        NamedThreadFactory factory = new NamedThreadFactory(getClass().getSimpleName());
        exec = Executors.newFixedThreadPool(NUM_THREADS, factory);
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
