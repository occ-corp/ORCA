
package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableColumn;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.infomodel.UsingDrugModel;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * 採用薬を追加するためのパネル
 *
 * @author masuda, Masuda Naika
 */

public class UsingDrugPanel {

    private static final String[] COLUMN_NAMES = {"登録日", "薬剤名", "分割", "通常用量", "最大用量", "長期禁止"};
    private static final String[] METHOD_NAMES = {"getCreatedStr", "getName", "getAdmin", "getUsualDose", "getMaxDose", "getHasLimit"};
    private static final Class[] CLASSES = {String.class, String.class, String.class, String.class, String.class, Boolean.class};
    private static final int[] COLUMN_WIDTH = {60, 240, 10, 10, 10, 10};
    private static final int ADMIN_COL = 2;
    private static final int USUALDOSE_COL = 3;
    private static final int MAXDOSE_COL = 4;
    private static final int LIMIT_COL = 5;
    private static final int START_NUM_ROWS = 1;
    
    private static final ImageIcon cancelIcon = ClientContext.getImageIcon("cancl_16.gif");
    private static final ImageIcon removeIcon = ClientContext.getImageIcon("del_16.gif");
    private static final ImageIcon saveIcon = ClientContext.getImageIcon("save_16.gif");
    
    private ListTableModel tableModel;
    private JDialog dialog;
    private JPanel view;

    private List<UsingDrugModel> removedList;    // データベースから削除予定のモデル
    private List<UsingDrugModel> updatedList;     // 編集したモデル

    private JButton btn_cancel;
    private JButton btn_remove;
    private JButton btn_save;
    private JTable table;

    public UsingDrugPanel() {

        initComponents();

    }

    @SuppressWarnings("unchecked")
    public void enter() {

        removedList= new ArrayList<UsingDrugModel>();
        updatedList = new ArrayList<UsingDrugModel>();

        tableModel = new ListTableModel(COLUMN_NAMES, START_NUM_ROWS, METHOD_NAMES, CLASSES) {
            // 編集は不可

            @Override
            public boolean isCellEditable(int row, int col) {
                switch (col) {
                    case ADMIN_COL:
                    case USUALDOSE_COL:
                    case MAXDOSE_COL:
                    case LIMIT_COL:
                        if (row <= tableModel.getObjectCount() - 1){
                            return true;
                        } else {
                            return false;
                        }
                    default:
                        return false;
                }
            }

            @Override
            public void setValueAt(Object o, int row, int col) {

                UsingDrugModel model = (UsingDrugModel) tableModel.getObject(row);
                switch (col) {
                    case ADMIN_COL:
                        String str = (String) o;
                        model.setAdmin(str);
                        addUpdatedModel(model);
                        break;
                    case USUALDOSE_COL:
                        str = (String) o;
                        model.setUsualDose(str);
                        addUpdatedModel(model);
                        break;
                    case MAXDOSE_COL:
                        str = (String) o;
                        model.setMaxDose(str);
                        addUpdatedModel(model);
                        break;
                    case LIMIT_COL:
                        Boolean b = (Boolean) o;
                        b = b != null ? b : false;
                        model.setHasLimit(b);
                        addUpdatedModel(model);
                        break;
                }
            }
        };

        table.setModel(tableModel);
        // ストライプテーブル
        StripeTableCellRenderer renderer = new StripeTableCellRenderer(table);
        renderer.setDefaultRenderer();

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 列幅を設定する
        int len = COLUMN_NAMES.length;
        TableColumn column;
        for (int i = 0; i < len; i++) {
            column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(COLUMN_WIDTH[i]);
        }

        // セルエディタを設定
        JTextField tf = new JTextField();
        tf.addFocusListener(AutoRomanListener.getInstance());

        // 採用薬を取得、新規のArrayListを用意する。
        UsingDrugs.getInstance().loadUsingDrugs();
        List<UsingDrugModel>list = new ArrayList<UsingDrugModel>(UsingDrugs.getInstance().getUsingDrugModelList());
        if (list != null) {
            // テーブルにセット
            Collections.sort(list);
            tableModel.setDataProvider(list);
        }
        // ダイアログを表示
        showDialog();
    }

    private void showDialog(){

        dialog = new JDialog((Frame) null, true);
        ClientContext.setDolphinIcon(dialog);
        dialog.setContentPane(view);

        // dialogのタイトルを設定
        String title = ClientContext.getFrameTitle("採用薬編集");
        dialog.setTitle(title);
        dialog.pack();
        ComponentMemory cm = new ComponentMemory(dialog, new Point(100, 100), dialog.getPreferredSize(), UsingDrugPanel.this);
        cm.setToPreferenceBounds();
        dialog.setVisible(true);
    }

    private void close(){
        // dialogを閉じる
        dialog.setVisible(false);
        dialog.dispose();
    }

    private void saveClose() {
        // 削除リストをデータベースから削除
        UsingDrugs.getInstance().removeUsingDrugs(removedList);
        // 更新リストをデータベースで更新
        UsingDrugs.getInstance().updateUsingDrugs(updatedList);

        close();
    }

    private void addUpdatedModel(UsingDrugModel model){
        // 新規のものならupdatedListに追加しない（多分ない）
        if (model.getId() == 0) {
            return;
        }
        // updatedListに登録されていなかったら登録する
        boolean found = false;
        for (UsingDrugModel udm : updatedList){
            if (udm.getId() == model.getId()){
                found = true;
                break;
            }
        }
        if (!found){
            updatedList.add(model);
        }
    }

    private void removeSelectedModel() {
        // テーブルで選択中のモデルを削除リストに追加
        int[] rows = table.getSelectedRows();
        // 削除する行番号がずれないように後ろから処理していく
        for (int i = rows.length - 1; i >= 0; --i) {
            UsingDrugModel model = (UsingDrugModel) tableModel.getObject(rows[i]);
            if (model != null) {
                if (model.getId() != 0) {
                    // 新たに追加したものでないならデータベースから削除
                    removedList.add(model);
                }
                tableModel.deleteAt(rows[i]);
                if (updatedList.contains(model)) {
                    updatedList.remove(model);
                }
            }
        }
        table.repaint();
    }

    private void initComponents() {

        btn_remove = new JButton("削除", removeIcon);
        btn_cancel = new JButton("取消", cancelIcon);
        btn_save = new JButton("保存", saveIcon);

        table = new JTable();
        JScrollPane scroll = new JScrollPane(table);

        view = new JPanel();
        view.setLayout(new BorderLayout());
        view.add(scroll, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(btn_remove);
        panel.add(Box.createHorizontalGlue());
        panel.add(btn_cancel);
        panel.add(btn_save);
        view.add(panel, BorderLayout.SOUTH);

        btn_cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        btn_remove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedModel();
            }
        });
        btn_save.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                saveClose();
            }
        });
    }
}
