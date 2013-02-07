package open.dolphin.order;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import open.dolphin.client.*;
import open.dolphin.dao.SqlMasterDao;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;
import open.dolphin.tr.MasterItemTransferHandler;
import open.dolphin.util.StringTool;
import open.dolphin.util.ZenkakuUtils;

/**
 *　AbstractStampEditor
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public abstract class AbstractStampEditor extends StampEditorConst {
    
    // ドルフィンのオーダ履歴用の名前
    protected String orderName;

    // ClaimBundle に設定する 診療行為区分 400,500,600 .. etc
    protected String classCode;

    // 診療行為区分定義のテーブルID == Claim007
    protected String classCodeId    = CLAIM_007;

    // ClaimItem (項目) の種別を定義しているテーブルID = Claim003
    protected String subclassCodeId = CLAIM_003;

    // このエディタのエンティティ
    protected String entity;
    
    // このエディタで組合わせが可能な点数マスタ項目の正規表現
    protected Pattern passPattern;

    // このエディタの診区正規表現パターン
    protected String shinkuRegExp;
    protected Pattern shinkuPattern;

    // このエディタの情報
    private String info;

    protected String implied007;

    protected JTextField searchTextField;

    protected JTextField countField;

    // 通知用の束縛サポート
    protected PropertyChangeSupport boundSupport;

    // StampEditor から起動された時 true
    // StampMaker から起動された時は false
    private boolean fromStampEditor;
    
    private boolean isAdmission;
    
    // 抽象メソッド
    public abstract JPanel getView();

    protected abstract void search(final String text, boolean hitRet);

    protected abstract void initComponents();
    
    protected abstract void checkValidation();

    /**
     * マスター検索で選択された点数オブジェクトをセットテーブルへ追加する。
     * @param tm 点数マスタ
     */
    protected abstract void addSelectedTensu(TensuMaster tm);
    
    
    /**
     * Entity からマスタ検索に必要な正規表現を生成する。
     * @param entity エンティティ
     * @return 正規表現を格納した Hashtable
     */
    private HashMap<String, String> getEditorSpec(String entity) {

        HashMap<String, String> ht = new HashMap<String, String>(10, 0.75f);

        String passRegExp = null;

        if (entity.equals(IInfoModel.ENTITY_BASE_CHARGE_ORDER)) {

            orderName = NAME_BASE_CHARGE;
            passRegExp = REG_BASE_CHARGE;
            shinkuRegExp = SHIN_BASE_CHARGE;
            info = INFO_BASE_CHARGE;


        } else if (entity.equals(IInfoModel.ENTITY_INSTRACTION_CHARGE_ORDER)) {

            orderName = NAME_INSTRACTION_CHARGE;
            passRegExp = REG_INSTRACTION_CHARGE;
            shinkuRegExp = SHIN_INSTRACTION_CHARGE;
            info = INFO_INSTRACTION_CHARGE;


        } else if (entity.equals(IInfoModel.ENTITY_MED_ORDER)) {

            orderName = NAME_MED_ORDER;
            passRegExp = REG_MED_ORDER;                     // 薬剤、用法、材料、その他(保険適用外医薬品）
            info = INFO_MED_ORDER;

        } else if (entity.equals(IInfoModel.ENTITY_INJECTION_ORDER)) {

            orderName = NAME_INJECTION_ORDER;
            passRegExp = REG_INJECTION_ORDER;               // 手技、その他、注射薬、材料
            shinkuRegExp = SHIN_INJECTION_ORDER;
            info = INFO_INJECTION_ORDER;


        } else if (entity.equals(IInfoModel.ENTITY_TREATMENT)) {

            orderName = NAME_TREATMENT;
            passRegExp = REG_TREATMENT;                     // 手技、その他、薬剤、材料
            shinkuRegExp = SHIN_TREATMENT;
            implied007 = IMPLIED_TREATMENT;
            info = INFO_TREATMENT;


        } else if (entity.equals(IInfoModel.ENTITY_SURGERY_ORDER)) {

            orderName = NAME_SURGERY_ORDER;
            passRegExp = REG_SURGERY_ORDER;                 // 手技、その他、薬剤、材料
            shinkuRegExp = SHIN_SURGERY_ORDER;
            info = INFO_SURGERY_ORDER;


        } else if (entity.equals(IInfoModel.ENTITY_BACTERIA_ORDER)) {

            orderName = NAME_BACTERIA_ORDER;
            passRegExp = REG_BACTERIA_ORDER;                // 手技、その他、薬剤、材料
            shinkuRegExp = SHIN_BACTERIA_ORDER;
            implied007 = IMPLIED_BACTERIA_ORDER;
            info = INFO_BACTERIA_ORDER;

        } else if (entity.equals(IInfoModel.ENTITY_PHYSIOLOGY_ORDER)) {

            orderName = NAME_PHYSIOLOGY_ORDER;
            passRegExp = REG_PHYSIOLOGY_ORDER;              // 手技、その他、薬剤、材料
            shinkuRegExp = SHIN_PHYSIOLOGY_ORDER;
            implied007 = IMPLIED_PHYSIOLOGY_ORDER;
            info = INFO_PHYSIOLOGY_ORDER;


        } else if (entity.equals(IInfoModel.ENTITY_LABO_TEST)) {

            orderName = NAME_LABO_TEST;
            passRegExp = REG_LABO_TEST;                     // 手技、その他、薬剤、材料
            shinkuRegExp = SHIN_LABO_TEST;
            implied007 = IMPLIED_LABO_TEST;
            info = INFO_LABO_TEST;


        } else if (entity.equals(IInfoModel.ENTITY_RADIOLOGY_ORDER)) {

            orderName = NAME_RADIOLOGY_ORDER;
            passRegExp = REG_RADIOLOGY_ORDER;               // 手技、その他、薬剤、材料、部位
            shinkuRegExp = SHIN_RADIOLOGY_ORDER;
            implied007 = IMPLIED_RADIOLOGY_ORDER;
            info = INFO_RADIOLOGY_ORDER;


        }   else if (entity.equals(IInfoModel.ENTITY_OTHER_ORDER)) {

            orderName = NAME_OTHER_ORDER;
            passRegExp = REG_OTHER_ORDER;                   // 手技、その他、薬剤、材料
            shinkuRegExp = SHIN_OTHER_ORDER;
            implied007 = IMPLIED_OTHER_ORDER;
            info = INFO_OTHER_ORDER;


        } else if (entity.equals(IInfoModel.ENTITY_GENERAL_ORDER)) {

            orderName = NAME_GENERAL_ORDER;
            passRegExp = REG_GENERAL_ORDER;                 // 手技、その他、薬剤、材料、用法、部位
            shinkuRegExp = SHIN_GENERAL_ORDER;
            info = INFO_GENERAL_ORDER;

        } else if (entity.equals(IInfoModel.ENTITY_DIAGNOSIS)) {

            orderName = NAME_DIAGNOSIS;
            passRegExp = REG_DIAGNOSIS;
        }

        ht.put(KEY_ORDER_NAME, orderName);

        if (passRegExp != null) {
            ht.put(KEY_PASS_REGEXP, passRegExp);
        }

        if (shinkuRegExp != null) {
            ht.put(KEY_SHIN_REGEXP, shinkuRegExp);
        }

        if (info != null) {
            ht.put(KEY_INFO, info);
        }

        if (implied007 != null) {
            ht.put(KEY_IMPLIED, implied007);
        }

        return ht;
    }

    private boolean isCode(String text) {

        if (text == null) {
            return false;
        }

        boolean maybe = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int type = Character.getType(c);
            if (type == Character.DECIMAL_DIGIT_NUMBER) {
                continue;
            } else {
                maybe = false;
                break;
            }
        }
        return maybe;
    }

    //-----------------------------------------
    // 点数で検索する場合の入力 =  ///11 etc
    //-----------------------------------------
    private boolean isTensuSearch(String text) {
        
        boolean maybe = (
                text != null 
                && text.length() > 3 
                && (text.startsWith(TENSU_SEARCH_HALF) || text.startsWith(TENSU_SEARCH_FULL)) );
        return maybe;
    }

    //---------------------------------------
    // 内容を編集できるコメントコード
    //---------------------------------------
    protected final boolean isNameEditableComment(String code) {

        if (code == null) {
            return false;
        }

        boolean ed = (code.startsWith(EDITABLE_COMMENT_81)
                || code.startsWith(EDITABLE_COMMENT_0081)
                || code.startsWith(EDITABLE_COMMENT_83)
                || code.startsWith(EDITABLE_COMMENT_0083)
                || code.startsWith(EDITABLE_COMMENT_85)
                || code.startsWith(EDITABLE_COMMENT_0085));

        return ed;
    }

    //---------------------------------------
    protected final boolean isNumberEditableComment(String code) {

        if (code == null) {
            return false;
        }

        boolean ed = (code.startsWith(EDITABLE_COMMENT_84)
                || code.startsWith(EDITABLE_COMMENT_0084));

        return ed;
    }
    
    protected final boolean isEditableNumber(String code) {
        
        if (code == null) {
            return false;
        }
        
        boolean ret = code.startsWith(ClaimConst.YAKUZAI_CODE_START)
                || code.startsWith(ClaimConst.ZAIRYO_CODE_START)
                || code.startsWith(EDITABLE_COMMENT_84)
                || code.startsWith(EDITABLE_COMMENT_0084)
                || code.startsWith(ClaimConst.SYUGI_CODE_START)
                || code.startsWith(ClaimConst.ZAIRYO_OTHER_START);
        
        return ret;
    }
    
    protected final boolean hasFixedName(String code) {
        return code != null &&
                code.matches(REGEXP_COMMENT_83);
    }
    
    //-----------------------------------------
    // 入力されたテキストから検索タイプを返す
    //-----------------------------------------
    protected final int getSearchType(String test, boolean hitReturn) {

        if (test == null || test.equals("")) {
            return TT_INVALID;
        }

        // *
        if (test.equals(ASTERISK_HALF) || test.equals(ASTERISK_FULL)) {
            return TT_LIST_TECH;
        }

        //  ///12
        if (test.startsWith(TENSU_SEARCH_HALF) || test.startsWith(TENSU_SEARCH_FULL)) {

            if (isTensuSearch(test) && hitReturn) {
                return TT_TENSU_SEARCH;
            } else {
                return TT_INVALID;
            }
        }

        // 81,82,83,84,85
        if ( (test.startsWith(COMMENT_SEARCH_HALF) || test.startsWith(COMMENT_SEARCH_FULL)) && test.length() >1 ) {
//            if (test.startsWith(COMMENT_85_SEARCH_HALF) || test.startsWith(COMMENT_85_SEARCH_FULL)) {
//                return TT_85_SEARCH;
//            } else {
//                return TT_CODE_SEARCH;
//            }
            return TT_CODE_SEARCH;
        }

        // .140 診療行為区分検索
        if ((test.startsWith(".") || test.startsWith("．")) && test.length()==4) {
            return TT_SHINKU_SERACH;
        }

        boolean textIsCode = isCode(test);

        // 6桁以上のコード
        if (textIsCode && test.length() > 5) {
            return TT_CODE_SEARCH;
        }

        // ２文字以上
        if ((!textIsCode) && test.length() > 1) {
            return TT_LETTER_SEARCH;
        }

        // １文字でreturn確定
        if ((!textIsCode) && hitReturn) {
            return TT_LETTER_SEARCH;
        }

        return TT_INVALID;
    }

    // 注射診区コード
    protected final boolean isInjection(String code) {
        return code != null &&
                (code.startsWith("31") || code.startsWith("32") || code.startsWith("33"));
    }

    public void dispose() {

        if (searchTextField != null) {
            searchTextField.setText("");
        }

        if (countField != null) {
            countField.setText("");
        }
    }

    /**
     * セットテーブルのMasterItemからClaimItemを生成する。
     * @param masterItem セットテーブルの行オブジェクト
     * @return ClaimItem
     */
    protected final ClaimItem masterToClaimItem(MasterItem masterItem) {

        ClaimItem ret = new ClaimItem();

        // コード
        ret.setCode(masterItem.getCode());

        // 名称
        ret.setName(masterItem.getName());

        // subclassCode(手技|薬剤|材料|部位|用法|その他)
        ret.setClassCode(String.valueOf(masterItem.getClassCode()));

        // Claim003
        ret.setClassCodeSystem(ClaimConst.SUBCLASS_CODE_ID);

        // 数量
        String number = trimToNullIfEmpty(masterItem.getNumber());
        if (number != null) {
            number = ZenkakuUtils.toHalfNumber(number);
            ret.setNumber(number);
            ret.setNumberCode(getNumberCode(masterItem.getClassCode()));
            ret.setNumberCodeSystem(ClaimConst.NUMBER_CODE_ID);
        }
        //System.err.println(number);

        // 単位
        String unit = trimToNullIfEmpty(masterItem.getUnit());
        if (unit != null) {
            ret.setUnit(unit);
        }

        // YKZ knb
        ret.setYkzKbn(masterItem.getYkzKbn());

        return ret;
    }

    /**
     * ClaimItemをセットテーブルの行MasterItemへ変換する。
     * @param claimItem ClaimItem
     * @return  MasterItem
     */
    protected final MasterItem claimToMasterItem(ClaimItem claimItem) {

        MasterItem ret = new MasterItem();

        // Code
        ret.setCode(claimItem.getCode());

        // Name
        ret.setName(claimItem.getName());

        // 手技・材料・薬品のフラグ
        String test = trimToNullIfEmpty(claimItem.getClassCode());
        if (test != null ) {
            ret.setClassCode(Integer.parseInt(test));
        }

        // 数量
        test = trimToNullIfEmpty(claimItem.getNumber());
        if (test != null) {
            test = ZenkakuUtils.toHalfNumber(test.trim());
            ret.setNumber(test);
        }

        // 単位
        test = trimToNullIfEmpty(claimItem.getUnit());
        if (test != null) {
            ret.setUnit(test.trim());
        }

        // YKZ kbn
        ret.setYkzKbn(claimItem.getYkzKbn());
        
        return ret;
    }

    /**
     * 点数マスタからMasterItemを生成する。
     * @param tm 点数マスタ
     * @return MasterItem
     */
    protected final MasterItem tensuToMasterItem(TensuMaster tm) {

        MasterItem ret = new MasterItem();

        // code
        ret.setCode(tm.getSrycd());

        // name
        ret.setName(tm.getName());

        // unit
        ret.setUnit(trimToNullIfEmpty(tm.getTaniname()));

        // ClaimInterface の　手技、薬剤、器材の別
        // 及び診療行為区分（診区）を設定する
        // 0: 手技  1: 材料  2: 薬剤 3: 用法 4:部位 5:その他
        String test = tm.getSlot();

        if (test.equals(ClaimConst.SLOT_SYUGI)) {

            // 手技
            ret.setClassCode(ClaimConst.SYUGI);

            // 診療行為区分 手技で設定している
            ret.setClaimClassCode(tm.getSrysyukbn());

            // もしかして数量があるかも...
            if (ret.getUnit()!=null) {
                ret.setNumber(DEFAULT_NUMBER);
            }
//masuda^   DataKbnを保存
            ret.setDataKbn(tm.getDataKbn());
//masuda$
        } else if (Pattern.compile(ClaimConst.SLOT_MEDICINE).matcher(test).find()) {

            // 薬剤
            ret.setClassCode(ClaimConst.YAKUZAI);

            ret.setYkzKbn(tm.getYkzkbn());
            //System.out.println("剤型区分=" + ret.getYkzKbn());

            String inputNum = DEFAULT_NUMBER;

            if (ret.getUnit()!= null && ret.getUnit().equals(ClaimConst.UNIT_T)) {
                //inputNum = Project.getString("defaultZyozaiNum", "3");
                inputNum = Project.getString("defaultZyozaiNum");

            } else if (ret.getUnit()!= null && ret.getUnit().equals(ClaimConst.UNIT_CAPSULE)) {
                //inputNum = Project.getString("defaultCapsuleNum", "1");   // ?
                //inputNum = Project.getString("defaultZyozaiNum", "3");
                //inputNum = Project.getString("defaultZyozaiNum");
                inputNum = Project.getString("defaultCapsuleNum");

            } else if (ret.getUnit()!= null && ret.getUnit().equals(ClaimConst.UNIT_G)) {
                //inputNum = Project.getString("defaultSanyakuNum", "1.0");
                inputNum = Project.getString("defaultSanyakuNum");

            } else if (ret.getUnit()!= null && ret.getUnit().equals(ClaimConst.UNIT_ML)) {
                //inputNum = Project.getString("defaultMizuyakuNum", "1");
                inputNum = Project.getString("defaultMizuyakuNum");

            } //else if (ret.getUnit().equals(ClaimConst.UNIT_CAPSULE)) {
                //inputNum = Project.getString("defaultKapuselNum", "1");
            //}

            // zero -> null 
            inputNum = (inputNum==null || inputNum.equals("") || inputNum.equals("0")) ? null : inputNum;
            ret.setNumber(inputNum);


        } else if (test.equals(ClaimConst.SLOT_ZAIRYO)) {
            // 材料
            ret.setClassCode(ClaimConst.ZAIRYO);
            ret.setNumber(DEFAULT_NUMBER);
            
//masuda^   コメントコードはClaimConst.OTHERにしておく
        } else if(ret.getCode().matches(ClaimConst.REGEXP_COMMENT_MED)){
            ret.setClassCode(ClaimConst.OTHER);
//masuda$
            
        } else if (test.equals(ClaimConst.SLOT_YOHO)) {
            // 用法
            ret.setClassCode(ClaimConst.ADMIN);
            ret.setName(ADMIN_MARK + tm.getName());
            ret.setDummy("X");
            ret.setBundleNumber(Project.getString("defaultRpNum", "1"));

        } else if (test.equals(ClaimConst.SLOT_BUI)) {
            // 部位
            ret.setClassCode(ClaimConst.BUI);

        } else if (test.equals(ClaimConst.SLOT_OTHER)) {
            // その他
            ret.setClassCode(ClaimConst.OTHER);
            if (ret.getUnit()!=null) {
                ret.setNumber(DEFAULT_NUMBER);
            }
        }

        return ret;
    }

    protected final String trimToNullIfEmpty(String test) {

        if (test == null) {
            return null;
        }

        test = test.trim();

        return test.equals("") ? null : test;
    }

    protected final String getClaim007Code(String code) {

        if (code == null) {
            return null;
        }

        if (code.equals(ClaimConst.INJECTION_311)) {
            return ClaimConst.INJECTION_310;

        } else if (code.equals(ClaimConst.INJECTION_321)) {
            return ClaimConst.INJECTION_320;

        } else if (code.equals(ClaimConst.INJECTION_331)) {
            return ClaimConst.INJECTION_330;

        } else {
            // 注射以外のケース
            return code;
        }
    }

    /**
     * Returns Claim004 Number Code 21 材料個数 when subclassCode = 1 11
     * 薬剤投与量（１回）when subclassCode = 2
     */
    private String getNumberCode(int subclassCode) {
        return subclassCode == 1 
                ? ClaimConst.ZAIRYO_KOSU 
                : ClaimConst.YAKUZAI_TOYORYO; // 材料個数 : 薬剤投与量１回
        // 2010 ORAC の実装
        //return ClaimConst.YAKUZAI_TOYORYO;
    }

    private void alertIpAddress() {

        String msg0 = "レセコンのIPアドレスが設定されていないため、マスターを検索できません。";
        String msg1 = "環境設定メニューからレセコンのIPアドレスを設定してください。";
        Object message = new String[]{msg0, msg1};
        Window parent = SwingUtilities.getWindowAncestor(getView());
        String title = ClientContext.getFrameTitle("マスタ検索");
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    protected final void alertSearchError(String err) {

        String msg0 = "マスターを検索できません。アクセスが許可されているかご確認ください。";
        String msg1 = err;

        Object message = new String[]{msg0, msg1};
        Window parent = SwingUtilities.getWindowAncestor(getView());
        String title = ClientContext.getFrameTitle("マスタ検索");
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    protected final boolean ipOk() {

        // CLAIM(Master) Address が設定されていない場合に警告する
        String address = Project.getString(Project.CLAIM_ADDRESS);
        if (address == null || address.equals("")) {
            alertIpAddress();
            return false;
        }

        return true;
    }

    
    public final void addPropertyChangeListener(String prop, PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(prop, listener);
    }


    public final void remopvePropertyChangeListener(String prop, PropertyChangeListener listener) {
        boundSupport.removePropertyChangeListener(prop, listener);
    }

    /**
     * @return the orderName
     */
    public final String getOrderName() {
        return orderName;
    }

    /**
     * @param orderName the orderName to set
     */
    public final void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    /**
     * @return the classCode
     */
    public final String getClassCode() {
        return classCode;
    }

    /**
     * @param classCode the classCode to set
     */
    public final void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    /**
     * @return the classCodeId
     */
    public final String getClassCodeId() {
        return classCodeId;
    }

    /**
     * @param classCodeId the classCodeId to set
     */
    public final void setClassCodeId(String classCodeId) {
        this.classCodeId = classCodeId;
    }

    /**
     * @return the subclassCodeId
     */
    public final String getSubclassCodeId() {
        return subclassCodeId;
    }

    /**
     * @param subclassCodeId the subclassCodeId to set
     */
    public final void setSubclassCodeId(String subclassCodeId) {
        this.subclassCodeId = subclassCodeId;
    }

    /**
     * @return the entity
     */
    public final String getEntity() {
        return entity;
    }

    /**
     * @param entity the entity to set
     */
    public final void setEntity(String entity) {
        this.entity = entity;
    }

    /**
     * @return the info
     */
    public final String getInfo() {
        return info;
    }

    /**
     * @param info the info to set
     */
    public final void setInfo(String info) {
        this.info = info;
    }

    public final String getImplied007() {
        return implied007;
    }

    public final void setImplied007(String default007) {
        this.implied007 = default007;
    }

    /**
     * @return the fromStampEditor
     */
    public final boolean getFromStampEditor() {
        return fromStampEditor;
    }

    /**
     * @param fromStampEditor the fromStampEditor to set
     */
    public final void setFromStampEditor(boolean fromStampEditor) {
        this.fromStampEditor = fromStampEditor;
    }

    
    public AbstractStampEditor() {
    }

    public AbstractStampEditor(String entity) {
        this(entity, true);
    }

    public AbstractStampEditor(String entity, boolean mode) {

        HashMap<String, String> ht = getEditorSpec(entity);

        this.setEntity(entity);
        this.setOrderName(ht.get(KEY_ORDER_NAME));

        if (ht.get(KEY_PASS_REGEXP) != null) {
            passPattern = Pattern.compile(ht.get(KEY_PASS_REGEXP));
        }

        if (ht.get(KEY_SHIN_REGEXP) != null) {
            shinkuRegExp = ht.get(KEY_SHIN_REGEXP);
            shinkuPattern = Pattern.compile(ht.get(KEY_SHIN_REGEXP));
        }

        if (ht.get(KEY_INFO) != null) {
            this.setInfo(ht.get(KEY_INFO));
        }

        if (ht.get(KEY_IMPLIED) != null) {
            this.setImplied007(ht.get(KEY_IMPLIED));
        }

        setFromStampEditor(mode);
    }

//masuda^    
    private static final String RT = "masterSearch.realTime";
    private static final String PT = "masterSearch.partialMatch";

    protected static final String REGEXP_COMMENT_ALL = "^8[1-4]|^008[1-6]"; // コメントコード全て
    protected static final String REGEXP_COMMENT_81  = "^81|^008[156]";     // 名称編集可能
    protected static final String REGEXP_COMMENT_83  = "^83|^0083";         // 固定文字あり、名称編集可
    protected static final String REGEXP_COMMENT_84  = "^84|^0084";         // 数量入力必要
    protected static final String REGEXP_RAD_BUI     = "^002";  // 放射線部位
    protected static final String REGEXP_RAD_ZAIRYO  = "^7";    // 放射線材料（フィルムなど）
    protected static final String REGEXP_RAD_SHUGI   = "^17";   // 放射線手技
    protected static final String REGEXP_LABO        = "^16";   // 検体検査
    
    protected abstract String[] getColumnNames();
    protected abstract String[] getColumnMethods();
    protected abstract int[] getColumnWidth();
    protected abstract String[] getSrColumnNames();
    protected abstract String[] getSrColumnMethods();
    protected abstract int[] getSrColumnWidth();
    
    public abstract IInfoModel[] getValue();
    public abstract void setValue(IInfoModel[] stamps);

    private IInfoModel[] oldValue;
    private Chart chart;
    
    protected final void setOldValue(IInfoModel[] oldValue) {
        this.oldValue = oldValue;
    }
    
    protected final IInfoModel[] getOldValue() {
        return oldValue;
    }

    // 検査エディタから検査パネルを開くとき＆ORCA処方参照時に使用
    public void setContext(Chart chart){
        this.chart = chart;
        setAdmissionFlg();
        
    }
    
    public final Chart getContext() {
        return chart;
    }
    
    private void setAdmissionFlg() {

        isAdmission = false;
        KarteEditor editor = chart.getKarteEditor();
        if (editor == null) {
            return;
        }
        AdmissionModel admission = editor.getModel().getDocInfoModel().getAdmissionModel();
        if (admission != null) {
            isAdmission = true;
        }
    }
    
    protected boolean isAdmission() {
        return isAdmission;
    }
    
    // 共通のコンポーネントをまとめて設定する
    protected final void setupOrderComponents() {
      
        // 採用薬を最新にする
        UsingDrugs.getInstance().loadUsingDrugs();

        final AbstractOrderView view = getOrderView();
        
        JTable setTable = view.getSetTable();
        // 数量入力: リターンキーで次のセルに移動するため
        //setTable.setCellSelectionEnabled(true);

        setTable.setDragEnabled(true);
        setTable.setDropMode(DropMode.INSERT);                          // INSERT
        setTable.setTransferHandler(new MasterItemTransferHandler());   // TransferHandler
        setTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 選択モード
        setTable.setRowSelectionAllowed(true);
        ListSelectionModel m = setTable.getSelectionModel();
        m.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    // ここでcheckValidationすると、DnDで順番入れ替えた後にvalidatainチェックできる
                    checkValidation();
                }
            }
        });
        
        // 列幅を設定する
        TableColumn column;
        int len = getColumnWidth().length;
        for (int i = 0; i < len; i++) {
            column = setTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(getColumnWidth()[i]);
        }
        
        // れんだら
        OrderSetTableCellRenderer  renderer = new OrderSetTableCellRenderer();
        renderer.setTable(setTable);
        renderer.setDefaultRenderer();
        setTable.setDropMode(DropMode.INSERT_ROWS);

        final JTable searchResultTable = view.getSearchResultTable();
        searchResultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultTable.setRowSelectionAllowed(true);
        ListSelectionModel lm = searchResultTable.getSelectionModel();
        lm.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    int row = view.getSearchResultTable().getSelectedRow();
                    ListTableModel<TensuMaster> searchResultModel =(ListTableModel<TensuMaster>) searchResultTable.getModel();
                    TensuMaster o = searchResultModel.getObject(row);
                    if (o != null) {
                        addSelectedTensu(o);
                        setFocusOnSearchTextFld();
                    }
                }
            }
        });
        
        len = getSrColumnWidth().length;
        for (int i = 0; i < len; i++) {
            column = searchResultTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(getSrColumnWidth()[i]);
        }
        // れんどら
        if (Project.getBoolean("masterItemColoring", true)) {
            TensuItemRenderer tRenderer = new TensuItemRenderer(passPattern, shinkuPattern);
            tRenderer.setTable(searchResultTable);
            tRenderer.setDefaultRenderer();
        } else {
            SearchResultTableRenderer srRenderer = new SearchResultTableRenderer();
            srRenderer.setTable(searchResultTable);
            srRenderer.setDefaultRenderer();
        }
        
        setupCommonComponents();
    }
    
    protected final void setupCommonComponents() {
        
        final AbstractOrderView view = getOrderView();
        
        // 件数フィールド
        countField = view.getCountField();
        
        // 検索フィールド
        DocumentListener dl = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (view.getRtCheck().isSelected()) {
                    search(view.getSearchTextField().getText().trim(), false);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (view.getRtCheck().isSelected()) {
                    search(view.getSearchTextField().getText().trim(), false);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (view.getRtCheck().isSelected()) {
                    search(view.getSearchTextField().getText().trim(), false);
                }
            }
        };
        searchTextField = view.getSearchTextField();
        searchTextField.getDocument().addDocumentListener(dl);
        searchTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search(view.getSearchTextField().getText().trim(), true);
//masuda^   検索後は全選択状態にする
                searchTextField.selectAll();
//masuda$
            }
        });
        searchTextField.addFocusListener(AutoKanjiListener.getInstance());
        
       // Real Time Search
        boolean rt = Project.getBoolean(RT, true);
        view.getRtCheck().setSelected(rt);
        view.getRtCheck().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                Project.setBoolean(RT, view.getRtCheck().isSelected());
            }
        });

        // 部分一致
        boolean pmatch = Project.getBoolean(PT, false);
        view.getPartialChk().setSelected(pmatch);
        view.getPartialChk().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                Project.setBoolean(PT, view.getPartialChk().isSelected());
            }
        });
        
        // スタンプ名フィールド
        view.getStampNameField().addFocusListener(AutoKanjiListener.getInstance());

        // OK & 連続ボタン
        view.getOkCntBtn().setEnabled(false);
        view.getOkCntBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boundSupport.firePropertyChange(VALUE_PROP, getOldValue(), getValue());
                clear();
            }
        });

        // OK ボタン
        view.getOkBtn().setEnabled(false);
        view.getOkBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boundSupport.firePropertyChange(VALUE_PROP, getOldValue(), getValue());
                dispose();
                boundSupport.firePropertyChange(EDIT_END_PROP, false, true);
            }
        });

        // 削除ボタン
        view.getDeleteBtn().setEnabled(false);
        view.getDeleteBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ListTableModel<MasterItem> tableModel = (ListTableModel<MasterItem>) view.getSetTable().getModel();
                int row = view.getSetTable().getSelectedRow();
                if (tableModel.getObject(row) != null) {
                    tableModel.deleteAt(row);
                    checkValidation();
                }
            }
        });

        // クリアボタン
        view.getClearBtn().setEnabled(false);
        view.getClearBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });
    }

    protected void clear() {
        
        AbstractOrderView view = getOrderView();
        ListTableModel<MasterItem> tableModel = getSetTableModel();
        tableModel.clear();
        view.getStampNameField().setText(DEFAULT_STAMP_NAME);
        checkValidation();
    }
    
    // 検索ルーチンを共通に
    protected final void doSearch(final String text, final int searchType) {
   
        // 件数をゼロにしておく
        final AbstractOrderView view = getOrderView();
        view.getCountField().setText("0");

        SwingWorker worker = new SwingWorker<List<TensuMaster>, Void>() {

            @Override
            protected List<TensuMaster> doInBackground() throws Exception {

                SqlMasterDao dao = SqlMasterDao.getInstance();
                
                SimpleDateFormat sdf = new SimpleDateFormat(effectiveFormat);
                String d = sdf.format(new Date());
                List<TensuMaster> result = null;

                switch (searchType) {

                    case TT_LIST_TECH:
                        break;

                    case TT_TENSU_SEARCH:
                        String ten = text.substring(3);
                        result = dao.getTensuMasterByTen(ZenkakuUtils.toHalfNumber(ten), d);
                        break;

                    case TT_85_SEARCH:
                        result = dao.getTensuMasterByCode("0085", d);
                        break;

                    case TT_CODE_SEARCH:
                        result = dao.getTensuMasterByCode(ZenkakuUtils.toHalfNumber(text), d);
                        break;

                    case TT_LETTER_SEARCH:
                        boolean pt = view.getPartialChk().isSelected();
                        //result = dao.getTensuMasterByName(StringTool.hiraganaToKatakana(text), d, pt);
                        result = dao.getTensuMasterByName(text, d, pt);
                        break;
                }

                if (!dao.isNoError()) {
                    throw new Exception(dao.getErrorMessage());
                }
                return result;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    List<TensuMaster> result = get();
                    // 採用薬をチェック、リストの上位に登録
                    List<TensuMaster> saiyou = new ArrayList<TensuMaster>();
                    List<TensuMaster> hisaiyou = new ArrayList<TensuMaster>();
                    for (TensuMaster tm : result) {
                         boolean inUse = UsingDrugs.getInstance().isInUse(tm.getSrycd());
                         tm.setInUse(inUse);
                        if (inUse) {
                            saiyou.add(tm);
                        } else {
                            hisaiyou.add(tm);
                        }
                    }
                    result.clear();
                    result.addAll(saiyou);
                    result.addAll(hisaiyou);
                    
                    ListTableModel<TensuMaster> srModel = (ListTableModel<TensuMaster>) view.getSearchResultTable().getModel();
                    srModel.setDataProvider(result);
                    int cnt = srModel.getObjectCount();
                    view.getCountField().setText(String.valueOf(cnt));
                    // 検索後は最初の行を表示させる
                    showFirstResult(view.getSearchResultTable());

                } catch (InterruptedException ex) {

                } catch (ExecutionException ex) {
                    alertSearchError(ex.getMessage());
                }
            }
        };

        worker.execute(); 
    }
    
    /**
     * validDataProp と emptyDataPropの通知を行う。
     */
    protected final void controlButtons(boolean setIsEmpty, boolean setIsValid) {

        // 共通のボタンコントロールはまとめてここで行う
        AbstractOrderView view = getOrderView();

        // ButtonControl
        view.getClearBtn().setEnabled(!setIsEmpty);
        view.getOkCntBtn().setEnabled(setIsValid && getFromStampEditor());
        view.getOkBtn().setEnabled(setIsValid && getFromStampEditor());

        // ListSelectionListenerからここに移動
        int row = view.getSetTable().getSelectedRow();
        ListTableModel<MasterItem> tableModel = (ListTableModel<MasterItem>) view.getSetTable().getModel();
        if (tableModel.getObject(row) != null) {
            view.getDeleteBtn().setEnabled(true);
        } else {
            view.getDeleteBtn().setEnabled(false);
        }

        if (boundSupport != null) {
            boundSupport.firePropertyChange(EMPTY_DATA_PROP, !setIsEmpty, setIsEmpty);
            boundSupport.firePropertyChange(VALIDA_DATA_PROP, !setIsValid, setIsValid);
        }
    }
    
    // 編集対象スタンプからスタンプ名などを設定する
    @SuppressWarnings("unchecked")
    protected BundleDolphin setInfoModels(IInfoModel[] value){

        // 連続して編集される場合があるのでテーブル内容等をクリアする
        clear();        
        if (value == null || value.length == 0) {
            return null;
        }
        
        ModuleModel target = ((ModuleModel[]) value)[0];
        // null であればリターンする
        if (target == null) {
            return null;
        }

        // Entityを保存する
        setEntity(target.getModuleInfoBean().getEntity());

        // Stamp 名と表示形式を設定する
        String stampName = target.getModuleInfoBean().getStampName();
        boolean serialized = target.getModuleInfoBean().isSerialized();

        // スタンプ名がエディタから発行の場合はデフォルトの名称にする
        // 歴史的なごり
        if (!serialized && stampName.startsWith(FROM_EDITOR_STAMP_NAME)) {
            stampName = DEFAULT_STAMP_NAME;
        } else if (stampName.equals("")) {
            stampName = DEFAULT_STAMP_NAME;
        }
        AbstractOrderView view = getOrderView();
        view.getStampNameField().setText(stampName);

        // Model を表示する
        BundleDolphin bundle = (BundleDolphin) target.getModel();
        if (bundle == null) {
            return null;
        }

        //-----------------------------
        // Bundle の 診療行為区分を保存
        //-----------------------------
        setClassCode(bundle.getClassCode());

        // ClaimItemをMasterItemへ変換してテーブルへ追加する
        ClaimItem[] items = bundle.getClaimItem();
        for (ClaimItem item : items) {
            getSetTableModel().addObject(claimToMasterItem(item));
        }
        
        // 編集元を保存する
        setOldValue(value);
        
        return bundle;
    };

    protected void updateMasterItems (final List<MasterItem> list){

        final AbstractOrderView view = (AbstractOrderView) getView();
        // ButtonControl
        view.getClearBtn().setEnabled(false);
        view.getOkCntBtn().setEnabled(false);
        view.getOkBtn().setEnabled(false);

        final BlockGlass blockGlass = new BlockGlass();
        Window parent = SwingUtilities.getWindowAncestor(view);
        // 親がJFrameのときとJDialogのときがある
        if (parent instanceof JFrame) {
            JFrame frame = (JFrame) parent;
            frame.setGlassPane(blockGlass);
            blockGlass.setSize(frame.getSize());
        } else if (parent instanceof JDialog) {
            JDialog dialog = (JDialog) parent;
            dialog.setGlassPane(blockGlass);
            blockGlass.setSize(dialog.getSize());
        }

        final SqlMiscDao dao2 = SqlMiscDao.getInstance();

        SwingWorker worker = new SwingWorker<List<TensuMaster>, Void>() {

            @Override
            protected List<TensuMaster> doInBackground() throws Exception {

                blockGlass.block();
                List<String> srycdList = new ArrayList<String>();
                for (MasterItem mi : list) {
                    srycdList.add(mi.getCode());
                }
                List<TensuMaster> result = dao2.getTensuMasterList(srycdList);
                if (!dao2.isNoError()){
                    throw new Exception();
                }
                return result;
            }
            @Override
            protected void done() {
                try {
                    List<TensuMaster> result = get();
                    for (MasterItem mItem : list){
                        for (TensuMaster tm : result) {
                            if (mItem.getCode().equals(tm.getSrycd())){
                                mItem.setClaimClassCode(tm.getSrysyukbn());
                                mItem.setDataKbn(tm.getDataKbn());
                                mItem.setYkzKbn(tm.getYkzkbn());
                                mItem.setSrysyuKbn(tm.getSrysyukbn());
                                break;
                            }
                        }
                    }

                } catch (Exception ex) {
                } finally {
                    checkValidation();
                    ((ListTableModel<MasterItem>) (view.getSetTable().getModel())).fireTableDataChanged();
                    blockGlass.unblock();
                }
            }

        };
        worker.execute();
    }
    
    
    private AbstractOrderView getOrderView() {
        return (AbstractOrderView) getView();
    }
    protected ListTableModel<MasterItem> getSetTableModel() {
        return (ListTableModel<MasterItem>) getOrderView().getSetTable().getModel();
    }
    
    protected final void showFirstResult(JTable table) {
        table.scrollRectToVisible(table.getCellRect(0, 0, true));
    }
    
    public final void setFocusOnSearchTextFld() {
        
        if (searchTextField == null) {
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                searchTextField.requestFocusInWindow();
            }
        });
    }
    
    // setTableのMouseMotionListener
    protected static class SetTableMouseMotionListener implements MouseMotionListener {

        @Override
        public void mouseDragged(MouseEvent e) {
            int ctrlMask = InputEvent.CTRL_DOWN_MASK;
            int action = ((e.getModifiersEx() & ctrlMask) == ctrlMask)
                    ? TransferHandler.COPY
                    : TransferHandler.MOVE;

            JTable setTable = (JTable) e.getSource();
            // 非選択状態からいきなりドラッグを開始すると cellEditor が残ってしまう問題の workaround
            if (setTable.isEditing()) {
                setTable.getCellEditor().stopCellEditing();
            }
            TransferHandler handler = setTable.getTransferHandler();
            handler.exportAsDrag(setTable, e, action);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }
    
    // 検索結果で採用薬を色分けするレンダラ
    protected static class SearchResultTableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            TensuMaster tm = ((ListTableModel<TensuMaster>) table.getModel()).getObject(row);
            if (tm != null && tm.isInUse() && !isSelected){
                setForeground(java.awt.Color.BLUE);
            }
            return this;
        }
    }
    
    /**
     * MasterPanelTableCellRenderer
     * 
     * @author pns
     * @author modified by masuda, Masuda Naika
     */
    protected static class OrderSetTableCellRenderer extends StripeTableCellRenderer {

        private boolean isTargetRow;
        private boolean isUnderline;
        private final Color LINE_COLOR = new Color(0x0A, 0x53, 0xB6);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            JTable.DropLocation loc = table.getDropLocation();
            int targetRow = (loc == null) ? -1 : table.getDropLocation().getRow();

            if (row == 0 && targetRow == 0) {
                isTargetRow = true;
                isUnderline = false;
            } else if (row == targetRow - 1) {
                isTargetRow = true;
                isUnderline = true;
            } else {
                isTargetRow = false;
            }

            return this;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (isTargetRow) {
                g.setColor(LINE_COLOR);
                if (isUnderline) {
                    g.fillRect(0, getSize().height - 2, getSize().width, getSize().height);
                } else {
                    g.fillRect(0, 0, getSize().width, 2);
                }
            }
        }
    }
//masuda$
}
