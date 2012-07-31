package open.dolphin.impl.server;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import open.dolphin.client.ClientContext;
import open.dolphin.client.ClientContextStub;
import open.dolphin.delegater.UserDelegater;
import open.dolphin.infomodel.UserModel;
import open.dolphin.plugin.PluginLoader;
import open.dolphin.project.Project;
import open.dolphin.project.ProjectStub;
import open.dolphin.server.PVTServer;
import org.apache.log4j.Logger;

/**
 * Console verion of PVT Server
 *
 * @author pns
 * @author modified by masuda, Masuda Naika
 */
public class StandAlonePVTServer {

    private static final String DEFAULT_FACILITY_OID = "1.3.6.1.4.1.9414.10.1";
    private PVTServer pvtServer;
    private Logger pvtLogger;
    private ScheduledFuture timerHandler;
    private static final int maxTryCount = 10;
    private boolean showGUI;

    public StandAlonePVTServer(boolean pro, String baseURI, String userId, String password) {

        // ClientContextとProjectを生成する
        ClientContext.setClientContextStub(new ClientContextStub(pro));
        Project.setProjectStub(new ProjectStub());

        pvtLogger = ClientContext.getPvtLogger();
        pvtLogger.info("Base URI: " + baseURI);
        pvtLogger.info("User ID: " + userId);

        // set dolphin server address
        if (baseURI != null) {
            // pref に設定した host address を Project に書き込み
            Project.getProjectStub().setServerURI(baseURI);
            pvtServer.setBindAddress(Project.getString(Project.CLAIM_BIND_ADDRESS));
            showGUI = false;
        } else {
            showGUI = true;
        }

        String fid = Project.isValid()
                ? Project.getFacilityId()
                : DEFAULT_FACILITY_OID;

        // 10秒ごとにログインをトライする
        Login l = new Login(fid, userId, password);
        ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
        timerHandler = schedule.scheduleWithFixedDelay(l, 0, 10, TimeUnit.SECONDS);
    }

    private class Login implements Runnable {

        private UserDelegater userDlg;
        private UserModel userModel;
        private String fid;
        private String userId;
        private String password;
        private int tryCount;

        private Login(String fid, String userId, String password) {

            userDlg = UserDelegater.getInstance();
            this.fid = fid;
            this.userId = userId;
            this.password = password;
            tryCount = 0;
        }

        @Override
        public void run() {
            userModel = null;
            try {
                userModel = userDlg.login(fid, userId, password);
            } catch (Exception ex) {
                System.out.println(ex);
            }
            ++tryCount;
            if (userModel != null) {
                startPVTServer();
                timerHandler.cancel(true);
                log("Login process completed");
            } else {
                if (tryCount < maxTryCount) {
                    log("Login failed. Retry in 10 sec.");
                } else {
                    // １０回失敗したらあきらめるｗ
                    log("Login failed. Quit program.");
                    timerHandler.cancel(true);
                    exit();
                }
            }
        }
    }

    private void log(String msg) {
        pvtLogger.info(msg);
        //System.out.println(msg);
    }

    @SuppressWarnings("unchecked")
    private void startPVTServer() {

        // plugin loader
        PluginLoader<PVTServer> loader = PluginLoader.load(PVTServer.class);

        Iterator<PVTServer> iter = loader.iterator();
        if (iter.hasNext()) {
            pvtServer = iter.next();
            pvtServer.setContext(null);
            pvtServer.setBindAddress(Project.getString(Project.CLAIM_BIND_ADDRESS));
            pvtServer.start();
        }

        // ^C でサーバを止めるための設定
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                exit();
            }
        });

        // GUI利用するならウィンドウを作成する
        if (showGUI) {
            JFrame jf = new JFrame("OpenDolphin");
//masuda^    アイコン設定
            ClientContext.setDolphinIcon(jf);
//masuda$
            JTextArea ja = new JTextArea();
            ja.setText("OpenDolphin\nPVT server is running.");
            ja.setEditable(false);
            jf.add(ja);
            jf.pack();

            jf.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    exit();
                }
            });
            jf.setVisible(true);
        }
    }

    private void exit() {

        log("Shutdown process starts.");
        if (pvtServer != null) {
            pvtServer.stop();
        }
        log("Shutdown process succeeded. Bye.");
        Runtime.getRuntime().halt(0);
    }

    public static void main(String[] args) {

        String usage = "Usage: java -cp OpenDolphin.jar open.dolphin.impl.server.StandAlonePVTServer -Uxxxx -Pxxxx -Sxxx.xxx.xxx.xxx";
        String userId = "";
        String userPassword = "";
        String baseURI = "";
        boolean pro = false;
        for (String arg : args) {
            if ("pro".equals(arg.toLowerCase())) {
                pro = true;
            }
            if ("-U".equals(arg.substring(0, 2))) {
                userId = arg.substring(2, arg.length());
            }
            if ("-P".equals(arg.substring(0, 2))) {
                userPassword = arg.substring(2, arg.length());
            }
            if ("-S".equals(arg.substring(0, 2))) {
                baseURI = arg.substring(2, arg.length());
            }
        }
        if (!userId.equals("") && !userPassword.equals("") && !baseURI.equals("")) {
            new StandAlonePVTServer(pro, baseURI, userId, userPassword);
        } else {
            System.out.println(usage);
            System.exit(1);
        }
    }
}