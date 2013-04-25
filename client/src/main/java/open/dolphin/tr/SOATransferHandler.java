package open.dolphin.tr;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import open.dolphin.client.GUIConst;
import open.dolphin.client.ImageEntry;
import open.dolphin.client.KartePane;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.SchemaModel;
import open.dolphin.stampbox.StampTreeNode;
import open.dolphin.util.ImageTool;

/**
 * KartePaneTransferHandler
 *
 * @author Minagawa,Kazushi. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class SOATransferHandler extends AbstractKarteTransferHandler {

    private static final SOATransferHandler instance;

    static {
        instance = new SOATransferHandler();
    }

    protected SOATransferHandler() {
    }

    public static SOATransferHandler getInstance() {
        return instance;
    }

    @Override
    protected Transferable createTransferable(JComponent src) {
        
        startTransfer(src);

        JTextComponent source = (JTextComponent) src;

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
            importDataFailed();
            return false;
        }

        Transferable tr = support.getTransferable();
        JTextComponent dest = (JTextComponent) support.getComponent();

        boolean imported = false;

        KartePane destPane = getKartePane(dest);

        if (tr.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor)) {
            // StampTreeNodeをインポートする, SOA/P
            imported = doStampInfoDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(stringFlavor)) {
            // テキストをインポートする SOA/P
            imported = doTextDrop(tr, dest);

        } else if (tr.isDataFlavorSupported(ImageEntryTransferable.imageEntryFlavor)) {
            // シェーマボックスからのDnDをインポートする SOA
            imported = doImageEntryDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(SchemaListTransferable.schemaListFlavor)) {
            // Paneからのシェーマをインポートする SOA
            imported = doSchemaDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            // 画像ファイルのドロップをインポートする SOA
            imported = doFileDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            // クリップボードの画像をインポートする SOA
            imported = doClippedImageDrop(tr, destPane);
        }

        if (imported) {
            importDataSuccess(dest);
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
        
        JTextComponent tc = (JTextComponent) support.getComponent();

        // 選択範囲内にDnDならtrue
        if (isDndOntoSelectedText(support)) {
            return false;
        }
        if (!tc.isEditable()) {
            return false;
        }
        
        if (hasFlavor(support)) {
            return true;
        }
        return false;
    }

    /**
     * Flavorリストのなかに受け入れられものがあるかどうかを返す。
     */
    private boolean hasFlavor(TransferSupport support) {

        // String ok
        if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return true;
        }
        // StampTreeNode OK
        if (support.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor)) {
            return true;
        }
        // Schema OK
        if (support.isDataFlavorSupported(SchemaListTransferable.schemaListFlavor)) {
            return true;
        }
        // Image OK
        if (support.isDataFlavorSupported(ImageEntryTransferable.imageEntryFlavor)) {
            return true;
        }
        // File OK
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return true;
        }
        // クリップボードの画像 OK
        if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return true;
        }

        return false;
    }

    /**
     * DropされたModuleInfo(StampInfo)をインポートする。
     * @param tr Transferable
     * @return 成功した時 true
     */
    protected boolean doStampInfoDrop(Transferable tr, KartePane soaPane) {

        try {
            // DropされたTreeNodeを取得する
            StampTreeNode droppedNode = (StampTreeNode) tr.getTransferData(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor);
            // 葉の場合
            if (droppedNode.isLeaf()) {
                ModuleInfoBean stampInfo = droppedNode.getStampInfo();
                String role = stampInfo.getStampRole();
                if (role.equals(IInfoModel.ROLE_TEXT)) {
                    soaPane.stampInfoDropped(stampInfo);
                } else if (role.equals(IInfoModel.ROLE_SOA)) {
                    soaPane.stampInfoDropped(stampInfo);
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
                    if (stampInfo.isSerialized() && (!stampInfo.getEntity().equals(IInfoModel.ENTITY_DIAGNOSIS))) {
                        if (role == null) {
                            role = stampInfo.getStampRole();
                        }
                        addList.add(stampInfo);
                    }
                }
            }

            // まとめてデータベースからフェッチしインポートする
            if (role.equals(IInfoModel.ROLE_TEXT)) {
                soaPane.textStampInfoDropped(addList);
            } else if (role.equals(IInfoModel.ROLE_SOA)) {
                soaPane.stampInfoDropped(addList);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    /**
     * Dropされたシェーマをインポーオする。
     * @param tr
     * @return
     */
    private boolean doSchemaDrop(Transferable tr, KartePane soaPane) {

        try {
            // Schemaリストを取得する
            SchemaList list = (SchemaList) tr.getTransferData(SchemaListTransferable.schemaListFlavor);
            SchemaModel[] schemas = list.getSchemaList();
//masuda^   スタンプコピー時に別患者のカルテかどうかをチェックする
            boolean differentKarte = false;
            long destKarteId = soaPane.getParent().getContext().getKarte().getId();
            for (SchemaModel mm : schemas) {
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
                int val = JOptionPane.showOptionDialog(soaPane.getParent().getContext().getFrame(), msg, "スタンプコピー",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                if (val != 1) {
                    // 取り消し
                    return false;
                }
            }
//masuda$

            for (int i = 0; i < schemas.length; i++) {
                soaPane.stampSchema(schemas[i]);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    /**
     * Dropされたイメージをインポートする。
     */
    private boolean doImageEntryDrop(final Transferable tr, KartePane kartePane) {

        try {
            // Imageを取得する
            ImageEntry entry = (ImageEntry) tr.getTransferData(ImageEntryTransferable.imageEntryFlavor);
            kartePane.imageEntryDropped(entry);
            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    /**
     * DropされたイメージFileをインポートする。
     */
    private boolean doFileDrop(Transferable tr, KartePane kartePane) {
        try {
            @SuppressWarnings("unchecked")
            List<File> list = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
            if (list != null && !list.isEmpty()) {
                File file = list.get(0);
                ImageEntry entry = ImageTool.getImageEntryFromFile(file);
                if (entry != null) {
                    kartePane.imageEntryDropped(entry);
                }
            }
        } catch (UnsupportedFlavorException ex) {
        } catch (IOException ex) {
        } catch (NullPointerException ex) {
        }
        return false;
    }

    // クリップボードの画像を受け入れる
    private boolean doClippedImageDrop(Transferable tr, KartePane kartePane) {

        try {
            Image image = (Image) tr.getTransferData(DataFlavor.imageFlavor);
            ImageEntry entry = ImageTool.getImageEntryFromImage(image);
            if (entry != null) {
                kartePane.imageEntryDropped(entry);
            }
        } catch (UnsupportedFlavorException ex) {
        } catch (IOException ex) {
        }
        return false;
    }

    private boolean canPaste(KartePane soaPane) {
        if (!soaPane.getTextPane().isEditable()) {
            return false;
        }
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (t == null) {
            return false;
        }
        if (t.isDataFlavorSupported(DataFlavor.stringFlavor)
                || t.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor)
                || t.isDataFlavorSupported(SchemaListTransferable.schemaListFlavor)
                || t.isDataFlavorSupported(ImageEntryTransferable.imageEntryFlavor)
                || t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                || t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return true;
        }
        return false;
    }


    @Override
    public void enter(JComponent jc, ActionMap map) {

        KartePane pPane = getKartePane((JTextComponent) jc);
        if (pPane.getTextPane().isEditable()) {
            map.get(GUIConst.ACTION_PASTE).setEnabled(canPaste(pPane));
            map.get(GUIConst.ACTION_INSERT_TEXT).setEnabled(true);
            //map.get(GUIConst.ACTION_INSERT_SCHEMA).setEnabled(true);
        }
    }

    @Override
    public void exit(JComponent jc) {
    }
}
