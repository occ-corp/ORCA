package open.dolphin.tr;

import java.awt.EventQueue;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

    //private static final int SHORTCUTKEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private static final String MED_TEIKI = "定期";
    private static final String MED_RINJI = "臨時";

    private static final StampHolderTransferHandler instance;

    // 選択している複数のStampHolderを記憶しておく
    private static final CopyOnWriteArrayList<StampHolder> selectedStampHolder;


    static {
        instance = new StampHolderTransferHandler();
        selectedStampHolder = new CopyOnWriteArrayList<StampHolder>();
    }


    private StampHolderTransferHandler() {
    }

    public static StampHolderTransferHandler getInstance() {
        return instance;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {

        clearVariables();
        // 複数stampを含んだtransferableを返す
        List<ModuleModel> stampList = new ArrayList<ModuleModel>();

        // OrderListのstampsを作る
        for (StampHolder sh : selectedStampHolder) {
            ModuleModel stamp = sh.getStamp();
            stampList.add(stamp);
        }
        ModuleModel[] stamps = stampList.toArray(new ModuleModel[0]);
        OrderList list = new OrderList(stamps);
        StampHolder source = (StampHolder) c;
        // ドラッグ元を設定する
        srcComponent = source.getKartePane().getTextPane();
        Transferable tr = new OrderListTransferable(list);
        return tr;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    private void replaceStamp(final StampHolder target, final ModuleInfoBean stampInfo) {
/*
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    StampDelegater sdl = StampDelegater.getInstance();
                    StampModel getStamp = sdl.getStamp(stampInfo.getStampId());
                    final ModuleModel stamp = new ModuleModel();
                    if (getStamp != null) {
                        stamp.setModel((IInfoModel) BeanUtils.xmlDecode(getStamp.getStampBytes()));
                        stamp.setModuleInfoBean(stampInfo);
                    }
                    Runnable awt = new Runnable() {

                        @Override
                        public void run() {
                            target.importStamp(stamp);
                        }
                    };
                    EventQueue.invokeLater(awt);

                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        };
        Thread t = new Thread(r);
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
*/
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
            return false;
        }

        final StampHolder target = (StampHolder) support.getComponent();
        Transferable tr = support.getTransferable();
        StampTreeNode droppedNode = null;

        try {
            droppedNode = (StampTreeNode) tr.getTransferData(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }

        if (droppedNode == null || (!droppedNode.isLeaf())) {
            return false;
        }

        final ModuleInfoBean stampInfo = droppedNode.getStampInfo();

        String role = stampInfo.getStampRole();

        if (!role.equals(IInfoModel.ROLE_P)) {
            return false;
        }

        if (Project.getBoolean("replaceStamp", false)) {
            replaceStamp(target, stampInfo);

        } else {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    confirmReplace(target, stampInfo);
                }
            };
            EventQueue.invokeLater(r);
        }
        return true;
    }

    @Override
    protected void exportDone(JComponent c, Transferable tr, int action) {

        // ココはスタンプをドラッグしたあと、ドラッグもとを削除するかどうか
        StampHolder source = (StampHolder) c;
        KartePane sourcePane = source.getKartePane();
        if (sourcePane.getTextPane() != destComponent) {
            return;
        }

        if (action != MOVE || !sourcePane.getTextPane().isEditable()) {
            return;
        }

        for (StampHolder sh : selectedStampHolder) {
            sh.getKartePane().removeStamp(sh);
        }

        selectedStampHolder.clear();
    }

    /**
     * インポート可能かどうかを返す。
     */
    @Override
    public boolean canImport(TransferSupport support) {

        StampHolder source = (StampHolder) support.getComponent();
        DataFlavor[] flavors = support.getDataFlavors();
        JTextPane tc = source.getKartePane().getTextPane();
        if (tc.isEditable() && hasFlavor(flavors)) {
            return true;
        }
        return false;
    }

    protected boolean hasFlavor(DataFlavor[] flavors) {
        for (DataFlavor flavor : flavors) {
            if (LocalStampTreeNodeTransferable.localStampTreeNodeFlavor.equals(flavor)) {
                return true;
            }
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

    public boolean isAvoidExit() {

        int modifiersEx = getModifiersEx();
        return  (modifiersEx & SHORTCUTKEY_DOWN_MASK) != 0 &&
                (modifiersEx & InputEvent.ALT_DOWN_MASK) == 0;
    }

    private void processStampSelection(StampHolder stampHolder) {

        int modifiersEx = getModifiersEx();
        
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

    // selectedStampHolderにあるStampHolderをexitしクリアする
    private void exitClearSelectedStampHolder() {

        for (StampHolder sh : selectedStampHolder) {
            sh.setSelected(false);
        }
        selectedStampHolder.clear();
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
            if ("medOrder".equals(sh.getStamp().getModuleInfoBean().getEntity())) {
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

        String groupMark = null;
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
