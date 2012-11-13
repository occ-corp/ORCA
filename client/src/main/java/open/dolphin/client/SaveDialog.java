package open.dolphin.client;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.SimpleDate;
import open.dolphin.project.Project;

/**
 * SaveDialog
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class SaveDialog {
    
    private static final String[] PRINT_COUNT 
            = {"0", "1",  "2",  "3",  "4", "5"};
    private static final String[] TITLE_LIST = {"経過記録", "処方", "処置", "検査", "画像", "指導"};
    private static final String TITLE = "ドキュメント保存";
    private static final String SAVE = "保存";
    private static final String TMP_SAVE = "仮保存";
    
    private Window parent;
    // ダイアログ
    private JDialog dialog;
    
    // 保存ボタン
    private JButton okButton;
    // キャンセルボタン
    private JButton cancelButton;
    // 仮保存ボタン
    private JButton tmpButton;
    
    private JCheckBox patientCheck;
    private JCheckBox clinicCheck;
    
    private JTextField titleField;
    private JComboBox titleCombo;
    private JComboBox printCombo;
    private JLabel departmentLabel;
    
    // CLAIM 送信
    private JCheckBox sendClaim;
    // LabTest 送信
    private JCheckBox sendLabtest;
    
    // 戻り値のSaveParams
    private SaveParams value;
    
    // 保存日変更関連
    private JTextField dateField;
    private JCheckBox cb_dateEnable;
    private JButton btnNow;

    // 入力値のSaveParams
    private SaveParams saveParams;
    // 退院日登録
    private JCheckBox cb_registEndDate;


    public SaveDialog(Window parent) {
        this.parent = parent;
    }
    
    public void start() {
        dialog.setVisible(true);
    }
    
    public SaveParams getValue() {
        return value;
    }
    
    /**
     * コンポーネントにSaveParamsの値を設定する。
     */
    public void setValue(SaveParams params) {
        
        saveParams = params;
        
        JPanel contentPanel = createComponent();

        Object[] options = new Object[]{okButton, tmpButton, cancelButton};

        JOptionPane jop = new JOptionPane(
                contentPanel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                okButton);

        dialog = jop.createDialog(parent, ClientContext.getFrameTitle(TITLE));
        
        // Titleを表示する
        // 修正元のタイトルもコンボボックスに入れる   
        String[] titles = new String[]{params.getOldTitle(), params.getTitle()};
        for (String str : titles) {
            if (str != null && (!str.equals("") && (!str.equals("経過記録")))) {
                titleCombo.insertItemAt(str, 0);
            }
        }
        titleCombo.setSelectedIndex(0);
        
        // 診療科を表示する
        // 受付情報からの診療科を設定する
        String val = params.getDepartment();
        if (val != null) {
            String[] depts = val.split("\\s*,\\s*");
            if (depts[0] != null) {
                departmentLabel.setText(depts[0]);
            } else {
                departmentLabel.setText(val);
            }
        }
        
        // 印刷部数選択
        int count = params.getPrintCount();
        if (count != -1) {
            printCombo.setSelectedItem(String.valueOf(count));
            
        } else {
            printCombo.setEnabled(false);
        }

        //--------------------------------
        // CLAIM 送信をチェックする
        //--------------------------------
        sendClaim.setSelected(params.isSendClaim());

        //-------------------------------
        // MML 送信の場合、アクセス権を設定する
        //-------------------------------
        if (params.getSendMML()) {
            // 患者への参照と診療歴のある施設の参照許可を設定する
            boolean permit = params.isAllowPatientRef();
            patientCheck.setSelected(permit);
            permit = params.isAllowClinicRef();
            clinicCheck.setSelected(permit);
            
        } else {
            // MML 送信をしないときdiasbleにする
            patientCheck.setEnabled(false);
            clinicCheck.setEnabled(false);
        }

        //--------------------------------
        // 送信機能の自体の enable/disable
        //--------------------------------
        boolean send = params.isSendEnabled();
        sendClaim.setEnabled(send);

        //-------------------------------
        // 検体検査オーダー送信
        //-------------------------------
        sendLabtest.setSelected(params.isSendLabtest() && params.isHasLabtest());
        sendLabtest.setEnabled((send && params.isHasLabtest()));
        
        checkTitle();
        
        controlButton();
    }

    
    /**
     * GUIコンポーネントを初期化する。
     */
    private JPanel createComponent() {
                
        // content
        JPanel content = new JPanel();
        content.setLayout(new GridLayout(0, 1));
        
        // 文書Title
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleCombo = new JComboBox(TITLE_LIST);
        titleCombo.setPreferredSize(new Dimension(220, titleCombo.getPreferredSize().height));
        titleCombo.setMaximumSize(titleCombo.getPreferredSize());
        titleCombo.setEditable(true);
        p.add(new JLabel("タイトル:"));
        p.add(titleCombo);
        content.add(p);
        
        // ComboBox のエディタコンポーネントへリスナを設定する
        titleField = (JTextField) titleCombo.getEditor().getEditorComponent();
        titleField.addFocusListener(AutoKanjiListener.getInstance());
        titleField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                checkTitle();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkTitle();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkTitle();
            }
        });
        
        // 診療科、印刷部数を表示するラベルとパネルを生成する
        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        departmentLabel = new JLabel();
        p1.add(new JLabel("診療科:"));
        p1.add(departmentLabel);
        
        p1.add(Box.createRigidArea(new Dimension(11, 0)));
        
        // Print
        printCombo = new JComboBox(PRINT_COUNT);
        printCombo.setSelectedIndex(1);
        p1.add(new JLabel("印刷部数:"));
        p1.add(printCombo);
        
        content.add(p1);
        
//masuda^
        // 保存日変更パネルを追加
        if (saveParams.isDateEditable()) {
            // デフォルトの保存日（現在）をセット
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_8601_DATE_FORMAT);
            String dateStr = frmt.format(saveParams.getConfirmed());
            dateField = new JTextField(12);
            dateField.setToolTipText("右クリックでカレンダーがポップアップします");
            dateField.setText(dateStr);
            dateField.addFocusListener(AutoRomanListener.getInstance());
            int[] range = {-12, 2};
            PopupListener pl = new PopupListener(dateField, range);
            
            DocumentListener dl = new DocumentListener() {
                @Override
                public void changedUpdate(DocumentEvent e) {
                }
                @Override
                public void insertUpdate(DocumentEvent e) {
                    controlButton();
                }
                @Override
                public void removeUpdate(DocumentEvent e) {
                    controlButton();
                }
            };
            dateField.getDocument().addDocumentListener(dl);

            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            cb_dateEnable = new JCheckBox("記録日変更:");
            cb_dateEnable.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent evt) {
                    boolean b = cb_dateEnable.isSelected();
                    dateField.setEnabled(b);
                    btnNow.setEnabled(b);
                }
            });
            
            btnNow = new JButton("現時刻");
            btnNow.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e) {
                    SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_8601_DATE_FORMAT);
                    dateField.setText(frmt.format(new Date()));
                }
            });
            p2.add(cb_dateEnable);
            dateField.setEnabled(false);
            p2.add(dateField);
            btnNow.setEnabled(false);
            p2.add(btnNow);
            content.add(p2);
        }
        
        // 退院日登録するか
        if (saveParams.isInHospital()) {
            cb_registEndDate = new JCheckBox("退院日登録する");
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            p3.add(cb_registEndDate);
            content.add(p3);
        }
//masuda$
        
        // AccessRightを設定するボタンとパネルを生成する
        patientCheck = new JCheckBox("患者に参照を許可する");
        clinicCheck = new JCheckBox("診療歴のある病院に参照を許可する");
        
        //---------------------------
        // CLAIM 送信ありなし
        //---------------------------
        sendClaim = new JCheckBox("診療行為を送信する（仮保存の場合は送信しない）");
        JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p5.add(sendClaim);
        content.add(p5);

        //---------------------------
        // 検体検査オーダー送信ありなし
        //---------------------------
        sendLabtest = new JCheckBox("検体検査オーダー（仮保存の場合はしない）");
        if (Project.getBoolean(Project.SEND_LABTEST)) {
            JPanel p6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            p6.add(sendLabtest);
            content.add(p6);
        }

        // OK button
        okButton = new JButton(SAVE);
        okButton.setToolTipText("診療行為の送信はチェックボックスに従います。");
        okButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // 戻り値のSaveparamsを生成する
                value = viewToModel(false);
                if (value != null) {
                    close();
                }
            }
        });
        okButton.setEnabled(false);
        
        // Cancel Button
        String buttonText =  (String)UIManager.get("OptionPane.cancelButtonText");
        cancelButton = new JButton(buttonText);
        cancelButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                value = null;
                close();
            }
        });
        
        // 仮保存 button
        tmpButton = new JButton(TMP_SAVE);
        tmpButton.setToolTipText("診療行為は送信しません。");
        tmpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // 戻り値のSaveparamsを生成する
                value = viewToModel(true);
                if (value != null) {
                    close();
                }
            }
        });
        tmpButton.setEnabled(false);
        
        return content;
    }
    
    private void close() {
        dialog.setVisible(false);
        dialog.dispose();
    }
    
    private void controlButton() {
        
        // 未来日カルテは仮保存のみ可能
        SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_8601_DATE_FORMAT);
        Date now = new Date();
        
        if (dateField == null) {
            if (saveParams.getConfirmed().after(now)) {
                okButton.setEnabled(false);
                tmpButton.setEnabled(true);
                setFocus(tmpButton);
            } else {
                okButton.setEnabled(true);
                tmpButton.setEnabled(true);
                setFocus(okButton);
            }
            return;
        }
        
        try {
            Date d = frmt.parse(dateField.getText().trim());
            
            if (d.after(now)) {
                okButton.setEnabled(false);
                tmpButton.setEnabled(true);
                //setFocus(tmpButton);  // これは不便と指摘
            } else {
                okButton.setEnabled(true);
                tmpButton.setEnabled(true);
                //setFocus(okButton);  // これは不便と指摘
            }
        } catch (Exception ex) {
            okButton.setEnabled(false);
            tmpButton.setEnabled(false);
        }
    }
    
    private void setFocus(final JComponent c) {
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                c.requestFocusInWindow();
            }
        });
    }
    
    /**
     * タイトルフィールドの有効性をチェックする。
     */
    private void checkTitle() {    
        boolean enabled = !titleField.getText().trim().isEmpty();
        okButton.setEnabled(enabled);
        tmpButton.setEnabled(enabled);
    }

    private SaveParams viewToModel(boolean temp) {
        
        // 戻り値のSaveparamsを生成する
        SaveParams model = new SaveParams();
        
        // 仮保存であることを設定する
        model.setTmpSave(temp);
        
        // 文書タイトルを取得する
        String val = (String) titleCombo.getSelectedItem();
        if (!val.isEmpty()) {
            model.setTitle(val);
        } else {
            if (!temp) {
                model.setTitle("経過記録");
            }
        }
        
        // Department
        val = departmentLabel.getText();
        model.setDepartment(val);
        
        // 印刷部数を取得する
        int count = Integer.parseInt((String)printCombo.getSelectedItem());
        model.setPrintCount(count);
        
        //-------------------
        // CLAIM 送信
        //-------------------
//masuda^   仮保存でもClaim送信可能にする
        //model.setSendClaim(!temp && sendClaim.isSelected());
        model.setSendClaim(sendClaim.isSelected());
//masuda$
        
        // 患者への参照許可を取得する
        boolean b = !temp && patientCheck.isSelected();
        model.setAllowPatientRef(b);
        
        // 診療歴のある施設への参照許可を設定する
        b = !temp && clinicCheck.isSelected();
        model.setAllowClinicRef(b);

        //-------------------
        // LabTest 送信
        //-------------------
        b = !temp && sendLabtest.isSelected();
        model.setSendLabtest(b);
        
        // 保存日を保存
        model.setConfirmed(saveParams.getConfirmed());
        if (cb_dateEnable != null && cb_dateEnable.isSelected()) {
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_8601_DATE_FORMAT);
            try {
                Date karteDate = frmt.parse(dateField.getText().trim());
                model.setConfirmed(karteDate);
            } catch (ParseException ex) {
                return null;
            }
        }
        // 退院日登録フラッグを設定
        if (cb_registEndDate != null && cb_registEndDate.isSelected()) {
            model.setRegistEndDate(cb_registEndDate.isSelected());
        }
        
        return model;
    }

    private class PopupListener extends PopupCalendarListener {

        private PopupListener(JTextField tf, int[] range) {
            super(tf, range);
        }

        @Override
        public void setValue(SimpleDate sd) {
            GregorianCalendar gc = new GregorianCalendar();
            gc.clear();
            gc.set(GregorianCalendar.YEAR, sd.getYear());
            gc.set(GregorianCalendar.MONTH, sd.getMonth());
            gc.set(GregorianCalendar.DATE, sd.getDay());
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_8601_DATE_FORMAT);
            tf.setText(frmt.format(gc.getTime()));
        }
    }
}