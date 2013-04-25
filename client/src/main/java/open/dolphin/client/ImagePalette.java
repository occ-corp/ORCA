package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Insets;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.swing.DefaultListModel;
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
    private DefaultListModel<ImageEntry> jlistModel;
    private List<URL> urlList;


    public ImagePalette(String[] columnNames, int columnCount, int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        initComponent();
    }

    public ImagePalette() {
        this(null, DEFAULT_COLUMN_COUNT, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
    }

    private void initComponent() {
        
        // ListModelとListを設定
        jlistModel = new DefaultListModel();
        ImageEntryJList<ImageEntry> imageList= new ImageEntryJList(jlistModel);
        imageList.setMaxIconTextWidth(DEFAULT_IMAGE_WIDTH);
        
        JScrollPane scroll = new JScrollPane(imageList);
        scroll.getVerticalScrollBar().setUnitIncrement(imageHeight / 2);
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(scroll);

        // transferHandler
        imageList.setTransferHandler(ImageEntryTransferHandler.getInstance());
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
                jlistModel.addElement(entry);
            } catch (MalformedURLException e) {
            }
        }
    }

    private void setImageIcon(ImageEntry entry) throws MalformedURLException {
        URL url = new URL(entry.getUrl());
        ImageIcon ic = new ImageIcon(url);
        ImageIcon icon = adjustImageSize(ic, imageWidth, imageHeight);
        entry.setImageIcon(icon);
        entry.setIconText(null);
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