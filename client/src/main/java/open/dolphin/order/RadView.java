
package open.dolphin.order;

import javax.swing.*;

/**
 * BaseVeiw改
 *
 * @author masuda, Masuda Naika
 */
public class RadView extends AbstractOrderView {
    
    private JCheckBox techCheck;
    private JCheckBox partCheck;
    private JTextField numberField;
    private JButton partBtn;
    private JButton zairyoBtn;
    private JButton shugiBtn;
    private JTextField commentField;

    public RadView() {
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
        partCheck = new JCheckBox("部位");
        partCheck.setToolTipText("部位がセットにあればチェックされます。");
        partCheck.setEnabled(false);
        infoPanel.add(partCheck);

        // コマンドパネル上部
        cmdPanel1.add(nameFieldLabel);
        cmdPanel1.add(stampNameField);
        cmdPanel1.add(new JLabel("数量・施行日"));
        numberField = new JTextField("1", 10);
        numberField.setToolTipText("入院手技の場合は施行日を'/1-3,5'の形式で入力します。");
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
        partBtn = new JButton("部位");
        cmdPanel2.add(partBtn);
        shugiBtn = new JButton("手技");
        cmdPanel2.add(shugiBtn);
        zairyoBtn = new JButton("材料");
        cmdPanel2.add(zairyoBtn);
        cmdPanel2.add(Box.createHorizontalGlue());
        cmdPanel2.add(countLabel);
        cmdPanel2.add(countField);

    }

    public JCheckBox getPartCheck() {
        return partCheck;
    }
    public JCheckBox getTechCheck() {
        return techCheck;
    }
    public JTextField getNumberField() {
        return numberField;
    }
    public JTextField getCommentField() {
        return commentField;
    }
    public JButton getPartBtn() {
        return partBtn;
    }
    public JButton getZairyoBtn() {
        return zairyoBtn;
    }
    public JButton getShugiBtn() {
        return shugiBtn;
    }
}
