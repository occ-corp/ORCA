package open.dolphin.order;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableColumn;
import open.dolphin.client.AutoKanjiListener;
import open.dolphin.client.AutoRomanListener;
import open.dolphin.client.DefaultCellEditor2;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;

/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class InstractionEditor extends AbstractStampEditor {

    private static final String[] COLUMN_NAMES = {"コード", "診療内容", "数 量", "単 位"};
    private static final String[] METHOD_NAMES = {"getCode", "getName", "getNumber", "getUnit"};
    private static final int[] COLUMN_WIDTH = {50, 200, 10, 10};
    private static final int NUMBER_COLUMN = 2;

//masuda^   yukoedymdを追加
    private static final String[] SR_COLUMN_NAMES = 
    {"種別", "コード", "名 称", "単位", "点数", "診区", "病診", "入外", "社老", "有効期限"};
    private static final String[] SR_METHOD_NAMES = 
    {"getSlot", "getSrycd", "getName", "getTaniname", "getTen",
        "getSrysyukbn", "getHospsrykbn", "getNyugaitekkbn", "getRoutekkbn","getYukoedymdStr"};
    private static final int[] SR_COLUMN_WIDTH = {10, 50, 200, 10, 10, 10, 5, 5, 5, 10};
    private static final int SR_NUM_ROWS = 1;
//masuda$

    private InstractionView view;

    private ListTableModel<MasterItem> tableModel;

    private ListTableModel<TensuMaster> searchResultModel;

//masuda^
    private static final int ITEMNAME_COLUMN = 1;
    // 83系コメントコード関連
    private static final String[] commentItem = {
        "ＡＦＰ",
        "ＰＩＶＫＡ２精密",
        "ＣＥＡ精密",
        "ＣＡ１９－９精密",
        "エラスターゼ１精密",
        "ＰＳＡ精密",
        "ＳＣＣ抗原精密",
        "ＰｒｏＧＲＰ精密",
        "ＮＳＥ精密",
        "サイトケラチン１９フラグメント精密",
        "ＤＵＰＡＮ－２精密",
        "ＳＰａｎ－１抗原精密",
        "ジギタリス",
        "テオフィリン",
        "プロカインアミド",
        "Ｎ－アセチルプロカインアミド",
        "ジソピラミド",
        "キニジン",
        "アプリンジン",
        "リドカイン",
        "塩酸ピルジカイニド",
        "プロパフェノン",
        "メキシレチン",
        "フレカイニド",
        "コハク酸シベンゾリン",
        "ピルメノール",
        "アミオダロン"
    };
//masuda$
    
    public InstractionEditor() {
        initComponents();
    }

    public InstractionEditor(String entity) {
        this(entity, true);
    }

    public InstractionEditor(String entity, boolean mode) {
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
        return view;
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

        // 診療行為区分
        String c007 = null;
        // 診療行為があるかどうかのフラグ
        boolean found = false;
        
        for (MasterItem masterItem : itemList) {

            ClaimItem item = masterToClaimItem(masterItem);

            // 在宅料の場合は薬剤・材料・加算料（院内・院外)に応じてClassCodeを設定
            // 指導料でない場合が、在宅料
            if (!found 
                    && masterItem.getClaimClassCode() != null 
                    && !masterItem.getClaimClassCode().startsWith(ClaimConst.RECEIPT_CODE_SHIDOU_START)) {

                boolean bOut = view.getOutRadio().isSelected();
                int clsCode = masterItem.getClassCode();    //手技・材料・薬剤・用法・部位etc
                if (clsCode == ClaimConst.YAKUZAI) {
                    c007 = bOut
                            ? ClaimConst.RECEIPT_CODE_ZAITAKU_YAKUZAI_EXT
                            : ClaimConst.RECEIPT_CODE_ZAITAKU_YAKUZAI_IN;
                    found = true;
                } else if (clsCode == ClaimConst.ZAIRYO) {
                    c007 = bOut
                            ? ClaimConst.RECEIPT_CODE_ZAITAKU_ZAIRYO_EXT
                            : ClaimConst.RECEIPT_CODE_ZAITAKU_ZAIRYO_IN;
                    found = true;
                } else if (clsCode == ClaimConst.OTHER || clsCode == ClaimConst.SYUGI) {
                    if (ClaimConst.DATAKBN_KASAN_SHUGI.equals(masterItem.getDataKbn())) {
                        c007 = ClaimConst.RECEIPT_CODE_ZAITAKU_KASAN;
                        found = true;
                    } else {
                        c007 = ClaimConst.RECEIPT_CODE_ZAITAKU;
                        found = true;
                    }
                }
                if (c007 != null) {
                    view.getCommentField().setText(MMLTable.getClaimClassCodeName(c007));
                }
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

        // 保存時に在宅料のClassCodeを自動判定するためにここでMasterItemのclassCodeを更新しておく
        if (IInfoModel.ENTITY_INSTRACTION_CHARGE_ORDER.equals(entity)) {
            updateMasterItems(tableModel.getDataProvider());
        }

        // Stateを変更する
        checkValidation();
    }

    @Override
    protected void checkValidation() {
        
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                boolean setIsEmpty = (tableModel.getObjectCount() == 0);

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

                    } else {
                        other++;
                    }
                }

                // 何かあればOK
                setIsValid = setIsValid && (techCnt > 0 || other > 0);

                // チェックボックスの設定
                view.getTechChk().setSelected((techCnt > 0));
                
                // 通知する
                controlButtons(setIsEmpty, setIsValid);
            }
        });
    }

    @Override
    protected void addSelectedTensu(TensuMaster tm) {

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

        boolean pass = true;
        pass = pass && ipOk();

        int searchType = getSearchType(text, hitReturn);

        pass = pass && (searchType!=TT_INVALID);

        if (!pass) {
            return;
        }

        doSearch(text, searchType);
    }

    @Override
    protected final void initComponents() {

        // View
        view = new InstractionView();

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
//masuda^                
                if (col == 1) {
                    // 83系コメントコードの場合"："までは編集してはいけない
                    if (hasFixedName(mItem.getCode())) {
                        String oldName = mItem.getName();
                        int pos = oldName.indexOf("：");
                        if (pos != -1) {
                            String doNotModify = oldName.substring(0, pos + 1);
                            if (!value.startsWith(doNotModify)){
                                return;
                            }
                        }
                    }
                    mItem.setName(value);
                    return;
//masuda$
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
        //masuda^   診療内容カラムにpopupListenerを追加しておく
        PopupListener popupListener = new PopupListener(tf2);
        //masuda$
        
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

        // スタンプ名フィールド
        view.getStampNameField().addFocusListener(AutoKanjiListener.getInstance());
        
//        // コメントフィールド
//masuda^   ↓復活
        view.getCommentField().addFocusListener(AutoKanjiListener.getInstance());
//masuda$
        
        // 共通の設定
        setupOrderComponents();
        
        // SearchTextFieldにフォーカスをあてる
        setFocusOnSearchTextFld();
    }

//masuda^
    // コメントコード入力用のpopuplistener
    private class PopupListener extends MouseAdapter implements ActionListener {

        //private JTextField tf;
        private JPopupMenu popup;

        private PopupListener(JTextField tf) {
            //this.tf = tf;
            tf.addMouseListener(PopupListener.this);

            popup = new JPopupMenu();
            for (String itemName : commentItem) {
                JMenuItem mi = new JMenuItem(itemName);
                mi.addActionListener(PopupListener.this);
                popup.add(mi);
            }
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

            JTable setTable = view.getSetTable();
            if (e.isPopupTrigger()) {
                if (setTable.getSelectedColumn() != ITEMNAME_COLUMN) {
                    return;
                }
                int row = setTable.getSelectedRow();
                MasterItem mItem = tableModel.getObject(row);
                // 83系コメントのみポップアップする
                String code = mItem.getCode();
                if (code.startsWith("83")) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTable setTable = view.getSetTable();
            setTable.getColumnModel().getColumn(ITEMNAME_COLUMN).getCellEditor().stopCellEditing();
            String str = e.getActionCommand();
            int row = setTable.getSelectedRow();
            MasterItem mItem = tableModel.getObject(row);
            String oldName = mItem.getName();
            tableModel.setValueAt(oldName + str, row, ITEMNAME_COLUMN);
        }
    }
}
