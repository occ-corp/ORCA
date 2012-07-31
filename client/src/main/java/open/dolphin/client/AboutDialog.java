package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;


/**
 * About dialog
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public class AboutDialog extends JDialog {
    
    /** Creates new AboutDialog */
    public AboutDialog(Frame f, String title, String imageFile) {
        
        super(f, title, true);
        
//masuda^  いたずら
        final JLabel label = new JLabel();
        final Icon[] icons = {
            ClientContext.getImageIcon(imageFile),
            ClientContext.getImageIcon("splash-usagi.jpg"),
            ClientContext.getImageIcon("masuda-naika.jpg")
        };
        label.setIcon(icons[0]);
        label.addMouseListener(new MouseAdapter() {

            int counter = 0;

            @Override
            public void mouseClicked(MouseEvent e) {

                if (++counter == icons.length) {
                    counter = 0;
                }
                label.setIcon(icons[counter]);

            }
        });
        int width = 0;
        int height = 0;
        for (Icon icon : icons) {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            width = Math.max(width, w);
            height = Math.max(height, h);
        }
        label.setPreferredSize(new Dimension(width, height));

        StringBuilder buf = new StringBuilder();
        buf.append(ClientContext.getString("productString"));
        buf.append("  Ver.");
        buf.append(ClientContext.getString("version"));
//masuda^
        buf.append(" Build ");
        buf.append(ClientContext.getString("buildDate"));

        String version = buf.toString();
        
        String[] copyright = ClientContext.getStringArray("copyrightString");
        
        Object[] message = new Object[] {
            //ClientContext.getImageIcon(imageFile),
            label,
            version,
            //copyright[0],
            //copyright[1],
            copyright
        };
//masdua$
        
        String[] options = {"閉じる"};
        JOptionPane optionPane = new JOptionPane(message,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                options[0]);
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                    close();
                }
            }
        });
        JPanel content = new JPanel(new BorderLayout());
        content.add(optionPane);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
        content.setOpaque(true);
        this.setContentPane(content);
        this.pack();
        Point loc = GUIFactory.getCenterLoc(this.getWidth(), this.getHeight());
        this.setLocation(loc);
        this.setVisible(true);
    }
    
    private void close() {
        this.setVisible(false);
        this.dispose();
    }
}