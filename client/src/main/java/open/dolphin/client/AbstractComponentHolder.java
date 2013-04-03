package open.dolphin.client;

import java.awt.Cursor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;

/**
 * ComponentHolder
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public abstract class AbstractComponentHolder extends JLabel implements MouseListener, DragGestureListener{

    public AbstractComponentHolder() {
        putClientProperty(GUIConst.PROP_KARTE_COMPOSITOR, AbstractComponentHolder.this);
        setFocusable(true);
        addMouseListener(AbstractComponentHolder.this);
        addMouseListener(new PopupListner());
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        
        ActionMap map = this.getActionMap();
        map.put(TransferHandler.getCutAction().getValue(Action.NAME), TransferHandler.getCutAction());
        map.put(TransferHandler.getCopyAction().getValue(Action.NAME), TransferHandler.getCopyAction());
        map.put(TransferHandler.getPasteAction().getValue(Action.NAME), TransferHandler.getPasteAction());

        // DragGestureを使ってみる
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(AbstractComponentHolder.this, DnDConstants.ACTION_COPY_OR_MOVE, AbstractComponentHolder.this);
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        
        // modifiersExを記録しておく
        int modifiersEx = e.getModifiersEx();
        FocusPropertyChangeListener.getInstance().setModifiersEx(modifiersEx);

        // StampEditor から戻った後に動作しないため
        boolean focus = requestFocusInWindow();

        if (!focus) {
            requestFocusInWindow();
        }

        if (e.getClickCount() == 2 && (!e.isPopupTrigger())) {
            edit();
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
    
    public abstract void edit();
    
    public abstract void mabeShowPopup(MouseEvent e);
    
    
    private class PopupListner extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getClickCount() != 2) {
                mabeShowPopup(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getClickCount() != 2) {
                mabeShowPopup(e);
            }
        }
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
}