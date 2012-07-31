
package open.dolphin.client;

import java.awt.*;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.text.*;

/**
 * KartePanelの抽象クラス
 * 改行文字を表示するEditorKitもここにある
 *
 * @author masuda, Masuda Naika
 */
public abstract class KartePanel extends Panel2 {//implements Scrollable {

    public static enum MODE {SINGLE_VIEWER, DOUBLE_VIEWER, SINGLE_EDITOR, DOUBLE_EDITOR};

    // タイムスタンプの foreground カラー
    private static final Color TIMESTAMP_FORE = Color.BLUE;
    // タイムスタンプのフォントサイズ
    private static final int TIMESTAMP_FONT_SIZE = 14;
    // タイムスタンプフォント
    private static final Font TIMESTAMP_FONT = new Font("Dialog", Font.PLAIN, TIMESTAMP_FONT_SIZE);
    private static final int tsHgap = 0;
    private static final int tsVgap = 3;
    // TextPaneの余白
    private static final int vMargin = 5;
    private static final int hMargin = 5;
    private static final Insets TEXT_PANE_MARGIN = new Insets(vMargin, hMargin, vMargin, hMargin);

    //private static final Dimension INITIAL_SIZE = new Dimension(1, 1);

    protected static final int hgap = 2;
    protected static final int vgap = 0;
    protected static final int rows = 1;
    protected static final int cols = 2;

    private JPanel timeStampPanel;
    private JPanel contentPanel;
    private JLabel timeStampLabel;

    // ファクトリー
    public static KartePanel createKartePanel(MODE mode, boolean verticalLayout) {

        switch (mode) {
            case SINGLE_VIEWER:
                return new KartePanel1(false);
            case DOUBLE_VIEWER:
                if (verticalLayout) {
                    return new KartePanel2V(false);
                } else {
                    return new KartePanel2(false);
                }
            case SINGLE_EDITOR:
                return new KartePanel1(true);
            case DOUBLE_EDITOR:
                return new KartePanel2(true);
        }
        return null;
    }

    // 抽象メソッド
    protected abstract void initComponents(boolean editor);

    public abstract JTextPane getPTextPane();

    public abstract JTextPane getSoaTextPane();
    
    public abstract boolean isSinglePane();

    public final JLabel getTimeStampLabel() {
        return timeStampLabel;
    }

    protected KartePanel() {
        initCommonComponents();
    }

    private void initCommonComponents() {

        //setPreferredSize(INITIAL_SIZE); // KartePanelが広がりすぎないように
        timeStampLabel = new JLabel();
        timeStampPanel = new JPanel();
        timeStampPanel.setLayout(new FlowLayout(FlowLayout.CENTER, tsHgap, tsVgap));
        timeStampLabel.setForeground(TIMESTAMP_FORE);
        timeStampLabel.setFont(TIMESTAMP_FONT);
        timeStampPanel.add(timeStampLabel);
        timeStampPanel.setOpaque(true);
        setLayout(new BorderLayout());
        add(timeStampPanel, BorderLayout.NORTH);
        contentPanel = new JPanel();
        add(contentPanel, BorderLayout.CENTER);
    }

    // 継承クラスから呼ばれる
    protected final JTextPane createTextPane(boolean editor) {
        JTextPane textPane = new JTextPane();
        textPane.setMargin(TEXT_PANE_MARGIN);
        textPane.setEditorKit(new KartePanelEditorKit(editor));
        return textPane;
    }

    protected final int getContainerWidth() {

        Container grandParent = getParent().getParent();
        int width = grandParent instanceof JViewport
                ? grandParent.getWidth()
                : getParent().getWidth();

        return width;
    }

    protected final JPanel getTimeStampPanel() {
        return timeStampPanel;
    }

    protected final JPanel getContentPanel() {
        return contentPanel;
    }

    /**
     * 改行文字を表示するEditorKit
     * @author masuda, Masuda Naika
     * http://terai.xrea.jp/Swing/ParagraphMark.html
     * http://abebas.sub.jp/java/JavaPrograming/01_Editor/011.html
     */
    private static final class KartePanelEditorKit extends StyledEditorKit {

        private static final Color COLOR = Color.GRAY;
        private static final String CR = "↲";
        private static final String EOF = "◀";
        private static final int crMargin = 20;
        private boolean showCr;

        public KartePanelEditorKit(boolean showCr) {
            this.showCr = showCr;
        }

        @Override
        public ViewFactory getViewFactory() {
            if (showCr) {
                return new VisibleCrViewFactory();
            } else {
                return new InvisibleCrViewFactory();
            }
        }

        private static final class VisibleCrViewFactory implements ViewFactory {

            @Override
            public View create(Element elem) {
                String kind = elem.getName();
                if (kind != null) {
                    if (kind.equals(AbstractDocument.ContentElementName)) {
                        return new LabelView(elem);
                    } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                        return new MyParagraphView(elem);
                    } else if (kind.equals(AbstractDocument.SectionElementName)) {
                        return new BoxView(elem, View.Y_AXIS);
                    } else if (kind.equals(StyleConstants.ComponentElementName)) {
                        return new MyComponentView(elem);
                    } else if (kind.equals(StyleConstants.IconElementName)) {
                        return new IconView(elem);
                    }
                }
                return new LabelView(elem);
            }
        }

       private static final class InvisibleCrViewFactory implements ViewFactory {

            @Override
            public View create(Element elem) {
                String kind = elem.getName();
                if (kind != null) {
                    if (kind.equals(AbstractDocument.ContentElementName)) {
                        return new LabelView(elem);
                    } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                        return new ParagraphView(elem);
                    } else if (kind.equals(AbstractDocument.SectionElementName)) {
                        return new BoxView(elem, View.Y_AXIS);
                    } else if (kind.equals(StyleConstants.ComponentElementName)) {
                        return new MyComponentView(elem);
                    } else if (kind.equals(StyleConstants.IconElementName)) {
                        return new IconView(elem);
                    }
                }
                return new LabelView(elem);
            }
        }

        private static final class MyComponentView extends ComponentView {

            public MyComponentView(Element elem) {
                super(elem);
            }


            // KartePane幅より広いスタンプの場合に直後の改行文字がwrapされないように
            // 厳密には正しくない
            @Override
            public float getPreferredSpan(int axis) {
                if (axis == View.X_AXIS && getComponent() instanceof StampHolder) {
                    return getStampHolderSpanX();
                    //return 0;
                }
                return super.getPreferredSpan(axis);
            }

            @Override
            public float getMaximumSpan(int axis) {

                if (axis == View.X_AXIS && getComponent() instanceof StampHolder) {
                    return getStampHolderSpanX();
                }
                return super.getMaximumSpan(axis);
            }

            private float getStampHolderSpanX() {

                float span = super.getPreferredSpan(View.X_AXIS);
                int width = getComponent().getParent().getParent().getWidth() - crMargin;
                if (span > width && width > 0) {
                    return width;
                }
                return span;
            }

        }

        private static final class MyParagraphView extends ParagraphView {

            public MyParagraphView(Element elem) {
                super(elem);
            }

            @Override
            public void paint(Graphics g, Shape a) {
                super.paint(g, a);
                try {
                    Shape paragraph = modelToView(getEndOffset(), a, Position.Bias.Backward);
                    Rectangle r = (paragraph == null) ? a.getBounds() : paragraph.getBounds();
                    int fontHeight = g.getFontMetrics().getHeight();
                    Color old = g.getColor();
                    g.setColor(COLOR);
                    if (getEndOffset() != getDocument().getEndPosition().getOffset()) {
                        g.drawString(CR, r.x + 1, r.y + (r.height + fontHeight) / 2 - 2);
                    } else {
                        g.drawString(EOF, r.x + 1, r.y + (r.height + fontHeight) / 2 - 2);
                    }
                    g.setColor(old);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }

    }
}
