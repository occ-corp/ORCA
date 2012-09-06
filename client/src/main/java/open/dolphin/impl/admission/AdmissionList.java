package open.dolphin.impl.admission;

import java.util.List;
import javax.swing.ImageIcon;
import open.dolphin.client.AbstractMainComponent;
import open.dolphin.client.ChartStateListener;
import open.dolphin.client.ClientContext;
import open.dolphin.impl.pvt.WatingListImpl;
import open.dolphin.infomodel.ChartStateMsgModel;
import open.dolphin.infomodel.PatientModel;

/**
 * 入院患者リスト
 * @author masuda, Masuda Naika
 */
public class AdmissionList extends AbstractMainComponent {
    
    // Window Title
    private static final String NAME = "入院リスト";
    
    // 来院テーブルのカラム名
    private static final String[] COLUMN_NAMES = {
        "部屋", "患者ID",  "氏   名", "性別",  "生年月日", "担当医", "診療科", "入院日", "状態"};
    // 来院テーブルのカラムメソッド
    private static final String[] PROPERTY_NAMES = {
        "getRoom", "getPatientId", "getPatientName", "getPatientGenderDesc", 
        "getPatientAgeBirthday", "getDoctorName", "getDeptName", "getAdmissionDate", 
        "getStateInteger"};
    // 来院テーブルのクラス名
    private static final Class[] COLUMN_CLASSES = {
        String.class, String.class, String.class, String.class, String.class, 
        String.class, String.class, String.class, Integer.class};
    // 来院テーブルのカラム幅
    private static final int[] COLUMN_WIDTH = {
        20, 80, 130, 40, 100, 100, 50, 80, 30};
    
    // Status　情報　メインウィンドウの左下に表示される内容
    private String statusInfo;
    
    // ネットワークアイコン
    private static final ImageIcon NETWORK_ICON = ClientContext.getImageIcon("ntwrk_16.gif");
    
    // このクライアントのUUID
    private String clientUUID;
    
    private List<PatientModel> patientList;
    
    private AdmissionListView view;
    
    public AdmissionList() {
        setName(NAME);
    }

    @Override
    public void start() {
        setup();
        initComponents();
        connect();
        startSyncMode();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stateChanged(List<ChartStateMsgModel> msgList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateLocalState(ChartStateMsgModel msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    // comet long polling機能を設定する
    private void startSyncMode() {
        setStatusInfo();
        getAdmittedPatients();
        ChartStateListener.getInstance().addListener(this);
        enter();
    }
    
    private void setup() {
        
    }
    private void initComponents() {
        
    }
    private void connect() {
        
    }
    
    private void setStatusInfo() {
        
    }
    
    private void getAdmittedPatients() {
        
    }
    
}
