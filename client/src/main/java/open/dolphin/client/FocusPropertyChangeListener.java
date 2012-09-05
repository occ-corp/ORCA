package open.dolphin.client;

import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;
import open.dolphin.tr.IKarteTransferHandler;

/**
 * FocusPropertyChangeListener
 * ChartMediatorから独立
 * このロジックは複雑だけど考えるのは楽しいネ！
 *
 * @author masuda, Masuda Naika
 */
public class FocusPropertyChangeListener implements PropertyChangeListener {

    private static final FocusPropertyChangeListener instance;
    private static final String PROPERTY_PERMANENT_FOCUS_OWNER = "permanentFocusOwner";
    //private static final String PROPERTY_FOCUS_OWNER = "focusOwner";

    // ComponentをクリックしたときのmodifiersEx
    private int modifiersEx;
    // old focused component
    private  JComponent oldComp;

    static {
        instance = new FocusPropertyChangeListener();
    }

    private FocusPropertyChangeListener() {
    }

    public static FocusPropertyChangeListener getInstance() {
        return instance;
    }

    public int getModifiersEx() {
        return modifiersEx;
    }

    public void setModifiersEx(int modifiersEx) {
        this.modifiersEx = modifiersEx;
    }

    public void register() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addPropertyChangeListener(instance);
    }

    public void dispose() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.removePropertyChangeListener(instance);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {

        String prop = e.getPropertyName();

        if (!PROPERTY_PERMANENT_FOCUS_OWNER.equals(prop)) {
            return;
        }

        JComponent newComp = (JComponent) e.getNewValue();

        if (newComp == null || newComp == oldComp) {
            return;
        }
        
        // focusOwnerになるのはJTextComponent とComponentHolder
        // native, numbusはコレしないとダメ。ボタンなどもfocus取ってしまう
        if (!(newComp instanceof JTextComponent || newComp instanceof ComponentHolder)) {
            return;
        }
        
        // ChartMediator is always in ChartFrame.class
        Container parent = newComp.getTopLevelAncestor();
        if (parent instanceof ChartFrame) {

            // exit old focused component
            if (oldComp != null) {
                TransferHandler tr = oldComp.getTransferHandler();
                if (tr != null && tr instanceof IKarteTransferHandler) {
                    IKarteTransferHandler handler = (IKarteTransferHandler) tr;
                    handler.exit(oldComp);
                }
            }

            // enter new focused karte compositor
            ChartFrame frame = (ChartFrame) parent;
            // get ChartMediator from ChartFrame
            ChartMediator mediator = frame.getChartMediator();
            if (mediator != null) {
                mediator.setCurrentFocusOwner(newComp);
            }

            oldComp = newComp;
            modifiersEx = 0;
        }
    }
}
