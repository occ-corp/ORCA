package open.dolphin.server.pvt;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Logger;
import javax.naming.NamingException;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.mbean.JndiUtil;
import open.dolphin.pvtclaim.PVTBuilder;
import open.dolphin.session.MasudaServiceBean;
import open.dolphin.session.PVTServiceBean;

/**
 * PvtPostTask
 *
 * @author masuda, Masuda Naika
 */
public class PvtPostTask implements Runnable {
    
    private static final Logger logger = Logger.getLogger(PvtPostTask.class.getSimpleName());
    
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
        
        MasudaServiceBean masudaService;
        PVTServiceBean pvtService;
        // ここはInjectできないんだな…
        try {
            masudaService = (MasudaServiceBean) JndiUtil.getJndiResource(MasudaServiceBean.class);
            pvtService = (PVTServiceBean) JndiUtil.getJndiResource(PVTServiceBean.class);
        } catch (NamingException ex) {
            return;
        }
                
        // CLAIM送信されたJMARI番号からfacilityIdを取得する
        String jmariNum = pvt.getJmariNumber();
        String fid = masudaService.getFidFromJmari(jmariNum);
        pvt.setFacilityId(fid);

        // 施設プロパティーを取得する
        Map<String, String> propMap = masudaService.getUserPropertyMap(fid);
        boolean pvtOnServer = Boolean.valueOf(propMap.get("pvtOnServer"));
        boolean fevOnServer = Boolean.valueOf(propMap.get("fevOnServer"));
        String sharePath = propMap.get("fevSharePath");
        boolean sendToFEV = 
                sharePath != null 
                && !sharePath.isEmpty() 
                && fevOnServer;

        // PVT登録処理
        if (pvtOnServer) {
            pvtService.addPvt(pvt);
            StringBuilder sb = new StringBuilder();
            sb.append("PVT post: ").append(pvt.getPvtDate());
            sb.append(", Fid=").append(pvt.getFacilityId());
            sb.append("(").append(jmariNum).append(")");
            sb.append(", PtID=").append(pvt.getPatientId());
            sb.append(", Name=").append(pvt.getPatientName());
            logger.info(sb.toString());
        }
        
        // FEV-70 export処理
        if (sendToFEV) {
            String ptId = pvt.getPatientId();
            PatientVisitModel oldPvt = masudaService.getLastPvtInThisMonth(fid, ptId);
            FEV70Exporter fev = new FEV70Exporter(pvt, oldPvt, sharePath);
            fev.export();
        }
    }
}
