package open.dolphin.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 * Base64Utils
 * @author masuda, masuda Naika
 */
public class Base64Utils {
    
    public static String getBase64(String str) {
        return getBase64(str.getBytes());
    }
    
    public static String getBase64(byte[] bytes) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream os = MimeUtility.encode(baos, "base64");
            os.write(bytes);
            os.close();
            baos.flush();
            //String base64 = baos.toString().replace("\n", "").replace("\r", "");
            String base64 = baos.toString().replace("\n", "");
            return base64;
        } catch (MessagingException ex) {
        } catch (IOException ex) {
        }
        return null;
    }
}
