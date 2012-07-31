package open.dolphin.client;

import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * １号カルテパネル
 *
 * @author masuda, Masuda Naika
 */
public final class KartePanel1 extends KartePanel {

    private JTextPane soaTextPane;

    public KartePanel1(boolean editor) {
        super();
        initComponents(editor);
    }

    @Override
    protected void initComponents(boolean editor) {

        JPanel contentPanel = getContentPanel();

        if (editor) {
            contentPanel.setLayout(new GridLayout(rows, cols, hgap, vgap));
            soaTextPane = createTextPane(true);
            JScrollPane scroll = new JScrollPane(soaTextPane);
            scroll.setBorder(null);
            contentPanel.add(scroll);
        } else {
            contentPanel.setLayout(new GridLayout(rows, cols, hgap, vgap));
            soaTextPane = createTextPane(false);
            contentPanel.add(soaTextPane);
        }
    }

    @Override
    public JTextPane getSoaTextPane() {
        return soaTextPane;
    }

    @Override
    public JTextPane getPTextPane() {
        return null;
    }
    
    @Override
    public boolean isSinglePane() {
        return true;
    }
    
    // KarteDocumentViewerのBoxLayoutがうまくやってくれるように
    @Override
    public Dimension getPreferredSize() {

        int w = getContainerWidth();
        int h = getTimeStampPanel().getPreferredSize().height;
        h -= 15;    // some adjustment
        h += soaTextPane.getPreferredSize().height;

        return new Dimension(w, h);
    }
}
