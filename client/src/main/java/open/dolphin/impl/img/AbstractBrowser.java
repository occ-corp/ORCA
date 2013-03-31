package open.dolphin.impl.img;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import open.dolphin.client.*;
import open.dolphin.helper.ImageHelper;
import open.dolphin.infomodel.PatientModel;
import org.apache.log4j.Level;

/**
 * AbstractBrowser
 *
 * @author Kazushi Minagawa, Digital Globe, Inc. 
 * @author modified by masuda, Masuda Naika
 */
public abstract class AbstractBrowser extends AbstractChartDocument {

    protected static final int MAX_IMAGE_SIZE       = 120;
    protected static final int CELL_WIDTH_MARGIN    = 20;
    protected static final int CELL_HEIGHT_MARGIN   = 20;

    protected static final String PROP_BASE_DIR         = "baseDir";
    protected static final String PROP_DROP_ACTION      = "dropAction";
    protected static final String PROP_COLUMN_COUNT     = "columnCount";
    protected static final String PROP_SHOW_FILE_NAME   = "showFileName";
    protected static final String PROP_DISPLAY_ATTR     = "displayAttr";
    protected static final String PROP_SORT_ATTR        = "sortAttr";
    protected static final String PROP_SORT_ORDER       = "sortOrder";

    protected static final String[] ACCEPT_FILE_TYPES = {"dcm","jpg", "png", "bmp", "gif", "tif"};
    protected static final String[] ACCEPT_DOC_TYPES = {"pdf", "doc","docx", "xls", "xlsx", "ppt","pptx", "odt"};
    protected static final String[] ACCEPT_DOC_ICONS = 
        {"pdf_icon40px.gif", "Word-32-d.gif","Word-32-d.gif", "Excel-32-d.gif", "Excel-32-d.gif", 
            "PowerPoint-32-d.gif", "PowerPoint-32-d.gif", "LibreWriterIcon.png"};

//    protected static final String[] OTHER_DOC_TYPES =
//        {"pdf", "txt", "rtf", "htm","html", "doc","docx", "xls", "xlsx", "ppt","pptx","pages", "numbers", "key"};
    protected static final String DEFAULT_DOC_ICON = "docs_32.gif";
    protected static final String ICON_HAS_IMAGE = "/open/dolphin/resources/images/play_16.gif";

    protected static SimpleDateFormat SDF = new SimpleDateFormat("yyyy年MM月dd日");

    protected Desktop desktop;
    protected boolean DEBUG;
    protected String imageBase;
    protected Properties properties;
    
//masuda^
    private int imageSize = MAX_IMAGE_SIZE;
    
    private JPanel imagePanel;

    protected void setImagePanel(JPanel panel) {
        imagePanel = panel;
        imagePanel.setTransferHandler(ImageBrowserPanelTransferHandler.getInstance());
        imagePanel.putClientProperty(GUIConst.PROP_KARTE_COMPOSITOR, AbstractBrowser.this);
    }
    public JPanel getImagePanel() {
        return imagePanel;
    }
//masuda$
    
    public AbstractBrowser() {
        DEBUG = (ClientContext.getBootLogger().getLevel()==Level.DEBUG);
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        } else {
            ClientContext.getBootLogger().warn("Desktop is not supported");
        }
    }

    /**
     * ブラウザ表示設定の規定値を返す。
     * @return Properties
     */
    protected Properties getProperties() {
        Properties defaults = new Properties();
        defaults.setProperty(PROP_DROP_ACTION, "copy");
        defaults.setProperty(PROP_COLUMN_COUNT, "5");
        defaults.setProperty(PROP_SHOW_FILE_NAME, "true");
        defaults.setProperty(PROP_DISPLAY_ATTR, "filename");
        defaults.setProperty(PROP_SORT_ATTR, "lastModified");
        defaults.setProperty(PROP_SORT_ORDER, "desc");
        properties = new Properties(defaults);
        return properties;
    }

    protected boolean dropIsMove() {
        return (!properties.getProperty(PROP_DROP_ACTION).equals("copy"));
    }
    
    protected int columnCount() {
        return Integer.parseInt(properties.getProperty(PROP_COLUMN_COUNT));
    }
    
    protected boolean showFilename() {
        return Boolean.parseBoolean(properties.getProperty(PROP_SHOW_FILE_NAME));
    }
    
    protected boolean displayIsFilename() {
        return (properties.getProperty(PROP_DISPLAY_ATTR).equals("filename"));
    }

    protected boolean sortIsLastModified() {
        return (properties.getProperty(PROP_SORT_ATTR).equals("lastModified"));
    }

    protected boolean sortIsDescending() {
        return (properties.getProperty(PROP_SORT_ORDER).equals("desc"));
    }

    protected String getSuffix(String path) {
        int index = (path != null) ? path.lastIndexOf('.') : -1;
        return (index >= 0) ? path.substring(index + 1).toLowerCase() : null;
    }

    protected boolean isImage(String ext) {
        boolean ret = false;
        if (ext != null) {
            for (String str : AbstractBrowser.ACCEPT_FILE_TYPES) {
                if (str.equals(ext)) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    protected int isDocument(String ext) {
        int ret = -1;
        if (ext!=null) {
            for (int i=0; i < AbstractBrowser.ACCEPT_DOC_TYPES.length; i++) {
                if (AbstractBrowser.ACCEPT_DOC_TYPES[i].equals(ext)) {
                    ret = i;
                    break;
                }
            }
        }
        return ret;
    }

    /**
     * Chart がプラグインをタブへ追加する場合にコールする。
     * 患者ディレクトリにファイルがあれば アイコンを返す。
     */
    @Override
    public ImageIcon getIconInfo(Chart ctx) {
        ImageIcon icon = null;
        PatientModel pm = ctx.getPatient();
        String pid = pm.getPatientId();
        if (hasImageOrPDF(pid)) {
            icon = ClientContext.getImageIcon(ICON_HAS_IMAGE);
        }
        return icon;
    }

    /**
     * 指定した患者のディレクトリにファイルが存在する場合は true を返す。
     */
    private boolean hasImageOrPDF(String patientId) {

        boolean ret = false;

        if (getImageBase() != null && patientId!= null) {

            StringBuilder sb = new StringBuilder();
            sb.append(getImageBase());
            if (! getImageBase().endsWith(File.separator)) {
                sb.append(File.separator);
            }
            sb.append(patientId);

            FileSystem fs = FileSystems.getDefault();
            Path imagePath = fs.getPath(sb.toString());
            
            if (Files.exists(imagePath) && Files.isDirectory(imagePath)) {
                try {
                    Iterator<Path> itr = Files.newDirectoryStream(imagePath).iterator();
                    ret = itr.hasNext();
                } catch (IOException ex) {
                }
            }
        }

        return ret;
    }

    /**
     * PDFや画像が保管されているベース（共有）ディレクトリを返す。
     * @return ベースディレクトリ名
     */
    public String getImageBase() {
        return this.imageBase;
    }
    
    /**
     * PDFや画像が保管されているベース（共有）ディレクトリを設定する。
     * @param base ベースディレクトリ名
     */
    public void setImageBase(String base) {
        String old = this.imageBase;
        this.imageBase = base;
        if (!this.imageBase.equals(old)) {
            scan();
        }
    }
    
    private void debug(URI uri, URL url, String path, String fileName) {
        if (DEBUG) {
            ClientContext.getBootLogger().debug("-------------------------------------------");
            ClientContext.getBootLogger().debug("URI = " + uri.toString());
            ClientContext.getBootLogger().debug("URL = " + url.toString());
            ClientContext.getBootLogger().debug("PATH = " + path);
            ClientContext.getBootLogger().debug("File Name = " + fileName);
        }
    }

    /**
     * 患者フォルダをスキャンする。
     */
    protected void scan() {
        
        imagePanel.removeAll();
        imagePanel.repaint();

        String imgLoc = getImgLocation();

        if (valueIsNullOrEmpty(imgLoc)) {
            return;
        }
        
        FileSystem fs = FileSystems.getDefault();
        final Path imagePath = fs.getPath(imgLoc);

        if (!Files.exists(imagePath) || !Files.isDirectory(imagePath)) {
            return;
        }

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                
                List<Path> pathList = new ArrayList<Path>();
                for (Iterator<Path> itr = Files.newDirectoryStream(imagePath).iterator(); itr.hasNext();) {
                    pathList.add(itr.next());
                }
                
                if (pathList.isEmpty()) {
                    return null;
                }

                // Sort
                if (sortIsLastModified()) {
                    // 最終更新日でソート
                    if (sortIsDescending()) {
                        Collections.sort(pathList, new Comparator() {

                            @Override
                            public int compare(final Object o1, final Object o2) {
                                try {
                                    long l1 = Files.getLastModifiedTime((Path) o1).toMillis();
                                    long l2 = Files.getLastModifiedTime((Path) o2).toMillis();
                                    return (l2 > l1) ? 1 : ((l2 < l1) ? -1 : 0);
                                } catch (IOException ex) {
                                }
                                return 0;
                            }
                        });
                    } else {
                        Collections.sort(pathList, new Comparator() {

                            @Override
                            public int compare(final Object o1, final Object o2) {
                                try {
                                    long l1 = Files.getLastModifiedTime((Path) o1).toMillis();
                                    long l2 = Files.getLastModifiedTime((Path) o2).toMillis();
                                    return (l1 > l2) ? 1 : ((l1 < l2) ? -1 : 0);
                                } catch (IOException ex) {
                                }
                                return 0;
                            }
                        });
                    }
                    
                } else {
                    // filename でソート
                    if (sortIsDescending()) {
                        Collections.sort(pathList, new Comparator() {

                            @Override
                            public int compare(final Object o1, final Object o2) {
                                String n1 = ((Path) o1).toString();
                                String n2 = ((Path) o2).toString();
                                return n2.compareTo(n1);
                            }
                        });
                    } else {
                        Collections.sort(pathList, new Comparator() {

                            @Override
                            public int compare(final Object o1, final Object o2) {
                                String n1 = ((Path) o1).toString();
                                String n2 = ((Path) o2).toString();
                                return n1.compareTo(n2);
                            }
                        });
                    }
                }

                for (Path filePath : pathList) {

                    URI uri = filePath.toUri();
                    URL url = uri.toURL();
                    String path = filePath.toString();
                    String fileName = filePath.getFileName().toString();
                    long last = Files.getLastModifiedTime(filePath).toMillis();

                    debug(uri, url, path, fileName);

                    if (Files.size(filePath) == 0) {
                        continue;
                    }

                    if (fileName.startsWith(".")) {
                        continue;
                    }

                    String suffix = getSuffix(path);
                    if (suffix == null) {
                        continue;
                    }

                    // Thumbnail
                    ImageEntry entry = new ImageEntry();
                    entry.setUrl(url.toString());
                    entry.setPath(path);
                    entry.setFileName(fileName);
                    entry.setLastModified(last);
                    //imageList.add(entry);
                    setImageIcon(entry);
                    
                    String lblName = displayIsFilename() ? entry.getFileName() : SDF.format(entry.getLastModified());
                    ImageLabel lbl = new ImageLabel(entry);
                    lbl.setText(lblName);
                    lbl.fixSize(MAX_IMAGE_SIZE + CELL_WIDTH_MARGIN, MAX_IMAGE_SIZE + CELL_WIDTH_MARGIN);
                    imagePanel.add(lbl);
                    imagePanel.revalidate();
                }
                //return imageList;
                return null;
            }
        };

        worker.execute();
    }

    private void setImageIcon(ImageEntry entry) {

        ImageIcon icon = entry.getImageIcon();
        String ext = getSuffix(entry.getFileName());

        if (icon == null && isImage(ext)) {
            try {
                BufferedImage image = ImageIO.read(new File(entry.getPath()));
                if (image != null) {
                    image = ImageHelper.getFirstScaledInstance(image, imageSize);
                    icon = new ImageIcon(image);
                    entry.setImageIcon(icon);
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace(System.err);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            } catch (Throwable e) {
                e.printStackTrace(System.err);
            }

        } else if (icon == null) {
            int index = isDocument(ext);
            String iconStr = (index >= 0) 
                    ? AbstractBrowser.ACCEPT_DOC_ICONS[index] 
                    : null;
            icon = (iconStr != null) 
                    ? ClientContext.getImageIcon(iconStr) 
                    : ClientContext.getImageIcon(AbstractBrowser.DEFAULT_DOC_ICON);
            entry.setImageIcon(icon);
        }
    }
    
    protected abstract String getImgLocation();

    protected abstract void initComponents();

    protected void openImage(ImageEntry entry) {

        if (desktop==null) {
            return;
        }

        // pathに空白があるとダメのworkaround
        String uri = "file:///" 
                + entry.getPath().replace(File.separator, "/").replace(" ", "%20");
        try {
            File f = new File(new URI(uri));
            desktop.open(f);
        } catch (Exception ex) {
            ClientContext.getBootLogger().warn(ex);
        }
    }
    
    @Override
    public void start() {
        initComponents();
        scan();
    }

    @Override
    public void stop() {
        // memory leak?
        if (imagePanel != null) {
            imagePanel.removeAll();
        }
    }

    protected boolean valueIsNullOrEmpty(String test) {
        return test == null || test.equals("");
    }

    protected boolean valueIsNotNullNorEmpty(String test) {
        return !valueIsNullOrEmpty(test);
    }
}
