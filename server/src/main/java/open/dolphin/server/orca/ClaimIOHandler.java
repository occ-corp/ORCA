package open.dolphin.server.orca;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import open.dolphin.infomodel.ClaimMessageModel;

/**
 * ClaimIOHandler
 * @author masuda, Masuda Naika
 */
public class ClaimIOHandler {
 
    private static final Logger logger = Logger.getLogger(ClaimIOHandler.class.getSimpleName());
    
    // Socket constants
    private static final byte EOT = 0x04;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    
    private AsyncContext ac;
    private ByteBuffer writeBuffer;

    
    public ClaimIOHandler(AsyncContext ac, String encoding) {
        
        this.ac = ac;

        try {
            ClaimMessageModel model = (ClaimMessageModel) 
                    ac.getRequest().getAttribute(ClaimMessageModel.class.getSimpleName());
            byte[] bytes = model.getContent().getBytes(encoding);
            writeBuffer = ByteBuffer.allocate(bytes.length + 1);
            writeBuffer.put(bytes);
            writeBuffer.put(EOT);
            writeBuffer.flip();
        } catch (UnsupportedEncodingException ex) {
            logger.warning(ex.getMessage());
        }
    }
    
    public void handle(SelectionKey key) throws ClaimException {

        if (key.isConnectable()) {
            doConnect(key);
        } else {
            if (key.isValid() && key.isReadable()) {
                doRead(key);
            }
            if (key.isValid() && key.isWritable()) {
                doWrite(key);
            }
        }
    }

    // 接続する
    private void doConnect(SelectionKey key) throws ClaimException {
        
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            key.interestOps(SelectionKey.OP_WRITE);
        } catch (IOException ex) {
            throw new ClaimException(ClaimException.ERROR_CODE.IO_ERROR, ac);
        }
    }
    
    // データを書き出す
    private void doWrite(SelectionKey key) throws ClaimException {

        SocketChannel channel = (SocketChannel) key.channel();

        try {
            channel.write(writeBuffer);
            if (writeBuffer.remaining() == 0) {
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (IOException ex) {
            throw new ClaimException(ClaimException.ERROR_CODE.IO_ERROR, ac);
        }
    }

    // 返事を受け取る
    private void doRead(SelectionKey key) throws ClaimException {

        ClaimException.ERROR_CODE errorCode;
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1);
            channel.read(byteBuffer);
            byteBuffer.flip();
            byte b = byteBuffer.get();

            switch (b) {
                case ACK:
                    errorCode = ClaimException.ERROR_CODE.NO_ERROR;
                    break;
                case NAK:
                    errorCode = ClaimException.ERROR_CODE.NAK_SIGNAL;
                    break;
                default:
                    errorCode = ClaimException.ERROR_CODE.IO_ERROR;
                    break;
            }
        } catch (IOException ex) {
            errorCode = ClaimException.ERROR_CODE.IO_ERROR;
        } finally {
            try {
                channel.close();
            } catch (IOException ex) {
                //errorCode = ClaimException.ERROR_CODE.IO_ERROR;
            }
        }
        // ClaimExceptionを投げて通信終了する
        throw new ClaimException(errorCode, ac);
    }
}
