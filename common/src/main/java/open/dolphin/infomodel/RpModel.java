package open.dolphin.infomodel;

import java.util.Date;

/**
 * RpModel
 * 
 * @author masuda, Masuda Naika
 */
public class RpModel {

    private String drugSrycd;
    private String drugName;
    private String adminSrycd;
    private String rpNumber;
    private String rpDay;
    private Date rpDate;
    
    public RpModel() {
    }

    public RpModel(String drugSrycd, String drugName, String adminSrycd, String rpNumber, String rpDay, Date rpDate) {
        this.drugSrycd = drugSrycd;
        this.drugName = drugName;
        this.adminSrycd = adminSrycd;
        this.rpNumber = rpNumber;
        this.rpDay = rpDay;
        this.rpDate = rpDate;
    }

    public String getDrugSrycd() {
        return drugSrycd;
    }
    
    public String getDrugName() {
        return drugName;
    }

    public String getAdminSrycd() {
        return adminSrycd;
    }

    public String getRpNumber() {
        return (rpNumber == null) ? "" : rpNumber;
    }

    public String getRpDay() {
        return (rpDay == null) ? "1" : rpDay;
    }
    
    public Date getRpDate() {
        return rpDate;
    }

    public void setDrugSrycd(String srycd) {
        drugSrycd = srycd;
    }
    
    public void setDrugName(String name) {
        drugName = name;
    }

    public void setAdminSrycd(String srycd) {
        adminSrycd = srycd;
    }

    public void setRpNumber(String rpNumber) {
        this.rpNumber = rpNumber;
    }

    public void setRpDay(String rpDay) {
        this.rpDay = rpDay;
    }
    
    public void setRpDate(Date date) {
        rpDate = date;
    }

    public boolean isSameWith(RpModel test) {

        if (test.getDrugSrycd() != null && test.getDrugSrycd().equals(drugSrycd)
                && test.getAdminSrycd() != null && test.getAdminSrycd().equals(adminSrycd)
                && test.getRpNumber() != null && test.getRpNumber().equals(rpNumber)) {
            return true;
        }
        return false;
    }
}

