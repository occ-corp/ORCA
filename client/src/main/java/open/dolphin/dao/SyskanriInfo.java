package open.dolphin.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Types;
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
    public static final String ORCA47 = "orca47";
    
    private String orcaVer;     // "orca46" or "orca45"
    
    private int hospNum;
    
    // 施設基準でflagが立っている物の番号リスト, 1006
    private List<Integer> syskanri1006;
    // 病床数, 1001
    private int bedNum;
    // ORCAに登録されている診療科IDと診療科名のマップ, 1005
    private Map<String, String> deptCodeDescMap;
    // ORCAに登録されている職員IDと氏名のマップ, 1010
    private Map<String, String> staffCodeNameMap;
    private Map<String, String> orcaUserCodeIdMap;
    
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
        staffCodeNameMap = new HashMap<String, String>();
        orcaUserCodeIdMap = new HashMap<String, String>();
        
        initialized = setHospNum();
        initialized &= setSyskanri1006();
        initialized &= getSyskanri1001();
        initialized &= getSyskanri1005();
        initialized &= getSyskanri1010();
    }

    public boolean isOrca45() {
        return ORCA45.equals(orcaVer);
    }
    
    public boolean isOrca46() {
        return ORCA46.equals(orcaVer);
    }
    
    public boolean isOrca47() {
        return ORCA47.equals(orcaVer);
    }
    
    public final int getHospNumFromSysKanriInfo() {
        return hospNum;
    }
    
    
//============================================================================//
    
    // 有床か無床か
    public boolean hasBed() {
        return bedNum > 0;
    }
    
    // 病床数
    public int getBedNum() {
        return bedNum;
    }
    
    // ORCAの診療科名を返す
    public String getOrcaDeptDesc(String code) {
        return deptCodeDescMap.get(code);
    }

    // 施設基準が設定されているかどうかを返す
    public boolean getSyskanriFlag(int code) {
        return syskanri1006.contains(code);
    }

    // ORCA職員IDから名前を取得する
    public String getOrcaStaffName(String code) {
        return staffCodeNameMap.get(code);
    }
    
    // ORCA職員名から職員IDを取得する
    public String getOrcaStaffCode(String userName) {
        return orcaUserCodeIdMap.get(userName);
    }
    
//============================================================================//
    
    // ORCAのデータベースバージョンとhospNumを取得する
    private boolean setHospNum() {
        
        boolean success = true;
        hospNum = 1;
        String jmari = Project.getString(Project.JMARI_CODE);

        StringBuilder sb = new StringBuilder();
        sb.append("select hospnum, kanritbl from tbl_syskanri where kanricd='1001' and kanritbl like '%");
        sb.append(jmari);
        sb.append("%'");
        String sql = sb.toString();
        
        List<List<String>> valuesList = executeStatement(sql);
        if (!valuesList.isEmpty()) {
            List<String> values = valuesList.get(0);
            hospNum = Integer.valueOf(values.get(0));
        }

        String dbVersion = null;
        sql = "select version from tbl_dbkanri where kanricd='ORCADB00'";
        
        valuesList = executeStatement(sql);
        if (!valuesList.isEmpty()) {
            List<String> values = valuesList.get(0);
            dbVersion = values.get(0);
        }

        if (ORCA_DB_VER45.equals(dbVersion)) {
            orcaVer = ORCA45;
        } else if (ORCA_DB_VER46.equals(dbVersion)) {
            orcaVer = ORCA46;
        } else {
            orcaVer = ORCA47;
        }
        return success;
    }
    
    // 施設情報フラグ情報を取得する
    private boolean setSyskanri1006() {

        final String sql = "select kbncd, kanritbl from tbl_syskanri where kanricd = '1006' order by kbncd";
        boolean success = true;
        
        List<List<String>> valuesList = executeStatement(sql);
        
        for (List<String> values : valuesList) {
            int kbncd = Integer.valueOf(values.get(0).trim());
            String kanritbl = values.get(1);
            for (int i = 0; i < kanritbl.length(); ++i) {
                int index = (kbncd - 1) * 500 + i + 1;
                char c = kanritbl.charAt(i);
                if (c == '1') {
                    syskanri1006.add(index);
                }
            }
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
    
    // ORCAの職員情報を取得する
    private boolean getSyskanri1010() {
        
        boolean success = true;
        final String kanricd = "1010";
        
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
                // 職員名を取得
                String value = map.get("SYS-1010-NAME");
                staffCodeNameMap.put(kbncd, value);
                // ORCA user IDとstaff code
                value = map.get("SYS-1010-USERID");
                orcaUserCodeIdMap.put(value, kbncd);
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
        
        int[] types = {Types.CHAR};
        String[] params = {kanricd};
        
        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        for (List<String> values : valuesList) {
            // kbccdは余分な空白は除去
            String kbncd = values.get(0).trim();
            // これがカオスのkanritbl。こっちはそのままで
            String kanritbl = values.get(1);
            KanriTblModel model = new KanriTblModel(kbncd, kanritbl);
            ret.add(model);
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
