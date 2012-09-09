package open.dolphin.impl.pacsservice;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.List;
import open.dolphin.client.MainWindow;
import open.dolphin.client.PacsService;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;

/**
 * Pacs接続サービス
 *
 * @author masuda, Masuda Naika
 */
public class PacsServiceImpl implements PacsService {

    private MainWindow context;
    private String name;
    private MyDcmQR myDcmQR;

    private String[] returnKeys = 
            new String[]{"PatientName", "PatientSex", "PatientBirthDate", "ModalitiesInStudy", "StudyDescription"};
    // 受け入れるModality
    private String[] storeTCs = 
            new String[]{"CR", "US", "CT", "MR", "SC", "ES"};
    // 受け入れるTransferSyntax
    private static final String[] DEF_TS2 = {
        UID.ExplicitVRLittleEndian, 
        UID.ImplicitVRLittleEndian};
    
    private boolean ivrle = false;

    public static final String PACS_IMAGE_ARRIVED = "pacsServiceImageArrived";
    private PropertyChangeSupport boundSupport;

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(PACS_IMAGE_ARRIVED, listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void start() {

        setupPacsConnection();
        try {
            myDcmQR.start();
        } catch (IOException ex) {
        }
        getLogger().info("PacsService started.");
    }

    @Override
    public void stop() {
        myDcmQR.stop();
        getLogger().info("PacsService stopped.");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public MainWindow getContext() {
        return context;
    }

    @Override
    public void setContext(MainWindow context) {
        this.context = context;
    }

    private Logger getLogger() {
        return Logger.getLogger("pacsService.logger");
    }

    // Pacs通信の初期設定
    private void setupPacsConnection() {

        String pacsRemoteHost = Project.getString(MiscSettingPanel.PACS_REMOTE_HOST, MiscSettingPanel.DEFAULT_PACS_REMOTE_HOST);
        int pacsRemotePort = Project.getInt(MiscSettingPanel.PACS_REMOTE_PORT, MiscSettingPanel.DEFAULT_PACS_REMOTE_PORT);
        String pacsRemoteAE = Project.getString(MiscSettingPanel.PACS_REMOTE_AE, MiscSettingPanel.DEFAULT_PACS_REMOTE_AE);
        String pacsLocalHost = Project.getString(MiscSettingPanel.PACS_LOCAL_HOST, MiscSettingPanel.DEFAULT_PACS_LOCAL_HOST);
        int pacsLocalPort = Project.getInt(MiscSettingPanel.PACS_LOCAL_PORT, MiscSettingPanel.DEFAULT_PACS_LOCAL_PORT);
        String pacsLocalAE =Project.getString(MiscSettingPanel.PACS_LOCAL_AE, MiscSettingPanel.DEFAULT_PACS_LOCAL_AE);

        myDcmQR = new MyDcmQR(pacsLocalAE);
        myDcmQR.setLocalHost(pacsLocalHost);
        myDcmQR.setLocalPort(pacsLocalPort);
        myDcmQR.setCalling(pacsLocalAE);
        myDcmQR.setRemoteHost(pacsRemoteHost);
        myDcmQR.setRemotePort(pacsRemotePort);
        myDcmQR.setCalledAET(pacsRemoteAE, false);
        myDcmQR.setMoveDest(pacsLocalAE);
        myDcmQR.setStoreTCs(storeTCs, DEF_TS2);
        myDcmQR.registerStorageService();
        myDcmQR.setIvrle(ivrle);
        myDcmQR.setPacsService(this);
    }

    // 受信した旨firePropertyChangeする。newにDicomObjectを登録する
    public void firePropertyChange(DicomObject object) {
        getLogger().debug("DicomObject Received:" + object.getString(Tag.SOPInstanceUID));
        boundSupport.firePropertyChange(PACS_IMAGE_ARRIVED, null, object);
    }

    // DicomObjectをretrieveする
    @Override
    public void retrieveDicomObject(DicomObject obj) throws Exception {
        myDcmQR.retrieveDicomObject(obj);
    }

    // matchingKeyでqueryする
    @Override
    public List<DicomObject> findStudy(String[] matchingKeys) throws Exception {
        myDcmQR.setReturnKeys(returnKeys);
        return myDcmQR.queryStudy(matchingKeys);
    }

}
