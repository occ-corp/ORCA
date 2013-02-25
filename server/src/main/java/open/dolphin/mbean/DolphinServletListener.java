package open.dolphin.mbean;

import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import open.dolphin.server.orca.OrcaService;
import open.dolphin.server.pvt.PvtServletServer;
import open.dolphin.session.MasudaServiceBean;

/**
 *
 * @author masuda, Masuda Naika
 */
@WebListener
public class DolphinServletListener implements ServletContextListener {
    
    private MasudaServiceBean masudaService;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        
        try {
            masudaService = (MasudaServiceBean) JndiUtil.getJndiResource(MasudaServiceBean.class);
        } catch (NamingException ex) {
            System.out.println(ex);
        }
        
        if (masudaService != null && masudaService.usePvtServletServer()) {
            PvtServletServer.getInstance().start();
        }
        OrcaService.getInstance().start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        
        if (masudaService != null && masudaService.usePvtServletServer()) {
            PvtServletServer.getInstance().dispose();
        }
        OrcaService.getInstance().dispose();
    }

}
