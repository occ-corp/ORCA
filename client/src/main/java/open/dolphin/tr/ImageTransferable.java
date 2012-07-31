
package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Imageのtransferable
 *
 * @author masuda, Masuda Naika
 */
public class ImageTransferable implements Transferable {

    private BufferedImage image;
    private DataFlavor[] flavor;

    public ImageTransferable(BufferedImage image) {
        this.image = image;
        flavor = new DataFlavor[]{DataFlavor.imageFlavor};
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavor;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return this.flavor[0].equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (!this.flavor[0].equals(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return image;
    }
}
