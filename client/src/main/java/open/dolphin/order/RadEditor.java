package open.dolphin.order;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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
 * RadEditor
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class RadEditor extends AbstractStampEditor {
    
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

    private RadView view;

    private ListTableModel<MasterItem> tableModel;

    private ListTableModel<TensuMaster> searchResultModel;

    public RadEditor() {
        initComponents();
    }

    public RadEditor(String entity) {
        this(entity, true);
    }

    public RadEditor(String entity, boolean mode) {
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

        List<ClaimItem> tmpList = new ArrayList<ClaimItem>();

        for (MasterItem masterItem : itemList) {

            ClaimItem item = masterToClaimItem(masterItem);
            // 部位を最初のアイテムにする
            if (item.getCode().startsWith(ClaimConst.RBUI_CODE_START)) {
                tmpList.add(0, item);
            } else {
                tmpList.add(item);
            }
        }
        // BundleDolphinにClaimItemをセット
        bundle.setClaimItem(tmpList.toArray(new ClaimItem[0]));

        // 診療行為区分は".700"固定にする
        String c007 = ClaimConst.RECEIPT_CODE_RADIOOGY;
        // 700 画像診断
        bundle.setClassCode(c007);
        // Claim007 固定の値
        bundle.setClassCodeSystem(getClassCodeId());
        // 上記テーブルで定義されている診療行為の名称
        bundle.setClassName(MMLTable.getClaimClassCodeName(c007));
        
        // バンドル数を設定
        String bundleNum =  view.getNumberField().getText().trim();
        if (bundleNum.isEmpty()) {
            bundleNum = "1";
        }
        bundle.setBundleNumber(bundleNum);
        
         // バンドルメモ復活
        String memo = view.getCommentField().getText();
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
            view.getCommentField().setText(bundle.getMemo());
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

                int techCnt = 0;        // 診療行為
                int partCnt = 0;        // 部位
                int other = 0;

                List<MasterItem> itemList = tableModel.getDataProvider();

                for (MasterItem item : itemList) {

                    if (item.getCode().startsWith("002")) {
                        // 互換性
                        partCnt++;

                    } else if (item.getClassCode() == ClaimConst.SYUGI) {
                        techCnt++;

                    } else {
                        other++;
                    }
                }

                // 何かあればよい事にする（したいそうだ）
                setIsValid = setIsValid && (techCnt > 0 || partCnt > 0 || other > 0);

                // チェックボックスの設定        
                view.getTechCheck().setSelected((techCnt > 0));
                view.getPartCheck().setSelected((partCnt > 0));
                
                // 通知する
                controlButtons(setIsEmpty, setIsValid);
            }
        });
    }

    @Override
    protected void search(final String text, boolean hitReturn) {

        boolean pass = ipOk();

        int searchType = getSearchType(text, hitReturn);

        pass = pass && (searchType!=TT_INVALID);

        if (!pass) {
            return;
        }

        doSearch(text, searchType);
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

        // MasterItem に変換する
        MasterItem item = tensuToMasterItem(tm);

        // 診療行為をスタンプ名に設定する
        if (item.getClassCode() == ClaimConst.SYUGI) {
            String name = view.getStampNameField().getText().trim();
            if (name.equals("") || name.equals(DEFAULT_STAMP_NAME)) {
                view.getStampNameField().setText(item.getName());
            }
        }

        // テーブルへ追加する
        tableModel.addObject(item);

        // バリデーションを実行する
        checkValidation();
    }

    @Override
    protected final void initComponents() {

        // View
        //view = editorButtonTypeIsIcon() ? new RadView() : new RadViewText();
        view =  new RadView();

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

                // null ok
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

                // 数量を設定するのは勝手
                mItem.setNumber((String) o);
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
        
        //
        // 検索結果テーブルを生成する
        //
        searchResultModel = new ListTableModel<TensuMaster>(SR_COLUMN_NAMES, SR_NUM_ROWS, SR_METHOD_NAMES, null) {

            @Override
            public Object getValueAt(int row, int col) {

                Object ret = super.getValueAt(row, col);

                switch (col) {

                    case 6:
                        // 病診
                        //System.out.println((String) ret);
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

        // 部位検索ボタン
        view.getPartBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
//masuda^   部位の検索もsearchに行わせる
                doSearch(REGEXP_RAD_BUI, TT_CODE_SEARCH);
//masuda$
            }
        });
//masuda^   材料ボタン、うちは使わないけど
        view.getZairyoBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch(REGEXP_RAD_ZAIRYO, TT_CODE_SEARCH);
            }
        });
        // 手技ボタン、スタンプ作ったからもう使わないけど
        view.getShugiBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch(REGEXP_RAD_SHUGI, TT_CODE_SEARCH);
            }
        });
        // スタンプ名フィールド
        view.getStampNameField().addFocusListener(AutoKanjiListener.getInstance());
        
        // コメントフィールド
//masuda^   復活
        view.getCommentField().addFocusListener(AutoKanjiListener.getInstance());
        
        // 数量フィールドに期間入力ポップアップを付ける
        view.getNumberField().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                mabeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mabeShowPopup(e);
            }

            private void mabeShowPopup(MouseEvent e) {
                if (isAdmission() && e.isPopupTrigger()) {
                    PeriodSelectDialog dialog = new PeriodSelectDialog();
                    dialog.setLocationRelativeTo(view);
                    dialog.pack();
                    dialog.setVisible(true);
                    String value = dialog.getValue();
                    dialog.dispose();
                    if (value != null && !value.isEmpty()) {
                        view.getNumberField().setText(value);
                    }
                }
            }
        });
//masuda$
        
        // 共通の設定
        setupOrderComponents();
        
        // SearchTextFieldにフォーカスをあてる
        setFocusOnSearchTextFld();
    }

}
