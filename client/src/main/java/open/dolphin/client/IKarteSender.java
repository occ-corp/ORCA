package open.dolphin.client;

import open.dolphin.infomodel.DocumentModel;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public interface IKarteSender {

    public enum RESULT {NO_ERROR, ERROR, SKIPPED};
    
    public Chart getContext();

    public void setContext(Chart context);

    //public void prepare(DocumentModel data);

    //public void send(DocumentModel data);
    public KarteSenderResult send(DocumentModel docModel);

}