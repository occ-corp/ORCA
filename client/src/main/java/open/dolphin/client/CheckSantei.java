
package open.dolphin.client;

import java.awt.Toolkit;
import java.util.*;
import javax.swing.JOptionPane;
import open.dolphin.dao.SqlETensuDao;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.delegater.DocumentDelegater;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.MMLDate;

/**
 * 簡易算定チェックなど
 * 現状では、下記の項目について、加算できるかどうかの判定をしたつもり
 * ああ、もう、グチャグチャ状態！！
 *
 * @author masuda, Masuda Naika
 */
public class CheckSantei extends CheckSanteiConst {
    
    protected Chart context;
    private KartePane kp;
    private long karteId;
    private Date karteDate;
    private Date karteDateTrimTime;

    private List<ModuleModel> stamps;
    private List<ClaimItem> allClaimItems;
    private List<SanteiHistoryModel> currentSanteiList;
    private List<SanteiHistoryModel> pastSanteiListMonth;
    protected List<RegisteredDiagnosisModel> diagnosis;

    protected StampHolder sourceStampHolder;   // MakeBaseChargeStampの編集元
    
    protected boolean gairaiKanriAvailable;
    protected boolean tokuShidouAvailable;
    protected boolean tokuShohouAvailable;
    protected boolean choukiAvailable;
    protected boolean yakujouAvailable;
    protected boolean zaitakuKanriAvailable;
    protected boolean isDoujitsu;
    protected boolean isShoshin;
    protected int pastTokuRyouyouCount;
    protected int pastTokuShohouCount;
    protected int pastChoukiShohouCount;
    protected int pastZaitakuKanriCount;
    protected int pastZaitakuHoumonCount;

    private boolean hasInMed;
    private boolean hasExMed;
    private boolean denwa;
    private int newTokuRyouyouCount;
    private int newTokuShohouCount;
    private int newChoukiShohouCount;
    private int newGairaikanriCount;
    private int newYakujoCount;
    private int newNoTokuRyouCount;
    private int pastNoTokuRyouCount;
    private int newZaitakuKanriCount;
    private int newZaitakuHoumonCount;
    
    private boolean newZaitakuInMed;
    private boolean pastZaitakuInMed;
    private boolean allHokatsuMed;
    private boolean hasTumorMarkers;

    protected boolean homeCare;
    protected boolean nursingHomeCare;
    protected boolean zaitakuSougouKanri;
    protected boolean exMed;
    
    protected JikangaiTaiou jikangaiTaiou;
    protected Shienshin zaitakuShien;
    protected boolean hasBed;

    // 薬剤情報提供料の判定を、処方が前回と違えば毎回可能か、同じ処方が月内にあれば不可とするか？
    private boolean followMedicom;
    
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
        
        // 変数群の設定
        setupVariables();
    }
    
    protected String check(boolean onSave) {

        // カルテセーブ時と基本料スタンプ作成時で処理をまとめたが、
        // 判断を微妙に変えなければならなくて、余計ややこしくなったｗ
        
        checkKarte(true);   // 過去カルテ
        checkKarte(false);  // 現在のカルテ

        int maxTokuRyouyou = defMaxTokuRyouyou;
        int maxTokuShohou = defMaxTokuShohou;
        int maxChouki = defMaxChouki;
        int maxNoTokuRyouCount = defMaxNoTokuRyouCount;

        if (!onSave) {
            maxTokuRyouyou--;
            maxTokuShohou--;
            maxChouki--;
            maxNoTokuRyouCount--;
        }

        StringBuilder sb = new StringBuilder();

        //------------
        // 外来管理加算
        //------------
        // 外来加算、できません
        if (!gairaiKanriAvailable && newGairaikanriCount != 0) {
            sb.append("外来管理加算は算定できません。\n");
        }
        if (newZaitakuHoumonCount != 0 && newGairaikanriCount != 0) {
            sb.append("外来管理加算は算定できません。\n");
            gairaiKanriAvailable = false;
        }

        //--------------------
        // 特定疾患療養管理加算
        //--------------------
        // 特定疾患療養管理、できます
        if (tokuShidouAvailable
                && newTokuRyouyouCount == 0
                && newNoTokuRyouCount + pastNoTokuRyouCount == 0
                && pastTokuRyouyouCount + newTokuRyouyouCount < maxTokuRyouyou
                && !denwa
                && !zaitakuSougouKanri) {
            sb.append("特定疾患療養管理料を算定できます。\n");
        }

        // 特定疾患療養管理、できません
        // 特定疾患病名がないので、できません
        if (!tokuShidouAvailable &&  newTokuRyouyouCount != 0) {
            sb.append("特定疾患療養管理料は算定できません。病名チェック要。\n");
        }
        // 電話再診では、できません
        if (denwa) {
            if (newTokuRyouyouCount != 0) {
                sb.append("電話再診では特定疾患療養管理料を算定できません。\n");
            }
            tokuShidouAvailable = false;
        }
        // 回数オーバーなので、できません
        if (newTokuRyouyouCount + pastTokuRyouyouCount > maxTokuRyouyou) {
            sb.append("特定疾患療養管理料は２回算定済みです。\n");
            tokuShidouAvailable = false;
        }
        // 他指導料と併算定、できません
        if (newNoTokuRyouCount + pastNoTokuRyouCount > 0) {
            if (newTokuRyouyouCount != 0) {
                sb.append("特定疾患療養管理料と併算定できない指導料があります。\n");
            }
            tokuShidouAvailable = false;
        }

        //------------
        // その他指導料
        //------------
        // その他指導料、できません
        if (newNoTokuRyouCount + pastNoTokuRyouCount > maxNoTokuRyouCount) {
            sb.append("指導料の算定回数、併算定チェック要。\n");
            tokuShidouAvailable = false;
        }

        //--------------------
        // 特定疾患処方管理加算
        //--------------------
        // 特定疾患処方管理加算、できます
        if (tokuShohouAvailable
                && !choukiAvailable
                && newTokuShohouCount == 0
                && newTokuShohouCount + pastTokuShohouCount < maxTokuShohou
                && pastChoukiShohouCount == 0
                && hasShohou()
                && !zaitakuSougouKanri) {
            sb.append("特定疾患処方管理加算を算定できます。\n");
        }

        // 特定疾患処方管理加算、できまへん
        // 特定疾患病名がないので、できません
        if (!tokuShohouAvailable && newTokuShohouCount != 0) {
            sb.append("特定疾患処方管理加算は算定できません。病名チェック要。\n");
        }
        // 処方がないので、できません
        if (!hasShohou()) {
            if (newTokuShohouCount != 0) {
                sb.append("処方がないので特疾処方管理加算は算定できません。\n");
            }
            tokuShohouAvailable = false;
        }
        // 長期処方があるので、できません
        if (pastChoukiShohouCount != 0) {
            if (newTokuShohouCount != 0) {
                sb.append("長期処方の算定歴あり、特疾処方管理加算は算定できません。\n");
            }
            tokuShohouAvailable = false;
        }
        // 回数オーバーなので、できません
        if (pastTokuShohouCount + newTokuShohouCount > maxTokuShohou) {
            sb.append("特定疾患処方管理加算は２回算定済みです。\n");
            tokuShohouAvailable = false;
        }

        //------------
        // 長期処方加算
        //------------
        // 長期処方、できます
        if (choukiAvailable
                && tokuShohouAvailable
                && newChoukiShohouCount == 0
                && newChoukiShohouCount + pastChoukiShohouCount < defMaxChouki
                && hasShohou()
                && !zaitakuSougouKanri) {
            if (pastTokuShohouCount == 0) {
                sb.append("長期処方を算定できます。\n");
            } else {
                sb.append("長期処方を算定できますが、\n");
                sb.append("特疾処方加算の算定歴あり、長期処方を行う場合は要訂正。\n");
            }
        }

        // 長期処方、できまへん
        // 長期処方は２８日以上でないと、できません
        if (!choukiAvailable && newChoukiShohouCount != 0) {
            sb.append("長期処方は算定できません。\n");
            choukiAvailable = false;
        }
        // 処方がないので、できません
        if (!hasShohou()) {
            if (newChoukiShohouCount != 0) {
                sb.append("処方がないので長期処方は算定できません。\n");
            }
            choukiAvailable = false;
        }
        // 特定疾患病名がないので、できません
        if (!tokuShohouAvailable) {
            if (newChoukiShohouCount != 0) {
                sb.append("長期処方は算定できません。病名チェック要。\n");
            }
            choukiAvailable = false;
        }
        // 特定疾患処方管理加算があるので、できません
        if (pastTokuShohouCount != 0) {
            if (newChoukiShohouCount != 0) {
                sb.append("特疾処方加算の算定歴あり、長期処方を行う場合は要訂正。\n");
            }
            choukiAvailable = false;
        }
        // 回数オーバーなので、できません
        if (newChoukiShohouCount + pastChoukiShohouCount > maxChouki) {
            sb.append("長期処方算定歴あり、長期処方は算定できません。\n");
            choukiAvailable = false;
        }

        //--------------
        // 薬剤情報提供料
        //--------------
        // 薬剤情報、できます
        if (yakujouAvailable
                && newYakujoCount == 0
                && hasInMed) {
            sb.append("薬剤変更がある場合は薬剤情報提供料を算定可能です。\n");
        }

        // 薬剤情報、できません
        if (newYakujoCount != 0) {
            if (!hasShohou()) {
                // 処方がないので、できません。
                sb.append("処方がないので薬剤情報提供料は算定できません。\n");
            } else if (!yakujouAvailable) {
                // 処方変更なしは、できません
                sb.append("処方変更がない場合は薬剤情報提供料を算定できません。\n");
            }
            if (!hasInMed && hasExMed) {
                // 院外なので、できません
                sb.append("院外処方なので薬剤情報提供料は算定できません。\n");
            }
        }
        
        // Ｃ管理？
        boolean cancerCare = context.getPatient().getSanteiInfoModel().isCancerCare();
        if (cancerCare && hasTumorMarkers) {
            sb.append("Ｃ管理の場合はTMを悪性腫瘍特異物質治療管理料で算定してください。\n");
        }

        //----------
        // 在宅管理料
        //----------
        zaitakuKanriAvailable = false;
        int houmonCount = 2;
        if (!onSave) {
            houmonCount--;
        }

        // 在宅管理料、できます
        if (newZaitakuKanriCount == 0
                && pastZaitakuKanriCount == 0
                && pastZaitakuHoumonCount + newZaitakuHoumonCount >= houmonCount) {
            sb.append("在宅医学管理料を算定できます。\n");
            zaitakuKanriAvailable = true;
        }
        // 在宅管理料、できません
        if (newZaitakuKanriCount != 0
                && pastZaitakuHoumonCount + newZaitakuHoumonCount < houmonCount) {
            sb.append("月２回目の訪問時まで在宅医学管理料は算定できません。\n");
        }
        if (newZaitakuKanriCount != 0 && pastZaitakuKanriCount != 0) {
            sb.append("在宅医学管理料は算定済みです。\n");
        }
        if (newTokuRyouyouCount + pastTokuRyouyouCount != 0 && newZaitakuKanriCount != 0) {
            sb.append("特定疾患療養管理料と併算定できません。\n");
            tokuShidouAvailable = false;
        }
        if (newTokuShohouCount + pastTokuShohouCount != 0 && newZaitakuKanriCount != 0) {
            sb.append("特定疾患処方管理加算と併算定できません。\n");
            tokuShohouAvailable = false;
        }
        if (newChoukiShohouCount + pastChoukiShohouCount != 0 && newZaitakuKanriCount != 0) {
            sb.append("長期処方と併算定できません。\n");
            choukiAvailable = false;
        }
        // 薬剤が包括でない場合
        //if ((newZaitakuInMed || pastZaitakuInMed) && !allHokatsuMed) {
        if (zaitakuSougouKanri && !allHokatsuMed && hasInMed && !hasExMed) {
            sb.append("処方は包括にしてください。\n");
        }

        return sb.toString();
    }

    // KarteEditorで保存するときに呼ばれる
    public boolean checkOnSave() {
        StringBuilder sb = new StringBuilder();
        
        // 基本料スタンプがあるかどうか
        boolean b = false;
        for (ModuleModel mm : stamps) {
            String orderName = mm.getModuleInfoBean().getStampName();
            if (orderName != null && orderName.contains("基本料")) {
                b = true;
                break;
            }
        }
        if (!b) {
            sb.append("基本料スタンプがありません。基本料を入力してください。\n");
        }

        // 併算定チェック
        sb.append(check(true));
        // 有効期限と中止項目チェック
        sb.append(checkYukoEndDisconItem());

        boolean ret = false;
        if (sb.length() > 0) {
            ret = true;
        }
        if (ret) {
            Toolkit.getDefaultToolkit().beep();
            String[] options = {"取消", "無視"};
            String title = ClientContext.getFrameTitle("チェック");
            int val = JOptionPane.showOptionDialog(context.getFrame(), sb.toString(), title,
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            if (val == 1) {
                // 無視なら
                ret = false;
            }
        }
        return ret;
    }
 
    private void checkKarte(boolean past) {

        List<SanteiHistoryModel> list = past
                ? pastSanteiListMonth
                : currentSanteiList;

        // カルテの特定疾患などの項目数をカウントする。
        int tmpTokuRyouyouCount = 0;
        int tmpTokuShohouCount = 0;
        int tmpChoukiShohouCount = 0;
        int tmpGairaikanriCount = 0;
        int tmpYakujoCount = 0;
        int tmpNoTokuRyouCount = 0;
        int tmpZaitakuKanriCount = 0;
        int tmpZaitakuHoumonCount = 0;
        boolean tmpIsDoujitsu = false;
        boolean tmpDenwa = false;
        boolean tmpZaitakuInMed = false;

        for (SanteiHistoryModel shm : list) {
            int srycd = Integer.valueOf(shm.getSrycd());
            int count = shm.getItemCount();
            switch (srycd) {
                case srycd_Tokutei_Ryouyou:
                    tmpTokuRyouyouCount += count;
                    break;
                case srycd_Tokutei_Shohou:
                case srycd_Tokutei_Shohou_Shohousen:
                    tmpTokuShohouCount += count;
                    break;
                case srycd_Chouki_Shohou:
                case srycd_Chouki_Shohousen:
                    tmpChoukiShohouCount += count;
                    break;
                case srycd_Gairaikanri_Kasan:
                    tmpGairaikanriCount += count;
                    break;
                case srycd_Yakuzaijouhou:
                    tmpYakujoCount += count;
                    break;
                case srycd_Saishin:
                case srycd_Saishin_Doujitsu:
                    // 同日再診判定
                    if (!tmpIsDoujitsu) {
                        long time1 = karteDateTrimTime.getTime();
                        long time2 = ModelUtils.getMidnightGc(shm.getSanteiDate()).getTimeInMillis();
                        tmpIsDoujitsu = (time1 == time2);
                    }
                    break;
                case srycd_Saishin_Denwa:
                case srycd_Saishin_Doujitsu_Denwa:
                    // 同日再診判定
                    if (!tmpIsDoujitsu) {
                        long time1 = karteDateTrimTime.getTime();
                        long time2 = ModelUtils.getMidnightGc(shm.getSanteiDate()).getTimeInMillis();
                        tmpIsDoujitsu = (time1 == time2);
                    }
                    tmpDenwa = true;
                    break;
                case srycd_ZaiiSoukanIn1:
                case srycd_ZaiiSoukanIn2:
                case srycd_ZaiiSoukanIn3:
                case srycd_ZaiiSoukanIn4:
                case srycd_TokuiSoukanIn1:
                case srycd_TokuiSoukanIn2:
                case srycd_TokuiSoukanIn3:
                case srycd_TokuiSoukanIn4:
                    tmpZaitakuKanriCount += count;
                    tmpZaitakuInMed = true;
                    break;
                case srycd_ZaiiSoukanEx1:
                case srycd_ZaiiSoukanEx2:
                case srycd_ZaiiSoukanEx3:
                case srycd_ZaiiSoukanEx4:
                case srycd_TokuiSoukanEx1:
                case srycd_TokuiSoukanEx2:
                case srycd_TokuiSoukanEx3:
                case srycd_TokuiSoukanEx4:
                    tmpZaitakuKanriCount += count;
                    tmpZaitakuInMed = false;
                    break;
                case srycd_HoumonShinsatsu_hidouitsu:
                case srycd_HoumonShinsatsu_douitsu_hitokutei:
                case srycd_HoumonShinsatsu_douitsu_tokutei:
                case srycd_Oushin:
                    tmpZaitakuHoumonCount += count;
                    break;
                default:
                    if (exclusiveTokuteiRyouyou(srycd)) {
                        tmpNoTokuRyouCount += count;
                    }
                    break;
            }
        }

        if (past) {
            pastTokuRyouyouCount = tmpTokuRyouyouCount;
            pastTokuShohouCount = tmpTokuShohouCount;
            pastChoukiShohouCount = tmpChoukiShohouCount;
            pastNoTokuRyouCount = tmpNoTokuRyouCount;
            pastZaitakuKanriCount = tmpZaitakuKanriCount;
            pastZaitakuHoumonCount = tmpZaitakuHoumonCount;
            isDoujitsu = tmpIsDoujitsu;
        } else {
            newTokuRyouyouCount = tmpTokuRyouyouCount;
            newTokuShohouCount = tmpTokuShohouCount;
            newChoukiShohouCount = tmpChoukiShohouCount;
            newGairaikanriCount = tmpGairaikanriCount;
            newYakujoCount = tmpYakujoCount;
            newNoTokuRyouCount = tmpNoTokuRyouCount;
            newZaitakuKanriCount = tmpZaitakuKanriCount;
            newZaitakuInMed = tmpZaitakuInMed;
            denwa = tmpDenwa;
        }
    }

    // 特定疾患療養管理加算と併算定できるかウルトラおおざっぱにチェックｗ
    private boolean exclusiveTokuteiRyouyou(int srycd){

        for (int code : srycd_ExclusiveTokuteiRyouyou){
            if (code == srycd) {
                return true;
            }
        }
        return isZaitakuKanri(srycd);
    }
    
    // 在宅管理があるかどうかウルトラおおざっぱにチェックｗｗ
    private boolean isZaitakuKanri(int srycd) {
        for (int code : srycd_ZaitakuKanri){
            if (code == srycd) {
                return true;
            }
        }
        return false;
    }
    
    // 処方の有無
    private boolean hasShohou() {
        return hasInMed || hasExMed;
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
        
        // ver 2.2m
        followMedicom = Project.getBoolean(MiscSettingPanel.FOLLOW_MEDICOM, true);  
        exMed = Project.getBoolean(MiscSettingPanel.RP_OUT, false);
        // ver 1.4m
        //followMedicom = MiscSettingPanel.getStub().getFollowMedicom();
        //exMed = MiscSettingPanel.getStub().getDefaultExMed();
        
        // 時間外対応加算フラグ
        if (dao.getSyskanriFlag(sk1006_jikangaiTaiou1)) {
            jikangaiTaiou = JikangaiTaiou.J_TAIOU1;
        } else if (dao.getSyskanriFlag(sk1006_jikangaiTaiou2)
                || dao.getSyskanriFlag(sk1006_chiikiKoken)) {
            jikangaiTaiou = JikangaiTaiou.J_TAIOU2;
        } else if (dao.getSyskanriFlag(sk1006_jikangaiTaiou3)) {
            jikangaiTaiou = JikangaiTaiou.J_TAIOU3;
        } else {
            jikangaiTaiou = JikangaiTaiou.J_TAIOU_NON;
        }
        
        // 有床か無床か
        hasBed = dao.hasBed();
        
        // 在宅療養支援診療所フラグ
        if (dao.getSyskanriFlag(sk1006_zaitakuShien1)
                || dao.getSyskanriFlag(sk1006_zaitakuShienHsp1)) {
            zaitakuShien = hasBed 
                    ? Shienshin.KYOKA_TANDOKU_WITH_BED  // 機能強化・単独・有床
                    : Shienshin.KYOKA_TANDOKU_WO_BED;   // 機能強化・単独・無床
        } else if (dao.getSyskanriFlag(sk1006_zaitakuShien2)
                || dao.getSyskanriFlag(sk1006_zaitakuShienHsp2)) {
            zaitakuShien = hasBed
                    ? Shienshin.KYOKA_RENKEI_WITH_BED   // 機能強化・連携・有床
                    : Shienshin.KYOKA_RENKEI_WO_BED;    // 機能強化・連携・無床
        } else if (dao.getSyskanriFlag(sk1006_zaitakuShien3old)
                || dao.getSyskanriFlag(sk1006_zaitakuShien3)) {
            zaitakuShien = Shienshin.SHIENSHIN;         // 普通の支援診
        } else {
            zaitakuShien = Shienshin.NON_SHIENSHIN;     // 非支援診
        }

        // 在宅時医学総合管理料
        SanteiInfoModel info = context.getPatient().getSanteiInfoModel();
        homeCare = info.isHomeMedicalCare();
        nursingHomeCare = info.isNursingHomeMedicalCare();
        zaitakuSougouKanri = dao.getSyskanriFlag(sk1006_zaiiSoukan);
        zaitakuSougouKanri = zaitakuSougouKanri && info.isZaitakuSougouKanri();
        
        // 登録されている病名を収集
        setupDiagnosis();
        
        // 外来管理加算と腫瘍マーカー設定
        setupGairaiKanriAndTumorMarkers();
        
        // 薬剤情報
        setupYakujouAvailable();
    }
    
    private void setupPastSanteiHistory() {
        
        Date fromThisMonth = getFromDateThisMonth(karteDate);
        // 今月の算定履歴を取得する
        pastSanteiListMonth = del.getSanteiHistory(karteId, fromThisMonth, karteDate, null);
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
                String srycd = ci.getCode();
                if (srycd != null) {
                    srycds.add(srycd);
                    allClaimItems.add(ci);
                }
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
                    shm.setSanteiDate(karteDate);
                    currentSanteiList.add(shm);
                }
            }
        }
    }
    
    private void setupDiagnosis() {
        
        // 病名を取得する
        DocumentDelegater ddl = DocumentDelegater.getInstance();
        diagnosis = ddl.getDiagnosisList(karteId, ModelUtils.AD1800, false);  // ver 2.2m
        //diagnosis = ddl.getDiagnosisList(karteId, new Date(0)); // ver 1.4m
        updateIkouTokutei2(diagnosis);
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
    
    private int parseInt(String str) {

        int num = 1;
        try {
            num = Integer.valueOf(str);
        } catch (Exception e) {
        }
        return num;
    }

    private Date[] getFromToDatePair() {
        
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(karteDate);
        int year = gc.get(GregorianCalendar.YEAR);
        int month = gc.get(GregorianCalendar.MONTH);
        gc.clear();
        gc.set(year, month, 1);    // 今月の初めから
        Date fromDate = gc.getTime();
        gc.setTime(karteDate);
        gc.add(GregorianCalendar.MILLISECOND, -1);  // 1msec前までｗ
        Date toDate = gc.getTime();
        
        return new Date[]{fromDate,toDate};
    }
    
    protected void setupYakujouAvailable() {

        choukiAvailable = false;
        yakujouAvailable = true;
        hasInMed = false;
        hasExMed = false;
        allHokatsuMed = true;
        int maxBundleNumber = 0;

        final String yakuzaiClassCode = "2";    // 薬剤のclaim class code

        // 編集中のカルテから薬剤を収集
        List<RpModel> currentRpContents = new ArrayList<RpModel>();
        for (ModuleModel mm : stamps) {
            if (IInfoModel.ENTITY_MED_ORDER.equals(mm.getModuleInfoBean().getEntity())) {

                ClaimBundle cb = (ClaimBundle) mm.getModel();
                String classCode = cb.getClassCode();
                String memo = cb.getMemo();
                // 院外処方がないかどうか調べる
                if (!hasExMed && 
                        ((classCode != null && classCode.endsWith("2")) 
                        || (memo != null && memo.contains("院外")))) {
                    hasExMed = true;
                }
                // 院内処方がないか調べる
                if (!hasInMed && 
                        ((classCode != null && classCode.endsWith("1"))
                        || (memo != null && memo.contains("院内")))) {
                    hasInMed = true;
                }
                // 包括処方以外がないか調べる
                if (allHokatsuMed && !cb.getClassCode().endsWith("3")) {
                    allHokatsuMed = false;
                }
                // 最大日数を調べる
                int rpDay = 0;
                try {
                    rpDay= Integer.valueOf(cb.getBundleNumber());
                    if (rpDay > maxBundleNumber) {
                        maxBundleNumber = rpDay;
                    }
                } catch (Exception e) {
                }
                // 薬剤を収集
                for (ClaimItem ci : cb.getClaimItem()) {
                    if (!yakuzaiClassCode.equals(ci.getClassCode())) {
                        continue;
                    }
                    // 薬剤なら
                    RpModel rpModel = 
                            new RpModel(ci.getCode(), ci.getName(), cb.getAdminCode(), 
                            ci.getNumber(), cb.getBundleNumber(), mm.getStarted());
                    currentRpContents.add(rpModel);
                }
            }
        }
        choukiAvailable = (maxBundleNumber >= defChoukiDay);

        // 院内処方がなければ薬剤情報は不可
        if (!hasInMed){
            yakujouAvailable = false;
            return;
        }

        // 今月の処方をデータベースから取得
        Date[] fromToDate = getFromToDatePair();
        Date fromDate = fromToDate[0];
        Date toDate = fromToDate[1];

        //薬剤のModuleModelを取得
        boolean lastOnly = !followMedicom;
        List<List<RpModel>> pastRpContentsList = del.getRpHistory(karteId, fromDate, toDate, lastOnly);
        if (pastRpContentsList == null || pastRpContentsList.isEmpty()) {
            return;
        }

        // 同じ処方があるかどうか調べる
        boolean sameRpExists = false;
        for (List<RpModel> pastRpContents : pastRpContentsList){
            if(isSameRp(currentRpContents, pastRpContents)){
                sameRpExists = true;
                break;
            }
        }
        yakujouAvailable = !sameRpExists;
    }

    private boolean isSameRp(List<RpModel> al1, List<RpModel> al2) {

        // 処方が同じかどうかを調べる
        // 薬剤数が違うなら、処方は違うはず！？
        if (al1.size() != al2.size()) {
            return false;
        }

        // 薬剤数が同じなら各項目を調べてみる
        for (RpModel rp1 : al1) {
            RpModel toRemove = null;
            for (RpModel rp2 : al2) {
                // 各々の薬剤項目が同じかどうか
                if (rp1.isSameWith(rp2)) {
                    // 同じ薬剤・用法・用量ならal2から項目を削除
                    toRemove = rp2;
                    break;
                }
            }
            if (toRemove != null) {
                al2.remove(toRemove);
            }
        }
        // すべて同じならal2の項目はなくなっているはず
        return al2.isEmpty();
    }
}
