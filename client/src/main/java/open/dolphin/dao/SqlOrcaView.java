package open.dolphin.dao;

import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import open.dolphin.client.ClientContext;
import open.dolphin.infomodel.RegisteredDiagnosisModel;

/**
 * ORCA に登録してある病名を検索するクラス。
 *
 * @author Minagawa, Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class SqlOrcaView extends SqlDaoBean {
    
    private static final String SELECT_TBL_PTBYOMEI =
            "select byomeicd_1, byomeicd_2, byomeicd_3, byomeicd_4,  byomeicd_5, "
            + "byomeicd_6, byomeicd_7, byomeicd_8, byomeicd_9,  byomeicd_10, "
            + "byomeicd_11, byomeicd_12, byomeicd_13, byomeicd_14,  byomeicd_15, "
            + "byomeicd_16, byomeicd_17, byomeicd_18, byomeicd_19,  byomeicd_20, "
            + "byomeicd_21, sryymd, utagaiflg, syubyoflg, tenkikbn, "
            + "tenkiymd, byomei "
            + "from tbl_ptbyomei ";
    
    private static final String WHERE_WITH_HOSPNUM =
            "where ptid=? and sryymd >= ? and sryymd <= ? and dltflg!=? and hospnum=? order by sryymd ";
    
    //private static final String WHERE_WO_HOSPNUM =
    //        "where ptid=? and sryymd >= ? and sryymd <= ? and dltflg!=? order by sryymd ";
    
    private static final String WHERE_WITH_HOSPNUM_AO =
            "where ptid=? and tenkikbn=? and dltflg!=? and hospnum=? order by sryymd ";
    
    private static final String WHERE_WO_HOSPNUM_AO=
            "where ptid=? and tenkikbn=? and dltflg!=? order by sryymd ";
    
    private static final String DESC = "desc";
    
    
    private static final SqlOrcaView instance;

    static {
        instance = new SqlOrcaView();
    }

    public static SqlOrcaView getInstance() {
        return instance;
    }
    
    /**
     * Creates a new instance of SqlOrcaView
     */
    private SqlOrcaView() {
    }
    
    /**
     * ORCA に登録してある病名を検索する。
     * @return RegisteredDiagnosisModelのリスト
     */
    public List<RegisteredDiagnosisModel> getOrcaDisease(String patientId, String from, String to, boolean ascend) {
        
        int hospNum = getHospNum();
        long orcaPtId = getOrcaPtID(patientId);
        if (orcaPtId == 0) {
            ClientContext.getBootLogger().warn("ptid=null");
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_TBL_PTBYOMEI);
        sb.append(WHERE_WITH_HOSPNUM);
        if (!ascend) {
            sb.append(DESC);
        }

        String sql = sb.toString();
        ClientContext.getBootLogger().debug(sql);

        int[] types = {Types.BIGINT, Types.CHAR, Types.CHAR, Types.CHAR, Types.INTEGER};
        String[] params = {String.valueOf(orcaPtId), from, to, "1", String.valueOf(hospNum)};

        List<List<String>> valuesList = executePreparedStatement(sql, types, params);

        List<RegisteredDiagnosisModel> collection = new ArrayList<RegisteredDiagnosisModel>();

        for (List<String> values : valuesList) {
            RegisteredDiagnosisModel ord = getRegisteredDiagnosisModel(values);
            collection.add(ord);
        }

        return collection;
    }

    /**
     * ORCA に登録してある直近の病名を検索する。
     *
     * @return RegisteredDiagnosisModelのリスト
     */
    public List<RegisteredDiagnosisModel> getActiveOrcaDisease(String patientId, boolean asc) {

        int hospNum = getHospNum();
        long orcaPtId = getOrcaPtID(patientId);
        if (orcaPtId == 0) {
            ClientContext.getBootLogger().warn("ptid=null");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_TBL_PTBYOMEI);
        sb.append(WHERE_WITH_HOSPNUM_AO);
        if (!asc) {
            sb.append(DESC);
        }

        String sql = sb.toString();
        ClientContext.getBootLogger().debug(sql);

        int[] types = {Types.BIGINT, Types.CHAR, Types.CHAR, Types.INTEGER};
        String[] params = {String.valueOf(orcaPtId), " ", "1", String.valueOf(hospNum)};

        List<List<String>> valuesList = executePreparedStatement(sql, types, params);
        List<RegisteredDiagnosisModel> collection = new ArrayList<RegisteredDiagnosisModel>();
        for (List<String> values : valuesList) {
            RegisteredDiagnosisModel ord = getRegisteredDiagnosisModel(values);
            collection.add(ord);
        }

        return collection;
    }
    
    // ORCA カテゴリ
    private void storeSuspectedDiagnosis(RegisteredDiagnosisModel rdm, String test) {
        if (test!=null) {
            if (test.equals("1")) {
                rdm.setCategory("suspectedDiagnosis");
                rdm.setCategoryDesc("疑い病名");
                rdm.setCategoryCodeSys("MML0015");

            } else if (test.equals("2")) {
//                rdm.setCategory("suspectedDiagnosis");
//                rdm.setCategoryDesc("急性");
//                rdm.setCategoryCodeSys("MML0012");

            } else if (test.equals("3")) {
                rdm.setCategory("suspectedDiagnosis");
                rdm.setCategoryDesc("疑い病名");
                rdm.setCategoryCodeSys("MML0015");
            }
        }
    }
    
    private void storeMainDiagnosis(RegisteredDiagnosisModel rdm, String test) {
        if (test!=null && test.equals("1")) {
            rdm.setCategory("mainDiagnosis");
            rdm.setCategoryDesc("主病名");
            rdm.setCategoryCodeSys("MML0012");
        }
    }

    // ORCA 転帰
    private void storeOutcome(RegisteredDiagnosisModel rdm, String data) {
        if (data != null) {
            if (data.equals("1")) {
                rdm.setOutcome("fullyRecovered");
                rdm.setOutcomeDesc("全治");
                rdm.setOutcomeCodeSys("MML0016");

            } else if (data.equals("2")) {
                rdm.setOutcome("died");
                rdm.setOutcomeDesc("死亡");
                rdm.setOutcomeCodeSys("MML0016");

            } else if (data.equals("3")) {
                rdm.setOutcome("pause");
                rdm.setOutcomeDesc("中止");
                rdm.setOutcomeCodeSys("MML0016");

            } else if (data.equals("8")) {
                rdm.setOutcome("transfer");
                rdm.setOutcomeDesc("転医");
                rdm.setOutcomeCodeSys("MML0016");
            }
        }
    }

    private String toDolphinDateStr(String orcaDate) {
        if (orcaDate==null || orcaDate.equals("")) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.applyPattern("yyyyMMdd");
            Date orca = sdf.parse(orcaDate);
            sdf.applyPattern("yyyy-MM-dd");
            String ret = sdf.format(orca);
            return ret;
        } catch (ParseException ex) {
            //ex.printStackTrace(System.err);
        }

        return null;
    }
    
    // ResultSetからRegisteredDiagnosisModelを
    private RegisteredDiagnosisModel getRegisteredDiagnosisModel(List<String> values) {
        
        RegisteredDiagnosisModel rd = new RegisteredDiagnosisModel();
        
        // 病名コード、修飾語も含めて
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < 21; ++i) {
            String code = values.get(i).trim();
            if ("".equals(code)) {
                break;
            }
            if (!first) {
                sb.append(".");
            } else {
                first = false;
            }
            code = code.replace("ZZZ", "");     // 修飾語のコードからZZZを削除
            sb.append(code);
        }
        rd.setDiagnosisCode(sb.toString());

        // 疾患開始日
        rd.setStartDate(toDolphinDateStr(values.get(21)));
        // 疑いフラグ
        storeSuspectedDiagnosis(rd, values.get(22));
        // 主病名フラグ
        storeMainDiagnosis(rd, values.get(23));
        // 転帰
        storeOutcome(rd, values.get(24));
        // 疾患終了日（転帰）
        rd.setEndDate(toDolphinDateStr(values.get(25)));
        // 疾患名
        rd.setDiagnosis(values.get(26));
        // 制御のための Status
        rd.setStatus("ORCA");

        return rd;
    }
}
