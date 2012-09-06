package open.dolphin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
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

    static {
        instance = new SqlMiscDao();
    }

    public static SqlMiscDao getInstance() {
        return instance;
    }

    private SqlMiscDao() {
    }

    // 入院中の患者を検索する
    public List<AdmissionModel> getInHospitalPatients(Date date) {
        
        final String sql = "select TP.ptnum, TN.brmnum, TN.nyuinka, TN.nyuinymd , TN.drcd1 "
                + "from tbl_ptnyuinrrk TN inner join tbl_ptnum TP on TP.ptid = TN.ptid "
                + "where TN.tennyuymd <= ? and TN.tenstuymd <= ? and TN.hospnum = ?";
        
        SimpleDateFormat frmt = new SimpleDateFormat("yyyyMMdd");
        String dateStr = frmt.format(date);
        int hospNum = getHospNum();
        
        Connection con = null;
        PreparedStatement ps = null;
        List<AdmissionModel> ret = new ArrayList<AdmissionModel>();

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, dateStr);
            ps.setString(2, dateStr);
            ps.setString(3, dateStr);
            ps.setInt(4, hospNum);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String patientId = rs.getString(1);
                String room = getRoomNumber(rs.getString(2));
                String dept = getDepartmentDesc(rs.getString(3));
                Date aDate = frmt.parse(rs.getString(4));
                String doctor = getOrcaStaffName(rs.getString(5));
                AdmissionModel model = new AdmissionModel();
                model.setPatientId(patientId);
                model.setRoom(room);
                model.setDepartment(dept);
                model.setStarted(aDate);
                model.setDoctorName(doctor);
                ret.add(model);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            processError(e);
            closeConnection(con);

        } finally {
            closeConnection(con);
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
            }
        }
        return sb.toString();
    }
    
    private String getDepartmentDesc(String code) {
        return SyskanriInfo.getInstance().getOrcaDeptDesc(code.trim());
    }

    
    public List<DrugInteractionModel> checkInteraction(Collection<String> drug1, Collection<String> drug2) {
        // 引数はdrugcdの配列ｘ２

        if (drug1 == null || drug1.isEmpty() || drug2 == null || drug2.isEmpty()) {
            return Collections.emptyList();
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

        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                ret.add(new DrugInteractionModel(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
            rs.close();
            closeStatement(st);
            closeConnection(con);
        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
        }

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
        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                ret = true;
            }
            rs.close();
            closeStatement(st);
            closeConnection(con);
        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
        }
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
        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                ret = true;
            }
            rs.close();
            closeStatement(st);
            closeConnection(con);
        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
        }
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
        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                TensuMaster tm = getTensuMaster(rs);
                // HashMapに登録していく
                int srycd = Integer.valueOf(tm.getSrycd());
                TensuMaster old = tmMap.get(srycd);
                if (old == null){
                    tmMap.put(srycd, tm);
                } else {
                    // 有効期限が新しければ更新する
                    if (Integer.parseInt(tm.getYukostymd()) <= todayInt
                            && Integer.parseInt(tm.getYukoedymd()) > Integer.parseInt(old.getYukoedymd())){
                        tmMap.put(srycd, tm);
                    }
                }
            }
            rs.close();
            closeStatement(st);
            closeConnection(con);
            return new ArrayList<TensuMaster>(tmMap.values());

        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
        }
        return null;
    }

    // 傷病名コードからまとめてDiseaseEntryを取得
    public List<DiseaseEntry> getDiseaseEntries(Collection<String> srycdList){
        
        if (srycdList == null || srycdList.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_TBL_BYOMEI);
        sb.append("where byomeicd in (");
        sb.append(getCodes(srycdList));
        sb.append(")");
        String sql = sb.toString();

        if (SyskanriInfo.getInstance().isOrca46()) {
            sql = sql.replace("icd10", "icd10_1");
        }

        Connection con = null;
        List<DiseaseEntry> collection = null;
        List<DiseaseEntry> outUse = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            collection = new ArrayList<DiseaseEntry>();
            outUse = new ArrayList<DiseaseEntry>();

            while (rs.next()) {
                DiseaseEntry de = getDiseaseEntry(rs);
                if (de.isInUse()) {
                    collection.add(de);
                } else {
                    outUse.add(de);
                }
            }
            rs.close();
            collection.addAll(outUse);

            closeStatement(st);
            closeConnection(con);
            return collection;

        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
        }
        return null;
    }

    // 患者の処方をORCAから取り出してMasterItemのリストとして返す
    public List<MasterItem> getMedMasterItemFromOrca(String patientId, String visitYMD) {

        final String ADMIN_MARK = "[用法] ";

        List<MasterItem> miList = new ArrayList<MasterItem>();
        List<String[]> dataList = new ArrayList<String[]>();      // {srycd, suryo}

        Connection con = null;
        Statement st = null;
        String sql = null;

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

        sb.append("main.");
        sb.append(dayColumn);
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
        sql = sb.toString();

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                for (int i = 1; i <= 5; ++i) {
                     // 外用薬ならsrykaisu=1、内服ならsrykaisu = 0
                    // 「２つ目の数量（分画数）がある場合それを表す」の意味は理解できなかったｗ

                    String srycd = rs.getString("srycd" + String.valueOf(i)).trim();
                    // 数量はfloatにしないと、0.5錠とかNGになる
                    float srysuryo = rs.getFloat("srysuryo" + String.valueOf(i));
                    int srykaisu = rs.getInt("srykaisu" + String.valueOf(i));
                    int inputnum = rs.getInt("inputnum" + String.valueOf(i));
                    // srycdが空白なら剤の終了
                    if ("".equals(srycd)) {
                        break;
                    }

                    if (srycd.startsWith("001")) {
                        // srycdが001から始まっていたら用法コード
                        // 内服なら'day_DD'のカラムから日数を、外用ならsrykaisuを返す
                        // 同日に同じ処方があると、その処方の日数は合計になる。
                        // tbl_sryacct_subを参照すれば分離できるが、めんどくさいｗ
                        srysuryo = (srykaisu == 0) ? rs.getInt(dayColumn) : srykaisu;
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
            rs.close();
            closeStatement(st);
            closeConnection(con);

        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
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
        Connection con = null;
        Statement st = null;
        String sql = null;

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
        sql = sb.toString();

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                orcaVisit.add(rs.getString(1));
            }
            rs.close();
            closeStatement(st);
            closeConnection(con);

        } catch (Exception e) {
            processError(e);
            closeStatement(st);
            closeConnection(con);
        }
        return orcaVisit;
    }

    public long getTableRowCount(String tableName) {

        Connection con = null;
        Statement st = null;
        
        long count = 0;

        String sql = "select count(*) from " + tableName;
        
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            if (rs.next()) {
                count = rs.getLong(1);
            }

            rs.close();
            st.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            processError(e);
            closeConnection(con);

        } finally {
            closeConnection(con);
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
