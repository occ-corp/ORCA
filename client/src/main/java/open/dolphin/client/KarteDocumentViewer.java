package open.dolphin.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.*;
import open.dolphin.delegater.DocumentDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.letter.KartePDFMaker;
import open.dolphin.project.Project;
import open.dolphin.util.MultiTaskExecutor;
import org.apache.log4j.Logger;

/**
 * DocumentViewer
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika, 11/06/04 -> 12/07/13
 */
public class KarteDocumentViewer extends AbstractChartDocument implements DocumentViewer {

    // Busy プロパティ名
    public static final String BUSY_PROP = "busyProp";
    // 更新を表す文字
    private static final String TITLE_UPDATE = "更新";
    private static final String TITLE = "参 照";
    // busy プリパティ
    private boolean busy;
    // 文書履を昇順で表示する場合に true
    private boolean ascending;
    // 文書の修正履歴を表示する場合に true
    private boolean showModified;
    // このクラスの状態マネージャ
    private StateMgr stateMgr;
    // 縦並びで表示する場合に true
    private boolean vsc;
    // 別ウィンドウで編集するか
    private boolean newWindow;

    // 選択されている karteViewer
    private KarteViewer selectedKarte;
    // 表示するscrollar pane
    private JScrollPane scroller;

    // DocumentModel.idとKarteViewerの対応マップ
    // docInfo.getDocId() (=String)ではなくgetDocPk() (=long)であることに注意
    private Map<Long, KarteViewer> karteViewerMap;
    // 今選択されているDocInfoModelの配列
    private DocInfoModel[] docInfoArray;
    // ScrollerPaneに入っている、KarteViewerを含んだPanal
    private KarteScrollerPanel scrollerPanel;
    // scrollerPanelに表示するKarteViewerのリスト
    private List<KarteViewer> viewerList;
    
    private Logger logger;

//pns^ 表示されているカルテの中身を検索する modified by masuda
    private FindAndView findAndView = new FindAndView();

    public void findFirst() {
        FindDialog dialog = new FindDialog(getContext());
        dialog.start();
        String searchText = dialog.getSearchText();
        if (!searchText.equals("")) {
            findAndView.showFirst(searchText, dialog.isSoaBoxOn(), dialog.isPBoxOn(), scrollerPanel);
        }
    }

    public void findNext() {
        findAndView.showNext(scrollerPanel);
    }

    public void findPrevious() {
        findAndView.showPrevious(scrollerPanel);
    }

    public void selectAll() {
        //this.getContext().getDocumentHistory().selectAll();
    }

    /**
     * 表示されているカルテを CLAIM 送信する
     * 元町皮ふ科
     */
    public void sendClaim() {

        // claim を送るのはカルテだけ
        String docType = getBaseKarte().getModel().getDocInfoModel().getDocType();
        if (!IInfoModel.DOCTYPE_KARTE.equals(docType)) {
            return;
        }

        DocumentModel model = getContext().getKarteModelToEdit(getBaseKarte().getModel());
        model.setKarteBean(getContext().getKarte());
        model.getDocInfoModel().setConfirmDate(new Date());

//        ClaimSender claimSender = new ClaimSender(getContext().getCLAIMListener());
//
//        // DG  DocInfoに設定されているGUIDに一致する保険情報モジュールを設定する
//        PVTHealthInsuranceModel applyIns = getContext().getHealthInsuranceToApply(model.getDocInfoModel().getHealthInsuranceGUID());
//        claimSender.setInsuranceToApply(applyIns);
//        claimSender.send(model);

        model.getDocInfoModel().setSendClaim(true);
/*
        ClaimSender claimSender = new ClaimSender();
        claimSender.setContext(getContext());
        claimSender.prepare(model);
        claimSender.send(model);
*/
        KarteContentSender sender = new KarteContentSender();
        sender.sendKarte(getContext(), model);
    }
//pns$

    /**
     * DocumentViewerオブジェクトを生成する。
     */
    public KarteDocumentViewer() {
        super();
        setTitle(TITLE);
        newWindow = Project.getBoolean(Project.KARTE_PLACE_MODE, true);
        viewerList = new ArrayList<KarteViewer>();

        vsc = Project.getBoolean(Project.KARTE_SCROLL_DIRECTION, true);
        scrollerPanel = new KarteScrollerPanel(this);
        logger = ClientContext.getBootLogger();
    }

    // KarteViewerのリストを返す。ScrollerPanelから参照
    public List<KarteViewer> getViewerList() {
        return viewerList;
    }
    
    public JScrollPane getScrollPane() {
        return scroller;
    }
    public boolean isVsc() {
        return vsc;
    }


    /**
     * busy かどうかを返す。
     * @return busy の時 true
     */
    public boolean isBusy() {
        return busy;
    }

    @Override
    public void start() {

        karteViewerMap = new HashMap<Long, KarteViewer>();
        connect();
        stateMgr = new StateMgr();
        enter();
    }

    @Override
    public void stop() {

        for (Iterator itr = karteViewerMap.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            KarteViewer viewer = (KarteViewer) entry.getValue();
            viewer.stop();
        }
        
        karteViewerMap.clear();
        karteViewerMap = null;
        viewerList.clear();
        viewerList = null;

        // ScrollerPanelの後片付け
        scrollerPanel.dispose();
        scrollerPanel = null;
    }

    @Override
    public void enter() {
        super.enter();
        stateMgr.enter();
    }

    /**
     * マウスクリック(選択)されたKarteViwerをselectedKarteに設定する。
     * 他のカルテが選択されている場合はそれを解除する。
     * StateMgrを Haskarte State にする。
     * @param view 選択されたKarteViwer
     */
    public void setSelectedKarte(KarteViewer viewer) {

        KarteViewer old = selectedKarte;
        selectedKarte = viewer;

        // 他のカルテが選択されている場合はそれを解除する
        if (old != null) {
            old.setSelected(false);
        }
        if (selectedKarte != null) {
            selectedKarte.setSelected(true);
            stateMgr.processCleanEvent();
        } else {
            stateMgr.processEmptyEvent();
        }
    }

    /**
     * 新規カルテ作成の元になるカルテを返す。
     * @return 作成の元になるカルテ
     */
    public KarteViewer getBaseKarte() {
        return selectedKarte;
    }

    /**
     * 文書履歴の抽出期間が変更された場合、
     * karteList をclear、選択されているkarteViewerを解除、sateMgrをNoKarte状態に設定する。
     */
    @Override
    public void historyPeriodChanged() {
        setSelectedKarte(null);
        getContext().showDocument(0);
    }

    /**
     * GUIコンポーネントにリスナを設定する。
     *
     */
    private void connect() {

        // 文書履歴に昇順／降順、修正履歴表示の設定をする
        // この値の初期値はデフォル値であり、個々のドキュメント（画面）単位にメニューで変更できる。（適用されるのは個々のドキュメントのみ）
        // デフォルト値の設定は環境設定で行う。
        ascending = getContext().getDocumentHistory().isAscending();
        showModified = getContext().getDocumentHistory().isShowModified();

        // windowサイズが変更されるとKarteViewerの高さを調節する
        getContext().getFrame().addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                resizeKarteViewers();
            }
        });
    }

    private void resizeKarteViewers() {

        // 親コンテナが決まっていなかったらリターン
        if (scrollerPanel == null || scrollerPanel.getParent() == null) {
            return;
        }

        // 現在地を保存
        final Point p = scroller.getViewport().getViewPosition();

        // scrollerPanelのサイズをWindowに合わせる
        Dimension d = scrollerPanel.getParent().getSize();
        scrollerPanel.setSize(d);   // setPreferredSize()にあらず

        // ViewPositionが原点になるので、現在地を回復
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                scroller.getViewport().setViewPosition(p);
            }
        });
    }

    /**
     * KarteViewerを生成し表示する。
     *
     * @param selectedHistories 選択された文書情報 DocInfo 配列
     */
    @Override
    public void showDocuments(final DocInfoModel[] selectedHistories, final JScrollPane scroller) {

        this.scroller = scroller;
        this.docInfoArray = selectedHistories;

        if (selectedHistories == null || selectedHistories.length == 0) {
//masuda^   文書がないならパネルをクリアする。フィルタリングで該当なしの場合にわかりにくいので
            scrollerPanel.removeAll();
            scroller.setViewportView(scrollerPanel);
//masuda$
            return;
        }

        List<Long> added = new ArrayList<Long>();

        // 選択リストにあって 現在の karteViewerMap にないものはデータベースから取得する
        for (DocInfoModel docInfo : selectedHistories) {
            long docPk = docInfo.getDocPk();
            if (!karteViewerMap.containsKey(docPk)) {
                added.add(docPk);
            }
        }

        if (added.isEmpty()) {
            // 追加されたものがない場合は、表示に飛ぶ
            showKarteViewers();
        } else {
            // 追加されたものがある場合はデータベースから取得する
            KarteTask task = new KarteTask(getContext(), added);
            task.execute();
        }
    }

    private KarteViewer createKarteViewer(DocInfoModel docInfo) {

        if (docInfo != null && docInfo.getDocType().equals(IInfoModel.DOCTYPE_S_KARTE)) {
            return KarteViewer.createKarteViewer(KarteViewer.MODE.SINGLE);
        }
        return KarteViewer.createKarteViewer(KarteViewer.MODE.DOUBLE);
    }

    @SuppressWarnings("unchecked")
    private void showKarteViewers() {

        //final LapTimer timer = new LapTimer();
        //timer.start();
        
        SwingWorker worker = new SwingWorker<Void, KarteViewer>() {

            @Override
            protected Void doInBackground() throws Exception {
                // まずは古いのを除去
                scrollerPanel.removeAll();
                scroller.setViewportView(scrollerPanel);

                // 縦並びかどうかに応じてレイアウトを設定
                if (vsc) {
                    scrollerPanel.setLayout(new BoxLayout(scrollerPanel, BoxLayout.Y_AXIS));
                } else {
                    scrollerPanel.setLayout(new BoxLayout(scrollerPanel, BoxLayout.X_AXIS));
                }

                // scrollerPanelに表示するKarteViewerのリスト
                viewerList.clear();

                // 選択されているDocInfoに対応するKarteViewerをviewerListに追加する
                for (DocInfoModel docInfo : docInfoArray) {
                    KarteViewer viewer = karteViewerMap.get(docInfo.getDocPk());
                    if (viewer != null) {
                        viewerList.add(viewer);
                    } else {
                        logger.debug("KarteDocumentViewer: KarteViewer is null :-(");
                    }
                    
                }
                // 時間軸でソート
                if (ascending) {
                    Collections.sort(viewerList, new KarteViewerComparator());
                } else {
                    Collections.sort(viewerList, Collections.reverseOrder(new KarteViewerComparator()));
                }

                // scrollerPanelに追加する
                for (int i = 0; i < viewerList.size(); ++i) {
                    KarteViewer viewer = viewerList.get(i);
                    // skip scrollのためにindexを振る
                    viewer.setIndex(i);
                    JPanel ui = viewer.getUI();
                    scrollerPanel.add(ui);
                }
                return null;
            }

            @Override
            protected void done() {
                // 文書履歴タブに変更
                getContext().showDocument(0);
                // 一つ目を選択し、フォーカスを設定する。フォーカスがないとキー移動できない
                if (!viewerList.isEmpty()) {
                    KarteViewer kv = ascending
                            ? viewerList.get(viewerList.size() - 1) 
                            : viewerList.get(0);
                    setSelectedKarte(kv);
                    kv.getUI().requestFocusInWindow();
                }
                // 先頭にスクロール
                scrollerPanel.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
                
                //timer.stop("Show KarteViewers");
            }
        };
        
        worker.execute();
    }

    /**
     * カルテを修正する。
     */
    public void modifyKarte() {

        // ReadOnly
        if (getContext().isReadOnly()) {
            return;
        }
        if (selectedKarte == null) {
            return;
        }
//masuda^
        // カルテ修正はひとつだけ
        ChartImpl chart = (ChartImpl) getContext();
        // すでに修正中の document があれば toFront するだけで帰る
        if (!canModifyKarte()) {
            return;
        }
//masuda$
        
        String docType = selectedKarte.getModel().getDocInfoModel().getDocType();
        //String dept = getContext().getPatientVisit().getDepartment();
        //String deptCode = getContext().getPatientVisit().getDepartmentCode();
        String deptName = getContext().getPatientVisit().getDeptName();
        String deptCode = getContext().getPatientVisit().getDeptCode();

        NewKarteParams params = new NewKarteParams(Chart.NewKarteOption.BROWSER_MODIFY);
        params.setDocType(docType);
        params.setDepartmentName(deptName);
        params.setDepartmentCode(deptCode);
        // このフラグはカルテを別ウインドウで編集するかどうか
        params.setOpenFrame(newWindow);

        DocumentModel editModel = chart.getKarteModelToEdit(selectedKarte.getModel());
        KarteEditor editor = chart.createEditor();
        editor.setModel(editModel);
        editor.setEditable(true);
        editor.setModify(true);
        int mode = docType.equals(IInfoModel.DOCTYPE_KARTE) ? KarteEditor.DOUBLE_MODE : KarteEditor.SINGLE_MODE;
        editor.setMode(mode);

        // Single Karte の場合 EF させない
        if (mode == 1) {
            params.setOpenFrame(false);
        }

        if (params.isOpenFrame()) {
            EditorFrame editorFrame = new EditorFrame();
            editorFrame.setChart(getContext());
            editorFrame.setKarteEditor(editor);
            editorFrame.start();
        } else {
            editor.setContext(chart);
            editor.initialize();
            editor.start();
            chart.addChartDocument(editor, TITLE_UPDATE);
        }
    }

//masuda^
    @Override
    public void print() {

        JCheckBox cb = new JCheckBox("PDFは昇順に印刷");
        //cb.setSelected(ascending);
        cb.setSelected(true);
        Object[] msg = new Object[2];
        msg[0] = "PDFファイルを作成しますか？";
        msg[1] = cb;
        int option = JOptionPane.showOptionDialog(
                getContext().getFrame(),
                msg,
                ClientContext.getFrameTitle("カルテ印刷"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"PDF作成", "イメージ印刷", "取消し"},
                "PDF作成");

        if (option == 0) {
            makePDF(cb.isSelected());
        } else if (option == 1) {
            printKarte();
        }
    }

    // カルテのPDFを作成する
    private void makePDF(boolean asc) {

        List<DocumentModel> docList = new ArrayList<DocumentModel>();
        for (KarteViewer viewer : viewerList) {
            docList.add(viewer.getModel());
        }

        KartePDFMaker maker = new KartePDFMaker();
        maker.setContext(getContext());
        maker.setDocumentList(docList);
        maker.setAscending(asc);
        maker.create();
    }

    // インスペクタに表示されているカルテをまとめて印刷する。
    private void printKarte() {

        // ブザイクなんだけど、あまり使わない機能なのでこれでヨシとする
        // 背景色が緑だとインクがもったいないので白にする。選択も解除しておく。
        for (DocInfoModel docInfo : docInfoArray) {
            KarteViewer viwer = karteViewerMap.get(docInfo.getDocPk());
            viwer.setBackground(Color.WHITE);
        }
        selectedKarte.setSelected(false);

        // 患者名を取得
        String name = getContext().getPatient().getFullName();
        String id = getContext().getPatient().getPatientId();
        // scrollerPanelを印刷する
        PrintKarteDocumentView.printComponent(scrollerPanel, name, id);

        // 背景色を戻しておく
        for (DocInfoModel docInfo : docInfoArray) {
            KarteViewer viwer = karteViewerMap.get(docInfo.getDocPk());
            viwer.setBackground(viwer.getSOAPane().getUneditableColor());
        }
        setSelectedKarte(null);
    }
//masuda$

    /**
     * 昇順表示にする。
     */
    public void ascending() {
        ascending = true;
        getContext().getDocumentHistory().setAscending(ascending);
    }

    /**
     * 降順表示にする。
     */
    public void descending() {
        ascending = false;
        getContext().getDocumentHistory().setAscending(ascending);
    }

    /**
     * 修正履歴の表示モードにする。
     */
    public void showModified() {
        showModified = !showModified;
        getContext().getDocumentHistory().setShowModified(showModified);
    }

    /**
     * karteList 内でダブルクリックされたカルテ（文書）を EditorFrame で開く。
     */
    public void openKarte() {
        // ReadOnly
        if (getContext().isReadOnly()) {
            return;
        }
        if (selectedKarte == null) {
            return;
        }

        EditorFrame editorFrame = new EditorFrame();
        editorFrame.setChart(getContext());

        // 表示している文書タイプに応じて Viewer を作成する
        DocumentModel model = selectedKarte.getModel();
        String docType = model.getDocInfoModel().getDocType();

        if (docType.equals(IInfoModel.DOCTYPE_S_KARTE)) {
            KarteViewer viwer = KarteViewer.createKarteViewer(KarteViewer.MODE.SINGLE);
            viwer.setModel(model);
            editorFrame.setKarteViewer(viwer);
            editorFrame.start();
        } else if (docType.equals(IInfoModel.DOCTYPE_KARTE)) {
//masuda^   カルテ表示はひとつだけ
            // すでに修正中の document があれば toFront するだけで帰る
            if (!canModifyKarte()) {
                return;
            }
//masuda$
            KarteViewer viwer = KarteViewer.createKarteViewer(KarteViewer.MODE.DOUBLE);
            viwer.setModel(model);
            editorFrame.setKarteViewer(viwer);
            editorFrame.start();
        }
    }

    /**
     * 表示選択されているカルテを論理削除する。
     * 患者を間違えた場合等に履歴に表示されないようにするため。
     */
    public void delete() {

        if (selectedKarte == null) {
            return;
        }

        // Dialog を表示し理由を求める
        String message = "このドキュメントを削除しますか ?   ";
        final JCheckBox box1 = new JCheckBox("作成ミス");
        final JCheckBox box2 = new JCheckBox("診察キャンセル");
        final JCheckBox box3 = new JCheckBox("その他");
        box1.setSelected(true);

        ActionListener al = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (box1.isSelected() || box2.isSelected()) {
                    //return;
                } else if (!box3.isSelected()) {
                    box3.setSelected(true);
                }
            }
        };

        box1.addActionListener(al);
        box2.addActionListener(al);
        box3.addActionListener(al);

        Object[] msg = new Object[5];
        msg[0] = message;
        msg[1] = box1;
        msg[2] = box2;
        msg[3] = box3;
        msg[4] = new JLabel(" ");
        String deleteText = "削除する";
        String cancelText = (String) UIManager.get("OptionPane.cancelButtonText");

        int option = JOptionPane.showOptionDialog(
                this.getUI(),
                msg,
                ClientContext.getFrameTitle("ドキュメント削除"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{deleteText, cancelText},
                cancelText);

        // キャンセルの場合はリターンする
        if (option != 0) {
            return;
        }

        // 削除する status = 'D'
        long deletePk = selectedKarte.getModel().getId();
        DeleteTask task = new DeleteTask(getContext(), deletePk);
        task.execute();
    }

    /**
     * データベースから取得したDocumentModelからKarteViewerを追加作成し表示する
     *
     * @param models KarteModel
     */
    private void addKarteViewer(List<DocumentModel> models) {

        if (models == null) {
            return;
        }

        // マルチスレッド化
        //LapTimer timer = new LapTimer();
        //timer.start();
        
        // タスクリストを準備する
        List<Callable<KarteViewer>> taskList = new ArrayList<Callable<KarteViewer>>();
        for (DocumentModel docModel : models) {
            Callable<KarteViewer> task = new MakeViewerTask(docModel);
            taskList.add(task);
        }
        
        // タスクリストを実行する
        MultiTaskExecutor exec = new MultiTaskExecutor(taskList);
        try {
            List<Future<KarteViewer>> futures = exec.execute();
            for (Future<KarteViewer> future : futures) {
                try {
                    // karteViewerMapに登録する
                    KarteViewer viewer = future.get();
                    karteViewerMap.put(viewer.getModel().getId(), viewer);
                } catch (ExecutionException ex) {
                    logger.debug(ex);
                }
            }
        } catch (InterruptedException ex) {
            logger.debug(ex);
        }
        
        //後片付け
        exec.dispose();

        //timer.stop("KarteRendering");
    }
    
    private class MakeViewerTask implements Callable {

        private DocumentModel docModel;
        
        private MakeViewerTask(DocumentModel docModel) {
            this.docModel = docModel;
        }
        
        @Override
        public KarteViewer call() throws Exception {
            
            // DocumentModelにDocInfoModelを設定する
            for (DocInfoModel info : docInfoArray) {
                if (docModel.getId() == info.getDocPk()) {
                    docModel.setDocInfoModel(info);
                    break;
                }
            }

            // シングル及び２号用紙の判定を行い、KarteViewer を生成する
            final KarteViewer viewer = createKarteViewer(docModel.getDocInfoModel());
            viewer.setContext(getContext());
            viewer.setModel(docModel);

            // このコールでモデルのレンダリングが開始される
            viewer.start();

            // ダブルクリックされたカルテを別画面で表示する
            // MouseListener を生成して KarteViewer の Pane にアタッチする
            final MouseListener ml = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    int cnt = e.getClickCount();
                    if (cnt == 2) {
                        // 選択した Karte を EditoFrame で開く
                        setSelectedKarte(viewer);
                        openKarte();
                    } else if (cnt == 1) {
                        setSelectedKarte(viewer);
                    }
                }
            };
            viewer.addMouseListener(ml);

            // KarteViewerのJTextPaneにKarteScrollerPanelのActionMapを設定する
            // これをしないとJTextPaneにフォーカスがあるとキーでスクロールできない
            ActionMap amap = scrollerPanel.getActionMap();
            viewer.setParentActionMap(amap);
            
            // karteViewerMapに登録する
            //karteViewerMap.put(docModel.getId(), viewer);
            
            return viewer;
        }
    }

    /**
     * 文書をデータベースから取得するタスククラス。
     */
    private final class KarteTask extends DBTask<List<DocumentModel>, Void> {

        private List<Long> docIdList;

        public KarteTask(Chart ctx, List<Long> docIdList) {
            super(ctx);
            this.docIdList = docIdList;
        }

        @Override
        protected List<DocumentModel> doInBackground() {
            
            //LapTimer timer = new LapTimer();
            //timer.start();
            
            logger.debug("KarteTask doInBackground");
            DocumentDelegater ddl = DocumentDelegater.getInstance();
            List<DocumentModel> result = ddl.getDocuments(docIdList);
            logger.debug("doInBackground noErr, return result");
            
            //timer.stop("Database access");
            
            if (result != null && !result.isEmpty()) {
                // KarteViewerを追加
                addKarteViewer(result);
            }
            return result;
        }

        @Override
        protected void succeeded(List<DocumentModel> result) {
            logger.debug("KarteTask succeeded");
            if (result != null && !result.isEmpty()) {
                // KarteViewerを追加
                //addKarteViewer(result);
                // KarteViewersを表示する
                showKarteViewers();
            }
        }
    }

    /**
     * カルテの削除タスククラス。
     */
    private final class DeleteTask extends DBTask<Boolean, Void> {

        private long docPk;
        private DocumentDelegater ddl;

        public DeleteTask(Chart ctx, long docPk) {
            super(ctx);
            this.docPk = docPk;
            this.ddl = DocumentDelegater.getInstance();
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            logger.debug("DeleteTask started");
            ddl.deleteDocument(docPk);
            return true;
        }

        @Override
        protected void succeeded(Boolean result) {
            logger.debug("DeleteTask succeeded");
            Chart chart = (KarteDocumentViewer.this).getContext();
            chart.getDocumentHistory().getDocumentHistory();
        }
    }

    /**
     * 抽象状態クラス。
     */
    private abstract class BrowserState {

        private BrowserState() {
        }

        protected abstract void enter();
    }

    /**
     * 表示するカルテがない状態を表す。
     */
    private final class EmptyState extends BrowserState {

        public EmptyState() {
        }

        @Override
        public void enter() {
            boolean canEdit = !isReadOnly();
            getContext().enabledAction(GUIConst.ACTION_NEW_KARTE, canEdit);     // 新規カルテ
            getContext().enabledAction(GUIConst.ACTION_NEW_DOCUMENT, canEdit);  // 新規文書
            getContext().enabledAction(GUIConst.ACTION_MODIFY_KARTE, false);    // 修正
            getContext().enabledAction(GUIConst.ACTION_DELETE, false);          // 削除
            getContext().enabledAction(GUIConst.ACTION_PRINT, false);           // 印刷
            getContext().enabledAction(GUIConst.ACTION_ASCENDING, false);       // 昇順
            getContext().enabledAction(GUIConst.ACTION_DESCENDING, false);      // 降順
            getContext().enabledAction(GUIConst.ACTION_SHOW_MODIFIED, false);   // 修正履歴表示
//pns^
            getContext().enabledAction(GUIConst.ACTION_FIND_FIRST, false);
            getContext().enabledAction(GUIConst.ACTION_FIND_NEXT, false);
            getContext().enabledAction(GUIConst.ACTION_FIND_PREVIOUS, false);
            getContext().enabledAction(GUIConst.ACTION_SEND_CLAIM, false);
//pns$
        }
    }

    /**
     * カルテが表示されている状態を表す。
     */
    private final class ClaenState extends BrowserState {

        public ClaenState() {
        }

        @Override
        public void enter() {

            //
            // 新規カルテが可能なケース 仮保存でないことを追加
            //
            boolean canEdit = !isReadOnly();
            boolean tmpKarte = false;
//masuda^   制限解除       
/*
            if (selectedKarte != null) {
                String state = selectedKarte.getModel().getDocInfoModel().getStatus();
                if (state.equals(IInfoModel.STATUS_TMP)) {
                    tmpKarte = true;
                }
            }
*/
//masuda$
            boolean newOk = (canEdit && !tmpKarte);
            getContext().enabledAction(GUIConst.ACTION_NEW_KARTE, newOk);        // 新規カルテ
            getContext().enabledAction(GUIConst.ACTION_NEW_DOCUMENT, canEdit);   // 新規文書
            getContext().enabledAction(GUIConst.ACTION_MODIFY_KARTE, canEdit);   // 修正
            getContext().enabledAction(GUIConst.ACTION_DELETE, canEdit);         // 削除
            getContext().enabledAction(GUIConst.ACTION_PRINT, true);             // 印刷
            getContext().enabledAction(GUIConst.ACTION_ASCENDING, true);         // 昇順
            getContext().enabledAction(GUIConst.ACTION_DESCENDING, true);        // 降順
            getContext().enabledAction(GUIConst.ACTION_SHOW_MODIFIED, true);     // 修正履歴表示
//pns^
            getContext().enabledAction(GUIConst.ACTION_FIND_FIRST, true);
            getContext().enabledAction(GUIConst.ACTION_FIND_NEXT, true);
            getContext().enabledAction(GUIConst.ACTION_FIND_PREVIOUS, true);
            getContext().enabledAction(GUIConst.ACTION_SEND_CLAIM, true);
//pns$
            //-----------------------------------------
            // CLAIM 送信が可能なケース
            //-----------------------------------------
            boolean sendOk = ((ChartImpl) getContext()).isSendClaim();
            sendOk = sendOk && (!tmpKarte);
            getContext().enabledAction(GUIConst.ACTION_SEND_CLAIM, sendOk);       // CLAIM送信
        }
    }

    /**
     * StateContext クラス。
     */
    private final class StateMgr {

        private BrowserState emptyState = new EmptyState();
        private BrowserState cleanState = new ClaenState();
        private BrowserState currentState;

        public StateMgr() {
            currentState = emptyState;
        }

        public void processEmptyEvent() {
            currentState = emptyState;
            this.enter();
        }

        public void processCleanEvent() {
            currentState = cleanState;
            this.enter();
        }

        public void enter() {
            currentState.enter();
        }
    }
    
//masuda^
    private boolean canModifyKarte() {
        
        DocumentModel base = selectedKarte.getModel();
        
        if (base == null || base.getDocInfoModel() == null) {
            return true;
        }

        List<EditorFrame> editorFrames = Dolphin.getInstance().getAllEditorFrames();
        if (editorFrames.isEmpty()) {
            return true;
        }

        long baseDocPk = base.getDocInfoModel().getDocPk();
        for (EditorFrame ef : editorFrames) {
            long parentDocPk = ef.getParentDocPk();
            if (baseDocPk == parentDocPk) {
                // parentPkが同じEditorFrameがある場合はFrameをtoFrontする
                ef.getFrame().setExtendedState(java.awt.Frame.NORMAL);
                ef.getFrame().toFront();
                return false;
            }
        }
        // STATUS_FINAL/TMP以外が設定されていたらenterしない
        String status = base.getDocInfoModel().getStatus();
        if (!IInfoModel.STATUS_FINAL.equals(status) && !IInfoModel.STATUS_TMP.equals(status)) {
            String title = ClientContext.getFrameTitle("カルテ編集");
            String msg = "このカルテは編集できません。";
            JOptionPane.showMessageDialog(getContext().getFrame(), msg, title, JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
    
    private class KarteViewerComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            KarteViewer k1 = (KarteViewer) o1;
            KarteViewer k2 = (KarteViewer) o2;
            return k1.getModel().getDocInfoModel().compareTo(k2.getModel().getDocInfoModel());
        }
        
    }
//masuda$
}
