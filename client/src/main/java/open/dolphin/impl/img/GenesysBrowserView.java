
package open.dolphin.impl.img;

import javax.swing.JButton;

/**
 * GenesysBrowserView改
 *
 * @author masuda, Masuda Naika
 */
public class GenesysBrowserView extends AbstractBrowserView {

    private JButton genesysBtn;

    public GenesysBrowserView() {
        super();

        initComponents();
    }

    private void initComponents() {
        genesysBtn = new JButton("Genesys");
        getNorthPanel().add(genesysBtn);
        getNorthPanel().add(getRefreshBtn());
        getNorthPanel().add(getSettingBtn());
    }

    public JButton getGenesysBtn() {
        return genesysBtn;
    }
}
