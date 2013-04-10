package open.dolphin.updater;

import java.sql.*;
import java.util.*;
import open.dolphin.infomodel.MsdUpdaterModel;
import org.hibernate.Session;

/**
 * RoutineMedUpdater
 * @author masuda, Masuda Naika
 */
public class RoutineMedUpdater extends AbstractUpdaterModule {
    
    private static final String VERSION_DATE = "2013-01-19";
    private static final String UPDATE_MEMO = "RoutineMed updated.";
    private static final String NO_UPDATE_MEMO = "RoutineMed not updated.";
    
    private boolean updated = false;

    @Override
    public String getVersionDateStr() {
        return VERSION_DATE;
    }

    @Override
    public String getModuleName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public MsdUpdaterModel start() {
        
        Session hibernateSession = em.unwrap(Session.class);
        hibernateSession.doWork(new UpdateWork());

        return updated
                ? getResult(UPDATE_MEMO)
                : getResult(NO_UPDATE_MEMO);
    }
    
    private class UpdateWork implements org.hibernate.jdbc.Work {

        @Override
        public void execute(Connection con) throws SQLException {
            
            final String sql1 = "select RoutineMedModel_id, moduleList_id from msd_routinemed_modulelist";
            final String sql2 = "update msd_routinemed set moduleIds = ? where id = ?";
            final String sql3 = "delete from msd_routinemed where moduleIds is null";
            final String sql4 = "drop table msd_routinemed_modulelist";
            
            Map<Long, String> map = new HashMap<Long, String>();
            
            Statement stmt = null;
            PreparedStatement ps = null;
            
            try {
                List<String> tblList = getTableList(con);
                if (!tblList.contains("msd_routinemed_modulelist")) {
                    updated = false;
                    return;
                }
                
                // ModuleModelのidリストを取得する
                stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql1);
                while (rs.next()) {
                    long routineMedId = rs.getLong(1);
                    long moduleId = rs.getLong(2);
                    String ids = map.get(routineMedId);
                    if (ids == null) {
                        ids = String.valueOf(moduleId);
                    } else {
                        ids += "," + String.valueOf(moduleId);
                    }
                    map.put(routineMedId, ids);
                }
                rs.close();
                stmt.close();

                // msd_routinemedテーブルにidリストを記録する
                ps = con.prepareStatement(sql2);
                for (Map.Entry entry : map.entrySet()) {
                    Long moduleId = (Long) entry.getKey();
                    String ids = (String) entry.getValue();
                    ps.setString(1, ids);
                    ps.setLong(2, moduleId);
                    ps.executeUpdate();
                }
                ps.close();
                
                // ゾンビを削除
                stmt = con.createStatement();
                stmt.executeUpdate(sql3);
                stmt.close();

                // msd_routinemed_modulelistテーブルを削除する
                stmt = con.createStatement();
                stmt.executeUpdate(sql4);
                stmt.close();
                updated = true;
            } catch (Exception ex) {
                System.err.println(ex);
                updated = false;
            } finally {
                try {
                    stmt.close();
                } catch (Exception ex) {
                }
                try {
                    ps.close();
                } catch (Exception ex) {
                }
            }
        }

        private List<String> getTableList(Connection connection) throws SQLException {

            List<String> result = new ArrayList<String>();
            ResultSet rs = null;
            try {
                final String[] types = {"TABLE"};
                DatabaseMetaData dbm = connection.getMetaData();
                rs = dbm.getTables(null, null, null, types);
                while (rs.next()) {
                    String tblName = rs.getString("TABLE_NAME").toLowerCase();
                    result.add(tblName);
                }
            } catch (SQLException e) {
                throw e;
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                    }
                }
            }
            return result;
        }
    }
}
