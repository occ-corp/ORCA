package open.dolphin.order;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import open.dolphin.client.ClientContext;

/**
 * 入院診療行為の回数と実施日を入力するダイアログ
 *
 * @author masuda, Masuda Naika
 */
public class PeriodSelectDialog extends JDialog {

    private int COLUMN_COUNT = 10;
    private JTable table;
    private DefaultTableModel tableModel;
    private JPanel panel;
    private JButton okBtn;
    private JButton cancelBtn;
    private JButton nextBtn;
    private JButton prevBtn;
    private JSpinner spinner;
    private GregorianCalendar gc;
    private String value;

    public PeriodSelectDialog() {
        initComponents();
        connect();
    }

    public String getValue() {
        return value;
    }

    private void initComponents() {

        setResizable(false);
        gc = new GregorianCalendar();

        ClientContext.setDolphinIcon(this);

        setTitle("期間入力");
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        setContentPane(panel);

        table = new JTable();
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(Color.LIGHT_GRAY);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(false);
        createTableModel();

        table.setDefaultRenderer(Object.class, new PeriodTableRenderer());
        panel.add(table);

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
        JLabel lbl = new JLabel("数量");
        spinner = new JSpinner();
        SpinnerNumberModel snm = new SpinnerNumberModel(1, 1, 10, 1);
        spinner.setModel(snm);
        prevBtn = new JButton("<");
        nextBtn = new JButton(">");
        cancelBtn = new JButton("取消");
        okBtn = new JButton("確定");
        btnPanel.add(lbl);
        btnPanel.add(spinner);
        btnPanel.add(Box.createHorizontalStrut(10));
        btnPanel.add(prevBtn);
        btnPanel.add(nextBtn);
        btnPanel.add(Box.createHorizontalGlue());
        btnPanel.add(cancelBtn);
        btnPanel.add(okBtn);
        panel.add(btnPanel);

        setModal(true);
    }

    // わけわかんねーｗ　'*1/1-3,5,7'のようなフォーマットにする。
    private void construct() {

        StringBuilder sb = new StringBuilder();
        sb.append("*");
        sb.append(String.valueOf(spinner.getValue()));
        sb.append("/");

        int[] columns = table.getSelectedColumns();
        int month = gc.get(GregorianCalendar.MONTH);
        List<Integer> list = new ArrayList<Integer>();

        // 月末を超えるものは無視する
        for (int col : columns) {
            GregorianCalendar tmp = (GregorianCalendar) table.getModel().getValueAt(0, col);
            if (month == tmp.get(GregorianCalendar.MONTH)) {
                int day = tmp.get(GregorianCalendar.DAY_OF_MONTH);
                list.add(day);
            }
            
        }

        int prevValue = -1;
        boolean series = false;

        for (int i = 0; i < list.size(); ++i) {

            int day = list.get(i);

            if (prevValue == -1) {
                sb.append(String.valueOf(day));

            } else {
                if (day == prevValue + 1) {
                    if (i == list.size() - 1) {
                        sb.append("-");
                        sb.append(String.valueOf(day));
                    }
                    series = true;
                } else {
                    if (series) {
                        sb.append("-");
                        sb.append(String.valueOf(prevValue));
                    }
                    sb.append(",");
                    sb.append(String.valueOf(day));
                    series = false;
                }
            }

            prevValue = day;
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

        prevBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                gc.add(GregorianCalendar.WEEK_OF_YEAR, -1);
                createTableModel();

            }
        });

        nextBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                gc.add(GregorianCalendar.WEEK_OF_YEAR, 1);
                createTableModel();
            }
        });
    }

    private void createTableModel() {

        final int len = COLUMN_COUNT;
        Object[] header = new Object[len];
        GregorianCalendar[][] obj = new GregorianCalendar[1][len];
        
        for (int i = 0; i < len; ++i) {
            GregorianCalendar tmp = new GregorianCalendar();
            tmp.setTime(gc.getTime());
            tmp.add(GregorianCalendar.DATE, i);
            obj[0][i] = tmp;
        }

        tableModel = new DefaultTableModel(obj, header);
        table.setModel(tableModel);
        table.setRowHeight(40);
        for (int i = 0; i < len; ++i) {
            table.getColumnModel().getColumn(i).setPreferredWidth(30);
        }
    }
    

    private class PeriodTableRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            this.setHorizontalAlignment(JLabel.CENTER);
            GregorianCalendar tmp = (GregorianCalendar) table.getModel().getValueAt(row, column);
            int dayOfWeek = tmp.get(GregorianCalendar.DAY_OF_WEEK);
            int date = tmp.get(GregorianCalendar.DAY_OF_MONTH);

            switch (dayOfWeek) {
                case GregorianCalendar.SUNDAY:
                    setForeground(Color.RED);
                    break;
                case GregorianCalendar.SATURDAY:
                    setForeground(Color.BLUE);
                    break;
                default:
                    setForeground(Color.BLACK);
                    break;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            sb.append(String.valueOf(date));
            sb.append("<br>");
            sb.append(getDayOfWeekStr(dayOfWeek));
            sb.append("</html>");
            setText(sb.toString());

            return this;
        }

        private String getDayOfWeekStr(int dayOfWeek) {

            switch (dayOfWeek) {
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
                case GregorianCalendar.SATURDAY:
                    return "土";
            }
            return null;
        }
    }
}
