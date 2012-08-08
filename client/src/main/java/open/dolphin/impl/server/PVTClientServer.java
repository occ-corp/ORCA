package open.dolphin.impl.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import open.dolphin.client.ClientContext;
import open.dolphin.client.MainWindow;
import open.dolphin.delegater.PVTDelegater;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.infomodel.UserModel;
import open.dolphin.server.PVTServer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * PVT socket server
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class PVTClientServer implements PVTServer {

    public static final String UTF8 = "UTF-8";
    public static final String SJIS = "SHIFT_JIS";
    public static final String EUC = "EUC_JIS";
    private static final int DEFAULT_PORT = 5002;
    
    private String encoding = UTF8;
    private int port = DEFAULT_PORT;
    
    // バインドアドレス
    private String bindAddress;
    // ServerSocketのスレッド nio!
    private Thread thread;
    private ServerThread serverThread;
    
    // PVT登録処理のSingle Thread Executor
    private ExecutorService exec;
    
    private MainWindow context;
    private String name;
    private UserModel user;
    private boolean DEBUG;
    private Logger logger;

    /**
     * Creates new ClaimServer
     */
    public PVTClientServer() {
        DEBUG = (ClientContext.getPvtLogger().getLevel() == Level.DEBUG);
        logger = ClientContext.getPvtLogger();
    }

    @Override
    public String getBindAddress() {
        return bindAddress;
    }

    @Override
    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
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
        return encoding;
    }

    @Override
    public void setEncoding(String enc) {
        encoding = enc;
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

    @Override
    public void start() {
        startService();
    }

    @Override
    public void stop() {
        stopService();
    }

    /**
     * 受付受信サーバを開始する。
     */
    public void startService() {

        if (exec != null) {
            shutdownExecutor();
        }
        exec = Executors.newSingleThreadExecutor();

        try {
            InetSocketAddress address = null;
            String test = getBindAddress();

            if (test != null && (!test.equals(""))) {
                debug("PVT ServerSocket bind address = " + getBindAddress());
                try {
                    InetAddress addr = InetAddress.getByName(test);
                    address = new InetSocketAddress(addr, port);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            if (address == null) {
                address = new InetSocketAddress(InetAddress.getLocalHost(), port);
            }

            logger.info("PVT Server is binded " + address + " with encoding: " + encoding);

            serverThread = new ServerThread(address);
            thread = new Thread(serverThread, "PVT server socket");
            thread.start();

        } catch (IOException e) {
            e.printStackTrace(System.err);
            logger.warn("IOException while creating the ServerSocket: " + e.toString());
        }
    }

    /**
     * 受付受信サーバをストップする。
     */
    public void stopService() {

        // ServerThreadを中止させる
        serverThread.stop();
        // thread終了を待つ
        try {
            thread.join();
        } catch (InterruptedException ex) {
        }
        // ServerSocketのThread破棄する
        thread.interrupt();
        thread = null;

        // SocketReadTaskをシャットダウンする
        shutdownExecutor();
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

    private final class ServerThread implements Runnable {
        
        private InetSocketAddress address;
        private ServerSocketChannel ssc = null;
        private Selector selector = null;
        private boolean isRunning;
        
        private ServerThread(InetSocketAddress address) {
            this.address = address;
            initialize();
        }

        private void stop() {
            isRunning = false;
            selector.wakeup();
        }
        
        private void initialize() {
            
            try {
                // ソケットチャネルを生成・設定
                ssc = ServerSocketChannel.open();
                ssc.socket().setReuseAddress(true);
                ssc.socket().bind(address);
                // ノンブロッキングモードに設定
                ssc.configureBlocking(false);
                // セレクタの生成
                selector = Selector.open();
                // ソケットチャネルをセレクタに登録
                ssc.register(selector, SelectionKey.OP_ACCEPT, new PvtClaimAcceptHandler(PVTClientServer.this));
            } catch (IOException ex) {
            }
        }

        @Override
        public void run() {
            
            isRunning = true;
            
            try {
                while (selector.select() >= 0 && isRunning) {

                    for (Iterator<SelectionKey> itr = selector.selectedKeys().iterator(); itr.hasNext();) {
                        SelectionKey key = itr.next();
                        itr.remove();
                        // アタッチしたオブジェクトに処理を委譲
                        IHandler handler = (IHandler) key.attachment();
                        handler.handle(key);
                    }
                }
            } catch (ClosedChannelException ex) {
                logger.warn("ソケットがクローズしています:" + ex);
            } catch (IOException ex) {
                logger.warn("通信エラーが発生しました" + ex);
            } finally {
                try {
                    for (SelectionKey key : selector.keys()) {
                        key.channel().close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    }
    
    // PvtClaimIOHanlderから呼ばれる
    public void putPvt(String pvtXml) {
        PutPvtTask task = new PutPvtTask(pvtXml);
        exec.submit(task);
    }
        
    private final class PutPvtTask implements Runnable {

        private String pvtXml;

        private PutPvtTask(String xml) {
            pvtXml = xml;
        }

        @Override
        public void run() {
            
            BufferedReader r = new BufferedReader(new StringReader(pvtXml));
            PVTBuilder builder = new PVTBuilder();
            builder.parse(r);
            PatientVisitModel model = builder.getProduct();

            //FEV-70に患者情報を送る
            SendPatientInfoToFEV.getInstance().send(model);
            // シングルトン化
            PVTDelegater pdl = PVTDelegater.getInstance();
            
            pdl.addPvt(model);
        }
    }
    
    private void debug(String msg) {
        if (DEBUG) {
            logger.debug(msg);
        }
    }
}
