package open.dolphin.toucha.model;

import java.util.List;
import open.dolphin.infomodel.PVTHealthInsuranceModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.SimpleAddressModel;

/**
 *
 * @author masuda, Masuda Naika
 */
public class PatientModelS {
    
    private String memo;
    
    private String patientId;
    private String patientName;
    private String patientSex;
    private String patientBirthday;
    private String postalCode;
    private String address;
    private String telephone;
    private String mobilePhone;
    private String insurerType;
    private String insurerNumber;
    private String insuranceCode;
    private String insuranceNumber;
    private String insuranceType;
    private String insuranceFrom;
    private String insuranceTo;
    private String insuranceRatio;
    
    private String patientAge;
    
    public PatientModelS() {
    }
    
    public PatientModelS(PatientModel pm, String memo) {
        setModel(pm, memo);
    }
    
    public void setLiteModel(PatientModel pm) {
        patientId = pm.getPatientId();
        patientName = pm.getFullName();
        patientSex = pm.getGenderDesc();
        patientAge = pm.getAgeBirthday2();
    }
    
    public final void setModel(PatientModel pm, String memo) {
        this.memo = memo;
        patientId = pm.getPatientId();
        patientName = pm.getFullName();
        patientSex = pm.getGenderDesc();
        patientBirthday = pm.getAgeBirthday2();
        
        SimpleAddressModel simpleAddress = pm.getAddress();
        if (simpleAddress != null) {
            postalCode = simpleAddress.getZipCode();
            address = simpleAddress.getAddress();
        }
        telephone = pm.getTelephone();
        mobilePhone = pm.getMobilePhone();
        List<PVTHealthInsuranceModel> insList = pm.getPvtHealthInsurances();
        if (insList != null && !insList.isEmpty()) {
            PVTHealthInsuranceModel ins = pm.getPvtHealthInsurances().get(0);
            insurerType = ins.getInsuranceClass();
            insurerNumber = ins.getInsuranceNumber();
            insuranceCode = ins.getClientGroup();
            insuranceNumber = ins.getClientNumber();
            insuranceType = Boolean.valueOf(ins.getFamilyClass()) ? "本人" : "家族";
            insuranceFrom = ins.getStartDate();
            insuranceTo = ins.getExpiredDate();
            insuranceRatio = ins.getPayOutRatio();
        }
    }
}
