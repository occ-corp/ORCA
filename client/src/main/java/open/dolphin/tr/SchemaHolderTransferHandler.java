package open.dolphin.tr;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
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
        List<SchemaModel> schemaList = new ArrayList<>();
        
        List<SchemaHolder> shList = selectedSchemaHolder;
        for (SchemaHolder sh : shList) {
            SchemaModel schema = sh.getSchema();
            schemaList.add(schema);
        }
        SchemaModel[] schemas = schemaList.toArray(new SchemaModel[0]);
        SchemaList list = new SchemaList();
        list.setSchemaList(schemas);
        
        // ドラッグ中のイメージを設定する
        Image image = createIconImage(shList);
        setDragImage(image);
        
        Transferable tr = new SchemaListTransferable(list);
        return tr;
    }
    
    private Image createIconImage(List<SchemaHolder> shList) {
        
        Dimension d = new Dimension();
        for (SchemaHolder sh : shList) {
            d.height += sh.getBounds().height;
            d.width = Math.max(d.width, sh.getBounds().width);
        }
        BufferedImage image =new BufferedImage(d.width, d.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();

        int y = 0;
        for (SchemaHolder lbl : shList) {
            BufferedImage bf = createComponentImage(lbl);
            g2d.drawImage(bf, null, 0, y);
            y += bf.getHeight();
        }
        g2d.dispose();
        
        int width = (int) (d.width * IconScale);
        int height =(int) (d.height * IconScale);
        
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
        
        for (SchemaHolder sh : selectedSchemaHolder) {
            sh.getKartePane().removeSchema(sh);
        }

        selectedSchemaHolder.clear();
        endTransfer();
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
        
        for (SchemaHolder sh : selectedSchemaHolder) {
            sh.getKartePane().removeSchema(sh);
        }
        selectedSchemaHolder.clear();
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action){
        // ドラッグしたのが選択されていない場合は複数選択を解除し、
        // ドラッグ開始したものを選択状態にする
        SchemaHolder source = (SchemaHolder) comp;
        
        if (!source.isSelected()) {
            schemaHolderSingleSelection(source);
        }
        
        super.exportAsDrag(comp, e, action);
    }

    @Override
    public void enter(JComponent jc, ActionMap map) {
        
        // StampHolderの選択は解除する
        exitClearSelectedStampHolder();
        
        SchemaHolder sh = (SchemaHolder) jc;
        
        boolean canCut = (sh.getKartePane().getTextPane().isEditable());
        map.get(GUIConst.ACTION_COPY).setEnabled(true);
        map.get(GUIConst.ACTION_CUT).setEnabled(canCut);
        map.get(GUIConst.ACTION_PASTE).setEnabled(false);
        
        processSchemaSelection(sh);
    }
    
    @Override
    public void exit(JComponent jc) {
        
        if (!isAvoidExit()) {
            exitClearSelectedSchemaHolder();
        }
    }
    
    private void processSchemaSelection(SchemaHolder schemaHolder) {
        
        // Shift/ALT押されてたらスタンプすべて選択
        if ((modifiersEx & InputEvent.ALT_DOWN_MASK) != 0
                || (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0) {
            setSchemaHolderSelectAll(schemaHolder);
            return;
        }
        //CTRL押されてたら選択<->非選択をトグル（MacならOptionキー）
        if ((modifiersEx & SHORTCUTKEY_DOWN_MASK) != 0 && (modifiersEx & InputEvent.ALT_DOWN_MASK) == 0) {
            setSchemaHolderToggleSelect(schemaHolder);
            return;
        }
        //何も押されてなかったら単一選択のはず
        schemaHolderSingleSelection(schemaHolder);
    }
    
    // ALT押されてたらスタンプすべて選択
    private void setSchemaHolderSelectAll(SchemaHolder schemaHolder) {

        exitClearSelectedSchemaHolder();
        List<SchemaHolder> list = schemaHolder.getKartePane().getDocument().getSchemaHolders();
        for (SchemaHolder sh : list) {
            addEnterSchemaHolder(sh);
        }
    }
    
    //CTRL押されてたら選択<->非選択をトグル
    private void setSchemaHolderToggleSelect(SchemaHolder schemaHolder) {

        if (schemaHolder.isSelected()) {
            removeExitSchemaHolder(schemaHolder);
        } else {
            addEnterSchemaHolder(schemaHolder);
        }
    }
    
    // SchemaHolderをexitし、除去する
    private void removeExitSchemaHolder(SchemaHolder sh) {
        selectedSchemaHolder.remove(sh);
        sh.setSelected(false);
    }
    
    public void schemaHolderSingleSelection(SchemaHolder schemaHolder) {
        exitClearSelectedSchemaHolder();
        selectedSchemaHolder.add(schemaHolder);
        schemaHolder.setSelected(true);
    }
    
    private void addEnterSchemaHolder(SchemaHolder schemaHolder) {

        //　KartePane毎にSchemaHolderの位置の順番で追加
        List<SchemaHolder> list = selectedSchemaHolder;
        int len = list.size();
        int pos = 0;

        while (pos < len) {
            SchemaHolder test = list.get(pos);
            if (test.getKartePane() == schemaHolder.getKartePane()) {
                break;
            }
            ++pos;
        }
        while (pos < len) {
            SchemaHolder test = list.get(pos);
            if (test.getKartePane() != schemaHolder.getKartePane()){
                break;
            }
            if (test.getStartPos() > schemaHolder.getStartPos()) {
                break;
            }
            ++pos;
        }
        list.add(pos, schemaHolder);

        schemaHolder.setSelected(true);
    }

    public List<SchemaHolder> getSelectedSchemaHolder() {
        return selectedSchemaHolder;
    }
}
