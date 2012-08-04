
package open.dolphin.impl.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * PvtClaimAcceptHandler
 * 
 * @author masuda, Masuda Naika
 */
public class PvtClaimAcceptHandler implements Handler {
    
    private PVTClientServer context;
    
    public PvtClaimAcceptHandler(PVTClientServer context) {
        this.context = context;
    }

    @Override
    public void handle(SelectionKey key) throws ClosedChannelException, IOException {
        
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();

        // アクセプト処理
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);

        // 入出力用のハンドラを生成し、アタッチする
        // 監視する操作は読み込みのみ
        PvtClaimIOHandler handler = new PvtClaimIOHandler(context);
        channel.register(key.selector(), SelectionKey.OP_READ, handler);
    }
}
