package open.dolphin.tr;

import java.awt.datatransfer.*;
import java.io.IOException;
import open.dolphin.client.ImageEntry;

/**
 * Transferable class of the ImageIcon.
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public final class ImageEntryTransferable extends DolphinTransferable {

    /** Data Flavor of this class */
    public static DataFlavor imageEntryFlavor = new DataFlavor(ImageEntry.class, "Image Entry");

    public static final DataFlavor[] flavors = {ImageEntryTransferable.imageEntryFlavor};

    private ImageEntry entry;

    /** Creates new ImgeIconTransferable */
    public ImageEntryTransferable(ImageEntry entry) {
        this.entry = entry;
    }

    @Override
    public synchronized DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return imageEntryFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {

        if (flavor.equals(imageEntryFlavor)) {
            return entry;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    @Override
    public String toString() {
        return "ImageEntry Transferable";
    }
}