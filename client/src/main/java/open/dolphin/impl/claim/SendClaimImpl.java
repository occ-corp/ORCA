package open.dolphin.impl.claim;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import open.dolphin.client.ClaimMessageEvent;
import open.dolphin.client.ClaimMessageListener;
import open.dolphin.client.ClientContext;
import open.dolphin.client.MainWindow;
import open.dolphin.project.Project;
import org.apache.log4j.Logger;

/**
 * SendClaimImpl
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika こりゃ失敗ｗ
 */
public class SendClaimImpl implements ClaimMessageListener {

    // Strings
    private final String retryString = "再試行";
    private final String dumpString = "ログへ記録";
    
    // Properties
    private String host;
    private int port;
    private String enc;
    private String name;
    
    private MainWindow context;

    private InetSocketAddress address;
    private Selector selector;
    private List<ClaimMessageEvent> queue;
    private Logger logger;
    private ExecutorService exec;

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

        address = new InetSocketAddress(getHost(), getPort());
        queue = new LinkedList<ClaimMessageEvent>();
        exec = Executors.newSingleThreadExecutor();

        try {
            // セレクタの生成
            selector = Selector.open();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }

        logger.info("SendClaim started with = host = " + getHost() + " port = " + getPort());
    }

    /**
     * プログラムを終了する。
     */
    @Override
    public void stop() {

        // 未送信キューがあるならば警告する
        if (queue != null && !queue.isEmpty()) {
            int option = alertDialog(ClaimException.ERROR_CODE.QUEUE_NOT_EMPTY, null);
            if (option == 1) {
                exec.submit(new QueueSendTask());
            }
        }

        logDump();
        
        try {
            selector.close();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        
        shutdownExecutor();
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
    public void claimMessageEvent(ClaimMessageEvent e) {
        queue.add(e);
        exec.submit(new QueueSendTask());
    }
    
    private void shutdownExecutor() {

        try {
            exec.shutdown();
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException ex) {
            exec.shutdownNow();
        } catch (NullPointerException ex) {
        }
        exec = null;
    }
    
    /**
     * Queue内の CLAIM message をログへ出力する。
     */
    private void logDump() {

        for (ClaimMessageEvent claimEvent : queue) {
            logger.warn(claimEvent.getClaimInsutance());
        }
        queue.clear();
    }

    private void warnLog(String result, ClaimMessageEvent evt) {
        logger.warn(getBasicInfo(result, evt));
        logger.warn(evt.getClaimInsutance());
    }

    private void log(String result, ClaimMessageEvent evt) {
        logger.info(getBasicInfo(result, evt));
    }

    private String getBasicInfo(String result, ClaimMessageEvent evt) {

        String id = evt.getPatientId();
        String nm = evt.getPatientName();
        String sex = evt.getPatientSex();
        String title = evt.getTitle();
        String timeStamp = evt.getConfirmDate();

        StringBuilder buf = new StringBuilder();
        buf.append(result);
        buf.append("[");
        buf.append(id);
        buf.append(" ");
        buf.append(nm);
        buf.append(" ");
        buf.append(sex);
        buf.append(" ");
        buf.append(title);
        buf.append(" ");
        buf.append(timeStamp);
        buf.append("]");

        return buf.toString();
    }

    private String getErrorInfo(ClaimException.ERROR_CODE errorCode) {

        String ret;
        switch (errorCode) {
            case NO_ERROR:
                ret = "No Error";
                break;
            case QUEUE_NOT_EMPTY:
                ret = "Queue is not empty";
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

    private int alertDialog(ClaimException.ERROR_CODE code, ClaimMessageEvent evt) {

        final String title = "OpenDolphin: CLAIM 送信";
        StringBuilder sb = new StringBuilder();

        switch (code) {
            case QUEUE_NOT_EMPTY:
                sb.append("未送信のCLAIM(レセプト)データが").append(queue.size());
                sb.append(" 個あります。\n");
                sb.append("CLAIM サーバとの接続を確認してください。\n");
                break;
            case NAK_SIGNAL:
                sb.append("CLAIM(レセプト)データがサーバにより拒否されました。\n");
                sb.append("送信中のデータはログに記録します。診療報酬の自動入力はできません。\n");
                break;
            case IO_ERROR:
                sb.append("CLAIM(レセプト)データの送信中にエラーがおきました。\n");
                sb.append("送信中のデータはログに記録します。診療報酬の自動入力はできません。\n");
                break;
            case CONNECTION_REJECT:
                sb.append("CLAIM(レセプト)サーバ ");
                sb.append("Host=").append(host);
                sb.append(" Port=").append(port);
                sb.append(" が応答しません。\n");
                sb.append("サーバの電源及び接続を確認してください。\n");
                break;
        }

        sb.append("1. 処理を再試行することもできます。\n");
        sb.append("2. 未送信データをログに記録して処理を継続することができます。\n");
        sb.append("   この場合、データは送信されず、診療報酬は手入力となります。\n");
        int option = JOptionPane.showOptionDialog(
                null,
                sb.toString(),
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null,
                new String[]{retryString, dumpString}, retryString);
        if (option == 1 && evt != null) {
            warnLog(getErrorInfo(code), evt);
        }
        return option;
    }

    private class QueueSendTask implements Runnable {

        @Override
        public void run() {

            try {
                boolean isRunning = true;
                for (ClaimMessageEvent evt : queue) {
                    SocketChannel channel = SocketChannel.open();
                    channel.configureBlocking(false);
                    ClaimIOHandler handler = new ClaimIOHandler(SendClaimImpl.this, evt);
                    channel.register(selector, SelectionKey.OP_CONNECT, handler);
                    channel.connect(address);
                }
                while (isRunning && selector.select() > 0) {
                    for (Iterator<SelectionKey> itr = selector.selectedKeys().iterator(); itr.hasNext();) {
                        SelectionKey key = itr.next();
                        itr.remove();
                        ClaimIOHandler handler = (ClaimIOHandler) key.attachment();
                        try {
                            handler.handle(key);
                        } catch (ClaimException ex) {
                            isRunning = false;
                            processError(ex);
                            break;
                        }
                        if (handler.isNoError()) {
                            ClaimMessageEvent evt = handler.getClaimEvent();
                            queue.remove(evt);
                            isRunning = false;
                        }
                    }
                }
            } catch (IOException ex) {
                logger.warn("通信エラーが発生しました" + ex);
            } finally {
                closeAllChannel();
            }
        }
    }

    private void closeAllChannel() {
        for (SelectionKey key : selector.keys()) {
            try {
                key.channel().close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    private void processError(ClaimException ex) {
        ClaimException.ERROR_CODE code = ex.getErrorCode();
        ClaimMessageEvent evt = ex.getClaimEvent();
        alertDialog(code, evt);
    }
}