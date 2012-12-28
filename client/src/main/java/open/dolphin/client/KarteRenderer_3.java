package open.dolphin.client;

import java.awt.Color;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.text.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.infomodel.*;

/**
 * KarteRenderer_2 改
 * 
 * STAX使ってみた、速くもならない。ボツ
 * @author masuda, Masuda Naika
 */
public class KarteRenderer_3 {
    
    private static final String COMPONENT_ELEMENT_NAME = "component";
    private static final String STAMP_HOLDER = "stampHolder";
    private static final String SCHEMA_HOLDER = "schemaHolder";
    private static final String ALIGNMENT_NAME = "Alignment";
    private static final String FOREGROUND_NAME = "foreground";
    private static final String SIZE_NAME = "size";
    private static final String BOLD_NAME = "bold";
    private static final String ITALIC_NAME = "italic";
    private static final String UNDERLINE_NAME = "underline";
    private static final String NAME_NAME = "name";
    private static final String LOGICAL_STYLE_NAME = "logicalStyle";
    private static final String[] REPLACES = new String[]{"<", ">", "&", "'", "\""};
    private static final String[] MATCHES = new String[]{"&lt;", "&gt;", "&amp;", "&apos;", "&quot;"};
    
    private static final String NAME_STAMP_HOLDER = "name=\"stampHolder\"";
    
    private enum ELEMENTS {paragraph, content, text, component, icon, kartePane, section, unknown}; 
    
    private static KarteRenderer_3 instance;
    
    static {
        instance = new KarteRenderer_3();
    }
    
    private KarteRenderer_3() {
    }
    
    public static KarteRenderer_3 getInstance() {
        return instance;
    }

    /**
     * DocumentModel をレンダリングする。
     *
     * @param model レンダリングする DocumentModel
     */
    public void render(DocumentModel model, KartePane soaPane, KartePane pPane) {

        List<ModuleModel> modules = model.getModules();

        // SOA と P のモジュールをわける
        // また夫々の Pane の spec を取得する
        List<ModuleModel> soaModules = new ArrayList<ModuleModel>();
        List<ModuleModel> pModules = new ArrayList<ModuleModel>();
        List<SchemaModel> schemas = model.getSchema();
        String soaSpec = null;
        String pSpec = null;

        for (ModuleModel bean : modules) {

            String role = bean.getModuleInfoBean().getStampRole();

            if (role.equals(IInfoModel.ROLE_SOA)) {
                soaModules.add(bean);
            } else if (role.equals(IInfoModel.ROLE_SOA_SPEC)) {
                soaSpec = ((ProgressCourse) bean.getModel()).getFreeText();
            } else if (role.equals(IInfoModel.ROLE_P)) {
                pModules.add(bean);
            } else if (role.equals(IInfoModel.ROLE_P_SPEC)) {
                pSpec = ((ProgressCourse) bean.getModel()).getFreeText();
            }
        }

        // 念のためソート
        Collections.sort(soaModules);
        Collections.sort(pModules);

        // この処理はなんだろう？ soaPaneにスタンプホルダ―？？？
        if (soaSpec != null && pSpec != null) {
            if (soaSpec.contains(NAME_STAMP_HOLDER)) {
                String sTmp = soaSpec;
                String pTmp = pSpec;
                soaSpec = pTmp;
                pSpec = sTmp;
            }
        }

        // SOA Pane をレンダリングする
        if (soaSpec == null || soaSpec.equals("")) {
            // soaにModuleModelはないはずだよね… あ、モディファイ版にはあるかもしれない…
            for (ModuleModel mm : soaModules) {
                soaPane.stamp(mm);
            }

        } else {
            //debug("Render SOA Pane");
            //debug("Module count = " + soaModules.size());
            new KartePaneRenderer().renderPane(soaSpec, soaModules, schemas, soaPane);
        }

        // P Pane をレンダリングする
        if (pSpec == null || pSpec.equals("")) {
            // 前回処方など適用
            for (ModuleModel mm : pModules) {
                pPane.stamp(mm);
            }
        } else {
            //debug("Render P Pane");
            //debug("Module count = " + pModules.size());
            new KartePaneRenderer().renderPane(pSpec, pModules, schemas, pPane);
            // StampHolder直後の改行がない場合は補う
            pPane.getDocument().fixCrAfterStamp();
        }
    }

    // クラスを分離した
    private class KartePaneRenderer {

        private KartePane kartePane;
        private boolean logicalStyle;
        private List<ModuleModel> modules;
        private List<SchemaModel> schemas;
        
        private String foreground;
        private String size;
        private String bold;
        private String italic;
        private String underline;

        /**
         * TextPane Dump の XML を解析する。
         *
         * @param xml TextPane Dump の XML
         */
        private void renderPane(String xml, List<ModuleModel> modules, List<SchemaModel> schemas, KartePane kartePane) {

            //debug(xml);
            
            this.modules = modules;
            this.schemas = schemas;
            this.kartePane = kartePane;

            XMLInputFactory factory = XMLInputFactory.newInstance();
            StringReader stream = null;
            XMLStreamReader reader = null;
            
            try {
                stream = new StringReader(xml);
                reader = factory.createXMLStreamReader(stream);
                
                while (reader.hasNext()) {
                    int eventType = reader.next();
                    switch (eventType) {
                        case XMLStreamReader.START_ELEMENT:
                            startElement(reader);
                            break;
                        case XMLStreamReader.END_ELEMENT:
                            endElement(reader);
                            break;
                    }
                }
                
            } catch (XMLStreamException ex) {
                //System.err.println(ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (XMLStreamException ex) {
                    }
                }
                if (stream != null) {
                    stream.close();
                }
            }
        }
        
        private ELEMENTS getValue(String eName) {
            ELEMENTS elm = ELEMENTS.unknown;
            try {
                elm = ELEMENTS.valueOf(eName);
            } catch (IllegalArgumentException ex) {
            }
            return elm;
        }

        private void startElement(XMLStreamReader reader) throws XMLStreamException {
            
            String eName = reader.getName().getLocalPart();
            ELEMENTS elm = getValue(eName);
            
            switch (elm) {
                case paragraph:
                    String lStyle = reader.getAttributeValue(null, LOGICAL_STYLE_NAME);
                    String alignStr = reader.getAttributeValue(null, ALIGNMENT_NAME);
                    startParagraph(lStyle, alignStr);
                    break;
                case content:
                    foreground = reader.getAttributeValue(null, FOREGROUND_NAME);
                    size = reader.getAttributeValue(null, SIZE_NAME);
                    bold = reader.getAttributeValue(null, BOLD_NAME);
                    italic = reader.getAttributeValue(null, ITALIC_NAME);
                    underline = reader.getAttributeValue(null, UNDERLINE_NAME);
                    break;
                case text:
                    String text = reader.getElementText();
                    startContent(foreground, size, bold, italic, underline, text);
                    break;
                case component:
                    String name = reader.getAttributeValue(null, NAME_NAME);
                    String number = reader.getAttributeValue(null, COMPONENT_ELEMENT_NAME);
                    startComponent(name, number);
                    break;
                case icon:
                    startIcon();
                    break;
                case kartePane:
                    startProgressCourse();
                    break;
                case section:
                    startSection();
                    break;
                default:
                    //debug("Other element:" + eName);
                    break;
            }
        }

        private void endElement(XMLStreamReader reader) {
            
            String eName = reader.getName().getLocalPart();
            ELEMENTS elm = getValue(eName);
            
            switch (elm) {
                case paragraph:
                    endParagraph();
                    break;
                case content:
                    endContent();
                    break;
                case component:
                    endComponent();
                    break;
                case icon:
                    endIcon();
                    break;
                case kartePane:
                    endProgressCourse();
                    break;
                case section:
                    endSection();
                    break;
                default:
                    //debug("Other element:" + eName);
                    break;
            }
        }

        private void startSection() {
        }

        private void endSection() {
        }

        private void startProgressCourse() {
        }

        private void endProgressCourse() {
        }

        private void startParagraph(String lStyle, String alignStr) {

            // if (lStyle != null) {
            kartePane.setLogicalStyle("default");
            logicalStyle = true;
            // }

            if (alignStr != null) {
                DefaultStyledDocument doc = (DefaultStyledDocument) kartePane.getTextPane().getDocument();
                Style style0 = doc.getStyle("default");
                Style style = doc.addStyle("alignment", style0);
                if (alignStr.equals("0")) {
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
                } else if (alignStr.equals("1")) {
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
                } else if (alignStr.equals("2")) {
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
                }
                kartePane.setLogicalStyle("alignment");
                logicalStyle = true;
            }
        }

        private void endParagraph() {
            //thePane.makeParagraph(); // trim() の廃止で廃止
            if (logicalStyle) {
                kartePane.clearLogicalStyle();
                logicalStyle = false;
            }
        }

        private void startContent(
                String foreground,
                String size,
                String bold,
                String italic,
                String underline,
                String text) {

            // 特殊文字を戻す
            for (int i = 0; i < REPLACES.length; i++) {
                text = text.replaceAll(MATCHES[i], REPLACES[i]);
            }

            // このコンテントに設定する AttributeSet
            MutableAttributeSet atts = new SimpleAttributeSet();

            // foreground 属性を設定する
            if (foreground != null) {
                StringTokenizer stk = new StringTokenizer(foreground, ",");
                if (stk.hasMoreTokens()) {
                    int r = Integer.parseInt(stk.nextToken());
                    int g = Integer.parseInt(stk.nextToken());
                    int b = Integer.parseInt(stk.nextToken());
                    StyleConstants.setForeground(atts, new Color(r, g, b));
                }
            }

            // size 属性を設定する
            if (size != null) {
                StyleConstants.setFontSize(atts, Integer.parseInt(size));
            }
            // bold 属性を設定する
            if (bold != null) {
                StyleConstants.setBold(atts, Boolean.valueOf(bold).booleanValue());
            }
            // italic 属性を設定する
            if (italic != null) {
                StyleConstants.setItalic(atts, Boolean.valueOf(italic).booleanValue());
            }
            // underline 属性を設定する
            if (underline != null) {
                StyleConstants.setUnderline(atts, Boolean.valueOf(underline).booleanValue());
            }

            // テキストを挿入する
            kartePane.insertFreeString(text, atts);
        }

        private void endContent() {
        }

        private void startComponent(String name, String number) {

            //debug("Entering startComponent");
            //debug("Name = " + name);
            //debug("Number = " + number);

            int index = Integer.valueOf(number);
            try {
                if (name != null && name.equals(STAMP_HOLDER)) {
                    ModuleModel stamp = modules.get(index);
                    kartePane.flowStamp(stamp);
                } else if (name != null && name.equals(SCHEMA_HOLDER)) {
                    kartePane.flowSchema(schemas.get(index));
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        private void endComponent() {
        }
        
        private void startIcon() {
        }

        private void endIcon() {
        }
    }

    //private void debug(String msg) {
    //    //ClientContext.getBootLogger().debug(msg);
    //}
}