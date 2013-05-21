package open.dolphin.impl.img;

import java.awt.Image;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import open.dolphin.client.*;
import open.dolphin.exception.DolphinException;
import open.dolphin.tr.DolphinTransferHandler;
import open.dolphin.tr.FileListTransferable;

/**
 * BrowserPanelTransferHandler
 *
 * @author modified by masuda, Masuda Naika
 */
public class ImageBrowserPanelTransferHandler extends DolphinTransferHandler {

    private AbstractBrowser browser;
    
    public ImageBrowserPanelTransferHandler(AbstractBrowser browser) {
        this.browser = browser;
    }

    @Override
    protected Transferable createTransferable(JComponent src) {

        JList imageList = (JList) src;
        ImageEntry entry = (ImageEntry) imageList.getSelectedValue();

        if (entry == null || entry.isDirectory()) {
            return null;
        }
        
        startTransfer(src);
        
        // ドラッグ中のイメージを設定する
        Image image = entry.getImageIcon().getImage();
        setDragImage(image);
        
        File f = new File(entry.getPath());
        File[] files = new File[]{f};
        Transferable tr = new FileListTransferable(files);

        return tr;
    }
    
    @Override
    public boolean importData(TransferHandler.TransferSupport support) {

        if (!canImport(support)) {
            importDataFailed();
            return false;
        }

        try {
            // Drag & Drop されたファイルのリストを得る
            Transferable t = support.getTransferable();
            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            List<File> allFiles = new ArrayList<File>();

            for (File file : files) {
                if (!file.isDirectory()) {
                    String name = file.getName();
                    if (name.startsWith(".")) {
                        continue;
                    }
                    if (file.length() == 0L) {
                        continue;
                    }
                    allFiles.add(file);
                } else {
                    listAll(file, allFiles);
                }
            }

            if (!allFiles.isEmpty()) {
                parseFiles(allFiles);

            }
            importDataSuccess(support.getComponent());
            return true;

        } catch (UnsupportedFlavorException ufe) {
            ufe.printStackTrace(System.err);
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
        
        importDataFailed();
        return false;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {

        // ファイルが消去されてたらアイコンを消す
        ImageEntryJList jlist = (ImageEntryJList) source;
        DefaultListModel<ImageEntry> model = (DefaultListModel) jlist.getModel();
        
        Enumeration<ImageEntry> enu = model.elements();
        FileSystem fs = FileSystems.getDefault();
        while (enu.hasMoreElements()) {
            ImageEntry entry = enu.nextElement();
            String path = entry.getPath();
            if (!Files.exists(fs.getPath(path))) {
                model.removeElement(entry);
            }
        }

        endTransfer();
    }

    @Override
    public boolean canImport(TransferSupport support) {

        if (!support.isDrop()) {
            return false;
        }
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return false;
        }
        if (support.getComponent() == srcComponent) {
            return false;
        }

        return true;
    }

    private void parseFiles(final List<File> imageFiles) {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {

                String baseDir = browser.getImageBase();
                if (baseDir == null) {
                    return null;
                }

                String patientId = browser.getContext().getPatient().getPatientId();
                StringBuilder sb = new StringBuilder();
                sb.append(baseDir).append(File.separator).append(patientId);
                String dirStr = sb.toString();
                File dir = new File(dirStr);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        throw new DolphinException("画像用のディレクトリを作成できません。");
                    }
                }

                // java.nio.file.Filesを使ってみる
                for (File src : imageFiles) {
                    Path srcPath = src.toPath();
                    Path destPath = new File(browser.getCurrentDir(), src.getName()).toPath();
                    if (browser.dropIsMove()) {
                        Files.move(srcPath, destPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.copy(srcPath, destPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                    }
                    browser.addThumbnailImageEntry(destPath);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException ex) {
                    ClientContext.getBootLogger().warn(ex);
                } catch (ExecutionException ex) {
                    ClientContext.getBootLogger().warn(ex);
                    Window parent = SwingUtilities.getWindowAncestor(browser.getUI());
                    String message = "ファイルをコピーできません。\n" + ex.getMessage();
                    String title = ClientContext.getFrameTitle(browser.getTitle());
                    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void listAll(File dir, List<File> list) {

        File[] files = dir.listFiles();

        for (File f : files) {
            if (f.isDirectory()) {
                listAll(f, list);
            } else {
                list.add(f);
            }
        }
    }
}
