package open.dolphin.mbean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.infomodel.StateMsgModel;

/**
 * カルテ状態とメッセージリストを保持するクラス
 * @author masuda, Masuda Naika
 */
public class FacilityContext {

    // 現在のStateMsgModel番号
    private int currentId;
    // StameMsgListのリスト
    private final List<StateMsgModel> stateMsgList;
    // 施設の待合リスト
    private final List<PatientVisitModel> pvtList;
    // カルテを開いている患者の、ptPkとownerUUIDのマップ
    private final Map<Long, String> patientMap;

    public FacilityContext() {
        currentId = 0;
        stateMsgList = new CopyOnWriteArrayList<StateMsgModel>();
        pvtList = new CopyOnWriteArrayList<PatientVisitModel>();
        patientMap = new ConcurrentHashMap<Long, String>();
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
    
    public Map<Long, String> getPtPkOwnerMap() {
        return patientMap;
    }
    
    public int getCurrentId() {
        return currentId;
    }
    
    public int getNextId() {
        return ++currentId;
    }
}

