package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import open.dolphin.order.MasterItem;
import open.dolphin.table.ListTableModel;


/**
 * MasterItemTransferHandler
 *
 * @author Minagawa,Kazushi. Digital Globe, Inc.
 *
 */
public final class MasterItemTransferHandler extends DolphinTransferHandler {
    
    private DataFlavor masterItemFlavor = MasterItemTransferable.masterItemFlavor;
    
    @Override
    protected Transferable createTransferable(JComponent src) {
        
        startTransfer(src);
        
        JTable sourceTable = (JTable) src;
        ListTableModel<MasterItem> tableModel = (ListTableModel<MasterItem>) sourceTable.getModel();
        int fromIndex = sourceTable.getSelectedRow();
        MasterItem mi = tableModel.getObject(fromIndex);
        if (mi != null) {
            return new MasterItemTransferable(mi);
        } else {
            endTransfer();
            return null;
        }
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }
    
    @Override
    public boolean importData(TransferSupport support) {
        
        if (!canImport(support)) {
            importDataFailed();
            return false;
        }

        try {
            JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
            int toIndex = dl.getRow();
            if (dl.isInsertRow() && toIndex>-1) {
                Transferable t = support.getTransferable();
                MasterItem dropItem = (MasterItem) t.getTransferData(masterItemFlavor);
                JTable dropTable = (JTable) support.getComponent();
                
                ListTableModel<MasterItem> tableModel = (ListTableModel<MasterItem>) dropTable.getModel();

                if (toIndex<tableModel.getObjectCount()) {
                    tableModel.addObject(toIndex, dropItem);
                } else {
                    tableModel.addObject(dropItem);
                }
                
                importDataSuccess(support.getComponent());
                return true;
            }
        } catch (Exception ioe) {
            ioe.printStackTrace(System.err);
        }
        
        importDataFailed();
        return false;
    }
    
    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        
        // export先がOpenDolphin以外なら削除しない
        if (isExportToOther()) {
            endTransfer();
            return;
        }

        boolean shouldRemove = (c == srcComponent);
        
        if (action == MOVE && shouldRemove) {
            JTable src = (JTable) c;
            ListTableModel<MasterItem> tableModel = (ListTableModel<MasterItem>) src.getModel();
            try {
                MasterItem mi = (MasterItem) data.getTransferData(masterItemFlavor);
                tableModel.delete(mi);
            } catch (UnsupportedFlavorException ex) {
            } catch (IOException ex) {
            }
        }
        endTransfer();
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return (support.isDrop() && support.isDataFlavorSupported(masterItemFlavor));
    }
}
