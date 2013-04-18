package open.dolphin.client;

import javax.swing.ImageIcon;
import open.dolphin.infomodel.ChartEventModel;

/**
 * IChartEventListener
 * @author masuda, Masuda Naika
 */
public interface IChartEventListener {
    
    // オープンアイコン
    public static final ImageIcon OPEN_ICON = ClientContext.getImageIconAlias("icon_karte_open_state_small");
    // ネットワークアイコン
    public static final ImageIcon NETWORK_ICON = ClientContext.getImageIconAlias("icon_karte_open_someone_small");
    
    // 変更処理 
    public void onEvent(ChartEventModel evt) throws Exception;
}
