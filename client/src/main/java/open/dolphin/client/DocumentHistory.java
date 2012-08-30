package open.dolphin.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import open.dolphin.delegater.DocumentDelegater;
import open.dolphin.delegater.LetterDelegater;
import open.dolphin.dto.DocumentSearchSpec;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * 文書履歴を取得し、表示するクラス。
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class DocumentHistory {

    // PropertyChange 名
    public static final String DOCUMENT_TYPE = "documentTypeProp";
    public static final String SELECTED_HISTORIES = "selectedHistories";
    public static final String SELECTED_KARTES = "selectedKartes";
    public static final String HISTORY_UPDATED = "historyUpdated";
    
    private static final String CMB_KARTE = "カルテ";
    private static final String CMB_LETTER = "紹介状/診断書";
    private static final Color SELF_INSURANCE_COLOR = new Color(255,255,102);


    // 文書履歴テーブル
    private ListTableModel<DocInfoModel> tableModel;
    
    private DocumentHistoryView view;
    
    // 抽出期間コンボボックス
    private JComboBox extractionCombo;
    
    // 文書種別コンボボックス
    private JComboBox contentCombo;
    
    // 件数フィールド 
    private JLabel countField;
    
    // 束縛サポート
    private PropertyChangeSupport boundSupport;
    
    // context 
    private ChartImpl context;
    
    // 選択された文書情報(DocInfo)の配列
    private DocInfoModel[] selectedHistories;
    
    // 抽出コンテント(文書種別)
    private String extractionContent;
    
    // 抽出開始日 
    private Date extractionPeriod;
    
    // 自動的に取得する文書数
    private int autoFetchCount;
    
    // 昇順降順のフラグ 
    private boolean ascending;
    
    // 修正版も表示するかどうかのフラグ
    private boolean showModified;
    
    // フラグ
    private boolean start;
    private NameValuePair[] contentObject;
    private NameValuePair[] extractionObjects;
    
    // Key入力をブロックするリスナ
    private BlockKeyListener blockKeyListener;
    
//masuda^
    // SearchResultを記録
    private SearchResultInspector searchResult;
    public void setSearchResult(SearchResultInspector sr){
        searchResult = sr;
    }
    public SearchResultInspector getSearchResult() {
        return searchResult;
    }
    // 自動的に取得する文書数のデフォルト値
    private int defaultAutoFetchCount;
    public int getAutoFetchCount() {
        return defaultAutoFetchCount;
    }
    
    private boolean extractionIndexUpdated;
    
    public static final NameValuePair[] EXTRACTION_OBJECTS;
    public static final NameValuePair[] CONTENT_OBJECTS;
    
    // 全履歴はaddValue = 0にする
    static {
        EXTRACTION_OBJECTS = new NameValuePair[]{
            new NameValuePair("1ヶ月", "-1"),
            new NameValuePair("3ヶ月", "-3"),
            new NameValuePair("半年", "-6"),
            new NameValuePair("1年", "-12"),
            new NameValuePair("2年", "-24"),
            new NameValuePair("3年", "-36"),
            new NameValuePair("5年", "-60"),
            new NameValuePair("10年", "-120"),
            new NameValuePair("20年", "-240"),
            new NameValuePair("全て", "0")
        };
        CONTENT_OBJECTS = new NameValuePair[]{
            new NameValuePair(CMB_KARTE, IInfoModel.DOCTYPE_KARTE),
            new NameValuePair(CMB_LETTER, IInfoModel.DOCTYPE_LETTER)
        };
    }
    
    private FILTER ins = FILTER.ALL;
    private static enum FILTER {
        ALL, PUBLIC, SELF, ADMISSION
    }
    private List<DocInfoModel> docInfoList;
//masuda$
    
//pns^  全部のカルテを選択する command-A を押すと，KarteDocumentViewer の selectAll が呼ばれて，そこからここが呼ばれる
    public void selectAll() {
        // modified by masuda
        // 検索タブなら検索タブでSelectAllする
        JTabbedPane tabPane = context.getPatientInspector().getTabbedPane();
        if (tabPane.getSelectedComponent() == searchResult.getPanel()) {
            searchResult.selectAll();
        } else {
            JTable table = view.getTable();
            ListTableModel<DocInfoModel> model = (ListTableModel<DocInfoModel>) table.getModel();
            int r = model.getObjectCount(); //rowCount だとだめ。データがないところも全部選択されてしまう
            ListSelectionModel lsm = table.getSelectionModel();
            lsm.setSelectionInterval(0, r - 1);
        }
    }
//pns$
    
    /**
     * 文書履歴オブジェクトを生成する。
     * @param owner コンテキシト
     */
    public DocumentHistory(ChartImpl context) {
        this.context = context;
        initComponent();
        connect();
        start = true;
    }

    /**
     * 履歴テーブルのコレクションを clear する。
     */
    public void clear() {
        if (tableModel != null && tableModel.getDataProvider() != null) {
            tableModel.getDataProvider().clear();
        }
    }

    public void requestFocus() {
        view.getTable().requestFocusInWindow();
    }

    /**
     * 束縛プロパティリスナを登録する。
     * @param propName プロパティ名
     * @param listener リスナ
     */
    public void addPropertyChangeListener(String propName, PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(propName, listener);
    }

    /**
     * 束縛プロパティを削除する。
     * @param propName プロパティ名
     * @param listener リスナ
     */
    public void removePropertyChangeListener(String propName, PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.removePropertyChangeListener(propName, listener);
    }

    /**
     * 選択された文書履歴(複数)を返す。
     * @return 選択された文書履歴(複数)
     */
    public DocInfoModel[] getSelectedHistories() {
        return selectedHistories;
    }

    /**
     * 束縛プロパティの選択された文書履歴(複数)を設定する。通知を行う。
     * @param newSelected 選択された文書履歴(複数)
     */
    public void setSelectedHistories(DocInfoModel[] newSelected) {

        DocInfoModel[] old = selectedHistories;
        selectedHistories = newSelected;
        //
        // リスナへ通知を行う
        //
        if (selectedHistories != null) {
            boundSupport.firePropertyChange(SELECTED_HISTORIES, old, selectedHistories);
        }
    }

    /**
     * 履歴の検索時にテーブルのキー入力をブロックする。
     * @param busy true の時検索中
     */
    public void blockHistoryTable(boolean busy) {
        if (busy) {
            view.getTable().addKeyListener(blockKeyListener);
        } else {
            view.getTable().removeKeyListener(blockKeyListener);
        }
    }

    /**
     * 文書履歴を Karte から取得し表示する。
     */
    public void showHistory() {
//masuda^
        if (extractionIndexUpdated) {
            getDocumentHistory();
            return;
        }
//masuda$        
        //List<DocInfoModel> list = (List<DocInfoModel>)context.getKarte().getEntryCollection("docInfo");
        List<DocInfoModel> list = context.getKarte().getDocInfoList();
        updateHistory(list);
    }

    /**
     * 文書履歴を取得する。
     * 取得するパラメータ(患者ID、文書タイプ、抽出期間)はこのクラスの属性として
     * 定義されている。これらのパラメータは comboBox等で選択される。値が変化する度に
     * このメソッドがコールされる。
     */
    public void getDocumentHistory() {

        if (start && extractionPeriod != null && extractionContent != null) {

            // 検索パラメータセットのDTOを生成する
            DocumentSearchSpec spec = new DocumentSearchSpec();
            spec.setKarteId(context.getKarte().getId());	// カルテID
            spec.setDocType(extractionContent);			// 文書タイプ
            spec.setFromDate(extractionPeriod);			// 抽出期間開始
            spec.setIncludeModifid(showModified);		// 修正履歴
            spec.setCode(DocumentSearchSpec.DOCTYPE_SEARCH);	// 検索タイプ
            spec.setAscending(ascending);
//masuda^   シングルトン化
            //DocInfoTask task = new DocInfoTask(context, spec, new DocumentDelegater());
            DocInfoTask task = new DocInfoTask(context, spec, DocumentDelegater.getInstance());
//masuda$
            task.execute();
        }
    }

    /**
     * 抽出期間等が変化し、履歴を再取得した場合等の処理で、履歴テーブルの更新、 最初の行の自動選択、束縛プロパティの変化通知を行う。
     */
    private void updateHistory(List<DocInfoModel> newHistory) {

//masuda^
        if (newHistory != null) {
            // データベースから取得したDocInfoを保存
            docInfoList = new ArrayList<DocInfoModel>(newHistory);

            // 保険でフィルタリング
            switch (ins) {
                case PUBLIC:
                    newHistory.clear();
                    for (DocInfoModel model : docInfoList) {
                        if (!model.getHealthInsurance().startsWith(IInfoModel.INSURANCE_SELF_PREFIX)) {
                            newHistory.add(model);
                        }
                    }
                    break;
                case SELF:
                    newHistory.clear();
                    for (DocInfoModel model : docInfoList) {
                        if (model.getHealthInsurance().startsWith(IInfoModel.INSURANCE_SELF_PREFIX)) {
                            newHistory.add(model);
                        }
                    }
                    break;
                case ADMISSION:
                    newHistory.clear();
                    for (DocInfoModel model : docInfoList) {
                        if (model.getAdmissionInfo() != null) {
                            newHistory.add(model);
                        }
                    }
                    break;
                case ALL:
                default:
                    break;
            }
        }

        // ソーティングする
        if (newHistory != null && !newHistory.isEmpty()) {
            if (ascending) {
                Collections.sort(newHistory);
            } else {
                Collections.sort(newHistory, Collections.reverseOrder());
            }
        }

        // 文書履歴テーブルにデータの Arraylist を設定する
        tableModel.setDataProvider(newHistory);

        // 束縛プロパティの通知を行う
        boundSupport.firePropertyChange(HISTORY_UPDATED, false, true);

        StringBuilder sb = new StringBuilder();
        switch (ins) {
            case PUBLIC:
                sb.append("健保 ");
                break;
            case SELF:
                sb.append("自費 ");
                break;
            case ADMISSION:
                sb.append("入院");
                break;
            case ALL:
            default:
                sb.append("全て ");
                break;                
        }

        if (newHistory != null && !newHistory.isEmpty()) {

            int cnt = newHistory.size();
            sb.append(String.valueOf(cnt));
            sb.append(" 件");
            int fetchCount = cnt > autoFetchCount ? autoFetchCount : cnt;

            // テーブルの最初の行の自動選択を行う
            JTable table = view.getTable();
            int first = 0;
            int last = 0;

            if (ascending) {
                last = cnt - 1;
                first = cnt - fetchCount;
            } else {
                first = 0;
                last = fetchCount - 1;
            }

            // 自動選択
            table.getSelectionModel().addSelectionInterval(first, last);
            // 選択した行が表示されるようにスクロールする
            Rectangle r = ascending ? table.getCellRect(last, 0, true) : table.getCellRect(0, 0, true);
            table.scrollRectToVisible(r);

        } else {
            sb.append("0 件");
        }
        countField.setText(sb.toString());
//masuda$
    }

    /**
     * 文書履歴のタイトルを変更する。
     */
    public void titleChanged(DocInfoModel docInfo) {

        if (docInfo != null && docInfo.getTitle() != null) {
//masuda^   シングルトン化
            //ChangeTitleTask task = new ChangeTitleTask(context, docInfo, new DocumentDelegater());
            ChangeTitleTask task = new ChangeTitleTask(context, docInfo, DocumentDelegater.getInstance());
//masuda$
            task.execute();
        }
    }

    /**
     * 抽出期間を変更し再検索する。
     */
    public void periodChanged(int state) {
        if (state == ItemEvent.SELECTED) {
            int index = extractionCombo.getSelectedIndex();
            NameValuePair pair = extractionObjects[index];
            String value = pair.getValue();
            int addValue = Integer.parseInt(value);
//masuda^   ほんとの全部に対応
            GregorianCalendar today = new GregorianCalendar();
            if (addValue != 0) {
                today.add(GregorianCalendar.MONTH, addValue);
                today.clear(GregorianCalendar.HOUR_OF_DAY);
                today.clear(GregorianCalendar.MINUTE);
                today.clear(GregorianCalendar.SECOND);
                today.clear(GregorianCalendar.MILLISECOND);
            } else {
                today.clear();
            }
//masuda$
            setExtractionPeriod(today.getTime());
        }
    }

    /**
     * 文書種別を変更し再検索する。
     */
    public void contentChanged(int state) {
        if (state == ItemEvent.SELECTED) {
            int index = contentCombo.getSelectedIndex();
            NameValuePair pair = contentObject[index];
//masuda^   紹介状ならautoFetchCountは1にする
            if (!IInfoModel.DOCTYPE_KARTE.equals(pair.getValue())) {
                autoFetchCount = 1;
            } else {
                autoFetchCount = defaultAutoFetchCount;
            }
//masuda$
            setExtractionContent(pair.getValue());
        }
    }

    /**
     * GUI コンポーネントを生成する。
     */
    private void initComponent() {

        view = new DocumentHistoryView();

        // 履歴テーブルのパラメータを取得する
        String[] columnNames = ClientContext.getStringArray("docHistory.columnNames"); // {"確定日", "内容"};
        //String[] methodNames = ClientContext.getStringArray("docHistory.methodNames"); // {"getFirstConfirmDateTrimTime",// "getTitle"};
//masuda    薬マークを付ける
        String[] methodNames = new String[]{"getFirstConfirmDateWithRp", "getTitle"};
        Class[] columnClasses = {String.class, String.class};

//masuda^
/*
        extractionObjects = new NameValuePair[7];
        extractionObjects[0] = new NameValuePair(YEAR_1, MONTH_12);
        extractionObjects[1] = new NameValuePair(YEAR_2, MONTH_24);
        extractionObjects[2] = new NameValuePair(YEAR_3, MONTH_36);
        extractionObjects[3] = new NameValuePair(YEAR_4, MONTH_48);
        extractionObjects[4] = new NameValuePair(YEAR_5, MONTH_60);
        extractionObjects[5] = new NameValuePair(YEAR_ALL, MONTH_120);
*/
        extractionObjects = EXTRACTION_OBJECTS;
//masuda$
        
        // 文書履歴テーブルを生成する
        tableModel = new ListTableModel<DocInfoModel>(columnNames, 20, methodNames, columnClasses) {

            @Override
            public boolean isCellEditable(int row, int col) {
//masuda^   ReadOnly
                if (context.isReadOnly()) {
                    return false;
                }
//masuda$
                if (col == 1 && getObject(row) != null) {
                    DocInfoModel docInfo = getObject(row);
                    return (docInfo.getDocType().equals(IInfoModel.DOCTYPE_KARTE) ||
                            docInfo.getDocType().equals(IInfoModel.DOCTYPE_S_KARTE));
                }
                return false;
            }

            @Override
            public void setValueAt(Object value, int row, int col) {

                if (col != 1 || value == null || value.equals("")) {
                    return;
                }

                DocInfoModel docInfo = getObject(row);
                if (docInfo == null) {
                    return;
                }

                if (docInfo.getDocType().equals(IInfoModel.DOCTYPE_KARTE) ||
                    docInfo.getDocType().equals(IInfoModel.DOCTYPE_S_KARTE)) {
                    // 文書タイトルを変更し通知する
                    docInfo.setTitle((String) value);
                    titleChanged(docInfo);
                }
            }
        };
        view.getTable().setModel(tableModel);
//masuda^
        // 行の高さ
        //view.getTable().setRowHeight(ClientContext.getMoreHigherRowHeight());

        // カラム幅を調整する
        //view.getTable().getColumnModel().getColumn(0).setPreferredWidth(90);
        //view.getTable().getColumnModel().getColumn(1).setPreferredWidth(190);

        if (ClientContext.isMac()) {
            view.getTable().getColumnModel().getColumn(0).setPreferredWidth(115);
        } else {
            view.getTable().getColumnModel().getColumn(0).setPreferredWidth(100);
        }
        view.getTable().getColumnModel().getColumn(1).setPreferredWidth(180);
        view.getTable().setFocusable(false);
//masuda$

        //-----------------------------------------------
        // Copy 機能を実装する
        //-----------------------------------------------
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        final AbstractAction copyAction = new AbstractAction("コピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyRow();
            }
        };
        view.getTable().getInputMap().put(copy, "Copy");
        view.getTable().getActionMap().put("Copy", copyAction);

        // Delete ACtion
        final AbstractAction deleteAction = new AbstractAction("削除") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                deleteRow();
            }
        };

        // 右クリックコピー
        view.getTable().addMouseListener(new MouseAdapter() {

            private void mabeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = view.getTable().rowAtPoint(e.getPoint());
                    DocInfoModel m = tableModel.getObject(row);
                    if (m == null) {
                        return;
                    }
                    JPopupMenu pop = new JPopupMenu();
                    JMenuItem item2 = new JMenuItem(copyAction);
                    pop.add(item2);

                    if (Project.getBoolean("allow.delete.letter", false) &&
                            extractionContent.equals(IInfoModel.DOCTYPE_LETTER)) {
                        pop.addSeparator();
                        JMenuItem item3 = new JMenuItem(deleteAction);
                        pop.add(item3);
                    }

                    pop.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mabeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mabeShowPopup(e);
            }
        });
        
        // タイトルカラムに IME ON を設定する
        JTextField tf = new JTextField();
        tf.addFocusListener(AutoKanjiListener.getInstance());
        TableColumn column = view.getTable().getColumnModel().getColumn(1);
        column.setCellEditor(new DefaultCellEditor2(tf));
        
//pns^  クリックしただけで，キャレットを表示しないまま編集が始まってしまうのを防ぐ
        view.getTable().putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
//pns$
//masuda^   ストライプテーブル
        // 奇数偶数レンダラを設定する
        //view.getTable().setDefaultRenderer(Object.class, new DocInfoRenderer());        
        DocInfoRenderer renderer = new DocInfoRenderer();
        renderer.setTable(view.getTable());
        renderer.setDefaultRenderer();
//masuda$

        // 文書種別(コンテントタイプ) ComboBox を生成する
//masuda^
        //contentObject = new NameValuePair[2];
        //contentObject[0] = new NameValuePair(CMB_KARTE, IInfoModel.DOCTYPE_KARTE);
        //contentObject[1] = new NameValuePair(CMB_LETTER, IInfoModel.DOCTYPE_LETTER);
        //contentObject[2] = new NameValuePair(CMB_REPLY, IInfoModel.DOCTYPE_LETTER_REPLY);
        //contentObject[3] = new NameValuePair(CMB_REPLY2, IInfoModel.DOCTYPE_LETTER_REPLY2);
        contentObject = CONTENT_OBJECTS;
//masdua$
        contentCombo = view.getDocTypeCombo();

        // 抽出機関 ComboBox を生成する
        extractionCombo = view.getExtractCombo();
//masuda^
        // extractionComboはextractionObjectsから再構成
        extractionCombo.removeAllItems();
        for (NameValuePair nvp : EXTRACTION_OBJECTS) {
            extractionCombo.addItem(nvp.getName());
        }
        // contentComboはcontentObjectから再構成
        contentCombo.removeAllItems();
        for (NameValuePair nvp : contentObject) {
            contentCombo.addItem(nvp.getName());
        }
//masuda$
        // 件数フィールドを生成する
        countField = view.getCntLbl();
        
//masuda^   件数フィールドをクリックすると全て選択する
        countField.setToolTipText("左クリックで全選択、右クリックで保険・入院選択");
        countField.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                maybeShowPopup(e);
            }
        });
//masuda$
    }

//masuda^   健保・自費のポップアップ
    private void maybeShowPopup(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {
        //if (!e.isPopupTrigger()) {
            selectAll();
            return;
        }
        JMenuItem all = new JMenuItem("全て");
        all.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ins = FILTER.ALL;
                updateHistory(docInfoList);
            }
        });

        JMenuItem publicIns = new JMenuItem("健保");
        publicIns.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ins = FILTER.PUBLIC;
                updateHistory(docInfoList);
            }
        });
        JMenuItem self = new JMenuItem("自費");
        self.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ins = FILTER.SELF;
                updateHistory(docInfoList);
            }
        });
        
        JMenuItem admission = new JMenuItem("入院");
        admission.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ins = FILTER.ADMISSION;
                updateHistory(docInfoList);
            }
        });
        
        JPopupMenu popup = new JPopupMenu();
        popup.add(all);
        popup.add(publicIns);
        popup.add(self);
        popup.show(e.getComponent(), 0, 0);
    }
//masuda$
    
    /**
     * レイアウトパネルを返す。
     * @return
     */
    public JPanel getPanel() {
        return (JPanel) view;
    }

    /**
     * Event 接続を行う
     */
    private void connect() {

        // 履歴テーブルで選択された行の文書を表示する
        ListSelectionModel slm = view.getTable().getSelectionModel();
        slm.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    JTable table = view.getTable();
                    int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length > 0) {
                        List<DocInfoModel> list = new ArrayList<DocInfoModel>(1);
                        for (int i = 0; i < selectedRows.length; i++) {
                            DocInfoModel obj = tableModel.getObject(selectedRows[i]);
                            if (obj != null) {
                                list.add(obj);
                            }
                        }
                        DocInfoModel[] selected = list.toArray(new DocInfoModel[list.size()]);
                        if (selected != null && selected.length > 0) {
                            setSelectedHistories(selected);
                        } else {
                            setSelectedHistories((DocInfoModel[]) null);
                        }
                    }
                }
            }
        });

        // 文書種別変更
        contentCombo.addItemListener(EventHandler.create(ItemListener.class, this, "contentChanged", "stateChange"));

        // 抽出期間コンボボックスの選択を処理する
        extractionCombo.addItemListener(EventHandler.create(ItemListener.class, this, "periodChanged", "stateChange"));

        // Preference から文書種別を設定する
        extractionContent = IInfoModel.DOCTYPE_KARTE;

        // Preference から抽出期間を設定する
        int past = Project.getInt(Project.DOC_HISTORY_PERIOD, -12);
        int index = NameValuePair.getIndex(String.valueOf(past), extractionObjects);
        
//masuda^   最終文書歴に合わせてExtraction Periodを設定する
        Date lastVisit = context.getKarte().getLastDocDate();
        if (lastVisit != null) {
            GregorianCalendar today = new GregorianCalendar();
            GregorianCalendar gc = new GregorianCalendar();
            gc.add(GregorianCalendar.MONTH, past);
            
            if (lastVisit.before(gc.getTime())) {
                
                for (int i = index; i < EXTRACTION_OBJECTS.length; ++i) {
                    int addValue = Integer.valueOf(EXTRACTION_OBJECTS[i].getValue());
                    // extractionPeriod == 0（全部）になったらbreak
                    if (addValue == 0) {
                        index = i;
                        break;
                    }
                    gc = new GregorianCalendar();
                    gc.setTime(lastVisit);
                    gc.add(GregorianCalendar.MONTH, -addValue);
                    // extractionPeriodが最終文書を含むようになったらbreak
                    if (gc.after(today)) {
                        index = i;
                        past = addValue;
                        break;
                    }
                }
                extractionIndexUpdated = true;
            }
        }
//masuda$
        
        extractionCombo.setSelectedIndex(index);
        GregorianCalendar today = new GregorianCalendar();
        today.add(GregorianCalendar.MONTH, past);
        today.clear(Calendar.HOUR_OF_DAY);
        today.clear(Calendar.MINUTE);
        today.clear(Calendar.SECOND);
        today.clear(Calendar.MILLISECOND);
        setExtractionPeriod(today.getTime());

        // Preference から自動文書取得数を設定する
        autoFetchCount = Project.getInt(Project.DOC_HISTORY_FETCHCOUNT, 1);
//masuda^   autoFetchCountを保存しておく
        defaultAutoFetchCount = autoFetchCount;
//masuda$
        
        // Preference から昇順降順を設定する
        ascending = Project.getBoolean(Project.DOC_HISTORY_ASCENDING);

        // Preference から修正履歴表示を設定する
        //showModified = Project.getBoolean(Project.DOC_HISTORY_SHOWMODIFIED, false);
        //showModified = Project.getBoolean(Project.DOC_HISTORY_SHOWMODIFIED);
        // 修正履歴は Project 設定から外す 常に false

        // 文書履歴テーブルのキーボード入力をブロックするリスナ
        blockKeyListener = new BlockKeyListener();
    }

    /**
     * 選択されている行をコピーする。
     */
    public void copyRow() {
        StringBuilder sb = new StringBuilder();
        int numRows = view.getTable().getSelectedRowCount();
        int[] rowsSelected = view.getTable().getSelectedRows();
        int numColumns = view.getTable().getColumnCount();

        for (int i = 0; i < numRows; i++) {

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
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
        }
    }

    /**
     * 削除、非公開
     */
    public void deleteRow() {
        int row = view.getTable().getSelectedRow();
        DocInfoModel m = tableModel.getObject(row);
        if (m==null) {
            return;
        }

        StringBuilder sb = new StringBuilder(m.getTitle());
        sb.append("を削除しますか?");
        String msg = sb.toString();

        Object[] cstOptions = new Object[]{"はい", "いいえ"};

        int select = JOptionPane.showOptionDialog(
                SwingUtilities.getWindowAncestor(view.getTable()),
                msg,
                ClientContext.getFrameTitle("削除"),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                ClientContext.getImageIcon("cancl_32.gif"),
                cstOptions,"はい");

        if (select != 0) {
            return;
        }

        DeleteTask task = new DeleteTask(context, m.getDocPk());
        task.execute();
    }

    /**
     * キーボード入力をブロックするリスナクラス。
     */
    class BlockKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {
            e.consume();
        }

        /** Handle the key-pressed event from the text field. */
        @Override
        public void keyPressed(KeyEvent e) {
            e.consume();
        }

        /** Handle the key-released event from the text field. */
        @Override
        public void keyReleased(KeyEvent e) {
            e.consume();
        }
    }

    /**
     * 検索パラメータの文書タイプを設定する。。
     * @param extractionContent 文書タイプ
     */
    public void setExtractionContent(String extractionContent) {
        String old = this.extractionContent;
        this.extractionContent = extractionContent;
        boundSupport.firePropertyChange(DOCUMENT_TYPE, old, this.extractionContent);
        getDocumentHistory();
    }

    /**
     * 検索パラメータの文書タイプを返す。
     * @return 文書タイプ
     */
    public String getExtractionContent() {
        return extractionContent;
    }

    /**
     * 検索パラメータの抽出期間を設定する。
     * @param extractionPeriod 抽出期間
     */
    public void setExtractionPeriod(Date extractionPeriod) {
        this.extractionPeriod = extractionPeriod;
        getDocumentHistory();
    }

    /**
     * 検索パラメータの抽出期間を返す。
     * @return 抽出期間
     */
    public Date getExtractionPeriod() {
        return extractionPeriod;
    }

    /**
     * 文書履歴表示の昇順/降順を設定する。
     * @param ascending 昇順の時 true
     */
    public void setAscending(boolean ascending) {
        this.ascending = ascending;
        getDocumentHistory();
//masuda^   ついでにSearchResultもソートしなおし
        searchResult.setAscending(ascending);
//masuda$
    }

    /**
     * 文書履歴表示の昇順/降順を返す。
     * @return 昇順の時 true
     */
    public boolean isAscending() {
        return ascending;
    }

    /**
     * 修正版を表示するかどうかを設定する。
     * @param showModifyed 表示する時 true
     */
    public void setShowModified(boolean showModifyed) {
        this.showModified = showModifyed;
        getDocumentHistory();
    }

    /**
     * 修正版を表示するかどうかを返す。
     * @return 表示する時 true
     */
    public boolean isShowModified() {
        return showModified;
    }

    /**
     * 検索タスク。
     */
    class DocInfoTask extends DBTask<List<DocInfoModel>, Void> {

        // Delegator
        private DocumentDelegater ddl;
        // 検索パラメータを保持するオブジェクト
        private DocumentSearchSpec spec;

        public DocInfoTask(Chart ctx, DocumentSearchSpec spec, DocumentDelegater ddl) {
            super(ctx);
            this.spec = spec;
            this.ddl = ddl;
        }

        @Override
        protected List<DocInfoModel> doInBackground() throws Exception {
            List<DocInfoModel> result = ddl.getDocumentList(spec);
            return result;
        }

        @Override
        protected void succeeded(List<DocInfoModel> result) {
            if (result != null) {
                updateHistory(result);
            }
        }
    }

    class DeleteTask extends DBTask<Void, Void> {

        // 検索パラメータを保持するオブジェクト
        private long spec;

        public DeleteTask(Chart ctx, long spec) {
            super(ctx);
            this.spec = spec;
        }

        @Override
        protected Void doInBackground() throws Exception {
//masuda^   シングルトン化
            //LetterDelegater ldl = new LetterDelegater();
            LetterDelegater ldl = LetterDelegater.getInstance();
//masuda$
            ldl.delete(spec);
            return null;
        }

        @Override
        protected void succeeded(Void result) {
            getDocumentHistory();
        }
    }

    /**
     * タイトル変更タスククラス。
     */
    class ChangeTitleTask extends DBTask<Boolean, Void> {

        // DocInfo
        private DocInfoModel docInfo;
        // Delegator
        private DocumentDelegater ddl;

        public ChangeTitleTask(Chart ctx, DocInfoModel docInfo, DocumentDelegater ddl) {
            super(ctx);
            this.docInfo = docInfo;
            this.ddl = ddl;
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            ddl.updateTitle(docInfo);
            //return new Boolean(ddl.isNoError());
            return true;
        }

        @Override
        protected void succeeded(Boolean result) {
        }
    }

//masuda^   ストライプテーブル
    private class DocInfoRenderer extends StripeTableCellRenderer {

        public DocInfoRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            DocInfoModel info = tableModel.getObject(row);

            // 自費保険のカラーリング
            if (info != null && !isSelected
                    && info.getDocType().equals(IInfoModel.DOCTYPE_KARTE)
                    && info.getHealthInsurance() != null
                    && info.getHealthInsurance().startsWith(IInfoModel.INSURANCE_SELF_PREFIX)) {
                setBackground(SELF_INSURANCE_COLOR);
            }

            return this;
        }
    }
//masuda$
}
