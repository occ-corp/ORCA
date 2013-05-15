package open.dolphin.tr;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.stampbox.StampTree;
import open.dolphin.stampbox.StampTreeNode;

/**
 * StampTreeTransferHandler
 *
 * @author Minagawa,Kazushi. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class StampTreeTransferHandler extends DolphinTransferHandler {

    // StampTreeNode Flavor
    private DataFlavor stampTreeNodeFlavor = LocalStampTreeNodeTransferable.localStampTreeNodeFlavor;
    // KartePaneからDropされるオーダのFlavor
    private DataFlavor orderFlavor = OrderListTransferable.orderListFlavor;
    // KartePaneからDropされるテキストFlavor
    //private DataFlavor stringFlavor = DataFlavor.stringFlavor;
    // 病名エディタからDropされるRegisteredDiagnosis Flavor
    private DataFlavor infoModelFlavor = InfoModelTransferable.infoModelFlavor;

    /**
     * 選択されたノードでDragを開始する。
     */
    @Override
    protected Transferable createTransferable(JComponent src) {
        
        startTransfer(src);
        StampTree sourceTree = (StampTree) src;
        StampTreeNode dragNode = (StampTreeNode) sourceTree.getLastSelectedPathComponent();
        
        // ドラッグ中のイメージを設定する
        Image image = createNodeImage(sourceTree);
        setDragImage(image);
        
        return new LocalStampTreeNodeTransferable(dragNode);
    }
    
    /**
     * DropされたFlavorをStampTreeにインポートする。
     */
    @Override
    public boolean importData(TransferSupport support) {

        if (!canImport(support)) {
            importDataFailed();
            return false;
        }

        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath path = dl.getPath();
        int childIndex = dl.getChildIndex();
        StampTreeNode parentNode = (StampTreeNode) path.getLastPathComponent();
        StampTree target = (StampTree) support.getComponent();
        String targetEntity = target.getEntity();
        Transferable tr = support.getTransferable();

        boolean imported = false;
        try {
            if (support.isDataFlavorSupported(orderFlavor)) {
                OrderList list = (OrderList) tr.getTransferData(orderFlavor);
                ModuleModel droppedStamp = list.getOrderList()[0];
                String droppedStampEntity = droppedStamp.getModuleInfoBean().getEntity();

                if (droppedStampEntity.equals(targetEntity)) {
                    imported = target.addStamp(parentNode, droppedStamp, childIndex);

                } else if (droppedStampEntity.equals(IInfoModel.ENTITY_LABO_TEST)
                        && (targetEntity.equals(IInfoModel.ENTITY_PHYSIOLOGY_ORDER) || targetEntity.equals(IInfoModel.ENTITY_BACTERIA_ORDER))) {
                    //-----------------------------------------
                    // drop が検体検査で受けが生体もしくは細菌の場合
                    // entity を受側に変更して受け入れる
                    //-----------------------------------------
                    droppedStamp.getModuleInfoBean().setEntity(targetEntity);
                    imported = target.addStamp(parentNode, droppedStamp, childIndex);

                } else if (droppedStampEntity.equals(IInfoModel.ENTITY_PHYSIOLOGY_ORDER)
                        && (targetEntity.equals(IInfoModel.ENTITY_LABO_TEST) || targetEntity.equals(IInfoModel.ENTITY_BACTERIA_ORDER))) {
                    //-----------------------------------------
                    // drop が生体検査で受けが検体もしくは細菌の場合
                    // entity を受側に変更して受け入れる
                    //-----------------------------------------
                    droppedStamp.getModuleInfoBean().setEntity(targetEntity);
                    imported = target.addStamp(parentNode, droppedStamp, childIndex);

                } else if (droppedStampEntity.equals(IInfoModel.ENTITY_BACTERIA_ORDER)
                        && (targetEntity.equals(IInfoModel.ENTITY_LABO_TEST) || targetEntity.equals(IInfoModel.ENTITY_PHYSIOLOGY_ORDER))) {
                    //-----------------------------------------
                    // drop が細菌検査で受けが検体もしくは生体の場合
                    // entity を受側に変更して受け入れる
                    //-----------------------------------------
                    droppedStamp.getModuleInfoBean().setEntity(targetEntity);
                    imported = target.addStamp(parentNode, droppedStamp, childIndex);

                } else if (targetEntity.equals(IInfoModel.ENTITY_PATH)) {
                    //---------------------
                    // パス Tree の場合
                    //---------------------
                    imported = target.addStamp(parentNode, droppedStamp, childIndex);

                } else {
                    // Rootの最後に追加する
                    //return target.addStamp(droppedStamp, null);
                    // これがいいかどうか.....
                    imported = false;
                }

            } else if (support.isDataFlavorSupported(stringFlavor)) {
                //-----------------------------------------
                // KartePaneからDropされたテキストをインポートする
                //-----------------------------------------
                String text = (String) tr.getTransferData(stringFlavor);
                if (targetEntity.equals(IInfoModel.ENTITY_TEXT)) {
                    imported = target.addTextStamp(parentNode, text, childIndex);
                } else {
                    imported = false;
                }
            } else if (support.isDataFlavorSupported(infoModelFlavor)) {
                //----------------------------------------------
                // DiagnosisEditorからDropされた病名をインポートする
                //----------------------------------------------
                RegisteredDiagnosisModel rd = (RegisteredDiagnosisModel) tr.getTransferData(InfoModelTransferable.infoModelFlavor);
                if (targetEntity.equals(IInfoModel.ENTITY_DIAGNOSIS)) {
                    imported = target.addDiagnosis(parentNode, rd, childIndex);
                } else {
                    imported = false;
                }

            } else if (support.isDataFlavorSupported(stampTreeNodeFlavor)) {
                //-----------------------------------------------
                // StampTree内のDnD, Dropされるノードを取得する
                //-----------------------------------------------
                StampTreeNode dropNode = (StampTreeNode) tr.getTransferData(stampTreeNodeFlavor);
                //------------------------------------------------------------------------
                // root までの親のパスのなかに自分がいるかどうかを判定する
                // Drop先が DragNode の子である時は DnD できない i.e 親が自分の子になることはできない
                //------------------------------------------------------------------------
                DefaultTreeModel model = (DefaultTreeModel) target.getModel();
                TreeNode[] parents = model.getPathToRoot(parentNode);
                boolean exist = false;
                for (TreeNode parent : parents) {
                    if (parent == (TreeNode) dropNode) {
                        exist = true;
                        Toolkit.getDefaultToolkit().beep();
                        break;
                    }
                }

                if (exist) {
                    imported = false;
                } else {
                    //System.err.println("1:"+ childIndex);
                    if (childIndex < 0) {
                        childIndex = 0;
                    }

                    // dropNodeの親==parentNodeの場合
                    // childIndexを補正する(dropNodeを最初に削除するため）
                    if (dropNode.getParent() == parentNode) {
                        int cnt = parentNode.getChildCount();
                        for (int i = 0; i < cnt; i++) {
                            if (parentNode.getChildAt(i) == dropNode) {
                                childIndex = childIndex > i ? childIndex - 1 : childIndex;
                                //System.err.println("2:"+ childIndex);
                                break;
                            }
                        }
                    }

                    // stampTreeNodeFlavorは参照のため最初に削除してから挿入する
                    model.removeNodeFromParent(dropNode);
                    model.insertNodeInto(dropNode, parentNode, childIndex);
                }

            } else {
                imported = false;
            }

        } catch (UnsupportedFlavorException ue) {
            ue.printStackTrace(System.err);

        } catch (IOException ie) {
            ie.printStackTrace(System.err);
        }

        if (imported) {
            importDataSuccess(target);
        } else {
            importDataFailed();
        }

        return imported;
    }

    /**
     * インポート可能かどうかを返す。
     */
    @Override
    public boolean canImport(TransferSupport support) {

        if (!support.isDrop()) {
            return false;
        }
//masuda^
        // スタンプ箱がロックされている場合はfalse
        StampTree stampTree = (StampTree) support.getComponent();
        if (stampTree.getStampBox().isLocked()) {
            return false;
        }

        if (support.isDataFlavorSupported(orderFlavor)
                || support.isDataFlavorSupported(stringFlavor)
                || support.isDataFlavorSupported(infoModelFlavor)
                || support.isDataFlavorSupported(stampTreeNodeFlavor)) {
            return true;
        }
//masuda$
        return false;
    }
}
