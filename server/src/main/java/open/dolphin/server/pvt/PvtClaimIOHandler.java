package open.dolphin.server.pvt;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * PvtClaimIOHandler
 * @author masuda, Masuda Naika
 */
public class PvtClaimIOHandler implements IHandler {
    
    private static final int EOT = 0x04;
    private static final int ACK = 0x06;
    //private static final int NAK = 0x15;
    private static final String encoding = "UTF-8";

    private static final int bufferSize = 8192;
    private ByteBuffer byteBuffer;
    
    private ByteArrayOutputStream baos;
    private BufferedOutputStream bos;
    
    private PvtServletServer server;
   
    private static final Logger logger = Logger.getLogger(PvtClaimIOHandler.class.getSimpleName());
    private static final boolean DEBUG = false;

    public PvtClaimIOHandler(PvtServletServer server) {
        
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
            int c = byteBuffer.get(readLen - 1);
            if (c == EOT) {
                // EOTを除いて書き出す
                bos.write(byteBuffer.array(), 0, readLen - 1);
                // writableにしてACKを返せるようにする
                key.interestOps(SelectionKey.OP_WRITE);
            } else {
                // byteBuffer全部書き出す
                bos.write(byteBuffer.array(), 0, readLen);
            }

        } catch (IOException ex) {
            debug("IOException while reading streams:" + ex);
        }
    }

    private void write(SelectionKey key) {

        SocketChannel channel = (SocketChannel) key.channel();

        try {
            // 取得したxmlをPVT登録キューに送る
            bos.flush();
            String pvtXml = baos.toString(encoding);
            // PvtClaimListenWorkに登録処理をさせる
            server.postPvt(pvtXml);
            
            // ACKを返す
            channel.write(ByteBuffer.wrap(new byte[]{ACK}));

        } catch (IOException ex) {
            debug("Exception while sending ACK:" + ex);
        } finally {
            try {
                // close channel
                channel.close();
                // close stream
                bos.close();
                baos.close();
            } catch (IOException ex) {
                debug("Exception while closing channel:" + ex);
            }
        }
    }
    
    private void debug(String msg) {
        if (DEBUG) {
            logger.info(msg);
        }
    }
}
