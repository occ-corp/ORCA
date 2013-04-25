package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import open.dolphin.infomodel.RegisteredDiagnosisModel;

/**
 * 疾患 Transferable クラス。
 * @author Kazushi Minagawa.
 */
public final class RegisteredDiagnosisTransferable extends DolphinTransferable {

    public static final DataFlavor registeredDiagnosisFlavor 
            = new DataFlavor(RegisteredDiagnosisModel.class, "RegisteredDiagnosis");
    public static final DataFlavor[] flavors = {registeredDiagnosisFlavor};
    private RegisteredDiagnosisModel diagnosis;

    public RegisteredDiagnosisTransferable(RegisteredDiagnosisModel diagnosis) {
        this.diagnosis = diagnosis;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return registeredDiagnosisFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {

        if (flavor.equals(registeredDiagnosisFlavor)) {
            return diagnosis;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
    @Override
    public String toString() {
        return "RegisteredDiagnosis Transferable";
    }
}
