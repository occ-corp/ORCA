package open.dolphin.dao;

import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import javax.sql.DataSource;
import open.dolphin.infomodel.DiseaseEntry;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.project.Project;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * SqlDaoBean
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public class SqlDaoBean extends DaoBean {

    private static final String DRIVER = "org.postgresql.Driver";
    private static final int PORT = 5432;
    private static final String DATABASE = "orca";
    private static final String USER = "orca";
    private static final String PASSWD = "";
    
    private String dataBase;
    private String driver;
    private boolean trace = true;

    /**
     * Creates a new instance of SqlDaoBean
     */
    public SqlDaoBean() {
//masuda^
        // DataSourceを設定
        if (dataSource == null) {
            setupDataSource();
        }
        setHospNum();
//masuda$
    }

//masuda^
    protected static final String ORCA_DB_VER45 = "040500-1";
    protected static final String ORCA_DB_VER46 = "040600-1";

    protected static final String SELECT_TBL_TENSU =
            "select srycd,name,kananame,taniname,tensikibetu,"
            + "ten,nyugaitekkbn,routekkbn,srysyukbn,hospsrykbn,"
            + "ykzkbn,yakkakjncd,yukostymd,yukoedymd,datakbn "
            + "from tbl_tensu ";
    
    protected static final String SELECT_TBL_TENSU2 =
            "select t.srycd,t.name,t.kananame,t.taniname,t.tensikibetu,"
            + "t.ten,t.nyugaitekkbn,t.routekkbn,t.srysyukbn,t.hospsrykbn,"
            + "t.ykzkbn,t.yakkakjncd,t.yukostymd,t.yukoedymd,t.datakbn "
            + "from tbl_tensu t ";
    
    protected static final String SELECT_TBL_BYOMEI =
            "select byomeicd,byomei,byomeikana,icd10,haisiymd,tokskncd "
            + "from tbl_byomei ";

    protected static final String HOSPNUM_SRYCD = " and hospnum = ? order by srycd";
    
    protected static final SimpleDateFormat yyyyMMddFrmt = new SimpleDateFormat("yyyyMMdd");
    protected static final DecimalFormat srycdFrmt = new DecimalFormat("000000000");
    
    
    private static DataSource dataSource;
    private static int hospNum;
    private static String dbVersion;

    
    protected DiseaseEntry getDiseaseEntry(ResultSet rs) throws SQLException {

        DiseaseEntry de = new DiseaseEntry();
        de.setCode(rs.getString(1));        // Code
        de.setName(rs.getString(2));        // Name
        de.setKana(rs.getString(3));         // Kana
        de.setIcdTen(rs.getString(4));      // IcdTen
        de.setDisUseDate(rs.getString(5));  // DisUseDate
        de.setByoKanrenKbn(rs.getInt(6));
        return de;
    }

    protected TensuMaster getTensuMaster(ResultSet rs) throws SQLException {

        TensuMaster tm = new TensuMaster();
        tm.setSrycd(rs.getString(1));
        tm.setName(rs.getString(2));
        tm.setKananame(rs.getString(3));
        tm.setTaniname(rs.getString(4));
        tm.setTensikibetu(rs.getString(5));
        tm.setTen(rs.getString(6));
        tm.setNyugaitekkbn(rs.getString(7));
        tm.setRoutekkbn(rs.getString(8));
        tm.setSrysyukbn(rs.getString(9));
        tm.setHospsrykbn(rs.getString(10));
        tm.setYkzkbn(rs.getString(11));
        tm.setYakkakjncd(rs.getString(12));
        tm.setYukostymd(rs.getString(13));
        tm.setYukoedymd(rs.getString(14));
        tm.setDataKbn(rs.getString(15));
        return tm;
    }
    // ひらがなをカタカナに変換
    protected String hiraganaToKatakana(String input) {

        final String Hiragana = "あいうえおかきくけこさしすせそ" +
                                "たちつてとなにぬねのはひふへほ" +
                                "まみむめもやゆよらりるれろわを" +
                                 "んっゃゅょぁぃぅぇぉゎ" +
                                "がぎぐげござじずぜぞだぢづでど" +
                                "ばびぶべぼぱぴぷぺぽ";
        final String Katakana = "アイウエオカキクケコサシスセソ" +
                                "タチツテトナニヌネノハヒフヘホ" +
                                "マミムメモヤユヨラリルレロワヲ" +
                                "ンッャュョァィゥェォヮ" +
                                "ガギグゲゴザジズゼゾダヂヅデド" +
                                "バビブベボパピプペポ";

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); ++i) {
            int pos = Hiragana.indexOf(input.substring(i, i + 1));
            if (pos != -1) {
                output.append(Katakana.substring(pos, pos + 1));
            } else {
                output.append(input.substring(i, i + 1));
            }
        }
        return output.toString();
    }

    // srycdのListからカンマ区切りの文字列を作る
    protected String getCodes(Collection<String> srycdList){

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String srycd : srycdList){
            if (!first){
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(addSingleQuote(srycd));
        }
        return sb.toString();
    }
    
    // ORCA 4.6対応
    protected String getOrcaDbVersion() {
        return dbVersion;
    }
    protected int getHospNum() {
        return hospNum;
    }

    // ORCAのデータベースバージョンとhospNumを取得する
    private void setHospNum() {

        if (dbVersion != null) {
            return;
        }

        Connection con = null;
        Statement st = null;
        String sql = null;
        hospNum = 1;
        String jmari = Project.getString(Project.JMARI_CODE);

        StringBuilder sb = new StringBuilder();
        sb.append("select hospnum, kanritbl from tbl_syskanri where kanricd='1001' and kanritbl like '%");
        sb.append(jmari);
        sb.append("%'");
        sql = sb.toString();
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                hospNum = rs.getInt(1);
            }
        } catch (Exception e) {
            processError(e);
            closeConnection(con);
            closeStatement(st);
        }

        sql = "select version from tbl_dbkanri where kanricd='ORCADB00'";
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                dbVersion = rs.getString(1);
            }
        } catch (Exception e) {
            processError(e);
            closeConnection(con);
            closeStatement(st);
        }
    }

    // ORCAのptidを取得する
    protected final long getOrcaPtID(String patientId){

        long ptid = 0;

        final String sql = "select ptid from tbl_ptnum where hospnum = ? and ptnum = ?";
        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, getHospNum());
            ps.setString(2, patientId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ptid = rs.getLong(1);
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

        return ptid;
    }
    
    private void setupDataSource() {
        
        setDriver(DRIVER);
        setHost(Project.getString(Project.CLAIM_ADDRESS));
        setPort(PORT) ;
        setDatabase(DATABASE);
        setUser(USER);
        setPasswd(PASSWD);
        
        ObjectPool connectionPool = new GenericObjectPool();
        ConnectionFactory connectionFactory = 
                new DriverManagerConnectionFactory(getURL(), getUser(), getPasswd());
        PoolableConnectionFactory poolableConnectionFactory =
                new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, true, true);
        dataSource = new PoolingDataSource(connectionPool);
    }

    // エラーを消去する。getConnection時に呼ばれる
    private void clearErrors() {
        errorCode = TT_NO_ERROR;
        errorMessage = null;
    }
//masuda$
    
    public Connection getConnection() throws Exception {
//masuda^
        // 呼び出し前にエラーをクリア。シングルトン化の影響ｗ
        clearErrors();
        return dataSource.getConnection();
        //return DriverManager.getConnection(getURL(), user, passwd);
//masuda$
    }

    public String getDriver() {
        return driver;
    }

    public final void setDriver(String driver) {

        this.driver = driver;

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException cnfe) {
            logger.warn("Couldn't find the driver!");
            logger.warn("Let's print a stack trace, and exit.");
            cnfe.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public final String getDatabase() {
        return dataBase;
    }

    public final void setDatabase(String base) {
        dataBase = base;
    }

    protected final String getURL() {
        StringBuilder buf = new StringBuilder();
        buf.append("jdbc:postgresql://");
        buf.append(host);
        buf.append(":");
        buf.append(port);
        buf.append("/");
        buf.append(dataBase);
        return buf.toString();
    }
     
    public boolean getTrace() {
        return trace;
    }
    
    public void setTrace(boolean b) {
        trace = b;
    }

    public String addSingleQuote(String s) {
        StringBuilder buf = new StringBuilder();
        buf.append("'");
        buf.append(s);
        buf.append("'");
        return buf.toString();
    }

    /**
     * To make sql statement ('xxxx',)<br>
     */
    public String addSingleQuoteComa(String s) {
        StringBuilder buf = new StringBuilder();
        buf.append("'");
        buf.append(s);
        buf.append("',");
        return buf.toString();
    }
    
    public void closeStatement(Statement st) {
        if (st != null) {
            try {
                st.close();
            }
            catch (SQLException e) {
            	e.printStackTrace(System.err);
            }
        }
    }

    public void closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
            }
            catch (SQLException e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    protected void debug(String msg) {
        logger.debug(msg);
    }
    
    protected void printTrace(String msg) {
        if (trace) {
            logger.debug(msg);
        }
    }
    
    protected void rollback(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
