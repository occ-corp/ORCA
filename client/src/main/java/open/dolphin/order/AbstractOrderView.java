
package open.dolphin.order;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.border.Border;
import open.dolphin.client.ClientContext;
import open.dolphin.project.Project;

/**
 * AbstractOrderView.java
 *
 * Orderで使う共通のパネル
 * @author masuda, Masuda Naika
 */
public abstract class AbstractOrderView extends JPanel {

    protected static final int TEXTFIELD_WIDTH = 20;
    private static final Border border = BorderFactory.createEtchedBorder();
    private static final ImageIcon infoIcon    = ClientContext.getImageIcon("about_16.gif");
    private static final ImageIcon deleteIcon  = ClientContext.getImageIcon("del_16.gif");
    private static final ImageIcon clearIcon   = ClientContext.getImageIcon("remov_16.gif");
    private static final ImageIcon okIcon      = ClientContext.getImageIcon("lgicn_16.gif");
    private static final ImageIcon okCntIcon   = ClientContext.getImageIcon("apps_16.gif");
    private static final ImageIcon loupeIcon   = ClientContext.getImageIcon("srch_16.gif");
    
    private static final String setTableToolTip     = "セット内容は Drag & Drop で順番を入れ替えることができます。";
    private static final String stampNameFldToolTip = "セット名を編集します。";
    private static final String deleteBtnToolTip    = "選択した項目を削除します。";
    private static final String clearBtnToolTip     ="セット内容をクリアします。";
    private static final String okBtnToolTip        = "セットをカルテに展開し終了します";
    private static final String okCntBtnToolTip     = "セットをカルテに展開し継続します。";
    private static final String searchFldToolTip    = "検索したい点数マスタ項目を入力します。";
    private static final String countFldToolTip     = "検索結果の件数を表示します。";

    private static final String deleteBtnText   = "削除";
    private static final String clearBtnText    = "クリア";
    private static final String okBtnText       = "展開";
    private static final String okCntBtnText    = "展開継続";
    
    protected JPanel infoPanel;
    protected JLabel infoLabel;

    protected JTable setTable;
    protected JScrollPane scrollSetTable;

    protected JPanel cmdPanel;

    protected JPanel cmdPanel1;
    protected JLabel nameFieldLabel;
    protected JTextField stampNameField;
    protected JButton deleteBtn;
    protected JButton clearBtn;
    protected JButton okBtn;
    protected JButton okCntBtn;
    
    protected JPanel cmdPanel2;
    protected JLabel loupeLabel;
    protected JTextField searchTextField;
    protected JCheckBox rtCheck;
    protected JCheckBox partialCheck;
    protected JLabel countLabel;
    protected JTextField countField;
    
    protected JTable searchResultTable;
    protected JScrollPane scrollSrTable;
    
    // Editor button
    private static final String STAMP_EDITOR_BUTTON_TYPE = "stamp.editor.buttonType";
    private static final String BUTTON_TYPE_IS_ICON = "icon";
    //private static final String BUTTON_TYPE_IS_ITEXT = "text";


    protected AbstractOrderView() {
        initCommonComponents();
    }

    protected abstract void initComponents();
    
    private void initCommonComponents() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        boolean iconTbn = editorButtonTypeIsIcon();
        //setBorder(border);

        // infoPanel
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.X_AXIS));
        infoLabel = new JLabel(infoIcon);
        infoLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        infoLabel.setVerticalTextPosition(SwingConstants.CENTER);
        infoPanel.add(infoLabel);

        // セットテーブル
        setTable = new JTable();
        setTable.setToolTipText(setTableToolTip);
        scrollSetTable = new JScrollPane(setTable);

        // コマンドパネル上段
        cmdPanel1 = new JPanel();
        cmdPanel1.setLayout(new BoxLayout(cmdPanel1, BoxLayout.X_AXIS));
        nameFieldLabel = new JLabel("セット名");
        stampNameField = new JTextField(TEXTFIELD_WIDTH);
        fixComponentSize(stampNameField);
        stampNameField.setToolTipText(stampNameFldToolTip);
        stampNameField.setBackground(new Color(255, 255, 0));
        deleteBtn = iconTbn ? new JButton(deleteIcon) : new JButton(deleteBtnText);
        deleteBtn.setToolTipText(deleteBtnToolTip);
        clearBtn = iconTbn ? new JButton(clearIcon) : new JButton(clearBtnText);
        clearBtn.setToolTipText(clearBtnToolTip);
        okBtn = iconTbn ? new JButton(okIcon) : new JButton(okBtnText);
        okBtn.setToolTipText(okBtnToolTip);
        okCntBtn = iconTbn ? new JButton(okCntIcon) : new JButton(okCntBtnText);
        okCntBtn.setToolTipText(okCntBtnToolTip);

        // コマンドパネル下段
        cmdPanel2 = new JPanel();
        cmdPanel2.setLayout(new BoxLayout(cmdPanel2, BoxLayout.X_AXIS));
        loupeLabel = new JLabel(loupeIcon);
        searchTextField = new JTextField(TEXTFIELD_WIDTH);
        fixComponentSize(searchTextField);
        searchTextField.setToolTipText(searchFldToolTip);
        rtCheck = new JCheckBox("RT");
        partialCheck = new JCheckBox("部分一致");
        countLabel = new JLabel("件数");
        countField = new JTextField(3);
        fixComponentSize(countField);
        countField.setToolTipText(countFldToolTip);

        // コマンドパネル上下段合体
        cmdPanel = new JPanel();
        cmdPanel.setLayout(new BoxLayout(cmdPanel, BoxLayout.Y_AXIS));
        cmdPanel.setBorder(border);
        cmdPanel.add(cmdPanel1);
        cmdPanel.add(cmdPanel2);

        // 検索結果テーブル
        searchResultTable = new JTable();
        scrollSrTable = new JScrollPane(searchResultTable);
        
        // 全体レイアウト
        add(infoPanel);
        add(scrollSetTable);
        add(cmdPanel);
        add(scrollSrTable);
    }
    
    // Editor Button Type
    private boolean editorButtonTypeIsIcon() {
        String prop = Project.getString(STAMP_EDITOR_BUTTON_TYPE);
        return prop.equals(BUTTON_TYPE_IS_ICON);
    }
    
    protected final void fixComponentSize(JComponent comp) {
        comp.setMaximumSize(comp.getPreferredSize());
    }
    
    /**
     * @return the clearBtn
     */
    public JButton getClearBtn() {
        return clearBtn;
    }

    /**
     * @return the countField
     */
    public JTextField getCountField() {
        return countField;
    }

    /**
     * @return the deleteBtn
     */
    public JButton getDeleteBtn() {
        return deleteBtn;
    }

    /**
     * @return the infoLabel
     */
    public JLabel getInfoLabel() {
        return infoLabel;
    }

    /**
     * @return the okBtn
     */
    public JButton getOkBtn() {
        return okBtn;
    }

    /**
     * @return the okCntBtn
     */
    public JButton getOkCntBtn() {
        return okCntBtn;
    }

    /**
     * @return the searchResultTabel
     */
    public JTable getSearchResultTable() {
        return searchResultTable;
    }

    /**
     * @return the searchTextField
     */
    public JTextField getSearchTextField() {
        return searchTextField;
    }

    /**
     * @return the stampNameField
     */
    public JTextField getStampNameField() {
        return stampNameField;
    }

    /**
     * @return the setTable
     */
    public JTable getSetTable() {
        return setTable;
    }

    /**
     * @return the rtCheck
     */
    public JCheckBox getRtCheck() {
        return rtCheck;
    }
    
    public JCheckBox getPartialChk() {
        return partialCheck;
    }
    
    protected void fixCmdPanelHeight() {
        // 高さを固定
        Dimension d = new Dimension(Integer.MAX_VALUE, cmdPanel.getPreferredSize().height);
        cmdPanel.setMaximumSize(d);  
    }
}
