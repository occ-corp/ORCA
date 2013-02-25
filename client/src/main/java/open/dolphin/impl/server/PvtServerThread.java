package open.dolphin.impl.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import open.dolphin.client.ClientContext;
import org.apache.log4j.Logger;

/**
 * PvtServerThread, client
 * @author masuda, Masuda Naika
 */
public class PvtServerThread implements Runnable {
    
    private static final Logger logger = ClientContext.getPvtLogger();

    private PVTClientServer server;

    private ServerSocketChannel ssc;
    private Selector selector;
    
    private boolean isRunning;
    
    public PvtServerThread(PVTClientServer server, InetSocketAddress address) throws IOException {
        this.server = server;
        initialize(address);
    }

    public void stop() {
        isRunning = false;
        selector.wakeup();
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
        ssc.register(selector, SelectionKey.OP_ACCEPT, new PvtClaimAcceptHandler(server));
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
            logger.warn("Socket was already closed.");
        } catch (IOException ex) {
            logger.warn("I/O error occured.");
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
