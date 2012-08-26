package open.dolphin.server.pvt;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Logger;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.pvtclaim.PVTBuilder;

/**
 * PvtPostTask
 *
 * @author masuda, Masuda Naika
 */
public class PvtPostTask implements Runnable {

    private static final Logger logger = Logger.getLogger(PvtPostTask.class.getSimpleName());
    private String pvtXml;
    private PvtServletServer server;

    public PvtPostTask(PvtServletServer server, String pvtXml) {
        this.server = server;
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

        // CLAIM送信されたJMARI番号からfacilityIdを取得する
        String jmariNum = pvt.getJmariNumber();
        String fid = server.getMasudaServiceBean().getFidFromJmari(jmariNum);
        pvt.setFacilityId(fid);

        // 施設プロパティーを取得する
        Map<String, String> propMap = server.getMasudaServiceBean().getUserPropertyMap(fid);
        boolean pvtOnServer = "true".equals(propMap.get("pvtOnServer"));
        boolean fevOnServer = "true".equals(propMap.get("fevOnServer"));
        String sharePath = propMap.get("fevSharePath");
        boolean sendToFEV = 
                sharePath != null 
                && !sharePath.isEmpty() 
                && fevOnServer;

        // PVT登録処理
        if (pvtOnServer) {
            server.getPvtServiceBean().addPvt(pvt);
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
            PatientVisitModel oldPvt = server.getMasudaServiceBean().getLastPvtInThisMonth(fid, ptId);
            FEV70Exporter fev = new FEV70Exporter(pvt, oldPvt, sharePath);
            fev.export();
        }
    }
}
