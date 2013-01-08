package open.dolphin.client;

/**
 * KarteSenderResult
 * 
 * @author masuda, Masuda Naika
 */
public class KarteSenderResult {
    
    public static final String NO_ERROR = "00";
    public static final String ERROR = "XXX";    // unclassified errors
    public static final String SKIPPED = "skipped";
    
    //public static final String ORCA_API = "ORCA API";
    //public static final String CLAIM = "CLAIM";
    //public static final String MML = "MML";
    //public static final String FALCO = "FALCO";
    
    private String type;
    private String code;
    private String msg;
    
    public KarteSenderResult(String type, String code, String msg) {
        this.type = type;
        this.code = code;
        this.msg = msg;
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
    
    public String getType() {
        return type;
    }
    public String getCode() {
        return code;
    }
    public String getMsg() {
        return msg;
    }
    
    public String getCodeAndMsg() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append(":");
        sb.append(code);
        sb.append(":");
        sb.append(msg);
        return sb.toString();
    }
    
    public boolean isError() {
        if (NO_ERROR.equals(code) || SKIPPED.equals(code)) {
            return false;
        }
        return true;
    }
}
