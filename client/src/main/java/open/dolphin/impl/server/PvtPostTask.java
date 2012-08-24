package open.dolphin.impl.server;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.Callable;
import open.dolphin.delegater.PVTDelegater;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.pvtclaim.PVTBuilder;

/**
 * PvtPostTask
 * @author masuda, Masuda Naika
 */
public class PvtPostTask implements Callable {
    
    private String pvtXml;
    
    public PvtPostTask(String pvtXml) {
        this.pvtXml = pvtXml;
    }

    @Override
    public PatientVisitModel call() throws Exception {
        BufferedReader r = new BufferedReader(new StringReader(pvtXml));
        PVTBuilder builder = new PVTBuilder();
        builder.parse(r);
        PatientVisitModel pvt = builder.getProduct();

        PVTDelegater pdl = PVTDelegater.getInstance();
        pdl.addPvt(pvt);
        
        return pvt;
    }
}
