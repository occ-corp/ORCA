package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import open.dolphin.stampbox.StampTree;
import open.dolphin.stampbox.StampTreeNode;

/**
 * AspStampTreeTransferHandler
 *
 * @author Minagawa,Kazushi
 *
 */
public class AspStampTreeTransferHandler extends DolphinTransferHandler {

    @Override
    protected Transferable createTransferable(JComponent c) {
        startTransfer(c);
        StampTree sourceTree = (StampTree) c;
        StampTreeNode dragNode = (StampTreeNode) sourceTree.getLastSelectedPathComponent();
        return new LocalStampTreeNodeTransferable(dragNode);
    }

    @Override
    public boolean importData(JComponent c, Transferable tr) {
        importDataFailed();
        return false;
    }

    @Override
    public boolean canImport(JComponent c, DataFlavor[] flavors) {
        return false;
    }
}