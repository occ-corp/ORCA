package open.dolphin.client;

import java.beans.PropertyChangeListener;
import open.dolphin.infomodel.DocumentModel;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public interface IKarteSender {

    public Chart getContext();

    public void setContext(Chart context);
    
    public void setModel(DocumentModel docModel);

    public void send();
    
    public void addListener(PropertyChangeListener listener);
    
    public void removeListeners();
    
    public void fireResult(KarteSenderResult result);
}