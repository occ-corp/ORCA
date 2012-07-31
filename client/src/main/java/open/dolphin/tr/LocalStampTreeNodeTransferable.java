package open.dolphin.tr;

import java.awt.datatransfer.*;
import java.io.IOException;
import open.dolphin.stampbox.StampTreeNode;

/**
 * Tranferable class of the StampTreeNode.
 *
 * @author  Kazushi Minagawa
 */
public class LocalStampTreeNodeTransferable implements Transferable, ClipboardOwner {

    /** Data Flavor of this class */
    public static DataFlavor localStampTreeNodeFlavor;

    static {
        try {
//masuda^   StampTreeNodeを移動させたので、余計な手間ｗ
            //localStampTreeNodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=open.dolphin.client.StampTreeNode");
            localStampTreeNodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + StampTreeNode.class.getName());
//masuda$
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    ;

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
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        AbstractKarteTransferHandler.clearVariables();
    }
}