
package open.dolphin.mbean;

import java.util.Timer;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.inject.Inject;
import javax.inject.Singleton;
import open.dolphin.session.ChartStateServiceBean;

/**
 *
 * @author masuda
 */
@Singleton
@Startup
public class ServletStartup {
    
private static final Logger logger = Logger.getLogger(ServletStartup.class.getSimpleName());

    @Inject
    private ChartStateServiceBean chartStateService;
    
    @Inject
    private ServletContextHolder contextHolder;

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
        contextHolder.setToday();
        chartStateService.renewPvtList();
    }
    @Timeout
    public void timeout(Timer timer) {
        logger.warning("ServletContextHolder: timeout occurred");
    }
}
