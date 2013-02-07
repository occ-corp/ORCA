
package open.dolphin.updater;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.infomodel.MsdUpdaterModel;

/**
 * AbstractUpdaterModule
 * 
 * @author masuda, Masuda Naika
 */
public abstract class AbstractUpdaterModule {
    
    protected EntityManager em;
    
    protected static final Logger logger = Logger.getLogger("open.dolphin.updater");
    
    public abstract MsdUpdaterModel start();

    public abstract String getVersionDateStr();

    public abstract String getModuleName();

    public Date getVersionDate() {
        return ModelUtils.getDateAsObject(getVersionDateStr());
    }
    
    protected void setEntityManager(EntityManager em) {
        this.em = em;
    }

    protected MsdUpdaterModel getResult(String memo) {
        
        SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        
        String moduleName = getModuleName();
        Date versionDate = getVersionDate();
        Date updateDate = new Date();
        
        MsdUpdaterModel ret = new MsdUpdaterModel();
        ret.setModuleName(moduleName);
        ret.setVersionDate(versionDate);
        ret.setMemo(memo);
        ret.setUpdateDate(updateDate);
        
        StringBuilder sb = new StringBuilder();
        sb.append(moduleName).append(", ");
        sb.append(frmt.format(versionDate)).append(", ");
        sb.append(memo);
        logger.info(sb.toString());
        return ret;
    }
}
