package open.dolphin.infomodel;

import java.util.HashMap;
import java.util.Map;

/**
 * MML Table Dictionary class.
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public final class MMLTable {

    /** Creates new MMLTable */
    public MMLTable() {
    }
    
    private static final Map<String, String> claimClassCode;
    static {
//masuda^   ORCA「データベーステーブル仕様書」(orca-table18.pdf)に合わせて変更
/*
        claimClassCode = new HashMap<String, String>(45, 0.75f);
        claimClassCode.put("110", "初診");
        claimClassCode.put("120", "再診(再診)");
        claimClassCode.put("122", "再診(外来管理加算)");
        claimClassCode.put("123", "再診(時間外)");
        claimClassCode.put("124", "再診(休日)");
        claimClassCode.put("125", "再診(深夜)");
        claimClassCode.put("130", "指導");
        claimClassCode.put("140", "在宅");
        claimClassCode.put("210", "投薬(内服・頓服・調剤)(入院外)");
        claimClassCode.put("211", "投薬(内服・頓服・調剤)(院内)");
        claimClassCode.put("212", "投薬(内服・頓服・調剤)(院外)");
        claimClassCode.put("230", "投薬(外用・調剤)(入院外)");
        claimClassCode.put("231", "投薬(外用・調剤)(院内)");
        claimClassCode.put("232", "投薬(外用・調剤)(院外)");
        claimClassCode.put("240", "投薬(調剤)(入院)");
        claimClassCode.put("250", "投薬(処方)");
        claimClassCode.put("260", "投薬(麻毒)");
        claimClassCode.put("270", "投薬(調基)");
        claimClassCode.put("300", "注射(生物学的製剤・精密持続点滴・麻薬)");
        claimClassCode.put("310", "注射(皮下筋肉内)");
        claimClassCode.put("320", "注射(静脈内)");
        claimClassCode.put("330", "注射(その他)");
        claimClassCode.put("311", "注射(皮下筋肉内)");
        claimClassCode.put("321", "注射(静脈内)");
        claimClassCode.put("331", "注射(その他)");
        claimClassCode.put("400", "処置");
        claimClassCode.put("500", "手術(手術)");
        claimClassCode.put("502", "手術(輸血)");
        claimClassCode.put("503", "手術(ギプス)");
        claimClassCode.put("540", "麻酔");
        claimClassCode.put("600", "検査");
        claimClassCode.put("700", "画像診断");
        claimClassCode.put("800", "その他");
        claimClassCode.put("903", "入院(入院料)");
        claimClassCode.put("906", "入院(外泊)");
        claimClassCode.put("910", "入院(入院時医学管理料)");
        claimClassCode.put("920", "入院(特定入院料・その他)");
        claimClassCode.put("970", "入院(食事療養)");
        claimClassCode.put("971", "入院(標準負担額)");
*/
        claimClassCode = new HashMap<String, String>(100, 0.75f);
        claimClassCode.put("110","初診料");
        claimClassCode.put("120","再診料");
        claimClassCode.put("130","管理料");
        claimClassCode.put("140","在宅料");
        claimClassCode.put("141","在宅薬剤");
        claimClassCode.put("142","在宅材料");
        claimClassCode.put("143","在宅加算料");
        claimClassCode.put("148","在宅薬剤（院外処方）");
        claimClassCode.put("149","在宅材料（院外処方）");
        claimClassCode.put("210","内服薬剤");
        claimClassCode.put("211","内服薬剤（院内処方）");
        claimClassCode.put("212","内服薬剤（院外処方）");
        claimClassCode.put("213","内服薬剤（処方のみ）");
        claimClassCode.put("214","内服薬剤（入院調剤料なし）");
        claimClassCode.put("290","内服薬剤（臨時投薬）");
        claimClassCode.put("291","内服薬剤（臨時投薬）（院内）");
        claimClassCode.put("292","内服薬剤（臨時投薬）（院外）");
        claimClassCode.put("220","頓服薬剤");
        claimClassCode.put("221","頓服薬剤（院内処方）");
        claimClassCode.put("222","頓服薬剤（院外処方）");
        claimClassCode.put("223","頓服薬剤（処方のみ）");
        claimClassCode.put("224","頓服薬剤（入院調剤料なし）");
        claimClassCode.put("230","外用薬剤");
        claimClassCode.put("231","外用薬剤（院内処方）");
        claimClassCode.put("232","外用薬剤（院外処方）");
        claimClassCode.put("233","外用薬剤（処方のみ）");
        claimClassCode.put("234","外用薬剤（入院調剤料なし）");
        claimClassCode.put("240","入院調剤料");
        claimClassCode.put("241","内服調剤料");
        claimClassCode.put("242","外用調剤料");
        claimClassCode.put("250","処方料");
        claimClassCode.put("260","麻毒加算");
        claimClassCode.put("270","調剤技術基本料");
        claimClassCode.put("310","皮下筋肉注射");
        claimClassCode.put("311","皮下筋肉注射（手技料なし）");
        claimClassCode.put("312","皮下筋肉注射（手技料変換なし）");
        claimClassCode.put("320","静脈注射");
        claimClassCode.put("321","静脈注射（手技料なし）");
        claimClassCode.put("330","点滴注射");
        claimClassCode.put("331","点滴注射（手技料なし）");
        claimClassCode.put("332","点滴注射（手術以外）");
        claimClassCode.put("334","在宅訪問点滴（薬剤料）");
        claimClassCode.put("340","その他注射");
        claimClassCode.put("350","中心静脈注射薬剤");
        claimClassCode.put("352","中心静脈注射薬剤（手術以外）");
        claimClassCode.put("400","処置行為");
        claimClassCode.put("401","処置薬剤");
        claimClassCode.put("402","処置材料");
        claimClassCode.put("403","処置加算料");
        claimClassCode.put("409","処置行為（労災読み替え加算対象外）");
        claimClassCode.put("500","手術");
        claimClassCode.put("501","手術薬剤");
        claimClassCode.put("502","手術材料");
        claimClassCode.put("510","輸血");
        claimClassCode.put("520","ギブス");
        claimClassCode.put("540","麻酔");
        claimClassCode.put("541","麻酔薬剤");
        claimClassCode.put("542","麻酔材料");
        claimClassCode.put("600","検査");
        claimClassCode.put("601","検査薬剤");
        claimClassCode.put("602","検査材料");
        claimClassCode.put("603","検査加算料");
        claimClassCode.put("610","検査（包括対象外）");
        claimClassCode.put("640","病理診断");
        claimClassCode.put("643","病理診断加算料");
        claimClassCode.put("700","画像診断");
        claimClassCode.put("701","画像診断薬剤");
        claimClassCode.put("702","画像診断材料");
        claimClassCode.put("703","Ｘ線フィルム");
        claimClassCode.put("704","画像診断加算料");
        claimClassCode.put("710","診断料");
        claimClassCode.put("711","Ｘ線診断料");
        claimClassCode.put("712","核医学診断料");
        claimClassCode.put("713","コンピューター診断料");
        claimClassCode.put("720","撮影料");
        claimClassCode.put("721","画像診断撮影料");
        claimClassCode.put("723","コンピューター撮影料（ＣＴ）");
        claimClassCode.put("724","コンピューター撮影料（ＭＲＩ）");
        claimClassCode.put("731","造影剤・注入手技（点滴）");
        claimClassCode.put("732","造影剤・注入手技（その他）");
        claimClassCode.put("800","その他");
        claimClassCode.put("810","リハビリ");
        claimClassCode.put("820","処方箋料");
        claimClassCode.put("830","精神科専門療法");
        claimClassCode.put("840","放射線治療");
        claimClassCode.put("850","療養担当手当");
        claimClassCode.put("890","入院料（外来）");
        claimClassCode.put("900","入院（入院料）");
        claimClassCode.put("920","入院（特定入院料・その他）");
        claimClassCode.put("930","老人一部負担金");
        claimClassCode.put("950","保険外（消費税なし）");
        claimClassCode.put("960","保険外（消費税あり）");
        claimClassCode.put("970","食事（食事療養）");
        claimClassCode.put("971","食事（標準負担額）");
        claimClassCode.put("980","コメント（処方せん備考）");
        claimClassCode.put("982","コメント（退院時処方せん備考）");
        claimClassCode.put("990","コメント");
        claimClassCode.put("991","コメント（摘要欄下部表示）");
//masuda$
    }    
    public static String getClaimClassCodeName(String key) {
        return (String)claimClassCode.get(key);
    }
    
//masuda^
    public static Map<String, String> getClaimClassCodeMap() {
        return claimClassCode;
    }
//masuda$
    
    // jma-receipt-manual-460.pdf P.746, MML0028
    private static final HashMap<String, String> departmentCode;
    static {
     
        departmentCode = new HashMap<String, String>(40, 1.0f);
        departmentCode.put("内科", "01");
        departmentCode.put("精神科", "02");
        departmentCode.put("神経科", "03");
        departmentCode.put("神経内科", "04");
        departmentCode.put("呼吸器科", "05");
        departmentCode.put("消化器科", "06");
        departmentCode.put("胃腸科", "07");
        departmentCode.put("循環器科", "08");
        departmentCode.put("小児科", "09");
        departmentCode.put("外科", "10");
        departmentCode.put("整形外科", "11");
        departmentCode.put("形成外科", "12");
        departmentCode.put("美容外科", "13");
        departmentCode.put("脳神経外科", "14");
        departmentCode.put("呼吸器外科", "15");
        departmentCode.put("心臓血管外科", "16");
        departmentCode.put("小児外科", "17");
        departmentCode.put("皮膚ひ尿器科", "18");
        departmentCode.put("皮膚科", "19");
        departmentCode.put("ひ尿器科", "20");
        departmentCode.put("性病科", "21");
        departmentCode.put("こう門科", "22");
        departmentCode.put("産婦人科", "23");
        departmentCode.put("産科", "24");
        departmentCode.put("婦人科", "25");
        departmentCode.put("眼科", "26");
        departmentCode.put("耳鼻いんこう科", "27");
        departmentCode.put("気管食道科", "28");
        departmentCode.put("理学診療科", "29"); // 欠？
        departmentCode.put("放射線科", "30");
        departmentCode.put("麻酔科", "31");
        departmentCode.put("人工透析科", "32"); // 欠？
        departmentCode.put("心療内科", "33");
        departmentCode.put("アレルギー科", "34");
        departmentCode.put("リウマチ科", "35");
        departmentCode.put("リハビリテーション科", "36");
        departmentCode.put("病理診断科", "37");
        departmentCode.put("臨床検査科", "38");
        departmentCode.put("救急科", "39");
        departmentCode.put("鍼灸", "A1");
        
        departmentCode.put("医事", "AM");  // Division of Accounting and Management
    }    
    public static String getDepartmentCode(String key) {
       return (String)departmentCode.get(key);
    }
    
    // MML0016 Outcome（転帰）
    private static final Map<String, String> mml0016Map;
    
    static {
        mml0016Map = new HashMap<String, String>();
        mml0016Map.put("died", "死亡");
        mml0016Map.put("worsening", "悪化");
        mml0016Map.put("unchanged", "不変");
        mml0016Map.put("recovering", "回復");
        mml0016Map.put("fullyRecovered", "全治");
        mml0016Map.put("sequelae", "続発症（の発生）");
        mml0016Map.put("end", "終了");
        mml0016Map.put("pause", "中止");
        mml0016Map.put("continued", "継続");
        mml0016Map.put("transfer", "転医");
        mml0016Map.put("transferAcute", "転医(急性病院へ）");
        mml0016Map.put("transferChronic", "転医(慢性病院へ）");
        mml0016Map.put("home", "自宅等へ退院");
        mml0016Map.put("unknown", "不明");
    }
    
    private static final Map<String, String> claim011Map;
    
    static {
        claim011Map = new HashMap<String, String>();
        claim011Map.put("001", "入院");
        claim011Map.put("002", "退院");
        claim011Map.put("003", "転棟");
        claim011Map.put("004", "転科");
        claim011Map.put("005", "転室");
        claim011Map.put("006", "外泊");
        claim011Map.put("007", "帰院");
        claim011Map.put("008", "担当医");
        claim011Map.put("009", "一般食");
        claim011Map.put("010", "特食（加算）");
        claim011Map.put("011", "選択食");
        claim011Map.put("012", "食止め");
    }
    
    private static final Map<String, String> claim012Map;
    
    static {
        claim012Map = new HashMap<String, String>();
        claim012Map.put("01", "一般入院");
        claim012Map.put("02", "特定入院");
    }
    
    private static final Map<String, String> claim013Map;
    
    static {
        claim013Map = new HashMap<String, String>();
        claim013Map.put("01", "継続入院");
    }
}