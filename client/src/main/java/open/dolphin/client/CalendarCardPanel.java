package open.dolphin.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import javax.swing.*;
import open.dolphin.infomodel.SimpleDate;

/**
 * CalendarCardPanel
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika 元町皮ふ科様の「最近３ヶ月の来院歴表示」を取り入れ
 */
public class CalendarCardPanel extends JPanel  {

    public static final String PICKED_DATE = "pickedDate";

    private JLabel titleLabel;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    private static final ImageIcon leftIcon = ClientContext.getImageIconAlias("icon_calendar_left");
    private static final ImageIcon downIcon = ClientContext.getImageIconAlias("icon_calendar_down");
    private static final ImageIcon rightIcon = ClientContext.getImageIconAlias("icon_calendar_right");
    private static final ImageIcon upIcon = ClientContext.getImageIconAlias("icon_calendar_up");
    private JButton upBtn = new JButton(upIcon);
    private JButton leftBtn = new JButton(leftIcon);
    private JButton downBtn = new JButton(downIcon);
    private JButton rightBtn = new JButton(rightIcon);

    private int current;
    private int[] range;

    private HashMap<Integer, LiteCalendarPanel> calendars = new HashMap<Integer, LiteCalendarPanel>(12,1.0f);
    private HashMap<Integer, LiteCalendarPanel> popupCalendars = new HashMap<Integer, LiteCalendarPanel>();
    private HashMap<String, Color> colorTable;
    private List<SimpleDate> markList;

    private PropertyChangeSupport boundSupport; // = new PropertyChangeSupport(this);
    private PropertyChangeListener calendarListener;

    private static final int TITLE_ALIGN = SwingConstants.CENTER;
    private static final int TITLE_FONT_SIZE = 14;
    private static final Font TITLE_FONT = new Font("Dialog", Font.PLAIN, TITLE_FONT_SIZE);

    private static final Color CALENDAR_BACK = ClientContext.getColor("color.CALENDAR_BACK");
    private static final Color titleFore = ClientContext.getColor("color.calendar.title.fore");
    private static final Color titleBack = ClientContext.getColor("color.calendar.title.back");

    private int titleAlign = TITLE_ALIGN;
    private Font titleFont = TITLE_FONT;

    /**
     * CalendarCardPanelを生成する。
     *
     * @param colorTable カラーテーブル
     */
    public CalendarCardPanel(HashMap<String, Color> colorTable) {

        cardPanel = new JPanel();
        cardLayout = new CardLayout();

        this.colorTable = colorTable;
        calendarListener = new CalendarListener(this);
        current = 0;

        LiteCalendarPanel lc = new LiteCalendarPanel(current, false);
        lc.addPropertyChangeListener(LiteCalendarPanel.SELECTED_DATE_PROP, calendarListener);
        lc.setEventColorTable(colorTable);
        SimpleDate today = new SimpleDate(new GregorianCalendar());
        lc.setToday(today);
        String name = String.valueOf(current);

        calendars.put(current, lc);

        cardPanel.setLayout(cardLayout);
        cardPanel.add(lc, name);

        JButton[] buttons = {leftBtn, downBtn, rightBtn, upBtn};
        for (JButton btn : buttons) {
            btn.setPreferredSize(new Dimension(15, 15));
            btn.setBackground(null);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
        }

        leftBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                current -= 1;
                controlNavigation();
                showCalendar();
            }
        });

        downBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                current = 0;
                controlNavigation();
                showCalendar();
            }
        });

        rightBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                current+=1;
                controlNavigation();
                showCalendar();
            }
        });

        titleLabel = new JLabel();
        titleLabel.setHorizontalAlignment(titleAlign);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(titleFore);
        titleLabel.setBackground(titleBack);
        titleLabel.setOpaque(true);

        updateTitle(lc, titleLabel);
        JPanel cmdPanel = new JPanel();
        cmdPanel.setOpaque(true);
        cmdPanel.setBackground(titleBack);
        JPanel btnPanel = createCommandPanel();
        cmdPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        // ３か月表示
        upBtn.setVisible(false);
        upBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                JPopupMenu popup = new JPopupMenu();
                popup.setBackground(CALENDAR_BACK);
                popup.addMouseWheelListener(new MyMouseAdapter2(popup));
                popup.add(getPopupCalendarPanel(current - 2));
                popup.add(getPopupCalendarPanel(current - 1));
                popup.add(getPopupCalendarPanel(current));
                int y = popup.getPreferredSize().height / 2;
                popup.show(cardPanel, 0, -y);
            }
        });
        cmdPanel.add(upBtn);
        cmdPanel.add(Box.createHorizontalStrut(btnPanel.getPreferredSize().width - upBtn.getPreferredSize().width));
        cmdPanel.add(titleLabel);
        cmdPanel.add(btnPanel);

        MyMouseAdapter adapter = new MyMouseAdapter();
        this.addMouseWheelListener(adapter);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(cmdPanel);
        this.add(cardPanel);

        // 高さを固定 EDTでないとダメ？
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                int h = getPreferredSize().height;
                fixHeight(CalendarCardPanel.this, h);
            }
        });
        this.putClientProperty(GUIConst.PROP_FIXED_HEIGHT, true);
        
        boundSupport = new PropertyChangeSupport(this);
    }
    
    private void fixHeight(JPanel panel, int height) {
        panel.setPreferredSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setMinimumSize(new Dimension(0, height));
    }

    private void updateTitle(LiteCalendarPanel lc, JLabel label) {
        StringBuilder buf = new StringBuilder();
        buf.append(lc.getYear());
        buf.append(ClientContext.getString("calendar.title.year"));
        buf.append(lc.getMonth() + 1);
        buf.append(ClientContext.getString("calendar.title.month"));
        label.setText(buf.toString());
    }

    @Override
    public void addPropertyChangeListener(String prop, PropertyChangeListener l) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(prop, l);
    }

    @Override
    public void removePropertyChangeListener(String prop, PropertyChangeListener l) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.removePropertyChangeListener(prop, l);
    }

    public void notifyPickedDate(SimpleDate picked) {
        boundSupport.firePropertyChange(PICKED_DATE, null, picked);
    }

    public int[] getRange() {
        return range;
    }

    public void setCalendarRange(int[] range) {
        this.range = range;
        controlNavigation();
    }

    public void setMarkList(List<SimpleDate> newMark) {

        if (markList != newMark) {
            markList = newMark;
        }
//masuda^
        //LiteCalendarPanel lc = (LiteCalendarPanel)calendars.get(String.valueOf(current));
        LiteCalendarPanel lc = calendars.get(current);
//masuda$
        lc.getTableModel().setMarkDates(markList);
    }

    private void controlNavigation() {
        if (range != null) {
            if (current == range[0]) {
                if (leftBtn.isEnabled()) {
                    leftBtn.setEnabled(false);
                }
                if (! rightBtn.isEnabled()) {
                    rightBtn.setEnabled(true);
                }
            } else if (current == range[1]) {
                if (rightBtn.isEnabled()) {
                    rightBtn.setEnabled(false);
                }
                if (! leftBtn.isEnabled()) {
                    leftBtn.setEnabled(true);
                }
            } else {
                if (! leftBtn.isEnabled()) {
                    leftBtn.setEnabled(true);
                }
                if (! rightBtn.isEnabled()) {
                    rightBtn.setEnabled(true);
                }
            }
        }
    }

    private void showCalendar() {

        String key = String.valueOf(current);
//masuda^
        //LiteCalendarPanel lc = (LiteCalendarPanel)calendars.get(key);
        LiteCalendarPanel lc = calendars.get(current);
//masuda$
        if (lc == null) {
            lc = new LiteCalendarPanel(current, false);
            lc.addPropertyChangeListener(LiteCalendarPanel.SELECTED_DATE_PROP, calendarListener);
            lc.setEventColorTable(colorTable);
            lc.getTableModel().setMarkDates(markList);
            //calendars.put(key, lc);
            cardPanel.add(lc, key);
            calendars.put(current, lc);
        } else {
            lc.getTableModel().setMarkDates(markList);
        }

        updateTitle(lc, titleLabel);
        cardLayout.show(cardPanel, key);
    }

    private JPanel createCommandPanel() {

//masuda^
        JPanel cmd = new JPanel(new java.awt.FlowLayout(FlowLayout.CENTER, 0, 0));
        cmd.add(Box.createHorizontalStrut(10));
        cmd.setOpaque(false);
//masuda$
        cmd.add(leftBtn);
        cmd.add(downBtn);
        cmd.add(rightBtn);
        return cmd;
    }

//masuda^
    private LiteCalendarPanel getPopupCalendarPanel(int n) {

        LiteCalendarPanel lc = popupCalendars.get(n);

        if (lc == null) {
            lc = new LiteCalendarPanel(n, true);
            lc.addPropertyChangeListener(LiteCalendarPanel.SELECTED_DATE_PROP, calendarListener);
            lc.setEventColorTable(colorTable);
            lc.getTableModel().setMarkDates(markList);
            //６週分の一部だけ表示
            Dimension d = lc.getPopupCalendarSize();
            lc.setPreferredSize(d);
            lc.setMaximumSize(d);
            lc.setMinimumSize(d);
            // 今月はtoday設定
            if (n == 0) {
                SimpleDate today = new SimpleDate(new GregorianCalendar());
                lc.setToday(today);
            }
            popupCalendars.put(n, lc);
        }

        return lc;
    }

    public void setUpBtnVisible(boolean b){
        upBtn.setVisible(b);
    }

    private class MyMouseAdapter2 extends MouseAdapter {

        private JPopupMenu popup;
        private int currentPos;

        private MyMouseAdapter2(JPopupMenu popup) {
            this.popup = popup;
            currentPos = current;
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int count = e.getWheelRotation();
            if (count > 0) {
                currentPos++;
              } else {
                currentPos--;
            }
            popup.removeAll();
            popup.add(getPopupCalendarPanel(currentPos - 2));
            popup.add(getPopupCalendarPanel(currentPos - 1));
            popup.add(getPopupCalendarPanel(currentPos));
            popup.revalidate();
            popup.repaint();
            e.consume();
        }
    }

    private class MyMouseAdapter extends MouseAdapter {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int count = e.getWheelRotation();
            if (count > 0) {
                current += 1;
                controlNavigation();
                showCalendar();
            } else {
                current -= 1;
                controlNavigation();
                showCalendar();
            }
        }
    }
//masuda$

//masuda class -> private static class
    private static class CalendarListener implements PropertyChangeListener {

        private CalendarCardPanel owner;

        public CalendarListener(CalendarCardPanel owner) {
            this.owner = owner;
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals(LiteCalendarPanel.SELECTED_DATE_PROP)) {
                SimpleDate sd = (SimpleDate)e.getNewValue();
                owner.notifyPickedDate(sd);
            }
        }
    }
}
