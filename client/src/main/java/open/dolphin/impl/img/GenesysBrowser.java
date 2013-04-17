package open.dolphin.impl.img;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import open.dolphin.client.ClientContext;
import open.dolphin.client.GUIConst;
import open.dolphin.client.ImageEntry;
import open.dolphin.project.Project;

/**
 * GenesysBrowser
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class GenesysBrowser extends AbstractBrowser {

    private static final String TITLE = "Genesys";
    private static final String GENESYS_FILE = "Genesys_";
    private static final String GENESYS_URL = "http://:genesysServer/T1Web/basis/search_Form.aspx?userid=:userid&pid=:pid";
    private static final String GENESYS_URL_WITH_SOP = "http://:genesysServer/T1Web/basis/search_Form.aspx?userid=:userid&pid=:pid&sop=:sop";
    private static final String PARAM_GENESYS_SERVER = ":genesysServer";
    private static final String PARAM_USER_ID = ":userid";
    private static final String PARAM_PID = ":pid";
    private static final String PARAM_SOP = ":sop";

    private static final String PROP_GENESYS_SERVER = "genesysServer";
    private static final String PROP_GENESYS_VIEWER = "genesysView";
    private static final String SETTING_FILE_NAME = "genesys.properties";

    private GenesysBrowserView view;

    
    public GenesysBrowser() {
        
        setTitle(TITLE);

        properties = getProperties();
        Project.loadProperties(properties, SETTING_FILE_NAME);

        String dir = properties.getProperty(PROP_BASE_DIR);
        imageBase = valueIsNotNullNorEmpty(dir) ? dir : null;
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

    public void viewGenesys() {

        // Genesys Server IP Address
        String genesysServer = properties.getProperty(PROP_GENESYS_SERVER);

        if (valueIsNullOrEmpty(genesysServer)) {
            return;
        }
        
        String url = GENESYS_URL;
        url = url.replaceFirst(PARAM_GENESYS_SERVER, genesysServer);
        url = url.replaceFirst(PARAM_USER_ID, Project.getUserId());
        url = url.replaceFirst(PARAM_PID, getContext().getPatient().getPatientId());

        // 既定のブラウザでオープンする
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(url));
            } catch (URISyntaxException ex) {
                ClientContext.getBootLogger().warn(ex);
            } catch (IOException ex) {
                ClientContext.getBootLogger().warn(ex);
            }
        }
    }

    private void openGenesysWithSop(String filename) {

        // Genesys Server IP Address
        String genesysServer = properties.getProperty(PROP_GENESYS_SERVER);

        if (valueIsNullOrEmpty(genesysServer)) {
            return;
        }

        int index1 = filename.lastIndexOf("_");
        int index2 = filename.lastIndexOf(".");
        String sop = filename.substring(index1+1, index2);

        String url = GENESYS_URL_WITH_SOP;
        url = url.replaceFirst(PARAM_GENESYS_SERVER, genesysServer);
        url = url.replaceFirst(PARAM_USER_ID, Project.getUserId());
        url = url.replaceFirst(PARAM_PID, getContext().getPatient().getPatientId());
        url = url.replaceFirst(PARAM_SOP, sop);

        // 既定のブラウザでオープンする
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(url));
            } catch (URISyntaxException ex) {
                ClientContext.getBootLogger().warn(ex);
            } catch (IOException ex) {
                ClientContext.getBootLogger().warn(ex);
            }
        }
    }

    @Override
    protected void openImage(ImageEntry entry) {

        // Genesys File
        String fileName = entry.getFileName();
        if (fileName.startsWith(GENESYS_FILE)) {
            openGenesysWithSop(fileName);

        } else {
            super.openImage(entry);
        }
    }

    private ActionMap getActionMap(ResourceBundle resource) {

        ActionMap ret = new ActionMap();

        String text = resource.getString("refresh.Action.text");
        ImageIcon icon = ClientContext.getImageIcon("os_refresh_16.png");
        AbstractAction refresh = new AbstractAction(text, icon) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                scan();
            }
        };
        ret.put("refresh", refresh);

        text = resource.getString("doSetting.Action.text");
        icon = ClientContext.getImageIcon("os_wrench_16.png");
        AbstractAction doSetting = new AbstractAction(text, icon) {

            @Override
            public void actionPerformed(ActionEvent ae) {

                // 現在のパラメータを保存し、Setting dialog を開始する
                boolean oldShow = showFilename();
                boolean oldDisplayIsFilename = displayIsFilename();
                boolean oldSortIsLastModified = sortIsLastModified();
                boolean oldSortIsDescending = sortIsDescending();
                String oldBase = properties.getProperty(PROP_BASE_DIR);
                oldBase = valueIsNotNullNorEmpty(oldBase) ? oldBase : "";

                // 設定ダイアログを起動する
                GenesysSetting setting = new GenesysSetting(GenesysBrowser.this, getUI());
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

                // Genesys ボタンの enabled
                boolean canLaunch = true;   //ClientContext.isWin();
                canLaunch = canLaunch && (valueIsNotNullNorEmpty(properties.getProperty("genesysServer")));
                view.getGenesysBtn().setEnabled(canLaunch);

                boolean needsRefresh = false;
                needsRefresh = (needsRefresh ||
                                (newShow!=oldShow) ||
                                (newDisplayIsFilename!=oldDisplayIsFilename) ||
                                (newSortIsLastModified!=oldSortIsLastModified) ||
                                (newSortIsDescending!=oldSortIsDescending));

                // ベースディレクトリ
                if (!newBase.equals(oldBase)) {
                    setImageBase(newBase);
                } else if (needsRefresh) {
                    scan();
                }
            }
        };
        ret.put("doSetting", doSetting);
        icon = ClientContext.getImageIcon("os_world_16.png");
        text = resource.getString("viewGenesys.Action.text");

         AbstractAction viewGenesys = new AbstractAction(text, icon) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                viewGenesys();
            }
         };
         ret.put("viewGenesys", viewGenesys);

        return ret;
    }
    
    @Override
    protected void initComponents() {
        
        ResourceBundle resource = ClientContext.getBundle(this.getClass());
        ActionMap map = getActionMap(resource);
        
        // ImageTable
        view = new GenesysBrowserView();
        JPanel imagePanel = view.getContentPanel();
        imagePanel.putClientProperty(GUIConst.PROP_KARTE_COMPOSITOR, GenesysBrowser.this);
        setImagePanel(imagePanel);

        // buttons
        view.getSettingBtn().setAction(map.get("doSetting"));
        view.getRefreshBtn().setAction(map.get("refresh"));
        boolean enabled = true;
        enabled = enabled && valueIsNotNullNorEmpty(properties.getProperty(PROP_BASE_DIR));
        view.getRefreshBtn().setEnabled(enabled);
        view.getGenesysBtn().setAction(map.get("viewGenesys"));
        boolean canLaunch = true;   //ClientContext.isWin();
        canLaunch = canLaunch && (valueIsNotNullNorEmpty(properties.getProperty("genesysServer")));
        view.getGenesysBtn().setEnabled(canLaunch);
        setUI(view);
    }
}
