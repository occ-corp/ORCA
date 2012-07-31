
package open.dolphin.order;

import java.text.SimpleDateFormat;

/**
 * StampEditorで使う定数群
 * AbstractStampEditorから分離した
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class StampEditorConst {
    
    public static final String VALUE_PROP = "value";
    public static final String VALIDA_DATA_PROP = "validData";
    public static final String EMPTY_DATA_PROP = "emptyData";
    public static final String EDIT_END_PROP = "editEnd";
    public static final String CURRENT_SHINKU_PROP = "currentShinkuProp";

    protected static final String DEFAULT_NUMBER = "1";

    protected static final String DEFAULT_STAMP_NAME     = "新規スタンプ";
    protected static final String FROM_EDITOR_STAMP_NAME = "エディタから";

    protected static final String[] MED_COST_FLGAS = {"廃","金","都","","","","","減","不"};
    protected static final String[] TOOL_COST_FLGAS = {"廃","金","都","","","%加","","","","乗"};
    protected static final String[] TREAT_COST_FLGAS = {"廃","金","","+点","都","%加","%減","減","-点"};
    protected static final String[] IN_OUT_FLAGS = {"入外","入","外"};
    protected static final String[] HOSPITAL_CLINIC_FLAGS = {"病診","病","診"};
    protected static final String[] OLD_FLAGS = {"社老","社","老"};

    protected static final String ADMIN_MARK = "[用法] ";
    protected static final String REG_ADMIN_MARK = "\\[用法\\] ";

    protected static final int START_NUM_ROWS = 20;
    
    // 組み合わせができるマスター項目
    protected static final String REG_BASE_CHARGE           = "[手そ]";
    protected static final String REG_INSTRACTION_CHARGE    = "[手そ薬材]";     // 在宅で薬剤、材料を追加
    protected static final String REG_MED_ORDER             = "[薬用材そ]";     // 保険適用外の医薬品等追加
    protected static final String REG_INJECTION_ORDER       = "[手そ注材]";
    protected static final String REG_TREATMENT             = "[手そ薬材]";
    protected static final String REG_SURGERY_ORDER         = "[手そ薬材]";
    protected static final String REG_BACTERIA_ORDER        = "[手そ薬材]";
    protected static final String REG_PHYSIOLOGY_ORDER      = "[手そ薬材]";
    protected static final String REG_LABO_TEST             = "[手そ薬材]";
    protected static final String REG_RADIOLOGY_ORDER       = "[手そ薬材部]";
    protected static final String REG_OTHER_ORDER           = "[手そ薬材]";
    protected static final String REG_GENERAL_ORDER         = "[手そ薬材用部]";

    // セットできる診療行為区分
    protected static final String SHIN_BASE_CHARGE           = "^(11|12)";
    protected static final String SHIN_INSTRACTION_CHARGE    = "^(13|14)";
    protected static final String SHIN_MED_ORDER             = "";              // 210|220|230
    protected static final String SHIN_INJECTION_ORDER       = "^3";            // 310|320|330
    protected static final String SHIN_TREATMENT             = "^4";
    protected static final String SHIN_SURGERY_ORDER         = "^5";
    protected static final String SHIN_BACTERIA_ORDER        = "^6";
    protected static final String SHIN_PHYSIOLOGY_ORDER      = "^6";
    protected static final String SHIN_LABO_TEST             = "^6";
    protected static final String SHIN_RADIOLOGY_ORDER       = "^7";
    protected static final String SHIN_OTHER_ORDER           = "^8";
    protected static final String SHIN_GENERAL_ORDER         = "\\d";

    // エディタに表示する名前
    protected static final String NAME_BASE_CHARGE           = "診断料";
    protected static final String NAME_INSTRACTION_CHARGE    = "管理料 ";       // 指導・在宅
    protected static final String NAME_MED_ORDER             = "処 方";
    protected static final String NAME_INJECTION_ORDER       = "注 射";
    protected static final String NAME_TREATMENT             = "処 置";
    protected static final String NAME_SURGERY_ORDER         = "手 術";
    protected static final String NAME_BACTERIA_ORDER        = "細菌検査";
    protected static final String NAME_PHYSIOLOGY_ORDER      = "生理・内視鏡検査";
    protected static final String NAME_LABO_TEST             = "検体検査";
    protected static final String NAME_RADIOLOGY_ORDER       = "放射線";
    protected static final String NAME_OTHER_ORDER           = "その他";
    protected static final String NAME_GENERAL_ORDER         = "汎 用";

    // 暗黙の診療行為区分
    protected static final String IMPLIED_BASE_CHARGE           = "";
    protected static final String IMPLIED_INSTRACTION_CHARGE    = "";
    protected static final String IMPLIED_MED_ORDER             = "";
    protected static final String IMPLIED_INJECTION_ORDER       = "";
    protected static final String IMPLIED_TREATMENT             = "400";
    protected static final String IMPLIED_SURGERY_ORDER         = "";
    protected static final String IMPLIED_BACTERIA_ORDER        = "600";
    protected static final String IMPLIED_PHYSIOLOGY_ORDER      = "600";
    protected static final String IMPLIED_LABO_TEST             = "600";
    protected static final String IMPLIED_RADIOLOGY_ORDER       = "700";
    protected static final String IMPLIED_OTHER_ORDER           = "800";
    protected static final String IMPLIED_GENERAL_ORDER         = "";

    // 情報
    protected static final String INFO_BASE_CHARGE           = "診断料（診区=110-120）";
    protected static final String INFO_INSTRACTION_CHARGE    = "管理料（診区=130-140）";
    protected static final String INFO_MED_ORDER             = "処 方";
    protected static final String INFO_INJECTION_ORDER       = "注 射（診区=300）";
    protected static final String INFO_TREATMENT             = "処 置（診区=400）";
    protected static final String INFO_SURGERY_ORDER         = "手 術（診区=500）";
    protected static final String INFO_BACTERIA_ORDER        = "細菌検査（診区=600）";
    protected static final String INFO_PHYSIOLOGY_ORDER      = "生理・内視鏡検査（診区=600）";
    protected static final String INFO_LABO_TEST             = "検体検査（診区=600）";
    protected static final String INFO_RADIOLOGY_ORDER       = "放射線（診区=700）";
    protected static final String INFO_OTHER_ORDER           = "その他（診区=800）";
    protected static final String INFO_GENERAL_ORDER         = "汎 用（診区=100-999）";

    // 病名
    protected static final String NAME_DIAGNOSIS             = "傷病名";
    protected static final String REG_DIAGNOSIS              = "[手そ薬材用部]";

    // 辞書のキー
    protected static final String KEY_ORDER_NAME    = "orderName";
    protected static final String KEY_PASS_REGEXP   = "passRegExp";
    protected static final String KEY_SHIN_REGEXP   = "shinkuRegExp";
    protected static final String KEY_INFO          = "info";
    protected static final String KEY_IMPLIED       = "implied007";

    // 編集可能コメント
    protected static final String EDITABLE_COMMENT_81   = "81";;   //"810000001";
    protected static final String EDITABLE_COMMENT_0081 = "0081";
    protected static final String EDITABLE_COMMENT_83   = "83";
    protected static final String EDITABLE_COMMENT_0083 = "0083";
    protected static final String EDITABLE_COMMENT_84   = "84";
    protected static final String EDITABLE_COMMENT_0084 = "0084";
    protected static final String EDITABLE_COMMENT_85   = "85";
    protected static final String EDITABLE_COMMENT_0085 = "0085";  //"008500000";

    // 検索特殊記号文字
    protected static final String ASTERISK_HALF = "*";
    protected static final String ASTERISK_FULL = "＊";
    protected static final String TENSU_SEARCH_HALF = "///";
    protected static final String TENSU_SEARCH_FULL = "／／／";
    protected static final String COMMENT_SEARCH_HALF = "8";
    protected static final String COMMENT_SEARCH_FULL = "８";
    protected static final String COMMENT_85_SEARCH_HALF = "85";
    protected static final String COMMENT_85_SEARCH_FULL = "８５";

    // 検索タイプ
    protected static final int TT_INVALID       = -1;
    protected static final int TT_LIST_TECH     = 0;
    protected static final int TT_TENSU_SEARCH  = 1;
    protected static final int TT_85_SEARCH     = 2;
    protected static final int TT_CODE_SEARCH   = 3;
    protected static final int TT_LETTER_SEARCH = 4;
    protected static final int TT_SHINKU_SERACH = 5;
    
    // Editor button
    protected static final String STAMP_EDITOR_BUTTON_TYPE = "stamp.editor.buttonType";
    protected static final String BUTTON_TYPE_IS_ICON = "icon";
    protected static final String BUTTON_TYPE_IS_ITEXT = "text";

    // ORCA 有効期限用のDF
    protected static final SimpleDateFormat effectiveFormat = new SimpleDateFormat("yyyyMMdd");
    
    protected static final String CLAIM_007 = "Claim007";
    protected static final String CLAIM_003 = "Claim003";
}
