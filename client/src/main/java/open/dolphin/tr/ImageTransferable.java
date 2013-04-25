package open.dolphin.tr;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Imageのtransferable
 *
 * @author masuda, Masuda Naika
 */
public class ImageTransferable extends DolphinTransferable {

    private Image image;
    private DataFlavor[] flavor;

    public ImageTransferable(Image image) {
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
    
    @Override
    public String toString() {
        return "java.awt.Image Transferable";
    }
}
