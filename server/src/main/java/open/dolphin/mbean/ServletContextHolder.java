package open.dolphin.mbean;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;
import javax.servlet.AsyncContext;

/**
 * ContextHolder
 * @author masuda, Masuda Naika
 */
@Singleton
public class ServletContextHolder {

    // 今日と明日
    private GregorianCalendar today;
    private GregorianCalendar tomorrow;

    private final List<AsyncContext> acList = new ArrayList<AsyncContext>();
    
    // facilityIdとfaciltyContextのマップ
    private Map<String, FacilityContext> facilityContextMap 
            = new ConcurrentHashMap<String, FacilityContext>();
    

    public List<AsyncContext> getAsyncContextList() {
        return acList;
    }

    public void addAsyncContext(AsyncContext ac) {
        synchronized (acList) {
            acList.add(ac);
        }
    }

    public void removeAsyncContext(AsyncContext ac) {
        synchronized (acList) {
            acList.remove(ac);
        }
    }

    public Map<String, FacilityContext> getFacilityContextMap() {
        return facilityContextMap;
    }    
    
    
    public FacilityContext getFacilityContext(String fid) {
        FacilityContext context = facilityContextMap.get(fid);
        if (context == null) {
            context = new FacilityContext();
            facilityContextMap.put(fid, context);
        }
        return context;
    }

    // 今日と明日を設定する
    public void setToday() {
        today= new GregorianCalendar();
        int year = today.get(GregorianCalendar.YEAR);
        int month = today.get(GregorianCalendar.MONTH);
        int date = today.get(GregorianCalendar.DAY_OF_MONTH);
        today.clear();
        today.set(year, month, date);

        tomorrow = new GregorianCalendar();
        tomorrow.setTime(today.getTime());
        tomorrow.add(GregorianCalendar.DAY_OF_MONTH, 1);
    }
    
    public GregorianCalendar getToday() {
        return today;
    }
    public GregorianCalendar getTomorrow() {
        return tomorrow;
    }
}
