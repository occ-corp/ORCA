
package open.dolphin.order;

import javax.swing.*;
import open.dolphin.client.ClientContext;
import open.dolphin.infomodel.ClaimConst;

/**
 * InjectionVIew改
 *
 * @author masuda, Masuda Naika
 */
public class InjectionView extends AbstractOrderView {
    
    private JCheckBox techCheck;
    private JTextField numberField;
    private JTextField commentField;
    private JButton btn_comment;
    private JCheckBox noChargeChk;
    private JComboBox shugiCmb;
    
    private static final String[] ITEM_NAME = {
        "- 注射手技選択 -", 
        "皮下・筋肉注射", 
        "静脈内注射", 
        "点滴注射", 
        "点滴注射(手技料無)", 
        "点滴注射(手術同日)", 
        "その他注射", 
        "中心静脈", 
        "中心静脈(手術同日)"};
    
    private static final String[] ITEM_CODE = {
        "", 
        ClaimConst.INJECTION_310, 
        ClaimConst.INJECTION_320, 
        ClaimConst.INJECTION_330, 
        ClaimConst.INJECTION_331, 
        ClaimConst.INJECTION_332, 
        ClaimConst.INJECTION_340, 
        ClaimConst.INJECTION_350, 
        ClaimConst.INJECTION_352};


    public InjectionView() {
        super();
        initComponents();
    }

    @Override
    protected final void initComponents() {

        // 情報パネル
        infoPanel.add(infoLabel);
        infoPanel.add(shinkuCmb);
        infoPanel.add(Box.createHorizontalGlue());
        techCheck = new JCheckBox("診療行為");
        techCheck.setToolTipText("診療行為がセットにあればチェックされます。");
        techCheck.setEnabled(false);
        infoPanel.add(techCheck);

        // コマンドパネル上部
        cmdPanel1.add(nameFieldLabel);
        cmdPanel1.add(stampNameField);
        cmdPanel1.add(new JLabel("数量・施行日"));
        numberField = new JTextField("1", 10);
        numberField.setToolTipText("入院手技の場合は施行日を'*2/1-3,5'の形式で入力します。");
        cmdPanel1.add(numberField);
        cmdPanel1.add(new JLabel("メモ"));
        commentField = new JTextField(TEXTFIELD_WIDTH);
        fixComponentSize(commentField);
        commentField.setToolTipText("メモを入力します。");
        cmdPanel1.add(commentField);
        cmdPanel1.add(Box.createHorizontalGlue());
        cmdPanel1.add(deleteBtn);
        cmdPanel1.add(clearBtn);
        cmdPanel1.add(okBtn);
        cmdPanel1.add(okCntBtn);

        // コマンドパネル下部
        cmdPanel2.add(loupeLabel);
        cmdPanel2.add(searchTextField);
        cmdPanel2.add(rtCheck);
        cmdPanel2.add(partialCheck);
        btn_comment = new JButton();
        btn_comment.setIcon(ClientContext.getImageIcon("sinfo_16.gif"));
        btn_comment.setToolTipText("コメントコードを検索します。");
        cmdPanel2.add(btn_comment);
        cmdPanel2.add(new JSeparator(JSeparator.VERTICAL));
        shugiCmb = createCmb();
        shugiCmb.setToolTipText("入院手技を指定します。");
        cmdPanel2.add(shugiCmb);
        noChargeChk = new JCheckBox("注射手技料なし");
        noChargeChk.setToolTipText("手技料を算定しない時チェックします。");
        cmdPanel2.add(noChargeChk);
        cmdPanel2.add(Box.createHorizontalGlue());
        cmdPanel2.add(countLabel);
        cmdPanel2.add(countField);
        
        // 高さを固定
        fixCmdPanelHeight();  
    }
    
    private JComboBox createCmb() {
        JComboBox cmb = new JComboBox();
        for (String name : ITEM_NAME) {
            cmb.addItem(name);
        }
        return cmb;
    }

    public String getSelectedClassCode() {
        int i = shugiCmb.getSelectedIndex();
        if (i == 0) {
            return null;
        }
        return ITEM_CODE[i];
    }
    
    public void selectCmbItem(String code) {
        for (int i = 0; i < ITEM_NAME.length; ++i) {
            String str = ITEM_NAME[i];
            if (str.equals(code)) {
                shugiCmb.setSelectedIndex(i);
                break;
            }
        }
    }

    public JCheckBox getTechChk() {
        return techCheck;
    }
    public JTextField getNumberField() {
        return numberField;
    }
    public JTextField getCommentField() {
        return commentField;
    }
    public JButton getCommentBtn() {
        return btn_comment;
    }
    public JCheckBox getNoChargeChk() {
        return noChargeChk;
    }
    public JComboBox getShugiCmb() {
        return shugiCmb;
    }
}
