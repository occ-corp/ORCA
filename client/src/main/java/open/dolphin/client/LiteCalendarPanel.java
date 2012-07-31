package open.dolphin.client;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import open.dolphin.infomodel.SimpleDate;
import open.dolphin.util.Holiday;

/**
 * LiteCalendarPanel
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public class LiteCalendarPanel extends JPanel implements PropertyChangeListener {

    //private static final long serialVersionUID = -3472737594106311587L;

    public static final String SELECTED_DATE_PROP = "selectedDateProp";
    public static final String MARK_LIST_PROP = "markListProp";

    // 表示のデフォルト設定
    private static final int TITLE_ALIGN = SwingConstants.CENTER;
    private static final int TITLE_FONT_SIZE = 14;
    private static final Font TITLE_FONT = new Font("Dialog", Font.PLAIN, TITLE_FONT_SIZE);
    private static final Font CALENDAR_FONT = new Font("Dialog", Font.PLAIN, ClientContext.getInt("calendar.font.size"));
    private static final Font OUTOF_MONTH_FONT = new Font("Dialog", Font.PLAIN, ClientContext.getInt("calendar.font.size.outOfMonth"));

    // カレンダテーブル
    private int relativeMonth;
    private int year;
    private int month;
    private CalendarTableModel tableModel;
    private CalendarTable table;
    private PropertyChangeSupport boundSupport;
    private Object selectedDate;
    private JLabel titleLabel;
    private SimpleDate today;

    private HashMap<String, Color> eventColorTable;

    // 表示用の属性
    private Color titleFore = ClientContext.getColor("color.calendar.title.fore");
    private Color titleBack = ClientContext.getColor("color.calendar.title.back");
    private int titleAlign = TITLE_ALIGN;
    private Font titleFont = TITLE_FONT;
    private int cellWidth = ClientContext.getInt("calendar.cell.width");
    private int cellHeight = ClientContext.getInt("calendar.cell.height");
    private int autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS;
    private boolean cellSelectionEnabled = true;

    private Color sundayFore = ClientContext.getColor("color.SUNDAY_FORE");
    private Color saturdayFore = ClientContext.getColor("color.SATURDAY_FORE");
    private Color weekdayFore = ClientContext.getColor("color.WEEKDAY_FORE");
    private Color outOfMothFore = ClientContext.getColor("color.OUTOFMONTH_FORE");
    private Color calendarBack = ClientContext.getColor("color.CALENDAR_BACK");
    private Color todayBack = ClientContext.getColor("color.TODAY_BACK");
    private Color birthdayBack = ClientContext.getColor("color.BIRTHDAY_BACK");

    private Font calendarFont = CALENDAR_FONT;
    private Font outOfMonthFont = OUTOF_MONTH_FONT;

    public LiteCalendarPanel() {
        super();
    }

    public LiteCalendarPanel(int n) {
        this(n, true);
    }

    public LiteCalendarPanel(int n, boolean addTitle) {

        // 作成するカレンダの当月を起点とする相対月数（n ケ月前/後)
        relativeMonth = n;
        GregorianCalendar gc = new GregorianCalendar();
        gc.clear(Calendar.MILLISECOND);
        gc.clear(Calendar.SECOND);
        gc.clear(Calendar.MINUTE);
        gc.clear(Calendar.HOUR_OF_DAY);
        gc.add(Calendar.MONTH, relativeMonth);
        year = gc.get(Calendar.YEAR);
        month = gc.get(Calendar.MONTH);

        tableModel = new CalendarTableModel(year, month);
        table = new CalendarTable(tableModel);
        setAutoResizeMode(autoResizeMode);
        table.setBackground(calendarBack);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(cellSelectionEnabled);

        setCellWidth(cellWidth);
        setCellHeight(cellHeight);

        // Replace DefaultRender
        DateRenderer dateRenderer = new DateRenderer();
        dateRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(java.lang.Object.class, dateRenderer);
//pns^
        dateRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(250,250,250));
        table.setRowSelectionAllowed(false);
//pns$
//masuda^
        // ヘッダー　センタリング
        //((JLabel)table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        // 土日色分けのヘッダレンダラ thx Dr. pns
        table.getTableHeader().setDefaultRenderer(new CalendarHeaderRenderer());
        // カラムのドラッグ・リサイズを不許可
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
//masuda$

        // MouseAdapter
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() != 1) {
                    return;
                }

                Point p = e.getPoint();
                int row = table.rowAtPoint(p);
                int col = table.columnAtPoint(p);
                if (row != -1 && col != -1) {
                    Object o = tableModel.getDate(row, col);
                    setSelectedDate(o);
                }
            }
        });

        StringBuilder buf = new StringBuilder();
        buf.append(year);
        buf.append(ClientContext.getString("calendar.title.year"));
        buf.append(month + 1);
        buf.append(ClientContext.getString("calendar.title.month"));
        setTitleLabel(new JLabel(buf.toString()));
        setTitleAlign(titleAlign);
        setTitleFont(titleFont);
        setTitleFore(titleFore);
        setTitleBack(titleBack);
        getTitleLabel().setOpaque(true);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (addTitle) {
//masuda^
            //this.add(getTitleLabel());
            JPanel panel = new JPanel(new java.awt.FlowLayout(FlowLayout.CENTER, 0, 0));
            panel.setBackground(titleBack);
            panel.setOpaque(true);
            panel.add(getTitleLabel());
            this.add(panel);
//masuda$
        }

        this.add(table.getTableHeader());
        this.add(table);
        this.setBorder(BorderFactory.createEtchedBorder());

        boundSupport = new PropertyChangeSupport(this);
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    @Override
    public void addPropertyChangeListener(String prop, PropertyChangeListener l) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(prop, l);
    }

    @Override
    public void removePropertyChangeListener(String prop,
            PropertyChangeListener l) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.removePropertyChangeListener(prop, l);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {

        String prop = e.getPropertyName();
        if (prop.equals(MARK_LIST_PROP)) {
            Collection list = (Collection) e.getNewValue();
            tableModel.setMarkDates(list);
        }
    }

    /**
     * 選択された日を通知する。
     */
    public void setSelectedDate(Object o) {
        Object old = selectedDate;
        selectedDate = o;
        if (selectedDate instanceof String) {
            SimpleDate sd = new SimpleDate(getYear(), getMonth(), Integer.parseInt((String) selectedDate));
            selectedDate = sd;
        }
        boundSupport.firePropertyChange(SELECTED_DATE_PROP, old, selectedDate);
    }

    public JTable getTable() {
        return table;
    }

    public CalendarTableModel getTableModel() {
        return tableModel;
    }

    public int getRelativeMonth() {
        return relativeMonth;
    }

    public SimpleDate getFirstDate() {
        return tableModel.getFirstDate();
    }

    public SimpleDate getLastDate() {
        return tableModel.getLastDate();
    }

    public HashMap<String, Color> getEventColorTable() {
        return eventColorTable;
    }

    public void setEventColorTable(HashMap<String, Color> ht) {
        eventColorTable = ht;
    }

    /**
     * @param titleFore
     *            The titleFore to set.
     */
    private void setTitleFore(Color titleFore) {
        this.titleFore = titleFore;
        getTitleLabel().setForeground(titleFore);
    }

    /**
     * @return Returns the titleFore.
     */
    public Color getTitleFore() {
        return titleFore;
    }

    /**
     * @param titleBack
     *            The titleBack to set.
     */
    private void setTitleBack(Color titleBack) {
        this.titleBack = titleBack;
        getTitleLabel().setBackground(titleBack);
    }

    /**
     * @param titleAlign
     *            The titleAlign to set.
     */
    private void setTitleAlign(int titleAlign) {
        this.titleAlign = titleAlign;
        getTitleLabel().setHorizontalAlignment(titleAlign);
    }

    /**
     * @param titleFont
     *            The titleFont to set.
     */
    private void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
        getTitleLabel().setFont(titleFont);
    }

    /**
     * @param cellWidth
     *            The cellWidth to set.
     */
    private void setCellWidth(int cellWidth) {
        this.cellWidth = cellWidth;
        TableColumn column = null;
        for (int i = 0; i < 7; i++) {
            column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(cellWidth);
        }
    }

    /**
     * @return Returns the cellWidth.
     */
    public int getCellWidth() {
        return cellWidth;
    }

    /**
     * @param cellHeight
     *            The cellHeight to set.
     */
    private void setCellHeight(int cellHeight) {
        this.cellHeight = cellHeight;
        table.setRowHeight(cellHeight);
    }

    /**
     * @return Returns the cellHeight.
     */
    public int getCellHeight() {
        return cellHeight;
    }

    /**
     * @param autoResize
     *            The autoResize to set.
     */
    private void setAutoResizeMode(int mode) {
        this.autoResizeMode = mode;
    }

    /**
     * @return Returns the autoResize.
     */
    public int getAutoResizeMode() {
        return autoResizeMode;
    }

    /**
     * @param cellSelectionEnabled
     *            The cellSelectionEnabled to set.
     */
    private void setCellSelectionEnabled(boolean cellSelectionEnabled) {
        this.cellSelectionEnabled = cellSelectionEnabled;
    }

    /**
     * @return Returns the cellSelectionEnabled.
     */
    public boolean isCellSelectionEnabled() {
        return cellSelectionEnabled;
    }

    /**
     * @param sundayFore
     *            The sundayFore to set.
     */
    public void setSundayFore(Color sundayFore) {
        this.sundayFore = sundayFore;
    }

    /**
     * @return Returns the sundayFore.
     */
    public Color getSundayFore() {
        return sundayFore;
    }

    /**
     * @param saturdayFore
     *            The saturdayFore to set.
     */
    public void setSaturdayFore(Color saturdayFore) {
        this.saturdayFore = saturdayFore;
    }

    /**
     * @return Returns the saturdayFore.
     */
    public Color getSaturdayFore() {
        return saturdayFore;
    }

    /**
     * @param weekdayFore
     *            The weekdayFore to set.
     */
    public void setWeekdayFore(Color weekdayFore) {
        this.weekdayFore = weekdayFore;
    }

    /**
     * @return Returns the weekdayFore.
     */
    public Color getWeekdayFore() {
        return weekdayFore;
    }

    /**
     * @param outOfMothFore
     *            The outOfMothFore to set.
     */
    public void setOutOfMothFore(Color outOfMothFore) {
        this.outOfMothFore = outOfMothFore;
    }

    /**
     * @return Returns the outOfMothFore.
     */
    public Color getOutOfMothFore() {
        return outOfMothFore;
    }

    /**
     * @param calendarBack
     *            The calendarBack to set.
     */
    public void setCalendarBack(Color calendarBack) {
        this.calendarBack = calendarBack;
    }

    /**
     * @return Returns the calendarBack.
     */
    public Color getCalendarBack() {
        return calendarBack;
    }

    /**
     * @param todayBack
     *            The todayBack to set.
     */
    public void setTodayBack(Color todayBack) {
        this.todayBack = todayBack;
    }

    /**
     * @return Returns the todayBack.
     */
    public Color getTodayBack() {
        return todayBack;
    }

    /**
     * @param birthdayBack
     *            The birthdayBack to set.
     */
    public void setBirthdayBack(Color birthdayBack) {
        this.birthdayBack = birthdayBack;
    }

    /**
     * @return Returns the birthdayBack.
     */
    public Color getBirthdayBack() {
        return birthdayBack;
    }

    /**
     * @param calendarFont
     *            The calendarFont to set.
     */
    public void setCalendarFont(Font calendarFont) {
        this.calendarFont = calendarFont;
    }

    /**
     * @return Returns the calendarFont.
     */
    public Font getCalendarFont() {
        return calendarFont;
    }

    /**
     * @param outOfMonthFont
     *            The outOfMonthFont to set.
     */
    public void setOutOfMonthFont(Font outOfMonthFont) {
        this.outOfMonthFont = outOfMonthFont;
    }

    /**
     * @return Returns the outOfMonthFont.
     */
    public Font getOutOfMonthFont() {
        return outOfMonthFont;
    }

    public void setToday(SimpleDate today) {
        this.today = today;
    }

    /**
     * @param titleLabel
     *            The titleLabel to set.
     */
    private void setTitleLabel(JLabel titleLabel) {
        this.titleLabel = titleLabel;
    }

    /**
     * @return Returns the titleLabel.
     */
    private JLabel getTitleLabel() {
        return titleLabel;
    }

    /**
     * Custom table cell renderer for the carendar panel.
     */
    protected class DateRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 5817292848730765481L;

        public DateRenderer() {
            super();
            this.setOpaque(true);
            this.setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean isFocused, int row,
                int col) {

            Component compo = super.getTableCellRendererComponent(table, value,
                    isSelected, isFocused, row, col);
            if (compo != null && value != null) {

                // 日を書く
                String day = null;
                Color color = null;

                if (value instanceof SimpleDate) {
                    day = ((SimpleDate) value).toString();
                    if (today != null
                            && today.compareTo((SimpleDate) value) == 0) {
                        // color = todayBack;
                        color = eventColorTable.get("TODAY");
                    } else {
                        color = eventColorTable.get(((SimpleDate) value).getEventCode());
                        // color = Color.black;
                    }

                } else if (value instanceof String) {
                    day = (String) value;
//masuda^   今月のみtoday色分け
                    if (today != null
                            && today.equalDate(year, month, Integer.parseInt(day))
                            && !tableModel.isOutOfMonth(row, col)) {
                    /*
                    if (today != null
                            && today.equalDate(year, month, Integer
                            .parseInt(day))) {
                    */
//masuda$
                        // color = todayBack;
                        color = eventColorTable.get("TODAY");
                    } else {
                        color = getCalendarBack();
                    }
                }

                ((JLabel) compo).setText(day);

                // 曜日によって ForeColor を変える
//masuda^   日曜もisHolidayにまとめた
                /*
                if (col == 0) {
                    this.setForeground(getSundayFore());

                } else if (col == 6) {
                */
                if (col == 6) {
                    this.setForeground(getSaturdayFore());

                } else {
                    this.setForeground(getWeekdayFore());
                }
                // 休日
                SimpleDate sd = tableModel.getDate(row, col);
                if (Holiday.isHoliday(new GregorianCalendar(sd.getYear(), sd.getMonth(), sd.getDay()))) {
                    this.setForeground(getSundayFore());
                }
//masuda$
                // このカレンダ月内の日かどうかでフォントを変える
                if (tableModel.isOutOfMonth(row, col)) {
                    this.setFont(getOutOfMonthFont());
//masuda^
                    //this.setBackground(getCalendarBack());
                    this.setBackground(color);
//masuda$

                } else {
                    this.setFont(getCalendarFont());
                    this.setBackground(color);
                }
            }
            return compo;
        }
    }

//masuda^   ３か月表示のときは月の週数に合わせてカレンダーを表示するため
    public Dimension getPopupCalendarSize() {
        int width = this.getPreferredSize().width;
        int height = getTitleLabel().getPreferredSize().height
                + table.getTableHeader().getPreferredSize().height
                + table.getRowHeight() * tableModel.getRealNumRows();
        return new Dimension(width, height);
    }

    // pns先生のコード、曜日のレンダラ
    private static final class CalendarHeaderRenderer extends DefaultTableCellRenderer {

        private static final Border border = UIManager.getBorder("TableHeader.cellBorder");

        public CalendarHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(t, v, isSelected, hasFocus, row, col);

            setBorder(border);

            switch (col) {
                case 0:
                    setForeground(Color.red);
                    break;
                case 6:
                    setForeground(Color.blue);
                    break;
                default:
                    setForeground(Color.black);
            }

            return this;
        }
    }

    // セルごとにToolTipを変える
    private class CalendarTable extends JTable {

        private CalendarTable(AbstractTableModel model) {
            super(model);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int row = rowAtPoint(event.getPoint());
            int col = columnAtPoint(event.getPoint());
            if (row == -1 || col == -1) {
                return null;
            }
            Object value = getValueAt(row, col);
            if (value instanceof SimpleDate) {
                SimpleDate sd = (SimpleDate) value;
                return sd.getText();
            }
            return null;
        }
    }
//masuda$
}
