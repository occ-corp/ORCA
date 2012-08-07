
package open.dolphin.impl.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

/**
 * IHanlder
 * @author masuda, Masuda Naika
 * http://itpro.nikkeibp.co.jp/article/COLUMN/20060515/237871/
 */
public interface IHandler {
    
    public void handle(SelectionKey key) throws ClosedChannelException, IOException;
    
}
