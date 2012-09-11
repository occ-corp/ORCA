package open.dolphin.client;

import java.awt.*;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JViewport;

/**
 * KartePanelの抽象クラス
 * 改行文字を表示するEditorKitもここにある
 *
 * @author masuda, Masuda Naika
 */
public abstract class KartePanel extends Panel2 {

    public static enum MODE {SINGLE_VIEWER, DOUBLE_VIEWER, SINGLE_EDITOR, DOUBLE_EDITOR};
    
    public static enum DOC_TYPE {OUT_PATIENT, ADMISSION, SELF_INSURANCE};
    
    private static final Color OUT_PATIENT_COLOR = new Color(0, 0, 0, 0);
    private static final Color SELF_INSURANCE_COLOR = new Color(255, 236, 103);
    private static final Color ADMISSION_COLOR = new Color(253, 202, 138);

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
    
    public void setTitleColor(DOC_TYPE type) {
        
        switch (type) {
            case ADMISSION:
                timeStampPanel.setBackground(ADMISSION_COLOR);
                break;
            case SELF_INSURANCE:
                timeStampPanel.setBackground(SELF_INSURANCE_COLOR);
                break;
            case OUT_PATIENT:    
            default:
                timeStampPanel.setBackground(OUT_PATIENT_COLOR);
                break;
        }
    }
}
