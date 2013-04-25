package open.dolphin.tr;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY_OR_MOVE;
import javax.swing.text.Position;

/**
 * OpenDolphinで使うTransferHandlerの基底クラス
 * @author masuda, Masuda Naika
 */
public abstract class DolphinTransferHandler extends TransferHandler {
    
    protected static final DataFlavor stringFlavor = DataFlavor.stringFlavor;

    protected static final int SHORTCUTKEY_DOWN_MASK =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.CTRL_MASK
            ? InputEvent.CTRL_DOWN_MASK     // windows
            : InputEvent.META_DOWN_MASK;    // mac
    
    protected static Component srcComponent;
    protected static Component destComponent;
    protected static Position startPos;
    protected static Position endPos;

    private static void clearVariables() {
        srcComponent =null;
        destComponent = null;
        startPos = null;
        endPos = null;
    }
    
    public static void startTransfer(Component source) {
        clearVariables();
        srcComponent = source;
    }
    
    public static void importDataSuccess(Component dest) {
        destComponent = dest;
    }
    
    public static void importDataFailed() {
        destComponent = null;
    }
    
    public static void endTransfer() {
        clearVariables();
    }
    
    // OpenDolphin以外にExportしたか
    protected boolean isExportToOther() {
        
        if (destComponent instanceof JComponent) {
            TransferHandler tr = ((JComponent) destComponent).getTransferHandler();
            if (tr != null && tr instanceof DolphinTransferHandler) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }
    
    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        endTransfer();
    }
}
