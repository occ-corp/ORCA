package open.dolphin.client;

import javax.swing.ImageIcon;
import open.dolphin.infomodel.ChartEventModel;

/**
 * IChartEventListener
 * @author masuda, Masuda Naika
 */
public interface IChartEventListener {
    
    // オープンアイコン
    public static final ImageIcon OPEN_ICON = ClientContext.getImageIcon("os_folder_blue_16.png");
    // ネットワークアイコン
    public static final ImageIcon NETWORK_ICON = ClientContext.getImageIcon("network-error-2_16.png");
    
    // 変更処理 
    public void onEvent(ChartEventModel evt) throws Exception;
}
