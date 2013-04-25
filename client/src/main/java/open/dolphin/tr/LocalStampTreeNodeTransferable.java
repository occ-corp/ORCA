package open.dolphin.tr;

import java.awt.datatransfer.*;
import java.io.IOException;
import open.dolphin.stampbox.StampTreeNode;

/**
 * Tranferable class of the StampTreeNode.
 *
 * @author  Kazushi Minagawa
 */
public class LocalStampTreeNodeTransferable extends DolphinTransferable {

    /** Data Flavor of this class */
    public static DataFlavor localStampTreeNodeFlavor 
            = new DataFlavor(StampTreeNode.class, "LocalStampTreeNode");

    public static final DataFlavor[] flavors = {LocalStampTreeNodeTransferable.localStampTreeNodeFlavor};

    private StampTreeNode node;

    /** Creates new StampTreeTransferable */
    public LocalStampTreeNodeTransferable(StampTreeNode node) {
        this.node = node;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return localStampTreeNodeFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
	    throws UnsupportedFlavorException, IOException {

        if (flavor.equals(localStampTreeNodeFlavor)) {
            return node;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
    
    @Override
    public String toString() {
        return "LocalStampTreeNode Transferable";
    }
}