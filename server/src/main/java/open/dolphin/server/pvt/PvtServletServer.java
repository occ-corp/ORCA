package open.dolphin.server.pvt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * PvtServerThread, server
 *
 * @author masuda, Masuda Naika
 */
public class PvtServletServer {

    private static final Logger logger = Logger.getLogger(PvtServletServer.class.getSimpleName());
    
    private static final int DEFAULT_PORT = 5002;
    private int port = DEFAULT_PORT;
    
    // ServerSocketのスレッド nio!
    private Thread thread;
    private PvtServerThread serverThread;
    // PVT登録処理のSingle Thread Executor
    private ExecutorService exec;

    private static PvtServletServer instance;
    
    static {
        instance = new PvtServletServer();
    }
    
    public static PvtServletServer getInstance() {
        return instance;
    }

    private PvtServletServer() {
    }

    public void start() {

        exec = Executors.newSingleThreadExecutor();

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), port);
            String msg = "PVT Server is binded " + address;
            logger.info(msg);

            serverThread = new PvtServerThread(address);
            thread = new Thread(serverThread, "PVT server socket");
            thread.start();

        } catch (IOException ex) {
            String msg = "IOException while creating the ServerSocket: " + ex.toString();
            logger.warning(msg);
        }
    }

    public void dispose() {

        // ServerThreadを中止させる
        serverThread.stop();

        // ServerSocketのThread破棄する
        thread.interrupt();
        thread = null;

        // SocketReadTaskをシャットダウンする
        shutdownExecutor();

        logger.info("PVT Server stopped.");
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
    public void postPvt(String pvtXml) {
        PvtPostTask task = new PvtPostTask(pvtXml);
        exec.submit(task);
    }
}
