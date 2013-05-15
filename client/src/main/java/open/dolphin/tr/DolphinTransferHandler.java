package open.dolphin.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY_OR_MOVE;
import javax.swing.text.Position;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

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
    
    // ComponentからBufferedImageを作成する
    protected BufferedImage createComponentImage(Component c) {
        Rectangle r = c.getBounds();
        BufferedImage image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();
        c.paint(g2d);
        g2d.dispose();
        return image;
    }
    
    // 選択中のTreeNodeのBufferedImageを作成する。pns先生のコードを拝借
    protected Image createNodeImage(JTree tree) {

        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }

        TreeCellRenderer renderer = tree.getCellRenderer();
        TreeNode node = (TreeNode) tree.getLastSelectedPathComponent();
        boolean selected = false;
        boolean expanded = tree.isExpanded(path);
        boolean isLeaf = node.isLeaf();
        int row = tree.getRowForPath(path);
        boolean hasFocus = true;

        Component c = renderer.getTreeCellRendererComponent(
                tree, node, selected, expanded, isLeaf, row, hasFocus);
        c.setSize(c.getPreferredSize());    // これ大事
        
        Image image = createComponentImage(c);

        return image;
    }
}
