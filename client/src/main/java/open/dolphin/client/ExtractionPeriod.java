
package open.dolphin.client;

import java.util.Date;
import java.util.GregorianCalendar;
import open.dolphin.infomodel.IInfoModel;

/**
 *
 * @author masuda, Masuda Naika
 */
public class ExtractionPeriod {

    private String name;
    private int fromMonth;
    private int toMonth;

    public ExtractionPeriod(String name, int fromMonth, int toMonth) {
        this.name = name;
        this.fromMonth = fromMonth;
        this.toMonth = toMonth;
    }

    public String getName() {
        return name;
    }
    public int getFromMonth() {
        return fromMonth;
    }
    public int getToMonth() {
        return toMonth;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public Date getFromDate() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.add(GregorianCalendar.MONTH, fromMonth);
        gc.set(GregorianCalendar.DATE, 1);
        gc.set(GregorianCalendar.HOUR_OF_DAY, 0);
        gc.clear(GregorianCalendar.MINUTE);
        gc.clear(GregorianCalendar.SECOND);
        gc.clear(GregorianCalendar.MILLISECOND);
        if (gc.getTime().before(IInfoModel.AD1800)) {
            gc.setTime(IInfoModel.AD1800);
        }
        return gc.getTime();
    }

    public Date getToDate() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.add(GregorianCalendar.MONTH, toMonth);
        gc.set(GregorianCalendar.DATE, 1);
        gc.set(GregorianCalendar.HOUR_OF_DAY, 0);
        gc.clear(GregorianCalendar.MINUTE);
        gc.clear(GregorianCalendar.SECOND);
        gc.clear(GregorianCalendar.MILLISECOND);
        return gc.getTime();
    }
    
    public static int getIndex(String name, ExtractionPeriod[] periods) {
        try {
            for (int i = 0; i < periods.length; ++i) {
                if (name != null && name.equals(periods[i].getName())) {
                    return i;
                }
            }
        } catch (Exception ex) {
        }
        return 0;
    }
    
    public static int getFromDateIndex(int value, ExtractionPeriod[] periods) {
        try {
            for (int i = 0; i < periods.length; ++i) {
                if (value == periods[i].getFromMonth()) {
                    return i;
                }
            }
        } catch (Exception ex) {
        }
        return 0;
    }
    
    public static int getAppropriateIndex(Date date, ExtractionPeriod[] periods) {
        try {
            for (int i = 0; i < periods.length; ++i) {
                Date fromDate = periods[i].getFromDate();
                Date toDate = periods[i].getToDate();
                if (date.after(fromDate) && toDate.after(date)) {
                    return i;
                }
            }
            return periods.length - 1;
        } catch (Exception ex) {
        }
        return 0;
    }
}
