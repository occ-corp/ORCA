
package open.dolphin.impl.routinemed;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.*;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.infomodel.RoutineMedModel;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * 各患者の定期処方などを管理する
 * 
 * @author masuda, Masuda Naika
 */
public class RoutineMedImpl extends AbstractChartDocument {
    
    private static final String TITLE = "薬歴";
    private final String[] COLUMN_NAMES 
            = new String[]{"登録日","しおり", "メモ"};
    private static final String[] PROPERTY_NAMES 
            = new String[]{"getRegistDateStr", "getBookmark","getMemo"};
    private static final Class[] COLUMN_CLASSES = 
            new Class[]{String.class, Boolean.class, String.class};
    private static final int[] COLUMN_WIDTH = 
            new int[]{100, 40, Integer.MAX_VALUE};
    
    private static final int START_NUM_ROWS = 1;
    private static final int BOOKMARK_COLUMN = 1;
    private static final int MEMO_COLUMN = 2;

    private static final ImageIcon deleteIcon = ClientContext.getImageIcon("os_delete_16.png");
    private static final ImageIcon saveIcon = ClientContext.getImageIcon("os_save_16.png");
    private static final ImageIcon updateIcon = ClientContext.getImageIcon("os_refresh_16.png");
    
    // 削除カラー
    private static final Color DELETE_COLOR = new Color(128, 128, 128);

    // 編集フラグ
    private static final String DELETE = "delete";
    private static final String EDITED = "edited";
    
    private JPanel panel;
    private JComboBox periodCombo;
    private JButton deleteBtn;
    private JButton updateBtn;
    private JButton saveBtn;
    private JTable table;
    private JPanel medPanel;
    private JScrollPane scrlMedPanel;
    private ListTableModel<RoutineMedModel> tableModel;
    private List<RoutineMedPanel> panelList;
    
    // undo関連
    private Deque undoQue;      // undo用のdeque
    private Deque redoQue;      // redo用のdeque
    private Action undoAction;  // undoAction
    private Action redoAction;  // redoAction
    private Action saveAction;  // saveAction
    private Action deleteAction;
    private Action updateAction;
    
    private ListTableSorter sorter;
    
    public RoutineMedImpl() {
        setTitle(TITLE);
        panelList = new ArrayList<RoutineMedPanel>();
    }
    
    @Override
    public void start() {
        initComponents();
        initialize();
        // ハマリ：enterしないとundoなど動かないぞ。
        enter();
        update();
    }

    @Override
    public void stop() {
        
    }
    
    @Override
    public void enter() {
        // ハマリ：enterしないとundoなど動かないぞ。
        super.enter();
        controlButton();
    }
    
    private void initialize() {
        
        // undo & redoアクションの設定
        undoAction = getContext().getChartMediator().getAction(GUIConst.ACTION_UNDO);
        undoAction.setEnabled(false);
        redoAction = getContext().getChartMediator().getAction(GUIConst.ACTION_REDO);
        redoAction.setEnabled(false);
        undoQue = new LinkedList<RoutineMedModel>();
        redoQue = new LinkedList<RoutineMedModel>();
    }
    
    private void initComponents() {
        
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // 操作パネル
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
        JLabel periodLbl = new JLabel("表示数: ");
        btnPanel.add(periodLbl);
        periodCombo = new JComboBox();
        periodCombo.addItem("１－５件");
        periodCombo.addItem("５－１０件");
        periodCombo.addItem("全部");
        periodCombo.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    update();
                }
            }
        });
        Dimension d = periodCombo.getPreferredSize();
        periodCombo.setMaximumSize(d);
        btnPanel.add(periodCombo);
        btnPanel.add(Box.createHorizontalGlue());
        
        deleteAction = new AbstractAction("削除", deleteIcon) {

            @Override
            public void actionPerformed(ActionEvent e) {
                delete();
            }
            
        };
        deleteBtn = new JButton(deleteAction);
        btnPanel.add(deleteBtn);
        
        updateAction = new AbstractAction("更新", updateIcon) {

            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
            
        };
        updateBtn = new JButton(updateAction);
        btnPanel.add(updateBtn);
        
        saveAction = new AbstractAction("保存", saveIcon) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                save();
            }
        };
        saveAction.setEnabled(false);
        saveBtn = new JButton(saveAction);
        btnPanel.add(saveBtn);
        
        int h = btnPanel.getPreferredSize().height;
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        panel.add(btnPanel);
        
        // 表
        tableModel = new ListTableModel<RoutineMedModel>(COLUMN_NAMES, START_NUM_ROWS, PROPERTY_NAMES, COLUMN_CLASSES);
        sorter = new ListTableSorter(tableModel);
        table = new JTable(sorter) {

            @Override
            public boolean isCellEditable(int row, int column) {
                
                if (isReadOnly()) {
                    return false;
                }
                if (column != MEMO_COLUMN && column != BOOKMARK_COLUMN) {
                    return false;
                }
                
                RoutineMedModel model = (RoutineMedModel) sorter.getObject(row);
                if (model == null) {
                    return false;
                }
                if (DELETE.equals(model.getStatus())) {
                    return false;
                }
                return true;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {

                int[] selectedRows = table.getSelectedRows();
                for (int selectedRow : selectedRows) {

                    RoutineMedModel oldMed = (RoutineMedModel) sorter.getObject(selectedRow);
                    if (oldMed == null) {
                        return;
                    }

                    RoutineMedModel newMed = oldMed.clone();
                    switch (column) {
                        case MEMO_COLUMN:
                            String memo = (String) aValue;
                            newMed.setMemo(memo);
                            newMed.setStatus(EDITED);
                            offerUndoQue(oldMed, newMed);
                            break;
                        case BOOKMARK_COLUMN:
                            boolean b = (aValue == null)
                                    ? false
                                    : (Boolean) aValue;
                            newMed.setBookmark(b);
                            newMed.setStatus(EDITED);
                            offerUndoQue(oldMed, newMed);
                            break;
                    }
                }
            }
        };
        
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);
        ListSelectionModel lm = table.getSelectionModel();
        lm.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    tableRowSelectionChanged();
                }
            }
        });
        
        JTextField tf = new JTextField();
        tf.addFocusListener(AutoKanjiListener.getInstance());
        DefaultCellEditor de = new DefaultCellEditor2(tf);
        de.setClickCountToStart(2);
        table.getColumnModel().getColumn(MEMO_COLUMN).setCellEditor(de);
        
        sorter.setTableHeader(table.getTableHeader());
        // カラム幅設定
        for (int i = 0; i < COLUMN_WIDTH.length; i++) {
            table.getColumnModel().getColumn(i).setMaxWidth(COLUMN_WIDTH[i]);
        }
        RoutineMedRenderer renderer = new RoutineMedRenderer();
        renderer.setTable(table);
        renderer.setDefaultRenderer();
        JScrollPane scroll = new JScrollPane(table);
        d = new Dimension(Integer.MAX_VALUE, 100);
        scroll.setPreferredSize(d);
        panel.add(scroll);
        
        // 処方
        medPanel = new JPanel();
        medPanel.setLayout(new BoxLayout(medPanel, BoxLayout.X_AXIS));
        scrlMedPanel = new JScrollPane(medPanel);
        d = new Dimension(Integer.MAX_VALUE, 400);
        scrlMedPanel.setPreferredSize(d);
        panel.add(scrlMedPanel);

        // UIに登録
        setUI(panel);
    }
    
    @Override
    public void save() {
        SimpleWorker worker = new SimpleWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                MasudaDelegater del = MasudaDelegater.getInstance();
                for (RoutineMedModel model : tableModel.getDataProvider()) {
                    String status = model.getStatus();
                    if (DELETE.equals(status)) {
                        del.removeRoutineMedModel(model);
                    } else if (EDITED.equals(status)) {
                        del.updateRoutineMedModel(model);
                    }
                }
                return null;
            }

            @Override
            protected void succeeded(Void result) {
                update();
            }
        };
        worker.execute();
    }
    
    private void update() {
        try {
            MasudaDelegater del = MasudaDelegater.getInstance();
            long karteId = getContext().getKarte().getId();
            int index = periodCombo.getSelectedIndex();
            int[] results = getFirstAndMaxResult(index);
            List<RoutineMedModel> list = del.getRoutineMedModels(karteId, results[0], results[1]);
            tableModel.setDataProvider(list);
            updateMedPanel(list);

            undoQue.clear();
            redoQue.clear();
            table.clearSelection();
            controlButton();
        } catch (Exception ex) {
        }
    }
    
    private void updateMedPanel(List<RoutineMedModel> list) {

        panelList.clear();
        medPanel.removeAll();

        if (list != null) {
            for (RoutineMedModel model : list) {
                RoutineMedPanel rmp = new RoutineMedPanel();
                rmp.setContext(getContext());
                rmp.setRoutineMedModel(model);
                rmp.render();
                medPanel.add(rmp);
                panelList.add(rmp);
            }
        }
        medPanel.repaint();
    }
    
    private void delete() {
        
        int[] selectedRows = table.getSelectedRows();
        for (int selectedRow : selectedRows) {
            RoutineMedModel oldMed = (RoutineMedModel) sorter.getObject(selectedRow);
            RoutineMedModel newMed = oldMed.clone();
            newMed.setStatus(DELETE);
            offerUndoQue(oldMed, newMed);
        }
        tableModel.fireTableDataChanged();
    }
    
    private void tableRowSelectionChanged() {

        // 選択したモデルに選択ボーダーをセットしviewportを移動する
        boolean first = true;
        Point p = null;
        int[] selectedRows = table.getSelectedRows();
        
        for (RoutineMedPanel rmp : panelList) {
            rmp.setSelected(false);
        }

        for (int row : selectedRows) {
            int index = sorter.modelIndex(row);
            RoutineMedPanel rmp = panelList.get(index);
            rmp.setSelected(true);
            if (first) {
                p = rmp.getLocation();
            }
        }

        if (p != null) {
            scrlMedPanel.getViewport().setViewPosition(p);
        }

        // 削除ボタンをコントロールする
        boolean flag = true;
        if (isReadOnly()) {
            flag = false;
        } else {
            // 選択された行のオブジェクトを得る
            
            for (int row : selectedRows) {
                if (row == -1) {
                    continue;
                }
                RoutineMedModel model = (RoutineMedModel) sorter.getObject(row);
                // ヌルの場合
                if (model == null) {
                    flag = false;
                    break;
                }
                String status = model.getStatus();
                if (status == null || EDITED.equals(status)) {
                    continue;
                } else {
                    flag = false;
                    break;
                }
            }
        }
        deleteAction.setEnabled(flag);
    }

    /**
     * undo, 保存ボタンのコントロール
     */
    private void controlButton(){

        if (undoQue != null && !undoQue.isEmpty()){
            undoAction.setEnabled(true);
            saveAction.setEnabled(true);
            // undoができる状態ならdirtyのはず
            setDirty(true);
        } else {
            undoAction.setEnabled(false);
            saveAction.setEnabled(false);
            // undoができない状態ならnot dirtyのはず
            setDirty(false);
        }
        if (redoQue != null && !redoQue.isEmpty()){
            redoAction.setEnabled(true);
        } else {
            redoAction.setEnabled(false);
        }
    }
    
    
    private int[] getFirstAndMaxResult(int index) {
        
        int[] ret = null;
        switch(index) {
            case 0:
                ret = new int[]{0, 5};
                break;
            case 1:
                ret = new int[]{5, 5};
                break;
            case 2:
                ret = new int[]{0, Integer.MAX_VALUE};
                break;
        }
        return ret;
    }
    
    /**
     * deque用のモデル
     */
    private static class RoutineMedDequeModel {

        private RoutineMedModel oldMed;
        private RoutineMedModel newMed;

        private RoutineMedDequeModel(RoutineMedModel oldMed, RoutineMedModel newMed){
            this.oldMed = oldMed;
            this.newMed = newMed;
        }
        private RoutineMedModel getOldMed(){
            return oldMed;
        }
        private RoutineMedModel getNewMed(){
            return newMed;
        }
    }


    private void offerUndoQue(RoutineMedModel oldMed, RoutineMedModel newMed) {

        RoutineMedDequeModel model = new RoutineMedDequeModel(oldMed, newMed);

        // dequeに登録
        undoQue.offerLast(model);
        // redoQueはクリア
        redoQue.clear();
        // 編集の場合はoldMedをnewMedで置き換える
        List<RoutineMedModel> list = tableModel.getDataProvider();

        long modelId = oldMed.getId();
        int index;
        // oldRdの位置を探す
        for (index = 0; index < list.size(); ++index) {
            if (list.get(index).getId() == modelId) {
                break;
            }
        }
        // 上書きする
        list.set(index, newMed);
        
        tableModel.fireTableDataChanged();
        controlButton();
    }

    public void undo() {

        // undoQueから取ってくる
        RoutineMedDequeModel model = (RoutineMedDequeModel) undoQue.pollLast();

        if (model == null){
            return;
        }
        // redoのためにredoQueに追加する
        redoQue.offerLast(model);

        RoutineMedModel oldMed = model.getOldMed();
        RoutineMedModel newMed = model.getNewMed();

        long modelId = newMed.getId();
        // tableModel内を検索
        List<RoutineMedModel> list = tableModel.getDataProvider();
        int index;
        for (index = 0; index < list.size(); ++index) {
            if (list.get(index).getId() == modelId) {
                break;
            }
        }
        list.set(index, oldMed);

        tableModel.fireTableDataChanged();
        controlButton();
    }

    public void redo() {

        // redoQueから取ってくる
        RoutineMedDequeModel model = (RoutineMedDequeModel) redoQue.pollLast();

        if (model == null){
            return;
        }
        // redoのundoのため、undoQueに追加する
        undoQue.offerLast(model);

        RoutineMedModel oldMed = model.getOldMed();
        RoutineMedModel newMed = model.getNewMed();

        // tableModel内を検索
        List<RoutineMedModel> list = tableModel.getDataProvider();
        long modelId = oldMed.getId();
        // tableModel内を検索
        int index;
        for (index = 0; index < list.size(); ++index) {
            if (list.get(index).getId() == modelId) {
                break;
            }
        }
        // 元のRegisteredDiagnosisModelに戻す
        list.set(index, newMed);

        tableModel.fireTableDataChanged();
        controlButton();
    }
    
    private class RoutineMedRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            RoutineMedModel model = (RoutineMedModel) sorter.getObject(row);
            if (DELETE.equals(model.getStatus())) {
                setForeground(DELETE_COLOR);
            }
            return this;
        }
    }
}
