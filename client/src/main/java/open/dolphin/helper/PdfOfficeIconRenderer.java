package open.dolphin.helper;

import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import open.dolphin.client.ClientContext;
import open.dolphin.client.NameValuePair;

/**
 *
 * @author Kazushi Minagawa.
 */
public class PdfOfficeIconRenderer extends DefaultListCellRenderer {
    
    private static final ImageIcon ICON_PDF = ClientContext.getImageIconAlias("icon_pdf_small");
    private static final ImageIcon ICON_OFFICE = ClientContext.getImageIconAlias("icon_plain_document_small");
    private static final Color DEFAULT_ODD_COLOR = ClientContext.getColor("color.odd");
    //private static final Color DEFAULT_EVENN_COLOR = ClientContext.getColor("color.even");
    private static final Color DEFAULT_EVENN_COLOR = ClientContext.getZebraColor();
    
    public PdfOfficeIconRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
                index, isSelected, cellHasFocus);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            
        } else {
            
            setForeground(list.getForeground());
            
            if ((index & (1)) == 0) {
                setBackground(DEFAULT_EVENN_COLOR);
            } else {
                setBackground(DEFAULT_ODD_COLOR);
            }
        }
        
        if (value != null) {
            NameValuePair pair = (NameValuePair)value;
            String test = pair.getValue();
            if (test!=null) {
                if (test.endsWith(".odt")) {
                    label.setIcon(ICON_OFFICE);
                } else if (test.endsWith("KarteEditor")) {
                    label.setIcon(null);
//masuda^   サマリー対応 
                } else if (test.endsWith("SummaryEditor")) {
                    label.setIcon(null);
//masuda$
                } else {
                    label.setIcon(ICON_PDF);
                }
            }
        } else {
            label.setIcon(null);
        }
        return label;
    }
}