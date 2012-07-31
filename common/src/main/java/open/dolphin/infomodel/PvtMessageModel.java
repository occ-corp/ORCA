
package open.dolphin.infomodel;

/**
 * Pvt状態変更通知用のモデル
 *
 * @author masuda, Masuda Naika
 */
public class PvtMessageModel {
    
    private String issuerUUID;
    private int command;
    
    private long pvtPk;
    private int state;
    private String memo;
    private int byomeiCount;
    private int byomeiCountToday;
    private String ownerUUID;
    private String facilityId;
    
    private PatientVisitModel pvt;
    
    public static final int CMD_STATE  = 1;
    public static final int CMD_ADD    = 2;
    public static final int CMD_DELETE = 3;
    public static final int CMD_RENEW  = 4;
    public static final int CMD_MERGE  = 5;
    
    public PvtMessageModel() {
    }
    
    public PvtMessageModel(PatientVisitModel pvt) {
        
        if (pvt == null) {
            return;
        }
        
        this.pvtPk = pvt.getId();
        this.state = pvt.getState();
        this.byomeiCount = pvt.getByomeiCount();
        this.byomeiCountToday = pvt.getByomeiCountToday();
        this.memo = pvt.getMemo();
        this.ownerUUID = pvt.getOwnerUUID();
        this.facilityId = pvt.getFacilityId();
    }

    public void setCommand(int command) {
        this.command = command;
    }
    public void setPvtPk(long pk) {
        pvtPk = pk;        
    }
    public void setState(int state) {
        this.state = state;
    }
    public void setByomeiCount(int count) {
        byomeiCount = count;
    }
    public void setByomeiCountToday(int count) {
        byomeiCountToday = count;
    }
    public void setIssuerUUID(String issuer) {
        issuerUUID = issuer;
    }
    public void setOwnerUUID(String owner) {
        ownerUUID = owner;
    }
    public void setPatientVisitModel(PatientVisitModel pvt) {
        this.pvt = pvt;
    }
    public void setMemo(String memo) {
        this.memo = memo;
    }
    public void setFacilityId(String fid) {
        facilityId = fid;
    }
    
    public int getCommand() {
        return command;
    }
    public long getPvtPk() {
        return pvtPk;
    }
    public int getState() {
        return state;
    }
    public int getByomeiCount() {
        return byomeiCount;
    }
    public int getByomeiCountToday() {
        return byomeiCountToday;
    }
    public String getIssuerUUID() {
        return issuerUUID;
    }
    public String getOwnerUUID() {
        return ownerUUID;
    }
    public PatientVisitModel getPatientVisitModel() {
        return pvt;
    }
    public String getMemo() {
        return memo;
    }
    public String getFacilityId() {
        return facilityId;
    }

}
