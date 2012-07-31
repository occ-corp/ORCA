
package open.dolphin.impl.lbtest;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableColumn;
import open.dolphin.client.CalendarCardPanel;
import open.dolphin.client.Chart;
import open.dolphin.client.ClientContext;
import open.dolphin.delegater.LaboDelegater;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;
import open.dolphin.tr.InFacilityLaboTransferHandler;

/**
 * 院内検査を登録するダイアログ
 * 
 * @author masuda, Masuda Naika
 */
public class InFacilityLabo {
    
    private static final char FULL_MINUS = (char) 65293;
    private static final char HALF_MINUS = '-';
    
    private static final char[] MATCHIES 
            = {'０', '１', '２', '３', '４', '５', '６', '７', '８', '９', '　',  '．', 'ー', FULL_MINUS};
    private static final char[] REPLACES 
            = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', '.', HALF_MINUS, HALF_MINUS};
    
    private static final String[] TEMPLATE_COL_NAME = {"検査項目"};
    private static final String[] TEMPLATE_COL_METHOD = {"getItemName"};
    private static final int[] TEMPLATE_COL_WIDTH = {20};
    private static final int START_NUM_ROW = 1;
    
    private static final String[] SET_COL_NAME = {"検査項目", "値", "異常", "基準値", "単位"};
    private static final String[] SET_COL_METHOD = {"getItemName", "getItemValue", "getAbnormalFlg", "getNormalValue", "getUnit"};
    private static final int[] SET_COL_WIDTH = {20, 10, 10, 20, 10};
    private static final int VALUE_COL = 1;
    private static final int NORMAL_COL = 3;
    private static final int UNIT_COL = 4;
    
    private static final ImageIcon closeIcon = ClientContext.getImageIcon("cancl_16.gif");
    private static final ImageIcon deleteIcon = ClientContext.getImageIcon("del_16.gif");
    private static final ImageIcon saveIcon = ClientContext.getImageIcon("save_16.gif");
    
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private static final SimpleDateFormat dateFrmt = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
    
    private JDialog dialog;
    private JPanel panel;
    private JTextField dateFld;
    private JTable setTable;
    private JTable templateTable;
    private JCheckBox editCheck;
    private JButton deleteBtn;
    private JButton closeBtn;
    private JButton saveBtn;
    private JPanel centerPanel;
    private JScrollPane rtScroll;
    
    private ListTableModel setTableModel;
    private ListTableModel templateTableModel;
    
    private Chart chart;
    private boolean male;
    private boolean toUpdate;
    
    public void setContext(Chart chart) {
        this.chart = chart;
        male = IInfoModel.MALE.equals(chart.getPatient().getGender());
    }

    public boolean start() {
        
        initComponents();
        connect();
        setupTables();
        dialog.setVisible(true);
        
        dialog.dispose();
        
        return toUpdate;
    }
    
    @SuppressWarnings("unchecked")
    private void setupTables() {
        
        templateTableModel = new ListTableModel(TEMPLATE_COL_NAME, START_NUM_ROW, TEMPLATE_COL_METHOD, null) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        setTableModel = new ListTableModel(SET_COL_NAME, START_NUM_ROW, SET_COL_METHOD, null) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == VALUE_COL || column == NORMAL_COL || column == UNIT_COL) {
                    return true;
                }
                return false;
            }

            @Override
            public void setValueAt(Object obj, int row, int column) {
                
                if (column != VALUE_COL && column != NORMAL_COL && column != UNIT_COL) {
                    return;
                }
                InFacilityLaboItem item = (InFacilityLaboItem) setTableModel.getObject(row);
                String value = (String) obj;
                value = toHalfNumber(value).trim();
                switch(column) {
                    case VALUE_COL:
                        item.setItemValue(value);
                        break;
                    case NORMAL_COL:
                        item.setNormalValue(value);
                        break;
                    case UNIT_COL:
                        item.setUnit(value);
                        break;
                }
                item.setAbnormalFlg(getAbnormalFlg(item));
            }
        };
        
        List<InFacilityLaboItem> defaultList = InFacilityLaboTable.createLaboItemList();
        templateTableModel.setDataProvider(defaultList);
        templateTable.setModel(templateTableModel);
        StripeTableCellRenderer renderer = new StripeTableCellRenderer(templateTable);
        renderer.setDefaultRenderer();
        templateTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // 列幅を設定する
        int len = TEMPLATE_COL_NAME.length;
        TableColumn column;
        for (int i = 0; i < len; i++) {
            column = templateTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(TEMPLATE_COL_WIDTH[i]);
        }
        templateTable.setDragEnabled(true);
        InFacilityLaboTransferHandler tHandler = new InFacilityLaboTransferHandler();
        tHandler.setEditable(false);
        templateTable.setTransferHandler(tHandler);

        MasudaDelegater del = MasudaDelegater.getInstance();
        List<InFacilityLaboItem> facilityList = del.getInFacilityLaboItemList();
        setTableModel.setDataProvider(facilityList);
        setTable.setModel(setTableModel);
        renderer = new StripeTableCellRenderer(setTable);
        renderer.setDefaultRenderer();
        setTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // 列幅を設定する
        len = SET_COL_NAME.length;
        for (int i = 0; i < len; i++) {
            column = setTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(SET_COL_WIDTH[i]);
        }
        setTable.setDragEnabled(true);
        setTable.setDropMode(DropMode.INSERT);
        setTable.setColumnSelectionAllowed(false);
        setTable.setCellSelectionEnabled(true);
        InFacilityLaboTransferHandler sHandler = new InFacilityLaboTransferHandler();
        sHandler.setEditable(true);
        setTable.setTransferHandler(sHandler);
        setTable.addMouseMotionListener(new SetTableMouseMotionListener());
        
    }
    
    private void initComponents() {
        
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
        JLabel dateLbl = new JLabel("検査日:");
        north.add(dateLbl);
        dateFld = new JTextField(10);
        dateFld.setMaximumSize(dateFld.getPreferredSize());
        dateFld.setEditable(false);
        dateFld.setText(dateFrmt.format(new Date()));
        north.add(dateFld);
        north.add(Box.createHorizontalGlue());
        editCheck = new JCheckBox("項目編集");
        north.add(editCheck);
        panel.add(north, BorderLayout.NORTH);

        JPanel south = new JPanel();
        south.setLayout(new FlowLayout());
        deleteBtn = new JButton("削除", deleteIcon);
        deleteBtn.setEnabled(false);
        south.add(deleteBtn);
        closeBtn = new JButton("閉じる", closeIcon);
        south.add(closeBtn);
        saveBtn = new JButton("保存", saveIcon);
        south.add(saveBtn);
        panel.add(south, BorderLayout.SOUTH);
        
        setTable = new JTable();
        JScrollPane scroll = new JScrollPane(setTable);
        centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
        centerPanel.add(scroll);
        panel.add(centerPanel, BorderLayout.CENTER);
        
        templateTable = new JTable();
        templateTable.setToolTipText("DnDで左の施設内検査項目テーブルに追加してください。");
        rtScroll = new JScrollPane(templateTable);
        Dimension d = new Dimension(200, 200);
        rtScroll.setPreferredSize(d);
        d = new Dimension(200, Integer.MAX_VALUE);
        rtScroll.setMaximumSize(d);
        
        dialog = new JDialog();
        String title = ClientContext.getFrameTitle("院内検査項目追加");
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.setContentPane(panel);
        ClientContext.setDolphinIcon(dialog);
        
        dialog.pack();
        dialog.setLocationRelativeTo(chart.getFrame());
    }
    
    private void connect() {
        
        editCheck.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (editCheck.isSelected()) {
                    deleteBtn.setEnabled(true);
                    centerPanel.add(rtScroll);
                } else {
                    deleteBtn.setEnabled(false);
                    centerPanel.remove(rtScroll);
                }
                dialog.pack();
            }
        });

        deleteBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                delete();
            }
        });

        closeBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });

        saveBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                save();
                toUpdate = true;
            }
        });
        
        // カレンダによる日付入力を設定する
        PopupListener pl = new PopupListener(dateFld);
    }
    
    private void delete() {
        int[] selectedRows = setTable.getSelectedRows();
        for (int i = selectedRows.length -1; i >= 0; --i) {
            setTableModel.deleteAt(selectedRows[i]);
        }
    }
    
    private void save() {
        
        @SuppressWarnings("unchecked")
        List<InFacilityLaboItem> list = setTableModel.getDataProvider();
        // laboCode(facilityId)を設定する
        String fid = Project.getFacilityId();
        for (InFacilityLaboItem item : list) {
            item.setLaboCode(fid);
        }
        // まずは施設内検査項目を登録する。
        MasudaDelegater del = MasudaDelegater.getInstance();
        del.updateInFacilityLaboItemList(list);
        
        // ついでNLaboModuleを登録する。
        String pid = chart.getPatient().getPatientId();
        String fidPid = fid + ":" + pid;
        String ptName = chart.getPatient().getFullName();
        String sampleDate = dateFld.getText().trim();
        // 検査箋（検査モジュール）のキー = patientId.sampleDate.labCode
        StringBuilder buf = new StringBuilder();
        buf.append(pid);
        buf.append(".");
        buf.append(sampleDate);
        buf.append(".");
        buf.append(fid);
        String testKey = buf.toString();
        
        // NLaboModuleを作成
        NLaboModule nLaboModule = new NLaboModule();
        
        // NLaboItemを作成
        List<NLaboItem> nLaboItemList = new ArrayList<NLaboItem>();
        for (InFacilityLaboItem item : list) {
            String itemValue = item.getItemValue();
            if (itemValue == null || itemValue.trim().isEmpty()){
                continue;
            }
            NLaboItem nLaboItem = createNLaboItem(item);
            nLaboItem.setPatientId(fidPid);
            nLaboItem.setLaboCode(fid);
            nLaboItem.setSampleDate(sampleDate);
            nLaboItem.setLaboModule(nLaboModule);
            nLaboItemList.add(nLaboItem);
        }
        // 空ならリターン
        if (nLaboItemList.isEmpty()) {
            return;
        }
        
        // NLaboModuleに情報設定
        nLaboModule.setPatientId(pid);  // ここはただのPatientID
        nLaboModule.setPatientName(ptName);
        nLaboModule.setPatientSex(male ? "M" : "F");
        nLaboModule.setSampleDate(sampleDate);
        nLaboModule.setLaboCenterCode(fid);
        nLaboModule.setModuleKey(testKey);
        nLaboModule.setReportFormat("NLab");
        //nLaboModule.setNumOfItems(String.valueOf(nLaboItemList.size()));
        nLaboModule.setItems(nLaboItemList);
        
        // NLaboModuleをデータベースに登録する
        LaboDelegater ldel = LaboDelegater.getInstance();
        ldel.postNLaboModule(nLaboModule);
    }
    
    private NLaboItem createNLaboItem(InFacilityLaboItem item) {
        
        NLaboItem nLaboItem = new NLaboItem();
        nLaboItem.setItemCode(item.getMedisCode());
        nLaboItem.setItemName(item.getItemName());
        nLaboItem.setMedisCode(item.getMedisCode());
        nLaboItem.setParentCode(item.getParentCode());
        nLaboItem.setGroupCode(item.getGroupCode());
        nLaboItem.setGroupName(item.getGroupName());
        nLaboItem.setLaboCode(item.getLaboCode());
        nLaboItem.setNormalValue(item.getNormalValue());
        nLaboItem.setSpecimenCode(item.getSpecimenCode());
        nLaboItem.setSpecimenName(item.getSpecimenName());
        nLaboItem.setReportStatus("E");
        nLaboItem.setValue(item.getItemValue());
        nLaboItem.setAbnormalFlg(item.getAbnormalFlg());
        nLaboItem.setUnit(item.getUnit());
        return nLaboItem;
    }
    
    private String getAbnormalFlg(InFacilityLaboItem item) {
        try {
            float value = Float.valueOf(item.getItemValue());
            String[] strs = item.getNormalValue().split("-");
            String low = strs[0].trim();
            String hi = strs[1].trim();
            
            if (low.isEmpty()) {
                float hiValue = Float.valueOf(hi);
                if (value > hiValue) {
                    return "H";
                }
            } else if (hi.isEmpty()) {
                float lowValue = Float.valueOf(low);
                if (value < lowValue) {
                    return "L";
                }
            }
            float hiValue = Float.valueOf(hi);
            float lowValue = Float.valueOf(low);
            if (value < lowValue) {
                return "L";
            } else if (value > hiValue) {
                return "H";
            }

        } catch (Exception e) {
        }
        return null;
    }
    
    private String getFormattedDate(SimpleDate sd) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.clear();
        gc.set(sd.getYear(), sd.getMonth(), sd.getDay());
        return dateFrmt.format(gc.getTime());
    }
     
    private String toHalfNumber(String test) {
        if (test != null) {
            for (int i = 0; i < MATCHIES.length; i++) {
                test = test.replace(MATCHIES[i], REPLACES[i]);
            }
        }
        return test;
    }
    
    // MouseMotionListener
    private class SetTableMouseMotionListener implements MouseMotionListener {

        @Override
        public void mouseDragged(MouseEvent e) {
            int ctrlMask = InputEvent.CTRL_DOWN_MASK;
            int action = ((e.getModifiersEx() & ctrlMask) == ctrlMask)
                    ? TransferHandler.COPY
                    : TransferHandler.MOVE;

            JTable setTable = (JTable) e.getSource();
            // 非選択状態からいきなりドラッグを開始すると cellEditor が残ってしまう問題の workaround
            if (setTable.isEditing()) {
                setTable.getCellEditor().stopCellEditing();
            }
            TransferHandler handler = setTable.getTransferHandler();
            handler.exportAsDrag(setTable, e, action);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }

    private class PopupListener extends MouseAdapter implements PropertyChangeListener {

        private JPopupMenu popup;

        private JTextField tf;

        public PopupListener(JTextField tf) {
            this.tf = tf;
            tf.addMouseListener(PopupListener.this);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {
                popup = new JPopupMenu();
                CalendarCardPanel cc = new CalendarCardPanel(ClientContext.getEventColorTable());
                cc.addPropertyChangeListener(CalendarCardPanel.PICKED_DATE, this);
                cc.setCalendarRange(new int[]{-12, 0});
                popup.insert(cc, 0);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals(CalendarCardPanel.PICKED_DATE)) {
                SimpleDate sd = (SimpleDate) e.getNewValue();
                tf.setText(getFormattedDate(sd));
                popup.setVisible(false);
                popup = null;
            }
        }
    }
}
