package open.dolphin.mbean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Timeout;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.AsyncContext;
import open.dolphin.session.ChartStateServiceBean;

/**
 * ContextHolder
 * @author masuda, Masuda Naika
 */
@Singleton
public class ServletContextHolder {

    // 今日と明日
    private GregorianCalendar today;
    private GregorianCalendar tomorrow;

    private static final Logger logger = Logger.getLogger(ServletContextHolder.class.getSimpleName());
    private final List<AsyncContext> acList = new ArrayList<AsyncContext>();
    
    // facilityIdとfaciltyContextのマップ
    private Map<String, FacilityContext> facilityContextMap 
            = new ConcurrentHashMap<String, FacilityContext>();
    
    @Inject
    private ChartStateServiceBean chartStateService;
   
   
    @PostConstruct
    public void init() {
        chartStateService.initializePvtList();
    }

    @PreDestroy
    public void stop() {
    }

    // 日付が変わったらpvtListをクリアしクライアントに伝える
    @Schedule(hour="0", minute="0", persistent=false)
    public void dayChange() {
        setToday();
        chartStateService.renewPvtList();
    }
    @Timeout
    public void timeout(Timer timer) {
        logger.warning("ServletContextHolder: timeout occurred");
    }

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
