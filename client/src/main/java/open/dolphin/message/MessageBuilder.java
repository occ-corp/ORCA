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

        String text = sw.toString();

        return text;
    }
}
