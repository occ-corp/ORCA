package open.dolphin.impl.orcaapi;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.ClientContext;
import open.dolphin.client.KarteSenderResult;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

/**
 * ORCA APIのデレゲータ
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaApiDelegater implements IOrcaApi {
    
    private static final String CHARSET_UTF8 = "; charset=UTF-8";
    private static final String MEDIATYPE_XML_UTF8 = MediaType.APPLICATION_XML + CHARSET_UTF8;
    private static final int HTTP200 = 200;

    private static final OrcaApiDelegater instance;

    private boolean DEBUG;
    private XMLOutputter outputter;
    private SAXBuilder builder;
    
    private List<PhysicianInfo> physicianList;
    private List<DepartmentInfo> deptList;
    
    static {
        instance = new OrcaApiDelegater();
    }
    
    public static OrcaApiDelegater getInstance() {
        return instance;
    }
    
    private OrcaApiDelegater() {
        DEBUG = (ClientContext.getBootLogger().getLevel()==Level.DEBUG);
        outputter = new XMLOutputter();
        builder = new SAXBuilder();
    }
    
    public KarteSenderResult sendMedicalModModel(MedicalModModel model) {
        
        final String path = "/api21/medicalmod";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add(CLASS, "01");
        
        Document post = new Document(new OrcaApiElement.MedicalMod(model));
        String xml = outputter.outputString(post);
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_XML_UTF8)
                .type(MEDIATYPE_XML_UTF8)
                .post(ClientResponse.class, xml);
        
        int status = response.getStatus();
        String resXml = response.getEntity(String.class);
        debug(status, resXml);
        
        if (status != HTTP200) {
            String code = "HTTP" + String.valueOf(status);
            String msg = "接続を確認してください。";
            return new KarteSenderResult(KarteSenderResult.ORCA_API, code, msg);
        }
        
        KarteSenderResult result;
        try {
            Document res = builder.build(new StringReader(resXml));
            String code = getElementText(res, API_RESULT);
            String msg = getElementText(res, API_RESULT_MESSAGE);
            result = new KarteSenderResult(KarteSenderResult.ORCA_API, code, msg);
        } catch (JDOMException ex) {
            result = new KarteSenderResult(KarteSenderResult.ORCA_API, KarteSenderResult.ERROR, ex.getMessage());
        } catch (IOException ex) {
            result = new KarteSenderResult(KarteSenderResult.ORCA_API, KarteSenderResult.ERROR, ex.getMessage());
        }

        return result;
    }
    
    private void getDepartmentInfo() {
        
        final String path = "/api01r/system01lst";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add(CLASS, "01");
        
        String xml = createSystem01ManagereqXml();
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_XML_UTF8)
                .type(MEDIATYPE_XML_UTF8)
                .post(ClientResponse.class, xml);
        
        int status = response.getStatus();
        String resXml = response.getEntity(String.class);
        debug(status, resXml);
        
        if (status != HTTP200) {
            return;
        }
        
        try {
            Document res = builder.build(new StringReader(resXml));
            String resResultCode = getElementText(res, API_RESULT);
            if (!API_NO_ERROR.equals(resResultCode)) {
                deptList = new DepartmentResParser().getList(res);
            }
        } catch (JDOMException ex) {
        } catch (IOException ex) {
        }
    }
    
    private void getDepartmentInfo2() {
        
        final String path = "/api01rv2/system01lstv2";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add(CLASS, "01");
        
        String xml = createSystem01ManagereqXml2();
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_XML_UTF8)
                .type(MEDIATYPE_XML_UTF8)
                .post(ClientResponse.class, xml);
        
        int status = response.getStatus();
        String resXml = response.getEntity(String.class);
        debug(status, resXml);
        
        if (status != HTTP200) {
            return;
        }
        
        try {
            Document res = builder.build(new StringReader(resXml));
            String resResultCode = getElementText2(res, API_RESULT);
            if (!API_NO_ERROR.equals(resResultCode)) {
                deptList = new DepartmentResParser().getList2(res);
            }
        } catch (JDOMException ex) {
        } catch (IOException ex) {
        }
    }
    
    private void getPhysicianInfo() {

        final String path = "/api01r/system01lst";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add(CLASS, "02");

        String xml = createSystem01ManagereqXml();
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_XML_UTF8)
                .type(MEDIATYPE_XML_UTF8)
                .post(ClientResponse.class, xml);
        
        int status = response.getStatus();
        String resXml = response.getEntity(String.class);
        debug(status, resXml);
        
        if (status != HTTP200) {
            return;
        }
        
        try {
            Document res = builder.build(new StringReader(resXml));
            String resResultCode = getElementText(res, API_RESULT);
            if (!API_NO_ERROR.equals(resResultCode)) {
                physicianList = new PhysicianResParser().getList(res);
            }
        } catch (JDOMException ex) {
        } catch (IOException ex) {
        }
    }
    
    private void getPhysicianInfo2() {

        final String path = "/api01rv2/system01lstv2t";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add(CLASS, "02");

        String xml = createSystem01ManagereqXml2();
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_XML_UTF8)
                .type(MEDIATYPE_XML_UTF8)
                .post(ClientResponse.class, xml);
        
        int status = response.getStatus();
        String resXml = response.getEntity(String.class);
        debug(status, resXml);
        
        if (status != HTTP200) {
            return;
        }
        
        try {
            Document res = builder.build(new StringReader(resXml));
            String resResultCode = getElementText2(res, API_RESULT);
            if (!API_NO_ERROR.equals(resResultCode)) {
                physicianList = new PhysicianResParser().getList2(res);
            }
        } catch (JDOMException ex) {
        } catch (IOException ex) {
        }
    }
    
    private String createSystem01ManagereqXml() {
        
        final SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd");
        Element data = new Element(DATA);
        Element record1 = new Element(RECORD);
        data.addContent(record1);
        Element record2 = new Element(RECORD);
        record2.setAttribute(new Attribute(NAME, "system01_managereq"));
        record1.addContent(record2);
        Element string2 = new Element(STRING);
        string2.setAttribute(new Attribute(NAME, "Base_Date"));
        string2.addContent(frmt.format(new Date()));
        record2.addContent(string2);
        
        Document post = new Document(data);
        String xml = outputter.outputString(post);
        return xml;
    }
    
    private String createSystem01ManagereqXml2() {
        
        final SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd");
        Element data = new Element(DATA);
        Element elm1 = new Element("system01_managereq");
        elm1.setAttribute(TYPE, RECORD);
        Element elm2 = new Element(BASE_DATE);
        elm2.setAttribute(TYPE, STRING);
        elm2.addContent(frmt.format(new Date()));
        elm1.addContent(elm2);
        data.addContent(elm1);
        
        Document post = new Document(data);
        String xml = outputter.outputString(post);
        return xml;
    }

    private WebResource.Builder getResource(String path, MultivaluedMap<String, String> qmap) {
        return OrcaApiClient.getInstance().getResource(path, qmap);
    }
    
    /**
     * JDOM Document から，指定した attribute を持つ最初の Element を返す
     * @param doc
     * @param attr
     * @return 
     */
    private Element getElement(Document doc, String attr) {
        Element ret = null;
        
        Iterator iter = doc.getDescendants(new ElementFilter("string"));        
        while(iter.hasNext()) {
            
            Element e = (Element) iter.next();
            Attribute a = e.getAttribute("name");
            
            if (a != null && attr.equals(a.getValue())) {
                ret = e;
                break;
            }
        }
        return ret;
    }
    
    private String getElementText(Document doc, String attr) {
        
        Element elm = getElement(doc, attr);
        if (elm == null) {
            return null;
        }
        return elm.getText();
    }
    
    private String getElementText2(Document doc, String name) {
        Iterator itr = doc.getDescendants(new ElementFilter(name));
        while(itr.hasNext()) {
            Element elm = (Element) itr.next();
            return elm.getText();
        }
        return null;
    }
    
    private void debug(int status, String entity) {
        if (DEBUG) {
            Logger logger = ClientContext.getClaimLogger();
            logger.debug("---------------------------------------");
            logger.debug("status = " + status);
            logger.debug(entity);
        }
    }
}
