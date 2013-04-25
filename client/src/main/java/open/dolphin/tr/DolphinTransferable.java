package open.dolphin.tr;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;

/**
 * OpenDolphinで使うTransferableの基底クラス
 * @author masuda, Masuda Naika
 */
public abstract class  DolphinTransferable implements Transferable, ClipboardOwner {
    
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        DolphinTransferHandler.endTransfer();
    }
}
