package open.dolphin.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.text.Position;
import javax.swing.tree.DefaultTreeCellRenderer;
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
    protected static int modifiersEx;

    private static void clearVariables() {
        srcComponent =null;
        destComponent = null;
        startPos = null;
        endPos = null;
        modifiersEx = 0;
    }
    
    public static void setModifiersEx(int modifier) {
        modifiersEx = modifier;
    }
    
    public static int getModifiersEx() {
        return modifiersEx;
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
    
    
    // 文字列のDragImageを作成する
    protected Image createDragImage(String str, Font font) {
        try {
            JLabel lbl = new JLabel();
            lbl.setBorder(null);
            lbl.setFont(font);
            lbl.setText(str);
            return createComponentImage(lbl);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }
    
    protected Image createDragImage(List<String> strList, Font font) {
        try {
            int size = strList.size();
            JLabel[] labels = new JLabel[size];
            for (int i = 0; i < size; ++i) {
                JLabel lbl = new JLabel();
                lbl.setBorder(null);
                lbl.setFont(font);
                lbl.setText(strList.get(i));
                labels[i] = lbl;
            }
            return createComponentImage(labels);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }
    
    // StampTreeのDragImageを作成する
    protected Image createDragImage(JTree tree, TreeNode node) {
        
        try {
            TreeCellRenderer renderer = tree.getCellRenderer();
            TreePath path = tree.getSelectionPath();

            boolean selected = false;
            boolean expanded = tree.isExpanded(path);
            boolean isLeaf = node.isLeaf();
            int row = tree.getRowForPath(path);

            Component c = renderer.getTreeCellRendererComponent(tree,
                    node, selected, expanded, isLeaf, row, isLeaf);

            // ストライプがついているので消す
            if (c instanceof DefaultTreeCellRenderer) {
                ((DefaultTreeCellRenderer) c).setBackgroundNonSelectionColor(null);
            } else {
                c.setBackground(null);
            }

            return createComponentImage(c);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }
    
    // コンポーネントのDragImageを作成する
    protected Image createDragImage(Component[] components, double scale) {

        try {
            BufferedImage image = createComponentImage(components);
            return createResizedImage(image, scale);
        } catch (Exception ex) {
        }
        return null;
    }
    
    private BufferedImage createComponentImage(Component[] components) throws Exception {
        
        // 各ComponentのBufferedImageを準備する
        int width = 0;
        int height = 0;
        for (Component c : components) {
            Dimension d = c.getSize();
            if (d.width == 0 || d.height == 0) {
                d = c.getPreferredSize();
                c.setSize(d);
            }
            width = Math.max(width, d.width);
            height += d.height;
        }
        
        // Componentsを縦に並べて描画する
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();
        AffineTransform at = new AffineTransform();
        for (Component c : components) {
            g2d.setTransform(at);
            c.paint(g2d);
            at.translate(0, c.getHeight());
        }
        g2d.dispose();
        
        return image;
    }
    
    // ComponentからBufferedImageを作成する
    private BufferedImage createComponentImage(Component c) throws Exception {
        
        Dimension d = c.getSize();
        if (d.width == 0 || d.height == 0) {
            d = c.getPreferredSize();
            c.setSize(d);
        }
        
        BufferedImage image = new BufferedImage(d.width, d.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();
        c.paint(g2d);
        g2d.dispose();
        
        return image;
    }
    
    // BufferedImageをリサイズする
    private Image createResizedImage(BufferedImage image, double scale) {
        
        if (scale == 1) {
            return image;
        }
        
        int width = (int) (image.getWidth() * scale);
        int height =(int) (image.getHeight() * scale);
        
        return image.getScaledInstance(width, height, Image.SCALE_FAST);
    }
}
