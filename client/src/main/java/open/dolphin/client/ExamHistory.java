package open.dolphin.client;

import java.awt.Color;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.ExamHistoryModel;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * 検査履歴を取得し、表示するクラス。
 * PatientInspector.javaにも追加コードあり
 *
 * @author masuda, Masuda Naika
 */
public class ExamHistory {

    private ListTableModel tableModel;     // 文書履歴テーブル
    private JTable table;
    private InspectorTablePanel view;
    private DocumentHistory docHistory;     // 文書履歴
    private ChartImpl context;
    private PatientInspector patientInspector;
    private List<DocInfoModel> docInfoList;
    public static final String ExamHistoryTitle = "検査";

    public ExamHistory(PatientInspector pi) {

        patientInspector = pi;
        context = pi.getContext();
        docHistory = pi.getDocumentHistory();

        docHistory.addPropertyChangeListener(DocumentHistory.HISTORY_UPDATED, new PropertyChangeListener() {

            // DocumentHistoryでhistory periodが変更されると、こっちでもupdateする
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateHistory();
            }
        });
        initComponent();
        connect();
    }

    /**
     * GUI コンポーネントを生成する。
     */
    private void initComponent() {

        view = new InspectorTablePanel();
        table = view.getTable();
        table.setFocusable(false);

        String[] columnNames = {"検査日", "内容"};
        String[] methodNames = {"getMmlExamDate", "getExamTitle"};
        Class[] columnClasses = {String.class, String.class};

        // 検査履歴テーブルを生成する
        tableModel = new ListTableModel(columnNames, 1, methodNames, columnClasses) {
            // テーブルは編集不可
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table.setModel(tableModel);
        // カラム幅を調整する
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        // ストライプテーブル
        StripeTableCellRenderer renderer = new StripeTableCellRenderer(table);
        renderer.setDefaultRenderer();

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    // レイアウトパネルを返す。
    public JPanel getPanel() {
        return view;
    }

    // 履歴テーブルのコレクションを clear する。
    public void clear() {
        tableModel.clear();
    }

    private void connect() {

        // 履歴テーブルで選択された行の文書を表示する
        ListSelectionModel slm = table.getSelectionModel();
        slm.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    int selectedRow = table.getSelectedRow();
                    // 行選択がないか範囲外なら戻る
                    if (selectedRow == -1 || selectedRow >= tableModel.getObjectCount()) {
                        return;
                    }
                    ExamHistoryModel eh = (ExamHistoryModel) tableModel.getObject(selectedRow);
                    long docPk = eh.getDocPk();

                    for (DocInfoModel dim : docInfoList) {
                        if (dim.getDocPk() == docPk) {
                            int index = docInfoList.indexOf(dim);
                            JTable docHistoryTable = ((DocumentHistoryView) docHistory.getPanel()).getTable();
                            int rows = docHistoryTable.getRowCount();
                            int autoFetchCount = docHistory.getAutoFetchCount();
                            int from;
                            int to;
                            if (docHistory.isAscending()) {
                                from = index;
                                to = index + autoFetchCount - 1;
                                to = (to > rows - 1) ? rows - 1 : to;
                            } else {
                                to = index;
                                from = index - autoFetchCount + 1;
                                from = (from < 0) ? 0 : from;
                            }
                            docHistoryTable.setRowSelectionInterval(from, to);
                            scrollToCenter(docHistoryTable, index, 0);
                            break;
                        }
                    }
                }
            }
        });
    }

    // http://www.exampledepot.com/egs/javax.swing.table/VisCenter.html
    private void scrollToCenter(JTable table, int rowIndex, int vColIndex) {
        if (!(table.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) table.getParent();

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);

        // The location of the view relative to the table
        Rectangle viewRect = viewport.getViewRect();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);

        // Scroll the area into view.
        viewport.scrollRectToVisible(rect);
    }

    @SuppressWarnings("unchecked")
    private void updateHistory() {
        
        // バックグラウンドで行う
        final long karteId = context.getKarte().getId();
        final Date fromDate = docHistory.getExtractionPeriod();
        final Date toDate = new Date();

        DocumentHistoryView dhView = (DocumentHistoryView) docHistory.getPanel();
        docInfoList = ((ListTableModel) dhView.getTable().getModel()).getDataProvider();

        final SimpleWorker worker = new SimpleWorker<List<ExamHistoryModel>, Void>() {

            @Override
            protected List<ExamHistoryModel> doInBackground() throws Exception {
                
                MasudaDelegater del = MasudaDelegater.getInstance();
                List<ExamHistoryModel> list = del.getExamHistory(karteId, fromDate, toDate);
                return list;
            }

            @Override
            protected void done() {
                try {
                    // テーブルモデルにセット
                    List<ExamHistoryModel> list = get();
                    if (list == null) {
                        tableModel.clear();
                        return;
                    }
                    boolean asc = docHistory.isAscending();
                    if (asc) {
                        Collections.sort(list, new ExamHistoryComparator());
                    } else {
                        Collections.sort(list, Collections.reverseOrder(new ExamHistoryComparator()));
                    }
                    Date lastExamDate = ModelUtils.AD1800;
                    if (!list.isEmpty()) {
                        lastExamDate = asc 
                                ? list.get(list.size() - 1).getExamDate()
                                : list.get(0).getExamDate();
                    }

                    int index = patientInspector.getTabbedPane().indexOfTab(ExamHistoryTitle);

                    // 最終検査から３か月経過していたらタブの文字を赤にする
                    GregorianCalendar gc = new GregorianCalendar();
                    gc.add(GregorianCalendar.MONTH, -3);
                    if (lastExamDate.before(gc.getTime())) {
                        patientInspector.getTabbedPane().setForegroundAt(index, Color.RED);
                    } else {
                        patientInspector.getTabbedPane().setForegroundAt(index, Color.BLACK);
                    }
                    tableModel.setDataProvider(list);

                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                }
            }
        };
        // ここは別スレッドで実行する
        //java.util.concurrent.Executors.newCachedThreadPool().execute(worker);
        worker.execute();
    }

    private class ExamHistoryComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            ExamHistoryModel e1 = (ExamHistoryModel) o1;
            ExamHistoryModel e2 = (ExamHistoryModel) o2;
            return e1.getExamDate().compareTo(e2.getExamDate());
        }
    }
}

