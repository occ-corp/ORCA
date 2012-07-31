package open.dolphin.impl.claim;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.JOptionPane;
import open.dolphin.client.ClaimMessageEvent;
import open.dolphin.client.ClaimMessageListener;
import open.dolphin.client.ClientContext;
import open.dolphin.client.MainWindow;
import open.dolphin.project.Project;
import org.apache.log4j.Logger;

/**
 * SendClaimPlugin
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika こりゃ失敗ｗ
 */
public class SendClaimImpl implements ClaimMessageListener {

    // Socket constants
    private final int EOT = 0x04;
    private final int ACK = 0x06;
    private final int NAK = 0x15;
    private final int DEFAULT_TRY_COUNT = 3;		// Socket 接続を試みる回数
    private final long DEFAULT_SLEEP_TIME = 20 * 1000L; // Socket 接続が得られなかった場合に次のトライまで待つ時間 msec
    
    // Alert constants
    private final int TT_NO_ERROR           = 0;
    private final int TT_QUEUE_NOT_EMPTY    = 1;
    private final int TT_NAK_SIGNAL         = 2;
    private final int TT_SENDING_TROUBLE    = 3;
    private final int TT_CONNECTION_REJECT  = 4;
    
    // Strings
    private final String retryString = "再試行";
    private final String dumpString = "ログへ記録";
    
    // Properties
    private String host;
    private int port;
    private String enc;
    
    private int tryCount = DEFAULT_TRY_COUNT;
    private long sleepTime = DEFAULT_SLEEP_TIME;
    
    private ExecutorService exec;
    private final List<ClaimMessageEvent> queue;
    private OrcaSocket orcaSocket;
    
    private MainWindow context;
    private String name;
    private Logger logger;

    /**
     * Creates new ClaimQue
     */
    public SendClaimImpl() {
        queue = new LinkedList<ClaimMessageEvent>();
        exec = Executors.newSingleThreadExecutor();
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

        if (orcaSocket == null) {
            orcaSocket = new OrcaSocket(getHost(), getPort(), sleepTime, tryCount);
        }
        if (exec != null) {
            shutdownExecutor();
        }
        exec = Executors.newSingleThreadExecutor();

        logger.info("SendClaim started with = host = " + getHost() + " port = " + getPort());
    }

    /**
     * プログラムを終了する。
     */
    @Override
    public synchronized void stop() {
        
        // 未送信キューがあるならば警告する
        if (queue != null && !queue.isEmpty()) {
            int option = alertDialog(TT_QUEUE_NOT_EMPTY);
            if (option == 1) {
                processQueue();
            }
        }

        shutdownExecutor();

        logDump();
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

    public int getTryCount() {
        return tryCount;
    }

    public void setTryCount(int val) {
        tryCount = val;
    }

    /**
     * カルテで CLAIM データが生成されるとこの通知を受ける。
     */
    @Override
    public synchronized void claimMessageEvent(ClaimMessageEvent e) {
        queue.add(e);
        processQueue();
    }

    private void processQueue() {

        if (queue == null || queue.isEmpty()) {
            return;
        }
        
        while (true) {

            List<Callable<ClaimMessageEvent>> taskList = new ArrayList<Callable<ClaimMessageEvent>>();
            for (ClaimMessageEvent claimEvent : queue) {
                Callable task = new ClaimSendTask(orcaSocket, claimEvent);
                taskList.add(task);
            }

            try {
                boolean retry = false; 
                List<Future<ClaimMessageEvent>> futures = exec.invokeAll(taskList);
                for (Future<ClaimMessageEvent> future : futures) {
                    ClaimMessageEvent result = future.get();
                    int errorCode = result.getErrorCode();
                    if (errorCode != TT_NO_ERROR) {
                        int option = alertDialog(errorCode);
                        if (option == 1) {
                            warnLog(getErrorInfo(errorCode), result);
                        } else {
                            // 再試行する
                            retry = true;
                            break;  // forをbreak
                        }
                    }
                    // キューから除去
                    queue.remove(result);
                }
                if (retry) {
                    continue;   // whileのcontinue
                }
            } catch (ExecutionException ex) {
                alertDialog(TT_SENDING_TROUBLE);
            } catch (InterruptedException ex) {
                alertDialog(TT_SENDING_TROUBLE);
            } finally {
                // ループを抜ける
                break;
            }
        }
    }

    private String getErrorInfo(int errorCode) {
        
        String ret = null;
        switch(errorCode) {
            case TT_NO_ERROR:
                ret = "No Error";
                break;
            case TT_QUEUE_NOT_EMPTY:
                ret = "Queue is not empty";
                break;
            case TT_NAK_SIGNAL:
                ret = "NAK signal received from ORCA";
                break;
            case TT_SENDING_TROUBLE:
                ret = "CLAIM sending trouble";
                break;
            case TT_CONNECTION_REJECT:
                ret = "CLAIM connection rejected";
                break;
            default:
                ret = "Unknown Error";
                break;
        }
        return ret;
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

    private int alertDialog(int code) {

        final String title = "OpenDolphin: CLAIM 送信";
        StringBuilder sb = new StringBuilder();

        switch (code) {
            case TT_QUEUE_NOT_EMPTY:
                sb.append("未送信のCLAIM(レセプト)データが").append(queue.size());
                sb.append(" 個あります。\n");
                sb.append("CLAIM サーバとの接続を確認してください。\n");
                break;
            case TT_NAK_SIGNAL:
                sb.append("CLAIM(レセプト)データがサーバにより拒否されました。\n");
                sb.append("送信中のデータはログに記録します。診療報酬の自動入力はできません。\n");
                break;
            case TT_SENDING_TROUBLE:
                sb.append("CLAIM(レセプト)データの送信中にエラーがおきました。\n");
                sb.append("送信中のデータはログに記録します。診療報酬の自動入力はできません。\n");
                break;
            case TT_CONNECTION_REJECT:
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
        return option;
    }

    /**
     * CLAIM 送信スレッド。
     */
    private class ClaimSendTask implements Callable {

        private OrcaSocket orcaSocket;
        private ClaimMessageEvent claimEvent;

        public ClaimSendTask(OrcaSocket orcaSocket, ClaimMessageEvent claimEvent) {
            this.orcaSocket = orcaSocket;
            this.claimEvent = claimEvent;
        }

        @Override
        public ClaimMessageEvent call() throws Exception {
            BufferedOutputStream writer = null;
            BufferedInputStream reader = null;
            Socket socket = null;
            try {
                // CLAIM Event を取得
                String instance = claimEvent.getClaimInsutance();

                // Gets connection
                socket = orcaSocket.getSocket();
                if (socket == null) {
                    claimEvent.setErrorCode(TT_CONNECTION_REJECT);
                    return claimEvent;
                }

                // Gets io stream
                OutputStream out = socket.getOutputStream();
                DataOutputStream dout = new DataOutputStream(out);
                writer = new BufferedOutputStream(dout);

                InputStream in = socket.getInputStream();
                DataInputStream din = new DataInputStream(in);
                reader = new BufferedInputStream(din);

                // Writes UTF8 data
                writer.write(instance.getBytes(enc));
                writer.write(EOT);
                writer.flush();

                // Reads result
                int c = reader.read();
                if (c == ACK) {
                    log("CLAIM ACK", claimEvent);
                    claimEvent.setErrorCode(TT_NO_ERROR);
                } else if (c == NAK) {
                    warnLog("received NAK", claimEvent);
                    claimEvent.setErrorCode(TT_NAK_SIGNAL);
                }

            } catch (IOException e) {
                e.printStackTrace(System.err);
                logger.warn(e.getMessage());
                claimEvent.setErrorCode(TT_SENDING_TROUBLE);

            } catch (Exception e) {
                e.printStackTrace(System.err);
                logger.warn(e.getMessage());
                claimEvent.setErrorCode(TT_SENDING_TROUBLE);
                
            } finally {
                try {
                    writer.close();
                    reader.close();
                    socket.close();
                } catch (Exception e) {
                }
            }

            return claimEvent;
        }
    }
}