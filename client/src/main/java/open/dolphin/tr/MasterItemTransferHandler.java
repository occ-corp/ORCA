package open.dolphin.tr;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JTable;
import open.dolphin.order.MasterItem;
import open.dolphin.table.ListTableModel;


/**
 * MasterItemTransferHandler
 *
 * @author Minagawa,Kazushi. Digital Globe, Inc.
 *
 */
public final class MasterItemTransferHandler extends DolphinTransferHandler {
    
    private static final DataFlavor FLAVOR = MasterItemTransferable.masterItemFlavor;
    private int fromIndex;
    
    @Override
    protected Transferable createTransferable(JComponent src) {
        
        startTransfer(src);
        
        JTable sourceTable = (JTable) src;
        ListTableModel<MasterItem> tableModel = (ListTableModel<MasterItem>) sourceTable.getModel();
        fromIndex = sourceTable.getSelectedRow();
        MasterItem mi = tableModel.getObject(fromIndex);
        if (mi != null) {
            // ドラッグ中のイメージを設定する
            Image image = createStringImage(mi.getName(), sourceTable.getFont());
            setDragImage(image);
            return new MasterItemTransferable(mi);
        }
        
        endTransfer();
        return null;
    }
    
    @Override
    public boolean importData(TransferSupport support) {
        
        JTable dropTable = (JTable) support.getComponent();
        if (!canImport(support) || dropTable != srcComponent) {
            importDataFailed();
            return false;
        }

        try {
            ListTableModel<MasterItem> tableModel = (ListTableModel<MasterItem>) dropTable.getModel();
            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
            int toIndex = dropLocation.getRow();
            tableModel.moveRow(fromIndex, (toIndex > fromIndex) ? --toIndex : toIndex);
            dropTable.getSelectionModel().setSelectionInterval(toIndex, toIndex);
            importDataSuccess(dropTable);
            return true;

        } catch (Exception ioe) {
            importDataFailed();
            return false;
        }
    }
    
    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        fromIndex = -1;
        endTransfer();
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }
    
    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() 
                && support.isDataFlavorSupported(FLAVOR);
    }
}
