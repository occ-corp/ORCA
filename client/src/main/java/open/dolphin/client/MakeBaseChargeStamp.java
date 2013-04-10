package open.dolphin.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.*;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.infomodel.*;

/**
 * MakeBaseChargeStamp.java
 * 再診料や外来管理加算など
 * 内科診療所で頻繁に算定する項目の入力補助パネル
 * Re-programed on 2012/01/06 わけわかんねーカオス状態ｗ
 * @author masuda, Masuda Naika
 */

public class MakeBaseChargeStamp extends CheckSantei {

    private boolean isModified;
    private ModuleModel retStamp;
    private JDialog dialog;
    private JPanel view;

    private static HashMap<Integer, ClaimItem> claimItemMap;

    private JButton btn_cancel;
    private JButton btn_insert;
    private JButton btn_update;
    private JCheckBox cb_chouki;
    private JCheckBox cb_denwa;
    private JCheckBox cb_doujitsu;
    private JCheckBox cb_dummy;
    private JCheckBox cb_gairaikanri;
    private JCheckBox cb_oushin;
    private JCheckBox cb_techou;
    private JCheckBox cb_tokuteishikkan;
    private JCheckBox cb_tokuteishohou;
    private JCheckBox cb_yakujou;
    private JCheckBox cb_genericName;
    private JLabel lbl_chouki;
    private JLabel lbl_gairai;
    private JLabel lbl_tokuRyou;
    private JLabel lbl_tokuShohou;
    private JRadioButton rb_exMed;
    private JRadioButton rb_inMed;
    private JRadioButton rb_jikangai;
    private JRadioButton rb_jikannai;
    private JRadioButton rb_kinkyu;
    private JRadioButton rb_kyujitsu;
    private JRadioButton rb_saishin;
    private JRadioButton rb_shinya;
    private JRadioButton rb_shoshin;
    private JRadioButton rb_riha1;
    private JRadioButton rb_riha2;
    private JRadioButton rb_yakan;
    
    private JCheckBox cb_zaitakuKanri;
    private JCheckBox cb_nursingHome;
    private JComboBox cmb_jikan;
    private JCheckBox cb_soukiKasan;
    private JCheckBox cb_juushouKasan;
    private JRadioButton rb_houmonShinsatsu;
    private JCheckBox cb_douitsu;
    private JLabel lbl_shienshin;

    // 往診診察時間コンボボックス
    private static final String[] timeItem = new String[]{
        "<60", "70", "80", "90",
        "100", "110", "120",
        "130", "140", "150",
        "160", "170", "180"};
    
    public static final String BCS_TITLE_IN = "基本料(院内)";
    public static final String BCS_TITLE_OUT = "基本料(院外)";
    
    // EditorFrameのwizardボタンを押したときはここから入る
    public final void enter(KarteEditor editor) {
        start(editor, null);
    }

    // KartePaneのStampHolderをクリックしたときはここから入る
    public final void enter2(StampHolder sh){
        Chart chart = sh.getKartePane().getParent().getContext();
        KarteEditor editor = chart.getKarteEditor();
        start(editor, sh);
    }

    public void exit() {
        collectData();
        makeRetModule();
        dialog.setVisible(false);
        dialog.dispose();
    }

    public ModuleModel getBaseChargeStamp() {
        return retStamp;
    }

    public boolean isModified() {
        return isModified;
    }

    private synchronized boolean prepareClaimItemMap(){

        if (claimItemMap != null){
            return true;
        }
        DecimalFormat srycdFrmt = new DecimalFormat(srycdFrmtStr);

        List<String> srycdList = new ArrayList<String>();
        srycdList.add(srycdFrmt.format(srycd_Saishin));
        srycdList.add(srycdFrmt.format(srycd_Saishin_Dummy));
        srycdList.add(srycdFrmt.format(srycd_Gairai_Riha1));
        srycdList.add(srycdFrmt.format(srycd_Gairai_Riha2));
        srycdList.add(srycdFrmt.format(srycd_Saishin_Denwa));
        srycdList.add(srycdFrmt.format(srycd_Saishin_Doujitsu));
        srycdList.add(srycdFrmt.format(srycd_Saishin_Doujitsu_Denwa));
        srycdList.add(srycdFrmt.format(srycd_Saishin_Jikangai));
        srycdList.add(srycdFrmt.format(srycd_Saishin_Kyujitsu));
        srycdList.add(srycdFrmt.format(srycd_Saishin_Shinya));
        srycdList.add(srycdFrmt.format(srycd_Oushin));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Kinkyu_Kasan1));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Shinya_Kasan1));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Yakan_Kasan1));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Kinkyu_Kasan2));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Shinya_Kasan2));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Yakan_Kasan2));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Kinkyu_Kasan3));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Shinya_Kasan3));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Yakan_Kasan3));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Kinkyu_Kasan4));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Shinya_Kasan4));
        srycdList.add(srycdFrmt.format(srycd_Oushin_Yakan_Kasan4));
        srycdList.add(srycdFrmt.format(srycd_Oushin_ShinryoJikan_Kasan));
        srycdList.add(srycdFrmt.format(srycd_Shoshin));
        srycdList.add(srycdFrmt.format(srycd_Shoshin_Jikangai_Kasan));
        srycdList.add(srycdFrmt.format(srycd_Shoshin_Kyujitsu_Kasan));
        srycdList.add(srycdFrmt.format(srycd_Shoshin_Shinya_Kasan));
        srycdList.add(srycdFrmt.format(srycd_Gairaikanri_Kasan));
        srycdList.add(srycdFrmt.format(srycd_Tokutei_Ryouyou));
        srycdList.add(srycdFrmt.format(srycd_Tokutei_Shohou));
        srycdList.add(srycdFrmt.format(srycd_Yakuzaijouhou));
        srycdList.add(srycdFrmt.format(srycd_Chouki_Shohou));
        srycdList.add(srycdFrmt.format(srycd_Chouki_Shohousen));
        srycdList.add(srycdFrmt.format(srycd_Tokutei_Shohou_Shohousen));
        srycdList.add(srycdFrmt.format(srycd_Techoukisai));
        srycdList.add(srycdFrmt.format(srycd_JikangaiTaikouKasan1));
        srycdList.add(srycdFrmt.format(srycd_JikangaiTaikouKasan2));
        srycdList.add(srycdFrmt.format(srycd_JikangaiTaikouKasan3));
        
        srycdList.add(srycdFrmt.format(srycd_ZaiiSoukanEx1));
        srycdList.add(srycdFrmt.format(srycd_ZaiiSoukanIn1));
        srycdList.add(srycdFrmt.format(srycd_ZaiiSoukanEx2));
        srycdList.add(srycdFrmt.format(srycd_ZaiiSoukanIn2));
        srycdList.add(srycdFrmt.format(srycd_ZaiiSoukanEx3));
        srycdList.add(srycdFrmt.format(srycd_ZaiiSoukanIn3));
        srycdList.add(srycdFrmt.format(srycd_ZaiiSoukanEx4));
        srycdList.add(srycdFrmt.format(srycd_ZaiiSoukanIn4));
        srycdList.add(srycdFrmt.format(srycd_TokuiSoukanEx1));
        srycdList.add(srycdFrmt.format(srycd_TokuiSoukanIn1));        
        srycdList.add(srycdFrmt.format(srycd_TokuiSoukanEx2));
        srycdList.add(srycdFrmt.format(srycd_TokuiSoukanIn2));
        srycdList.add(srycdFrmt.format(srycd_TokuiSoukanEx3));
        srycdList.add(srycdFrmt.format(srycd_TokuiSoukanIn3));        
        srycdList.add(srycdFrmt.format(srycd_TokuiSoukanEx4));
        srycdList.add(srycdFrmt.format(srycd_TokuiSoukanIn4));
        
        srycdList.add(srycdFrmt.format(srycd_ZaitakuSoukiKasan));
        srycdList.add(srycdFrmt.format(srycd_JuushoushaKasan));
        srycdList.add(srycdFrmt.format(srycd_HoumonShinsatsu_hidouitsu));
        srycdList.add(srycdFrmt.format(srycd_HoumonShinsatsu_douitsu_hitokutei));
        srycdList.add(srycdFrmt.format(srycd_HoumonShinsatsu_douitsu_tokutei));
        srycdList.add(srycdFrmt.format(srycd_HoumounShinsatsuJikan));
        
        srycdList.add(srycdFrmt.format(srycd_Yakuzaijouhou));
        srycdList.add(srycdFrmt.format(srycd_Techoukisai));
        srycdList.add(srycdFrmt.format(srycd_GenericName_Kasan));

        // ORCAにマスターを問い合わせるようにした
        final SqlMiscDao dao = SqlMiscDao.getInstance();
        List<TensuMaster> list = dao.getTensuMasterList(srycdList);
        if (!dao.isNoError()) {
            return false;
        }
        claimItemMap = new HashMap<Integer, ClaimItem>();
        for (TensuMaster tm : list){
            claimItemMap.put(Integer.valueOf(tm.getSrycd()), tensuMasterToClaimItem(tm));
        }
        return true;
    }

    private void start(KarteEditor editor, StampHolder sh) {

        // 編集元のスタンプホルダを設定する
        setSourceStampHolder(sh);
        
        KartePane kp = editor.getPPane();

        boolean failed = prepareClaimItemMap();
        if (!failed) {
            String title = ClientContext.getFrameTitle("基本料");
            JFrame frame = (context != null) ? context.getFrame() : null;
            JOptionPane.showMessageDialog(frame, "ORCAとの接続を確認してください。",
                    title, JOptionPane.ERROR_MESSAGE);
            return;
        }

        // KartePaneからModuleModelを取得する
        KarteStyledDocument doc = (KarteStyledDocument) kp.getTextPane().getDocument();
        List<ModuleModel> stamps = doc.getStamps();
        
        // CheckSanteiの初期化
        try {
            init(editor.getContext(), stamps, editor.getModel().getDocInfoModel().getFirstConfirmDate());
        } catch (Exception ex) {
            return;
        }

        if (diagnosis == null || diagnosis.isEmpty()) {
            String title = ClientContext.getFrameTitle("基本料");
            JFrame frame = (context != null) ? context.getFrame() : null;
            JOptionPane.showMessageDialog(frame, "病名がありません。病名登録してください。",
                    title, JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        initComponents();
        
        check(false);
        setInfoLabel();

        // 新規作成の場合はカルテの内容に応じてチェックを入れる
        if (sh == null) {
            autoCheckup();
        } else {
            // 既存の編集の場合
            checkup(sh);
        }
        // dialogを表示
        showDialog();
    }

    private void showDialog(){

        dialog = new JDialog((Frame) null, true);
        ClientContext.setDolphinIcon(dialog);

        dialog.setContentPane(view);

        // dialogのタイトルを設定
        dialog.setTitle("基本料スタンプ");
        dialog.pack();
        ComponentMemory cm = new ComponentMemory(dialog, new Point(100, 100), dialog.getPreferredSize(), MakeBaseChargeStamp.this);
        cm.setToPreferenceBounds();
        //dialog.setSize(dialog.getPreferredSize());
        //dialog.setResizable(false);
        dialog.setVisible(true);
    }

    // 既存スタンプからチェックボックスを設定
    private void checkup(StampHolder sh) {

        rb_jikannai.setSelected(true);
        // 院内・院外処方はPreferrenceから設定
        if (exMed){
            rb_exMed.setSelected(true);
        } else {
            rb_inMed.setSelected(true);
        }
        controlExMedRadio();

        BundleDolphin bundle = (BundleDolphin) sh.getStamp().getModel();
        ClaimItem[] ci = bundle.getClaimItem();
        for (ClaimItem item : ci) {
            int srycd = Integer.valueOf(item.getCode());
            switch (srycd) {
                case srycd_Saishin:
                    rb_saishin.setSelected(true);
                    break;
                case srycd_Saishin_Dummy:
                    rb_saishin.setSelected(true);
                    cb_dummy.setSelected(true);
                    break;
                case srycd_Gairai_Riha1:
                    rb_riha1.setSelected(true);
                    break;
                case srycd_Gairai_Riha2:
                    rb_riha2.setSelected(true);
                    break;
                case srycd_Saishin_Denwa:
                    rb_saishin.setSelected(true);
                    cb_denwa.setSelected(true);
                    break;
                case srycd_Saishin_Doujitsu:
                    rb_saishin.setSelected(true);
                    cb_doujitsu.setSelected(true);
                    break;
                case srycd_Saishin_Doujitsu_Denwa:
                    rb_saishin.setSelected(true);
                    cb_doujitsu.setSelected(true);
                    cb_denwa.setSelected(true);
                    break;
                case srycd_Saishin_Jikangai:
                    rb_saishin.setSelected(true);
                    rb_jikangai.setSelected(true);
                    break;
                case srycd_Saishin_Kyujitsu:
                    rb_saishin.setSelected(true);
                    rb_kyujitsu.setSelected(true);
                    break;
                case srycd_Saishin_Shinya:
                    rb_saishin.setSelected(true);
                    rb_shinya.setSelected(true);
                    break;
                case srycd_Oushin:
                    cb_oushin.setSelected(true);
                    break;
                case srycd_Oushin_Kinkyu_Kasan1:
                case srycd_Oushin_Kinkyu_Kasan2:
                case srycd_Oushin_Kinkyu_Kasan3:
                case srycd_Oushin_Kinkyu_Kasan4:
                    cb_oushin.setSelected(true);
                    rb_kinkyu.setSelected(true);
                    break;
                case srycd_Oushin_Shinya_Kasan1:
                case srycd_Oushin_Shinya_Kasan2:
                case srycd_Oushin_Shinya_Kasan3:
                case srycd_Oushin_Shinya_Kasan4:
                    cb_oushin.setSelected(true);
                    rb_shinya.setSelected(true);
                    break;
                case srycd_Oushin_Yakan_Kasan1:
                case srycd_Oushin_Yakan_Kasan2:
                case srycd_Oushin_Yakan_Kasan3:
                case srycd_Oushin_Yakan_Kasan4:
                    cb_oushin.setSelected(true);
                    rb_yakan.setSelected(true);
                    break;
                case srycd_Oushin_ShinryoJikan_Kasan:
                    cb_oushin.setSelected(true);
                    setOushinJikanCombo(item);
                    break;
                case srycd_Shoshin:
                    rb_shoshin.setSelected(true);
                    break;
                case srycd_Shoshin_Jikangai_Kasan:
                    rb_shoshin.setSelected(true);
                    rb_jikangai.setSelected(true);
                    break;
                case srycd_Shoshin_Kyujitsu_Kasan:
                    rb_shoshin.setSelected(true);
                    rb_kyujitsu.setSelected(true);
                    break;
                case srycd_Shoshin_Shinya_Kasan:
                    rb_shoshin.setSelected(true);
                    rb_shinya.setSelected(true);
                    break;
                case srycd_Gairaikanri_Kasan:
                    cb_gairaikanri.setSelected(true);
                    break;
                case srycd_Tokutei_Ryouyou:
                    cb_tokuteishikkan.setSelected(true);
                    break;
                case srycd_Tokutei_Shohou:
                    cb_tokuteishohou.setSelected(true);
                    rb_inMed.setSelected(true);
                    break;
                case srycd_Tokutei_Shohou_Shohousen:
                    cb_tokuteishohou.setSelected(true);
                    rb_exMed.setSelected(true);
                    break;
                case srycd_Chouki_Shohou:
                    cb_chouki.setSelected(true);
                    rb_inMed.setSelected(true);
                    break;
                case srycd_Chouki_Shohousen:
                    cb_chouki.setSelected(true);
                    rb_exMed.setSelected(true);
                    break;
                case srycd_Yakuzaijouhou:
                    cb_yakujou.setSelected(true);
                    rb_inMed.setSelected(true);
                    break;
                case srycd_Techoukisai:
                    cb_yakujou.setSelected(true);
                    cb_techou.setSelected(true);
                    rb_inMed.setSelected(true);
                    break;
                case srycd_GenericName_Kasan:
                    cb_genericName.setSelected(true);
                    rb_exMed.setSelected(true);
                    break;

                // 在宅
                case srycd_ZaiiSoukanIn1:
                case srycd_ZaiiSoukanIn2:
                case srycd_ZaiiSoukanIn3:
                case srycd_ZaiiSoukanIn4:
                    cb_zaitakuKanri.setSelected(true);
                    cb_nursingHome.setSelected(false);
                    rb_inMed.setSelected(true);
                    break;
                case srycd_TokuiSoukanIn1:
                case srycd_TokuiSoukanIn2:
                case srycd_TokuiSoukanIn3:
                case srycd_TokuiSoukanIn4:
                    cb_zaitakuKanri.setSelected(true);
                    cb_nursingHome.setSelected(true);
                    rb_inMed.setSelected(true);
                    break;
                case srycd_ZaiiSoukanEx1:
                case srycd_ZaiiSoukanEx2:
                case srycd_ZaiiSoukanEx3:
                case srycd_ZaiiSoukanEx4:
                    cb_zaitakuKanri.setSelected(true);
                    cb_nursingHome.setSelected(false);
                    rb_inMed.setSelected(true);
                    break;
                case srycd_TokuiSoukanEx1:
                case srycd_TokuiSoukanEx2:
                case srycd_TokuiSoukanEx3:
                case srycd_TokuiSoukanEx4:
                    cb_zaitakuKanri.setSelected(true);
                    cb_nursingHome.setSelected(true);
                    rb_exMed.setSelected(true);
                    break;
                case srycd_ZaitakuSoukiKasan:
                    cb_soukiKasan.setSelected(true);
                    break;
                case srycd_JuushoushaKasan:
                    cb_juushouKasan.setSelected(true);
                    break;
                case srycd_HoumonShinsatsu_hidouitsu:
                    rb_houmonShinsatsu.setSelected(true);
                    cb_douitsu.setSelected(false);
                    break;
                case srycd_HoumonShinsatsu_douitsu_tokutei:
                case srycd_HoumonShinsatsu_douitsu_hitokutei:
                    rb_houmonShinsatsu.setSelected(true);
                    cb_douitsu.setSelected(true);
                    break;
                case srycd_HoumounShinsatsuJikan:
                    setOushinJikanCombo(item);
                    break;
            }
        }
    }

    // 往診・訪問診療時間コンボボックスを設定する
    private void setOushinJikanCombo(ClaimItem ci) {
        String numStr = ci.getNumber();
        int num = (numStr == null) ? 0 : Integer.valueOf(numStr);
        int time = 60;
        int i = 0;
        while (time < num) {
            time = time + 10;
            ++i;
            if (time > 180) {
                i = timeItem.length - 1;
                break;
            }
        }
        cmb_jikan.setSelectedIndex(i);
    }

    // 新規の場合
    private void autoCheckup() {

        // 院内・院外処方はPreferrenceから設定
        if (exMed){
            rb_exMed.setSelected(true);
        } else {
            rb_inMed.setSelected(true);
        }
        controlExMedRadio();
        
        rb_jikannai.setSelected(true);  // 時間内・時間外の判定はやっていない

        // 特定疾患管理
        setTokuShidouEnable(tokuShidouAvailable);
        // 特定処方
        setTokuShohouEnable(tokuShohouAvailable);
        // 長期処方
        setChoukiEnable(choukiAvailable);
        // 初再診
        if (!rb_shoshin.isSelected()) {
            rb_saishin.setSelected(true);
        }
        // 外来管理加算・初再診
        if (isShoshin) {
            rb_shoshin.setSelected(true);
            setGairaiKanriEnable(false);
        } else {
            rb_saishin.setSelected(true);
            setGairaiKanriEnable(true);
            // 同日判定
            if (isDoujitsu) {
                cb_doujitsu.setSelected(true);
            }
        }
        // 薬剤情報
        cb_yakujou.setSelected(yakujouAvailable);

        // 在宅
        cb_nursingHome.setSelected(false);
        cb_soukiKasan.setEnabled(false);
        cb_soukiKasan.setSelected(false);
        cb_juushouKasan.setEnabled(false);
        cb_juushouKasan.setSelected(false);
        
        if (homeCare || nursingHomeCare) {
            // 訪問診察料、別建物デフォルト
            rb_houmonShinsatsu.setSelected(true);
            cb_douitsu.setSelected(false);
            cb_nursingHome.setSelected(nursingHomeCare);
        }

        cb_zaitakuKanri.setSelected(zaitakuKanriAvailable);
        //setZaitakuSougouKanriEnable(zaitakuSougouKanri);

        cmb_jikan.setSelectedIndex(0);
    }

    protected void makeRetModule() {

        List<SrycdNumberPair> srycdList = collectData();
        if (srycdList.isEmpty()) {
            return;
        }

        // 常に新規のモデルとして返す
        retStamp = new ModuleModel();
        ModuleInfoBean moduleInfo = retStamp.getModuleInfoBean();
        moduleInfo.setEntity(IInfoModel.ENTITY_GENERAL_ORDER);
        moduleInfo.setStampRole(IInfoModel.ROLE_P);

        // スタンプ名を設定する
        if (rb_inMed.isSelected()) {
            moduleInfo.setStampName("基本料(院内)");
        } else {
            moduleInfo.setStampName("基本料(院外)");
        }
        // BundleDolphin を生成する
        BundleDolphin bundle = new BundleDolphin();
        // Dolphin Appli で使用するオーダ名称を設定する
        bundle.setOrderName(IInfoModel.TABNAME_GENERAL);

        boolean first = true;
        String claimClassCode = null;
        for (SrycdNumberPair pair : srycdList) {
            ClaimItem item = claimItemMap.get(pair.getSrycd());
            if (item == null) {
                continue;
            }
            if (first) {
                claimClassCode = item.getSrysyuKbn();
                first = false;
            }
            int num = pair.getNumber();
            if (num > 1) {
                item.setNumber(String.valueOf(num));
            }
            bundle.addClaimItem(item);
        }

        // バンドル数は"1"
        bundle.setBundleNumber("1");
        // 診療行為コード
        bundle.setClassCode(claimClassCode);
        bundle.setClassName(MMLTable.getClaimClassCodeName(claimClassCode));
        bundle.setClassCodeSystem(ClaimConst.CLASS_CODE_ID);    // Claim007 固定の値

        retStamp.setModel((IInfoModel) bundle);
        isModified = true;
    }

    private List<SrycdNumberPair> collectData() {

        List<SrycdNumberPair> srycdList = new ArrayList<SrycdNumberPair>();

        // 初診
        if (rb_shoshin.isSelected()) {
            srycdList.add(new SrycdNumberPair(srycd_Shoshin));
            if (rb_jikangai.isSelected()) {
                srycdList.add(new SrycdNumberPair(srycd_Shoshin_Jikangai_Kasan));
            }
            if (rb_kyujitsu.isSelected()) {
                srycdList.add(new SrycdNumberPair(srycd_Shoshin_Kyujitsu_Kasan));
            }
            if (rb_shinya.isSelected()) {
                srycdList.add(new SrycdNumberPair(srycd_Shoshin_Shinya_Kasan));
            }
        }
        // 再診
        if (rb_saishin.isSelected()) {
            if (!cb_dummy.isSelected()) {
                if (cb_denwa.isSelected()) {
                    SrycdNumberPair pair = cb_doujitsu.isSelected()
                            ? new SrycdNumberPair(srycd_Saishin_Doujitsu_Denwa)
                            : new SrycdNumberPair(srycd_Saishin_Denwa);
                    srycdList.add(pair);
                } else {
                    SrycdNumberPair pair = cb_doujitsu.isSelected()
                            ? new SrycdNumberPair(srycd_Saishin_Doujitsu)
                            : new SrycdNumberPair(srycd_Saishin);
                    srycdList.add(pair);
                }
                if (rb_jikangai.isSelected()) {
                    srycdList.add(new SrycdNumberPair(srycd_Saishin_Jikangai));
                }
                if (rb_kyujitsu.isSelected()) {
                    srycdList.add(new SrycdNumberPair(srycd_Saishin_Kyujitsu));
                }
                if (rb_shinya.isSelected()) {
                    srycdList.add(new SrycdNumberPair(srycd_Saishin_Shinya));
                }
            } else {
                srycdList.add(new SrycdNumberPair(srycd_Saishin_Dummy));
            }
            // 時間外対応加算
            if (!cb_dummy.isSelected()) {
                switch (jikangaiTaiou) {
                    case J_TAIOU1:
                        srycdList.add(new SrycdNumberPair(srycd_JikangaiTaikouKasan1));
                        break;
                    case J_TAIOU2:
                        srycdList.add(new SrycdNumberPair(srycd_JikangaiTaikouKasan2));
                        break;
                    case J_TAIOU3:
                        srycdList.add(new SrycdNumberPair(srycd_JikangaiTaikouKasan3));
                        break;
                    case J_TAIOU_NON:
                    default:
                        break;
                }
            }
        }
        
        // リハ１と２
        if (rb_riha1.isSelected()) {
            srycdList.add(new SrycdNumberPair(srycd_Gairai_Riha1));
        }
        if (rb_riha2.isSelected()) {
            srycdList.add(new SrycdNumberPair(srycd_Gairai_Riha2));
        }
        
        // 往診
        if (cb_oushin.isSelected()) {
            srycdList.add(new SrycdNumberPair(srycd_Oushin));
            switch (zaitakuShien) {
                case SHIENSHIN:
                    if (rb_yakan.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Yakan_Kasan2));
                    }
                    if (rb_shinya.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Shinya_Kasan2));
                    }
                    if (rb_kinkyu.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Kinkyu_Kasan2));
                    }
                    break;
                case KYOKA_RENKEI_WITH_BED:
                case KYOKA_TANDOKU_WITH_BED:
                    if (rb_yakan.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Yakan_Kasan3));
                    }
                    if (rb_shinya.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Shinya_Kasan3));
                    }
                    if (rb_kinkyu.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Kinkyu_Kasan3));
                    }
                    break;
                case KYOKA_RENKEI_WO_BED:
                case KYOKA_TANDOKU_WO_BED:
                    if (rb_yakan.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Yakan_Kasan4));
                    }
                    if (rb_shinya.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Shinya_Kasan4));
                    }
                    if (rb_kinkyu.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Kinkyu_Kasan4));
                    }
                    break;
                case NON_SHIENSHIN:
                default:
                    if (rb_yakan.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Yakan_Kasan1));
                    }
                    if (rb_shinya.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Shinya_Kasan1));
                    }
                    if (rb_kinkyu.isSelected()) {
                        srycdList.add(new SrycdNumberPair(srycd_Oushin_Kinkyu_Kasan1));
                    }
                    break;
            }
            
            // 時間加算は数量を設定する
            int i = cmb_jikan.getSelectedIndex();
            if (i != 0) {
                int number = i * 10 + 60;
                srycdList.add(new SrycdNumberPair(srycd_Oushin_ShinryoJikan_Kasan, number));
            }
        }
        // 外来管理加算
        if (cb_gairaikanri.isSelected()) {
            srycdList.add(new SrycdNumberPair(srycd_Gairaikanri_Kasan));
        }
        // 特定疾患療養管理料
        if (cb_tokuteishikkan.isSelected()) {
            srycdList.add(new SrycdNumberPair(srycd_Tokutei_Ryouyou));
        }
        // 特定疾患処方管理加算
        if (cb_tokuteishohou.isSelected()) {
            if (rb_inMed.isSelected()) {
                srycdList.add(new SrycdNumberPair(srycd_Tokutei_Shohou));
            } else {
                srycdList.add(new SrycdNumberPair(srycd_Tokutei_Shohou_Shohousen));
            }
        }
        // 長期投薬加算
        if (cb_chouki.isSelected()) {
            if (rb_inMed.isSelected()) {
                srycdList.add(new SrycdNumberPair(srycd_Chouki_Shohou));
            } else {
                srycdList.add(new SrycdNumberPair(srycd_Chouki_Shohousen));
            }
        }

        // 在宅時医学総合管理料・特定施設入居時等医学総合管理料
        if (cb_zaitakuKanri.isSelected()) {
            
            boolean rbInMed = rb_inMed.isSelected();
            
            switch (zaitakuShien) {
                case SHIENSHIN:
                    if (!cb_nursingHome.isSelected()) {
                        SrycdNumberPair pair = rbInMed
                                ? new SrycdNumberPair(srycd_ZaiiSoukanIn2)
                                : new SrycdNumberPair(srycd_ZaiiSoukanEx2);
                        srycdList.add(pair);
                    } else {
                        SrycdNumberPair pair = rbInMed
                                ? new SrycdNumberPair(srycd_TokuiSoukanIn2)
                                : new SrycdNumberPair(srycd_TokuiSoukanEx2);
                        srycdList.add(pair);
                    }
                    break;
                case KYOKA_RENKEI_WITH_BED:
                case KYOKA_TANDOKU_WITH_BED:
                    if (!cb_nursingHome.isSelected()) {
                        SrycdNumberPair pair = rbInMed
                                ? new SrycdNumberPair(srycd_ZaiiSoukanIn3)
                                : new SrycdNumberPair(srycd_ZaiiSoukanEx3);
                        srycdList.add(pair);
                    } else {
                        SrycdNumberPair pair = rbInMed
                                ? new SrycdNumberPair(srycd_TokuiSoukanIn3)
                                : new SrycdNumberPair(srycd_TokuiSoukanEx3);
                        srycdList.add(pair);
                    }
                    break;
                case KYOKA_RENKEI_WO_BED:
                case KYOKA_TANDOKU_WO_BED:
                    if (!cb_nursingHome.isSelected()) {
                        SrycdNumberPair pair = rbInMed
                                ? new SrycdNumberPair(srycd_ZaiiSoukanIn4)
                                : new SrycdNumberPair(srycd_ZaiiSoukanEx4);
                        srycdList.add(pair);
                    } else {
                        SrycdNumberPair pair = rbInMed
                                ? new SrycdNumberPair(srycd_TokuiSoukanIn4)
                                : new SrycdNumberPair(srycd_TokuiSoukanEx4);
                        srycdList.add(pair);
                    }
                    break;
                case NON_SHIENSHIN:
                default:
                    if (!cb_nursingHome.isSelected()) {
                        SrycdNumberPair pair = rbInMed
                                ? new SrycdNumberPair(srycd_ZaiiSoukanIn1)
                                : new SrycdNumberPair(srycd_ZaiiSoukanEx1);
                        srycdList.add(pair);
                    } else {
                        SrycdNumberPair pair = rbInMed
                                ? new SrycdNumberPair(srycd_TokuiSoukanIn1)
                                : new SrycdNumberPair(srycd_TokuiSoukanEx1);
                        srycdList.add(pair);
                    }
                    break;
            }

            // 在宅移行早期加算
            if (cb_soukiKasan.isSelected()) {
                srycdList.add(new SrycdNumberPair(srycd_ZaitakuSoukiKasan));
            }
            // 重症者加算
            if (cb_juushouKasan.isSelected()) {
                srycdList.add(new SrycdNumberPair(srycd_JuushoushaKasan));
            }
        }

        // 在宅患者訪問診療料
        if (rb_houmonShinsatsu.isSelected()) {
            if (cb_douitsu.isSelected()) {
                SrycdNumberPair pair = cb_nursingHome.isSelected()
                        ? new SrycdNumberPair(srycd_HoumonShinsatsu_douitsu_tokutei)
                        : new SrycdNumberPair(srycd_HoumonShinsatsu_douitsu_hitokutei);
                srycdList.add(pair);
            } else {
                srycdList.add(new SrycdNumberPair(srycd_HoumonShinsatsu_hidouitsu));
            }
            // 時間加算は数量を設定する
            int i = cmb_jikan.getSelectedIndex();
            if (i != 0) {
                int number = i * 10 + 60;
                srycdList.add(new SrycdNumberPair(srycd_HoumounShinsatsuJikan, number));
            }
        }

        // 薬剤情報提供料・手帳記載加算
        if (rb_inMed.isSelected() && cb_yakujou.isSelected()) {
            if (cb_techou.isSelected()) {
                srycdList.add(new SrycdNumberPair(srycd_Yakuzaijouhou));
                srycdList.add(new SrycdNumberPair(srycd_Techoukisai));
            } else {
                srycdList.add(new SrycdNumberPair(srycd_Yakuzaijouhou));
            }
        }
        
        // 一般名処方加算
        if (rb_exMed.isSelected() && cb_genericName.isSelected()) {
            srycdList.add(new SrycdNumberPair(srycd_GenericName_Kasan));
        }

        return srycdList;
    }

    private void setInfoLabel() {

        if (gairaiKanriAvailable){
            lbl_gairai.setText("可");
        } else {
            lbl_gairai.setText("不可");
        }

        lbl_tokuRyou.setText(String.valueOf(pastTokuRyouyouCount) + "回");
        lbl_tokuShohou.setText(String.valueOf(pastTokuShohouCount) + "回");
        lbl_chouki.setText(String.valueOf(pastChoukiShohouCount) + "回");

        // 初診が選択されてたら変更しない。とりあえず。
        if (rb_shoshin.isSelected()){
            isShoshin = true;
        }
    }

    // GUI
    private void initComponents() {

        view = new JPanel();
        view.setLayout(new BoxLayout(view, BoxLayout.X_AXIS));

        // １列目
        rb_shoshin = new JRadioButton("初診");
        rb_saishin = new JRadioButton("再診");
        cb_denwa = new JCheckBox("電話");
        cb_doujitsu = new JCheckBox("同日");
        cb_dummy = new JCheckBox("DMY");
        cb_oushin = new JCheckBox("往診");
        rb_riha1 = new JRadioButton("リハ1");
        rb_riha2 = new JRadioButton("リハ2");
        rb_houmonShinsatsu = new JRadioButton("訪問");
        cb_douitsu = new JCheckBox("同一");

        // ２列目
        rb_jikannai = new JRadioButton("時間内");
        rb_jikangai = new JRadioButton("時間外");
        rb_kyujitsu = new JRadioButton("休日");
        rb_shinya = new JRadioButton("深夜");
        rb_kinkyu = new JRadioButton("緊急");
        rb_yakan = new JRadioButton("夜間");
        rb_inMed = new JRadioButton("院内");
        rb_exMed = new JRadioButton("院外");
        cmb_jikan = new JComboBox(timeItem);
        
        // ３列目
        cb_gairaikanri = new JCheckBox("外来管理加算");
        cb_tokuteishikkan = new JCheckBox("特定疾患療養管理料");
        cb_tokuteishohou = new JCheckBox("特定処方管理加算");
        cb_yakujou = new JCheckBox("薬剤情報提供料");
        cb_techou = new JCheckBox("手帳記載");
        cb_genericName = new JCheckBox("一般名処方加算");
        cb_chouki = new JCheckBox("長期投薬");
        cb_zaitakuKanri = new JCheckBox("在宅時医学管理料");
        cb_nursingHome = new JCheckBox("施設");
        cb_soukiKasan = new JCheckBox("早期");
        cb_juushouKasan = new JCheckBox("重症");
        lbl_shienshin = new JLabel();
        
        // 在宅療養支援診療所
        switch (zaitakuShien) {
            case KYOKA_TANDOKU_WO_BED:
                lbl_shienshin.setText("強単無床");
                break;
            case KYOKA_TANDOKU_WITH_BED:
                lbl_shienshin.setText("強単有床");
                break;
            case KYOKA_RENKEI_WO_BED:
                lbl_shienshin.setText("強連無床");
                break;
            case KYOKA_RENKEI_WITH_BED:
                lbl_shienshin.setText("強連有床");
                break;
            case SHIENSHIN:
                lbl_shienshin.setText("支援診");
                break;
            case NON_SHIENSHIN:
            default:
                lbl_shienshin.setText("非支援診");
                break;
        }
        
        lbl_gairai = new JLabel();
        lbl_tokuRyou = new JLabel();
        lbl_tokuShohou = new JLabel();
        lbl_chouki = new JLabel();

        btn_update = new JButton("更新");
        btn_cancel = new JButton("取消");
        btn_insert = new JButton("挿入");

        ButtonGroup bg = new ButtonGroup();
        bg.add(rb_shoshin);
        bg.add(rb_saishin);
        bg.add(rb_houmonShinsatsu);
        bg.add(rb_riha1);
        bg.add(rb_riha2);
        bg = new ButtonGroup();
        bg.add(rb_jikannai);
        bg.add(rb_jikangai);
        bg.add(rb_kinkyu);
        bg.add(rb_kyujitsu);
        bg.add(rb_shinya);
        bg.add(rb_yakan);
        bg = new ButtonGroup();
        bg.add(rb_inMed);
        bg.add(rb_exMed);

        // １列目
        JPanel panel = createLeftAlignedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        rb_shoshin.setAlignmentX(Component.LEFT_ALIGNMENT);
        rb_saishin.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(rb_shoshin);
        
        panel.add(rb_saishin);
        JPanel tmp = createLeftAlignedPanel();
        tmp.setLayout(new BoxLayout(tmp, BoxLayout.X_AXIS));
        tmp.add(Box.createHorizontalStrut(10));
        tmp.add(cb_denwa);
        tmp.setMaximumSize(tmp.getPreferredSize());
        panel.add(tmp);
        tmp = createLeftAlignedPanel();
        tmp.setLayout(new BoxLayout(tmp, BoxLayout.X_AXIS));
        tmp.add(Box.createHorizontalStrut(10));
        tmp.add(cb_doujitsu);
        tmp.setMaximumSize(tmp.getPreferredSize());
        panel.add(tmp);
        tmp = createLeftAlignedPanel();
        tmp.setLayout(new BoxLayout(tmp, BoxLayout.X_AXIS));
        tmp.add(Box.createHorizontalStrut(10));
        tmp.add(cb_dummy);
        tmp.setMaximumSize(tmp.getPreferredSize());
        panel.add(tmp);
        
        panel.add(rb_riha1);
        panel.add(rb_riha2);
        
        tmp = createLeftAlignedPanel();
        tmp.setLayout(new BoxLayout(tmp, BoxLayout.X_AXIS));
        rb_houmonShinsatsu.setAlignmentX(Component.LEFT_ALIGNMENT);
        tmp.add(rb_houmonShinsatsu);
        panel.add(tmp);
        tmp = createLeftAlignedPanel();
        tmp.setLayout(new BoxLayout(tmp, BoxLayout.X_AXIS));
        tmp.add(Box.createHorizontalStrut(10));
        cb_douitsu.setAlignmentX(Component.LEFT_ALIGNMENT);
        tmp.add(cb_douitsu);
        tmp.setMaximumSize(tmp.getPreferredSize());
        panel.add(tmp);
        
        rb_inMed.setAlignmentX(Component.LEFT_ALIGNMENT);
        rb_exMed.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb_oushin.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cb_oushin);

        panel.add(Box.createGlue());
        view.add(panel);

        // ２列目
        panel = createLeftAlignedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(rb_jikannai);
        panel.add(rb_jikangai);
        panel.add(rb_kyujitsu);
        panel.add(rb_shinya);
        panel.add(rb_kinkyu);
        panel.add(rb_yakan);
        panel.add(Box.createVerticalStrut(5));
        panel.add(rb_inMed);
        panel.add(rb_exMed);
        
        cmb_jikan.setMaximumSize(cmb_jikan.getPreferredSize());
        cmb_jikan.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(Box.createVerticalStrut(5));
        panel.add(cmb_jikan);
        panel.add(Box.createGlue());
        view.add(panel);

        // ３列目
        panel = createLeftAlignedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        tmp = createLeftRightPanel(new JComponent[]{cb_gairaikanri, lbl_gairai});
        panel.add(tmp);
        tmp = createLeftRightPanel(new JComponent[]{cb_tokuteishikkan, lbl_tokuRyou});
        panel.add(tmp);
        tmp = createLeftRightPanel(new JComponent[]{cb_tokuteishohou, lbl_tokuShohou});
        panel.add(tmp);
        tmp = createLeftRightPanel(new JComponent[]{cb_yakujou, cb_techou, cb_genericName});
        panel.add(tmp);
        tmp = createLeftRightPanel(new JComponent[]{cb_genericName});
        panel.add(tmp);
        tmp = createLeftRightPanel(new JComponent[]{cb_chouki, lbl_chouki});
        panel.add(tmp);
        tmp = createLeftRightPanel(new JComponent[]{cb_zaitakuKanri, lbl_shienshin});
        panel.add(tmp);

        tmp = createLeftAlignedPanel();
        tmp.setLayout(new BoxLayout(tmp, BoxLayout.X_AXIS));
        tmp.add(Box.createHorizontalStrut(10));
        tmp.add(cb_nursingHome);
        tmp.add(cb_soukiKasan);
        tmp.add(cb_juushouKasan);
        tmp.setMaximumSize(tmp.getPreferredSize());
        panel.add(tmp);
        
        tmp = createLeftAlignedPanel();
        tmp.setLayout(new FlowLayout());
        tmp.add(btn_update);
        tmp.add(btn_cancel);
        tmp.add(btn_insert);
        tmp.setMaximumSize(tmp.getPreferredSize());
        panel.add(tmp);
        panel.add(Box.createGlue());
        view.add(panel);

        btn_insert.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });
        btn_cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        btn_update.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                autoCheckup();
            }
        });

        // ボタンの処理
        rb_saishin.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                
                setSaishinSubSelected(false);
                cb_douitsu.setSelected(false);

                boolean b = rb_saishin.isSelected();
                setSaishinSubEnabled(b);
                cb_douitsu.setEnabled(!b);
                setGairaiKanriEnable(b);
            }
        });
        
        rb_riha1.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                
                setSaishinSubSelected(false);
                cb_douitsu.setSelected(false);
                cb_zaitakuKanri.setSelected(false);
                setZaitakuSubSelected(false);

                boolean b = rb_riha1.isSelected();
                setSaishinSubEnabled(!b);
                cb_douitsu.setEnabled(!b);
                cb_zaitakuKanri.setEnabled(!b);
                setZaitakuSubEnabled(!b);
                setGairaiKanriEnable(!b);
            }
        });
        rb_riha2.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                
                setSaishinSubSelected(false);
                cb_douitsu.setSelected(false);
                cb_zaitakuKanri.setSelected(false);
                setZaitakuSubSelected(false);

                boolean b = rb_riha2.isSelected();
                setSaishinSubEnabled(!b);
                cb_douitsu.setEnabled(!b);
                cb_zaitakuKanri.setEnabled(!b);
                setZaitakuSubEnabled(!b);
                setGairaiKanriEnable(!b);
            }
        });
        
        rb_shoshin.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {

                setSaishinSubSelected(false);
                cb_douitsu.setSelected(false);

                boolean b = rb_shoshin.isSelected();
                setSaishinSubEnabled(!b);
                cb_douitsu.setEnabled(!b);
                setTokuShidouEnable(!b);
                setGairaiKanriEnable(!b);
            }
        });
        
        cb_oushin.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                
                setSaishinSubSelected(false);
                cb_douitsu.setSelected(false);

                boolean b = cb_oushin.isSelected();
                rb_saishin.setSelected(b);
                setSaishinSubEnabled(!b);
                cb_douitsu.setEnabled(b);
                rb_houmonShinsatsu.setEnabled(!b);
            }
        });
        
        rb_houmonShinsatsu.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                
                setSaishinSubSelected(false);
                cb_oushin.setSelected(false);

                boolean b = rb_houmonShinsatsu.isSelected();
                setSaishinSubEnabled(!b);
                cb_oushin.setEnabled(!b);
                setGairaiKanriEnable(!b);

            }
        });
        cb_denwa.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                
                boolean b = cb_denwa.isSelected();
                setGairaiKanriEnable(!b);
                setTokuShidouEnable(!b);
            }
        });

        cb_zaitakuKanri.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {

                cb_douitsu.setSelected(false);
                setZaitakuSubSelected(false);
                
                boolean b = cb_zaitakuKanri.isSelected();
                setZaitakuSubEnabled(b);
                setGairaiKanriEnable(!b);
                setTokuShidouEnable(!b);
                setTokuShohouEnable(!b);
                setChoukiEnable(!b);
            }
        });

        rb_exMed.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                controlExMedRadio();
            }
        });

        cb_tokuteishohou.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (cb_tokuteishohou.isSelected()) {
                    cb_chouki.setSelected(false);
                }
            }
        });
        cb_chouki.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (cb_chouki.isSelected()) {
                    cb_tokuteishohou.setSelected(false);
                }
            }
        });
        cb_yakujou.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (!cb_yakujou.isSelected()) {
                    cb_techou.setSelected(false);
                }
            }
        });
        cb_techou.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (cb_techou.isSelected()) {
                    cb_yakujou.setSelected(true);
                }
            }
        });
    }

    private void setSaishinSubSelected(boolean b) {
        cb_denwa.setSelected(b);
        cb_doujitsu.setSelected(b);
        cb_dummy.setSelected(b);
    }
    
    private void setSaishinSubEnabled(boolean b) {
        cb_denwa.setEnabled(b);
        cb_doujitsu.setEnabled(b);
        cb_dummy.setEnabled(b);
    }
    
    private void setZaitakuSubSelected(boolean b) {
        cb_nursingHome.setSelected(b);
        cb_soukiKasan.setSelected(b);
        cb_juushouKasan.setSelected(b);
    }
    private void setZaitakuSubEnabled(boolean b) {
        cb_nursingHome.setEnabled(b);
        cb_soukiKasan.setEnabled(b);
        cb_juushouKasan.setEnabled(b);
    }
    
    private void setTokuShidouEnable(boolean b) {
        if (tokuShidouAvailable && b) {
            cb_tokuteishikkan.setSelected(true);
            cb_tokuteishikkan.setEnabled(true);
        } else {
            cb_tokuteishikkan.setSelected(false);
            cb_tokuteishikkan.setEnabled(false);
        }
    }
    private void setTokuShohouEnable(boolean b) {
        if (tokuShohouAvailable && b) {
            cb_tokuteishohou.setSelected(true);
            cb_tokuteishohou.setEnabled(true);
        } else {
            cb_tokuteishohou.setSelected(false);
            cb_tokuteishohou.setEnabled(false);
        }
    }
    private void setChoukiEnable(boolean b) {
        if (choukiAvailable && b) {
            cb_chouki.setSelected(true);
            cb_chouki.setEnabled(true);
        } else {
            cb_chouki.setSelected(false);
            cb_chouki.setEnabled(false);
        }
    }

    private void setGairaiKanriEnable(boolean b) {
        if (gairaiKanriAvailable && b) {
            cb_gairaikanri.setSelected(true);
            cb_gairaikanri.setEnabled(true);
            lbl_gairai.setText("可");
        } else {
            cb_gairaikanri.setSelected(false);
            cb_gairaikanri.setEnabled(false);
            lbl_gairai.setText("不可");
        }
    }
    
    private void controlExMedRadio() {
        
        if (rb_exMed.isSelected()) {
            cb_yakujou.setSelected(false);
            cb_yakujou.setEnabled(false);
            cb_techou.setSelected(false);
            cb_techou.setEnabled(false);
            cb_genericName.setEnabled(true);
        } else {
            cb_yakujou.setEnabled(true);
            cb_techou.setEnabled(true);
            cb_genericName.setSelected(false);
            cb_genericName.setEnabled(false);
        }
    }

    private ClaimItem tensuMasterToClaimItem(TensuMaster tm){
        final String classCode = String.valueOf(ClaimConst.SYUGI);  // "0"手技
        ClaimItem cItem = new ClaimItem();
        cItem.setCode(tm.getSrycd()); // コード
        cItem.setName(tm.getName());// 名称
        cItem.setClassCode(classCode);// 診療種別区分は"0"
        cItem.setClassCodeSystem(ClaimConst.SUBCLASS_CODE_ID);// Claim003
        cItem.setSrysyuKbn(tm.getSrysyukbn());
        return cItem;
    }

    private JPanel createLeftAlignedPanel(){
        JPanel panel = new JPanel();
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
    
    private JPanel createLeftRightPanel(JComponent[] components) {
        JPanel panel = createLeftAlignedPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
        int count = components.length;
        components[0].setAlignmentX(Component.LEFT_ALIGNMENT);
        components[count - 1].setAlignmentX(Component.RIGHT_ALIGNMENT);
        for (int i = 0; i < count; ++i) {
            if (i != 0) {
                panel.add(Box.createGlue());
            }
            panel.add(components[i]);
        }
        // 高さのみ固定
        Dimension d = panel.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        panel.setMaximumSize(d);
        return panel;
    }

    private class SrycdNumberPair {

        private int srycd;
        private int number;

        private SrycdNumberPair(int srycd, int number) {
            this.srycd = srycd;
            this.number = number;
        }

        private SrycdNumberPair(int srycd) {
            this.srycd = srycd;
            number = 1;
        }

        private int getSrycd() {
            return srycd;
        }

        private int getNumber() {
            return number;
        }
    }
}
