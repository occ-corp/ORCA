package open.dolphin.server.pvt;

import java.io.*;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;

/**
 * フクダ電子心電図ファイリングFEV-70に患者情報を送る
 *
 * @author masuda, Masuda Naika
 */
public class FEV70Exporter {

    private PatientVisitModel pvt;
    private PatientVisitModel oldPvt;
    private String sharePath;

    public FEV70Exporter(PatientVisitModel pvt, PatientVisitModel oldPvt, String sharePath) {
        this.pvt = pvt;
        this.oldPvt = oldPvt;
        this.sharePath = sharePath;
    }

    public void export() {

        try {
            // 今月受診がないなら送信
            if (oldPvt == null) {
                makeFile(pvt);
                return;
            }

            // 名前・誕生日・性別が変わっていたら送信
            PatientModel oldModel = oldPvt.getPatientModel();
            String oldBD = oldModel.getBirthday();
            String oldName = oldModel.getFullName();
            String oldGender = oldModel.getGenderDesc();
            String newBD = pvt.getPatientBirthday();
            String newName = pvt.getPatientName();
            String newGender = pvt.getPatientGenderDesc();
            if (!oldBD.equals(newBD) || !oldName.equals(newName) || !oldGender.equals(newGender)) {
                makeFile(pvt);
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
    }

    private void makeFile(PatientVisitModel model) throws FileNotFoundException, IOException {

        if (!sharePath.endsWith(File.separator)) {
            sharePath = sharePath + File.separator;
        }

        File folder = new File(sharePath);
        if (!folder.exists()) {
            folder.mkdir();
        }

        String patientId = model.getPatientId();
        String patientName = model.getPatientName();
        String patientSex = "1";
        if ("女".equals(model.getPatientGenderDesc())) {
            patientSex = "2";
        }
        String patientBD = model.getPatientBirthday().replace("-", "/");

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
