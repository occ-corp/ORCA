package open.dolphin.stampbox;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import open.dolphin.client.ClientContext;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.helper.ProgressMonitorWorker;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.GUIDGenerator;
import org.apache.log4j.Logger;

/**
 * StampTree
 *
 * @author Kazushi Minagawa, Digital Globe, Inc. 
 * @author modified by masuda, Masuda Naika, refactored on 2013/05/17
 */
public class StampTree extends JTree implements TreeModelListener {

    public static final String SELECTED_NODE_PROP = "selectedNodeProp";
    private static final int TOOLTIP_LENGTH = 35;
    private static final ImageIcon ASP_ICON = ClientContext.getImageIconAlias("icon_world_small");
    private static final ImageIcon LOCAL_ICON = ClientContext.getImageIconAlias("icon_stamp_drag_leaf");
    private static final String NEW_FOLDER_NAME = "新規フォルダ";

    // ASP Tree かどうかのフラグ 
    private boolean asp;
    // 個人用Treeかどうかのフラグ 
    private boolean userTree;
    // StampBox
    private StampBoxPlugin stampBox;
    // Logger, Application
    private Logger logger;

    /**
     * StampTreeオブジェクトを生成する。
     *
     * @param model TreeModel
     */
    public StampTree(TreeModel model) {

        super(model);

        logger = ClientContext.getBootLogger();

        putClientProperty("JTree.lineStyle", "Angled"); // 水平及び垂直線を使用する
        setEditable(false); // ノード名を編集不可にする
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); // Single Selection// にする
        setRootVisible(false);
        setDragEnabled(true);
        
        setDropMode(DropMode.ON_OR_INSERT);
        TreeCellRenderer renderer = new StampTreeRenderer();
        setCellRenderer(renderer);
        
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
        StampTreeNode node = (StampTreeNode) getModel().getRoot();
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
    
    /**
     * 1. KartePaneから drop されたスタンプをツリーに加える。
     */
    public boolean addStamp(final StampTreeNode parent, final ModuleModel[] stamps, final int childIndex) {

        if (parent == null || stamps == null || stamps.length == 0) {
            return false;
        }
        
        final String message = "スタンプ保存";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ModuleModel mm : stamps) {
            if (!first) {
                sb.append("、");
            } else {
                first = false;
            }
            sb.append(mm.getModuleInfoBean().getStampName());
        }
        sb.append("を保存しています...");
        final String note = sb.toString();
        
        Component c = SwingUtilities.getWindowAncestor(this);
        ProgressMonitorWorker worker = new ProgressMonitorWorker<List<ModuleInfoBean>, Void>(c, message, note) {

            @Override
            protected List<ModuleInfoBean> doInBackground() throws Exception {
                
                logger.debug("addStamp doInBackground");
                
                List<StampModel> stampList = new ArrayList<>();
                List<ModuleInfoBean> infoList = new ArrayList<>();
                
                for (ModuleModel stamp : stamps) {
                    //---------------------------------------
                    // Drop された Stamp の ModuleInfoを得る
                    //---------------------------------------
                    ModuleInfoBean droppedInfo = stamp.getModuleInfoBean();

                    //----------------------------------------------
                    // データベースへ droppedStamp のデータモデルを保存する
                    //----------------------------------------------
                    // Entityを生成する
                    StampModel stampModel = new StampModel();
                    String stampId = GUIDGenerator.generate(stampModel);        // stampId
                    stampModel.setId(stampId);
                    stampModel.setUserId(Project.getUserModel().getId());       // userId
                    stampModel.setEntity(droppedInfo.getEntity());              // entity
                    stampModel.setStampBytes(getXMLBytes(stamp.getModel()));    // XML

                    //----------------------------------------------
                    // Tree に加える新しい StampInfo を生成する
                    //----------------------------------------------
                    ModuleInfoBean info = new ModuleInfoBean();
                    info.setStampName(droppedInfo.getStampName());  // オリジナル名
                    info.setEntity(droppedInfo.getEntity());        // Entity
                    info.setStampRole(droppedInfo.getStampRole());  // Role
                    info.setStampMemo(constractToolTip(stamp));     // Tooltip
                    info.setStampId(stampId);                       // StampID
                    
                    stampList.add(stampModel);
                    infoList.add(info);
                }

                StampDelegater sdl = StampDelegater.getInstance();
                sdl.putStamp(stampList);
                
                return infoList;
            }

            @Override
            protected void succeeded(List<ModuleInfoBean> list) {
                logger.debug("addStamp succeeded");
                for (int i = list.size() - 1; i >= 0; --i) {
                    ModuleInfoBean info = list.get(i);
                    StampTreeNode node = new StampTreeNode(info);
                    DefaultTreeModel model = (DefaultTreeModel) getModel();
                    int childCount = parent.getChildCount();
                    int index = (childIndex >= 0 && childIndex < childCount)
                            ? childIndex : childCount;
                    model.insertNodeInto(node, parent, index);
                }
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
        };

        worker.execute();
        return true;
    }

    /**
     * 2. スタンプ道具箱エディタで編集されたスタンプを加える。
     */
    public boolean addOrReplaceStamp(ModuleModel[] newStamps) {

        if (newStamps == null || newStamps.length == 0) {
            return false;
        }

        StampTreeNode target = getSelectedNode();
        ModuleModel firstStamp = newStamps[0];
        String stampId = firstStamp.getModuleInfoBean().getStampId();
        if (stampId != null) {
            StampTreeNode srcNode = getSourceNode(stampId);
            if (srcNode != null) {
                // 一つ目は置換する
                replaceStamp(srcNode, firstStamp);
                // 編集元が一つで、新たに複数スタンプになった場合、引き続き残りを追加する
                if (newStamps.length > 1) {
                    target = srcNode;
                    newStamps = Arrays.copyOfRange(newStamps, 1, newStamps.length);
                } else {
                    return true;
                }
            }
        }
        
        if (target == null) {
            // 選択されていない場合はroot nodeをtargetにする
            target = (StampTreeNode) getModel().getRoot();
        }

        if (target.isLeaf()) {
            // Leafの場合はその下に追加
            StampTreeNode parent = (StampTreeNode) target.getParent();
            int childIndex = parent.getIndex(target) + 1;
            return addStamp(parent, newStamps, childIndex);
        } else {
            // Nodeの場合は最後に追加
            int childIndex = target.getChildCount();
            return addStamp(target, newStamps, childIndex);
        }
    }
    
    /**
     * Diagnosis Table から Drag & Drop されたRegisteredDiagnosisをスタンプ化する。
     */
    public boolean addDiagnosis(final StampTreeNode parent, final RegisteredDiagnosisModel[] rds, final int childIndex) {

        if (parent == null || rds == null || rds.length == 0) {
            return false;
        }
        
        final String message = "スタンプ保存";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (RegisteredDiagnosisModel rd : rds) {
            if (!first) {
                sb.append("、");
            } else {
                first = false;
            }
            sb.append(rd.getDiagnosis());
        }
        sb.append("を保存しています...");
        final String note = sb.toString();
        Component c = SwingUtilities.getWindowAncestor(this);
        
        ProgressMonitorWorker worker = new ProgressMonitorWorker<List<ModuleInfoBean>, Void>(c, message, note) {

            @Override
            protected List<ModuleInfoBean> doInBackground() throws Exception {
                logger.debug("addDiagnosis doInBackground");
                
                List<StampModel> stampList = new ArrayList<>();
                List<ModuleInfoBean> infoList = new ArrayList<>();
                for (RegisteredDiagnosisModel model : rds) {
                    
                    // データベースへ永続化するデータモデルを生成する
                    RegisteredDiagnosisModel rd = new RegisteredDiagnosisModel();
                    rd.setDiagnosis(model.getDiagnosis());
                    rd.setDiagnosisCode(model.getDiagnosisCode());
                    rd.setDiagnosisCodeSystem(model.getDiagnosisCodeSystem());

                    StampModel stamp = new StampModel();
                    String stampId = GUIDGenerator.generate(stamp);
                    stamp.setId(stampId);
                    stamp.setUserId(Project.getUserModel().getId());
                    stamp.setEntity(IInfoModel.ENTITY_DIAGNOSIS);
                    stamp.setStampBytes(getXMLBytes(rd));
                    stampList.add(stamp);

                    // Tree に加える 新しい StampInfo を生成する
                    ModuleInfoBean info = new ModuleInfoBean();
                    info.setStampId(stampId);                       // Stamp ID
                    info.setStampName(model.getDiagnosis());          // 傷病名
                    info.setEntity(IInfoModel.ENTITY_DIAGNOSIS);    // カテゴリ
                    info.setStampRole(IInfoModel.ENTITY_DIAGNOSIS); // Role

                    StringBuilder sb = new StringBuilder();
                    sb.append(model.getDiagnosis());
                    String cd = model.getDiagnosisCode();
                    if (cd != null) {
                        sb.append("(");
                        sb.append(cd);
                        sb.append(")"); // Tooltip
                    }
                    info.setStampMemo(sb.toString());
                    infoList.add(info);
                }
                
                StampDelegater sdl = StampDelegater.getInstance();
                sdl.putStamp(stampList);
                
                return infoList;
            }

            @Override
            protected void succeeded(List<ModuleInfoBean> list) {
                logger.debug("addDiagnosis succeeded");
                for (int i = list.size() - 1; i >= 0; --i) {
                    ModuleInfoBean info = list.get(i);
                    StampTreeNode node = new StampTreeNode(info);
                    DefaultTreeModel model = (DefaultTreeModel) getModel();
                    int childCount = parent.getChildCount();
                    int index = (childIndex >= 0 && childIndex < childCount)
                            ? childIndex : childCount;
                    model.insertNodeInto(node, parent, index);
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
        };

        worker.execute();
        return true;
    }

    /**
     * エディタで生成した病名リストを登録する。
     */
    public boolean addOrReplaceDiagnosis(RegisteredDiagnosisModel[] newRds) {
        
        if (newRds == null || newRds.length == 0) {
            return false;
        }
        
        StampTreeNode target = getSelectedNode();
        RegisteredDiagnosisModel firstRd = newRds[0];
        
        // 置換の場合
        String stampId = firstRd.getStampId();
        if (stampId != null) {
            StampTreeNode srcNode = getSourceNode(stampId);
            if (srcNode != null) {
                // 一つ目は置換する
                replaceRd(srcNode, firstRd);
                // 編集元が一つで、新たに複数スタンプになった場合。 実際はないはず
                if (newRds.length > 1) {
                    target = srcNode;
                    newRds = Arrays.copyOfRange(newRds, 1, newRds.length);
                } else {
                    return true;
                }
            }
        }

        if (target == null) {
            // 選択されていない場合はroot nodeをtargetにする
            target = (StampTreeNode) getModel().getRoot();
        }
        
        if (target.isLeaf()) {
            // Leafの場合はその下に追加
            StampTreeNode parent = (StampTreeNode) target.getParent();
            int childIndex = parent.getIndex(target) + 1;
            return addDiagnosis(parent, newRds, childIndex);
        } else {
            // Nodeの場合は最後に追加
            int childIndex = target.getChildCount();
            return addDiagnosis(target, newRds, childIndex);
        }
    }
    
    /**
     * テキストスタンプを追加する。
     */
    public boolean addTextStamp(final StampTreeNode parent, final String text, final int childIndex) {

        if (parent == null || text == null || text.isEmpty()) {
            return false;
        }
        
        int len = Math.min(16, text.length());
        int pos = text.indexOf("\n");
        if (pos != -1) {
            len = Math.min(len, pos);
        }
        final String message = "スタンプ保存";
        final String stampName = text.substring(0, len);
        final String note = stampName + "を保存しています...";
        Component c = SwingUtilities.getWindowAncestor(this);

        ProgressMonitorWorker worker = new ProgressMonitorWorker<ModuleInfoBean, Void>(c, message, note) {

            @Override
            protected ModuleInfoBean doInBackground() throws Exception {
                logger.debug("addTextStamp doInBackground");
                
                TextStampModel stamp = new TextStampModel();
                stamp.setText(text);

                // データベースへ Stamp のデータモデルを永続化する
                StampModel addStamp = new StampModel();
                String stampId = GUIDGenerator.generate(addStamp);
                addStamp.setId(stampId);
                addStamp.setUserId(Project.getUserModel().getId());
                addStamp.setEntity(IInfoModel.ENTITY_TEXT);
                addStamp.setStampBytes(getXMLBytes((IInfoModel) stamp));

                // Tree へ加える 新しい StampInfo を生成する
                ModuleInfoBean info = new ModuleInfoBean();
                info.setStampName(stampName);               //
                info.setEntity(IInfoModel.ENTITY_TEXT);     // カテゴリ
                info.setStampRole(IInfoModel.ENTITY_TEXT);  // Role
                info.setStampMemo(text);                    // Tooltip
                info.setStampId(stampId);                   // Stamp ID

                StampDelegater sdl = StampDelegater.getInstance();
                sdl.putStamp(addStamp);
                
                return info;
            }

            @Override
            protected void succeeded(ModuleInfoBean info) {
                logger.debug("addTextStamp succeeded");
                StampTreeNode node = new StampTreeNode(info);
                DefaultTreeModel model = (DefaultTreeModel) getModel();
                int childCount = parent.getChildCount();
                int index = (childIndex >= 0 && childIndex < childCount)
                        ? childIndex : childCount;
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
        };
        
        worker.execute();
        return true;
    }

    /**
     * テキストスタンプを追加する。
     */
    public boolean addOrReplaceTextStamp(ModuleModel[] newStamps) {
        
        if (newStamps == null || newStamps.length == 0) {
            return false;
        }
        
        StampTreeNode target = getSelectedNode();
        ModuleModel firstMm = newStamps[0];
        
        TextStampModel firstStamp = (TextStampModel) firstMm.getModel();
        String text = firstStamp.getText();
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 置換の場合
        String stampId = firstMm.getModuleInfoBean().getStampId();
        if (stampId != null) {
            StampTreeNode srcNode = getSourceNode(stampId);
            if (srcNode != null) {
                // 一つ目は置換する
                replaceStamp(srcNode, firstMm);
                return true;
            }
            // TextStampは複数スタンプなし
        }

        if (target == null) {
            // 選択されていない場合はroot nodeをtargetにする
            target = (StampTreeNode) getModel().getRoot();
        }
        
        if (target.isLeaf()) {
            // Leafの場合はその下に追加
            StampTreeNode parent = (StampTreeNode) target.getParent();
            int childIndex = parent.getIndex(target) + 1;
            return addTextStamp(parent, text, childIndex);
        } else {
            // Nodeの場合は最後に追加
            int childIndex = target.getChildCount();
            return addTextStamp(target, text, childIndex);
        }
    }
    
    // 編集元のスタンプのStampTreeNodeを取得する
    private StampTreeNode getSourceNode(String stampId) {
        
        if (stampId == null || stampId.isEmpty()) {
            return null;
        }
        
        StampTreeNode rootNode = (StampTreeNode) getModel().getRoot();
        Enumeration e = rootNode.preorderEnumeration();
        
        while (e.hasMoreElements()) {
            StampTreeNode node = (StampTreeNode) e.nextElement();
            if (node.isLeaf() && node.getUserObject() instanceof ModuleInfoBean) {
                ModuleInfoBean info = (ModuleInfoBean) node.getUserObject();
                if (stampId.equals(info.getStampId())) {
                    return node;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Stampを置き換える。
     *
     * @param stamp 置き換えるスタンプ
     */
    private void replaceStamp(StampTreeNode node, ModuleModel stamp) {

        if (node == null || !node.isLeaf() || stamp == null) {
            return;
        }
        
        // 置き換える Stamp の ModuleInfoを得る
        ModuleInfoBean info = stamp.getModuleInfoBean();
        // memoを更新
        info.setStampMemo(constractToolTip(stamp));
        // nodeを更新
        node.setUserObject(info);
        ((DefaultTreeModel) getModel()).nodeChanged(node);

        //-------------------------------------------------
        // データベースへ stampToReplcae のデータモデルを保存する
        // Entityを生成する
        //-------------------------------------------------
        final StampModel stampModel = new StampModel();
        stampModel.setId(info.getStampId());                        // stampId
        stampModel.setUserId(Project.getUserModel().getId());       // userId
        stampModel.setEntity(info.getEntity());                     // entity
        stampModel.setStampBytes(getXMLBytes(stamp.getModel()));    // XML
        
        SwingWorker worker = new SwingWorker(){

            @Override
            protected Object doInBackground() throws Exception {
                StampDelegater sdl = StampDelegater.getInstance();
                sdl.replaceStamp(stampModel);
                return null;
            }
        };
        worker.execute();
    }
    
    private void replaceRd(StampTreeNode node, RegisteredDiagnosisModel rd) {
        
        if (node == null || !node.isLeaf() || rd == null) {
            return;
        }
        
        // StampInfoを置換する
        ModuleInfoBean info = new ModuleInfoBean();
        info.setStampId(rd.getStampId());               // Stamp ID
        info.setStampName(rd.getDiagnosis());           // 傷病名
        info.setEntity(IInfoModel.ENTITY_DIAGNOSIS);    // カテゴリ
        info.setStampRole(IInfoModel.ENTITY_DIAGNOSIS); // Role

        StringBuilder sb = new StringBuilder();
        sb.append(rd.getDiagnosis());
        String cd = rd.getDiagnosisCode();
        if (cd != null) {
            sb.append("(");
            sb.append(cd);
            sb.append(")"); // Tooltip
        }
        info.setStampMemo(sb.toString());
        
        // nodeを更新
        node.setUserObject(info);
        ((DefaultTreeModel) getModel()).nodeChanged(node);

        // データベースへ永続化するデータモデルを生成する
        RegisteredDiagnosisModel model = new RegisteredDiagnosisModel();
        model.setDiagnosis(rd.getDiagnosis());
        model.setDiagnosisCode(rd.getDiagnosisCode());
        model.setDiagnosisCodeSystem(rd.getDiagnosisCodeSystem());

        final StampModel stampModel = new StampModel();
        stampModel.setId(rd.getStampId());                      // stampId
        stampModel.setUserId(Project.getUserModel().getId());   // userId
        stampModel.setEntity(IInfoModel.ENTITY_DIAGNOSIS);      // entity
        stampModel.setStampBytes(getXMLBytes(model));           // XML
        
        SwingWorker worker = new SwingWorker(){

            @Override
            protected Object doInBackground() throws Exception {
                StampDelegater sdl = StampDelegater.getInstance();
                sdl.replaceStamp(stampModel);
                return null;
            }
        };
        worker.execute();
    }
    
    /**
     * スタンプの情報を表示するための文字列を生成する。
     *
     * @param stamp 情報を生成するスタンプ
     * @return スタンプの情報文字列
     */
    protected String constractToolTip(ModuleModel stamp) {
        
        String ret = stamp.getModel().toString();
        if (ret.length() > TOOLTIP_LENGTH) {
            ret = ret.substring(0, TOOLTIP_LENGTH - 3).replace("\n", ",");
            ret += "...";
        } else {
            ret = ret.replace("\n", ",");
        }

        return ret;
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

        // 削除するノードを取得する
        // 右クリックで選択されている
        final StampTreeNode theNode = getSelectedNode();
        if (theNode == null) {
            return;
        }

        // このノードをルートにするサブツリーを前順走査する列挙を生成して返します。
        // 列挙の nextElement() メソッドによって返される最初のノードは、この削除するノードです。
        Enumeration e = theNode.preorderEnumeration();

        // このリストのなかに削除するノードとその子を含める
        final List<String> deleteList = new ArrayList<String>();

        // エディタから発行があるかどうかのフラグ
        boolean hasEditor = false;

        // 列挙する
        while (e.hasMoreElements()) {

            logger.debug("stampTree deleteNode e.hasMoreElements()");
            StampTreeNode node = (StampTreeNode) e.nextElement();

            if (node.isLeaf()) {

                ModuleInfoBean info = (ModuleInfoBean) node.getUserObject();
                String stampId = info.getStampId();
                
                // エディタから発行がある場合は中止する
                if (info.getStampName().equals("エディタから発行...") && (!info.isSerialized())) {
                    hasEditor = true;
                    break;
                }
                
                // IDが付いているもののみを加える
                if (stampId != null) {
                    deleteList.add(stampId);
                    logger.debug("added " + info.getStampName());
                }
            }
        }

        // エディタから発行が有った場合はダイアログを表示し
        // リターンする
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

        // 削除するフォルダが空の場合は削除してリターンする
        // リストのサイズがゼロかつ theNode が葉でない時
        if (deleteList.isEmpty() && (!theNode.isLeaf())) {
            DefaultTreeModel model = (DefaultTreeModel) (StampTree.this).getModel();
            model.removeNodeFromParent(theNode);
            return;
        }

        final String message = "スタンプ削除";
        final String note = "スタンプを削除しています...";
        Component c = SwingUtilities.getWindowAncestor(this);
        ProgressMonitorWorker worker = new ProgressMonitorWorker<Void, Void>(c, message, note) {

            @Override
            protected Void doInBackground() throws Exception {
                logger.debug("deleteNode doInBackground");
                StampDelegater sdl = StampDelegater.getInstance();
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
        };

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

        // 生成位置となる選択されたノードを得る
        StampTreeNode selected = getSelectedNode();

        if (selected != null && selected.isLeaf()) {
            // 選択位置のノードが葉の場合、その前に挿入する
            StampTreeNode newParent = (StampTreeNode) selected.getParent();
            int index = newParent.getIndex(selected);
            DefaultTreeModel model = (DefaultTreeModel) this.getModel();
            model.insertNodeInto(folder, newParent, index);

        } else if (selected != null && (!selected.isLeaf())) {
            // 選択位置のノードが子を持つ時、最後の子として挿入する
            DefaultTreeModel model = (DefaultTreeModel) this.getModel();
            model.insertNodeInto(folder, selected, selected.getChildCount());
        }
    }

    /**
     * コピー。
     */
    public void copy() {
        Action a = this.getActionMap().get(TransferHandler.getCopyAction().getValue(Action.NAME));
        if (a != null) {
            a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        }
    }

    /**
     * ペースト。
     */
    public void paste() {
        Action a = this.getActionMap().get(TransferHandler.getPasteAction().getValue(Action.NAME));
        if (a != null) {
            a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
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
        XMLEncoder e = new XMLEncoder(bo);
        e.writeObject(bean);
        e.close();
        return bo.toByteArray();
    }
    

    // Quaquaでなくてもストライプに
    // http://nadeausoftware.com/articles/2008/01/java_tip_how_add_zebra_background_stripes_jtree
    
    private static final Color DEFAULT_ODD_COLOR = ClientContext.getColor("color.odd");
    //private static final Color DEFAULT_EVEN_COLOR = ClientContext.getColor("color.even");
    private static final Color DEFAULT_EVEN_COLOR = ClientContext.getZebraColor();
    private static final Color[] ROW_COLORS = {DEFAULT_EVEN_COLOR, DEFAULT_ODD_COLOR};


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

            // http://nadeausoftware.com/articles/2008/01/java_tip_how_add_zebra_background_stripes_jtree
            if (c instanceof DefaultTreeCellRenderer) {
                ((DefaultTreeCellRenderer) c).setBackgroundNonSelectionColor(ROW_COLORS[row & 1]);
            } else {
                c.setBackground(ROW_COLORS[row & 1]);
            }
            return c;
        }
    }
    
    public StampBoxPlugin getStampBox() {
        return stampBox;
    }
}