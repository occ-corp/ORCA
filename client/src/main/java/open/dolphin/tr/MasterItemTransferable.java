package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import open.dolphin.order.MasterItem;

/**
 * マスタアイテム Transferable クラス。
 * @author Kazushi Minagawa.
 */
public final class MasterItemTransferable extends DolphinTransferable {

    public static final DataFlavor masterItemFlavor = new DataFlavor(MasterItem.class, "MasterItem");
    public static final DataFlavor[] flavors = {masterItemFlavor};
    private MasterItem masterItem;

    public MasterItemTransferable(MasterItem masterItem) {
        this.masterItem = masterItem;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return masterItemFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {

        if (flavor.equals(masterItemFlavor)) {
            return masterItem;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
    
    @Override
    public String toString() {
        return "MasterItem Transferable";
    }
}
