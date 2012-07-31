package open.dolphin.order;

import java.awt.Color;
import java.awt.Component;
import java.util.regex.Pattern;
import javax.swing.JTable;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * TensuItemRenderer
 * 
 * @author Kazushi Minagawa.
 * @author modified by masuda, Masuda Naika
 */
public final class TensuItemRenderer extends StripeTableCellRenderer {

    private static final Color THEC_COLOR = new Color(204, 255, 102);
    private static final Color MEDICINE_COLOR = new Color(255, 204, 0);
    private static final Color MATERIAL_COLOR = new Color(153, 204, 255);
    private static final Color OTHER_COLOR = new Color(255, 255, 255);
    private Pattern passPattern;
    private Pattern shinkuPattern;

    public TensuItemRenderer(Pattern passPattern, Pattern shinkuPattern) {
        super();
        this.passPattern = passPattern;
        this.shinkuPattern = shinkuPattern;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        @SuppressWarnings("unchecked")
        ListTableModel<TensuMaster> tm = (ListTableModel<TensuMaster>) table.getModel();
        
        TensuMaster item = tm.getObject(row);

        if (item != null) {

            String slot = item.getSlot();

            if (passPattern != null && passPattern.matcher(slot).find()) {

                String srycd = item.getSrycd();

                if (srycd.startsWith(ClaimConst.SYUGI_CODE_START)
                        && shinkuPattern != null
                        && shinkuPattern.matcher(item.getSrysyukbn()).find()) {
                    setBackground(THEC_COLOR);

                } else if (srycd.startsWith(ClaimConst.YAKUZAI_CODE_START)) {
                    //内用1、外用6、注射薬4
                    String ykzkbn = item.getYkzkbn();

                    if (ykzkbn.equals(ClaimConst.YKZ_KBN_NAIYO)) {
                        setBackground(MEDICINE_COLOR);

                    } else if (ykzkbn.equals(ClaimConst.YKZ_KBN_INJECTION)) {
                        setBackground(MEDICINE_COLOR);

                    } else if (ykzkbn.equals(ClaimConst.YKZ_KBN_GAIYO)) {
                        setBackground(MEDICINE_COLOR);

                    } else {
                        setBackground(OTHER_COLOR);
                    }

                } else if (srycd.startsWith(ClaimConst.ZAIRYO_CODE_START)) {
                    setBackground(MATERIAL_COLOR);

                } else if (srycd.startsWith(ClaimConst.ADMIN_CODE_START)) {
                    setBackground(OTHER_COLOR);

                } else if (srycd.startsWith(ClaimConst.RBUI_CODE_START)) {
                    setBackground(THEC_COLOR);

                } else {
                    setBackground(OTHER_COLOR);
                }

            } else {
                setBackground(OTHER_COLOR);
            }

        } else {
            setBackground(OTHER_COLOR);
        }

        return this;
    }
}
