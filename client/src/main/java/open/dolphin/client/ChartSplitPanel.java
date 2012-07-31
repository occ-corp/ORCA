package open.dolphin.client;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import open.dolphin.project.Project;

/**
 * ChartSplitPanel
 *
 * @author masuda, Masuda Naika
 */
public class ChartSplitPanel extends JPanel {

    private static final Dimension DEFAULT_SIZE = new Dimension(100, 100);
    private static final int SP_WIDTH = 7;
    private final Border SELECTED_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
    private final Border EMPTY_BORDER = BorderFactory.createLineBorder(new Color(0, 0, 0, 0));
    private final Cursor UD_CURSOR = new Cursor(Cursor.N_RESIZE_CURSOR);
    private final Cursor LR_CURSOR = new Cursor(Cursor.E_RESIZE_CURSOR);
    private final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private final Color COLOR_MARK = Color.GRAY;
    private final String DEFAULT_LEFT = "278,1098";
    private final String DEFAULT_INSPECTORS = "260,45,260,375,260,184,260,390,260,100";
    private final String DEFAULT_RIGHT = "689,1098";
    private Chart context;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel splitPanel;
    private Rectangle spRect;

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (spRect != null) {
            g.drawRect(spRect.x, spRect.y, spRect.width, spRect.height);
        }
    }

    public void setContext(Chart chart) {
        context= chart;
    }

    public void setLeftPanel(JPanel panel) {
        leftPanel = panel;
    }

    public void setRightPanel(JPanel panel) {
        rightPanel = panel;
    }

    public void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setAlignmentY(TOP_ALIGNMENT);
        splitPanel = new SplitterPanel();
        add(leftPanel);
        add(splitPanel);
        add(rightPanel);
        loadSize();
    }

    public void saveSize() {

        // EDTからする必要あり
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Component[] comps = leftPanel.getComponents();

                // left Panel
                String data = removeLastComma(getSizeString(leftPanel));
                Project.setString("chartPanelLeftSize", data);

                // left inspector panels
                StringBuilder sb = new StringBuilder();
                for (Component comp : comps) {
                    sb.append(getSizeString(comp));
                }
                data = removeLastComma(sb.toString());
                Project.setString("chartInspectorsSize", data);

                // right Panel
                data = removeLastComma(getSizeString(rightPanel));
                Project.setString("chartPanelRightSize", data);
            }
        });
    }

    private String getSizeString(Component c) {

        StringBuilder sb = new StringBuilder();
        Rectangle r = c.getBounds();
        sb.append(String.valueOf(r.width)).append(",");
        sb.append(String.valueOf(r.height)).append(",");
        return sb.toString();
    }

    private String removeLastComma(String str) {

        if (str.endsWith(",")) {
            int len = str.length();
            return str.substring(0, len - 1);
        }
        return str;
    }

    public void loadSize() {

        // EDTでなくてよい？
        String str = Project.getString("chartPanelLeftSize", DEFAULT_LEFT);
        List<Dimension> list = getDimensionList(str);
        Dimension d = list.get(0);
        leftPanel.setPreferredSize(d);
        leftPanel.setMaximumSize(new Dimension(d.width, Integer.MAX_VALUE));
        leftPanel.setMinimumSize(new Dimension(d.width, 0));

        str = Project.getString("chartInspectorsSize", DEFAULT_INSPECTORS);
        list = getDimensionList(str);
        // 記録されているものとサイズが違えばデフォルトを使用する。
        if (list.size() != DEFAULT_INSPECTORS.length()) {
            list = getDimensionList(DEFAULT_INSPECTORS);
        }
        Component[] comps = leftPanel.getComponents();
        for (int i = 0; i < comps.length; ++i) {
            d = (i < list.size()) ? list.get(i) : DEFAULT_SIZE;
            JComponent comp = (JComponent) comps[i];
            Boolean fixedHeight = (Boolean) comp.getClientProperty("fixedHeight");
            if (fixedHeight == null || !fixedHeight) {
                comp.setPreferredSize(d);
            }
        }

        str = Project.getString("chartPanelRightSize", DEFAULT_RIGHT);
        list = getDimensionList(str);
        rightPanel.setPreferredSize(list.get(0));

    }

    private List<Dimension> getDimensionList(String str) {

        String[] sizes = str.split(",");
        List<Dimension> ret = new ArrayList<Dimension>();
        for (int i = 0; i < sizes.length; i += 2) {
            Dimension d = new Dimension();
            d.width = Integer.valueOf(sizes[i]);
            d.height = Integer.valueOf(sizes[i + 1]);
            ret.add(d);
        }
        return ret;
    }

    // パネル区切りのパネル
    private class SplitterPanel extends JPanel implements MouseListener, MouseMotionListener, ComponentListener {

        private boolean selected;
        private List<Mark> markList;
        private Mark selectedMark;

        private SplitterPanel() {
            markList = new ArrayList<Mark>();
            setBorder(EMPTY_BORDER);
            addMouseListener(SplitterPanel.this);
            addMouseMotionListener(SplitterPanel.this);

            setPreferredSize(new Dimension(SP_WIDTH, 0));
            setMinimumSize(new Dimension(SP_WIDTH, 0));
            setMaximumSize(new Dimension(SP_WIDTH, Integer.MAX_VALUE));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Color c = g.getColor();
            g.setColor(COLOR_MARK);
            for (Mark mark : markList) {
                Polygon p = mark.getPolygon();
                if (p != null) {
                    g.fillPolygon(p);
                }
            }
            int x = SP_WIDTH / 2 + 1;
            int y = getHeight() / 2;
            g.fillRect(x - 2, y - 3, 3, 6);

            g.setColor(c);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            setSelected(!selected);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            Point p = e.getPoint();
            Mark mark = getSelectedMark(p);
            if (mark != null) {
                selectedMark = mark;
                setCursor(UD_CURSOR);
            } else if (selected) {
                setCursor(LR_CURSOR);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {

            if (selectedMark != null) {
                layoutLeftPanel();
                selectedMark = null;
                saveSize();
            } else if (spRect != null) {
                spRect = null;
                Point p = e.getPoint();
                Dimension d = leftPanel.getSize();
                int leftWidth = p.x + getBounds().x - 1;
                d.width = leftWidth;
                leftPanel.setPreferredSize(d);
                d = rightPanel.getSize();
                int rightWidth = ChartSplitPanel.this.getWidth() - leftWidth - SP_WIDTH;
                d.width = rightWidth;
                rightPanel.setPreferredSize(d);
                revalidate();
                saveSize();
            }
            setCursor(DEFAULT_CURSOR);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedMark != null) {
                Point p = e.getPoint();
                selectedMark.setPoint(p);
                repaint();
            } else {
                Rectangle r = getBounds();
                r.x += e.getX();
                spRect = r;
                ChartSplitPanel.this.repaint();

            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        private void renewMark() {
            markList.clear();
            Component[] comps = leftPanel.getComponents();
            if (comps.length == 0) {
                return;
            }
            for (int i = 1; i < comps.length; ++i) {
                Component comp = comps[i];
                int y = comp.getY();
                Mark mark = new Mark(new Point(0, y));
                markList.add(mark);
            }
        }

        private void setSelected(boolean b) {

            if (b) {
                setBorder(SELECTED_BORDER);
                if (context != null) {
                    context.getFrame().addComponentListener(SplitterPanel.this);
                }
                renewMark();
            } else {
                setBorder(EMPTY_BORDER);
                if (context != null) {
                    context.getFrame().removeComponentListener(SplitterPanel.this);
                }
                markList.clear();
            }
            selected = b;
        }

        private Mark getSelectedMark(Point p) {
            for (Mark mark : markList) {
                if (mark.getPolygon().contains(p)) {
                    return mark;
                }
            }
            return null;
        }

        private void layoutLeftPanel() {

            List<Integer> markY = new ArrayList<Integer>();
            for (Mark mark : markList) {
                int y = mark.getPoint().y;
                markY.add(y);
            }
            Collections.sort(markY);
            Insets insets = leftPanel.getInsets();
            markY.add(0, insets.top);
            markY.add(leftPanel.getSize().height - 1 - insets.bottom);

            Component[] comps = leftPanel.getComponents();
            for (int i = 0; i < comps.length; ++i) {
                int currY = markY.get(i);
                int nextY = markY.get(i + 1);
                JComponent comp = (JComponent) comps[i];
                Boolean fixedHeight = (Boolean) comp.getClientProperty("fixedHeight");
                if (fixedHeight != null && fixedHeight) {
                    nextY = currY + comp.getSize().height;
                    markY.set(i + 1, nextY);
                }
                Dimension d = comp.getSize();
                d.height = nextY - currY;
                comp.setPreferredSize(d);
            }

            leftPanel.revalidate();

            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    renewMark();
                    splitPanel.repaint();
                }
            });
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (selected) {
                renewMark();
                repaint();
            }
        }

        @Override
        public void componentMoved(ComponentEvent e) {
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }
    }

    private class Mark {

        private Point p;
        private Polygon polygon;

        private Mark(Point p) {
            setPoint(p);
        }

        private Point getPoint() {
            return p;
        }

        private void setPoint(Point p) {
            this.p = p;
            p.x = 0;
            p.y = Math.min(p.y, getHeight() - SP_WIDTH);
            p.y = Math.max(p.y, 0);
            polygon = createMark(p);
        }

        private Polygon getPolygon() {
            return polygon;
        }

        private Polygon createMark(Point p) {
            Polygon pol = new Polygon();
            pol.addPoint(p.x, p.y);
            pol.addPoint(p.x + SP_WIDTH - 1, p.y - SP_WIDTH / 2);
            pol.addPoint(p.x + SP_WIDTH - 1, p.y + SP_WIDTH / 2);
            return pol;
        }
    }
}
