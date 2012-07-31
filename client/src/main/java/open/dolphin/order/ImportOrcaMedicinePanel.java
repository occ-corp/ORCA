
package open.dolphin.order;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.TableColumn;
import open.dolphin.client.BlockGlass;
import open.dolphin.client.ClientContext;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * Orcaの処方を参照するパネル
 *
 * @author masuda, Masuda Naika
 */

public class ImportOrcaMedicinePanel {

    private static final int period = -6;  // うんヶ月前から
    private String patientId;
    private boolean importFlag = false;
    private static final String[] COLUMN_NAMES = {"診療内容", "数量", "単位", " ", "回数"};
    private static final String[] METHOD_NAMES = {"getName", "getNumber", "getUnit", "getDummy", "getBundleNumber"};
    private static final int[] COLUMN_WIDTH = {240, 10, 10, 10, 10};
    private static final int ROWS = 1;
    private ListTableModel<MasterItem> tableModel;
    private static final String label = "ORCAの過去うんヶ月の処方を参照します。";
    private BlockGlass blockGlass;

    private JButton btn_Cancel;
    private JButton btn_Copy;
    private JButton btn_SelectAll;
    private JComboBox cb_YMD;
    private JLabel lbl_Name;
    private JLabel lbl_Copyright;
    private JTable tbl_MasterItem;
    private JPanel view;
    private JDialog dialog;

    public ImportOrcaMedicinePanel() {
        initComponents();
    }

    public void enter(String ptid) {

        // 開始
        patientId = ptid;
        getOrcaVisit();
        showDialog();
    }

    public List<MasterItem> getMasterItemList() {
        // tableで選択されているもののみ返す
        List<MasterItem> ret = new ArrayList<MasterItem>();
        int[] selected = tbl_MasterItem.getSelectedRows();
        int rowCount = tableModel.getObjectCount();
        List<MasterItem> lst = tableModel.getDataProvider();
        for (int i : selected) {
            if (i < rowCount) {
                ret.add(lst.get(i));
            }
        }
        return ret;
    }

    public boolean getImportFlag() {
        return importFlag;
    }

    private void showDialog() {

        dialog = new JDialog((Frame) null, true);
        ClientContext.setDolphinIcon(dialog);
        dialog.setContentPane(view);
        dialog.pack();

        blockGlass = new BlockGlass();
        dialog.setGlassPane(blockGlass);
        blockGlass.setSize(dialog.getSize());

        // dialogのタイトルを設定
        String title = ClientContext.getFrameTitle("ORCA処方参照");
        dialog.setTitle(title);
        ComponentMemory cm = new ComponentMemory(dialog, new Point(100, 100), dialog.getPreferredSize(), ImportOrcaMedicinePanel.this);
        cm.setToPreferenceBounds();

        dialog.setVisible(true);
    }


    private void updateTable(final String ymd) {
        // 処方内容テーブルを更新する
        final SqlMiscDao dao = SqlMiscDao.getInstance();

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                blockGlass.block();
                return dao.getMedMasterItemFromOrca(patientId, ymd);

            }

            @SuppressWarnings("unchecked")
            @Override
            protected void done() {
                try {
                    List<MasterItem> result = (List<MasterItem>) get();
                    tableModel.setDataProvider(result);
                    tbl_MasterItem.repaint();
                    blockGlass.unblock();
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                }

            }
        };
        worker.execute();
    }

    private void getOrcaVisit() {
        // ORCAに記録されている受診日を取得する
        GregorianCalendar cal = new GregorianCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        // 検索終了日は今
        final String endDate = sdf.format(cal.getTime());
        // 検索開始日は設定ヶ月前
        cal.add(GregorianCalendar.MONTH, period);
        final String startDate = sdf.format(cal.getTime());
        // ORCAに記録されている受診日を取得
        final SqlMiscDao dao = SqlMiscDao.getInstance();
        final boolean descFlag = true;
        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                return  dao.getOrcaVisit(patientId, startDate, endDate, descFlag, "medOrder");
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    List<String> result = (List<String>) get();
                    // combo boxに登録
                    cb_YMD.removeAllItems();
                    for (String item : result) {
                        cb_YMD.addItem(item);
                    }
                    if (cb_YMD.getItemCount() > 0) {
                        cb_YMD.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                }
            }
        };
        worker.execute();
    }

    private void close() {
        dialog.setVisible(false);
        dialog.dispose();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        view = new JPanel();
        view.setLayout(new BorderLayout());

        tbl_MasterItem = new JTable();
        JScrollPane scroll = new JScrollPane(tbl_MasterItem);

        btn_Copy = new JButton("取込");
        btn_Cancel = new JButton("取消");
        btn_SelectAll = new JButton("全選択");
        cb_YMD = new JComboBox();
        cb_YMD.setMaximumSize(new Dimension(150, 40));  // てきとー
        lbl_Name = new JLabel(label.replace("うん", String.valueOf(-period)));
        lbl_Copyright = new JLabel("Masuda Naika, Wakayama City");


        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(Box.createVerticalStrut(10));
        panel.add(lbl_Name);
        panel.add(Box.createHorizontalGlue());
        panel.add(lbl_Copyright);
        north.add(panel);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(cb_YMD);
        panel.add(Box.createHorizontalGlue());
        panel.add(btn_SelectAll);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(btn_Cancel);
        panel.add(btn_Copy);
        north.add(panel);
        north.add(Box.createVerticalStrut(10));
        view.add(north, BorderLayout.NORTH);
        view.add(scroll, BorderLayout.CENTER);

        tableModel = new ListTableModel(COLUMN_NAMES, ROWS, METHOD_NAMES, null) {
            // 編集は不可

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tbl_MasterItem.setModel(tableModel);
        // ストライプテーブル
        StripeTableCellRenderer renderer = new StripeTableCellRenderer(tbl_MasterItem);
        renderer.setDefaultRenderer();

        tbl_MasterItem.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // 列幅を設定する
        int len = COLUMN_NAMES.length;
        TableColumn column;
        for (int i = 0; i < len; i++) {
            column = tbl_MasterItem.getColumnModel().getColumn(i);
            column.setPreferredWidth(COLUMN_WIDTH[i]);
        }

        cb_YMD.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String ymd = (String) cb_YMD.getSelectedItem();
                    if (ymd != null) {
                        updateTable(ymd);
                    }
                }
            }
        });
        btn_Copy.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                importFlag = true;
                close();
            }
        });
        btn_Cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        btn_SelectAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int objectCount = tableModel.getObjectCount();
                if (objectCount != 0) {
                    tbl_MasterItem.setRowSelectionInterval(0, objectCount - 1);
                }
            }
        });
    }
}
