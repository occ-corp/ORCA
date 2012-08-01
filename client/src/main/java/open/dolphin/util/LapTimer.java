
package open.dolphin.util;

/**
 * LapTimer
 * 
 * @author masuda, Masuda Naika
 */
public class LapTimer {

    private long sTime;

    public  void start() {
        sTime = System.currentTimeMillis();
    }

    public void stop(String msg) {
        long eTime = System.currentTimeMillis();
        System.out.println(msg + " in msec :" + String.valueOf(eTime - sTime));
    }
}
