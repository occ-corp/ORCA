package open.dolphin.ra.pvt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.resource.spi.work.Work;

/**
 *　PvtClaimListenWork
 * @author masuda, Masuda Naika
 */
public class PvtClaimListenWork implements Work {
    
    private static final int DEFAULT_CLAIM_LISTEN_PORT = 5002;
    
    private static final Logger logger = Logger.getLogger(PvtClaimListenWork.class.getSimpleName());
    private static final boolean DEBUG = false;

    private int claimListenPort = DEFAULT_CLAIM_LISTEN_PORT;
    private ServerSocketChannel ssc = null;
    
    private Selector selector = null;
    private boolean isRunning = false;

    private PvtClaimResourceAdapter ra;

    public void setClaimListenPort(int port) {
        claimListenPort = port;
    }
    
    public int getClaimListenPort() {
        return claimListenPort;
    }
    
    public PvtClaimListenWork(PvtClaimResourceAdapter ra) {
        this.ra = ra;
        initialize();
    }
    
    private void initialize() {

        try {
            // ソケットチャネルを生成・設定
            ssc = ServerSocketChannel.open();
            ssc.socket().setReuseAddress(true);
            // アドレスを取得・設定
            InetAddress lh = InetAddress.getLocalHost();
            InetSocketAddress isa = new InetSocketAddress(lh, getClaimListenPort());
            ssc.socket().bind(isa);
            // ノンブロッキングモードに設定
            ssc.configureBlocking(false);
            // セレクタの生成
            selector = Selector.open();
            // ソケットチャネルをセレクタに登録
            ssc.register(selector, SelectionKey.OP_ACCEPT, new PvtClaimAcceptHandler(PvtClaimListenWork.this));
        } catch (IOException ex) {
            debug(ex.toString());
        }
    }

    @Override
    public void run() {

        isRunning = true;
        
        try {
            while (isRunning && selector.select() > 0) {
                for (Iterator<SelectionKey> itr = selector.selectedKeys().iterator(); itr.hasNext();) {
                    SelectionKey key = itr.next();
                    itr.remove();
                    // アタッチしたオブジェクトに処理を委譲
                    IHandler handler = (IHandler) key.attachment();
                    handler.handle(key);
                }
            }
        } catch (ClosedChannelException ex) {
            debug("ソケットがクローズしています:" + ex);
        } catch (IOException ex) {
            debug("通信エラーが発生しました" + ex);
        } finally {
            try {
                for (SelectionKey key : selector.keys()) {
                    key.channel().close();
                }
            } catch (IOException ex) {
                debug(ex.toString());
            }
        }
    }

    @Override
    public void release() {
        isRunning = false;
        selector.wakeup();
    }
    
    public void postPvt(String pvtXml) {
        ra.postPvt(pvtXml);
    }
    
    private void debug(String msg) {
        if (DEBUG) {
            logger.info(msg);
        }
    }
}
