package open.dolphin.impl.pacsservice;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import open.dolphin.client.MainWindow;
import open.dolphin.client.PacsService;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.PDVInputStream;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.DicomService;
import org.dcm4che2.net.service.StorageService;

/**
 * Pacs接続サービス
 *
 * @author masuda, Masuda Naika
 */
public class PacsServiceImpl implements PacsService {

    private MainWindow context;
    private String name;
    private MyDcmQR myDcmQR;

    private String[] returnKeys = new String[]{"PatientName", "PatientSex", "PatientBirthDate", "ModalitiesInStudy", "StudyDescription"};
    // 受け入れるModality
    private String[] storeTCs = new String[]{"CR", "US", "CT", "MR", "SC", "ES"};
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
        myDcmQR.setStoreTCs(storeTCs);
        myDcmQR.registerStorageService();
        myDcmQR.setIvrle(ivrle);
        myDcmQR.setPacsService(this);
    }

    // 受信した旨firePropertyChangeする。newにDicomObjectを登録する
    private void firePropertyChange(DicomObject object) {
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


    private static class MyDcmQR extends DcmQR {

        private PacsServiceImpl pacsService;
        private String[] returnKeys;
        private boolean ivrle;

        private MyDcmQR(String name) {
            super(name);
        }

        private void setPacsService(PacsServiceImpl service) {
            this.pacsService = service;
        }

        private void setReturnKeys(String[] returnKeys) {
            this.returnKeys = returnKeys;
        }

        private void setIvrle(boolean b) {
            ivrle = b;
        }

        // 患者ＩＤでstudyを検索する
        private List<DicomObject> queryStudy(String[] matchingKeys) throws Exception {

            setCFind(true);
            getKeys().clear();
            setQueryLevelToStudy();
            // 検索条件の設定
            for (int i = 1; i < matchingKeys.length; i++, i++) {
                addMatchingKey(Tag.toTagPath(matchingKeys[i - 1]), matchingKeys[i]);
            }
            // return keyの設定
            for (int i = 0; i < returnKeys.length; i++) {
                addReturnKey(Tag.toTagPath(returnKeys[i]));
            }
            configureFindTransferCapability(ivrle);
            List<DicomObject> result = connect(null);
            return result;
        }

        private void setQueryLevelToStudy() {
            getKeys().putString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
            for (int tag : STUDY_RETURN_KEYS) {
                getKeys().putNull(tag, null);
            }
        }

        // 画像取得
        private void retrieveDicomObject(DicomObject obj) throws Exception {
            setCFind(false);
            configureMoveTransferCapability(ivrle);
            //configureMoveTransferCapability(obj);
            connect(obj);
        }

        // store capabilityを設定する
        private void setStoreTCs(String[] storeTCs) {
            for (String storeTC : storeTCs) {
                String cuid;
                String[] tsuids;
                cuid = storeTC;
                tsuids = DEF_TS2;
                try {
                    cuid = CUID.valueOf(cuid).uid;
                } catch (IllegalArgumentException e) {
                }
                addStoreTransferCapability(cuid, tsuids);
            }
        }

        // Pacs serverとの通信
        private List<DicomObject> connect(DicomObject obj) throws Exception {

            List<DicomObject> result = null;
            try {
                try {
                    open();
                } catch (Exception e) {
                    throw new Exception("Failed to open server.", e);
                }
                if (isCFind()) {
                    result = query();
                } else {
                    result = Collections.singletonList(obj);
                    move(result);
                    //get(result);
                }
                try {
                    close();
                } catch (InterruptedException e) {
                }
            } catch (IOException e) {
                throw new Exception("IOException.", e);
            } catch (InterruptedException e) {
                throw new Exception("InterruptedException.", e);
            }
            return result;
        }

        // Storage Serviceを設定する
        private void registerStorageService() {
            if (!storeTransferCapability.isEmpty()) {
                ae.register(createStorageService());
            }
        }

        // find時のTransferCapabilityを設定する
        private void configureFindTransferCapability(boolean ivrle) {
            String[] findcuids = qrlevel.getFindClassUids();
            TransferCapability[] tcs = new TransferCapability[findcuids.length];
            int i = 0;
            for (String cuid : findcuids) {
                tcs[i++] = mkFindTC(cuid, ivrle ? IVRLE_TS : NATIVE_LE_TS);
            }
            ae.setTransferCapability(tcs);

        }

        // move時のTransferCapabilityを設定する
        private void configureMoveTransferCapability(boolean ivrle) {
            String[] movecuids = !isCFind() ? qrlevel.getMoveClassUids() : EMPTY_STRING;
            TransferCapability[] tcs = new TransferCapability[movecuids.length + storeTransferCapability.size()];
            int i = 0;
            for (String cuid : movecuids) {
                tcs[i++] = mkRetrieveTC(cuid, ivrle ? IVRLE_TS : NATIVE_LE_TS);
            }
            for (TransferCapability tc : storeTransferCapability) {
                tcs[i++] = tc;
            }
            ae.setTransferCapability(tcs);
        }
/*
        private void configureMoveTransferCapability(DicomObject obj) {
            String[] movecuids = qrlevel.getMoveClassUids();
            TransferCapability[] tcs = new TransferCapability[movecuids.length + 1];
            int i = 0;
            for (String cuid : movecuids) {
                tcs[i++] = mkRetrieveTC(cuid, ivrle ? IVRLE_TS : NATIVE_LE_TS);
            }
            String cuid = CUID.valueOf(obj.getString(Tag.ModalitiesInStudy)).uid;
            tcs[i] = new TransferCapability(cuid, DEF_TS, TransferCapability.SCP);
            ae.setTransferCapability(tcs);
        }
*/
        private DicomService createStorageService() {
            String[] cuids = new String[storeTransferCapability.size()];
            int i = 0;
            for (TransferCapability tc : storeTransferCapability) {
                cuids[i++] = tc.getSopClass();
            }
            return new StorageService(cuids) {

                @Override
                protected void onCStoreRQ(Association as, int pcid, DicomObject rq,
                        PDVInputStream dataStream, String tsuid, DicomObject rsp)
                        throws IOException, DicomServiceException {
                    String cuid = rq.getString(Tag.AffectedSOPClassUID);
                    String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
                    DicomObject object = dataStream.readDataset();
                    object.initFileMetaInformation(cuid, iuid, tsuid);
                    pacsService.firePropertyChange(object);
                }
            };
        }
    }
}

