package open.dolphin.dao;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import open.dolphin.delegater.OrcaDelegater;
import open.dolphin.infomodel.DiseaseEntry;
import open.dolphin.infomodel.OrcaSqlModel;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.project.Project;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

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

    protected static final String ORCA_DB_VER45 = "040500-1";
    protected static final String ORCA_DB_VER46 = "040600-1";
    protected static final String ORCA_DB_VER47 = "040700-1";

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
            "select byomeicd,byomei,byomeikana,icd10_1,haisiymd,tokskncd "
            + "from tbl_byomei ";

    protected static final String HOSPNUM_SRYCD = " and hospnum = ? order by srycd";
    
    protected static final DecimalFormat srycdFrmt = new DecimalFormat("000000000");
    
    private static DataSource dataSource;
    
    
    /**
     * Creates a new instance of SqlDaoBean
     */
    public SqlDaoBean() {
        // DataSourceを設定
        setDriver(DRIVER);
        setHost(Project.getString(Project.CLAIM_ADDRESS));
        setPort(PORT);
        setDatabase(DATABASE);
        setUser(USER);
        setPasswd(PASSWD);
    }

    protected DiseaseEntry getDiseaseEntry(List<String> values) {

        DiseaseEntry de = new DiseaseEntry();
        de.setCode(values.get(0));        // Code
        de.setName(values.get(1));        // Name
        de.setKana(values.get(2));         // Kana
        de.setIcdTen(values.get(3));      // IcdTen
        de.setDisUseDate(values.get(4));  // DisUseDate
        de.setByoKanrenKbn(Integer.valueOf(values.get(5)));
        return de;
    }
    
    protected TensuMaster getTensuMaster(List<String> values) {

        TensuMaster tm = new TensuMaster();
        tm.setSrycd(values.get(0));
        tm.setName(values.get(1));
        tm.setKananame(values.get(2));
        tm.setTaniname(values.get(3));
        tm.setTensikibetu(values.get(4));
        tm.setTen(values.get(5));
        tm.setNyugaitekkbn(values.get(6));
        tm.setRoutekkbn(values.get(7));
        tm.setSrysyukbn(values.get(8));
        tm.setHospsrykbn(values.get(9));
        tm.setYkzkbn(values.get(10));
        tm.setYakkakjncd(values.get(11));
        tm.setYukostymd(values.get(12));
        tm.setYukoedymd(values.get(13));
        tm.setDataKbn(values.get(14));
        return tm;
    }
    
    private List<String> getColumnValues(ResultSet rs) throws SQLException {

        List<String> values = new ArrayList<String>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; ++i) {
            int type = meta.getColumnType(i);
            switch (type) {
                case Types.SMALLINT:
                case Types.NUMERIC:
                case Types.INTEGER:
                    values.add(String.valueOf(rs.getInt(i)));
                    break;
                default:
                    values.add(rs.getString(i));
                    break;
            }
        }
        return values;
    }
    
    private boolean isClient() {
        String str = Project.getString(Project.CLAIM_SENDER);
        boolean client = (str == null || Project.CLAIM_CLIENT.equals(str));
        return client;
    }
    
    protected List<List<String>> executeStatement(String sql) {
        
        if (isClient()) {
            return executeStatement1(sql);
        }
        return executeStatement2(sql);
    }
   
    private List<List<String>> executeStatement1(String sql) {

        Connection con = null;
        Statement st = null;
        List<List<String>> valuesList = new ArrayList<List<String>>();

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                List<String> values = getColumnValues(rs);
                valuesList.add(values);
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

        return valuesList;
    }
    
    private List<List<String>> executeStatement2(String sql) {
        
        OrcaSqlModel sqlModel = new OrcaSqlModel();
        sqlModel.setUrl(getURL());
        sqlModel.setSql(sql);

        try {
            OrcaSqlModel result = OrcaDelegater.getInstance().executeQuery(sqlModel);
            if (result != null) {
                String errMsg = result.getErrorMessage();
                if (errMsg != null) {
                    processError(new SQLException(result.getErrorMessage()));
                } else {
                    return result.getValuesList();
                }
            }
        } catch (Exception ex) {
        }

        return Collections.emptyList();
    }

    protected List<List<String>> executePreparedStatement(String sql, int[] types, String[] params) {

        if (isClient()) {
            return executePreparedStatement1(sql, types, params);
        }
        return executePreparedStatement2(sql, types, params);
    }
    
    private List<List<String>> executePreparedStatement1(String sql, int[] types, String[] params) {

        Connection con = null;
        PreparedStatement ps = null;
        List<List<String>> valuesList = new ArrayList<List<String>>();

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            
            for (int i = 0; i < types.length; ++i) {
                int type = types[i];
                String param = params[i];
                switch (type) {
                    case Types.INTEGER:
                        ps.setInt(i + 1, Integer.valueOf(param));
                        break;
                    case Types.BIGINT:
                        ps.setLong(i + 1, Long.valueOf(param));
                        break;
                    case Types.FLOAT:
                        ps.setFloat(i + 1, Float.valueOf(param));
                        break;
                    case Types.CHAR:
                    default:
                        ps.setString(i + 1, param);
                        break;
                }
            }
            
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                List<String> values = getColumnValues(rs);
                valuesList.add(values);
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

        return valuesList;
    }
    
    private List<List<String>> executePreparedStatement2(String sql, int[] types, String[] params) {
        
        OrcaSqlModel sqlModel = new OrcaSqlModel();
        sqlModel.setPreparedStatement(true);
        sqlModel.setUrl(getURL());
        sqlModel.setSql(sql);
        
        for (int i = 0; i < types.length; ++i) {
            int type = types[i];
            switch (type) {
                case Types.INTEGER:
                case Types.FLOAT:
                case Types.BIGINT:
                    sqlModel.addParameter(type, String.valueOf(params[i]));
                    break;
                case Types.CHAR:
                default:
                    sqlModel.addParameter(type, params[i]);
                    break;
            }
        }
        try {
            OrcaSqlModel result = OrcaDelegater.getInstance().executeQuery(sqlModel);

            if (result != null) {
                String errMsg = result.getErrorMessage();
                if (errMsg != null) {
                    processError(new SQLException(result.getErrorMessage()));
                } else {
                    return result.getValuesList();
                }
            }
        } catch (Exception ex) {
        }
        
        return Collections.emptyList();
    }
    
    protected final int getHospNum() {
        return SyskanriInfo.getInstance().getHospNumFromSysKanriInfo();
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

    // ORCAのptidを取得する
    protected final long getOrcaPtID(String patientId){

        long ptid = 0;
        int hospNum = getHospNum();
        
        final String sql = "select ptid from tbl_ptnum where hospnum = ? and ptnum = ?";
        
        int[] types = {Types.INTEGER, Types.CHAR};
        String[] params = {String.valueOf(hospNum), patientId};
        
        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        if (!valuesList.isEmpty()) {
            List<String> values = valuesList.get(0);
            ptid = Long.valueOf(values.get(0));
        }

        return ptid;
    }

    public Connection getConnection() throws Exception {
        
        if (dataSource == null) {
            setupDataSource();
        }
        // 呼び出し前にエラーをクリア。シングルトン化の影響ｗ
        errorCode = TT_NO_ERROR;
        errorMessage = null;
        
        return dataSource.getConnection();
    }
    
    private void setupDataSource() {
        PoolProperties p = new PoolProperties();
        p.setDriverClassName(getDriver());
        p.setUrl(getURL());
        p.setUsername(getUser());
        p.setPassword(getPasswd());
        p.setDefaultReadOnly(true);
        dataSource = new DataSource();
        dataSource.setPoolProperties(p);
    }

    public String getDriver() {
        return driver;
    }

    public final void setDriver(String driver) {
        this.driver = driver;
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
     * To make sql statement ('xxxx',)
     */
    public String addSingleQuoteComa(String s) {
        StringBuilder buf = new StringBuilder();
        buf.append("'");
        buf.append(s);
        buf.append("',");
        return buf.toString();
    }
    
    public void closeStatement(Statement st) {
        try {
            st.close();
        } catch (SQLException e) {
        } catch (NullPointerException e) {
        }
    }

    public void closePreparedStatement(PreparedStatement ps) {
        try {
            ps.close();
        } catch (SQLException e) {
        } catch (NullPointerException e) {
        }
    }

    public void closeConnection(Connection con) {
        try {
            con.close();
        } catch (SQLException e) {
        } catch (NullPointerException e) {
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
        try {
            con.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
