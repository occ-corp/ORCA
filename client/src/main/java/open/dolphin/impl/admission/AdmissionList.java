package open.dolphin.impl.admission;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import open.dolphin.client.AbstractMainComponent;
import open.dolphin.client.ClientContext;
import open.dolphin.client.Dolphin;
import open.dolphin.client.StateChangeListener;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.infomodel.AdmissionModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.infomodel.StateMsgModel;
import open.dolphin.project.Project;
import open.dolphin.table.ColumnSpec;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * 入院患者リスト
 * @author masuda, Masuda Naika
 */
public class AdmissionList extends AbstractMainComponent {
    
    // Window Title
    private static final String NAME = "入院リスト";

    // 来院テーブルのカラム名
    private static final String[] COLUMN_NAMES = {
        "部屋", "患者ID",  "氏   名", "性別",  "生年月日", 
        "担当医", "診療科", "入院日", "状態"};
    // 来院テーブルのカラムメソッド
    private static final String[] PROPERTY_NAMES = {
        "getRoom", "getPatientId", "getFullName", "getGenderDesc", "getAgeBirthday", 
        "getDoctorName", "getDeptName", "getAdmissionDate", "isOpened"};
    // 来院テーブルのクラス名
    private static final Class[] COLUMN_CLASSES = {
        String.class, String.class, String.class, String.class, String.class, 
        String.class, String.class, String.class, String.class};
    // 来院テーブルのカラム幅
    private static final int[] COLUMN_WIDTH = {
        20, 80, 130, 40, 100, 100, 50, 80, 30};
    
    private String clientUUID;
    
    // Status　情報　メインウィンドウの左下に表示される内容
    private String statusInfo;
    
    // オープンアイコン
    private static final ImageIcon OPEN_ICON = ClientContext.getImageIcon("open_16.gif");
    // ネットワークアイコン
    private static final ImageIcon NETWORK_ICON = ClientContext.getImageIcon("ntwrk_16.gif");
    
    // カラム仕様名
    private static final String COLUMN_SPEC_NAME = "admissionTable.column.spec";
    // カラム仕様リスト
    private List<ColumnSpec> columnSpecs;
    
    // View panel
    private AdmissionListView view;
    // Table
    private JTable table;
    // Table Model
    private ListTableModel<PatientModel> tableModel;
    // TableSorter
    private ListTableSorter sorter;

    private int stateColumn;
    
    // 選択されている行を保存
    private int selectedRow;
    
    private Action openKarteAction;
    private Action copyAction;
    
    
    public AdmissionList() {
        setName(NAME);
        clientUUID = Dolphin.getInstance().getClientUUID();
    }

    @Override
    public void start() {
        setup();
        initComponents();
        connect();
        startSyncMode();
    }

    @Override
    public void stop() {
        
        if (columnSpecs != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < columnSpecs.size(); i++) {
                ColumnSpec cs = columnSpecs.get(i);
                cs.setWidth(table.getColumnModel().getColumn(i).getPreferredWidth());
                sb.append(cs.getName()).append(",");
                sb.append(cs.getMethod()).append(",");
                sb.append(cs.getCls()).append(",");
                sb.append(cs.getWidth()).append(",");
            }
            sb.setLength(sb.length() - 1);
            String line = sb.toString();
            Project.setString(COLUMN_SPEC_NAME, line);
        }
        // ChartStateListenerから除去する
        StateChangeListener.getInstance().removeListener(this);
    }
    /**
     * メインウインドウのタブで受付リストに切り替わった時 コールされる。
     */
    @Override
    public void enter() {
        controlMenu();
        getContext().getStatusLabel().setText(statusInfo);
    }
    
    // comet long polling機能を設定する
    private void startSyncMode() {
        setStatusInfo();
        getAdmittedPatients();
        StateChangeListener.getInstance().addListener(this);
        enter();
    }
    
    private void setup() {
        
        // Table deafult
        String defaultLine;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            String name = COLUMN_NAMES[i];
            String method = PROPERTY_NAMES[i];
            String cls = COLUMN_CLASSES[i].getName();
            String width = String.valueOf(COLUMN_WIDTH[i]);
            sb.append(name).append(",");
            sb.append(method).append(",");
            sb.append(cls).append(",");
            sb.append(width).append(",");
        }
        sb.setLength(sb.length() - 1);
        defaultLine = sb.toString();
        
        // preference から
        String line = Project.getString(COLUMN_SPEC_NAME, defaultLine);

        // 仕様を保存
        columnSpecs = new ArrayList<ColumnSpec>();
        String[] params = line.split(",");
        
        // 保存していた名称・メソッド・クラスが同じか調べる
        int len = params.length / 4;
        // 項目数が同じか？
        boolean same = len == COLUMN_NAMES.length;
        // 各項目は同じか
        if (same) {
            List<String> savedColumns = new ArrayList<String>();
            List<String> savedProps = new ArrayList<String>();
            List<String> savedClasses = new ArrayList<String>();
            for (int i = 0; i < len; ++i) {
                int k = 4 * i;
                savedColumns.add(params[k]);
                savedProps.add(params[k + 1]);
                savedClasses.add(params[k + 2]);
            }
            for (int i = 0; i < len; ++i) {
                savedColumns.remove(COLUMN_NAMES[i]);
                savedProps.remove(PROPERTY_NAMES[i]);
                savedClasses.remove(COLUMN_CLASSES[i].getName());
            }
            // 同じならば空のはず
            same &= savedColumns.isEmpty() && savedProps.isEmpty() && savedClasses.isEmpty();
        }
        // 保存していた情報数が現在と違う場合は破棄
        if (!same) {
            params = defaultLine.split(",");
            len = params.length / 4;
        }

        for (int i = 0; i < len; i++) {
            int k = 4 * i;
            String name = params[k];
            String method = params[k + 1];
            String cls = params[k + 2];
            int width = Integer.parseInt(params[k + 3]);
            ColumnSpec cp = new ColumnSpec(name, method, cls, width);
            columnSpecs.add(cp);
        }

        // Scan して state カラムを設定する
        for (int i = 0; i < columnSpecs.size(); i++) {
            ColumnSpec cs = columnSpecs.get(i);
            String test = cs.getMethod();
            if (test.equals("isOpened")) {
                stateColumn = i;
            }
        }
    }
    
    /**
     * GUI コンポーネントを初期化しレアイアウトする。
     */
    private void initComponents() {
        // View クラスを生成しこのプラグインの UI とする
        view = new AdmissionListView();
        setUI(view);

        view.getInfoLbl().setText("");
        
        //------------------------------------------
        // View のテーブルモデルを置き換える
        //------------------------------------------
        int len = columnSpecs.size();
        String[] columnNames = new String[len];
        String[] methods = new String[len];
        Class[] cls = new Class[len];
        int[] width = new int[len];
        try {
            for (int i = 0; i < len; i++) {
                ColumnSpec cp = columnSpecs.get(i);
                columnNames[i] = cp.getName();
                methods[i] = cp.getMethod();
                cls[i] = Class.forName(cp.getCls());
                width[i] = cp.getWidth();
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
        
        table = view.getTable();
        tableModel = new ListTableModel<PatientModel>(columnNames, 1, methods, cls) {

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        // sorter組み込み
        sorter = new ListTableSorter(tableModel);
        table.setModel(sorter);
        sorter.setTableHeader(table.getTableHeader());
        
        // 選択モード
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 連ドラ
        PatientListTableRenderer renderer = new PatientListTableRenderer();
        renderer.setTable(table);
        renderer.setDefaultRenderer();

        // カラム幅
        changeColumnWidth();
        
    }
    
    private void changeColumnWidth() {

        for (int i = 0; i < columnSpecs.size(); ++i) {
            ColumnSpec cs = columnSpecs.get(i);
            int width = cs.getWidth();
            TableColumn tc = table.getColumnModel().getColumn(i);
            if (width != 0) {
                tc.setMaxWidth(Integer.MAX_VALUE);
                tc.setPreferredWidth(width);
                tc.setWidth(width);
            } else {
                tc.setMaxWidth(0);
                tc.setMinWidth(0);
                tc.setPreferredWidth(0);
                tc.setWidth(0);

            }
        }
        table.repaint();
    }
    
    private void connect() {
        
        // tableModel のカラム変更関連イベント
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {

            @Override
            public void columnAdded(TableColumnModelEvent tcme) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent tcme) {
            }

            @Override
            public void columnMoved(TableColumnModelEvent tcme) {
                int from = tcme.getFromIndex();
                int to = tcme.getToIndex();
                ColumnSpec moved = columnSpecs.remove(from);
                columnSpecs.add(to, moved);
            }

            @Override
            public void columnMarginChanged(ChangeEvent ce) {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent lse) {
            }
        });
        

        // 来院リストテーブル 選択
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    selectedRow = table.getSelectedRow();
                    controlMenu();
                }
            }
        });

        // 来院リストテーブル ダブルクリック
        view.getTable().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openKarte();
                }
            }
        });

        // コンテキストメニューを登録する
        view.getTable().addMouseListener(new ContextListener());

        // 靴のアイコンをクリックした時来院情報を検索する
        view.getUpdateBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                renewList();
            }
        });

        //-----------------------------------------------
        // Copy 機能を実装する
        //-----------------------------------------------
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        copyAction = new AbstractAction("コピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyRow();
            }
        };
        table.getInputMap().put(copy, "Copy");
        table.getActionMap().put("Copy", copyAction);
        
        // カルテオープンアクションを設定
        openKarteAction = new AbstractAction("カルテを開く") {

            @Override
            public void actionPerformed(ActionEvent e) {
                openKarte();
            }
        };
    }
    
    /**
     * 選択されている行をコピーする。
     */
    public void copyRow() {

        StringBuilder sb = new StringBuilder();
        int numRows = table.getSelectedRowCount();
        int[] rowsSelected = table.getSelectedRows();
        int numColumns = table.getColumnCount();

        for (int i = 0; i < numRows; i++) {
            if (sorter.getObject(rowsSelected[i]) != null) {
                StringBuilder s = new StringBuilder();
                for (int col = 0; col < numColumns; col++) {
                    Object o = table.getValueAt(rowsSelected[i], col);
                    if (o != null) {
                        s.append(o.toString());
                    }
                    s.append(",");
                }
                if (s.length() > 0) {
                    s.setLength(s.length() - 1);
                }
                sb.append(s.toString()).append("\n");
            }
        }
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
        }
    }
    
    private void setStatusInfo() {
        
    }
    
    private void getAdmittedPatients() {
        
    }
    
    private void controlMenu() {
        
    }
    
    private void openKarte() {
        
        PatientModel patient = getSelectedPatient();
        PatientVisitModel pvt = new PatientVisitModel();
        pvt.setPatientModel(patient);
        
        // 受け付けを通していないので診療科はユーザ登録してあるものを使用する
        // 診療科名、診療科コード、医師名、医師コード、JMARI
        // 2.0
        pvt.setDeptName(Project.getUserModel().getDepartmentModel().getDepartmentDesc());
        pvt.setDeptCode(Project.getUserModel().getDepartmentModel().getDepartment());
        pvt.setDoctorName(Project.getUserModel().getCommonName());
        if (Project.getUserModel().getOrcaId() != null) {
            pvt.setDoctorId(Project.getUserModel().getOrcaId());
        } else {
            pvt.setDoctorId(Project.getUserModel().getUserId());
        }
        pvt.setJmariNumber(Project.getString(Project.JMARI_CODE));
        
        // カルテコンテナを生成する
        getContext().openKarte(pvt);
    }
    
    private void renewList() {
        
        SwingWorker worker = new SwingWorker<List<PatientModel>, Void>() {

            @Override
            protected List<PatientModel> doInBackground() throws Exception {
                setBusy(true);
                Date today = new Date();
                List<AdmissionModel> list = SqlMiscDao.getInstance().getInHospitalPatients(today);
                List<PatientModel> ret = MasudaDelegater.getInstance().getAdmittedPatients(list);
                return ret;
            }

            @Override
            protected void done() {
                try {
                    List<PatientModel> list = get();
                    tableModel.setDataProvider(list);
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                }

                updateInfo();
                setBusy(false);
            }
        };
        worker.execute();
    }

    private void updateInfo() {
        SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        int num = (tableModel.getDataProvider() != null)
                ? tableModel.getDataProvider().size()
                : 0;
        StringBuilder sb = new StringBuilder();
        sb.append(frmt.format(new Date()));
        sb.append(" 入院患者数：");
        sb.append(String.valueOf(num));
        sb.append(" 人");
        view.getInfoLbl().setText(sb.toString());
    }
    
    
    private PatientModel getSelectedPatient() {
        selectedRow = table.getSelectedRow();
        return (PatientModel) sorter.getObject(selectedRow);
    }
    
    /**
     * テーブル及びアイコンの enable/diable 制御を行う。
     *
     * @param busy pvt 検索中は true
     */
    private void setBusy(final boolean busy) {
        
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (busy) {
                    view.getUpdateBtn().setEnabled(false);
                    if (getContext().getCurrentComponent() == getUI()) {
                        getContext().block();
                        getContext().getProgressBar().setIndeterminate(true);
                    }
                    selectedRow = table.getSelectedRow();
                } else {
                    view.getUpdateBtn().setEnabled(true);
                    if (getContext().getCurrentComponent() == getUI()) {
                        getContext().unblock();
                        getContext().getProgressBar().setIndeterminate(false);
                        getContext().getProgressBar().setValue(0);
                    }
                    table.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
                }
            }
        });
    }
    
    /**
     * 受付リストのコンテキストメニュークラス。
     */
    private class ContextListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            mabeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mabeShowPopup(e);
        }

        public void mabeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {

                final JPopupMenu contextMenu = new JPopupMenu();

                int row = table.rowAtPoint(e.getPoint());
                PatientModel pm = getSelectedPatient();
                
                if (row == selectedRow && pm != null && !pm.isOpened()) {
                    contextMenu.add(new JMenuItem(openKarteAction));
                    contextMenu.addSeparator();
                }

                // 表示カラム設定
                JMenu menu = new JMenu("表示カラム");
                contextMenu.add(menu);
                for (ColumnSpec cs : columnSpecs) {
                    final MyCheckBoxMenuItem cbm = new MyCheckBoxMenuItem(cs.getName());
                    cbm.setColumnSpec(cs);
                    if (cs.getWidth() != 0) {
                        cbm.setSelected(true);
                    }
                    cbm.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (cbm.isSelected()) {
                                cbm.getColumnSpec().setWidth(50);
                            } else {
                                cbm.getColumnSpec().setWidth(0);
                            }
                            changeColumnWidth();
                        }
                    });
                    menu.add(cbm);
                }

                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    
    private class PatientListTableRenderer extends StripeTableCellRenderer {

        public PatientListTableRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {

            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
            
            PatientModel pm = (PatientModel) sorter.getObject(row);
            
            if (pm != null && col == stateColumn) {
                if (pm.isOpened()) {
                    if (clientUUID.equals(pm.getOwnerUUID())) {
                        setIcon(OPEN_ICON);
                    } else {
                        setIcon(NETWORK_ICON);
                    }
                } else {
                    setIcon(null);
                }
                setText("");
            } else {
                setIcon(null);
                setText(value == null ? "" : value.toString());
            }

            return this;
        }
    }
    
    private class MyCheckBoxMenuItem extends JCheckBoxMenuItem {
        
        private ColumnSpec cs;
        
        private MyCheckBoxMenuItem(String name) {
            super(name);
        }
        
        private void setColumnSpec(ColumnSpec cs) {
            this.cs = cs;
        }
        private ColumnSpec getColumnSpec() {
            return cs;
        }
    }

    // ChartStateListener
    @Override
    public void onMessage(StateMsgModel msg) {

    }
}
