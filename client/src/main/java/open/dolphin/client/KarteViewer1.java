package open.dolphin.client;

import java.awt.Color;
import java.awt.event.MouseListener;
import javax.swing.ActionMap;
import open.dolphin.infomodel.IInfoModel;

/**
 * KarteViewer1
 *
 * @author masuda, Masuda Naika
 */
public class KarteViewer1 extends KarteViewer {

    // SOA Pane
    private KartePane soaPane;

    private void initialize() {

        KartePanel kartePanel = KartePanel.createKartePanel(KartePanel.MODE.SINGLE_VIEWER, false);

        // SOA Pane を生成する
        soaPane = new KartePane();
        soaPane.setRole(IInfoModel.ROLE_SOA);
        soaPane.setTextPane(kartePanel.getSoaTextPane());

        // Schema 画像にファイル名を付けるのために必要
        String docId = getModel().getDocInfoModel().getDocId();
        soaPane.setDocId(docId);

        kartePanel.setBorder(NOT_SELECTED_BORDER);

        setKartePanel(kartePanel);
        setUI(kartePanel);
        
        // DocumentModelのstatusをKartePaneに保存しておく
        // KarteViewerのpopup制御に利用
        String status = getModel().getDocInfoModel().getStatus();
        soaPane.setDocStatus(status);
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
        //new KarteRenderer_2(soaPane, null).render(getModel());
        KarteRenderer_2.getInstance().render(getModel(), soaPane, null);
//masuda$
        
        // モデル表示後にリスナ等を設定する
        ChartMediator mediator = getContext().getChartMediator();
        soaPane.init(false, mediator);

    }

    @Override
    public void stop() {
        soaPane.clear();
        soaPane = null;
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
        return null;
    }

    @Override
    public void addMouseListener(MouseListener ml) {
        soaPane.getTextPane().addMouseListener(ml);
    }

    @Override
    public void setBackground(Color c) {
        soaPane.getTextPane().setBackground(c);
    }

    @Override
    public void setParentActionMap(ActionMap amap) {
        soaPane.getTextPane().getActionMap().setParent(amap);
    }
}
