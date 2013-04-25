package open.dolphin.tr;

import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.JComponent;
import javax.swing.JTable;
import open.dolphin.client.DiagnosisDocument;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.stampbox.StampTreeNode;
import open.dolphin.table.ListTableSorter;

/**
 * DiagnosisTransferHandler
 *
 * @author Minagawa,Kazushi
 *
 */
public class DiagnosisTransferHandler extends DolphinTransferHandler {

    private RegisteredDiagnosisModel dragItem;
    private DiagnosisDocument parent;
    
    private int action;

    public DiagnosisTransferHandler(DiagnosisDocument parent) {
        super();
        this.parent = parent;
    }

    public int getTransferAction() {
        return action;
    }
    
    @Override
    protected Transferable createTransferable(JComponent src) {

        startTransfer(src);
        JTable sourceTable = (JTable) src;

//masuda^   table sorter対応
        ListTableSorter sorter = (ListTableSorter) sourceTable.getModel();
        dragItem = (RegisteredDiagnosisModel) sorter.getObject(sourceTable.getSelectedRow());
        return dragItem != null ? new InfoModelTransferable(dragItem) : null;
//masuda$
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    public boolean importData(TransferSupport support) {

        if (!canImport(support)) {
            importDataFailed();
            return false;
        }
        
        try {
//masuda^   sorterもあるしてっぺん固定
/*
             // 病名の挿入位置を決めておく
             JTable dropTable = (JTable) c;
             int index = dropTable.getSelectedRow();
             if (index < 0) {
             index = 0;
             }
             */
//masuda$
            action = support.getDropAction();
            int index = 0;

            // Dropされたノードを取得する
            Transferable t = support.getTransferable();
            StampTreeNode droppedNode = (StampTreeNode) t.getTransferData(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor);

            // Import するイストを生成する
            ArrayList<ModuleInfoBean> importList = new ArrayList<ModuleInfoBean>(3);

            // 葉の場合
            if (droppedNode.isLeaf()) {
                ModuleInfoBean stampInfo = droppedNode.getStampInfo();
                if (stampInfo.getEntity().equals(IInfoModel.ENTITY_DIAGNOSIS)) {
                    if (stampInfo.isSerialized()) {
                        importList.add(stampInfo);
                    } else {
                        parent.openEditor2();
                        importDataSuccess(support.getComponent());
                        return true;
                    }

                } else {
                    Toolkit.getDefaultToolkit().beep();
                    importDataFailed();
                    return false;
                }

            } else {
                // Dropされたノードの葉を列挙する
                Enumeration e = droppedNode.preorderEnumeration();
                while (e.hasMoreElements()) {
                    StampTreeNode node = (StampTreeNode) e.nextElement();
                    if (node.isLeaf()) {
                        ModuleInfoBean stampInfo = node.getStampInfo();
                        if (stampInfo.isSerialized() && (stampInfo.getEntity().equals(IInfoModel.ENTITY_DIAGNOSIS))) {
                            importList.add(stampInfo);
                        }
                    }
                }
            }
            // まとめてデータベースからフェッチしインポートする
            if (importList.size() > 0) {
                parent.importStampList(importList, index);
                importDataSuccess(support.getComponent());
                return true;

            } else {
                importDataFailed();
                return false;
            }

        } catch (Exception ioe) {
            ioe.printStackTrace(System.err);
        }

        importDataFailed();
        return false;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor);
    }
}
