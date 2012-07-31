package open.dolphin.tr;

import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * Transferable class of the Icon list.
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public final class SchemaListTransferable implements Transferable, ClipboardOwner {

    /** Data Flavor of this class */
    public static DataFlavor schemaListFlavor = new DataFlavor(open.dolphin.tr.SchemaList.class, "Schema List");

    public static final DataFlavor[] flavors = {SchemaListTransferable.schemaListFlavor};

    private SchemaList list;

    /** Creates new SchemaListTransferable */
    public SchemaListTransferable(SchemaList list) {
        this.list = list;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
	return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return schemaListFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
	    throws UnsupportedFlavorException, IOException {

        if (flavor.equals(schemaListFlavor)) {
            return list;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    @Override
    public String toString() {
        return "Icon List Transferable";
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        AbstractKarteTransferHandler.clearVariables();
    }
}