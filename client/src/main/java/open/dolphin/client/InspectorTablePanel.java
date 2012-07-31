
package open.dolphin.client;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * InspectorTablePanel
 *
 * @author masuda, Masuda Naika
 */
public class InspectorTablePanel extends JPanel {

    private JScrollPane scroll;
    private JTable table;

    public InspectorTablePanel() {
        
        table = new JTable();
        scroll = new JScrollPane(table);
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        GroupLayout.ParallelGroup hGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        hGroup.addComponent(scroll, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE);
        layout.setHorizontalGroup(hGroup);
        GroupLayout.ParallelGroup vGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        vGroup.addComponent(scroll, GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE);
        layout.setVerticalGroup(vGroup);
    }

    public JTable getTable() {
        return table;
    }
}
