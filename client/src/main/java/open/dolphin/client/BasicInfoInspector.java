package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.infomodel.SimpleAddressModel;

/**
 * BasicInfoInspector
 * @author Kazushi Minagawa.
 * @author modified by masuda, Masuda Naika
 */
public class BasicInfoInspector {

    private JPanel basePanel; // このクラスのパネル
    private JLabel nameLabel;
    private JLabel addressLabel;

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

        addressLabel = new JLabel("　");
        addressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        addressLabel.setForeground(foreground);
        addressLabel.setOpaque(false);

        basePanel = new JPanel(new BorderLayout(0, 2));
        basePanel.add(nameLabel, BorderLayout.NORTH);
        basePanel.add(addressLabel, BorderLayout.SOUTH);
        basePanel.setOpaque(true);

        fixHeight(basePanel, PANEL_HEIGHT);
        basePanel.putClientProperty("fixedHeight", true);
    }

    private void fixHeight(JPanel panel, int height) {
        panel.setPreferredSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setMinimumSize(new Dimension(0, height));
    }
}
