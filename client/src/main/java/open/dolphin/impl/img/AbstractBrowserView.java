
package open.dolphin.impl.img;

import java.awt.BorderLayout;
import javax.swing.*;
import open.dolphin.client.ClientContext;
import open.dolphin.client.ImagePanel;

/**
 * AbstractBrowserView
 * @author masuda, Masuda Naika
 */
public class AbstractBrowserView extends JPanel {
    
    private JPanel north;
    private ImagePanel center;
    private JLabel dirLbl;
    private JButton refreshBtn;
    private JButton settingBtn;

    protected AbstractBrowserView() {
        
        dirLbl = new JLabel(ClientContext.getImageIcon("os_information_16.png"));
        north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
        north.add(dirLbl);
        north.add(Box.createHorizontalGlue());
        
        center = new ImagePanel();
        
        refreshBtn = new JButton("更新");
        settingBtn = new JButton("設定");
        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(center);
        add(scroll, BorderLayout.CENTER);
    }
    
    protected JPanel getNorthPanel(){
        return north;
    };
    
    public ImagePanel getContentPanel() {
        return center;
    }
    
    public JLabel getDirLbl() {
        return dirLbl;
    }

    public JButton getRefreshBtn() {
        return refreshBtn;
    }

    public JButton getSettingBtn() {
        return settingBtn;
    }
}
