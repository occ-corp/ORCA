package open.dolphin.impl.orcaapi;

import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import open.dolphin.project.Project;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * Orca Api で診療内容送信
 * @author pns
 * @author modified by masuda, Masuda Naika
 */
public class OrcaApi {
    public static final String MEDICALMOD = "/api21/medicalmod";
    public static final String MEDICALMOD_ADD = MEDICALMOD + "?class=01";
    //public static final String MEDICALMOD_DELETE = MEDICALMOD + "?class=02";
    //public static final String MEDICALMOD_REPLACE = MEDICALMOD + "?class=03"; // 外来未対応

    private static OrcaApi orcaApi = new OrcaApi();
    
    private URI medicalModAdd;//, medicalModDelete, medicalModReplace;
    private SAXBuilder builder = new SAXBuilder();
    private XMLOutputter outputter = new XMLOutputter();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    
    private OrcaApi() {
        
        //URI 作成
        try {
            String host = Project.getString(Project.CLAIM_ADDRESS);
            String http = "http://" + host + ":8000";
            
            medicalModAdd = new URI(http + MEDICALMOD_ADD);
            //medicalModDelete = new URI(http + MEDICALMOD_DELETE);
            //medicalModReplace = new URI(http + MEDICALMOD_REPLACE);
            
        } catch (URISyntaxException ex) {
            System.out.println("OrcaApi.java: " + ex);
        }
        
        //Format format = outputter.getFormat();
        //format.setEncoding("UTF-8");
        //format.setLineSeparator("\n");
        //format.setIndent("  ");
        //outputter.setFormat(format);
    }
    
    /**
     * OrcaApi のインスタンスを返す
     * @return 
     */
    public static OrcaApi getInstance() {
        return orcaApi;
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
    
    /**
     * authenticate
     * ID，パスワード情報は途中で変わっている可能性があるので
     * 毎回 GET/POST の前に authenticate する
     */
    private void authenticate() {
        Authenticator.setDefault(new Authenticator(){
            @Override protected PasswordAuthentication getPasswordAuthentication(){
                return new PasswordAuthentication(
                        Project.getString(Project.ORCA_USER_ID), 
                        Project.getString(Project.ORCA_USER_PASSWORD).toCharArray());
            }
        });            
    }
    
    /**
     * URI に対して JDOM Document を POST して，レスポンスを JDOM Document として返す
     * @param uri
     * @param doc
     * @return 
     */
    public Document post(URI uri, Document doc) {
        Document responce = null;
        try {
            authenticate();
            URLConnection con = uri.toURL().openConnection();
            
            con.setDoOutput(true);
            outputter.output(doc, con.getOutputStream());
            responce = builder.build(con.getInputStream());
                        
        } catch (MalformedURLException ex) {
            System.out.println("OrcaApi.java: " + ex);
        } catch (IOException ex) {
            System.out.println("OrcaApi.java: " + ex);
            //ex.printStackTrace();
        } catch (JDOMException ex) {
            System.out.println("OrcaApi.java: " + ex);
            //ex.printStackTrace();
        }
        
        return responce;
    }
    
    /**
     * URI から GET する
     * @param uri
     * @return 
     */
    public Document get(URI uri) {
        Document responce = null;
        try {
            authenticate();
            URLConnection con = uri.toURL().openConnection();
            responce = builder.build(con.getInputStream());
                        
        } catch (MalformedURLException ex) {
            System.out.println("OrcaApi.java: " + ex);
        } catch (IOException ex) {
            System.out.println("OrcaApi.java: " + ex);
            //ex.printStackTrace();
        } catch (JDOMException ex) {
            System.out.println("OrcaApi.java: " + ex);
            //ex.printStackTrace();
        }
        
        return responce;
    }
        
    /**
     * 診療内容・病名を ORCA に送る
     * @param medicalModModel 
     */
     public void send(final MedicalModModel model) {

        Runnable r = new Runnable() {
            
            @Override
            public void run() {
                
                Document post = new Document(new OrcaApiElement.MedicalMod(model));
                Document responce = post(medicalModAdd, post);

                Element resultMessage = getElement(responce, "Api_Result_Message");

                if (resultMessage != null) {
                    String patientId = model.getContext().getPatient().getPatientId();
                    String message = resultMessage.getText();
                    // 病名だけ送った場合，登録対象のデータが無いと言われる
                    if ("登録対象のデータがありません".equals(message)) {
                        System.out.printf("[%s]病名 %s\n", patientId, "病名登録処理終了");
                    } else {
                        System.out.printf("[%s]病名 %s\n", patientId, message);
                    }
                }
                //System.out.printf("responce\n%s", outputter.outputString(responce));
            }
        };
        executor.submit(r);
    }
     
    public void dispose() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
        } catch (NullPointerException ex) {
        }
        executor = null;
    }
}
