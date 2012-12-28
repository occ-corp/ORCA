package open.dolphin.impl.orcaapi;

/**
 * ORCA APIで返ってくる医師情報
 * 
 * @author masuda, Masuda Naika
 */
public class PhysicianInfo {
    
    private String code;
    private String wholeName;
    private String wholeNameInKana;
    private String physicianPermissionId;
    private String drugPermissionId;
    private String departmentCode1;
    private String departmentCode2;
    private String departmentCode3;
    private String departmentCode4;
    private String departmentCode5;
    
    public void setCode(String code) {
        this.code = code;
    }
    public void setWholeName(String name) {
        wholeName = name;
    }
    public void setWholeNameInKana(String kanaName) {
        wholeNameInKana = kanaName;
    }
    public void setPhysicianPermissionId(String id) {
        physicianPermissionId = id;
    }
    public void setDrugPermissionId(String id) {
        drugPermissionId = id;
    }
    public void setDepartmentCode1(String code) {
        departmentCode1 = code;
    }
    public void setDepartmentCode2(String code) {
        departmentCode2 = code;
    }
    public void setDepartmentCode3(String code) {
        departmentCode3 = code;
    }
    public void setDepartmentCode4(String code) {
        departmentCode4 = code;
    }
    public void setDepartmentCode5(String code) {
        departmentCode5 = code;
    }
    
    public String getCode() {
        return code;
    }
    public String getWholeName() {
        return wholeName;
    }
    public String getWholeNameInKana() {
        return wholeNameInKana;
    }
    public String getPhysicianPermissionId() {
        return physicianPermissionId;
    }
    public String getDrugPermissionId() {
        return drugPermissionId;
    }
    public String getDepartmentCode1() {
        return departmentCode1;
    }
    public String getDepartmentCode2() {
        return departmentCode2;
    }
    public String getDepartmentCode3() {
        return departmentCode3;
    }
    public String getDepartmentCode4() {
        return departmentCode4;
    }
    public String getDepartmentCode5() {
        return departmentCode5;
    }
}
