package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.Image;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import open.dolphin.tr.ImageEntryTransferHandler;

/**
 * ImagePalette
 *
 * @author Minagawa,Kazushi. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class ImagePalette extends JPanel {

    private static final int DEFAULT_COLUMN_COUNT = 3;  // not used
    private static final int DEFAULT_IMAGE_WIDTH  = 120;
    private static final int DEFAULT_IMAGE_HEIGHT = 120;

    private int imageWidth;
    private int imageHeight;
    private ImagePanel imagePanel;
   
    private List<URL> urlList;
    
    private static final int MARGIN = 5;

    public ImagePalette(String[] columnNames, int columnCount, int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        initComponent();
    }

    public ImagePalette() {
        this(null, DEFAULT_COLUMN_COUNT, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
    }

    private void initComponent() {

        // Image panel を生成する
        imagePanel = new ImagePanel();
        // TransferHandlerを設定する
        imagePanel.setTransferHandler(ImageEntryTransferHandler.getInstance());

        this.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(imagePanel);
        scroll.getVerticalScrollBar().setUnitIncrement(imageHeight / 2);
        this.add(scroll, BorderLayout.CENTER);

    }

    public void setUrlList(List<URL> list) {
        urlList = list;
        refresh();
    }

    public void dispose() {
    }

    public void refresh() {
        
        if (urlList == null || urlList.isEmpty()) {
            return;
        }
        
        for (URL url : urlList) {
            try {
                ImageEntry entry = new ImageEntry();
                entry.setUrl(url.toString());
                setImageIcon(entry);
                ImageLabel lbl = new ImageLabel(entry);
                lbl.fixSize(imageWidth + MARGIN, imageHeight + MARGIN);
                imagePanel.add(lbl);
            } catch (MalformedURLException e) {
            }
        }
    }

    private void setImageIcon(ImageEntry entry) throws MalformedURLException {
        URL url = new URL(entry.getUrl());
        ImageIcon ic = new ImageIcon(url);
        ImageIcon icon = adjustImageSize(ic, imageWidth, imageHeight);
        entry.setImageIcon(icon);
    }
    
    private ImageIcon adjustImageSize(ImageIcon icon, int width, int height) {

        if ((icon.getIconHeight() > height) || (icon.getIconWidth() > width)) {
            Image img = icon.getImage();
            float hRatio = (float) icon.getIconHeight() / height;
            float wRatio = (float) icon.getIconWidth() / width;
            int h, w;
            if (hRatio > wRatio) {
                h = height;
                w = (int) (icon.getIconWidth() / hRatio);
            } else {
                w = width;
                h = (int) (icon.getIconHeight() / wRatio);
            }
            img = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);

        } else {
            return icon;
        }
    }
}