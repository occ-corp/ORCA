package open.dolphin.delegater;

import com.sun.jersey.api.client.AsyncWebResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
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
        ClientConfig config = new DefaultClientConfig();
        setupOreOreSSL(config);

        client = Client.create(config);
        client2 = Client.create(config);
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
        boolean useSSL = Project.getBoolean(MiscSettingPanel.USE_SSL, MiscSettingPanel.DEFAULT_USE_SSL);
        if (useSSL) {
            baseURI = baseURI.replace("http", "https").replace(":8080", ":8443");
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
    
    // オレオレSSL復活ｗ
    private void setupOreOreSSL(ClientConfig config) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            TrustManager[] certs = {new OreOreTrustManager()};
            ctx.init(null, certs, new SecureRandom());
            HostnameVerifier verifier = new OreOreHostnameVerifier();
            HTTPSProperties prop = new HTTPSProperties(verifier, ctx);
            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, prop);
        } catch (NoSuchAlgorithmException ex) {
        } catch (KeyManagementException ex) {
        }
    }

    private class OreOreHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String string, SSLSession ssls) {
            return true;
        }
        
    }
    private class OreOreTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
