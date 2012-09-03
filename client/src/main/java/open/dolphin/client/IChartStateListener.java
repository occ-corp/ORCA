package open.dolphin.client;

import java.util.List;
import open.dolphin.infomodel.ChartStateMsgModel;

/**
 *
 * @author masuda, Masuda Naika
 */
public interface IChartStateListener {
    
    public void stateChanged(List<ChartStateMsgModel> msgList);
    
}
