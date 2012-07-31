
package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import open.dolphin.infomodel.InFacilityLaboItem;
import open.dolphin.table.ListTableModel;

/**
 * 院内検査項目のTransferHandler
 * 
 * @author masuda, Masuda Naika
 */
public class InFacilityLaboTransferHandler extends TransferHandler {

    private DataFlavor inFacilityLaboItem = InFacilityLaboItemTransferable.inFacilityLaboItemFlavor;
    
    private boolean editable;
    
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
    
    
    @Override
    protected Transferable createTransferable(JComponent c) {
        JTable sourceTable = (JTable) c;

        @SuppressWarnings("unchecked")
        ListTableModel<InFacilityLaboItem> tableModel = (ListTableModel<InFacilityLaboItem>) sourceTable.getModel();
        int[] selectedRows = sourceTable.getSelectedRows();
        List<InFacilityLaboItem> list = new ArrayList<InFacilityLaboItem>();
        for (int row : selectedRows) {
            list.add(tableModel.getObject(row));
        }
        if (list.isEmpty()) {
            return null;
        }
        return new InFacilityLaboItemTransferable(list.toArray(new InFacilityLaboItem[0])); 
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }
    
    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        
        if (!canImport(support)) {
            return false;
        }
        if (!editable) {
            return false;
        }

        try {
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            int toIndex = dl.getRow();
            if (dl.isInsertRow() && toIndex > -1) {
                Transferable t = support.getTransferable();
                InFacilityLaboItem[] dropItems = (InFacilityLaboItem[]) t.getTransferData(inFacilityLaboItem);
                JTable dropTable = (JTable) support.getComponent();
                
                @SuppressWarnings("unchecked")
                ListTableModel<InFacilityLaboItem> tableModel = (ListTableModel<InFacilityLaboItem>) dropTable.getModel();

                for (InFacilityLaboItem dropItem : dropItems) {
                    if (toIndex < tableModel.getObjectCount()) {
                        tableModel.addObject(toIndex, dropItem);
                    } else {
                        tableModel.addObject(dropItem);
                    }
                    toIndex++;
                }
                return true;
            }
        } catch (Exception ioe) {
            ioe.printStackTrace(System.err);
        }
        
        return false;
    }
    
    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        
        if (!editable) {
            return;
        }
        try {
            if (action == MOVE) {
                JTable sourceTable = (JTable) c;
                InFacilityLaboItem[] dropItems = (InFacilityLaboItem[]) data.getTransferData(inFacilityLaboItem);
                @SuppressWarnings("unchecked")
                ListTableModel<InFacilityLaboItem> tableModel = (ListTableModel<InFacilityLaboItem>) sourceTable.getModel();
                for (InFacilityLaboItem item : dropItems) {
                    tableModel.delete(item);
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return support.isDrop() && support.isDataFlavorSupported(inFacilityLaboItem);
    }
    
}
