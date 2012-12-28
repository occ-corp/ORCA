package open.dolphin.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
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
        
        List<KarteSenderResult> errors = new ArrayList<KarteSenderResult>();
        
        PluginLoader<IKarteSender> loader = PluginLoader.load(IKarteSender.class);
        Iterator<IKarteSender> iter = loader.iterator();
        while (iter.hasNext()) {
            IKarteSender sender = iter.next();
            sender.setContext(chart);
            //sender.prepare(model);
            KarteSenderResult result = sender.send(model);
            if (result.isError()) {
                errors.add(result);
            }
        }
        
        if (!errors.isEmpty()) {
            String title = ClientContext.getFrameTitle("カルテ送信");
            StringBuilder sb = new StringBuilder();
            sb.append("診療行為送信でエラーが発生しました。").append("\n");
            for (KarteSenderResult ksr : errors) {
                sb.append(ksr.getCodeAndMsg()).append("\n");
            }
            String message = sb.toString();
            JOptionPane.showMessageDialog(chart.getFrame(), message, title, JOptionPane.ERROR_MESSAGE);
        }
    }

    public void sendDiagnosis(Chart chart, List<RegisteredDiagnosisModel> rdList) {
        
        List<KarteSenderResult> errors = new ArrayList<KarteSenderResult>();
        
        PluginLoader<IDiagnosisSender> loader = PluginLoader.load(IDiagnosisSender.class);
        Iterator<IDiagnosisSender> iter = loader.iterator();
        while (iter.hasNext()) {
            IDiagnosisSender sender = iter.next();
            sender.setContext(chart);
            //sender.prepare(rdList);
            KarteSenderResult result =  sender.send(rdList);
            if (result.isError()) {
                errors.add(result);
            }
        }
        
        if (!errors.isEmpty()) {
            String title = ClientContext.getFrameTitle("病名送信");
            StringBuilder sb = new StringBuilder();
            sb.append("病名送信でエラーが発生しました。");
            for (KarteSenderResult ksr : errors) {
                sb.append(ksr.getCodeAndMsg()).append("\n");
            }
            String message = sb.toString();
            JOptionPane.showMessageDialog(chart.getFrame(), message, title, JOptionPane.ERROR_MESSAGE);
        }
    }
}
