package open.dolphin.order;

import java.awt.Toolkit;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.TableColumn;
import open.dolphin.client.*;
import open.dolphin.dao.SqlMasterDao;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import open.dolphin.util.CheckTonyo;
import open.dolphin.util.MMLDate;
import open.dolphin.util.ZenkakuUtils;

/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class RpEditor extends AbstractStampEditor {

    private static final String[] COLUMN_NAMES = 
    {"コード", "診療内容", "数量", "単位", " ", "日数/回数", "薬価"};
    private static final String[] METHOD_NAMES = 
    {"getCode", "getName", "getNumber", "getUnit", "getDummy", "getBundleNumber", "getYakka"};
    private static final int[] COLUMN_WIDTH = {30, 200, 5, 5, 5, 5, 50};
    private static final int ONEDAY_COLUMN = 2;
    private static final int BUNDLE_COLUMN = 5;

    private static final String[] SR_COLUMN_NAMES = 
    {"種別", "コード", "名 称", "単位", "点数", "薬価基準", "有効期限"};
    private static final String[] SR_METHOD_NAMES = 
    {"getSlot", "getSrycd", "getName", "getTaniname", "getTen","getYakkakjncd", "getYukoedymdStr"};
    private static final int[] SR_COLUMN_WIDTH = {10, 50, 200, 10, 10, 10, 10};
    private static final int SR_NUM_ROWS = 1;

    private static final String[] ADMIN_CODE_REGEXP = 
    {"","0010001","0010002","0010003","0010004",
        "(0010005|0010007)","0010006","0010008","0010009","001",
        ClaimConst.REGEXP_COMMENT_MED};
    private static final String[] ADMIN_CATEGORY =
    {"用法選択","内服１回等(100)","内服２回等(200)","内服３回等(300)","内服４回等(400)",
        "点眼等(500,700)","塗布等(600)","頓用等(800)","吸入等(900)","全て",
        "コメント"};

    private static final String IN_MEDICINE     = "院内処方";
    private static final String EXT_MEDICINE    = "院外処方";

    private RpView view;

    private ListTableModel<MasterItem> tableModel;

    private ListTableModel<TensuMaster> searchResultModel;

//masuda^
    private static final String NYUIN_MEDICINE  = "入院処方";
    private static final String NYUIN_MED_NC    = "入院調無";
    private static final String IN_MED_HOUKATSU = "院内包括";
    private static final String TEIKI = "定期";
    private static final String RINJI = "臨時";
    private static final String NYUIN = "入院";
    // 用法のキャッシュ
    private static HashMap<String, List<TensuMaster>> adminCache = new HashMap<String, List<TensuMaster>>();
//masuda$

    public RpEditor() {
        initComponents();
    }

    public RpEditor(String entity) {
        this(entity, true);
    }

    public RpEditor(String entity, boolean mode) {
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
        return (JPanel)view;
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

    private ModuleModel createModuleModel() {

        ModuleModel retModel = new ModuleModel();
        BundleMed med = new BundleMed();
        retModel.setModel(med);

        // StampInfoを設定する
        ModuleInfoBean moduleInfo = retModel.getModuleInfoBean();
        moduleInfo.setEntity(getEntity());
        moduleInfo.setStampRole(IInfoModel.ROLE_P);

        //　スタンプ名を設定する
        String stampName = view.getStampNameField().getText().trim();
        if (!stampName.equals("")) {
            moduleInfo.setStampName(stampName);
        } else {
            moduleInfo.setStampName(DEFAULT_STAMP_NAME);
        }

        return retModel;
    }

    @Override
    public IInfoModel[] getValue() {

        List<ModuleModel> retList = new ArrayList<ModuleModel>();
        List<MasterItem> items = tableModel.getDataProvider();

        ModuleModel mm = createModuleModel();
        BundleMed bundle = (BundleMed) mm.getModel();
        
        String ykzKbn = null;
        for (MasterItem mItem : items) {

             switch (mItem.getClassCode()) {
                case ClaimConst.YAKUZAI:
                     bundle.addClaimItem(masterToClaimItem(mItem));
                    // 剤型区分と院内／院外による診療種別区分を行う
                     if (bundle.getClassCode() == null) {
                         if (mItem.getYkzKbn() != null) {
                            // 剤型区分を保存
                            ykzKbn = mItem.getYkzKbn();
                        }
                    }
                    break;
                case ClaimConst.ADMIN:
                    String ommit = mItem.getName().replaceAll(REG_ADMIN_MARK, "");
                    bundle.setAdmin(ommit);
                    String adminCode = mItem.getCode();
                    bundle.setAdminCode(adminCode);

                    String bNum = trimToNullIfEmpty(mItem.getBundleNumber());
                    if (bNum != null) {
                        bNum = ZenkakuUtils.toHalfNumber(bNum);
                        bundle.setBundleNumber(bNum);
                    }

                    // メモ　院内処方、院外処方など
                    String memo = getMemo();
                    bundle.setMemo(memo);

                    // classCode
                    String rCode = getClassCode(ykzKbn, adminCode);
                    bundle.setClassCode(rCode);
                    
                    if (bundle.getClassCode() == null) {
                        // 保険適用外の医薬品と用法でOKとするため
                        bundle.setClassCode(ClaimConst.RECEIPT_CODE_NAIYO);
                    }
                    
                    bundle.setClassCodeSystem(ClaimConst.CLASS_CODE_ID);
                    bundle.setClassName(MMLTable.getClaimClassCodeName(bundle.getClassCode()));
                    retList.add(mm);

                    mm = createModuleModel();
                    bundle = (BundleMed) mm.getModel();
                    break;
                default:
                    bundle.addClaimItem(masterToClaimItem(mItem));
                    break;
            }
        }

        int i = 1;  //処方番号カウンタ
        for (ModuleModel stamp : retList){
            // スタンプ名に連番を打つ
            String stampName = stamp.getModuleInfoBean().getStampName();
            if (TEIKI.equals(stampName)) {
                stamp.getModuleInfoBean().setStampName(TEIKI + " - " + intToZenkaku(i));
            } else if (RINJI.equals(stampName)) {
                stamp.getModuleInfoBean().setStampName(RINJI + " - " + intToZenkaku(i));
            } else if (NYUIN.equals(stampName)) {
                stamp.getModuleInfoBean().setStampName(NYUIN + " - " + intToZenkaku(i));
            }
            ++i;
        }

        return retList.toArray(new ModuleModel[0]);
    }
    
    // バンドルメモを作成する
    private String getMemo() {

        boolean inMed = view.getInRadio().isSelected();
        boolean nyuin = view.getRbAdmission().isSelected();
        boolean houkatsu = view.getInRadio().isSelected() && view.getCbHoukatsu().isSelected();
        boolean noCharge = view.getCbNoCharge().isSelected();
        
        String memo;
        
        if (nyuin) {
            if (noCharge) {
                memo = NYUIN_MED_NC;    // 入院調無
            } else {
                memo = NYUIN_MEDICINE;  // 入院処方
            }
        } else {
            if (houkatsu) {
                memo = IN_MED_HOUKATSU; // 院内包括
            } else {
                memo = inMed ? IN_MEDICINE : EXT_MEDICINE;  // 院内処方・院外処方
            }
        }
        return memo;
    }
    
    // ClassCodeを作成する
    private String getClassCode(String ykzKbn, String adminCode) {
        
        boolean inMed = view.getInRadio().isSelected();
        boolean rinji = view.getRbRinji().isSelected();
        boolean nyuin = view.getRbAdmission().isSelected();
        boolean houkatsu = view.getInRadio().isSelected() && view.getCbHoukatsu().isSelected();
        boolean noCharge = view.getCbNoCharge().isSelected();
        boolean tonyo = CheckTonyo.isTonyo(adminCode);
        
        String rCode = null;
        
        if (ykzKbn.equals(ClaimConst.YKZ_KBN_NAIYO)) {
            if (tonyo) {
                // 頓用
                if (nyuin) {
                    if (noCharge) {
                        rCode = ClaimConst.RECEIPT_CODE_TONYO_NYUIN_NC;
                    } else {
                        rCode = ClaimConst.RECEIPT_CODE_TONYO;
                    }
                } else {
                    if (houkatsu) {
                        rCode = ClaimConst.RECEIPT_CODE_TONYO_HOKATSU;
                    } else {
                        rCode = inMed ? ClaimConst.RECEIPT_CODE_TONYO_IN : ClaimConst.RECEIPT_CODE_TONYO_EXT;
                    }
                }
            } else if (rinji) {
                // 臨時
                if (nyuin) {
                    rCode = ClaimConst.RECEIPT_CODE_RINJI;
                } else {
                    if (houkatsu) {
                        rCode = ClaimConst.RECEIPT_CODE_RINJI;
                    } else {
                        rCode = inMed ? ClaimConst.RECEIPT_CODE_RINJI_IN : ClaimConst.RECEIPT_CODE_RINJI_EXT;
                    }
                }
            } else {
                // 内服
                if (nyuin) {
                    if (noCharge) {
                        rCode = ClaimConst.RECEIPT_CODE_NAIYO_NYUIN_NC;
                    } else {
                        rCode = ClaimConst.RECEIPT_CODE_NAIYO;
                    }
                } else {
                    if (houkatsu) {
                        rCode = ClaimConst.RECEIPT_CODE_NAIYO_HOKATSU;
                    } else {
                        rCode = inMed ? ClaimConst.RECEIPT_CODE_NAIYO_IN : ClaimConst.RECEIPT_CODE_NAIYO_EXT;
                    }
                }
            }

        } else if (ykzKbn.equals(ClaimConst.YKZ_KBN_GAIYO)) {
            // 外用
            if (nyuin) {
                if (noCharge) {
                    rCode = ClaimConst.RECEIPT_CODE_GAIYO_NYUIN_NC;
                } else {
                    rCode = ClaimConst.RECEIPT_CODE_GAIYO;
                }
            } else {
                if (houkatsu) {
                    rCode = ClaimConst.RECEIPT_CODE_GAIYO_HOKATSU;
                } else {
                    rCode = inMed ? ClaimConst.RECEIPT_CODE_GAIYO_IN : ClaimConst.RECEIPT_CODE_GAIYO_EXT;
                }
            }
        }
        return rCode;
    }
   
    @Override
    public void setValue(IInfoModel[] value) {

        // 連続して編集される場合があるのでテーブル内容等をクリアする
        clear();
        if (value == null) {
            return;
        }
        setOldValue(value);
        ModuleModel[] stamps = (ModuleModel[]) value;
        // null であればリターンする
        if (stamps == null || stamps.length == 0) {
            return;
        }

        // 院外処方かどうかのflag
        boolean bOut = Project.getBoolean(Project.RP_OUT, true);

        // 最初のスタンプからEntityを保存する
        setEntity(stamps[0].getModuleInfoBean().getEntity());
        // 最初のスタンプからスタンプ名を引き継ぐ
        String stampName = stamps[0].getModuleInfoBean().getStampName();
        boolean serialized = stamps[0].getModuleInfoBean().isSerialized();
        if (!serialized && stampName.startsWith(FROM_EDITOR_STAMP_NAME)) {
            stampName = DEFAULT_STAMP_NAME;
        } else if (stampName.equals("")) {
            stampName = DEFAULT_STAMP_NAME;
        }
        view.getStampNameField().setText(stampName);

        BundleMed med = (BundleMed) stamps[0].getModel();
        if (med == null) {
            return;
        }

        // Memo
        String memo = med.getMemo();
        if (EXT_MEDICINE.equals(memo)) {
            bOut= true;
        }
        view.getOutRadio().setSelected(bOut);

        // 最初のスタンプがclassCodeが290（臨時）か、スタンプ名に臨時を含んでいたら臨時ボタンをセットする
        if (med.getClassCode().startsWith(ClaimConst.RECEIPT_CODE_RINJI.substring(0, 2)) || stampName.contains(RINJI)) {
            view.getRbRinji().setSelected(true);
        } else {
            view.getRbTeiki().setSelected(true);
        }
        // 最初のスタンプが包括ならば包括チェックボックスを設定する
        if (med.getClassCode().endsWith("3")) {
            view.getCbHoukatsu().setSelected(true);
        }
        // 最初のスタンプが入院ならば入院ラジオをセットする
        if (NYUIN_MEDICINE.equals(med.getMemo())) {
            view.getRbAdmission().setSelected(true);
        } else if (NYUIN_MED_NC.equals(med.getMemo())) {
            view.getRbAdmission().setSelected(true);
            view.getCbNoCharge().setSelected(true);
        }

        for (ModuleModel mm : stamps) {
            med = (BundleMed) mm.getModel();
            // ClaimItemをMasterItemへ変換してテーブルへ追加する
            ClaimItem[] items = med.getClaimItem();
            for (ClaimItem item : items) {
                MasterItem mi = claimToMasterItem(item);
                // classCodeに応じてMasterItemに薬剤区分を記録。あとでclaimClassCodeをきめるのに使用する
                // これも毎回ORCAに問い合わせてもいいのだが
                String ykzKbn = null;
                if (med.getClassCode() != null) {
                    String cCode = med.getClassCode();
                    if (cCode.startsWith(ClaimConst.RECEIPT_CODE_GAIYO.substring(0, 2))) {       //外用
                        ykzKbn = ClaimConst.YKZ_KBN_GAIYO;
                    } else { //内服・臨時・頓服
                        ykzKbn = ClaimConst.YKZ_KBN_NAIYO;
                    }
                }
                mi.setYkzKbn(ykzKbn);
                tableModel.addObject(mi);
            }

            // Save Administration
            if (med.getAdmin() != null) {
                MasterItem item = new MasterItem();
                item.setClassCode(ClaimConst.ADMIN);
                item.setCode(med.getAdminCode());
                item.setName(ADMIN_MARK + med.getAdmin());
                item.setDummy("X");
                String bNumber = med.getBundleNumber();
                bNumber = ZenkakuUtils.toHalfNumber(bNumber);
                item.setBundleNumber(bNumber);
                tableModel.addObject(item);
            }
        }
        checkValidation();
    }

    @Override
    protected void search(final String text, boolean hitReturn) {

        boolean pass = ipOk();

        int searchType = getSearchType(text, hitReturn);

        pass = pass && (searchType!=TT_INVALID);
        pass = pass && (searchType!=TT_LIST_TECH);

        if (!pass) {
            return;
        }

        doSearch(text, searchType);
    }

    private void getUsage(final String regExp) {

        if (!ipOk()) {
            return;
        }

        // 件数をゼロにしておく
        view.getCountField().setText("0");

        SwingWorker worker = new SwingWorker<List<TensuMaster>, Void>() {

            @Override
            protected List<TensuMaster> doInBackground() throws Exception {
//masuda^   用法はまずキャッシュを参照
                List<TensuMaster> cache = adminCache.get(regExp);
                if (cache != null){
                    return cache;
                }
                SqlMasterDao dao = SqlMasterDao.getInstance();
//masuda$
                String d = effectiveFormat.format(new Date());
                
                List<TensuMaster> result = dao.getTensuMasterByCode(regExp, d);

                if (!dao.isNoError()) {
                    throw new Exception(dao.getErrorMessage());
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    List<TensuMaster> result = get();
                    searchResultModel.setDataProvider(result);
                    int cnt = searchResultModel.getObjectCount();
                    view.getCountField().setText(String.valueOf(cnt));
//masuda^
                    showFirstResult(view.getSearchResultTable());
                    adminCache.put(regExp, new ArrayList<TensuMaster>(result));
//masuda$
                } catch (InterruptedException ex) {

                } catch (ExecutionException ex) {
                    alertSearchError(ex.getMessage());
                }
            }
        };

        worker.execute();
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

                // 薬剤またはその他と用法があること
                int medCnt = 0;
                int useCnt = 0;
                int other = 0;

                List<MasterItem> itemList = tableModel.getDataProvider();

                for (MasterItem item : itemList) {

                    if (item.getClassCode() == ClaimConst.YAKUZAI) {
                        medCnt++;

                    } else if (item.getClassCode() == ClaimConst.ADMIN) {
                        useCnt++;

                    } else {
                        // 2010-03-09
                        // 保険適用外医薬品等を許可する
                        // ただし何かは不明
                        other++;
                    }
                }

                setIsValid = setIsValid && (medCnt > 0 || other > 0);
                // 複数剤編集のため
                // 項目の並びが正しいか？
                int num = itemList.size();
                boolean flag = false;
                for (int i = 0; i < num; ++i) {
                    int code = itemList.get(i).getClassCode();
                    if (code == ClaimConst.YAKUZAI || code == ClaimConst.OTHER) {
                        flag = true;
                        setIsValid = false;
                    } else if (code == ClaimConst.ADMIN && flag) {
                        flag = false;
                        setIsValid = true;
                    } else {
                        setIsValid = false;
                        break;
                    }
                }
                // チェックボックスの設定
                view.getMedicineCheck().setSelected((medCnt > 0));
                view.getUsageCheck().setSelected((useCnt == 1));
                
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

        // MasterItem に変換する
        MasterItem item = tensuToMasterItem(tm);
        
//masuda^   採用薬の通常用量をセットする
/*
        // item が用法であった場合、テーブルのアイテムをスキャンし、外用薬があった場合は数量を1にする
        if (item.getClassCode()==ClaimConst.ADMIN) {
            List<MasterItem> list = tableModel.getDataProvider();
            if (list!=null) {
                for (MasterItem mi : list) {
                    if (mi.getYkzKbn()!=null && mi.getYkzKbn().equals(ClaimConst.YKZ_KBN_GAIYO)) {
                        item.setBundleNumber("1");
                        break;
                    }
                }
            }
        }
*/
        // 薬剤と用法以外ならスキップする
        if (!tm.getSrycd().matches(ClaimConst.REGEXP_COMMENT_MED)) {
            String inputNum = null;
            String maxDose = null;
            String admin = null;
            String unit = null;

            boolean showUsage = false;
            if (item.getClassCode() == ClaimConst.ADMIN) {
                inputNum = item.getBundleNumber();
            } else {
                inputNum = item.getNumber();
                UsingDrugModel udm = UsingDrugs.getInstance().getUsingDrugModel(item.getCode());
                if (udm != null) {
                    String usualDose = udm.getUsualDose();
                    if (usualDose != null && !"".equals(usualDose.trim())){
                      inputNum = usualDose;
                    }
                    maxDose = udm.getMaxDose();
                    admin = udm.getAdmin();
                    unit = item.getUnit();
                    showUsage = true;
                }
            }

            // スクリーンテンキーのダイアログを表示する
            String num = inputFromSTK(showUsage, inputNum, maxDose, admin, unit);
            // "x"で閉じたらnullがかえってくる。中止する
            if (num == null) {
                view.getSearchResultTable().clearSelection();
                return;
            }

            // 単位がｇで１０ｇ未満なら小数点第一位まで
            if ("ｇ".equals(item.getUnit())) {
                Float weight = Float.valueOf(num);
                DecimalFormat decimalFormat = new DecimalFormat("#");
                if (weight < 10) {
                    decimalFormat = new DecimalFormat("#.0##");
                }
                num = decimalFormat.format(weight);
            }
            if (item.getClassCode() == ClaimConst.YAKUZAI) {
                item.setNumber(num);
            } else if (item.getClassCode() == ClaimConst.ADMIN) {
                item.setBundleNumber(num);
            }
        }
//masuda$
        
        // 医薬品名をスタンプ名の候補にする
        String name = view.getStampNameField().getText().trim();
        if (name.equals("") || name.equals(DEFAULT_STAMP_NAME)) {
            view.getStampNameField().setText(item.getName());
        }

        // テーブルへ追加する
        tableModel.addObject(item);

        // バリデーションを実行する
        checkValidation();
        
//masuda^   挿入後は用法コンボをリセットする
        view.getUsageCombo().setSelectedIndex(0);
//masuda$
    }

    @Override
    protected final void initComponents() {

        // View
        //view = editorButtonTypeIsIcon() ? new RpView() : new RpViewText();
        view = new RpView();

        // Info Label
        view.getInfoLabel().setText(this.getInfo());

        // セットテーブルを生成する
        tableModel = new ListTableModel<MasterItem>(COLUMN_NAMES, START_NUM_ROWS, METHOD_NAMES, null) {

            // 数量と回数のみ編集可能
            @Override
            public boolean isCellEditable(int row, int col) {
                
                // 元町皮膚科
                if (col == 1) {
                    String code = (String) this.getValueAt(row, 0);
                    return isNameEditableComment(code);
                }

                // 用法
                if (col == BUNDLE_COLUMN) {
                    String code = (String) this.getValueAt(row, 0);
                    return (code != null && code.startsWith(ClaimConst.ADMIN_CODE_START));
                }

                // 数量
                if (col == ONEDAY_COLUMN) {
                    String code = (String) this.getValueAt(row, 0);
                    return isEditableNumber(code);
                }

                return false;
            }

            @Override
            public void setValueAt(Object o, int row, int col) {

                MasterItem mItem = getObject(row);

                if (mItem == null) {
                    return;
                }

                String value = (String) o;
                if (value != null) {
                    value = value.trim();
                }

                // コメント編集 元町皮膚科
                if (col == 1 && isNameEditableComment(mItem.getCode())) {
                    mItem.setName(value);
                    return;
                }

                // null
                if (value == null || value.equals("")) {
                    boolean test = 
                            (col == ONEDAY_COLUMN
                            && (mItem.getClassCode() == ClaimConst.SYUGI || mItem.getClassCode() == ClaimConst.OTHER));
                    if (test) {
                        mItem.setNumber(null);
                        mItem.setUnit(null);
                    }
                    checkValidation();
                    return;
                }

                if (col == ONEDAY_COLUMN && mItem.getClassCode()!=ClaimConst.ADMIN) {
                    mItem.setNumber(value);
                    checkValidation();
                    return;
                }

                if (col == BUNDLE_COLUMN && mItem.getClassCode()==ClaimConst.ADMIN) {
                    mItem.setBundleNumber(value);
                    checkValidation();
                }
            }
        };
        
        JTable setTable = view.getSetTable();
        setTable.setModel(tableModel);
//masuda^
        setTable.addMouseMotionListener(new SetTableMouseMotionListener());
        // 中止項目登録のためsetTableにPopupListener2を設定する
        PopupListener2 popupListener2 = new PopupListener2(setTable);
//masuda$
        // 数量カラムにセルエディタを設定する
        JTextField tf = new JTextField();
        tf.addFocusListener(AutoRomanListener.getInstance());
        TableColumn column = setTable.getColumnModel().getColumn(ONEDAY_COLUMN);
        DefaultCellEditor de = new DefaultCellEditor2(tf);
        int ccts = Project.getInt("order.table.clickCountToStart", 1);
        de.setClickCountToStart(ccts);
        column.setCellEditor(de);
//masuda  ScreenTenKeyをpopupするため。
        PopupListener popupListener = new PopupListener(tf);
        
        // 診療内容カラム(column number = 1)にセルエディタを設定する 元町皮膚科
        JTextField tf2 = new JTextField();
        tf2.addFocusListener(AutoKanjiListener.getInstance());
        column = setTable.getColumnModel().getColumn(1);
        DefaultCellEditor de2 = new DefaultCellEditor2(tf2);
        de2.setClickCountToStart(ccts);
        column.setCellEditor(de2);

        // 日数回数カラム
        JTextField tf3 = new JTextField();
        tf3.addFocusListener(AutoRomanListener.getInstance());
        column = setTable.getColumnModel().getColumn(BUNDLE_COLUMN);
        DefaultCellEditor de3 = new DefaultCellEditor2(tf3);
        de3.setClickCountToStart(ccts);
        column.setCellEditor(de3);
//masuda  ScreenTenKeyをpopupするため。
        PopupListener popupListener1 = new PopupListener(tf3);
        
        //
        // 検索結果テーブルを生成する
        //
        JTable searchResultTable = view.getSearchResultTable();
        searchResultModel = new ListTableModel<TensuMaster>(SR_COLUMN_NAMES, SR_NUM_ROWS, SR_METHOD_NAMES, null);
        searchResultTable.setModel(searchResultModel);

        // 用法検索
        JComboBox usage = view.getUsageCombo();
//masuda^
        usage.removeAllItems();
        int s = ADMIN_CATEGORY.length;
        for (int i = 0; i < s; ++i) {
            usage.addItem(ADMIN_CATEGORY[i]);
        }
//masuda$
        
        usage.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JComboBox cb = (JComboBox)ae.getSource();
                int index = cb.getSelectedIndex();
                String regExp = ADMIN_CODE_REGEXP[index];
                if (!regExp.equals("")) {
                    getUsage(regExp);
                }
            }
        });
        
        // スタンプ名フィールド
        view.getStampNameField().addFocusListener(AutoKanjiListener.getInstance());

        // 定期・臨時・入院ボタン
        final JRadioButton rb_teiki = view.getRbTeiki();
        final JRadioButton rb_rinji = view.getRbRinji();
        final JRadioButton rb_nyuin = view.getRbAdmission();
        
        ActionListener al1 = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String text = view.getStampNameField().getText();
                if (rb_teiki.isSelected() && !text.startsWith(TEIKI)) {
                    view.getStampNameField().setText(TEIKI);
                } else if (rb_rinji.isSelected() && !text.startsWith(RINJI)) {
                    view.getStampNameField().setText(RINJI);
                } else if (rb_nyuin.isSelected() && !text.startsWith(NYUIN)) {
                    view.getStampNameField().setText(NYUIN);
                }
            }
        };
        rb_teiki.addActionListener(al1);
        rb_rinji.addActionListener(al1);
        rb_nyuin.addActionListener(al1);

        // 院内、院外ボタン
        JRadioButton inBtn = view.getInRadio();
        JRadioButton outBtn = view.getOutRadio();
        ButtonGroup g = new ButtonGroup();
        g.add(inBtn);
        g.add(outBtn);
        
        // 包括チェックボックス
        view.getInRadio().addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean b = view.getInRadio().isSelected();
                view.getCbHoukatsu().setEnabled(b);
                if (!b) {
                    view.getCbHoukatsu().setSelected(false);
                }
            }
        });
        
        // 薬価計算ボタン
        view.getBtnYakka().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // SHIFTが押されていたらORCA参照
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) {
                    if (getFromStampEditor() && getContext().getPatient().getPatientId() != null){
                        referOrca();
                    }
                } else {
                    // そうでなかったら薬価確認
                    calcYakka();
                }
            }
        });

        // 共通の設定
        setupOrderComponents();
        
        // SearchTextFieldにフォーカスをあてる
        setFocusOnSearchTextFld();
//masuda$
    }
    
//masuda^    
    @Override
    public void setContext(Chart chart) {
        super.setContext(chart);
        controlBtn();
    }
    
    // 外来か入院かに応じてボタンを制御する
    private void controlBtn() {
        
        JRadioButton inBtn = view.getInRadio();
        JRadioButton outBtn = view.getOutRadio();
        boolean bOut = Project.getBoolean(Project.RP_OUT, true);
        
        // KarteEditorを取得する
        KarteEditor editor = getContext().getKarteEditor();
        if (editor == null) {
            outBtn.setSelected(bOut);
            inBtn.setSelected(!bOut);
            return;
        }

        JRadioButton rb_teiki = view.getRbTeiki();
        JRadioButton rb_rinji = view.getRbRinji();
        JRadioButton rb_nyuin = view.getRbAdmission();
        JCheckBox cbHoukatsu = view.getCbHoukatsu();
        JCheckBox cbNoCharge = view.getCbNoCharge();
        
        // KarteEditorのDocumentModel.docInfoからAdmissionModelを取得する
        AdmissionModel admission = editor.getModel().getDocInfoModel().getAdmissionModel();
        
        if (admission != null) {
            bOut = false;
            rb_nyuin.setSelected(true);
            rb_rinji.setSelected(false);
            rb_rinji.setEnabled(false);
            cbHoukatsu.setSelected(false);
            cbHoukatsu.setEnabled(false);
        } else {
            rb_teiki.setSelected(true);
            rb_nyuin.setEnabled(false);
            cbNoCharge.setSelected(false);
            cbNoCharge.setEnabled(false);
        }
        outBtn.setSelected(bOut);
        inBtn.setSelected(!bOut);
    }

    // マスターで薬剤を選択したらScreenTenKeyから必ず入力させる
    private String inputFromSTK(boolean showInfo, String usualDose, String maxDose, String admin, String unit){

        if ("カプセル".equals(unit)) {
            unit = "Ｃ";
        }
        ScreenTenKey stk = new ScreenTenKey();
        stk.setInput(usualDose);
        if (showInfo) {
            StringBuilder sb = new StringBuilder();
            sb.append("用量：");
            sb.append(usualDose);
            if (maxDose != null) {
                sb.append("-");
                sb.append(maxDose);
            }
            if (unit != null) {
                sb.append(unit);
            }
            if (admin != null) {
                sb.append("／分");
                sb.append(admin);
            }
            stk.setMemo(sb.toString());
        }
        String out = stk.enterDialog();
        stk = null;
        return out;
    }

    // ScreenTenKeyのポップアップ
    private class PopupListener extends MouseAdapter implements PropertyChangeListener {

        private JTextField tf;
        private JPopupMenu popup;
        private ScreenTenKey stk;

        private PopupListener(JTextField tf) {
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
                stk = new ScreenTenKey();
                JTable setTable = view.getSetTable();
                int col = setTable.getSelectedColumn();
                int row = setTable.getSelectedRow();
                if (col!=ONEDAY_COLUMN && col!=BUNDLE_COLUMN){
                    return;
                }

                stk.setInput((String) setTable.getValueAt(row, col));
                popup.insert(stk.getPopupPanel(), 0);
                stk.addPropertyChangeListener(this);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            // ScreenTenKeyで入力されるとpropertyChangeイベントが起こる masuda
            String str = (String) e.getNewValue();
            tf.setText(str);
            JTable setTable = view.getSetTable();
            setTable.getColumnModel().getColumn(ONEDAY_COLUMN).getCellEditor().stopCellEditing();
            setTable.getColumnModel().getColumn(BUNDLE_COLUMN).getCellEditor().stopCellEditing();
            stk.removePropertyChangeListener(this);
            stk = null;
            popup.setVisible(false);
            popup = null;
        }
    }
    private class PopupListener2 extends MouseAdapter{

        private JTable table;

        private PopupListener2(JTable table) {
            this.table = table;
            table.addMouseListener(PopupListener2.this);
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

                int row = table.getSelectedRow();

                if (row < tableModel.getRowCount() && row >= 0) {

                    final MasterItem mi = tableModel.getObject(row);
                    // 薬剤以外ならpopupしない
                    if (mi.getClassCode() != ClaimConst.YAKUZAI) {
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("「");
                    sb.append(mi.getName());
                    sb.append("」を中止項目に追加");
                    JMenuItem item = new JMenuItem(sb.toString());
                    JPopupMenu pop = new JPopupMenu();
                    pop.add(item);

                    item.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            SwingWorker worker = new SwingWorker<Void, Void>() {

                                @Override
                                protected Void doInBackground() throws Exception {
                                    DisconItemModel model = new DisconItemModel();
                                    model.setItemName(mi.getName());
                                    model.setDate(MMLDate.getDate());
                                    DisconItems.getInstance().addDisconItems(Collections.singletonList(model));
                                    return null;
                                }
                            };
                            worker.execute();
                        }
                    });
                    pop.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }

    // MedicineTableの薬価を計算する
    private void calcYakka() {

        final List<MasterItem> items = tableModel.getDataProvider();

        final SqlMiscDao dao2 = SqlMiscDao.getInstance();

        final SwingWorker worker = new SwingWorker<List<TensuMaster>, Void>() {

            @Override
            protected List<TensuMaster> doInBackground() throws Exception {

                List<String> srycdList = new ArrayList<String>();
                for (MasterItem mi : items) {
                    srycdList.add(mi.getCode());
                }

                List<TensuMaster> result = dao2.getTensuMasterList(srycdList);
                if (!dao2.isNoError()) {
                    throw new Exception(dao2.getErrorMessage());
                }
                return result;
            }
            @Override
            protected void done() {
                try {
                    List<TensuMaster> list = get();
                    HashMap<Integer, Float> result = new HashMap<Integer, Float>();
                    for (TensuMaster tm : list) {
                        result.put(Integer.valueOf(tm.getSrycd()), Float.valueOf(tm.getTen()));
                    }
                    float totalCost = 0;
                    for (MasterItem mItem : items) {
                        int classCode = mItem.getClassCode();
                        if (classCode == ClaimConst.YAKUZAI && mItem.getNumber() != null) {
                            float yakka = result.get(Integer.valueOf(mItem.getCode()));
                            float num = Float.valueOf(mItem.getNumber());
                            mItem.setYakka(yakka);
                            totalCost += yakka * num;
                        } else if (classCode == ClaimConst.ADMIN) {
                            mItem.setYakka(totalCost);
                            totalCost = 0;
                        }
                    }
                    tableModel.fireTableDataChanged();
                } catch (Exception ex) {
                }
            }
        };
        worker.execute();
    }

    private String intToZenkaku(int num){
        String[] NumZenkaku = {"０","１","２","３","４","５","６","７","８","９"};
        StringBuilder sb = new StringBuilder();
        String str = String.valueOf(num);
        for (int i = 0; i < str.length(); ++i){
            int n = Integer.valueOf(str.substring(i, i + 1));
            sb.append(NumZenkaku[n]);
        }
        return sb.toString();
    }

    // ORCA処方参照
    private void referOrca() {
        String patientId = getContext().getPatient().getPatientId();
        if (patientId == null) {
            return;
        }
        ImportOrcaMedicinePanel panel = new ImportOrcaMedicinePanel();
        panel.enter(patientId);

        if (panel.getImportFlag()) {
            // テーブルにMasterItemを追加
            List<MasterItem> miList = panel.getMasterItemList();
            tableModel.addAll(miList);
            // スタンプ名の設定
            String name = view.getStampNameField().getText().trim();
            if (name.equals("") || name.equals(DEFAULT_STAMP_NAME)) {
                // スタンプ名はボタンに応じて定期・臨時にしておく
                if (view.getRbTeiki().isSelected()) {
                    view.getStampNameField().setText(TEIKI);
                } else {
                    view.getStampNameField().setText(RINJI);
                }
            }
            checkValidation();
        }
        panel = null;
    }
//masuda$
}
