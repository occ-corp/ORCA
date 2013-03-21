package open.dolphin.toucha;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;

/**
 * Templateというものをつかってみる
 * 
 * @author masuda, Masuda Naika
 */
public class TemplateLoader {
    
    private static final String ENCODING = "UTF-8";
    private static final String RESOURCE_BASE = "/";

    public Template newTemplate(String templateName) {
        
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        InputStream instream = this.getClass().getResourceAsStream(RESOURCE_BASE + templateName);

        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(instream, ENCODING);
            SimpleNode node = runtimeServices.parse(reader, templateName);
            Template template = new Template();
            template.setRuntimeServices(runtimeServices);
            template.setData(node);
            template.initDocument();
            return template;
            
        } catch (ParseException ex) {
        } catch (UnsupportedEncodingException ex) {
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
            }
            try {
                if (instream != null) {
                    instream.close();
                }
            } catch (IOException ex) {
            }
        }
        return null;
    }
}
