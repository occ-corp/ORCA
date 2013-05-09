package open.dolphin.impl.img;

import java.awt.Container;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;
import open.dolphin.client.*;
import open.dolphin.helper.WindowSupport;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.util.FileIconMaker;
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

    protected static final String ICON_HAS_IMAGE = "icon_indicate_has_iamges_or_pdfs";
    private static final String ICON_PARENT_FOLDER = "icon_parent_folder";
    private static final String FOLDER_ICON = "icon_foldr";

    protected static final String SDF_FORMAT = "yyyy年MM月dd日";

    protected Desktop desktop;
    protected boolean DEBUG;
    protected String imageBase;
    protected Properties properties;
    
//masuda^
    protected int imageSize = MAX_IMAGE_SIZE;
    protected DefaultListModel<ImageEntry> listModel;
    private Path rootPath;
    private String currentDir;

    protected void setRootPath(String strPath) {
        FileSystem fs = FileSystems.getDefault();
        rootPath = fs.getPath(strPath);
    }

    public String getCurrentDir() {
        return currentDir;
    }
    private Map<String, ImageEntry> iconCache;
//masuda$
    
    public AbstractBrowser() {
        DEBUG = (ClientContext.getBootLogger().getLevel()==Level.DEBUG);
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        } else {
            ClientContext.getBootLogger().warn("Desktop is not supported");
        }
        listModel = new DefaultListModel<ImageEntry>();
        iconCache = new HashMap<>();
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
            icon = ClientContext.getImageIconAlias(ICON_HAS_IMAGE);
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
            scan(getImgLocation());
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
    protected void scan(String imgLoc) {
        
        currentDir = imgLoc;
        listModel.clear();

        if (valueIsNullOrEmpty(imgLoc)) {
            return;
        }
        
        FileSystem fs = FileSystems.getDefault();
        final Path imagePath = fs.getPath(imgLoc);

        if (!Files.exists(imagePath) || !Files.isDirectory(imagePath)) {
            return;
        }

        SwingWorker worker = new SwingWorker<Void, ImageEntry>() {

            @Override
            protected Void doInBackground() throws Exception {
                
                List<Path> pathList = new ArrayList<>();

                try (DirectoryStream<Path> ds = Files.newDirectoryStream(imagePath)) {
                    for (Path path : ds) {
                        pathList.add(path);
                    }
                } catch (IOException | DirectoryIteratorException ex) {
                }

                // 親フォルダに戻るアイコンを追加する
                if (imagePath.compareTo(rootPath) > 0) {
                    ImageEntry entry = createImageEntry(imagePath.getParent());
                    entry.setImageIcon(ClientContext.getImageIconAlias(ICON_PARENT_FOLDER));
                    entry.setIconText("一つ上へ");
                    entry.setDirectory(true);
                    publish(entry);
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
                                    FileTime l1 = Files.getLastModifiedTime((Path) o1);
                                    FileTime l2 = Files.getLastModifiedTime((Path) o2);
                                    return l2.compareTo(l1);
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
                                    FileTime l1 = Files.getLastModifiedTime((Path) o1);
                                    FileTime l2 = Files.getLastModifiedTime((Path) o2);
                                    return l1.compareTo(l2);
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
                                String n1 = ((Path) o1).getFileName().toString();
                                String n2 = ((Path) o2).getFileName().toString();
                                return n2.compareTo(n1);
                            }
                        });
                    } else {
                        Collections.sort(pathList, new Comparator() {

                            @Override
                            public int compare(final Object o1, final Object o2) {
                                String n1 = ((Path) o1).getFileName().toString();
                                String n2 = ((Path) o2).getFileName().toString();
                                return n1.compareTo(n2);
                            }
                        });
                    }
                }
                
                for (Path path : pathList) {

                    URI uri = path.toUri();
                    URL url = uri.toURL();
                    String pathStr = path.toString();
                    String fileName = path.getFileName().toString();

                    debug(uri, url, pathStr, fileName);
                    
                    if (Files.isDirectory(path)) {
                        ImageEntry entry = createImageEntry(path);
                        entry.setImageIcon(ClientContext.getImageIconAlias(FOLDER_ICON));
                        entry.setIconText(entry.getFileName());
                        entry.setDirectory(true);
                        publish(entry);
                        continue;
                    }
                    
                    if (Files.size(path) == 0) {
                        continue;
                    }

                    if (fileName.startsWith(".")) {
                        continue;
                    }

                    String suffix = getSuffix(pathStr);
                    if (suffix == null) {
                        continue;
                    }

                    // Thumbnail
                    ImageEntry entry = createThumbnailImageEntry(path);
                    publish(entry);
                }
                
                return null;
            }

            @Override
            protected void process(List<ImageEntry> chunks) {
                for (ImageEntry entry : chunks) {
                    listModel.addElement(entry);
                }
            }
        };

        worker.execute();
    }
    
    private ImageEntry createThumbnailImageEntry(Path path) throws IOException {
        ImageEntry entry = createImageEntry(path);
        entry.setImageIcon(FileIconMaker.createIcon(path, imageSize));
        SimpleDateFormat sdf = new SimpleDateFormat(SDF_FORMAT);
        String lblName = displayIsFilename()
                ? entry.getFileName()
                : sdf.format(entry.getLastModified());
        entry.setIconText(lblName);
        return entry;
    }
    
    public void addThumbnailImageEntry(Path path) throws IOException {
        ImageEntry entry = createThumbnailImageEntry(path);
        listModel.addElement(entry);
    }
    
    private ImageEntry createImageEntry(Path path) throws IOException {

        URI uri = path.toUri();
        URL url = uri.toURL();
        String pathStr = path.toString();
        String fileName = path.getFileName().toString();
        long last = Files.getLastModifiedTime(path).toMillis();

        String key = pathStr + ":" + String.valueOf(last);
        ImageEntry entry = iconCache.get(key);
        if (entry == null) {
            entry = new ImageEntry();
            entry.setUrl(url.toString());
            entry.setPath(pathStr);
            entry.setFileName(fileName);
            entry.setLastModified(last);
            iconCache.put(key, entry);
        }
        
        return entry;
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
        scan(getImgLocation());
    }

    @Override
    public void stop() {
        // memory leak?
        //if (imagePanel != null) {
        //    imagePanel.removeAll();
        //}
        iconCache.clear();
    }

    protected boolean valueIsNullOrEmpty(String test) {
        return test == null || test.equals("");
    }

    protected boolean valueIsNotNullNorEmpty(String test) {
        return !valueIsNullOrEmpty(test);
    }
    
    protected class ImageListMouseAdapter extends MouseAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {

            if (e.getClickCount() == 2) {
                JList imageList = (JList) e.getComponent();
                ImageEntry entry = (ImageEntry) imageList.getSelectedValue();
                if (entry != null && (!entry.isDirectory())) {
                    openImage(entry);
                } else if (entry != null && entry.isDirectory()) {
                    scan(entry.getPath());
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger() && e.getClickCount() == 1) {
                JList imageList = (JList) e.getComponent();
                ImageEntry entry = (ImageEntry) imageList.getSelectedValue();
                if (entry == null) {
                    return;
                }
                
                JPopupMenu contextMenu = new JPopupMenu();
                JMenuItem micp = new JMenuItem("コピー");
                Container c = imageList.getTopLevelAncestor();
                if (c instanceof JFrame) {
                    JFrame frame = (JFrame) c;
                    Object objMediator = WindowSupport.getMediator(frame);
                    if (objMediator != null && objMediator instanceof ChartMediator) {
                        ChartMediator mediator = (ChartMediator) objMediator;
                        Action copy = mediator.getAction(GUIConst.ACTION_COPY);
                        copy.setEnabled(true);
                        micp.setAction(copy);
                        contextMenu.add(micp);
                    }
                }
                contextMenu.show(imageList, e.getX(), e.getY());
            }
        }
    }
}
