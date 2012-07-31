package open.dolphin.setting;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import open.dolphin.client.AutoRomanListener;
import open.dolphin.client.ClientContext;
import open.dolphin.client.GUIFactory;
import open.dolphin.client.RegexConstrainedDocument;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.helper.GridBagBuilder;
import open.dolphin.project.Project;
import open.dolphin.project.ProjectStub;

/**
 * ClaimSettingPanel
 *
 * @author Kazushi Minagawa Digital Globe, Inc.
 * @author pns
 * @author modified by masuda, Masuda Naika
 *
 */
public class ClaimSettingPanel extends AbstractSettingPanel {
    
    private static final String ID = "claimSetting";
    private static final String TITLE = "レセコン";
    private static final String ICON = "calc_16.gif";
    
    // GUI staff
    private JRadioButton sendClaimYes;
    private JRadioButton sendClaimNo;
    private JComboBox claimHostCombo;
    private JCheckBox claim01;
    private JTextField jmariField;
    private JTextField claimAddressField;
    private JTextField claimPortField;
    private JCheckBox useAsPVTServer;
    private JTextField bindAddress;

    // ORCA API
    private JRadioButton useOrcaApi;
    private JRadioButton useClaim;
    private JTextField orcaUserIdField;
    private JPasswordField orcaPasswordField;
    private JTextField orcaStaffCodeField;
    private JButton orcaStaffCodeButton;
    
    /** 画面モデル */
    private ClaimModel model;
    
    private StateMgr stateMgr;
    
    
    public ClaimSettingPanel() {
        this.setId(ID);
        this.setTitle(TITLE);
        this.setIcon(ICON);
    }
    
    /**
     * GUI 及び State を生成する。
     */
    @Override
    public void start() {
        
        //
        // モデルを生成し初期化する
        //
        model = new ClaimModel();
        model.populate(getProjectStub());
        
        //
        // GUIを構築する
        //
        initComponents();
        
        //
        // bind する
        //
        bindModelToView();
    }
    
    /**
     * 設定値を保存する。
     */
    @Override
    public void save() {
        bindViewToModel();
        model.restore(getProjectStub());
    }
    
    /**
     * GUIを構築する
     */
    private void initComponents() {
        
        // 診療行為送信ボタン
        ButtonGroup bg1 = new ButtonGroup();
        sendClaimYes = GUIFactory.createRadioButton("送信する", null, bg1);
        sendClaimNo = GUIFactory.createRadioButton("送信しない", null, bg1);

//pns^
        // ORCA API
        ButtonGroup bg2 = new ButtonGroup();
        useClaim = GUIFactory.createRadioButton("CLAIM", null, bg2);
        useOrcaApi = GUIFactory.createRadioButton("ORCA API", null, bg2);
        orcaUserIdField = new JTextField(10);
        orcaPasswordField = new JPasswordField(10);
        orcaStaffCodeField = new JTextField(10);
        orcaStaffCodeButton = new JButton("コード検索");
        orcaStaffCodeButton.addActionListener(new ActionListener(){
            /**
             * Orca Dao を使って，職員コードを検索する
             */
            @Override
            public void actionPerformed(ActionEvent ev) {
                orcaStaffCodeField.setText("");
                SqlMiscDao dao = SqlMiscDao.getInstance();
                String orcaStaffCode = dao.getOrcaStaffCode(orcaUserIdField.getText().trim());
                orcaStaffCodeField.setText(orcaStaffCode);
            }
        });
//pns$
        
        // 01 小児科等
        claim01 = new JCheckBox("デフォルト01を使用");
        
        // JMARI、ホスト名、アドレス、ポート番号
        String[] hostNames = ClientContext.getStringArray("settingDialog.claim.hostNames");
        claimHostCombo = new JComboBox(hostNames);
        jmariField = GUIFactory.createTextField(10, null, null, null);
        jmariField.setToolTipText("医療機関コードの数字部分のみ12桁を入力してください。");
        claimAddressField = GUIFactory.createTextField(12, null, null, null);
        claimPortField = GUIFactory.createTextField(5, null, null, null);
        
        // 受付受信ボタン
        useAsPVTServer = GUIFactory.createCheckBox("このマシンでORCAからの受付情報を受信する", null);
        useAsPVTServer.setToolTipText("このマシンでORCAからの受付情報を受信する場合はチェックしてください");
        bindAddress = GUIFactory.createTextField(12, null, null, null);
        bindAddress.setToolTipText("複数ネットワークカードがある場合、受付受信サーバのバインドアドレスを入力してください");
        
        // CLAIM（請求）送信情報
        GridBagBuilder gbl = new GridBagBuilder("CLAIM（請求データ）送信");
        int row = 0;
        JLabel label = new JLabel("診療行為送信:");
        JPanel panel = GUIFactory.createRadioPanel(new JRadioButton[]{sendClaimYes,sendClaimNo});
        gbl.add(label, 0, row, GridBagConstraints.EAST);
        gbl.add(panel, 1, row, GridBagConstraints.CENTER);
        JPanel sendClaim = gbl.getProduct();
        
        // レセコン情報
//pns^
        //gbl = new GridBagBuilder("レセコン情報");
        //row = 0;
        //label = new JLabel("機種:");
        //gbl.add(label,          0, row, GridBagConstraints.EAST);
        //gbl.add(claimHostCombo, 1, row, GridBagConstraints.WEST);
        //row++;
        
        row = 0;
        gbl = new GridBagBuilder("ORCA通信情報");
        label = new JLabel("通信方法:");
        JPanel vPanel = GUIFactory.createRadioPanel(new JRadioButton[]{useClaim, useOrcaApi});
        gbl.add(label,  0, row, GridBagConstraints.EAST);
        gbl.add(vPanel, 1, row, GridBagConstraints.WEST);
        
        row++;
        label = new JLabel("ORCA ログインID:");
        gbl.add(label,  0, row, GridBagConstraints.EAST);
        gbl.add(orcaUserIdField,1, row, GridBagConstraints.WEST);
        
        row++;
        label = new JLabel("ORCA パスワード:");
        gbl.add(label,  0, row, GridBagConstraints.EAST);
        gbl.add(orcaPasswordField,1, row, GridBagConstraints.WEST);
                
        row++;
        label = new JLabel("ORCA 職員コード:");
        JPanel doctorPanel = new JPanel();
        doctorPanel.setLayout(new BoxLayout(doctorPanel, BoxLayout.X_AXIS));
        doctorPanel.add(orcaStaffCodeField);
        doctorPanel.add(orcaStaffCodeButton);
        gbl.add(label,  0, row, GridBagConstraints.EAST);
        gbl.add(doctorPanel,1, row, GridBagConstraints.WEST);
        
        row++;
        label = new JLabel("CLAIM診療科コード:");
        gbl.add(label,  0, row, GridBagConstraints.EAST);
        gbl.add(claim01,1, row, GridBagConstraints.WEST);
        
        row++;
        label = new JLabel("医療機関ID:  JPN");
        gbl.add(label,      0, row, GridBagConstraints.EAST);
        gbl.add(jmariField, 1, row, GridBagConstraints.WEST);
        
        row++;
        label = new JLabel("IPアドレス:");
        gbl.add(label,             0, row, GridBagConstraints.EAST);
        gbl.add(claimAddressField, 1, row, GridBagConstraints.WEST);
        
        row++;
        label = new JLabel("ポート番号:");
        gbl.add(label,          0, row, GridBagConstraints.EAST);
        gbl.add(claimPortField, 1, row, GridBagConstraints.WEST);
        JPanel port = gbl.getProduct();
        
        // レセコンからの受付受信
        gbl = new GridBagBuilder("受付情報の受信");
        label = new JLabel("バインドアドレス(オプション):");
        gbl.add(useAsPVTServer, 0, 0, 2, 1, GridBagConstraints.EAST);
        gbl.add(label,          0, 1, GridBagConstraints.EAST);
        gbl.add(bindAddress,    1, 1, GridBagConstraints.WEST);
        JPanel pvt = gbl.getProduct();
        
        // 全体レイアウト
        gbl = new GridBagBuilder();
        gbl.add(sendClaim, 0, 0, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(port,      0, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(pvt,       0, 2, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(new JLabel(""), 0, 3, GridBagConstraints.BOTH,  1.0, 1.0);
        setUI(gbl.getProduct());

        connect();       
    }
    
    /**
     * リスナを接続する。
     */
    private void connect() {
        
        stateMgr = new StateMgr();
        
        // DocumentListener
        DocumentListener dl = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                stateMgr.checkState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                stateMgr.checkState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                stateMgr.checkState();
            }
        };

        String jmariPattern = "[0-9]*";
        RegexConstrainedDocument jmariDoc = new RegexConstrainedDocument(jmariPattern);
        jmariField.setDocument(jmariDoc);
        jmariField.getDocument().addDocumentListener(dl);
        jmariField.addFocusListener(AutoRomanListener.getInstance());
        
        String portPattern = "[0-9]*";
        RegexConstrainedDocument portDoc = new RegexConstrainedDocument(portPattern);
        claimPortField.setDocument(portDoc);
        claimPortField.getDocument().addDocumentListener(dl);
        claimPortField.addFocusListener(AutoRomanListener.getInstance());
        
        String ipPattern = "[A-Za-z0-9.\\-_]*";
        RegexConstrainedDocument ipDoc = new RegexConstrainedDocument(ipPattern);
        claimAddressField.setDocument(ipDoc);
        claimAddressField.getDocument().addDocumentListener(dl);
        claimAddressField.addFocusListener(AutoRomanListener.getInstance());
        
        String ipPattern2 = "[A-Za-z0-9.\\-_]*";
        RegexConstrainedDocument ipDoc2 = new RegexConstrainedDocument(ipPattern2);
        bindAddress.setDocument(ipDoc2);
        bindAddress.getDocument().addDocumentListener(dl);
        bindAddress.addFocusListener(AutoRomanListener.getInstance());
        
        // アクションリスナ
        ActionListener al = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                stateMgr.controlClaim();
            }
        };
        sendClaimYes.addActionListener(al);
        sendClaimNo.addActionListener(al);
        
        // バインドアドレス
        ActionListener al3 = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                stateMgr.controlBindAddress();
            }
        };
        useAsPVTServer.addActionListener(al3);
        
        // orca api
        ActionListener al2 = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                stateMgr.controlUseOrcaApi();
            }
        };
        useOrcaApi.addActionListener(al2);
        useClaim.addActionListener(al2);
        orcaUserIdField.getDocument().addDocumentListener(dl);
        orcaUserIdField.addFocusListener(AutoRomanListener.getInstance());
        orcaPasswordField.getDocument().addDocumentListener(dl);
        orcaPasswordField.addFocusListener(AutoRomanListener.getInstance());
        orcaStaffCodeField.getDocument().addDocumentListener(dl);
        orcaStaffCodeField.addFocusListener(AutoRomanListener.getInstance());
    }
    
    /**
     * ModelToView
     */
    private void bindModelToView() {
        //
        // 診療行為送信を選択する
        //
        boolean sending = model.isSendClaim();
        sendClaimYes.setSelected(sending);
        sendClaimNo.setSelected(!sending);
        claimPortField.setEnabled(sending);
        
        // JMARICode
        String jmari = model.getJmariCode();
        jmari = jmari != null ? jmari : "";
        if (!jmari.equals("") && jmari.startsWith("JPN")) {
            jmari = jmari.substring(3);
            jmariField.setText(jmari);
        }
        
        // CLAIM ホストのIPアドレスを設定する
        String val = model.getClaimAddress();
        val = val != null ? val : "";
        claimAddressField.setText(val);
        
        // CLAIM ホストのポート番号を設定する
        val = String.valueOf(model.getClaimPort());
        val = val != null ? val : "";
        claimPortField.setText(val);
        
        // ホスト名
        val = model.getClaimHostName();
        val = val != null ? val : "";
        claimHostCombo.setSelectedItem(val);
        
        // 受付受信
        boolean tmp = model.isUseAsPVTServer();
        useAsPVTServer.setSelected(tmp);
        
        // バインドアドレス
        String bindAddr = model.getBindAddress();
        bindAddr = bindAddr != null ? bindAddr : "";
        bindAddress.setText(bindAddr);
        bindAddress.setEnabled(tmp);
        
        // 01 小児科
        claim01.setSelected(model.isClaim01());
        
        // orca api
        boolean orcaApi = model.isUseOrcaApi();
        if (orcaApi) {
            useOrcaApi.setSelected(true);
        } else {
            useClaim.setSelected(true);
        }
        orcaUserIdField.setEnabled(orcaApi);
        orcaUserIdField.setText(model.getOrcaUserId());
        orcaPasswordField.setEnabled(orcaApi);
        orcaPasswordField.setText(model.getOrcaPassword());
        orcaStaffCodeField.setEnabled(orcaApi);
        orcaStaffCodeField.setText(model.getOrcaStaffCode());
        orcaStaffCodeButton.setEnabled(orcaApi);

    }
    
    /**
     * ViewToModel
     */
    private void bindViewToModel() {
        //
        // 診療行為送信、仮保存時、修正時、病名送信
        // の設定を保存する
        //
        model.setSendClaim(sendClaimYes.isSelected());
        
        // JMARI
        String jmari = jmariField.getText().trim();
        if (!jmari.equals("")) {
            model.setJmariCode("JPN"+jmari);
        } else {
            model.setJmariCode(null);
        }
        
        // ホスト名を保存する
        String val = (String)claimHostCombo.getSelectedItem();
        model.setClaimHostName(val);
        
        // IPアドレスを保存する
        val = claimAddressField.getText().trim();
        model.setClaimAddress(val);
        
        // ポート番号を保存する
        val = claimPortField.getText().trim();
        try {
            int port = Integer.parseInt(val);
            model.setClaimPort(port);
            
        } catch (NumberFormatException e) {
            model.setClaimPort(5001);
        }
        
        // 受付受信を保存する
        model.setUseAsPVTServer(useAsPVTServer.isSelected());
        
        // バインドアドレスを保存する
        val = bindAddress.getText().trim();
        model.setBindAddress(val);
        
        // 01 小児科
        model.setClaim01(claim01.isSelected());
        
        // orca api
        model.setUseOrcaApi(useOrcaApi.isSelected());
        model.setOrcaUserId(orcaUserIdField.getText().trim());
        model.setOrcaPassword(new String(orcaPasswordField.getPassword()).trim());
        model.setOrcaStaffCode(orcaStaffCodeField.getText().trim());
    }
    
    /**
     * 画面も出るクラス。
     */
    class ClaimModel {
        
        private boolean sendClaim;
        private String claimHostName;
        private String version;
        private String jmariCode;
        private String claimAddress;
        private int claimPort;
        private boolean useAsPvtServer;
        private String bindAddress;
        private boolean claim01;
        
        private boolean useOrcaApi;
        private String orcaUserId;
        private String orcaPassword;
        private String orcaStaffCode;
        
        public void populate(ProjectStub stub) {
            
            // 診療行為送信
            setSendClaim(Project.getBoolean(Project.SEND_CLAIM)); // stub.getSendClaim()
            
            // JMARI code
            setJmariCode(Project.getString(Project.JMARI_CODE)); // stub.getJMARICode()
            
            // CLAIM ホストのIPアドレス
            setClaimAddress(Project.getString(Project.CLAIM_ADDRESS));  // stub.getClaimAddress()
            
            // CLAIM ホストのポート番号
            setClaimPort(Project.getInt(Project.CLAIM_PORT)); // stub.getClaimPort()
            
            // ホスト名
            setClaimHostName(Project.getString(Project.CLAIM_HOST_NAME)); // stub.getClaimHostName()
            
            // 受付受信
            setUseAsPVTServer(Project.getBoolean(Project.USE_AS_PVT_SERVER));    // stub.getUseAsPVTServer()
            
            // バインドアドレス
            setBindAddress(Project.getString(Project.CLAIM_BIND_ADDRESS));   // stub.getBindAddress()
            
            // 01 小児科等
            setClaim01(Project.getBoolean(Project.CLAIM_01));   // stub.isClaim01()
            
            // orca api
            setUseOrcaApi(Project.getBoolean(Project.USE_ORCA_API, false));
            setOrcaUserId(Project.getString(Project.ORCA_USER_ID));
            setOrcaPassword(Project.getString(Project.ORCA_USER_PASSWORD));
            setOrcaStaffCode(Project.getString(Project.ORCA_STAFF_CODE));
        }
        
        public void restore(ProjectStub stub) {
            
            // 診療行為送信
            Project.setBoolean(Project.SEND_CLAIM, isSendClaim());  //stub.setSendClaim(isSendClaim());
            
            // JMARI
            Project.setString(Project.JMARI_CODE, getJmariCode());    //stub.setJMARICode(getJmariCode());
            
            // CLAIM ホストのIPアドレス
            Project.setString(Project.CLAIM_ADDRESS, getClaimAddress());        //stub.setClaimAddress(getClaimAddress());
            
            // CLAIM ホストのポート番号
            Project.setInt(Project.CLAIM_PORT, getClaimPort());        //stub.setClaimPort(getClaimPort());
            
            // ホスト名
            Project.setString(Project.CLAIM_HOST_NAME, getClaimHostName());        //stub.setClaimHostName(getClaimHostName());
            
            // 受付受信
            Project.setBoolean(Project.USE_AS_PVT_SERVER, isUseAsPVTServer());        //stub.setUseAsPVTServer(isUseAsPVTServer());
            
            // バインドアドレス
            Project.setString(Project.CLAIM_BIND_ADDRESS, getBindAddress());            //stub.setBindAddress(getBindAddress());
            
            // 01 小児科
            Project.setBoolean(Project.CLAIM_01, isClaim01());        //stub.setClaim01(isClaim01());
            
            // orca api
            Project.setBoolean(Project.USE_ORCA_API, isUseOrcaApi());
            Project.setString(Project.ORCA_USER_ID, getOrcaUserId());
            Project.setString(Project.ORCA_USER_PASSWORD, getOrcaPassword());
            Project.setString(Project.ORCA_STAFF_CODE, getOrcaStaffCode());
        }
        
        public boolean isSendClaim() {
            return sendClaim;
        }
        
        public void setSendClaim(boolean sendClaim) {
            this.sendClaim = sendClaim;
        }
        
        public boolean isUseAsPVTServer() {
            return useAsPvtServer;
        }
        
        public void setUseAsPVTServer(boolean useAsPvtServer) {
            this.useAsPvtServer = useAsPvtServer;
        }
        
        public String getClaimHostName() {
            return claimHostName;
        }
        
        public void setClaimHostName(String claimHostName) {
            this.claimHostName = claimHostName;
        }
        
        public String getClaimAddress() {
            return claimAddress;
        }
        
        public void setClaimAddress(String claimAddress) {
            this.claimAddress = claimAddress;
        }
        
        public String getBindAddress() {
            return bindAddress;
        }
        
        public void setBindAddress(String bindAddress) {
            this.bindAddress = bindAddress;
        }
        
        public int getClaimPort() {
            return claimPort;
        }
        
        public void setClaimPort(int claimPort) {
            this.claimPort = claimPort;
        }

        public String getJmariCode() {
            return jmariCode;
        }

        public void setJmariCode(String jmariCode) {
            this.jmariCode = jmariCode;
        }
        
        public boolean isClaim01() {
            return claim01;
        }
        
        public void setClaim01(boolean b) {
            this.claim01 = b;
        }
//pns^
        public boolean isUseOrcaApi() {
            return useOrcaApi;
        }
        
        public void setUseOrcaApi(boolean b) {
            this.useOrcaApi = b;
        }
        
        public String getOrcaUserId() {
            return orcaUserId == null? "" : orcaUserId;
        }
        
        public void setOrcaUserId(String id) {
            this.orcaUserId = id;
        }
        
        public String getOrcaPassword() {
            return orcaPassword == null? "" : orcaPassword;
        }
        
        public void setOrcaPassword(String pass) {
            this.orcaPassword = pass;
        }
        
        public String getOrcaStaffCode() {
            return orcaStaffCode == null? "" : orcaStaffCode;
        }
        
        public void setOrcaStaffCode(String id) {
            this.orcaStaffCode = id;
        }
//pns$
    }
    
    class StateMgr {
        
        public void checkState() {
            
            AbstractSettingPanel.State newState = isValid()
            ? AbstractSettingPanel.State.VALID_STATE
                    : AbstractSettingPanel.State.INVALID_STATE;
            if (newState != state) {
                setState(newState);
            }
        }
        
        public void controlClaim() {
            //
            // 診療行為の送信を行う場合のみ
            // 仮保存、修正、病名送信、ホスト選択、ポートがアクティブになる
            //
            boolean b = sendClaimYes.isSelected();
            
            claimPortField.setEnabled(b);
            
            this.checkState();
        }
        
        public void controlVersion() {
            boolean b = true;   //v40.isSelected();
            jmariField.setEnabled(b);
            this.checkState();
        }
        
        public void controlBindAddress() {
            boolean b = useAsPVTServer.isSelected();
            bindAddress.setEnabled(b);
            this.checkState();
        }

//pns^
        public void controlUseOrcaApi() {
            boolean orcaApi = useOrcaApi.isSelected();
            orcaUserIdField.setEnabled(orcaApi);
            orcaPasswordField.setEnabled(orcaApi);
            orcaStaffCodeField.setEnabled(orcaApi);
            orcaStaffCodeButton.setEnabled(orcaApi);
            claimPortField.setEnabled(!orcaApi);
            this.checkState();
        }
//pns$
        
        private boolean isValid() {
            
            boolean jmariOk = false;
            boolean claimAddrOk;
            boolean claimPortOk;
            boolean bindAdrOk;
            boolean orcaApiOk;
            
            String code = jmariField.getText().trim();
            if (!code.equals("") && code.length() == 12) {
                jmariOk = true;
            }

            if (sendClaimYes.isSelected()) {
                claimAddrOk = !claimAddressField.getText().trim().equals("");
                
                if (useOrcaApi.isSelected()) {
                    claimPortOk = true;
                    orcaApiOk = !orcaUserIdField.getText().trim().equals("") && !orcaStaffCodeField.getText().trim().equals("");
                    
                } else {
                    claimPortOk = !claimPortField.getText().trim().equals("");
                    orcaApiOk = true;
                }
            } else {
                claimAddrOk = true;
                claimPortOk = true;
                orcaApiOk = true;
            }
            
            if (useAsPVTServer.isSelected()) {
                String test = bindAddress.getText().trim();
                if (test != null && (!test.equals(""))) {
                    bindAdrOk = isIPAddress(test);
                } else {
                    bindAdrOk = true;
                }
            } else {
                bindAdrOk = true;
            }
            
            return jmariOk && claimAddrOk && claimPortOk && bindAdrOk && orcaApiOk;
        }

        private boolean isIPAddress(String test) {
            return !(test == null || test.equals(""));
        }
        
        private boolean isPort(String test) {
            
            boolean ret = false;
            
            if (test != null) {
                try {
                    int port = Integer.parseInt(test);
                    ret = !(port < 0 || port > 65535);
                }catch (Exception e) {
                }
            }
            
            return ret;
        }
    }
}
