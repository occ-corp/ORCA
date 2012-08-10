package open.dolphin.session;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import open.dolphin.ra.api.IPvtClaimListener;

/**
 *
 * @author masuda
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "pvt", propertyValue = "post")})
public class PvtClaimMDB implements IPvtClaimListener {

    @Override
    public void onPvt(String pvtXml) {
        System.out.println(pvtXml);
    }
    
}
