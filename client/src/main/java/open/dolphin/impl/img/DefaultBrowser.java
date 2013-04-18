package open.dolphin.impl.img;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import open.dolphin.client.ClientContext;
import open.dolphin.client.GUIConst;
import open.dolphin.project.Project;

/**
 * DefaultBrowser
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class DefaultBrowser extends AbstractBrowser {

    private static final String TITLE = "PDF・画像";
    private static final String SETTING_FILE_NAME = "image-browser.properties";

    private DefaultBrowserView view;

    public DefaultBrowser() {

        setTitle(TITLE);

        properties = getProperties();

        // Convert the old properties
        Properties old = Project.loadPropertiesAsObject("imageBrowserProp2.xml");
        if (old != null) {
            Enumeration e = old.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String val = old.getProperty(key);
                properties.setProperty(key, val);
            }
            Project.storeProperties(properties, SETTING_FILE_NAME);
            Project.deleteSettingFile("imageBrowserProp2.xml");

        } else {
            Project.loadProperties(properties, SETTING_FILE_NAME);
        }

        // Base directory
        String value = properties.getProperty(PROP_BASE_DIR);
        imageBase = valueIsNotNullNorEmpty(value) ? value : null;
//masuda^   imageBaseが設定されていないならばpdfDirectoryを使用する
        String pdfDir = ClientContext.getPDFDirectory();
        if (imageBase == null && pdfDir != null && !pdfDir.isEmpty()) {
            imageBase = pdfDir;
        }
//masuda$
    }

    @Override
    protected String getImgLocation() {

        if (getContext() == null) {
            view.getDirLbl().setText("");
            return null;
        }

        if (valueIsNullOrEmpty(getImageBase())) {
            view.getDirLbl().setText("画像ディレクトリが指定されていません。");
            return null;
        }

        String pid = getContext().getPatient().getPatientId();
        StringBuilder sb = new StringBuilder();
        sb.append(getImageBase());
        if (!getImageBase().endsWith(File.separator)) {
            sb.append(File.separator);
        }

        sb.append(pid);
        String loc = sb.toString();
        if (loc.length() > 33) {
            sb = new StringBuilder();
            sb.append(loc.substring(0, 15));
            sb.append("...");
            int pos = loc.length() - 15;
            sb.append(loc.substring(pos));
            view.getDirLbl().setText(sb.toString());

        } else {
            view.getDirLbl().setText(loc);
        }

        return loc;
    }

    private ActionMap getActionMap(ResourceBundle resource) {

        ActionMap ret = new ActionMap();

        ImageIcon icon = ClientContext.getImageIconAlias("icon_refresh_small");
        AbstractAction refresh = new AbstractAction("更新", icon) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                scan();
            }
        };
        ret.put("refresh", refresh);

        icon = ClientContext.getImageIconAlias("icon_setting_small");
        AbstractAction doSetting = new AbstractAction("設定", icon) {

            @Override
            public void actionPerformed(ActionEvent ae) {

                // 現在のパラメータを保存し、Setting dialog を開始する
                int oldCount = columnCount();
                boolean oldShow = showFilename();
                boolean oldDisplayIsFilename = displayIsFilename();
                boolean oldSortIsLastModified = sortIsLastModified();
                boolean oldSortIsDescending = sortIsDescending();
                String oldBase = properties.getProperty(PROP_BASE_DIR);
                oldBase = valueIsNotNullNorEmpty(oldBase) ? oldBase : "";

                // 設定ダイアログを起動する
                DefaultSetting setting = new DefaultSetting(DefaultBrowser.this, getUI());
                setting.start();

                // 結果は properties にセットされて返ってくるので save する
                Project.storeProperties(properties, SETTING_FILE_NAME);

                // 新たに設定された値を読む
                boolean newShow = showFilename();
                boolean newDisplayIsFilename = displayIsFilename();
                boolean newSortIsLastModified = sortIsLastModified();
                boolean newSortIsDescending = sortIsDescending();
                String newBase = properties.getProperty(PROP_BASE_DIR);
                newBase = valueIsNotNullNorEmpty(newBase) ? newBase : "";

                // 更新ボタンの enabled
                boolean canRefresh = true;
                canRefresh = canRefresh && (!newBase.equals(""));
                view.getRefreshBtn().setEnabled(canRefresh);

                boolean needsRefresh = false;

                needsRefresh = (needsRefresh
                        || (newShow != oldShow)
                        || (newDisplayIsFilename != oldDisplayIsFilename)
                        || (newSortIsLastModified != oldSortIsLastModified)
                        || (newSortIsDescending != oldSortIsDescending));

                // ベースディレクトリ
                if (!newBase.equals(oldBase)) {
                    setImageBase(newBase);
                } else if (needsRefresh) {
                    scan();
                }
            }
        };
        ret.put("doSetting", doSetting);

        return ret;
    }

    @Override
    protected void initComponents() {

        ResourceBundle resource = ClientContext.getBundle(this.getClass());
        ActionMap map = getActionMap(resource);

        // ImageTable
        view = new DefaultBrowserView();
        JPanel imagePanel = view.getContentPanel();
        imagePanel.putClientProperty(GUIConst.PROP_KARTE_COMPOSITOR, DefaultBrowser.this);
        setImagePanel(imagePanel);
 
        // Button
        view.getSettingBtn().setAction(map.get("doSetting"));
        view.getSettingBtn().setToolTipText("画像ディレクトリ等の設定を行います。");
        view.getRefreshBtn().setAction(map.get("refresh"));
        view.getRefreshBtn().setToolTipText("表示を更新します。");

        boolean canRefresh = true;
        canRefresh = canRefresh && (valueIsNotNullNorEmpty(properties.getProperty(PROP_BASE_DIR)));
        view.getRefreshBtn().setEnabled(canRefresh);

        view.getDirLbl().setToolTipText("画像・PDFディレクトリの場所を表示してます。");
        setUI(view);
    }
}
