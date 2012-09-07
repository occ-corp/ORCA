package open.dolphin.mbean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.infomodel.StateMsgModel;

/**
 * 施設の待合リストとChartStateMsgModelを保持するクラス
 * @author masuda, Masuda Naika
 */
public class FacilityContext {

    private int msgCounter;
    private final List<StateMsgModel> chartStateMsgList;
    private final List<PatientVisitModel> pvtList;

    public FacilityContext() {
        msgCounter = 0;
        chartStateMsgList = new CopyOnWriteArrayList<StateMsgModel>();
        pvtList = new CopyOnWriteArrayList<PatientVisitModel>();
    }
    
    public List<StateMsgModel> getStateMsgList() {
        return chartStateMsgList;
    }
    
    public void clearStateMsgList() {
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
        
        List<StateMsgModel> toRemove = new ArrayList<StateMsgModel>();
        for(StateMsgModel msg : chartStateMsgList) {
            if (msg.getId() <= minId) {
                toRemove.add(msg);
            }
        }
        chartStateMsgList.removeAll(toRemove);
    }
}

