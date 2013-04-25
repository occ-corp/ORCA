package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JTable;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.table.ListTableModel;


/**
 * RegisteredDiagnosisTransferHandler
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika, from 1.4
 */
public class RegisteredDiagnosisTransferHandler extends DolphinTransferHandler {

    private DataFlavor registeredDiagnosisFlavor = RegisteredDiagnosisTransferable.registeredDiagnosisFlavor;

    private int fromIndex;
    private int toIndex;

    @Override
    protected Transferable createTransferable(JComponent src) {
        
        startTransfer(src);
        
        JTable sourceTable = (JTable) src;
        ListTableModel<RegisteredDiagnosisModel> tableModel = (ListTableModel<RegisteredDiagnosisModel>) sourceTable.getModel();
        fromIndex = sourceTable.getSelectedRow();
        RegisteredDiagnosisModel rd = tableModel.getObject(fromIndex);
        if (rd != null) {
            return new RegisteredDiagnosisTransferable(rd);
        }
        
        endTransfer();
        return null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    public boolean importData(TransferSupport support) {
        
        if (!canImport(support)) {
            importDataFailed();
            return false;
        }

        try {
            JTable dropTable = (JTable) support.getComponent();

            ListTableModel<RegisteredDiagnosisModel> tableModel = (ListTableModel<RegisteredDiagnosisModel>) dropTable.getModel();
            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
            toIndex = dropLocation.getRow();
            if (dropTable == srcComponent) {
                tableModel.moveRow(fromIndex, (toIndex > fromIndex) ? --toIndex : toIndex);
            } else {
                RegisteredDiagnosisModel rd = (RegisteredDiagnosisModel) 
                        support.getTransferable().getTransferData(registeredDiagnosisFlavor);
                tableModel.addObject(toIndex, rd);
            }
            importDataSuccess(dropTable);
            return true;

        } catch (Exception ioe) {
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
        
        JTable sourceTable = (JTable) c;
        sourceTable.getSelectionModel().setSelectionInterval(toIndex, toIndex);
        
        fromIndex = -1;
        toIndex = -1;
        endTransfer();
    }

    @Override
    public boolean canImport(TransferSupport support) {

        if (!support.isDrop()) {
            return false;
        }
        if (support.isDataFlavorSupported(registeredDiagnosisFlavor)) {
            return true;
        }
        return false;
    }
}
