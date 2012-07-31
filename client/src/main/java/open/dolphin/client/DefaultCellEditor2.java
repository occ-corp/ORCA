
package open.dolphin.client;

import java.awt.Color;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

/**
 *
 */
public class DefaultCellEditor2 extends DefaultCellEditor {
    /**
     * Constructs a DefaultCellEditor that uses a text field.
     *
     * @param textField  a JTextField object
     */
    public DefaultCellEditor2(JTextField textField) {
        super(textField);
        textField.setBorder(new LineBorder(Color.gray));
    }

    /**
     * Constructs a DefaultCellEditor object that uses
     * a check box.
     *
     * @param checkBox  a JCheckBox object
     */
    public DefaultCellEditor2(JCheckBox checkBox) {
        super(checkBox);
        checkBox.setBorder(new LineBorder(Color.gray));
    }

    /**
     * Constructs a DefaultCellEditor object that uses a
     * combo box.
     *
     * @param comboBox  a JComboBox object
     */
    public DefaultCellEditor2(JComboBox comboBox) {
        super(comboBox);
        comboBox.setBorder(new LineBorder(Color.gray));
    }
}
