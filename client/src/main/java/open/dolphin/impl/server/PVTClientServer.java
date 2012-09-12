package open.dolphin.impl.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import open.dolphin.client.ClientContext;
import open.dolphin.client.MainWindow;
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
    private PvtServerThread serverThread;
    
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
            
            if (test != null && !test.isEmpty()) {
                debug("PVT ServerSocket bind address = " + getBindAddress());
                try {
                    InetAddress addr = InetAddress.getByName(test);
                    address = new InetSocketAddress(addr, port);
                } catch (UnknownHostException e) {
                    debug("Invalid bind address. Use localhost.");
                }
            }

            if (address == null) {
                address = new InetSocketAddress(InetAddress.getLocalHost(), port);
            }

            logger.info("PVT Server is binded " + address + " with encoding: " + encoding);

            serverThread = new PvtServerThread(PVTClientServer.this, address);
            thread = new Thread(serverThread, "PVT server socket");
            thread.start();

        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            logger.warn("IOException while creating the ServerSocket: " + ex.toString());
        }
    }

    /**
     * 受付受信サーバをストップする。
     */
    public void stopService() {

        // ServerThreadを中止させる
        serverThread.stop();

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
    
    // PvtClaimIOHanlderから呼ばれる
    public void putPvt(String pvtXml) {
        
        PvtPostTask task = new PvtPostTask(pvtXml);
        exec.submit(task);
    }
    
    private void debug(String msg) {
        if (DEBUG) {
            logger.debug(msg);
        }
    }
}
