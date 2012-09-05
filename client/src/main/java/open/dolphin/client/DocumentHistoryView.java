
package open.dolphin.client;

import java.awt.BorderLayout;
import javax.swing.*;

/**
 * DocumentHistoryView改
 *
 * @author masuda, Masuda Naika
 */
public class DocumentHistoryView extends JPanel{

    private JTable table;
    private JScrollPane scroll;
    private JComboBox docTypeCombo;
    private JCheckBox deptChk;
    private JComboBox periodCombo;
    private JLabel cntLbl;


    public DocumentHistoryView() {

        table = new JTable();
        scroll = new JScrollPane(table);
        docTypeCombo = new JComboBox();
        fixComponentSize(docTypeCombo);
        deptChk = new JCheckBox("自科");
        fixComponentSize(deptChk);
        periodCombo = new JComboBox();
        fixComponentSize(periodCombo);
        cntLbl = new JLabel("0件");
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.add(docTypeCombo);
        south.add(deptChk);
        south.add(periodCombo);
        south.add(Box.createHorizontalGlue());
        south.add(cntLbl);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        
    }

    private void fixComponentSize(JComponent comp) {
        comp.setMaximumSize(comp.getPreferredSize());
    }

    public JLabel getCntLbl() {
        return cntLbl;
    }

    public JComboBox getDocTypeCombo() {
        return docTypeCombo;
    }
    
    public JCheckBox getDeptChk() {
        return deptChk;
    }

    public JComboBox getExtractCombo() {
        return periodCombo;
    }

    public JTable getTable() {
        return table;
    }
}
