
package open.dolphin.order;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;

/**
 * DiseaseVeiw改
 *
 * @author masuda, Masuda Naika
 */
public class DiseaseView extends AbstractOrderView {
    
    private JCheckBox diseaseCheck;
    private JButton btn_modifier;

    public DiseaseView() {
        super();
        initComponents();
    }

    @Override
    protected final void initComponents() {

        // 情報パネル
        infoLabel.setText("傷病名");
        infoPanel.add(infoLabel);
        infoPanel.add(Box.createHorizontalGlue());
        diseaseCheck = new JCheckBox("傷病名");
        diseaseCheck.setToolTipText("傷病名が含まれていればチェックされます。");
        diseaseCheck.setEnabled(false);
        infoPanel.add(diseaseCheck);

        // コマンドパネル上部
        nameFieldLabel.setText("連結傷病名");
        cmdPanel1.add(nameFieldLabel);
        cmdPanel1.add(stampNameField);
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
        btn_modifier = new JButton("修飾語");
        btn_modifier.setToolTipText("修飾語を検索します。");
        cmdPanel2.add(btn_modifier);
        cmdPanel2.add(Box.createHorizontalGlue());
        cmdPanel2.add(countLabel);
        cmdPanel2.add(countField);

    }

    public JCheckBox getDiseaseCheck() {
        return diseaseCheck;
    }
    public JButton getModifierBtn() {
        return btn_modifier;
    }

}
