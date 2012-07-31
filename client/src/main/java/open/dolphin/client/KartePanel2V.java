package open.dolphin.client;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 * ２号カルテパネル 所見と処置が縦並び。もはや２号用紙ではないｗ
 *
 * @author masuda, Masuda Naika
 */
public final class KartePanel2V extends KartePanel {

    private JTextPane pTextPane;
    private JTextPane soaTextPane;

    // 縦並びのギャップ・余白
    private static final int vgap2 = 2;
    private static final int timeStampMargin = 30;

    public KartePanel2V(boolean editor) {
        super();
        initComponents(editor);
    }

    @Override
    protected void initComponents(boolean editor) {

        JPanel contentPanel = getContentPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        soaTextPane = createTextPane(false);
        contentPanel.add(soaTextPane);
        contentPanel.add(Box.createVerticalStrut(vgap2));
        pTextPane = createTextPane(false);
        contentPanel.add(pTextPane);
    }

    @Override
    public JTextPane getSoaTextPane() {
        return soaTextPane;
    }

    @Override
    public JTextPane getPTextPane() {
        return pTextPane;
    }
    
    @Override
    public boolean isSinglePane() {
        return false;
    }
    
    @Override
    public Dimension getPreferredSize() {

        Dimension d = getTimeStampPanel().getPreferredSize();
        int h = d.height;
        int w = d.width + timeStampMargin;

        int hsoa = soaTextPane.getPreferredSize().height;
        int hp = pTextPane != null
                ? pTextPane.getPreferredSize().height : 0;
        soaTextPane.setMaximumSize(new Dimension(w, hsoa));

        h += hsoa + hp;
        return new Dimension(w, h);
    }
}
