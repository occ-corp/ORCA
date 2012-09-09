package open.dolphin.client;

import open.dolphin.infomodel.ChartEvent;

/**
 * IChartEventListener
 * @author masuda, Masuda Naika
 */
public interface IChartEventListener {

    // 変更処理
    public void onEvent(ChartEvent evt);
}
