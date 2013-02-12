package open.dolphin.client;

import java.beans.PropertyChangeListener;
import java.util.List;
import open.dolphin.infomodel.RegisteredDiagnosisModel;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public interface IDiagnosisSender {
    
    public Chart getContext();

    public void setContext(Chart context);
    
    public void setModel(List<RegisteredDiagnosisModel> rdModel);

    public void send();

    public void addListener(PropertyChangeListener listener);
    
    public void removeListeners();
    
    public void fireResult(KarteSenderResult result);
}
