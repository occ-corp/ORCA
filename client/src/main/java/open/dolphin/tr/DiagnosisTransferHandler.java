package open.dolphin.tr;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
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
public class DiagnosisTransferHandler extends TransferHandler {

    private JTable sourceTable;

    private RegisteredDiagnosisModel dragItem;

    private boolean shouldRemove;

    private DiagnosisDocument parent;


    public DiagnosisTransferHandler(DiagnosisDocument parent) {
        super();
        this.parent = parent;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        sourceTable = (JTable) c;
//masuda^   table sorter対応
        //ListTableModel<RegisteredDiagnosisModel> tableModel = (ListTableModel<RegisteredDiagnosisModel>) sourceTable.getModel();
        //dragItem = tableModel.getObject(sourceTable.getSelectedRow());
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
    public boolean importData(JComponent c, Transferable t) {

        if (canImport(c, t.getTransferDataFlavors())) {

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
                int index = 0;

                // Dropされたノードを取得する
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
                            shouldRemove = false;
                            return true;
                        }

                    } else {
                        Toolkit.getDefaultToolkit().beep();
                        return false;
                    }

                } else {
                    // Dropされたノードの葉を列挙する
                    Enumeration e = droppedNode.preorderEnumeration();
                    while (e.hasMoreElements()) {
                        StampTreeNode node = (StampTreeNode) e.nextElement();
                        if (node.isLeaf()) {
                            ModuleInfoBean stampInfo = node.getStampInfo();
                            if (stampInfo.isSerialized() && (stampInfo.getEntity().equals(IInfoModel.ENTITY_DIAGNOSIS)) ) {
                                importList.add(stampInfo);
                            }
                        }
                    }
                }
                // まとめてデータベースからフェッチしインポートする
                if (importList.size() > 0) {
                    parent.importStampList(importList, index);
                    return true;

                } else {
                    return false;
                }

            } catch (Exception ioe) {
                ioe.printStackTrace(System.err);
            }
        }

        return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
// masuda   病名を削除することはなかろう
/*
        if (action == MOVE && shouldRemove) {
            ListTableModel<RegisteredDiagnosisModel> tableModel = (ListTableModel<RegisteredDiagnosisModel>) sourceTable.getModel();
            tableModel.delete(dragItem);
        }
*/
    }

    @Override
    public boolean canImport(JComponent c, DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (LocalStampTreeNodeTransferable.localStampTreeNodeFlavor.equals(flavors[i])) {
                JTable t = (JTable) c;
                t.getSelectionModel().setSelectionInterval(0,0);
                return true;
            }
        }
        return false;
    }
}
