package open.dolphin.message;

import java.io.*;
import open.dolphin.client.ClientContext;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * DML を 任意のMessage に翻訳するクラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 *
 */
public final class MessageBuilder {
    //public class MessageBuilder implements IMessageBuilder {
    
//masuda^   UTF-8に変更
    //private static final String ENCODING = "SHIFT_JIS";
    private static final String ENCODING = "UTF-8";
//masuda$
    
    /** テンプレートファイル */
    private String templateFile;
    
    /** テンプレートファイルのエンコーディング */
    private String encoding = ENCODING;
    
    private Logger logger;
    
    public MessageBuilder() {
        logger = ClientContext.getBootLogger();
        logger.debug("MessageBuilder constracted");
    }
    
    public String getTemplateFile() {
        return templateFile;
    }
    
    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    //@Override
//    public String build(String dml) {
//
//        String ret = null;
//
//        try {
//            // Document root をVelocity 変数にセットする
//            SAXBuilder sbuilder = new SAXBuilder();
//            Document root = sbuilder.build(new BufferedReader(new StringReader(dml)));
//            VelocityContext context = ClientContext.getVelocityContext();
//            context.put("root", root);
//
//            // Merge する
//            StringWriter sw = new StringWriter();
//            BufferedWriter bw = new BufferedWriter(sw);
//            InputStream instream = ClientContext.getTemplateAsStream(templateFile);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, encoding));
//            Velocity.evaluate(context, bw, "MessageBuilder", reader);
//            bw.flush();
//            bw.close();
//
//            ret = sw.toString();
//
//        } catch (Exception e) {
//            e.printStackTrace(System.err);
//        }
//
//        return ret;
//    }
    
    public String build(Object helper) {
        
        logger.debug("MessageBuilder build");
        
        String ret = null;
        String name = helper.getClass().getName();
        int index = name.lastIndexOf('.');
        name = name.substring(index + 1);
        StringBuilder sb = new StringBuilder();
        sb.append(name.substring(0, 1).toLowerCase());
        sb.append(name.substring(1));
        name = sb.toString();

        try {
            logger.debug("MessageBuilder try");
            VelocityContext context = ClientContext.getVelocityContext();
            context.put(name, helper);
            
            // このスタンプのテンプレートファイルを得る
//masuda^   デフォルト診療科を使うかはhelper側で処理する
            String tFile = name + ".vm";
/*
            String tFile;
            if (Project.getBoolean(Project.CLAIM_01)) {
                tFile = name + "_01.vm";
            } else {
                tFile = name + ".vm";
            }
*/
//masuda$
            logger.debug("template file = " + tFile);
            
            // Merge する
            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);
            InputStream instream = ClientContext.getTemplateAsStream(tFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, encoding));
            Velocity.evaluate(context, bw, name, reader);
            logger.debug("Velocity.evaluated");
            bw.flush();
            bw.close();
            reader.close();
            
            ret = sw.toString();
            
        } catch (Exception e) {
            logger.warn(e);
        }
        
        return ret;
    }
}
