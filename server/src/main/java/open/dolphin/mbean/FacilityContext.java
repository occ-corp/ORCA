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

    private int currentId;
    private final List<StateMsgModel> stateMsgList;
    private final List<PatientVisitModel> pvtList;

    public FacilityContext() {
        currentId = 0;
        stateMsgList = new CopyOnWriteArrayList<StateMsgModel>();
        pvtList = new CopyOnWriteArrayList<PatientVisitModel>();
    }
    
    public List<StateMsgModel> getStateMsgList() {
        return stateMsgList;
    }
    
    public void clearStateMsgList() {
        stateMsgList.clear();
        currentId = 0;
    }
    
    public List<PatientVisitModel> getPvtList() {
        return pvtList;
    }
    
    public int getCurrentId() {
        return currentId;
    }
    
    public int getNextId() {
        return ++currentId;
    }
}

