package open.dolphin.client;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import javax.swing.table.AbstractTableModel;
import open.dolphin.infomodel.SimpleDate;


/**
 * CalendarTableModel
 *
 * @author Kazushi Minagawa Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class CalendarTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
        "日", "月", "火", "水", "木", "金", "土"
    };
    private String[] columnNames = COLUMN_NAMES;
    private Object[][] data;
    private Collection markDates;
    private int year;
    private int month;
    private int startDay;
    private int firstCell;
    private int lastCell;
    private int numCols = columnNames.length;
    private int numRows;
    private int numDaysOfMonth;

    private GregorianCalendar startDate;
//masuda^ ３か月表示のときに利用
    private int realNumRows;
    public int getRealNumRows() {
        return realNumRows;
    }
//masuda$

    /**
     * CalendarTableModel を生成する。
     * @param year   カレンダの年
     * @param month　 カレンダの月
     */
    public CalendarTableModel(int year, int month) {

        this.year = year;
        this.month = month;

        // 作成する月の最初の日  yyyyMM1
        GregorianCalendar gc = new GregorianCalendar(year, month, 1);

        // 最初の日は週の何日目か
        // 1=SUN 6=SAT
        firstCell = gc.get(Calendar.DAY_OF_WEEK);
        firstCell--;  // table のセル番号へ変換する

        // この月の日数を得る
        numDaysOfMonth = gc.getActualMaximum(Calendar.DAY_OF_MONTH);

        // その月の最後の日を求める 1日 + （日数-1）
        gc.add(Calendar.DAY_OF_MONTH, numDaysOfMonth - 1);

        // 最後の日はその月の何週目か
        numRows = gc.get(Calendar.WEEK_OF_MONTH);

        // それは週の何日目か
        lastCell = gc.get(Calendar.DAY_OF_WEEK);
        lastCell--;

        // １次元のセル番号へ変換する
        lastCell += (numRows-1)*numCols; // table のセル番号へ変換する

        // このカレンダの表示開始日を求める
        // 一度一日に戻し、それからさらにカラム番号分の日数を引く
        gc.add(Calendar.DAY_OF_MONTH, 1 - numDaysOfMonth);
        gc.add(Calendar.DAY_OF_MONTH, -firstCell);
        startDate = (GregorianCalendar) gc.clone();

        startDay = gc.get(Calendar.DAY_OF_MONTH);
//masuda^   面倒なのでいつでも６週分表示する。本当の行数は保存しておく
        realNumRows = numRows;
        numRows = 6;
//masuda$
        // 空のデータ配列
        data = new Object[numRows][numCols];

    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public int getRowCount() {
        return numRows;
    }

    @Override
    public int getColumnCount() {
        return numCols;
    }

    @Override
    public Object getValueAt(int row, int col) {

        // Cell 番号を得る
        int cellNumber = row*numCols + col;
        Object ret = null;
//masuda^
        ret = data[row][col];
        // SimpleDateが設定されてなかったら
        if (ret == null) {
            // 先月か
            if (cellNumber < firstCell) {
                ret = String.valueOf(startDay + cellNumber);
            // 来月か
            } else if (cellNumber > lastCell) {
                ret = String.valueOf(cellNumber - lastCell);
            // 当月
            } else {
                ret = String.valueOf(1 + cellNumber - firstCell);
            }
        }
//masuda$
        return ret;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
//masuda^
        /*
        int cellNumber = row*numCols + col;

        // 先月または来月の時は何もしない
        if ( (cellNumber < firstCell) || (cellNumber > lastCell) ) {
            return;
        }
        */
//masuda$
        // 当月の場合はそれを単純に設定する
        data[row][col] = value;
    }

    public void setMarkDates(Collection c) {

        this.markDates = c;
        clear();
        if (markDates != null) {
            Iterator iter = markDates.iterator();
            SimpleDate date = null;

            while (iter.hasNext()) {
                date = (SimpleDate)iter.next();
//masuda^
                int monthDiff = date.getYear() * 12 + date.getMonth() - (year * 12 + month);
                int cellNumber = 0;

                if (monthDiff == 0) {
                    cellNumber = firstCell + (date.getDay() - 1);
                } else if (monthDiff == -1) {
                    GregorianCalendar gc = new GregorianCalendar(date.getYear(), date.getMonth(), 1);
                    int lastLastDayOfMonth = gc.getActualMaximum(Calendar.DAY_OF_MONTH);
                    cellNumber = firstCell - (lastLastDayOfMonth - date.getDay()) - 1;
                } else if (monthDiff == 1) {
                    cellNumber = lastCell + date.getDay();
                } else {
                    continue;
                }

                int row = cellNumber / numCols;
                int col = cellNumber % numCols;
                if (row >= 0 && row < numRows && col >= 0 && col < numCols) {
                    setValueAt(date, row, col);
                }
//masuda$
            }
        }
        this.fireTableDataChanged();
    }

    public Collection getMarkDates() {
        return markDates;
    }

    public void clear() {
        data = new Object[numRows][numCols];
    }

    public boolean isOutOfMonth(int row, int col) {
        int cellNumber = row*numCols + col;
        return ((cellNumber < firstCell) || (cellNumber > lastCell));
    }

    public SimpleDate getFirstDate() {
        return new SimpleDate(year, month, 1);
    }

    public SimpleDate getLastDate() {
        return new SimpleDate(year, month, numDaysOfMonth);
    }

    public SimpleDate getDate(int row, int col) {
        int cellNumber = row*numCols + col;
        GregorianCalendar gc = (GregorianCalendar) startDate.clone();
        gc.add(Calendar.DAY_OF_MONTH, cellNumber);
        int y = gc.get(Calendar.YEAR);
        int m = gc.get(Calendar.MONTH);
        int d = gc.get(Calendar.DAY_OF_MONTH);
        return new SimpleDate(y, m, d);
    }
}
