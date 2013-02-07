package open.dolphin.impl.labrcv;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import open.dolphin.client.ClientContext;
import open.dolphin.infomodel.NLaboItem;
import open.dolphin.infomodel.NLaboModule;
import open.dolphin.util.CharsetDetector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Hl7Parser
 * 和歌山市医師会成人病センター仕様
 *
 * @author root
 * @author modified by masuda, Masuda Naika
 */
public class Hl7Parser implements LabResultParser {

    private Boolean DEBUG;
    private Logger logger;

    private static final String HL7 = "HL7";

    public Hl7Parser() {
        logger = ClientContext.getLaboTestLogger();
        DEBUG = (logger.getLevel() == Level.DEBUG);
    }

    @Override
    public List<NLaboImportSummary> parse(File file) {

        Hl7 hl7 = new Hl7();
        List<HL7ResultSet> list = hl7.getHL7ResultSets(file.getPath());

        if (DEBUG) {
            logger.debug(list.size());
            logger.debug("結果");
        }

        String currentKey = null;
        NLaboModule curModule = null;
        List<NLaboModule> allModules = new ArrayList<NLaboModule>();

        for (HL7ResultSet resultSet : list) {

            if (DEBUG) {

                logger.debug("-----------------------");
                logger.debug(resultSet.number);
                logger.debug("検査会社名:" + resultSet.studyCo);
                logger.debug("受信施設名:" + resultSet.receptionIns);
                logger.debug("透析前後:" + resultSet.dialysisBA);
                logger.debug("手術後:" + resultSet.operationBA);
                logger.debug("カルテ番号:" + resultSet.karteNo);
                logger.debug("カナ患者名:" + resultSet.patientNameKANA);
                logger.debug("漢字患者名:" + resultSet.patientNameKANJI);
                logger.debug("患者生年月日:" + resultSet.patientBirthdate);
                logger.debug("患者年齢:" + resultSet.patientAge);
                logger.debug("性別:" + resultSet.sex);
                logger.debug("患者区分:" + resultSet.patientDiv);
                logger.debug("担当医:" + resultSet.medicalAtt);
                logger.debug("採取日:" + resultSet.studyDate);
                logger.debug("検査項目コード:" + resultSet.studyCode);
                logger.debug("検査項目名略称:" + resultSet.studyNickname);
                logger.debug("検査項目名:" + resultSet.studyName);
                logger.debug("MEDISコード:" + resultSet.medisCode);

                logger.debug("グループコード:" + resultSet.groupCode);
                logger.debug("グループ名称:" + resultSet.groupName);
                logger.debug("検査項目コード・親:" + resultSet.studyCodeP);

                logger.debug("検査材料コード:" + resultSet.specimenCode);
                logger.debug("検査材料名称:" + resultSet.specimenName);

                logger.debug("基準値:" + resultSet.standardval);
                logger.debug("単位名称:" + resultSet.unit);
                logger.debug("検査結果タイプ:" + resultSet.rType);
                logger.debug("検査結果:" + resultSet.studyResult);
                logger.debug("異常区分:" + resultSet.abnormaldiv);
                logger.debug("副成分(坑酸菌結果区分):" + resultSet.fktest);
                logger.debug("報告状況コード:" + resultSet.reportCode);
                logger.debug("コメント１コード:" + resultSet.reportCode1);
                logger.debug("コメント１:" + resultSet.reportName1);
            }

            // LabModule の Key を生成する
            StringBuilder sb = new StringBuilder();
            sb.append(resultSet.karteNo).append(".");
            sb.append(resultSet.studyDate).append(".");
            sb.append(resultSet.studyCo);
            String testKey = sb.toString();

            if (!testKey.equals(currentKey)) {
                System.out.println("test =" + testKey + " current = " + currentKey);
                // 新規LabModuleを生成しリストに加える
                curModule = new NLaboModule();
                curModule.setLaboCenterCode(resultSet.studyCo);
                curModule.setPatientId(resultSet.karteNo);
                curModule.setPatientName(resultSet.patientNameKANJI);
                curModule.setPatientSex(resultSet.sex);
                curModule.setSampleDate(getYMD(resultSet.studyDate));
                curModule.setModuleKey(currentKey);
                curModule.setReportFormat(HL7);
                allModules.add(curModule);

                currentKey = testKey;
            }

            // NLaboItemを生成し関係を構築する
            NLaboItem item = new NLaboItem();
            curModule.addItem(item);
            item.setLaboModule(curModule);

            item.setLaboCode(resultSet.studyCo);            //検査会社名
            item.setPatientId(resultSet.karteNo);           //カルテ番号
            item.setSampleDate(getYMD(resultSet.studyDate)); //検体採取日
            //item.setLipemia(resultSet.nyubi);             //乳ビ
            //item.setHemolysis(resultSet.hemolysis);       //溶血
            //item.setDialysis(resultSet.dialysisBA);       //透析前後
            //item.setDialysis(resultSet.operationBA);      //手術前後　項目にはない
//masuda^
            //item.setReportStatus(resultSet.reportCode);     //報告状況コード
            String reportCode = resultSet.reportCode;
            if ("C".equals(reportCode) || "F".equals(reportCode)) {
                item.setReportStatus("E");
            } else {
                item.setReportStatus("M");
            }
//masuda$
            item.setGroupCode(resultSet.groupCode);         //グループコード
            item.setGroupName(resultSet.groupName);         //グループ名称
            item.setParentCode(resultSet.studyCodeP);       //検査項目コード・親
            item.setItemCode(resultSet.studyCode);          //検査項目コード
            item.setMedisCode(resultSet.medisCode);         //MEDISコード
            item.setItemName(resultSet.studyNickname);      //検査項目略称
            item.setItemName(resultSet.studyName);          //検査項目名
            item.setAbnormalFlg(resultSet.abnormaldiv);     //異常区分
            item.setNormalValue(resultSet.standardval);     //基準値
            item.setValue(resultSet.studyResult);           //検査結果
            item.setUnit(resultSet.unit);                   //単位
            item.setSpecimenCode(resultSet.specimenCode);   //検査材料コード
            item.setSpecimenName(resultSet.specimenName);   //検査材料名称
            item.setCommentCode1(resultSet.reportCode1);    //報告コメントコード1
            item.setComment1(resultSet.reportName1);        //報告コメント名称1
            item.setCommentCode2(resultSet.reportCode2);    //報告コメントコード2
            item.setComment2(resultSet.reportName2);        //報告コメント名称2

        }

        // サマリを生成する
        List<NLaboImportSummary> retList = new ArrayList<NLaboImportSummary>();

        for (NLaboModule module : allModules) {

            NLaboImportSummary summary = new NLaboImportSummary();
//masuda^
            summary.setReportFormat(module.getReportFormat());
//masuda$
            summary.setLaboCode(module.getLaboCenterCode());
            summary.setPatientId(module.getPatientId());
            summary.setPatientName(module.getPatientName());
            summary.setPatientSex(module.getPatientSex());
            summary.setSampleDate(module.getSampleDate());
            summary.setNumOfTestItems(String.valueOf(module.getItems().size()));
            summary.setModule(module);
            retList.add(summary);
        }

        return retList;
    }

    private String getYMD(String str) {
        if (str == null) {
            return str;
        }
        String ymd = str.replace("-", "").replace("/", "");
        if (ymd.length() != 8) {
            return str;
        }
        String year = ymd.substring(0, 4);
        String month = ymd.substring(4, 6);
        String day = ymd.substring(6, 8);
        StringBuilder sb = new StringBuilder();
        sb.append(year).append("-");
        sb.append(month).append("-");
        sb.append(day);
        return sb.toString();
    }
}

class CodeMap {

    // 検体
    static final HashMap<String, String> specimenCodeMap = new HashMap<String, String>();
    // 分析物
    static final HashMap<String, String> analyteCodeMap = new HashMap<String, String>();

    // 項目の親コード・子コードのキャッシュ　分画関連
    static HashMap<String, String> parentCodeMap = new HashMap<String, String>();


    static {
        // 検体
        specimenCodeMap.put("001", "尿(含むその他)");
        specimenCodeMap.put("002", "自然排尿");
        specimenCodeMap.put("003", "新鮮尿");
        specimenCodeMap.put("004", "蓄尿");
        specimenCodeMap.put("005", "時間尿");
        specimenCodeMap.put("006", "早朝尿");
        specimenCodeMap.put("007", "負荷後尿");
        specimenCodeMap.put("008", "分杯尿");
        specimenCodeMap.put("009", "カテーテル採取尿");
        specimenCodeMap.put("010", "尿ろ紙");
        specimenCodeMap.put("011", "膀胱穿刺");
        specimenCodeMap.put("012", "動物尿");
        specimenCodeMap.put("015", "便");
        specimenCodeMap.put("017", "血液(含むその他)");
        specimenCodeMap.put("018", "全血");
        specimenCodeMap.put("019", "全血(添加物入り)");
        specimenCodeMap.put("020", "動脈血");
        specimenCodeMap.put("021", "毛細管血");
        specimenCodeMap.put("022", "血漿");
        specimenCodeMap.put("023", "血清");
        specimenCodeMap.put("024", "血球浮遊液");
        specimenCodeMap.put("025", "赤血球");
        specimenCodeMap.put("026", "リンパ球");
        specimenCodeMap.put("027", "血小板");
        specimenCodeMap.put("028", "白血球");
        specimenCodeMap.put("029", "臍帯血");
        specimenCodeMap.put("030", "溶血液");
        specimenCodeMap.put("031", "除タンパク液");
        specimenCodeMap.put("032", "血液抽出液");
        specimenCodeMap.put("033", "血液ろ紙");
        specimenCodeMap.put("034", "血液塗抹標本");
        specimenCodeMap.put("036", "動物血");
        specimenCodeMap.put("037", "動物全血");
        specimenCodeMap.put("038", "動物血漿");
        specimenCodeMap.put("039", "動物血清");
        specimenCodeMap.put("040", "穿刺液(含むその他)");
        specimenCodeMap.put("041", "髄液");
        specimenCodeMap.put("042", "胸水");
        specimenCodeMap.put("043", "腹水");
        specimenCodeMap.put("044", "関節液");
        specimenCodeMap.put("045", "心嚢液");
        specimenCodeMap.put("046", "骨髄液");
        specimenCodeMap.put("047", "羊水");
        specimenCodeMap.put("048", "腰椎");
        specimenCodeMap.put("049", "骨髄塗抹標本");
        specimenCodeMap.put("050", "分泌液(含むその他)");
        specimenCodeMap.put("051", "消化器系からの分泌液");
        specimenCodeMap.put("052", "胃液");
        specimenCodeMap.put("053", "十二指腸液");
        specimenCodeMap.put("054", "胆汁");
        specimenCodeMap.put("055", "膵液");
        specimenCodeMap.put("056", "唾液");
        specimenCodeMap.put("059", "前立腺液");
        specimenCodeMap.put("060", "精液");
        specimenCodeMap.put("061", "喀痰");
        specimenCodeMap.put("062", "乳汁");
        specimenCodeMap.put("063", "鼻汁");
        specimenCodeMap.put("064", "咽喉からの分泌液");
        specimenCodeMap.put("065", "耳からの分泌液");
        specimenCodeMap.put("066", "目からの分泌液");
        specimenCodeMap.put("067", "膣からの分泌液");
        specimenCodeMap.put("068", "皮膚からの分泌液(汗)");
        specimenCodeMap.put("069", "気管からの分泌液");
        specimenCodeMap.put("070", "組織*(含むその他)");
        specimenCodeMap.put("071", "生検組織*");
        specimenCodeMap.put("072", "試験切除組織*");
        specimenCodeMap.put("073", "手術切除組織*");
        specimenCodeMap.put("074", "剖検切除組織*");
        specimenCodeMap.put("075", "固定組織*");
        specimenCodeMap.put("077", "毛髪");
        specimenCodeMap.put("078", "爪");
        specimenCodeMap.put("080", "菌株");
        specimenCodeMap.put("081", "結石(含むその他)");
        specimenCodeMap.put("082", "尿路系結石");
        specimenCodeMap.put("083", "胆石");
        specimenCodeMap.put("085", "擦過物");
        specimenCodeMap.put("086", "膿(含むその他)");
        specimenCodeMap.put("087", "開放性の膿");
        specimenCodeMap.put("088", "非開放性の膿");
        specimenCodeMap.put("089", "水泡内容物");
        specimenCodeMap.put("090", "嘔吐物");
        specimenCodeMap.put("091", "洗浄液");
        specimenCodeMap.put("092", "血液以外の抽出液");
        specimenCodeMap.put("093", "浸出液");
        specimenCodeMap.put("094", "塗抹標本(血液、骨髄以外)");
        specimenCodeMap.put("095", "透析液");
        specimenCodeMap.put("096", "かん流液");
        specimenCodeMap.put("097", "培養液");
        specimenCodeMap.put("098", "ペア材料");
        specimenCodeMap.put("099", "その他の材料");

        // 分析物
        analyteCodeMap.put("1A", "一般検査/尿一般検査");
        analyteCodeMap.put("1B", "一般検査/糞便検査");
        analyteCodeMap.put("1C", "一般検査/髄液検査");
        analyteCodeMap.put("1Z", "一般検査/その他");
        analyteCodeMap.put("2A", "血液学的検査/血液一般・形態検査");
        analyteCodeMap.put("2B", "血液学的検査/凝固・線溶関連検査");
        analyteCodeMap.put("2C", "血液学的検査/血液化学検査");
        analyteCodeMap.put("2Z", "血液学的検査/その他");
        analyteCodeMap.put("3A", "生化学的検査/蛋白・膠質反応");
        analyteCodeMap.put("3B", "生化学的検査/酵素および関連物質");
        analyteCodeMap.put("3C", "生化学的検査/低分子窒素化合物");
        analyteCodeMap.put("3D", "生化学的検査/糖質および関連物質");
        analyteCodeMap.put("3E", "生化学的検査/有機酸");
        analyteCodeMap.put("3F", "生化学的検査/脂質および関連物質");
        analyteCodeMap.put("3G", "生化学的検査/ビタミンおよび関連物質");
        analyteCodeMap.put("3H", "生化学的検査/電解質・血液ガス");
        analyteCodeMap.put("3I", "生化学的検査/生体微量金属");
        analyteCodeMap.put("3J", "生化学的検査/生体色素関連物質");
        analyteCodeMap.put("3K", "生化学的検査/毒物・産業医学的代謝物質");
        analyteCodeMap.put("3L", "生化学的検査/薬物");
        analyteCodeMap.put("3M", "生化学的検査/薬物");
        analyteCodeMap.put("3Z", "生化学的検査/その他");
        analyteCodeMap.put("4A", "内分泌学的検査/視床下部・下垂体ホルモン");
        analyteCodeMap.put("4B", "内分泌学的検査/甲状腺ホルモンおよび結合蛋白");
        analyteCodeMap.put("4C", "内分泌学的検査/副甲状腺ホルモン");
        analyteCodeMap.put("4D", "内分泌学的検査/副甲状腺ホルモンおよび結合蛋白");
        analyteCodeMap.put("4E", "内分泌学的検査/副腎髄質ホルモン");
        analyteCodeMap.put("4F", "内分泌学的検査/性腺・胎盤ホルモンおよび結合蛋白");
        analyteCodeMap.put("4G", "内分泌学的検査/膵・消化管ホルモン");
        analyteCodeMap.put("4H", "内分泌学的検査/ホルモン受容体");
        analyteCodeMap.put("4Z", "内分泌学的検査/その他");
        analyteCodeMap.put("5A", "免疫学的検査/免疫グロブリン");
        analyteCodeMap.put("5B", "免疫学的検査/補体および関連物質");
        analyteCodeMap.put("5C", "免疫学的検査/血漿蛋白");
        analyteCodeMap.put("5D", "免疫学的検査/腫瘍関連抗原");
        analyteCodeMap.put("5E", "免疫学的検査/感染症(非ウイルス)関連検査");
        analyteCodeMap.put("5F", "免疫学的検査/ウイルス感染症検査");
        analyteCodeMap.put("5G", "免疫学的検査/自己免疫関連検査");
        analyteCodeMap.put("5H", "免疫学的検査/免疫血液学的検査");
        analyteCodeMap.put("5I", "免疫学的検査/細胞性免疫検査");
        analyteCodeMap.put("5J", "免疫学的検査/サイトカイン");
        analyteCodeMap.put("5K", "免疫学的検査/HLA");
        analyteCodeMap.put("5Z", "免疫学的検査/その他");
        analyteCodeMap.put("6A", "微生物学的検査/塗抹・形態検査");
        analyteCodeMap.put("6B", "微生物学的検査/培養同定検査");
        analyteCodeMap.put("6C", "微生物学的検査/薬剤感受性検査");
        analyteCodeMap.put("6Z", "微生物学的検査/その他");
        analyteCodeMap.put("7A", "病理学的検査/細胞診検査");
        analyteCodeMap.put("7B", "病理学的検査/病理組織検査");
        analyteCodeMap.put("7C", "病理学的検査/迅速凍結組織検査");
        analyteCodeMap.put("7D", "病理学的検査/電子顕微鏡検査");
        analyteCodeMap.put("7Z", "病理学的検査/その他");
        analyteCodeMap.put("8A", "その他の検体検査/負荷試験・機能検査");
        analyteCodeMap.put("8B", "その他の検体検査/遺伝子関連検査(染色体)");
        analyteCodeMap.put("8C", "その他の検体検査/遺伝子関連検査");
        analyteCodeMap.put("8Z", "その他の検体検査/その他");
        analyteCodeMap.put("9A", "生理機能検査/循環器機能検査");
        analyteCodeMap.put("9B", "生理機能検査/脳・神経機能検査");
        analyteCodeMap.put("9C", "生理機能検査/呼吸機能検査");
        analyteCodeMap.put("9D", "生理機能検査/前庭・聴力機能検査");
        analyteCodeMap.put("9E", "生理機能検査/眼科関連機能検査");
        analyteCodeMap.put("9F", "生理機能検査/超音波検査");
        analyteCodeMap.put("9N", "生理機能検査/健診関連");
        analyteCodeMap.put("9Z", "生理機能検査/その他");
    }
}

// ResultSet
class HL7ResultSet {

    String studyCo;             //検査会社名
    String receptionIns;        //受信施設名
    String karteNo;             //カルテ番号
    String studyDate;           //採取日
    String patientNameKANA;     //カナ患者名
    String patientNameKANJI;    //漢字患者名
    String patientBirthdate;    //生年月日
    String patientAge;          //年齢
    String sex;                 //性別
    String patientDiv;          //患者区分
    String medicalAtt;          //担当医
    String nyubi;               //乳ビ
    String hemolysis;           //溶血
    String dialysisBA;          //透析前後
    String operationBA;         //手術前後　項目にはない
    String reportCode;          //報告状況コード
    String groupCode;           //グループコード
    String groupName;           //グループ名称
    String studyCodeP;          //検査項目コード・親
    String studyCode;           //検査項目コード
    String medisCode;           //MEDISコード
    String studyNickname;       //検査項目略称
    String studyName;           //検査項目名
    String abnormaldiv;         //異常区分
    String standardval;         //基準値
    String studyResult;         //検査結果
    String unit;                //単位
    String specimenCode;        //検査材料コード
    String specimenName;        //検査材料名称
    String reportCode1;         //報告コメントコード1
    String reportName1;         //報告コメント名称1
    String reportCode2;         //報告コメントコード2
    String reportName2;         //報告コメント名称2
    String number;
    String rType;               //検査結果タイプ
    String fktest;              //副成分取り出しテスト
}

//MSH
class Hl7MSH {

    String studyCo;             //検査会社名     <-送信アプリケーション
    String receptionIns;        //受信施設名     <-受信アプリケーション？ masuda

    public Hl7MSH setParam() {
        studyCo         = "3.0";
        receptionIns    = "5.0";
        return this;
    }
}

//PID
class Hl7PID {

    String karteNo;             //カルテ番号
    String patientNameKANA;     //カナ患者名
    String patientNameKANJI;    //漢字患者名
    String patientBirthdate;    //生年月日
    String patientAge;          //年齢
    String sex;                 //性別
    String patientNameAlias;    //患者別名  added by masuda

    public Hl7PID setParam() {
        karteNo             = "3.0";
        patientNameKANA     = "5.0.1.~.0";
        patientNameKANJI    = "5.0.1.~.1";
        patientBirthdate    = "7.0";
        patientAge          = "7.2";
        sex                 = "8.0";
        patientNameAlias    = "9.0";
        return this;
    }
}

//PV1
class Hl7PV1 {

    String patientDiv;          //患者区分
    String medicalAtt;          //担当医

    public Hl7PV1 setParam() {
        patientDiv = "2.0";
        medicalAtt = "7.0";
        return this;
    }
}

//NTE
class Hl7NTE {

    String reportCode1;     // コメント１
    String reportName1;

    public Hl7NTE setParam() {
        reportCode1 = "3.0";
        reportName1 = "3.1";
        return this;
    }
}

//OBR
class Hl7OBR {

    String dialysisBA;          //透析前後              <- End Dateでは？
    String operationBA;         //手術前後　項目にはない <- Priorityでは？ masuda
//masuda^
    String fillerField;         // 実施者フィールド
    String requestDate;         // 要求日時
    String observationDate;     // 検査／採取日時
    String specimenCode;        // 検査材料
    String specimenName;        // 検体種類名称
//masuda$

    public Hl7OBR setParam() {

        dialysisBA      = "27.4";
        operationBA     = "27.5";
//masuda^
        fillerField     = "20.0";   // 実施者フィールド
        requestDate     = "6.0";    // 要求日時
        observationDate = "7.0";    // 検査／採取日時
        specimenCode    = "15.0";   // 検査材料
        specimenName    = "15.1";   // 検体種類名称
//masuda$
        return this;
    }
}

//OBX
class Hl7OBX {

    String studyDate;           //採取日
    String nyubi;               //乳ビ
    String hemolysis;           //溶血
    //String dialysisBA;        //透析前後
    //String OperationBA;       //手術前後　項目にはない
    String reportCode;          //報告状況コード
    String groupCode;           //グループコード
    String groupName;           //グループ名称
    String studyCodeP;          //検査項目コード・親
    String studyCode;           //検査項目コード
    String medisCode;           //MEDISコード
    String studyNickname;       //検査項目略称
    String studyName;           //検査項目名
    String abnormaldiv;         //異常区分
    String standardval;         //基準値
    String studyResult;         //検査結果（NM,SN,RP,CWE)
    String unit;                //単位
    String inspectionMate;      //検査材料コード
    String inspectionName;      //検査材料名称
    String reportCode1;         //報告コメントコード1
    String reportName1;         //報告コメント名称1
    String reportCode2;         //報告コメントコード2
    String reportName2;         //報告コメント名称2
    String number;
    String rType;
    String cweStudyResult;
    String snStudyResult;
    String stStudyResult;
    String nmStudyResult;
    String rpStudyResult;
    //
    String fktest;              //副成分取り出しテスト

//masuda^
    String studyCo;             // 検査会社
    String codingSystem;        // <name of coding system (IS)> 分画区分
    String alternateID;         // <alternate identifier (ST)>
    final String CS_SINGLE = "0";   // 単独の項目
    final String CS_PARENT = "2";   // 分画の親
    final String CS_CHILD  = "1";   // 分画の子
//masuda$

    public Hl7OBX setParam() {

        studyDate       = "14.0";
        nyubi           = null;
        hemolysis       = null;
        //dialysisBA    = "27.2";
        //OperationBA   = "24.3";
        groupCode       = null;
        groupName       = null;
        studyCodeP      = null;

//masuda^   和歌山市医師会成人病センター
        //reportCode    = null;
        //studyCode     = "3.3";
        //medisCode     = "3.3";
        //abnormaldiv   = null;
        reportCode      = "11.0";
        studyCode       = "3.0";
        medisCode       = "3.0";
        abnormaldiv     = "8.0";
        codingSystem    = "3.2";
        alternateID     = "3.3";
        studyCo         = "15.0";

        studyNickname   = "3.4";    // NickNameとName逆ｗ
        studyName       = "3.1";
//masuda$
        standardval     = "7.0";
        // NM,SN,RP,CWE
        nmStudyResult   = "5.0";
        snStudyResult   = "5.0";
        stStudyResult   = "5.0";
        rpStudyResult   = "5.0"; //?????
        cweStudyResult  = "5.0";
        //
        unit            = "6.1";
        inspectionMate  = null;
        inspectionName  = null;
        reportCode1     = null;
        reportName1     = null;
        reportCode2     = null;
        reportName2     = null;
        number          = "1.0";
        rType           = "2.0";
        fktest          = "3.6.&.2";

        return this;
    }
}

class Hl7 {

    //Defaut
    private char m_fSep = '|';  //フィールド
    private static final char m_cSep = '^';  //成分
    private static final char n_rSep = '~';  //反復
    private static final char m_scSep = '&';  //副成分

    private boolean isNull(String str) {
        //System.out.println(str);
        return str == null || "".equals(str.trim());
    }

    //MSHセグメント
    private Hl7MSH getHl7MSH(String line) {

        Hl7MSH param = new Hl7MSH().setParam();
        Hl7MSH ret = new Hl7MSH();

        try {
            m_fSep = line.charAt(3);
            //検査会社名
            ret.studyCo = getHL7ItemString(line, param.studyCo);
            //受信施設名
            ret.receptionIns = getHL7ItemString(line, param.receptionIns);
        } catch (Exception ex) {
            System.out.println("例外" + ex + "が発生しました");
        }

        return ret;
    }

    //PIDセグメント
    private Hl7PID getHl7PID(String line) {

        Hl7PID param = new Hl7PID().setParam();
        Hl7PID ret = new Hl7PID();

        try {
            ret.karteNo = getHL7ItemString(line, param.karteNo);
            ret.patientNameKANA = getHL7ItemString(line, param.patientNameKANA);
            ret.patientNameKANJI = getHL7ItemString(line, param.patientNameKANJI);
            ret.patientBirthdate = getHL7ItemString(line, param.patientBirthdate);
            ret.patientAge = getHL7ItemString(line, param.patientAge);
            ret.sex = getHL7ItemString(line, param.sex);
//masuda^   和歌山市医師会成人病センター
            if (isNull(ret.patientNameKANJI) || isNull(ret.patientNameKANA)) {
                String val = getHL7ItemString(line, param.patientNameAlias);
                ret.patientNameKANA = val;
                ret.patientNameKANJI = val;
            }
//masuda$
        } catch (Exception ex) {
            System.out.println("例外" + ex + "が発生しました");
        }

        return ret;
    }

//masuda^
    //NTEセグメント
    private Hl7NTE getHl7NTE(String line) {
        Hl7NTE param = new Hl7NTE().setParam();
        Hl7NTE ret = new Hl7NTE();
        try {
            ret.reportCode1 = getHL7ItemString(line, param.reportCode1);
            ret.reportName1 = getHL7ItemString(line, param.reportName1);
        } catch (Exception ex) {
            System.out.println("例外" + ex + "が発生しました");
        }

        return ret;
    }
//masuda$

    //PV1セグメント
    private Hl7PV1 getHl7PV1(String line) {

        Hl7PV1 param = new Hl7PV1().setParam();
        Hl7PV1 ret = new Hl7PV1();
        try {
            ret.patientDiv = getHL7ItemString(line, param.patientDiv);
            ret.medicalAtt = getHL7ItemString(line, param.medicalAtt);
        } catch (Exception ex) {
            System.out.println("例外" + ex + "が発生しました");
        }

        return ret;
    }

    //OBRセグメント
    private Hl7OBR getHl7OBR(String line) {

        Hl7OBR param = new Hl7OBR().setParam();
        Hl7OBR ret = new Hl7OBR();

        try {
//masuda^
            //ret.dialysisBA = getHL7ItemString(line, param.dialysisBA);
            //ret.operationBA = getHL7ItemString(line, param.operationBA);
            ret.specimenCode = getHL7ItemString(line, param.specimenCode);
            ret.observationDate = getHL7ItemString(line, param.observationDate);
            ret.fillerField = getHL7ItemString(line, param.fillerField);
//masuda$
        } catch (Exception ex) {
            System.out.println("例外" + ex + "が発生しました");
        }

        return ret;
    }

    //OBXセグメント
    private Hl7OBX getHl7OBX(String line) {

        Hl7OBX param = new Hl7OBX().setParam();
        Hl7OBX ret = new Hl7OBX();

        try {
            ret.fktest = getHL7ItemString(line, param.fktest);
            ret.number = getHL7ItemString(line, param.number);
            //採取日
            ret.studyDate = getHL7ItemString(line, param.studyDate);
            //検査コード
            ret.studyCode = getHL7ItemString(line, param.studyCode);
            //検査項目略称
            ret.studyNickname = getHL7ItemString(line, param.studyNickname);
            //検査項目名称
            ret.studyName = getHL7ItemString(line, param.studyName);
            // ＭＥＤＩＳコード
            ret.medisCode = getHL7ItemString(line, param.medisCode);
            //基準値
            ret.standardval = getHL7ItemString(line, param.standardval);
            //単位名称
            ret.unit = getHL7ItemString(line, param.unit);
            //結果タイプ
            ret.rType = getHL7ItemString(line, param.rType);
            if (ret.rType.equals("ST")) {
                ret.studyResult = getHL7ItemString(line, param.stStudyResult);
            } else if (ret.rType.equals("NM")) {
                ret.studyResult = getHL7ItemString(line, param.nmStudyResult);
            } else if (ret.rType.equals("SN")) {
                ret.studyResult = getHL7ItemString(line, param.snStudyResult);
            } else if (ret.rType.equals("CWE")) {
                ret.studyResult = getHL7ItemString(line, param.cweStudyResult);
            } else if (ret.rType.equals("RP")) {
                ret.studyResult = "????????";
            }
//masuda^
            // 検査結果状態
            ret.reportCode = getHL7ItemString(line, param.reportCode);
            // 異常値
            ret.abnormaldiv = getHL7ItemString(line, param.abnormaldiv);
            // グループコードはないので分析物コードで代用
            String group = ret.medisCode.substring(0, 2);
            ret.groupCode = group;
            ret.groupName = CodeMap.analyteCodeMap.get(group);
            // 検査会社
            ret.studyCo = getHL7ItemString(line, param.studyCo);

            // 検査項目コード・親の処理
            String is = getHL7ItemString(line, param.codingSystem);
            String st = getHL7ItemString(line, param.alternateID);

            if (ret.CS_SINGLE.equals(is)) {
                ret.studyCodeP = ret.studyCode;
            } else if (ret.CS_PARENT.equals(is)) {
                CodeMap.parentCodeMap.put(st, ret.studyCode);
                ret.studyCodeP = ret.studyCode;
            } else if (ret.CS_CHILD.equals(is)) {
                ret.studyCodeP = CodeMap.parentCodeMap.get(st);
            }

            if (isNull(ret.studyName)) {
                ret.studyName = ret.studyNickname;
            }
//masuda$
        } catch (Exception ex) {
            System.out.println("例外" + ex + "が発生しました");
        }

        return ret;
    }

    public List<HL7ResultSet> getHL7ResultSets(String fname) {

        int pIdCount = 0;
        String line;
        List<HL7ResultSet> retList = new ArrayList<HL7ResultSet>();
        BufferedReader br = null;
        Hl7MSH hl7MSH = null;
        Hl7PID hl7PID = null;
        Hl7PV1 hl7PV1 = null;
        Hl7OBR hl7OBR = null;
        Hl7OBX hl7OBX = null;
//masuda^
        Hl7NTE hl7NTE = null;
//masuda$
        CodeMap.parentCodeMap.clear();

        try {
//masuda^
            String encoding = CharsetDetector.getFileEncoding(fname);
            if (!(CharsetDetector.UTF8.equals(encoding))) {
                encoding = CharsetDetector.SJIS;
            }
            FileInputStream is = new FileInputStream(fname);
            InputStreamReader in = new InputStreamReader(is, encoding);
            br = new BufferedReader(in);
            //br = new BufferedReader(new FileReader(fname));
//masuda$
            while ((line = br.readLine()) != null) {
                String adt = line.substring(0, 3);
                if (adt.equals("MSH")) {
                    hl7MSH = getHl7MSH(line);
                }
                break;
            }
            HL7ResultSet previousSet = null;
            while ((line = br.readLine()) != null) {
                String adt = line.substring(0, 3);
                if (adt.equals("PV1")) {
                    hl7PV1 = getHl7PV1(line);
                    continue;
                }
                if (adt.equals("IN1")) {
                    continue;
                }
                if (adt.equals("ORC")) {
                    continue;
                }
                if (adt.equals("OBR")) {
                    //透析前後
                    // 検体コード、検査／採取日時
                    hl7OBR = getHl7OBR(line);
                    continue;
                }
                if (adt.equals("PID")) {
                    hl7PID = getHl7PID(line);
                    pIdCount++;
                    continue;
                }
//masuda^
                if (adt.equals("NTE") && previousSet != null) {
                    hl7NTE = getHl7NTE(line);
                    previousSet.reportCode1 = hl7NTE.reportCode1;
                    previousSet.reportName1 = hl7NTE.reportName1;
                    continue;
                }
                if (!adt.equals("OBX")) {
                    continue;
                }
//masuda$
                HL7ResultSet resultSet = new HL7ResultSet();
                previousSet = resultSet;
                resultSet.studyCo = hl7MSH.studyCo;                     //検査会社名
                resultSet.receptionIns = hl7MSH.receptionIns;           //受信施設
                resultSet.dialysisBA = hl7OBR.dialysisBA;               //透析前後
                resultSet.operationBA = hl7OBR.operationBA;
                resultSet.karteNo = hl7PID.karteNo;                     //患者ＩＤ（カルテＮｏ）
                resultSet.patientNameKANA = hl7PID.patientNameKANA;     //患者氏名カナ
                resultSet.patientNameKANJI = hl7PID.patientNameKANJI;   //患者氏名漢字
                resultSet.patientBirthdate = hl7PID.patientBirthdate;   //生年月日
                resultSet.patientAge = hl7PID.patientAge;               //年齢
                resultSet.sex = hl7PID.sex;                             //性別
//masuda^   和歌山市医師会成人病センターのデータにはPV1がない
                if (hl7PV1 != null) {
                    resultSet.patientDiv = hl7PV1.patientDiv;           //患者区分
                    resultSet.medicalAtt = hl7PV1.medicalAtt;           //担当医
                }
                // 検体
                String sCode = hl7OBR.specimenCode;
                resultSet.specimenCode = sCode;
                // FALCO徳島対応
                String specimenName = CodeMap.specimenCodeMap.get(sCode);
                if (specimenName == null) {
                    specimenName = hl7OBR.specimenName;
                }
                resultSet.specimenName = specimenName;
                // 採取日
                resultSet.studyDate = hl7OBR.observationDate;
                // 実施者フィールド
                resultSet.studyCo = hl7OBR.fillerField;
//masuda$

                if (adt.equals("OBX")) {
                    hl7OBX = getHl7OBX(line);
                    resultSet.fktest = hl7OBX.fktest;
                    resultSet.number = hl7OBX.number;
                    //採取日
//masuda^   採取日はOBRのobservationDateを使う
                    //resultSet.studyDate = hl7OBX.studyDate;
                    // CWEは無視    FALCO徳島
                    if ("CWE".equals(hl7OBX.rType)) {
                        continue;
                    }
//masuda$
                    //検査コード
                    resultSet.studyCode = hl7OBX.studyCode;
                    //検査項目略称
                    resultSet.studyNickname = hl7OBX.studyNickname;
                    //検査項目名称
                    resultSet.studyName = hl7OBX.studyName;
                    // ＭＥＤＩＳコード
                    resultSet.medisCode = hl7OBX.medisCode;
                    resultSet.unit = hl7OBX.unit;
                    resultSet.standardval = hl7OBX.standardval;
                    //結果タイプ
                    resultSet.rType = hl7OBX.rType;
                    //検査結果
                    resultSet.studyResult = hl7OBX.studyResult;
//masuda^
                    //和歌山市医師会成人病センターのデータではMSHに検査会社名が入っていない
                    if (isNull(resultSet.studyCo)) {
                        resultSet.studyCo = hl7OBX.studyCo;
                    }
                    // グループ
                    resultSet.groupCode = hl7OBX.groupCode;
                    resultSet.groupName = hl7OBX.groupName;
                    // 親コード 親コードがない場合 studyCode を設定する
                    String parentCode = hl7OBX.studyCodeP;
                    if (parentCode == null) {
                        parentCode = hl7OBX.studyCode;
                    }
                    resultSet.studyCodeP = parentCode;
                    // 異常区分
                    resultSet.abnormaldiv = hl7OBX.abnormaldiv;
                    // 検査結果状態
                    resultSet.reportCode = hl7OBX.reportCode;
//masuda$
                }

                retList.add(resultSet);

            }
        //System.out.println(PIDCount);

        } catch (IOException ex) {
            System.out.println("ファイル例外" + ex + "が発生しました");
        } catch (Exception ex) {
            System.out.println("その他例外" + ex + "が発生しました");
            System.out.println(ex.getStackTrace());
        } finally {
            try {
                br.close();
            } catch (Exception ex) {
            }
        }

        return retList;
    }

    private String getHL7ItemString(String line, String param) {

        int cnt = 0;
        int repeatNo = -1;
        int subCompositeNo = -1;

        line = line + String.valueOf(m_fSep);
        //反復チェック
        int p = param.indexOf('~');
        int pos = p;
        if (p != -1) {
            String repeat = param.substring(pos + 2);
            p = repeat.indexOf('.');
            if (p == -1) {
                repeatNo = Integer.valueOf(repeat);
            } else {
                repeat = param.substring(pos + 2, pos + 2 + p);
                repeatNo = Integer.valueOf(repeat);
            }
        }
        //副成分
        p = param.indexOf('&');
        pos = p;
        if (p != -1) {
            String subComposite = param.substring(pos + 2);
            p = subComposite.indexOf('.');
            if (p == -1) {
                subCompositeNo = Integer.valueOf(subComposite);
            }
        }

        p = param.indexOf('.');
        String fieldNoStr = param.substring(0, p);
        int fieldNo = Integer.valueOf(fieldNoStr);
        param = param.substring(p + 1);
        //パラメタ正規化
        if (repeatNo != -1) {
            p = param.indexOf('~');
            param = param.substring(0, p - 1);
        }
        if (subCompositeNo != -1) {
            p = param.indexOf('&');
            if (p != -1) {
                param = param.substring(0, p - 1);
            }
        }

        StringBuilder sb = new StringBuilder();

        try {

            p = line.indexOf('|');
            while ((pos = line.indexOf(m_fSep, p + 1)) != -1) {
                String item = line.substring(p + 1, pos);
                p = pos;
                if (++cnt == fieldNo) {
                    //反復チェック
                    for (int i = 0; i < repeatNo; i++) {
                        p = item.indexOf('~');
                        item = item.substring(p + 1);
                    }
                    //^
                    if (param == null) {
                        sb.append(item);
                        break;
                    } else {
                        p = 0;
                        String fieldStr = param;
                        while ((pos = fieldStr.indexOf('.', p)) != -1) {
                            //pos = fieldStr.indexOf('.', p);
                            String fieldItem = fieldStr.substring(p, p + 1);
                            int n = Integer.valueOf(fieldItem);
                            String subFieldStr = getHL7FactorString(item, n, m_cSep);
                            if (subFieldStr != null) {
                                //副成分
                                if (subCompositeNo != -1) {
                                    subFieldStr = getHL7FactorString(subFieldStr, subCompositeNo, m_scSep);
                                }
//masuda^   bug???
                                sb.append(subFieldStr);
                                sb.append(" ");
                            }
                            fieldStr = fieldStr.substring(pos + 1);
                        /*
                        sb.append(wsstr);
                        wstr = wstr.substring(pos + 1);
                        sb.append(" ");
                         */
//masuda$
                        }
                        int n = Integer.valueOf(fieldStr);
                        String subFieldStr = getHL7FactorString(item, n, m_cSep);
                        if (subFieldStr != null) {
                            //副成分
                            if (subCompositeNo != -1) {
                                subFieldStr = getHL7FactorString(subFieldStr, subCompositeNo, m_scSep);
                            }
                            sb.append(subFieldStr);
                        }
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("例外" + ex + "が発生しました");
        }

        return sb.toString();
    }

    private String getHL7FactorString(String line, int no, char sep) {

        int cnt = 0;
        String item = null;
        String rItem = null;

        try {

            while (true) {
                int p = 0;
                int pos = line.indexOf(sep, p);

                if (pos == 0) {
                    p++;
                    cnt++;
                    line = line.substring(p);
                    continue;
                }

                if (pos != -1) {
                    item = line.substring(p, pos);
                    p = pos;
                } else if (pos == -1) {
                    item = line.substring(p);
                }

                if (cnt++ == no) {
                    rItem = item;
                    break;
                }

                if (pos != -1) {
                    p++;
                }

                line = line.substring(p);
                if (pos == -1) {
                    break;
                }
            }

        } catch (Exception ex) {
            System.out.println("例外" + ex + "が発生しました ");
            System.out.println(ex.getStackTrace());
        }
        return rItem;
    }
}

