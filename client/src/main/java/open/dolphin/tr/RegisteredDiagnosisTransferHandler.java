package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.table.ListTableModel;


/**
 * RegisteredDiagnosisTransferHandler
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika, from 1.4
 */
public class RegisteredDiagnosisTransferHandler extends TransferHandler {

    private DataFlavor registeredDiagnosisFlavor = RegisteredDiagnosisTransferable.registeredDiagnosisFlavor;

    private JTable sourceTable;
    private boolean shouldRemove;
    private int fromIndex;
    private int toIndex;


    @Override
    @SuppressWarnings("unchecked")
    protected Transferable createTransferable(JComponent c) {
        
        sourceTable = (JTable) c;
        ListTableModel<RegisteredDiagnosisModel> tableModel = (ListTableModel<RegisteredDiagnosisModel>) sourceTable.getModel();
        fromIndex = sourceTable.getSelectedRow();
        RegisteredDiagnosisModel dragItem = tableModel.getObject(fromIndex);
        return dragItem != null ? new RegisteredDiagnosisTransferable(dragItem) : null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferSupport support) {
        
        if (!canImport(support)) {
            return false;
        }

        try {
            JTable dropTable = (JTable) support.getComponent();

            ListTableModel<RegisteredDiagnosisModel> tableModel = (ListTableModel<RegisteredDiagnosisModel>) dropTable.getModel();
            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
            toIndex = dropLocation.getRow();
            shouldRemove = (dropTable == sourceTable);
            if (shouldRemove) {
                tableModel.moveRow(fromIndex, (toIndex > fromIndex) ? --toIndex : toIndex);
            }
            sourceTable.getSelectionModel().setSelectionInterval(toIndex, toIndex);
            return true;

        } catch (Exception ioe) {
        }
        
        return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {

        shouldRemove = false;
        fromIndex = -1;
        toIndex = -1;
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
