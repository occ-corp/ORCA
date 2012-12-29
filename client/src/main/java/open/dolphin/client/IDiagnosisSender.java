package open.dolphin.client;

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

    //public void prepare(List<RegisteredDiagnosisModel> data);

    //public void send(List<RegisteredDiagnosisModel> data);
    public KarteSenderResult send(List<RegisteredDiagnosisModel> rdModel);

}
