package open.dolphin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
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
    
    private static final String QUERY_DISEASE_BY_NAME_45 =
            QUERY_DISEASE_BY_NAME.replace("icd10_1", "icd10");
    
    private static final String QUERY_DISEASE_BY_CODE = SELECT_TBL_BYOMEI
            + "where byomeicd ~ ? and haisiymd >= ?";
    
    private static final String QUERY_DISEASE_BY_CODE_45 = 
            QUERY_DISEASE_BY_CODE.replace("icd10_1", "icd10");
    
    
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
        
        int[] types = {Types.CHAR, Types.CHAR, Types.CHAR, Types.INTEGER};
        String[] params = {shinku, now, now, String.valueOf(hospNum)};
        
        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        for (List<String> values : valuesList) {
            TensuMaster t = getTensuMaster(values);
            ret.add(t);
        }

        return ret;
    }

    public List<TensuMaster> getTensuMasterByName(String name, String now, boolean partialMatch) {

        // 結果を格納するリスト
        List<TensuMaster> ret = new ArrayList<TensuMaster>();

        // 半角英数字を全角へ変換する
        name = StringTool.toZenkakuUpperLower(name);
        String kana = StringTool.hiraganaToKatakana(name);

        // SQL 文
        boolean one = (name.length() == 1);
        StringBuilder buf = new StringBuilder();
        if (one) {
            buf.append(QUERY_TENSU_BY_1_NAME);
        } else {
            buf.append(QUERY_TENSU_BY_NAME);
            if (!partialMatch) {
                name = "^" + name;
                kana = "^" + kana;
            }
        }
        String sql = buf.toString();
        int hospNum = getHospNum();
        
        int[] types = {Types.CHAR, Types.CHAR, Types.CHAR, Types.CHAR, Types.INTEGER};
        String[] params = {name, kana, now, now, String.valueOf(hospNum)};
        
        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        for (List<String> values : valuesList) {
            TensuMaster t = getTensuMaster(values);
            ret.add(t);
        }

        return ret;
    }

    public List<TensuMaster> getTensuMasterByCode(String regExp, String now) {

        // 結果を格納するリスト
        List<TensuMaster> ret = new ArrayList<TensuMaster>();

        // SQL 文
        String sql = QUERY_TENSU_BY_CODE;
        int hospNum = getHospNum();
        
        int[] types = {Types.CHAR, Types.CHAR, Types.CHAR, Types.INTEGER};
        String[] params = {"^" + regExp, now, now, String.valueOf(hospNum)};
        
        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        for (List<String> values : valuesList) {
            TensuMaster t = getTensuMaster(values);
            ret.add(t);
        }

        return ret;
    }

    public List<TensuMaster> getTensuMasterByTen(String ten, String now) {

        // 結果を格納するリスト
        List<TensuMaster> ret = new ArrayList<TensuMaster>();

        // SQL 文
        String sql =QUERY_TENSU_BY_TEN;
        int hospNum = getHospNum();
        
        String[] tens = ten.split("-");
        int[] types = {Types.FLOAT, Types.FLOAT, Types.CHAR, Types.CHAR, Types.INTEGER};
        String[] params = {null, null, now, now, String.valueOf(hospNum)};
        if (tens.length > 1) {
            params[0] = tens[0];
            params[1] = tens[1];
        } else {
            params[0] = tens[0];
            params[1] = tens[0];
        }

        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        for (List<String> values : valuesList) {
            TensuMaster t = getTensuMaster(values);
            ret.add(t);
        }

        return ret;
    }

    public List<DiseaseEntry> getDiseaseByName(String name, String now, boolean partialMatch) {

        // 結果を格納するリスト
        List<DiseaseEntry> ret = new ArrayList<DiseaseEntry>();

        // SQL 文
        String sql = SyskanriInfo.getInstance().isOrca45()
                ? QUERY_DISEASE_BY_NAME_45
                : QUERY_DISEASE_BY_NAME;

        if (!partialMatch) {
            name = "^" + name;
        }

        int[] types = {Types.CHAR, Types.CHAR, Types.CHAR};
        String[] params = {name, name, now};
        
        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        for (List<String> values : valuesList) {
            DiseaseEntry d = getDiseaseEntry(values);
            ret.add(d);
        }

        return ret;
    }
    
    public List<DiseaseEntry> getDiseaseByCode(String code, String now, boolean partialMatch) {

        // 結果を格納するリスト
        List<DiseaseEntry> ret = new ArrayList<DiseaseEntry>();

        // SQL 文
        String sql = SyskanriInfo.getInstance().isOrca45()
                ? QUERY_DISEASE_BY_CODE_45
                : QUERY_DISEASE_BY_CODE;

        Connection con = null;
        PreparedStatement ps;

        if (!partialMatch) {
            code = "^" + code;
        }
        
        int[] types = {Types.CHAR, Types.CHAR};
        String[] params = {code, now};
        
        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        
        for (List<String> values : valuesList) {
            DiseaseEntry d = getDiseaseEntry(values);
            ret.add(d);
        }

        return ret;
    }
}