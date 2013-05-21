package open.dolphin.tr;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.List;
import javax.swing.*;
import open.dolphin.client.GUIConst;
import open.dolphin.client.KartePane;
import open.dolphin.client.StampHolder;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.StampModel;
import open.dolphin.project.Project;
import open.dolphin.stampbox.StampTreeNode;
import open.dolphin.util.BeanUtils;

/**
 * StampHolderTransferHandler
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class StampHolderTransferHandler extends AbstractKarteTransferHandler {
    
    private static final double DRAG_IMAGE_SCALE = 0.7;
    
    private static final String MED_TEIKI = "定期";
    private static final String MED_RINJI = "臨時";

    private static final StampHolderTransferHandler instance;

    static {
        instance = new StampHolderTransferHandler();
    }

    private StampHolderTransferHandler() {
    }

    public static StampHolderTransferHandler getInstance() {
        return instance;
    }

    @Override
    protected Transferable createTransferable(JComponent src) {
        
        // 複数stampを含んだtransferableを返す
        int size = selectedStampHolder.size();
        if (size == 0) {
            return null;
        }        

        startTransfer(src);
        
        StampHolder[] stampList = selectedStampHolder.toArray(new StampHolder[size]);
        ModuleModel[] stamps = new ModuleModel[size];
        
        // OrderListのstampsを作る
        for (int i = 0; i < size; ++i) {
            stamps[i] = stampList[i].getStamp();
        }
        OrderList list = new OrderList(stamps);
        
        // ドラッグ中のイメージを設定する
        Image image = createDragImage(stampList, DRAG_IMAGE_SCALE);
        setDragImage(image);
        
        Transferable tr = new OrderListTransferable(list);
        return tr;
    }
    

    
    private void replaceStamp(final StampHolder target, final ModuleInfoBean stampInfo) {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                StampDelegater sdl = StampDelegater.getInstance();
                StampModel getStamp = sdl.getStamp(stampInfo.getStampId());
                final ModuleModel stamp = new ModuleModel();
                if (getStamp != null) {
                    stamp.setModel((IInfoModel) BeanUtils.xmlDecode(getStamp.getStampBytes()));
                    stamp.setModuleInfoBean(stampInfo);
                }
                target.importStamp(stamp);
                return null;
            }
        };
        worker.execute();
    }

    private void confirmReplace(StampHolder target, ModuleInfoBean stampInfo) {

        Window w = SwingUtilities.getWindowAncestor(target);
        String replace = "置き換える";
        String cancel = "取消し";

         int option = JOptionPane.showOptionDialog(
                 w,
                 "スタンプを置き換えますか?",
                 "スタンプ Drag and Drop",
                 JOptionPane.DEFAULT_OPTION,
                 JOptionPane.QUESTION_MESSAGE,
                 null,
                 new String[]{replace, cancel}, replace);

         if (option == 0) {
             replaceStamp(target, stampInfo);
         }
    }

    @Override
    public boolean importData(TransferSupport support) {

        if (!canImport(support)) {
            importDataFailed();
            return false;
        }

        final StampHolder dest = (StampHolder) support.getComponent();
        Transferable tr = support.getTransferable();
        StampTreeNode droppedNode;

        try {
            droppedNode = (StampTreeNode) tr.getTransferData(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            importDataFailed();
            return false;
        }

        if (droppedNode == null || (!droppedNode.isLeaf())) {
            importDataFailed();
            return false;
        }

        final ModuleInfoBean stampInfo = droppedNode.getStampInfo();

        String role = stampInfo.getStampRole();

        if (!role.equals(IInfoModel.ROLE_P)) {
            importDataFailed();
            return false;
        }

        if (Project.getBoolean("replaceStamp", false)) {
            replaceStamp(dest, stampInfo);

        } else {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    confirmReplace(dest, stampInfo);
                }
            };
            EventQueue.invokeLater(r);
        }
        
        importDataSuccess(dest);
        return true;
    }

    @Override
    protected void exportDone(JComponent c, Transferable tr, int action) {

        // export先がOpenDolphin以外なら削除しない
        if (isExportToOther()) {
            endTransfer();
            return;
        }
        
        // ココはスタンプをドラッグしたあと、ドラッグもとを削除するかどうか
        StampHolder source = (StampHolder) c;
        KartePane sourcePane = source.getKartePane();
        if (sourcePane.getTextPane() != destComponent) {
            endTransfer();
            return;
        }

        if (action != MOVE || !sourcePane.getTextPane().isEditable()) {
            endTransfer();
            return;
        }

        for (StampHolder sh : selectedStampHolder) {
            sh.getKartePane().removeStamp(sh);
        }

        selectedStampHolder.clear();
        endTransfer();
    }

    /**
     * インポート可能かどうかを返す。
     */
    @Override
    public boolean canImport(TransferSupport support) {
        
        if (!support.isDrop()) {
            return false;
        }
        
        StampHolder source = (StampHolder) support.getComponent();
        JTextPane tc = source.getKartePane().getTextPane();
        if (!tc.isEditable()) {
            return false;
        }
        if (support.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor)) {
            return true;
        }
        return false;
    }

    /**
     * スタンプをクリップボードへ転送する。
     */
    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {

        Transferable tr = createTransferable(comp);
        clip.setContents(tr, null);

        if (action != MOVE) {
            return;
        }

        for (StampHolder sh : selectedStampHolder) {
            sh.getKartePane().removeStamp(sh);
        }
        selectedStampHolder.clear();
    }

    @Override
    public void exportAsDrag(JComponent comp, java.awt.event.InputEvent e, int action){
        // ドラッグしたのが選択されていない場合は複数選択を解除し、
        // ドラッグ開始したものを選択状態にする

        StampHolder source = (StampHolder) comp;

        if (!source.isSelected()) {
            stampHolderSingleSelection(source);
        }

        super.exportAsDrag(comp, e, action);
    }

    @Override
    public void enter(JComponent jc, ActionMap map) {
        
        // SchemaHolderの選択は解除する
        exitClearSelectedSchemaHolder();
        
        StampHolder sh = (StampHolder) jc;

        boolean canCut = (sh.getKartePane().getTextPane().isEditable());
        map.get(GUIConst.ACTION_COPY).setEnabled(true);
        map.get(GUIConst.ACTION_CUT).setEnabled(canCut);
        map.get(GUIConst.ACTION_PASTE).setEnabled(false);

        processStampSelection(sh);
    }

    @Override
    public void exit(JComponent jc) {
        if (!isAvoidExit()) {
            exitClearSelectedStampHolder();
        }
    }

    private void processStampSelection(StampHolder stampHolder) {

        // CTRL+SHIFTなら薬剤すべて選択
        if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0 && (modifiersEx & SHORTCUTKEY_DOWN_MASK) != 0){
            setStampHolderMedicineSelect(stampHolder);
            return;
        }
        // ALT押されてたらスタンプすべて選択
        if ((modifiersEx & InputEvent.ALT_DOWN_MASK) != 0) {
            setStampHolderSelectAll(stampHolder);
            return;
        }
        // Shift押されてたらグループ選択
        if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0) {
            setStampHolderGroupSelect(stampHolder);
            return;
        }
        //CTRL押されてたら選択<->非選択をトグル（MacならOptionキー）
        if ((modifiersEx & SHORTCUTKEY_DOWN_MASK) != 0 && (modifiersEx & InputEvent.ALT_DOWN_MASK) == 0) {
            setStampHolderToggleSelect(stampHolder);
            return;
        }
        //何も押されてなかったら単一選択のはず
        stampHolderSingleSelection(stampHolder);
    }

    public void stampHolderSingleSelection(StampHolder stampHolder) {
        exitClearSelectedStampHolder();
        selectedStampHolder.add(stampHolder);
        stampHolder.setSelected(true);
    }

    private void addEnterStampHolder(StampHolder stampHolder) {

        //　KartePane毎にStampHolderの位置の順番で追加
        List<StampHolder> list = selectedStampHolder;
        int len = list.size();
        int pos = 0;

        while (pos < len) {
            StampHolder test = list.get(pos);
            if (test.getKartePane() == stampHolder.getKartePane()) {
                break;
            }
            ++pos;
        }
        while (pos < len) {
            StampHolder test = list.get(pos);
            if (test.getKartePane() != stampHolder.getKartePane()){
                break;
            }
            if (test.getStartPos() > stampHolder.getStartPos()) {
                break;
            }
            ++pos;
        }
        list.add(pos, stampHolder);

        stampHolder.setSelected(true);
    }


    // Ctrl + Shift押されてたら薬剤すべて選択
    private void setStampHolderMedicineSelect(StampHolder stampHolder) {

        exitClearSelectedStampHolder();

        List<StampHolder> list = stampHolder.getKartePane().getDocument().getStampHolders();
        for (StampHolder sh : list) {
            if (IInfoModel.ENTITY_MED_ORDER.equals(sh.getStamp().getModuleInfoBean().getEntity())) {
                addEnterStampHolder(sh);
            }
        }
    }

    // ALT押されてたらスタンプすべて選択
    private void setStampHolderSelectAll(StampHolder stampHolder) {

        exitClearSelectedStampHolder();
        List<StampHolder> list = stampHolder.getKartePane().getDocument().getStampHolders();
        for (StampHolder sh : list) {
            addEnterStampHolder(sh);
        }
    }

    //CTRL押されてたら選択<->非選択をトグル
    private void setStampHolderToggleSelect(StampHolder stampHolder) {

        if (stampHolder.isSelected()) {
            removeExitStampHolder(stampHolder);
        } else {
            addEnterStampHolder(stampHolder);
        }
    }

    // StampHolderをexitし、除去する
    private void removeExitStampHolder(StampHolder sh) {

        selectedStampHolder.remove(sh);
        sh.setSelected(false);
    }

    // Shift押されてたらグループ選択。同じスタンプ名のものをグループとする
    private void setStampHolderGroupSelect(StampHolder stampHolder) {

        // まずは複数選択解除しておく
        exitClearSelectedStampHolder();

        String groupMark;
        String stampName = stampHolder.getStamp().getModuleInfoBean().getStampName();
        // StampNameが定期・臨時を含んでいたらそれをgroupMarkとする
        if (stampName.contains(MED_TEIKI)) {
            groupMark = MED_TEIKI;
        } else if (stampName.contains(MED_RINJI)) {
            groupMark = MED_RINJI;
        } else {
            groupMark = stampName;
        }

        List<StampHolder> list = stampHolder.getKartePane().getDocument().getStampHolders();
        for (StampHolder sh : list) {
            //スキャンしたStampHolderのnameが一致したら選択追加
            String name = sh.getStamp().getModuleInfoBean().getStampName();
            if (name.contains(groupMark)) {
                addEnterStampHolder(sh);
            }
        }
    }

    public void deleteSelectedStampHolder() {
        for (StampHolder sh : selectedStampHolder) {
            sh.getKartePane().removeStamp(sh);
        }
    }

    public List<StampHolder> getSelectedStampHolder() {
        return selectedStampHolder;
    }
}
