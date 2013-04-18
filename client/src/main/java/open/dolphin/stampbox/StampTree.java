package open.dolphin.stampbox;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.XMLEncoder;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import open.dolphin.client.ClientContext;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.GUIDGenerator;
import org.apache.log4j.Logger;

/**
 * StampTree
 *
 * @author Kazushi Minagawa, Digital Globe, Inc. 
 * @author modified by masuda, Masuda Naika
 */
public class StampTree extends JTree implements TreeModelListener {

    public static final String SELECTED_NODE_PROP = "selectedNodeProp";
    private static final int TOOLTIP_LENGTH = 35;
    private static final ImageIcon ASP_ICON = ClientContext.getImageIconAlias("icon_world_small");
    private static final ImageIcon LOCAL_ICON = ClientContext.getImageIconAlias("icon_stamp_drag_leaf");
    private static final String NEW_FOLDER_NAME = "新規フォルダ";
    private static final String STAMP_SAVE_TASK_NAME = "スタンプ保存";
    // ASP Tree かどうかのフラグ 
    private boolean asp;
    // 個人用Treeかどうかのフラグ 
    private boolean userTree;
    // StampBox
    private StampBoxPlugin stampBox;
    // Logger, Application
    private Logger logger;
    // timerTask 関連
    private SimpleWorker worker;
    private javax.swing.Timer taskTimer;
    private ProgressMonitor monitor;
    private int delayCount;
    private int maxEstimation = 120 * 1000;   // 120 秒
    private int delay = 300;               // 300 mmsec

    /**
     * StampTreeオブジェクトを生成する。
     *
     * @param model TreeModel
     */
    public StampTree(TreeModel model) {

        super(model);

        logger = ClientContext.getBootLogger();

        this.putClientProperty("JTree.lineStyle", "Angled"); // 水平及び垂直線を使用する
        this.setEditable(false); // ノード名を編集不可にする
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); // Single Selection// にする

        this.setRootVisible(false);
        this.setDragEnabled(true);

//masuda^
        TreeCellRenderer renderer = new StampTreeRenderer();
        setCellRenderer(renderer);
        /*
         * // デフォルトのセルレンダラーを置き換える final TreeCellRenderer oldRenderer =
         * this.getCellRenderer(); TreeCellRenderer r = new TreeCellRenderer() {
         *
         * @Override public Component getTreeCellRendererComponent(JTree tree,
         * Object value, boolean selected, boolean expanded, boolean leaf, int
         * row, boolean hasFocus) {
         *
         * Component c = oldRenderer.getTreeCellRendererComponent(tree, value,
         * selected, expanded, leaf, row, hasFocus); if (leaf && c instanceof
         * JLabel) { JLabel l = (JLabel) c; Object o = ((StampTreeNode)
         * value).getUserObject(); if (o instanceof ModuleInfoBean) {
         *
         * // 固有のアイコンを設定する if (isAsp()) { l.setIcon(ASP_ICON); } else {
         * l.setIcon(LOCAL_ICON); } // ToolTips を設定する
         * l.setToolTipText(((ModuleInfoBean) o).getStampMemo()); } } return c;
         * } }; this.setCellRenderer(r);
         */
//masuda$

        // Listens TreeModelEvent
        model.addTreeModelListener(StampTree.this);

        // Enable ToolTips
        StampTree.this.enableToolTips(true);
    }

    /**
     * このStampTreeのTreeInfoを返す。
     *
     * @return Tree情報
     */
    public TreeInfo getTreeInfo() {
        StampTreeNode node = (StampTreeNode) this.getModel().getRoot();
        TreeInfo info = (TreeInfo) node.getUserObject();
        return info;
    }

    /**
     * このStampTreeのエンティティを返す。
     *
     * @return エンティティ
     */
    public String getEntity() {
        return getTreeInfo().getEntity();
    }

    /**
     * このStampTreeの名前を返す。
     *
     * @return 名前
     */
    public String getTreeName() {
        return getTreeInfo().getName();
    }

    /**
     * UserTreeかどうかを返す。
     *
     * @return UserTreeの時true
     */
    public boolean isUserTree() {
        return userTree;
    }

    /**
     * UserTreeかどうかを設定する。
     *
     * @param userTree UserTreeの時true
     */
    public void setUserTree(boolean userTree) {
        this.userTree = userTree;
    }

    /**
     * ASP提供Treeかどうかを返す。
     *
     * @return ASP提供の時 true
     */
    public boolean isAsp() {
        return asp;
    }

    /**
     * ASP提供Treeかどうかを設定する。
     *
     * @param asp ASP提供の時 true
     */
    public void setAsp(boolean asp) {
        this.asp = asp;
    }

    /**
     * Enable or disable tooltip
     */
    public void enableToolTips(boolean state) {

        ToolTipManager mgr = ToolTipManager.sharedInstance();
        if (state) {
            // Enable tooltips
            mgr.registerComponent(this);

        } else {
            mgr.unregisterComponent(this);
        }
    }

    /**
     * Set StampBox reference
     */
    public void setStampBox(StampBoxPlugin stampBox) {
        this.stampBox = stampBox;
    }

    /**
     * 選択されているノードを返す。
     */
    public StampTreeNode getSelectedNode() {
        return (StampTreeNode) this.getLastSelectedPathComponent();
    }

    /**
     * 引数のポイント位置のノードを返す。
     */
    public StampTreeNode getNode(Point p) {
        TreePath path = this.getPathForLocation(p.x, p.y);
        return (path != null)
                ? (StampTreeNode) path.getLastPathComponent()
                : null;
    }

    /**
     * このStampTreeにenter()する。
     */
    public void enter() {
    }

    public boolean addStamp(final StampTreeNode parent, ModuleModel droppedStamp, final int childIndex) {

        boolean ret = false;
        if (parent == null || droppedStamp == null) {
            return ret;
        }

        //---------------------------------------
        // Drop された Stamp の ModuleInfoを得る
        //---------------------------------------
        ModuleInfoBean droppedInfo = droppedStamp.getModuleInfoBean();

        //----------------------------------------------
        // データベースへ droppedStamp のデータモデルを保存する
        //----------------------------------------------
        // Entityを生成する
        final StampModel stampModel = new StampModel();
        final String stampId = GUIDGenerator.generate(stampModel);      // stampId
        stampModel.setId(stampId);
        stampModel.setUserId(Project.getUserModel().getId());           // userId
        stampModel.setEntity(droppedInfo.getEntity());                  // entity
        stampModel.setStampBytes(getXMLBytes(droppedStamp.getModel())); // XML

        //----------------------------------------------
        // Tree に加える新しい StampInfo を生成する
        //----------------------------------------------
        final ModuleInfoBean info = new ModuleInfoBean();
        info.setStampName(droppedInfo.getStampName());      // オリジナル名
        info.setEntity(droppedInfo.getEntity());            // Entity
        info.setStampRole(droppedInfo.getStampRole());      // Role
        info.setStampMemo(constractToolTip(droppedStamp));  // Tooltip
        info.setStampId(stampId);                           // StampID

        worker = new SimpleWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                logger.debug("addStamp doInBackground");
//masuda^
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                String ret = sdl.putStamp(stampModel);
                return ret;
            }

            @Override
            protected void succeeded(String result) {
                logger.debug("addStamp succeeded");
                StampTreeNode node = new StampTreeNode(info);
                DefaultTreeModel model = (DefaultTreeModel) StampTree.this.getModel();
                int index = childIndex != -1 ? childIndex : parent.getChildCount();
                model.insertNodeInto(node, parent, index);
            }

            @Override
            protected void cancelled() {
                logger.debug("addStamp cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                logger.debug("addStamp failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ保存";
        String note = info.getStampName() + "を保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();
        return true;
    }

    /**
     * 1. KartePaneから drop されたスタンプをツリーに加える。 2. スタンプ道具箱エディタで編集されたスタンプを加える。
     */
    public boolean addStamp(ModuleModel droppedStamp, final StampTreeNode selected) {

        boolean ret = false;
        if (droppedStamp == null) {
            return ret;
        }

        //---------------------------------------
        // Drop された Stamp の ModuleInfoを得る
        //---------------------------------------
        ModuleInfoBean droppedInfo = droppedStamp.getModuleInfoBean();

        //----------------------------------------------
        // データベースへ droppedStamp のデータモデルを保存する
        //----------------------------------------------
        // Entityを生成する
        final StampModel stampModel = new StampModel();
        final String stampId = GUIDGenerator.generate(stampModel);      // stampId
        stampModel.setId(stampId);
        stampModel.setUserId(Project.getUserModel().getId());           // userId
        stampModel.setEntity(droppedInfo.getEntity());                  // entity
        stampModel.setStampBytes(getXMLBytes(droppedStamp.getModel())); // XML

        //----------------------------------------------
        // Tree に加える新しい StampInfo を生成する
        //----------------------------------------------
        final ModuleInfoBean info = new ModuleInfoBean();
        info.setStampName(droppedInfo.getStampName());      // オリジナル名
        info.setEntity(droppedInfo.getEntity());            // Entity
        info.setStampRole(droppedInfo.getStampRole());      // Role
        info.setStampMemo(constractToolTip(droppedStamp));  // Tooltip
        info.setStampId(stampId);                           // StampID

        worker = new SimpleWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                logger.debug("addStamp doInBackground");
//masuda^
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                String ret = sdl.putStamp(stampModel);
                return ret;
            }

            @Override
            protected void succeeded(String result) {
                logger.debug("addStamp succeeded");
                addInfoToTree(info, selected);
            }

            @Override
            protected void cancelled() {
                logger.debug("addStamp cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                logger.debug("addStamp failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ保存";
        String note = info.getStampName() + "を保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();
        return true;
    }

    /**
     * StampTree に新しいノードを加える。
     *
     * @param info 追加するノードの情報
     * @param selected カーソルの下にあるノード(Drop 位置のノード）
     */
    public void addInfoToTree(ModuleInfoBean info, StampTreeNode selected) {

        //----------------------------------------------
        // StampInfo から新しい StampTreeNode を生成する
        //----------------------------------------------
        StampTreeNode node = new StampTreeNode(info);

        //----------------------------------------------
        // Drop 位置のノードによって追加する位置を決める
        //----------------------------------------------
        if (selected != null && selected.isLeaf()) {
            //----------------------------------------------
            // Drop位置のノードが葉の場合、その前に挿入する
            //----------------------------------------------
            StampTreeNode newParent = (StampTreeNode) selected.getParent();
            int index = newParent.getIndex(selected);
            DefaultTreeModel model = (DefaultTreeModel) this.getModel();
            model.insertNodeInto(node, newParent, index);
            //----------------------------------------------
            // 追加したノードを選択する
            //----------------------------------------------
            TreeNode[] path = model.getPathToRoot(node);
            ((JTree) this).setSelectionPath(new TreePath(path));

        } else if (selected != null && (!selected.isLeaf())) {
            //----------------------------------------------
            // Drop位置のノードが子を持つ時、最後の子として挿入する
            //----------------------------------------------
            DefaultTreeModel model = (DefaultTreeModel) this.getModel();
            model.insertNodeInto(node, selected, selected.getChildCount());
            //----------------------------------------------
            // 追加したノードを選択する
            //----------------------------------------------
            TreeNode[] path = model.getPathToRoot(node);
            ((JTree) this).setSelectionPath(new TreePath(path));

        } else {
            //---------------------------------------------------------
            // Drop 位置のノードが null でコールされるケースがある
            // 1. このtreeのスタンプではない場合、該当するTreeのルートに加える
            // 2. パス Tree など、まだノードを持たない初期状態の時
            //---------------------------------------------------------

            // Stamp ボックスから entity に対応する tree を得る
            StampTree another = stampBox.getStampTree(info.getEntity());
            boolean myTree = (another == this);
            final String treeName = another.getTreeName();
            DefaultTreeModel model = (DefaultTreeModel) another.getModel();
            StampTreeNode root = (StampTreeNode) model.getRoot();
            root.add(node);
            model.reload(root);

            //---------------------------------------------------------
            // 追加したノードを選択する
            //---------------------------------------------------------
            TreeNode[] path = model.getPathToRoot(node);
            ((JTree) this).setSelectionPath(new TreePath(path));

            // メッセージを表示する
            if (!myTree) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        StringBuilder buf = new StringBuilder();
                        buf.append("スタンプは個人用の ");
                        buf.append(treeName);
                        buf.append(" に保存しました。");
                        JOptionPane.showMessageDialog(
                                StampTree.this,
                                buf.toString(),
                                ClientContext.getFrameTitle(STAMP_SAVE_TASK_NAME),
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }
        }
    }

    /**
     * Stampを置き換える。
     *
     * @param stampToReplcae 置き換えるスタンプ
     */
    public void replaceStamp(ModuleModel stampToReplcae) {

        if (stampToReplcae == null) {
            return;
        }

        // 置き換える Stamp の ModuleInfoを得る
        ModuleInfoBean stampInfo = stampToReplcae.getModuleInfoBean();
//masuda^   memoを更新
        stampInfo.setStampMemo(constractToolTip(stampToReplcae));
//masuda$

        //-------------------------------------------------
        // データベースへ stampToReplcae のデータモデルを保存する
        // Entityを生成する
        //-------------------------------------------------
        final StampModel stampModel = new StampModel();
        final String stampId = stampInfo.getStampId();                      // stampId
        stampModel.setId(stampId);
        stampModel.setUserId(Project.getUserModel().getId());               // userId
        stampModel.setEntity(stampInfo.getEntity());                        // entity
        stampModel.setStampBytes(getXMLBytes(stampToReplcae.getModel()));   // XML

        worker = new SimpleWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                logger.debug("replaceStamp doInBackground");
//masuda^
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                String ret = sdl.replaceStamp(stampModel);
                return ret;
            }

            @Override
            protected void succeeded(String result) {
                logger.debug("replaceStamp succeeded");
            }

            @Override
            protected void cancelled() {
                logger.debug("replaceStamp cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                logger.debug("replaceStamp failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ保存";
        String note = stampInfo.getStampName() + "を保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();
    }

    /**
     * Diagnosis Table から Drag & Drop されたRegisteredDiagnosisをスタンプ化する。
     */
    public boolean addDiagnosis(RegisteredDiagnosisModel rd, final StampTreeNode selected) {

        if (rd == null) {
            return false;
        }

        // クリア
        rd.setId(0L);
        rd.setKarteBean(null);
        rd.setUserModel(null);
        rd.setDiagnosisCategoryModel(null);
        rd.setDiagnosisOutcomeModel(null);
        rd.setFirstEncounterDate(null);
        rd.setStartDate(null);
        rd.setEndDate(null);
        rd.setRelatedHealthInsurance(null);
        rd.setFirstConfirmDate(null);
        rd.setConfirmDate(null);
        rd.setStatus(null);
        rd.setPatientLiteModel(null);
        rd.setUserLiteModel(null);

        RegisteredDiagnosisModel add = new RegisteredDiagnosisModel();
        add.setDiagnosis(rd.getDiagnosis());
        add.setDiagnosisCode(rd.getDiagnosisCode());
        add.setDiagnosisCodeSystem(rd.getDiagnosisCodeSystem());

        ModuleModel stamp = new ModuleModel();
        stamp.setModel(add);

        // データベースへ Stamp のデータモデルを永続化する
        final StampModel addStamp = new StampModel();
        final String stampId = GUIDGenerator.generate(addStamp);
        addStamp.setId(stampId);
        addStamp.setUserId(Project.getUserModel().getId());
        addStamp.setEntity(IInfoModel.ENTITY_DIAGNOSIS);
        addStamp.setStampBytes(getXMLBytes(stamp.getModel()));

        // Tree に加える 新しい StampInfo を生成する
        final ModuleInfoBean info = new ModuleInfoBean();
        info.setStampId(stampId);                       // Stamp ID
        info.setStampName(add.getDiagnosis());          // 傷病名
        info.setEntity(IInfoModel.ENTITY_DIAGNOSIS);    // カテゴリ
        info.setStampRole(IInfoModel.ENTITY_DIAGNOSIS); // Role

        StringBuilder buf = new StringBuilder();
        buf.append(add.getDiagnosis());
        String cd = add.getDiagnosisCode();
        if (cd != null) {
            buf.append("(");
            buf.append(cd);
            buf.append(")"); // Tooltip
        }
        info.setStampMemo(buf.toString());

        worker = new SimpleWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                logger.debug("addDiagnosis doInBackground");
//masuda^
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                String ret = sdl.putStamp(addStamp);
                return ret;
            }

            @Override
            protected void succeeded(String result) {
                logger.debug("addDiagnosis succeeded");
                addInfoToTree(info, selected);
            }

            @Override
            protected void cancelled() {
                logger.debug("addDiagnosis cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                logger.warn("addDiagnosis failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ保存";
        String note = info.getStampName() + "を保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();

        return true;
    }

    /**
     * Diagnosis Table から Drag & Drop されたRegisteredDiagnosisをスタンプ化する。
     */
    public boolean addDiagnosis(final StampTreeNode parent, RegisteredDiagnosisModel rd, final int childIndex) {

        if ((parent == null) || (rd == null)) {
            return false;
        }

        // クリア
        rd.setId(0L);
        rd.setKarteBean(null);
        rd.setUserModel(null);
        rd.setDiagnosisCategoryModel(null);
        rd.setDiagnosisOutcomeModel(null);
        rd.setFirstEncounterDate(null);
        rd.setStartDate(null);
        rd.setEndDate(null);
        rd.setRelatedHealthInsurance(null);
        rd.setFirstConfirmDate(null);
        rd.setConfirmDate(null);
        rd.setStatus(null);
        rd.setPatientLiteModel(null);
        rd.setUserLiteModel(null);

        RegisteredDiagnosisModel add = new RegisteredDiagnosisModel();
        add.setDiagnosis(rd.getDiagnosis());
        add.setDiagnosisCode(rd.getDiagnosisCode());
        add.setDiagnosisCodeSystem(rd.getDiagnosisCodeSystem());

        ModuleModel stamp = new ModuleModel();
        stamp.setModel(add);

        // データベースへ Stamp のデータモデルを永続化する
        final StampModel addStamp = new StampModel();
        final String stampId = GUIDGenerator.generate(addStamp);
        addStamp.setId(stampId);
        addStamp.setUserId(Project.getUserModel().getId());
        addStamp.setEntity(IInfoModel.ENTITY_DIAGNOSIS);
        addStamp.setStampBytes(getXMLBytes(stamp.getModel()));

        // Tree に加える 新しい StampInfo を生成する
        final ModuleInfoBean info = new ModuleInfoBean();
        info.setStampId(stampId);                       // Stamp ID
        info.setStampName(add.getDiagnosis());          // 傷病名
        info.setEntity(IInfoModel.ENTITY_DIAGNOSIS);    // カテゴリ
        info.setStampRole(IInfoModel.ENTITY_DIAGNOSIS); // Role

        StringBuilder buf = new StringBuilder();
        buf.append(add.getDiagnosis());
        String cd = add.getDiagnosisCode();
        if (cd != null) {
            buf.append("(");
            buf.append(cd);
            buf.append(")"); // Tooltip
        }
        info.setStampMemo(buf.toString());

        worker = new SimpleWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                logger.debug("addDiagnosis doInBackground");
//masuda^
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                String ret = sdl.putStamp(addStamp);
                return ret;
            }

            @Override
            protected void succeeded(String result) {
                logger.debug("addDiagnosis succeeded");
                StampTreeNode node = new StampTreeNode(info);
                DefaultTreeModel model = (DefaultTreeModel) StampTree.this.getModel();
                int index = childIndex != -1 ? childIndex : parent.getChildCount();
                model.insertNodeInto(node, parent, index);
            }

            @Override
            protected void cancelled() {
                logger.debug("addDiagnosis cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                logger.warn("addDiagnosis failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ保存";
        String note = info.getStampName() + "を保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();

        return true;
    }

    /**
     * エディタで生成した病名リストを登録する。
     */
    public void addDiagnosis(List<RegisteredDiagnosisModel> list) {

        //System.err.println("StampTree: addDiagnosis");

        if (list == null || list.isEmpty()) {
            return;
        }

        final ArrayList<StampModel> stampList = new ArrayList<StampModel>();
        final ArrayList<ModuleInfoBean> infoList = new ArrayList<ModuleInfoBean>();

        for (RegisteredDiagnosisModel rd : list) {
            // クリア
            rd.setId(0L);
            rd.setKarteBean(null);
            rd.setUserModel(null);
            rd.setDiagnosisCategoryModel(null);
            rd.setDiagnosisOutcomeModel(null);
            rd.setFirstEncounterDate(null);
            rd.setStartDate(null);
            rd.setEndDate(null);
            rd.setRelatedHealthInsurance(null);
            rd.setFirstConfirmDate(null);
            rd.setConfirmDate(null);
            rd.setStatus(null);
            rd.setPatientLiteModel(null);
            rd.setUserLiteModel(null);

            RegisteredDiagnosisModel add = new RegisteredDiagnosisModel();
            add.setDiagnosis(rd.getDiagnosis());
            add.setDiagnosisCode(rd.getDiagnosisCode());
            add.setDiagnosisCodeSystem(rd.getDiagnosisCodeSystem());
//            System.err.println(add.getDiagnosis());
//            System.err.println(add.getDiagnosisCode());
//            System.err.println(add.getDiagnosisCodeSystem());

            ModuleModel stamp = new ModuleModel();
            stamp.setModel(add);

            // データベースへ Stamp のデータモデルを永続化する
            StampModel addStamp = new StampModel();
            String stampId = GUIDGenerator.generate(addStamp);
            addStamp.setId(stampId);
            addStamp.setUserId(Project.getUserModel().getId());
            addStamp.setEntity(IInfoModel.ENTITY_DIAGNOSIS);
            addStamp.setStampBytes(getXMLBytes(stamp.getModel()));
//            System.err.println(addStamp.getId());
//            System.err.println(addStamp.getUserId());
//            System.err.println(addStamp.getEntity());
            stampList.add(addStamp);

            // Tree に加える 新しい StampInfo を生成する
            ModuleInfoBean info = new ModuleInfoBean();
            info.setStampId(stampId);                       // Stamp ID
            info.setStampName(add.getDiagnosis());          // 傷病名
            info.setEntity(IInfoModel.ENTITY_DIAGNOSIS);    // カテゴリ
            info.setStampRole(IInfoModel.ENTITY_DIAGNOSIS); // Role

            StringBuilder buf = new StringBuilder();
            buf.append(add.getDiagnosis());
            String cd = add.getDiagnosisCode();
            if (cd != null) {
                buf.append("(");
                buf.append(cd);
                buf.append(")"); // Tooltip
            }
            info.setStampMemo(buf.toString());
            infoList.add(info);
        }

        worker = new SimpleWorker<List<String>, Void>() {

            @Override
            protected List<String> doInBackground() throws Exception {
                logger.debug("addDiagnosis doInBackground");
//masuda^
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                sdl.putStamp(stampList);
                return null;
            }

            @Override
            protected void succeeded(List<String> result) {
                logger.debug("addDiagnosis succeeded");
                for (ModuleInfoBean info : infoList) {
//masuda^   スタンプ挿入位置を選択中の位置にする
                    StampTreeNode target = getSelectedNode();
                    addInfoToTree(info, target);
//masuda$
                }
            }

            @Override
            protected void cancelled() {
                logger.debug("addDiagnosis cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                logger.warn("addDiagnosis failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ保存";
        String note = "病名スタンプを保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();
    }

    /**
     * テキストスタンプを追加する。
     */
    public boolean addTextStamp(final StampTreeNode parent, String text, final int childIndex) {

        if ((parent == null) || (text == null) || (text.length() == 0) || text.equals("")) {
            return false;
        }

        TextStampModel stamp = new TextStampModel();
        stamp.setText(text);

        // データベースへ Stamp のデータモデルを永続化する
        final StampModel addStamp = new StampModel();
        final String stampId = GUIDGenerator.generate(addStamp);
        addStamp.setId(stampId);
        addStamp.setUserId(Project.getUserModel().getId());
        addStamp.setEntity(IInfoModel.ENTITY_TEXT);
        addStamp.setStampBytes(getXMLBytes((IInfoModel) stamp));

        // Tree へ加える 新しい StampInfo を生成する
        final ModuleInfoBean info = new ModuleInfoBean();
        int len = text.length() > 16 ? 16 : text.length();
        String name = text.substring(0, len);
        len = name.indexOf("\n");
        if (len > 0) {
            name = name.substring(0, len);
        }
        info.setStampName(name);                    //
        info.setEntity(IInfoModel.ENTITY_TEXT);     // カテゴリ
        info.setStampRole(IInfoModel.ENTITY_TEXT);  // Role
        info.setStampMemo(text);                    // Tooltip
        info.setStampId(stampId);                   // Stamp ID

        worker = new SimpleWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                logger.debug("addTextStamp doInBackground");
//masuda^
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                String ret = sdl.putStamp(addStamp);
                return ret;
            }

            @Override
            protected void succeeded(String result) {
                logger.debug("addTextStamp succeeded");
                StampTreeNode node = new StampTreeNode(info);
                DefaultTreeModel model = (DefaultTreeModel) StampTree.this.getModel();
                int index = childIndex != -1 ? childIndex : parent.getChildCount();
                model.insertNodeInto(node, parent, index);
            }

            @Override
            protected void cancelled() {
                logger.debug("addTextStamp cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                logger.warn("addTextStamp failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ保存";
        String note = info.getStampName() + "を保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();

        return true;
    }

    /**
     * テキストスタンプを追加する。
     */
    public boolean addTextStamp(String text, final StampTreeNode selected) {

        if ((text == null) || (text.length() == 0) || text.equals("")) {
            return false;
        }

        TextStampModel stamp = new TextStampModel();
        stamp.setText(text);

        //
        // データベースへ Stamp のデータモデルを永続化する
        //
        final StampModel addStamp = new StampModel();
        final String stampId = GUIDGenerator.generate(addStamp);
        addStamp.setId(stampId);
        addStamp.setUserId(Project.getUserModel().getId());
        addStamp.setEntity(IInfoModel.ENTITY_TEXT);
        addStamp.setStampBytes(getXMLBytes((IInfoModel) stamp));

        //
        // Tree へ加える 新しい StampInfo を生成する
        //
        final ModuleInfoBean info = new ModuleInfoBean();
        int len = text.length() > 16 ? 16 : text.length();
        String name = text.substring(0, len);
        len = name.indexOf("\n");
        if (len > 0) {
            name = name.substring(0, len);
        }
        info.setStampName(name);                    //
        info.setEntity(IInfoModel.ENTITY_TEXT);     // カテゴリ
        info.setStampRole(IInfoModel.ENTITY_TEXT);  // Role
        info.setStampMemo(text);                    // Tooltip
        info.setStampId(stampId);                   // Stamp ID

        worker = new SimpleWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                logger.debug("addTextStamp doInBackground");
//masuda^
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                String ret = sdl.putStamp(addStamp);
                return ret;
            }

            @Override
            protected void succeeded(String result) {
                logger.debug("addTextStamp succeeded");
                addInfoToTree(info, selected);
            }

            @Override
            protected void cancelled() {
                logger.debug("addTextStamp cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                logger.warn("addTextStamp failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ保存";
        String note = info.getStampName() + "を保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();

        return true;
    }

    /**
     * スタンプの情報を表示するための文字列を生成する。
     *
     * @param stamp 情報を生成するスタンプ
     * @return スタンプの情報文字列
     */
    protected String constractToolTip(ModuleModel stamp) {

        String ret = null;

        try {
            StringBuilder buf = new StringBuilder();
            BufferedReader reader = new BufferedReader(new StringReader(stamp.getModel().toString()));

            String line;
            while ((line = reader.readLine()) != null) {

                buf.append(line);

                if (buf.length() < TOOLTIP_LENGTH) {
                    buf.append(",");
                } else {
                    break;
                }
            }
            reader.close();
            if (buf.length() > TOOLTIP_LENGTH) {
                buf.setLength(TOOLTIP_LENGTH);
            }
            buf.append("...");
            ret = buf.toString();

        } catch (IOException e) {
            e.toString();
        }

        return ret;
    }

    /**
     * スタンプタスク共通の warning ダイアログを表示する。
     *
     * @param title ダイアログウインドウに表示するタイトル
     * @param message　エラーメッセージ
     */
    private void warning(String message) {
        String title = ClientContext.getString("stamptree.title");
        JOptionPane.showMessageDialog(
                StampTree.this,
                message,
                ClientContext.getFrameTitle(title),
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * ノードの名前を変更する。
     */
    public void renameNode() {

        if (!isUserTree()) {
            return;
        }

        // Root へのパスを取得する
        StampTreeNode node = getSelectedNode();
        if (node == null) {
            return;
        }
        TreeNode[] nodes = node.getPath();
        TreePath path = new TreePath(nodes);

        // 編集を開始する
        this.setEditable(true);
        this.startEditingAtPath(path);
        // this.setEditable (false); は TreeModelListener で行う
    }

    /**
     * ノードを削除する。
     */
    public void deleteNode() {

        logger.debug("stampTree deleteNode");

        if (!isUserTree()) {
            return;
        }

        //
        // 削除するノードを取得する
        // 右クリックで選択されている
        //
        final StampTreeNode theNode = getSelectedNode();
        if (theNode == null) {
            return;
        }

        //
        // このノードをルートにするサブツリーを前順走査する列挙を生成して返します。
        // 列挙の nextElement() メソッドによって返される最初のノードは、この削除するノードです。
        //
        Enumeration e = theNode.preorderEnumeration();

        //
        // このリストのなかに削除するノードとその子を含める
        //
        final ArrayList<String> deleteList = new ArrayList<String>();

        // エディタから発行があるかどうかのフラグ
        boolean hasEditor = false;

        // 列挙する
        while (e.hasMoreElements()) {

            logger.debug("stampTree deleteNode e.hasMoreElements()");
            StampTreeNode node = (StampTreeNode) e.nextElement();

            if (node.isLeaf()) {

                ModuleInfoBean info = (ModuleInfoBean) node.getUserObject();
                String stampId = info.getStampId();
                //
                // エディタから発行がある場合は中止する
                //
                if (info.getStampName().equals("エディタから発行...") && (!info.isSerialized())) {
                    hasEditor = true;
                    break;
                }

                //
                // IDが付いているもののみを加える
                //
                if (stampId != null) {
                    deleteList.add(stampId);
                    logger.debug("added " + info.getStampName());
                }
            }
        }

        //
        // エディタから発行が有った場合はダイアログを表示し
        // リターンする
        //
        if (hasEditor) {
            String msg0 = "エディタから発行は消去できません。フォルダに含まれている";
            String msg1 = "場合は Drag & Drop で移動後、再度実行してください。";
            String taskTitle = ClientContext.getString("stamptree.title");
            JOptionPane.showMessageDialog(
                    (Component) null,
                    new Object[]{msg0, msg1},
                    ClientContext.getFrameTitle(taskTitle),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        //
        // 削除するフォルダが空の場合は削除してリターンする
        // リストのサイズがゼロかつ theNode が葉でない時
        // 
        if (deleteList.isEmpty() && (!theNode.isLeaf())) {
            DefaultTreeModel model = (DefaultTreeModel) (StampTree.this).getModel();
            model.removeNodeFromParent(theNode);
            return;
        }

        // データベースのスタンプを削除するデリゲータを生成する
//masuda^   シングルトン化
        //final StampDelegater sdl = new StampDelegater();
        final StampDelegater sdl = StampDelegater.getInstance();
//masuda$

        worker = new SimpleWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                logger.debug("deleteNode doInBackground");
                sdl.removeStamps(deleteList);
                return null;
            }

            @Override
            protected void succeeded(Void result) {
                logger.debug("deleteNode succeeded");

                //---------------------------------------
                // 成功している場合は Tree からノードを削除する
                //---------------------------------------
                DefaultTreeModel model = (DefaultTreeModel) (StampTree.this).getModel();
                model.removeNodeFromParent(theNode);
            }

            @Override
            protected void cancelled() {
                logger.debug("deleteNode cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                //---------------------------------------
                // 失敗した場合も削除する 2011-02-9
                //---------------------------------------
                DefaultTreeModel model = (DefaultTreeModel) (StampTree.this).getModel();
                model.removeNodeFromParent(theNode);
                logger.debug("deleteNode failed");
                logger.warn(cause.getCause());
                logger.warn(cause.getMessage());
            }

            @Override
            protected void startProgress() {
                delayCount = 0;
                taskTimer.start();
            }

            @Override
            protected void stopProgress() {
                taskTimer.stop();
                monitor.close();
                taskTimer = null;
                monitor = null;
            }
        };

        String message = "スタンプ削除";
        String note = "スタンプを削除しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        maxEstimation = 60 * 1000;
        delay = 300;
        monitor = new ProgressMonitor(c, message, note, 0, maxEstimation / delay);

        taskTimer = new Timer(delay, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delayCount++;

                if (monitor.isCanceled() && (!worker.isCancelled())) {
                    worker.cancel(true);

                } else {
                    monitor.setProgress(delayCount);
                }
            }
        });

        worker.execute();
    }

    /**
     * 新規のフォルダを追加する
     */
    public void createNewFolder() {

        if (!isUserTree()) {
            return;
        }

        // フォルダノードを生成する
        StampTreeNode folder = new StampTreeNode(NEW_FOLDER_NAME);

        //
        // 生成位置となる選択されたノードを得る
        //
        StampTreeNode selected = getSelectedNode();

        if (selected != null && selected.isLeaf()) {
            //
            // 選択位置のノードが葉の場合、その前に挿入する
            //
            StampTreeNode newParent = (StampTreeNode) selected.getParent();
            int index = newParent.getIndex(selected);
            DefaultTreeModel model = (DefaultTreeModel) this.getModel();
            model.insertNodeInto(folder, newParent, index);

        } else if (selected != null && (!selected.isLeaf())) {
            //
            // 選択位置のノードが子を持つ時、最後の子として挿入する
            //
            DefaultTreeModel model = (DefaultTreeModel) this.getModel();
            model.insertNodeInto(folder, selected, selected.getChildCount());
        }

        //TreePath parentPath = new TreePath(parent.getPath());
        //this.expandPath(parentPath);
    }

    /**
     * コピー。
     */
    public void copy() {
        Action a = this.getActionMap().get(TransferHandler.getCopyAction().getValue(Action.NAME));
        if (a != null) {
            a.actionPerformed(new ActionEvent(this,
                    ActionEvent.ACTION_PERFORMED,
                    null));
        }
    }

    /**
     * ペースト。
     */
    public void paste() {
        Action a = this.getActionMap().get(TransferHandler.getPasteAction().getValue(Action.NAME));
        if (a != null) {
            a.actionPerformed(new ActionEvent(this,
                    ActionEvent.ACTION_PERFORMED,
                    null));
        }
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        this.setEditable(false);
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
    }

    private byte[] getXMLBytes(Object bean) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        // ムダ？ masuda
        //XMLEncoder e = new XMLEncoder(new BufferedOutputStream(bo));
        XMLEncoder e = new XMLEncoder(bo);
        e.writeObject(bean);
        e.close();
        return bo.toByteArray();
    }
    
    
//masuda^   pns先生のStampTreeDropTargetListenerからコードを拝借
    
    private static final Color DEFAULT_ODD_COLOR = ClientContext.getColor("color.odd");
    //private static final Color DEFAULT_EVEN_COLOR = ClientContext.getColor("color.even");
    private static final Color DEFAULT_EVEN_COLOR = ClientContext.getZebraColor();
    private static final Color[] ROW_COLORS = {DEFAULT_EVEN_COLOR, DEFAULT_ODD_COLOR};
    private static final Color UNLOCKED_COLOR = Color.black;
    private static final Color LOCKED_COLOR = Color.lightGray;

    private static enum DropPosition {

        TOP, BOTTOM, CENTER
    }

    private static enum DrawMode {

        SQUARE, UNDER_LINE, UPPER_LINE
    }

    public enum InsertPosition {

        AFTER, BEFORE, INTO_FOLDER
    };

    private Color lineColor = Color.blue;
    private Object targetNode;
    private boolean isTargetNode;
    private DrawMode drawMode;

    // Quaquaでなくてもストライプに
    // http://nadeausoftware.com/articles/2008/01/java_tip_how_add_zebra_background_stripes_jtree
    @Override
    public void paintComponent(java.awt.Graphics g) {

        // Paint zebra background stripes
        final java.awt.Insets insets = getInsets();
        final int w = getWidth() - insets.left - insets.right;
        final int h = getHeight() - insets.top - insets.bottom;
        final int x = insets.left;
        int y = insets.top;
        int nRows = 0;
        int startRow = 0;
        int rowH = getRowHeight();
        if (rowH > 0) {
            nRows = h / rowH;
        } else {
            // Paint non-uniform height rows first
            final int nItems = getRowCount();
            rowH = 17; // A default for empty trees
            for (int i = 0; i < nItems; i++, y += rowH) {
                rowH = getRowBounds(i).height;
                g.setColor(ROW_COLORS[i & 1]);
                g.fillRect(x, y, w, rowH);
            }
            // Use last row height for remainder of tree area
            nRows = nItems + (insets.top + h - y) / rowH;
            startRow = nItems;
        }
        for (int i = startRow; i < nRows; i++, y += rowH) {
            g.setColor(ROW_COLORS[i & 1]);
            g.fillRect(x, y, w, rowH);
        }
        final int remainder = insets.top + h - y;
        if (remainder > 0) {
            g.setColor(ROW_COLORS[nRows & 1]);
            g.fillRect(x, y, w, remainder);
        }

        // Paint component
        setOpaque(false);
        super.paintComponent(g);
        setOpaque(true);
    }

    // スタンプツリーのセルレンダラー
    private class StampTreeRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (leaf && c instanceof JLabel) {
                JLabel l = (JLabel) c;
                Object o = ((StampTreeNode) value).getUserObject();
                if (o instanceof ModuleInfoBean) {
                    // 固有のアイコンを設定する
                    if (((StampTree) tree).isAsp()) {
                        l.setIcon(ASP_ICON);
                    } else {
                        l.setIcon(LOCAL_ICON);
                    }
                    // ToolTips を設定する
                    l.setToolTipText(((ModuleInfoBean) o).getStampMemo());
                }
            }

            
            isTargetNode = (targetNode != null && targetNode == value);

            // http://nadeausoftware.com/articles/2008/01/java_tip_how_add_zebra_background_stripes_jtree
            if (!(c instanceof DefaultTreeCellRenderer)) {
                c.setBackground(ROW_COLORS[row & 1]);
            } else {
                ((DefaultTreeCellRenderer) c).setBackgroundNonSelectionColor(ROW_COLORS[row & 1]);
            }
            return c;
        }

        /**
         * TransferHandler の repaint() を受けて，Drop 場所をマークする
         *
         * @param g
         */

        @Override
        public void paintComponent(Graphics g) {

            super.paintComponent(g);
            if (!isTargetNode) {
                return;
            }

            g.setColor(lineColor);
            switch (drawMode) {
                case UNDER_LINE:
                    g.fillRect(0, getSize().height - 2, getSize().width, getSize().height);
                    break;
                case UPPER_LINE:
                    g.fillRect(0, 0, getSize().width, 2);
                    break;
                case SQUARE:
                    g.fillRect(0, 0, getSize().width, 2);
                    g.fillRect(0, getSize().height - 2, getSize().width, getSize().height);
                    g.fillRect(0, 0, 2, getSize().height);
                    g.fillRect(getSize().width - 2, 0, getSize().width, getSize().height);
                    break;
            }
        }

    }

    public void paintDropPointMark(Point p) {

        if (p == null) {
            targetNode = null;
            repaint();
            return;
        }

        TreePath target = getClosestPathForLocation(p.x, p.y);

        if (target == null) {
            return;
        }
        
        // レンダラで drop 先を表示するのに使う色をセット
        if (stampBox.isLocked()) {
            lineColor = LOCKED_COLOR;
        } else {
            lineColor = UNLOCKED_COLOR;
        }
        
        // drop しようとしている部分が見えるまでスクロール
        Rectangle r = getPathBounds(target);
        scrollTargetToVisible(target);
        targetNode = target.getLastPathComponent();
        StampTreeNode node = (StampTreeNode) target.getLastPathComponent();

        if (node.isLeaf()) {
            if (topOrBottom(p, r) == DropPosition.TOP) {
                targetWithUpperLine(target);
            } else {
                drawMode = DrawMode.UNDER_LINE;
            }
        } else {
            switch (topOrBottomOrCenter(p, r)) {
                case TOP:
                    targetWithUpperLine(target);
                    break;
                case BOTTOM:
                    targetWithUnderLine();
                    break;
                default: //CENTER
                    targetWithSquare();
            }
        }
        repaint();
    }

    private void targetWithUpperLine(TreePath target) {

        // 一番上か，上と親が違う場合は UpperLine で処理。それ以外は UnderLine に変換
        int row = getRowForPath(target);
        boolean parentIsDifferent;
        if (row == 0) {
            parentIsDifferent = true;
        } else {
            StampTreeNode thisNode = (StampTreeNode) target.getLastPathComponent();
            StampTreeNode aboveNode = (StampTreeNode) getPathForRow(row - 1).getLastPathComponent();
            parentIsDifferent = (thisNode.getParent() != aboveNode.getParent());
        }
        if (parentIsDifferent) {
            drawMode = DrawMode.UPPER_LINE;
        } else {
            // UnderLine で処理
            target = getPathForRow(row - 1);
            targetNode = target.getLastPathComponent();
            targetWithUnderLine();
        }
    }
    
    private void targetWithUnderLine() {
        drawMode = DrawMode.UNDER_LINE;
    }

    private void targetWithSquare() {
        drawMode = DrawMode.SQUARE;
    }
    
    private DropPosition topOrBottom(Point p, Rectangle r) {
        int offsetToTop = p.y - r.y;
        if (offsetToTop < r.height / 2) {
            return DropPosition.TOP;
        } else {
            return DropPosition.BOTTOM;
        }
    }

    private DropPosition topOrBottomOrCenter(Point p, Rectangle r) {
        int offsetToTop = p.y - r.y;
        int offsetToBottom = r.y + r.height - p.y;
        if (offsetToTop < r.height / 3) {
            return DropPosition.TOP;
        } else if (offsetToBottom < r.height / 3) {
            return DropPosition.BOTTOM;
        } else {
            return DropPosition.CENTER;
        }
    }

    private void scrollTargetToVisible(TreePath target) {


        int row = getRowForPath(target);
        if (row >= 1) {
            scrollRowToVisible(row - 1);
        }
        if (row < getRowCount()) {
            scrollRowToVisible(row + 1);
        }
        scrollRowToVisible(row);
    }

    public StampBoxPlugin getStampBox() {
        return stampBox;
    }
}