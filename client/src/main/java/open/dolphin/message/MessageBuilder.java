package open.dolphin.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import open.dolphin.client.ClientContext;
import open.dolphin.util.TemplateLoader;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

/**
 * DML を 任意のMessage に翻訳するクラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class MessageBuilder {
    
    private static MessageBuilder instance;

    private Logger logger;
    
    private Template claimTemplate;
    private Template diseaseTemplate;
    
    static {
        instance = new MessageBuilder();
    }
    
    private MessageBuilder() {
        logger = ClientContext.getBootLogger();
        logger.debug("MessageBuilder constracted");
        prepareTemplates();
    }
    
    public static MessageBuilder getInstance() {
        return instance;
    }
    
    private void prepareTemplates() {
        TemplateLoader templateLoader = new TemplateLoader();
        claimTemplate = templateLoader.newTemplate("claimHelper.vm");
        diseaseTemplate = templateLoader.newTemplate("diseaseHelper.vm");
    }

    public String build(IMessageHelper helper) {
        
        logger.debug("MessageBuilder build");
        
        Template template = null;
        VelocityContext context = new VelocityContext();
        
        if (helper instanceof ClaimHelper) {
            template = claimTemplate;
            context.put("claimHelper", helper);
        } else if (helper instanceof DiseaseHelper) {
            template = diseaseTemplate;
            context.put("diseaseHelper", helper);
        }
        
        StringWriter sw = new StringWriter();
        template.merge(context, sw);
        
        // 余分な空白を除去する
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new StringReader(sw.toString()));
            String str;
            while((str = br.readLine()) != null) {
                sb.append(str.trim()).append("\n");
            }
        } catch (IOException ex) {
        }

        String text = sb.toString();

        return text;
    }
}
