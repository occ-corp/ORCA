
package open.dolphin.client;
/*
import java.text.SimpleDateFormat;
import java.util.*;
import open.dolphin.dao.SqlETensuDao;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.delegater.DocumentDelegater;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.MMLDate;
*/
/**
 * CheckSantei2 骨折り損
 * 
 * @author masuda, Masuda Naika
 */
public class CheckSantei2 extends CheckSanteiConst {
/*
    protected Chart context;
    private KartePane kp;
    private long karteId;
    private Date karteDate;
    private Date karteDateTrimTime;
    
    protected StampHolder sourceStampHolder;   // MakeBaseChargeStampの編集元
    private List<ModuleModel> stamps;
    private List<ClaimItem> allClaimItems;
    private List<SanteiHistoryModel> currentSanteiList;
    private List<SanteiHistoryModel> pastSanteiListDay;
    private List<SanteiHistoryModel> pastSanteiListMonth;
    private List<SanteiHistoryModel> pastSanteiListWeek;
    
    protected List<RegisteredDiagnosisModel> diagnosis;
    
    protected boolean gairaiKanriAvailable;
    protected boolean tokuShidouAvailable;
    protected boolean tokuShohouAvailable;
    protected boolean choukiAvailable;
    protected boolean yakujouAvailable;
    protected boolean zaitakuKanriAvailable;
    protected boolean isDoujitsu;
    protected boolean isShoshin;
    private boolean hasTumorMarkers;
    
    protected boolean homeCare;
    protected boolean nursingHomeCare;
    protected boolean zaitakuSougouKanri;
    protected boolean exMed;
    
    protected int jikangaiTaiou;
    protected Shienshin zaitakuShien;

    // 薬剤情報提供料の判定を、処方が前回と違えば毎回可能か、同じ処方が月内にあれば不可とするか？
    private boolean followMedicom;
    
    private static final SimpleDateFormat dateFrmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
    
    private SqlETensuDao eTenDao;
    private MasudaDelegater del;
    
    
    public void init(KartePane kartePane, Date date) {
        
        eTenDao = SqlETensuDao.getInstance();
        del = MasudaDelegater.getInstance();
        
        kp = kartePane;
        context = kartePane.getParent().getContext();
        karteId = context.getKarte().getId();

        // 未確定なら現在の日付
        karteDate = (date == null) ? new Date() : date;
        karteDateTrimTime = ModelUtils.getMidnightGc(karteDate).getTime();
        
        // カルテ上のスタンプを収集
        setupCurrentSanteiHistory();
        
        // 過去の算定履歴を取得
        setupPastSanteiHistory();
        
        // 登録されている病名を収集
        setupDiagnosis();
        
        // 変数群の設定
        setupVariables();
    }

    public void check() {
        StringBuilder sb = new StringBuilder();
        String ret = checkInclusion();
        if (ret != null) {
            sb.append(ret);
        }
        ret = checkExclusion();
        if (ret != null) {
            sb.append(ret);
        }
        ret = checkCountLimit();
        if (ret != null) {
            sb.append(ret);
        }
        System.out.println(sb.toString());
    }
    
    private String checkInclusion() {
        
        if (currentSanteiList.isEmpty()) {
            return null;
        }
        
        // 現在のカルテに関連する包括グループを列挙する
        Set<String> h_groups = new HashSet<String>();
        Set<String> srycds = new HashSet<String>();
        for (SanteiHistoryModel shm: currentSanteiList) {
            ETensuModel1 etm = shm.getEtensuModel1();
            String srycd = shm.getSrycd();
            if (etm.getH_tani1() != 0) {
                h_groups.add(etm.getH_group1());
                srycds.add(srycd);
            }
            if (etm.getH_tani2() != 0) {
                h_groups.add(etm.getH_group2());
                srycds.add(srycd);
            }
            if (etm.getH_tani3() != 0) {
                h_groups.add(etm.getH_group3());
                srycds.add(srycd);
            }
        }
        // 関係なければすぐ帰る
        if (h_groups.isEmpty()) {
            return null;
        }
        
        // データベースに問い合わせる
        List<ETensuModel2> eTenList = eTenDao.getETensuModel2(karteDate, h_groups, srycds);
        
        // 改めて現在のカルテに関する包括項目を検索する
        StringBuilder sb = new StringBuilder();
        
        for (SanteiHistoryModel shmTest : currentSanteiList) {
            
            ETensuModel1 etm = shmTest.getEtensuModel1();
            if (etm.getH_tani1() == 0 && etm.getH_tani2() == 0 && etm.getH_tani3() == 0) {
                continue;
            }
            
            int[] h_taniArray = {etm.getH_tani1(), etm.getH_tani2(), etm.getH_tani3()};
            
            for (int h_tani : h_taniArray) {

                // 包括単位に応じて調べる算定履歴リストを選択する
                List<SanteiHistoryModel> pastList = null;
                String inclusiveUnit = null;
                switch (h_tani) {
                    case 1:
                        pastList = pastSanteiListDay;
                        inclusiveUnit = "１日につき";
                        break;
                    case 2:
                        pastList = pastSanteiListMonth;
                        inclusiveUnit = "同一月内";
                        break;
                    case 3:
                        pastList = currentSanteiList;
                        inclusiveUnit = "同時";
                        break;
                    case 5:
                        pastList = pastSanteiListWeek;
                        inclusiveUnit = "手術前１週間";
                        break;
                    case 6:
                        // not implemented
                        inclusiveUnit = "１手術につき";
                        break;
                    default:
                        continue;
                }
                
                // 包括コメントを作成する
                ModuleModel mmTest = shmTest.getModuleModel();
                String nameTest = getClaimItemName(mmTest, shmTest.getItemIndex());
                
                // 対象算定履歴から包括に含まれるものをスキャンする
                for (SanteiHistoryModel shmPast : pastList) {
                    for (ETensuModel2 etm2 : eTenList) {
                        if (shmPast.getSrycd().equals(etm2.getSrycd())) {
                            ModuleModel pastMm = shmPast.getModuleModel();
                            String pastName = getClaimItemName(pastMm, shmPast.getItemIndex());
                            Date started = pastMm.getStarted();
                            String pastDate = started != null ? dateFrmt.format(pastMm.getStarted()) : null;
                            sb.append(nameTest).append("は");
                            if (pastDate != null) {
                                sb.append(pastDate).append("の");
                            }
                            sb.append(pastName).append("と");
                            sb.append(inclusiveUnit).append("包括です\n");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
    
    private String checkExclusion() {
        
        if (currentSanteiList.isEmpty()) {
            return null;
        }
        
        // 現在のカルテに関連する背反項目のsrycdをHashSetのリストに列挙する
        final int exTypeCount = 4;  // 背反テーブルの数
        List<Set<String>> srycdSetList = new ArrayList<Set<String>>(exTypeCount);
        for (int i = 0; i < exTypeCount; ++i) {
            srycdSetList.add(new HashSet<String>());
        }
        Set<String> srycds = new HashSet<String>();
        for (SanteiHistoryModel shm: currentSanteiList) {
            ETensuModel1 etm = shm.getEtensuModel1();
            String srycd = shm.getSrycd();
            srycds.add(srycd);
            if (etm.getR_day() != 0) {
                srycdSetList.get(0).add(srycd);
            }
            if (etm.getR_month() != 0) {
                srycdSetList.get(1).add(srycd);
            }
            if (etm.getR_same() != 0) {
                srycdSetList.get(2).add(srycd);
            }
            if (etm.getR_week() != 0) {
                srycdSetList.get(3).add(srycd);
            }
        }
        
        // 背反テーブルごとにETensuModel3を取得する
        List<List<ETensuModel3>> etm3ListList = new ArrayList<List<ETensuModel3>>(exTypeCount);
        for (int i = 0; i < exTypeCount; ++i) {
            etm3ListList.add(eTenDao.getETensuModel3(i, karteDate, srycds, srycdSetList.get(i)));
        }
        
        // 改めて現在のカルテに関する背反項目を検索する
        StringBuilder sb = new StringBuilder();
        for (SanteiHistoryModel shmTest : currentSanteiList) {
            ETensuModel1 etm = shmTest.getEtensuModel1();
            List<Integer> tableList = new ArrayList<Integer>();
            if (etm.getR_day() != 0) {
                tableList.add(0);
            }
            if (etm.getR_month() != 0) {
                tableList.add(1);
            }
            if (etm.getR_same() != 0) {
                tableList.add(2);
            }
            if (etm.getR_week() != 0) {
                tableList.add(3);
            }
            if (tableList.isEmpty()) {
                continue;
            }
            
            for (int tableNo : tableList) {
                List<ETensuModel3> etm3List = etm3ListList.get(tableNo);
                String exTypeName = null;
                List<SanteiHistoryModel> pastList = null;
                switch (tableNo) {
                    case 0:
                        exTypeName = "１日につき";
                        pastList = pastSanteiListDay;
                        break;
                    case 1:
                        exTypeName = "同一月内";
                        pastList = pastSanteiListMonth;
                        break;
                    case 2:
                        exTypeName = "同時";
                        pastList = currentSanteiList;
                        break;
                    case 3:
                        exTypeName = "１週間につき";
                        pastList = pastSanteiListWeek;
                        break;
                    default:
                        continue;
                }
                // 背反コメントを作成する
                ModuleModel mmTest = shmTest.getModuleModel();
                String nameTest = getClaimItemName(mmTest, shmTest.getItemIndex());
                
                for (SanteiHistoryModel shmPast : pastList) {
                    ModuleModel pastMm = shmPast.getModuleModel();
                    String pastName = getClaimItemName(pastMm, shmPast.getItemIndex());
                    Date started = pastMm.getStarted();
                    String pastDate = started != null ? dateFrmt.format(pastMm.getStarted()) : null;
                    for (ETensuModel3 etm3 : etm3List) {
                        if (shmPast.getSrycd().equals(etm3.getSrycd2())) {
                            sb.append(nameTest).append("は");
                            if (pastDate != null) {
                                sb.append(pastDate).append("の");
                            }
                            sb.append(pastName).append("と");
                            sb.append(exTypeName).append("背反です\n");
                        }
                    }
                }
            }
        }
        
        return sb.toString();
    }
    
    private String checkCountLimit() {
        
        if (currentSanteiList.isEmpty()) {
            return null;
        }
        
        Set<String> srycds = new HashSet<String>();
        for (SanteiHistoryModel shm : currentSanteiList) {
            srycds.add(shm.getSrycd());
        }

        // データベースに問い合わせる
        List<ETensuModel5> eTenList = eTenDao.getETensuModel5(karteDate, srycds);
        
        // 改めて現在のカルテに関する背反項目を検索する
        StringBuilder sb = new StringBuilder();
        for (SanteiHistoryModel shmTest : currentSanteiList) {
            ETensuModel1 etm = shmTest.getEtensuModel1();
            if (etm.getC_kaisu() == 0) {
                continue;
            }
            for (ETensuModel5 etm5 : eTenList) {
                String srycdEtm5 = etm5.getSrycd();
                if (!srycdEtm5.equals(shmTest.getSrycd())) {
                    continue;
                }
                int unitCode = etm5.getTanicd();
                // 算定単位に応じて調べる算定履歴リストを選択する
                List<SanteiHistoryModel> shmList = null;
                String unitName = null;

                switch (unitCode) {
                    case ETensuModel5.UNIT_DAY:
                        shmList = pastSanteiListDay;
                        unitName = "日";
                        break;
                    case ETensuModel5.UNIT_WEEK:
                        shmList = pastSanteiListWeek;
                        unitName = "週";
                        break;
                    case ETensuModel5.UNIT_MONTH:
                        shmList = pastSanteiListMonth;
                        unitName = "月";
                        break;
                    // 重くなるのでここからは省略したい
                    case ETensuModel5.UNIT_2M:
                        Date fromDate = getFromDateMonthAgo(karteDate, 2);
                        shmList = del.getSanteiHistory(karteId, fromDate, karteDate, Collections.singletonList(srycdEtm5));
                        unitName = "２月";
                        break;
                    case ETensuModel5.UNIT_3M:
                        fromDate = getFromDateMonthAgo(karteDate, 3);
                        shmList = del.getSanteiHistory(karteId, fromDate, karteDate, Collections.singletonList(srycdEtm5));
                        unitName = "３月";
                        break;
                    case ETensuModel5.UNIT_4M:
                        fromDate = getFromDateMonthAgo(karteDate, 4);
                        shmList = del.getSanteiHistory(karteId, fromDate, karteDate, Collections.singletonList(srycdEtm5));
                        unitName = "４月";
                        break;
                    case ETensuModel5.UNIT_6M:
                        fromDate = getFromDateMonthAgo(karteDate, 6);
                        shmList = del.getSanteiHistory(karteId, fromDate, karteDate, Collections.singletonList(srycdEtm5));
                        unitName = "６月";
                        break;
                    case ETensuModel5.UNIT_12M:
                        fromDate = getFromDateMonthAgo(karteDate, 12);
                        shmList = del.getSanteiHistory(karteId, fromDate, karteDate, Collections.singletonList(srycdEtm5));
                        unitName = "１２月";
                        break;
                    case ETensuModel5.UNIT_5Y:
                        fromDate = getFromDateMonthAgo(karteDate, 60);
                        shmList = del.getSanteiHistory(karteId, fromDate, karteDate, Collections.singletonList(srycdEtm5));
                        unitName = "５年";
                        break;
                    case ETensuModel5.UNIT_PATIENT:
                        fromDate = new Date(0);
                        shmList = del.getSanteiHistory(karteId, fromDate, karteDate, Collections.singletonList(srycdEtm5));
                        unitName = "患者";
                        break;
                    default:
                        continue;
                }
                
                // 回数制限コメントを作成する
                int totalCount = 0;
                int countLimit = etm5.getKaisu();
                // 過去の算定履歴リストに現在のカルテにある分を足してカウントする
                shmList.addAll(currentSanteiList);
                for (SanteiHistoryModel shmPast : shmList) {
                    if (shmPast.getSrycd().equals(srycdEtm5)) {
                        totalCount += shmPast.getItemCount();
                    }
                }
                if (countLimit < totalCount) {
                    ModuleModel mmTest = shmTest.getModuleModel();
                    String nameTest = getClaimItemName(mmTest, shmTest.getItemIndex());
                    sb.append(nameTest).append("は");
                    sb.append(unitName).append("あたり");
                    sb.append(countLimit).append("回制限です\n");
                }
            }
        }

        return sb.toString();
    }
    
    private void setupPastSanteiHistory() {
        
        Date fromToday = getFromDateToday(karteDate);
        Date fromWeekAgo = getFromDateWeekAgo(karteDate);
        Date fromThisMonth = getFromDateThisMonth(karteDate);
        
        // 今月の算定履歴を取得する
        pastSanteiListMonth = del.getSanteiHistory(karteId, fromThisMonth, karteDate, null);
        // １週間、今日に分類して登録する
        pastSanteiListDay = new ArrayList<SanteiHistoryModel>();
        pastSanteiListWeek = new ArrayList<SanteiHistoryModel>();
        
        for (SanteiHistoryModel shm : pastSanteiListMonth) {
            ModuleModel mm = shm.getModuleModel();
            mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
            Date date = shm.getModuleModel().getStarted();
            if (!fromToday.after(date)) {
                pastSanteiListDay.add(shm);
            }
            if (!fromWeekAgo.after(date)) {
                pastSanteiListWeek.add(shm);
            }
        }
    }
    
    private void setupCurrentSanteiHistory() {

        stamps = new ArrayList<ModuleModel>();
        allClaimItems = new ArrayList<ClaimItem>();

        // KartePaneからStampHolderを取得する
        KarteStyledDocument doc = (KarteStyledDocument) kp.getTextPane().getDocument();
        List<StampHolder> list = doc.getStampHolders();
        
        // 現在のカルテにある全てのsrycdをとりあえず列挙する
        Set<String> srycds = new HashSet<String>();
        for (StampHolder sh : list) {
            if (sh == sourceStampHolder) {
                continue;
            }
            ModuleModel mm = sh.getStamp();
            stamps.add(mm);
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            for (ClaimItem ci : cb.getClaimItem()) {
                srycds.add(ci.getCode());
                allClaimItems.add(ci);
            }
        }
        
        // 現在のカルテの電子点数表に関連する項目を取得する
        List<ETensuModel1> eTenList = eTenDao.getETensuModel1(karteDate, srycds);
        // 一旦HashMapに登録
        Map<String, ETensuModel1> map = new HashMap<String, ETensuModel1>();
        for (ETensuModel1 etm : eTenList) {
            map.put(etm.getSrycd(), etm);
        }
        
        // 全てのModuleModelをスキャンして、現在のカルテの算定履歴リストを作成する。
        currentSanteiList = new ArrayList<SanteiHistoryModel>();
        for (ModuleModel mm : stamps) {
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            int bundleNumber = parseInt(cb.getBundleNumber());
            for (int i = 0; i < cb.getClaimItem().length; ++i) {
                ClaimItem ci = cb.getClaimItem()[i];
                ETensuModel1 etm = map.get(ci.getCode());
                if (etm != null) {
                    int claimNumber = parseInt(ci.getNumber());
                    int count = bundleNumber * claimNumber;
                    SanteiHistoryModel shm = new SanteiHistoryModel();
                    shm.setSrycd(ci.getCode());
                    shm.setModuleModel(mm);
                    shm.setItemIndex(i);
                    shm.setItemCount(count);
                    shm.setETensuModel1(etm);
                    currentSanteiList.add(shm);
                }
            }
        }
    }

    // 有効期限と中止項目をチェックする
    private String checkYukoEndDisconItem() {
        
        int todayInt = MMLDate.getTodayInt();

        StringBuilder sb = new StringBuilder();

        // tbl_tensuを参照して有効期限が設定されていないか調べる
        int len = allClaimItems.size();

        if (len > 0) {
            List<String> srycdList = new ArrayList<String>();
            for (ClaimItem ci : allClaimItems) {
                srycdList.add(ci.getCode());
            }
            SqlMiscDao dao = SqlMiscDao.getInstance();
            List<TensuMaster> list = dao.getTensuMasterList(srycdList);

            // 有効期限が設定されているものをしらべる
            for (TensuMaster tm : list) {
                String yukoedymd = tm.getYukoedymd();
                if (todayInt > Integer.valueOf(yukoedymd)) {
                    sb.append(tm.getName());
                    sb.append("は");
                    sb.append(yukoedymd);
                    sb.append("で終了です。\n");
                }
            }
            // 中止項目かどうか調べる
            // 中止項目リストを最新にする
            DisconItems.getInstance().loadDisconItems();
            for (ClaimItem ci : allClaimItems) {
                String name = ci.getName();
                if (DisconItems.getInstance().isDiscon(name)) {
                    sb.append(name);
                    sb.append("は中止項目です。\n");
                }
            }
        }

        return sb.toString();
    }
    
    
    private void setupVariables() {
        
        SqlMiscDao dao = SqlMiscDao.getInstance();
        
        followMedicom = Project.getBoolean(MiscSettingPanel.FOLLOW_MEDICOM, true);  // ver 2.2m
        exMed = Project.getBoolean(MiscSettingPanel.RP_OUT, false);  // ver 2.2m
        //followMedicom = MiscSettingPanel.getStub().getFollowMedicom();  // ver 1.4m
        //exMed = MiscSettingPanel.getStub().getDefaultExMed();  // ver 1.4m
        
        // 時間外対応加算フラグ
        if (dao.getSyskanriFlag(sk1006_jikangaiTaiou1)) {
            jikangaiTaiou = 1;
        } else if (dao.getSyskanriFlag(sk1006_jikangaiTaiou2)
                || dao.getSyskanriFlag(sk1006_chiikiKoken)) {
            jikangaiTaiou = 2;
        } else if (dao.getSyskanriFlag(sk1006_jikangaiTaiou3)) {
            jikangaiTaiou = 3;
        }
        
        // 在宅療養支援診療所フラグ
        if (dao.getSyskanriFlag(sk1006_zaitakuShien1)
                || dao.getSyskanriFlag(sk1006_zaitakuShienHsp1)) {
            zaitakuShien = Shienshin.KYOKA_TANDOKU; // 機能強化・単独
        } else if (dao.getSyskanriFlag(sk1006_zaitakuShien2)
                || dao.getSyskanriFlag(sk1006_zaitakuShienHsp2)) {
            zaitakuShien = Shienshin.KYOKA_RENKEI;  // 機能強化・連携
        } else if (dao.getSyskanriFlag(sk1006_zaitakuShien3old)
                || dao.getSyskanriFlag(sk1006_zaitakuShien3)) {
            zaitakuShien = Shienshin.FUTSU;         // 普通の支援診
        } else {
            zaitakuShien = Shienshin.NON_SHIENSHIN;           // 非支援診
        }
        
        // 在宅時医学総合管理料
        SanteiInfoModel info = context.getPatient().getSanteiInfoModel();
        homeCare = info.isHomeMedicalCare();
        nursingHomeCare = info.isNursingHomeMedicalCare();
        zaitakuSougouKanri = dao.getSyskanriFlag(sk1006_zaiiSoukan);
        zaitakuSougouKanri = zaitakuSougouKanri && info.isZaitakuSougouKanri();
        
        // 外来管理加算と腫瘍マーカー設定
        setupGairaiKanriAndTumorMarkers();
        
        // 特定疾患と初診設定
        checkDiagnosis();
    }
    
    // 登録されている病名から特定疾患の有無と初診かどうかを調べる
    private void checkDiagnosis() {

        tokuShidouAvailable = false;
        tokuShohouAvailable = false;
        isShoshin = true;

        for (RegisteredDiagnosisModel rdm : diagnosis) {

            // 特定疾患療養管理加算・初診について調べる
            Date started = ModelUtils.getStartDate(rdm.getStarted()).getTime();
            Date ended = ModelUtils.getEndedDate(rdm.getEnded()).getTime();
            if (ModelUtils.isDateBetween(started, ended, karteDateTrimTime)) {
                if (rdm.getByoKanrenKbn() == ClaimConst.BYOKANRENKBN_TOKUTEI) {
                    // カルテの記録日の時点で終了してなかったら特定疾患処方管理加算は算定可能
                    tokuShohouAvailable = true;
                    // 以下、特定疾患療養管理料の判定
                    GregorianCalendar gc = new GregorianCalendar();
                    gc.setTime(started);
                    gc.add(GregorianCalendar.MONTH, 1);
                    if (!karteDateTrimTime.before(gc.getTime())) {
                        tokuShidouAvailable = true;
                    }
                }
                // 初診は開始日が今日だけ
                if (started.getTime() != karteDateTrimTime.getTime()) {
                    isShoshin = false;
                }
            }
            // 調べ終わったら途中でbreakする
            if (tokuShidouAvailable && tokuShohouAvailable && !isShoshin) {
                break;
            }
        }
    }
    
    // 外来管理加算算定可否と腫瘍マーカーの有無を設定する
    private void setupGairaiKanriAndTumorMarkers() {

        Set<String> srycdList = new HashSet<String>();
        for (SanteiHistoryModel shm : currentSanteiList) {
            srycdList.add(shm.getSrycd());
        }

        SqlMiscDao dao = SqlMiscDao.getInstance();
        // 外来管理加算
        gairaiKanriAvailable = !dao.hasGairaiKanriKbn(srycdList);
        // 腫瘍マーカー有無
        hasTumorMarkers = dao.hasTumorMarkers(srycdList);
    }

    
    // 病名を取得する
    private void setupDiagnosis() {

        DocumentDelegater ddl = DocumentDelegater.getInstance();
        diagnosis = ddl.getDiagnosisList(karteId, new Date(0), false);  // ver 2.2m
        //diagnosis = ddl.getDiagnosisList(karteId, new Date(0)); // ver 1.4m
        updateIkouTokutei2(diagnosis);
    }

    // 移行病名と病関連区分を設定する
    private void updateIkouTokutei2(List<RegisteredDiagnosisModel> list) {

        List<String> srycdList = new ArrayList<String>();
        for (RegisteredDiagnosisModel rd : list) {
            // 病名コードを切り出し（接頭語，接尾語は捨てる）
            String[] code = rd.getDiagnosisCode().split("\\.");
            for (String str : code) {
                if (str.length() == 7) {     // 病名コードの桁数は７
                    srycdList.add(str);
                }
            }
        }

        final SqlMiscDao dao = SqlMiscDao.getInstance();
        List<DiseaseEntry> disList = dao.getDiseaseEntries(srycdList);
        if (disList == null || !dao.isNoError()){
            return;
        }

        for (RegisteredDiagnosisModel rd : list) {
            String codeRD = rd.getDiagnosisCode();
            for (DiseaseEntry de : disList) {
                String codeDE = de.getCode();
                if (codeDE.equals(codeRD)) {
                    // 移行病名セット
                    boolean b = "99999999".equals(de.getDisUseDate());
                    rd.setIkouByomei(b);
                    // byokanrankbnも、うｐだて
                    rd.setByoKanrenKbn(de.getByoKanrenKbn());
                    break;
                }
            }
        }
    }
    
    private int parseInt(String str) {

        int num = 1;
        try {
            num = Integer.valueOf(str);
        } catch (Exception e) {
        }
        return num;
    }
    
    // ModuleModelの指定番目のClaimItemの名前を返す
    private String getClaimItemName(ModuleModel mm, int index) {
        ClaimBundle cb = (ClaimBundle) mm.getModel();
        String name = cb.getClaimItem()[index].getName();
        return name;
    }
    
    // 今月の１日を返す
    private Date getFromDateThisMonth(Date date) {
        
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        int year = gc.get(GregorianCalendar.YEAR);
        int month = gc.get(GregorianCalendar.MONTH);
        gc.clear();
        gc.set(year, month, 1);     // 今月の初めから
        Date fromDate = gc.getTime();

        return fromDate;
    }
    
    // 指定ヶ月前の１日を返す
    private Date getFromDateMonthAgo(Date date, int monthAgo) {
        
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        int year = gc.get(GregorianCalendar.YEAR);
        int month = gc.get(GregorianCalendar.MONTH);
        gc.clear();
        gc.set(year, month, 1);
        gc.add(GregorianCalendar.MONTH, -monthAgo + 1);
        Date fromDate = gc.getTime();

        return fromDate;
    }
    
    // 今日の０時を返す
    private Date getFromDateToday(Date date) {
        
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        int year = gc.get(GregorianCalendar.YEAR);
        int month = gc.get(GregorianCalendar.MONTH);
        int day = gc.get(GregorianCalendar.DATE);
        gc.clear();
        gc.set(year, month, day);  // 今日の０時
        Date fromDate = gc.getTime();
        
        return fromDate;
    }
    
    // １週間前を返す
    private Date getFromDateWeekAgo(Date date) {
        
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        int year = gc.get(GregorianCalendar.YEAR);
        int month = gc.get(GregorianCalendar.MONTH);
        int day = gc.get(GregorianCalendar.DATE);
        gc.clear();
        gc.set(year, month, day);
        gc.add(GregorianCalendar.DATE, -7); // １週間前
        GregorianCalendar gc1 = new GregorianCalendar();
        gc.clear();
        gc1.set(year, month, 1);
        // １週間前が先月なら今月頭
        Date fromDate = (gc.before(gc1)) ? gc1.getTime() : gc.getTime();
        
        return fromDate;
    }
*/
}
