package open.dolphin.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import open.dolphin.project.Project;

/**
 * tbl_syskanriのカオスな情報を扱う
 * @author masuda, Masuda Naika
 */
public class SyskanriInfo extends SqlDaoBean {

    private static final SyskanriInfo instance;

    public static final String ORCA46 = "orca46";
    public static final String ORCA45 = "orca45";
    
    private String orcaVer;     // "orca46" or "orca45"
    
    private int hospNum;
    
    private List<Integer> syskanri1006;
    
    private int bedNum;
    
    private Map<String, String> deptCodeDescMap;
    
    private static boolean initialized = false;

    static {
        instance = new SyskanriInfo();
    }
    
    public SyskanriInfo() {
    }
    
    public static SyskanriInfo getInstance() {
        if (!initialized) {
            instance.initialize();
        }
        return instance;
    }
    
    private void initialize() {
        syskanri1006 = new ArrayList<Integer>();
        deptCodeDescMap = new HashMap<String, String>();
        initialized = setHospNum();
        initialized &= setSyskanri1006();
        initialized &= getSyskanri1001();
        initialized &= getSyskanri1005();
    }

    
    public String getOrcaVer() {
        return orcaVer;
    }
    
    public boolean isOrca46() {
        return ORCA46.equals(orcaVer);
    }
    
    @Override
    public final int getHospNum() {
        return hospNum;
    }

    public String getOrcaStaffCode(String userName) {
        
        String orcaStaffCode = "";
        
        final String sql = "select kbncd, kanritbl from tbl_syskanri where kanricd = '1010'";
        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()) {
                String kbncd = rs.getString(1);
                String orcaUserName = rs.getString(2).substring(0, 16).trim();
                if (userName.equals(orcaUserName)) {
                    orcaStaffCode = kbncd.trim();
                    break;
                }
            }
            rs.close();
        } catch (Exception e) {
            processError(e);
        } finally {
            closeStatement(st);
            closeConnection(con);
        }
        
        return orcaStaffCode;
    }
    
    // 有床か無床か
    public boolean hasBed() {
        return bedNum > 0;
    }
    
    // 病床数
    public int getBedNum() {
        return bedNum;
    }

    public boolean getSyskanriFlag(int code) {
        return syskanri1006.contains(code);
    }

    
//============================================================================//
    
    // ORCAのデータベースバージョンとhospNumを取得する
    private boolean setHospNum() {
        
        Connection con = null;
        Statement st = null;

        boolean success = true;
        hospNum = 1;
        String jmari = Project.getString(Project.JMARI_CODE);

        StringBuilder sb = new StringBuilder();
        sb.append("select hospnum, kanritbl from tbl_syskanri where kanricd='1001' and kanritbl like '%");
        sb.append(jmari);
        sb.append("%'");
        String sql = sb.toString();
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                hospNum = rs.getInt(1);
            }
            rs.close();
        } catch (Exception e) {
            processError(e);
            success = false;
        } finally {
            closeStatement(st);
            closeConnection(con);
        }

        String dbVersion = null;
        sql = "select version from tbl_dbkanri where kanricd='ORCADB00'";
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                dbVersion = rs.getString(1);
            }
            rs.close();
        } catch (Exception e) {
            processError(e);
            success = false;
        } finally {
            closeStatement(st);
            closeConnection(con);
        }
        
        if (ORCA_DB_VER46.equals(dbVersion)) {
            orcaVer = ORCA46;
        } else if (ORCA_DB_VER45.equals(dbVersion)) {
            orcaVer = ORCA45;
        }
        return success;
    }
    
    // 施設情報フラグ情報を取得する
    private boolean setSyskanri1006() {

        final String sql = "select kbncd, kanritbl from tbl_syskanri where kanricd = '1006' order by kbncd";
        boolean success = true;
        
        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            while (rs.next()) {
                int kbncd = rs.getInt(1);
                String kanritbl = rs.getString(2);
                for (int i = 0; i < kanritbl.length(); ++i) {
                    int index = (kbncd - 1) * 500 + i + 1;
                    char c = kanritbl.charAt(i);
                    if (c == '1') {
                        syskanri1006.add(index);
                    }
                }
            }
            rs.close();
        } catch (Exception e) {
            processError(e);
            success = false;
        } finally {
            closeStatement(st);
            closeConnection(con);            
        }
        
        return success;
    }

    // kanricd 1001情報を取得。今は病床数のみ
    private boolean getSyskanri1001() {
        
        boolean success = true;
        final String kanricd = "1001";
        
        List<KanriTblModel> list = getKanriTblModel(kanricd);
        if (list.isEmpty()) {
            return false;
        }
        
        KanriTblModel model = list.get(0);
        String kanritbl = model.getKanritbl();
        
        // CPSKxxx.csvを読み込むIncReaderを準備する
        IncReader reader = new IncReader(kanricd, orcaVer);

        try {
            // CPSKxxxx.csvからカラム名とデータ位置・データ長のマップを取得する
            Map<String, String> map = reader.getMap(kanritbl);
            // 病床数を取得
            String value = map.get("SYS-1001-BEDSU");
            bedNum = Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            success = false;
        } catch (UnsupportedEncodingException ex) {
            success = false;
        } catch (IOException ex) {
            success = false;
        }

        return success;
    }
    
    // kanricd 1005情報を取得。科目IDと科目名の対応
    private boolean getSyskanri1005() {
        
        boolean success = true;
        final String kanricd = "1005";
        
        List<KanriTblModel> list = getKanriTblModel(kanricd);
        if (list.isEmpty()) {
            return false;
        }
        
        // CPSKxxx.csvを読み込むIncReaderを準備する
        IncReader reader = new IncReader(kanricd, orcaVer);
        
        for (KanriTblModel model : list) {
            
            String kbncd = model.getKbncd();
            String kanritbl = model.getKanritbl();
            
            try {
                // CPSKxxxx.csvからカラム名とデータ位置・データ長のマップを取得する
                Map<String, String> map = reader.getMap(kanritbl);
                // 診療科名を取得
                String value = map.get("SYS-1005-SRYKANAME");
                deptCodeDescMap.put(kbncd, value);
            } catch (UnsupportedEncodingException ex) {
                success = false;
                break;
            } catch (IOException ex) {
                success = false;
                break;
            }
        }
        return success;
    }
    
    // 管理テーブル情報を取得する
    private List<KanriTblModel> getKanriTblModel(String kanricd) {
        
        final String sql = "select kbncd, kanritbl from tbl_syskanri where kanricd = ?";
        List<KanriTblModel> ret = new ArrayList<KanriTblModel>();
        
        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, kanricd);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String kbncd = rs.getString(1).trim();
                String kanritbl = rs.getString(2);
                KanriTblModel model = new KanriTblModel(kbncd, kanritbl);
                ret.add(model);
            }
            rs.close();

        } catch (Exception e) {
            processError(e);
        } finally {
            closePreparedStatement(ps);
            closeConnection(con);
        }
        return ret;
    }
    
    // kbncdとkanritblのペアクラス
    private class KanriTblModel {
        
        private String kbncd;
        
        private String kanritbl;
        
        private KanriTblModel(String kbncd, String kanritbl) {
            this.kbncd = kbncd;
            this.kanritbl = kanritbl;
        }
        private String getKbncd() {
            return kbncd;
        }
        private String getKanritbl() {
            return kanritbl;
        }
        
    }
}
