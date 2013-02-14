package open.dolphin.impl.claim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import open.dolphin.client.ClaimMessageEvent;

/**
 * ClaimIOHandler
 * @author masuda, Masuda Naika
 */
public class ClaimIOHandler {
 
    // Socket constants
    private static final byte EOT = 0x04;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    
    private ClaimMessageEvent evt;
    private ByteBuffer writeBuffer;

    
    public ClaimIOHandler(ClaimMessageEvent evt, String encoding) {
        
        this.evt = evt;

        try {
            byte[] bytes = evt.getClaimInsutance().getBytes(encoding);
            writeBuffer = ByteBuffer.allocate(bytes.length + 1);
            writeBuffer.put(bytes);
            writeBuffer.put(EOT);
            writeBuffer.flip();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace(System.err);
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
            throw new ClaimException(ClaimException.ERROR_CODE.IO_ERROR, evt);
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
            throw new ClaimException(ClaimException.ERROR_CODE.IO_ERROR, evt);
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
        throw new ClaimException(errorCode, evt);
    }
}
