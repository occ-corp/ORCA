package open.dolphin.client;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.dao.SqlOrcaView;
import open.dolphin.delegater.DocumentDelegater;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.*;
import open.dolphin.order.StampEditor;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.stampbox.StampBoxPlugin;
import open.dolphin.stampbox.StampTree;
import open.dolphin.table.ColumnSpecHelper;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;
import open.dolphin.tr.DiagnosisTransferHandler;
import open.dolphin.util.AgeCalculator;
import open.dolphin.util.BeanUtils;
import open.dolphin.util.MMLDate;
import open.dolphin.util.NonHidePopupMenu;
import org.apache.log4j.Logger;

/**
 * DiagnosisDocument
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika 手抜きでほぼ1.4mのままwww
 */
public final class DiagnosisDocument extends AbstractChartDocument implements PropertyChangeListener {

    private static final String TITLE = "傷病名";

    // 傷病名テーブルのカラム番号定義
    private static final int DIAGNOSIS_COL  = 0;
    private static final int CATEGORY_COL   = 1;
    private static final int OUTCOME_COL    = 2;
    private static final int START_DATE_COL = 3;
    private static final int END_DATE_COL   = 4;

    // GUI コンポーネント定義
    //private static final String RESOURCE_BASE = "/open/dolphin/resources/images/";
    private static final String DELETE_BUTTON_IMAGE     = "icon_delete_small";
    private static final String ADD_BUTTON_IMAGE        = "icon_add_small";
    private static final String UPDATE_BUTTON_IMAGE     = "icon_save_small";
    private static final String ORCA_VIEW_IMAGE         = "icon_import_orca_diagnosis";
    private static final String ORCA_IMPORT_IMAGE       = "icon_import_orca_star";

    /** JTableレンダラ用のカラー */
    private static final Color ORCA_BACK = new Color(227, 250, 207); //ClientContext.getColor("color.CALENDAR_BACK");
    private static final Color DELETED_COLOR = new Color(128, 128, 128); //ClientContext.getColor("watingList.color.pvtCancel");

    // status flag関連
    private static final String DIAGNOSIS_EDITED    = "edited";
    private static final String DIAGNOSIS_DELETED   = "deleted";
    private static final String DIAGNOSIS_FINAL     = "F";
    private static final String ORCA_RECORD         = "ORCA";

    // diagTable
    private static final int clickCountToStartEdit = 2;
    private static final int startNumRows = 1;
    // カラム仕様ヘルパー
    private static final String COLUMN_SPEC_NAME = "diagTable.column.spec";
    private static final String[] COLUMN_NAMES 
            = {"疾患名/修飾語", "分 類", "転 帰", "疾患開始日", "疾患終了日", "特定疾患"};
    private static final String[] PROPERTY_NAMES 
            = {"getAliasOrName", "getCategoryDesc", "getOutcomeDesc", "getStartDate", "getEndDate", "getByoKanrenKbnStr"};
    private static final Class[] COLUMN_CLASSES 
            = {String.class, String.class, String.class, String.class, String.class, String.class};
    private static final int[] COLUMN_WIDTH = {180, 80, 80, 80, 80, 40};
    private ColumnSpecHelper columnHelper;

    private static final String[] COLUMN_TOOLTIPS = new String[]{null,
        "クリックするとコンボボックスが立ち上がります。", "クリックするとコンボボックスが立ち上がります。",
        "右クリックでカレンダがポップアップします。", "右クリックでカレンダがポップアップします。", null};
    
    //private static final SimpleDateFormat yyyyMMddFrmt = new SimpleDateFormat("yyyyMMdd");

    private ListTableModel<RegisteredDiagnosisModel> tableModel; // TableModel
    private ListTableSorter sorter;

    private JCheckBox cb_wareki;                // 和暦表示チェックボックス
    private boolean importBtnEnabled = false;
    private JTable diagTable;                   // 病歴テーブル
    private JComboBox extractionCombo;          // 抽出期間コンボ
    private JLabel countLbl;                    // 件数ラベル
    
    private AbstractAction addAction;           // 新規病名エディタ
    private AbstractAction deleteAction;        // 既存傷病名の削除
    private AbstractAction updateAction;        // 既存傷病名の転帰等の更新
    private AbstractAction orcaAction;          // ORCA action
    private JButton orcaButton;                 // ORCA View ボタン
    private AbstractAction activeAction;        // active病名のみ表示
    private JCheckBox activeBox;                // active病名チェックボックス
    
    private Logger logger = ClientContext.getBootLogger();

    // 昇順降順フラグ
    private boolean ascend;

    // 病名修飾語リスト 8000台は後ろにつく修飾語
    private final static LinkedHashMap<String, String> diagnosisModifiers;
    static {
        diagnosisModifiers = new LinkedHashMap<String, String>();
        diagnosisModifiers.put("右", "2056");
        diagnosisModifiers.put("左", "2049");
        diagnosisModifiers.put("両", "2057");
        diagnosisModifiers.put("の急性増悪", "8061");
        diagnosisModifiers.put("の二次感染", "8069");
        diagnosisModifiers.put("の再発", "8065");
        diagnosisModifiers.put("の術後", "8048");
        diagnosisModifiers.put("の治療後", "8075");

    }
    // undo関連
    private Deque undoQue;      // undo用のdeque
    private Deque redoQue;      // redo用のdeque
    private long rdId = 0;      // 新規登録のRegisteredDiagnosisModelには負のIDを振っていく
    private Action undoAction;  // undoAction
    private Action redoAction;  // redoAction

    private int selectedRow;
    
    private List<RegisteredDiagnosisModel> allDiagnosis;

//masuda    最終受診日＝今日受診している場合は今日，していないばあいは最後の受診日
    private String lastVisit;
    private DiagnosisTransferHandler tr;

    /**
     *  Creates new DiagnosisDocument
     */
    public DiagnosisDocument() {
        setTitle(TITLE);
    }

    /**
     * GUI コンポーネントを生成初期化する。
     */
    private void initialize() {

        // コマンドボタンパネルを生成する
        JPanel cmdPanel = createButtonPanel2();

        // Dolphin 傷病歴パネルを生成する
        JPanel dolphinPanel = createDignosisPanel();

        // 抽出期間パネルを生成する
        JPanel filterPanel = createFilterPanel();

        JPanel content = new JPanel(new BorderLayout(0, 7));
        content.add(cmdPanel, BorderLayout.NORTH);
        content.add(dolphinPanel, BorderLayout.CENTER);
        content.add(filterPanel, BorderLayout.SOUTH);

        // 全体をレイアウトする
        JPanel myPanel = getUI();
        myPanel.setLayout(new BorderLayout(0, 7));
        myPanel.add(content);
        myPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));

        // Preference から昇順降順を設定する
        ascend = Project.getBoolean(Project.DIAGNOSIS_ASCENDING, false);

//masuda     最終受診日を調べる。今日受診していたらpvtDate
        ChartImpl chart = (ChartImpl) getContext();
        PatientVisitModel pvt = chart.getPatientVisit();
        String visit = pvt.getPvtDateTrimTime();
        // fakePvtの場合はpvtId = 0
        if (visit != null && pvt.getId() != 0) {
            lastVisit = visit;
        } else {
            final SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
            Date lastDocDate = getContext().getKarte().getLastDocDate();
            lastVisit = (lastDocDate == null) ? MMLDate.getDate() : frmt.format(lastDocDate);
        }
//masuda    undo & redoアクションの設定
        undoAction = getContext().getChartMediator().getAction(GUIConst.ACTION_UNDO);
        undoAction.setEnabled(false);
        redoAction = getContext().getChartMediator().getAction(GUIConst.ACTION_REDO);
        redoAction.setEnabled(false);
        undoQue = new LinkedList<RegisteredDiagnosisDequeModel>();
        redoQue = new LinkedList<RegisteredDiagnosisDequeModel>();
    }

//masuda^   ChartMediatorから引っ越し
    private  boolean hasTree(String entity) {
        StampBoxPlugin stBox = Dolphin.getInstance().getStampBox();
        StampTree tree = stBox.getStampTree(entity);
        return tree != null;
    }
//masuda$

    /**
     * コマンドボタンパネルをする。
     */
    private JPanel createButtonPanel2() {

        // 更新ボタン
        updateAction = new AbstractAction("保存", createImageIcon(UPDATE_BUTTON_IMAGE)) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                save();
            }
        };
        updateAction.setEnabled(false);
        JButton updateButton = new JButton(updateAction);
        updateButton.setToolTipText("追加変更した傷病名をデータベースに反映します。");

        // 削除ボタン
        deleteAction = new AbstractAction("削除", createImageIcon(DELETE_BUTTON_IMAGE)) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                delete();
            }
        };
        deleteAction.setEnabled(false);
        JButton deleteButton = new JButton(deleteAction);
        deleteButton.setToolTipText("選択した傷病名を削除します。");

        // 新規登録ボタン
        addAction = new AbstractAction("追加", createImageIcon(ADD_BUTTON_IMAGE)) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                
            }
        };
        JButton addButton = new JButton(addAction);
        addButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                
                if (!e.isPopupTrigger()) {
                    // ASP StampBox が選択されていて傷病名Treeがない場合がある
                    if (hasTree(IInfoModel.ENTITY_DIAGNOSIS)) {
                        //JPopupMenu popup = new JPopupMenu();
                        JPopupMenu popup = new NonHidePopupMenu();
                        getContext().getChartMediator().addDiseaseMenu(popup);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    } else {
                        Toolkit.getDefaultToolkit().beep();
                        String msg1 = "現在使用中のスタンプボックスには傷病名がありません。";
                        String msg2 = "個人用のスタンプボックス等に切り替えてください。";
                        Object obj = new String[]{msg1, msg2};
                        String title = ClientContext.getFrameTitle("傷病名追加");
                        Component comp = getUI();
                        JOptionPane.showMessageDialog(comp, obj, title, JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        // Depends on readOnly prop
        addAction.setEnabled(!isReadOnly());
        addButton.setToolTipText("傷病名を追加します。");

        // ORCA View
        orcaAction = new AbstractAction("ORCA", createImageIcon(ORCA_VIEW_IMAGE)) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                viewOrca();
            }
        };
        orcaButton = new JButton(orcaAction);
        orcaButton.setToolTipText("ORCAに登録してある病名を参照または取り込みます。");
        
//masuda    和暦選択チェックボックスを追加
        cb_wareki = new JCheckBox("和暦表示");
        boolean b = Project.getBoolean(MiscSettingPanel.PREFER_WAREKI, MiscSettingPanel.DEFAULT_PREFER_WAREKI);
        cb_wareki.setSelected(b);
        cb_wareki.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean b = cb_wareki.isSelected();
                Project.setBoolean(MiscSettingPanel.PREFER_WAREKI, b);
                diagTable.repaint();
            }
        });
        
        // ボタンパネル
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalStrut(5));
        p.add(orcaButton);
        p.add(Box.createHorizontalGlue());
        p.add(cb_wareki);
        p.add(deleteButton);
        p.add(Box.createHorizontalStrut(5));
        p.add(addButton);
        p.add(Box.createHorizontalStrut(5));
        p.add(updateButton);
        
        return p;
    }

    /**
     * 既傷病歴テーブルを生成する。
     */
    private JPanel createDignosisPanel() {

        // 傷病歴テーブルを生成する
        diagTable = new JTable() {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    @Override
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return COLUMN_TOOLTIPS[realIndex];
                    }
                };
            }
        };
        //列の入れ替えを禁止
        diagTable.getTableHeader().setReorderingAllowed(false);
        
        // Diagnosis テーブルモデルを生成する
        setupTableModel();

//pns^
        diagTable.setShowGrid(false);
        diagTable.putClientProperty("JTable.autoStartsEdit", false); //キー入力によるセル編集開始を禁止
//pns$
//masuda    ストライプテーブル
        DolphinOrcaRenderer renderer = new DolphinOrcaRenderer();
        renderer.setTable(diagTable);
        renderer.setDefaultRenderer();
        // ??
        diagTable.setSurrendersFocusOnKeystroke(true);
        // 行選択が起った時のリスナを設定する
        //diagTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        diagTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        diagTable.setRowSelectionAllowed(true);
        ListSelectionModel m = diagTable.getSelectionModel();
        m.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                rowSelectionChanged(e);
            }
        });

//masuda    ソーター組み込み
        sorter = new ListTableSorter(tableModel);
        diagTable.setModel(sorter);
        sorter.setTableHeader(diagTable.getTableHeader());
        
        // カラム幅更新
        columnHelper.updateColumnWidth();
        // ColumnHelperでカラム変更関連イベントを設定する
        columnHelper.connect();
        
        // Category comboBox 入力を設定する
        String[] values = ClientContext.getStringArray("diagnosis.category");
        String[] descs = ClientContext.getStringArray("diagnosis.categoryDesc");
        String[] codeSys = ClientContext.getStringArray("diagnosis.categoryCodeSys");
        DiagnosisCategoryModel[] categoryList = new DiagnosisCategoryModel[values.length + 1];
        DiagnosisCategoryModel dcm = new DiagnosisCategoryModel();
        dcm.setDiagnosisCategory("");
        dcm.setDiagnosisCategoryDesc("");
        dcm.setDiagnosisCategoryCodeSys("");
        categoryList[0] = dcm;
        for (int i = 0; i < values.length; i++) {
            dcm = new DiagnosisCategoryModel();
            dcm.setDiagnosisCategory(values[i]);
            dcm.setDiagnosisCategoryDesc(descs[i]);
            dcm.setDiagnosisCategoryCodeSys(codeSys[i]);
            categoryList[i + 1] = dcm;
        }
        final JComboBox categoryCombo = new JComboBox(categoryList);
        TableColumn column = diagTable.getColumnModel().getColumn(CATEGORY_COL);
//pns　 JComboBox を細かくコントロ－ルするために MyCellEditor を作った（一番最後参照）
        column.setCellEditor(new MyCellEditor(categoryCombo));

        // Outcome comboBox 入力を設定する
        String[] ovalues = ClientContext.getStringArray("diagnosis.outcome");
        String[] odescs = ClientContext.getStringArray("diagnosis.outcomeDesc");
        String ocodeSys = ClientContext.getString("diagnosis.outcomeCodeSys");
        DiagnosisOutcomeModel[] outcomeList = new DiagnosisOutcomeModel[ovalues.length + 1];
        DiagnosisOutcomeModel dom = new DiagnosisOutcomeModel();
        dom.setOutcome("");
        dom.setOutcomeDesc("");
        dom.setOutcomeCodeSys("");

//pns   転帰を消せるようにする
        outcomeList[0] = dom;

        for (int i = 0; i < ovalues.length; i++) {
            dom = new DiagnosisOutcomeModel();
            dom.setOutcome(ovalues[i]);
            dom.setOutcomeDesc(odescs[i]);
            dom.setOutcomeCodeSys(ocodeSys);
            outcomeList[i + 1] = dom;
        }
        JComboBox outcomeCombo = new JComboBox(outcomeList);
        column = diagTable.getColumnModel().getColumn(OUTCOME_COL);
//pns   JComboBox の細かいコントロールのため MyCellEditor を使う
        column.setCellEditor(new MyCellEditor(outcomeCombo));

//pns^  diagnosis でポップアップが効くようにする
        column = diagTable.getColumnModel().getColumn(DIAGNOSIS_COL);
        JTextField t = new JTextField();
        t.setEditable(false);
        final DefaultCellEditor diagCellEditor = new DiagnosisCellEditor(t);
        diagCellEditor.setClickCountToStart(clickCountToStartEdit);
        column.setCellEditor(diagCellEditor);
//pns$

        // Start Date && EndDate Col にポップアップカレンダーを設定する
//masuda    和暦で入力できるようにMTSHも入力可能とする
        String datePattern = "[0-9\\-/\\.mMtTsShH]*";
        column = diagTable.getColumnModel().getColumn(START_DATE_COL);
        JTextField tf = new JTextField();
        // IME を OFF にする
        tf.addFocusListener(AutoRomanListener.getInstance());
        final int[] range = {-12, 2};
        
        tf.setDocument(new RegexConstrainedDocument(datePattern));
        DefaultCellEditor de1 = new DefaultCellEditor2(tf);
        column.setCellEditor(de1);
        de1.setClickCountToStart(clickCountToStartEdit);
        PopupCalendarListener pcl1 = new PopupCalendarListener(tf, range);
        pcl1.setCellEditor(de1);
        
        column = diagTable.getColumnModel().getColumn(END_DATE_COL);
        tf = new JTextField();
        // IME を OFF にする
        tf.addFocusListener(AutoRomanListener.getInstance());
        tf.setDocument(new RegexConstrainedDocument(datePattern));
        DefaultCellEditor de2 = new DefaultCellEditor2(tf);
        column.setCellEditor(de2);
        de2.setClickCountToStart(clickCountToStartEdit);
        PopupCalendarListener pcl2 = new PopupCalendarListener(tf, range);
        pcl2.setCellEditor(de2);

        // TransferHandler を設定する
        tr = new DiagnosisTransferHandler(this);
        diagTable.setTransferHandler(tr);
        diagTable.setDragEnabled(true);

        // Layout
        JScrollPane scroller = new JScrollPane(diagTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel p = new JPanel(new BorderLayout());
        p.add(scroller, BorderLayout.CENTER);

        return p;
    }

    /**
     * tableModelを設定する。
     */
    private void setupTableModel() {
        
        // ColumnSpecHelperを準備する
        columnHelper = new ColumnSpecHelper(COLUMN_SPEC_NAME,
                COLUMN_NAMES, PROPERTY_NAMES, COLUMN_CLASSES, COLUMN_WIDTH);
        columnHelper.loadProperty();
        
        // ColumnSpecHelperにテーブルを設定する
        columnHelper.setTable(diagTable);

        //------------------------------------------
        // View のテーブルモデルを置き換える
        //------------------------------------------
        String[] columnNames = columnHelper.getTableModelColumnNames();
        String[] methods = columnHelper.getTableModelColumnMethods();
        Class[] cls = columnHelper.getTableModelColumnClasses();
        
        tableModel = new ListTableModel<RegisteredDiagnosisModel>(columnNames, startNumRows, methods, cls) {

            // Diagnosisは編集不可
            @Override
            public boolean isCellEditable(int row, int col) {

                // licenseCodeで制御
                if (isReadOnly()) {
                    return false;
                }
                // 病名レコードが存在しない場合は false
                RegisteredDiagnosisModel entry = getObject(row);
                if (entry == null) {
                    return false;
                }
                // ORCA に登録されている病名の場合
                if (entry.getStatus() != null && entry.getStatus().equals(ORCA_RECORD)) {
                    return false;
                }
                switch (col) {
                    case DIAGNOSIS_COL:
                    case CATEGORY_COL:
                    case OUTCOME_COL:
                    case START_DATE_COL:
                    case END_DATE_COL:
                        return true;
                    default:
                        return false;
                }
            }

            // オブジェクトの値を設定する
            @Override
            public void setValueAt(Object value, int row, int col) {

//masuda    選択中の複数行を一括変更するようにした
                if (value == null) {
                    return;
                }

                switch (col) {
                    case DIAGNOSIS_COL:
                        break;

                    case CATEGORY_COL:
                        // JComboBox から選択
                        DiagnosisCategoryModel categoryModel = (DiagnosisCategoryModel) value;

                        int[] selectedRows = diagTable.getSelectedRows();
                        String newCategory = categoryModel.getDiagnosisCategory();
                        newCategory = ( newCategory != null && (!newCategory.equals("")) )? newCategory : null;

                        // 選択されている行すべてに変更を行う
                        for (int tableRow : selectedRows) {
                            RegisteredDiagnosisModel oldRd = (RegisteredDiagnosisModel) sorter.getObject(tableRow);
                            // 複製を作成
                            RegisteredDiagnosisModel newRd = duplicateRd(oldRd);
                            String oldCategory = oldRd.getCategory();
                            if (newCategory == null) {
                                if (oldCategory != null) {
                                    newRd.setDiagnosisCategoryModel(null);
                                    newRd.setStatus(DIAGNOSIS_EDITED);
                                    offerUndoQue(oldRd, newRd);
                                } else {
                                    // newCategory == null && oldCategory == nullなら変更なしなので何もしない
                                }
                            } else if (!newCategory.equals(oldCategory)) {
                                // newCategoryとoldCategoryが違う場合
                                newRd.setCategory(categoryModel.getDiagnosisCategory());
                                newRd.setCategoryDesc(categoryModel.getDiagnosisCategoryDesc());
                                newRd.setCategoryCodeSys(categoryModel.getDiagnosisCategoryCodeSys());
                                newRd.setStatus(DIAGNOSIS_EDITED);
                                offerUndoQue(oldRd, newRd);
                            }
                        }
                        break;

                    case OUTCOME_COL:
                        // JComboBox から選択
                        DiagnosisOutcomeModel outcomeModel = (DiagnosisOutcomeModel) value;
                        String newOutcome = outcomeModel.getOutcome();
                        newOutcome = ( newOutcome != null && (!newOutcome.equals("")) )? newOutcome : null;
                        selectedRows = diagTable.getSelectedRows();
                        for (int tableRow : selectedRows) {
                            RegisteredDiagnosisModel oldRd = (RegisteredDiagnosisModel) sorter.getObject(tableRow);
                            RegisteredDiagnosisModel newRd = duplicateRd(oldRd);
                            String oldOutcome = oldRd.getOutcome();

                            if (newOutcome == null) {
                                if (oldOutcome != null) {
                                    newRd.setDiagnosisOutcomeModel(null);
                                    //pns 転帰が消去されたら，疾患終了日を消すと同時に，開始日を今日にする
                                    newRd.setEndDate(null);
                                    newRd.setStartDate(lastVisit);
                                    newRd.setStatus(DIAGNOSIS_EDITED);
                                    offerUndoQue(oldRd, newRd);
                                } else {
                                    // newOutcome == null && oldOutcome == nullなら変更なしなので何もしない
                                }
                            } else if (!newOutcome.equals(oldOutcome)) {
                                // newOutcomeとoldOutcomeが違う場合
                                newRd.setOutcome(outcomeModel.getOutcome());
                                newRd.setOutcomeDesc(outcomeModel.getOutcomeDesc());
                                newRd.setOutcomeCodeSys(outcomeModel.getOutcomeCodeSys());
                                // 疾患終了日を入れる
                                if (Project.getBoolean("autoOutcomeInput", false)) {
                                    String val = newRd.getEndDate();
                                    if (val == null || val.equals("")) {
                                        // masuda 転帰日の自動入力を月末にする
                                        GregorianCalendar gc = new GregorianCalendar();
                                        int year = gc.get(GregorianCalendar.YEAR);
                                        int month = gc.get(GregorianCalendar.MONTH);
                                        int day = gc.getActualMaximum(GregorianCalendar.DATE);
                                        gc.set(year, month, day, 0, 0, 0);
                                        String today = MMLDate.getDate(gc);
                                        newRd.setEndDate(today);
                                    }
                                }
                                newRd.setStatus(DIAGNOSIS_EDITED);
                                offerUndoQue(oldRd, newRd);
                            }
                        }
                        break;

                    case START_DATE_COL:
                    case END_DATE_COL:
                        String newDate = (String) value;
                        newDate = convertToSeireki(newDate);  // 西暦に整形する
                        if (newDate == null) {
                            diagTable.getCellEditor(row, col).cancelCellEditing();
                        }
                        selectedRows = diagTable.getSelectedRows();
                        for (int tableRow : selectedRows) {
                            RegisteredDiagnosisModel oldRd = (RegisteredDiagnosisModel) sorter.getObject(tableRow);
                            RegisteredDiagnosisModel newRd = duplicateRd(oldRd);
                            if (col == START_DATE_COL) {
                                newRd.setStartDate(newDate);
                            } else {
                                newRd.setEndDate(newDate);
                            }
                            newRd.setStatus(DIAGNOSIS_EDITED);
                            offerUndoQue(oldRd, newRd);
                        }
                        break;
                }
            }
        };
    }

    /**
     * 抽出期間パネルを生成する。
     */
    private JPanel createFilterPanel() {

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalStrut(7));

        // 抽出期間コンボボックス
        p.add(new JLabel("抽出期間(過去)"));
        p.add(Box.createHorizontalStrut(5));
        NameValuePair[] extractionObjects = ClientContext.getNameValuePair("diagnosis.combo.period");
        extractionCombo = new JComboBox(extractionObjects);
        int currentDiagnosisPeriod = Project.getInt(Project.DIAGNOSIS_PERIOD, 0);
        int selectIndex = NameValuePair.getIndex(String.valueOf(currentDiagnosisPeriod), extractionObjects);
        extractionCombo.setSelectedIndex(selectIndex);
        extractionCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                extPeriodChanged(e);
            }
        });

        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        comboPanel.add(extractionCombo);
        
        // Active 病名のみ表示
        activeAction = new AbstractAction("アクティブ病名のみ") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                filterDiagnosis();
            }
        };

        activeBox = new JCheckBox();
        // Projectからアクティブ病名のみを表示するかどうか設定する
        boolean activeOnly = Project.getBoolean("diagnosis.activeOnly");
        activeBox.setSelected(activeOnly);
        activeBox.setAction(activeAction);
        comboPanel.add(Box.createHorizontalStrut(5));
        comboPanel.add(activeBox);

        p.add(comboPanel);
        p.add(Box.createHorizontalGlue());

        // 件数ラベル
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        countLbl = new JLabel();
        countPanel.add(countLbl);

        p.add(countPanel);
        p.add(Box.createHorizontalStrut(7));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));

        return p;
    }

    /**
     * 行選択が起った時のボタン制御を行う。
     */
    private void rowSelectionChanged(ListSelectionEvent e) {

        if (e.getValueIsAdjusting() == false) {
            // 削除ボタンをコントロールする
            if (isReadOnly()) {
                return;
            }
            boolean flag = true;
            // 選択された行のオブジェクトを得る
            int[] selectedRows = diagTable.getSelectedRows();
            for (int row : selectedRows) {
                if (row == -1) {
                    continue;
                }
                RegisteredDiagnosisModel rd = (RegisteredDiagnosisModel) sorter.getObject(row);
                // ヌルの場合
                if (rd == null) {
                    flag = false;
                    break;
                }
                String status = rd.getStatus();
                if (status == null ||
                        DIAGNOSIS_FINAL.equals(status) ||
                        DIAGNOSIS_EDITED.equals(status)) {
                    continue;
                } else {
                    flag = false;
                    break;
                }
            }
            deleteAction.setEnabled(flag);
        }
    }

    /**
     * 抽出期間を変更した場合に再検索を行う。
     * ORCA 病名ボタンが disable であれば検索後に enable にする。
     */
    private void extPeriodChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            getDiagnosisHistory();
        }
    }

    public JTable getDiagnosisTable() {
        return diagTable;
    }

    @Override
    public void start() {
        initialize();
        getDiagnosisHistory();
        enter();
    }

    @Override
    public void stop() {
        if (tableModel != null) {
            tableModel.clear();
        }
        // ColumnSpecsを保存する
        if (columnHelper != null) {
            columnHelper.saveProperty();
        }
    }

    @Override
    public void enter() {
        super.enter();
//masuda    タブが選択されたらcontrollButtonする。
        controlButton();
        getContext().enabledAction(GUIConst.ACTION_SEND_CLAIM, true);
    }
    /**
     * 傷病名エディタからデータを受け取りテーブルへ追加する。
     */
    @Override
    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent e) {

        RegisteredDiagnosisModel[] newRds = (RegisteredDiagnosisModel[]) e.getNewValue();
        if (newRds == null || newRds.length == 0) {
            return;
        }
        RegisteredDiagnosisModel newRd = newRds[0];   // 最初のものだけ

        RegisteredDiagnosisModel[] oldRds = (RegisteredDiagnosisModel[]) e.getOldValue();
        RegisteredDiagnosisModel oldRd =
                (oldRds == null || oldRds.length == 0) ? null : oldRds[0];

        if (oldRd != null) {
            // 既存病名の編集の場合、IdはoldRdのものを引き継ぐ
            newRd.setId(oldRd.getId());
            newRd.setStatus(DIAGNOSIS_EDITED);
            offerUndoQue(oldRd, newRd);
        } else {
            // 同じ病名がないか調べる
            if (disallowSameDiagnosis(newRd)){
                return;
            }
            // 新規病名はLastVisitを疾患開始日として設定する pns
            newRd.setStartDate(lastVisit);
            newRd.setStatus(DIAGNOSIS_EDITED);
            newRd.setId(--rdId);
            offerUndoQue(null, newRd);
            //checkIkouByomei(newRd);     // 移行病名チェック pns
        }

        // 病名編集後にstopCellEditorする
        if (diagTable.getCellEditor() != null) {
            diagTable.getCellEditor().stopCellEditing();
            int rowCount = diagTable.getRowCount();
            if (selectedRow >= 0 && selectedRow < rowCount) {
                diagTable.setRowSelectionInterval(selectedRow, selectedRow);
            }
        }
    }

//masuda^    同じ病名がないか調べる
    private boolean disallowSameDiagnosis(RegisteredDiagnosisModel newRd){

        String diagCd = getRealDiagnosisCode(newRd);
        for (RegisteredDiagnosisModel rd : allDiagnosis) {
            String testCd = getRealDiagnosisCode(rd);
            if (diagCd != null && diagCd.equals(testCd) 
                    && newRd.getId() != rd.getId()) {   // 編集もとは除く
                int index = tableModel.getDataProvider().indexOf(rd);
                // activeのみへの対応
                String msgNonActive = "（非アクティブ）";
                if (index != -1) {
                    int sIndex = sorter.modelIndex(index);
                    diagTable.setRowSelectionInterval(sIndex, sIndex);
                    msgNonActive = "";
                }
                String[] options = {"取消", "無視"};
                String msg = newRd.getDiagnosisName() + "は重複しています。" + msgNonActive;
                int val = JOptionPane.showOptionDialog(getContext().getFrame(), msg, "傷病名エディタ",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                if (val != 1) {
                    return true;   // 取り消し
                }
            }
        }
        return false;
    }

    // 修飾語を除去
    private String getRealDiagnosisCode(RegisteredDiagnosisModel model) {

        // 病名コードを切り出し（接頭語，接尾語は捨てる）
        String diagCd = model.getDiagnosisCode();
        String[] code = diagCd.split("\\.");
        for (String str : code) {
            if (str.length() == 7) {     // 病名コードの桁数は７
                return str;
            }
        }
        return diagCd;
    }
//masuda$

    /**
     * 傷病名スタンプを取得する worker を起動する。
     */
    public void importStampList(final List<ModuleInfoBean> stampList, final int insertRow) {

//pns^  4 個以上一気にドロップされたら警告を出す
        if (stampList.size() >= 4) {
            int ans = JOptionPane.showConfirmDialog(getContext().getFrame(),
                    stampList.size() + "個のスタンプが同時にドロップされましたが続けますか", "スタンプ挿入確認",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) {
                return;
            }
        }
//pns$
        final StampDelegater sdl = StampDelegater.getInstance();

        DBTask task = new DBTask<List<StampModel>, Void>(getContext()) {

            @Override
            protected List<StampModel> doInBackground() throws Exception {
                List<StampModel> result = sdl.getStamps(stampList);
                return result;
            }

            @Override
            protected void succeeded(List<StampModel> list) {
                logger.debug("importStampList succeeded");
                if (list != null) {
                    for (int i = list.size() - 1; i > -1; i--) {
                        insertStamp(list.get(i), insertRow);
                    }
                }
//pns           病名を drop した場合，ここに入ってくる
                controlButton();
            }
        };

        task.execute();
    }

    /**
     * 傷病名スタンプをデータベースから取得しテーブルへ挿入する。
     * Worker Thread で実行される。
     * @param stampInfo
     */
    private void insertStamp(StampModel sm, int row) {

        // TableSorterを導入していることもあり挿入場所の指定は無視ｗ
        if (sm != null) {
            RegisteredDiagnosisModel module = (RegisteredDiagnosisModel) BeanUtils.xmlDecode(sm.getStampBytes());
//masuda^   同じ病名のチェック
            if (disallowSameDiagnosis(module)){
                return;
            }
//masuda$
//pns^
            // 疾患開始日を lastVisit に設定
            module.setStartDate(lastVisit);

            // diagnosis に「疑い」が入っていたら，疑いにセットする
            String diag = module.getDiagnosis();
            if (diag.endsWith("の疑い")) {
                String diagCode = module.getDiagnosisCode();
                module.setDiagnosis(diag.replace("の疑い", ""));
                module.setDiagnosisCode(diagCode.replace(".8002", ""));
                module.setCategory("suspectedDiagnosis");
                module.setCategoryDesc("疑い病名");
                module.setCategoryCodeSys("MML0015");
            }
            // CTRLキー(Windows)が押されていたら，疑いにセットする
            int action = tr.getTransferAction();
            if (action == java.awt.dnd.DnDConstants.ACTION_COPY) {
                module.setCategory("suspectedDiagnosis");
                module.setCategoryDesc("疑い病名");
                module.setCategoryCodeSys("MML0015");
            }

            // 移行病名チェック
            checkIkouByomei(module);
//pns$
            int cnt = tableModel.getObjectCount();
            module.setStatus(DIAGNOSIS_EDITED);
            module.setId(--rdId);
            offerUndoQue(null, module);

            // 挿入したdiagnoisisのrowを選択する
            for (int i = 0; i < cnt; ++i){
                if (rdId == tableModel.getObject(i).getId()){
                    diagTable.getSelectionModel().setSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    /**
     * 選択された行のデータを削除する。
     */
    public void delete() {

        // 選択された行のオブジェクトを取得する
        int[] selectedRows = diagTable.getSelectedRows();
        for (int row : selectedRows) {
            RegisteredDiagnosisModel oldRd = (RegisteredDiagnosisModel) sorter.getObject(row);
            if (oldRd == null) {
                continue;
            }
            // status を削除に変更する
            RegisteredDiagnosisModel newRd = duplicateRd(oldRd);
            newRd.setStatus(DIAGNOSIS_DELETED);
            offerUndoQue(oldRd, newRd);
        }

        controlButton();
    }

    /**
     * 新規及び変更された傷病名を保存する。
     */
    @Override
    public void save() {
        // データベースにない追加の項目
        List<RegisteredDiagnosisModel> addedDiagnosis = new ArrayList<RegisteredDiagnosisModel>();
        // データベースにある変更した項目
        List<RegisteredDiagnosisModel> updatedDiagnosis = new ArrayList<RegisteredDiagnosisModel>();
        // データベースから削除する項目
        List<RegisteredDiagnosisModel> deletedDiagnosis = new ArrayList<RegisteredDiagnosisModel>();
        
        for (RegisteredDiagnosisModel rd : allDiagnosis){
            String status = rd.getStatus();
            if (DIAGNOSIS_EDITED.equals(status)) {
                // 既存(Id>0)を編集したものはupdatedDiagnosisに登録
                if (rd.getId() > 0) {
                    updatedDiagnosis.add(rd);
                // 新規(Id<0)を編集したものはaddedDiagnosisに登録する
                } else {
                    addedDiagnosis.add(rd);
                }
            // 新たに登録したもの(Idは負)はdeletedRdIdからは除外する
            } else if (DIAGNOSIS_DELETED.equals(status) && rd.getId() > 0){
                deletedDiagnosis.add(rd);
            }
        }

        final boolean sendDiagnosis =
                Project.getBoolean(Project.SEND_DIAGNOSIS) 
                && ((ChartImpl) getContext()).getCLAIMListener() != null;
        logger.debug("sendDiagnosis = " + sendDiagnosis);

        // continue to save
        Date confirmed = new Date();
        logger.debug("confirmed = " + confirmed);

        boolean go = true;

        for (RegisteredDiagnosisModel rd : addedDiagnosis) {

            logger.debug("added rd = " + rd.getDiagnosis());
            logger.debug("id = " + rd.getId());

            // 開始日、終了日はテーブルから取得している
            // TODO confirmed, recorded
            rd.setId(0);    // Idは0にしておく
            rd.setKarteBean(getContext().getKarte());           // Karte
            rd.setUserModel(Project.getUserModel());          // Creator
            rd.setConfirmed(confirmed);                     // 確定日
            rd.setRecorded(confirmed);                      // 記録日
            rd.setStatus(IInfoModel.STATUS_FINAL);

            // 開始日=適合開始日 not-null
            if (rd.getStarted() == null) {
                rd.setStarted(confirmed);
            }

            // TODO トラフィック
            rd.setPatientLiteModel(getContext().getPatient().patientAsLiteModel());
            rd.setUserLiteModel(Project.getUserModel().getLiteModel());

            // 転帰をチェックする
            if (!isValidOutcome(rd)) {
                go = false;
                break;
            }
        }
        if (!go) {
            return;
        }

        for (RegisteredDiagnosisModel rd : updatedDiagnosis) {

            logger.debug("updated rd = " + rd.getDiagnosis());
            logger.debug("id = " + rd.getId());


            // 現バージョンは上書きしている
            rd.setUserModel(Project.getUserModel());
            rd.setConfirmed(confirmed);
            rd.setRecorded(confirmed);
            rd.setStatus(IInfoModel.STATUS_FINAL);

            // TODO トラフィック
            rd.setPatientLiteModel(getContext().getPatient().patientAsLiteModel());
            rd.setUserLiteModel(Project.getUserModel().getLiteModel());

            // 転帰をチェックする
            if (!isValidOutcome(rd)) {
                go = false;
                break;
            }
        }
        if (!go) {
            return;
        }
        
        // 中途終了データ作成API(claim)(傷病名削除) (R201207-015)
        // 転帰にdeleteを設定する
        for (RegisteredDiagnosisModel rd : deletedDiagnosis) {
            rd.setOutcome("delete");
            // TODO トラフィック ??
            rd.setPatientLiteModel(getContext().getPatient().patientAsLiteModel());
            rd.setUserLiteModel(Project.getUserModel().getLiteModel());
        }

        final DocumentDelegater ddl = DocumentDelegater.getInstance();
        DiagnosisPutTask task = new DiagnosisPutTask(
                getContext(), 
                addedDiagnosis, 
                updatedDiagnosis, 
                deletedDiagnosis,
                sendDiagnosis, ddl);
        task.execute();

        undoQue.clear();
        redoQue.clear();
        diagTable.clearSelection();

        controlButton();
    }

    private boolean isValidOutcome(RegisteredDiagnosisModel rd) {

        if (rd.getOutcome() == null) {
            return true;
        }

        String start = rd.getStartDate();
        String end = rd.getEndDate();

        if (start == null) {
            JOptionPane.showMessageDialog(
                    getContext().getFrame(),
                    "疾患の開始日がありません。",
                    ClientContext.getFrameTitle("病名チェック"),
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (end == null) {
            JOptionPane.showMessageDialog(
                    getContext().getFrame(),
                    "疾患の終了日がありません。",
                    ClientContext.getFrameTitle("病名チェック"),
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        Date startDate = null;
        Date endDate = null;
        boolean formatOk = true;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            start = start.replace("/", "-");
            end = end.replace("/", "-");
            startDate = sdf.parse(start);
            endDate = sdf.parse(end);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("日付のフォーマットが正しくありません。");
            sb.append("\n");
            sb.append("「yyyy-MM-dd」の形式で入力してください。");
            sb.append("\n");
            sb.append("右クリックでカレンダが使用できます。");
            JOptionPane.showMessageDialog(
                    getContext().getFrame(),
                    sb.toString(),
                    ClientContext.getFrameTitle("病名チェック"),
                    JOptionPane.WARNING_MESSAGE);
            formatOk = false;
        }

        if (!formatOk) {
            return false;
        }

        if (endDate.before(startDate)) {
            StringBuilder sb = new StringBuilder();
            sb.append("疾患の終了日が開始日以前になっています。");
            JOptionPane.showMessageDialog(
                    getContext().getFrame(),
                    sb.toString(),
                    ClientContext.getFrameTitle("病名チェック"),
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        return true;
    }

//pns^
    /**
     * 傷病名件数を設定する。 modified by pns, additional modify by masuda
     * @param cnt 傷病名件数
     */
    public void setDiagnosisCount() {
        
        if (allDiagnosis == null) {
            return;
        }

        // 病名数が変化していることを伝える masuda
        tableModel.fireTableDataChanged();

        // 実際の病名数、今日の病名数をカウント
        int byomeiCount = 0;
        int byomeiCountToday = 0;
        int totalByomeiCount = 0;
        Date today = ModelUtils.getMidnightGc(new Date()).getTime();

        for (RegisteredDiagnosisModel rd : allDiagnosis) {
            String status = rd.getStatus();
            if (!DIAGNOSIS_DELETED.equals(status) && !ORCA_RECORD.equals(status)) {
                setActiveFlag(rd);
                totalByomeiCount++;
                Date start = ModelUtils.getStartDate(rd.getStarted()).getTime();
                if (start.getTime() == today.getTime()) {
                    byomeiCountToday++;
                }
                if (rd.isActive()) {
                    byomeiCount++;
                }
            }
        }
        // pvtに病名数を設定
        PatientVisitModel pvt = getContext().getPatientVisit();
        pvt.setByomeiCount(byomeiCount);
        pvt.setByomeiCountToday(byomeiCountToday);

        // countLbl にセット
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("総病名:");
            sb.append(totalByomeiCount);
            sb.append(" 有効:");
            sb.append(byomeiCount);
            sb.append(" 今日:");
            sb.append(byomeiCountToday);
            sb.append("件");
            countLbl.setText(sb.toString());
        } catch (RuntimeException e) {
            countLbl.setText("");
        }
    }
//pns$

    /**
     * 傷病名エディタを開く。
     */
    public void openEditor2() {

        Window lock = SwingUtilities.getWindowAncestor(this.getUI());
        StampEditor editor = new StampEditor((RegisteredDiagnosisModel[])null, this, lock);
    }

//masuda^ 既存病名編集
    private void openEditor3() {
        selectedRow = diagTable.getSelectedRow();   // ここは一個だけ
        RegisteredDiagnosisModel model = (RegisteredDiagnosisModel) sorter.getObject(selectedRow);
        // 編集するRegisteredDiagnosisModelをeditorに伝える
        Window lock = SwingUtilities.getWindowAncestor(this.getUI());
        StampEditor stampEditor = new StampEditor(new RegisteredDiagnosisModel[]{model}, this, lock);
    }
//masuda$

//pns^
    /**
     * 選択された診断を CLAIM 送信する
     */
    public void sendClaim() {

        // 選択された診断を CLAIM 送信する
        List<RegisteredDiagnosisModel> rdList = new ArrayList<RegisteredDiagnosisModel>();
        Date confirmed = new Date();
        int rows[] = diagTable.getSelectedRows();
        for (int row : rows) {
            RegisteredDiagnosisModel rd = (RegisteredDiagnosisModel) sorter.getObject(row);
            rd.setKarteBean(getContext().getKarte());           // Karte
            rd.setUserModel(Project.getUserModel());          // Creator
            rd.setConfirmed(confirmed);                     // 確定日
            rd.setRecorded(confirmed);                      // 記録日
            // 開始日=適合開始日 not-null
            if (rd.getStarted() == null) {
                rd.setStarted(confirmed);
            }
            rd.setPatientLiteModel(getContext().getPatient().patientAsLiteModel());
            rd.setUserLiteModel(Project.getUserModel().getLiteModel());

            // 転帰をチェックする
            if (!isValidOutcome(rd)) {
                return;
            }

            rdList.add(rd);
        }

        if (!rdList.isEmpty()) {
//masuda^
/*
            IDiagnosisSender sender = new DiagnosisSender();
            sender.setContext(getContext());
            sender.prepare(rdList);
            sender.send(rdList);

*/
            KarteContentSender.getInstance().sendDiagnosis(getContext(), rdList);
//masuda$
        }
    }
//pns$

    /**
     * 指定期間以降の傷病名を検索してテーブルへ表示する。
     * バッググランドスレッドで実行される。
     */
    public void getDiagnosisHistory() {

        NameValuePair pair = (NameValuePair) extractionCombo.getSelectedItem();
        int past = Integer.parseInt(pair.getValue());
        Date fromDate;
        if (past != 0) {
            GregorianCalendar today = new GregorianCalendar();
            today.add(GregorianCalendar.MONTH, past);
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.clear(Calendar.MINUTE);
            today.clear(Calendar.SECOND);
            today.clear(Calendar.MILLISECOND);
            fromDate = today.getTime();
        } else {
            fromDate = ModelUtils.AD1800;
        }
        
        final Date searchFrom = fromDate;
        //final boolean activeOnly = activeBox.isSelected();
        final boolean activeOnly = false;

        DBTask task = new DBTask<List<RegisteredDiagnosisModel>, Void>(getContext()) {

            @Override
            protected List<RegisteredDiagnosisModel> doInBackground() throws Exception {
                DocumentDelegater ddl = DocumentDelegater.getInstance();
                List<RegisteredDiagnosisModel> result = 
                        ddl.getDiagnosisList(getContext().getKarte().getId(), searchFrom, activeOnly);
                return result;
            }

            @Override
            protected void succeeded(List<RegisteredDiagnosisModel> list) {
                
                if (list == null) {
                    return;
                }
                
                if (ascend) {
                    Collections.sort(list);
                } else {
                    Collections.sort(list, Collections.reverseOrder());
                }
                updateIkouTokutei2(list);
                allDiagnosis = list;
                setDiagnosisCount();
                filterDiagnosis();
            }
        };

        task.execute();
    }

    // フィルタリングしてtableModelを設定する
    private void filterDiagnosis() {
        
        boolean activeOnly = activeBox.isSelected();
        
        List<RegisteredDiagnosisModel> rdList;
        
        if (activeOnly) {
            rdList = new ArrayList<RegisteredDiagnosisModel>();
            for (RegisteredDiagnosisModel rd : allDiagnosis) {
                if (rd.isActive()) {
                    rdList.add(rd);
                }
            }
        } else {
            rdList = allDiagnosis;
        }
        
        tableModel.setDataProvider(new ArrayList<RegisteredDiagnosisModel>(rdList));
    }
    
//masuda^
    /**
     * DiagnosisPutTask
     */
    private class DiagnosisPutTask extends DBTask<List<Long>, Void> {

        //private Chart chart;
        private List<RegisteredDiagnosisModel> added;
        private List<RegisteredDiagnosisModel> updated;
        private List<RegisteredDiagnosisModel> deleted;
        private boolean sendClaim;
        private DocumentDelegater ddl;

        public DiagnosisPutTask(
                Chart chart,
                List<RegisteredDiagnosisModel> added,
                List<RegisteredDiagnosisModel> updated,
                List<RegisteredDiagnosisModel> deleted,
                boolean sendClaim,
                DocumentDelegater ddl) {

            super(chart);
            this.added = added;
            this.updated = updated;
            this.deleted = deleted;
            this.sendClaim = sendClaim;
            this.ddl = ddl;
        }

        @Override
        protected List<Long> doInBackground() throws Exception {

            logger.debug("doInBackground");

            // 更新する
            if (updated != null && !updated.isEmpty()) {
                logger.debug("ddl.updateDiagnosis");
                ddl.updateDiagnosis(updated);
            }

            List<Long> result = null;

            // 保存する
            if (added != null && !added.isEmpty()) {
                logger.debug("ddl.putDiagnosis");
                result = ddl.putDiagnosis(added);

                logger.debug("ddl.putDiagnosis() is NoErr");
                for (int i = 0; i < added.size(); i++) {
                    long pk = result.get(i).longValue();
                    logger.debug("persist id = " + pk);
                    RegisteredDiagnosisModel rd = added.get(i);
                    rd.setId(pk);
                }

            }
            
            // 削除も保存時に行う
            if (deleted != null && !deleted.isEmpty()) {
                List<Long> removeList = new ArrayList<Long>();
                for (RegisteredDiagnosisModel rd : deleted) {
                    removeList.add(rd.getId());
                }
                ddl.removeDiagnosis(removeList);
            }

            // CLAIM 送信する
            List<RegisteredDiagnosisModel> rdList = new ArrayList<RegisteredDiagnosisModel>();
            rdList.addAll(added);
            rdList.addAll(updated);
            rdList.addAll(deleted);
            if (sendClaim && !rdList.isEmpty()) {
                KarteContentSender.getInstance().sendDiagnosis(getContext(), rdList);
            }

            return result;
        }

        @Override
        protected void succeeded(List<Long> list) {
            
            logger.debug("DiagnosisPutTask succeeded");
            
            // データベースから削除する項目はなくてもテーブルからは削除
            for (Iterator<RegisteredDiagnosisModel> itr = allDiagnosis.iterator(); itr.hasNext();) {
                RegisteredDiagnosisModel rd = itr.next();
                if (DIAGNOSIS_DELETED.equals(rd.getStatus())) {
                    itr.remove();
                }
            }
            filterDiagnosis();
        }
    }
//masuda$
    
//masuda
    private class DolphinOrcaRenderer extends StripeTableCellRenderer {

        /** Creates new IconRenderer */
        public DolphinOrcaRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {

            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
            RegisteredDiagnosisModel rd = (RegisteredDiagnosisModel) sorter.getObject(row);

            // 選択状態の場合はStripeTableCellRendererの配色を上書きしない
            if (rd != null && !isSelected) {
                // 前景色の設定
                if (DIAGNOSIS_DELETED.equals(rd.getStatus())) {
                    setForeground(DELETED_COLOR);   // 削除病名はグレー文字
                } else if (rd.isIkouByomei()) {
                    setForeground(Color.RED);   // 移行病名
                }
                // 背景色の設定
                if (ORCA_RECORD.equals(rd.getStatus())) {
                    setBackground(ORCA_BACK);   // ORCA病名は背景うす緑
                }
            }

            if (value != null) {
            // 和暦が選択されていたら、開始日・終了日のカラムは和暦に変換して表示する masuda
                if (cb_wareki.isSelected() && (col == START_DATE_COL || col == END_DATE_COL)) {
                    setText(AgeCalculator.toNengo((String) value));
                } else {
                    setText((String) value);
                }
            }
            
            return this;
        }
    }

//pns^
    /**
     * JComboBox を細かくコントロールするための Cell Editor
     */
    private static class MyCellEditor extends DefaultCellEditor2 {

        JComboBox combo;

        public MyCellEditor(JComboBox c) {
            super(c);
            setClickCountToStart(clickCountToStartEdit);
            this.combo = c;

        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int col) {

            // value は String で入ってくる　combo.setSelectedItem(value)は効かない
            switch (col) {
                case CATEGORY_COL:
                case OUTCOME_COL:
                    if (value != null) {
                        int index = itemToIndex(combo, value.toString());
                        combo.setSelectedIndex(index);
                    }
                    break;
            }
            return combo;
        }
    }

    /**
     * JComboBox の項目から index を返す
     * @param combo
     * @param item
     * @return
     */
    public static int itemToIndex(JComboBox combo, String item) {
        int index = 0;
        for(int i=0; i<combo.getItemCount(); i++) {
            if (item.equals(combo.getItemAt(i).toString())) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * 病名修飾語をポップアップで加えられるようにする CellEditor
     * The original was programmed by Dr. pns, modified by masuda
     */
    private class DiagnosisCellEditor extends DefaultCellEditor2 {

        private JPopupMenu popup;

        public DiagnosisCellEditor(JTextField textField) {
            super(textField);
            createPopupMenu();
            textField.addMouseListener(new MouseAdapter() {
/*
                @Override
                public void mouseClicked(MouseEvent e) {
                    int count = e.getClickCount();
                    if (count == 1 && e.getButton() != MouseEvent.BUTTON1) {
                        // 右クリックならpopup
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    } else if (count == 2) {
                        // ダブルクリックなら編集
                        openEditor3();
                    }
                }
*/
                @Override
                public void mousePressed(MouseEvent e){
                    maybePopup(e);
                }
                @Override
                public void mouseReleased(MouseEvent e){
                    maybePopup(e);
                }
                private void maybePopup(MouseEvent e){
                    int count = e.getClickCount();
                    if (count == 1 && e.isPopupTrigger()) {
                        // 右クリックならpopup
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    } else if (count == 3) {
                        // トリプルクリックなら編集
                        openEditor3();
                    }
                }
            });
        }

        @SuppressWarnings("unchecked")
        private void createPopupMenu () {

            popup = new JPopupMenu();
            Set set = diagnosisModifiers.keySet();
            boolean flag = true;

            for (Iterator<String> itr = set.iterator(); itr.hasNext();){
                final String itemName = itr.next();
                final String modifierCode = diagnosisModifiers.get(itemName);
                // 前と後の境目でseparatorを挿入する
                if (flag && modifierCode.startsWith("8")){
                    flag = false;
                    popup.addSeparator();
                }

                JMenuItem item = new JMenuItem(itemName);
                popup.add(item);
                item.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setNewDiagnosis(itemName, modifierCode);
                        //cancelCellEditing();
                        stopCellEditing();
                    }
                });

            }
        }
/*
        // popup 終了
        @Override
        public void cancelCellEditing() {
            //super.cancelCellEditing();
            diagTable.requestFocusInWindow();
        }
*/
        private void setNewDiagnosis(String itemName, String modifierCode) {
            //新しく作った診断を設定
            int r = diagTable.getSelectedRow();
            RegisteredDiagnosisModel newRd = (RegisteredDiagnosisModel) sorter.getObject(r);
            RegisteredDiagnosisModel oldRd = duplicateRd(newRd);
            // 8000台は後ろにつく修飾語
            if (modifierCode.startsWith("8")) {
                newRd.setDiagnosis(oldRd.getDiagnosis() + itemName);
                newRd.setDiagnosisCode(oldRd.getDiagnosisCode() + "." + modifierCode);
            } else {
                newRd.setDiagnosis(itemName + oldRd.getDiagnosis());
                newRd.setDiagnosisCode(modifierCode + "." + oldRd.getDiagnosisCode());
            }
            newRd.setStatus(DIAGNOSIS_EDITED);
            offerUndoQue(oldRd, newRd);
            cancelCellEditing();
        }
    }
//pns$

    /**
     * deque用のモデル
     */
    private static class RegisteredDiagnosisDequeModel {

        private RegisteredDiagnosisModel oldRd;
        private RegisteredDiagnosisModel newRd;

        private RegisteredDiagnosisDequeModel(RegisteredDiagnosisModel oldRd, RegisteredDiagnosisModel newRd){
            this.oldRd = oldRd;
            this.newRd = newRd;
        }
        private RegisteredDiagnosisModel getOldRd(){
            return oldRd;
        }
        private RegisteredDiagnosisModel getNewRd(){
            return newRd;
        }
    }


    private void offerUndoQue(RegisteredDiagnosisModel oldRd, RegisteredDiagnosisModel newRd) {

        RegisteredDiagnosisDequeModel model = new RegisteredDiagnosisDequeModel(oldRd, newRd);

        // dequeに登録
        undoQue.offerLast(model);
        // redoQueはクリア
        redoQue.clear();
        // tableModelに変更を加える
        // oldRd == nullは新規病名追加の場合。tableModelに追加する
        if (oldRd == null) {
            if (ascend) {
                allDiagnosis.add(newRd);
            } else {
                allDiagnosis.add(0, newRd);
            }
        // 編集の場合はoldRdをnewRdで置き換える
        } else {
            long modelId = oldRd.getId();
            int index;
            // oldRdの位置を探す
            for (index = 0; index < allDiagnosis.size(); ++index) {
                if (allDiagnosis.get(index).getId() == modelId) {
                    break;
                }
            }
            // 上書きする
            allDiagnosis.set(index, newRd);
        }

        controlButton();
        filterDiagnosis();
    }

    public void undo() {

        // undoQueから取ってくる
        RegisteredDiagnosisDequeModel model = (RegisteredDiagnosisDequeModel) undoQue.pollLast();

        if (model == null){
            return;
        }
        // redoのためにredoQueに追加する
        redoQue.offerLast(model);

        RegisteredDiagnosisModel oldRd = model.getOldRd();
        RegisteredDiagnosisModel newRd = model.getNewRd();
        // newRdは編集を取り消すRegisteredDiagnosisModel。tableModel内を検索
        // RegisteredDiagnosisModelはKarteEntryBeanなので、idでequalの判断をされている
        // newRdは編集を取り消すRegisteredDiagnosisModel。Idを取得
        long modelId = newRd.getId();
        // tableModel内を検索
        int index;
        for (index = 0; index <allDiagnosis.size(); ++index) {
            if (allDiagnosis.get(index).getId() == modelId) {
                break;
            }
        }
        // oldRd == nullは、追加した病名の場合
        if (oldRd == null) {
            allDiagnosis.remove(index);
        // 病名の編集の場合はoldRdに戻す
        } else {
            allDiagnosis.set(index, oldRd);
        }

        controlButton();
        filterDiagnosis();
    }

    public void redo() {

        // redoQueから取ってくる
        RegisteredDiagnosisDequeModel model = (RegisteredDiagnosisDequeModel) redoQue.pollLast();

        if (model == null){
            return;
        }
        // redoのundoのため、undoQueに追加する
        undoQue.offerLast(model);

        RegisteredDiagnosisModel oldRd = model.getOldRd();
        RegisteredDiagnosisModel newRd = model.getNewRd();

        // oldRd == nullなら、新規追加のundoのredoなのでリストに追加する。挿入位置はキニシナイ
        if (oldRd == null) {
            if (ascend) {
                allDiagnosis.add(newRd);
            } else {
                allDiagnosis.add(0, newRd);
            }
        // oldRd != nullなら、変更のundoのredo
        } else {
            // tableModel内を検索
            //int index = list.indexOf(oldRd);
            long modelId = oldRd.getId();
            // tableModel内を検索
            int index;
            for (index = 0; index < allDiagnosis.size(); ++index) {
                if (allDiagnosis.get(index).getId() == modelId) {
                    break;
                }
            }
            // 元のRegisteredDiagnosisModelに戻す
            allDiagnosis.set(index, newRd);
        }

        controlButton();
        filterDiagnosis();
    }

    /**
     * undo, 保存ボタンのコントロール
     */
    private void controlButton(){

        // 病名数を更新
        setDiagnosisCount();

        if (undoQue != null && !undoQue.isEmpty()){
            undoAction.setEnabled(true);
            updateAction.setEnabled(true);
            // undoができる状態ならdirtyのはず
            setDirty(true);
        } else {
            undoAction.setEnabled(false);
            updateAction.setEnabled(false);
            // undoができない状態ならnot dirtyのはず
            setDirty(false);
        }
        if (redoQue != null && !redoQue.isEmpty()){
            redoAction.setEnabled(true);
        } else {
            redoAction.setEnabled(false);
        }
    }

    /**
     * RegisteredDiagnosisModelを複製する
     */
    private RegisteredDiagnosisModel duplicateRd(RegisteredDiagnosisModel source){

        //byte[] bean = BeanUtils.xmlEncode(source);
        //RegisteredDiagnosisModel model = (RegisteredDiagnosisModel) BeanUtils.xmlDecode(bean);
        RegisteredDiagnosisModel model = (RegisteredDiagnosisModel) BeanUtils.deepCopy(source);
        return model;
    }

//masuda^   和暦で入力したら変換する
    private String convertToSeireki(String input) {
        
        Locale locale = new Locale("ja","JP","JP"); 
        SimpleDateFormat frmtWareki = new SimpleDateFormat("Gyy-MM-dd", locale);
        SimpleDateFormat frmtSeireki = new SimpleDateFormat("yyyy-MM-dd");
        
        input = input.replaceAll("[/\\.]", "-");
        int len = input.length();
        
        if (!input.contains("-") && len > 4) {
            StringBuilder sb = new StringBuilder();
            sb.append(input);
            sb.insert(len - 2, "-");
            sb.insert(len - 4, "-");
            input = sb.toString();
        }
        
        // 西暦に変換してみる
        try {
            Date d = frmtSeireki.parse(input);
            return frmtSeireki.format(d);
        } catch (ParseException ex) {
        }
        
        // 西暦がダメなら和暦？
        try {
            Date d = frmtWareki.parse(input);
            return frmtSeireki.format(d);
        } catch (ParseException ex) {
        }
        
        return null;
    }
//masuda$

    /**
     * ORCAに登録されている病名を取り込む。（テーブルへ追加する）
     * 検索後、ボタンを disabled にする。
     */
    public void viewOrca() {

//masuda^   ORCA病名を一度参照していたら次は病名のインポート
            if (importBtnEnabled){
                importOrcaDisease();
                orcaAction.setEnabled(false);
                return;
            }
//masuda$

        // 患者IDを取得する
        final String patientId = getContext().getPatient().getPatientId();

        DBTask task = new DBTask<List<RegisteredDiagnosisModel>, Void>(getContext()) {

            @Override
            protected List<RegisteredDiagnosisModel> doInBackground() throws Exception {
                SqlOrcaView dao = SqlOrcaView.getInstance();
                
                // activeOnlyか否かで処理を分ける　吉井クリニック　當房さまのご指摘
                boolean activeOnly = activeBox.isSelected();
                List<RegisteredDiagnosisModel> result = activeOnly
                        ? dao.getActiveOrcaDisease(patientId, ascend)
                        : dao.getOrcaDisease(patientId, "00000000", "99999999", ascend);
                if (dao.isNoError()) {
                    return result;
                } else {
                    throw new Exception(dao.getErrorMessage());
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void succeeded(List<RegisteredDiagnosisModel> result) {
                if (result != null && !result.isEmpty()) {
                    if (ascend) {
                        Collections.sort(result);
                    } else {
                        Collections.sort(result, Collections.reverseOrder());
                    }
                    for (RegisteredDiagnosisModel rd : result){
                        // ORCA病名を取り込んだものにIDを割り当てる
                        rd.setId(--rdId);
                        // Active flagを設定する
                        setActiveFlag(rd);
                    }
//masuda^   ORCAの病名を参照したらボタンをインポートボタンに変更する     
                    updateIkouTokutei2(result);
                    allDiagnosis.addAll(result);
                    orcaButton.setIcon(ClientContext.getImageIconAlias(ORCA_IMPORT_IMAGE));
                    orcaButton.setToolTipText("ORCAの病名をインポートします。");
                    importBtnEnabled = true;
                    filterDiagnosis();
//masuda$
                }
                orcaAction.setEnabled(true);
            }
        };

        task.execute();
    }
    
    /**
     * ImageIcon を返す
     */
    private ImageIcon createImageIcon(String name) {
        //String res = RESOURCE_BASE + name;
        //return new ImageIcon(this.getClass().getResource(res));
        return ClientContext.getImageIconAlias(name);
    }
    
//masuda^   Orcaの病名をOpenDolphinにインポート
    private void importOrcaDisease() {

        // ORCAの病名
        List<RegisteredDiagnosisModel> orcaRdList = new ArrayList<RegisteredDiagnosisModel>();
        // Dolphinの病名
        List<RegisteredDiagnosisModel> dolphinRdList = new ArrayList<RegisteredDiagnosisModel>();
        
        // まずはtableModelのRegisteredDiagnosisModelを分類
        for (RegisteredDiagnosisModel rd : allDiagnosis){
            String status = rd.getStatus();
            if (ORCA_RECORD.equals(status)){
                orcaRdList.add(rd);
            } else if (!DIAGNOSIS_DELETED.equals(status)){
                // 削除した病名は含めない
                dolphinRdList.add(rd);
            }
        }
        // 同等の病名がないか検索していく
        for (RegisteredDiagnosisModel orcaRd : orcaRdList){
            boolean found = false;
            for (RegisteredDiagnosisModel dolphinRd : dolphinRdList) {
                if (isSameDiagnosis(orcaRd, dolphinRd)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // 違う病名は新たに登録する
                //RegisteredDiagnosisModel newRd = convertDisOrcaToDolphin(orcaRd);
                RegisteredDiagnosisModel newRd = duplicateRd(orcaRd);
                newRd.setStatus(DIAGNOSIS_EDITED);
                offerUndoQue(orcaRd, newRd);
            }
        }
    }
/*
    private RegisteredDiagnosisModel convertDisOrcaToDolphin(RegisteredDiagnosisModel orca){
        // ORCAの転帰をDolphin風に変換して新しいRegisterdDiagnosisModelを返す

        final String codeSystem = ClientContext.getString("mml.codeSystem.diseaseMaster");
        final DiagnosisOutcomeModel[] diagnosisOutcomeModel = ClientContext.getDiagnosisOutcomeModel();

        RegisteredDiagnosisModel ret = duplicateRd(orca);

        ret.setKarteBean(getContext().getKarte());
        ret.setDiagnosisCodeSystem(codeSystem);
        if (IInfoModel.ORCA_OUTCOME_RECOVERED.equals(orca.getOutcomeDesc())) {
            ret.setDiagnosisOutcomeModel(diagnosisOutcomeModel[1]);      // 治癒->全治
        } else if (IInfoModel.ORCA_OUTCOME_DIED.equals(orca.getOutcomeDesc())) {
            ret.setDiagnosisOutcomeModel(diagnosisOutcomeModel[6]);      // 死亡->死亡
        } else if (IInfoModel.ORCA_OUTCOME_END.equals(orca.getOutcomeDesc())) {
            ret.setDiagnosisOutcomeModel(diagnosisOutcomeModel[4]);      // 中止->中止
        } else if (IInfoModel.ORCA_OUTCOME_TRANSFERED.equals(orca.getOutcomeDesc())) {
            ret.setDiagnosisOutcomeModel(diagnosisOutcomeModel[8]);      // 移行->不変
        } else {
            ret.setDiagnosisOutcomeModel(null);
        }
        return ret;
    }
*/
    private boolean isSameDiagnosis(RegisteredDiagnosisModel rd1, RegisteredDiagnosisModel rd2) {

        // ORCAとDolphinでは転帰が微妙にチガウ
        boolean ret;
        ret = isEqualString(rd1.getDiagnosisCode(), rd2.getDiagnosisCode());    // 傷病名コードが同じ？
        ret = ret && isEqualString(rd1.getStartDate(), rd2.getStartDate());     // 開始日は？
        ret = ret && isEqualString(rd1.getEndDate(), rd2.getEndDate());         // 終了日は？
        ret = ret && isEqualString(rd1.getOutcomeDesc(), rd2.getOutcomeDesc()); // 転帰が同じ？
        // カテゴリ同じ？
        DiagnosisCategoryModel dcm1 = rd1.getDiagnosisCategoryModel();
        DiagnosisCategoryModel dcm2 = rd2.getDiagnosisCategoryModel();
        String category1 = (dcm1 == null) ? null : dcm1.getDiagnosisCategory();
        String category2 = (dcm2 == null) ? null : dcm2.getDiagnosisCategory();
        ret = ret && isEqualString(category1, category2);
        return ret;
    }

    private boolean isEqualString(String str1, String str2) {
        if (str1 == null && str2 == null){
            return true;
        } else if (str1 != null){
            return str1.equals(str2);
        }
        return false;
    }
/*
    private boolean isSameOutcome(String orcaOutcome, String dolphinOutcome) {

        if (dolphinOutcome == null && orcaOutcome == null) {
            return true;
        } else if (IInfoModel.ORCA_OUTCOME_RECOVERED.equals(orcaOutcome)) {
            if ("全治".equals(dolphinOutcome) || "回復".equals(dolphinOutcome)) {
                return true;
            }
        } else if (IInfoModel.ORCA_OUTCOME_DIED.equals(orcaOutcome)) {
            if ("死亡".equals(dolphinOutcome)) {
                return true;
            }
        } else if (IInfoModel.ORCA_OUTCOME_END.equals(orcaOutcome)) {
            if ("中止".equals(dolphinOutcome) || "終了".equals(dolphinOutcome)) {
                return true;
            }
        }
        return false;
    }
*/
    private void checkIkouByomei(RegisteredDiagnosisModel rd) {
        List<RegisteredDiagnosisModel> list = new ArrayList<RegisteredDiagnosisModel>();
        list.add(rd);
        updateIkouTokutei2(list);
    }

    private void updateIkouTokutei2(final List<RegisteredDiagnosisModel> list) {

        final SqlMiscDao dao = SqlMiscDao.getInstance();

        SwingWorker worker = new SwingWorker<List<DiseaseEntry>, Void>() {

            @Override
            protected List<DiseaseEntry> doInBackground() throws Exception {

                List<String> srycdList = new ArrayList<String>();
                for (RegisteredDiagnosisModel rd : list) {
                    // 病名コードを切り出し（接頭語，接尾語は捨てる）
                    String srycd = extractSrycd(rd.getDiagnosisCode());
                    if (srycd != null) {
                        srycdList.add(srycd);
                    }
                }
                List<DiseaseEntry> list = dao.getDiseaseEntries(srycdList);
                return list;
            }

            @Override
            protected void done() {

                List<DiseaseEntry> deList;
                try {
                    deList = get();
                    if (deList == null) {
                        return;
                    }
                    for (RegisteredDiagnosisModel rd : list) {
                        String codeRD = extractSrycd(rd.getDiagnosisCode());
                        if (codeRD != null) {
                            for (DiseaseEntry de : deList) {
                                String codeDE = de.getCode();
                                if (codeDE.equals(codeRD)) {
                                    // 移行病名セット
                                    boolean b = !"99999999".equals(de.getDisUseDate());
                                    rd.setIkouByomei(b);
                                    // byokanrankbnも、うｐだて
                                    rd.setByoKanrenKbn(de.getByoKanrenKbn());
                                    break;
                                }
                            }
                        }
                    }
                    diagTable.repaint();
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                }
            }
        };
        worker.execute();
    }

    // 病名コードを切り出し（接頭語，接尾語は捨てる）
    private String extractSrycd(String diagnosisCode) {
        String[] code = diagnosisCode.split("\\.");
        for (String srycd : code) {
            if (srycd.length() == 7) {     // 病名コードの桁数は７
                return srycd;
            }
        }
        return null;
    }
    
    private void setActiveFlag(RegisteredDiagnosisModel rd) {
        Date today = ModelUtils.getMidnightGc(new Date()).getTime();
        Date start = ModelUtils.getStartDate(rd.getStarted()).getTime();
        Date ended = ModelUtils.getEndedDate(rd.getEnded()).getTime();
        boolean active = ModelUtils.isDateBetween(start, ended, today);
        rd.setActive(active);
    }
//masuda$
}