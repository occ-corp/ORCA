package open.dolphin.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.*;

/**
 * RpEditor.javaでスクリーンテンキーをpopup、あるいは
 * JDialogに表示する, Re-programed on 2010/11/24
 *
 * @author masuda, Masuda Naika
 */

public class ScreenTenKey {

    public static final String SCREEN_TENKEY_OUTPUT = "ScreenTenKeyOutput";
    private PropertyChangeSupport boundSupport;
    private String input;
    private String memo;
    private boolean popup;
    private boolean overwriteNext;
    private JDialog dialog;
    private JPanel tenkeyPanel;
    private JTextField tf_number;
    private boolean validResult = false;
    private TenkeyActionListener listener;

    public ScreenTenKey() {
        initComponents();
    }

    public void setInput(String str) {
        overwriteNext = true;
        input = str;
    }

    public void setMemo(String str) {
        memo = str;
    }

    public JPanel getPopupPanel() {
        setText(input);
        popup = true;
        return tenkeyPanel;
    }

    public String enterDialog() {
        setText(input);
        popup = false;

        // dialogを表示する
        dialog = new JDialog((Frame) null, true);
        ClientContext.setDolphinIcon(dialog);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        dialog.add(tenkeyPanel, BorderLayout.CENTER);

        // dialogのタイトルを設定
        dialog.setTitle("数量入力");
        if (memo != null) {
            JLabel lbl = new JLabel(memo);
            dialog.add(lbl, BorderLayout.NORTH);
        }
        dialog.pack();
        int h = dialog.getPreferredSize().height;
        int w = dialog.getPreferredSize().width;

        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        int x = pointerInfo.getLocation().x - w / 2;
        int y = pointerInfo.getLocation().y - h / 2;
        dialog.setLocation(x, y);
        dialog.setVisible(true);

        if (validResult) {
            return getText();
        } else {
            return null;
        }
    }

    private void exit() {
        // popupの場合はfirePropertyChangeする
        // RpEditorでキャッチする
        if (popup) {
            boundSupport.firePropertyChange(SCREEN_TENKEY_OUTPUT, null, getText());
        } else {
            validResult = true;
            dialog.setVisible(false);
            dialog.dispose();
        }
        listener = null;
    }

    private void initComponents() {

        listener = new TenkeyActionListener();
        tenkeyPanel = new JPanel();
        tenkeyPanel.setLayout(new BorderLayout());
        tf_number = new JTextField();
        tf_number.setFont(new Font("Dialog", Font.PLAIN, 22));
        tf_number.setHorizontalAlignment(JTextField.RIGHT);
        tenkeyPanel.add(tf_number, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 3));
        panel.add(makeButton("1", "１"));
        panel.add(makeButton("2", "２"));
        panel.add(makeButton("3", "３"));
        panel.add(makeButton("4", "４"));
        panel.add(makeButton("5", "５"));
        panel.add(makeButton("6", "６"));
        panel.add(makeButton("7", "７"));
        panel.add(makeButton("8", "８"));
        panel.add(makeButton("9", "９"));
        panel.add(makeButton("0", "０"));
        panel.add(makeButton(".", "．"));
        panel.add(makeButton("Ｃ", "Ｃ"));
        panel.add(makeButton("14", "14"));
        panel.add(makeButton("28", "28"));
        panel.add(makeButton("Ent", "Ent"));

        tenkeyPanel.add(panel, BorderLayout.CENTER);
    }

    private TenkeyButton makeButton(String cmd, String text) {

        TenkeyButton btn = new TenkeyButton(cmd, text);
        btn.addActionListener(listener);
        return btn;
    }

    private class TenkeyActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = ((TenkeyButton) e.getSource()).getCommand();
            tenkeyAction(cmd);
        }
    }

    private void tenkeyAction(String cmd) {

        boolean overwrite = false;

        String str = getText();
        if ("Ent".equals(cmd)) {
            exit();
            return;
        } else if ("Ｃ".equals(cmd)) {
            cmd = "0";
            overwrite = true;
        } else if ("14".equals(cmd) || "28".equals(cmd)) {
            overwrite = true;
        } else if (".".equals(cmd)) {
            if (str.contains(".")) {
                return;
            } else if (overwriteNext) {
                cmd = "0.";
            }
        }
        if (overwrite || overwriteNext) {
            setText(cmd);
            overwriteNext = overwrite;
        } else {
            setText(getText() + cmd);
        }
    }

    private String getText() {
        return tf_number.getText().trim();
    }

    private void setText(String text) {
        tf_number.setText(text);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(SCREEN_TENKEY_OUTPUT, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (boundSupport != null) {
            boundSupport.removePropertyChangeListener(listener);
        }
    }

    private class TenkeyButton extends JButton {

        private String cmd;

        private TenkeyButton(String cmd, String text) {
            setText(text);
            this.cmd = cmd;
        }

        private String getCommand() {
            return cmd;
        }
    }
}
