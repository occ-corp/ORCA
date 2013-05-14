package open.dolphin.tr;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import open.dolphin.client.GUIConst;
import open.dolphin.client.KartePane;
import open.dolphin.client.SchemaHolder;
import open.dolphin.infomodel.SchemaModel;


/**
 * SchemaHolderTransferHandler
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public class SchemaHolderTransferHandler extends AbstractKarteTransferHandler {
    
    private static final double IconScale = 0.6;

    private static final SchemaHolderTransferHandler instance;

    static {
        instance = new SchemaHolderTransferHandler();
    }

    private SchemaHolderTransferHandler() {
    }

    public static SchemaHolderTransferHandler getInstance() {
        return instance;
    }

    @Override
    protected Transferable createTransferable(JComponent src) {

        startTransfer(src);
        
        SchemaHolder source = (SchemaHolder) src;
        SchemaModel schema = source.getSchema();
        SchemaList list = new SchemaList();
        list.setSchemaList(new SchemaModel[]{schema});
        
        // ドラッグ中のイメージを設定する
        Image image = createIconImage(src);
        setDragImage(image);
        
        Transferable tr = new SchemaListTransferable(list);
        return tr;
    }
    
    private Image createIconImage(JComponent src) {

        BufferedImage image = getImageFromComponent(src);
        int width = (int) (image.getWidth() * IconScale);
        int height =(int) (image.getHeight() * IconScale);
        
        return image.getScaledInstance(width, height, Image.SCALE_FAST);
    }
    
    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {

        // export先がOpenDolphin以外なら削除しない
        if (isExportToOther()) {
            endTransfer();
            return;
        }
        
        SchemaHolder source = (SchemaHolder) c;
        KartePane sourcePane = source.getKartePane();
        
        if (sourcePane.getTextPane() != destComponent) {
            endTransfer();
            return;
        }

        if (action != MOVE || !sourcePane.getTextPane().isEditable()) {
            endTransfer();
            return;
        }

        sourcePane.removeSchema(source);
    }

    @Override
    public boolean canImport(TransferSupport support) {
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

        SchemaHolder source = (SchemaHolder) comp;
        source.getKartePane().removeSchema(source);
    }

    @Override
    public void exportAsDrag(JComponent comp, java.awt.event.InputEvent e, int action){
        comp.requestFocusInWindow();
        super.exportAsDrag(comp, e, action);
    }

    @Override
    public void enter(JComponent jc, ActionMap map) {

        SchemaHolder sh = (SchemaHolder) jc;
        sh.setSelected(true);

        boolean canCut = (sh.getKartePane().getTextPane().isEditable());
        map.get(GUIConst.ACTION_COPY).setEnabled(true);
        map.get(GUIConst.ACTION_CUT).setEnabled(canCut);
        map.get(GUIConst.ACTION_PASTE).setEnabled(false);
    }

    @Override
    public void exit(JComponent jc) {
        
        SchemaHolder sh = (SchemaHolder) jc;
        if (sh != null) {
            sh.setSelected(false);
        }
    }
}
