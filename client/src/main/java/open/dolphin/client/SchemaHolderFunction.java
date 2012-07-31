package open.dolphin.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import open.dolphin.infomodel.SchemaModel;
import open.dolphin.plugin.PluginLoader;
import org.apache.log4j.Logger;

/**
 * SchemaHolderFunction
 * @author masuda, Masuda Naika
 */
public class SchemaHolderFunction {
    
    private SchemaHolder selectedSchema;
    
    private AbstractAction deleteAction;

    private static SchemaHolderFunction instance;

    static {
        instance = new SchemaHolderFunction();
    }

    private SchemaHolderFunction() {
        setupActions();
    }

    public static SchemaHolderFunction getInstance() {
        return instance;
    }
    
    // スタンプホルダにDELETEキーでのスタンプ削除アクションを登録する
    public  void setDeleteAction(final SchemaHolder sh) {
        sh.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSchema");
        sh.getActionMap().put("deleteSchema", deleteAction);
    }
    
    public void setSelectedSchema(SchemaHolder sh) {
        selectedSchema = sh;
    }
    
    public Logger getLogger() {
        return ClientContext.getBootLogger();
    }
    
    // アクションを設定する
    private void setupActions() {

        // スタンプ削除アクション、これはDELETEキーで
        deleteAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSchema();
            }
        };
    }
    
    // 選択中のスタンプホルダを削除する deleteAction
    private void deleteSchema() {
        
        if (selectedSchema != null) {
            selectedSchema.getKartePane().removeSchema(selectedSchema);
        }
    }
    
    public void showPopupMenu(Point p) {
        
        KartePane kartePane = selectedSchema.getKartePane();

        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);
        ChartMediator mediator = kartePane.getMediator();
        popup.add(mediator.getAction(GUIConst.ACTION_CUT));
        popup.add(mediator.getAction(GUIConst.ACTION_COPY));
        popup.add(mediator.getAction(GUIConst.ACTION_PASTE));
        popup.addSeparator();

        // 右クリックで編集
        AbstractAction action = new AbstractAction("編集") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                edit();
            }
        };
        popup.add(action);
        popup.show((Component) selectedSchema, p.x, p.y);
    }
    
    public void edit() {
        
        getLogger().debug("SchemaHolder edit");
        KartePane kartePane = selectedSchema.getKartePane();
        SchemaModel schema = selectedSchema.getSchema();
        
        try {
            PluginLoader<SchemaEditor> loader = PluginLoader.load(SchemaEditor.class);
            Iterator<SchemaEditor> iter = loader.iterator();
            if (iter.hasNext()) {
                final SchemaEditor editor = iter.next();
                editor.setSchema(schema);
                editor.setEditable(kartePane.getTextPane().isEditable());
                editor.addPropertyChangeListener(selectedSchema);
                Runnable awt = new Runnable() {

                    @Override
                    public void run() {
                        editor.start();
                    }
                };
                EventQueue.invokeLater(awt);
            }
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            getLogger().warn(e);
        }
    }
    
    public ImageIcon getAdjustedImage(ImageIcon icon, Dimension dim) {
        
        getLogger().debug("SchemaHolder adjustImageSize");
        
        if ( (icon.getIconHeight() > dim.height) ||
                (icon.getIconWidth() > dim.width) ) {
            Image img = icon.getImage();
            float hRatio = (float)icon.getIconHeight() / dim.height;
            float wRatio = (float)icon.getIconWidth() / dim.width;
            int h, w;
            if (hRatio > wRatio) {
                h = dim.height;
                w = (int)(icon.getIconWidth() / hRatio);
            } else {
                w = dim.width;
                h = (int)(icon.getIconHeight() / wRatio);
            }
            img = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } else {
            return icon;
        }
    }
}
