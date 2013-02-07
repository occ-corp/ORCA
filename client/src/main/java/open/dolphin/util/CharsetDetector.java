package open.dolphin.util;

import java.io.FileInputStream;
import java.io.IOException;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *
 * @author masuda, Masuda Naika
 */
public class CharsetDetector {
    
    public static final String SJIS = "Shift_JIS";
    public static final String JIS = "ISO-2022-JP";
    public static final String UTF8 = "UTF-8";
    public static final String LATIN = "ISO-8859-1";

    public static String getFileEncoding(String fileName) {
        
        String encoding = null;
        FileInputStream fis = null;
        try {
            byte[] buf = new byte[4096];
            fis = new FileInputStream(fileName);
            UniversalDetector detector = new UniversalDetector(null);
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();
            encoding = detector.getDetectedCharset();
            detector.reset();
        } catch (IOException ex) {
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
            }
        }
        return encoding;
    }
    
    public static String getStringEncoding(byte[] bytes) {

        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();
        
        return encoding;
    }
}
