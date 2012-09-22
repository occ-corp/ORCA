package open.dolphin.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import javax.swing.*;
import open.dolphin.infomodel.*;

/**
 * BasicInfoInspector
 * @author Kazushi Minagawa.
 * @author modified by masuda, Masuda Naika
 */
public class BasicInfoInspector {

    private JPanel basePanel; // このクラスのパネル
    private JLabel nameLabel;
    private JLabel addressLabel;
    
    private JToggleButton summaryBtn;

    private static final ImageIcon rightIcon = ClientContext.getImageIcon("arrow-right.gif");
    private static final ImageIcon leftIcon = ClientContext.getImageIcon("arrow-left.gif");
    
    private static final Color foreground = ClientContext.getColor("patientInspector.basicInspector.foreground");
    private static final Color maleColor = new Color(230, 243, 243);    // やわらかい色に
    private static final Color femaleColor = new Color(254, 221, 242);
    private static final Color unknownColor = Color.LIGHT_GRAY;
    private static final int PANEL_HEIGHT = 40;
    
    // Context このインスペクタの親コンテキスト
    private ChartImpl context;


    /**
     * BasicInfoInspectorオブジェクトを生成する。
     */
    public BasicInfoInspector(ChartImpl context) {
        this.context = context;
        initComponent();
        update();
    }

    /**
     * レイウアトのためにこのインスペクタのコンテナパネルを返す。
     * @return コンテナパネル
     */
    public JPanel getPanel() {
        return basePanel;
    }

    /**
     * 患者の基本情報を表示する。
     */
    private void update() {

        StringBuilder sb = new StringBuilder();
        sb.append(context.getPatient().getFullName());
        sb.append("  ");
        // 和暦を含める
        String age = ModelUtils.getAgeBirthday2(context.getPatient().getBirthday());
        sb.append(age);
        nameLabel.setText(sb.toString());

        SimpleAddressModel address = context.getPatient().getSimpleAddressModel();
        if (address != null) {
            String adr = address.getAddress();
            if (adr != null) {
                adr = adr.replaceAll("　", " ");
            }
            addressLabel.setText(adr);
        } else {
            addressLabel.setText("");
        }

        String gender = context.getPatient().getGenderDesc();

        Color color;
        if (gender.equals(IInfoModel.MALE_DISP)) {
            color = maleColor;
        } else if (gender.equals(IInfoModel.FEMALE_DISP)) {
            color = femaleColor;
        } else {
            color = unknownColor;
        }

        basePanel.setBackground(color);
    }

    /**
     * GUI コンポーネントを初期化する。
     */
    private void initComponent() {

        nameLabel = new JLabel("　");
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setForeground(foreground);
        nameLabel.setOpaque(false);
        
        summaryBtn = new JToggleButton(rightIcon);
        summaryBtn.setPreferredSize(new Dimension(15, 15));
        summaryBtn.setBorderPainted(false);
        summaryBtn.setContentAreaFilled(false);
        summaryBtn.setFocusPainted(false);
        summaryBtn.setVisible(false);
        
        JPanel north = new JPanel();
        north.setLayout(new BorderLayout());
        north.add(summaryBtn, BorderLayout.WEST);
        north.add(nameLabel, BorderLayout.CENTER);
        north.setForeground(foreground);
        north.setOpaque(false);
        addressLabel = new JLabel("　");
        addressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        addressLabel.setForeground(foreground);
        addressLabel.setOpaque(false);

        basePanel = new JPanel(new BorderLayout(0, 2));
        basePanel.add(north, BorderLayout.NORTH);
        basePanel.add(addressLabel, BorderLayout.SOUTH);
        basePanel.setOpaque(true);

        fixHeight(basePanel, PANEL_HEIGHT);
        basePanel.putClientProperty("fixedHeight", true);
        
//masuda^ サマリー表示
        DocumentModel summary = context.getKarte().getSummary();
        if (summary != null) {
            JTextArea ta = createTextArea(summary);
            summaryBtn.addActionListener(new PopupAction(ta));
            summaryBtn.setVisible(true);
        }
//masuda$
    }

    private void fixHeight(JPanel panel, int height) {
        panel.setPreferredSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setMinimumSize(new Dimension(0, height));
    }
    
    private JTextArea createTextArea(DocumentModel summary) {

        final SimpleDateFormat sdf = new SimpleDateFormat(IInfoModel.KARTE_DATE_FORMAT);
        
        ModuleModel mm = summary.getModule(IInfoModel.MODULE_PROGRESS_COURSE);
        ProgressCourse pc = (ProgressCourse) ModelUtils.xmlDecode(mm.getBeanBytes());
        
        boolean first = true;
        String xml = pc.getFreeText();
        StringBuilder sb = new StringBuilder();
        sb.append(sdf.format(mm.getFirstConfirmed()));
        sb.append("\n");
        
        String head[] = xml.split("<text>");
        for (String str : head) {
            String tail[] = str.split("</text>");
            if (tail.length == 2) {
                if (first) {
                    first = false;
                } else {
                    sb.append("\n");
                }
                sb.append(tail[0].trim());
            }
        }
        String text = sb.toString();

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setBackground(new Color(255, 255, 200));
        ta.setBorder(BorderFactory.createEtchedBorder());
        ta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        ta.setText(text);

        return ta;
    }
    
    
    private class PopupAction extends AbstractAction {

        private Popup popup;
        private PopupFactory factory;
        private JTextArea ta;
        
        private PopupAction(JTextArea ta) {
            factory = PopupFactory.getSharedInstance();
            this.ta = ta;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {

            Point p = basePanel.getLocationOnScreen();
            if (p == null) {
                p = new Point(0, 0);
            }
            p.x += 15;
            
            if (summaryBtn.isSelected()) {
                if (popup == null) {
                    popup = factory.getPopup(basePanel, ta, p.x, p.y);
                }
                popup.show();
                summaryBtn.setIcon(leftIcon);
            } else {
                if (popup != null) {
                    popup.hide();
                    popup = null;
                }
                summaryBtn.setIcon(rightIcon);
            }
        }
    }
}
