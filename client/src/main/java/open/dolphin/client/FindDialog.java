package open.dolphin.client;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * FindDialog の JDialog バージョン
 * @author pns
 * @author modified by masuda, Masuda Naika
 */
public class FindDialog {

    private JDialog dialog;
    private Frame parent;
    private boolean isSearchReady;
    private boolean isTextReady;
    private boolean isSoapBoxReady;

    private boolean isSoaBoxChecked; // soaBox がチェックされているかどうか
    private boolean isPBoxChecked; // pBox がチェックされているかどうか

    private static String searchText;
    private JCheckBox soaBox;
    private JCheckBox pBox;
    private JTextField searchTextField;
    private JButton searchButton;
    private JButton cancelButton;

    public FindDialog(Chart context) {      // 引数をFrameから変更
        parent = context.getFrame();
        searchText = context.getPatient().getSearchText();
    }

    public void start() {
        initComponents();
        connect();
        dialog.setVisible(true);
    }

    private void initComponents() {

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // TextField
        searchTextField = new JTextField(20);
        searchTextField.addFocusListener(AutoKanjiListener.getInstance());
        content.add(new JLabel("検索語"));
        content.add(searchTextField);
        // CheckBox
        soaBox = new JCheckBox("所見・症状欄");
        pBox = new JCheckBox("処置欄");
        content.add(soaBox);
        content.add(pBox);
        // Buttons
        searchButton = new JButton("検索");
        cancelButton = new JButton("キャンセル");

        Object[] options = new Object[] { searchButton, cancelButton };

        // OptinoPane
        JOptionPane jop = new JOptionPane(
                content,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                searchButton);

        dialog = jop.createDialog(parent, "カルテ検索");

        // 初期値設定
        soaBox.setSelected(true);
        pBox.setSelected(true);
        searchButton.setEnabled(false);
        searchTextField.setText(searchText);
        checkState();
    }

    private void connect() {

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                searchTextField.requestFocusInWindow();
                searchTextField.selectAll();
            }
            @Override
            public void windowActivated(WindowEvent e) {
                searchTextField.requestFocusInWindow();
                searchTextField.selectAll();
            }
        });

        // 検索文字列があれば，検索ボタンを有効化する　入力中にチェックする
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                checkState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkState();
            }
        });

        // リターンが押されたときの処理
        searchTextField.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                onTextAction();
            }
        });

        // ボタン類
        searchButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        pBox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                checkState();
            }
        });

        soaBox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                checkState();
            }
        });
    }

    // 検索ボタンの ON/OFF 制御
    private void setSearchButtonState() {
        if (isSoapBoxReady && isTextReady) {
            isSearchReady = true;
        }
        else {
            isSearchReady = false;
        }
        searchButton.setEnabled(isSearchReady);
        searchButton.setSelected(isSearchReady);
    }


    private void doSearch() {
        searchText = searchTextField.getText();
        close();
    }

    private void doCancel() {
        searchText = "";
        close();
    }

    // 検索ボタンの状態を制御
    private void checkState () {
        if (searchTextField.getText().equals("")) {
            isTextReady = false;
        }
        else {
            isTextReady = true;
        }

        isSoaBoxChecked = soaBox.isSelected();
        isPBoxChecked = pBox.isSelected();
        if (isSoaBoxChecked || isPBoxChecked) {
            isSoapBoxReady = true;
        }
        else {
            isSoapBoxReady = false;
        }

        setSearchButtonState();
    }

    private void close() {
        dialog.setVisible(false);
        dialog.dispose();
    }

    /**
     * Text フィールドでリターンキーが押された時の処理を行う。
     */
    private void onTextAction() {
        if (isTextReady && isSearchReady) {
            searchButton.doClick();
        }
    }

    // 結果を保持している部分
    public String getSearchText() {
       return searchText;
    }

    public boolean isSoaBoxOn() {
        return isSoaBoxChecked;
    }

    public boolean isPBoxOn() {
        return isPBoxChecked;
    }

}
