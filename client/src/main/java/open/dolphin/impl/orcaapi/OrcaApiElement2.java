package open.dolphin.impl.orcaapi;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import open.dolphin.infomodel.*;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;

/**
 * Orca API2 用の Element を JDOM で作成する
 * @author pns (original)
 * @author modified by masuda, Masuda Naika
 */
public class OrcaApiElement2 implements IOrcaApi {
    
    /**
     * 中途終了データ作成（/api21/medicalmod）
     */
    public static class MedicalMod extends Element {

        public MedicalMod(MedicalModModel model) {
            super(DATA);
            Element medicalReq = new Medicalreq(model);
            addContent(medicalReq);
        }
    }
    
    /**
     * 公費の Element
     */
    public static class PublicInsurance_Information extends Element {
        
        //private static final long serialVersionUID = 1L;
        
        public PublicInsurance_Information(PVTPublicInsuranceItemModel[] models) {
            super("PublicInsurance_Information");
            setAttribute(TYPE, ARRAY);
            
            for(PVTPublicInsuranceItemModel m : models) {
                Element record = new Element("PublicInsurance_Information_child").setAttribute(TYPE, RECORD);
                //record.addContent(new Element("PublicInsurance_Class").setAttribute(TYPE, STRING).addContent(""));
                record.addContent(new Element("PublicInsurance_Name").setAttribute(TYPE, STRING).addContent(m.getProviderName()));
                record.addContent(new Element("PublicInsurer_Number").setAttribute(TYPE, STRING).addContent(m.getProvider()));
                record.addContent(new Element("PublicInsuredPerson_Number").setAttribute(TYPE, STRING).addContent(m.getRecipient()));
                record.addContent(new Element("Certificate_IssuedDate").setAttribute(TYPE, STRING).addContent(m.getStartDate()));
                record.addContent(new Element("Certificate_ExpiredDate").setAttribute(TYPE, STRING).addContent(m.getExpiredDate()));
                addContent(record);
            }
        }
    }
    
    /**
     * 健康保険の Element
     */
    public static class HealthInsurance_Information extends Element {
        
        //private static final long serialVersionUID = 1L;
        
        public HealthInsurance_Information(PVTHealthInsuranceModel model) {
            
            super("HealthInsurance_Information");
            setAttribute(TYPE, RECORD);
            
            if (model != null) {
                String orcaInsuranceClassCode = convertToOrcaInsuranceClassCode(model.getInsuranceClassCode());
                addContent(new Element("InsuranceProvider_Class").setAttribute(TYPE, STRING).addContent(orcaInsuranceClassCode));
                addContent(new Element("InsuranceProvider_Number").setAttribute(TYPE, STRING).addContent(model.getInsuranceNumber()));
                addContent(new Element("InsuranceProvider_WholeName").setAttribute(TYPE, STRING).addContent(model.getInsuranceClass()));
                addContent(new Element("HealthInsuredPerson_Symbol").setAttribute(TYPE, STRING).addContent(model.getClientGroup()));
                addContent(new Element("HealthInsuredPerson_Number").setAttribute(TYPE, STRING).addContent(model.getClientNumber()));
                addContent(new Element("RelationToInsuredPerson").setAttribute(TYPE, STRING).addContent(Boolean.valueOf(model.getFamilyClass()) ? "1" : "2"));
                addContent(new Element("Certificate_IssuedDate").setAttribute(TYPE, STRING).addContent(model.getStartDate()));
                addContent(new Element("Certificate_ExpiredDate").setAttribute(TYPE, STRING).addContent(model.getExpiredDate()));
                PVTPublicInsuranceItemModel[] publicInsuranceModels = model.getPVTPublicInsuranceItem();
                if (publicInsuranceModels != null) {
                    addContent(new PublicInsurance_Information(publicInsuranceModels));
                }
            }
        }
    }
    
    /**
     * dolphin の値（= claim で受け取る値）　　　　　　　　　　　　　OrcaApi の値
     * Rx：労災・自賠（x：該当の保険番号マスタの保険番号の3桁目）      971（労災）, 973（自賠）
　　　* Zx：自費（xは同上）                                      980
　　　* Ax：治験 90x（xは同上）　　(ver 4.5.0以降)
　　　* Bx：治験 91x（xは同上）　　(ver 4.5.0以降)
　　　* K5：公害                                               975
　　　* 39：後期高齢者　　　　　　　　　　　　　　　　　　　　　　　　　　039
　　　* 40：後期特療費(後期高齢者医療特別療養費)                     040
　　　* 09：協会けんぽ                                          090
　　　* XX：公費単独                                            980
     * 
     * @param claimInsuranceClassCode
     * @return 
     */
    private static String convertToOrcaInsuranceClassCode(String claimInsuranceClassCode) {
        if (claimInsuranceClassCode == null) return "";        
        if (claimInsuranceClassCode.equals("R1")) return "971";
        if (claimInsuranceClassCode.equals("R3")) return "973";
        if (claimInsuranceClassCode.equals("K5")) return "975";
        if (claimInsuranceClassCode.equals("XX")) return "980";
        if (claimInsuranceClassCode.startsWith("Z")) return "980";
        if (claimInsuranceClassCode.matches("[0-9][0-9]")) return "0" + claimInsuranceClassCode;
        return "";
    }

    /**
     * ClaimItem 部分 の Element
     */
    public static class Medication_info extends Element {

        private static final long serialVersionUID = 1L;

        public Medication_info(ClaimItem[] items) {
            super("Medication_info");
            setAttribute(TYPE, ARRAY);

            for (ClaimItem i : items) {
                Element record = new Element("Medication_info_child").setAttribute(TYPE, RECORD);
                record.addContent(new Element("Medication_Code").setAttribute(TYPE, STRING).addContent(i.getCode()));
                record.addContent(new Element("Medication_Name").setAttribute(TYPE, STRING).addContent(i.getName()));
                record.addContent(new Element("Medication_Number").setAttribute(TYPE, STRING).addContent(i.getNumber()));
                addContent(record);
            }
        }
    }
    
    /**
     * ClaimBundle 部分の Element
     */
    public static class Medical_Information extends Element {

        //private static final long serialVersionUID = 1L;

        public Medical_Information(Collection<ClaimBundle> models) {
            
            super("Medical_Information");
            setAttribute(TYPE, ARRAY);
            
            if (models != null && !models.isEmpty()) {
                for (ClaimBundle cb : models) {

                    Element record = new Element("Medical_Information_child").setAttribute(TYPE, RECORD);
                    record.addContent(new Element("Medical_Class").setAttribute(TYPE, STRING).addContent(cb.getClassCode()));
                    record.addContent(new Element("Medical_Class_Name").setAttribute(TYPE, STRING).addContent(cb.getClassName()));
                    record.addContent(new Element("Medical_Class_Number").setAttribute(TYPE, STRING).addContent(cb.getBundleNumber()));

                    ClaimItem[] claimItems = cb.getClaimItem();
                    Medication_info medicationInfo = null;
                    if (claimItems != null) {
                        medicationInfo = new Medication_info(claimItems);
                        record.addContent(medicationInfo);
                    }
                    addContent(record);

                    // admin がある場合は Medication_info にくっつけることになっている
                    if (cb.getAdmin() != null && medicationInfo != null) {
                        Element admin = new Element("Medication_info_child").setAttribute(TYPE, RECORD);
                        admin.addContent(new Element("Medication_Code").setAttribute(TYPE, STRING).addContent(cb.getAdminCode()));
                        admin.addContent(new Element("Medication_Name").setAttribute(TYPE, STRING).addContent(cb.getAdmin()));
                        medicationInfo.addContent(admin);
                    }
                }
            }
        }
    }
    
    /**
     * 病名部分の Element
     */
    public static class Disease_Information extends Element {

        private static final long serialVersionUID = 1L;

        public Disease_Information(List<RegisteredDiagnosisModel> models) {
            
            super("Disease_Information");
            setAttribute(TYPE, ARRAY);

            if (models != null && !models.isEmpty()) {

                for (RegisteredDiagnosisModel m : models) {
                    Element record = new Element("Disease_Information_child").setAttribute(TYPE, RECORD);
                    
                    record.addContent(new Element("Disease_Code").setAttribute(TYPE, STRING).addContent(convertToOrcaByomei(m.getDiagnosisCode())));
                    record.addContent(new Element("Disease_Name").setAttribute(TYPE, STRING).addContent(m.getDiagnosis()));

                    String category = m.getCategory();
                    if ("mainDiagnosis".equals(category)) {
                        record.addContent(new Element("Disease_Category").setAttribute(TYPE, STRING).addContent("PD"));
                    } else if ("suspectedDiagnosis".equals(category)) {
                        record.addContent(new Element("Disease_SuspectedFlag").setAttribute(TYPE, STRING).addContent("S"));
                    }

                    record.addContent(new Element("Disease_StartDate").setAttribute(TYPE, STRING).addContent(m.getStartDate()));
                    record.addContent(new Element("Disease_EndDate").setAttribute(TYPE, STRING).addContent(m.getEndDate()));

                    // 転帰はdeleteなら'O 削除'、その他は'F 完治'、空白なら設定なし。
                    if ("delete".equals(m.getOutcome())) {
                        record.addContent(new Element("Disease_Outcome").setAttribute(TYPE, STRING).addContent("O"));
                    } else if (!"".equals(m.getOutcome())) {
                        record.addContent(new Element("Disease_Outcome").setAttribute(TYPE, STRING).addContent("F"));
                    }
                    addContent(record);
                }
            }
        }
    }
    
    /**
     * RegisteredDiagnosisModel の病名コードを，Orca Api 用に変換する
     *  eg) 1013.7061017 → ZZZ1013,7061017
     * @param claimByomei
     * @return 
     */
    private static String convertToOrcaByomei(String claimByomei) {
        String[] singles = claimByomei.split("\\.");
        StringBuilder b = new StringBuilder();

        for (String s : singles) {
            if (s.length() == 4) {
                b.append("ZZZ");
            }
            b.append(s).append(",");
        }
        return StringUtils.chop(b.toString());
    }

    /**
     * 診療行為の Diagnosis_Information Element
     */
    public static class Diagnosis_Information extends Element {

        //private static final long serialVersionUID = 1L;

        public Diagnosis_Information(MedicalModModel model) {
            
            super("Diagnosis_Information");
            setAttribute(TYPE, RECORD);
            
            addCommonContent(model.getDepartmentCode(), model.getPhysicianCode());
            // 保険
            addContent(new HealthInsurance_Information(model.getInsuranceModel()));
            // 診療行為
            addContent(new Medical_Information(model.getClaimBundleList()));
            // 病名
            addContent(new Disease_Information(model.getDiagnosisList()));
        }

        private void addCommonContent(String departmentCode, String physicianCode) {
            addContent(new Element("Department_Code").setAttribute(TYPE, STRING).addContent(departmentCode));
            addContent(new Element("Physician_Code").setAttribute(TYPE, STRING).addContent(physicianCode));
        }
    }
    
    /**
     * medicalreq Element
     */
    public static class Medicalreq extends Element {

        public Medicalreq(MedicalModModel model) {
            super("medicalreq");
            setAttribute(TYPE, RECORD);
            addCommonContent(model);
            addContent(new Diagnosis_Information(model));
        }

        private void addCommonContent(MedicalModModel model) {

            String patientId = model.getContext().getPatient().getPatientId();
            Date performDate = model.getPerformDate();
            String medicalUid = model.getMedicalUid() != null ? model.getMedicalUid() : "";
            String[] date = ModelUtils.getDateTimeAsString(performDate).split("T");
            String inOut = model.getAdmissonFlg() ? "I" : "O";

            addContent(new Element("Patient_ID").setAttribute(TYPE, STRING).addContent(patientId));
            addContent(new Element("Perform_Date").setAttribute(TYPE, STRING).addContent(date[0]));
            addContent(new Element("Perform_Time").setAttribute(TYPE, STRING).addContent(date[1]));
            addContent(new Element("Medical_Uid").setAttribute(TYPE, STRING).addContent(medicalUid));
            addContent(new Element("InOut").setAttribute(TYPE, STRING).addContent(inOut));
        }
    }
}
