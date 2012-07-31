
package open.dolphin.order;

import javax.swing.*;
import open.dolphin.client.ClientContext;

/**
 * InjectionVIew改
 *
 * @author masuda, Masuda Naika
 */
public class InjectionView extends AbstractOrderView {
    
    private JCheckBox techCheck;
    private JComboBox numberCombo;
    private JTextField commentField;
    private JButton btn_comment;
    private JCheckBox noChargeChk;


    public InjectionView() {
        super();
        initComponents();
    }

    @Override
    protected final void initComponents() {

        // 情報パネル
        infoPanel.add(infoLabel);
        infoPanel.add(Box.createHorizontalGlue());
        techCheck = new JCheckBox("診療行為");
        techCheck.setToolTipText("診療行為がセットにあればチェックされます。");
        techCheck.setEnabled(false);
        infoPanel.add(techCheck);

        // コマンドパネル上部
        cmdPanel1.add(nameFieldLabel);
        cmdPanel1.add(stampNameField);
        cmdPanel1.add(new JLabel("数量"));
        numberCombo = new JComboBox(new String[]{"1", "2", "3","4", "5", "6","7", "8", "9","10"});
        fixComponentSize(numberCombo);
        cmdPanel1.add(numberCombo);
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
        noChargeChk = new JCheckBox("注射手技料なし");
        noChargeChk.setToolTipText("手技料を算定しない時チェックします。");
        cmdPanel2.add(noChargeChk);
        cmdPanel2.add(Box.createHorizontalGlue());
        cmdPanel2.add(countLabel);
        cmdPanel2.add(countField);

    }


    public JCheckBox getTechChk() {
        return techCheck;
    }
    public JComboBox getNumberCombo() {
        return numberCombo;
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
}
