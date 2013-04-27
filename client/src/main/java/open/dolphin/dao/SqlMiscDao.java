package open.dolphin.dao;

import java.sql.Types;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import open.dolphin.infomodel.*;
import open.dolphin.order.MasterItem;
import open.dolphin.util.MMLDate;

/**
 * ORCAに問い合わせる諸々
 *
 * @author masuda, Masuda Naika
 */
public final class SqlMiscDao extends SqlDaoBean {

    private static final SqlMiscDao instance;
    
    // srycdと検査等実施判断グループ区分のマップ
    private Map<String, Integer> hokatsuKbnMap;

    static {
        instance = new SqlMiscDao();
    }

    public static SqlMiscDao getInstance() {
        return instance;
    }

    private SqlMiscDao() {
        hokatsuKbnMap = new HashMap<String, Integer>();
    }
    
    // 検査等実施判断グループ区分を調べる
    public Map<String, Integer> getHokatsuKbnMap(List<String> srycds) {
        
        List<String> srycdsToGet = new ArrayList<String>();
        for (String srycd : srycds) {
            if (!hokatsuKbnMap.containsKey(srycd)) {
                srycdsToGet.add(srycd);
            }
        }
        if (srycdsToGet.isEmpty()) {
            return hokatsuKbnMap;
        }
        
        int hospNum = getHospNum();
        StringBuilder sb = new StringBuilder();
        sb.append("select srycd, houksnkbn from tbl_tensu ");
        sb.append("where yukoedymd = '99999999' ");
        sb.append("and srycd in (").append(getCodes(srycdsToGet)).append(") ");
        sb.append("and hospnum = ").append(String.valueOf(hospNum));
        
        final String sql = sb.toString();
        
        List<List<String>> valuesList = executeStatement(sql);
        for (List<String> values : valuesList) {
            String srycd = values.get(0);
            String kbnStr = values.get(1);
            int kbn = (kbnStr != null) ? Integer.valueOf(kbnStr) : 0;
            hokatsuKbnMap.put(srycd, kbn);
        }

        return hokatsuKbnMap;
    }

    // 入院中の患者を検索し入院モデルを作成する
    public List<AdmissionModel> getInHospitalPatients(Date date) {
        
        final String sql = "select TP.ptnum, TN.brmnum, TN.nyuinka, TN.nyuinymd , TN.drcd1 "
                + "from tbl_ptnyuinrrk TN inner join tbl_ptnum TP on TP.ptid = TN.ptid "
                + "where TN.tennyuymd <= ? and ? <= TN.tenstuymd and TN.hospnum = ?";
        
        SimpleDateFormat frmt = new SimpleDateFormat("yyyyMMdd");
        String dateStr = frmt.format(date);
        int hospNum = getHospNum();
        
        List<AdmissionModel> ret = new ArrayList<AdmissionModel>();
        
        int[] types = {Types.CHAR, Types.CHAR, Types.INTEGER};
        String[] params = {dateStr, dateStr, String.valueOf(hospNum)};
        
        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        for (List<String> values : valuesList) {
            String patientId = values.get(0).trim();
            String room = getRoomNumber(values.get(1).trim());
            String dept = getDepartmentDesc(values.get(2).trim());
            Date aDate = null;
            try {
                aDate = frmt.parse(values.get(3).trim());
            } catch (ParseException ex) {
            }
            String doctor = getOrcaStaffName(values.get(4).trim());

            // 新たに作成
            AdmissionModel model = new AdmissionModel();
            model.setId(0L);
            model.setPatientId(patientId);
            model.setRoom(room);
            model.setDepartment(dept);
            model.setStarted(aDate);
            model.setDoctorName(doctor);
            ret.add(model);
        }

        return ret;
    }
    
    private String getOrcaStaffName(String code) {
        String staffName = SyskanriInfo.getInstance().getOrcaStaffName(code);
        return staffName;
    }

    private String getRoomNumber(String str) {
        
        // 病棟番号を除去
        str = str.substring(2);
        // 先頭のゼロを除去
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (found) {
                sb.append(c);
            } else if (c != '0') {
                found = true;
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private String getDepartmentDesc(String code) {
        return SyskanriInfo.getInstance().getOrcaDeptDesc(code.trim());
    }

    
    public List<DrugInteractionModel> checkInteraction(Collection<String> src1, Collection<String> src2) {
        // 引数はdrugcdの配列ｘ２

        if (src1 == null || src1.isEmpty() || src2 == null || src2.isEmpty()) {
            return Collections.emptyList();
        }
        
        // コードが後発品ならばその先発品のコードを追加する
        Collection<String> allDrug = new ArrayList<>();
        allDrug.addAll(src1);
        allDrug.addAll(src2);
        
        // ゾロと先発の対応リストを作成する one-to-many
        List<ZoroBrandPair> zoroBrandList = getZoroBrandPair(allDrug);

        Collection<String> drug1 = new ArrayList<>(src1);
        for (String srycd : src1) {
            List<ZoroBrandPair> pairs = getBrands(zoroBrandList, srycd);
            for (ZoroBrandPair pair : pairs) {
                drug1.add(pair.brandSrycd);
            }
        }
        
        Collection<String> drug2 = new ArrayList<>(src2);
        for (String srycd : src2) {
            List<ZoroBrandPair> pairs = getBrands(zoroBrandList, srycd);
            for (ZoroBrandPair pair : pairs) {
                drug2.add(pair.brandSrycd);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        List<DrugInteractionModel> ret = new ArrayList<DrugInteractionModel>();

        // SQL文を作成
        sb.append("select drugcd, drugcd2, TI.syojyoucd, syojyou ");
        sb.append("from tbl_interact TI inner join tbl_sskijyo TS on TI.syojyoucd = TS.syojyoucd ");
        sb.append("where (drugcd in (");
        sb.append(getCodes(drug1));
        sb.append(") and drugcd2 in (");
        sb.append(getCodes(drug2));
        sb.append("))");
        String sql = sb.toString();

        List<List<String>> valuesList = executeStatement(sql);
        for (List<String> values : valuesList) {
            String srycd1 = values.get(0);
            String srycd2 = values.get(1);
            String sskijo = values.get(2);
            String syojoucd = values.get(3);
            String brandName1 = null;
            String brandName2 = null;
            
            ZoroBrandPair zoro = getZoro(zoroBrandList, srycd1);
            // ゾロのエイリアスとしての先発薬の場合
            if (zoro != null) {
                List<ZoroBrandPair> brands = getBrands(zoroBrandList, zoro.zoroSrycd);
                if (!brands.isEmpty()) {
                    srycd1 = zoro.zoroSrycd;
                    boolean first = true;
                    sb = new StringBuilder();
                    for (ZoroBrandPair pair : brands) {
                        if (!first) {
                            sb.append(",");
                        } else {
                            first = false;
                        }
                        sb.append(pair.brandName);
                    }
                    brandName1 = sb.toString();
                }
            }

            zoro = getZoro(zoroBrandList, srycd2);
            if (zoro != null) {
                List<ZoroBrandPair> brands = getBrands(zoroBrandList, zoro.zoroSrycd);
                if (!brands.isEmpty()) {
                    srycd2 = zoro.zoroSrycd;
                    boolean first = true;
                    sb = new StringBuilder();
                    for (ZoroBrandPair pair : brands) {
                        if (!first) {
                            sb.append(",");
                        } else {
                            first = false;
                        }
                        sb.append(pair.brandName);
                    }
                    brandName2 = sb.toString();
                }
            }
            
            ret.add(new DrugInteractionModel(srycd1, srycd2, sskijo, syojoucd, brandName1, brandName2));
        }

        return ret;
    }
    
    private class ZoroBrandPair {

        String zoroSrycd;
        String zoroName;
        String brandSrycd;
        String brandName;
    }

    private ZoroBrandPair getZoro(List<ZoroBrandPair> list, String brandSrycd) {
        for (ZoroBrandPair pair : list) {
            if (brandSrycd.equals(pair.brandSrycd)) {
                return pair;
            }
        }
        return null;
    }

    private List<ZoroBrandPair> getBrands(List<ZoroBrandPair> list, String zoroSrycd) {
        List<ZoroBrandPair> ret = new ArrayList<>();
        for (ZoroBrandPair pair : list) {
            if (zoroSrycd.equals(pair.zoroSrycd)) {
                ret.add(pair);
            }
        }
        return ret;
    }
    
    private List<ZoroBrandPair> getZoroBrandPair(Collection codes) {
        
        List<ZoroBrandPair> ret = new ArrayList<>();
        
        // 後発薬の薬価基準コードを取得する。先頭９ケタ
        StringBuilder sb = new StringBuilder();
        sb.append("select distinct srycd,name,yakkakjncd from tbl_tensu where srycd in (");
        sb.append(getCodes(codes));
        sb.append(") and kouhatukbn='1' and yukoedymd='99999999'");
        String sql = sb.toString();
        
        Map<String, ZoroBrandPair> yakkakjncdMap = new HashMap<>();
        List<List<String>> valuesList = executeStatement(sql);
        for (List<String> values : valuesList) {
            ZoroBrandPair pair = new ZoroBrandPair();
            pair.zoroSrycd = values.get(0);
            pair.zoroName = values.get(1);
            // javaのsubstringはyakkakjncd.substring(0,9)である
            String yakkakjncd = values.get(2).substring(0, 9);
            yakkakjncdMap.put(yakkakjncd, pair);
        }
        
        if (yakkakjncdMap.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 先発薬を調べる
        sb = new StringBuilder();
        // sqlのsubstringはsubstring(yakkakjncd,0,10)である
        sb.append("select distinct srycd,name,yakkakjncd from tbl_tensu where substring(yakkakjncd,0,10) in (");
        sb.append(getCodes(yakkakjncdMap.keySet()));
        sb.append(") and kouhatukbn<>'1' and yukoedymd='99999999'");
        sql = sb.toString();
        valuesList = executeStatement(sql);
        for (List<String> values : valuesList) {
            String brandSrycd = values.get(0);
            String brandName = values.get(1);
            String yakkakjncd = values.get(2).substring(0, 9);
            ZoroBrandPair mapPair = yakkakjncdMap.get(yakkakjncd);
            if (mapPair != null) {
                ZoroBrandPair pair = new ZoroBrandPair();
                pair.brandSrycd = brandSrycd;
                pair.brandName = brandName;
                pair.zoroSrycd = mapPair.zoroSrycd;
                pair.zoroName = mapPair.zoroName;
                ret.add(pair);
            }
        }
        
        yakkakjncdMap.clear();

        return ret;
    }

    // srycdからgairaiKanriKbnの有無をチェックする。算定チェックに利用
    public boolean hasGairaiKanriKbn(Collection<String> srycdList){

        if (srycdList == null || srycdList.isEmpty()) {
            return false;
        }
        int hospNum = getHospNum();
        boolean ret = false;

        StringBuilder sb = new StringBuilder();
        sb.append("select gaikanrikbn from tbl_tensu ");
        sb.append("where yukoedymd = '99999999' and ");
        sb.append("gaikanrikbn = 1 and ");      //１：外来管理加算が算定できない診療行為
        sb.append("hospnum = ");
        sb.append(String.valueOf(hospNum));
        sb.append(" and ");
        sb.append("srycd in (");
        sb.append(getCodes(srycdList));
        sb.append(")");

        String sql = sb.toString();

        List<List<String>> valuesList = executeStatement(sql);
        ret = !valuesList.isEmpty();

        return ret;
    }

    // srycdから腫瘍マーカー検査の有無をチェックする。算定チェックに利用
    public boolean hasTumorMarkers(Collection<String> srycdList){

        if (srycdList == null || srycdList.isEmpty()) {
            return false;
        }

        int hospNum = getHospNum();
        boolean ret = false;

        StringBuilder sb = new StringBuilder();
        sb.append("select houksnkbn from tbl_tensu ");
        sb.append("where yukoedymd = '99999999' and ");
        sb.append("houksnkbn = 5 and ");      //５：腫瘍マーカー
        sb.append("hospnum = ");
        sb.append(String.valueOf(hospNum));
        sb.append(" and ");
        sb.append("srycd in (");
        sb.append(getCodes(srycdList));
        sb.append(")");

        String sql = sb.toString();
        
        List<List<String>> valuesList = executeStatement(sql);
        ret = !valuesList.isEmpty();

        return ret;
    }
    
/*  もっさり。。。
    // srycdからTensuMasterをまとめて取得。LaboTestPanel, BaseEditor, RpEditor, ImportOrcaMedicine, CheckSanteiで利用
    public List<TensuMaster> getTensuMasterList(List<String> srycdList) {

        if (srycdList == null || srycdList.isEmpty()) {
            return null;
        }

        // 結果を格納するリスト
        List<TensuMaster> ret = new ArrayList<TensuMaster>();
        
        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_TBL_TENSU2);
        sb.append("where t.hospnum = ");
        sb.append(String.valueOf(getHospNum()));
        sb.append(" and ");
        sb.append("t.srycd in (");
        sb.append(getCodes(srycdList));
        sb.append(")");
        sb.append(" and ");
        sb.append(" t.yukoedymd = (select max(t2.yukoedymd) from tbl_tensu t2 where t.srycd = t2.srycd group by t2.srycd)");
        String sql = sb.toString();

        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                TensuMaster tm = getTensuMaster(rs);
                ret.add(tm);
            }
            rs.close();
            closeStatement(st);
            closeConnection(con);
            return ret;

        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
        }
        return null;
    }
*/
    // srycdからTensuMasterをまとめて取得。LaboTestPanel, BaseEditor, RpEditor, ImportOrcaMedicine, CheckSanteiで利用
    public List<TensuMaster> getTensuMasterList(Collection<String> srycdList) {
        
        if (srycdList == null || srycdList.isEmpty()) {
            return Collections.emptyList();
        }

        int todayInt = MMLDate.getTodayInt();
        int hospNum = getHospNum();
        
        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_TBL_TENSU);
        sb.append("where hospnum = ");
        sb.append(String.valueOf(hospNum));
        sb.append(" and ");
        sb.append("srycd in (");
        sb.append(getCodes(srycdList));
        sb.append(")");
        String sql = sb.toString();

        HashMap<Integer, TensuMaster> tmMap = new HashMap<Integer, TensuMaster>();
        
        List<List<String>> valuesList = executeStatement(sql);
        for (List<String> values : valuesList) {
            TensuMaster tm = getTensuMaster(values);
            // HashMapに登録していく
            int srycd = Integer.valueOf(tm.getSrycd());
            TensuMaster old = tmMap.get(srycd);
            if (old == null) {
                tmMap.put(srycd, tm);
            } else {
                // 有効期限が新しければ更新する
                if (Integer.parseInt(tm.getYukostymd()) <= todayInt
                        && Integer.parseInt(tm.getYukoedymd()) > Integer.parseInt(old.getYukoedymd())) {
                    tmMap.put(srycd, tm);
                }
            }
        }
        return new ArrayList<TensuMaster>(tmMap.values());
    }

    // 傷病名コードからまとめてDiseaseEntryを取得
    public List<DiseaseEntry> getDiseaseEntries(Collection<String> srycdList){
        
        if (srycdList == null || srycdList.isEmpty()) {
            return Collections.emptyList();
        }
        String selectSql = SyskanriInfo.getInstance().isOrca45()
                ? SELECT_TBL_BYOMEI.replace("icd10_1", "icd10")
                : SELECT_TBL_BYOMEI;
        
        StringBuilder sb = new StringBuilder();
        sb.append(selectSql);
        sb.append("where byomeicd in (");
        sb.append(getCodes(srycdList));
        sb.append(")");
        String sql = sb.toString();

        List<DiseaseEntry> collection = new ArrayList<DiseaseEntry>();
        List<DiseaseEntry> outUse = new ArrayList<DiseaseEntry>();
        
        List<List<String>> valuesList = executeStatement(sql);
        for (List<String> values : valuesList) {
            DiseaseEntry de = getDiseaseEntry(values);
            if (de.isInUse()) {
                collection.add(de);
            } else {
                outUse.add(de);
            }
        }
        collection.addAll(outUse);
        
        return collection;
    }

    // 患者の処方をORCAから取り出してMasterItemのリストとして返す
    public List<MasterItem> getMedMasterItemFromOrca(String patientId, String visitYMD) {

        final String ADMIN_MARK = "[用法] ";

        List<MasterItem> miList = new ArrayList<MasterItem>();
        List<String[]> dataList = new ArrayList<String[]>();      // {srycd, suryo}

        String sryYM = visitYMD.substring(0, 6);
        String sryD = String.valueOf(Integer.parseInt(visitYMD.substring(6)));

        // まずは調べたい日の処方のコード・数量、用法のコード・日数をピックアップ
        String dayColumn = "day_" + sryD;

        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("act.srycd1,act.srysuryo1,act.srykaisu1,inputnum1,");
        sb.append("act.srycd2,act.srysuryo2,act.srykaisu2,inputnum2,");
        sb.append("act.srycd3,act.srysuryo3,act.srykaisu3,inputnum3,");
        sb.append("act.srycd4,act.srysuryo4,act.srykaisu4,inputnum4,");
        sb.append("act.srycd5,act.srysuryo5,act.srykaisu5,inputnum5,");
        sb.append("main.").append(dayColumn);
        sb.append(" from tbl_sryact act");
        sb.append(" join tbl_sryacct_main main on act.zainum=main.zainum and act.ptid=main.ptid");

        sb.append(" where ");
        // ↓では院外処方がダメ
        //sb.append(" main.srykbn~'2[123]' and main.ykzten<>0");
        sb.append(" act.srysyukbn~'2[1239].'");
        sb.append(" and main.ptid=");
        sb.append(String.valueOf(getOrcaPtID(patientId)));
        sb.append(" and main.sryym=");
        sb.append(addSingleQuote(sryYM));
        sb.append(" and main.");
        sb.append(dayColumn);
        sb.append("<>0 order by act.rennum, act.zainum");
        String sql = sb.toString();
        
        List<List<String>> valuesList = executeStatement(sql);
        
        for (List<String> values : valuesList) {
            
            int dayColumnValue = Integer.valueOf(values.get(20));
            
            for (int i = 0; i < 4; ++i) {
                // 外用薬ならsrykaisu=1、内服ならsrykaisu = 0
                // 「２つ目の数量（分画数）がある場合それを表す」の意味は理解できなかったｗ

                String srycd = values.get(i * 4).trim();
                // 数量はfloatにしないと、0.5錠とかNGになる
                float srysuryo = Float.valueOf(values.get(i * 4 + 1));
                int srykaisu = Integer.valueOf(values.get(i * 4 + 2));
                int inputnum = Integer.valueOf(values.get(i * 4 + 3));
                // srycdが空白なら剤の終了
                if (srycd.isEmpty()) {
                    break;
                }

                if (srycd.startsWith("001")) {
                    // srycdが001から始まっていたら用法コード
                    // 内服なら'day_DD'のカラムから日数を、外用ならsrykaisuを返す
                    // 同日に同じ処方があると、その処方の日数は合計になる。
                    // tbl_sryacct_subを参照すれば分離できるが、めんどくさいｗ
                    srysuryo = (srykaisu == 0) ? dayColumnValue : srykaisu;
                    DecimalFormat frmt = new DecimalFormat("0.###");
                    String value = frmt.format(srysuryo);
                    dataList.add(new String[]{srycd, value});
                } else if (inputnum != 0) {
                    // おそらくコメント
                    dataList.add(new String[]{srycd, ""});
                } else {
                    // 普通の薬
                    DecimalFormat frmt = new DecimalFormat("0.###");
                    String value = frmt.format(srysuryo);
                    dataList.add(new String[]{srycd, value});
                }
            }
        }

        // 調べたsrycdに応じたTensuMasterを取得して、MasterItemのリストに追加する。
        // まずはTensuMasterをまとめて取得しHashMapに登録する
        // srycd群を用意
        int len = dataList.size();
        if (len == 0) {
            return Collections.emptyList();
        }
        List<String> srycdList = new ArrayList<String>(len);
        for (int i = 0; i < len; ++i) {
            srycdList.add(dataList.get(i)[0]);
        }
        // ORCAに問い合わせ
        List<TensuMaster> result = getTensuMasterList(srycdList);
        // HashMapに登録
        HashMap<Integer, TensuMaster> tensuMasterMap = new HashMap<Integer, TensuMaster>();
        for (TensuMaster tm : result) {
            tensuMasterMap.put(Integer.valueOf(tm.getSrycd()), tm);
        }
        // MasterItemを作成する
        for (int i = 0; i < dataList.size(); ++i) {
            String srycd = dataList.get(i)[0];
            String value = dataList.get(i)[1];

            if (!srycd.startsWith("001")) {
                // 薬剤コード
                TensuMaster tm = tensuMasterMap.get(Integer.valueOf(srycd));
                if (tm == null) {
                    continue;
                }
                // MedicineのTensuMasterを作成
                MasterItem mItem = new MasterItem();
                mItem.setClassCode(ClaimConst.YAKUZAI);
                mItem.setCode(tm.getSrycd());
                mItem.setUnit(tm.getTaniname());
                mItem.setName(tm.getName());
                mItem.setNumber(value);
                mItem.setYkzKbn(tm.getYkzkbn());
                mItem.setDataKbn(tm.getDataKbn());
                miList.add(mItem);

            } else {
                // 用法コード
                TensuMaster tm = tensuMasterMap.get(Integer.valueOf(srycd));
                if (tm == null) {
                    continue;
                }
                // AdminのTensuMasterを作成
                MasterItem mItem = new MasterItem();
                mItem.setClassCode(ClaimConst.ADMIN);
                mItem.setCode(tm.getSrycd());
                mItem.setName(ADMIN_MARK + tm.getName());
                mItem.setDummy("X");
                mItem.setBundleNumber(value);
                mItem.setDataKbn(tm.getDataKbn());
                miList.add(mItem);
            }
        }
        return miList;
    }


    // 期間内の受診日を取得する。返り値は"YYYYMMDD"形式の文字列リスト
    public List<String> getOrcaVisit(String patientId, String startDate, String endDate, boolean desc, String search) {

        List<String> orcaVisit = new ArrayList<String>();
        long ptid = getOrcaPtID(patientId);

        StringBuilder sb = new StringBuilder();
        sb.append("select sryymd");
        for (int i = 1; i <= 15; ++i) {
            sb.append(",zainum");
            sb.append(String.valueOf(i));
        }
        sb.append(" from tbl_jyurrk where ");
        sb.append("to_date(sryymd,'YYYYMMDD') ");
        sb.append("between to_date('");
        sb.append(startDate);
        sb.append("','YYYYMMDD') and to_date('");
        sb.append(endDate);
        sb.append("','YYYYMMDD') and ptid=");
        sb.append(String.valueOf(ptid));
        if ("medOrder".equals(search)){
            sb.append(" and (srykbn2 = '01' or srykbn3 = '01' or srykbn4 = '01')");
        }
        sb.append(" order by rennum asc, to_date(sryymd,'YYYYMMDD')");
        if (desc){
            sb.append(" desc");
        }
        String sql = sb.toString();

        List<List<String>> valuesList = executeStatement(sql);
        
        for (List<String> values : valuesList) {
            orcaVisit.add(values.get(0));
        }

        return orcaVisit;
    }

    public long getTableRowCount(String tableName) {

        long count = 0;

        String sql = "select count(*) from " + tableName;
        
        List<List<String>> valuesList = executeStatement(sql);
        if (!valuesList.isEmpty()) {
            List<String> values = valuesList.get(0);
            count = Long.valueOf(values.get(0));
        }

        return count;
    }
    
/*
    public List<String[]> getTekiouByomeiCd(String[] codes){
        StringBuilder sb = new StringBuilder();
        Connection con = null;
        Statement st = null;
        String sql = null;
        boolean first = true;

        // sql文を作成
        sb.append("select srycd, khnbyomeicd, byomei from tbl_tekioubyomei where ");
        sb.append("srycd in (");
        for (String code : codes){
            if (!first){
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(addSingleQuote(code));
        }
        sb.append(")");

        sql = sb.toString();
        List<String[]> ret = new ArrayList();

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()){
                ret.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
            }
            closeStatement(st);
            closeConnection(con);
        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
        }

        return ret;
    }
*/

}
