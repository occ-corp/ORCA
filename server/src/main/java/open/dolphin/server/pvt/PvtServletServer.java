package open.dolphin.server.pvt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import open.dolphin.infomodel.PatientVisitModel;

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

    // PvtClaimIOHanlderから呼ばれる
    public void postPvt(String pvtXml) {
        try {
            // Pvtをサーバーに登録する
            Future<PatientVisitModel>future = exec.submit(new PvtPostTask(pvtXml));
            // FEV-70にexportする
            PatientVisitModel pvt = future.get();
            if (pvt != null) {
                exec.submit(new FevPostTask(pvt));
            }
        } catch (NamingException ex) {
        } catch (InterruptedException ex) {
        } catch (ExecutionException ex) {
        }
    }

}
