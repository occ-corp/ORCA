package open.dolphin.updater;

import java.sql.*;
import open.dolphin.infomodel.MsdUpdaterModel;
import org.hibernate.Session;

/**
 * DbSchemaUpdater
 *
 * @author masuda, Masuda Naika
 */
public class DbSchemaUpdater extends AbstractUpdaterModule {
    
    // 無茶危なっかしいｗｗｗ

    private static final String VERSION_DATE = "2012-08-23";
    private static final String UPDATE_MEMO = "Database schema updated.";
    private static final String NO_UPDATE_MEMO = "Database schema not updated.";
    
    private static final String MYSQL = "mysql";
    private static final String POSTGRESQL = "postgresql";
    
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String TYPE_NAME = "TYPE_NAME";
    private static final String MEDIUM_BLOB = "MEDIUMBLOB";
    
    boolean updated = false;

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

            DatabaseMetaData dmd = con.getMetaData();
            String database = dmd.getDatabaseProductName().toLowerCase();
            boolean mysql = database.contains(MYSQL);
            boolean postgres = database.contains(POSTGRESQL);
            
            boolean hasMemo2 = false;
            boolean hasByomei = false;
            boolean hasByomeiToday = false;
            String columnName;
            String tableName;
            ResultSet rs;
            
            if (mysql) {
                columnName = "memo2";
                tableName = "d_patient_memo";
                rs = dmd.getColumns(null, null, tableName, columnName);
                if (rs.next()) {
                    String name = rs.getString(COLUMN_NAME);
                    hasMemo2 = columnName.equals(name);
                }
                rs.close();

                columnName = "byomei";
                tableName = "d_patient_visit";
                rs = dmd.getColumns(null, null, tableName, columnName);
                if (rs.next()) {
                    String name = rs.getString(COLUMN_NAME);
                    hasByomei = columnName.equals(name);
                }
                rs.close();

                columnName = "byomeitoday";
                tableName = "d_patient_visit";
                rs = dmd.getColumns(null, null, tableName, columnName);
                if (rs.next()) {
                    String name = rs.getString(COLUMN_NAME);
                    hasByomeiToday = columnName.equals(name);
                }
                rs.close();
            }
/*
            boolean hasCancerCare = false;
            columnName = "cancerCare";
            tableName = "d_patient";
            rs = dmd.getColumns(null, null, tableName, columnName);
            if (rs.next()) {
                String name = rs.getString(COLUMN_NAME);
                hasCancerCare = columnName.equals(name);
            }
            rs.close();

            boolean hasZaitakuSougouKanri = false;
            columnName = "zaitakuSougouKanri";
            tableName = "d_patient";
            rs = dmd.getColumns(null, null, tableName, columnName);
            if (rs.next()) {
                String name = rs.getString(COLUMN_NAME);
                hasZaitakuSougouKanri = columnName.equals(name);
            }
            rs.close();

            boolean hasHomeMedicalCare = false;
            columnName = "homeMedicalCare";
            tableName = "d_patient";
            rs = dmd.getColumns(null, null, tableName, columnName);
            if (rs.next()) {
                String name = rs.getString(COLUMN_NAME);
                hasHomeMedicalCare = columnName.equals(name);
            }
            rs.close();

            boolean hasNursingHomeMedicalCare = false;
            columnName = "nursingHomeMedicalCare";
            tableName = "d_patient";
            rs = dmd.getColumns(null, null, tableName, columnName);
            if (rs.next()) {
                String name = rs.getString(COLUMN_NAME);
                hasNursingHomeMedicalCare = columnName.equals(name);
            }
            rs.close();

*/
            boolean treeBytesIsMediumBlob = false;
            columnName = "treeBytes";
            tableName = "d_stamp_tree";
            rs = dmd.getColumns(null, null, tableName, columnName);
            if (rs.next()) {
                String type = rs.getString(TYPE_NAME);
                treeBytesIsMediumBlob = MEDIUM_BLOB.equals(type);
            }
            rs.close();
            
            boolean pub_treeBytesIsMediumBlob = false;
            columnName = "treeBytes";
            tableName = "d_published_tree";
            rs = dmd.getColumns(null, null, tableName, columnName);
            if (rs.next()) {
                String type = rs.getString(TYPE_NAME);
                pub_treeBytesIsMediumBlob = MEDIUM_BLOB.equals(type);
            }
            rs.close();

            boolean jpegByteIsMediumBlob = false;
            columnName = "jpegByte";
            tableName = "d_image";
            rs = dmd.getColumns(null, null, tableName, columnName);
            if (rs.next()) {
                String type = rs.getString(TYPE_NAME);
                jpegByteIsMediumBlob =MEDIUM_BLOB.equals(type);
            }
            rs.close();

            
            Statement stmt = con.createStatement();
            
            if (mysql) {
                logger.info("Database is MySQL.");
                
                // memo2 -> memo
                if (hasMemo2) {
                    stmt.addBatch("update d_patient_memo set memo2 = memo where memo2 is null");
                    stmt.addBatch("alter table d_patient_memo drop column memo");
                    stmt.addBatch("alter table d_patient_memo change memo2 memo mediumtext");
                }
                // StampTree.treeBytes LOB -> MEDIUMBLOB
                if (!treeBytesIsMediumBlob) {
                    stmt.addBatch("alter table d_stamp_tree modify treeBytes mediumblob");
                }
                // PublishedTree.treeBytes LOB -> MEDIUMBLOB
                if (!pub_treeBytesIsMediumBlob) {
                    stmt.addBatch("alter table d_published_tree modify treeBytes mediumblob");
                }
                // SchemaModel.jpegBytes LOB -> MEDIUMBLOB
                if (!jpegByteIsMediumBlob) {
                    stmt.addBatch("alter table d_image modify jpegByte mediumblob");
                }
/*
                // delete santei info from d_patient
                if (hasCancerCare) {
                    stmt.addBatch("alter table d_patient drop column cancerCare");
                }
                if (hasZaitakuSougouKanri) {
                    stmt.addBatch("alter table d_patient drop column zaitakuSougouKanri");
                }
                if (hasHomeMedicalCare) {
                    stmt.addBatch("alter table d_patient drop column homeMedicalCare");
                }
                if (hasNursingHomeMedicalCare) {
                    stmt.addBatch("alter table d_patient drop column nursingHomeMedicalCare");
                }
*/
                // delete byomei column
                if (hasByomei) {
                    stmt.addBatch("alter table d_patient_visit drop column byomei");
                }
                // delete byomeiCount column
                if (hasByomeiToday ){
                    stmt.addBatch("alter table d_patient_visit drop column byomeitoday");
                }
                
                try {
                    stmt.executeBatch();
                    updated = true;
                } catch (Exception e) {
                    updated = false;
                }

                stmt.close();
             }

            if (postgres) {
                logger.info("Database is PostgreSQL.");
                
                // memo2 -> memo
                if (hasMemo2) {
                    stmt.addBatch("update d_patient_memo set memo2 = memo where memo2 is null");
                    stmt.addBatch("alter table d_patient_memo drop column memo");
                    stmt.addBatch("alter table d_patient_memo rename memo2 to memo");
                }
/*
                // delete santei info from d_patient
                if (hasCancerCare) {
                    stmt.addBatch("alter table d_patient drop column cancerCare");
                }
                if (hasZaitakuSougouKanri) {
                    stmt.addBatch("alter table d_patient drop column zaitakuSougouKanri");
                }
                if (hasHomeMedicalCare) {
                    stmt.addBatch("alter table d_patient drop column homeMedicalCare");
                }
                if (hasNursingHomeMedicalCare) {
                    stmt.addBatch("alter table d_patient drop column nursingHomeMedicalCare");
                }
*/
                // delete byomei column
                if (hasByomei) {
                    stmt.addBatch("alter table d_patient_visit drop column byomei");
                }
                // delete byomeiCount column
                if (hasByomeiToday) {
                    stmt.addBatch("alter table d_patient_visit drop column byomeiToday");
                }
                
                try {
                    stmt.executeBatch();
                    updated = true;
                } catch (Exception e) {
                    updated = false;
                }

                stmt.close();
            }
        }
    }
}
