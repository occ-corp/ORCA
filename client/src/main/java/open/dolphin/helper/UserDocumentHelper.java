package open.dolphin.helper;

import java.awt.Window;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import open.dolphin.client.ClientContext;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc. 
 * @author modified by masuda, Masuda Naika
 */
public class UserDocumentHelper {

    /**
     * PDF/OpenOffice差し込み文書Fileへのパスを生成する。 File name =
     * 患者氏名_文書名_YYYY-MM-DD(N).pdf/odt
     */
    public static String createPathToDocument(String dirStr, String docName, String ext, String ptName, Date d, Window parent) {

        // direcory をチェックする
        File dir;
        if (dirStr == null || dirStr.equals("")) {
            dir = new File(ClientContext.getPDFDirectory());
        } else {
            dir = new File(dirStr);
            if (!dir.exists()) {
                boolean ok = dir.mkdir();
                if (!ok) {
                    // dirStr!=null で dirが生成できない時
                    // PDF directory を使用する これは生成されている
                    dir = new File(ClientContext.getPDFDirectory());
                }
            }
        }

        // 拡張子チェック
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }

        // 患者氏名の空白を削除する
        ptName = ptName.replace(" ", "");
        ptName = ptName.replace("　", "");

        // 日付
        String dStr = new SimpleDateFormat("yyyy-MM-dd").format(d);

        // File 名を構成する
        StringBuilder sb = new StringBuilder();
        sb.append(ptName).append("_");
        sb.append(docName).append("_");
        sb.append(dStr);
        String fileName = sb.toString();
        sb.append(ext);
        String test = sb.toString();
        
        // FileChooserを表示
        JFileChooser fileChooser = new JFileChooser(dirStr);
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setDialogTitle("PDF出力");
        File current = fileChooser.getCurrentDirectory();
        fileChooser.setSelectedFile(new File(current.getPath(), test));
        int selected = fileChooser.showSaveDialog(parent);
        if (selected != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File ret = fileChooser.getSelectedFile();

        if (ret.exists()) {
            String title = "上書き確認";
            String message = "既存のファイル " + ret.toString() + "\n" + "を上書きしようとしています。続けますか？";
            selected = showConfirmDialogCancelDefault(title, message, parent);
            
            switch (selected) {
                case 0:
                    return null;
                case 1:
                    // 存在しなくなるまで (n) をつける
                    int cnt = 0;
                    do {
                        cnt++;
                        sb = new StringBuilder();
                        sb.append(fileName);
                        sb.append("(").append(cnt).append(")").append(ext);
                        test = sb.toString();
                        ret = new File(dir, test);
                    } while (ret.exists());
            }
        }

        return ret.getPath();
    }
    
    private static int showConfirmDialogCancelDefault(String title, String message, Window parent) {

        String[] options = {"いいえ", "番号振り", "はい"};
        int selected = JOptionPane.showOptionDialog(parent, message, title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        return selected;
    }
}
