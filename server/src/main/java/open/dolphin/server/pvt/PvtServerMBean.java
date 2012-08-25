package open.dolphin.server.pvt;

import javax.annotation.ManagedBean;
import open.dolphin.session.MasudaServiceBean;
import open.dolphin.session.PVTServiceBean;

/**
 * PvtServerMBean
 * 
 * @author masuda, Masuda Naika
 */
@ManagedBean
public interface PvtServerMBean {
    
    public PVTServiceBean getPvtServiceBean();
    
    public MasudaServiceBean getMasudaServiceBean();
}
