package open.dolphin.impl.pinfo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import open.dolphin.client.AbstractChartDocument;
import open.dolphin.client.ClientContext;
import open.dolphin.client.DefaultCellEditor2;
import open.dolphin.delegater.PatientDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.PVTHealthInsuranceModel;
import open.dolphin.infomodel.PVTPublicInsuranceItemModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * Documet to show Patient and Health Insurance info.
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public class PatientInfoDocument extends AbstractChartDocument {
    
    // Title
    private static final String TITLE = "患者情報";
    
    // 患者属性名
    private static final String[] PATIENT_ATTRS = { 
        "患者 ID", "氏  名", "カナ", "ローマ字 *","性  別", "生年月日", "国  籍 *", "婚姻状況 *", "郵便番号", "住  所", "電  話", "携帯電話 *", "電子メール *"
    };
    
    // Info アイコン
    private static final String INFO_BUTTON_IMAGE   = "about_16.gif";
    
    private static final String INFO = "* の項目は編集が可能です";
    
    // カラム名
    private static final String[] COLUMN_NAMES = { "項   目", "値" };
    
    // 編集可能な行
//masuda^   患者IDも編集可能にする masuda
    //private static final int[] EDITABLE_ROWS = {3, 6, 7, 11, 12};
    private static final int[] EDITABLE_ROWS = {0, 3, 6, 7, 11, 12};
//masuda$
    
    // 保存アイコン
    private static final String SAVE_ICON = "save_16.gif";
    
    // 保存ボタン
    private JButton saveBtn;
    
    // テーブルモデル
    private PatientInfoTableModel pModel;
    
    // 属性表示テーブル
    private JTable pTable;
    
    // State Context
    private StateContext stateMgr;
    
    /** 
     * Creates new PatientInfoDocument 
     */
    public PatientInfoDocument() {
        setTitle(TITLE);
        
    }
    
    private void initialize() {
       
        JComponent compo = createComponent();
       
        //
        // 保存ボタンを生成する
        //
        JPanel cmdPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cmdPanel.add(new JLabel(ClientContext.getImageIcon(INFO_BUTTON_IMAGE)));
        cmdPanel.add(new JLabel(INFO));
        saveBtn = new JButton(ClientContext.getImageIcon(SAVE_ICON));
        saveBtn.setEnabled(false);
        //saveBtn.setMargin(new Insets(0,0,0,0));
        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        cmdPanel.add(saveBtn);

        JPanel content = new JPanel(new BorderLayout());
        content.add(cmdPanel, BorderLayout.NORTH);
        content.add(compo, BorderLayout.CENTER);
        //content.setBorder(BorderFactory.createTitledBorder("患者情報"));

        JPanel myPanel = getUI();
        myPanel.setLayout(new BorderLayout());
        myPanel.add(content, BorderLayout.CENTER);
        myPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
        
        stateMgr = new StateContext();
        enter();
    }
    
    @Override
    public void start() {
        initialize();
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void enter() {
        super.enter();
        if (stateMgr != null) {
            stateMgr.enter();
        }
    }
    
    @Override
    public boolean isDirty() {
        if (stateMgr != null) {
            return stateMgr.isDirtyState();
        } else {
            return super.isDirty();
        }
    }
    
    /**
     * 患者情報を更新する。
     */
    @Override
    public void save() {
        
        final PatientModel update = getContext().getPatient();
//masuda^   シングルトン化
        //final PatientDelegater pdl = new PatientDelegater();
        final PatientDelegater pdl = PatientDelegater.getInstance();
//masuda$
        
        DBTask task = new DBTask<Void, Void>(getContext()) {
            
            @Override
            public Void doInBackground() throws Exception {
                pdl.updatePatient(update);
                return null;
            }
            
            @Override
            public void succeeded(Void result) {
                stateMgr.processSavedEvent();
            }
        };
        
        task.execute();
    }
    
    private JComponent createComponent() {
        
        // 患者モデルを取得する
        PatientModel patient = getContext().getPatient();
        Collection<PVTHealthInsuranceModel> insList = patient.getPvtHealthInsurances();
        
        // 患者情報テーブルを生成する
        pModel = new PatientInfoTableModel(patient, PATIENT_ATTRS, COLUMN_NAMES);
        pTable = new JTable(pModel);

        // 行の高さ
        pTable.setRowHeight(ClientContext.getMoreHigherRowHeight());

        // レンダラ
//masuda^   ストライプテーブル
        //pTable.setDefaultRenderer(Object.class, new OddEvenRowRenderer());
        StripeTableCellRenderer pRenderer = new StripeTableCellRenderer(pTable);
        pRenderer.setDefaultRenderer();
        // 行の高さ
        pTable.setRowHeight(ClientContext.getMoreHigherRowHeight());
        // エディタ
        TableColumn column = pTable.getColumnModel().getColumn(1);
        //DefaultCellEditor de = new DefaultCellEditor(new JTextField());
        DefaultCellEditor de = new DefaultCellEditor2(new JTextField());
        de.setClickCountToStart(2);
        column.setCellEditor(de);
        // 幅
        //pTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        //pTable.getColumnModel().getColumn(1).setPreferredWidth(500);
//masuda$
        //-----------------------------------------------
        // Copy 機能を実装する
        //-----------------------------------------------
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        final AbstractAction copyAction = new AbstractAction("コピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyRow();
            }
        };
        pTable.getInputMap().put(copy, "Copy");
        pTable.getActionMap().put("Copy", copyAction);

        //-------------------------------------------------
        // Copy menu を加える
        //-------------------------------------------------
        pTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent me) {
                mabeShowPopup(me);
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                mabeShowPopup(me);
            }

            public void mabeShowPopup(MouseEvent e) {

                if (!e.isPopupTrigger()) {
                    return;
                }

                int row = pTable.rowAtPoint(e.getPoint());

                if (row < 0) {
                    return;
                }

                JPopupMenu contextMenu = new JPopupMenu();
                contextMenu.add(new JMenuItem(copyAction));
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        
        // 配置する
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(pTable);
        
        //
        // 健康保険情報テーブルを生成する
        //
        if (insList != null) {
            
            for (PVTHealthInsuranceModel insurance : insList) {
                HealthInsuranceTableModel hModel = new HealthInsuranceTableModel(
                        insurance, COLUMN_NAMES);
                JTable hTable = new JTable(hModel);

                // 行の高さ
                hTable.setRowHeight(ClientContext.getMoreHigherRowHeight());

                // レンダラ
//masuda^   ストライプテーブル
                //hTable.setDefaultRenderer(Object.class, new OddEvenRowRenderer());
                StripeTableCellRenderer hRenderer = new StripeTableCellRenderer(hTable);
                hRenderer.setDefaultRenderer();
                // 行の高さ
                hTable.setRowHeight(ClientContext.getMoreHigherRowHeight());
                // 幅
                //hTable.getColumnModel().getColumn(0).setPreferredWidth(200);
                //hTable.getColumnModel().getColumn(1).setPreferredWidth(500);
//masuda$
                // 配置する
                panel.add(Box.createVerticalStrut(7));
                panel.add(hTable);
            }
        }
        
//masuda^   Ｃ管理、在宅
        PatientManagementModel mModel = new PatientManagementModel(patient);
        JTable mTable = new JTable(mModel);
        StripeTableCellRenderer mRenderer = new StripeTableCellRenderer(mTable);
        mRenderer.setDefaultRenderer();
        mTable.setRowHeight(ClientContext.getMoreHigherRowHeight());
        panel.add(Box.createVerticalStrut(7));
        panel.add(mTable);
//masuda$
        
        JScrollPane scroller = new JScrollPane(panel);
        
        return scroller;
    }

    /**
     * 選択されている行をコピーする。
     */
    public void copyRow() {
        StringBuilder sb = new StringBuilder();
        int numRows = pTable.getSelectedRowCount();
        int[] rowsSelected = pTable.getSelectedRows();
        int numColumns =   pTable.getColumnCount();

        for (int i = 0; i < numRows; i++) {

            StringBuilder s = new StringBuilder();
            for (int col = 0; col < numColumns; col++) {
                Object o = pTable.getValueAt(rowsSelected[i], col);
                if (o!=null) {
                    s.append(o.toString());
                }
                s.append(",");
            }
            if (s.length()>0) {
                s.setLength(s.length()-1);
            }
            sb.append(s.toString()).append("\n");

        }
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
        }
    }
    
//masuda^   算定情報TableModel
    private class PatientManagementModel extends AbstractTableModel {
        
        // KarteBean
        private PatientModel patient;
        
        // 属性名の配列
        private final String[] attributes = {"Ｃ管理", "在宅時(特定施設入居時)医学総合管理", "在宅", "特定施設入居"};

        
        public PatientManagementModel(PatientModel patient) {
            this.patient = patient;
        }
        
        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public int getRowCount() {
            return attributes.length;
        }

        // boolean値でチェックボックスを表示する
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        @Override
        public Object getValueAt(int row, int col) {
            
            if (col == 0) {
                // 属性名を返す
                return attributes[row];
            } else if (col == 1 && patient != null) {
                switch (row) {
                    case 0:
                        return patient.getSanteiInfoModel().isCancerCare();
                    case 1:
                        return patient.getSanteiInfoModel().isZaitakuSougouKanri();
                    case 2:
                        return patient.getSanteiInfoModel().isHomeMedicalCare();
                    case 3:
                        return patient.getSanteiInfoModel().isNursingHomeMedicalCare();
                }
            }
            return null;
        }
        
        @Override
        public void setValueAt(Object value, int row, int col) {

            if (value == null || col == 0) {
                return;
            }

            boolean b = (Boolean) value;
            switch (row) {
                case 0:
                    patient.getSanteiInfoModel().setCancerCare(b);
                    stateMgr.processDirtyEvent();
                    break;
                case 1:
                    patient.getSanteiInfoModel().setZaitakuSougouKanri(b);
                    if (b && !patient.getSanteiInfoModel().isHomeMedicalCare()
                            && !patient.getSanteiInfoModel().isNursingHomeMedicalCare()) {
                        patient.getSanteiInfoModel().setHomeMedicalCare(true);
                        fireTableDataChanged();
                    }
                    stateMgr.processDirtyEvent();
                    break;
                case 2:
                    patient.getSanteiInfoModel().setHomeMedicalCare(b);
                    if (b) {
                        patient.getSanteiInfoModel().setNursingHomeMedicalCare(!b);
                        fireTableDataChanged();
                    }
                    stateMgr.processDirtyEvent();
                    break;
                case 3:
                    patient.getSanteiInfoModel().setNursingHomeMedicalCare(b);
                    if (b) {
                        patient.getSanteiInfoModel().setHomeMedicalCare(!b);
                        fireTableDataChanged();
                    }
                    stateMgr.processDirtyEvent();
                    break;
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {

            if (getContext().isReadOnly()) {
                return false;
            }

            if (col == 1) {
                return true;
            }
            return false;
        }
    }
//masuda$
    
    /**
     * 患者情報を表示する TableModel クラス。
     */
    protected class PatientInfoTableModel extends AbstractTableModel {
        
        // 患者モデル
        private PatientModel patient;
        
        // 属性名の配列
        private String[] attributes;
        
        // カラム名の配列
        private String[] columnNames;
        
        public PatientInfoTableModel(PatientModel patient, String[] attrs, String[] columnNames) {
            this.patient = patient;
            this.attributes = attrs;
            this.columnNames = columnNames;
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public int getRowCount() {
            return PATIENT_ATTRS.length;
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            //
            // 編集可能な行である場合に true 
            //
//masuda^   ReadOnly
            if (getContext().isReadOnly()) {
                return false;
            }
//masuda$
            boolean ret = false;
            if (col == 1) {
                for (int i = 0; i < EDITABLE_ROWS.length; i++) {
                    if (row == EDITABLE_ROWS[i]) {
                        ret = true;
                        break;
                    }
                }
            }
            return ret;
        }
        
        @Override
        public Object getValueAt(int row, int col) {
            
            String ret = null;
            
            if (col == 0) {
                //
                // 属性名を返す
                //
                ret = attributes[row];
                
            } else if (col == 1 && patient != null) {
                
                //
                // 患者属性を返す
                //
                
                switch (row) {
                    
                    case 0:
                        ret = patient.getPatientId();
                        break;
                        
                    case 1:
                        ret = patient.getFullName();
                        break;
                        
                    case 2:
                        ret = patient.getKanaName();
                        break;
                        
                    case 3:
                        ret = patient.getRomanName();
                        break;
                        
                    case 4:
                        ret = patient.getGender();
                        ret = ret.toLowerCase();
                        if (ret.startsWith("m") || ret.startsWith("男")) {
                            ret = "男性";
                        } else if (ret.startsWith("f") || ret.startsWith("女")) {
                            ret = "女性";
                        }
                        break;
                        
                    case 5:
//masuda^
                        /*
                        int[] spec = AgeCalculater.getAgeSpec(patient.getBirthday());
                        StringBuilder sb = new StringBuilder();
                        if (spec[0]!=-1) {
                            sb.append(spec[0]).append("歳 ");
                            sb.append(spec[1]).append("ヶ月 ");
                            sb.append(spec[2]).append("日");
                        }
                        sb.append(" (").append(patient.getBirthday()).append(")");
                        ret = sb.toString();
                        break;
                        */
                        ret = patient.getAgeBirthday2();
                        break;
//masuda
                    case 6:
                        ret = patient.getNationality();
                        break;
                        
                    case 7:
                        ret = patient.getMaritalStatus();
                        break;
                        
                    case 8:
                        ret = patient.contactZipCode();
                        break;
                        
                    case 9:
                        ret = patient.contactAddress();
                        if (ret != null) {
                            //ret = ret.replaceAll("　", " ");
                            ret = ret.replace("　", " ");
                        }
                        break;
                        
                    case 10:
                        ret = patient.getTelephone();
                        break;
                        
                    case 11:
                        ret = patient.getMobilePhone();
                        break;
                        
                    case 12:
                        ret = patient.getEmail();
                        break;
                        
                }
            }
            return ret;
        }
        
        
        /**
         * 属性値を変更する。
         * @param value 属性値
         * @param row 行
         * @param col 列
         */
        @Override
        public void setValueAt(Object value, int row, int col) {
            
            if (value == null || value.equals("") || col == 0) {
                return;
            }
            
            String strValue = (String) value;
            
            switch (row) {
//masuda^   Patient ID を編集する
                case 0:
                    String msg = "患者ＩＤを書き換えます。よろしいですか？";
                    Object[] options = {"取消", "了承"};
                    int val = JOptionPane.showOptionDialog(getContext().getFrame(), msg, "OpenDolphin",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                            null, options, options[0]);
                    if (val == 1) {
                        patient.setPatientId(strValue);
                        stateMgr.processDirtyEvent();
                    }
                    break;
//masuda$
                case 3:
                    //
                    // ローマ字
                    //
                    patient.setRomanName(strValue);
                    stateMgr.processDirtyEvent();
                    break;
                    
                case 6:
                    //
                    // 国籍
                    //
                    patient.setNationality(strValue);
                    stateMgr.processDirtyEvent();
                    break;
                    
                case 7:
                    //
                    // 婚姻状況
                    //
                    patient.setMaritalStatus(strValue);
                    stateMgr.processDirtyEvent();
                    break;
                    
               case 11:
                    //
                    // 携帯電話
                    //
                    patient.setMobilePhone(strValue);
                    stateMgr.processDirtyEvent();
                    break;     
                    
                case 12:
                    //
                    // 電子メール
                    //
                    patient.setEmail(strValue);
                    stateMgr.processDirtyEvent();
                    break;
            }
        }
    }
    
    /**
     * 保険情報を表示する TableModel クラス。
     */
    protected class HealthInsuranceTableModel extends AbstractTableModel {
        
        private String[] columnNames;
        
        private List<String[]> data;
        
        public HealthInsuranceTableModel(PVTHealthInsuranceModel insurance,
                String[] columnNames) {
            this.columnNames = columnNames;
            data = getData(insurance);
        }
        
        private List<String[]> getData(PVTHealthInsuranceModel insurance) {
            
            if (insurance == null) {
                return null;
            }
            
            List<String[]> list = new ArrayList<String[]>();
            
            String[] rowData = new String[2];
            rowData[0] = "保険種別";
            rowData[1] = insurance.getInsuranceClass();
            list.add(rowData);
            
            rowData = new String[2];
            rowData[0] = "保険種別コード";
            rowData[1] = insurance.getInsuranceClassCode();
            list.add(rowData);
            
            rowData = new String[2];
            rowData[0] = "保険者番号";
            rowData[1] = insurance.getInsuranceNumber();
            list.add(rowData);
            
            rowData = new String[2];
            rowData[0] = "被保険者記号";
            rowData[1] = insurance.getClientGroup();
            list.add(rowData);
            
            rowData = new String[2];
            rowData[0] = "被保険者番号";
            rowData[1] = insurance.getClientNumber();
            list.add(rowData);
            
            rowData = new String[2];
            rowData[0] = "本人家族区分";
            String test = insurance.getFamilyClass();
            if (Boolean.valueOf(test)) {
                test = "本人";
            } else {
                test = "家族";
            }
            rowData[1] = test;
            list.add(rowData);
            
            rowData = new String[2];
            rowData[0] = "開始日";
            rowData[1] = insurance.getStartDate();
            list.add(rowData);
            
            rowData = new String[2];
            rowData[0] = "有効期限";
            rowData[1] = insurance.getExpiredDate();
            list.add(rowData);
            
            String[] vals = insurance.getContinuedDisease();
            if (vals != null) {
                int count = vals.length;
                for (int i = 0; i < count; i++) {
                    rowData = new String[2];
                    rowData[0] = "継続適応疾患名";
                    rowData[1] = vals[i];
                    list.add(rowData);
                }
            }
            
            rowData = new String[2];
            rowData[0] = "入院時の負担率";
            rowData[1] = insurance.getPayInRatio();
            list.add(rowData);
            
            rowData = new String[2];
            rowData[0] = "外来時の負担率";
            rowData[1] = insurance.getPayOutRatio();
            list.add(rowData);
            
            PVTPublicInsuranceItemModel[] pbi = insurance
                    .getPVTPublicInsuranceItem();
            if (pbi == null) {
                return list;
            }
            int count = pbi.length;
            for (int i = 0; i < count; i++) {
                PVTPublicInsuranceItemModel item = pbi[i];
                
                rowData = new String[2];
                rowData[0] = "公費の優先順位";
                rowData[1] = item.getPriority();
                list.add(rowData);
                
                rowData = new String[2];
                rowData[0] = "公費負担名称";
                rowData[1] = item.getProviderName();
                list.add(rowData);
                
                rowData = new String[2];
                rowData[0] = "負担者番号";
                rowData[1] = item.getProvider();
                list.add(rowData);
                
                rowData = new String[2];
                rowData[0] = "受給者番号";
                rowData[1] = item.getRecipient();
                list.add(rowData);
                
                rowData = new String[2];
                rowData[0] = "開始日";
                rowData[1] = item.getStartDate();
                list.add(rowData);
                
                rowData = new String[2];
                rowData[0] = "有効期限";
                rowData[1] = item.getExpiredDate();
                list.add(rowData);
                
                rowData = new String[2];
                rowData[0] = "負担率";
                rowData[1] = item.getPaymentRatio();
                list.add(rowData);
                
                rowData = new String[2];
                rowData[0] = "負担率または負担金";
                rowData[1] = item.getPaymentRatioType();
                list.add(rowData);
            }
            
            return list;
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public int getRowCount() {
            return data != null ? data.size() : 5;
        }
        
        @Override
        public Object getValueAt(int row, int col) {
            
            if (data == null) {
                return null;
            }
            
            if (row >= data.size()) {
                return null;
            }
            
            String[] rowData = data.get(row);
            
            return (Object) rowData[col];
        }
    }
    
    abstract class State {
        
        public abstract void enter();
        
    }
    
    class CleanState extends State {
        
        @Override
        public void enter() {
            saveBtn.setEnabled(false);
            setDirty(false);
        }
    }
    
    class DirtyState extends State {
        
        @Override
        public void enter() {
            saveBtn.setEnabled(true);
        }
    }
    
    class StateContext {
        
        private CleanState cleanState = new CleanState();
        private DirtyState dirtyState = new DirtyState();
        private State curState;
        
        public StateContext() {
            curState = cleanState;
        }
        
        public void enter() {
            curState.enter();
        }
        
        public void processSavedEvent() {
            curState = cleanState;
            this.enter();
        }
        
        public void processDirtyEvent() {
            if (!isDirtyState()) {
                curState = dirtyState;
                this.enter();
            }
        }
        
        public boolean isDirtyState() {
            return curState == dirtyState;
        }
    }
}