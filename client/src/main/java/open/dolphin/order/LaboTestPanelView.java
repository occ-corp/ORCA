package open.dolphin.order;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import open.dolphin.infomodel.TensuMaster;

/**
 * 検体検査パネル LaboTestPanelView
 * GUI手打ち版
 *
 * @author masuda, Masuda Naika
 */
public class LaboTestPanelView extends JPanel{

    private JButton btn_cancel;
    private JButton btn_clearAll;
    private JButton btn_tenkai;
    private JLabel lbl_mishutoku;
    private JLabel lbl_otherItem;
    private JComboBox cmbStamp;
    private List<LaboCheckBox> checkBoxList;

    private static final int MARGIN = 5;

    public LaboTestPanelView() {
        checkBoxList = new ArrayList<LaboCheckBox>();
        initComponents();
    }
    
    public List<LaboCheckBox> getCheckBoxList() {
        return checkBoxList;
    }

    public JButton getBtnCancel() {
        return btn_cancel;
    }

    public JButton getBtnClearAll() {
        return btn_clearAll;
    }

    public JButton getBtnTenkai() {
        return btn_tenkai;
    }

    public JLabel getLblMishutoku() {
        return lbl_mishutoku;
    }

    public JLabel getLblOtherItem() {
        return lbl_otherItem;
    }

    public JComboBox getCmbStamp() {
        return cmbStamp;
    }


    public class LaboCheckBox extends JCheckBox {

        private int srycd;
        private TensuMaster tm;

        private LaboCheckBox(int srycd, String text) {
            this.srycd = srycd;
            setText(text);
            checkBoxList.add(LaboCheckBox.this);
        }

        public int getSrycd() {
            return srycd;
        }

        public TensuMaster getTensuMaster() {
            return tm;
        }

        public void setTensuMaster(TensuMaster tm) {
            this.tm = tm;
        }
    }

    private void initComponents() {

        // 全体はX軸のBoxLayout
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        // パネルのマージン設定
        this.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        
        // 一列目
        JPanel columnPanel = createBoxYPanel();     // 検査項目を縦並びに登録するパネルを作成
        // 血液学検査
        JPanel hamatology = createBoxYPanel();      // 血液学検査のパネルを作成
        hamatology.setBorder(BorderFactory.createEtchedBorder());
        JLabel lbl = new JLabel("血液学検査");       // 血液学検査パネルのタイトルを設定
        hamatology.add(lbl);
        hamatology.add(new LaboCheckBox(160008010, "末梢血液一般"));  // 検査項目を追加
        hamatology.add(new LaboCheckBox(160191510, "像（自動）"));
        hamatology.add(new LaboCheckBox(160008210, "像（鏡検）"));
        hamatology.add(new LaboCheckBox(160007910, "網状赤血球"));
        hamatology.add(new LaboCheckBox(160039110, "ABO血液型"));
        hamatology.add(new LaboCheckBox(160039210, "Rh(D)型"));
        columnPanel.add(hamatology);                // 列パネルに血液学検査パネルを追加

        // 凝固・線溶検査
        columnPanel.add(Box.createVerticalStrut(5));
        JPanel coagulation = createBoxYPanel();
        coagulation.setBorder(BorderFactory.createEtchedBorder());
        coagulation.add(new JLabel("凝固・線溶検査"));
        coagulation.add(new LaboCheckBox(160012010, "PT"));
        coagulation.add(new LaboCheckBox(160012310, "APTT"));
        //coagulation.add(new LaboCheckBox(160012610, "フィブリノゲン"));
        coagulation.add(new LaboCheckBox(160191610, "フィブリノゲン"));
        coagulation.add(new LaboCheckBox(160113510, "AT-III"));
        coagulation.add(new LaboCheckBox(160114010, "Ｄダイマー"));
        //coagulation.add(new LaboCheckBox(160014510, "FDP定量"));
        coagulation.add(new LaboCheckBox(160191910, "FDP定量"));
        columnPanel.add(coagulation);

        // 免疫血清学検査
        columnPanel.add(Box.createVerticalStrut(5));
        JPanel serology = createBoxYPanel();
        serology.setBorder(BorderFactory.createEtchedBorder());
        serology.add(new JLabel("免疫血清学検査"));
        serology.add(new LaboCheckBox(160054710, "CRP定量"));
        //serology.add(new LaboCheckBox(160053110, "RF定量"));
        serology.add(new LaboCheckBox(160195610, "RF定量"));
        serology.add(new LaboCheckBox(160173150, "MMP-3"));
        serology.add(new LaboCheckBox(160039910, "ASO"));
        serology.add(new LaboCheckBox(160039310, "間接クームス"));
        serology.add(new LaboCheckBox(160039410, "直接クームス"));
        serology.add(new LaboCheckBox(160052710, "寒冷凝集反応"));
        serology.add(new LaboCheckBox(160054310, "抗DNA抗体"));
        //serology.add(new LaboCheckBox(160182510, "抗核抗体"));
        serology.add(new LaboCheckBox(160195710, "抗核抗体"));
        serology.add(new LaboCheckBox(160168550, "KL-6"));
        columnPanel.add(serology);
        
        // 一列目全体を追加
        fixSize(columnPanel);
        this.add(columnPanel);
        
        // 二列目
        columnPanel = createBoxYPanel();
        columnPanel.setBorder(BorderFactory.createEtchedBorder());
        // 生化学検査
        JPanel biochemLabel = createBoxXPanel();
        lbl = new JLabel("生化学検査");
        biochemLabel.add(lbl);
        columnPanel.add(biochemLabel);

        // 生化学左右の入るパネル
        JPanel biochem = createBoxXPanel();
        // 生化学・左
        JPanel biochemLeft = createBoxYPanel();
        biochemLeft.add(new LaboCheckBox(160017410, "総蛋白"));
        biochemLeft.add(new LaboCheckBox(160018910, "アルブミン"));
        biochemLeft.add(new LaboCheckBox(160022810, "蛋白分画"));
        biochemLeft.add(new LaboCheckBox(160017010, "総ビリルビン"));
        biochemLeft.add(new LaboCheckBox(160017110, "直接ビリルビン"));
        biochemLeft.add(new LaboCheckBox(160022510, "GOT(AST)"));
        biochemLeft.add(new LaboCheckBox(160022610, "GPT(ALT)"));
        biochemLeft.add(new LaboCheckBox(160019510, "LDH"));
        biochemLeft.add(new LaboCheckBox(160020410, "γGTP"));
        biochemLeft.add(new LaboCheckBox(160020010, "ALP"));
        biochemLeft.add(new LaboCheckBox(160020510, "LAP"));
        biochemLeft.add(new LaboCheckBox(160020210, "ChE"));
        biochemLeft.add(new LaboCheckBox(160017850, "ZTT"));
        biochemLeft.add(new LaboCheckBox(160020310, "アミラーゼ"));
        biochemLeft.add(new LaboCheckBox(160024010, "リパーゼ"));
        biochemLeft.add(new LaboCheckBox(160020610, "CPK"));
        biochemLeft.add(new LaboCheckBox(160019310, "UA"));
        biochemLeft.add(new LaboCheckBox(160019010, "BUN"));
        biochemLeft.add(new LaboCheckBox(160019210, "クレアチニン"));
        biochemLeft.add(new LaboCheckBox(160021110, "Na・Cl"));
        biochemLeft.add(new LaboCheckBox(160021410, "K"));
        biochemLeft.add(new LaboCheckBox(160021510, "Ca"));
        biochemLeft.add(new LaboCheckBox(160021810, "無機P"));
        biochemLeft.add(new LaboCheckBox(160022210, "Mg"));
        biochem.add(biochemLeft);
        
        // 生化学・右
        JPanel biochemRight = createBoxYPanel();
        biochemRight.add(new LaboCheckBox(160022410, "総コレステロール"));
        biochemRight.add(new LaboCheckBox(160023410, "HDL-コレステロール"));
        biochemRight.add(new LaboCheckBox(160020910, "中性脂肪"));
        biochemRight.add(new LaboCheckBox(160167250, "LDL-コレステロール"));
        biochemRight.add(createSlimSeparator());
        biochemRight.add(new LaboCheckBox(160026810, "LDHアイソザイム"));
        biochemRight.add(new LaboCheckBox(160026310, "ALPアイソザイム"));
        biochemRight.add(new LaboCheckBox(160026410, "AMYアイソザイム"));
        biochemRight.add(new LaboCheckBox(160026710, "CPKアイソザイム"));
        biochemRight.add(createSlimSeparator());
        biochemRight.add(new LaboCheckBox(160019410, "グルコース"));
        biochemRight.add(new LaboCheckBox(160010010, "HbA1c"));
        biochemRight.add(new LaboCheckBox(160031510, "インスリン(IRI)"));
        biochemRight.add(new LaboCheckBox(160162050, "抗GAD抗体"));
        biochemRight.add(createSlimSeparator());
        biochemRight.add(new LaboCheckBox(160054910, "血清補体価 CH50"));
        biochemRight.add(new LaboCheckBox(160124350, "C3"));
        biochemRight.add(new LaboCheckBox(160124450, "C4"));
        biochemRight.add(new LaboCheckBox(160055210, "IgG"));
        biochemRight.add(new LaboCheckBox(160055010, "IgA"));
        biochemRight.add(new LaboCheckBox(160055310, "IgM"));
        //biochemRight.add(new LaboCheckBox(160118810, "非特異的IgE"));
        biochemRight.add(new LaboCheckBox(160197910, "非特異的IgE"));
        biochemRight.add(createSlimSeparator());
        biochemRight.add(new LaboCheckBox(160022110, "血清鉄"));
        biochemRight.add(new LaboCheckBox(160028710, "TIBC"));
        //biochemRight.add(new LaboCheckBox(160036810, "フェリチン"));
        biochemRight.add(new LaboCheckBox(160192510, "フェリチン定量"));
        biochemRight.add(new LaboCheckBox(160125650, "エリスロポエチン精密"));
        biochemRight.add(new LaboCheckBox(160029710, "ビタミンB12"));
        biochemRight.add(new LaboCheckBox(160115310, "葉酸"));
        biochem.add(biochemRight);
        columnPanel.add(biochem);
        
        // 二列目全体を追加
        fixSize(columnPanel);
        this.add(columnPanel);

        // 三列目
        columnPanel = createBoxYPanel();
        // 肝炎関連検査
        JPanel hepatitis = createBoxYPanel();
        hepatitis.setBorder(BorderFactory.createEtchedBorder());
        hepatitis.add(new JLabel("肝炎関連検査"));
        hepatitis.add(new LaboCheckBox(160046810, "HBs抗原定性"));
        hepatitis.add(new LaboCheckBox(160118510, "HCV抗体"));
        hepatitis.add(new LaboCheckBox(160049210, "HBs抗原精密"));
        hepatitis.add(new LaboCheckBox(160047410, "HBs抗体定性"));
        hepatitis.add(new LaboCheckBox(160049510, "HBs抗体精密"));
        hepatitis.add(new LaboCheckBox(160050010, "HBe抗原"));
        hepatitis.add(new LaboCheckBox(160050110, "HBe抗体"));
        hepatitis.add(new LaboCheckBox(160120710, "HBc抗体"));
        hepatitis.add(new LaboCheckBox(160121010, "IgM HBc抗体"));
        hepatitis.add(new LaboCheckBox(160160350, "HBV DNA定量"));
        hepatitis.add(new LaboCheckBox(160162450, "HCV群別"));
        hepatitis.add(new LaboCheckBox(160158450, "HCV RNA定量"));
        hepatitis.add(new LaboCheckBox(160167750, "HCV抗原(コア蛋白)"));
        hepatitis.add(new LaboCheckBox(160120810, "HA抗体"));
        hepatitis.add(new LaboCheckBox(160120910, "IgM HA抗体"));
        columnPanel.add(hepatitis);

        // 感染症関連検査
        columnPanel.add(Box.createVerticalStrut(5));
        JPanel infection = createBoxYPanel();
        infection.setBorder(BorderFactory.createEtchedBorder());
        infection.add(new JLabel("感染症関連検査"));
        infection.add(new LaboCheckBox(160039810, "梅毒定性(ガラス板)"));
        infection.add(new LaboCheckBox(160040910, "梅毒定性(TPHA)"));
        infection.add(new LaboCheckBox(160041010, "マイコプラズマ(CF)"));
        infection.add(new LaboCheckBox(160141850, "カンジダ抗原"));
        infection.add(new LaboCheckBox(160044010, "百日咳抗体"));
        infection.add(new LaboCheckBox(160167350, "C.ニューモニエIgG"));
        infection.add(new LaboCheckBox(160167450, "C.ニューモニエIgA"));
        infection.add(new LaboCheckBox(160169450, "インフルエンザ抗原"));
        columnPanel.add(infection);
        
        // 三列目全体
        fixSize(columnPanel);
        this.add(columnPanel);
        
        // 四列目・五列目・ボタン類
        columnPanel = createBoxYPanel();        // 全体
        JPanel fourthAndFifth = createBoxXPanel();   // 腫瘍、高血圧、内分泌、尿便
        // TM関連検査、四列目上
        JPanel fourth = createBoxYPanel();
        JPanel tumor = createBoxYPanel();
        tumor.setBorder(BorderFactory.createEtchedBorder());
        tumor.add(new JLabel("TM関連検査"));
        tumor.add(new LaboCheckBox(160036710, "AFP"));
        //tumor.add(new LaboCheckBox(160117110, "PIVKA-II"));
        tumor.add(new LaboCheckBox(160193310, "PIVKA-II"));
        tumor.add(new LaboCheckBox(160036510, "CEA"));
        tumor.add(new LaboCheckBox(160037210, "CA19-9"));
        tumor.add(new LaboCheckBox(160037710, "エラスターゼI"));
        tumor.add(new LaboCheckBox(160037510, "PSA"));
        tumor.add(new LaboCheckBox(160037410, "SCC"));
        tumor.add(new LaboCheckBox(160162250, "pro GRP"));
        tumor.add(new LaboCheckBox(160037910, "NSE"));
        tumor.add(new LaboCheckBox(160159050, "CKフラグメント(CYFRA)"));
        tumor.add(new LaboCheckBox(160036910, "DUPAN-2"));
        tumor.add(new LaboCheckBox(160117010, "SUPAN-1"));
        tumor.add(new LaboCheckBox(160158050, "可溶性IL-2R"));
        fourth.add(tumor);

        // 高血圧・心循環系検査、四列目下
        fourth.add(Box.createVerticalStrut(5));
        JPanel cardiology = createBoxYPanel();
        cardiology.setBorder(BorderFactory.createEtchedBorder());
        cardiology.add(new JLabel("高血圧・心循環系検査"));
        cardiology.add(new LaboCheckBox(160034110, "アルドステロン"));
        cardiology.add(new LaboCheckBox(160032210, "レニン活性"));
        cardiology.add(new LaboCheckBox(160162350, "BNP"));
        cardiology.add(new LaboCheckBox(160181250, "NT-proBNP"));
        cardiology.add(new LaboCheckBox(160116310, "HANP"));
        cardiology.add(new LaboCheckBox(160152850, "心筋トロポニンT"));
        fourth.add(cardiology);
        fourthAndFifth.add(fourth);

        // 内分泌機能関連検査、五列目上
        JPanel fifth = createBoxYPanel();
        JPanel endocrine = createBoxYPanel();
        endocrine = createBoxYPanel();
        endocrine.setBorder(BorderFactory.createEtchedBorder());
        endocrine.add(new JLabel("内分泌機能関連検査"));
        endocrine.add(new LaboCheckBox(160031710, "TSH"));
        endocrine.add(new LaboCheckBox(160033210, "fT3"));
        endocrine.add(new LaboCheckBox(160033310, "fT4"));
        endocrine.add(new LaboCheckBox(160031310, "T3"));
        endocrine.add(new LaboCheckBox(160031810, "T4"));
        endocrine.add(new LaboCheckBox(160035810, "TSHレセプター抗体"));
        endocrine.add(new LaboCheckBox(160157450, "抗TPO抗体"));
        endocrine.add(new LaboCheckBox(160141750, "抗サイログロブリン抗体"));
        endocrine.add(new LaboCheckBox(160176750, "マイクロゾームテスト"));
        endocrine.add(new LaboCheckBox(160035610, "ACTH"));
        endocrine.add(new LaboCheckBox(160034010, "コルチゾール"));
        fifth.add(endocrine);
 
        // 尿・便、五列目下
        fifth.add(Box.createVerticalStrut(5));
        JPanel urine = createBoxYPanel();
        urine.setBorder(BorderFactory.createEtchedBorder());
        urine.add(new JLabel("尿・便検査"));
        urine.add(new LaboCheckBox(160000310, "尿一般"));
        urine.add(new LaboCheckBox(160005010, "尿沈渣"));
        urine.add(new LaboCheckBox(160000410, "尿蛋白定量"));
        urine.add(new LaboCheckBox(160004810, "尿微量アルブミン"));
        urine.add(new LaboCheckBox(160132150, "尿クレアチニン"));
        urine.add(new LaboCheckBox(160006510, "便ヒトヘモグロビン"));
        urine.add(new LaboCheckBox(160177150, "尿中肺炎球菌抗原"));
        fifth.add(urine);

        // 外来迅速検体検査加算
        fifth.add(Box.createVerticalStrut(10));
        JPanel rapid = createBoxYPanel();
        rapid.setBorder(BorderFactory.createEtchedBorder());
        rapid.add(new LaboCheckBox(160177770, "外来迅速検体検査加算"));
        fifth.add(rapid);
        
        fourthAndFifth.add(fifth);
        columnPanel.add(fourthAndFifth);        

        // ボタン類
        JPanel cmdPanel = createBoxYPanel();  // コンボボックス、ラベル、ボタン
        // 上
        JPanel cmdRow = createBoxXPanel();
        cmdRow.add(Box.createHorizontalGlue());
        cmdRow.add(new JLabel("スタンプ参照"));
        cmbStamp = new JComboBox();
        cmbStamp.setMaximumRowCount(20);    // quaquaでは無視される
        cmbStamp.addItem("12345678901234567890");
        fixSize(cmbStamp);
        cmdRow.add(cmbStamp);
        cmdRow.add(Box.createHorizontalStrut(10));
        cmdPanel.add(cmdRow);
        // 下
        cmdPanel.add(Box.createVerticalStrut(5));
        cmdRow = createBoxXPanel();
        JPanel lblPanel = createBoxYPanel();
        lbl_otherItem = new JLabel("他検査項目あり");
        lbl_otherItem.setForeground(Color.RED);
        lbl_mishutoku = new JLabel("コード未取得です");
        lblPanel.add(lbl_otherItem);
        lblPanel.add(lbl_mishutoku);
        cmdRow.add(lblPanel);
        cmdRow.add(Box.createHorizontalGlue());
        JPanel btnPanel = createBoxXPanel();
        btn_clearAll = new JButton("クリア");
        btn_cancel = new JButton("取消");
        btn_tenkai = new JButton("展開");
        btnPanel.add(btn_clearAll);
        btnPanel.add(btn_cancel);
        btnPanel.add(btn_tenkai);
        cmdRow.add(btnPanel);
        cmdRow.add(Box.createHorizontalStrut(10));
        cmdPanel.add(cmdRow);
        columnPanel.add(Box.createVerticalStrut(20));
        columnPanel.add(cmdPanel);

        // 四・五列目全体
        fixSize(columnPanel);
        this.add(columnPanel);

    }

    private void fixSize(Component comp) {
        comp.setMaximumSize(comp.getPreferredSize());
    }

    private JComponent createSlimSeparator() {
        JComponent comp = (JComponent) Box.createVerticalStrut(1);
        comp.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        return comp;
    }

    private JPanel createBoxXPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setAlignmentY(TOP_ALIGNMENT);
        return panel;
    }

    private JPanel createBoxYPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setAlignmentY(TOP_ALIGNMENT);
        return panel;
    }
}
