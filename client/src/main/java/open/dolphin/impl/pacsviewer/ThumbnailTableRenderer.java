
package open.dolphin.impl.pacsviewer;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import open.dolphin.util.DicomImageEntry;

/**
 * サムネイルテーブルのレンダラ
 *
 * @author masuda, Masuda Naika
 */

public class ThumbnailTableRenderer extends DefaultTableCellRenderer {

    public ThumbnailTableRenderer() {
        setVerticalTextPosition(JLabel.BOTTOM);
        setHorizontalTextPosition(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean isFocused,
            int row, int col) {
        Component compo = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                isFocused,
                row, col);
        JLabel l = (JLabel) compo;

        if (value != null) {
            DicomImageEntry entry = (DicomImageEntry) value;
            l.setIcon(entry.getImageIcon());
            String name = entry.getFileName();
            l.setText(name);
            l.setToolTipText(name);
        } else {
            l.setIcon(null);
            l.setText(null);
        }
        return compo;
    }
}
