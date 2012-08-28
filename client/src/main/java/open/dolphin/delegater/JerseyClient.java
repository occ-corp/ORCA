package open.dolphin.delegater;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.util.HashUtil;

/**
 * JerseyClient
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class JerseyClient {

    private static final JerseyClient instance = new JerseyClient();
    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";
    private String baseURI;
    private String userName;
    private String password;
    
    private WebResource webResource;
    private WebResource webResource2;

    private static final int TIMEOUT1 = 30;

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

        int readTimeout = TIMEOUT1 * 1000;
        
        Client client = Client.create();
        client.setReadTimeout(readTimeout);
        webResource = client.resource(baseURI);

        // pvt同期用のクライアントを別に用意する
        Client client2 = Client.create();
        webResource2 = client2.resource(baseURI);
    }

    // QueryParam付のWebResource
    public WebResource.Builder getResource(String path, MultivaluedMap<String, String> qmap) {
        if (qmap != null) {
            return webResource.path(path).queryParams(qmap).header(USER_NAME, userName).header(PASSWORD, password);
        }
        return webResource.path(path).header(USER_NAME, userName).header(PASSWORD, password);
    }
    
    // pvt同期用のクライアント
    public WebResource.Builder getAsyncResource(String path) {
        return webResource2.path(path).header(USER_NAME, userName).header(PASSWORD, password);
    }
}
