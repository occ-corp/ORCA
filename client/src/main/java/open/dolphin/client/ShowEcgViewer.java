
package open.dolphin.client;

import java.io.IOException;
import javax.swing.JOptionPane;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;

/**
 * フクダ電子 心電図ビューア FEV-40を起動する
 *
 * @author masuda, Masuda Naika
 */

public class ShowEcgViewer {
    
    private static final String WINE_KEY = "~/.wine/";
    private static final String USER_HOME = "~";
    private static final String WINE_PATH = "/opt/local/bin/wine";

    public  ShowEcgViewer() {
    }

    public void enter(Chart context) {

        String id = context.getPatient().getPatientId();
        String fev40Path = Project.getString(MiscSettingPanel.FEV40_PATH, MiscSettingPanel.DEFAULT_FEV40_PATH);

        if (fev40Path == null || fev40Path.trim().equals("")) {
            return;
        }


        try {
            String[] cmd = null;
            if (fev40Path.contains(WINE_KEY)) {
                String home = System.getProperty("user.home");
                fev40Path = fev40Path.replace(USER_HOME, home);
                cmd = new String[]{WINE_PATH, fev40Path, id};
            } else {
                cmd = new String[]{fev40Path, id};
            }
            new ProcessBuilder(cmd).start();
            
        } catch (IOException ex) {
            String msg = "FEF-40を起動できません。\n" + ex.toString();
            String title = ClientContext.getFrameTitle("心電図参照");
            JOptionPane.showMessageDialog(context.getFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
        }
    }
}
