package open.dolphin.tr;

import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import open.dolphin.client.FocusPropertyChangeListener;
import open.dolphin.client.KartePane;
import open.dolphin.client.KarteStyledDocument;

/**
 * AbstractKarteTransferHandler
 * 
 * @author masuda, Masuda Naika
 */
public abstract class AbstractKarteTransferHandler extends DolphinTransferHandler implements IKarteTransferHandler{

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        
        // export先がOpenDolphin以外なら削除しない
        if (isExportToOther()) {
            endTransfer();
            return;
        }
        
        // 違うComponent間なら削除しない
        if (srcComponent != destComponent) {
            endTransfer();
            return;
        }

        JTextComponent tc = (JTextComponent) c;
        if (action != MOVE || !tc.isEditable()) {
            endTransfer();
            return;
        }

        if (startPos == null || endPos == null) {
            endTransfer();
            return;
        }
        
        if (startPos.getOffset() == endPos.getOffset()) {
            endTransfer();
            return;
        }

        // Drag元から選択中のテキストを削除する
        try {
            int start = startPos.getOffset();
            int end = endPos.getOffset();
            tc.getDocument().remove(start, end - start);
        } catch (BadLocationException e) {
        }
        endTransfer();
    }

    /**
     * クリップボードへデータを転送する。
     */
    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        super.exportToClipboard(comp, clip, action);
        // cut の場合を処理する
        if (action == MOVE) {
            JTextComponent pane = (JTextComponent) comp;
            if (pane.isEditable()) {
                pane.replaceSelection("");
            }
        }
    }
    
    protected final KartePane getKartePane(JTextComponent tc) {
        KarteStyledDocument doc = (KarteStyledDocument) tc.getDocument();
        return doc.getKartePane();
    }
    
    protected final boolean setSelectedTextArea(JTextComponent tc) {

        Document doc = tc.getDocument();
        int start = tc.getSelectionStart();
        int end = tc.getSelectionEnd();
        if (start == end) {
            return false;
        }
        try {
            startPos = doc.createPosition(start);
            endPos = doc.createPosition(end);
        } catch (BadLocationException e) {
        }
        return true;
    }

    protected final boolean isDndOntoSelectedText(TransferSupport support) {

        // DnDでないならばfalse
        if (!support.isDrop()) {
            return false;
        }
        if (startPos == null || endPos == null) {
            return false;
        }
        JTextComponent tc = (JTextComponent) support.getComponent();
        //support.setShowDropLocation(true);
        Point p = support.getDropLocation().getDropPoint();
        int caretPos = tc.viewToModel(p);
        // 選択範囲内にDnDならtrue
        if (tc == srcComponent && caretPos >= startPos.getOffset() && caretPos <= endPos.getOffset()) {
            return true;
        }
        return false;
    }

    // テキストをインポートする
    protected final boolean doTextDrop(Transferable tr, JTextComponent tc) {

        try {
            String str = (String) tr.getTransferData(DataFlavor.stringFlavor);
            tc.replaceSelection(str);
            // 他アプリからだとMOVEとしない。srcComponent = nullである。
            boolean toRemove = srcComponent != null;
            return toRemove;
        } catch (UnsupportedFlavorException ex) {
        } catch (IOException ex) {
        }
        return false;
    }

    // modifiersExを返す
    protected final int getModifiersEx() {
        int modifiersEx = FocusPropertyChangeListener.getInstance().getModifiersEx();
        return modifiersEx;
    }
}
