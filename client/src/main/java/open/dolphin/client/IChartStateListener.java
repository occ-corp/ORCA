package open.dolphin.client;

import java.util.List;
import open.dolphin.infomodel.ChartStateMsgModel;

/**
 * IChartStateListener
 * @author masuda, Masuda Naika
 */
public interface IChartStateListener {
    
    // サーバーからの状態通知を処理する
    public void stateChanged(List<ChartStateMsgModel> msgList);
    
    // クライアントローカルの変更処理
    public void updateLocalState(ChartStateMsgModel msg);
}
