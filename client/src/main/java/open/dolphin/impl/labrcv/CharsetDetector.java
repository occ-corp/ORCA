
package open.dolphin.impl.labrcv;

import java.io.FileInputStream;
import java.io.IOException;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *
 * @author masuda, Masuda Naika
 */
public class CharsetDetector {

    public static String getEncoding(String fileName) {
        
        final String SJIS = "SJIS";
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
        
        return ("UTF-8".equals(encoding)) ? encoding : SJIS;
    }
}
