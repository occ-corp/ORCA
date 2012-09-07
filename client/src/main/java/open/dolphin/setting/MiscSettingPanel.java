package open.dolphin.setting;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import open.dolphin.client.AutoRomanListener;
import open.dolphin.client.ClientContext;
import open.dolphin.client.GUIFactory;
import open.dolphin.client.RegexConstrainedDocument;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.GridBagBuilder;
import open.dolphin.infomodel.UserPropertyModel;
import open.dolphin.project.Project;

/**
 * 増田内科 追加機能の設定パネル
 *
 * Masuda Naika Clinic, Wakayama City
 * @author masuda, Masuda Naika
 */
public class MiscSettingPanel extends AbstractSettingPanel {

    private static final String ID = "miscSetting";
    private static final String TITLE = "その他";
    private static final String ICON = "confg_16.gif";

    // preference名
    public static final String LBLPRT_ADDRESS = "lblPrtAddress";
    public static final String LBLPRT_PORT = "lblPrtPort";
    public static final String FEV_SHAREPATH = "fevSharePath";
    public static final String USE_FEV = "useFev";
    public static final String FEV40_PATH = "fev40Path";
    public static final String USE_WINE = "useWine";
    public static final String WINE_PATH = "winePath";
    public static final String SEND_PATIENT_INFO = "sendPatientInfo";
    public static final String PVT_ON_SERVER = "pvtOnServer";
    public static final String FEV_ON_SERVER = "fevOnServer";

    public static final String FOLLOW_MEDICOM = "followMedicom";
    public static final String SANTEI_CHECK = "santeiCheck";

    public static final String STAMP_HOLDER_CELLPADDING = "stampHolderCellPadding";
    public static final String USE_RSB = "useRSB";
    public static final String RSB_URL = "rsbURL";
    public static final String RSB_DRS_PATH = "rsbDrsPath";
    public static final String RSB_LINK_PATH = "rsbLinkPath";
    public static final String RSB_RSN_PATH = "rsbRsnPath";
    public static final String RSB_BROWSER_PATH = "rsbBrowserPath";
    public static final String USE_PACS = "usePacs";
    public static final String PACS_REMOTE_HOST = "pacsServerIp";
    public static final String PACS_REMOTE_PORT = "pacsServerPort";
    public static final String PACS_REMOTE_AE = "pacsServerAE";
    public static final String PACS_LOCAL_HOST = "pacsClientIp";
    public static final String PACS_LOCAL_PORT = "pacsClientPort";
    public static final String PACS_LOCAL_AE = "pacsClientAE";
    public static final String PACS_USE_SUFFIXSEARCH = "pacsUseSuffixSearch";
    //public static final String USE_JMS = "useJms";
    public static final String RP_OUT = "rp.out";
    public static final String KARTE_SCROLL_TYPE = "karteScrollType";
    public static final String USE_VERTICAL_LAYOUT = "verticalLayout";

    public static final String PREFER_WAREKI = "preferWareki";
    public static final String ENABLE_DENSHI = "enableDenshi";
    public static final String USE_HIBERNATE_SEARCH = "useHibernateSearch";
    public static final String PACS_SHOW_IMAGEINFO = "pacsShowImageInfo";
    public static final String PACS_VIEWER_GAMMA = "pacsViewerGamma";

    // preferencesのdefault
    public static final String DEFAULT_LBLPRT_ADDRESS = null;
    public static final int DEFAULT_LBLPRT_PORT = 9100;
    public static final boolean DEFAULT_USEFEV = false;
    public static final String DEFAULT_FEV40_PATH = "C:\\Program Files\\Fukuda Denshi\\ECG Viewer FEV-40\\FEV-40.EXE";
    public static final String DEFAULT_WINE_PATH = "/opt/local/bin/wine";
    public static final boolean DEFAULT_USE_WINE = false;
    public static final boolean DEFAULT_SENDPATIENTINFO = false;
    public static final String DEFAULT_SHAREPATH = null;
    public static final boolean DEFAULT_FOLLOW_MEDICOM = true;
    public static final boolean DEFAULT_SANTEI_CHECK = true;
    public static final boolean DEFAULT_EX_MED = false;
    
    public static final boolean DEFAULT_PVT_ON_SERVER = false;
    public static final boolean DEFAULT_FEV_ON_SERVER = false;

   // public static final int DEFAULT_STAMP_HOLDER_CELLPADDING = 3;

    public static final boolean DEFAULT_USE_RSB = false;
    public static final String DEFAULT_RSB_URL = "http://localhost/~rsn";
    public static final String DEFAULT_RSB_DRS_PATH = "C:\\DRS\\";
    public static final String DEFAULT_RSB_LINK_PATH = "C:\\RSB_LINK\\";
    public static final String DEFAULT_RSB_RSN_PATH = "C:\\User\\rsn\\public_html\\";
    public static final String DEFAULT_RSB_BROWSER_PATH = "";

    public static final boolean DEFAULT_USE_PACS = false;
    public static final boolean DEFAULT_PACS_SUFFIX_SEARCH = false;
    public static final String DEFAULT_PACS_REMOTE_HOST = "localhost";
    public static final String DEFAULT_PACS_REMOTE_AE = "PACSSERVER";
    public static final int DEFAULT_PACS_REMOTE_PORT = 104;
    public static final String DEFAULT_PACS_LOCAL_HOST = "localhost";
    public static final String DEFAULT_PACS_LOCAL_AE = "Dolphin1";
    public static final int DEFAULT_PACS_LOCAL_PORT = 8104;
    public static final boolean DEFAULT_USE_JMS = false;

    public static final int DEFAULT_KARTE_SCROLL = 0;
    public static final int SKIP_KARTE_SCROLL = 1;
    public static final int PAGE_KARTE_SCROLL = 2;
    public static final boolean DEFAULT_USE_VERTICAL_LAYOUT = false;
    public static final boolean DEFAULT_PREFER_WAREKI = false;
    public static final boolean DEFAULT_HIBERNATE_SEARCH = false;
    public static final double DEFAULT_PACS_GAMMA = 1;
    public static final boolean DEFAULT_PACS_SHOW_IMAGEINFO = true;

    // GUI staff
    private JTextField tf_lblPrtAddress;
    private JTextField tf_lblPrtPort;
    private JCheckBox cb_UseFev;
    private JCheckBox cb_UseWine;
    private JCheckBox cb_SendPatientInfo;
    private JCheckBox cb_Yakujo;
    private JCheckBox cb_Santei;
    private JButton btn_openFEV;
    private JButton btn_openWine;
    private JTextField tf_fevSharePath;
    private JTextField tf_fev40Path;
    private JTextField tf_winePath;
    private JLabel lbl_useWine;
    private JLabel lbl_winePath;
    private JLabel lbl_fev40Path;
    private JRadioButton rb_inMed;
    private JRadioButton rb_exMed;
    //private JComboBox cmb_cellPadding;
    //private JCheckBox cb_useJms;
    private JCheckBox cb_verticalLayout;
    private JComboBox cmb_karteScroll;

    private JLabel lbl_fev70;
    private JLabel lbl_fevShareFolder;
    
    private JCheckBox cb_PvtOnServer;
    private JCheckBox cb_FevOnServer;
    
    private JButton btn_discardSize;
    private JButton btn_openBase;
    private JButton btn_saveProp;
    private JButton btn_loadProp;
    
/*    
    private JLabel lbl_rsbURL;
    private JLabel lbl_rsbDrsPath;
    private JLabel lbl_rsbLinkPath;
    private JLabel lbl_rsbRsnPath;
    private JLabel lbl_rsbBrowser;

    private JCheckBox cb_useRsb;
    private JButton btn_openRSB;
    private JTextField tf_rsbBrowserPath;
    private JTextField tf_rsbURL;
    private JTextField tf_rsbRsnPath;
    private JTextField tf_rsbDrsPath;
    private JTextField tf_rsbLinkPath;
*/
    private JCheckBox cb_UsePacs;
    private JTextField tf_pacsRemoteHost;
    private JTextField tf_pacsRemotePort;
    private JTextField tf_pacsRemoteAE;
    private JTextField tf_pacsLocalHost;
    private JTextField tf_pacsLocalPort;
    private JTextField tf_pacsLocalAE;
    private JCheckBox cb_SuffixSearch;
    private JLabel lbl_remoteHost;
    private JLabel lbl_remotePort;
    private JLabel lbl_remoteAE;
    private JLabel lbl_localHost;
    private JLabel lbl_localPort;
    private JLabel lbl_localAE;

    /** 画面モデル */
    private MiscModel model;
    private StateMgr stateMgr;

    public MiscSettingPanel() {
        this.setId(ID);
        this.setTitle(TITLE);
        this.setIcon(ICON);
    }

    /**
     * GUI 及び State を生成する。
     */
    @Override
    public void start() {

        // モデルを生成し初期化する
        model = new MiscModel();
        model.populate();

        // GUIを構築する
        initComponents();

        // bind する
        bindModelToView();
    }

    /**
     * 設定値を保存する。
     */
    @Override
    public void save() {
        bindViewToModel();
        model.restore();
    }

    /**
     * GUIを構築する
     */
    private void initComponents() {

        String programFolder = ClientContext.isWin()
                ? System.getenv("PROGRAMFILES")
                : "~/.wine/drive_c/Program Files/";
        String userHome = System.getProperty("user.home");

        // ラベルプリンタ、FEV-40
        GridBagBuilder gbl = new GridBagBuilder("ラベルプリンタQL-580N設定");
        tf_lblPrtAddress = GUIFactory.createTextField(10, null, null, null);
        tf_lblPrtPort = GUIFactory.createTextField(5, null, null, null);

        int row = 0;
        JLabel label = new JLabel("IPアドレス:");
        gbl.add(label, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_lblPrtAddress, 1, row, GridBagConstraints.WEST);

        row++;
        label = new JLabel("ポート番号:");
        gbl.add(label, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_lblPrtPort, 1, row, GridBagConstraints.WEST);
        JPanel port = gbl.getProduct();

        row = 0;
        gbl = new GridBagBuilder("フクダ電子心電図ファイリング設定");
        label = new JLabel("この端末でFEV-40を使用する");
        cb_UseFev = new JCheckBox();
        cb_UseFev.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                controlFEV();
            }
        });

        gbl.add(cb_UseFev, 0, row, GridBagConstraints.EAST);
        gbl.add(label, 1, row, GridBagConstraints.WEST);

        row++;
        lbl_fev40Path = new JLabel("FEV-40のパス");
        tf_fev40Path = GUIFactory.createTextField(20, null, null, null);
        btn_openFEV = new JButton("開く");
        MyBtnActionListener listener = new MyBtnActionListener(programFolder, tf_fev40Path);
        btn_openFEV.addActionListener(listener);
        gbl.add(lbl_fev40Path, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_fev40Path, 1, row, GridBagConstraints.CENTER);
        gbl.add(btn_openFEV, 2, row, GridBagConstraints.WEST);
        
        row++;
        lbl_useWine = new JLabel("Wineを使う");
        cb_UseWine = new JCheckBox();
        gbl.add(cb_UseWine, 0, row, GridBagConstraints.EAST);
        gbl.add(lbl_useWine, 1, row, GridBagConstraints.WEST);
        
        row++;
        lbl_winePath = new JLabel("Wineのパス");
        tf_winePath = GUIFactory.createTextField(20, null, null, null);
        btn_openWine = new JButton("開く");
        listener = new MyBtnActionListener(userHome, tf_winePath);
        btn_openWine.addActionListener(listener);
        gbl.add(lbl_winePath, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_winePath, 1, row, GridBagConstraints.CENTER);
        gbl.add(btn_openWine, 2, row, GridBagConstraints.WEST);
        
        row++;
        lbl_fev70 = new JLabel("PVT受信時、FEV-70に患者情報自動登録を行う");
        cb_SendPatientInfo = new JCheckBox();
        gbl.add(cb_SendPatientInfo, 0, row, GridBagConstraints.EAST);
        gbl.add(lbl_fev70, 1, row, GridBagConstraints.WEST);

        row++;
        lbl_fevShareFolder = new JLabel("共有フォルダ");
        gbl.add(lbl_fevShareFolder, 0, row, GridBagConstraints.EAST);
        tf_fevSharePath = GUIFactory.createTextField(20, null, null, null);
        gbl.add(tf_fevSharePath, 1, row, GridBagConstraints.WEST);
        JPanel fev = gbl.getProduct();

        cb_SendPatientInfo.setEnabled(false);
        tf_fevSharePath.setEnabled(false);
        lbl_fev70.setEnabled(false);
        lbl_fevShareFolder.setEnabled(false);

        row++;
        gbl = new GridBagBuilder("算定チェック");
        label = new JLabel("同一月に同一処方があれば薬剤情報提供料算定不可とする");
        cb_Yakujo = new JCheckBox();
        gbl.add(cb_Yakujo, 0, row, GridBagConstraints.EAST);
        gbl.add(label, 1, row, GridBagConstraints.WEST);

        row++;
        label = new JLabel("基本料スタンプ機能を使用する");
        cb_Santei = new JCheckBox();
        gbl.add(cb_Santei, 0, row, GridBagConstraints.EAST);
        gbl.add(label, 1, row, GridBagConstraints.WEST);

        JPanel yakujo = gbl.getProduct();

        // 全体レイアウト
        gbl = new GridBagBuilder();
        gbl.add(port, 0, 0, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(fev, 0, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(yakujo, 0, 2, GridBagConstraints.HORIZONTAL, 1.0, 0.0);

        JPanel setting = gbl.getProduct();
        setting.setLayout(new BoxLayout(setting, BoxLayout.Y_AXIS));

        // 設定２
        JPanel setting2 = new JPanel();
        setting2.setLayout(new BoxLayout(setting2, BoxLayout.Y_AXIS));
        gbl = new GridBagBuilder("処方デフォルト設定");
        rb_inMed = new JRadioButton("院内処方");
        rb_exMed = new JRadioButton("院外処方");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rb_inMed);
        bg.add(rb_exMed);
        JPanel medP = GUIFactory.createRadioPanel(new JRadioButton[]{rb_inMed, rb_exMed});
        gbl.add(medP, 0, 0, GridBagConstraints.CENTER);
        JPanel medPanel = gbl.getProduct();
/*
        gbl = new GridBagBuilder("チャート状態を同期する");
        cb_useJms = new JCheckBox("サーバーと同期");
        gbl.add(cb_useJms, 0, 0, GridBagConstraints.CENTER);
        JPanel chartSyncPanel = gbl.getProduct();

        gbl = new GridBagBuilder("スタンプホルダ");
        cmb_cellPadding = new JComboBox();
        for (int i = 0; i < 4; ++i) {
            cmb_cellPadding.addItem(i);
        }

        gbl.add(new JLabel("スタンプ行間隔"), 0, 0, GridBagConstraints.EAST);
        gbl.add(cmb_cellPadding, 1, 0, GridBagConstraints.WEST);
        JPanel stamp = gbl.getProduct();
*/        
        
        gbl = new GridBagBuilder("設定");
        btn_discardSize = new JButton("インスペクタサイズ初期化");
        btn_discardSize.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                discardInspectorSize();
            }
        });

        btn_openBase = new JButton("ベースフォルダを開く");
        btn_openBase.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                File file = new File(ClientContext.getBaseDirectory());
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException ex) {
                }
            }
        });
        
        btn_saveProp = new JButton("設定をサーバーに保存");
        btn_saveProp.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                saveProperties();
            }
        });
        btn_loadProp = new JButton("設定をサーバーから読込");
        btn_loadProp.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                loadProperties();
            }
        });
        gbl.add(btn_openBase, 0, 0, GridBagConstraints.CENTER);
        gbl.add(btn_discardSize, 1, 0, GridBagConstraints.CENTER);
        gbl.add(btn_saveProp, 0, 1, GridBagConstraints.CENTER);
        gbl.add(btn_loadProp, 1, 1, GridBagConstraints.CENTER);
        JPanel inspector = gbl.getProduct();
        
        gbl = new GridBagBuilder("サーバー設定");
        cb_PvtOnServer = new JCheckBox("PVT受信登録処理をサーバーで行う");
        cb_FevOnServer = new JCheckBox("FEV患者登録処理をサーバーで行う");
        cb_FevOnServer.setToolTipText("出力先フォルダは設定１で入力してください");
        gbl.add(cb_PvtOnServer, 0, 0, GridBagConstraints.CENTER);
        gbl.add(cb_FevOnServer, 0, 1, GridBagConstraints.CENTER);
        JPanel pvt = gbl.getProduct();
        
        gbl = new GridBagBuilder("カルテスクロール");
        //cb_skipScroll = new JCheckBox("スキップスクロール有効");
        //gbl.add(cb_skipScroll, 0, 0, GridBagConstraints.CENTER);
        String[] items = new String[]{"ノーマル", "スキップ", "ページ"};
        cmb_karteScroll = new JComboBox(items);
        gbl.add(cmb_karteScroll, 0, 0, GridBagConstraints.CENTER);
        cb_verticalLayout = new JCheckBox("水平配置カルテでは所見と処置を縦に並べる");
        gbl.add(cb_verticalLayout, 0, 1, GridBagConstraints.CENTER);
        JPanel karteScroll = gbl.getProduct();

        gbl = new GridBagBuilder("電子点数表");
        JButton btn_etensu = new JButton("電子点数表登録");
        btn_etensu.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                RegistETensuData reg = new RegistETensuData();
                reg.startRegist(MiscSettingPanel.this.getContext());
            }
        });
        JButton btn_santei = new JButton("算定歴初期化");
        btn_santei.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                RegistETensuData reg = new RegistETensuData();
                reg.startInitSantei(MiscSettingPanel.this.getContext());
            }
        });

        gbl.add(btn_etensu, 0, 0, GridBagConstraints.CENTER);
        gbl.add(btn_santei, 1, 0, GridBagConstraints.CENTER);
        JPanel etensu = gbl.getProduct();
        if (!isLoginState()) {
            btn_etensu.setEnabled(false);
            btn_santei.setEnabled(false);
        }

        // 全体レイアウト
        gbl = new GridBagBuilder();
        gbl.add(medPanel, 0, 0, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        //gbl.add(stamp, 0, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(inspector, 0, 2, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(pvt, 0, 3, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        //gbl.add(chartSyncPanel, 0, 3, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(karteScroll, 0, 4, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(etensu, 0, 5, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        setting2.add(gbl.getProduct());
/*
        // RSB設定
        row = 0;
        JPanel settingRSB = new JPanel();
        settingRSB.setLayout(new BoxLayout(settingRSB, BoxLayout.Y_AXIS));
        gbl = new GridBagBuilder("RS_Base設定");
        label = new JLabel("RS_Baseを使用");
        cb_useRsb = new JCheckBox();
        cb_useRsb.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                controlRSB();
            }
        });

        gbl.add(cb_useRsb, 0, row, GridBagConstraints.EAST);
        gbl.add(label, 1, row, GridBagConstraints.WEST);
        row++;
        tf_rsbBrowserPath = GUIFactory.createTextField(20, null, null, null);
        lbl_rsbBrowser = new JLabel("ブラウザ指定:");
        btn_openRSB = new JButton("開く");
        listener = new MyBtnActionListener(programFolder, tf_rsbBrowserPath);
        btn_openRSB.addActionListener(listener);
        gbl.add(lbl_rsbBrowser, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_rsbBrowserPath, 1, row, GridBagConstraints.CENTER);
        gbl.add(btn_openRSB, 2, row, GridBagConstraints.WEST);
        row++;
        tf_rsbURL = GUIFactory.createTextField(20, null, null, null);
        lbl_rsbURL = new JLabel("URL:");
        gbl.add(lbl_rsbURL, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_rsbURL, 1, row, GridBagConstraints.WEST);
        row++;
        tf_rsbRsnPath = GUIFactory.createTextField(20, null, null, null);
        lbl_rsbRsnPath = new JLabel("rsn Path:");
        gbl.add(lbl_rsbRsnPath, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_rsbRsnPath, 1, row, GridBagConstraints.WEST);
        row++;
        tf_rsbDrsPath = GUIFactory.createTextField(20, null, null, null);
        lbl_rsbDrsPath = new JLabel("DRS Path:");
        gbl.add(lbl_rsbDrsPath, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_rsbDrsPath, 1, row, GridBagConstraints.WEST);
        row++;
        tf_rsbLinkPath = GUIFactory.createTextField(20, null, null, null);
        lbl_rsbLinkPath = new JLabel("Link Path:");
        gbl.add(lbl_rsbLinkPath, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_rsbLinkPath, 1, row, GridBagConstraints.WEST);
        JPanel rsb = gbl.getProduct();

        // 全体レイアウト
        gbl = new GridBagBuilder();
        gbl.add(rsb, 0, 0, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        settingRSB.add(gbl.getProduct());
*/
        // PACS
        gbl = new GridBagBuilder("PACSサーバー設定");
        tf_pacsRemoteHost = GUIFactory.createTextField(15, null, null, null);
        tf_pacsRemotePort = GUIFactory.createTextField(5, null, null, null);
        tf_pacsRemoteAE = GUIFactory.createTextField(15, null, null, null);
        tf_pacsLocalHost = GUIFactory.createTextField(15, null, null, null);
        tf_pacsLocalPort = GUIFactory.createTextField(5, null, null, null);
        tf_pacsLocalAE = GUIFactory.createTextField(15, null, null, null);

        row = 0;
        label = new JLabel("PACS接続機能を利用する");
        cb_UsePacs = new JCheckBox();
        cb_UsePacs.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                controlPacs();
            }
        });
        gbl.add(cb_UsePacs, 0, row, GridBagConstraints.EAST);
        gbl.add(label, 1, row, GridBagConstraints.WEST);

        row++;
        lbl_remoteHost = new JLabel("IPアドレス:");
        gbl.add(lbl_remoteHost, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_pacsRemoteHost, 1, row, GridBagConstraints.WEST);

        row++;
        lbl_remotePort = new JLabel("ポート番号:");
        gbl.add(lbl_remotePort, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_pacsRemotePort, 1, row, GridBagConstraints.WEST);
        row++;
        lbl_remoteAE = new JLabel("AEタイトル:");
        gbl.add(lbl_remoteAE, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_pacsRemoteAE, 1, row, GridBagConstraints.WEST);
        JPanel server = gbl.getProduct();

        gbl = new GridBagBuilder("クライアント設定");
        row = 0;
        lbl_localHost = new JLabel("IPアドレス:");
        gbl.add(lbl_localHost, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_pacsLocalHost, 1, row, GridBagConstraints.WEST);

        row++;
        lbl_localPort = new JLabel("ポート番号:");
        gbl.add(lbl_localPort, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_pacsLocalPort, 1, row, GridBagConstraints.WEST);
        row++;
        lbl_localAE = new JLabel("AEタイトル:");
        gbl.add(lbl_localAE, 0, row, GridBagConstraints.EAST);
        gbl.add(tf_pacsLocalAE, 1, row, GridBagConstraints.WEST);
        JPanel client = gbl.getProduct();

        gbl = new GridBagBuilder("患者ID検索");
        row = 0;
        cb_SuffixSearch = new JCheckBox("後方一致検索する");
        gbl.add(cb_SuffixSearch, 0, row, GridBagConstraints.EAST);
        JPanel search = gbl.getProduct();

        // 全体レイアウト
        gbl = new GridBagBuilder();
        gbl.add(server, 0, 0, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(client, 0, 1, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(search, 0, 2, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        JPanel pacsSetting = gbl.getProduct();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("設定１", setting);
        tabbedPane.addTab("設定２", setting2);
        //tabbedPane.addTab("RS_Base", settingRSB);
        tabbedPane.addTab("PACS", pacsSetting);

        getUI().setLayout(new BorderLayout());
        getUI().add(tabbedPane);

        controlFEV();
        //controlRSB();
        controlPacs();

        connect();
    }

    private class MyBtnActionListener implements ActionListener {

        private JTextField tf;
        private String currentDirectory;

        private MyBtnActionListener(String currentDirectory, JTextField tf) {
            this.tf = tf;
            this.currentDirectory = currentDirectory;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser(currentDirectory);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getPath();
                tf.setText(path);
            }
        }
    }
/*
    private void controlRSB() {
        boolean b = cb_useRsb.isSelected();
        tf_rsbURL.setEnabled(b);
        tf_rsbDrsPath.setEnabled(b);
        tf_rsbLinkPath.setEnabled(b);
        tf_rsbRsnPath.setEnabled(b);
        lbl_rsbURL.setEnabled(b);
        lbl_rsbDrsPath.setEnabled(b);
        lbl_rsbLinkPath.setEnabled(b);
        lbl_rsbRsnPath.setEnabled(b);
        lbl_rsbBrowser.setEnabled(b);
        btn_openRSB.setEnabled(b);
        tf_rsbBrowserPath.setEnabled(b);
    }
*/
    private void controlFEV() {
        boolean b = cb_UseFev.isSelected();
        cb_SendPatientInfo.setEnabled(b);
        tf_fevSharePath.setEnabled(b);
        lbl_fev70.setEnabled(b);
        lbl_fevShareFolder.setEnabled(b);
        lbl_fev40Path.setEnabled(b);
        tf_fev40Path.setEnabled(b);
        btn_openFEV.setEnabled(b);
        cb_UseWine.setEnabled(b);
        lbl_winePath.setEnabled(b);
        tf_winePath.setEnabled(b);
        btn_openWine.setEnabled(b);
        lbl_useWine.setEnabled(b);
    }

    private void controlPacs() {
        boolean b = cb_UsePacs.isSelected();
        tf_pacsRemoteHost.setEnabled(b);
        tf_pacsRemotePort.setEnabled(b);
        tf_pacsRemoteAE.setEnabled(b);
        tf_pacsLocalHost.setEnabled(b);
        tf_pacsLocalPort.setEnabled(b);
        tf_pacsLocalAE.setEnabled(b);
        lbl_remoteHost.setEnabled(b);
        lbl_remotePort.setEnabled(b);
        lbl_remoteAE.setEnabled(b);
        lbl_localHost.setEnabled(b);
        lbl_localPort.setEnabled(b);
        lbl_localAE.setEnabled(b);
        cb_SuffixSearch.setEnabled(b);
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

        String portPattern = "[0-9]*";
        RegexConstrainedDocument portDoc = new RegexConstrainedDocument(portPattern);
        tf_lblPrtPort.setDocument(portDoc);
        tf_lblPrtPort.getDocument().addDocumentListener(dl);
        tf_lblPrtPort.addFocusListener(AutoRomanListener.getInstance());

        String ipPattern = "[A-Za-z0-9.]*";
        RegexConstrainedDocument ipDoc = new RegexConstrainedDocument(ipPattern);
        tf_lblPrtAddress.setDocument(ipDoc);
        tf_lblPrtAddress.getDocument().addDocumentListener(dl);
        tf_lblPrtAddress.addFocusListener(AutoRomanListener.getInstance());
        
        // ログインしていないと利用不可
        if (!isLoginState()) {
            btn_discardSize.setEnabled(false);
            btn_openBase.setEnabled(false);
            btn_loadProp.setEnabled(false);
            btn_saveProp.setEnabled(false);
        }
    }

    /**
     * ModelToView
     */
    private void bindModelToView() {

        // ラベルプリンタのIPアドレスを設定する
        String val = model.lblPrtAddress;
        val = val != null ? val : "";
        tf_lblPrtAddress.setText(val);

        // ラベルプリンタのポート番号を設定する
        val = String.valueOf(model.lblPrtPort);
        val = val != null ? val : "";
        tf_lblPrtPort.setText(val);

        // FEV40.EXE, 共有フォルダを設定する
        val = model.fevSharePath;
        val = val != null ? val : "";
        tf_fevSharePath.setText(val);
        val = model.fev40Path;
        val = val != null ? val : "";
        tf_fev40Path.setText(val);
        
        // Wine
        val = model.winePath;
        val = val != null ? val : "";
        tf_winePath.setText(val);
        cb_UseWine.setSelected(model.useWine);

        // Use Fev
        cb_UseFev.setSelected(model.useFEV);
        // SendPatient Info
        cb_SendPatientInfo.setSelected(model.sendPatientInfo);

        // 薬剤情報
        cb_Yakujo.setSelected(model.followMedicom);
        // 算定
        cb_Santei.setSelected(model.santeiCheck);

        // 処方デフォルト
        if (model.defaultExMed) {
            rb_exMed.setSelected(true);
        } else {
            rb_inMed.setSelected(true);
        }
        // Chart stateをサーバーと同期
        //cb_useJms.setSelected(model.useJms);
        // CellPadding
        //cmb_cellPadding.setSelectedItem(model.stampHolderCellPadding);
        // Skip Scroll
        //cb_skipScroll.setSelected(model.skipScroll);
        cmb_karteScroll.setSelectedIndex(model.karteScrollType);
        // vertical layout
        cb_verticalLayout.setSelected(model.verticalLayout);
/*
        // RS_Base
        cb_useRsb.setSelected(model.useRsb);
        val = model.rsbURL;
        val = val != null ? val : "";
        tf_rsbURL.setText(val);
        val = model.rsbDrsPath;
        val = val != null ? val : "";
        tf_rsbDrsPath.setText(val);
        val = model.rsbLinkPath;
        val = val != null ? val : "";
        tf_rsbLinkPath.setText(val);
        val = model.rsbRsnPath;
        val = val != null ? val : "";
        tf_rsbRsnPath.setText(val);
        val = model.rsbBrowserPath;
        val = val != null ? val : "";
        tf_rsbBrowserPath.setText(val);
*/
        // PVT
        cb_PvtOnServer.setSelected(model.pvtOnServer);
        cb_FevOnServer.setSelected(model.fevOnServer);
        // Pacs
        cb_UsePacs.setSelected(model.usePacs);
        cb_SuffixSearch.setSelected(model.useSuffixSearch);
        val = model.remoteHost;
        val = val != null ? val : "";
        tf_pacsRemoteHost.setText(val);
        val = model.remoteAE;
        val = val != null ? val : "";
        tf_pacsRemoteAE.setText(val);
        val = String.valueOf(model.remotePort);
        val = val != null ? val : "";
        tf_pacsRemotePort.setText(val);
        val = model.localHost;
        val = val != null ? val : "";
        tf_pacsLocalHost.setText(val);
        val = model.localAE;
        val = val != null ? val : "";
        tf_pacsLocalAE.setText(val);
        val = String.valueOf(model.localPort);
        val = val != null ? val : "";
        tf_pacsLocalPort.setText(val);
    }

    /**
     * ViewToModel
     */
    private void bindViewToModel() {

        // IPアドレスを保存する
        model.lblPrtAddress = tf_lblPrtAddress.getText().trim();

        // ポート番号を保存する
        try {
            int port = Integer.parseInt(tf_lblPrtPort.getText().trim());
            model.lblPrtPort = port;

        } catch (NumberFormatException e) {
            model.lblPrtPort = 9100;
        }

        // FEV
        model.useFEV = cb_UseFev.isSelected();
        model.fev40Path = tf_fev40Path.getText().trim();
        model.fevSharePath = tf_fevSharePath.getText().trim();
        model.sendPatientInfo = cb_SendPatientInfo.isSelected();
        model.useWine = cb_UseWine.isSelected();
        model.winePath = tf_winePath.getText().trim();
        // 薬剤情報
        model.followMedicom = cb_Yakujo.isSelected();
        // 算定
        model.santeiCheck = cb_Santei.isSelected();
        // 院内・院外処方デフォルト
        model.defaultExMed = rb_exMed.isSelected();
        // cell padding
        //int i = (Integer) cmb_cellPadding.getSelectedItem();
        //model.stampHolderCellPadding = i;
        //StampRenderingHints.getInstance().setCellPadding(i);
        // Skip scroll
        //model.skipScroll = cb_skipScroll.isSelected();
        model.karteScrollType = cmb_karteScroll.getSelectedIndex();
        // vertical layout
        model.verticalLayout = cb_verticalLayout.isSelected();
/*
        // RS_Base
        model.useRsb = cb_useRsb.isSelected();
        model.rsbURL = tf_rsbURL.getText().trim();
        model.rsbDrsPath = tf_rsbDrsPath.getText().trim();
        model.rsbLinkPath = tf_rsbLinkPath.getText().trim();
        model.rsbRsnPath = tf_rsbRsnPath.getText().trim();
        model.rsbBrowserPath = tf_rsbBrowserPath.getText().trim();
*/
        // PVT
        model.pvtOnServer = cb_PvtOnServer.isSelected();
        model.fevOnServer = cb_FevOnServer.isSelected();
        
        // Pacs
        model.usePacs = cb_UsePacs.isSelected();
        model.useSuffixSearch = cb_SuffixSearch.isSelected();
        model.remoteHost = tf_pacsRemoteHost.getText().trim();
        model.remoteAE = tf_pacsRemoteAE.getText().trim();
        model.remotePort = Integer.valueOf(tf_pacsRemotePort.getText());
        model.localHost = tf_pacsLocalHost.getText().trim();
        model.localAE = tf_pacsLocalAE.getText().trim();
        model.localPort = Integer.valueOf(tf_pacsLocalPort.getText());

        // Chart stateをサーバーと同期
        //model.useJms = cb_useJms.isSelected();
    }

    /**
     * 画面モデルクラス。
     */
    private class MiscModel {

        private String lblPrtAddress;
        private int lblPrtPort;
        private boolean useFEV;
        private boolean sendPatientInfo;
        private String fevSharePath;
        private String fev40Path;
        private boolean useWine;
        private String winePath;
        private boolean followMedicom;
        private boolean santeiCheck;
        private boolean defaultExMed;
        //private int stampHolderCellPadding;
        private boolean verticalLayout;
        private int karteScrollType;
/*
        private boolean useRsb;
        private String rsbURL;
        private String rsbDrsPath;
        private String rsbLinkPath;
        private String rsbRsnPath;
        private String rsbBrowserPath;
        private boolean useJms;
*/
        private boolean pvtOnServer;
        private boolean fevOnServer;
        
        private boolean usePacs;
        private boolean useSuffixSearch;
        private String remoteHost;
        private int remotePort;
        private String remoteAE;
        private String localHost;
        private int localPort;
        private String localAE;

        public void populate() {

            // ラベルプリンタのIPアドレス
            lblPrtAddress = Project.getString(LBLPRT_ADDRESS, DEFAULT_LBLPRT_ADDRESS);
            // ラベルプリンタのポート番号
            lblPrtPort = Project.getInt(LBLPRT_PORT, DEFAULT_LBLPRT_PORT);
            // FEV
            useFEV = Project.getBoolean(USE_FEV, DEFAULT_USEFEV);
            sendPatientInfo = Project.getBoolean(SEND_PATIENT_INFO, DEFAULT_SENDPATIENTINFO);
            fevSharePath = Project.getString(FEV_SHAREPATH, DEFAULT_SHAREPATH);
            fev40Path = Project.getString(FEV40_PATH, DEFAULT_FEV40_PATH);
            useWine = Project.getBoolean(USE_WINE, DEFAULT_USE_WINE);
            winePath = Project.getString(WINE_PATH, DEFAULT_WINE_PATH);
            // 薬剤情報
            followMedicom = Project.getBoolean(FOLLOW_MEDICOM, DEFAULT_FOLLOW_MEDICOM);
            // 算定
            santeiCheck = Project.getBoolean(SANTEI_CHECK, DEFAULT_SANTEI_CHECK);
            // 院内院外
            defaultExMed = Project.getBoolean(RP_OUT, DEFAULT_EX_MED);
            // cellPadding
            //stampHolderCellPadding = Project.getInt(STAMP_HOLDER_CELLPADDING, DEFAULT_STAMP_HOLDER_CELLPADDING);
            // Skip scroll
            karteScrollType = Project.getInt(KARTE_SCROLL_TYPE, DEFAULT_KARTE_SCROLL);
            // vertical layout
            verticalLayout = Project.getBoolean(USE_VERTICAL_LAYOUT, DEFAULT_USE_VERTICAL_LAYOUT);
/*
            // RS_Base
            useRsb = Project.getBoolean(USE_RSB, DEFAULT_USE_RSB);
            rsbURL = Project.getString(RSB_URL, DEFAULT_RSB_URL);
            rsbDrsPath = Project.getString(RSB_DRS_PATH, DEFAULT_RSB_DRS_PATH);
            rsbLinkPath = Project.getString(RSB_LINK_PATH, DEFAULT_RSB_LINK_PATH);
            rsbRsnPath = Project.getString(RSB_RSN_PATH, DEFAULT_RSB_RSN_PATH);
            rsbBrowserPath = Project.getString(RSB_BROWSER_PATH, DEFAULT_RSB_BROWSER_PATH);
*/
            // PVT
            pvtOnServer = Project.getBoolean(PVT_ON_SERVER, DEFAULT_PVT_ON_SERVER);
            fevOnServer = Project.getBoolean(FEV_ON_SERVER, DEFAULT_FEV_ON_SERVER);
            
            // Pacs
            usePacs = Project.getBoolean(USE_PACS, DEFAULT_USE_PACS);
            useSuffixSearch = Project.getBoolean(PACS_USE_SUFFIXSEARCH, DEFAULT_PACS_SUFFIX_SEARCH);
            remoteHost = Project.getString(PACS_REMOTE_HOST, DEFAULT_PACS_REMOTE_HOST);
            remotePort = Project.getInt(PACS_REMOTE_PORT, DEFAULT_PACS_REMOTE_PORT);
            remoteAE = Project.getString(PACS_REMOTE_AE, DEFAULT_PACS_REMOTE_AE);
            localHost = Project.getString(PACS_LOCAL_HOST, DEFAULT_PACS_LOCAL_HOST);
            localPort = Project.getInt(PACS_LOCAL_PORT, DEFAULT_PACS_LOCAL_PORT);
            localAE = Project.getString(PACS_LOCAL_AE, DEFAULT_PACS_LOCAL_AE);

            // Chart stateをサーバーと同期
            //useJms = Project.getBoolean(RP_OUT, DEFAULT_USE_JMS);
        }

        public void restore() {

            // ラベルプリンタのIPアドレス
            Project.setString(LBLPRT_ADDRESS, lblPrtAddress);
            // ラベルプリンタのポート番号
            Project.setInt(LBLPRT_PORT, lblPrtPort);

            Project.setBoolean(USE_FEV, useFEV);
            Project.setBoolean(SEND_PATIENT_INFO, sendPatientInfo);
            Project.setString(FEV_SHAREPATH, fevSharePath);
            Project.setString(FEV40_PATH, fev40Path);
            Project.setBoolean(USE_WINE, useWine);
            Project.setString(WINE_PATH, winePath);
            Project.setBoolean(FOLLOW_MEDICOM, followMedicom);
            Project.setBoolean(SANTEI_CHECK, santeiCheck);
            Project.setBoolean(RP_OUT, defaultExMed);
            //Project.setInt(STAMP_HOLDER_CELLPADDING, stampHolderCellPadding);
            Project.setInt(KARTE_SCROLL_TYPE, karteScrollType);
            Project.setBoolean(USE_VERTICAL_LAYOUT, verticalLayout);
/*
            // RS_Base
            Project.setBoolean(USE_RSB, useRsb);
            Project.setString(RSB_URL, rsbURL);
            Project.setString(RSB_DRS_PATH, rsbDrsPath);
            Project.setString(RSB_LINK_PATH, rsbLinkPath);
            Project.setString(RSB_RSN_PATH, rsbRsnPath);
            Project.setString(RSB_BROWSER_PATH, rsbBrowserPath);
*/
            // PVT
            Project.setBoolean(PVT_ON_SERVER, pvtOnServer);
            Project.setBoolean(FEV_ON_SERVER, fevOnServer);
            
            // Pacs
            Project.setString(PACS_REMOTE_HOST, remoteHost);
            Project.setInt(PACS_REMOTE_PORT, remotePort);
            Project.setString(PACS_REMOTE_AE, remoteAE);
            Project.setString(PACS_LOCAL_HOST, localHost);
            Project.setInt(PACS_LOCAL_PORT, localPort);
            Project.setString(PACS_LOCAL_AE, localAE);
            Project.setBoolean(USE_PACS ,usePacs);
            Project.setBoolean(PACS_USE_SUFFIXSEARCH, useSuffixSearch);

            // Chart stateをサーバーと同期
            //Project.setBoolean(USE_JMS, useJms);
        }
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

        private boolean isValid() {

            // ラベルプリンタのip addressが設定されていない場合は、ラベルプリンタ不使用として
            // valid stateとする masuda
            if ("".equals(tf_lblPrtAddress.getText())) {
                return true;
            }

            boolean lblPrtAddrOk = !tf_lblPrtAddress.getText().trim().isEmpty();
            boolean lblPrtPortOk = !tf_lblPrtPort.getText().trim().isEmpty();

            return (lblPrtAddrOk && lblPrtPortOk);
        }
    }

    private boolean isValidIp(String val) {
        if (val == null) {
            return false;
        }
        if ("localhost".equals(val.toLowerCase())) {
            return true;
        }
        if (val.matches("[0-9][0-9.]+$")) {
            return true;
        }
        return false;
    }

    private boolean isValidPort(String val) {
        if (val == null) {
            return false;
        }
        if (val.matches("^[0-9]+$")) {
            return true;
        }
        return false;
    }

    private void discardInspectorSize() {
        Project.getUserDefaults().remove("chartPanelLeftSize");
        Project.getUserDefaults().remove("chartInspectorsSize");
        Project.getUserDefaults().remove("chartPanelRightSize");
    }
    
    private void loadProperties() {
        
        Properties prop = Project.getUserDefaults();
        String userId = Project.getUserModel().getUserId();
        List<UserPropertyModel> list = MasudaDelegater.getInstance().getUserProperties(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (UserPropertyModel propModel : list) {
            prop.put(propModel.getKey(), propModel.getValue());
        }
    }
    
    private void saveProperties() {
        
        // サーバーに保存する前にPropertiesを更新する
        getContext().saveOnly();
        
        List<UserPropertyModel> list = new ArrayList<UserPropertyModel>();
        Properties prop = Project.getUserDefaults();
        String idAsLocal = Project.getUserModel().idAsLocal();
        String userId = Project.getUserModel().getUserId();
        String facilityId = Project.getFacilityId();
        
        for (Iterator itr = prop.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            UserPropertyModel propModel = new UserPropertyModel();
            propModel.setKey(key);
            propModel.setValue(value);
            propModel.setFacilityId(facilityId);

            if (!propModel.isFacilityCommon(key)) {
                // ユーザー固有ならばuserId(=idAsLocal)を設定する
                propModel.setUserId(idAsLocal);
            } else {
                // 施設共通ならfacilityIdを設定する
                propModel.setUserId(facilityId);
            }
            list.add(propModel);
        }
        
        MasudaDelegater.getInstance().postUserProperties(userId, list);
    }
}
