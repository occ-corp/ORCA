package open.dolphin.client;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.project.Project;
import open.dolphin.stampbox.StampBoxPlugin;
import open.dolphin.stampbox.StampTree;
import open.dolphin.stampbox.StampTreeNode;
import open.dolphin.tr.LocalStampTreeNodeTransferable;

/**
 * KartePane の抽象コードヘルパークラス。
 *
 * @author Kazyshi Minagawa
 */
public abstract class AbstractCodeHelper {
    
    /** キーワードの境界となる文字 */
    private static final String[] WORD_SEPARATOR = {" ", " ", "、", "。", "\n", "\t"};
    
    private static final String LISTENER_METHOD = "importStamp";
    
    protected static final Icon icon = ClientContext.getImageIconAlias("icon_foldr_small");
    
    /** 対象の KartePane */
    private KartePane kartePane;
    
    /** KartePane の JTextPane */
    private JTextPane textPane;
    
    /** 補完リストメニュー */
    protected JPopupMenu popup;
    
    /** キーワードパターン */
    protected Pattern pattern;
    
    /** キーワードの開始位置 */
    private int start;
    
    /** キーワードの終了位置 */
    private int end;
    
    /** ChartMediator */
    private ChartMediator mediator;
    
    /** 修飾キー */
    private int MODIFIER;
    
    
    /** 
     * Creates a new instance of CodeHelper 
     */
    public AbstractCodeHelper(KartePane kartePane, ChartMediator mediator) {
        
        this.kartePane = kartePane;
        this.mediator = mediator;
        this.textPane = kartePane.getTextPane();
        
        String modifier = Project.getString("modifier");
        
        if (modifier.equals("ctrl")) {
            MODIFIER =  KeyEvent.CTRL_DOWN_MASK;
        } else if (modifier.equals("meta")) {
            MODIFIER =  KeyEvent.META_DOWN_MASK;
        }

        this.textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (ClientContext.isMac()) {

                    // 元町皮膚科アイデア
                    if ((e.getModifiersEx() == MODIFIER) && e.getKeyCode() == KeyEvent.VK_ENTER) {
                        buildAndShowPopup();
                    }
                }
                else {
                    if ((e.getModifiersEx() == MODIFIER) && e.getKeyCode() == KeyEvent.VK_SPACE) {
                        buildAndShowPopup();
                    }
                }
            }
        });
    }
    
    protected abstract void buildPopup(String text);
    
    protected void buildEntityPopup(String entity) {
        
        //
        // 引数の entityに対応する StampTree を取得する
        //
//masuda^
        //StampBoxPlugin stampBox = mediator.getStampBox();
        StampBoxPlugin stampBox = Dolphin.getInstance().getStampBox();
//masuda$
        
        StampTree tree = stampBox.getStampTree(entity);
        if (tree == null) {
            return;
        }
        
        popup = new JPopupMenu();

        HashMap<Object, Object> ht = new HashMap<Object, Object>(5, 0.75f);
        
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        ht.put(rootNode, popup);
        
        Enumeration e = rootNode.preorderEnumeration();
        
        if (e != null) {
            
            e.nextElement(); // consume root
            
            while (e.hasMoreElements()) {
                
                StampTreeNode node = (StampTreeNode) e.nextElement();
                
                if (!node.isLeaf()) {
                    
                    JMenu subMenu = new JMenu(node.getUserObject().toString());
                    if (node.getParent() == rootNode) {
                        JPopupMenu parent = (JPopupMenu) ht.get(node.getParent());
                        parent.add(subMenu);
                        ht.put(node, subMenu);
                    } else {
                        JMenu parent = (JMenu) ht.get(node.getParent());
                        parent.add(subMenu);
                        ht.put(node, subMenu);   
                    }
                    
            
                    // 配下の子を全て列挙しJmenuItemにまとめる
                    JMenuItem item = new JMenuItem(node.getUserObject().toString());
                    item.setIcon(icon);
                    subMenu.add(item);
                    
                    addActionListner(item, node);
                
                } else if (node.isLeaf()) {
                    
                    ModuleInfoBean info = (ModuleInfoBean) node.getUserObject();
                    String stampName = info.getStampName();
                     
                    JMenuItem item = new JMenuItem(stampName);
                    addActionListner(item, node);
                    
                    if (node.getParent() == rootNode) {
                        JPopupMenu parent = (JPopupMenu) ht.get(node.getParent());
                        parent.add(item);
                    } else {
                        JMenu parent = (JMenu) ht.get(node.getParent());
                        parent.add(item);
                    }
                }
            }
        }
    }
    
    protected void addActionListner(final JMenuItem item, final StampTreeNode node) {
//masuda    reflectionはキライ
/*
        ReflectActionListener ral = new ReflectActionListener(this, LISTENER_METHOD, 
                            new Class[]{JComponent.class, TransferHandler.class, LocalStampTreeNodeTransferable.class},
                            new Object[]{textPane, textPane.getTransferHandler(), new LocalStampTreeNodeTransferable(node)});
*/
        ActionListener al = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TransferHandler handler = textPane.getTransferHandler();
                LocalStampTreeNodeTransferable tr = new LocalStampTreeNodeTransferable(node);
                importStamp(textPane, handler, tr);
            }
            
        };
        
        item.addActionListener(al);
    }

    protected void showPopup() {
        
        if (popup == null || popup.getComponentCount() < 1) {
            return;
        }
        
        try {
            int pos = textPane.getCaretPosition();
            Rectangle r = textPane.modelToView(pos);
            popup.show (textPane, r.x, r.y);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    private void importStamp(JComponent comp, TransferHandler handler, LocalStampTreeNodeTransferable tr) {
        
        textPane.setSelectionStart(start);
        textPane.setSelectionEnd(end);
        textPane.replaceSelection("");
        handler.importData(comp, tr);
        closePopup();
    }
    
    protected void closePopup() {
        if (popup != null) {
            popup.removeAll();
            popup = null;
        }
    }

    /**
     * 単語の境界からキャレットの位置までのテキストを取得し、
     * 長さがゼロ以上でれば補完メニューをポップアップする。
     */
    protected void buildAndShowPopup() {

        end = textPane.getCaretPosition();
        start = end;
        boolean found = false;

        while (start > 0) {
            
            start--;
  
            try {
                String text = textPane.getText(start, 1);
                for (String test : WORD_SEPARATOR) {
                    if (test.equals(text)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    start++;
                    break;
                }
                
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        try {
            
            String str = textPane.getText(start, end - start);
            
            if (str.length() > 0) {
                buildPopup(str);
                showPopup();
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
