package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * SearchResultInspector.java
 * カルテ検索を行う。
 * PatientInspector.java, DocumentBridgeImpl.java, DocumentHistory.javaにも追加コードあり。
 *
 * @author masuda, Masuda Naika
 */

public class SearchResultInspector {

    // PropertyChange 名
    public static final String SELECTED_HISTORIES = DocumentHistory.SELECTED_HISTORIES;
    // 文書履歴テーブル
    private ListTableModel<DocInfoModel> tableModel;
    // タブペインに表示するパネル
    private JPanel panel;
    // 虫眼鏡
    private JLabel loope;
    // 検索語TextField
    private JTextField searchFld;
    // 件数フィールド
    private JLabel countField;
    // 検索結果テーブル
    private JTable resultTable;
    // 束縛サポート
    private PropertyChangeSupport boundSupport;
    // context
    private ChartImpl context;
    // 選択された文書情報(DocInfo)の配列
    private DocInfoModel[] selectedHistories;
    // 昇順降順のフラグ
    private boolean ascending;
    // 自動的に取得する文書数
    //private int autoFetchCount;


    public static final String SearchResultTitle = "検索";

    /**
     * 検索結果オブジェクトを生成する。
     * @param owner コンテキシト
     */
    public SearchResultInspector(PatientInspector pi) {
        context = pi.getContext();
        initComponent();
        connect();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                searchFld.requestFocusInWindow();
            }
        });
    }

    public void selectAll() {
        ListTableModel model = (ListTableModel) resultTable.getModel();
        int r = model.getObjectCount();
        ListSelectionModel lsm = resultTable.getSelectionModel();
        lsm.setSelectionInterval(0, r - 1);
    }

    /**
     * 履歴テーブルのコレクションを clear する。
     */
    public void clear() {
        if (tableModel != null && tableModel.getDataProvider() != null) {
            tableModel.clear();
        }
    }

    /**
     * 束縛プロパティリスナを登録する。
     * @param propName プロパティ名
     * @param listener リスナ
     */
    public void addPropertyChangeListener(String propName, PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(propName, listener);
    }

    /**
     * 束縛プロパティを削除する。
     * @param propName プロパティ名
     * @param listener リスナ
     */
    public void removePropertyChangeListener(String propName, PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.removePropertyChangeListener(propName, listener);
    }


    /**
     * 束縛プロパティの選択された文書履歴(複数)を設定する。通知を行う。
     * @param newSelected 選択された文書履歴(複数)
     */
    private void setSelectedHistories(DocInfoModel[] newSelected) {

        DocInfoModel[] old = selectedHistories;
        selectedHistories = newSelected;
        // リスナへ通知を行う
        if (selectedHistories != null) {
            boundSupport.firePropertyChange(SELECTED_HISTORIES, old, selectedHistories);
        }
    }

    /**
     * 文書履歴を取得する。
     */
    private void getDocInfo() {

        // データベースからDocInfoを取得する
        List<Long> docPkList = context.getPatient().getDocPkList();
        DocInfoTask task = new DocInfoTask(context, docPkList);
        task.execute();
    }

    /**
     * 文書履歴表示の昇順/降順を設定する。
     * DocumentHistoryでsetAscendingが呼ばれるとこちらも呼ばれる
     * @param ascending 昇順の時 true
     */
    public void setAscending(boolean ascending) {
        this.ascending = ascending;
        updateHistory(tableModel.getDataProvider());
    }

    /**
     * 履歴を再取得した場合等の処理で、履歴テーブルの更新、 最初の行の自動選択、束縛プロパティの変化通知を行う。
     */
    @SuppressWarnings("unchecked")
    private void updateHistory(List<DocInfoModel> newHistory) {

        // ソーティングする
        if (newHistory != null && !newHistory.isEmpty()) {
            if (ascending) {
                Collections.sort(newHistory);
            } else {
                Collections.sort(newHistory, Collections.reverseOrder());
            }
        }

        // 文書履歴テーブルにデータの Arraylist を設定する
        tableModel.setDataProvider(newHistory);

        if (newHistory != null && !newHistory.isEmpty()) {

            int cnt = newHistory.size();
            countField.setText(String.valueOf(cnt) + " 件");

        } else {
            countField.setText("0 件");
        }
    }

    /**
     * GUI コンポーネントを生成する。
     */
    private void initComponent() {

        // 履歴テーブルのパラメータを取得する

        String[] columnNames = new String[]{"確定日","内容"};
        String[] methodNames = new String[]{"getFirstConfirmDateWithRp", "getTitle"};
        Class[] columnClasses = {String.class, String.class};

        // 文書履歴テーブルを生成する
        tableModel = new ListTableModel<DocInfoModel>(columnNames, 1, methodNames, columnClasses) {

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

        };
        resultTable = new JTable();
        resultTable.setFocusable(false);
        resultTable.setModel(tableModel);
        JScrollPane center = new JScrollPane(resultTable);

        // カラム幅を調整する
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(180);

        // ストライプテーブル
        StripeTableCellRenderer renderer = new StripeTableCellRenderer(resultTable);
        renderer.setDefaultRenderer();

        // 件数フィールドを生成する
        countField = new JLabel("0 件");
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.add(Box.createHorizontalGlue());
        south.add(countField);
        // 件数フィールドをクリックすると全て選択する
        countField.setToolTipText("クリックで全選択");
        countField.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                selectAll();
            }
        });
        // 検索語フィールド
        searchFld = new JTextField();
        searchFld.addFocusListener(AutoKanjiListener.getInstance());
        // 虫眼鏡
        loope = new JLabel("検索");
        loope.setIcon(ClientContext.getImageIcon("srch_24.gif"));
        JPanel north = new JPanel();
        north.setLayout(new BorderLayout());
        north.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
        north.add(loope, BorderLayout.WEST);
        north.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);
        north.add(searchFld, BorderLayout.CENTER);
        String searchText = context.getPatient().getSearchText();
        if (searchText == null) {
            searchFld.setText("");
        } else {
            searchFld.setText(searchText);
            getDocInfo();
        }

        // レイアウトする
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(north, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
    }

    /**
     * レイアウトパネルを返す。
     * @return
     */
    public JPanel getPanel() {
        //return (JPanel) view;
        return panel;
    }

    /**
     * Event 接続を行う
     */
    private void connect() {

        // 履歴テーブルで選択された行の文書を表示する
        ListSelectionModel slm = resultTable.getSelectionModel();
        slm.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    int[] selectedRows = resultTable.getSelectedRows();
                    if (selectedRows.length > 0) {
                        List<DocInfoModel> list = new ArrayList<DocInfoModel>(1);
                        for (int i = 0; i < selectedRows.length; i++) {
                            DocInfoModel obj = tableModel.getObject(selectedRows[i]);
                            if (obj != null) {
                                list.add(obj);
                            }
                        }
                        DocInfoModel[] selected = list.toArray(new DocInfoModel[0]);
                        if (selected != null && selected.length > 0) {
                            setSelectedHistories(selected);
                        } else {
                            setSelectedHistories((DocInfoModel[]) null);
                        }
                    }
                }
            }
        });

        // 検索語が入力されると検索する
        searchFld.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = searchFld.getText();
                // ２文字以上で検索
                if (!"".equals(searchText.trim()) && searchText.length() > 1){
                    executeSearch(searchText);
                }
            }
        });
        // Preference から昇順降順を設定する
        ascending = Project.getBoolean(Project.DOC_HISTORY_ASCENDING, false);
    }

    private void executeSearch(String text){
        // Hibernate searchで患者カルテのModuleModelを全文検索する
        MasudaDelegater del = MasudaDelegater.getInstance();
        long karteId = context.getKarte().getId();
        List<PatientModel> result = del.getKarteFullTextSearch(karteId, text);
        if (result != null && !result.isEmpty()) {
            PatientModel pm = result.get(0);
            List<Long> docPkList = pm.getDocPkList();
            context.getPatient().setDocPkList(docPkList);
            getDocInfo();
        } else {
            updateHistory(null);
        }
        context.getKarte().getPatient().setSearchText(text);
    }

    /**
     * 検索タスク。
     */
    private class DocInfoTask extends DBTask<List<DocInfoModel>, Void> {

        private List<Long> docPkList;

        public DocInfoTask(Chart ctx, List<Long> docPkList) {
            super(ctx);
            this.docPkList = docPkList;
        }

        @Override
        protected List<DocInfoModel> doInBackground() throws Exception {

            MasudaDelegater ddl = MasudaDelegater.getInstance();
            List<DocInfoModel> result = ddl.getDocumentList(docPkList);
            return result;
        }

        @Override
        protected void succeeded(List<DocInfoModel> result) {
            if (result != null) {
                updateHistory(result);
            }
        }
    }
}
