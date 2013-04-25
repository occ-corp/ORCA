package open.dolphin.tr;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 */
public final class FileListTransferable extends DolphinTransferable {
    
    private List<File> fileList;

    public FileListTransferable(File[] files) {
        fileList = Arrays.asList(files);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {DataFlavor.javaFileListFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor df) {
        return df.equals(DataFlavor.javaFileListFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor df) {
        if (!isDataFlavorSupported(df)) {
            return null;
        }
        return fileList;
    }

    @Override
    public String toString() {
        return "FileList Transferable";
    }
}
