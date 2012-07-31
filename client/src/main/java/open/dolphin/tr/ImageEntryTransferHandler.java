
package open.dolphin.tr;

import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.TransferHandler.TransferSupport;
import open.dolphin.client.ImageEntry;
import open.dolphin.client.ImageLabel;
import open.dolphin.client.ImagePanel;

/**
 * ImageLabelTransferHandler
 * 内容はImageEntryTransferable
 *
 * @author masuda, Masuda Naika
 */
public class ImageEntryTransferHandler extends AbstractImagePanelTransferHandler {
    
    private static ImageEntryTransferHandler instance;

    static {
        instance = new ImageEntryTransferHandler();
    }

    private ImageEntryTransferHandler() {
    }
    
    public static ImageEntryTransferHandler getInstance() {
        return instance;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        ImagePanel imagePanel = (ImagePanel) c;
        ImageLabel imageLabel = imagePanel.getSelectedImageLabel();
        if (imageLabel == null) {
            return null;
        }
        ImageEntry entry = imageLabel.getImageEntry();
        Transferable tr = new ImageEntryTransferable(entry);
        return tr;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return false;
    }

    @Override
    public boolean importData(TransferSupport support) {
        return false;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void maybeShowPopup(MouseEvent e) {
    }
}
