package open.dolphin.ra.pvt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PvtClaimAcceptHandler
 *
 * @author masuda, Masuda Naika
 * http://itpro.nikkeibp.co.jp/article/COLUMN/20060515/237871/
 */
public class PvtClaimAcceptHandler implements IHandler {

    private static final Logger logger = Logger.getLogger(PvtClaimAcceptHandler.class.getSimpleName());

    private PvtClaimListenWork work;

    public PvtClaimAcceptHandler(PvtClaimListenWork work) {
        this.work = work;
    }

    @Override
    public void handle(SelectionKey key) throws ClosedChannelException, IOException {

        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();

        // アクセプト処理
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);

        // 入出力用のハンドラを生成し、アタッチする
        // 監視する操作は読み込みのみ
        PvtClaimIOHandler handler = new PvtClaimIOHandler(work);
        channel.register(key.selector(), SelectionKey.OP_READ, handler);

        // ログ出力
        String addr = ((InetSocketAddress) channel.getRemoteAddress()).getAddress().getHostAddress();
        String time = DateFormat.getDateTimeInstance().format(new Date());
        logger.log(Level.INFO, "PVT CLAIM Connected from {0} at {1}", new Object[]{addr, time});
    }

}
