
package open.dolphin.dao;

import java.sql.*;
import java.util.Date;
import java.util.*;
import open.dolphin.infomodel.*;

/**
 * 電子点数表を参照するDAO
 * 
 * @author masuda, Masuda Naika
 */
public class SqlETensuDao extends SqlDaoBean {
    
    private static final String TBL_ETENSU_1 = "tbl_etensu_1";
    
    private static final String SELECT_ETENSU_1
            = "select srycd, yukostymd, yukoedymd, "
            + "h_tani1, h_group1, h_tani2, h_group2, h_tani3, h_group3, "
            + "r_day, r_month, r_same, r_week, n_group, c_kaisu, chgymd "
            + "from tbl_etensu_1";
    private static final String SELECT_ETENSU_2
            = "select h_group, srycd, yukostymd, yukoedymd, chgymd "
            + "from tbl_etensu_2";
    private static final String SELECT_ETENSU_2_JMA
            = "select h_group, srycd, yukostymd, yukoedymd, chgymd "
            + "from tbl_etensu_2_jma";
    private static final String SELECT_ETENSU_2_OFF
            = "select hospnum, h_group, srycd, yukostymd, yukoedymd, termid, opid, chgymd, upymd, uphms "
            + "from tbl_etensu_2_off ";
    private static final String SELECT_ETENSU_2_SAMPLE
            = "select h_group, srycd, yukostymd, yukoedymd, rennum, samplecd, chgymd "
            + "from tbl_etensu_2_sample";
    private static final String SELECT_ETENSU_3_1
            = "select srycd1, srycd2, yukostymd, yukoedymd, haihan, tokurei, chgymd "
            + "from tbl_etensu_3_1";
    private static final String SELECT_ETENSU_3_2
            = "select srycd1, srycd2, yukostymd, yukoedymd, haihan, tokurei, chgymd "
            + "from tbl_etensu_3_2";
    private static final String SELECT_ETENSU_3_3
            = "select srycd1, srycd2, yukostymd, yukoedymd, haihan, tokurei, chgymd "
            + "from tbl_etensu_3_3";
    private static final String SELECT_ETENSU_3_4
            = "select srycd1, srycd2, yukostymd, yukoedymd, haihan, tokurei, chgymd "
           + "from tbl_etensu_3_4";
    private static final String SELECT_ETENSU_4
            = "select n_group, srycd, yukostymd, yukoedymd, kasan, chgymd "
            + "from tbl_etensu_4";
    private static final String SELECT_ETENSU_5
            = "select srycd, yukostymd, yukoedymd, tanicd, taniname, kaisu, tokurei, chgymd "
            + "from tbl_etensu_5";
    
    private static final String WHERE_ETENSU_RELATED
            = " where not (h_tani1 = 0 and h_tani2 = 0 and h_tani3 = 0 "
            + "and r_day = 0 and r_month = 0 and r_same = 0 and r_week = 0 "
            + "and n_group = 0 and c_kaisu = 0)";

    
    private static final SqlETensuDao instance;

    static {
        instance = new SqlETensuDao();
    }

    public static SqlETensuDao getInstance() {
        return instance;
    }

    private SqlETensuDao() {
    }
    
    public List<ETensuModel1> getETensu1List(long offset, int limit) {
        
        // 結果を格納するリスト
        List<ETensuModel1> ret = new ArrayList<ETensuModel1>();

        Connection con = null;
        PreparedStatement ps;
        String sql = SELECT_ETENSU_1 + WHERE_ETENSU_RELATED + " limit ? offset ?";

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, limit);
            ps.setLong(2, offset);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ETensuModel1 model = createETensuModel1(rs);
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
    
    public List<ETensuModel1> getETensuModel1(Date date, Collection<String> srycds) {
        
        if (srycds == null || srycds.isEmpty()) {
            return Collections.emptyList();
        }
        
        StringBuilder sb = new StringBuilder();
        String ymd = addSingleQuote(yyyyMMddFrmt.format(date));
        sb.append(SELECT_ETENSU_1);
        sb.append(" where srycd in (").append(getCodes(srycds)).append(")");
        sb.append(" and yukostymd <= ").append(ymd);
        sb.append(" and yukoedymd >= ").append(ymd);
        
        String sql = sb.toString();
        Connection con = null;
        Statement st = null;
        List<ETensuModel1> list = new ArrayList<ETensuModel1>();
        
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            while(rs.next()) {
                ETensuModel1 etm1 = createETensuModel1(rs);
                list.add(etm1);
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

        return list;
    }
    
    public List<ETensuModel2> getETensuModel2(Date date, Collection<String> h_groups, Collection<String> srycds) {
        
        if (h_groups == null || h_groups.isEmpty() || srycds == null || srycds.isEmpty()) {
            return Collections.emptyList();
        }
        
        String ymd = addSingleQuote(yyyyMMddFrmt.format(date));
        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_ETENSU_2);
        sb.append(" where h_group = (").append(getCodes(h_groups)).append(")");
        sb.append(" and srycd in (").append(getCodes(srycds)).append(")");
        sb.append(" and yukostymd <= ").append(ymd);
        sb.append(" and yukoedymd >= ").append(ymd);
        
        String sql = sb.toString();
        Connection con = null;
        Statement st = null;
        List<ETensuModel2> list = new ArrayList<ETensuModel2>();
        
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            while(rs.next()) {
                ETensuModel2 etm2 = createETensuModel2(rs);
                list.add(etm2);
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

        return list;
    }
    
    public List<ETensuModel3> getETensuModel3(int tableNumber, Date date, Collection<String> srycds1, Collection<String> srycds2) {
        
        if (srycds1 == null || srycds1.isEmpty() || srycds2 == null || srycds2.isEmpty()) {
            return Collections.emptyList();
        }
        
        String ymd = addSingleQuote(yyyyMMddFrmt.format(date));
        StringBuilder sb = new StringBuilder();
        switch(tableNumber) {
            case 1:     // １日につき
                sb.append(SELECT_ETENSU_3_1);
                break;
            case 2:     // 同一月内
                sb.append(SELECT_ETENSU_3_2);
                break;
            case 3:     // 同時
                sb.append(SELECT_ETENSU_3_3);
                break;
            case 4:     // １週間につき
                sb.append(SELECT_ETENSU_3_4);
                break;
            default:
                return Collections.emptyList();
        }
        sb.append(" where srycd1 in (").append(getCodes(srycds1)).append(")");
        sb.append(" and srycd2 in (").append(getCodes(srycds2)).append(")");
        sb.append(" and yukostymd <= ").append(ymd);
        sb.append(" and yukoedymd >= ").append(ymd);
        
        String sql = sb.toString();
        Connection con = null;
        Statement st = null;
        List<ETensuModel3> list = new ArrayList<ETensuModel3>();
        
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            while(rs.next()) {
                ETensuModel3 etm3 = createETensuModel3(rs);
                list.add(etm3);
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

        return list;
    }
    
    public List<ETensuModel5> getETensuModel5(Date date, Collection<String> srycds) {
        
        if (srycds == null || srycds.isEmpty()) {
            return Collections.emptyList();
        }
        
        StringBuilder sb = new StringBuilder();
        String ymd = addSingleQuote(yyyyMMddFrmt.format(date));
        sb.append(SELECT_ETENSU_5);
        sb.append(" where srycd in (").append(getCodes(srycds)).append(")");
        sb.append(" and yukostymd <= ").append(ymd);
        sb.append(" and yukoedymd >= ").append(ymd);
        
        String sql = sb.toString();
        Connection con = null;
        Statement st = null;
        List<ETensuModel5> list = new ArrayList<ETensuModel5>();
        
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            while(rs.next()) {
                ETensuModel5 etm5 = createETensuModel5(rs);
                list.add(etm5);
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

        return list;
    }
    
    private ETensuModel1 createETensuModel1(ResultSet rs) throws SQLException {
        int i = 0;
        ETensuModel1 model = new ETensuModel1();
        model.setSrycd(rs.getString(++i));
        model.setYukostymd(rs.getString(++i));
        model.setYukoedymd(rs.getString(++i));
        model.setH_tani1(rs.getInt(++i));
        model.setH_group1(rs.getString(++i));
        model.setH_tani2(rs.getInt(++i));
        model.setH_group2(rs.getString(++i));
        model.setH_tani3(rs.getInt(++i));
        model.setH_group3(rs.getString(++i));
        model.setR_day(rs.getInt(++i));
        model.setR_month(rs.getInt(++i));
        model.setR_same(rs.getInt(++i));
        model.setR_week(rs.getInt(++i));
        model.setN_group(rs.getInt(++i));
        model.setC_kaisu(rs.getInt(++i));
        model.setChgymd(rs.getString(++i));
        return model;
    }
    
    private ETensuModel2 createETensuModel2(ResultSet rs) throws SQLException {
        int i = 0;
        ETensuModel2 model = new ETensuModel2();
        model.setH_group(rs.getString(++i));
        model.setSrycd(rs.getString(++i));
        model.setYukostymd(rs.getString(++i));
        model.setYukoedymd(rs.getString(++i));
        model.setChgymd(rs.getString(++i));
        return model;
    }

    private ETensuModel2 createETensuModel2_OFF(ResultSet rs) throws SQLException {
        int i = 0;
        ETensuModel2 model = new ETensuModel2();
        model.setHospnum(rs.getString(++i));
        model.setH_group(rs.getString(++i));
        model.setSrycd(rs.getString(++i));
        model.setYukostymd(rs.getString(++i));
        model.setYukoedymd(rs.getString(++i));
        model.setTermid(rs.getString(++i));
        model.setOpid(rs.getString(++i));
        model.setChgymd(rs.getString(++i));
        model.setUpymd(rs.getString(++i));
        model.setUphms(rs.getString(++i));
        return model;
    }

    private ETensuModel2 createETensuModel2_SAMPLE(ResultSet rs) throws SQLException {
        int i = 0;
        ETensuModel2 model = new ETensuModel2();
        model.setH_group(rs.getString(++i));
        model.setSrycd(rs.getString(++i));
        model.setYukostymd(rs.getString(++i));
        model.setYukoedymd(rs.getString(++i));
        model.setRennum(rs.getInt(++i));
        model.setSamplecd(rs.getString(++i));
        model.setChgymd(rs.getString(++i));
        return model;
    }
    
    private ETensuModel3 createETensuModel3(ResultSet rs) throws SQLException {
        int i = 0;
        ETensuModel3 model = new ETensuModel3();
        model.setSrycd1(rs.getString(++i));
        model.setSrycd2(rs.getString(++i));
        model.setYukostymd(rs.getString(++i));
        model.setYukoedymd(rs.getString(++i));
        model.setHaihan(rs.getInt(++i));
        model.setTokurei(rs.getInt(++i));
        model.setChgymd(rs.getString(++i));
        return model;
    }

    private ETensuModel4 createETensuModel4(ResultSet rs) throws SQLException {
        int i = 0;
        ETensuModel4 model = new ETensuModel4();
        model.setN_group(rs.getString(++i));
        model.setSrycd(rs.getString(++i));
        model.setYukostymd(rs.getString(++i));
        model.setYukoedymd(rs.getString(++i));
        model.setKasan(rs.getInt(++i));
        model.setChgymd(rs.getString(++i));
        return model;
    }
    
    private ETensuModel5 createETensuModel5(ResultSet rs) throws SQLException {
        int i = 0;
        ETensuModel5 model = new ETensuModel5();
        model.setSrycd(rs.getString(++i));
        model.setYukostymd(rs.getString(++i));
        model.setYukoedymd(rs.getString(++i));
        model.setTanicd(rs.getInt(++i));
        model.setTaniname(rs.getString(++i));
        model.setKaisu(rs.getInt(++i));
        model.setTokurei(rs.getInt(++i));
        model.setChgymd(rs.getString(++i));
        return model;
    }

    public long getETensu1RowCount() {
        String condition = "et" + WHERE_ETENSU_RELATED;
        return getTableRowCount(TBL_ETENSU_1, condition);
    }
            
    private long getTableRowCount(String tableName, String condition) {

        Connection con = null;
        Statement st = null;
        
        long count = 0;

        String sql = "select count(*) from " + tableName + " " + condition;
        
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
}
