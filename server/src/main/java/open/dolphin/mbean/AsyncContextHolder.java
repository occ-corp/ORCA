package open.dolphin.mbean;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import javax.servlet.AsyncContext;

/**
 * AsyncContextHolder
 * @author masuda, Masuda Naika
 */
@Singleton
public class AsyncContextHolder {
    
    private final List<AsyncContext> acList = new ArrayList<AsyncContext>();
    
    public List<AsyncContext> getAsyncContextList() {
        return acList;
    }
    
    public void addAsyncContext(AsyncContext ac) {
        synchronized(acList) {
            acList.add(ac);
        }
    }
    
    public void removeAsyncContext(AsyncContext ac) {
        synchronized(acList) {
            acList.remove(ac);
        }
    }
}
