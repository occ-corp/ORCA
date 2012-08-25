package open.dolphin.server.pvt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import open.dolphin.session.MasudaServiceBean;
import open.dolphin.session.PVTServiceBean;

/**
 * PvtServletServer
 * 
 * Java EE環境における非同期プログラミング
 * http://d.hatena.ne.jp/nekop/20120417/1334654442
 * @author masuda, Masuda Naika
 */
@WebListener
public class PvtServletServer implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(PvtServletServer.class.getSimpleName());
    private static final String UTF8 = "UTF-8";
    private static final int DEFAULT_PORT = 5002;
    
    private String encoding = UTF8;
    private int port = DEFAULT_PORT;
    
    // ServerSocketのスレッド nio!
    private Thread thread;
    private PvtServerThread serverThread;
    // PVT登録処理のSingle Thread Executor
    private ExecutorService exec;
    
    @Inject
    private PVTServiceBean pvtServiceBean;
    
    @Inject
    private MasudaServiceBean masudaServiceBean;

    
    @Override
    public void contextInitialized(ServletContextEvent sce) {

        exec = Executors.newSingleThreadExecutor();

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), port);

            String msg = "PVT Server is binded " + address + " with encoding: " + encoding;
            logger.info(msg);

            serverThread = new PvtServerThread(PvtServletServer.this, address);
            thread = new Thread(serverThread, "PVT server socket");
            thread.start();

        } catch (IOException ex) {
            String msg = "IOException while creating the ServerSocket: " + ex.toString();
            logger.warning(msg);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        
        // ServerThreadを中止させる
        serverThread.stop();
        // thread終了を待つ
        try {
            thread.join(100);
        } catch (InterruptedException ex) {
        }
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
    
    public String getEncoding() {
        return encoding;
    }
    
    public PVTServiceBean getPvtServiceBean() {
        return pvtServiceBean;
    }
    
    public MasudaServiceBean getMasudaServiceBean() {
        return masudaServiceBean;
    }

    // PvtClaimIOHanlderから呼ばれる
    public void postPvt(String pvtXml) {
        PvtPostTask task = new PvtPostTask(this, pvtXml);
        exec.submit(task);
    }
}
