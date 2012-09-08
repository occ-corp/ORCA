package open.dolphin.mbean;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import open.dolphin.infomodel.PatientModel;
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
    // カルテを開いている患者リスト。ここに記録するのはidとownerUUIDだけのfake model
    private final Set<PatientModel> patientSet;

    public FacilityContext() {
        currentId = 0;
        stateMsgList = new CopyOnWriteArrayList<StateMsgModel>();
        pvtList = new CopyOnWriteArrayList<PatientVisitModel>();
        patientSet = new CopyOnWriteArraySet<PatientModel>();
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
    
    public Set<PatientModel> getPatientSet() {
        return patientSet;
    }
    
    public int getCurrentId() {
        return currentId;
    }
    
    public int getNextId() {
        return ++currentId;
    }
    
    // PatientModel.idからownerUUIDを取得する
    public String getPtOwnerUUID(long ptPk) {
        for (PatientModel pm : patientSet) {
            if (ptPk == pm.getId()) {
                return pm.getOwnerUUID();
            }
        }
        return null;
    }
}

