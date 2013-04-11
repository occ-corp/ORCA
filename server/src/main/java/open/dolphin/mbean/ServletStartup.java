package open.dolphin.mbean;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import open.dolphin.session.ChartEventServiceBean;
import open.dolphin.updater.Updater;
import org.hibernate.Session;

/**
 * スタートアップ時にUpdaterとStateServiceBeanを自動実行
 * @author masuda, Masuda Naika
 */
@Singleton
@Startup
public class ServletStartup {
    
private static final Logger logger = Logger.getLogger(ServletStartup.class.getSimpleName());

    @Inject
    private ChartEventServiceBean eventServiceBean;
    
    @Inject
    private Updater updater;
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @PersistenceContext
    private EntityManager em;

    @PostConstruct
    public void init() {
        updater.start();
        eventServiceBean.start();
        setupDatabaseType();
    }

    @PreDestroy
    public void stop() {
    }

    // 日付が変わったらpvtListをクリアしクライアントに伝える
    @Schedule(hour="0", minute="0", persistent=false)
    public void dayChange() {
        eventServiceBean.renewPvtList();
    }
    @Timeout
    public void timeout(Timer timer) {
        logger.warning("ServletStartup: timeout occurred");
    }
    
    private void setupDatabaseType() {
        Session hibernateSession = em.unwrap(Session.class);
        hibernateSession.doWork(new DetectDatabaseWork());
    }
    
    private class DetectDatabaseWork implements org.hibernate.jdbc.Work {

        @Override
        public void execute(Connection con) throws SQLException {
            DatabaseMetaData dmd = con.getMetaData();
            String database = dmd.getDatabaseProductName();
            contextHolder.setDatabase(database);
            logger.log(Level.INFO, "Database is {0}", database);
        }
    }
}
