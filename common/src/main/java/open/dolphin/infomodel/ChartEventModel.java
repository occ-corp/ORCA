package open.dolphin.infomodel;

/**
 * Chart event通知用のモデル
 *
 * @author masuda, Masuda Naika
 */
public class ChartEventModel {
    
    private String issuerUUID;
    private EVENT eventType;
    
    private long pvtPk;
    private int state;
    private String memo;
    private int byomeiCount;
    private int byomeiCountToday;
    private String ownerUUID;
    private String facilityId;
    
    private PatientVisitModel pvt;
    
    private long ptPk;
    private PatientModel patient;
    
    // messaging
    private String msgUUID;     // メッセージ固有ID
    private String replyToUUID; // どのメッセージへの返答か
    private String msgOwner;    // メッセージ発行者
    private String msgTitle;    // JOptionPaneのタイトル
    private String msgContent;  // JOptionPaneのメッセージ本文
    private String[] msgOpts;   // JOptionPaneに表示するコマンド
    private int msgTimeout;     // タイムアウト　未使用
    
    public static enum EVENT {PVT_STATE, PVT_ADD, PVT_DELETE, PVT_RENEW, PVT_MERGE, PM_MERGE, 
        MSG_BROADCAST, MSG_REPLY, REQUEST_REBOOT};

    public ChartEventModel() {
    }
    
    public ChartEventModel(String issuerUUID) {
        this.issuerUUID = issuerUUID;
    }

    public void setParamFromPvt(PatientVisitModel pvt) {
        
        if (pvt == null) {
            return;
        }
        
        this.pvtPk = pvt.getId();
        this.state = pvt.getState();
        this.byomeiCount = pvt.getByomeiCount();
        this.byomeiCountToday = pvt.getByomeiCountToday();
        this.memo = pvt.getMemo();
        this.ownerUUID = pvt.getPatientModel().getOwnerUUID();
        this.facilityId = pvt.getFacilityId();
        this.ptPk = pvt.getPatientModel().getId();
    }
    
    public void setEventType(EVENT eventType) {
        this.eventType = eventType;
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
        issuerUUID= issuer;
    }
    public void setOwnerUUID(String owner) {
        ownerUUID = owner;
    }
    public void setPatientVisitModel(PatientVisitModel pvt) {
        this.pvt = pvt;
    }
    public void setPtPk(long ptPk) {
        this.ptPk = ptPk;
    }
    public void setPatientModel(PatientModel patient) {
        this.patient = patient;
    }
    public void setMemo(String memo) {
        this.memo = memo;
    }
    public void setFacilityId(String fid) {
        facilityId = fid;
    }
    
    public EVENT getEventType() {
        return eventType;
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
    public long getPtPk() {
        return ptPk;
    }
    public PatientModel getPatientModel() {
        return patient;
    }
    public String getMemo() {
        return memo;
    }
    public String getFacilityId() {
        return facilityId;
    }
    
    // messaging
    public void setMsgUUID(String msgUUID) {
        this.msgUUID = msgUUID;
    }
    public void setReplyToUUID(String replyToUUID) {
        this.replyToUUID = replyToUUID;
    }
    public void setMsgOwner(String msgOwner) {
        this.msgOwner = msgOwner;
    }
    public void setMsgTitle(String msgTitle) {
        this.msgTitle = msgTitle;
    }
    public void setMsgContent(String msgContent) {
        this.msgContent = msgContent;
    }
    public void setMsgOpts(String[] msgOpts) {
        this.msgOpts = msgOpts;
    }
    public void setMsgTimeout(int msgTimeout) {
        this.msgTimeout = msgTimeout;
    }
    
    public String getMsgUUID() {
        return msgUUID;
    }
    public String getReplyToUUID() {
        return replyToUUID;
    }
    public String getMsgOwner() {
        return msgOwner;
    }
    public String getMsgTitle() {
        return msgTitle;
    }
    public String getMsgContent() {
        return msgContent;
    }
    public String[] getMsgOpts() {
        return msgOpts;
    }
    public int getMsgTimeout() {
        return msgTimeout;
    }
}
