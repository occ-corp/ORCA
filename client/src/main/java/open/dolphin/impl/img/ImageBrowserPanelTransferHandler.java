package open.dolphin.impl.img;

import java.awt.Container;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import open.dolphin.client.*;
import open.dolphin.exception.DolphinException;
import open.dolphin.helper.WindowSupport;
import open.dolphin.tr.AbstractImagePanelTransferHandler;
import open.dolphin.tr.FileListTransferable;

/**
 * BrowserPanelTransferHandler
 *
 * @author modified by masuda, Masuda Naika
 */
public class ImageBrowserPanelTransferHandler extends AbstractImagePanelTransferHandler {

    private static ImageBrowserPanelTransferHandler instance;

    static {
        instance = new ImageBrowserPanelTransferHandler();
    }

    private ImageBrowserPanelTransferHandler() {
    }

    public static ImageBrowserPanelTransferHandler getInstance() {
        return instance;
    }


    @Override
    public void mouseClicked(MouseEvent e) {

        ImagePanel imagePanel = (ImagePanel) e.getComponent();
        ImageLabel imageLabel = imagePanel.getSelectedImageLabel();
        if (imageLabel == null) {
            return;
        }

        if (e.getClickCount() == 2) {
            AbstractBrowser browser = (AbstractBrowser) imagePanel.getClientProperty(GUIConst.PROP_KARTE_COMPOSITOR);
            browser.openImage(imageLabel.getImageEntry());
        }
    }

    @Override
    public void maybeShowPopup(MouseEvent e) {

        ImagePanel imagePanel = (ImagePanel) e.getComponent();
        ImageLabel imageLabel = imagePanel.getSelectedImageLabel();
        if (imageLabel == null) {
            return;
        }

        if (e.isPopupTrigger() && e.getClickCount() == 1) {
            JPopupMenu contextMenu = new JPopupMenu();
            JMenuItem micp = new JMenuItem("コピー");
            Container c = imagePanel.getTopLevelAncestor();
            if (c instanceof JFrame) {
                JFrame frame = (JFrame) c;
                Object objMediator = WindowSupport.getMediator(frame);
                if (objMediator != null && objMediator instanceof ChartMediator) {
                    ChartMediator mediator = (ChartMediator) objMediator;
                    Action copy = mediator.getAction(GUIConst.ACTION_COPY);
                    copy.setEnabled(imageLabel != null);
                    micp.setAction(copy);
                    contextMenu.add(micp);
                }
            }
            contextMenu.show(imagePanel, e.getX(), e.getY());
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {

        ImagePanel imagePanel = (ImagePanel) c;
        ImageLabel imageLabel = imagePanel.getSelectedImageLabel();
        if (imageLabel == null) {
            return null;
        }
        ImageEntry entry = imageLabel.getImageEntry();
        if (entry != null) {
            File f = new File(entry.getPath());
            File[] files = new File[1];
            files[0] = f;
            Transferable tr = new FileListTransferable(files);
            return tr;
        }
        return null;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {

        if (!canImport(support)) {
            return false;
        }

        if (isCopyToSameDir(support)) {
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

            if (allFiles.size() > 0) {
                JPanel panel = (JPanel) support.getComponent();
                AbstractBrowser browser = (AbstractBrowser) panel.getClientProperty(GUIConst.PROP_KARTE_COMPOSITOR);
                if (browser != null) {
                    parseFiles(allFiles, browser);
                }
            }
            return true;

        } catch (UnsupportedFlavorException ufe) {
            ufe.printStackTrace(System.err);
        } catch (IOException ieo) {
            ieo.printStackTrace(System.err);
        }
        return false;
    }

    @Override
    public boolean canImport(TransferSupport support) {

        if (!support.isDrop()) {
            return false;
        }
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return false;
        }
        /*
        // canImportで同じフォルダかどうか調べたいがbugのためムリ ;_;
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6759788 bug!
        if (isCopyToSameDir(support)) {
        return false;
        }
         */
        return true;
    }

    // 同じフォルダ間のコピーかどうかを調べる
    private boolean isCopyToSameDir(TransferSupport support) {

        JPanel panel = (JPanel) support.getComponent();
        // ImageBrowserを取得
        AbstractBrowser browser = (AbstractBrowser) panel.getClientProperty(GUIConst.PROP_KARTE_COMPOSITOR);
        if (browser != null) {
            // browserから対象フォルダを取得
            String imgLoc = browser.getImgLocation();
            Transferable tr = support.getTransferable();
            try {
                List<File> files = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                // ドラッグしたファイルがbrowserのフォルダならばtrueを返す
                for (File file : files) {
                    String filePath = file.getParent();
                    if (imgLoc != null && imgLoc.equals(filePath)) {
                        return true;
                    }
                }
            } catch (UnsupportedFlavorException ex) {
            } catch (IOException ex) {
            }
        }
        return false;
    }

    private void parseFiles(final List<File> imageFiles, final AbstractBrowser context) {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {

                String baseDir = context.getImageBase();
                if (baseDir == null) {
                    return null;
                }

                String patientId = context.getContext().getPatient().getPatientId();
                StringBuilder sb = new StringBuilder();
                sb.append(baseDir).append(File.separator).append(patientId);
                String dirStr = sb.toString();
                File dir = new File(dirStr);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        throw new DolphinException("画像用のディレクトリを作成できません。");
                    }
                }

                for (File src : imageFiles) {
                    File dest = new File(dirStr, src.getName());
                    FileChannel in = (new FileInputStream(src)).getChannel();
                    FileChannel out = (new FileOutputStream(dest)).getChannel();
                    in.transferTo(0, src.length(), out);
                    in.close();
                    out.close();
                    dest.setLastModified(src.lastModified());
                }

                if (context.dropIsMove()) {
                    while (imageFiles.size() > 0) {
                        File delete = imageFiles.remove(0);
                        delete.delete();
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    context.scan();
                } catch (InterruptedException ex) {
                    ClientContext.getBootLogger().warn(ex);
                } catch (ExecutionException ex) {
                    ClientContext.getBootLogger().warn(ex);
                    Window parent = SwingUtilities.getWindowAncestor(context.getUI());
                    String message = "ファイルをコピーできません。\n" + ex.getMessage();
                    String title = ClientContext.getFrameTitle(context.getTitle());
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
