package open.dolphin.server.pvt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * PvtServerThread
 *
 * @author masuda, Masuda Naika
 */
public class PvtServerThread implements Runnable {

    private static final Logger logger = Logger.getLogger(PvtServerThread.class.getSimpleName());
    
    private ServerSocketChannel ssc;
    private Selector selector;
    
    private boolean isRunning;

    public PvtServerThread(InetSocketAddress address) throws IOException {
        initialize(address);
        isRunning = true;
    }

    private void initialize(InetSocketAddress address) throws IOException {

        // ソケットチャネルを生成・設定
        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        ssc.socket().bind(address);
        // ノンブロッキングモードに設定
        ssc.configureBlocking(false);
        // セレクタの生成
        selector = Selector.open();
        // ソケットチャネルをセレクタに登録
        ssc.register(selector, SelectionKey.OP_ACCEPT, new PvtClaimAcceptHandler());
    }

    @Override
    public void run() {

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
            logger.warning("Socket was already closed.");
        } catch (IOException ex) {
            logger.warning("I/O error occured.");
        } finally {
            for (SelectionKey key : selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException ex) {
                    //ex.printStackTrace(System.err);
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        selector.wakeup();
    }
}
