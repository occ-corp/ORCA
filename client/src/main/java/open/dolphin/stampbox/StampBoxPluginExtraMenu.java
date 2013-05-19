package open.dolphin.stampbox;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import open.dolphin.client.BlockGlass;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.StampModel;
import open.dolphin.project.Project;
import open.dolphin.tr.StampTreeTransferHandler;

/**
 * StampBox の特別メニュー
 * @author pns
 * @author modified by masuda, Masuda Naika
 */
public class StampBoxPluginExtraMenu extends MouseAdapter {

    private JPopupMenu popup;
    private StampBoxPlugin context;
    private AbstractStampBox stampBox;


    public StampBoxPluginExtraMenu(StampBoxPlugin ctx) {
        super();
        context = ctx;
        stampBox = context.getUserStampBox();
//      stampBox = context.getCurrentBox();

        buildPopupMenu();
    }
    private BlockGlass getBlockGlass() {
        BlockGlass blockGlass = context.getBlockGlass();
        blockGlass.setSize(context.getFrame().getSize());
        return blockGlass;
    }

    @Override
    public void mousePressed(MouseEvent e) {
//masuda    SHIFTを押してクリックの場合のみpopupを表示する
        if (e.isShiftDown()) {
            popup.show((Component) e.getSource(), e.getX(), e.getY());
        }
    }

    /**
     * スタンプを xml ファイルに書き出す
     */
    private void exportUserStampBox() {

        // 保存する StampTree の XML データを生成する

//masuda^   blockGlassを入れたりSwingWorkerを入れたり・・・

//masuda    エクスポートデータ作成より前にファイル選択させる
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setDialogTitle("スタンプエクスポート");
        File current = fileChooser.getCurrentDirectory();
        fileChooser.setSelectedFile(new File(current.getPath(), "DolphinStamp.xml"));
        int selected = fileChooser.showSaveDialog(context.getFrame());

        if (selected == JFileChooser.APPROVE_OPTION) {
            final Path path = fileChooser.getSelectedFile().toPath();
            if (!Files.exists(path)|| overwriteConfirmed(path)) {
                
                BlockGlass blockGlass = getBlockGlass();
                blockGlass.setText("スタンプ箱をエクスポート中です。");
                blockGlass.block();
                
                SwingWorker worker = new SwingWorker<String, Void>() {

                    @Override
                    protected String doInBackground() throws Exception {
//masuda    stampBytesを含めたデータを書き出す
                        ExtendedStampTreeXmlBuilder builder = new ExtendedStampTreeXmlBuilder();
                        ExtendedStampTreeXmlDirector director = new ExtendedStampTreeXmlDirector(builder);
                        String ret = director.build(stampBox.getAllTrees());
                        return ret;
                    }

                    @Override
                    protected void done() {
                        try {
                            String xml = get();
                            Charset cs = Charset.forName("UTF-8");
                            Files.write(path, xml.getBytes(cs));
                        } catch (InterruptedException ex) {
                            processException(ex);
                        } catch (ExecutionException ex) {
                            processException(ex);
                        } catch (IOException ex) {
                            processException(ex);
                        }

                        BlockGlass blockGlass = getBlockGlass();
                        blockGlass.unblock();
                    }
                };
                worker.execute();
            }
        }
//masuda$
    }

    /**
     * ファイル上書き確認ダイアログを表示する。
     * @param file 上書き対象ファイル
     * @return 上書きOKが指示されたらtrue
     */
    private boolean overwriteConfirmed(Path path){
        String title = "上書き確認";
        String message = "既存のファイル " + path.getFileName().toString() + "\n"
                        +"を上書きしようとしています。続けますか？";

        int confirm = JOptionPane.showConfirmDialog(
            context.getFrame(), message, title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE );

        if(confirm == JOptionPane.OK_OPTION) {
            return true;
        }

        return false;
    }

    /**
     * xml ファイルから新しい userStampBox を作る
     */
    private void importUserStampBox() {

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setDialogTitle("スタンプインポート");
        File current = fileChooser.getCurrentDirectory();
        fileChooser.setSelectedFile(new File(current.getPath(), "DolphinStamp.xml"));
        int selected = fileChooser.showSaveDialog(context.getFrame());

        if (selected == JFileChooser.APPROVE_OPTION) {
            
            final Path path = fileChooser.getSelectedFile().toPath();
            BlockGlass blockGlass = getBlockGlass();
            blockGlass.setText("スタンプ箱インポート中です。");
            blockGlass.block();
            
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){

                @Override
                protected Void doInBackground() throws Exception {
                    
                    Charset cs = Charset.forName("UTF-8");
                    try (BufferedReader reader = Files.newBufferedReader(path, cs)) {
//masuda^   stampBytesを含めたデータを読み込む
                        ExtendedStampTreeDirector director = 
                                new ExtendedStampTreeDirector(new ExtendedStampTreeBuilder());
//masuda$
                        List<StampTree> userTrees = director.build(reader);
                        reader.close();

                        int currentTab = stampBox.getSelectedIndex();
                        StampTreeTransferHandler transferHandler = new StampTreeTransferHandler();
                        for (final StampTree stampTree : userTrees) {
                            // ORCA は無視
                            if (stampTree.getEntity().equals(IInfoModel.ENTITY_ORCA)) {
                                continue;
                            }
                            // 読み込んだ stampTree から StampTreePanel を作る
                            stampTree.setUserTree(true);
                            stampTree.setTransferHandler(transferHandler);
                            stampTree.setStampBox(context);
                            StampTreePanel treePanel = new StampTreePanel(stampTree);

                            // 作った StampTreePanel を該当する tab に replace
                            String treeName = stampTree.getTreeName();
                            int index = stampBox.indexOfTab(treeName);
                            stampBox.removeTabAt(index);
                            //stampBox.addTab(treeName, treePanel, index);
                            stampBox.add(treePanel, treeName, index);
                        }
                        stampBox.setSelectedIndex(currentTab);

                    } catch (IOException ex) {
                        processException(ex);
                    }

                    return null;
                }

                @Override
                protected void done() {
                    BlockGlass blockGlass = getBlockGlass();
                    blockGlass.unblock();
                }

            };
            worker.execute();

        }
    }

    private void buildPopupMenu() {
        popup = new JPopupMenu();
        JMenuItem item = new JMenuItem("スタンプをファイルに保存する");
        item.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                exportUserStampBox();
            }
        });
        popup.add(item);
        item = new JMenuItem("スタンプをファイルから読み込む");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                importUserStampBox();
            }
        });
        popup.add(item);
        popup.addSeparator();
        item = new JMenuItem("ゾンビ退治する");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    removeZombies();
                } catch (Exception ex) {
                    processException(ex);
                }
            }
        });
        popup.add(item);
    }
    
    private void removeZombies() throws Exception {
        
        BlockGlass blockGlass = getBlockGlass();
        blockGlass.setText("スタンプ箱のゾンビ退治中です。");
        blockGlass.block();
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            
            @Override
            protected String doInBackground() throws Exception {
                
                // 先にスタンプツリーのゾンビを退治する
                StringBuilder sb = new StringBuilder();
                sb.append("ツリーゾンビ退治\n");

                // ユーザーのすべてのスタンプをデータベースから取得しHashMapに登録する
                long userId = Project.getUserModel().getId();
                List<StampModel> allStamps = StampDelegater.getInstance().getAllStamps(userId);
                Map<String, StampModel> allStampMap = new HashMap<>();
                for (StampModel stamp : allStamps) {
                    allStampMap.put(stamp.getId(), stamp);
                }

                // ORCA以外のスタンプツリーを取得する
                int cnt = 0;
                List<StampTree> treeList = stampBox.getAllTreesExceptOrca();
                for (StampTree tree : treeList) {
                    // 各ツリーのゾンビを抹消する
                    TreeZombieBuster buster = new TreeZombieBuster(tree, allStampMap);
                    cnt += buster.removeZombie();

                }
                sb.append(cnt).append("件のゾンビを退治しました！\n");

                // 引き続きデータベースのゾンビを退治する
                sb.append("スタンプゾンビ退治\n");
                // 現在のORCA以外のスタンプツリーのModuleInfoBeanを取得する
                List<ModuleInfoBean> allInfos = getAllStampInfos();
                for (ModuleInfoBean info : allInfos) {
                    // HashMapから削除していく
                    allStampMap.remove(info.getStampId());
                }
                // 残ったものがデータベース側のゾンビである
                List<String> removeList = new ArrayList<>();
                removeList.addAll(allStampMap.keySet());

                // データベースから削除する
                //cnt = removeList.size();
                cnt = StampDelegater.getInstance().removeStamps(removeList);

                sb.append(cnt).append("件のゾンビを退治しました！\n");

                String msg = sb.toString();
                return msg;
            }

            @Override
            protected void done() {
                try {
                    BlockGlass blockGlass = getBlockGlass();
                    blockGlass.unblock();
                    String msg = get();
                    JOptionPane.showMessageDialog(null, msg, "ゾンビ退治", JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException ex) {
                    processException(ex);
                } catch (ExecutionException ex) {
                    processException(ex);
                }
            }
        };
        worker.execute();
    }

    private class TreeZombieBuster{
        
        private StampTree tree;
        private Map<String, StampModel> map;
        private int removeCount;
        
        private TreeZombieBuster(StampTree tree, Map<String, StampModel> map) {
            this.tree = tree;
            this.map = map;
        }
        
        private int removeZombie() {
            System.out.println(tree.getEntity() + "ツリーを処理中です。\n");
            DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
            StampTreeNode root = (StampTreeNode) treeModel.getRoot();
            removeTreeZombie(root);
            return removeCount;
        }
        
        private void removeTreeZombie(StampTreeNode node) {
            if (node.isLeaf() && node.getUserObject() instanceof ModuleInfoBean) {
                ModuleInfoBean info = (ModuleInfoBean) node.getUserObject();
                // エディタから発行のStampIdはnull
                String stampId = info.getStampId();
                if (stampId != null && !map.containsKey(info.getStampId())) {
                    node.removeFromParent();
                    StringBuilder sb = new StringBuilder();
                    sb.append("ゾンビ：").append(info.getStampName()).append(" ");
                    sb.append(info.getStampId()).append("\n");
                    System.out.println(sb.toString());
                    removeCount++;
                }
            }
            int cnt = node.getChildCount();
            for (int i = 0; i < cnt; ++i) {
                StampTreeNode child = (StampTreeNode) node.getChildAt(i);
                removeTreeZombie(child);
            }
        }
    }
    
    // ORCA以外のユーザースタンプのModuleInfoBeanを取得する
    private List<ModuleInfoBean> getAllStampInfos() {
        List<StampTree> treeList = stampBox.getAllTreesExceptOrca();
        List<ModuleInfoBean> infoList = new ArrayList<>();
        for (StampTree tree : treeList) {
            infoList.addAll(stampBox.getAllStamps(tree.getEntity()));
        }
        return infoList;
    }

    private void processException(Exception ex) {
        System.err.println("StampBoxPluginExtraMenu.java: " + ex);
    }
}