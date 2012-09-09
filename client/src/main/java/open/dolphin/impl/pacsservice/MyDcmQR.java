package open.dolphin.impl.pacsservice;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.PDVInputStream;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.DicomService;
import org.dcm4che2.net.service.StorageService;

/**
 * MyDcmQR
 * @author masuda, Masuda Naika
 */
public class MyDcmQR extends DcmQR {

    private PacsServiceImpl pacsService;
    private String[] returnKeys;
    private boolean ivrle;

    public MyDcmQR(String name) {
        super(name);
    }

    public void setPacsService(PacsServiceImpl service) {
        this.pacsService = service;
    }

    public void setReturnKeys(String[] returnKeys) {
        this.returnKeys = returnKeys;
    }

    public void setIvrle(boolean b) {
        ivrle = b;
    }

    // 患者ＩＤでstudyを検索する
    public List<DicomObject> queryStudy(String[] matchingKeys) throws Exception {

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
    public void retrieveDicomObject(DicomObject obj) throws Exception {
        setCFind(false);
        configureMoveTransferCapability(ivrle);
        //configureMoveTransferCapability(obj);
        connect(obj);
    }

    // store capabilityを設定する
    public void setStoreTCs(String[] storeTCs, String[] tsuids) {
        for (String storeTC : storeTCs) {
            String cuid = storeTC;
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
    public void registerStorageService() {
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
