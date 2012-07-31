
package open.dolphin.impl.img;

/**
 * DefaultBrowserView改
 *
 * @author masuda, Masuda Naika
 */
public class DefaultBrowserView extends AbstractBrowserView {

    public DefaultBrowserView() {
        super();
        initComponents();
    }

    private void initComponents() {
        getNorthPanel().add(getRefreshBtn());
        getNorthPanel().add(getSettingBtn());
    }
}
