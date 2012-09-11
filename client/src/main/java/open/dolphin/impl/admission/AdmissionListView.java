package open.dolphin.impl.admission;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.*;
import open.dolphin.client.ClientContext;

/**
 * 入院リストのGUI
 * @author masuda, Masuda Naika
 * 
 * "hospital_red_2_24.png" was obtained from
 * http://www.gettyicons.com/free-icon/108/point-of-interest-icon-set/free-clinic-icon-png/
 */
public class AdmissionListView extends JPanel {
    
    private JButton updateBtn;
    private JLabel infoLbl;
    private JTable table;

    public AdmissionListView() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        updateBtn = new JButton();
        updateBtn.setIcon(ClientContext.getImageIcon("hospital_red_2_24.png"));
        updateBtn.setAlignmentY(BOTTOM_ALIGNMENT);
        panel.add(updateBtn);

        infoLbl = new JLabel();
        infoLbl.setFont(new Font("Lucida Grande", 0, 10));
        infoLbl.setAlignmentY(BOTTOM_ALIGNMENT);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(infoLbl);
        panel.add(Box.createHorizontalGlue());

        table = new JTable();
        JScrollPane scroll = new JScrollPane(table);

        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.NORTH);
        this.add(scroll, BorderLayout.CENTER);
    }

    public JButton getUpdateBtn() {
        return updateBtn;
    }

    public JTable getTable() {
        return table;
    }

    public JLabel getInfoLbl() {
        return infoLbl;
    }
}
