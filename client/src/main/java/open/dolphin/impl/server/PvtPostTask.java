package open.dolphin.impl.server;

import java.io.BufferedReader;
import java.io.StringReader;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.delegater.PVTDelegater;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.project.Project;
import open.dolphin.pvtclaim.PVTBuilder;
import open.dolphin.setting.MiscSettingPanel;

/**
 * PvtPostTask
 * @author masuda, Masuda Naika
 */
public class PvtPostTask implements Runnable {
    
    private String pvtXml;
    
    public PvtPostTask(String pvtXml) {
        this.pvtXml = pvtXml;
    }

    @Override
    public void run() {
        
        // pvtXmlからPatientVisitModelを作成する
        BufferedReader br = new BufferedReader(new StringReader(pvtXml));
        PVTBuilder builder = new PVTBuilder();
        builder.parse(br);
        PatientVisitModel pvt = builder.getProduct();

        // pvtがnullなら何もせずリターン
        if (pvt == null) {
            return;
        }
        
        // PVT登録処理
        PVTDelegater pdl = PVTDelegater.getInstance();
        pdl.addPvt(pvt);
        
        // FEV70 export処理
        String sharePath = Project.getString(MiscSettingPanel.FEV_SHAREPATH, MiscSettingPanel.DEFAULT_SHAREPATH);
        boolean sendToFEV = sharePath != null 
                && !sharePath.isEmpty() 
                && Project.getBoolean(MiscSettingPanel.SEND_PATIENT_INFO, MiscSettingPanel.DEFAULT_SENDPATIENTINFO);

        if (sendToFEV) {
            PatientVisitModel oldPvt = MasudaDelegater.getInstance().getLastPvtInThisMonth(pvt);
            FEV70Exporter fev = new FEV70Exporter(pvt, oldPvt, sharePath);
            fev.export();
        }
    }
}
