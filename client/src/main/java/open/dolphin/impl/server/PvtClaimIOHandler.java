
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
        if (key.isReadable()) {
            read(key);
        }

        // 書きこみ可であれば、書きこみを行う
        if (key.isWritable() && key.isValid()) {
            write(key);
        }
    }
    
    private void read(SelectionKey key) {
        
        SocketChannel channel = (SocketChannel) key.channel();
        byte[] buffer = new byte[16384];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        
        try {
            int readLen = channel.read(byteBuffer);
            if (readLen < 1) {
                return;
            }
            if (buffer[readLen - 1] == PVTClientServer.EOT) {
                // EOTを除いてbosに書き出し
                bos.write(buffer, 0, readLen - 1);
                // writableにしてACKを返せるようにする
                key.interestOps(SelectionKey.OP_WRITE);
            } else {
                // bosに書き出し
                bos.write(buffer, 0, readLen);
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
                bos.close();
                baos.close();
            } catch (IOException ex) {
                ClientContext.getPvtLogger().warn("Exception while closing channel:" + ex);
            }
        }
    }
    
}
