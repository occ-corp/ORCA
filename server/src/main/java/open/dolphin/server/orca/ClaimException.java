package open.dolphin.server.orca;

import javax.servlet.AsyncContext;

/**
 * ClaimException
 * @author masuda, Masuda Naika
 */
public class ClaimException extends Exception {

    public static enum ERROR_CODE {

        NO_ERROR, CONNECTION_REJECT, IO_ERROR, NAK_SIGNAL
    };
    
    private ERROR_CODE code;
    private AsyncContext ac;

    public ClaimException(ERROR_CODE code, AsyncContext ac) {
        this.code = code;
        this.ac = ac;
    }

    public ERROR_CODE getErrorCode() {
        return code;
    }

    public AsyncContext getAsyncContext() {
        return ac;
    }
}
