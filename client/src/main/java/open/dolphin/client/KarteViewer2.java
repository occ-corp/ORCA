package open.dolphin.client;

import java.awt.Color;
import java.awt.event.MouseListener;
import javax.swing.ActionMap;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;

/**
 * KarteViwer2
 *
 * @author masuda, Masuda Naika
 */
public class KarteViewer2 extends KarteViewer {

    // SOA Pane
    private KartePane soaPane;
    // P Pane
    private KartePane pPane;

    private void initialize() {

        Chart parent = getContext();
        boolean verticalLayout = false;
        if (parent != null && parent instanceof ChartImpl) {
            boolean vsc = Project.getBoolean(Project.KARTE_SCROLL_DIRECTION);
            boolean vl = Project.getBoolean(MiscSettingPanel.USE_VERTICAL_LAYOUT);
            verticalLayout = !vsc && vl;
        }

        KartePanel kartePanel = KartePanel.createKartePanel(KartePanel.MODE.DOUBLE_VIEWER, verticalLayout);

        // SOA Pane を生成する
        soaPane = new KartePane();
        soaPane.setRole(IInfoModel.ROLE_SOA);
        soaPane.setTextPane(kartePanel.getSoaTextPane());

        // P Pane を生成する
        pPane = new KartePane();
        pPane.setRole(IInfoModel.ROLE_P);
        pPane.setTextPane(kartePanel.getPTextPane());

        // Schema 画像にファイル名を付けるのために必要
        // Schema 画像にファイル名を付けるのために必要
        String docId = getModel().getDocInfoModel().getDocId();
        soaPane.setDocId(docId);

        kartePanel.setBorder(NOT_SELECTED_BORDER);

        setKartePanel(kartePanel);
        setUI(kartePanel);
    }

    /**
     * プログラムを開始する。
     */
    @Override
    public void start() {

        // Creates GUI
        initialize();

        if (getModel() == null) {
            return;
        }

        // タイトルを設定する
        setTitle();

        // レンダリングする
//masuda^
        //new KarteRenderer_2(soaPane, pPane).render(getModel());
        KarteRenderer_2.getInstance().render(getModel(), soaPane, pPane);
//masuda$
        
        // モデル表示後にリスナ等を設定する
        ChartMediator mediator = getContext().getChartMediator();
        soaPane.init(false, mediator);
        pPane.init(false, mediator);

    }

    @Override
    public void stop() {
        soaPane.clear();
        soaPane = null;
        pPane.clear();
        pPane = null;
    }

    /**
     * SOA Pane を返す。
     * @return soaPane
     */
    @Override
    public KartePane getSOAPane() {
        return soaPane;
    }

    /**
     * P Pane を返す。
     * @return pPane
     */
    @Override
    public KartePane getPPane() {
        return pPane;
    }

    @Override
    public void addMouseListener(MouseListener ml) {
        soaPane.getTextPane().addMouseListener(ml);
        pPane.getTextPane().addMouseListener(ml);
    }

    @Override
    public void setBackground(Color c) {
        soaPane.getTextPane().setBackground(c);
        pPane.getTextPane().setBackground(c);
    }

    @Override
    public void setParentActionMap(ActionMap amap) {
        soaPane.getTextPane().getActionMap().setParent(amap);
        pPane.getTextPane().getActionMap().setParent(amap);
    }
}
