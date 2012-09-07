
package open.dolphin.client;

import java.io.File;
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
    
    private static final String TILDE_SLASH = "~/";
    private static final int TILDE_SLASH_LENGTH = TILDE_SLASH.length();
    
    public  ShowEcgViewer() {
    }

    public void enter(Chart context) {

        String id = context.getPatient().getPatientId();
        String fev40Path = Project.getString(MiscSettingPanel.FEV40_PATH, MiscSettingPanel.DEFAULT_FEV40_PATH);
        String winePath = Project.getString(MiscSettingPanel.WINE_PATH, MiscSettingPanel.DEFAULT_WINE_PATH);
        boolean useWine = Project.getBoolean(MiscSettingPanel.USE_WINE, MiscSettingPanel.DEFAULT_USE_WINE);
        String userHome = System.getProperty("user.home");
        
        if (fev40Path == null || fev40Path.trim().equals("")) {
            return;
        }
        
        if (fev40Path.startsWith(TILDE_SLASH)) {
            StringBuilder sb = new StringBuilder();
            sb.append(userHome);
            sb.append(File.separator);
            sb.append(fev40Path.substring(TILDE_SLASH_LENGTH));
            fev40Path = sb.toString();
        }

        try {
            String[] cmd = useWine
                    ? new String[]{winePath, fev40Path, id}
                    : new String[]{fev40Path, id};

            new ProcessBuilder(cmd).start();
            
        } catch (IOException ex) {
            String msg = "FEF-40を起動できません。\n" + ex.toString();
            String title = ClientContext.getFrameTitle("心電図参照");
            JOptionPane.showMessageDialog(context.getFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
        }
    }
}
