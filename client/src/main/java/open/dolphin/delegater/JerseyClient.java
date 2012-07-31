package open.dolphin.delegater;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.util.HashUtil;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 */
public class JerseyClient {

    private static final JerseyClient instance = new JerseyClient();
    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";
    private String baseURI;
    private WebResource webResource;
    private String userName;
    private String password;
    
    private WebResource webResource2;
    
//masuda^   timeoutをのばす
    private static final int TIMEOUT1 = 30;
    private static final int TIMEOUT2 = 60;
//masuda$

    private JerseyClient() {
    }

    public static JerseyClient getInstance() {
        return instance;
    }

    public void setUpAuthentication(String username, String password, boolean hashPass) {
        try {
            this.userName = username;
            this.password = hashPass ? password : HashUtil.MD5(password);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String uri) {

        String oldURI = baseURI;
        baseURI = uri;

        if (baseURI == null || baseURI.equals(oldURI)) {
            return;
        }
        
//masuda^
        //int readTimeout = Project.getInt("jersey.read.timeout") * 1000;
        int readTimeout = TIMEOUT1 * 1000;
//masuda$
        
        Client client = Client.create();
        client.setReadTimeout(readTimeout);
        webResource = client.resource(baseURI);

//masuda^   pvt同期用のクライアントを別に用意する
        Client client2 = Client.create();
        client2.setReadTimeout(TIMEOUT2 * 1000);
        webResource2 = client2.resource(baseURI);
//masuda$
    }

    public WebResource.Builder getResource(String path) {
        return webResource.path(path).header(USER_NAME, userName).header(PASSWORD, password);
    }
    
//masuda^
    // QueryParam付のWebResource
    public WebResource.Builder getQueryResource(String path, MultivaluedMap<String, String> qmap) {
        webResource.path(path).queryParams(qmap).header(USER_NAME, userName).header(PASSWORD, password).toString();
        return webResource.path(path).queryParams(qmap).header(USER_NAME, userName).header(PASSWORD, password);
    }
    
    // pvt同期用のクライアントを別に用意する
    public WebResource.Builder getAsyncResource(String path) {
        return webResource2.path(path).header(USER_NAME, userName).header(PASSWORD, password);
    }
    
    public String getUserName() {
        return userName;
    }
    public String getPassword() {
        return password;
    }
//masuda$
}
