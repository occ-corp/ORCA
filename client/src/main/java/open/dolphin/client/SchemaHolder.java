package open.dolphin.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.border.Border;
import javax.swing.text.Position;
import open.dolphin.infomodel.SchemaModel;

/**
 * スタンプのデータを保持するコンポーネントで TextPane に挿入される。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class SchemaHolder extends AbstractComponentHolder implements ComponentHolder {

    private static final Color BACKGROUND = Color.WHITE;
    private static final Color SELECTED_BORDER = new Color(255, 0, 153);
    private static final Color NON_SELECTED_BORDER = new Color(0, 0, 0, 0); // 透明
    private static final Border nonSelectedBorder = BorderFactory.createLineBorder(NON_SELECTED_BORDER);
    private static final Border selectedBorder = BorderFactory.createLineBorder(SELECTED_BORDER);

    private static final int FIXED_SIZE = 192;
    private int fixedWidth = FIXED_SIZE;
    private int fixedHeight = FIXED_SIZE;
    
    private boolean selected;
    
    private Position start;
    //private Position end; //endPositionはstart+1で代用

    private SchemaModel schema;
    private KartePane kartePane;
    
    private SchemaHolderFunction function;
    
    
    public SchemaHolder(KartePane kartePane, SchemaModel schema) {
        super();
        function = SchemaHolderFunction.getInstance();
        function.setDeleteAction(SchemaHolder.this);

        this.kartePane = kartePane;
        setSize(fixedWidth, fixedHeight);
        setMaximumSize(new Dimension(fixedWidth, fixedHeight));
        setMinimumSize(new Dimension(fixedWidth, fixedHeight));
        setPreferredSize(new Dimension(fixedWidth, fixedHeight));
        setDoubleBuffered(false);
        setOpaque(true);
        setBackground(BACKGROUND);
        
        this.schema = schema;
        setImageIcon(schema.getIcon());
    }
    
    public void setImageIcon(ImageIcon icon) {
        Dimension d = new Dimension(fixedWidth, fixedHeight);
        ImageIcon adjusted = function.getAdjustedImage(icon, d);
        setIcon(adjusted);
    }
    
    @Override
    public int getContentType() {
        return ComponentHolder.TT_IMAGE;
    }
    
    @Override
    public KartePane getKartePane() {
        return kartePane;
    }
    
    public SchemaModel getSchema() {
        return schema;
    }
    
    @Override
    public boolean isSelected() {
        return selected;
    }
    
    @Override
    public void mabeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            SchemaHolder sh = (SchemaHolder) e.getComponent();
            function.setSelectedSchema(sh);
            function.showPopupMenu(e.getPoint());
        }
    }
    
    @Override
    public void setSelected(boolean selected) {
        
        if (selected) {
            this.setBorder(selectedBorder);
            this.selected = true;
        } else {
            this.setBorder(nonSelectedBorder);
            this.selected = false;
        }
    }
    
    @Override
    public void edit() {

        function.setSelectedSchema(this);
        function.edit();
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        
        function.getLogger().debug("SchemaHolder propertyChange");
        SchemaModel newSchema = (SchemaModel)e.getNewValue();
        if (newSchema ==  null) {
            return;
        }
        
        schema = newSchema;
        setImageIcon(schema.getIcon());
        kartePane.setDirty(true);
    }
    
    @Override
    public void setEntry(Position start, Position end) {
        this.start = start;
        //this.end = end;
    }
    
    @Override
    public int getStartPos() {
        return start.getOffset();
    }
    
    @Override
    public int getEndPos() {
        //return end.getOffset();
        int ret = getStartPos() + 1;
        return ret;
    }
    
}