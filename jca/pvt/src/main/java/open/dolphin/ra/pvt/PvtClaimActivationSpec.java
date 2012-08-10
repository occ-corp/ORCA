package open.dolphin.ra.pvt;

import javax.resource.ResourceException;
import javax.resource.spi.*;
import open.dolphin.ra.api.IPvtClaimListener;

/**
 * PvtClaimActivationSpec
 * @author masuda, Masuda Naika
 */
@Activation(messageListeners = {IPvtClaimListener.class})
public class PvtClaimActivationSpec implements ActivationSpec {
    
    private PvtClaimResourceAdapter ra;
    
    @ConfigProperty(defaultValue = "")
    private String pvt;
    
    public PvtClaimActivationSpec(){
    }
    
    public String getPvt() {
        return pvt;
    }
    public void setPvt(String pvt) {
        this.pvt = pvt;
    }

    @Override
    public void validate() throws InvalidPropertyException {
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        this.ra = (PvtClaimResourceAdapter) ra;
    }
}
