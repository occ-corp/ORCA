
package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * MainView改
 *
 * @author masuda, Masuda Naika
 */
public class MainView extends JPanel{

    private static final Font lblFont = new Font("Lucida Grande", 0, 10);
    private static final Dimension pbSize = new Dimension(80, 15);

    private JLabel dateLbl;
    private JProgressBar progressBar;
    private JLabel statusLbl;
    private JTabbedPane tabbedPane;

    public MainView() {

        dateLbl = new JLabel();
        dateLbl.setFont(lblFont);
        statusLbl = new JLabel();
        statusLbl.setFont(lblFont);
        progressBar = new JProgressBar();
        progressBar.setMinimumSize(pbSize);
        progressBar.setPreferredSize(pbSize);
        progressBar.setMaximumSize(pbSize);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(new EmptyBorder(3, 3, 3, 3));
        panel.add(statusLbl);
        panel.add(Box.createHorizontalGlue());
        panel.add(progressBar);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(dateLbl);
        tabbedPane = new JTabbedPane();

        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.SOUTH);
        this.add(tabbedPane, BorderLayout.CENTER);

    }


    public JLabel getDateLbl() {
        return dateLbl;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getStatusLbl() {
        return statusLbl;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
}
