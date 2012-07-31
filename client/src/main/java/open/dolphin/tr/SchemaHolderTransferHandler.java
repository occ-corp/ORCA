package open.dolphin.tr;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
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
    protected Transferable createTransferable(JComponent c) {

        clearVariables();
        SchemaHolder source = (SchemaHolder) c;
        srcComponent = source.getKartePane().getTextPane();
        SchemaModel schema = source.getSchema();
        SchemaList list = new SchemaList();
        list.schemaList = new SchemaModel[]{schema};
        Transferable tr = new SchemaListTransferable(list);
        return tr;
    }

    @Override
	public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {

        SchemaHolder source = (SchemaHolder) c;
        KartePane sourcePane = source.getKartePane();

        if (sourcePane.getTextPane() != destComponent) {
            return;
        }

        if (action != MOVE || !sourcePane.getTextPane().isEditable()) {
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
