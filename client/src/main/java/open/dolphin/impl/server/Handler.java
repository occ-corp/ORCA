
package open.dolphin.impl.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

/**
 *
 * @author masuda
 */
public interface Handler {
    
    public void handle(SelectionKey key) throws ClosedChannelException, IOException;
    
}
