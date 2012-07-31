
package open.dolphin.impl.routinemed;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import open.dolphin.client.*;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.RoutineMedModel;
import open.dolphin.tr.StampHolderTransferHandler;

/**
 *
 * @author masuda, Masuda Naika
 */
public class RoutineMedPanel extends JPanel {
    
    // タイムスタンプの foreground カラー
    private static final Color TIMESTAMP_FORE = Color.BLUE;
    private static final Color UNEDITABLE_COLOR = new Color(227, 250, 207);
    // タイムスタンプのフォントサイズ
    private static final int TIMESTAMP_FONT_SIZE = 14;
    // タイムスタンプフォント
    private static final Font TIMESTAMP_FONT = new Font("Dialog", Font.PLAIN, TIMESTAMP_FONT_SIZE);
    // TextPaneの余白
    private static final int vMargin = 5;
    private static final int hMargin = 5;
    private static final Insets TEXT_PANE_MARGIN = new Insets(vMargin, hMargin, vMargin, hMargin);
    
    private static final String ROLE_RMP = "rmp";
    
    private static final int MEMO_HEIGHT = 80;
    
    private static final Color NON_SELECTED_BORDER = new Color(0, 0, 0, 0); // 透明
    private static final Color SELECTED_BORDER = new Color(255, 0, 153);
    private static final Border nonSelectedBorder = BorderFactory.createLineBorder(NON_SELECTED_BORDER);
    private static final Border selectedBorder = BorderFactory.createLineBorder(SELECTED_BORDER);

    private JLabel timeStampLabel;
    private JTextArea memoArea;
    private JPanel contentPanel;
    
    private KartePane kartePane;
    private Chart chart;
    
    private int index;
    
    private RoutineMedModel medModel;
    
    public RoutineMedPanel() {
        initComponents();
    }
    
    private void initComponents() {

        // タイトル
        timeStampLabel = new JLabel();
        timeStampLabel.setHorizontalAlignment(JLabel.CENTER);
        timeStampLabel.setForeground(TIMESTAMP_FORE);
        timeStampLabel.setFont(TIMESTAMP_FONT);
        
        memoArea = new JTextArea();
        memoArea.setLineWrap(true);
        memoArea.setEditable(false);
        memoArea.setMinimumSize(new Dimension(0, MEMO_HEIGHT));
        memoArea.setPreferredSize(new Dimension(0, MEMO_HEIGHT));
        JScrollPane scroll = new JScrollPane(memoArea);
        
        contentPanel = new JPanel();
        contentPanel.setOpaque(true);
        contentPanel.setBackground(UNEDITABLE_COLOR);
        
        // 全体レイアウト
        setLayout(new BorderLayout());
        add(timeStampLabel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(scroll, BorderLayout.SOUTH);
        
        setBorder(nonSelectedBorder);
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    public int getIndex() {
        return index;
    }
    
    public void setContext(Chart chart) {
        this.chart = chart;
    }

    public void setRoutineMedModel(RoutineMedModel model) {
        medModel = model;
    }
    
    public RoutineMedModel getRoutineMedModel() {
        return medModel;
    }
    
    public void setSelected(boolean selected) {
        if (selected) {
            setBorder(selectedBorder);
        } else {
            setBorder(nonSelectedBorder);
        }
    }
    
    public void render() {
        
        // タイトルを設定する
        timeStampLabel.setText(medModel.getRegistDateStr());
        memoArea.setText(medModel.getMemo());        
        
        // kartePaneを作成
        JTextPane textPane = new JTextPane();
        contentPanel.removeAll();
        contentPanel.add(textPane);
        textPane.setMargin(TEXT_PANE_MARGIN);
        kartePane = new KartePane();
        kartePane.setRole(ROLE_RMP);
        kartePane.setTextPane(textPane);
        kartePane.setParent(null);
        
        // レンダリングする
        List<ModuleModel> list = medModel.getModuleList();
        KarteStyledDocument doc = kartePane.getDocument();
        for (ModuleModel stamp : list) {
            // StampHolderを作成し
            StampHolder h = new StampHolder(kartePane, stamp);
            // TransferHandlerを設定する
            h.setTransferHandler(StampHolderTransferHandler.getInstance());
            // textPaneにスタンプを配置する
            doc.flowStamp(h);
            doc.insertFreeString("\n", null);
        }
        // リスナーを設定する
        ChartMediator mediator = chart.getChartMediator();
        kartePane.init(false, mediator);
    }
    
    @Override
    public Dimension getMaximumSize() {
        Dimension d = getPreferredSize();
        d.height = Integer.MAX_VALUE;
        return d;
    }
}
