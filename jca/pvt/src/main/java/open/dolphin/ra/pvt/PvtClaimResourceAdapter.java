package open.dolphin.ra.pvt;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.transaction.xa.XAResource;
import open.dolphin.ra.api.IPvtClaimListener;

/**
 *　PvtClaimResourceAdapter
 * @author masuda, Masuda Naika
 */
@Connector(
        description = "PVT Claim listener",
        displayName = "PVT Claim Resource Adapter",
        vendorName = "Masuda Naika",
        eisType = "PVTCLAIM",
        version = "2.3")
public class PvtClaimResourceAdapter implements ResourceAdapter, Serializable {
    
    private static final Logger logger = Logger.getLogger(PvtClaimResourceAdapter.class.getSimpleName());
    private static final boolean DEBUG = true;
    
    private ConcurrentHashMap<ActivationSpec, MessageEndpointFactory> factories =
            new ConcurrentHashMap<ActivationSpec, MessageEndpointFactory>();
    
    private BootstrapContext context;

    private Work claimListenWork;

    @Override
    public void start(BootstrapContext context) throws ResourceAdapterInternalException {
        
        this.context = context;
        
        try {
            claimListenWork = new PvtClaimListenWork(this);
            context.getWorkManager().startWork(claimListenWork);
        } catch (WorkException ex) {
            throw new ResourceAdapterInternalException(ex);
        }
    }

    @Override
    public void stop() {
        claimListenWork.release();

    }

    @Override
    public void endpointActivation(MessageEndpointFactory factory, ActivationSpec spec) throws ResourceException {
        if (!(spec instanceof PvtClaimActivationSpec)) {
            throw new NotSupportedException("invalid spec");
        }
        factories.put(spec, factory);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory factory, ActivationSpec spec) {
        factories.remove(spec);
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return null;
    }
    
    public ConcurrentHashMap<ActivationSpec, MessageEndpointFactory> getFactories() {
        return factories;
    }

    public void postPvt(String pvtXml) {
        
        Work work = new PvtBuildWork(pvtXml);
        try {
            context.getWorkManager().startWork(work);
        } catch (WorkException ex) {
            debug(ex.toString());
        }
    }
    
    private class PvtBuildWork implements Work {
        
        private String pvtXml;
        
        private PvtBuildWork(String pvtXml) {
            this.pvtXml = pvtXml;
        }

        @Override
        public void release() {
        }

        @Override
        public void run() {

            for (Iterator itr = getFactories().entrySet().iterator(); itr.hasNext();) {
                Map.Entry entry = (Map.Entry) itr.next();
                PvtClaimActivationSpec spec = (PvtClaimActivationSpec) entry.getKey();
                spec.setPvt("post");
                MessageEndpointFactory factory = (MessageEndpointFactory) entry.getValue();
                try {
                    IPvtClaimListener endpoint = (IPvtClaimListener) factory.createEndpoint(null);
                    endpoint.onPvt(pvtXml);
                } catch (UnavailableException ex) {
                    debug(ex.toString());
                }
            }
        }
        
    }
    
    private void debug(String msg) {
        if (DEBUG) {
            logger.info(msg);
        }
    }
    
    // 手抜き
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PvtClaimResourceAdapter) {
            return super.equals(obj);
        }
        return false;
    }
}
