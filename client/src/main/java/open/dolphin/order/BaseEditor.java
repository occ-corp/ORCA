package open.dolphin.order;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.table.TableColumn;
import open.dolphin.client.AutoKanjiListener;
import open.dolphin.client.AutoRomanListener;
import open.dolphin.client.DefaultCellEditor2;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;

/**
 * BaseEditor
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class BaseEditor extends AbstractStampEditor {
    
    private static final String[] COLUMN_NAMES = {"コード", "診療内容", "数 量", "単 位"};
    private static final String[] METHOD_NAMES = {"getCode", "getName", "getNumber", "getUnit"};
    private static final int[] COLUMN_WIDTH = {50, 200, 10, 10};
    private static final int NUMBER_COLUMN = 2;
    private static final int ITEMNAME_COLUMN = 1;

//masuda^   yukoedymdを追加
    private static final String[] SR_COLUMN_NAMES = 
    {"種別", "コード", "名 称", "単位", "点数", "診区", "病診", "入外", "社老", "有効期限"};
    private static final String[] SR_METHOD_NAMES = 
    {"getSlot", "getSrycd", "getName", "getTaniname", "getTen",
        "getSrysyukbn", "getHospsrykbn", "getNyugaitekkbn", "getRoutekkbn","getYukoedymdStr"};
    private static final int[] SR_COLUMN_WIDTH = {10, 50, 200, 10, 10, 10, 5, 5, 5, 10};
    private static final int SR_NUM_ROWS = 1;
//masuda$

    private BaseView view;

    private ListTableModel<MasterItem> tableModel;

    private ListTableModel<TensuMaster> searchResultModel;

    public BaseEditor() {
        initComponents();
    }

    public BaseEditor(String entity) {
        this(entity, true);
    }

    public BaseEditor(String entity, boolean mode) {
        super(entity, mode);
        initComponents();
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
    
//masuda
    @Override
    public IInfoModel[] getValue() {

        // 常に新規のモデルとして返す
        ModuleModel retModel = new ModuleModel();
        ModuleInfoBean moduleInfo = retModel.getModuleInfoBean();
        moduleInfo.setEntity(getEntity());
        moduleInfo.setStampRole(IInfoModel.ROLE_P);

        // スタンプ名を設定する
        String text = view.getStampNameField().getText().trim();
        if (!text.equals("")) {
            moduleInfo.setStampName(text);
        } else {
            moduleInfo.setStampName(DEFAULT_STAMP_NAME);
        }

        // BundleDolphin を生成する
        BundleDolphin bundle = new BundleDolphin();

        // Dolphin Appli で使用するオーダ名称を設定する
        // StampHolder で使用される（タブ名に相当）
        bundle.setOrderName(getOrderName());

        // セットテーブルのマスターアイテムを取得する
        List<MasterItem> itemList = tableModel.getDataProvider();

        // 診療行為があるかどうかのフラグ
        boolean found = false;
        
        String c007 = null;

        for (MasterItem masterItem : itemList) {
            
//masuda^   ウソ診療行為区分設定
            if (masterItem.getCode().startsWith(".")) {
                c007 = masterItem.getSrysyuKbn();
                found = true;
                continue;
            }
//masuda$
            // マスタアイテムを ClaimItem に変換する
            ClaimItem item = masterToClaimItem(masterItem);

            // 診区を設定する
            // 自費項目がある場合は自費
            String srycd = masterItem.getCode();
            if (!found && srycd.startsWith(ClaimConst.JIHI_CODE_START)) {
                c007 = ClaimConst.RECEIPT_CODE_JIHI;
                found = true;
            }
            if (!found && srycd.startsWith(ClaimConst.JIHI_EXTAX_CODE_START)) {
                c007 = ClaimConst.RECEIPT_CODE_JIHI_EXTAX;
                found = true;
            }
            // 最初に見つかった手技の診区をあとで ClaimBundle に設定する
            if (!found && masterItem.getClassCode() == ClaimConst.SYUGI) {
                // 集計先をマスタアイテム自体へ持たせている
                c007 = getClaim007Code(masterItem.getClaimClassCode());
                if (c007 != null) {
                    found = true;
                }
            }
            bundle.addClaimItem(item);
        }

        // 診療行為区分を設定する
        if (c007 == null) {
            c007 = (getClassCode() != null) ? getClassCode() : getImplied007();
        }
        if (c007 == null) {
            c007 = ClaimConst.RECEIPT_CODE_OTHER;
        }
        if (c007 != null) {
            bundle.setClassCode(c007);
            // Claim007 固定の値
            bundle.setClassCodeSystem(getClassCodeId());
            // 上記テーブルで定義されている診療行為の名称
            bundle.setClassName(MMLTable.getClaimClassCodeName(c007));
        }

        // バンドル数を設定
        String bundleNum =  view.getNumberField().getText().trim();
        bundle.setBundleNumber(bundleNum);
        
        // バンドルメモ復活
        String memo = view.getCommentField().getText().trim();
        if (!memo.equals("")) {
            bundle.setMemo(memo);
        }
        
        retModel.setModel((InfoModel) bundle);

        return new ModuleModel[]{retModel};
    }

//masuda
    @Override
     public void setValue(IInfoModel[] value) {

        // 共通の設定
        BundleDolphin bundle = setInfoModels(value);
        if (bundle == null) {
            return;
        }

        // バンドル数を数量フィールドへ設定する
        String number = bundle.getBundleNumber();
        view.getNumberField().setText(number);
        
        // メモ復活
        String memo = bundle.getMemo();
        if (memo != null) {
            view.getCommentField().setText(memo);
        }
        
        // Stateを変更する
        checkValidation();
    }

    @Override
    protected void checkValidation() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                boolean setIsEmpty = tableModel.getObjectCount() == 0;

                if (setIsEmpty) {
                    view.getStampNameField().setText(DEFAULT_STAMP_NAME);
                }

                boolean setIsValid = true;

                int techCnt = 0;
                int other = 0;

                List<MasterItem> itemList = tableModel.getDataProvider();

                for (MasterItem item : itemList) {

                    if (item.getClassCode() == ClaimConst.SYUGI) {
                        techCnt++;
//masuda^   ウソ診療行為区分はカウントしない
                    } else if (!item.getCode().startsWith(".")){
                        other++;
                    }
//masuda$
                }

                // 何かあればOK
                setIsValid = setIsValid && (techCnt > 0 || other > 0);

                // チェックボックスの設定
                view.getTechCheck().setSelected((techCnt > 0));

                // 通知する
                controlButtons(setIsEmpty, setIsValid);
            }
        });
    }

    @Override
    protected void addSelectedTensu(TensuMaster tm) {
        
//masuda^   ウソ診療行為区分設定
        if (tm.getSrycd().startsWith(".")) {
            MasterItem usoMi = new MasterItem();
            usoMi.setCode(tm.getSrycd());
            usoMi.setName(tm.getName());
            usoMi.setSrysyuKbn(tm.getSrysyukbn());
            // 先頭に追加
            tableModel.addObject(0, usoMi);
            checkValidation();
            return;
        }
//masuda$

        // 項目の受け入れ試験
        String test = tm.getSlot();

        if (passPattern==null || (!passPattern.matcher(test).find())) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        // 診療区分の受け入れ試験
        if (test.equals(ClaimConst.SLOT_SYUGI)) {
            String shinku = tm.getSrysyukbn();
            if (shinkuPattern==null || (!shinkuPattern.matcher(shinku).find())) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
        }

        // MasterItem に変換する 0xFF0D
        MasterItem item = tensuToMasterItem(tm);
        
        // 手技の場合にスタンプ名を設定する
        if (item.getClassCode() == ClaimConst.SYUGI) {
            String stName = view.getStampNameField().getText().trim();
            if (stName.equals("") || stName.equals(DEFAULT_STAMP_NAME)) {
                view.getStampNameField().setText(item.getName());
            }
        }

        // テーブルへ追加する
        tableModel.addObject(item);

        // バリデーションを実行する
        checkValidation();
    }

    @Override
    protected void search(final String text, boolean hitReturn) {

        boolean pass = ipOk();

        int searchType = getSearchType(text, hitReturn);

        pass = pass && (searchType != TT_INVALID);

        if (!pass) {
            return;
        }

        doSearch(text, searchType);
    }

    @Override
    protected final void initComponents() {

        // View
        //view = editorButtonTypeIsIcon() ? new BaseView() : new BaseViewText();
        view = new BaseView();

        // Info Label
        view.getInfoLabel().setText(this.getInfo());

        //------------------------------------------
        // セットテーブルを生成する
        //------------------------------------------
        tableModel = new ListTableModel<MasterItem>(COLUMN_NAMES, START_NUM_ROWS, METHOD_NAMES, null) {

            // NUMBER_COLUMN を編集可能にする
            @Override
            public boolean isCellEditable(int row, int col) {
                
                // 元町皮膚科
                if (col == 1) {
                    String code = (String) this.getValueAt(row, 0);
                    return isNameEditableComment(code);
                }
                // 数量
                if (col == NUMBER_COLUMN) {
                    String code = (String) this.getValueAt(row, 0);
                    return isEditableNumber(code);
                }
                return false;
            }

            // NUMBER_COLUMN に値を設定する
            @Override
            public void setValueAt(Object o, int row, int col) {

                MasterItem mItem = getObject(row);

                if (mItem == null) {
                    return;
                }

                String value = (String) o;
                if (o != null) {
                    value = value.trim();
                }

                // コメント編集 元町皮膚科
                if (col == 1 && isNameEditableComment(mItem.getCode())) {
                    mItem.setName(value);
                    return;
                }

                // 数量
                int code = mItem.getClassCode();

                if (value == null || value.equals("")) {

                    boolean test = (code==ClaimConst.SYUGI ||
                                    code==ClaimConst.OTHER ||
                                    code==ClaimConst.BUI);
                    if (test) {
                        mItem.setNumber(null);
                        mItem.setUnit(null);
                    }
                    checkValidation();
                    return;
                }

                mItem.setNumber(value);
                checkValidation();
            }
        };
        
        JTable setTable = view.getSetTable();
        setTable.setModel(tableModel);

        // 数量カラムにセルエディタを設定する
        JTextField tf = new JTextField();
        tf.addFocusListener(AutoRomanListener.getInstance());
        TableColumn column = setTable.getColumnModel().getColumn(NUMBER_COLUMN);
        DefaultCellEditor de = new DefaultCellEditor2(tf);
        int ccts = Project.getInt("order.table.clickCountToStart", 1);
        de.setClickCountToStart(ccts);
        column.setCellEditor(de);

        // 診療内容カラム(column number = 1)にセルエディタを設定する 元町皮膚科
        JTextField tf2 = new JTextField();
        tf2.addFocusListener(AutoKanjiListener.getInstance());
        column = setTable.getColumnModel().getColumn(1);
        DefaultCellEditor de2 = new DefaultCellEditor2(tf2);
        de2.setClickCountToStart(ccts);
        column.setCellEditor(de2);
        
        //--------------------------
        // 検索結果テーブルを生成する
        //--------------------------
        searchResultModel = new ListTableModel<TensuMaster>(SR_COLUMN_NAMES, SR_NUM_ROWS, SR_METHOD_NAMES, null) {

            @Override
            public Object getValueAt(int row, int col) {

                Object ret = super.getValueAt(row, col);

                switch (col) {

                    case 6:
                        // 病診
                        if (ret!=null) {
                            int index = Integer.parseInt((String) ret);
                            ret = HOSPITAL_CLINIC_FLAGS[index];
                        }
                        break;

                    case 7:
                        // 入外
                        if (ret!=null) {
                            int index = Integer.parseInt((String) ret);
                            ret = IN_OUT_FLAGS[index];
                        }
                        break;

                    case 8:
                        // 社老
                        if (ret!=null) {
                            int index = Integer.parseInt((String) ret);
                            ret = OLD_FLAGS[index];
                        }
                        break;
                }

                return ret;

            }
        };
        JTable searchResultTable = view.getSearchResultTable();
        searchResultTable.setModel(searchResultModel);
       
//masuda^   検査エディタパネルボタンなど
        JButton btn_laboTest = view.getLaboTestBtn();
        JRadioButton inBtn = view.getInRadio();
        JRadioButton outBtn = view.getOutRadio();
        JButton btn_comment = view.getCommentBtn();
        JButton btn_classCode = view.getClassCodeBtn();

        btn_laboTest.setVisible(false);
        btn_classCode.setVisible(false);
        inBtn.setVisible(false);
        outBtn.setVisible(false);

        if (IInfoModel.ENTITY_LABO_TEST.equals(entity)) {
            btn_laboTest.setVisible(true);
            btn_laboTest.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    showLaboTestPanel();
                }
            });
        } else {
            // ウソ診療行為区分入力ボタンはラボ以外で有効
            btn_classCode.setVisible(true);
            btn_classCode.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    searchClaimClassCode();
                }
            });
        }
        
        btn_comment.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch(REGEXP_COMMENT_ALL, TT_CODE_SEARCH);
            }

        });
        
        // 共通の設定
        setupOrderComponents();
        
        // SearchTextFieldにフォーカスをあてる
        setFocusOnSearchTextFld();
//masuda$
    }
    
//masuda^   
    // 検査エディタボタンを押したときの処理
    private void showLaboTestPanel() {

        LaboTestPanel ltp = new LaboTestPanel(this);
        ltp.setMasterItemList(tableModel.getDataProvider());
        ltp.enter();

        // Dialogが閉じられたらここから再開 masuda
        if (ltp.isModified()) {
            tableModel.setDataProvider(ltp.getMasterItemList());
        }
        checkValidation();
    }
    
    // ウソ診療行為区分設定
    private void searchClaimClassCode() {
        
        List<TensuMaster> list = new ArrayList<TensuMaster>();
        Map<String,String> mmlMap = MMLTable.getClaimClassCodeMap();
        for (Iterator itr = mmlMap.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            String code = (String) entry.getKey();
            String name = (String) entry.getValue();
            TensuMaster usoTm = new TensuMaster();
            usoTm.setSrycd("." + code);
            usoTm.setSrysyukbn(code);
            usoTm.setName(name);
            list.add(usoTm);
        }
        Collections.sort(list, new UsoTmComparator());
        

        ListTableModel<TensuMaster> srModel = (ListTableModel<TensuMaster>) view.getSearchResultTable().getModel();
        srModel.setDataProvider(list);
        int cnt = srModel.getObjectCount();
        view.getCountField().setText(String.valueOf(cnt));
        showFirstResult(view.getSearchResultTable());
    }
    
    private class UsoTmComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            TensuMaster tm1 = (TensuMaster) o1;
            TensuMaster tm2 = (TensuMaster) o2;
            return tm1.getSrycd().compareTo(tm2.getSrycd());
        }
    }
//masuda$
}
