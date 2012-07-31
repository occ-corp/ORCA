
package open.dolphin.client;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import open.dolphin.tr.AbstractImagePanelTransferHandler;
import open.dolphin.util.ModifiedFlowLayout;

/**
 * ImagePanel
 *
 * @author masuda, Masuda Naika
 */
public final class ImagePanel extends JPanel implements MouseListener, DragGestureListener{
    
    private JComponent selectedComp;
    private static final Color SELECTED_COLOR = new Color(56, 117, 215);
    
    public ImagePanel() {
        setLayout(new ModifiedFlowLayout(FlowLayout.LEFT));
        setOpaque(true);
        setFocusable(true);
        setBackground(Color.WHITE);
        addMouseListener(ImagePanel.this);
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(ImagePanel.this, DnDConstants.ACTION_COPY_OR_MOVE, ImagePanel.this);
        ActionMap map = getActionMap();
        //map.put(TransferHandler.getCutAction().getValue(Action.NAME), TransferHandler.getCutAction());
        map.put(TransferHandler.getCopyAction().getValue(Action.NAME), TransferHandler.getCopyAction());
        //map.put(TransferHandler.getPasteAction().getValue(Action.NAME), TransferHandler.getPasteAction());
    }

    public JComponent getSelectedComponent() {
        return selectedComp;
    }

    public ImageLabel getSelectedImageLabel() {
        if (selectedComp instanceof ImageLabel) {
            return (ImageLabel) selectedComp;
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        TransferHandler tr = getTransferHandler();
        if (tr instanceof AbstractImagePanelTransferHandler) {
            AbstractImagePanelTransferHandler handler = (AbstractImagePanelTransferHandler) tr;
            handler.mouseClicked(e);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

        // ここでフォーカスとImageLabelの選択処理をする。
        requestFocusInWindow();
        JComponent comp = (JComponent) getComponentAt(e.getPoint());
        if (comp == this) {
            setSelectedComponent(null);
        } else {
            setSelectedComponent(comp);
        }

        maybeShowPopup(e);
    }

    private void setSelectedComponent(JComponent comp) {

        if (selectedComp != null) {
            selectedComp.setBackground(null);
            selectedComp.setForeground(Color.BLACK);
        }
        if (comp != null) {
            comp.setBackground(SELECTED_COLOR);
            comp.setForeground(Color.WHITE);
            selectedComp = comp;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        TransferHandler tr = getTransferHandler();
        if (tr instanceof AbstractImagePanelTransferHandler) {
            AbstractImagePanelTransferHandler handler = (AbstractImagePanelTransferHandler) tr;
            handler.maybeShowPopup(e);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {

        if (selectedComp == null) {
            return;
        }

        int action = dge.getDragAction();
        InputEvent event = dge.getTriggerEvent();
        JComponent comp = (JComponent) dge.getComponent();
        TransferHandler handler = comp.getTransferHandler();
        if (handler != null) {
            handler.exportAsDrag(comp, event, action);
        }
    }
}
