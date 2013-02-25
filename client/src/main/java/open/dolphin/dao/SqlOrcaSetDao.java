package open.dolphin.dao;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;

/**
 * ORCA の入力セットマスタを検索するクラス。
 *
 * @author Minagawa, Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class SqlOrcaSetDao extends SqlDaoBean {

    //private static final String S_SET = "S";
    //private static final String P_SET = "P";
    private static final String RP_KBN_START = "2";
    private static final String SHINRYO_KBN_START = ".";
    private static final int SHINRYO_KBN_LENGTH = 3;
    private static final int DEFAULT_BUNDLE_NUMBER = 1;
    private static final String KBN_RP = "220";
    private static final String KBN_RAD = "700";
    private static final String KBN_GENERAL = "999";
    
    private static final SqlOrcaSetDao instance;

    static {
        instance = new SqlOrcaSetDao();
    }
    
    private SqlOrcaSetDao() {
    }

    public static SqlOrcaSetDao getInstance() {
        return instance;
    }
    
    /**
     * ORCA の入力セットコード（約束処方、診療セット）を返す。
     * @return 入力セットコード(OrcaInputCd)の昇順リスト
     */
    public List<OrcaInputCd> getOrcaInputSet() {
         
        debug("getOrcaInputSet()");
        List<OrcaInputCd> collection = new ArrayList<OrcaInputCd>();

        StringBuilder sb = new StringBuilder();
        sb.append("select * from tbl_inputcd where ");
        if (true) {
            int hospnum = getHospNum();
            sb.append("hospnum=");
            sb.append(hospnum);
            sb.append(" and ");
        } 
        sb.append("inputcd like 'P%' or inputcd like 'S%' order by inputcd");
        String sql = sb.toString();
        debug(sql);
        
        boolean v4 = true;  //Project.getOrcaVersion().startsWith("4") ? true : false;
        
        List<List<String>> valuesList = executeStatement(sql);
        
        for (List<String> values : valuesList) {

            debug("got from tbl_inputcd");

            OrcaInputCd inputCd = new OrcaInputCd();

            if (!v4) {
                inputCd.setHospId(values.get(0));
                inputCd.setCdsyu(values.get(1));
                inputCd.setInputCd(values.get(2));
                inputCd.setSryKbn(values.get(3));
                inputCd.setSryCd(values.get(4));
                inputCd.setDspSeq(Integer.valueOf(values.get(5)));
                inputCd.setDspName(values.get(6));
                inputCd.setTermId(values.get(7));
                inputCd.setOpId(values.get(8));
                inputCd.setCreYmd(values.get(9));
                inputCd.setUpYmd(values.get(10));
                inputCd.setUpHms(values.get(11));

                String cd = inputCd.getInputCd();
                if (cd.length() > 6) {
                    cd = cd.substring(0, 6);
                    inputCd.setInputCd(cd);
                }

            } else {
                inputCd.setCdsyu(values.get(0));
                inputCd.setInputCd(values.get(1));
                inputCd.setSryKbn(values.get(2));
                inputCd.setSryCd(values.get(3));
                inputCd.setDspSeq(Integer.valueOf(values.get(4)));
                inputCd.setDspName(values.get(5));
                inputCd.setTermId(values.get(6));
                inputCd.setOpId(values.get(7));
                inputCd.setCreYmd(values.get(8));
                inputCd.setUpYmd(values.get(9));
                inputCd.setUpHms(values.get(10));

                String cd = inputCd.getInputCd();
                if (cd.length() > 6) {
                    cd = cd.substring(0, 6);
                    inputCd.setInputCd(cd);
                }

                debug("getCdsyu = " + inputCd.getCdsyu());
                debug("getInputCd = " + inputCd.getInputCd());
                debug("getSryKbn = " + inputCd.getSryKbn());
                debug("getSryCd = " + inputCd.getSryCd());
                debug("getDspSeq = " + String.valueOf(inputCd.getDspSeq()));
                debug("getDspName = " + inputCd.getDspName());
                debug("getTermId = " + inputCd.getTermId());
                debug("getOpId " + inputCd.getOpId());
                debug("getCreYmd " + inputCd.getCreYmd());
                debug("getUpYmd " + inputCd.getUpYmd());
                debug("getUpHms " + inputCd.getUpHms());

                ModuleInfoBean info = inputCd.getStampInfo();
                debug("getStampName = " + info.getStampName());
                debug("getStampRole = " + info.getStampRole());
                debug("getEntity = " + info.getEntity());
                debug("getStampId = " + info.getStampId());
            }

            collection.add(inputCd);
        }

        return collection;

    }
    
    /**
     * 指定された入力セットコードから診療セットを Stamp にして返す。
     * @param inputSetInfo 入力セットの StampInfo
     * @return 入力セットのStampリスト
     */
    public List<ModuleModel> getStamp(ModuleInfoBean inputSetInfo) {

        String setCd = inputSetInfo.getStampId(); // stampId=setCd; セットコード
        String stampName = inputSetInfo.getStampName();
        debug("getStamp()");
        debug("setCd = " + setCd);
        debug("stampName = " + stampName);

        int hospnum = getHospNum();
        String sql1 = "select inputcd,suryo1,kaisu from tbl_inputset where hospnum=? and setcd=? order by setseq";
        String sql2 = "select srysyukbn,name,taniname,ykzkbn from tbl_tensu where hospnum=? and srycd=?";

        List<ModuleModel> retSet = new ArrayList<ModuleModel>();

        // setCd を検索する
        int[] types1 = {Types.INTEGER, Types.CHAR};
        String[] params1 = {String.valueOf(hospnum), setCd};
        List<List<String>> valuesList1 = executePreparedStatement(sql1, types1, params1);

        List<OrcaInputSet> list = new ArrayList<OrcaInputSet>();

        for (List<String> values1 : valuesList1) {

            debug("got from tbl_inputset");
            OrcaInputSet inputSet = new OrcaInputSet();
            inputSet.setInputCd(values1.get(0));                // .210 616130532 ...
            inputSet.setSuryo1(Float.valueOf(values1.get(1)));  // item の個数
            inputSet.setKaisu(Integer.valueOf(values1.get(2))); // バンドル数

            debug("getInputCd = " + inputSet.getInputCd());
            debug("getSuryo1 = " + String.valueOf(inputSet.getSuryo1()));
            debug("getKaisu = " + String.valueOf(inputSet.getKaisu()));

            list.add(inputSet);
        }

        if (list.isEmpty()) {
            return retSet;
        }
        
        BundleDolphin bundle = null;
        ModuleModel stamp;

        for (OrcaInputSet inputSet : list) {

            String inputcd = inputSet.getInputCd();
            debug("inputcd = " + inputcd);

            if (inputcd.startsWith(SHINRYO_KBN_START)) {

                stamp = createStamp(stampName, inputcd);
                if (stamp != null) {
                    bundle = (BundleDolphin) stamp.getModel();
                    retSet.add(stamp);
                }
                debug("created stamp " + inputcd);

            } else {

                int[] types2 = {Types.INTEGER, Types.CHAR};
                String[] params2 = {String.valueOf(hospnum), inputcd};
                List<List<String>> valuesList2 = executePreparedStatement(sql2, types2, params2);

                if (!valuesList2.isEmpty()) {
                    List<String> values2 = valuesList2.get(0);

                    debug("got from tbl_tensu");
                    String code = inputcd;
                    String kbn = values2.get(0);
                    String name = values2.get(1);
                    String number = String.valueOf(inputSet.getSuryo1());
                    String unit = values2.get(2);

                    debug("code = " + code);
                    debug("kbn = " + kbn);
                    debug("name = " + name);
                    debug("number = " + number);
                    debug("unit = " + unit);

                    ClaimItem item = new ClaimItem();
                    item.setCode(code);
                    item.setName(name);
                    item.setNumber(number);
                    item.setClassCodeSystem(ClaimConst.SUBCLASS_CODE_ID);

                    if (code.startsWith(ClaimConst.SYUGI_CODE_START)) {
                        // 手技の場合
                        debug("item is tech");
                        item.setClassCode(String.valueOf(ClaimConst.SYUGI));

                        if (bundle == null) {
                            stamp = createStamp(stampName, kbn);
                            if (stamp != null) {
                                bundle = (BundleDolphin) stamp.getModel();
                                retSet.add(stamp);
                            }
                        }

                        if (bundle != null) {
                            bundle.addClaimItem(item);
                        }

                    } else if (code.startsWith(ClaimConst.YAKUZAI_CODE_START)) {
                        // 薬剤の場合
                        debug("item is medicine");
                        item.setClassCode(String.valueOf(ClaimConst.YAKUZAI));
                        item.setNumberCode(ClaimConst.YAKUZAI_TOYORYO);
                        item.setNumberCodeSystem(ClaimConst.NUMBER_CODE_ID);
                        item.setUnit(unit);

                        if (bundle == null) {
                            String receiptCode = values2.get(3).equals(ClaimConst.YKZ_KBN_NAIYO)
                                    ? ClaimConst.RECEIPT_CODE_NAIYO
                                    : ClaimConst.RECEIPT_CODE_GAIYO;
                            stamp = createStamp(stampName, receiptCode);
                            if (stamp != null) {
                                bundle = (BundleDolphin) stamp.getModel();
                                retSet.add(stamp);
                            }
                        }

                        if (bundle != null) {
                            bundle.addClaimItem(item);
                        }

                    } else if (code.startsWith(ClaimConst.ZAIRYO_CODE_START)) {
                        // 材料の場合
                        debug("item is material");
                        item.setClassCode(String.valueOf(ClaimConst.ZAIRYO));
                        item.setNumberCode(ClaimConst.ZAIRYO_KOSU);
                        item.setNumberCodeSystem(ClaimConst.NUMBER_CODE_ID);
                        item.setUnit(unit);

                        if (bundle == null) {
                            stamp = createStamp(stampName, KBN_GENERAL);
                            if (stamp != null) {
                                bundle = (BundleDolphin) stamp.getModel();
                                retSet.add(stamp);
                            }
                        }

                        if (bundle != null) {
                            bundle.addClaimItem(item);
                        }


                    } else if (code.startsWith(ClaimConst.ADMIN_CODE_START)) {
                        // 用法の場合
                        debug("item is administration");
                        if (bundle == null) {
                            stamp = createStamp(stampName, KBN_RP);
                            if (stamp != null) {
                                bundle = (BundleDolphin) stamp.getModel();
                                retSet.add(stamp);
                            }
                        }

                        if (bundle != null) {
                            if (bundle instanceof BundleMed) {
                                debug("cur bundle is BundleMed");
                                bundle.setAdmin(name);
                                bundle.setAdminCode(code);
                                bundle.setBundleNumber(String.valueOf(inputSet.getKaisu()));
                            } else {
                                debug("cur bundle is ! BundleMed");
                                bundle.addClaimItem(item);
                            }
                        }

                    } else if (inputcd.startsWith(ClaimConst.RBUI_CODE_START)) {
                        // 放射線部位の場合
                        debug("item is rad loc.");
                        item.setClassCode(String.valueOf(ClaimConst.SYUGI));

                        if (bundle == null) {
                            stamp = createStamp(stampName, KBN_RAD);
                            if (stamp != null) {
                                bundle = (BundleDolphin) stamp.getModel();
                                retSet.add(stamp);
                            }
                        }

                        if (bundle != null) {
                            bundle.addClaimItem(item);
                        }

                    } else {
                        debug("item is other");
                        if (bundle == null) {
                            stamp = createStamp(stampName, KBN_GENERAL);
                            if (stamp != null) {
                                bundle = (BundleDolphin) stamp.getModel();
                                retSet.add(stamp);
                            }
                        }
                        
                        if (bundle != null) {
                            bundle.addClaimItem(item);
                        }
                    }
                }
            }
        }

        return retSet;
    }
    
    /**
     * Stampを生成する。
     * @param stampName Stamp名
     * @param code 診療区分コード
     * @return Stamp
     */
    private ModuleModel createStamp(String stampName, String code) {
        
        ModuleModel stamp = null;
        
        if (code != null) {
            
            if (code.startsWith(SHINRYO_KBN_START)) {
                code = code.substring(1);
            }
            
            if (code.length() > SHINRYO_KBN_LENGTH) {
                code = code.substring(0, SHINRYO_KBN_LENGTH);
            }
            
            stamp = new ModuleModel();
            ModuleInfoBean stampInfo = stamp.getModuleInfoBean();
            stampInfo.setStampName(stampName);
            stampInfo.setStampRole(IInfoModel.ROLE_P);  // ROLE_ORCA -> EOLE_P
            //stampInfo.setStampMemo(code);
            BundleDolphin bundle;
                
            if (code.startsWith(RP_KBN_START)) {
                
                bundle = new BundleMed();
                stamp.setModel(bundle);
                
                String inOut = Project.getBoolean(Project.RP_OUT, true)
                               ? ClaimConst.EXT_MEDICINE
                               : ClaimConst.IN_MEDICINE;
                bundle.setMemo(inOut);
                
            } else {
                
                bundle = new BundleDolphin();
                stamp.setModel(bundle);
            }
            
            bundle.setClassCode(code);
            bundle.setClassCodeSystem(ClaimConst.CLASS_CODE_ID);
            bundle.setClassName(MMLTable.getClaimClassCodeName(code));
            bundle.setBundleNumber(String.valueOf(DEFAULT_BUNDLE_NUMBER));

            String[] entityOrder = getEntityOrderName(code);
            if (entityOrder != null) {
                stampInfo.setEntity(entityOrder[0]);
                bundle.setOrderName(entityOrder[1]);
            }
        } 
        
        return stamp;
    }
    
    private String[] getEntityOrderName(String receiptCode) {
        
        try {
            int number = Integer.parseInt(receiptCode);
            
            if (number >= 110 && number <= 125) {
                return new String[]{IInfoModel.ENTITY_BASE_CHARGE_ORDER, "診断料"};
            
            } else if (number >= 130 && number <= 150) {
                return new String[]{IInfoModel.ENTITY_INSTRACTION_CHARGE_ORDER, "指導・在宅"};
                
            } else if (number >= 200 && number <= 299) {
                return new String[]{IInfoModel.ENTITY_MED_ORDER, "RP"};
            
            } else if (number >= 300 && number <= 352) {
                return new String[]{IInfoModel.ENTITY_INJECTION_ORDER, "注 射"};
            
            } else if (number >= 400 && number <= 499) {
                return new String[]{IInfoModel.ENTITY_TREATMENT, "処 置"};
            
            } else if (number >= 500 && number <= 599) {
                return new String[]{IInfoModel.ENTITY_SURGERY_ORDER, "手術"};
            
            } else if (number >= 600 && number <= 699) {
                return new String[]{IInfoModel.ENTITY_LABO_TEST, "検査"};
            
            } else if (number >= 700 && number <= 799) {
                return new String[]{IInfoModel.ENTITY_RADIOLOGY_ORDER, "放射線"};
            
            } else if (number >= 800 && number <= 899) {
                return new String[]{IInfoModel.ENTITY_OTHER_ORDER, "その他"};
                
            } else {
                return new String[]{IInfoModel.ENTITY_GENERAL_ORDER, "汎 用"};
            }
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        
        return null;
    }

    @Override
    protected void debug(String msg) {
        //System.out.println(msg);
    }
}
