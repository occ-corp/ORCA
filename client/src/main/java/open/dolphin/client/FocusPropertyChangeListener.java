package open.dolphin.client;

import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.TransferHandler;
import open.dolphin.helper.WindowSupport;
import open.dolphin.tr.DolphinTransferHandler;
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
        
        // DolphinTransferHandlerが設定されていない場合はChartMediator処理は不要
        // KarteViewerのJTextPaneは例外
        if (!(newComp instanceof JTextPane)) {
            TransferHandler t = newComp.getTransferHandler();
            if (t == null || !(t instanceof DolphinTransferHandler)) {
                return;
            }
        }

        // get Mediator from focused JFrame
        Container parent = newComp.getTopLevelAncestor();
        if (!(parent instanceof JFrame)) {
            return;
        }

        JFrame frame = (JFrame) parent;
        Object objMediator = WindowSupport.getMediator(frame);
        if (objMediator == null) {
            return;
        }
        
        if (objMediator instanceof ChartMediator) {
            ChartMediator mediator = (ChartMediator) objMediator;
            // exit old focused component
            if (oldComp != null) { 
                TransferHandler tr = oldComp.getTransferHandler();
                if (tr != null && tr instanceof IKarteTransferHandler) {
                    IKarteTransferHandler handler = (IKarteTransferHandler) tr;
                    handler.exit(oldComp);
                }
            }
            mediator.setCurrentFocusOwner(newComp);
            
        } else if (objMediator instanceof open.dolphin.client.Dolphin.Mediator) {
            // do nothing
        }

        oldComp = newComp;
    }
}
