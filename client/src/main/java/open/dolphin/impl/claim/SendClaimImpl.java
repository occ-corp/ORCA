package open.dolphin.impl.claim;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import open.dolphin.client.*;
import open.dolphin.delegater.OrcaDelegater;
import open.dolphin.project.Project;
import org.apache.log4j.Logger;

/**
 * SendClaimImpl
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika こりゃ失敗ｗ
 */
public class SendClaimImpl implements ClaimMessageListener {

    // Properties
    private String host;
    private int port;
    private String enc;
    private String name;
    private MainWindow context;
    private Logger logger;
    
    private Thread thread;
    private ClaimSendTask claimSendTask;
    
    private static final String CLAIM = "CLAIM";

    /**
     * Creates new ClaimQue
     */
    public SendClaimImpl() {
        logger = ClientContext.getClaimLogger();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public MainWindow getContext() {
        return context;
    }

    @Override
    public void setContext(MainWindow context) {
        this.context = context;
    }

    /**
     * プログラムを開始する。
     */
    @Override
    public void start() {

        setHost(Project.getString(Project.CLAIM_ADDRESS));
        setPort(Project.getInt(Project.CLAIM_PORT));
        setEncoding(Project.getString(Project.CLAIM_ENCODING));
        claimSendTask = new ClaimSendTask();
        thread = new Thread(claimSendTask, "Claim send thread");
        thread.start();

        logger.info("SendClaim started with = host = " + getHost() + " port = " + getPort());
    }

    /**
     * プログラムを終了する。
     */
    @Override
    public void stop() {
        claimSendTask.stop();
        thread.interrupt();
        thread = null;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getEncoding() {
        return enc;
    }

    @Override
    public void setEncoding(String enc) {
        this.enc = enc;
    }

    /**
     * カルテで CLAIM データが生成されるとこの通知を受ける。
     */
    @Override
    public void claimMessageEvent(ClaimMessageEvent evt) {
        boolean client = Project.CLAIM_CLIENT.equals(Project.getString(Project.CLAIM_SENDER));
        if (client) {
            claimSendTask.sendClaim(evt);
        } else {
            OrcaDelegater.getInstance().sendClaim(evt);
        }
    }
    
    private class ClaimSendTask implements Runnable {

        private List<ClaimMessageEvent> queue;
        private boolean isRunning;
        private Selector selector;

        private ClaimSendTask() {
            queue = new LinkedList<ClaimMessageEvent>();
            isRunning = true;
            try {
                selector = Selector.open();
            } catch (IOException ex){
                ex.printStackTrace(System.err);
            } 
        }

        private synchronized void sendClaim(ClaimMessageEvent evt) {
            queue.add(evt);
            selector.wakeup();
        }
        
        @Override
        public void run() {
            try {
                while (selector.select() >= 0 && isRunning) {
                    if (!queue.isEmpty()) {
                        InetSocketAddress address = new InetSocketAddress(getHost(), getPort());
                        SocketChannel channel = SocketChannel.open();
                        channel.socket().setReuseAddress(true);
                        channel.configureBlocking(false);
                        channel.connect(address);
                        for (Iterator<ClaimMessageEvent> itr = queue.iterator(); itr.hasNext();) {
                            ClaimMessageEvent evt = itr.next();
                            // registerは同一スレッド内でないとダメ!!
                            ClaimIOHandler handler = new ClaimIOHandler(evt, getEncoding());
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
                logger.warn("通信エラーが発生しました" + ex);
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
                        ex.printStackTrace(System.err);
                    }
                }
            } catch (ClosedSelectorException ex) {
            }
        }

        private void stop() {
            isRunning = false;
            selector.wakeup();
        }
    }

    private void processException(ClaimException ex) {

        ClaimException.ERROR_CODE errCode = ex.getErrorCode();
        String errMsg = getErrorInfo(errCode);
        boolean noError = (errCode == ClaimException.ERROR_CODE.NO_ERROR);

        Object evtSource = ex.getClaimEvent().getSource();
        if (evtSource instanceof ClaimSender) {
            ClaimSender sender = (ClaimSender) evtSource;
            KarteSenderResult result = !noError
                    ? new KarteSenderResult(CLAIM, KarteSenderResult.ERROR, errMsg, sender)
                    : new KarteSenderResult(CLAIM, KarteSenderResult.NO_ERROR, null, sender);
            sender.fireResult(result);
        } else if (evtSource instanceof DiagnosisSender) {
            DiagnosisSender sender = (DiagnosisSender) evtSource;
            KarteSenderResult result = !noError
                    ? new KarteSenderResult(CLAIM, KarteSenderResult.ERROR, errMsg, sender)
                    : new KarteSenderResult(CLAIM, KarteSenderResult.NO_ERROR, null, sender);
            sender.fireResult(result);
        }
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