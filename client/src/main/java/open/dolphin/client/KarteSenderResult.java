package open.dolphin.client;

/**
 * KarteSenderResult
 * 
 * @author masuda, Masuda Naika
 */
public class KarteSenderResult {
    
    public static final String PROP_KARTE_SENDER_RESULT = "karteSenderResult";
    public static final String PROP_DIAG_SENDER_RESULT = "diagSenderResult";
    
    public static final String NO_ERROR = "00";
    public static final String REPLACED = "80";
    public static final String ERROR = "XXX";    // unclassified errors
    public static final String SKIPPED = "skipped";
    
    private IKarteSender karteSender;
    private IDiagnosisSender diagnosisSender;
    
    private String type;
    private String code;
    private String msg;
    
    public KarteSenderResult(String type, String code, String msg) {
        this.type = type;
        this.code = code;
        this.msg = msg;
    }
    
    public KarteSenderResult(String type, String code, String msg, IKarteSender karteSender) {
        this.type = type;
        this.code = code;
        this.msg = msg;
        this.karteSender = karteSender;
    }
    
    public KarteSenderResult(String type, String code, String msg, IDiagnosisSender diagnosisSender) {
        this.type = type;
        this.code = code;
        this.msg = msg;
        this.diagnosisSender = diagnosisSender;
    }

    public void setType(String type) {
        this.type = type;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public void setKarteSender(IKarteSender sender) {
        this.karteSender = sender;
    }
    public void setDiagnosisSender(IDiagnosisSender sender) {
        this.diagnosisSender = sender;
    }
    
    public String getType() {
        return type;
    }
    public String getCode() {
        return code;
    }
    public String getMsg() {
        return msg;
    }
    public IKarteSender getKarteSender() {
        return karteSender;
    }
    public IDiagnosisSender getDiagnosisSender() {
        return diagnosisSender;
    }
    
    public String getCodeAndMsg() {
        String ptName = null;
        String ptId = null;
        String saveTime = null;
        if (karteSender != null) {
            ptId = karteSender.getContext().getPatient().getPatientId();
            ptName = karteSender.getContext().getPatient().getFullName();
        } else if (diagnosisSender != null) {
            ptId = diagnosisSender.getContext().getPatient().getPatientId();
            ptName = diagnosisSender.getContext().getPatient().getFullName();
        }
        StringBuilder sb = new StringBuilder();
        if (ptName != null && ptId != null) {
            sb.append(ptId).append(" ").append(ptName).append(" 様のカルテ、\n");
        }
        sb.append(type);
        sb.append(":");
        sb.append(code);
        sb.append(":");
        sb.append(msg == null ? "" : msg);
        return sb.toString();
    }
    
    public boolean isError() {
        if (NO_ERROR.equals(code) 
                || REPLACED.equals(code)
                || SKIPPED.equals(code)) {
            return false;
        }
        return true;
    }
    
    public void removeListener() {
        if (karteSender != null) {
            karteSender.removeListeners();
        }
        if (diagnosisSender != null) {
            diagnosisSender.removeListeners();
        }
    }
}
