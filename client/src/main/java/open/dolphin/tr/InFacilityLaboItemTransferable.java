
package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import open.dolphin.infomodel.InFacilityLaboItem;

/**
 * InFacilityLaboItemTransferable
 * 
 * @author masuda, Masuda Naka
 */
public class InFacilityLaboItemTransferable implements Transferable {
    
    public static DataFlavor inFacilityLaboItemFlavor = new DataFlavor(InFacilityLaboItem.class, "InFacilityLaboItem");

    public static final DataFlavor[] flavors = {InFacilityLaboItemTransferable.inFacilityLaboItemFlavor};

    private InFacilityLaboItem[] items;
    
    public InFacilityLaboItemTransferable(InFacilityLaboItem[] items) {
        this.items = items;
    }
    
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return inFacilityLaboItemFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        
        if (flavor.equals(inFacilityLaboItemFlavor)) {
            return items;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
