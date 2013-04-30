package open.dolphin.server.orca;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import open.dolphin.infomodel.OrcaSqlModel;
import org.apache.tomcat.jdbc.pool.PoolProperties;

/**
 * OrcaService
 * @author masuda, Masuda Naika
 */
public class OrcaService {
    
    private static final Logger logger = Logger.getLogger(OrcaService.class.getSimpleName());
    private static final String POSTGRES_DRIVER = "org.postgresql.Driver";
    private static final String user = "orca";
    private static final String passwd = "";
    
    private SendClaimTask task;
    private Thread thread;
    private Map<String, DataSource> dataSourceMap;
    
    private static OrcaService instance;
    
    static {
        instance = new OrcaService();
    }
    
    public static OrcaService getInstance() {
        return instance;
    }
    
    private OrcaService() {
    }
    
    public void start() {
        
        dataSourceMap = new ConcurrentHashMap<String, DataSource>();
        task = new SendClaimTask();
        thread = new Thread(task, "Claim send thread");
        thread.start();
        logger.info("Server ORCA service started.");
    }
    
    public void dispose() {
        task.stop();
        thread.interrupt();
        thread = null;
        logger.info("Server ORCA service stopped.");
    }

    public void sendClaim(AsyncContext ac) {
        task.sendClaim(ac);
    }
    
    public OrcaSqlModel executeSql(OrcaSqlModel sqlModel) {

        if (sqlModel.isPreparedStatement()) {
            executePreparedStatement(sqlModel);
        } else {
            executeStatement(sqlModel);
        }

        return sqlModel;
    }
    
    private void executeStatement(OrcaSqlModel sqlModel) {
        
        Connection con = null;
        Statement st;
        List<List<String>> valuesList = new ArrayList<List<String>>();

        try {
            con = getConnection(sqlModel.getUrl());
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sqlModel.getSql());

            while (rs.next()) {
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
                valuesList.add(values);
            }
            
            rs.close();
            st.close();
        } catch (SQLException ex) {
            sqlModel.setErrorMessage(ex.getMessage());
        } catch (ClassNotFoundException ex) {
            sqlModel.setErrorMessage(ex.getMessage());
        } catch (NullPointerException ex) {
            sqlModel.setErrorMessage(ex.getMessage());
        } finally {
            closeConnection(con);
        }

        sqlModel.setValuesList(valuesList);
    }
    
    private void executePreparedStatement(OrcaSqlModel sqlModel) {
        
        Connection con = null;
        PreparedStatement ps;
        List<List<String>> valuesList = new ArrayList<List<String>>();
        
        try {
            con = getConnection(sqlModel.getUrl());
            ps = con.prepareStatement(sqlModel.getSql());

            int paramCount = sqlModel.getParamList().size();
            for (int i = 0; i < paramCount; ++i) {
                int type = sqlModel.getTypeList().get(i);
                String param = sqlModel.getParamList().get(i);
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
                valuesList.add(values);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            sqlModel.setErrorMessage(ex.getMessage());
        } catch (ClassNotFoundException ex) {
            sqlModel.setErrorMessage(ex.getMessage());
        } catch (NullPointerException ex) {
            sqlModel.setErrorMessage(ex.getMessage());
        } finally {
            closeConnection(con);
        }

        sqlModel.setValuesList(valuesList);
    }

    private Connection getConnection(String url) 
            throws ClassNotFoundException, SQLException, NullPointerException {
        DataSource ds = getDataSource(url, user, passwd);
        return ds.getConnection();
    }
    
    private DataSource getDataSource(String url, String user, String pass) {

        DataSource ds = dataSourceMap.get(url);
        if (ds == null) {
            try {
                ds = setupDataSource(url, user, pass);
                dataSourceMap.put(url, ds);
            } catch (ClassNotFoundException ex) {
            }
        }
        return ds;
    }
    
    private DataSource setupDataSource(String url, String user, String pass) throws ClassNotFoundException {

        PoolProperties p = new PoolProperties();
        p.setDriverClassName(POSTGRES_DRIVER);
        p.setUrl(url);
        p.setUsername(user);
        p.setPassword(pass);
        p.setDefaultReadOnly(true);
        p.setMaxActive(5);
        p.setInitialSize(1);
        p.setMaxWait(5000);
        p.setRemoveAbandonedTimeout(30);
        p.setRemoveAbandoned(true);
        DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ((org.apache.tomcat.jdbc.pool.DataSource) ds).setPoolProperties(p);

        return ds;
    }
    
    private void closeConnection(Connection con) {
        try {
            con.close();
        } catch (SQLException e) {
        } catch (NullPointerException e) {
        }
    }
}
