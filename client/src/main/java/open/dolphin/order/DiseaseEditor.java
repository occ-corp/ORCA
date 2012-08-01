package open.dolphin.order;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import open.dolphin.client.*;
import open.dolphin.dao.SqlMasterDao;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.infomodel.DiseaseEntry;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;
import open.dolphin.tr.RegisteredDiagnosisTransferHandler;
import open.dolphin.util.BeanUtils;
import open.dolphin.util.StringTool;

/**
 * 傷病名編集テーブルクラス。
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public final class DiseaseEditor extends AbstractStampEditor {
    
    private static final String[] COLUMN_NAMES = {"コード", "疾患名/修飾語", "エイリアス"};
    private static final String[] METHOD_NAMES = {"getDiagnosisCode", "getDiagnosisName", "getDiagnosisAlias"};
    private static final int[] COLUMN_WIDTH = {20, 135, 135};
    
    private static final String[] SR_COLUMN_NAMES = {"コード", "名 称", "カナ", "ICD10", "特定疾患"};
    private static final String[] SR_METHOD_NAMES = {"getCode", "getName", "getKana", "getIcdTen", "getByoKanrenKbnStr"};
    private static final int[] SR_COLUMN_WIDTH = {10, 135, 135, 10, 10};
    private static final int SR_NUM_ROWS = 1;
    
    // 傷病名の修飾語コード
    private static final String MODIFIER_CODE = "ZZZ";
    
    // 傷病名手入力時につけるコード
    private static final String HAND_CODE = "0000999";
    
    // Diagnosis table のパラメータ
    private static final int NAME_COL       = 1;
    private static final int ALIAS_COL      = 2;
    private static final int DISEASE_NUM_ROWS = 10;
    
//masuda ↓復活
    private static final String TOOLTIP_TABLE  = "コードのカラムで Drag & Drop で順番を入れ替えることができます";
    private static final String TOOLTIP_COMBINE  = "テーブルの行を連結して修飾語付きの傷病名にします";
    
    // Table model
    private DiseaseView view;

    private ListTableModel<RegisteredDiagnosisModel> tableModel;

    private ListTableModel<DiseaseEntry> searchResultModel;

    public DiseaseEditor() {
        this(true);
    }

    public DiseaseEditor(boolean mode) {
        super();
        initComponents();
        this.setFromStampEditor(mode);
        this.setOrderName("傷病名");
    }
    
    @Override
    protected String[] getColumnNames() {
        return COLUMN_NAMES;
    }

    @Override
    protected String[] getColumnMethods() {
        return METHOD_NAMES;
    }

    @Override
    protected int[] getColumnWidth() {
        return COLUMN_WIDTH;
    }

    @Override
    protected String[] getSrColumnNames() {
        return SR_COLUMN_NAMES;
    }

    @Override
    protected String[] getSrColumnMethods() {
        return SR_METHOD_NAMES;
    }

    @Override
    protected int[] getSrColumnWidth() {
        return SR_COLUMN_WIDTH;
    }
    
    @Override
    public JPanel getView() {
        return (JPanel) view;
    }

    @Override
    public void dispose() {

        if (tableModel != null) {
            tableModel.clear();
        }

        if (searchResultModel != null) {
            searchResultModel.clear();
        }

        super.dispose();
    }

    /**
     * 傷病名テーブルをスキャンし修飾語つきの傷病にして返す。
     */
    @Override
    public IInfoModel[] getValue() {

        RegisteredDiagnosisModel diagnosis = null;

        StringBuilder name = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // テーブルをスキャンする
        int count = tableModel.getObjectCount();
        for (int i = 0; i < count; i++) {

            RegisteredDiagnosisModel diag = tableModel.getObject(i);
            String diagCode = diag.getDiagnosisCode();

            if (!diagCode.startsWith(MODIFIER_CODE)) {
                //
                // 修飾語でない場合は基本病名と見なし、パラメータを設定する
                //
//masuda^   病名以外は編集元を引き継ぐ
                if (getOldValue() != null && getOldValue().length != 0){
                    RegisteredDiagnosisModel oldRd = ((RegisteredDiagnosisModel[])getOldValue())[0];
                    diagnosis = duplicateRd(oldRd);
                } else {
                    diagnosis = new RegisteredDiagnosisModel();
                }
                diagnosis.setByoKanrenKbn(diag.getByoKanrenKbn());
//masuda$
                diagnosis.setDiagnosisCodeSystem(diag.getDiagnosisCodeSystem());

            } else {
                // ZZZ をトリムする ORCA 実装
                diagCode = diagCode.substring(MODIFIER_CODE.length());
            }

            // コードを . で連結する
            if (code.length() > 0) {
                code.append(".");
            }
            code.append(diagCode);

            // 名前を連結する
            name.append(diag.getDiagnosis());

        }

        if (diagnosis != null && name.length() > 0 && code.length() > 0) {

            // 名前とコードを設定する
            diagnosis.setDiagnosis(name.toString());
            diagnosis.setDiagnosisCode(code.toString());
            List<RegisteredDiagnosisModel> ret = new ArrayList<RegisteredDiagnosisModel>(1);
            ret.add(diagnosis);

            return ret.toArray(new RegisteredDiagnosisModel[0]);

        } else {
            return null;
        }
    }
    
    @Override
    public void setValue(IInfoModel[] value) {

//masuda^
        // 連続して編集される場合があるのでテーブル内容等をクリアする
        clear();
        if (value == null || value.length == 0) {
            return;
        }
        setOldValue(value);
        RegisteredDiagnosisModel rd = ((RegisteredDiagnosisModel[]) value)[0];
        // null であればリターンする
        if (rd == null) {
            return;
        }

        // ここからは既存病名の編集
        // 修飾語を含んでいなければ（病名コードに"."がなければ）そのままセットする
        if (!rd.getDiagnosisCode().contains(".")){
            tableModel.addObject(rd);
            return;
        }

        // 修飾語を含む傷病名を編集する場合はコードからMasuterItemを調べてtableに追加 masuda
        final String codeSystem = ClientContext.getString("mml.codeSystem.diseaseMaster");
        String[] srycdArray = rd.getDiagnosisCode().split("\\.");
        final List<String> srycdList = new ArrayList<String>(srycdArray.length);
                for (String srycd : srycdArray) {
            // 修飾語は４桁。コードにZZZを追加する。
            if (srycd.length() == 4){
                srycdList.add(MODIFIER_CODE + srycd);
            } else {
                srycdList.add(srycd);
            }
        }

        final SqlMiscDao dao2 = SqlMiscDao.getInstance();
        final BlockGlass blockGlass = new BlockGlass();
        // 親がJFrameのときとJDialogのときがある masuda
        Window parent = SwingUtilities.getWindowAncestor(getView());
        if (parent instanceof JFrame) {
            JFrame frame = (JFrame) parent;
            frame.setGlassPane(blockGlass);
            blockGlass.setSize(frame.getSize());
        } else if (parent instanceof JDialog) {
            JDialog dialog = (JDialog) parent;
            dialog.setGlassPane(blockGlass);
            blockGlass.setSize(dialog.getSize());
        }

        SwingWorker worker = new SwingWorker<List<DiseaseEntry>, Void>() {

            @Override
            protected List<DiseaseEntry> doInBackground() throws Exception {
                blockGlass.block();
                // 傷病名コードからDiseaseEntryを取得
                List<DiseaseEntry> result = dao2.getDiseaseEntries(srycdList);
                if (result == null || !dao2.isNoError()){
                    throw new Exception(dao2.getErrorMessage());
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    List<DiseaseEntry> result = get();
                    if (result == null) {
                        return;
                    }
                    List<DiseaseEntry> deList = result;
                    // 取得したDiseaseEntryからRegisteredDiagnosisModelを作成しテーブルに追加
                    // 順番がばらばらで帰ってくるので元の順に並べ替える
                    for (String code : srycdList) {
                        for (DiseaseEntry de : deList) {
                            if (code.equals(de.getCode())) {
                                RegisteredDiagnosisModel model = new RegisteredDiagnosisModel();
                                model.setDiagnosis(de.getName());
                                model.setDiagnosisCode(de.getCode());
                                model.setDiagnosisCodeSystem(codeSystem);
                                model.setByoKanrenKbn(de.getByoKanrenKbn());
                                tableModel.addObject(model);
                                break;
                            }
                        }
                    }
                    // 状態マシンへイベントを送信する
                    checkValidation();
                } catch (Exception ex) {
                    String msg = "ORCAに接続できません";
                    String title = ClientContext.getFrameTitle("傷病名エディタ");
                    JOptionPane.showMessageDialog(getView(), msg, title, JOptionPane.ERROR_MESSAGE);
                } finally {
                    blockGlass.unblock();
                }
            }
        };

        worker.execute();
//masuda$
    }
    

    @Override
    protected void checkValidation() {
        
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                boolean setIsEmpty = (tableModel.getObjectCount() == 0);

                boolean setIsValid = true;

                int diseaseCnt = 0;
                List<RegisteredDiagnosisModel> itemList = tableModel.getDataProvider();

                for (RegisteredDiagnosisModel diag : itemList) {

                    if (diag.getDiagnosisCode().startsWith(MODIFIER_CODE)) {
                        continue;

                    } else {
                        diseaseCnt++;
                    }
                }

                setIsValid = setIsValid && (diseaseCnt > 0);
                setIsValid = setIsValid && isValidDiagnosis();
                reconstructDiagnosis();

                // 傷病名チェックボックス
                view.getDiseaseCheck().setSelected((diseaseCnt > 0));
                
                // 通知する
                controlButtons(setIsEmpty, setIsValid);
            }
        });
    }

    @Override
    protected void addSelectedTensu(TensuMaster tm) {
        // No use
    }

    @Override
    protected void search(final String text, boolean hitReturn) {

        boolean pass = ipOk();

        final int searchType = getSearchType(text, hitReturn);
        pass = pass && (searchType==TT_LETTER_SEARCH);

        if (!pass) {
            return;
        }

        // 件数をゼロにしておく
        view.getCountField().setText("0");

        // 検索を実行する
        SwingWorker worker = new SwingWorker<List<DiseaseEntry>, Void>() {

            @Override
            protected List<DiseaseEntry> doInBackground() throws Exception {

                SqlMasterDao dao = SqlMasterDao.getInstance();
                String d = effectiveFormat.format(new Date());
                boolean b = view.getPartialChk().isSelected();
                List<DiseaseEntry> result;
//masuda    修飾語ボタンの処理
                if (text.startsWith(MODIFIER_CODE)) {
                    result = dao.getDiseaseByCode(text, d, true);
                } else {
                    result = dao.getDiseaseByName(StringTool.hiraganaToKatakana(text), d, b);
                }
                if (!dao.isNoError()) {
                    throw new Exception(dao.getErrorMessage());
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    List<DiseaseEntry> result = get();
                    searchResultModel.setDataProvider(result);
                    int cnt = searchResultModel.getObjectCount();
                    view.getCountField().setText(String.valueOf(cnt));
//masuda    検索後は最初の行を表示させる
                    showFirstResult(view.getSearchResultTable());
                    
                } catch (InterruptedException ex) {

                } catch (ExecutionException ex) {
                    alertSearchError(ex.getMessage());
                }
            }
        };

        worker.execute();
    }

    @Override
    protected void clear() {
        tableModel.clear();
        view.getStampNameField().setText("");
        checkValidation();
    }

    @Override
    protected final void initComponents() {

        view = new DiseaseView();
        //view = editorButtonTypeIsIcon() ? new DiseaseView() : new DiseaseViewText();
        
        // 病名テーブルを生成する
        tableModel = new ListTableModel<RegisteredDiagnosisModel>(COLUMN_NAMES, DISEASE_NUM_ROWS, METHOD_NAMES, null) {
            
            // 病名カラムも修飾語の編集が可能
            @Override
            public boolean isCellEditable(int row, int col) {
                
                boolean ret = false;
                
                RegisteredDiagnosisModel model = getObject(row);
                
                if (col == NAME_COL) {
                    if (model == null) {
                        ret = true;
//pns^              HAND_CODE 以外，編集不可とする
/*
                    } else if (!model.getDiagnosisCode().startsWith(MODIFIER_CODE)) {
                        ret = true;
*/
                    }
                    } else if (model.getDiagnosisCode().equals(HAND_CODE)) {
                        ret = true;
//pns$
                } else if (col == ALIAS_COL) {
                    if (model != null && (!model.getDiagnosisCode().startsWith(MODIFIER_CODE))) {
                        ret = true;
                    }
                }
                
                return ret;
            }
            
            @Override
            public void setValueAt(Object o, int row, int col) {
                
                if (o == null) {
                    return;
                }
                
                int index = ((String)o).indexOf(',');
                if (index > 0) {
                    return;
                }
                
                RegisteredDiagnosisModel model = getObject(row);
                String value = (String) o;
                
                switch (col) {
                    
                    case NAME_COL:
                        //
                        // 病名が手入力された場合は、コードに 0000999 を設定する
                        //
                        if (!value.equals("")) {
                            if (model != null) {
                                model.setDiagnosis(value);
                                model.setDiagnosisCode(HAND_CODE);
                                fireTableCellUpdated(row, col);

                            } else {
                                model = new RegisteredDiagnosisModel();
                                model.setDiagnosis(value);
                                model.setDiagnosisCode(HAND_CODE);
                                addObject(model);
                                checkValidation();
                            }
                        }
                        break;
                        
                    case ALIAS_COL:
                        //
                        // エイリアスの入力があった場合
                        //
                        if (model != null) {
                            String test = model.getDiagnosis();
                            int idx = test.indexOf(',');
                            if (idx >0 ) {
                                test = test.substring(0, idx);
                                test = test.trim();
                            }
                            if (value.equals("")) {
                                model.setDiagnosis(test);
                            } else {
                                StringBuilder sb = new StringBuilder();
                                sb.append(test);
                                sb.append(",");
                                sb.append(value);
                                model.setDiagnosis(sb.toString());
                            }
                        }
                        break;
                }
            }
        };
        
        // SetTable を生成し transferHandler を生成する
        JTable table = view.getSetTable();
        table.setModel(tableModel);
        
//masuda^   tool tip textを復活
        table.setToolTipText(TOOLTIP_TABLE);
//masuda$
        
        // Set Table の行の高さ
        //table.setRowHeight(ClientContext.getMoreHigherRowHeight());

        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT);
 //masuda^ 
        // TransferHandler
        table.setTransferHandler(new RegisteredDiagnosisTransferHandler());
        table.addMouseMotionListener(new SetTableMouseMotionListener());
//masuda$
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        ListSelectionModel m = table.getSelectionModel();
        m.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
//masuda^   DeleteBtnのコントロールはcheckValidationに移動
                    checkValidation();
//masuda$
                }
            }
        });

//masuda^   ストライプテーブル
        //table.setDefaultRenderer(Object.class, new OddEvenRowRenderer());
        StripeTableCellRenderer setRenderer = new StripeTableCellRenderer(table);
        setRenderer.setDefaultRenderer();
//masuda$
        
        // CellEditor を設定する
        // 疾患名
        TableColumn column = table.getColumnModel().getColumn(NAME_COL);
        JTextField nametf = new JTextField();
        nametf.addFocusListener(AutoKanjiListener.getInstance());
        DefaultCellEditor nameEditor = new DefaultCellEditor2(nametf);
        int clickCountToStart = Project.getInt("diagnosis.table.clickCountToStart", 1);
        nameEditor.setClickCountToStart(clickCountToStart);
        column.setCellEditor(nameEditor);

        // 病名エイリアス
        column = table.getColumnModel().getColumn(ALIAS_COL);
        JTextField aliastf = new JTextField();
        aliastf.addFocusListener(AutoRomanListener.getInstance()); // alias 
        DefaultCellEditor aliasEditor = new DefaultCellEditor2(aliastf);
        aliasEditor.setClickCountToStart(clickCountToStart);
        column.setCellEditor(aliasEditor);
        
        // 列幅設定
        int len = COLUMN_WIDTH.length;
        for (int i = 0; i < len; i++) {
            column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(COLUMN_WIDTH[i]);
        }

        //
        // 病名マスタ検索結果テーブル
        //
        searchResultModel = new ListTableModel<DiseaseEntry>(SR_COLUMN_NAMES, 20, SR_METHOD_NAMES, null);

        JTable searchResultTable = view.getSearchResultTable();
        searchResultTable.setModel(searchResultModel);
        //searchResultTable.setRowHeight(ClientContext.getHigherRowHeight());
        searchResultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultTable.setRowSelectionAllowed(true);
        ListSelectionModel lm = searchResultTable.getSelectionModel();
        lm.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {

                if (e.getValueIsAdjusting() == false) {

                    int row = view.getSearchResultTable().getSelectedRow();

                    DiseaseEntry o = searchResultModel.getObject(row);

                    if (o != null) {

                        String codeSystem = ClientContext.getString("mml.codeSystem.diseaseMaster");
                        RegisteredDiagnosisModel model = new RegisteredDiagnosisModel();
                        model.setDiagnosis(o.getName());
                        model.setDiagnosisCode(o.getCode());
                        model.setDiagnosisCodeSystem(codeSystem);
//masuda^   ByoKanrenKbnを設定
                        model.setByoKanrenKbn(o.getByoKanrenKbn());
//masuda$
                        tableModel.addObject(model);
                        checkValidation();
                    }
                    //searchTextField.requestFocus();
                    setFocusOnSearchTextFld();
                }
            }
        });

        len = SR_COLUMN_WIDTH.length;
        for (int i = 0; i < len; i++) {
            column = searchResultTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(SR_COLUMN_WIDTH[i]);
        }
        
//masuda^   ストライプテーブル
        //searchResultTable.setDefaultRenderer(Object.class, new OddEvenRowRenderer());
        StripeTableCellRenderer srRenderer = new StripeTableCellRenderer(searchResultTable);
        srRenderer.setDefaultRenderer();
//masuda$
        
        // 複合病名フィールド
        JTextField combinedDiagnosis = view.getStampNameField();
        combinedDiagnosis.setEditable(false);
        combinedDiagnosis.setToolTipText(TOOLTIP_COMBINE);
        
//masuda^   修飾語ボタン
        view.getModifierBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search(MODIFIER_CODE, true);
            }
        });
//masuda$
        
        // 共通の設定
        setupCommonComponents();
        
        // SearchTextFieldにフォーカスをあてる
        setFocusOnSearchTextFld();
    }

    /**
     * テーブルをスキャンし、傷病名コンポジットする。
     */
    public void reconstructDiagnosis() {
//masuda^
        StringBuilder sb = new StringBuilder();
        int count = tableModel.getObjectCount();
        for (int i = 0; i < count; i++) {
            RegisteredDiagnosisModel diag = tableModel.getObject(i);
            sb.append(diag.getDiagnosis());
        }
        view.getStampNameField().setText(sb.toString());
    }
    
//masuda^   使わない
    /**
     * 修飾語をふくんでいるかどうかを返す。
     */
/*
    private boolean hasModifier() {
        boolean hasModifier = false;
        int count = tableModel.getObjectCount();
        for (int i = 0; i < count; i++) {
            RegisteredDiagnosisModel diag = tableModel.getObject(i);
            if (diag.getDiagnosisCode().startsWith(MODIFIER_CODE)) {
                hasModifier = true;
                break;
            }
        }
        return hasModifier;
    }
*/

    private boolean isValidDiagnosis(){
        boolean prePosition = true;
        List<RegisteredDiagnosisModel> itemList = tableModel.getDataProvider();
        for (RegisteredDiagnosisModel rd : itemList) {
            String srycd = rd.getDiagnosisCode();
            if (prePosition) {
                if (srycd.matches("ZZZ[0-7][0-9]{3}")) {
                    continue;
                } else if (!srycd.startsWith("ZZZ")) {
                    prePosition = false;
                    continue;
                } else {
                    return false;
                }
            } else {
                if (srycd.startsWith("ZZZ8")){
                    continue;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * RegisteredDiagnosisModelを複製する
     */
    private RegisteredDiagnosisModel duplicateRd(RegisteredDiagnosisModel source){

        byte[] bean = BeanUtils.xmlEncode(source);
        RegisteredDiagnosisModel model = (RegisteredDiagnosisModel) BeanUtils.xmlDecode(bean);
        return model;
    }
//masuda$
}

