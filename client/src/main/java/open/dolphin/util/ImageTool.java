package open.dolphin.util;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import open.dolphin.client.ClientContext;
import open.dolphin.client.ImageEntry;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.infomodel.SchemaModel;
import open.dolphin.tr.ImageTransferable;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.imageio.plugins.dcm.DicomImageReadParam;
import org.dcm4che2.io.DicomOutputStream;

/**
 * ImageTool.java
 * 画像関連の諸々
 *
 * @author masuda, Masuda Naika
 */
public class ImageTool {

    private static final int MAX_IMAGE_WIDTH = ClientContext.getInt("image.max.width");
    private static final int MAX_IMAGE_HEIGHT = ClientContext.getInt("image.max.height");
    public static final Dimension MAX_IMAGE_SIZE = new Dimension(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
    public static final Dimension MAX_ICON_SIZE = new Dimension(120, 120);

    // DicomObjectからBufferedImageを作成
    public static BufferedImage getDicomImage(DicomObject obj) throws IOException {

        byte[] dicomBytes = toByteArray(obj);
        BufferedImage image = getPixelDataAsBufferedImage(dicomBytes);
        return image;
    }

    // http://forums.dcm4che.org/jiveforums/thread.jspa?threadID=2611&tstart=-1
    private static byte[] toByteArray(DicomObject obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        DicomOutputStream dos = new DicomOutputStream(bos);
        dos.writeDicomFile(obj);
        dos.close();
        byte[] data = baos.toByteArray();
        return data;
    }

    private static BufferedImage getPixelDataAsBufferedImage(byte[] dicomData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(dicomData);
        Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
        ImageReader reader = iter.next();
        DicomImageReadParam param = (DicomImageReadParam) reader.getDefaultReadParam();
        ImageInputStream iis = ImageIO.createImageInputStream(bais);
        reader.setInput(iis, false);
        BufferedImage buff = reader.read(0, param);
        iis.close();
        if (buff == null) {
            throw new IOException("Could not read Dicom file. Maybe pixel data is invalid.");
        }
        return buff;
    }

    // ファイルからイメージエントリーを作成する
    public static ImageEntry getImageEntryFromFile(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        ImageEntry entry = (ImageEntry) prepareImageEntry(image);
        entry.setUrl(file.toURI().toString());
        entry.setPath(file.getPath());
        entry.setFileName(file.getName());
        return entry;
    }

    // java.awt.ImageからImageEntryを作成（クリップボードからのペースト時に使用）
    public static ImageEntry getImageEntryFromImage(Image image) throws IOException {
        BufferedImage buf = createBufferedImage(image);
        ImageEntry entry = (ImageEntry) prepareImageEntry(buf);
        return entry;
    }
    
    /**
     * シェーマモデルをエントリに変換する。
     * @param schema シェーマモデル
     * @param iconSize アイコンのサイズ
     * @return ImageEntry
     */
    public static ImageEntry getImageEntryFromSchema(SchemaModel schema, Dimension iconSize) {

        ImageEntry model = new ImageEntry();

        model.setId(schema.getId());
        model.setConfirmDate(ModelUtils.getDateTimeAsString(schema.getConfirmed()));  // First?
        model.setContentType(schema.getExtRefModel().getContentType());
        model.setTitle(schema.getExtRefModel().getTitle());
        model.setMedicalRole(schema.getExtRefModel().getMedicalRole());

        byte[] bytes = schema.getJpegByte();

        // Create ImageIcon
        ImageIcon icon = new ImageIcon(bytes);
        if (icon != null) {
            model.setImageIcon(adjustImageSize(icon, iconSize));
        }

        return model;
    }
    
    // DicomObjectからDicomImageEntryを作成
    public static DicomImageEntry getImageEntryFromDicom(DicomObject object) throws IOException {

        BufferedImage image = getDicomImage(object);
        DicomImageEntry entry = prepareImageEntry(image);
        // file nameの設定
        StringBuilder sb = new StringBuilder();
        sb.append(object.getString(Tag.PatientName));
        sb.append("_");
        sb.append(object.getString(Tag.StudyDate));
        entry.setFileName(sb.toString());
        // TitleにSOPInstanceUIDを設定しておく
        entry.setTitle(object.getString(Tag.SOPInstanceUID));
        // iconText
        entry.setIconText(entry.getFileName());
        
        return entry;
    }

    // ImageIconとjpegBytesなどを設定しておく
    private static DicomImageEntry prepareImageEntry(BufferedImage image) throws IOException {
        DicomImageEntry entry = new DicomImageEntry();
        BufferedImage iconImage = adjustImageSize(image, MAX_ICON_SIZE);
        ImageIcon icon = new ImageIcon();
        icon.setImage(iconImage);
        // Dolphin_ja.propertiesで設定されているmax.image.widthまで縮小して、編集用にjpegを別に保存しておく
        BufferedImage resized = adjustImageSize(image, MAX_IMAGE_SIZE);
        entry.setResizedJpegBytes(getJpegBytes(resized));

        entry.setNumImages(1);
        entry.setWidth(resized.getWidth());
        entry.setHeight(resized.getHeight());
        entry.setImageIcon(icon);
        return entry;
    }

    // BufferedImageをクリップボードに転送する
    public static void copyToClipboard(BufferedImage image) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        ImageTransferable it = new ImageTransferable(image);
        clip.setContents(it, null);
    }

    // Componentのハードコピーをクリップボードに転送する
    public static void copyToClipboard(Component cmp) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        ImageTransferable it = new ImageTransferable(getImageFromComponent(cmp));
        clip.setContents(it, null);
    }

    // ComponentからBufferedImageを作成する
    public static BufferedImage getImageFromComponent(Component cmp) {
        Rectangle d = cmp.getBounds();
        BufferedImage image = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = image.createGraphics();
        cmp.paint(g2d);
        g2d.dispose();
        return image;
    }

    // BufferedImageからjpegのbytesを作成する
    private static byte[] getJpegBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", bos);
        return bos.toByteArray();
    }
    
    // byte[]からBufferedImageを作る
    public static BufferedImage getBufferedImage(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
    
    // ImageからjpegのBytesを作成する
    public static byte[] getJpegBytes(Image image) throws IOException {
        BufferedImage bf = createBufferedImage(image);
        return getJpegBytes(bf);
    }

    // ImageIconのサイズを調節する
    public static ImageIcon adjustImageSize(ImageIcon icon, Dimension dim) {
        BufferedImage bf = createBufferedImage(icon.getImage());
        Image resized = getFirstScaledInstance(bf, dim);
        return new ImageIcon(resized);

    }

    // BufferedImage
    public static BufferedImage adjustImageSize(BufferedImage bf, Dimension dim) {
        return getFirstScaledInstance(bf, dim);
    }

/*  色がgetFirstScaledInstanceと違う。なんでかわからん
    private static Image adjustImageSize1(Image image, Dimension dim) {

        int imageHeight = image.getHeight(null);
        int imageWidth = image.getWidth(null);

        if (imageHeight > dim.height || imageWidth > dim.width) {
            float hRatio = (float) imageHeight / dim.height;
            float wRatio = (float) imageWidth / dim.width;
            int h, w;
            if (hRatio > wRatio) {
                h = dim.height;
                w = (int) (imageWidth / hRatio);
            } else {
                w = dim.width;
                h = (int) (imageHeight / wRatio);
            }
            Image resized = image.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return resized;
        } else {
            return image;
        }
    }
*/

    private static BufferedImage getFirstScaledInstance(BufferedImage inImage, Dimension dim) {

        if (inImage.getWidth() <= dim.width && inImage.getHeight() <= dim.height) {
            return inImage;
        }

        BufferedImage outImage = null;

        try {
            // Determine the scale.
            double scaleH = (double) dim.height / (double) inImage.getHeight();
            double scaleW = (double) dim.width  / (double) inImage.getWidth();
            double scale = Math.min(scaleH, scaleW);

            // Determine size of new image.
            int scaledW = (int) (scale * (double) inImage.getWidth());
            int scaledH = (int) (scale * (double) inImage.getHeight());

            // Create an image buffer in which to paint on.
            outImage = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_BGR);

            // Set the scale.
            AffineTransform tx = new AffineTransform();

            // If the image is smaller than the desired image size,
            if (scale < 1.0d) {
                tx.scale(scale, scale);
            }

            // Paint image.
            Graphics2D g2d = outImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(inImage, tx, null);
            g2d.dispose();

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return outImage;
    }

    private static BufferedImage createBufferedImage(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        BufferedImage bf = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D g = bf.createGraphics();
        g.setPaint(Color.WHITE);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bf;
	}
}
