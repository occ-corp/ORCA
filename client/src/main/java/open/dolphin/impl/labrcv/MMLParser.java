
package open.dolphin.impl.labrcv;

import java.io.*;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import open.dolphin.client.ClientContext;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.GUIDGenerator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 * MMLParser
 *
 * @author masuda, Masuda Naika
 */

public class MMLParser implements LabResultParser {
    
    //private static final Namespace xhtml = Namespace.getNamespace("xhtml","http://www.w3.org/1999/xhtml");
    private static final Namespace mmlCm = Namespace.getNamespace("mmlCm","http://www.medxml.net/MML/SharedComponent/Common/1.0");
    //private static final Namespace mmlNm = Namespace.getNamespace("mmlNm","http://www.medxml.net/MML/SharedComponent/Name/1.0");
    //private static final Namespace mmlFc = Namespace.getNamespace("mmlFc","http://www.medxml.net/MML/SharedComponent/Facility/1.0");
    //private static final Namespace mmlDp = Namespace.getNamespace("mmlDp","http://www.medxml.net/MML/SharedComponent/Department/1.0");
    //private static final Namespace mmlAd = Namespace.getNamespace("mmlAd","http://www.medxml.net/MML/SharedComponent/Address/1.0");
    //private static final Namespace mmlPh = Namespace.getNamespace("mmlPh","http://www.medxml.net/MML/SharedComponent/Phone/1.0");
    //private static final Namespace mmlPsi = Namespace.getNamespace("mmlPsi","http://www.medxml.net/MML/SharedComponent/PersonalizedInfo/1.0");
    //private static final Namespace mmlCi = Namespace.getNamespace("mmlCi","http://www.medxml.net/MML/SharedComponent/CreatorInfo/1.0");
    //private static final Namespace mmlPi = Namespace.getNamespace("mmlPi","http://www.medxml.net/MML/ContentModule/PatientInfo/1.0");
    //private static final Namespace mmlBc = Namespace.getNamespace("mmlBc","http://www.medxml.net/MML/ContentModule/BaseClinic/1.0");
    //private static final Namespace mmlFcl = Namespace.getNamespace("mmlFcl","http://www.medxml.net/MML/ContentModule/FirstClinic/1.0");
    //private static final Namespace mmlHi = Namespace.getNamespace("mmlHi","http://www.medxml.net/MML/ContentModule/HealthInsurance/1.1");
    //private static final Namespace mmlLs = Namespace.getNamespace("mmlLs","http://www.medxml.net/MML/ContentModule/Lifestyle/1.0");
    //private static final Namespace mmlPc = Namespace.getNamespace("mmlPc","http://www.medxml.net/MML/ContentModule/ProgressCourse/1.0");
    //private static final Namespace mmlRd = Namespace.getNamespace("mmlRd","http://www.medxml.net/MML/ContentModule/RegisteredDiagnosis/1.0");
    //private static final Namespace mmlSg = Namespace.getNamespace("mmlSg","http://www.medxml.net/MML/ContentModule/Surgery/1.0");
    //private static final Namespace mmlSm = Namespace.getNamespace("mmlSm","http://www.medxml.net/MML/ContentModule/Summary/1.0");
    private static final Namespace mmlLb = Namespace.getNamespace("mmlLb","http://www.medxml.net/MML/ContentModule/test/1.0");
    //private static final Namespace mmlRp = Namespace.getNamespace("mmlRp","http://www.medxml.net/MML/ContentModule/report/1.0");
    //private static final Namespace mmlRe = Namespace.getNamespace("mmlRe","http://www.medxml.net/MML/ContentModule/Referral/1.0");
    //private static final Namespace mmlSc = Namespace.getNamespace("mmlSc","http://www.medxml.net/MML/SharedComponent/Security/1.0");
    //private static final Namespace claim = Namespace.getNamespace("claim","http://www.medxml.net/claim/claimModule/2.1");
    //private static final Namespace claimA = Namespace.getNamespace("claimA","http://www.medxml.net/claim/claimAmountModule/2.1");
    
    private String patientId;
    private String patientIdType;
    private String patientIdTypeTableId;
    private String moduleUUID;
    private String confirmDate;

    private LaboModuleValue laboModule;
    private LaboSpecimenValue laboSpecimen;
    private LaboItemValue laboItem;
    
    private NLaboModule nLaboModule;
    private NLaboItem nLaboItem;
    
    private String sampleDate;
    private String reportStatus;
    private String laboCode;
    private String specimenName;
    private String specimenCode;

    private String encoding;
    private Logger logger;
    private boolean DEBUG = false;

    private static final String MML = "MML";

    public MMLParser() {
        logger = ClientContext.getLaboTestLogger();
        DEBUG = (logger.getLevel() == Level.DEBUG);
        encoding = "SHIFT_JIS";
    }

    /**
     * 引数のMML検査結果ファイルをパースしその中に含まれる
     * 検査結果モジュールのリストを返す。
     * @param file MML検査結果ファイル
     * @return パースしたモジュール LaboModuleValue のリスト
     */
    @Override
    public List<NLaboImportSummary> parse(File labFile) throws IOException {

        if (labFile == null ) {
            return null;
        }
        
        try {
            String name = labFile.getName();
            logger.info(name + " のパースを開始します");

//masuda^
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(labFile), encoding));
            //encoding = CharsetDetector.getEncoding(labFile.getPath());
            //FileInputStream is = new FileInputStream(labFile);
            //InputStreamReader in = new InputStreamReader(is, encoding);
            //BufferedReader reader = new BufferedReader(in);
//masuda$
            parse(reader);
            reader.close();
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            logger.warn("Exception while building LaboModules" + e.toString());
        }

        // サマリを生成する
        NLaboImportSummary summary = new NLaboImportSummary();
        summary.setReportFormat(MML);
        summary.setLaboCode(nLaboModule.getLaboCenterCode());
        summary.setPatientId(nLaboModule.getPatientId());
        summary.setPatientName(nLaboModule.getPatientName());
        summary.setPatientSex(nLaboModule.getPatientSex());
        summary.setSampleDate(nLaboModule.getSampleDate());
        summary.setNumOfTestItems(String.valueOf(nLaboModule.getItems().size()));
        summary.setModule(nLaboModule);
        summary.setLabo13(laboModule);

        return Collections.singletonList(summary);
    }
    
    /**
     * 入力ストリームの検査結果をパースする。
     */
    public void parse(BufferedReader reader) throws IOException, Exception {

        SAXBuilder docBuilder = new SAXBuilder();
        Document doc = docBuilder.build(reader);
        Element root = doc.getRootElement();

        // Headerをパースする
        parseHeader(root.getChild("MmlHeader"));

        // Bodyをパースする
        parseBody(root.getChild("MmlBody"));

    }
    
    /**
     * MMLヘッダーをパースする。
     * 取得するのは MasterIdの mmlCm:Id 患者IDのみ。
     * @param header ヘッダー要素
     */
    private void parseHeader(Element header) {
        
        Element masterIdElement = header.getChild("masterId");
        Element id = masterIdElement.getChild("Id", mmlCm);
        if (id == null) {
            logger.info("id is null");
        }
        
        patientId = id.getTextTrim();
        patientIdType = id.getAttributeValue("type", mmlCm);
        patientIdTypeTableId = id.getAttributeValue("tableId", mmlCm);
        
        logger.debug("patientId = " + patientId);
        logger.debug("patientIdType = " + patientIdType);
        logger.debug("patientId TableId = " + patientIdTypeTableId);
    }
    
    /**
     * MML Bodyをパースする。
     * @param body Body要素
     */
    private void parseBody(Element body) {
        
        // MmlModuleItem のリストを得る
        List children = body.getChildren("MmlModuleItem");

        // それをイテレートする
        for (Iterator iterator = children.iterator(); iterator.hasNext();) {
            
            Element moduleItem = (Element) iterator.next();
            
            // ModuleItem = docInfo + content なので夫々の要素を得る
            Element docInfo = moduleItem.getChild("docInfo");
            Element content = moduleItem.getChild("content");
            
            // docInfo の contentModuleType を調べる
            String attr = docInfo.getAttributeValue("contentModuleType");
            
            // LaboTest Module のみをパースする
            if (attr.equals("test")) {
                
                // uuid を取得する
                moduleUUID = docInfo.getChild("docId").getChildTextTrim("uid");
                logger.debug("module UUID = " + moduleUUID);
                if (moduleUUID == null || moduleUUID.length() != 32) {
                    moduleUUID = GUIDGenerator.generate(this);
                    logger.debug("Changed module UUID to " + moduleUUID);
                }
                
                // 確定日を取得する
                confirmDate = docInfo.getChildTextTrim("confirmDate");
                logger.debug("confirmDate = " + confirmDate);
                int tIndex = confirmDate.indexOf("T");
                if (tIndex < 0) {
                    confirmDate += "T00:00:00";
                    logger.debug("Changed confirmDate to " + confirmDate);
                }
                
                // 解析するモジュールはmmlLb:TestModule
                Element testModule = content.getChild("TestModule", mmlLb);
                
                // この要素に対応するオブジェクトを生成し、リストへ加える
                laboModule = new LaboModuleValue();
                
                // これまでに取得した基本情報を設定する
                // 患者ID、ModuleUUID、確定日を設定する
                laboModule.setUserModel(Project.getUserModel());
                laboModule.setPatientId(patientId);
                laboModule.setPatientIdType(patientIdType);
                laboModule.setPatientIdTypeCodeSys(patientIdTypeTableId);
                laboModule.setDocId(moduleUUID);
                
                // 確定日、適合開始日、記録日を設定する
                Date confirmed = ModelUtils.getDateTimeAsObject(confirmDate);
                laboModule.setConfirmed(confirmed);
                laboModule.setStarted(confirmed);
                laboModule.setRecorded(new Date());
                laboModule.setStatus("F");
                
                // NLaboのこの要素に対応するオブジェクトを生成
                nLaboModule = new NLaboModule();
                // 患者情報を設定する。MMLには患者名はないんだよなぁ… masuda
                nLaboModule.setPatientId(patientId);
                nLaboModule.setPatientName("");
                nLaboModule.setPatientSex("");

                // 要素をパースする
                parseTestModule(testModule);

            }
        } 
    }
    
    /**
     * Content要素をパースする。
     * クライアント情報、ラボセンター情報、検体情報、検査項目情報を取得する。
     * @param content 検査結果があるコンテント要素
     */
    private void parseTestModule(Element testModule) {
        
        // コンテント要素の子を列挙する
        List children = testModule.getChildren();
        Iterator iterator = children.iterator();
        
        while (iterator.hasNext()) {
            
            Element child = (Element) iterator.next();
            //String ename = child.getName();
            String qname = child.getQualifiedName();
            Namespace ns = child.getNamespace();
            debug(child.toString());
            String val = null;
            
            if (qname.equals("mmlLb:information")) {
                // mmlLb:information要素をパースする
                //logger.debug("infomation　をパース中");
                
                // 登録ID属性を取得する
                val = child.getAttributeValue("registId", ns);
                logger.debug("registId = " + val);
                laboModule.setRegistId(val);
                
                // サンプルタイム属性を取得する
                val = child.getAttributeValue("sampleTime", ns);
                logger.debug("sampleTime = " + val);
                laboModule.setSampleTime(val);
                
                int pos = val.indexOf("T");
                sampleDate = val.substring(0, pos);
                nLaboModule.setSampleDate(sampleDate);
                
                // 登録時刻属性を取得する
                val = child.getAttributeValue("registTime", ns);
                logger.debug("registTime = " + val);
                laboModule.setRegistTime(val);
                
                // 報告時間属性を取得する
                val = child.getAttributeValue("reportTime", ns);
                logger.debug("reportTime = " + val);
                laboModule.setReportTime(val);

            } else if (qname.equals("mmlLb:reportStatus")) {
                // mmlLb:reportStatus要素をパースする
                //logger.info("reportStatus　をパース中");
                
                // レポートステータスを取得する
                val = child.getTextTrim();
                logger.debug("reportStatus = " + val);
                laboModule.setReportStatus(val);
                
                // statusCodeを取得する
                val = child.getAttributeValue("statusCode", ns);
                logger.debug("statusCode = " + val);
                laboModule.setReportStatusCode(val);

                if ("final".equals(val)) {
                    reportStatus = "E";
                } else if ("mid".equals(val)) {
                    reportStatus = "M";
                } else {
                    reportStatus = null;
                }
                
                // statusCodeIdを取得する
                val = child.getAttributeValue("statusCodeId", ns);
                logger.debug("statusCodeId = " + val);
                laboModule.setReportStatusCodeId(val);
                
            } else if (qname.equals("mmlLb:facility")) {
                // クライアント施設情報をパースする
                //logger.debug("facility　をパース中");
                
                // 施設を取得する
                val = child.getTextTrim();
                logger.debug("facility = " + val);
                laboModule.setClientFacility(val);
                
                // 施設コード属性を取得する
                val = child.getAttributeValue("facilityCode", ns);
                logger.debug("facilityCode = " + val);
                laboModule.setClientFacilityCode(val);
                
                // 施設コード体系を登録する
                val = child.getAttributeValue("facilityCodeId", ns);
                logger.debug("facilityCodeId = " + val);
                laboModule.setClientFacilityCodeId(val);
                
            } else if (qname.equals("mmlLb:laboratoryCenter")) {
                // ラボセンター情報をパースする
                //logger.debug("laboratoryCenter　をパース中");
                
                // ラボセンターを取得する
                val = child.getTextTrim();
                logger.debug("laboratoryCode = " + val);
                laboModule.setLaboratoryCenter(val);
                laboCode = val;
                nLaboModule.setLaboCenterCode(laboCode);
                
                // ラボコードを取得する
                val = child.getAttributeValue("centerCode", ns);
                logger.debug("centerCode = " + val);
                laboModule.setLaboratoryCenterCode(val);
                
                // ラボコード体系を取得する
                val = child.getAttributeValue("centerCodeId", ns);
                logger.debug("centerCodeId = " + val);
                laboModule.setLaboratoryCenterCodeId(val);
                
            } else if (qname.equals("mmlLb:laboTest")) {
                // labotest要素をパースする
                //logger.debug("labotest　をパース中");
                
            } else if (qname.equals("mmlLb:specimen")) {
                // 検体情報をパースする
                //logger.debug("specimen　をパース中");
                laboSpecimen = new LaboSpecimenValue();
                laboModule.addLaboSpecimen(laboSpecimen);
                laboSpecimen.setLaboModule(laboModule);	// 関係を設定する
                
                specimenName = "";
                specimenCode = "";
                
            } else if (qname.equals("mmlLb:specimenName")) {
                // 検体名を取得する
                //logger.debug("specimenName　をパース中");
                val = child.getTextTrim();
                logger.debug("specimenName = " + val);
                laboSpecimen.setSpecimenName(val);
                specimenName = val;
                
                // spCodeを取得する
                val = child.getAttributeValue("spCode", ns);
                logger.debug("spCode = " + val);
                laboSpecimen.setSpecimenCode(val);
                specimenCode = val;
                
                // spCodeIdを取得する
                val = child.getAttributeValue("spCodeId", ns);
                logger.debug("spCodeId = " + val);
                laboSpecimen.setSpecimenCodeId(val);
                
            } else if (qname.equals("mmlLb:item")) {
                // 検査項目をパースする
                //logger.debug("item　をパース中");
                laboItem = new LaboItemValue();
                //laboItem.setId(GUIDGenerator.generate(laboItem)); // EJB3.0で変更
                laboSpecimen.addLaboItem(laboItem);
                laboItem.setLaboSpecimen(laboSpecimen);	// 関係を設定する
                
                nLaboItem = new NLaboItem();
                nLaboItem.setPatientId(patientId);
                //nLaboItem.setLaboCode(laboCode);
                nLaboItem.setSampleDate(getYMD(sampleDate));
                nLaboItem.setSpecimenCode(specimenCode);
                nLaboItem.setSpecimenName(specimenName);
                nLaboItem.setReportStatus(reportStatus);
                nLaboModule.addItem(nLaboItem);
                nLaboItem.setLaboModule(nLaboModule);
                
            } else if (qname.equals("mmlLb:itemName")) {
                // 検査項目名をパースする
                //logger.debug("itemName　をパース中");

                // 検査項目名を取得する
                val = child.getTextTrim();
                logger.debug("itemName = " + val);
                laboItem.setItemName(val);
                nLaboItem.setItemName(val);
                
                // 項目コードを取得する
                val = child.getAttributeValue("itCode", ns);
                logger.debug("itCode = " + val);
                laboItem.setItemCode(val);
                nLaboItem.setItemCode(val);
                nLaboItem.setParentCode(val);

                // 項目コード体系を取得する
                val = child.getAttributeValue("itCodeId", ns);
                logger.debug("itCodeId = " + val);
                laboItem.setItemCodeId(val);

                // MMLではグループコードがないっす…　生化１とか血液１とか
                nLaboItem.setGroupCode(specimenCode);
                nLaboItem.setGroupName(specimenName);

            } else if (qname.equals("mmlLb:value")) {
                // 検査値をパースする
                //logger.debug("value　をパース中");
                
                // 値を取得する
                val = child.getTextTrim();
                logger.debug("value = " + val);
                laboItem.setItemValue(val);
                nLaboItem.setValue(val);
                
            } else if (qname.equals("mmlLb:numValue")) {
                // 数値要素をパースする
                //logger.debug("numValue　をパース中");
                
                // up
                String up = child.getAttributeValue("up", ns);
                logger.debug("up = " + up);
                laboItem.setUp(val);

                // low
                String low = child.getAttributeValue("low", ns);
                logger.debug("low = " + low);
                laboItem.setLow(val);

                // normal
                val = child.getAttributeValue("normal", ns);
                laboItem.setNormal(val);
                if (up != null && low != null) {
                    val = low + "-" + up;
                    nLaboItem.setNormalValue(val);
                } else {
                    logger.debug("low = " + val);
                    nLaboItem.setNormalValue(val);
                }
                logger.debug("normal = " + val);

                // out
                val = child.getAttributeValue("out", ns);
                logger.debug("out = " + val);
                laboItem.setNout(val);
                if (!"N".equals(val)) {
                    nLaboItem.setAbnormalFlg(val);
                }

            } else if (qname.equals("mmlLb:unit")) {
                // 単位情報を取得する
                // value
                val = child.getTextTrim();
                logger.debug("unit = " + val);
                laboItem.setUnit(val);
                nLaboItem.setUnit(val); 

                // uCode
                val = child.getAttributeValue("uCode", ns);
                logger.debug("uCode = " + val);
                laboItem.setUnitCode(val);

                // uCodeId
                val = child.getAttributeValue("uCodeId", ns);
                logger.debug("uCodeId = " + val);
                laboItem.setUnitCodeId(val);
            }
            
            parseTestModule(child);
        }
    }

    private String getYMD(String str) {
        if (str == null) {
            return str;
        }
        String ymd = str.replace("-", "");
        if (ymd.length() != 8) {
            return str;
        }
        String year = ymd.substring(0, 4);
        String month = ymd.substring(4, 6);
        String day = ymd.substring(6, 8);
        StringBuilder sb = new StringBuilder();
        sb.append(year).append("/");
        sb.append(month).append("/");
        sb.append(day);
        return sb.toString();
    }

    private void debug(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }
      
}
