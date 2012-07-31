
package open.dolphin.client;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;

/**
 * RS_Baseと連携？リンク方法が公開されていたので…
 * 僕はRS_Base持ってないんで動作確認全くしておらずｗ
 *
 * @author masuda, Masuda Naika
 */

public class RsbLink {

    private static final String kanjaGamenLink = "2000.cgi?show=ID";
    private static final String laboDataLink = "labo_ini.cgi?ID=enlarge";
    private static final String idLinkFileName = "ID.dat";
    private static final String showRsbTopSpecialID = "999999999999999";
    //private static final String ptInfoFileName = "kanja.txt";
    //private static final String uketsukeFileName = "IDuke.csv";     // IDはPVTのidがいいか？
    //private static final String shohouFileName = "IDSyoho.csv";     // IDはdocIdがいいか？
    //private static final String shokenFileName = "ID_shoken.dat";   // IDはdocIdがいいか？

private String rsbUrl = Project.getString(MiscSettingPanel.RSB_URL, MiscSettingPanel.DEFAULT_RSB_URL);
    private String rsbRsnPath = Project.getString(MiscSettingPanel.RSB_RSN_PATH, MiscSettingPanel.DEFAULT_RSB_RSN_PATH);
    private String rsbBrowserPath = Project.getString(MiscSettingPanel.RSB_BROWSER_PATH, MiscSettingPanel.DEFAULT_RSB_BROWSER_PATH);

    public  RsbLink(){
    }

    // RS_Baseと電子カルテの画面連携
    public void rsbOpenKanjaGamenLink(String ptid){
        String url = rsbUrl;
        if (!rsbUrl.endsWith("/")){
            url = url + "/";
        }
        url = url + kanjaGamenLink.replace("ID", ptid);
        openRsb(url);
    }
    public void rsbOpenLaboLink(String ptid){
        String url = rsbUrl;
        if (!rsbUrl.endsWith("/")){
            url = url + "/";
        }
        url = url + laboDataLink.replace("ID", ptid);
        openRsb(url);
    }

    private void openRsb(String url) {
        try {
            if ("".equals(rsbBrowserPath.trim())) {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(new URI(url));
            } else {
                    String[] args = new String[]{rsbBrowserPath, url};
                    new ProcessBuilder(args).start();
            }
        } catch (URISyntaxException ex) {
        } catch (IOException ex) {
        }
    }
    // RS_Baseと電子カルテの画面自動連携
    public void showRsbTop(){
        doAutoLink(showRsbTopSpecialID);
    }

    public void doAutoLink(String ptId){

        String rsnPath = rsbRsnPath;

        if (!rsnPath.endsWith(File.separator)){
            rsnPath = rsnPath + File.separator;
        }

        String fileName = rsnPath + idLinkFileName.replace(".dat", "");

        try {

            File oldFile = new File(fileName + ".da_");
            if (oldFile.exists()) {
                oldFile.delete();
            }

            FileOutputStream fos = new FileOutputStream(fileName + ".da_");
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(ptId);
            bw.close();
            osw.close();

            oldFile = new File(fileName + ".dat");
            if (oldFile.exists()) {
                oldFile.delete();
            }

            File objFile = new File(fileName + ".da_");
            objFile.renameTo(new File(fileName + ".dat"));

        } catch (Exception e) {
        }
    }
}
