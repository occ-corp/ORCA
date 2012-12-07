package open.dolphin.impl.orcaapi;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import open.dolphin.infomodel.*;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

/**
 * Orca API 用の Element を JDOM で作成する
 * @author pns
 * @author modified by masuda, Masuda Naika
 */
public class OrcaApiElement {
    
    /**
     * 中途終了データ作成（/api21/medicalmod）
     */
    public static class MedicalMod extends Element {

        public MedicalMod(MedicalModModel model) {
            super("data");
            Element record = new Element("record");
            record.addContent(new medicalreq(model));
            addContent(record);
        }
    }
    
    /**
     * 公費の Element
     */
    public static class PublicInsurance_Information extends Element {
        
        private static final long serialVersionUID = 1L;
        
        public PublicInsurance_Information(PVTPublicInsuranceItemModel[] models) {
            super("array");
            setAttribute("name", getClass().getSimpleName());
            
            for(PVTPublicInsuranceItemModel m : models) {
                Element record = new Element("record");
                //record.addContent(new Element("string").setAttribute("name", "PublicInsurance_Class").addContent(""));
                record.addContent(new Element("string").setAttribute("name", "PublicInsurance_Name").addContent(m.getProviderName()));
                record.addContent(new Element("string").setAttribute("name", "PublicInsurer_Number").addContent(m.getProvider()));
                record.addContent(new Element("string").setAttribute("name", "PublicInsuredPerson_Number").addContent(m.getRecipient()));
                record.addContent(new Element("string").setAttribute("name", "Certificate_IssuedDate").addContent(m.getStartDate()));
                record.addContent(new Element("string").setAttribute("name", "Certificate_ExpiredDate").addContent(m.getExpiredDate()));
                addContent(record);
            }
        }
    }
    
    /**
     * 健康保険の Element
     */
    public static class HealthInsurance_Information extends Element {
        
        private static final long serialVersionUID = 1L;
        
        public HealthInsurance_Information(PVTHealthInsuranceModel model) {
            
            super("record");
            setAttribute("name", getClass().getSimpleName());
            if (model != null) {
                String orcaInsuranceClassCode = convertToOrcaInsuranceClassCode(model.getInsuranceClassCode());
                addContent(new Element("string").setAttribute("name", "InsuranceProvider_Class").addContent(orcaInsuranceClassCode));
                addContent(new Element("string").setAttribute("name", "InsuranceProvider_Number").addContent(model.getInsuranceNumber()));
                addContent(new Element("string").setAttribute("name", "InsuranceProvider_WholeName").addContent(model.getInsuranceClass()));
                addContent(new Element("string").setAttribute("name", "HealthInsuredPerson_Symbol").addContent(model.getClientGroup()));
                addContent(new Element("string").setAttribute("name", "HealthInsuredPerson_Number").addContent(model.getClientNumber()));
                addContent(new Element("string").setAttribute("name", "RelationToInsuredPerson").addContent(Boolean.valueOf(model.getFamilyClass()) ? "1" : "2"));
                addContent(new Element("string").setAttribute("name", "Certificate_IssuedDate").addContent(model.getStartDate()));
                addContent(new Element("string").setAttribute("name", "Certificate_ExpiredDate").addContent(model.getExpiredDate()));
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
            super("array");
            setAttribute("name", getClass().getSimpleName());

            for (ClaimItem i : items) {
                Element record = new Element("record");
                record.addContent(new Element("string").setAttribute("name", "Medication_Code").addContent(i.getCode()));
                record.addContent(new Element("string").setAttribute("name", "Medication_Name").addContent(i.getName()));
                record.addContent(new Element("string").setAttribute("name", "Medication_Number").addContent(i.getNumber()));
                addContent(record);
            }
        }
    }
    
    /**
     * ClaimBundle 部分の Element
     */
    public static class Medical_Information extends Element {

        private static final long serialVersionUID = 1L;

        public Medical_Information(Collection<ClaimBundle> models) {
            
            super("array");
            setAttribute("name", getClass().getSimpleName());
            
            if (models != null && !models.isEmpty()) {
                for (ClaimBundle cb : models) {

                    Element record = new Element("record");
                    record.addContent(new Element("string").setAttribute("name", "Medical_Class").addContent(cb.getClassCode()));
                    record.addContent(new Element("string").setAttribute("name", "Medical_Class_Name").addContent(cb.getClassName()));
                    record.addContent(new Element("string").setAttribute("name", "Medical_Class_Number").addContent(cb.getBundleNumber()));

                    ClaimItem[] claimItems = cb.getClaimItem();
                    if (claimItems != null) {
                        record.addContent(new Medication_info(claimItems));
                    }
                    addContent(record);

                    // admin がある場合は Medication_info にくっつけることになっている
                    if (cb.getAdmin() != null) {
                        Element admin = new Element("record");
                        admin.addContent(new Element("string").setAttribute("name", "Medication_Code").addContent(cb.getAdminCode()));
                        admin.addContent(new Element("string").setAttribute("name", "Medication_Name").addContent(cb.getAdmin()));
                        record.getChild("array").addContent(admin);
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
            
            super("array");
            setAttribute("name", getClass().getSimpleName());

            if (models != null && !models.isEmpty()) {

                for (RegisteredDiagnosisModel m : models) {
                    Element record = new Element("record");
                    record.addContent(new Element("string").setAttribute("name", "Disease_Code").addContent(convertToOrcaByomei(m.getDiagnosisCode())));
                    record.addContent(new Element("string").setAttribute("name", "Disease_Name").addContent(m.getDiagnosis()));

                    String category = m.getCategory();
                    if ("mainDiagnosis".equals(category)) {
                        record.addContent(new Element("string").setAttribute("name", "Disease_Category").addContent("PD"));
                    } else if ("suspectedDiagnosis".equals(category)) {
                        record.addContent(new Element("string").setAttribute("name", "Disease_SuspectedFlag").addContent("S"));
                    }

                    record.addContent(new Element("string").setAttribute("name", "Disease_StartDate").addContent(m.getStartDate()));
                    record.addContent(new Element("string").setAttribute("name", "Disease_EndDate").addContent(m.getEndDate()));

                    // 転帰はdeleteなら'O 削除'、その他は'F 完治'、空白なら設定なし。
                    if ("delete".equals(m.getOutcome())) {
                        record.addContent(new Element("string").setAttribute("name", "Disease_Outcome").addContent("O"));
                    } else if (!"".equals(m.getOutcome())) {
                        record.addContent(new Element("string").setAttribute("name", "Disease_Outcome").addContent("F"));
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

        private static final long serialVersionUID = 1L;

        public Diagnosis_Information(MedicalModModel model) {
            
            super("record");
            
            addCommonContent(model.getDepartmentCode(), model.getPhysicianCode());
            // 保険
            addContent(new HealthInsurance_Information(model.getInsuranceModel()));
            // 診療行為
            addContent(new Medical_Information(model.getClaimBundleList()));
            // 病名
            addContent(new Disease_Information(model.getDiagnosisList()));
        }

        private void addCommonContent(String departmentCode, String physicianCode) {
            setAttribute("name", getClass().getSimpleName());
            addContent(new Element("string").setAttribute("name", "Department_Code").addContent(departmentCode));
            addContent(new Element("string").setAttribute("name", "Physician_Code").addContent(physicianCode));
        }
    }
    
    /**
     * medicalreq Element
     */
    public static class medicalreq extends Element {

        public medicalreq(MedicalModModel model) {
            super("record");
            addCommonContent(model);
            addContent(new Diagnosis_Information(model));
        }

        private void addCommonContent(MedicalModModel model) {

            String patientId = model.getContext().getPatient().getPatientId();
            Date performDate = model.getPerformDate();
            String medicalUid = model.getMedicalUid() != null ? model.getMedicalUid() : "";
            String[] date = ModelUtils.getDateTimeAsString(performDate).split("T");

            setAttribute("name", getClass().getSimpleName());
            addContent(new Element("string").setAttribute("name", "Patient_ID").addContent(patientId));
            addContent(new Element("string").setAttribute("name", "Perform_Date").addContent(date[0]));
            addContent(new Element("string").setAttribute("name", "Perform_Time").addContent(date[1]));
            addContent(new Element("string").setAttribute("name", "Medical_Uid").addContent(medicalUid));
        }
    }
}
