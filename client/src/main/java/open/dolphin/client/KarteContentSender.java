package open.dolphin.client;

import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.plugin.PluginLoader;

/**
 * KarteContentSender
 * 
 * @author masuda, Masuda Naika
 */
public class KarteContentSender implements PropertyChangeListener {
    
    private static final KarteContentSender instance;
    
    static {
        instance = new KarteContentSender();
    }
    
    private KarteContentSender() {
    }
    
    public static KarteContentSender getInstance() {
        return instance;
    }

    public void sendKarte(final Chart chart, final DocumentModel model) {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                PluginLoader<IKarteSender> loader = PluginLoader.load(IKarteSender.class);
                Iterator<IKarteSender> iter = loader.iterator();

                while (iter.hasNext()) {
                    IKarteSender sender = iter.next();
                    sender.setContext(chart);
                    sender.setModel(model);
                    sender.addListener(instance);
                    sender.send();
                }
                return null;
            }
        };
        
        worker.execute();
    }

    public void sendDiagnosis(final Chart chart, final List<RegisteredDiagnosisModel> rdList) {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                PluginLoader<IDiagnosisSender> loader = PluginLoader.load(IDiagnosisSender.class);
                Iterator<IDiagnosisSender> iter = loader.iterator();

                while (iter.hasNext()) {
                    IDiagnosisSender sender = iter.next();
                    sender.setContext(chart);
                    sender.setModel(rdList);
                    sender.addListener(instance);
                    sender.send();
                }
                return null;
            }
        };
        
        worker.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        
        String prop = evt.getPropertyName();
        KarteSenderResult result = (KarteSenderResult) evt.getNewValue();
        if (!result.isError()) {
            result.removeListener();
            return;
        }
        
        final String[] options = {"再送信", "取消"};
        if (KarteSenderResult.PROP_KARTE_SENDER_RESULT.equals(prop)) {
            Toolkit.getDefaultToolkit().beep();
            IKarteSender sender = result.getKarteSender();
            Chart context = sender.getContext();
            JFrame frame = (context == null) ? null : context.getFrame();
            StringBuilder sb = new StringBuilder();
            sb.append("診療行為送信でエラーが発生しました。").append("\n");
            sb.append(result.getCodeAndMsg()).append("\n");
            String msg = sb.toString();
            
            int val = JOptionPane.showOptionDialog(frame, msg, "カルテ送信",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            
            if (val == 0) {
                // 再送信
                sender.send();
            } else {
                // 取消
                result.removeListener();
            }

        } else if (KarteSenderResult.PROP_DIAG_SENDER_RESULT.equals(prop)) {
            Toolkit.getDefaultToolkit().beep();
            IDiagnosisSender sender = result.getDiagnosisSender();
            Chart context = sender.getContext();
            JFrame frame = (context == null) ? null : context.getFrame();
            StringBuilder sb = new StringBuilder();
            sb.append("病名送信でエラーが発生しました。").append("\n");
            sb.append(result.getCodeAndMsg()).append("\n");
            String msg = sb.toString();
            
            int val = JOptionPane.showOptionDialog(frame, msg, "病名送信",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            
            if (val == 1) {
                // 再送信
                sender.send();
            } else {
                // 取消
                result.removeListener();
            }
        }
    }
}
