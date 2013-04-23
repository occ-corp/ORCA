package open.dolphin.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public class AgeCalculator {
    
    private static final String SAI = "歳";

    public static String getAgeAndBirthday(String mmlBirthday, int monthAge) {

        int[] spec = getAgeSpec(mmlBirthday);

        if (spec[0] != -1 && spec[1] != -1) {

            StringBuilder sb = new StringBuilder();
            sb.append(spec[0]);

            if (spec[0] < monthAge && spec[1] != 0) {
                sb.append(".").append(spec[1]);
            }

            sb.append(" ").append(SAI);
            sb.append(" (").append(mmlBirthday).append(")");
            return sb.toString();
        }
        return null;
    }

    public static String getAge(String mmlBirthday, int monthAge) {

        int[] spec = getAgeSpec(mmlBirthday);

        if (spec[0] != -1 && spec[1] != -1) {

            StringBuilder sb = new StringBuilder();
            sb.append(spec[0]);

            if (spec[0] < monthAge && spec[1] != 0) {
                sb.append(".").append(spec[1]);
            }
            return sb.toString();
        }
        return null;
    }

    public static int[] getAgeSpec(String mmlBirthday) {

        try {
            GregorianCalendar gc1 = getCalendar(mmlBirthday);
            GregorianCalendar gc2 = new GregorianCalendar(); // Today
            int years = 0;
            int month = 0;
            int days = 0;

            gc1.clear(Calendar.MILLISECOND);
            gc1.clear(Calendar.SECOND);
            gc1.clear(Calendar.MINUTE);
            gc1.clear(Calendar.HOUR_OF_DAY);

            gc2.clear(Calendar.MILLISECOND);
            gc2.clear(Calendar.SECOND);
            gc2.clear(Calendar.MINUTE);
            gc2.clear(Calendar.HOUR_OF_DAY);

            while (gc1.before(gc2)) {
                gc1.add(Calendar.YEAR, 1);
                years++;
            }

            gc1.add(Calendar.YEAR, -1);
            years--;

            while (gc1.before(gc2)) {
                gc1.add(Calendar.MONTH, 1);
                month++;
            }
            gc1.add(Calendar.MONTH, -1);
            month--;

            while (gc1.before(gc2)) {
                gc1.add(Calendar.DAY_OF_MONTH, 1);
                days++;
            }
            days--;

            return new int[]{years, month, days};

        } catch (Exception e) {
            e.printStackTrace(System.err);
            return new int[]{-1, -1, -1};
        }
    }

    public static GregorianCalendar getCalendar(String mmlDate) {

        try {
            // Trim time if contains
            mmlDate = trimTime(mmlDate);
            String[] cmp = mmlDate.split("-");
            String yearSt = cmp[0];
            String monthSt = cmp[1];
            if (monthSt.startsWith("0")) {
                monthSt = monthSt.substring(1);
            }
            String daySt = cmp[2];
            if (daySt.startsWith("0")) {
                daySt = daySt.substring(1);
            }
            int year = Integer.parseInt(yearSt);
            int month = Integer.parseInt(monthSt);
            month--;
            int day = Integer.parseInt(daySt);

            return new GregorianCalendar(year, month, day);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public static String trimTime(String mmlDate) {

        if (mmlDate != null) {
            int index = mmlDate.indexOf('T');
            if (index > -1) {
                return mmlDate.substring(0, index);
            } else {
                return mmlDate;
            }
        }
        return null;
    }
    
//masuda^   和暦を含めた生年月日
    public static String getAgeAndBirthday2(String mmlBirthday, int monthAge){

        String age = getAge(mmlBirthday, monthAge);
        if (age != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(age);
            sb.append(" ");
            sb.append(SAI);
            sb.append(" ([");
            sb.append(toNengo(mmlBirthday).substring(0, 3));
            sb.append("]");
            sb.append(mmlBirthday);
            sb.append(")");
            return sb.toString();
        }
        return null;
    }

    // 西暦=>年号変換
    public static String toNengo(String mmlBirthday) {
        GregorianCalendar gc = getCalendar(mmlBirthday);
        Locale locale = new Locale("ja","JP","JP"); 
        SimpleDateFormat frmt = new SimpleDateFormat("Gyy-MM-dd", locale);
        return frmt.format(gc.getTime());
    }
    
    public static String toNengoKanji(String mmlBirthday) {
        GregorianCalendar gc = getCalendar(mmlBirthday);
        Locale locale = new Locale("ja","JP","JP"); 
        SimpleDateFormat frmt = new SimpleDateFormat("GGGG y 年 M 月 d 日", locale);
        return frmt.format(gc.getTime());
    }

    // test(mmlDate形式)当時の年齢を計算する
    public static String getAge2(String mmlBirthday, String test) {

        try {
            GregorianCalendar gc1 = getCalendar(mmlBirthday);
            GregorianCalendar gc2 = getCalendar(test);
            int years = 0;

            gc1.clear(GregorianCalendar.MILLISECOND);
            gc1.clear(GregorianCalendar.SECOND);
            gc1.clear(GregorianCalendar.MINUTE);
            gc1.clear(GregorianCalendar.HOUR_OF_DAY);

            gc2.clear(GregorianCalendar.MILLISECOND);
            gc2.clear(GregorianCalendar.SECOND);
            gc2.clear(GregorianCalendar.MINUTE);
            gc2.clear(GregorianCalendar.HOUR_OF_DAY);

            while (gc1.before(gc2)) {
                gc1.add(GregorianCalendar.YEAR, 1);
                years++;
            }
            years--;

            int month = 12;

            while (gc1.after(gc2)) {
                gc1.add(GregorianCalendar.MONTH, -1);
                month--;
            }

            StringBuilder buf = new StringBuilder();
            buf.append(years);
            if (month != 0) {
                buf.append(".");
                buf.append(month);
            }
            return buf.toString();

        } catch (Exception e) {
            return null;
        }
    }
//masuda$
}
