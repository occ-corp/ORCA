
package open.dolphin.client;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;

/**
 * public class ChartImplExtensions {

 * @author masuda, Masuda Naika
 */
public class ChartImplExtensions extends AbstractChartExtensions {
    
    private static final ImageIcon ICON_ECG = ClientContext.getImageIcon("ecg_24.gif");
    private static final ImageIcon ICON_MED = ClientContext.getImageIcon("med_24.gif");
    private static final ImageIcon ICON_RSB = ClientContext.getImageIcon("rsb_24.gif");
    
    public ChartImplExtensions(Chart context) {
        this.context = context;
    }
    
    @Override
    public JToolBar createToolBar() {
        JToolBar myToolBar = new JToolBar();

        // ChartImplのボタンを追加
        addChartBtn(myToolBar);

        // 共通ボタンを追加
        addCommonBtn(myToolBar);
        enableExtBtn(false);

        return myToolBar;
    }

    @Override
    protected ChartImpl getContext() {
        return (ChartImpl) context;
    }

    
    // ChartImplのボタンを追加。FEV-40と薬剤相互作用
    private void addChartBtn(JToolBar myToolBar) {
        // FEV-40
        if (Project.getBoolean(MiscSettingPanel.USE_FEV, false)) {
            JButton ecgBtn = new JButton();
            ecgBtn.setIcon(ICON_ECG);
            ecgBtn.setToolTipText("心電図を開きます。");
            myToolBar.add(ecgBtn);
            ecgBtn.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    showECG();
                }
            });
        }

        // 薬剤相互作用検索関連
        JButton interactionBtn = new JButton();
        interactionBtn.setIcon(ICON_MED);
        interactionBtn.setToolTipText("薬剤併用情報を調べます。");
        myToolBar.add(interactionBtn);
        interactionBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                checkInteraction();
            }
        });

        // RSBボタンを追加
/*
        boolean useRsb = Project.getBoolean(MiscSettingPanel.USE_RSB, false);
        if (ClientContext.isWin() && useRsb) {
            addRsbBtn(myToolBar);
        }
*/
    }

    // RSBボタンを追加
    private void addRsbBtn(JToolBar myToolBar) {

        JButton rsbBtn = new JButton();
        rsbBtn.setIcon(ICON_RSB);
        rsbBtn.setToolTipText("RS_Baseと画面連携します。");
        myToolBar.add(rsbBtn);
        rsbBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                RsbLink rsb = new RsbLink();
                rsb.doAutoLink(context.getPatient().getPatientId());
            }
        });
        rsbBtn.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                maybePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybePopup(e);
            }

            private void maybePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem mi1 = new JMenuItem("患者画面表示");
                    popup.add(mi1);
                    JMenuItem mi2 = new JMenuItem("血液データ表示");
                    popup.add(mi2);
                    JMenuItem mi3 = new JMenuItem("RS_Base Top");
                    popup.add(mi3);
                    mi1.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            RsbLink rsb = new RsbLink();
                            rsb.rsbOpenKanjaGamenLink(context.getPatient().getPatientId());
                        }
                    });
                    mi2.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            RsbLink rsb = new RsbLink();
                            rsb.rsbOpenLaboLink(context.getPatient().getPatientId());
                        }
                    });
                    mi3.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            RsbLink rsb = new RsbLink();
                            rsb.showRsbTop();
                        }
                    });

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    // FEF-40を起動する
    private void showECG() {
        new ShowEcgViewer().enter(context);
    }

    // 薬剤相互作用検索
    private void checkInteraction() {

        CheckInteractionPanel panel = new CheckInteractionPanel();
        panel.enter(context);
        panel = null;
    }
}
