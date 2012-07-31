
package open.dolphin.updater;

import java.util.Date;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.MsdUpdaterModel;

/**
 * Updator
 * 
 * @author masuda, Masuda Naika
 */
@Startup
@Singleton
public class Updater {
    
    private static final String SQL = 
            "select count(m) from MsdUpdaterModel m where m.moduleName = :moduleName and m.versionDate = :verDate";
    
    private static final AbstractUpdaterModule[] modules = new AbstractUpdaterModule[]{
        new AddInitialUser(),   // 初期ユーザ―登録
        new DbSchemaUpdater(),  // Database Schemaを変更
        new LetterConverter(),  // Letterを新フォーマットに変換
        new PvtStateUpdater(),  // 今日の診察終了PvtStateを変換
    };

    @PersistenceContext
    private EntityManager em;
    
    @PostConstruct
    public void init() {
        start();
    }
    
    private void start() {

        for (AbstractUpdaterModule module : modules) {
            
            String moduleName = module.getModuleName();
            Date versionDate = module.getVersionDate();
            long count = (Long) em.createQuery(SQL)
                    .setParameter("moduleName", moduleName)
                    .setParameter("verDate", versionDate)
                    .getSingleResult();
            if (count == 0) {
                module.setEntityManager(em);
                MsdUpdaterModel ret = module.start();
                if (ret != null) {
                    em.persist(ret);
                }
            }
        }
    }
    
}
