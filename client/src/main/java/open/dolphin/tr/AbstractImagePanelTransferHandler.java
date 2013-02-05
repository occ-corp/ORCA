
package open.dolphin.tr;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import open.dolphin.client.GUIConst;

/**
 * AbstractImagePanelTransferHandler
 * イメージパネルのTransferHandlerの抽象クラス
 *
 * @author masuda, Masuda Naika
 */
public abstract class AbstractImagePanelTransferHandler extends TransferHandler implements IKarteTransferHandler{

    public abstract void mouseClicked(MouseEvent e);

    public abstract void maybeShowPopup(MouseEvent e);

    @Override
    protected abstract Transferable createTransferable(JComponent c);

    @Override
    public abstract boolean canImport(TransferSupport support);

    @Override
    public abstract boolean importData(TransferSupport support);

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        Transferable tr = createTransferable(comp);
        clip.setContents(tr, null);
    }

    @Override
    public void enter(JComponent jc, ActionMap map) {
        map.get(GUIConst.ACTION_COPY).setEnabled(true);
        map.get(GUIConst.ACTION_CUT).setEnabled(false);
        map.get(GUIConst.ACTION_PASTE).setEnabled(false);
    }

    @Override
    public void exit(JComponent jc) {
    }
}
