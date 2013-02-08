package open.dolphin.message;

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
        claimTemplate = TemplateLoader.newTemplate("claimHelper.vm");
        diseaseTemplate = TemplateLoader.newTemplate("diseaseHelper.vm");
    }

    public String build(IMessageHelper helper) {
        
        logger.debug("MessageBuilder build");
        
        String name = helper.getTemplateName();
        Template template = null;
        if ("claimHelper".equals(name)) {
            template = claimTemplate;
        } else if ("diseaseHelper".equals(name)) {
            template = diseaseTemplate;
        }
        
        VelocityContext context = new VelocityContext();
        context.put(name, helper);
        
        StringWriter sw = new StringWriter();
        template.merge(context, sw);

        String text = sw.toString();

        return text;
    }
}
