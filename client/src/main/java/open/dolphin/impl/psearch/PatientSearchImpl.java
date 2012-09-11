package open.dolphin.impl.psearch;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.*;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.delegater.PVTDelegater;
import open.dolphin.delegater.PatientDelegater;
import open.dolphin.dto.PatientSearchSpec;
import open.dolphin.helper.KeyBlocker;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.table.*;
import open.dolphin.util.AgeCalculator;
import open.dolphin.util.StringTool;

/**
 * 患者検索PatientSearchPlugin
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public class PatientSearchImpl extends AbstractMainComponent {

    private final String NAME = "患者検索";
    private static final String[] COLUMN_NAMES 
            = {"ID", "氏名", "カナ", "性別", "生年月日", "受診日", "状態"};
    private final String[] PROPERTY_NAMES 
            = {"patientId", "fullName", "kanaName", "genderDesc", "ageBirthday", "pvtDateTrimTime", "isOpened"};
    private static final Class[] COLUMN_CLASSES = {
        String.class, String.class, String.class, String.class, String.class, 
        String.class, String.class};
    private final int[] COLUMN_WIDTH = {50, 100, 120, 30, 100, 80, 20};
    private final int START_NUM_ROWS = 1;
   
    // カラム仕様名
    private static final String COLUMN_SPEC_NAME = "patientSearchTable.withoutAddress.column.spec";
    // カラム仕様ヘルパー
    private ColumnSpecHelper columnHelper;
    
    private static final String KEY_AGE_DISPLAY = "patientSearchTable.withoutAddress.ageDisplay";
    
    // 選択されている患者情報
    private PatientModel selectedPatient;
    // 年齢表示
    private boolean ageDisplay;
    // 年齢生年月日メソッド
    private final String[] AGE_METHOD = new String[]{"ageBirthday", "birthday"};
    // 受診日メソッド
    private static final String[] PVTDATE_METHOD = new String[]{"pvtDateTrimTime", "pvtDateTrimDate"};
    
    private static final String FINISHED = "finished";
    
    // View
    private PatientSearchView view;
    private KeyBlocker keyBlocker;

    // カラム仕様リスト
    private int ageColumn;
    private int pvtDateColumn;
    private int stateColumn;
    
    private ListTableModel<PatientModel> tableModel;
    private ListTableSorter sorter;
    private AbstractAction copyAction;
    
    private String clientUUID;
    private ChartEventListener cel;

    
    /** Creates new PatientSearch */
    public PatientSearchImpl() {
        setName(NAME);
        cel = ChartEventListener.getInstance();
        clientUUID = cel.getClientUUID();
    }

    @Override
    public void start() {
        setup();
        initComponents();
        connect();
        enter();
    }

    @Override
    public void enter() {
        controlMenu();
//pns   入ってきたら，キーワードフィールドにフォーカス
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                view.getKeywordFld().requestFocusInWindow();
                view.getKeywordFld().selectAll();
            }
        });
        
    }

    @Override
    public void stop() {
        
        // ColumnSpecsを保存する
        if (columnHelper != null) {
            columnHelper.saveProperty();
        }
        // ChartStateListenerから除去する
        cel.removeListener(this);
    }

    public PatientModel getSelectedPatient() {
        return selectedPatient;
    }

    public void setSelectedPatinet(PatientModel model) {
        selectedPatient = model;
        controlMenu();
    }

    @SuppressWarnings("unchecked")
    public ListTableModel<PatientModel> getTableModel() {
        return (ListTableModel<PatientModel>) view.getTable().getModel();
    }

    /**
     * 年齢表示をオンオフする。
     */
    public void switchAgeDisplay() {
        
        if (view.getTable() == null) {
            return;
        }

        ageDisplay = !ageDisplay;
        Project.setBoolean(KEY_AGE_DISPLAY, ageDisplay);
        String method = ageDisplay ? AGE_METHOD[0] : AGE_METHOD[1];
        ListTableModel tModel = getTableModel();
        tModel.setProperty(method, ageColumn);

        List<ColumnSpec> columnSpecs = columnHelper.getColumnSpecs();
        for (int i = 0; i < columnSpecs.size(); i++) {
            ColumnSpec cs = columnSpecs.get(i);
            String test = cs.getMethod();
            if (test.toLowerCase().endsWith("birthday")) {
                cs.setMethod(method);
                break;
            }
        }
    }

    /**
     * メニューを制御する
     */
    private void controlMenu() {

        PatientModel pvt = getSelectedPatient();
        boolean enabled = canOpen(pvt);
        getContext().enabledAction(GUIConst.ACTION_OPEN_KARTE, enabled);
    }

    /**
     * カルテを開くことが可能かどうかを返す。
     * @return 開くことが可能な時 true
     */
    private boolean canOpen(PatientModel patient) {
        if (patient == null) {
            return false;
        }

        if (isKarteOpened(patient)) {
            return false;
        }

        return true;
    }

    /**
     * カルテがオープンされているかどうかを返す。
     * @return オープンされている時 true
     */
    private boolean isKarteOpened(PatientModel patient) {
        if (patient != null) {
            boolean opened = false;
            List<ChartImpl> allCharts = Dolphin.getInstance().getAllCharts();
            for (ChartImpl chart : allCharts) {
                if (chart.getPatient().getId() == patient.getId()) {
                    opened = true;
                    break;
                }
            }
            return opened;
        }
        return false;
    }

    /**
     * 受付リストのコンテキストメニュークラス。
     */
    private class ContextListener extends MouseAdapter {

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

                int row = view.getTable().rowAtPoint(e.getPoint());
                PatientModel obj = (PatientModel) sorter.getObject(row);
                int selected = view.getTable().getSelectedRow();

                if (row == selected && obj != null) {
                    contextMenu.add(new JMenuItem(new ReflectAction("カルテを開く", PatientSearchImpl.this, "openKarte")));
                    contextMenu.addSeparator();
                    contextMenu.add(new JMenuItem(copyAction));
                    contextMenu.add(new JMenuItem(new ReflectAction("受付登録", PatientSearchImpl.this, "addAsPvt")));
                    contextMenu.addSeparator();
                }

                JCheckBoxMenuItem item = new JCheckBoxMenuItem("年齢表示");
                contextMenu.add(item);
                item.setSelected(ageDisplay);
                item.addActionListener(EventHandler.create(ActionListener.class, PatientSearchImpl.this, "switchAgeDisplay"));
                
//pns^  検索結果をファイル保存
                if (view.getTable().getRowCount() > 0) {
                    contextMenu.add(new JMenuItem(new ReflectAction("検索結果ファイル保存", PatientSearchImpl.this, "exportSearchResult")));
                }
//pns$
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    private void setup() {
        
        // ColumnSpecHelperを準備する
        columnHelper = new ColumnSpecHelper(COLUMN_SPEC_NAME,
                COLUMN_NAMES, PROPERTY_NAMES, COLUMN_CLASSES, COLUMN_WIDTH);
        columnHelper.loadProperty();

        // Scan して age / pvtDate カラムを設定する
        ageColumn = columnHelper.getColumnPositionEndsWith("birthday");
        pvtDateColumn = columnHelper.getColumnPositionStartWith("pvtdate");
        stateColumn = columnHelper.getColumnPosition("isOpened");
        
        ageDisplay = Project.getBoolean(KEY_AGE_DISPLAY, true);
    }
    
    /**
     * GUI コンポーネントを初期化する。
     *
     */
    private void initComponents() {
        
        // View
        view = new PatientSearchView();
        setUI(view);

        // ColumnSpecHelperにテーブルを設定する
        columnHelper.setTable(view.getTable());

        //------------------------------------------
        // View のテーブルモデルを置き換える
        //------------------------------------------
        String[] columnNames = columnHelper.getTableModelColumnNames();
        String[] methods = columnHelper.getTableModelColumnMethods();
        Class[] cls = columnHelper.getTableModelColumnClasses();

        // テーブルモデルを設定
        tableModel = new ListTableModel<PatientModel>(columnNames, START_NUM_ROWS, methods, cls) {

            @Override
            public Object getValueAt(int row, int col) {

                Object ret = null;

                if (col == ageColumn && ageDisplay) {

                    PatientModel p = getObject(row);

                    if (p != null) {
                        int showMonth = Project.getInt("ageToNeedMonth", 6);
                        ret = AgeCalculator.getAgeAndBirthday(p.getBirthday(), showMonth);
                    }
                } else {

                    ret = super.getValueAt(row, col);
                }

                return ret;
            }
        };
        
//masuda^   table sorter 組み込み
        sorter = new ListTableSorter(tableModel);
        view.getTable().setModel(sorter);
        sorter.setTableHeader(view.getTable().getTableHeader());
//masuda$
        // カラム幅更新
        columnHelper.updateColumnWidth();

        // 連ドラ、梅ちゃん先生
        PatientListTableRenderer renderer = new PatientListTableRenderer();
        renderer.setTable(view.getTable());
        renderer.setDefaultRenderer();

        // HibernateSearchを使用するかなど
        final JComboBox methodCombo = view.getMethodCombo();
        if (!useHibernateSearch()) {
            methodCombo.setSelectedItem(PatientSearchView.ALL_SEARCH);
        }
        
        methodCombo.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    boolean b = (methodCombo.getSelectedItem() == PatientSearchView.HIBERNATE_SEARCH);
                    Project.setBoolean(MiscSettingPanel.USE_HIBERNATE_SEARCH, b);
                }
            }
        });

        // カルテ検索Radioをシフト右クリックでインデックス作成
        view.getKarteSearchBtn().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                maybePopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                maybePopup(e);
            }
            private void maybePopup(MouseEvent e) {
                if ( e.isPopupTrigger() && e.isShiftDown()
                        && view.getKarteSearchBtn().isSelected()
                        && methodCombo.getSelectedItem() == PatientSearchView.HIBERNATE_SEARCH) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem mi;
                    mi = new JMenuItem("インデックス作成");
                    popup.add(mi);
                    mi.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            makeInitialIndex();
                        }
                    });
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        view.getPtSearchBtn().addItemListener(new ItemListener(){

            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean b = !view.getPtSearchBtn().isSelected();
                view.getMethodCombo().setEnabled(b);
            }
        });

        // 処方切れ検索
        view.getLoupeLbl().addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                maybePopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                maybePopup(e);
            }
            private void maybePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem mi;
                    mi = new JMenuItem("処方切れ患者検索");
                    popup.add(mi);
                    mi.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            checkShohougire();
                        }
                    });
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        // 件数を0件にする
        updateStatusLabel();
    }

    /**
     * コンポーンントにリスナを登録し接続する。
     */
    private void connect() {

        // ColumnHelperでカラム変更関連イベントを設定する
        columnHelper.connect();
        // ChartEventListenerに登録する
        cel.addListener(this);

        EventAdapter adp = new EventAdapter(view.getKeywordFld(), view.getTable());

        // カレンダによる日付検索を設定する
        PopupListener pl = new PopupListener(view.getKeywordFld());

        // コンテキストメニューを設定する
        view.getTable().addMouseListener(new ContextListener());

        keyBlocker = new KeyBlocker(view.getKeywordFld());

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
        view.getTable().getInputMap().put(copy, "Copy");
        view.getTable().getActionMap().put("Copy", copyAction);
    }

    private class EventAdapter extends MouseAdapter implements ActionListener, ListSelectionListener {

        public EventAdapter(JTextField tf, JTable tbl) {

            boolean autoIme = Project.getBoolean("autoIme", true);
            if (autoIme) {
                tf.addFocusListener(AutoKanjiListener.getInstance());
            } else {
                tf.addFocusListener(AutoRomanListener.getInstance());
            }
            tf.addActionListener(EventAdapter.this);

            tbl.getSelectionModel().addListSelectionListener(EventAdapter.this);
            tbl.addMouseListener(EventAdapter.this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField tf = (JTextField) e.getSource();
            String test = tf.getText().trim();
            if (!test.equals("")) {
                find(test);
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting() == false) {
                JTable table = view.getTable();
                //ListTableModel<PatientModel> tableModel = getTableModel();
                int row = table.getSelectedRow();
//pns   row = -1 でここに入ってくることあり
                if (row >= 0) {
                    PatientModel patient = (PatientModel) sorter.getObject(row);
                    setSelectedPatinet(patient);
                } else {
                    setSelectedPatinet(null);
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
//masuda    nullかどうかの判断はopenKarte()でされるのでここでは不要か
                openKarte();
            }
        }
    }

    /**
     * 選択されている行をコピーする。
     */
    public void copyRow() {

        StringBuilder sb = new StringBuilder();
        int numRows = view.getTable().getSelectedRowCount();
        int[] rowsSelected = view.getTable().getSelectedRows();
        int numColumns =   view.getTable().getColumnCount();

        for (int i = 0; i < numRows; i++) {
            if (tableModel.getObject(rowsSelected[i]) != null) {
                StringBuilder s = new StringBuilder();
                for (int col = 0; col < numColumns; col++) {
                    Object o = view.getTable().getValueAt(rowsSelected[i], col);
                    if (o!=null) {
                        s.append(o.toString());
                    }
                    s.append(",");
                }
                if (s.length()>0) {
                    s.setLength(s.length()-1);
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
     * カルテを開く。
     * @param value 対象患者
     */
    public void openKarte() {

        if (canOpen(getSelectedPatient())) {

            // 来院情報を生成する
            PatientModel pm = getSelectedPatient();
            PatientVisitModel pvt = cel.createFakePvt(pm);
            // カルテコンテナを生成する
            getContext().openKarte(pvt);
        }
    }

    // EVT から
    private void doStartProgress() {
        getContext().getProgressBar().setIndeterminate(true);
        getContext().getGlassPane().block();
        keyBlocker.block();
    }

    // EVT から
    private void doStopProgress() {
        getContext().getProgressBar().setIndeterminate(false);
        getContext().getProgressBar().setValue(0);
        getContext().getGlassPane().unblock();
        keyBlocker.unblock();
    }

    /**
     * リストで選択された患者を受付に登録する。
     */
    public void addAsPvt() {

        // 来院情報を生成する
        SimpleWorker worker = new SimpleWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                PatientModel pm = getSelectedPatient();
                PatientVisitModel pvt = cel.createFakePvt(pm);
                PVTDelegater pdl = PVTDelegater.getInstance();
                pdl.addPvt(pvt);
                return null;
            }

            @Override
            protected void succeeded(Void result) {
            }

            @Override
            protected void failed(Throwable cause) {
            }

            @Override
            protected void startProgress() {
                doStartProgress();
            }

            @Override
            protected void stopProgress() {
                doStopProgress();
            }
        };

        worker.execute();
    }


    /**
     * 検索を実行する。
     * @param text キーワード
     */
    private void find(String text) {
        
        if (view.getPtSearchBtn().isSelected()) {

            PatientSearchSpec spec = new PatientSearchSpec();

            if (isDate(text)) {
                spec.setCode(PatientSearchSpec.DATE_SEARCH);
                spec.setDigit(text);

            } else if (StringTool.startsWithKatakana(text)) {
                spec.setCode(PatientSearchSpec.KANA_SEARCH);
                spec.setName(text);

            } else if (StringTool.startsWithHiragana(text)) {
                text = StringTool.hiraganaToKatakana(text);
                spec.setCode(PatientSearchSpec.KANA_SEARCH);
                spec.setName(text);

            } else if (isNameAddress(text)) {
                spec.setCode(PatientSearchSpec.NAME_SEARCH);
                spec.setName(text);

            } else {

                if (Project.getBoolean("zero.paddings.id.search", false)) {
                    int len = text.length();
                    int paddings = Project.getInt("patient.id.length", 0) - len;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < paddings; i++) {
                        sb.append("0");
                    }
                    sb.append(text);
                    text = sb.toString();
                }

                spec.setCode(PatientSearchSpec.DIGIT_SEARCH);
                spec.setDigit(text);
            }
            
            // PVT searchの場合はgetPvtTrimDate, その他はgetPvtTrimTime
            boolean trimDate = (spec.getCode() == PatientSearchSpec.DATE_SEARCH);
            setPvtDateMethodTrimDate(trimDate);

            SearchTask task = new SearchTask(spec);
            task.execute();

        } else {
            // 全文検索
            // 文字数が１文字だと hibernate search が止まってしまう
            if (useHibernateSearch() && text.length() <= 1) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(view),
                        "検索文字列は２文字以上入力して下さい",  "検索文字列入力エラー", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // pvtDateのmethod変更。いつもtrimDate = false
            setPvtDateMethodTrimDate(false);
            FullTextSearchTask task  = new FullTextSearchTask(text);
            task.execute();
        }

    }

    // カルテ検索のタスク
    private class SearchTask extends SimpleWorker<Collection<PatientModel>, Void> {

        private PatientSearchSpec searchSpec;

        private SearchTask(PatientSearchSpec spec) {
            searchSpec = spec;
        }

        @Override
        protected Collection<PatientModel> doInBackground() throws Exception {

            PatientDelegater pdl = PatientDelegater.getInstance();
            Collection<PatientModel> result = pdl.getPatients(searchSpec);
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void succeeded(Collection<PatientModel> result) {

            if (result != null){
                tableModel.setDataProvider((List<PatientModel>) result);
            } else {
                tableModel.clear();
            }
            updateStatusLabel();
        }

        @Override
        protected void failed(Throwable cause) {
        }

        @Override
        protected void startProgress() {
            doStartProgress();
        }

        @Override
        protected void stopProgress() {
            doStopProgress();
        }
    }

    private boolean isDate(String text) {
        boolean maybe = false;
        if (text != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.parse(text);
                maybe = true;

            } catch (Exception e) {
            }
        }

        return maybe;
    }

    private boolean isNameAddress(String text) {
        boolean maybe = false;
        if (text != null) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.getType(c) == Character.OTHER_LETTER) {
                    maybe = true;
                    break;
                }
            }
        }
        return maybe;
    }

    /**
     * テキストフィールドへ日付を入力するためのカレンダーポップアップメニュークラス。
     */
    private class PopupListener extends MouseAdapter implements PropertyChangeListener {

        /** ポップアップメニュー */
        private JPopupMenu popup;
        /** ターゲットのテキストフィールド */
        private JTextField tf;

        public PopupListener(JTextField tf) {
            this.tf = tf;
            tf.addMouseListener(PopupListener.this);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {
                popup = new JPopupMenu();
                CalendarCardPanel cc = new CalendarCardPanel(ClientContext.getEventColorTable());
                cc.addPropertyChangeListener(CalendarCardPanel.PICKED_DATE, this);
                cc.setCalendarRange(new int[]{-12, 0});
                popup.insert(cc, 0);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals(CalendarCardPanel.PICKED_DATE)) {
                SimpleDate sd = (SimpleDate) e.getNewValue();
                tf.setText(SimpleDate.simpleDateToMmldate(sd));
                popup.setVisible(false);
                popup = null;
                String test = tf.getText().trim();
                if (!test.equals("")) {
                    find(test);
                }
            }
        }
    }
    
    
//pns^
    /**
     * 検索結果をファイルに書き出す
     */
    public void exportSearchResult() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(view) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (!file.exists() || isOverwriteConfirmed(file)) {

                try {
                    FileWriter writer = new FileWriter(file);
                    JTable table = view.getTable();
                    // 書き出す内容
                    StringBuilder sb = new StringBuilder();
                    for (int row = 0; row < table.getRowCount(); row++) {
                        for (int column = 0; column < table.getColumnCount(); column++) {
                            sb.append(column == 0 ? "" : ',');
                            sb.append('"');
                            sb.append(table.getValueAt(row, column));
                            sb.append('"');
                        }
                        sb.append('\n');
                    }
                    writer.write(sb.toString());
                    // close
                    writer.close();

                } catch (IOException ex) {
                    System.out.println("PatientSearchImpl.java: " + ex);
                }
            }
        }
    }

    /**
     * ファイル上書き確認ダイアログを表示する。
     * @param file 上書き対象ファイル
     * @return 上書きOKが指示されたらtrue
     */
    private boolean isOverwriteConfirmed(File file) {
        String title = "上書き確認";
        String message = "既存のファイル「" + file.toString() + "」\n" + "を上書きしようとしています。続けますか？";

        int confirm = JOptionPane.showConfirmDialog(
                view, message, title,
                JOptionPane.WARNING_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);

        if (confirm == JOptionPane.OK_OPTION) {
            return true;
        }

        return false;
    }
//pns$
    
//masuda^
    // ステータスラベルに検索件数を表示
    private void updateStatusLabel() {
        int count = tableModel.getObjectCount();
        String msg = String.valueOf(count) + "件";
        this.getContext().getStatusLabel().setText(msg);
    }

    // pvtDateのmethodを変更
    private void setPvtDateMethodTrimDate(boolean trimDate) {

        String method = tableModel.getProperty(pvtDateColumn);
        if (trimDate) {
            if (!PVTDATE_METHOD[1].equals(method)) {
                tableModel.setProperty(PVTDATE_METHOD[1], pvtDateColumn);
            }
        } else {
            if (!PVTDATE_METHOD[0].equals(method)) {
                tableModel.setProperty(PVTDATE_METHOD[0], pvtDateColumn);
            }
        }
    }

    // 処方切れチェック
    public void checkShohougire(){

        SwingWorker worker = new SwingWorker<List<PatientModel>, Void>() {

            @Override
            protected List<PatientModel> doInBackground() throws Exception {
                doStartProgress();
                getContext().getGlassPane().setText("処方切れ患者を検索中です。");
                CheckMedication cm = new CheckMedication();
                List<PatientModel> result = cm.getShohougirePatient();
                return result;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done(){
                try {
                    List<PatientModel> result = get();
                    if (result != null) {
                        setPvtDateMethodTrimDate(false);
                        tableModel.setDataProvider(result);
                    } else {
                        tableModel.clear();
                    }
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                } finally{
                    doStopProgress();
                    updateStatusLabel();
                }
            }
        };
        worker.execute();
    }
    
    private class FullTextSearchTask extends SimpleWorker<List<PatientModel>, String[]> {

        private String searchText;
        private ProgressMonitor progressMonitor;
        private final String message = "カルテ内検索";
        private final String progressNote = "<html>「%s」を検索中<br>（%d％完了，%d件発見）";
        private final String startingNote = "処理を開始します。";
        private final String initialNote = "<html><br>";


        public FullTextSearchTask(String searchText) {
            this.searchText = searchText;
        }

        @Override
        protected List<PatientModel> doInBackground() throws Exception {

            doStartProgress();

            progressMonitor = new ProgressMonitor(view, message, initialNote, 0, 100);

            boolean hibernateSearch = view.getMethodCombo().getSelectedItem() == PatientSearchView.HIBERNATE_SEARCH;

            // 患者検索
            if (!hibernateSearch) {
                progressMonitor.setMillisToDecideToPopup(0); // この処理は絶対時間がかかるので，すぐ出す
                progressMonitor.setMillisToPopup(0);
                return grepSearch();
            } else {
                return hibernateSearch();
            }
        }

        private List<PatientModel> hibernateSearch() {
            // カルテ内検索をちょっとインチキする(Hibernate Search)
            publish(new String[]{startingNote, "50"});
            MasudaDelegater dl = MasudaDelegater.getInstance();
            // 患者を絞らない場合は karteId = 0 を設定する
            return dl.getKarteFullTextSearch(0, searchText);
        }

        @SuppressWarnings("unchecked")
        private List<PatientModel> grepSearch() {

            final int maxResult = 500;
            final boolean progressCourseOnly 
                    = view.getMethodCombo().getSelectedItem() == PatientSearchView.CONTENT_SEARCH;

            // 検索開始
            MasudaDelegater dl = MasudaDelegater.getInstance();
            HashSet<PatientModel> pmSet = new HashSet<PatientModel>();
            SearchResultModel srm = new SearchResultModel();

            long fromId = 0;
            int page = 0;
            // progress bar 表示
            publish(new String[]{startingNote, "0"});
            
            while (srm != null) {
                // キャンセルされた場合
                if (progressMonitor.isCanceled()) {
                    return new ArrayList<PatientModel>(pmSet);
                }
                srm = dl.getSearchResult(searchText, fromId, maxResult, progressCourseOnly);

                if (srm != null) {
                    long moduleCount = srm.getTotalCount();
                    fromId = srm.getDocPk();
                    List<PatientModel> newList = srm.getResultList();

                    for (PatientModel pm : newList) {
                        boolean found = false;
                        for (PatientModel old : pmSet) {
                            if (old.getId() == pm.getId()) {
                                List<Long> docIdList = old.getDocPkList();
                                if (docIdList != null) {
                                    HashSet<Long> pkSet = new HashSet<Long>();
                                    pkSet.addAll(docIdList);
                                    if (pm.getDocPkList() != null) {
                                        pkSet.addAll(pm.getDocPkList());
                                    }
                                    pm.setDocPkList(new ArrayList(pkSet));
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            pmSet.add(pm);
                        }
                    }
                    page++;
                    // progress bar 表示
                    int ratio = (moduleCount == 0)
                            ? 0 : (int) (100 * page * maxResult / moduleCount);
                    String msg = String.format(progressNote, searchText, ratio, pmSet.size());
                    publish(new String[]{msg, String.valueOf(ratio)});
                }

            }
            return new ArrayList<PatientModel>(pmSet);
        }

        @Override
        protected void process(List<String[]> chunks) {
            for (String[] chunk : chunks) {
                progressMonitor.setNote(chunk[0]);
                progressMonitor.setProgress(Integer.valueOf(chunk[1]));
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void succeeded(List<PatientModel> result) {
            if (result != null) {
                tableModel.setDataProvider(result);
            } else {
                tableModel.clear();
            }
            updateStatusLabel();
            progressMonitor.close();
            doStopProgress();
        }

        @Override
        protected void failed(Throwable cause) {
            cause.printStackTrace(System.err);
            doStopProgress();
        }
    }
    
    private void makeInitialIndex() {

        IndexTaskWorker worker = new IndexTaskWorker();
        getContext().getGlassPane().setText("インデックス作成は時間がかかります。");
        worker.execute();
    }
    
    private class IndexTaskWorker extends SimpleWorker<Void, String[]> {

        private ProgressMonitor progressMonitor;
        private final String message = "インデックス作成";
        private final String progressNote = "<html>索引を作成中<br>%d件中、%d％完了";
        private final String startingNote = "処理を開始します。";
        private final String initialNote = "<html><br>";

        @Override
        protected Void doInBackground() {

            doStartProgress();
            // progress bar 設定
            progressMonitor = new ProgressMonitor(view, message, initialNote, 0, 100);
            progressMonitor.setMillisToDecideToPopup(0); // この処理は絶対時間がかかるので，すぐ出す
            progressMonitor.setMillisToPopup(0);
            progressMonitor.setProgress(0);

            // 索引作成開始
            MasudaDelegater dl = MasudaDelegater.getInstance();

            // maxResult毎にインデックス作成する
            final int maxResults = 200;
            long fromDocPk = 0;
            int page = 0;
            String ret = null;
            // progress bar 表示
            publish(new String[]{startingNote, "0"});
            
            while (!FINISHED.equals(ret)) {
                // キャンセルされた場合
                if (progressMonitor.isCanceled()) {
                    break;
                }
                ret = dl.makeDocumentModelIndex(fromDocPk, maxResults);
                if (!FINISHED.equals(ret)) {
                    String[] str = ret.split(",");
                    long totalModelCount = Long.valueOf(str[1]);
                    fromDocPk = Long.valueOf(str[0]);
                    page++;
                    // progress bar 表示
                    int ratio = (totalModelCount == 0)
                        ? 0 : (int) (100 * page * maxResults / totalModelCount);
                    String msg = String.format(progressNote, totalModelCount, ratio);
                    publish(new String[]{msg, String.valueOf(ratio)});
                }
            }
            return null;
        }

        @Override
        protected void process(List<String[]> chunks) {
            for (String[] chunk : chunks) {
                progressMonitor.setNote(chunk[0]);
                progressMonitor.setProgress(Integer.valueOf(chunk[1]));
            }
        }

        @Override
        protected void done() {
            getContext().getGlassPane().setText("");
            progressMonitor.close();
            doStopProgress();
        }

        @Override
        protected void failed(Throwable cause) {
            cause.printStackTrace(System.err);
            doStopProgress();
        }
    }
    
    private boolean useHibernateSearch() {
        boolean b = Project.getBoolean(MiscSettingPanel.USE_HIBERNATE_SEARCH, MiscSettingPanel.DEFAULT_HIBERNATE_SEARCH);
        return b;
    }
//masuda$

    // ChartEventListener
    @Override
    public void onEvent(ChartEventModel evt) {

        int sRow = -1;
        long ptPk = evt.getPtPk();
        List<PatientModel> list = tableModel.getDataProvider();
        ChartEventModel.EVENT eventType = evt.getEventType();
        
        switch (eventType) {
            case PVT_STATE:
                for (int row = 0; row < list.size(); ++row) {
                    PatientModel pm = list.get(row);
                    if (ptPk == pm.getId()) {
                        sRow = row;
                        pm.setOwnerUUID(evt.getOwnerUUID());
                        break;
                    }
                }
                break;
            case PM_MERGE:
                for (int row = 0; row < list.size(); ++row) {
                    PatientModel pm = list.get(row);
                    if (ptPk == pm.getId()) {
                        sRow = row;
                        //pm = msg.getPatientModel();
                        list.set(row, evt.getPatientModel());
                        break;
                    }
                }
                break;            
            case PVT_MERGE:
                for (int row = 0; row < list.size(); ++row) {
                    PatientModel pm = list.get(row);
                    if (ptPk == pm.getId()) {
                        sRow = row;
                        //pm = msg.getPatientVisitModel().getPatientModel();
                        list.set(row, evt.getPatientVisitModel().getPatientModel());
                        break;
                    }
                }
                break;
            default:
                break;
        }
        
        if (sRow != -1) {
            tableModel.fireTableRowsUpdated(sRow, sRow);
        }
    }
    
    private class PatientListTableRenderer extends StripeTableCellRenderer {

        public PatientListTableRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {

            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
            
            PatientModel pm = (PatientModel) sorter.getObject(row);
            
            if (pm != null && col == stateColumn) {
                setHorizontalAlignment(JLabel.CENTER);
                if (pm.isOpened()) {
                    if (clientUUID.equals(pm.getOwnerUUID())) {
                        setIcon(OPEN_ICON);
                    } else {
                        setIcon(NETWORK_ICON);
                    }
                } else {
                    setIcon(null);
                }
                setText("");
            } else {
                setHorizontalAlignment(JLabel.LEFT);
                setIcon(null);
                setText(value == null ? "" : value.toString());
            }

            return this;
        }
    }
}