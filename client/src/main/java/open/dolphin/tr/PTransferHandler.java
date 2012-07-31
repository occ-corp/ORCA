package open.dolphin.tr;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import open.dolphin.client.GUIConst;
import open.dolphin.client.KartePane;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.stampbox.StampTreeNode;

/**
 * PTransferHandler
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class PTransferHandler extends AbstractKarteTransferHandler {

    private static final PTransferHandler instance;

    static {
        instance = new PTransferHandler();
    }

    private PTransferHandler() {
    }

    public static PTransferHandler getInstance() {
        return instance;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {

        JTextComponent source = (JTextComponent) c;
        clearVariables();
        srcComponent = c;

        // テキストの選択範囲を記憶
        boolean b = setSelectedTextArea(source);
        if (!b) {
            return null;
        }

        String data = source.getSelectedText();
        return new StringSelection(data);
    }

    // KartePaneにTransferableをインポートする
    @Override
    public boolean importData(TransferSupport support) {

        if (!canImport(support)) {
            return false;
        }

        Transferable tr = support.getTransferable();
        JTextComponent tc = (JTextComponent) support.getComponent();

        boolean imported = false;

        KartePane destPane = (KartePane) tc.getClientProperty(GUIConst.PROP_KARTE_COMPOSITOR);

        if (tr.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor)) {
            // StampTreeNodeをインポートする, SOA/P
            imported = doStampInfoDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(OrderListTransferable.orderListFlavor)) {
            // KartePaneからのオーダスタンプをインポートする P
            imported = doStampDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(stringFlavor)) {
            // テキストをインポートする SOA/P
            imported = doTextDrop(tr, tc);
        }

        if (imported) {
            destComponent = tc;
        }

        return imported;
    }

    /**
     * インポート可能かどうかを返す。
     */
    @Override
    public boolean canImport(TransferSupport support) {

        JTextComponent tc = (JTextComponent) support.getComponent();

        // 選択範囲内にDnDならtrue
        if (isDndOntoSelectedText(support)) {
            return false;
        }
        if (tc.isEditable() && hasFlavor(support.getDataFlavors())) {
            return true;
        }
        return false;
    }

    /**
     * Flavorリストのなかに受け入れられものがあるかどうかを返す。
     */
    private boolean hasFlavor(DataFlavor[] flavors) {

        for (DataFlavor flavor : flavors) {
            // String OK
            if (DataFlavor.stringFlavor.equals(flavor)) {
                return true;
            }
            // StampTreeNode(FromStampTree) OK
            if (LocalStampTreeNodeTransferable.localStampTreeNodeFlavor.equals(flavor)) {
                return true;
            }
            // OrderStamp List OK
            if (OrderListTransferable.orderListFlavor.equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * DropされたModuleInfo(StampInfo)をインポートする。
     * @param tr Transferable
     * @return 成功した時 true
     */
    private boolean doStampInfoDrop(Transferable tr, KartePane pPane) {

        try {
            // 葉の場合
            StampTreeNode droppedNode = (StampTreeNode) tr.getTransferData(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor);
            if (droppedNode.isLeaf()) {
                ModuleInfoBean stampInfo = droppedNode.getStampInfo();
                String role = stampInfo.getStampRole();
                if (role.equals(IInfoModel.ROLE_P)) {
                    pPane.stampInfoDropped(stampInfo);
                } else if (role.equals(IInfoModel.ROLE_TEXT)) {
                    pPane.stampInfoDropped(stampInfo);
                } else if (role.equals(IInfoModel.ROLE_ORCA_SET)) {
                    pPane.stampInfoDropped(stampInfo);
                }
                return true;
            }

            // Dropされたノードの葉を列挙する
            Enumeration e = droppedNode.preorderEnumeration();
            ArrayList<ModuleInfoBean> addList = new ArrayList<ModuleInfoBean>(5);
            String role = null;
            while (e.hasMoreElements()) {
                StampTreeNode node = (StampTreeNode) e.nextElement();
                if (node.isLeaf()) {
                    ModuleInfoBean stampInfo = node.getStampInfo();
                    role = stampInfo.getStampRole();
                    if (stampInfo.isSerialized() && (role.equals(IInfoModel.ROLE_P) || (role.equals(IInfoModel.ROLE_TEXT))) ) {
                        addList.add(stampInfo);
                    }
                }
            }

            if (role == null) {
                return true;
            }

            // まとめてデータベースからフェッチしインポートする
            if (role.equals(IInfoModel.ROLE_TEXT)) {
                pPane.textStampInfoDropped(addList);
            } else if (role.equals(IInfoModel.ROLE_P)) {
                pPane.stampInfoDropped(addList);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    /**
     * DropされたStamp(ModuleModel)をインポートする。
     * @param tr Transferable
     * @return インポートに成功した時 true
     */
    private boolean doStampDrop(Transferable tr, KartePane kartePane) {

        try {
            // スタンプのリストを取得する
            OrderList list = (OrderList) tr.getTransferData(OrderListTransferable.orderListFlavor);
            ModuleModel[] stamps = list.orderList;

//masuda^   スタンプコピー時に別患者のカルテかどうかをチェックする
            boolean differentKarte = false;
            long destKarteId = kartePane.getParent().getContext().getKarte().getId();
            for (ModuleModel mm : stamps) {
                if (mm.getKarteBean() == null) {
                    continue;
                }
                long karteId = mm.getKarteBean().getId();
                if (karteId != destKarteId) {
                    differentKarte = true;
                    break;
                }
            }
            if (differentKarte) {
                String[] options = {"取消", "無視"};
                String msg = "異なる患者カルテにスタンプをコピーしようとしています。\n継続しますか？";
                int val = JOptionPane.showOptionDialog(kartePane.getParent().getContext().getFrame(), msg, "スタンプコピー",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                if (val != 1) {
                    // 取り消し
                    return false;
                }
            }
//masuda$
            // pPaneにスタンプを挿入する
            for (int i = 0; i < stamps.length; i++) {
                kartePane.stamp(stamps[i]);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    private boolean canPaste(KartePane pPane) {
        if (!pPane.getTextPane().isEditable()) {
            return false;
        }
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (t == null) {
            return false;
        }
        if (t.isDataFlavorSupported(DataFlavor.stringFlavor) ||
            t.isDataFlavorSupported(OrderListTransferable.orderListFlavor) ||
            t.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor)) {
            return true;
        }
        return false;
    }

    @Override
    public void enter(JComponent jc, ActionMap map) {

        KartePane pPane = (KartePane) jc.getClientProperty(GUIConst.PROP_KARTE_COMPOSITOR);
        if (pPane.getTextPane().isEditable()) {
            map.get(GUIConst.ACTION_PASTE).setEnabled(canPaste(pPane));
            map.get(GUIConst.ACTION_INSERT_STAMP).setEnabled(true);
            //map.get(GUIConst.ACTION_INSERT_STAMP).setEnabled(true);
        }
    }

    @Override
    public void exit(JComponent jc) {
    }
}
