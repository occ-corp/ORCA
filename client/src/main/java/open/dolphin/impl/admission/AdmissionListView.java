package open.dolphin.impl.admission;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.*;
import open.dolphin.client.ClientContext;

/**
 * 入院リストのGUI
 * @author masuda, Masuda Naika
 */
public class AdmissionListView extends JPanel {
    
    private JButton kutuBtn;
    private JLabel infoLbl;
    private JTable table;

    public AdmissionListView() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        kutuBtn = new JButton();
        kutuBtn.setIcon(ClientContext.getImageIcon("kutu01.gif"));
        kutuBtn.setAlignmentY(BOTTOM_ALIGNMENT);
        panel.add(kutuBtn);

        infoLbl = new JLabel();
        infoLbl.setFont(new Font("Lucida Grande", 0, 10));
        infoLbl.setAlignmentY(BOTTOM_ALIGNMENT);
        panel.add(infoLbl);
        panel.add(Box.createHorizontalGlue());

        table = new JTable();
        JScrollPane scroll = new JScrollPane(table);

        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.NORTH);
        this.add(scroll, BorderLayout.CENTER);
    }

    public JButton getKutuBtn() {
        return kutuBtn;
    }

    public JTable getTable() {
        return table;
    }

    public JLabel getInfoLbl() {
        return infoLbl;
    }
}
