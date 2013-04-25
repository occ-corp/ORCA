package open.dolphin.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import open.dolphin.client.ClientContext;
import open.dolphin.helper.ImageHelper;

/**
 * ファイルのアイコン作成
 * @author masuda, Masuda Naika
 */
public class FileIconMaker {
    
    private static final String DEFAULT_DOC_ICON = "icon_default_document";
    private static final String FOLDER_ICON = "icon_foldr";
    private static final String[] IMAGE_FILE_EXTS = {".dcm", ".jpg", ".png", ".bmp", ".gif", ".tif"};
    private static final String[][] EXT_ICON_PAIRS = {
        {".pdf", "icon_pdf"},
        {".doc", "icon_word"},
        {".docx", "icon_word"},
        {".xls", "icon_excel"},
        {".xlsx", "icon_excel"},
        {".ppt", "icon_power_point"},
        {".pptx", "icon_power_point"},
        {".odt", "icon_libre_writer"}
    };
    
    public static ImageIcon createIcon(Path path, int imageSize) {
        
        ImageIcon icon = null;
        
        String fileName = path.getFileName().toString();
        boolean isImage = isImageFile(fileName);
        
        if (isImage) {
            try {
                InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
                BufferedImage image = ImageIO.read(is);
                icon = createThumbnail(image, imageSize);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        } else {
            icon = getDefaultIcon(path.toFile());
        }
        
        return icon;
    }

    public static ImageIcon createIcon(File file, int imageSize) {

        ImageIcon icon = null;

        String fileName = file.getName();
        boolean isImage = isImageFile(fileName);
        
        if (isImage) {
            try {
                BufferedImage image = ImageIO.read(file);
                icon = createThumbnail(image, imageSize);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        } else {
            icon = getDefaultIcon(file);
        }

        return icon;
    }
    
    private static boolean isImageFile(String fileName) {
        
        fileName = fileName.toLowerCase();
        boolean isImage = false;
        
        for (String ext : IMAGE_FILE_EXTS) {
            if (fileName.endsWith(ext)) {
                isImage = true;
                break;
            }
        }
        
        return isImage;
    }
    
    private static ImageIcon createThumbnail(BufferedImage image, int imageSize) {
        ImageIcon icon = null;
        if (image != null) {
            image = ImageHelper.getFirstScaledInstance(image, imageSize);
            icon = new ImageIcon(image);
        }
        return icon;
    }
    
    private static ImageIcon getDefaultIcon(File file) {

        ImageIcon icon = null;
        if (file.isDirectory()) {
            icon = ClientContext.getImageIconAlias(FOLDER_ICON);
            return icon;
        }
        
        String fileName = file.getName().toLowerCase();
        
        for (String[] pair : EXT_ICON_PAIRS) {
            if (fileName.endsWith(pair[0])) {
                icon = ClientContext.getImageIconAlias(pair[1]);
                return icon;
            }
        }
/*
        try {
            //@SuppressWarnings("all")
            Image img = sun.awt.shell.ShellFolder.getShellFolder(file).getIcon(true);
            icon = new ImageIcon(img);
        } catch (FileNotFoundException ex) {
        }
*/
        if (icon == null) {
            icon = ClientContext.getImageIconAlias(DEFAULT_DOC_ICON);
        }
        
        return icon;
    }
}
