package open.dolphin.tr;

import javax.swing.ActionMap;
import javax.swing.JComponent;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 */
public interface IKarteTransferHandler {

    public void enter(JComponent jc, ActionMap map);

    public void exit(JComponent jc);
}
