package open.dolphin.server.pvt;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

/**
 * IHanlder
 * @author masuda, Masuda Naika
 */
public interface IHandler {
    
    public void handle(SelectionKey key) throws ClosedChannelException, IOException;
    
}
