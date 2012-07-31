
package open.dolphin.client;

import java.util.Iterator;
import java.util.List;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.plugin.PluginLoader;

/**
 * KarteContentSender
 * 
 * @author masuda, Masuda Naika
 */
public class KarteContentSender {
    
    public void sendKarte(Chart chart, DocumentModel model) {
        
        PluginLoader<IKarteSender> loader = PluginLoader.load(IKarteSender.class);
        Iterator<IKarteSender> iter = loader.iterator();
        while (iter.hasNext()) {
            IKarteSender sender = iter.next();
            sender.setContext(chart);
            sender.prepare(model);
            sender.send(model);
        }
    }

    public void sendDiagnosis(Chart chart, List<RegisteredDiagnosisModel> rdList) {
        
        PluginLoader<IDiagnosisSender> loader = PluginLoader.load(IDiagnosisSender.class);
        Iterator<IDiagnosisSender> iter = loader.iterator();
        while (iter.hasNext()) {
            IDiagnosisSender sender = iter.next();
            sender.setContext(chart);
            sender.prepare(rdList);
            sender.send(rdList);
        }
    }
}
