package open.dolphin.server.orca;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import open.dolphin.infomodel.ClaimMessageModel;
import open.dolphin.rest.OrcaResource;

/**
 * SendClaimTask
 * @author masuda, Mausda Naika
 */
public class SendClaimTask implements Runnable {
    
    private static final String UTF8 = "UTF-8";
    private static final int DEFAULT_PORT = 5002;
    
    private static final String NO_ERROR = "00";
    private static final String ERROR = "XXX";
    
    private static final Logger logger = Logger.getLogger(SendClaimTask.class.getSimpleName());

    private List<AsyncContext> queue;
    
    private Selector selector;
    private boolean isRunning;

    
    public  SendClaimTask() {
        
        queue = new LinkedList<AsyncContext>();
        isRunning = true;
        
        try {
            // セレクタの生成
            selector = Selector.open();
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        }
    }
    
    public synchronized void sendClaim(AsyncContext ac) {
        queue.add(ac);
        selector.wakeup();
    }
    
    private int getPort(ClaimMessageModel model) {
        int port = model.getPort();
        return (port == 0) ? DEFAULT_PORT : port;
    }
    
    private String getEncoding(ClaimMessageModel model) {
        String encoding = model.getEncoding();
        return (encoding == null) ? UTF8 : encoding;
    }

    @Override
    public void run() {
        
        try {
            while (selector.select() >= 0 && isRunning) {
                if (!queue.isEmpty()) {
                    for (Iterator<AsyncContext> itr = queue.iterator(); itr.hasNext();) {
                        AsyncContext ac = itr.next();
                        ClaimMessageModel model = (ClaimMessageModel) 
                                ac.getRequest().getAttribute(ClaimMessageModel.class.getSimpleName());
                        InetSocketAddress address = new InetSocketAddress(model.getAddress(), getPort(model));
                        SocketChannel channel = SocketChannel.open();
                        channel.socket().setReuseAddress(true);
                        channel.configureBlocking(false);
                        channel.connect(address);
                        // registerは同一スレッド内でないとダメ!!
                        ClaimIOHandler handler = new ClaimIOHandler(ac, getEncoding(model));
                        channel.register(selector, SelectionKey.OP_CONNECT, handler);
                        itr.remove();
                    }
                    continue;
                }
                
                // 実際の処理
                for (Iterator<SelectionKey> itr = selector.selectedKeys().iterator(); itr.hasNext();) {
                    SelectionKey key = itr.next();
                    itr.remove();
                    ClaimIOHandler handler = (ClaimIOHandler) key.attachment();
                    try {
                        handler.handle(key);
                    } catch (ClaimException ex) {
                        processException(ex);
                    }
                }
                
            }
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        } catch (ClosedSelectorException ex) {
        } finally {
            closeAllChannel();
        }
    }

    private void closeAllChannel() {
        try {
            for (SelectionKey key : selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException ex) {
                    logger.warning(ex.getMessage());
                }
            }
        } catch (ClosedSelectorException ex) {
        }
    }

    public void stop() {
        isRunning = false;
        selector.wakeup();
    }
    
    private void processException(ClaimException ex) {
        
        ClaimException.ERROR_CODE code = ex.getErrorCode();
        String errMsg = getErrorInfo(code);
        
        AsyncContext ac = ex.getAsyncContext();
        ClaimMessageModel model = (ClaimMessageModel) 
                ac.getRequest().getAttribute(ClaimMessageModel.class.getSimpleName());
        
        if (code == ClaimException.ERROR_CODE.NO_ERROR) {
            model.setErrorCode(NO_ERROR);
        } else {
            model.setErrorCode(ERROR);
            model.setErrorMsg(errMsg);
        }

        ac.dispatch(OrcaResource.CLAIMRES_URL);
    }
    
    private String getErrorInfo(ClaimException.ERROR_CODE errorCode) {

        String ret;
        switch (errorCode) {
            case NO_ERROR:
                ret = "No Error";
                break;
            case NAK_SIGNAL:
                ret = "NAK signal received from ORCA";
                break;
            case IO_ERROR:
                ret = "I/O error";
                break;
            case CONNECTION_REJECT:
                ret = "CLAIM connection rejected";
                break;
            default:
                ret = "Unknown Error";
                break;
        }
        return ret;
    }
}
