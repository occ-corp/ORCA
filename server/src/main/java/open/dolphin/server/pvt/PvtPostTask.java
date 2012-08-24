package open.dolphin.server.pvt;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.Callable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.pvtclaim.PVTBuilder;
import open.dolphin.session.PVTServiceBean;

/**
 * PvtPostTask
 * @author masuda, Masuda Naika
 */
public class PvtPostTask implements Callable {
    
    private static final String jndiName 
            = "java:global/OpenDolphin-server-2.3/" + PVTServiceBean.class.getSimpleName();
    
    private String pvtXml;

    // ここはInjectionダメみたい
    private PVTServiceBean pvtServiceBean;
    
    public PvtPostTask(String pvtXml) throws NamingException {
        InitialContext ic = new InitialContext();
        pvtServiceBean = (PVTServiceBean) ic.lookup(jndiName);
        this.pvtXml = pvtXml;
    }

    @Override
    public PatientVisitModel call() throws Exception {
        BufferedReader br = new BufferedReader(new StringReader(pvtXml));
        PVTBuilder builder = new PVTBuilder();
        builder.parse(br);
        PatientVisitModel pvt = builder.getProduct();
        pvtServiceBean.addPvt(pvt);
        return pvt;
    }
    
 }
