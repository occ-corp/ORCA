package open.dolphin.order;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.GregorianCalendar;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
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
    
    public String getValue() {
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
        table.setDefaultRenderer(Object.class, new PeriodTableRenderer());
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
    
    // わけわかんねーｗ　/1-3,5,7のようなフォーマットにする。
    private void construct() {
        
        StringBuilder sb = new StringBuilder();
        sb.append("/");
        
        int[] columns = table.getSelectedColumns();
        int len = table.getModel().getColumnCount();
        int topOfSeq=  -1;
        boolean first = true;
        
        for (int i = 0; i < len; ++i) {
            
            boolean found = false;
            for (int j = 0; j < columns.length; ++j) {
                if (i == columns[j]) {
                    found = true;
                }
            }

            if (found) {
                String str = (String) table.getModel().getValueAt(0, i);
                if (topOfSeq == -1) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(str);
                    topOfSeq = i;
                } else if (i == len - 1) {
                    sb.append("-");
                    sb.append(str);
                }
            } else {
                if (topOfSeq != -1 && i - topOfSeq > 1) {
                    String str = (String) table.getModel().getValueAt(0, i - 1);
                    sb.append("-");
                    sb.append(str);
                }
                topOfSeq = -1;
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
    
    private class PeriodTableRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            String dayOfWeek = (String) table.getModel().getValueAt(1, column);
            if ("土".equals(dayOfWeek)) {
                setForeground(Color.BLUE);
            } else if ("日".equals(dayOfWeek)) {
                setForeground(Color.RED);
            } else {
                setForeground(Color.BLACK);
            }

            return this;
        }
    }
}
