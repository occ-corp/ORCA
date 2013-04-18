
package open.dolphin.impl.psearch;

import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import open.dolphin.client.ClientContext;

/**
 * PatientSearchView改
 * 
 * @author masuda, Masuda Naika
 */

public class PatientSearchView extends JPanel {
    
    private AddressTipsTable table;
    private JLabel loupeLbl;
    private JTextField keywordFld;
    private JRadioButton karteSearchBtn;
    private JRadioButton ptSearchBtn;
    private JComboBox methodCombo;

    public static final String HIBERNATE_SEARCH = "Ｈ検";
    public static final String ALL_SEARCH = "全て";
    public static final String CONTENT_SEARCH = "本文";
    
    public PatientSearchView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(new EmptyBorder(5, 0, 5, 0));

        loupeLbl = new JLabel();
        loupeLbl.setIcon(ClientContext.getImageIconAlias("icon_search"));
        loupeLbl.setToolTipText("popupから処方切れ患者を検索できます");
        panel.add(loupeLbl);
        panel.add(Box.createHorizontalStrut(5));

        ButtonGroup group = new ButtonGroup();
        ptSearchBtn = new JRadioButton("患者");
        ptSearchBtn.setToolTipText("患者氏名、電話番号、ID、来院日から患者検索");
        panel.add(ptSearchBtn);
        karteSearchBtn = new JRadioButton("カルテ");
        karteSearchBtn.setToolTipText("入力されたキーワードをカルテから検索");
        panel.add(karteSearchBtn);
        group.add(ptSearchBtn);
        group.add(karteSearchBtn);
        ptSearchBtn.setSelected(true);
/*
        cb_fullTextSearch = new JCheckBox("全文");
        panel.add(cb_fullTextSearch);
        panel.add(Box.createHorizontalStrut(5));
*/
        methodCombo = new JComboBox();
        methodCombo.addItem(HIBERNATE_SEARCH);
        methodCombo.addItem(ALL_SEARCH);
        methodCombo.addItem(CONTENT_SEARCH);
        methodCombo.setEnabled(false);
        panel.add(methodCombo);
        panel.add(Box.createHorizontalStrut(5));

        keywordFld = new JTextField();
        keywordFld.setToolTipText("キーワードを入力");
        panel.add(keywordFld);

        table = new AddressTipsTable();
        //table.getTableHeader().setToolTipText("タイトルバーをクリックするとソートできます");
        JScrollPane scroll = new JScrollPane(table);

        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.NORTH);
        this.add(scroll, BorderLayout.CENTER);
    }

    public JTable getTable() {
        return table;
    }

    public JTextField getKeywordFld() {
        return keywordFld;
    }

    public JRadioButton getKarteSearchBtn() {
        return karteSearchBtn;
    }

    public JRadioButton getPtSearchBtn() {
        return ptSearchBtn;
    }

    public JLabel getLoupeLbl() {
        return loupeLbl;
    }
    public JComboBox getMethodCombo() {
        return methodCombo;
    }
}
