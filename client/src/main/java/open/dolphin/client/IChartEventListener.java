package open.dolphin.client;

import open.dolphin.infomodel.ChartEventModel;

/**
 * IChartEventListener
 * @author masuda, Masuda Naika
 */
public interface IChartEventListener {

    // 変更処理
    public void onEvent(ChartEventModel evt);
}
