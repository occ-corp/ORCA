package open.dolphin.impl.server;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.Date;
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

/**
 * PVT socket server
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class PVTClientServer implements PVTServer {

    public static final int EOT = 0x04;
    public static final int ACK = 0x06;
    public static final int NAK = 0x15;
    public static final String UTF8 = "UTF8";
    public static final String SJIS = "SHIFT_JIS";
    public static final String EUC = "EUC_JIS";
    private static final int DEFAULT_PORT = 5002;
    
    private String encoding = UTF8;
    private int port = DEFAULT_PORT;
    
    // バインドアドレス
    private String bindAddress;
    // ServerSocket
    private ServerSocket serverSocket;
    // ServerSocketのループスレッド
    private Thread serverThread;
    // PVT登録処理のSingle Thread Executor
    private ExecutorService exec;
    
    private MainWindow context;
    private String name;
    private UserModel user;
    private boolean DEBUG;

    /**
     * Creates new ClaimServer
     */
    public PVTClientServer() {
        DEBUG = (ClientContext.getPvtLogger().getLevel() == Level.DEBUG);
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
                if (DEBUG) {
                    ClientContext.getPvtLogger().debug("PVT ServerSocket bind address = " + getBindAddress());
                }
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

            serverSocket = new ServerSocket();
            serverSocket.bind(address);
            ClientContext.getPvtLogger().info("PVT Server is binded " + address + " with encoding: " + encoding);

            serverThread = new Thread(new ServerThread(), "PVT server socket");
            serverThread.start();

        } catch (IOException e) {
            e.printStackTrace(System.err);
            ClientContext.getPvtLogger().warn("IOException while creating the ServerSocket: " + e.toString());
        }
    }

    /**
     * 受付受信サーバをストップする。
     */
    public void stopService() {

        // SocketExceptionを起こしてserverThreadを中止させる
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace(System.err);
                ClientContext.getPvtLogger().warn(e);
            }
        }

        // ServerSocketのThread破棄する
        try {
            serverThread.interrupt();
            serverThread = null;
        } catch (Exception e) {
        }

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

        @Override
        public void run() {

            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    SocketReadTask task = new SocketReadTask(socket);
                    exec.submit(task);
                }
            } catch (SocketException ex) {
                // serverSocketが閉じられるとSocketExceptionが起こりループから脱出できる
                ClientContext.getPvtLogger().info("PVTServer stopped");
            } catch (InterruptedIOException ex) {
                ex.printStackTrace(System.err);
                ClientContext.getPvtLogger().info("PVTServer stopped");
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                ClientContext.getPvtLogger().warn("Exception while listening for connections:" + ex);
            }
        }
    }

    private final class SocketReadTask implements Runnable {

        private Socket socket;

        private SocketReadTask(Socket socket) {
            this.socket = socket;
        }

        private void printInfo() {
            String addr = socket.getInetAddress().getHostAddress();
            String time = DateFormat.getDateTimeInstance().format(new Date());
            ClientContext.getPvtLogger().info("Connected from " + addr + " at " + time);
        }

        @Override
        public void run() {

            try {
                printInfo();

                BufferedInputStream reader = new BufferedInputStream(new DataInputStream(socket.getInputStream()));
                BufferedOutputStream writer = new BufferedOutputStream(new DataOutputStream(socket.getOutputStream()));

                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                BufferedOutputStream buf = new BufferedOutputStream(bo);

                byte[] buffer = new byte[16384];

                while (true) {

                    int readLen = reader.read(buffer);

                    if (readLen == -1) {
                        if (DEBUG) {
                            ClientContext.getPvtLogger().debug("EOF");
                        }
                        break;
                    }

                    if (buffer[readLen - 1] == EOT) {
                        buf.write(buffer, 0, readLen - 1);
                        buf.flush();
                        String recieved = bo.toString(encoding);
                        int len = recieved.length();
                        bo.close();
                        buf.close();
                        if (DEBUG) {
                            ClientContext.getPvtLogger().info("Recieved EOT length = " + len + " bytes");
                            ClientContext.getPvtLogger().debug(recieved);
                        }

                        // add queue
                        processPvt(recieved);

                        // Reply ACK
                        if (DEBUG) {
                            ClientContext.getPvtLogger().debug("return ACK");
                        }
                        writer.write(ACK);
                        writer.flush();

                    } else {
                        buf.write(buffer, 0, readLen);
                    }
                }

                reader.close();
                writer.close();
                socket.close();
                socket = null;

            } catch (IOException e) {
                ClientContext.getPvtLogger().warn("IOException while reading streams");
                ClientContext.getPvtLogger().warn("Exception details:" + e);

            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                        socket = null;
                    } catch (IOException e2) {
                        ClientContext.getPvtLogger().warn("Exception while closing socket conenction after reading streams");
                        ClientContext.getPvtLogger().warn("Exception details:" + e2);
                    }
                }
            }
        }
    }

    private void processPvt(final String pvtXml) {

        BufferedReader r = new BufferedReader(new StringReader(pvtXml));
        PVTBuilder builder = new PVTBuilder();
        builder.parse(r);
        PatientVisitModel model = builder.getProduct();

        //FEV-70に患者情報を送る
        SendPatientInfoToFEV.send(model);
        // シングルトン化
        PVTDelegater pdl = PVTDelegater.getInstance();

        pdl.addPvt(model);
    }
}