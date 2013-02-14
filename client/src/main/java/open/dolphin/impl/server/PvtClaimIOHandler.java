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
 * PvtClaimIOHandler, client
 *
 * @author masuda, Masuda Naika
 * http://itpro.nikkeibp.co.jp/article/COLUMN/20060515/237871/
 */
public class PvtClaimIOHandler implements IHandler {
    
    private static final byte EOT = 0x04;
    private static final byte ACK = 0x06;
    //private static final byte NAK = 0x15;

    private static final int bufferSize = 8192;
    private ByteBuffer byteBuffer;
    
    private PVTClientServer server;
    private ByteArrayOutputStream baos;
    private BufferedOutputStream bos;
   

    public PvtClaimIOHandler(PVTClientServer server) {
        this.server = server;
        baos = new ByteArrayOutputStream();
        bos = new BufferedOutputStream(baos);
        byteBuffer = ByteBuffer.allocate(bufferSize);
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

        try {
            byteBuffer.clear();
            int readLen = channel.read(byteBuffer);
            byte b = byteBuffer.get(readLen - 1);
            if (b == EOT) {
                // EOTを除いて書き出す
                bos.write(byteBuffer.array(), 0, readLen - 1);
                // writableにしてACKを返せるようにする
                key.interestOps(SelectionKey.OP_WRITE);
            } else {
                // byteBuffer全部書き出す
                bos.write(byteBuffer.array(), 0, readLen);
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
            bos.flush();
            String pvtXml = baos.toString(server.getEncoding());
            server.putPvt(pvtXml);
            // ACKを返す
            channel.write(ByteBuffer.wrap(new byte[]{ACK}));

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
