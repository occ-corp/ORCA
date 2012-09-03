package open.dolphin.mbean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import open.dolphin.infomodel.ChartStateMsgModel;
import open.dolphin.infomodel.PatientVisitModel;

/**
 * 施設の待合リストとChartStateMsgModelを保持するクラス
 * @author masuda, Masuda Naika
 */
public class FacilityContext {

    private int msgCounter;
    private final List<ChartStateMsgModel> chartStateMsgList;
    private final List<PatientVisitModel> pvtList;

    public FacilityContext() {
        msgCounter = 0;
        chartStateMsgList = new CopyOnWriteArrayList<ChartStateMsgModel>();
        pvtList = new CopyOnWriteArrayList<PatientVisitModel>();
    }
    
    public List<ChartStateMsgModel> getChartStateMsgList() {
        return chartStateMsgList;
    }
    
    public void clearChartStateMsgList() {
        chartStateMsgList.clear();
        msgCounter = 0;
    }
    
    public List<PatientVisitModel> getPvtList() {
        return pvtList;
    }
    
    public int getMsgCounter() {
        return msgCounter;
    }
    
    public void incrementMsgCounter() {
        msgCounter++;
    }
    
    public void cleanUpMsgList(int minId) {
        
        List<ChartStateMsgModel> toRemove = new ArrayList<ChartStateMsgModel>();
        for(ChartStateMsgModel msg : chartStateMsgList) {
            if (msg.getId() < minId) {
                toRemove.add(msg);
            }
        }
        chartStateMsgList.removeAll(toRemove);
    }
}

