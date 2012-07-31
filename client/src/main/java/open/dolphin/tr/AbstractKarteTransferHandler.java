
package open.dolphin.tr;

import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.io.IOException;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import open.dolphin.client.FocusPropertyChangeListener;

/**
 * AbstractKarteTransferHandler
 * 
 * @author masuda, Masuda Naika
 */
public abstract class AbstractKarteTransferHandler extends TransferHandler implements IKarteTransferHandler{

    protected static final DataFlavor stringFlavor = DataFlavor.stringFlavor;
    protected static Component srcComponent;
    protected static Component destComponent;
    protected static Position startPos;
    protected static Position endPos;

    protected static final int SHORTCUTKEY_DOWN_MASK =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.CTRL_MASK
            ? InputEvent.CTRL_DOWN_MASK     // windows
            : InputEvent.META_DOWN_MASK;    // mac

    @Override
    public abstract void enter(JComponent jc, ActionMap map);

    @Override
    public abstract void exit(JComponent jc);

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {

        // 違うComponent間なら削除しない
        if (srcComponent != destComponent) {
            return;
        }

        JTextComponent tc = (JTextComponent) c;
        if (action != MOVE || !tc.isEditable()) {
            return;
        }

        if (startPos == null || endPos == null) {
            return;
        }
        if (startPos.getOffset() == endPos.getOffset()) {
            return;
        }

        // Drag元から選択中のテキストを削除する
        try {
            int start = startPos.getOffset();
            int end = endPos.getOffset();
            tc.getDocument().remove(start, end - start);
        } catch (BadLocationException e) {
        }
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

    public static void clearVariables() {
        srcComponent =null;
        destComponent = null;
        startPos = null;
        endPos = null;
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
