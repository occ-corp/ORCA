package open.dolphin.impl.orcaapi;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.ClientContext;
import open.dolphin.client.KarteSenderResult;
import open.dolphin.dao.SyskanriInfo;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
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
    private static final String ORCA_API = "ORCA API";

    private static final OrcaApiDelegater instance;

    private boolean DEBUG;
    private XMLOutputter outputter;
    private SAXBuilder builder;
    
    private List<PhysicianInfo> physicianList;
    private List<DepartmentInfo> deptList;
    
    private boolean xml2;
    
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
        xml2 = SyskanriInfo.getInstance().isOrca47();
    }
    
    public KarteSenderResult sendMedicalModModel(MedicalModModel model) {
        
        final String path = xml2
                ? "/api21/medicalmodv2"
                : "/api21/medicalmod";
        
        final Document post = xml2
                ? new Document(new OrcaApiElement2.MedicalMod(model))
                : new Document(new OrcaApiElement.MedicalMod(model));
        final String xml = outputter.outputString(post);

        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add(CLASS, "01");

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
            return new KarteSenderResult(ORCA_API, code, msg);
        }
        
        KarteSenderResult result;
        try {
            Document res = builder.build(new StringReader(resXml));
            MedicalreqResParser parser = new MedicalreqResParser(res);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            result = new KarteSenderResult(ORCA_API, code, msg);
        } catch (JDOMException ex) {
            result = new KarteSenderResult(ORCA_API, KarteSenderResult.ERROR, ex.getMessage());
        } catch (IOException ex) {
            result = new KarteSenderResult(ORCA_API, KarteSenderResult.ERROR, ex.getMessage());
        }

        return result;
    }
    
    private void getDepartmentInfo() {
        
        final String path = xml2
                ? "/api01rv2/system01lstv2"
                : "/api01r/system01lst";
        
        final String xml = xml2
                ? createSystem01ManagereqXml2()
                : createSystem01ManagereqXml();
        
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add(CLASS, "01");
        
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
            DepartmentResParser parser = new DepartmentResParser(res);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            if (!API_NO_ERROR.equals(code)) {
                deptList = parser.getList();
            }
        } catch (JDOMException ex) {
        } catch (IOException ex) {
        }
    }

    private void getPhysicianInfo() {

        final String path = xml2
                ? "/api01rv2/system01lstv2t"
                : "/api01r/system01lst";
        
        final String xml = xml2
                ? createSystem01ManagereqXml2()
                : createSystem01ManagereqXml();
        
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add(CLASS, "02");

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
            PhysicianResParser parser = new PhysicianResParser(res);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            if (!API_NO_ERROR.equals(code)) {
                physicianList = parser.getList();
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
    
    private void debug(int status, String entity) {
        if (DEBUG) {
            Logger logger = ClientContext.getClaimLogger();
            logger.debug("---------------------------------------");
            logger.debug("status = " + status);
            logger.debug(entity);
        }
    }
}
