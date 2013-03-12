package open.dolphin.util;

import java.awt.Dimension;
import java.io.StringReader;
import java.util.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.infomodel.*;

/**
 * KarteHtmlRenderer
 * @author masuda, Masuda Naika
 */
public class KarteHtmlRenderer {

    private static final String COMPONENT_ELEMENT_NAME = "component";
    private static final String STAMP_HOLDER = "stampHolder";
    private static final String SCHEMA_HOLDER = "schemaHolder";
    private static final String NAME_NAME = "name";
    
    private static final String CR = "\n";
    private static final String BR = "<BR>";
    private static final Dimension imageSize = new Dimension(240, 240);
    
    private enum ELEMENTS {paragraph, content, text, component, icon, kartePane, section, unknown};
    
    private static KarteHtmlRenderer instance;
    
    static {
        instance = new KarteHtmlRenderer();
    }
    
    private KarteHtmlRenderer() {
    }
    
    public static KarteHtmlRenderer getInstance() {
        return instance;
    }

    /**
     * DocumentModel をレンダリングする。
     *
     * @param model レンダリングする DocumentModel
     */
    public String[] render(DocumentModel model) {

        List<ModuleModel> modules = model.getModules();

        // SOA と P のモジュールをわける
        // また夫々の Pane の spec を取得する
        List<ModuleModel> soaModules = new ArrayList<ModuleModel>();
        List<ModuleModel> pModules = new ArrayList<ModuleModel>();
        List<SchemaModel> schemas = model.getSchema();
        String soaSpec = null;
        String pSpec = null;

        for (ModuleModel bean : modules) {
            
            bean.setModel((InfoModel) BeanUtils.xmlDecode(bean.getBeanBytes()));

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

        // SOA Pane をレンダリングする
        StringBuilder sb = new StringBuilder();
        new KartePaneRenderer_StAX().renderPane(soaSpec, soaModules, schemas, sb);
        String soaPane = sb.toString();

        // P Pane をレンダリングする
        sb = new StringBuilder();
        new KartePaneRenderer_StAX().renderPane(pSpec, pModules, schemas, sb);
        String pPane = sb.toString();
        
        return new String[]{soaPane, pPane};
    }

    private class KartePaneRenderer_StAX {

        private StringBuilder htmlBuff;
        private List<ModuleModel> modules;
        private List<SchemaModel> schemas;
        private boolean componentFlg;

        /**
         * TextPane Dump の XML を解析する。
         *
         * @param xml TextPane Dump の XML
         */
        private void renderPane(String xml, List<ModuleModel> modules, List<SchemaModel> schemas, StringBuilder sb) {
            
            this.modules = modules;
            this.schemas = schemas;
            this.htmlBuff = sb;
            htmlBuff.append("<HTML><BODY>");

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
                    }
                }
                
            } catch (XMLStreamException ex) {
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
            htmlBuff.append("</BODY></HTML>");
        }
        
        private ELEMENTS getValue(String eName) {
            try {
                return ELEMENTS.valueOf(eName);
            } catch (IllegalArgumentException ex) {
                return ELEMENTS.unknown;
            }
        }

        private void startElement(XMLStreamReader reader) throws XMLStreamException {
            
            String eName = reader.getName().getLocalPart();
            ELEMENTS elm = getValue(eName);
            
            switch (elm) {
                case text:
                    String text = reader.getElementText();
                    // Component直後の改行を消す
                    if (componentFlg && text.startsWith(CR)) {
                        text = text.substring(1);
                    }
                    componentFlg = false;
                    startContent(text);
                    break;
                case component:
                    componentFlg = true;
                    String name = reader.getAttributeValue(null, NAME_NAME);
                    String number = reader.getAttributeValue(null, COMPONENT_ELEMENT_NAME);
                    startComponent(name, number);
                    break;
                default:
                    break;
            }
        }

        private void startContent(String text) {

            // 特殊文字を戻す
            //text = XmlUtils.fromXml(text);
            text = text.replace(CR, BR);
            // テキストを挿入する
            htmlBuff.append(text);
        }
        
        private void startComponent(String name, String number) {

            int index = Integer.valueOf(number);
            
            if (name != null && name.equals(STAMP_HOLDER)) {
                ModuleModel stamp = modules.get(index);
                String str = StampRenderingHints.getInstance().getStampHtml(stamp);
                htmlBuff.append(str);
                
            } else if (name != null && name.equals(SCHEMA_HOLDER)) {
                SchemaModel schema = schemas.get(index);
                byte[] bytes = ImageTool.getJpegBytes(schema.getJpegByte(), imageSize);
                if (bytes != null) {
                    String base64 = Base64Utils.getBase64(bytes);
                    htmlBuff.append("<img src=\"data:image/jpeg;base64,\n");
                    htmlBuff.append(base64);
                    htmlBuff.append("\" alt=\"img\">");
                }
            }
        }
    }
}