package open.dolphin.client;

import javax.swing.ImageIcon;
import open.dolphin.infomodel.ChartEventModel;

/**
 * IChartEventListener
 * @author masuda, Masuda Naika
 */
public interface IChartEventListener {
    
    // オープンアイコン
    public static final ImageIcon OPEN_ICON = ClientContext.getImageIcon("open_16.gif");
    // ネットワークアイコン
    public static final ImageIcon NETWORK_ICON = ClientContext.getImageIcon("ntwrk_16.gif");
    
    // 変更処理 
    public void onEvent(ChartEventModel evt) throws Exception;
}
