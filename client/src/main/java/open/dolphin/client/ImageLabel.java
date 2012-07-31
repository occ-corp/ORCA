package open.dolphin.client;

import java.awt.Dimension;
import javax.swing.JLabel;

/**
 * ImageLabel
 * パネルに表示するためのImageEntryのアイコンラベル
 * 
 * @author masuda, masuda Naika
 */
public class ImageLabel extends JLabel {

    private ImageEntry entry;

    public ImageLabel(ImageEntry entry) {
        this.entry = entry;
        setIcon(entry.getImageIcon());
        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
        setHorizontalTextPosition(JLabel.CENTER);
        setVerticalTextPosition(JLabel.BOTTOM);
        setBackground(null);
        setOpaque(true);
    }
    
    public ImageEntry getImageEntry() {
        return entry;
    }

    public void setImageEntry(ImageEntry entry) {
        this.entry = entry;
    }

    public final void fixToImageSize(int marginX, int marginY) {
        int width  = entry.getImageIcon().getIconWidth();
        int height = entry.getImageIcon().getIconHeight();
        int fontHeight = getFontMetrics(getFont()).getHeight();
        width += marginX;
        height += marginY;
        if (getText() != null && !"".endsWith(getText().trim())){
            height += fontHeight;
        }
        fixSize(new Dimension (width, height));
    }

    public void fixSize(int fixedWidth, int fixedHeight) {
        fixSize(new Dimension(fixedWidth, fixedHeight));
    }
    
    public void fixSize(Dimension d) {
        setSize(d);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }
}
