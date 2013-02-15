package open.dolphin.client;

import java.beans.PropertyChangeListener;
import javax.swing.ImageIcon;

/**
 * IChartEventListener
 * @author masuda, Masuda Naika
 */
public interface IChartEventListener extends PropertyChangeListener {
    
    // オープンアイコン
    public static final ImageIcon OPEN_ICON = ClientContext.getImageIcon("open_16.gif");
    // ネットワークアイコン
    public static final ImageIcon NETWORK_ICON = ClientContext.getImageIcon("ntwrk_16.gif");
}
