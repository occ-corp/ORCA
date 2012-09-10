package open.dolphin.impl.pacsviewer;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * listTableに保存するobject
 *
 * @author masuda, Masuda Naika
 */
public class ListDicomObject implements Comparable {

    private DicomObject object;
    private String ptId;
    private String ptName;
    private String ptSex;
    private String ptBirthDate;
    private String modalities;
    private String description;
    private String studyDate;
    private String numberOfImage;

    public ListDicomObject(DicomObject obj) {
        object = obj;
        ptId = object.getString(Tag.PatientID);
        ptName = object.getString(Tag.PatientName).replace("^", " ");
        ptSex = object.getString(Tag.PatientSex);
        ptBirthDate = object.getString(Tag.PatientBirthDate);
        modalities = object.getString(Tag.ModalitiesInStudy);
        description = object.getString(Tag.StudyDescription);
        studyDate = object.getString(Tag.StudyDate);
        numberOfImage = object.getString(Tag.NumberOfStudyRelatedInstances);
    }

    public DicomObject getDicomObject() {
        return object;
    }

    public String getPtId() {
        return ptId;
    }

    public String getPtName() {
        return ptName;
    }

    public String getPtSex() {
        return ptSex;
    }

    public String getPtBirthDate() {
        return ptBirthDate;
    }

    public String getModalities() {
        return modalities;
    }

    public String getDescription() {
        return description;
    }

    public String getStudyDate() {
        return studyDate;
    }

    public String getNumberOfImage() {
        return numberOfImage;
    }

    @Override
    public int compareTo(Object o) {
        int sDate = Integer.parseInt(studyDate);
        ListDicomObject test = (ListDicomObject) o;
        int tDate = Integer.parseInt(test.getStudyDate());
        if (sDate == tDate) {
            return 0;
        } else if (sDate > tDate) {
            return 1;
        } else {
            return -1;
        }
    }
}
