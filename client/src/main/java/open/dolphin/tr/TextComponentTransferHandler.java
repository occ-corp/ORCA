package open.dolphin.tr;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import open.dolphin.client.GUIConst;
import open.dolphin.infomodel.*;

/**
 * TextComponentTransferHandler (renamed from BundleTransferHandler)
 * @author Minagawa,Kazushi. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class TextComponentTransferHandler extends AbstractKarteTransferHandler {

    private static final TextComponentTransferHandler instance;

    static {
        instance = new TextComponentTransferHandler();
    }

    private TextComponentTransferHandler() {
    }

    public static TextComponentTransferHandler getInstance() {
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

    @Override
    public boolean importData(TransferSupport support) {

        if (!canImport(support)) {
            importDataFailed();
            return false;
        }

        Transferable tr = support.getTransferable();
        JTextComponent dest = (JTextComponent) support.getComponent();

        boolean imported = false;

        if (tr.isDataFlavorSupported(OrderListTransferable.orderListFlavor)) {
            // KartePaneからのオーダスタンプをインポートする P
            imported =  doStampDrop(tr, dest);
        } else if (tr.isDataFlavorSupported(stringFlavor)) {
            // テキストをインポートする SOA/P
            imported = doTextDrop(tr, dest);
//masuda^   病名エディタからDropされるRegisteredDiagnosis Flavor
        } else if (tr.isDataFlavorSupported(InfoModelTransferable.infoModelFlavor)) {
            imported = doDiagnosisDrop(tr, dest);
        }
//masuda$
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
        
        if (!support.isDrop()) {
            return false;
        }
        
        // 選択範囲内にDnDならtrue
        if (isDndOntoSelectedText(support)){
            return false;
        }

        JTextComponent tc = (JTextComponent) support.getComponent();
        boolean canImport = true;
        canImport = canImport && tc.isEditable();
        canImport = canImport && (support.isDataFlavorSupported(DataFlavor.stringFlavor)
                || support.isDataFlavorSupported(OrderListTransferable.orderListFlavor)
//masuda^   病名エディタからDropされるRegisteredDiagnosis Flavor
                || support.isDataFlavorSupported(InfoModelTransferable.infoModelFlavor));
//masuda$
        return canImport;
    }

    /**
     * DropされたStamp(ModuleModel)をインポートする。
     * @param tr Transferable
     * @return インポートに成功した時 true
     */
    private boolean doStampDrop(Transferable tr, JTextComponent tc) {

        try {
            // スタンプのリストを取得する
            OrderList list = (OrderList) tr.getTransferData(OrderListTransferable.orderListFlavor);
            ModuleModel[] stamps = list.getOrderList();
            // pPaneにスタンプを挿入する
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < stamps.length; i++) {
                IInfoModel model = stamps[i].getModel();
                if (model instanceof BundleMed) {
                    BundleMed bm = (BundleMed) model;
                    sb.append(bm.getAdminDisplayString2());
                } else if (model instanceof BundleDolphin) {
                    BundleDolphin bd = (BundleDolphin) model;
                    sb.append(bd.toString());
                }
            }
            tc.replaceSelection(sb.toString());

            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    // DiagnosisDocumentからの病名を挿入する
    private boolean doDiagnosisDrop(Transferable tr, JTextComponent tc) {
        try {
            RegisteredDiagnosisModel rd = (RegisteredDiagnosisModel) tr.getTransferData(InfoModelTransferable.infoModelFlavor);
            StringBuilder sb = new StringBuilder();
            sb.append("＃");
            sb.append(rd.getDiagnosis());
            sb.append("(").append(rd.getStartDate()).append(")");
            tc.replaceSelection(sb.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }
    
    @Override
    public void enter(JComponent jc, ActionMap map) {

        JTextComponent tc = (JTextComponent) jc;
        map.get(GUIConst.ACTION_CUT).setEnabled(tc.isEditable());
        map.get(GUIConst.ACTION_COPY).setEnabled(true);
        boolean pasteOk = (tc.isEditable() && canPaste(tc));
        map.get(GUIConst.ACTION_PASTE).setEnabled(pasteOk);
    }

    @Override
    public void exit(JComponent jc) {
    }

    private boolean canPaste(JTextComponent tc) {

        if (!tc.isEditable()) {
            return false;
        }
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (t == null) {
            return false;
        }

        boolean canImport = true;
        canImport = canImport && (t.isDataFlavorSupported(DataFlavor.stringFlavor)
                || t.isDataFlavorSupported(OrderListTransferable.orderListFlavor));
        return canImport;
    }
}
