package open.dolphin.impl.img;

import java.awt.BorderLayout;
import javax.swing.*;
import open.dolphin.client.ClientContext;

/**
 * AbstractBrowserView
 * @author masuda, Masuda Naika
 */
public class AbstractBrowserView extends JPanel {
    
    private JPanel north;
    private JLabel dirLbl;
    private JButton refreshBtn;
    private JButton settingBtn;

    protected AbstractBrowserView() {
        
        dirLbl = new JLabel(ClientContext.getImageIconAlias("icon_info_small"));
        north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
        north.add(dirLbl);
        north.add(Box.createHorizontalGlue());

        refreshBtn = new JButton("更新");
        settingBtn = new JButton("設定");
        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
    }
    
    protected JPanel getNorthPanel(){
        return north;
    };
    
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
