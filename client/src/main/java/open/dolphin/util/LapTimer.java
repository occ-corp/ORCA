
package open.dolphin.util;

/**
 * LapTimer
 * 
 * @author masuda, Masuda Naika
 */
public class LapTimer {

    private long sTime;
    private StringBuilder sb;
    
    public LapTimer() {
        sTime = System.currentTimeMillis();
        sb = new StringBuilder();
        sb.append("Lap timer started at ").append(sTime).append("\n");
    }

    public void lap(String msg) {
        long t = System.currentTimeMillis();
        sb.append(msg).append(" at ").append(t);
        sb.append(" (").append(t - sTime).append(")\n");
    }
    
    public void stop() {
        long t = System.currentTimeMillis();
        sb.append("Lap timer stopped at").append(t);;
        sb.append(" (").append(t - sTime).append(")\n");
        System.out.println(sb.toString());

    }
}
