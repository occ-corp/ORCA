package open.dolphin.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.JPopupMenu;

/**
 *
 * @author masuda, Masuda Naika
 */
public class PopupMenuUtil {
    
    public static void showPopup(Component invoker, JPopupMenu popup, int x, int y) {
        
        Dimension popupSize = popup.getPreferredSize();
        Point invokerScreenLoc = invoker.getLocationOnScreen();
        Point popupScreenLoc = new Point(invokerScreenLoc.x + x, invokerScreenLoc.y + y);
        
        GraphicsConfiguration gc = getCurrentGraphicsConfiguration(invoker, new Point(x, y));
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Insets screenInsets = toolkit.getScreenInsets(gc);
        
        Rectangle screenRect = gc.getBounds();
        screenRect.x += screenInsets.left;
        screenRect.y += screenInsets.top;
        screenRect.width -= screenInsets.right;
        screenRect.height -= screenInsets.bottom;
        
        int offsetX = popupScreenLoc.x + popupSize.width - screenRect.width;
        if (offsetX > 0) {
            x -= offsetX;
        }
        int offsetY = popupScreenLoc.y + popupSize.height - screenRect.height;
        if (offsetY > 0) {
            y -= offsetY;
        }
        
        popup.show(invoker, x, y);
    }

    private static GraphicsConfiguration getCurrentGraphicsConfiguration(Component invoker, Point popupLocation) {
        
        GraphicsConfiguration gc = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                GraphicsConfiguration dgc = gd.getDefaultConfiguration();
                if (dgc.getBounds().contains(popupLocation)) {
                    gc = dgc;
                    break;
                }
            }
        }

        // If not found and we have invoker, ask invoker about his gc
        if (gc == null && invoker != null) {
            gc = invoker.getGraphicsConfiguration();
        }
        return gc;
    }
}
