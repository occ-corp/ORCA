package open.dolphin.impl.server;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import open.dolphin.client.ClientContext;

/**
 * PvtClaimIOHandler
 *
 * @author masuda, Masuda Naika
 * http://itpro.nikkeibp.co.jp/article/COLUMN/20060515/237871/
 */
public class PvtClaimIOHandler implements Handler {

    private PVTClientServer context;
    private ByteArrayOutputStream baos;
    private BufferedOutputStream bos;

    public PvtClaimIOHandler(PVTClientServer context) {
        this.context = context;
        baos = new ByteArrayOutputStream();
        bos = new BufferedOutputStream(baos);
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
                bos.write(byteBuffer.array());
                // writableにしてACKを返せるようにする
                key.interestOps(SelectionKey.OP_WRITE);
            } else {
                // byteBuffer全部書き出す
                byteBuffer.flip();
                bos.write(byteBuffer.array());
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
            baos.flush();
            String pvtXml = baos.toString(context.getEncoding());
            context.putPvt(pvtXml);
            // ACKを返す
            channel.write(ByteBuffer.wrap(new byte[]{PVTClientServer.ACK}));

        } catch (IOException ex) {
            ClientContext.getPvtLogger().warn("Exception while sending ACK:" + ex);
        } finally {
            try {
                // close channel
                channel.close();
                // close stream
                bos.close();
                baos.close();
            } catch (IOException ex) {
                ClientContext.getPvtLogger().warn("Exception while closing channel:" + ex);
            }
        }
    }
}
