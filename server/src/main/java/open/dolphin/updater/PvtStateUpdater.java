
package open.dolphin.updater;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.MsdUpdaterModel;
import open.dolphin.infomodel.PatientVisitModel;

/**
 * 翌日から2.2mを使おうとして待合リストを見て
 * 夜中になんじゃこりゃとならないために
 * 
 * @author masuda, Masuda Naika
 */
public class PvtStateUpdater extends AbstractUpdaterModule {
    
    private static final String VERSION_DATE = "2012-03-10";
    private static final String UPDATE_MEMO = "PvtState updated";
    private static final String NO_UPDATE_MEMO = "PvtState not updated";

    @Override
    public String getVersionDateStr() {
        return VERSION_DATE;
    }

    @Override
    public String getModuleName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public MsdUpdaterModel start() {
        
        SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        String pvtDate = frmt.format(new Date());
        
        String sql = "from PatientVisitModel p where p.pvtDate like :pvtDate";
        List<PatientVisitModel> pvtList = 
                em.createQuery(sql)
                .setParameter("pvtDate", pvtDate + "%")
                .getResultList();
        
        for (PatientVisitModel pvt : pvtList) {
            int oldState = pvt.getState();
            if (oldState == 1) {    // OLD: saved state
                pvt.setState(2);    // NEW: 1 << ChartImpl.BIT_SAVE_CLAIM
                em.merge(pvt);
            }
        }
        
        return null;
    }
}
