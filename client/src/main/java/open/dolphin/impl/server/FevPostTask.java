package open.dolphin.impl.server;

import java.io.*;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;

/**
 * フクダ電子心電図ファイリングFEV-70に患者情報を送る
 * 
 * @author masuda, Masuda Naika
 */
public class FevPostTask implements Runnable {
    
    private PatientVisitModel model;
    private boolean sendToFEV;
    private String sharePath;
    
    public FevPostTask(PatientVisitModel pvt) {
        
        if (pvt == null) {
            sendToFEV = false;
            return;
        }
        this.model = pvt;
        sharePath = Project.getString(MiscSettingPanel.FEV_SHAREPATH, MiscSettingPanel.DEFAULT_SHAREPATH);
        sendToFEV = sharePath != null 
                && !sharePath.isEmpty() 
                && Project.getBoolean(MiscSettingPanel.SEND_PATIENT_INFO, MiscSettingPanel.DEFAULT_SENDPATIENTINFO);
    }

    @Override
    public void run() {
        
        if (!sendToFEV) {
            return;
        }

        try {
            MasudaDelegater del = MasudaDelegater.getInstance();
            PatientVisitModel pvt = del.getLastPvtInThisMonth(model);
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
