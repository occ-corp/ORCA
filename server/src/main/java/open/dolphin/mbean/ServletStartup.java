package open.dolphin.mbean;

import java.util.Timer;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.inject.Inject;
import open.dolphin.session.StateServiceBean;
import open.dolphin.updater.Updater;

/**
 * スタートアップ時にUpdaterとStateServiceBeanを自動実行
 * @author masuda, Masuda Naika
 */
@Singleton
@Startup
public class ServletStartup {
    
private static final Logger logger = Logger.getLogger(ServletStartup.class.getSimpleName());

    @Inject
    private StateServiceBean stateServiceBean;
    
    @Inject
    private Updater updater;

    @PostConstruct
    public void init() {
        updater.start();
        stateServiceBean.initializePvtList();
    }

    @PreDestroy
    public void stop() {
    }

    // 日付が変わったらpvtListをクリアしクライアントに伝える
    @Schedule(hour="0", minute="0", persistent=false)
    public void dayChange() {
        stateServiceBean.renewPvtList();
    }
    @Timeout
    public void timeout(Timer timer) {
        logger.warning("ServletStartup: timeout occurred");
    }
}
