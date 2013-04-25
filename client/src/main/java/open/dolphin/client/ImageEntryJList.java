package open.dolphin.client;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

/**
 * ImageEntryのJList
 * @author masuda, Masuda Naika
 */
public class ImageEntryJList<E> extends JList implements DragGestureListener {
    
    private int maxIconTextWidth;
    private Insets margin = new Insets(5, 5, 5, 5);
    
    public ImageEntryJList(DefaultListModel<ImageEntry> model) {
        super(model);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        setVisibleRowCount(0);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // レンダラ設定
        setCellRenderer(new ImageEntryJListCellRenderer());
        
        setDropMode(DropMode.INSERT);
        // quaquaではDrag時にクリックしなおさないといけないので…
        //imageList.setDragEnabled(true);
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(
                ImageEntryJList.this, DnDConstants.ACTION_COPY_OR_MOVE, ImageEntryJList.this);
    }
    
    public void setMaxIconTextWidth(int width) {
        maxIconTextWidth = width;
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        int action = dge.getDragAction();
        InputEvent event = dge.getTriggerEvent();
        JComponent comp = (JComponent) dge.getComponent();
        TransferHandler handler = comp.getTransferHandler();
        if (handler != null) {
            handler.exportAsDrag(comp, event, action);
        }
    }
    
    private class ImageEntryJListCellRenderer extends DefaultListCellRenderer {
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            ImageEntry entry = (ImageEntry) value;
            setIcon(entry.getImageIcon());
            String text = entry.getIconText();
            if (text != null && !text.isEmpty()) {
                FontMetrics fm = getFontMetrics(getFont());
                text = getWidthLimitHtml(fm, maxIconTextWidth, text);
            }
            setText(text);

            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.CENTER);
            setHorizontalTextPosition(JLabel.CENTER);
            setVerticalTextPosition(JLabel.BOTTOM);
            setBorder(new EmptyBorder(margin));
            
            return this;
        }
    }

    private String getWidthLimitHtml(FontMetrics fm, int maxWidth, String text) {

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        int len = text.length();
        int lineWidth = 0;
        for (int i = 0; i < len; ++i) {
            char c = text.charAt(i);
            int charWidth = fm.charWidth((int) c);
            if (maxWidth < charWidth + lineWidth) {
                sb.append("<br>");
                lineWidth = 0;
            }
            sb.append(c);
            lineWidth += charWidth;
        }

        sb.append("</html>");
        return sb.toString();
    }
}
