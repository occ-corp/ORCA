
package open.dolphin.client;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 * ChartImplとEditorFrameの拡張JFrame
 *
 * @author masuda, Masuda Naika
 */
public class ChartFrame extends JFrame {

    private ChartMediator mediator;

    public ChartFrame(String title) {
        super(title);
        ImageIcon icon = ClientContext.getClientContextStub().getImageIcon("dolphinIcon.png");
        this.setIconImage(icon.getImage());
    }

    public void setChartMediator(ChartMediator mediator) {
        this.mediator = mediator;
    }
    public ChartMediator getChartMediator() {
        return mediator;
    }
}
