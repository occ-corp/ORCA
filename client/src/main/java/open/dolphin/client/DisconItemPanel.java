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
import open.dolphin.infomodel.DisconItemModel;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;
import open.dolphin.util.MMLDate;

/**
 * 中止項目を編集するパネル
 *
 * @author masuda, Masuda Naika
 */
public class DisconItemPanel {

    private static final String[] COLUMN_NAMES = {"日付", "名称", "メモ"};
    private static final String[] METHOD_NAMES = {"getDate", "getItemName", "getMemo"};
    private static final int[] COLUMN_WIDTH = {50, 170, 100};
    private static final int START_NUM_ROWS = 1;
    private static final int DATE_COL = 0;
    private static final int NAME_COL = 1;
    private static final int MEMO_COL = 2;
    
    private static final ImageIcon addIcon = ClientContext.getImageIcon("add_16.gif");
    private static final ImageIcon cancelIcon = ClientContext.getImageIcon("cancl_16.gif");
    private static final ImageIcon removeIcon = ClientContext.getImageIcon("del_16.gif");
    private static final ImageIcon saveIcon = ClientContext.getImageIcon("save_16.gif");
    
    private ListTableModel tableModel;
    private List<DisconItemModel> addedList = new ArrayList<DisconItemModel>();
    private List<DisconItemModel> removedList = new ArrayList<DisconItemModel>();
    private List<DisconItemModel> updatedList = new ArrayList<DisconItemModel>();     // 編集したモデル
    private JDialog dialog;
    private JPanel view;
    private JButton btn_add;
    private JButton btn_cancel;
    private JButton btn_remove;
    private JButton btn_save;
    private JTable table;

    public DisconItemPanel() {

        initComponents();
    }

    @SuppressWarnings("unchecked")
    public void enter() {

        tableModel = new ListTableModel(COLUMN_NAMES, START_NUM_ROWS, METHOD_NAMES, null) {

            @Override
            public boolean isCellEditable(int row, int col) {
                switch (col) {
                    case DATE_COL:
                    case NAME_COL:
                    case MEMO_COL:
                        if (row <= tableModel.getObjectCount() - 1) {
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
                if (o == null || ((String) o).trim().equals("")) {
                    return;
                }
                DisconItemModel model = (DisconItemModel) tableModel.getObject(row);
                String str = (String) o;
                str = str.replace(".", "-");
                str = str.replace("/", "-");

                switch (col) {
                    case DATE_COL:
                        model.setDate(str);
                        addUpdatedModel(model);
                        break;
                    case NAME_COL:
                        model.setItemName(str);
                        addUpdatedModel(model);
                        break;
                    case MEMO_COL:
                        model.setMemo(str);
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

        // 中止項目を取得。新規のArrayListを用意する
        List<DisconItemModel> list = DisconItems.getInstance().getDisconItemList();

        if (list != null) {
            // テーブルにセット
            Collections.sort(list);
            tableModel.setDataProvider(list);
        }
        showDialog();
    }

    private void showDialog() {

        dialog = new JDialog((Frame) null, true);
//masuda^    アイコン設定
        ClientContext.setDolphinIcon(dialog);
//masuda$
        dialog.setModal(true);
        dialog.setContentPane(view);

        // dialogのタイトルを設定
        String title = ClientContext.getFrameTitle("中止項目編集");
        dialog.setTitle(title);
        dialog.pack();
        ComponentMemory cm = new ComponentMemory(dialog, new Point(100, 100), dialog.getPreferredSize(), DisconItemPanel.this);
        cm.setToPreferenceBounds();
        dialog.setVisible(true);
    }

    private void close() {
        // dialogを閉じる
        dialog.setVisible(false);
        dialog.dispose();
    }

    private void saveClose() {
        DisconItems.getInstance().removeDisconItems(removedList);
        DisconItems.getInstance().updateDisconItems(updatedList);
        DisconItems.getInstance().addDisconItems(addedList);
        // DisconItemsを更新しておく
        DisconItems.getInstance().loadDisconItems();
        close();
    }

    @SuppressWarnings("unchecked")
    private void addNewModel() {
        DisconItemSubPanel panel = new DisconItemSubPanel(dialog);
        panel.enter();
        DisconItemModel model = panel.getModel();
        panel = null;
        tableModel.addObject(model);
        addedList.add(model);
    }

    private void addUpdatedModel(DisconItemModel model) {
        if (!updatedList.contains(model) && model.getId() != 0) {
            // 新たに追加したものでないなら更新リストに追加
            updatedList.add(model);
        }
    }

    private void removeSelectedModel() {
        // テーブルで選択中のモデルを削除リストに追加
        int[] rows = table.getSelectedRows();
        // 削除する行番号がずれないように後ろから処理していく
        for (int i = rows.length - 1; i >= 0; --i) {
            DisconItemModel model = (DisconItemModel) tableModel.getObject(rows[i]);
            if (model != null) {
                if (model.getId() != 0) {
                    // 新たに追加したものでないならデータベースから削除
                    removedList.add(model);
                }
                tableModel.deleteAt(rows[i]);
                if (updatedList.contains(model) && model.getId() != 0) {
                    // 新たに追加したものでなくて更新リストに入っているのはリストから削除する
                    updatedList.remove(model);
                }
                if (addedList.contains(model)) {
                    addedList.remove(model);
                }
            }
        }
        table.repaint();
    }

    private void initComponents() {


        table = new JTable();
        JScrollPane scroll = new JScrollPane(table);

        btn_add = new JButton("追加", addIcon);
        btn_remove = new JButton("削除", removeIcon);
        btn_cancel = new JButton("取消", cancelIcon);
        btn_save = new JButton("保存", saveIcon);

        view = new JPanel();
        view.setLayout(new BorderLayout());

        view.add(scroll, BorderLayout.CENTER);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(btn_add);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(btn_remove);
        panel.add(Box.createHorizontalGlue());
        panel.add(btn_cancel);
        panel.add(btn_save);
        view.add(panel, BorderLayout.SOUTH);

        btn_add.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addNewModel();
            }
        });
        btn_save.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                saveClose();
            }
        });
        btn_remove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedModel();
            }
        });
        btn_cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    private static final class DisconItemSubPanel {

        private JButton btn_subAdd;
        private JButton btn_subCancel;
        private JLabel lbl_date;
        private JLabel lbl_itemName;
        private JLabel lbl_memo;
        private JTextField tf_date;
        private JTextField tf_itemName;
        private JTextField tf_memo;
        private JDialog dialog_sub;
        private JPanel view_sub;
        private JDialog parent;
        private DisconItemModel newModel;

        private DisconItemSubPanel(JDialog parent) {
            initComponents();
            this.parent = parent;
        }

        private void initComponents() {

            btn_subAdd = new JButton("追加");
            btn_subCancel = new JButton("取消");
            lbl_date = new JLabel("日付");
            lbl_itemName = new JLabel("項目");
            lbl_memo = new JLabel("メモ");
            tf_date = new JTextField(10);
            tf_itemName = new JTextField(20);
            tf_memo = new JTextField(10);

            view_sub = new JPanel();
            view_sub.setLayout(new BorderLayout());
            JPanel south = new JPanel();
            south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
            south.add(Box.createHorizontalGlue());
            south.add(btn_subCancel);
            south.add(btn_subAdd);
            view_sub.add(south, BorderLayout.SOUTH);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            JPanel sub = new JPanel();
            sub.setLayout(new BoxLayout(sub, BoxLayout.Y_AXIS));
            lbl_date.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            tf_date.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            sub.add(lbl_date);
            sub.add(tf_date);
            panel.add(sub);
            sub = new JPanel();
            sub.setLayout(new BoxLayout(sub, BoxLayout.Y_AXIS));
            lbl_itemName.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            tf_itemName.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            sub.add(lbl_itemName);
            sub.add(tf_itemName);
            panel.add(sub);
            sub = new JPanel();
            sub.setLayout(new BoxLayout(sub, BoxLayout.Y_AXIS));
            lbl_memo.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            tf_memo.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            sub.add(lbl_memo);
            sub.add(tf_memo);
            panel.add(sub);
            view_sub.add(panel, BorderLayout.CENTER);

            btn_subAdd.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String itemName = tf_itemName.getText();
                    if (itemName != null) {
                        newModel = new DisconItemModel();
                        String str = tf_date.getText();
                        str = str.replace(".", "-");
                        str = str.replace("/", "-");
                        newModel.setDate(str);
                        newModel.setItemName(itemName);
                        newModel.setMemo(tf_memo.getText());
                    }
                    closeSubPanel();
                }
            });
            btn_subCancel.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    closeSubPanel();
                }
            });

        }

        private void showDialog() {

            dialog_sub = new JDialog(parent, true);
//masuda^    アイコン設定
            ClientContext.setDolphinIcon(dialog_sub);
//masuda$
            
            dialog_sub.add(view_sub);
            // dialogのタイトルを設定
            String title = ClientContext.getFrameTitle("中止項目追加");
            dialog_sub.setTitle(title);
            dialog_sub.pack();
            dialog_sub.setLocationRelativeTo(parent);
            dialog_sub.setVisible(true);
        }

        private void enter() {
            tf_date.setText(MMLDate.getDate());
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    tf_itemName.requestFocusInWindow();
                }
            });

            showDialog();

        }

        private DisconItemModel getModel() {
            return newModel;
        }

        private void closeSubPanel () {
            dialog_sub.setVisible(false);
        }
    }
}
