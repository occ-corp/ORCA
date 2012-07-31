
package open.dolphin.client;

import java.awt.event.MouseEvent;
import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import open.dolphin.infomodel.AllergyModel;
import open.dolphin.table.ListTableModel;

/**
 * AllergyView改
 * @author masuda, Masuda Naika
 */
public class AllergyView extends JPanel{
    
    private JScrollPane scroll;
    private JTable table;

    public AllergyView() {
        
        table = new RowTipsTable();
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
    
    // メモはToolTipTextで表示する
    private class RowTipsTable extends JTable {

        @Override
        public String getToolTipText(MouseEvent e) {
            
            ListTableModel<AllergyModel> list = (ListTableModel<AllergyModel>) getModel();
            int row = rowAtPoint(e.getPoint());
            AllergyModel model = list.getObject(row);
            if (model != null) {
                return model.getMemo();
            }
            return null;
        }
    }
}
