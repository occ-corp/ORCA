package open.dolphin.impl.falco;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import open.dolphin.client.Chart;
import open.dolphin.client.IKarteSender;
import open.dolphin.client.KarteSenderResult;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class FalcoSender implements IKarteSender {

    private static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private Chart context;
    private DocumentModel sendModel;
    private PropertyChangeSupport boundSupport;
    //private String insuranceFacilityId;
    //private String path;
    //private List<BundleDolphin> sendList;
    //private String orderNumber;
    private static final String FALCO = "FALCO";

    private static String createOrderNumber() {
        StringBuilder sb = new StringBuilder();
        sb.append("DL");
        sb.append(SDF.format(new Date()));
        return sb.toString();
    }

    @Override
    public Chart getContext() {
        return context;
    }

    @Override
    public void setContext(Chart context) {
        this.context = context;
    }
    
    @Override
    public void setModel(DocumentModel sendModel) {
        this.sendModel = sendModel;
    }

    @Override
    public void addListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(KarteSenderResult.PROP_KARTE_SENDER_RESULT, listener);
    }
    
    @Override
    public void removeListeners() {
        if (boundSupport != null) {
            for (PropertyChangeListener listener : boundSupport.getPropertyChangeListeners()) {
                boundSupport.removePropertyChangeListener(KarteSenderResult.PROP_KARTE_SENDER_RESULT, listener);
            }
        }
    }

    @Override
    public void fireResult(KarteSenderResult result) {
        if (boundSupport != null) {
            boundSupport.firePropertyChange(KarteSenderResult.PROP_KARTE_SENDER_RESULT, null, result);
        }
    }
    
    @Override
    public void send() {
        
        if (sendModel == null || (!sendModel.getDocInfoModel().isSendLabtest())) {
            fireResult(new KarteSenderResult(FALCO, KarteSenderResult.SKIPPED, null, this));
            return;
        }
        
        // 検体検査オーダーを抽出する
        List<ModuleModel> modules = sendModel.getModules();
        if (modules == null || modules.isEmpty()) {
            fireResult(new KarteSenderResult(FALCO, KarteSenderResult.SKIPPED, null, this));
            return;
        }

        List<BundleDolphin> sendList = new ArrayList<BundleDolphin>();
        for (ModuleModel module : modules) {
            ModuleInfoBean info = module.getModuleInfoBean();
            if (info.getEntity().equals(IInfoModel.ENTITY_LABO_TEST)) {
                BundleDolphin send = (BundleDolphin) module.getModel();
                ClaimItem[] items = send.getClaimItem();
                if (items != null && items.length > 0) {
                    sendList.add(send);
                }
            }
        }

        if (sendList.isEmpty()) {
            fireResult(new KarteSenderResult(FALCO, KarteSenderResult.SKIPPED, null, this));
            return;
        }        

        // オーダー番号を docInfo へ設定する
        String orderNumber;
        if (sendModel.getDocInfoModel().getLabtestOrderNumber() == null) {
            orderNumber = createOrderNumber();
            sendModel.getDocInfoModel().setLabtestOrderNumber(orderNumber);
        } else {
            // 修正の場合は設定されている
            orderNumber = sendModel.getDocInfoModel().getLabtestOrderNumber();
        }
        
        // 保健医療機関コード
        String insuranceFacilityId = Project.getString(Project.SEND_LABTEST_FACILITY_ID);
        if (insuranceFacilityId == null || insuranceFacilityId.length() < 10) {
            //throw new DolphinException("保険医療機関コードが設定されていません。");
            fireResult(new KarteSenderResult(FALCO, 
                    KarteSenderResult.ERROR, "保険医療機関コードが設定されていません。", this));
            return;
        }
        insuranceFacilityId += "00";

        // 検査オーダーの出力先パス
        String path = Project.getString(Project.SEND_LABTEST_PATH);
        if (path == null) {
            //throw new DolphinException("検体検査オーダーの出力先パスが設定されていません。");
            fireResult(new KarteSenderResult(FALCO, 
                    KarteSenderResult.ERROR, "検体検査オーダーの出力先パスが設定されていません。", this));
            return;
        }

        // 送信する
        PatientModel patient = context.getPatient();
        UserModel user = Project.getUserModel();

        HL7Falco falco = new HL7Falco();
        int ret = falco.order(patient, user, sendList, insuranceFacilityId, orderNumber, path);
        
        KarteSenderResult result =  (ret == 0) 
                ? new KarteSenderResult(FALCO, KarteSenderResult.NO_ERROR, null, this)
                : new KarteSenderResult(FALCO, KarteSenderResult.ERROR, "File IO Error.", this);
        fireResult(result);
    }
}
