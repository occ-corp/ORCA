package open.dolphin.impl.orcaapi;

import java.util.Iterator;
import open.dolphin.dao.SyskanriInfo;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

/**
 * AbstractOrcaApiParser
 * @author masuda, Masuda Naika
 */
public abstract class AbstractOrcaApiParser implements IOrcaApi {
    
    protected Document doc;
    protected boolean xml2;
    
    public AbstractOrcaApiParser(Document doc) {
        xml2 = SyskanriInfo.getInstance().isOrca47();
        this.doc = doc;
    }
    
    public String getApiResult() {
        
        final String name = "Api_Result";
        return xml2 
                ? getElementText2(name) 
                : getElementText(name);
    }
    
    public String getApiResultMessage() {
        
        final String name = "Api_Result_Message";
        return xml2 
                ? getElementText2(name) 
                : getElementText(name);
    }

    /**
     * JDOM Document から，指定した attribute を持つ最初の Element を返す
     * @param doc
     * @param attr
     * @return 
     */
    private Element getElement(String attr) {
        Element ret = null;
        
        Iterator iter = doc.getDescendants(new ElementFilter(STRING));        
        while(iter.hasNext()) {
            
            Element e = (Element) iter.next();
            Attribute a = e.getAttribute(NAME);
            
            if (a != null && attr.equals(a.getValue())) {
                ret = e;
                break;
            }
        }
        return ret;
    }
    
    protected String getElementText(String attr) {
        
        Element elm = getElement(attr);
        if (elm == null) {
            return null;
        }
        return elm.getText();
    }
    
    protected String getElementText2(String name) {
        
        Iterator itr = doc.getDescendants(new ElementFilter(name));
        while(itr.hasNext()) {
            Element elm = (Element) itr.next();
            return elm.getText();
        }
        return null;
    }
    
}
