package open.dolphin.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import open.dolphin.client.ClientContext;
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

    public static Template newTemplate(String templateName) {
        
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        InputStream instream = ClientContext.getTemplateAsStream(templateName);

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
                reader.close();
            } catch (IOException ex) {
            }
            try {
                instream.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }
}
