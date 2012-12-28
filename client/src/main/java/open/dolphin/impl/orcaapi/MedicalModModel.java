package open.dolphin.impl.orcaapi;

import java.util.Date;
import java.util.List;
import open.dolphin.client.Chart;
import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.PVTHealthInsuranceModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;

/**
 * ORCA APIで送信する途中終了データモデル
 * 
 * @author masuda, Masuda Naika
 */
public class MedicalModModel {
    
    private Chart context;
    
    private String medicalUid;
    
    private Date performDate;
    
    private String departmentCode;
    
    private String physicianCode;
    
    private boolean admissionFlg;
    
    private PVTHealthInsuranceModel insModel;
    
    private List<ClaimBundle> claimBundleList;
    
    private List<RegisteredDiagnosisModel> diagnosisList;
    
    
    public void setContext(Chart context) {
        this.context = context;
    }
    
    public Chart getContext() {
        return context;
    }
    
    public void setMedicalUid(String uid) {
        medicalUid = uid;
    }
    
    public String getMedicalUid() {
        return medicalUid;
    }
    
    public void setPerformDate(Date d) {
        performDate = d;
    }
    
    public Date getPerformDate() {
        return performDate;
    }
    
    public void setDepartmentCode(String deptCode) {
        departmentCode = deptCode;
    }
    
    public String getDepartmentCode() {
        return departmentCode;
    }
    
    public void setPhysicianCode(String physicianCode) {
        this.physicianCode = physicianCode;
    }
    
    public String getPhysicianCode() {
        return physicianCode;
    }
    
    public void setAdmissionFlg(boolean flg) {
        admissionFlg = flg;
    }
    
    public boolean getAdmissonFlg() {
        return admissionFlg;
    }
    
    public void setInsuranceModel(PVTHealthInsuranceModel insModel) {
        this.insModel = insModel;
    }

    public PVTHealthInsuranceModel getInsuranceModel() {
        return insModel;
    }
    
    public void setClaimBundleList(List<ClaimBundle> list) {
        claimBundleList = list;
    }
    
    public List<ClaimBundle> getClaimBundleList() {
        return claimBundleList;
    }

    public void setDiagnosisList(List<RegisteredDiagnosisModel> list) {
        diagnosisList = list;
    }
    
    public List<RegisteredDiagnosisModel> getDiagnosisList() {
        return diagnosisList;
    }
}
