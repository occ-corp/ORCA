package open.dolphin.stampbox;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import open.dolphin.client.BlockGlass;
import open.dolphin.infomodel.IInfoModel;
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

            final File file = fileChooser.getSelectedFile();
            if (!file.exists() || overwriteConfirmed(file)) {

                SwingWorker worker = new SwingWorker<String, Void>() {

                    @Override
                    protected String doInBackground() throws Exception {
//masuda    stampBytesを含めたデータを書き出す
                        ExtendedStampTreeXmlBuilder builder = new ExtendedStampTreeXmlBuilder();
                        ExtendedStampTreeXmlDirector director = new ExtendedStampTreeXmlDirector(builder);
                        BlockGlass blockGlass = getBlockGlass();
                        blockGlass.setText("スタンプ箱をエクスポート中です。");
                        blockGlass.block();
                        ArrayList<StampTree> publishList = new ArrayList<StampTree>(IInfoModel.STAMP_ENTITIES.length);
                        publishList.addAll(stampBox.getAllTrees());
                        String ret = director.build(publishList);
                        return ret;
                    }

                    @Override
                    protected void done() {
                        String xml = null;
                        FileOutputStream fos = null;
                        OutputStreamWriter writer = null;

                        try {
                            xml = get();
                            fos = new FileOutputStream(file);
                            writer = new OutputStreamWriter(fos, "UTF-8");
                            // 書き出す内容
                            writer.write(xml);
                        } catch (InterruptedException ex) {
                            processException(ex);
                        } catch (ExecutionException ex) {
                            processException(ex);
                        } catch (FileNotFoundException ex) {
                            processException(ex);
                        } catch (UnsupportedEncodingException ex) {
                            processException(ex);
                        } catch (IOException ex) {
                            processException(ex);
                        } finally {
                            try {
                                writer.close();
                                fos.close();
                            } catch (IOException ex) {
                                processException(ex);
                            } catch (NullPointerException ex) {
                                processException(ex);
                            }
                        }
                        BlockGlass blockGlass = getBlockGlass();
                        blockGlass.unblock();
                    }

                    private void processException(Exception ex){
                        System.out.println("StampBoxPluginExtraMenu.java: " + ex);
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
    private boolean overwriteConfirmed(File file){
        String title = "上書き確認";
        String message = "既存のファイル " + file.toString() + "\n"
                        +"を上書きしようとしています。続けますか？";

        int confirm = JOptionPane.showConfirmDialog(
        //int confirm = MyJSheet.showConfirmDialog(
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
            final File file = fileChooser.getSelectedFile();

            SwingWorker worker = new SwingWorker(){

                @Override
                protected Object doInBackground() throws Exception {
                    BlockGlass blockGlass = getBlockGlass();
                    blockGlass.setText("スタンプ箱インポート中です。");
                    blockGlass.block();
                    try {
                        // xml ファイルから StampTree 作成
                        FileInputStream in = new FileInputStream(file);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//masuda^   stampBytesを含めたデータを読み込む
                        ExtendedStampTreeDirector director
                                = new ExtendedStampTreeDirector(new ExtendedStampTreeBuilder());
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

                    } catch (FileNotFoundException ex) {
                        processException(ex);
                    } catch (UnsupportedEncodingException ex) {
                        processException(ex);
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

                private void processException(Exception ex) {
                    System.out.println("StampBoxPluginExtraMenu.java: " + ex);
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
    }
}