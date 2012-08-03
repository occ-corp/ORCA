package open.dolphin.impl.server;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.text.DateFormat;
import java.util.Date;
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

            ClientContext.getPvtLogger().info("PVT Server is binded " + address + " with encoding: " + encoding);

            serverThread = new Thread(new ServerThread(address), "PVT server socket");
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
        
        private InetSocketAddress address;
        private ServerSocketChannel ssc = null;
        private Selector selector = null;
        private boolean isRunning = false;
        private ServerThread(InetSocketAddress address) {
            this.address = address;
            initialize();
        }
        
        private void initialize() {
            try {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                ssc.socket().bind(address);
                selector = SelectorProvider.provider().openSelector();
                ssc.register(selector, SelectionKey.OP_ACCEPT);
            } catch (IOException ex) {
            }
        }
        
        private void setRunning(boolean b) {
            isRunning = b;
        }
        
        private boolean isRunning() {
            return isRunning;
        }
        
        private void dispose() throws IOException {
            isRunning = false;
            ssc.close();
        }

        @Override
        public void run() {

            setRunning(true);

            while (isRunning()) {
                try {
                    if (selector.select() > 0) {
                        for (Iterator<SelectionKey> itr = selector.selectedKeys().iterator(); itr.hasNext();) {
                            SelectionKey sk = itr.next();
                            itr.remove();
                            // The key contains the channel ready for accept
                            ServerSocketChannel ready = (ServerSocketChannel) sk.channel();
                            // accept the incoming socket
                            SocketChannel channel = ready.accept();
                            // serve the client in a separate thread
                            exec.submit(new SocketReadTask(channel));
                        }
                    }
                } catch (IOException ex) {
                    break;
                }
            }
        }
    }

    private final class SocketReadTask implements Runnable {

        private SocketChannel channel;

        private SocketReadTask(SocketChannel channel) {
            this.channel = channel;
        }

        private void printInfo() throws IOException {
            String addr = channel.getRemoteAddress().toString();
            String time = DateFormat.getDateTimeInstance().format(new Date());
            ClientContext.getPvtLogger().info("Connected from " + addr + " at " + time);
        }

        @Override
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(16384);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(baos);

            try {
                printInfo();

                while (true) {
                    int readLen = channel.read(buffer);
                    if (readLen == -1) {
                        if (DEBUG) {
                            ClientContext.getPvtLogger().debug("EOF");
                        }
                        break;
                    }
                    int pos = buffer.position();
                    byte b = buffer.get(pos - 1);

                    if (b == EOT) {
                        buffer.position(pos - 1);
                        buffer.flip();
                        bos.write(buffer.array());
                        bos.flush();
                        String received = baos.toString(encoding);
                        if (DEBUG) {
                            ClientContext.getPvtLogger().info("Recieved EOT length = " + received.length() + " bytes");
                            ClientContext.getPvtLogger().debug(received);
                        }
                        // add queue
                        processPvt(received);
                        // Reply ACK
                        if (DEBUG) {
                            ClientContext.getPvtLogger().debug("return ACK");
                        }
                        channel.write(ByteBuffer.wrap(new byte[]{ACK}));
                    } else {
                        buffer.flip();
                        bos.write(buffer.array());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            } finally {
                try {
                    baos.close();
                    bos.close();
                    channel.finishConnect();
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
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
