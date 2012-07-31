package open.dolphin.impl.pvt;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.*;
import open.dolphin.client.ClientContext;

/**
 * WatingListView改
 *
 * @author masuda, Masuda Naika
 */
public class WatingListView extends JPanel {

    private JButton kutuBtn;
    private JLabel pvtInfoLbl;
    private RowTipsTable pvtTable;

    public WatingListView() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        kutuBtn = new JButton();
        kutuBtn.setIcon(ClientContext.getImageIcon("kutu01.gif"));
        kutuBtn.setAlignmentY(BOTTOM_ALIGNMENT);
        panel.add(kutuBtn);

        pvtInfoLbl = new JLabel();
        pvtInfoLbl.setFont(new Font("Lucida Grande", 0, 10));
        pvtInfoLbl.setAlignmentY(BOTTOM_ALIGNMENT);
        panel.add(pvtInfoLbl);
        panel.add(Box.createHorizontalGlue());

	JLabel underGoLbl = new JLabel();
	underGoLbl.setIcon(ClientContext.getImageIcon("apps_16.gif"));
	underGoLbl.setText("検査・処置等");
	underGoLbl.setAlignmentY(BOTTOM_ALIGNMENT);
	panel.add(underGoLbl);
/*
        JLabel openLbl = new JLabel();
        openLbl.setIcon(ClientContext.getImageIcon("open_16.gif"));
        openLbl.setText("オープン");
        openLbl.setAlignmentY(BOTTOM_ALIGNMENT);
        panel.add(openLbl);
*/
        JLabel flagLbl = new JLabel();
        flagLbl.setIcon(ClientContext.getImageIcon("flag_16.gif"));
        flagLbl.setText("診察終了");
        flagLbl.setAlignmentY(BOTTOM_ALIGNMENT);
        panel.add(flagLbl);

        pvtTable = new RowTipsTable();
        JScrollPane scroll = new JScrollPane(pvtTable);

        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.NORTH);
        this.add(scroll, BorderLayout.CENTER);
    }

    public JButton getKutuBtn() {
        return kutuBtn;
    }

    public JTable getTable() {
        return pvtTable;
    }

    public JLabel getPvtInfoLbl() {
        return pvtInfoLbl;
    }
}
