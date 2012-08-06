package open.dolphin.impl.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import open.dolphin.client.ClientContext;

/**
 * PvtClaimIOHandler
 *
 * @author masuda, Masuda Naika
 * http://itpro.nikkeibp.co.jp/article/COLUMN/20060515/237871/
 */
public class PvtClaimIOHandler implements Handler {

    private PVTClientServer context;
    private StringBuffer sb;        // thread safeにしたほうがよいか？
    private Charset charset;

    public PvtClaimIOHandler(PVTClientServer context) {
        this.context = context;
        sb = new StringBuffer();
        charset = Charset.forName(context.getEncoding());
    }

    @Override
    public void handle(SelectionKey key) throws ClosedChannelException, IOException {

        // 読みこみ可であれば、読みこみを行う
        if (key.isValid() && key.isReadable()) {
            read(key);
        }
        // 書きこみ可であれば、書きこみを行う
        if (key.isValid() && key.isWritable()) {
            write(key);
        }
    }

    private void read(SelectionKey key) {

        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(16384);

        try {
            int lastBytePos = channel.read(byteBuffer) - 1;
            int c = byteBuffer.get(lastBytePos);
            if (c == PVTClientServer.EOT) {
                // EOTを除いて書き出す
                byteBuffer.rewind();
                byteBuffer.limit(lastBytePos);
                sb.append(charset.decode(byteBuffer).toString());
                // writableにしてACKを返せるようにする
                key.interestOps(SelectionKey.OP_WRITE);
            } else {
                // byteBuffer全部書き出す
                byteBuffer.flip();
                sb.append(charset.decode(byteBuffer).toString());
            }

        } catch (IOException ex) {
            ClientContext.getPvtLogger().warn("IOException while reading streams");
            ClientContext.getPvtLogger().warn("Exception details:" + ex);
        }
    }

    private void write(SelectionKey key) {

        SocketChannel channel = (SocketChannel) key.channel();

        try {
            // 取得したxmlをPVT登録キューに送る
            String pvtXml = sb.toString();
            context.putPvt(pvtXml);
            // ACKを返す
            channel.write(ByteBuffer.wrap(new byte[]{PVTClientServer.ACK}));

        } catch (IOException ex) {
            ClientContext.getPvtLogger().warn("Exception while sending ACK:" + ex);
        } finally {
            try {
                // close channel
                channel.close();
            } catch (IOException ex) {
                ClientContext.getPvtLogger().warn("Exception while closing channel:" + ex);
            }
        }
    }
}
