package open.dolphin.stampbox;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;
import open.dolphin.infomodel.ModuleInfoBean;

/**
 * StampTreePopupAdapter
 *
 * @author  Kazushi Minagawa
 */
public class StampTreePopupAdapter extends MouseAdapter {
    
    public StampTreePopupAdapter() {
    }
    
    @Override
    public void mousePressed(MouseEvent evt) {
        maybePopup(evt);
    }
    
    @Override
    public void mouseReleased(MouseEvent evt) {
        maybePopup(evt);
    }
    
    private void maybePopup(MouseEvent evt) {
        
        if (evt.isPopupTrigger()) {

            // イベントソースの StampTree を取得する
            final StampTree tree = (StampTree) evt.getSource();
//masuda^   ロック中はポップアップしない
            if (tree.getStampBox().isLocked()) {
                return;
            }
//masuda$
            int x = evt.getX();
            int y = evt.getY();
            
            // クリック位置へのパスを得る
            TreePath destPath = tree.getPathForLocation(x, y);
            if (destPath == null) {
                return;
            }
            
            // クリック位置の Node を得る
            StampTreeNode node = (StampTreeNode) destPath.getLastPathComponent();
            
            if (node.isLeaf()) {
                // Leaf なので StampInfo 　を得る
                ModuleInfoBean info = (ModuleInfoBean) node.getUserObject();
                
                // Editable
                if ( ! info.isEditable() ) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
            }
            
            // Popupする
//masuda^   reflectionはキライ
            JPopupMenu popup = new JPopupMenu();
            popup.add(new JMenuItem(new AbstractAction("コピー"){

                @Override
                public void actionPerformed(ActionEvent e) {
                    tree.copy();
                }
            }));
            popup.addSeparator();
            popup.add(new JMenuItem(new AbstractAction("新規フォルダ"){

                @Override
                public void actionPerformed(ActionEvent e) {
                    tree.createNewFolder();
                }
            }));
            popup.add(new JMenuItem(new AbstractAction("名称変更"){

                @Override
                public void actionPerformed(ActionEvent e) {
                    tree.renameNode();
                }
            }));
            popup.addSeparator();
            popup.add(new JMenuItem(new AbstractAction("削 除"){

                @Override
                public void actionPerformed(ActionEvent e) {
                    tree.deleteNode();
                }
            }));
//masuda$
            popup.show(evt.getComponent(),x, y);
        }
    }
}