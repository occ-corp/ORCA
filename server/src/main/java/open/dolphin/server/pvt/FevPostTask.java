package open.dolphin.server.pvt;

import java.io.*;
import java.util.Map;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.session.MasudaServiceBean;

/**
 * フクダ電子心電図ファイリングFEV-70に患者情報を送る
 * 
 * @author masuda, Masuda Naika
 */
public class FevPostTask implements Runnable {
    
    private static final String jndiName 
            = "java:global/OpenDolphin-server-2.3/" + MasudaServiceBean.class.getSimpleName();

    private PatientVisitModel model;
    private boolean sendToFEV;
    private String sharePath;
    
    // ここはInjectionダメみたい
    private MasudaServiceBean masudaServiceBean;
    
    public FevPostTask( PatientVisitModel pvt) throws NamingException {

        InitialContext ic = new InitialContext();
        masudaServiceBean = (MasudaServiceBean) ic.lookup(jndiName);
        if (pvt == null) {
            sendToFEV = false;
            return;
        }
        this.model = pvt;
        Map<String, String> propMap = masudaServiceBean.getUserPropertyMap(pvt.getFacilityId());
        sharePath = propMap.get("fevSharePath");
        String value = propMap.get("fevOnServer");
        sendToFEV = sharePath != null && !sharePath.isEmpty() && "true".equals(value); 
    }
    
    @Override
    public void run() {

        if (!sendToFEV) {
            return;
        }

        try {
            String fid = model.getFacilityId();
            String ptId = model.getPatientId();
            PatientVisitModel pvt = masudaServiceBean.getLastPvtInThisMonth(fid, ptId);

            // 今月受診がないなら送信
            if (pvt == null) {
                makeFile(model);
                return;
            }

            // 名前・誕生日・性別が変わっていたら送信
            PatientModel oldModel = pvt.getPatientModel();
            String oldBD = oldModel.getBirthday();
            String oldName = oldModel.getFullName();
            String oldGender = oldModel.getGenderDesc();
            String newBD = model.getPatientBirthday();
            String newName = model.getPatientName();
            String newGender = model.getPatientGenderDesc();
            if (!oldBD.equals(newBD) || !oldName.equals(newName) || !oldGender.equals(newGender)) {
                makeFile(model);
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
    }
    
    private void makeFile(PatientVisitModel pvt) throws FileNotFoundException, IOException{

        if (!sharePath.endsWith(File.separator)) {
            sharePath = sharePath + File.separator;
        }

        File folder = new File(sharePath);
        if (!folder.exists()) {
            folder.mkdir();
        }

        String patientId = pvt.getPatientId();
        String patientName = pvt.getPatientName();
        String patientSex = "1";
        if ("女".equals(pvt.getPatientGenderDesc())) {
            patientSex = "2";
        }
        String patientBD = pvt.getPatientBirthday().replace("-", "/");

        StringBuilder sb = new StringBuilder();
        sb.append(patientId);
        sb.append(",");
        sb.append(patientName);
        sb.append(",");
        sb.append(patientSex);
        sb.append(",");
        sb.append(patientBD);
        sb.append(",,,,,,,,\n");

        String fileName = sharePath + "ID_" + patientId;
        File oldFile = new File(fileName + ".cs_");
        if (oldFile.exists()) {
            oldFile.delete();
        }

        FileOutputStream fos = new FileOutputStream(fileName + ".cs_");
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(sb.toString());
        bw.close();
        osw.close();

        oldFile = new File(fileName + ".csv");
        if (oldFile.exists()) {
            oldFile.delete();
        }

        File objFile = new File(fileName + ".cs_");
        objFile.renameTo(new File(fileName + ".csv"));
    }
}
