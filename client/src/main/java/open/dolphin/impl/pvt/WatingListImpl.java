package open.dolphin.impl.pvt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import open.dolphin.client.*;
import open.dolphin.delegater.PVTDelegater;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.table.ColumnSpec;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;
import open.dolphin.util.AgeCalculator;
import org.apache.commons.lang.time.DurationFormatUtils;

/**
 * 受付リスト。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class WatingListImpl extends AbstractMainComponent {

    // Window Title
    private static final String NAME = "受付リスト";
    // 担当分のみを表示するかどうかの preference key
    private static final String ASSIGNED_ONLY = "assignedOnly";
    // 修正送信アイコンの配列インデックス
    private static final int INDEX_MODIFY_SEND_ICON = 1;
    // 担当医未定の ORCA 医師ID
    private static final String UN_ASSIGNED_ID = "18080";
    private String orcaId;
    
    // JTableレンダラ用の男性カラー
    private static final Color MALE_COLOR = new Color(230, 243, 243);
    // JTableレンダラ用の女性カラー
    private static final Color FEMALE_COLOR = new Color(254, 221, 242);
    // 受付キャンセルカラー
    private static final Color CANCEL_PVT_COLOR = new Color(128, 128, 128);
    // その他カラー by pns
    private static final Color SHOSHIN_COLOR = new Color(180,220,240); //青っぽい色
    private static final Color KARTE_EMPTY_COLOR = new Color(250,200,160); //茶色っぽい色
    private static final Color DIAGNOSIS_EMPTY_COLOR = new Color(243,255,15); //黄色
    
    // 来院テーブルのカラム名
    private static final String[] COLUMN_NAMES = {
        "受付", "患者ID", "来院時間", "氏   名", "性別", "保険", 
        "生年月日", "担当医", "診療科", "予約", "メモ", "状態"};
    // 来院テーブルのカラムメソッド
    private static final String[] PROPERTY_NAMES = {
        "getNumber", "getPatientId", "getPvtDateTrimDate", "getPatientName", "getPatientGenderDesc", "getFirstInsurance",
        "getPatientAgeBirthday", "getDoctorName", "getDeptName", "getAppointment", "getMemo", "getStateInteger"};
    // 来院テーブルのクラス名
    private static final Class[] COLUMN_CLASSES = {
        Integer.class, String.class, String.class, String.class, String.class, String.class, 
        String.class, String.class, String.class, String.class, String.class, Integer.class};
    // 来院テーブルのカラム幅
    private static final int[] COLUMN_WIDTH = {
        20, 80, 60, 100, 40, 130, 
        130, 50, 60, 40, 80, 30};
    // 年齢生年月日メソッド 
    private final String[] AGE_METHOD = {"getPatientAgeBirthday", "getPatientBirthday"};
    
    // カラム仕様リスト
    private List<ColumnSpec> columnSpecs;
    // 受付時間カラム
    private int visitedTimeColumn;
    // 性別カラム
    private int sexColumn;
    // 年齢表示カラム
    private int ageColumn;
    // 来院情報テーブルのメモカラム
    private int memoColumn;
    // 来院情報テーブルのステータスカラム
    private int stateColumn;

    // PVT Table 
    private JTable pvtTable;
    // Table Model
    private ListTableModel<PatientVisitModel> pvtTableModel;
    // TableSorter
    private ListTableSorter sorter;
    
    // 性別レンダラフラグ 
    private boolean sexRenderer;
    // 年齢表示 
    private boolean ageDisplay;
    // 選択されている行を保存
    private int selectedRow;
    // View class
    private WatingListView view;
    // 更新時刻フォーマッタ
    private SimpleDateFormat timeFormatter;
    
    // Chart State
    private Integer[] chartBitArray = {
        new Integer(ChartImpl.BIT_OPEN), new Integer(ChartImpl.BIT_MODIFY_CLAIM), new Integer(ChartImpl.BIT_SAVE_CLAIM)};
    // Chart State を表示するアイコン
    private ImageIcon[] chartIconArray = {
        ClientContext.getImageIcon("open_16.gif"), 
        ClientContext.getImageIcon("sinfo_16.gif"), 
        ClientContext.getImageIcon("flag_16.gif")};
    // State ComboBox
    private Integer[] userBitArray = {0, 3, 4, 5, 6};
    private ImageIcon[] userIconArray = {
        null, 
        ClientContext.getImageIcon("apps_16.gif"), 
        ClientContext.getImageIcon("fastf_16.gif"), 
        ClientContext.getImageIcon("cart_16.gif"), 
        ClientContext.getImageIcon("cancl_16.gif")};
    private ImageIcon modifySendIcon;
    
    // Status　情報　メインウィンドウの左下に表示される内容
    private String statusInfo;
    
    // ネットワークアイコン
    private static final ImageIcon NETWORK_ICON = ClientContext.getImageIcon("ntwrk_16.gif");

    // State 設定用のcombobox model
    private BitAndIconPair[] stateComboArray;
    // State 設定用のcombobox
    private JComboBox stateCmb;
    private AbstractAction copyAction;

    // 受付数・待ち時間の更新間隔
    private static final int intervalSync = 60;

    // pvtUpdateTask
    private ScheduledExecutorService executor;
    private ScheduledFuture schedule;
    private Runnable timerTask;
    
    // このクライアントのUUID
    private String clientUUID;
    
    // pvtCount
    private int totalPvtCount;
    private int waitingPvtCount;
    private Date waitingPvtDate;
    
    // PvtMessageの現番号
    private int currentId;
    // PatientVisitModelの全部
    private List<PatientVisitModel> pvtList;
    
    // pvt delegater
    private PVTDelegater pvtDelegater;

    /**
     * Creates new WatingList
     */
    public WatingListImpl() {
        setName(NAME);
    }
    
    /**
     * プログラムを開始する。
     */
    @Override
    public void start() {
        setup();
        initComponents();
        connect();
        startSyncMode();
    }
    
    private void setup() {

        // pvtTable deafult
        String defaultLine;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            String name = COLUMN_NAMES[i];
            String method = PROPERTY_NAMES[i];
            String cls = COLUMN_CLASSES[i].getName();
            String width = String.valueOf(COLUMN_WIDTH[i]);
            sb.append(name).append(",");
            sb.append(method).append(",");
            sb.append(cls).append(",");
            sb.append(width).append(",");
        }
        sb.setLength(sb.length() - 1);
        defaultLine = sb.toString();

        // preference から
        String line = Project.getString("pvtTable.column.spec", defaultLine);

        // 仕様を保存
        columnSpecs = new ArrayList<ColumnSpec>();
        String[] params = line.split(",");
        int len = params.length / 4;
        // 保存していた情報数が現在と違う場合は破棄
        if (len != COLUMN_NAMES.length) {
            params = defaultLine.split(",");
            len = params.length / 4;
        }
        for (int i = 0; i < len; i++) {
            int k = 4 * i;
            String name = params[k];
            String method = params[k + 1];
            String cls = params[k + 2];
            int width = Integer.parseInt(params[k + 3]);
            ColumnSpec cp = new ColumnSpec(name, method, cls, width);
            columnSpecs.add(cp);
        }

        // Scan して age, memo, state カラムを設定する
        for (int i = 0; i < columnSpecs.size(); i++) {
            ColumnSpec cs = columnSpecs.get(i);
            String test = cs.getMethod();

            if (test.equals("getPvtDateTrimDate")) {
                visitedTimeColumn = i;

            } else if (test.equals("getPatientGenderDesc")) {
                sexColumn = i;

            } else if (test.endsWith("Birthday")) {
                ageColumn = i;

            } else if (test.equals("getMemo")) {
                memoColumn = i;

            } else if (test.equals("getStateInteger")) {
                stateColumn = i;
            }
        }

        // 修正送信アイコンを決める
        if (Project.getBoolean("change.icon.modify.send", true)) {
            modifySendIcon = ClientContext.getImageIcon("sinfo_16.gif");
        } else {
            modifySendIcon = ClientContext.getImageIcon("flag_16.gif");
        }
        chartIconArray[INDEX_MODIFY_SEND_ICON] = modifySendIcon;

        stateComboArray = new BitAndIconPair[userBitArray.length];
        for (int i = 0; i < userBitArray.length; i++) {
            stateComboArray[i] = new BitAndIconPair(userBitArray[i], userIconArray[i]);
        }
        stateCmb = new JComboBox(stateComboArray);
        ComboBoxRenderer renderer = new ComboBoxRenderer();
        renderer.setPreferredSize(new Dimension(30, ClientContext.getHigherRowHeight()));
        stateCmb.setRenderer(renderer);
        stateCmb.setMaximumRowCount(userBitArray.length);

        sexRenderer = Project.getBoolean("sexRenderer", false);
        ageDisplay = Project.getBoolean("ageDisplay", true);
        timeFormatter = new SimpleDateFormat("HH:mm");
        
        executor = Executors.newSingleThreadScheduledExecutor();
        clientUUID = Dolphin.getInstance().getClientUUID();
        pvtDelegater = PVTDelegater.getInstance();
        orcaId = Project.getUserModel().getOrcaId();
    }
    
    /**
     * GUI コンポーネントを初期化しレアイアウトする。
     */
    private void initComponents() {

        // View クラスを生成しこのプラグインの UI とする
        view = new WatingListView();
        setUI(view);

        view.getPvtInfoLbl().setText("");

        //------------------------------------------
        // View のテーブルモデルを置き換える
        //------------------------------------------
        int len = columnSpecs.size();
        String[] colunNames = new String[len];
        String[] methods = new String[len];
        Class[] cls = new Class[len];
        int[] width = new int[len];
        try {
            for (int i = 0; i < len; i++) {
                ColumnSpec cp = columnSpecs.get(i);
                colunNames[i] = cp.getName();
                methods[i] = cp.getMethod();
                cls[i] = Class.forName(cp.getCls());
                width[i] = cp.getWidth();
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
        pvtTable = view.getTable();
        pvtTableModel = new ListTableModel<PatientVisitModel>(colunNames, 1, methods, cls) {

            @Override
            public boolean isCellEditable(int row, int col) {

                boolean canEdit = true;

                // メモか状態カラムの場合
                canEdit = canEdit && ((col == memoColumn) || (col == stateColumn));

                // null でない場合
                canEdit = canEdit && (getObject(row) != null);

                if (!canEdit) {
                    return false;
                }

                // statusをチェックする
                PatientVisitModel pm = getObject(row);
                int state = pm.getState();

                if ((state & (1 << ChartImpl.BIT_CANCEL)) != 0) {
                    // cancel case
                    canEdit = false;

                } else {
                    // Chartビットがたっている場合は不可
                    for (int i = 0; i < chartBitArray.length; i++) {
                        if ((state & (1 << chartBitArray[i])) != 0) {
                            canEdit = false;
                            break;
                        }
                    }
                }

                return canEdit;
            }

            @Override
            public Object getValueAt(int row, int col) {

                Object ret = null;

                if (col == ageColumn && ageDisplay) {

                    PatientVisitModel p = getObject(row);

                    if (p != null) {
                        int showMonth = Project.getInt("ageToNeedMonth", 6);
                        ret = AgeCalculator.getAgeAndBirthday(p.getPatientModel().getBirthday(), showMonth);
                    }
                } else {

                    ret = super.getValueAt(row, col);
                }

                return ret;
            }

            @Override
            public void setValueAt(Object value, int row, int col) {

                // ここはsorterから取得したらダメ
                //final PatientVisitModel pvt = (PatientVisitModel) sorter.getObject(row);
                final PatientVisitModel pvt = (PatientVisitModel) pvtTableModel.getObject(row);
                
                if (pvt == null || value == null) {
                    return;
                }

                // Memo
                if (col == memoColumn) {
                    String memo = ((String) value).trim();
                    if (memo != null && (!memo.equals(""))) {
                        PvtMessageModel msg = new PvtMessageModel(pvt);
                        updateState(msg);
                    }

                } else if (col == stateColumn) {

                    // State ComboBox の value
                    BitAndIconPair pair = (BitAndIconPair) value;
                    int theBit = pair.getBit().intValue();

                    if (theBit == ChartImpl.BIT_CANCEL) {

                        Object[] cstOptions = new Object[]{"はい", "いいえ"};

                        StringBuilder sb = new StringBuilder(pvt.getPatientName());
                        sb.append("様の受付を取り消しますか?");
                        String msg = sb.toString();

                        int select = JOptionPane.showOptionDialog(
                                SwingUtilities.getWindowAncestor(pvtTable),
                                msg,
                                ClientContext.getFrameTitle(getName()),
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                ClientContext.getImageIcon("cancl_32.gif"),
                                cstOptions, "はい");

                        System.err.println("select=" + select);

                        if (select != 0) {
                            return;
                        }
                    }

                    // unset all
                    int state = 0;

                    // set the bit
                    if (theBit != 0) {
                        state = state | (1 << theBit);
                    }

                    PvtMessageModel msg = new PvtMessageModel(pvt);
                    msg.setState(state);
                    updateState(msg);
                }
            }
        };

        // sorter組み込み
        sorter = new ListTableSorter(pvtTableModel);
        pvtTable.setModel(sorter);
        sorter.setTableHeader(pvtTable.getTableHeader());

        // 選択モード
        pvtTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 行高
        //if (ClientContext.isWin()) {
        //    pvtTable.setRowHeight(ClientContext.getMoreHigherRowHeight());
        //} else {
        //    pvtTable.setRowHeight(ClientContext.getHigherRowHeight());
        //}

        // カラム幅
        //for (int i = 0; i < width.length; i++) {
        //    pvtTable.getColumnModel().getColumn(i).setPreferredWidth(width[i]);
        //}
        //pvtTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        changeColumnWidth();

        // Memo 欄 clickCountToStart=1
        JTextField tf = new JTextField();
        tf.addFocusListener(AutoKanjiListener.getInstance());
        DefaultCellEditor de = new DefaultCellEditor2(tf);
        de.setClickCountToStart(1);
        pvtTable.getColumnModel().getColumn(memoColumn).setCellEditor(de);

        // 性別レンダラを生成する
        MaleFemaleRenderer sRenderer = new MaleFemaleRenderer();
        sRenderer.setTable(pvtTable);
        // Center Renderer
        CenterRenderer centerRenderer = new CenterRenderer();
        centerRenderer.setTable(pvtTable);

        for (int i = 0; i < columnSpecs.size(); i++) {

            if (i == visitedTimeColumn || i == sexColumn) {
                pvtTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);

            } else if (i == stateColumn) {
                // カルテ(PVT)状態レンダラ
                KarteStateRenderer renderer = new KarteStateRenderer();
                renderer.setTable(pvtTable);
                renderer.setHorizontalAlignment(JLabel.CENTER);
                pvtTable.getColumnModel().getColumn(i).setCellRenderer(renderer);

            } else {
                pvtTable.getColumnModel().getColumn(i).setCellRenderer(sRenderer);
            }
        }

        // PVT状態設定エディタ
        pvtTable.getColumnModel().getColumn(stateColumn).setCellEditor(new DefaultCellEditor(stateCmb));

        // 担当分のみを表示するかどうかにチェックする
        //view.getAssignedMeChk().setSelected(Project.getBoolean(FILTER_ASSIGNED_FOR_ME, false));
    }

    /**
     * コンポーネントにイベントハンドラーを登録し相互に接続する。
     */
    private void connect() {

        // pvtTableModel のカラム変更関連イベント
        pvtTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {

            @Override
            public void columnAdded(TableColumnModelEvent tcme) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent tcme) {
            }

            @Override
            public void columnMoved(TableColumnModelEvent tcme) {
                int from = tcme.getFromIndex();
                int to = tcme.getToIndex();
                ColumnSpec moved = columnSpecs.remove(from);
                columnSpecs.add(to, moved);
            }

            @Override
            public void columnMarginChanged(ChangeEvent ce) {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent lse) {
            }
        });

        // Chart のリスナになる
        // 患者カルテの Open/Save/SaveTemp の通知を受けて受付リストの表示を制御する
        ChartImpl.addPropertyChangeListener(ChartImpl.CHART_STATE, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(ChartImpl.CHART_STATE)) {
                    updateState((PvtMessageModel) evt.getNewValue());
                }
            }
        });

        // 来院リストテーブル 選択
        pvtTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    selectedRow = pvtTable.getSelectedRow();
                    controlMenu();
                }
            }
        });

        // 来院リストテーブル ダブルクリック
        view.getTable().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openKarte();
                }
            }
        });

        // コンテキストメニューを登録する
        view.getTable().addMouseListener(new ContextListener());

        // 靴のアイコンをクリックした時来院情報を検索する
        view.getKutuBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // 同期モードではPvtListを取得し直し
                SwingWorker worker = new SwingWorker() {

                    @Override
                    protected Object doInBackground() throws Exception {
                        getFullPvt();
                        return null;
                    }
                };
                worker.execute();
            }
        });

        //-----------------------------------------------
        // Copy 機能を実装する
        //-----------------------------------------------
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        copyAction = new AbstractAction("コピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyRow();
            }
        };
        pvtTable.getInputMap().put(copy, "Copy");
        pvtTable.getActionMap().put("Copy", copyAction);
    }

    // comet long polling機能を設定する
    private void startSyncMode() {
        setStatusInfo();
        getFullPvt();
        PvtEventListener listener = new PvtEventListener();
        pvtDelegater.addPropertyChangeListener(PVTDelegater.PVT_MESSAGE_EVENT, listener);
        pvtDelegater.subscribePvt();
        timerTask = new UpdatePvtInfoTask();
        restartTimer();
        enter();
    }
    
    // サーバーからのプッシュを処理する
    private class PvtEventListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            
            List<PvtMessageModel> list = pvtDelegater.getPvtMessageList(currentId);
            currentId = (Integer) evt.getNewValue();
            if (list == null) {
                return;
            }
            for (PvtMessageModel msg : list) {
                if (!clientUUID.equals(msg.getIssuerUUID())) {
                    updatePvtList(msg);
                }
            }

            // PvtInfoを更新する
            countPvt();
            updatePvtInfo();
        }
    }
    
    /**
     * タイマーをリスタートする。
     */
    private void restartTimer() {

        if (schedule != null && !schedule.isCancelled()) {
            if (!schedule.cancel(true)) {
                return;
            }
        }

        // 同期モードでは毎分０秒に待ち患者数を更新する
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(now.getTime());
        gc.clear(GregorianCalendar.SECOND);
        gc.clear(GregorianCalendar.MILLISECOND);
        gc.add(GregorianCalendar.MINUTE, 1);
        long delay = gc.getTimeInMillis() - now.getTimeInMillis();
        long interval = intervalSync * 1000;

        schedule = executor.scheduleWithFixedDelay(timerTask, delay, interval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * メインウインドウのタブで受付リストに切り替わった時 コールされる。
     */
    @Override
    public void enter() {
        controlMenu();
        getContext().getStatusLabel().setText(statusInfo);
    }

    /**
     * プログラムを終了する。
     */
    @Override
    public void stop() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columnSpecs.size(); i++) {
            ColumnSpec cs = columnSpecs.get(i);
            cs.setWidth(pvtTable.getColumnModel().getColumn(i).getPreferredWidth());
            sb.append(cs.getName()).append(",");
            sb.append(cs.getMethod()).append(",");
            sb.append(cs.getCls()).append(",");
            sb.append(cs.getWidth()).append(",");
        }
        sb.setLength(sb.length() - 1);
        String line = sb.toString();
        Project.setString("pvtTable.column.spec", line);
        pvtDelegater.disposePollingTask();
    }


    /**
     * 性別レンダラかどうかを返す。
     *
     * @return 性別レンダラの時 true
     */
    public boolean isSexRenderer() {
        return sexRenderer;
    }

    /**
     * レンダラをトグルで切り替える。
     */
    public void switchRenderere() {
        sexRenderer = !sexRenderer;
        Project.setBoolean("sexRenderer", sexRenderer);
        if (pvtTable != null) {
            pvtTableModel.fireTableDataChanged();
        }
    }

    /**
     * 年齢表示をオンオフする。
     */
    public void switchAgeDisplay() {
        if (pvtTable != null) {
            ageDisplay = !ageDisplay;
            Project.setBoolean("ageDisplay", ageDisplay);
            String method = ageDisplay ? AGE_METHOD[0] : AGE_METHOD[1];
            pvtTableModel.setProperty(method, ageColumn);
            for (int i = 0; i < columnSpecs.size(); i++) {
                ColumnSpec cs = columnSpecs.get(i);
                String test = cs.getMethod();
                if (test.toLowerCase().endsWith("birthday")) {
                    cs.setMethod(method);
                    break;
                }
            }
        }
    }

    /**
     * テーブル及び靴アイコンの enable/diable 制御を行う。
     *
     * @param busy pvt 検索中は true
     */
    private void setBusy(final boolean busy) {
        
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (busy) {
                    view.getKutuBtn().setEnabled(false);
                    if (getContext().getCurrentComponent() == getUI()) {
                        getContext().block();
                        getContext().getProgressBar().setIndeterminate(true);
                    }
                    selectedRow = pvtTable.getSelectedRow();
                } else {
                    view.getKutuBtn().setEnabled(true);
                    if (getContext().getCurrentComponent() == getUI()) {
                        getContext().unblock();
                        getContext().getProgressBar().setIndeterminate(false);
                        getContext().getProgressBar().setValue(0);
                    }
                    pvtTable.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
                }
            }
        });
    }

    /**
     * 選択されている来院情報を設定返す。
     *
     * @return 選択されている来院情報
     */
    public PatientVisitModel getSelectedPvt() {
        selectedRow = pvtTable.getSelectedRow();
        return (PatientVisitModel) sorter.getObject(selectedRow);
    }



    /**
     * カルテオープンメニューを制御する。
     */
    private void controlMenu() {
        PatientVisitModel pvt = getSelectedPvt();
        boolean enabled = canOpen(pvt);
        getContext().enabledAction(GUIConst.ACTION_OPEN_KARTE, enabled);
    }

    public void openKarte() {

        PatientVisitModel pvt = getSelectedPvt();
        if (pvt == null) {
            return;
        }
        getContext().openKarte(pvt);
    }

  /**
     * カルテを開くことが可能かどうかを返す。
     *
     * @return 開くことが可能な時 true
     */
    private boolean canOpen(PatientVisitModel pvt) {
        
        if (pvt == null) {
            return false;
        }
        // Cancelなら開けない
        if (pvt.hasStateBit(ChartImpl.BIT_CANCEL)) {
            return false;
        }
        // 開いてたら開けない
        if (pvt.hasStateBit(ChartImpl.BIT_OPEN)) {
            return false;
        }
        return true;
    }

    /**
     * 受付リストのコンテキストメニュークラス。
     */
    class ContextListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            mabeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mabeShowPopup(e);
        }

        public void mabeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {

                final JPopupMenu contextMenu = new JPopupMenu();
                String pop3 = "偶数奇数レンダラを使用する";
                String pop4 = "性別レンダラを使用する";
                String pop5 = "年齢表示";
                String pop6 = "担当分のみ表示";
                String pop7 = "修正送信を注意アイコンにする";

                int row = pvtTable.rowAtPoint(e.getPoint());
                PatientVisitModel obj = getSelectedPvt();
                
                if (row == selectedRow && obj != null && !obj.hasStateBit(ChartImpl.BIT_CANCEL)) {
                    String pop1 = "カルテを開く";
                    contextMenu.add(new JMenuItem(
                            new ReflectAction(pop1, WatingListImpl.this, "openKarte")));
                    contextMenu.addSeparator();
                    contextMenu.add(new JMenuItem(copyAction));
                    // pvt削除
                    contextMenu.add(new JMenuItem(
                            new ReflectAction("受付削除", WatingListImpl.this, "removePvt")));
                    contextMenu.addSeparator();
                }
                
                // pvt cancelのundo
                if (row == selectedRow && obj != null && obj.hasStateBit(ChartImpl.BIT_CANCEL)) {
                    contextMenu.add(new JMenuItem(
                            new ReflectAction("キャンセル取消", WatingListImpl.this, "undoCancelPvt")));
                    contextMenu.addSeparator();
                }
                
                JRadioButtonMenuItem oddEven = new JRadioButtonMenuItem(
                        new ReflectAction(pop3, WatingListImpl.this, "switchRenderere"));
                JRadioButtonMenuItem sex = new JRadioButtonMenuItem(
                        new ReflectAction(pop4, WatingListImpl.this, "switchRenderere"));
                ButtonGroup bg = new ButtonGroup();
                bg.add(oddEven);
                bg.add(sex);
                contextMenu.add(oddEven);
                contextMenu.add(sex);
                if (sexRenderer) {
                    sex.setSelected(true);
                } else {
                    oddEven.setSelected(true);
                }

                JCheckBoxMenuItem item = new JCheckBoxMenuItem(pop5);
                contextMenu.add(item);
                item.setSelected(ageDisplay);
                item.addActionListener(
                        EventHandler.create(ActionListener.class, WatingListImpl.this, "switchAgeDisplay"));

                // 担当分のみ表示: getOrcaId() != nullでメニュー
                if (Project.getUserModel().getOrcaId() != null) {
                    contextMenu.addSeparator();

                    // 担当分のみ表示
                    JCheckBoxMenuItem item2 = new JCheckBoxMenuItem(pop6);
                    contextMenu.add(item2);
                    item2.setSelected(Project.getBoolean(ASSIGNED_ONLY, false));
                    item2.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            boolean now = Project.getBoolean(ASSIGNED_ONLY, false);
                            Project.setBoolean(ASSIGNED_ONLY, !now);
                            filterPatients();
                        }
                    });
                }

                // 修正送信を注意アイコンにする ON/OF default = ON
                JCheckBoxMenuItem item3 = new JCheckBoxMenuItem(pop7);
                contextMenu.add(item3);
                item3.setSelected(Project.getBoolean("change.icon.modify.send", true));
                item3.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        boolean curIcon = Project.getBoolean("change.icon.modify.send", true);
                        boolean change = !curIcon;
                        Project.setBoolean("change.icon.modify.send", change);
                        changeModiSendIcon();
                    }
                });
                
                JMenu menu = new JMenu("表示カラム");
                contextMenu.add(menu);
                for (ColumnSpec cs : columnSpecs) {
                    final MyCheckBoxMenuItem cbm = new MyCheckBoxMenuItem(cs.getName());
                    cbm.setColumnSpec(cs);
                    if (cs.getWidth() != 0) {
                        cbm.setSelected(true);
                    }
                    cbm.addActionListener(new ActionListener(){

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (cbm.isSelected()) {
                                cbm.getColumnSpec().setWidth(50);
                            } else {
                                cbm.getColumnSpec().setWidth(0);
                            }
                            changeColumnWidth();
                        }
                    });
                    menu.add(cbm);
                }

                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    
    private class MyCheckBoxMenuItem extends JCheckBoxMenuItem {
        
        private ColumnSpec cs;
        
        private MyCheckBoxMenuItem(String name) {
            super(name);
        }
        
        private void setColumnSpec(ColumnSpec cs) {
            this.cs = cs;
        }
        private ColumnSpec getColumnSpec() {
            return cs;
        }
    }
    
    private void changeColumnWidth() {
 
        for (int i = 0; i < columnSpecs.size(); ++i) {
            ColumnSpec cs = columnSpecs.get(i);
            int width = cs.getWidth();
            TableColumn tc = pvtTable.getColumnModel().getColumn(i);
            if (width != 0) {
                tc.setMaxWidth(Integer.MAX_VALUE);
                tc.setPreferredWidth(width);
                tc.setWidth(width);
            } else {
                tc.setMaxWidth(0);
                tc.setMinWidth(0);
                tc.setPreferredWidth(0);
                tc.setWidth(0);
                
            }
        }
        pvtTable.repaint();
    }

    /**
     * 修正送信アイコンを決める
     *
     * @param change
     */
    private void changeModiSendIcon() {

        // 修正送信アイコンを決める
        if (Project.getBoolean("change.icon.modify.send", true)) {
            modifySendIcon = ClientContext.getImageIcon("sinfo_16.gif");
        } else {
            modifySendIcon = ClientContext.getImageIcon("flag_16.gif");
        }
        chartIconArray[INDEX_MODIFY_SEND_ICON] = modifySendIcon;

        // 表示を更新する
        pvtTableModel.fireTableDataChanged();
    }

    /**
     * 選択されている行をコピーする。
     */
    public void copyRow() {

        StringBuilder sb = new StringBuilder();
        int numRows = pvtTable.getSelectedRowCount();
        int[] rowsSelected = pvtTable.getSelectedRows();
        int numColumns = pvtTable.getColumnCount();

        for (int i = 0; i < numRows; i++) {
            if (sorter.getObject(rowsSelected[i]) != null) {
                StringBuilder s = new StringBuilder();
                for (int col = 0; col < numColumns; col++) {
                    Object o = pvtTable.getValueAt(rowsSelected[i], col);
                    if (o != null) {
                        s.append(o.toString());
                    }
                    s.append(",");
                }
                if (s.length() > 0) {
                    s.setLength(s.length() - 1);
                }
                sb.append(s.toString()).append("\n");
            }
        }
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
        }
    }
    
    /**
     * 選択した患者の受付キャンセルをundoする。masuda
     */
    public void undoCancelPvt() {

        final PatientVisitModel pvtModel = getSelectedPvt();

        // ダイアログを表示し確認する
        StringBuilder sb = new StringBuilder(pvtModel.getPatientName());
        sb.append("様の受付キャンセルを取り消しますか?");
        if (!showCancelDialog(sb.toString())) {
            return;
        }
        
        // updateStateする。
        int state = pvtModel.getState();
        state = state & ~(1 << ChartImpl.BIT_CANCEL);
        pvtModel.setState(state);
        PvtMessageModel msg = new PvtMessageModel(pvtModel);
        updateState(msg);
    }
    
    /**
     * 選択した患者の受付を削除する。masuda
     */
    public void removePvt() {

        final PatientVisitModel pvtModel = getSelectedPvt();

        // ダイアログを表示し確認する
        StringBuilder sb = new StringBuilder(pvtModel.getPatientName());
        sb.append("様の受付を削除しますか?");
        if (!showCancelDialog(sb.toString())) {
            return;
        }
        
        // 自クライアントのWaitingListを変更
        final PvtMessageModel msg = new PvtMessageModel(pvtModel);
        msg.setIssuerUUID(clientUUID);
        msg.setCommand(PvtMessageModel.CMD_DELETE);
        updatePvtList(msg);
        
        SwingWorker worker = new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() throws Exception {

                // サーバーに通知
                pvtDelegater.removePvt(pvtModel.getId());
                return null;
            }
        };
        worker.execute();
    }
    
    private boolean showCancelDialog(String msg) {

        final String[] cstOptions = new String[]{"はい", "いいえ"};

        int select = JOptionPane.showOptionDialog(
                SwingUtilities.getWindowAncestor(pvtTable),
                msg,
                ClientContext.getFrameTitle(getName()),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                ClientContext.getImageIcon("cancl_32.gif"),
                cstOptions, cstOptions[1]);
        return (select == 0);
    }
    
    /**
     * KarteStateRenderer カルテ（チャート）の状態をレンダリングするクラス。
     */
    private class KarteStateRenderer extends StripeTableCellRenderer {

        /**
         * Creates new IconRenderer
         */
        public KarteStateRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {

            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
            
            PatientVisitModel pvt = (PatientVisitModel) sorter.getObject(row);
            Color fore = pvt != null && pvt.hasStateBit(ChartImpl.BIT_CANCEL) ? CANCEL_PVT_COLOR : table.getForeground();
            this.setForeground(fore);
            
            // 選択状態の場合はStripeTableCellRendererの配色を上書きしない
            if (pvt != null && !isSelected) {
                if (isSexRenderer()) {
                    if (pvt.getPatientModel().getGender().equals(IInfoModel.MALE)) {
                        this.setBackground(MALE_COLOR);
                    } else if (pvt.getPatientModel().getGender().equals(IInfoModel.FEMALE)) {
                        this.setBackground(FEMALE_COLOR);
                    }
                }
                // 病名の状態に応じて背景色を変更 pns
                if (!pvt.hasStateBit(ChartImpl.BIT_CANCEL)) {
                    // 初診
                    if (pvt.isShoshin()) {
                        this.setBackground(SHOSHIN_COLOR);
                    }
                    // 病名ついてない
                    if (!pvt.hasByomei()) {
                        this.setBackground(DIAGNOSIS_EMPTY_COLOR);
                    }
                }
            }

            if (value != null && col == stateColumn) {

                int state = ((Integer) value).intValue();

                ImageIcon icon = null;

                // 最初に chart bit をテストする
                for (int i = 0; i < chartBitArray.length; i++) {
                    if ((state & (1 << chartBitArray[i])) != 0) {
                        if (i == ChartImpl.BIT_OPEN && !clientUUID.equals(pvt.getOwnerUUID())) {
                            icon = NETWORK_ICON;
                        } else {
                            icon = chartIconArray[i];
                        }
                        break;
                    }
                }

                // user bit をテストする
                if (icon == null) {

                    // bit 0 はパス
                    for (int i = 1; i < userBitArray.length; i++) {

                        int bit = userBitArray[i].intValue();
                        if ((state & (1 << bit)) != 0) {
                            icon = userIconArray[i];
                            break;
                        }
                    }
                }

                if (pvt.hasStateBit(ChartImpl.BIT_UNFINISHED)) {
                    setBackground(KARTE_EMPTY_COLOR);
                }

                this.setIcon(icon);
                this.setText("");

            } else {
                setIcon(null);
                this.setText(value == null ? "" : value.toString());
            }
            return this;
        }
    }

    /**
     * KarteStateRenderer カルテ（チャート）の状態をレンダリングするクラス。
     */
    private class MaleFemaleRenderer extends StripeTableCellRenderer {

        public MaleFemaleRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {

            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
            
            PatientVisitModel pvt = (PatientVisitModel) sorter.getObject(row);
            
            if (pvt != null) {
                if (pvt.hasStateBit(ChartImpl.BIT_CANCEL)) {
                    this.setForeground(CANCEL_PVT_COLOR);
                } else {
                    // 選択状態の場合はStripeTableCellRendererの配色を上書きしない
                    if (isSexRenderer() && !isSelected) {
                        if (pvt.getPatientModel().getGender().equals(IInfoModel.MALE)) {
                            this.setBackground(MALE_COLOR);
                        } else if (pvt.getPatientModel().getGender().equals(IInfoModel.FEMALE)) {
                            this.setBackground(FEMALE_COLOR);
                        }
                    }
                }
            }

            return this;
        }
    }

    private class CenterRenderer extends MaleFemaleRenderer {

        public CenterRenderer() {
            super();
            this.setHorizontalAlignment(JLabel.CENTER);
        }
    }

    /**
     * Iconを表示するJComboBox Renderer.
     */
    private class ComboBoxRenderer extends JLabel implements ListCellRenderer {

        public ComboBoxRenderer() {
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            BitAndIconPair pair = (BitAndIconPair) value;

            setIcon(pair.getIcon());
            return this;
        }
    }

    private class BitAndIconPair {

        private Integer bit;
        private ImageIcon icon;

        public BitAndIconPair(Integer bit, ImageIcon icon) {
            this.bit = bit;
            this.icon = icon;
        }

        public Integer getBit() {
            return bit;
        }

        public ImageIcon getIcon() {
            return icon;
        }
    }
    
    // 左下のstatus infoを設定する
    private void setStatusInfo() {

        StringBuilder sb = new StringBuilder();
        sb.append("更新間隔: ");
        sb.append(intervalSync);
        sb.append("秒 ");
        sb.append("同期");
        statusInfo = sb.toString();
    }

    // 更新時間・待ち人数などを設定する
    private void updatePvtInfo() {

        String waitingTime = "00:00";
        Date now = new Date();

        final StringBuilder sb = new StringBuilder();
        sb.append(timeFormatter.format(now));
        sb.append(" | ");
        sb.append("来院数");
        sb.append(String.valueOf(totalPvtCount));
        sb.append(" 待ち");
        sb.append(String.valueOf(waitingPvtCount));
        sb.append(" 待時間 ");
        if (waitingPvtDate != null && now.after(waitingPvtDate)){
            waitingTime = DurationFormatUtils.formatPeriod(waitingPvtDate.getTime(), now.getTime(), "HH:mm");
        }
        sb.append(waitingTime);
        view.getPvtInfoLbl().setText(sb.toString());
    }

//pns^
    /**
     * 来院数，待人数，待時間表示, modified by masuda
     */
    private void countPvt() {

        waitingPvtCount = 0;
        totalPvtCount = 0;
        waitingPvtDate = null;

        List<PatientVisitModel> dataList = pvtTableModel.getDataProvider();

        for (int i = 0; i < dataList.size(); i++) {
            PatientVisitModel pvt = dataList.get(i);
            if (!pvt.hasStateBit(ChartImpl.BIT_SAVE_CLAIM) && !pvt.hasStateBit(ChartImpl.BIT_MODIFY_CLAIM)) {
                // 診察未終了レコードをカウント，最初に見つかった未終了レコードの時間から待ち時間を計算
                ++waitingPvtCount;
                if (waitingPvtDate == null) {
                    waitingPvtDate = ModelUtils.getDateTimeAsObject(pvt.getPvtDate());
                }
            }
            if (!pvt.hasStateBit(ChartImpl.BIT_CANCEL)) {
                ++totalPvtCount;
            }
        }
    }
//pns$
    // 最終行を表示する
    private void showLastRow() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                int lastRow = pvtTableModel.getObjectCount() - 1;
                pvtTable.scrollRectToVisible(pvtTable.getCellRect(lastRow, 0, true));
            }
        });
    }
    
    private class UpdatePvtInfoTask implements Runnable {

        @Override
        public void run() {
            // 同期時は時刻と患者数を更新するのみ
            updatePvtInfo();
        }
    }
    
    // pvtを全取得する
    private void getFullPvt() {

        setBusy(true);

        // サーバーからpvtListを取得する
        PvtListModel model = pvtDelegater.getPvtListModel();
        pvtList = model.getPvtList();
        currentId = model.getNextId();

        // フィルタリング
        filterPatients();
        
        // 最終行までスクロール
        showLastRow();
        countPvt();
        updatePvtInfo();
        
        setBusy(false);
    }
    
    // 受付番号を振り、フィルタリングしてtableModelに設定する
    private void filterPatients() {
        
        boolean assignedOnly = Project.getBoolean(ASSIGNED_ONLY, false);
        
        List<PatientVisitModel> list;
        
        if (assignedOnly) {
            list = new ArrayList<PatientVisitModel>();
            for (PatientVisitModel pvt : pvtList) {
                String doctorId = pvt.getDoctorId();
                if (doctorId == null || doctorId.equals(orcaId) || doctorId.equals(UN_ASSIGNED_ID)) {
                    list.add(pvt);
                }
            }
        } else {
            list = new ArrayList<PatientVisitModel>(pvtList);
        }
        
        for (int i = 0; i < list.size(); ++i) {
            PatientVisitModel pvt = list.get(i);
            pvt.setNumber(i + 1);
        }
        pvtTableModel.setDataProvider(list);
        //pvtTable.repaint();
    }
    
    private void updateState(final PvtMessageModel msg) {

        // まずは自クライアントの受付リストの状態カラムを更新する
        msg.setIssuerUUID(clientUUID);
        msg.setCommand(PvtMessageModel.CMD_STATE);
        
        updatePvtList(msg);

        // データベースを更新する
        SimpleWorker worker = new SimpleWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                // サーバーに投げる
                pvtDelegater.updatePvtState(msg);
                return null;
            }

            @Override
            protected void succeeded(Void result) {
                ClientContext.getBootLogger().debug("ChartState の更新成功");
            }

            @Override
            protected void failed(Throwable cause) {
                ClientContext.getBootLogger().warn("ChartState の更新失敗");
            }
        };

        worker.execute();
    }
    
     // 待合リストを更新する
    private void updatePvtList(PvtMessageModel msg) {

        int command = msg.getCommand();
        List<PatientVisitModel> tableDataList = pvtTableModel.getDataProvider();
        boolean assignedOnly = Project.getBoolean(ASSIGNED_ONLY, false);

        switch (command) {
            case PvtMessageModel.CMD_ADD:
                PatientVisitModel model = msg.getPatientVisitModel();
                // pvtListに追加
                pvtList.add(model);
                // 担当でないならばテーブルに追加しない
                if (assignedOnly) {
                    String doctorId = model.getDoctorId();
                    if (doctorId != null && !doctorId.equals(orcaId) && !doctorId.equals(UN_ASSIGNED_ID)) {
                        break;
                    }
                }
                int sRow = selectedRow;
                pvtTableModel.addObject(model);
                // 番号を振る
                model.setNumber(tableDataList.size());
                // 選択中の行を保存
                // 保存した選択中の行を選択状態にする
                pvtTable.getSelectionModel().addSelectionInterval(sRow, sRow);
                // 追加した行は見えるようにスクロールする
                showLastRow();
                break;
                
            case PvtMessageModel.CMD_STATE:
                // pvtListを検索
                List<PatientVisitModel> toUpdateList = new ArrayList<PatientVisitModel>(2);
                for (PatientVisitModel pvt : pvtList) {
                    if (msg.getPvtPk() == pvt.getId()) {
                        toUpdateList.add(pvt);
                        break;
                    }
                }
                // テーブルの対応する行を検索
                sRow = -1;
                for (int row = 0; row < tableDataList.size(); ++row) {
                    PatientVisitModel pvt = tableDataList.get(row);
                    if (pvt.getId() == msg.getPvtPk()) {
                        toUpdateList.add(pvt);
                        sRow = row;
                        break;
                    }
                }
                // 更新する
                for (PatientVisitModel pvt : toUpdateList) {
                    pvt.setByomeiCount(msg.getByomeiCount());
                    pvt.setByomeiCountToday(msg.getByomeiCountToday());
                    pvt.setMemo(msg.getMemo());
                    // 所有権を変更するのは、所有権が設定されていないか、発行者が所有権を持っている場合である
                    if (pvt.getOwnerUUID() == null
                            || msg.getIssuerUUID().equals(pvt.getOwnerUUID())) {
                        // 発行者がpvtの所有者ならばstateを変更する
                        int newState = msg.getState();
                        pvt.setState(newState);
                        if (pvt.hasStateBit(ChartImpl.BIT_OPEN)) {
                            // Chartを開いたら所有権セットする
                            pvt.setOwnerUUID(msg.getOwnerUUID());
                        } else {
                            // Chartを閉じたら所有権を手放す
                            pvt.setOwnerUUID(null);
                            // PvtMessageModelのOwnerUUIDにもnullをセットする
                            msg.setOwnerUUID(null);
                        }
                    }
                }
                // テーブルのアイコンを更新する
                if (sRow != -1) {
                    pvtTableModel.fireTableRowsUpdated(sRow, sRow);
                }
                break;
                
            case PvtMessageModel.CMD_DELETE:
                // pvtListから削除
                PatientVisitModel toRemove = null;
                for (PatientVisitModel pvt : pvtList) {
                    if (msg.getPvtPk() == pvt.getId()) {
                        toRemove = pvt;
                        break;
                    }
                }
                if (toRemove != null) {
                    pvtList.remove(toRemove);
                }
                
                // 該当するpvtを削除し受付番号を振りなおす
                int counter = 0;
                toRemove = null;
                for (PatientVisitModel pm : tableDataList) {
                    if (pm.getId() == msg.getPvtPk()) {
                        toRemove = pm;
                    } else {
                        pm.setNumber(++counter);
                    }
                }
                if (toRemove != null) {
                    pvtTableModel.delete(toRemove);
                }
                break;
                
            case PvtMessageModel.CMD_RENEW:
                // 日付が変わるとCMD_RENEWが送信される。pvtListをサーバーから取得する
                getFullPvt();
                break;
                
            case PvtMessageModel.CMD_MERGE:
                // 同じ時刻のPVTで、PVTには追加されず、患者情報や保険情報の更新のみの場合
                // pvtListに変更
                for (PatientVisitModel pvt : pvtList) {
                    if (msg.getPvtPk() == pvt.getId()) {
                        // 受付番号を継承
                        int num = pvt.getNumber();
                        pvt = msg.getPatientVisitModel();
                        pvt.setNumber(num);
                        break;
                    }
                }
                // tableModelに変更
                for (int row = 0; row < tableDataList.size(); ++row) {
                    PatientVisitModel pvt = tableDataList.get(row);
                    if (pvt.getId() == msg.getPvtPk()) {
                        // 選択中の行を保存
                        sRow = selectedRow;
                        pvtTableModel.setObject(row, msg.getPatientVisitModel());
                        // 保存した選択中の行を選択状態にする
                        pvtTable.getSelectionModel().addSelectionInterval(sRow, sRow);
                        break;
                    }
                }
                break;
        }
    }
}