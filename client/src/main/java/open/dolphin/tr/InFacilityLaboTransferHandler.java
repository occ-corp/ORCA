package open.dolphin.tr;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import open.dolphin.infomodel.InFacilityLaboItem;
import open.dolphin.table.ListTableModel;

/**
 * 院内検査項目のTransferHandler
 * 
 * @author masuda, Masuda Naika
 */
public class InFacilityLaboTransferHandler extends DolphinTransferHandler {

    private DataFlavor inFacilityLaboItem = InFacilityLaboItemTransferable.inFacilityLaboItemFlavor;
    
    private boolean editable;
    
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
    
    @Override
    protected Transferable createTransferable(JComponent src) {
        
        startTransfer(src);
        JTable sourceTable = (JTable) src;

        ListTableModel<InFacilityLaboItem> tableModel = (ListTableModel<InFacilityLaboItem>) sourceTable.getModel();
        int[] selectedRows = sourceTable.getSelectedRows();
        int size = selectedRows.length;
        if (size == 0) {
            endTransfer();
            return null;
        }
        
        InFacilityLaboItem[] items = new InFacilityLaboItem[size];
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            InFacilityLaboItem item = tableModel.getObject(selectedRows[i]);
            items[i] = item;
            strList.add(item.getItemName());
        }

        // ドラッグ中のイメージを設定する
        Image image = createDragImage(strList, sourceTable.getFont());
        setDragImage(image);
        
        return new InFacilityLaboItemTransferable(items); 
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
        if (!editable) {
            importDataFailed();
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
        
        // export先がOpenDolphin以外なら削除しない
        if (isExportToOther()) {
            endTransfer();
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
        
        endTransfer();
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && support.isDataFlavorSupported(inFacilityLaboItem);
    }
    
}
