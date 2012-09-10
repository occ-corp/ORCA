package open.dolphin.order;

import java.awt.Dimension;
import javax.swing.*;
import open.dolphin.client.ClientContext;

/**
 * RpVeiw改
 *
 * @author masuda, Masuda Naika
 */
public class RpView extends AbstractOrderView {

    private static final ImageIcon yakkaIcon = ClientContext.getImageIcon("calc_16.gif");
    
    private JCheckBox medicineCheck;
    private JCheckBox usageCheck;
    private JComboBox usageCombo;
    private JRadioButton inRadio;
    private JRadioButton outRadio;
    private JRadioButton admRadio;
    private JRadioButton rb_teiki;
    private JRadioButton rb_rinji;
    private JButton btn_yakka;
    private JCheckBox cbHoukatsu;
    private JCheckBox cbNoCharge;

    public RpView() {
        super();
        initComponents();
    }

    @Override
    protected final void initComponents() {

        // 情報パネル
        infoPanel.add(infoLabel);
        infoPanel.add(Box.createHorizontalGlue());
        medicineCheck = new JCheckBox("薬剤");
        medicineCheck.setToolTipText("薬剤がセットにあればチェックされます。");
        medicineCheck.setEnabled(false);
        infoPanel.add(medicineCheck);
        usageCheck = new JCheckBox("用法");
        usageCheck.setToolTipText("セットに用法があればチェックされます。");
        usageCheck.setEnabled(false);
        infoPanel.add(usageCheck);

        // コマンドパネル上部
        cmdPanel1.add(nameFieldLabel);
        cmdPanel1.add(stampNameField);
        rb_teiki = new JRadioButton("定期");
        rb_teiki.setToolTipText("定期処方の時選択します。");
        rb_rinji = new JRadioButton("臨時");
        rb_rinji.setToolTipText("臨時処方の時選択します。");
        admRadio = new JRadioButton("入院");
        admRadio.setToolTipText("入院処方のとき選択します。");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rb_teiki);
        bg.add(rb_rinji);
        bg.add(admRadio);
        JPanel pnl = new JPanel();
        //pnl.setLayout(new FlowLayout(FlowLayout.LEFT));
        pnl.setBorder(BorderFactory.createEtchedBorder());
        pnl.add(rb_teiki);
        pnl.add(rb_rinji);  
        pnl.add(admRadio);
        cmdPanel1.add(pnl);
        
        inRadio = new JRadioButton("院内");
        inRadio.setToolTipText("院内処方の時選択します。");
        outRadio = new JRadioButton("院外");
        outRadio.setToolTipText("院外処方の時選択します。");
        bg = new ButtonGroup();
        bg.add(inRadio);
        bg.add(outRadio);
        pnl = new JPanel();
        //pnl.setLayout(new FlowLayout(FlowLayout.LEFT));
        pnl.setBorder(BorderFactory.createEtchedBorder());
        pnl.add(inRadio);
        pnl.add(outRadio);
        cmdPanel1.add(pnl);

        cbHoukatsu = new JCheckBox("包括");
        cmdPanel1.add(cbHoukatsu);
        cbNoCharge = new JCheckBox("調無");
        cbNoCharge.setToolTipText("入院処方で調剤料を算定しない場合に選択します。");
        cmdPanel1.add(cbNoCharge);
        cmdPanel1.add(Box.createHorizontalGlue());
        btn_yakka = new JButton(yakkaIcon);
        btn_yakka.setToolTipText("剤の薬価計算/ORCA処方参照");
        cmdPanel1.add(btn_yakka);
        cmdPanel1.add(deleteBtn);
        cmdPanel1.add(clearBtn);
        cmdPanel1.add(okBtn);
        cmdPanel1.add(okCntBtn);

        // コマンドパネル下部
        cmdPanel2.add(loupeLabel);
        cmdPanel2.add(searchTextField);
        cmdPanel2.add(rtCheck);
        cmdPanel2.add(partialCheck);
        usageCombo = new JComboBox(new String[]{"用法選択"});
        fixComponentSize(usageCombo);
        cmdPanel2.add(usageCombo);
        cmdPanel2.add(Box.createHorizontalGlue());
        cmdPanel2.add(countLabel);
        cmdPanel2.add(countField);

        // 処方エディタではセットテーブルを大きめにする
        scrollSetTable.setPreferredSize(new Dimension(300, 300));
        scrollSrTable.setPreferredSize(new Dimension(300, 200));
    }

    public JCheckBox getMedicineCheck() {
        return medicineCheck;
    }
    public JCheckBox getUsageCheck() {
        return usageCheck;
    }
    public JButton getBtnYakka() {
        return btn_yakka;
    }
    public JRadioButton getRbTeiki() {
        return rb_teiki;
    }
    public JRadioButton getRbRinji() {
        return rb_rinji;
    }    
    public JComboBox getUsageCombo() {
        return usageCombo;
    }
    public JRadioButton getInRadio() {
        return inRadio;
    }
    public JRadioButton getOutRadio() {
        return outRadio;
    }
    public JCheckBox getCbHoukatsu() {
        return cbHoukatsu;
    }
    public JRadioButton getRbAdmission() {
        return admRadio;
    }
    public JCheckBox getCbNoCharge() {
        return cbNoCharge;
    }
}
