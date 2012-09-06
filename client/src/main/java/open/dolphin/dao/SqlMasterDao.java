package open.dolphin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import open.dolphin.infomodel.DiseaseEntry;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.util.StringTool;

/**
 * SqlMasterDao
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public final class SqlMasterDao extends SqlDaoBean {

    private static final String QUERY_TENSU_BY_SHINKU = SELECT_TBL_TENSU
            + "where srysyukbn ~ ? and yukostymd <= ? and yukoedymd >= ?"
            + HOSPNUM_SRYCD;
    
    private static final String QUERY_TENSU_BY_NAME = SELECT_TBL_TENSU
            + "where (name ~ ? or kananame ~ ?) and yukostymd <= ? and yukoedymd >= ?"
            + HOSPNUM_SRYCD;
    
    private static final String QUERY_TENSU_BY_1_NAME = SELECT_TBL_TENSU
            + "where (name = ? or kananame = ?) and yukostymd <= ? and yukoedymd >= ?"
            + HOSPNUM_SRYCD;
    
    private static final String QUERY_TENSU_BY_CODE = SELECT_TBL_TENSU
            + "where srycd ~ ? and yukostymd <= ? and yukoedymd >= ?"
            + HOSPNUM_SRYCD;
    
    private static final String QUERY_TENSU_BY_TEN = SELECT_TBL_TENSU
            + "where ten >= ? and ten <= ? and yukostymd <= ? and yukoedymd >= ?"
            + HOSPNUM_SRYCD;
    
    private static final String QUERY_DISEASE_BY_NAME = SELECT_TBL_BYOMEI
            + "where (byomei ~ ? or byomeikana ~ ?) and haisiymd >= ?";
    
    private static final String QUERY_DISEASE_BY_NAME_46 =
            QUERY_DISEASE_BY_NAME.replace("icd10", "icd10_1");
    
    private static final String QUERY_DISEASE_BY_CODE = SELECT_TBL_BYOMEI
            + "where byomeicd ~ ? and haisiymd >= ?";
    
    private static final String QUERY_DISEASE_BY_CODE_46 = 
            QUERY_DISEASE_BY_CODE.replace("icd10", "icd10_1");
    
    
    private static final SqlMasterDao instance;

    static {
        instance = new SqlMasterDao();
    }

    public static SqlMasterDao getInstance() {
        return instance;
    }

    private SqlMasterDao() {
    }

    public List<TensuMaster> getTensuMasterByShinku(String shinku, String now) {

        // 結果を格納するリスト
        List<TensuMaster> ret = new ArrayList<TensuMaster>();

        // SQL 文
        String sql = QUERY_TENSU_BY_SHINKU;
        int hospNum = getHospNum();
        
        Connection con = null;
        PreparedStatement ps;

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, shinku);
            ps.setString(2, now);
            ps.setString(3, now);
            ps.setInt(4, hospNum);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                TensuMaster t = getTensuMaster(rs);
                ret.add(t);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            processError(e);
        } finally {
            closeConnection(con);
        }

        return ret;
    }

    public List<TensuMaster> getTensuMasterByName(String name, String now, boolean partialMatch) {

        // 結果を格納するリスト
        List<TensuMaster> ret = new ArrayList<TensuMaster>();

        // 半角英数字を全角へ変換する
        name = StringTool.toZenkakuUpperLower(name);

        // SQL 文
        boolean one = (name.length() == 1);
        StringBuilder buf = new StringBuilder();
        if (one) {
            buf.append(QUERY_TENSU_BY_1_NAME);
        } else {
            buf.append(QUERY_TENSU_BY_NAME);
            if (!partialMatch) {
                name = "^" + name;
            }
        }
        String sql = buf.toString();
        int hospNum = getHospNum();
        
        Connection con = null;
        PreparedStatement ps;

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, name);
            ps.setString(3, now);
            ps.setString(4, now);
            ps.setInt(5, hospNum);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                TensuMaster t = getTensuMaster(rs);
                ret.add(t);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            processError(e);
        } finally {
            closeConnection(con);
        }

        return ret;
    }

    public List<TensuMaster> getTensuMasterByCode(String regExp, String now) {

        // 結果を格納するリスト
        List<TensuMaster> ret = new ArrayList<TensuMaster>();

        // SQL 文
        String sql = QUERY_TENSU_BY_CODE;
        int hospNum = getHospNum();

        Connection con = null;
        PreparedStatement ps;

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            // 増田内科 コール側で ^ をとる
            ps.setString(1, "^" + regExp);
            ps.setString(2, now);
            ps.setString(3, now);
            ps.setInt(4, hospNum);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                TensuMaster t = getTensuMaster(rs);
                ret.add(t);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            processError(e);
        } finally {
            closeConnection(con);
        }

        return ret;
    }

    public List<TensuMaster> getTensuMasterByTen(String ten, String now) {

        // 結果を格納するリスト
        List<TensuMaster> ret = new ArrayList<TensuMaster>();

        // SQL 文
        String sql =QUERY_TENSU_BY_TEN;
        int hospNum = getHospNum();

        Connection con = null;
        PreparedStatement ps;

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            String[] params = ten.split("-");
            if (params.length > 1) {
                ps.setFloat(1, Float.parseFloat(params[0]));
                ps.setFloat(2, Float.parseFloat(params[1]));
            } else {
                ps.setFloat(1, Float.parseFloat(params[0]));
                ps.setFloat(2, Float.parseFloat(params[0]));
            }

            ps.setString(3, now);
            ps.setString(4, now);
            ps.setInt(5, hospNum);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                TensuMaster t = getTensuMaster(rs);
                ret.add(t);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            processError(e);
        } finally {
            closeConnection(con);
        }

        return ret;
    }

    public List<DiseaseEntry> getDiseaseByName(String name, String now, boolean partialMatch) {

        // 結果を格納するリスト
        List<DiseaseEntry> ret = new ArrayList<DiseaseEntry>();

        // SQL 文
//masuda^ Version46 対応
        String sql = SyskanriInfo.getInstance().isOrca46()
                ? QUERY_DISEASE_BY_NAME_46
                : QUERY_DISEASE_BY_NAME;
//masuda$

        Connection con = null;
        PreparedStatement ps;

        if (!partialMatch) {
            name = "^" + name;
        }

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, name);
            ps.setString(3, now);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DiseaseEntry de = getDiseaseEntry(rs);
                ret.add(de);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            processError(e);
        } finally {
            closeConnection(con);
        }

        return ret;
    }
    
    public List<DiseaseEntry> getDiseaseByCode(String code, String now, boolean partialMatch) {

        // 結果を格納するリスト
        List<DiseaseEntry> ret = new ArrayList<DiseaseEntry>();

        // SQL 文
//masuda^ Version46 対応
        String sql = SyskanriInfo.getInstance().isOrca46()
                ? QUERY_DISEASE_BY_CODE_46
                : QUERY_DISEASE_BY_CODE;
//masuda$

        Connection con = null;
        PreparedStatement ps;

        if (!partialMatch) {
            code = "^" + code;
        }

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, code);
            ps.setString(2, now);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DiseaseEntry de = getDiseaseEntry(rs);
                ret.add(de);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            processError(e);
        } finally {
            closeConnection(con);
        }

        return ret;
    }
}