package open.dolphin.impl.pacsviewer;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * 画像情報のクラス
 * @author masuda, Masuda Naika
 */
public class DicomImageInfo {

    private String institutionName;
    private String patientID;
    private String patientName;
    private String studyDate;
    private String seriesNumber;
    private String instanceNumber;
    private String patientAgeSex;

    public DicomImageInfo(DicomObject obj) {
        institutionName = nz(obj.getString(Tag.InstitutionName));
        patientID = nz(obj.getString(Tag.PatientID));
        patientAgeSex = nz(obj.getString(Tag.PatientAge)) + " " + nz(obj.getString(Tag.PatientSex));
        patientName = nz(obj.getString(Tag.PatientName)).replace("^", " ");
        studyDate = nz(obj.getString(Tag.StudyDate));
        seriesNumber = nz(obj.getString(Tag.SeriesNumber));
        instanceNumber = nz(obj.getString(Tag.InstanceNumber));
    }

    private String nz(String str) {
        return (str == null) ? "" : str;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public String getPatientID() {
        return patientID;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getStudyDate() {
        return studyDate;
    }

    public String getSeriesNumber() {
        return seriesNumber;
    }

    public String getInstanceNumber() {
        return instanceNumber;
    }

    public String getPatientAgeSex() {
        return patientAgeSex;
    }
}
