
package open.dolphin.order;

import javax.swing.*;
import open.dolphin.client.ClientContext;

/**
 * BaseVeiw改
 *
 * @author masuda, Masuda Naika
 */
public class BaseView extends AbstractOrderView {
    
    private JCheckBox techCheck;
    private JTextField numberField;
    private JRadioButton inRadio;
    private JRadioButton outRadio;
    private JButton btn_laboTest;
    private JTextField commentField;
    private JButton btn_comment;
    private JButton btn_classCode;


    public BaseView() {
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
        btn_laboTest = new JButton();
        btn_laboTest.setIcon(ClientContext.getImageIcon("opts_16.gif"));
        btn_laboTest.setToolTipText("検査エディタを開きます。");
        cmdPanel1.add(btn_laboTest);
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
        btn_classCode = new JButton();
        btn_classCode.setIcon(ClientContext.getImageIcon("favs_16.gif"));
        btn_classCode.setToolTipText("診療行為区分を強制指定します。");
        cmdPanel2.add(btn_classCode);
        inRadio = new JRadioButton("院内");
        inRadio.setToolTipText("院内処方の時選択します。");
        outRadio = new JRadioButton("院外");
        outRadio.setToolTipText("院外処方の時選択します。");
        ButtonGroup bg = new ButtonGroup();
        bg.add(inRadio);
        bg.add(outRadio);
        cmdPanel2.add(inRadio);
        cmdPanel2.add(outRadio);
        cmdPanel2.add(Box.createHorizontalGlue());
        cmdPanel2.add(countLabel);
        cmdPanel2.add(countField);
        
        // 高さを固定
        fixCmdPanelHeight();  
    }


    public JCheckBox getTechCheck() {
        return techCheck;
    }
    public JTextField getNumberField() {
        return numberField;
    }
    public JRadioButton getInRadio() {
        return inRadio;
    }
    public JRadioButton getOutRadio() {
        return outRadio;
    }
    public JButton getLaboTestBtn() {
        return btn_laboTest;
    }
    public JTextField getCommentField() {
        return commentField;
    }
    public JButton getCommentBtn() {
        return btn_comment;
    }
    public JButton getClassCodeBtn() {
        return btn_classCode;
    }
    
}
