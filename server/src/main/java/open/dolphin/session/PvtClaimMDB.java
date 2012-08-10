package open.dolphin.session;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import open.dolphin.ra.api.IPvtClaimListener;

/**
 * PvtClaimMDB
 * @author masuda, Masuda Naika
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "pvt", propertyValue = "post")})
public class PvtClaimMDB implements IPvtClaimListener {

    @Inject
    private PVTServiceBean pvtServiceBean;
    
    @Override
    public void onPvt(String pvtXml) {
        pvtServiceBean.parseAndAddPvt(pvtXml);
    }
    
}
