
package open.dolphin.client;

import java.beans.PropertyChangeListener;
import java.util.List;
import org.dcm4che2.data.DicomObject;

/**
 * Pacs接続サービスのインターフェース
 *
 * @author masuda, Masuda Naika
 */
public interface PacsService extends MainService {


    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);

    public void retrieveDicomObject(DicomObject currentDicomObject) throws Exception;

    public List<DicomObject> findStudy(String[] matchingKeys) throws Exception;

}
