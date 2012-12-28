package open.dolphin.impl.orcaapi;

/**
 * ORCA APIで返ってくる診療科情報
 * 
 * @author masuda, Masuda Naika
 */
public class DepartmentInfo {
    
    private String code;
    private String wholeName;
    private String name1;
    private String name2;
    private String name3;
    private String receiptCode;
    
    public void setCode(String code) {
        this.code = code;
    }
    public void setWholeName(String name) {
        wholeName = name;
    }
    public void setName1(String name) {
        name1 = name;
    }
    public void setName2(String name) {
        name2 = name;
    }
    public void setName3(String name) {
        name3 = name;
    }
    public void setReceiptCode(String code) {
        receiptCode = code;
    }
    
    public String getCode() {
        return code;
    }
    public String getWholeName() {
        return wholeName;
    }
    public String getName1() {
        return name1;
    }
    public String getName2() {
        return name2;
    }
    public String getName3() {
        return name3;
    }
    public String getReceiptCode() {
        return receiptCode;
    }
}
