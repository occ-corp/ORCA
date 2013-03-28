package open.dolphin.delegater;

import com.sun.jersey.api.client.AsyncWebResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.util.HashUtil;

/**
 * JerseyClient
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class JerseyClient {

    private static final JerseyClient instance;
    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";
    private static final String CLIENT_UUID = "clientUUID";
    private static final int TIMEOUT1 = 30;
    
    private String clientUUID;
    private String baseURI;
    private String userName;
    private String password;
    
    private Client client;
    private Client client2;
    private WebResource webResource;
    private AsyncWebResource asyncResource;
    
    static {
        instance = new JerseyClient();
    }

    private JerseyClient() {
        clientUUID = Dolphin.getInstance().getClientUUID();
        client = Client.create();
        client2 = Client.create();
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
        client.setReadTimeout(readTimeout);
        webResource = client.resource(baseURI);

        // pvt同期用のクライアントを別に用意する
        asyncResource = client2.asyncResource(baseURI);
    }

    // QueryParam付のWebResource
    public WebResource.Builder getResource(String path, MultivaluedMap<String, String> qmap) {

        if (qmap != null) {
            return webResource.path(path).queryParams(qmap)
                    .header(USER_NAME, userName).header(PASSWORD, password);
        } else {
            return webResource.path(path)
                    .header(USER_NAME, userName).header(PASSWORD, password);
        }
    }
    
    // pvt同期用のクライアント

    public AsyncWebResource.Builder getAsyncResource(String path) {
        return asyncResource.path(path)
                .header(USER_NAME, userName)
                .header(PASSWORD, password)
                .header(CLIENT_UUID, clientUUID);
    }
}
