package open.dolphin.client;

import java.util.Date;
import java.util.List;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.infomodel.StateMsgModel;
import open.dolphin.project.Project;

/**
 * AbstractStateListener
 * @author masuda, Masuda Naika
 */
public abstract class AbstractStateListener {
    
    protected String clientUUID;
    protected String orcaId;
    protected String deptCode;
    protected String departmentDesc;
    protected String doctorName;
    protected String userId;
    protected String jmariCode;
    
    public AbstractStateListener() {
        clientUUID = Dolphin.getInstance().getClientUUID();
        orcaId = Project.getUserModel().getOrcaId();
        deptCode = Project.getUserModel().getDepartmentModel().getDepartment();
        departmentDesc = Project.getUserModel().getDepartmentModel().getDepartmentDesc();
        doctorName = Project.getUserModel().getCommonName();
        userId = Project.getUserModel().getUserId();
        jmariCode = Project.getString(Project.JMARI_CODE);
    }
    
    // サーバーからの状態通知を処理する
    public final void processMessage(List<StateMsgModel> msgList) {
        // 自クライアント以外の変更が送られてくる
        if (msgList == null || msgList.isEmpty()) {
            return;
        }
        for (StateMsgModel msg : msgList) {
            stateChanged(msg);
        }
    }
    
    // 変更処理
    protected abstract void stateChanged(StateMsgModel msg);

    // FakePatientVisitModelを作る
    protected PatientVisitModel createFakePvt(PatientModel pm) {

        // 来院情報を生成する
        PatientVisitModel pvt = new PatientVisitModel();
        pvt.setId(0L);
        pvt.setPatientModel(pm);

        //--------------------------------------------------------
        // 受け付けを通していないのでログイン情報及び設定ファイルを使用する
        // 診療科名、診療科コード、医師名、医師コード、JMARI
        // 2.0
        //---------------------------------------------------------
        pvt.setDeptName(departmentDesc);
        pvt.setDeptCode(deptCode);
        pvt.setDoctorName(doctorName);
        if (orcaId != null) {
            pvt.setDoctorId(orcaId);
        } else {
            pvt.setDoctorId(userId);
        }
        pvt.setJmariNumber(jmariCode);
        
        // 来院日
        pvt.setPvtDate(ModelUtils.getDateTimeAsString(new Date()));
        return pvt;
    }
}
