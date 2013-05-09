package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.swing.*;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.helper.MenuSupport;
import open.dolphin.helper.WindowSupport;

/**
 * ImageBox
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class ImageBox extends AbstractMainTool {
    
    private static final String[] FOLDER_NAMES = {
        "1-全身・躯幹", "2-頭頚部", "3-上肢", "4-下肢"};

    private static final int DEFAULT_IMAGE_WIDTH 	= 80; //120
    private static final int DEFAULT_IMAGE_HEIGHT 	= 80;
    private static final String[] DEFAULT_IMAGE_SUFFIX = {".jpg", ".png", ".bmp", ".gif"};
    private String imageLocation;
    private JTabbedPane tabbedPane;
    private int imageWidth = DEFAULT_IMAGE_WIDTH;
    private int imageHeight = DEFAULT_IMAGE_HEIGHT;
    private String[] suffix = DEFAULT_IMAGE_SUFFIX;
    private int defaultWidth = 406;
    private int defaultHeight = 587;
    private int defaultLocX = 537;
    private int defaultLocY = 22;

    private JFrame frame;
    
    private String title = "シェーマボックス";
    
//pns^  SchemaBox でもメニューを出すため
    private MenuSupport mediator;
//pns$
    private static final FolderImagePair[] DEFAULT_IMAGES = {
        new FolderImagePair(FOLDER_NAMES[0], "01-001.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-002.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-003.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-004.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-005.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-006.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-007.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-008.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-009.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-010.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-011.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-012.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-013.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-014.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-015.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-016.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-017.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-018.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-019.JPG"),
        new FolderImagePair(FOLDER_NAMES[0], "01-020.JPG"),
        
        new FolderImagePair(FOLDER_NAMES[1], "02-001.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-002.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-003.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-004.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-005.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-006.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-007.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-008.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-009.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-010.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-011.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-012.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-013.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-014.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-015.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-016.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-017.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-018.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-019.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-020.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-021.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-022.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-023.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-024.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-025.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-026.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-027.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-028.JPG"),
        new FolderImagePair(FOLDER_NAMES[1], "02-029.JPG"),
        
        new FolderImagePair(FOLDER_NAMES[2], "03-001.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-002.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-003.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-004.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-005.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-006.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-007.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-008.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-009.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-010.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-011.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-012.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-013.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-014.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-015.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-016.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-017.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-018.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-019.JPG"),
        new FolderImagePair(FOLDER_NAMES[2], "03-020.JPG"),
        
        new FolderImagePair(FOLDER_NAMES[3], "04-001.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-002.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-003.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-004.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-005.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-006.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-007.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-008.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-009.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-010.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-011.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-012.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-013.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-014.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-015.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-016.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-017.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-018.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-019.JPG"),
        new FolderImagePair(FOLDER_NAMES[3], "04-020.JPG"),
    };
    
    private static final String RES_BASE = "/open/dolphin/resources/schema/";
    
    private static class FolderImagePair {

        private String folder;
        private String fileName;

        private FolderImagePair(String folder, String fileName) {
            this.folder = folder;
            this.fileName = fileName;
        }

        private String getFolderName() {
            return folder;
        }

        private String getFileName() {
            return fileName;
        }
    }
    
    @Override
    public void start() {
//masuda    既に一度シェーマボックスを起動したことがあるかチェックする
        if (frame == null) {
            initComponent();
            connect();
            setImageLocation(ClientContext.getSchemaDirectory());
        }
        frame.setVisible(true);
    }

    @Override
    public void stop() {
//masuda    pns先生に倣いframeを隠すのみにした
        frame.setVisible(false);
    }

    public String getImageLocation() {
        return imageLocation;
    }

    public void setImageLocation(String loc) {

        this.imageLocation = loc;

        refresh();
    }

    public void refresh() {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                createImagePalettes();
                return null;
            }
        };

        worker.execute();
    }

    private void initComponent() {

        // TabbedPane を生成する
        tabbedPane = new JTabbedPane();
        tabbedPane.setToolTipText("ダブルクリックで画像更新します");
        tabbedPane.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    refresh();
                }
            }
        });

        // 全体を配置する
        JPanel p = new JPanel(new BorderLayout());
        p.add(tabbedPane, BorderLayout.CENTER);

//pns^  mac で SchemaBox にもメニューバーを出す
        if (ClientContext.isMac()) {
            WindowSupport windowSupport = WindowSupport.create(title);
            frame = windowSupport.getFrame();
            javax.swing.JMenuBar myMenuBar = windowSupport.getMenuBar();
            mediator = new MenuSupport(this);
            AbstractMenuFactory appMenu = AbstractMenuFactory.getFactory();

            // mainWindow の menuSupport をセットしておけばメニュー処理は mainWindow がしてくれる
            appMenu.setMenuSupports(getContext().getMenuSupport(), mediator);
            appMenu.build(myMenuBar);
            mediator.registerActions(appMenu.getActionMap());
            mediator.disableAllMenus();
            String[] enables = new String[]{
                GUIConst.ACTION_SHOW_STAMPBOX,
                GUIConst.ACTION_SET_KARTE_ENVIROMENT
            };
            mediator.enableMenus(enables);
        } else {
            frame = new JFrame(title);
            // アイコン設定
            ClientContext.setDolphinIcon(frame);
        }
//pns$
        
//masuda^
        ComponentMemory cm = new ComponentMemory(frame,
                new Point(defaultLocX, defaultLocY),
                new Dimension(defaultWidth, defaultHeight),
                ImageBox.this);
        cm.setToPreferenceBounds();
//masuda$
        
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                processWindowClosing();
            }
        });
        frame.getContentPane().add(p);
    }

    private void connect() {
    }

    private void createImagePalettes() {
        
        tabbedPane.removeAll();
        
        // タブ（フォルダ名）と画像URLリストのマップ
        Map<String, List<URL>> map = new LinkedHashMap<>();
        
        // デフォルトイメージ
        for (FolderImagePair pair : DEFAULT_IMAGES) {
            String folderName = pair.getFolderName();
            String fileName = pair.getFileName();
            List<URL> urlList = map.get(folderName);
            if (urlList == null) {
                urlList = new ArrayList<>();
            }
            
            try {
                URL url = getClass().getResource(RES_BASE + fileName);
                if (url != null) {
                    urlList.add(url);
                }
            } catch (Exception e) {
            }
            map.put(folderName, urlList);
        }

        // ユーザーイメージ
        FileSystem fs = FileSystems.getDefault();
        Path imagePath = fs.getPath(imageLocation);
        DirectoryFilter dirFilter = new DirectoryFilter();
        ImageFileFilter imageFilter = new ImageFileFilter(suffix);

        try (DirectoryStream<Path> directories = Files.newDirectoryStream(imagePath, dirFilter)) {
            for (Path dirPath : directories) {
                String folderName = dirPath.getFileName().toString();
                List<URL> urlList = map.get(folderName);
                if (urlList == null) {
                    urlList = new ArrayList<>();
                }
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dirPath, imageFilter)) {
                    for (Path filePath : ds) {
                        URL url = filePath.toUri().toURL();
                        if (url != null) {
                            urlList.add(url);
                        }
                    }
                } catch (IOException | DirectoryIteratorException ex) {
                }
                map.put(folderName, urlList);
            }
        } catch (IOException | DirectoryIteratorException ex) {
        }
     
        for (Map.Entry entry : map.entrySet()) {
            String folderName = (String) entry.getKey();
            List<URL> urlList = (List<URL>) entry.getValue();
            ImagePalette imagePalette = new ImagePalette(null, 0, imageWidth, imageHeight);
            imagePalette.setUrlList(urlList);
            tabbedPane.addTab(folderName, imagePalette);
        }
    }

    private class DirectoryFilter implements DirectoryStream.Filter<Path> {

        @Override
        public boolean accept(Path entry) throws IOException {
            return Files.isDirectory(entry);
        }
    }
    
    private class ImageFileFilter implements DirectoryStream.Filter<Path> {

        private String[] suffix;

        public ImageFileFilter(String[] suffix) {
            this.suffix = suffix;
        }

        @Override
        public boolean accept(Path entry) throws IOException {
            String path = entry.toString().toLowerCase();
            for (String str : suffix) {
                if (path.endsWith(str)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    public void processWindowClosing() {
        stop();
    }

    /**
     * @param imageWidth The imageWidth to set.
     */
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    /**
     * @return Returns the imageWidth.
     */
    public int getImageWidth() {
        return imageWidth;
    }

    /**
     * @param imageHeight The imageHeight to set.
     */
    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    /**
     * @return Returns the imageHeight.
     */
    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * @param suffix The suffix to set.
     */
    public void setSuffix(String[] suffix) {
        this.suffix = suffix;
    }

    /**
     * @return Returns the suffix.
     */
    public String[] getSuffix() {
        return suffix;
    }
}