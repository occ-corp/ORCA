package open.dolphin.client;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.SchemaModel;
import open.dolphin.util.XmlUtils;
import org.apache.log4j.Logger;

/**
 * KartePane の dumper
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author a little modified by masuda, Masuda Naika
 */
public final class KartePaneDumper_2 {
    
    private static final char DQ = '\"';
    private static final String FOREGROUND = "foreground";
    private static final String CONTENT = "content";
    private static final String NAME = "name";
    
    private List<ModuleModel> moduleList;
    private List<SchemaModel> schemaList;
    private String spec;
    private Logger logger;
    
    /** Creates a new instance of TextPaneDumpBuilder */
    public KartePaneDumper_2() {
        logger = ClientContext.getBootLogger();
        moduleList = new ArrayList<ModuleModel>();
        schemaList = new ArrayList<SchemaModel>();
    }
    
    /**
     * ダンプした Document の XML 定義を返す。
     *
     * @return Documentの内容を XML で表したもの
     */
    public String getSpec() {
        logger.debug(spec);
        return spec;
    }
    
    /**
     * ダンプした Documentに含まれている ModuleModelを返す。
     *
     * @return
     */
    public ModuleModel[] getModule() {

        if (!moduleList.isEmpty()) {
            return moduleList.toArray(new ModuleModel[moduleList.size()]);
        }
        return null;
    }
    
    /**
     * ダンプした Documentに含まれている SchemaModel を返す。
     *
     * @return
     */
    public SchemaModel[] getSchema() {

        if (!schemaList.isEmpty()) {
            return schemaList.toArray(new SchemaModel[schemaList.size()]);
        }
        return null;
    }
    
    /**
     * 引数の Document をダンプする。
     *
     * @param doc ダンプするドキュメント
     */
    public void dump(DefaultStyledDocument doc) {
        
        StringBuilder sb = new StringBuilder();

        try {
            // ルート要素から再帰的にダンプする
            Element root = doc.getDefaultRootElement();
            writeElemnt(root, sb);
            
            // 出力バッファーをフラッシュしペインのXML定義を生成する
            spec = sb.toString();
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * 要素を再帰的にダンプする。
     * @param element 要素
     * @param writer	出力ライター
     * @throws IOException
     * @throws BadLocationException
     */
    private void writeElemnt(Element element, StringBuilder sb)
            throws IOException, BadLocationException {
        
        // 要素の開始及び終了のオフセット値を保存する
        int start = element.getStartOffset();
        int end = element.getEndOffset();
        logger.debug("start = " + start);
        logger.debug("end = " + end);

        String elmName = element.getName();
        boolean contentFlg = CONTENT.equals(elmName);
        
        // このエレメントの属性セットを得る
        AttributeSet atts = element.getAttributes();
        
        // 属性値の文字列表現
        String asString = null;
        
        // 属性を調べる
        if (atts != null) {
            
            StringBuilder attrBuf = new StringBuilder();
            
            // 全ての属性を列挙する
            Enumeration names = atts.getAttributeNames();
            
            while (names.hasMoreElements()) {
                
                // 属性の名前を得る
                Object nextName = names.nextElement();
                String attrName = nextName.toString();
                
                if (nextName != StyleConstants.ResolveAttribute) {
                    
                    logger.debug("attribute name = " + attrName);
                    
                    // $enameは除外する
                    if (attrName.startsWith("$")) {
                        continue;
                    }
                    
                    // foreground 属性の場合は再構築の際に利用しやすい形に分解する
                    if (FOREGROUND.equals(attrName)) {
                        Color c = (Color) atts.getAttribute(StyleConstants.Foreground);
                        logger.debug("color = " + c.toString());
                        addAttribute(attrName, attrBuf);
                        attrBuf.append(DQ);
                        attrBuf.append(String.valueOf(c.getRed())).append(",");
                        attrBuf.append(String.valueOf(c.getGreen())).append(",");
                        attrBuf.append(String.valueOf(c.getBlue()));
                        attrBuf.append(DQ);
                        
                    } else {
                        // 属性セットから名前をキーにして属性オブジェクトを取得する
                        Object attObject = atts.getAttribute(nextName);
                        logger.debug("attribute object = " + attObject.toString());
                        
                        if (attObject instanceof StampHolder) {
                            // スタンプの場合
                            StampHolder sh = (StampHolder) attObject;
                            moduleList.add(sh.getStamp());
                            String value = String.valueOf(moduleList.size() - 1); // ペインに出現する順番をこの属性の値とする
                            addAttribute(attrName, attrBuf);
                            attrBuf.append(addQuote(value));
                            
                        } else if (attObject instanceof SchemaHolder) {
                            // シュェーマの場合
                            SchemaHolder ch = (SchemaHolder) attObject;
                            schemaList.add(ch.getSchema());
                            String value = String.valueOf(schemaList.size() - 1); // ペインに出現する順番をこの属性の値とする
                            addAttribute(attrName, attrBuf);
                            attrBuf.append(addQuote(value));
                            
                        } else {
                            // それ以外の属性についてはそのまま記録する
                            // <content start="1" end="2" name="stampHolder">となるのを防ぐ
                            if (!(contentFlg && NAME.equals(attrName))) {
                                addAttribute(attrName, attrBuf);
                                attrBuf.append(addQuote(attObject.toString()));
                            }
                        }
                    }
                }
            }
            asString = attrBuf.toString();
        }

        // <要素名 start="xx" end="xx" + asString>
        sb.append("<").append(elmName);
        sb.append(" start=").append(addQuote(start));
        sb.append(" end=").append(addQuote(end));
        if (asString != null && !asString.isEmpty()) {
            sb.append(asString);
        }
        sb.append(">");
        
        // content要素の場合はテキストを抽出する
        if (contentFlg) {
            int len = end - start;
            String text = element.getDocument().getText(start, len);
            logger.debug("text = " + text);

            // 特定の文字列を置換する
            text = XmlUtils.toXml(text);
            sb.append("<text>").append(text).append("</text>");
        }
        
        // 子要素について再帰する
        int children = element.getElementCount();
        for (int i = 0; i < children; i++) {
            writeElemnt(element.getElement(i), sb);
        }
        
        // この属性を終了する
        // </属性名>
        sb.append("</").append(elmName).append(">");
    }
    
    private void addAttribute(String attrName, StringBuilder sb) {
        sb.append(" ").append(attrName).append("=");
    }
    
    private String addQuote(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(DQ).append(str).append(DQ);
        return sb.toString();
    }
    
    private String addQuote(int num) {
        return addQuote(String.valueOf(num));
    }
}