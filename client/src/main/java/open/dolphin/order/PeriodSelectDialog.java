package open.dolphin.order;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.GregorianCalendar;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author masuda
 */
public class PeriodSelectDialog extends JDialog {
    
    private JTable table;
    private JPanel panel;
    private JButton okBtn;
    private JButton cancelBtn;
    
    private String value;
    
    public PeriodSelectDialog() {
        initComponents();
        connect();
    }
    
    public String getValeu() {
        return value;
    }
    
    private void initComponents() {
        
        setTitle("期間入力");
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        setContentPane(panel);
        
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(false);
        DefaultTableModel tblModel = createTableModel();
        table.setModel(tblModel);
        for (int i = 0; i < tblModel.getColumnCount(); ++i) {
            table.getColumnModel().getColumn(i).setPreferredWidth(30);
        }
        panel.add(table);
        
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        cancelBtn = new JButton("取消");
        okBtn = new JButton("確定");
        btnPanel.add(cancelBtn);
        btnPanel.add(okBtn);
        panel.add(btnPanel);
        
        setModal(true);
    }
    
    private void construct() {
        
        StringBuilder sb = new StringBuilder();
        sb.append("/");
        
        int[] columns = table.getSelectedColumns();

        int topOfSeq=  -1;
        int len = table.getModel().getColumnCount();
        for (int i = 0; i < len; ++i) {
            boolean found = false;
            for(int j = 0; j < columns.length; ++j) {
                if (i == columns[j]) {
                    found = true;
                }
            }
            if (found) {
                if (topOfSeq == -1 || i == len - 1) {
                    String str = (String) table.getModel().getValueAt(0, i);
                    sb.append(str);
                    topOfSeq = i;
                }
            } else {
                if (topOfSeq != -1) {
                    if (i - topOfSeq > 1) {
                        sb.append("-");
                        String str = (String) table.getModel().getValueAt(0, i -1);
                        sb.append(str);
                    } else {
                        sb.append(",");
                        topOfSeq = -1;
                    }
                }
            }
        }
        value = sb.toString();
    }
    
    private void connect() {
        
        okBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                construct();
                setVisible(false);
            }
        });

        cancelBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
    }
    
    private DefaultTableModel createTableModel() {
        GregorianCalendar gc = new GregorianCalendar();
        int today = gc.get(GregorianCalendar.DAY_OF_MONTH);
        int dayOfWeek = gc.get(GregorianCalendar.DAY_OF_WEEK);
        int lastDay = gc.getActualMaximum(GregorianCalendar.DATE);
        int len = lastDay - today + 1;
        len = Math.min(len, 7);
        String[] header = new String[len];
        String[][] obj = new String[2][len];
        for (int i = 0; i < len; ++i) {
            obj[0][i] = String.valueOf(i + today);
            obj[1][i] = getDayOfWeek(i + dayOfWeek);
        }
        DefaultTableModel tblModel = new DefaultTableModel(obj, header);
        return tblModel;
    }
    
    private String getDayOfWeek(int i) {
        
        switch(i % 7) {
            case GregorianCalendar.SUNDAY:
                return "日";
            case GregorianCalendar.MONDAY:
                return "月";
            case GregorianCalendar.TUESDAY:
                return "火";
            case GregorianCalendar.WEDNESDAY:
                return "水";
            case GregorianCalendar.THURSDAY:
                return "木";
            case GregorianCalendar.FRIDAY:
                return "金";
            case 0:
                return "土";
        }
        return null;
    }
    
}
