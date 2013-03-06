
package open.dolphin.util;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author masuda, Masuda Naika
 */
public class ImageTool {
    
    public static byte[] getJpegBytes(byte[] bytes, Dimension dim) {
        try {
            BufferedImage src =ImageIO.read(new ByteArrayInputStream(bytes));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(getFirstScaledInstance(src, dim), "jpeg", bos);
            return bos.toByteArray();
        } catch (IOException ex) {
        }
        return null;
    }

    private static BufferedImage getFirstScaledInstance(BufferedImage inImage, Dimension dim) {

        if (inImage.getWidth() <= dim.width && inImage.getHeight() <= dim.height) {
            return inImage;
        }

        // Determine the scale.
        double scaleH = (double) dim.height / (double) inImage.getHeight();
        double scaleW = (double) dim.width / (double) inImage.getWidth();
        double scale = Math.min(scaleH, scaleW);

        // Determine size of new image.
        int scaledW = (int) (scale * (double) inImage.getWidth());
        int scaledH = (int) (scale * (double) inImage.getHeight());

        // Create an image buffer in which to paint on.
        BufferedImage outImage = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_BGR);

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

        return outImage;
    }
}
