package open.dolphin.delegater;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.util.HashUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;

/**
 * RESTEasyClient
 * @author masuda, Masuda Naika
 */
public class RESTEasyClient {

    private String clientUUID;
    private String baseURI;
    private String userName;
    private String password;
    private String facilityId;
    
    private SSLSocketFactory sslSocketFactory;
    
    private static final RESTEasyClient instance;
    
    static {
        instance = new RESTEasyClient();
    }

    private RESTEasyClient() {
        clientUUID = Dolphin.getInstance().getClientUUID();
        try {
            setupOreOreSSL();
        } catch (NoSuchAlgorithmException ex) {
        } catch (KeyManagementException ex) {
        }
    }

    public static RESTEasyClient getInstance() {
        return instance;
    }

    public void setUpAuthentication(String username, String password, boolean hashPass) {

        String[] fidUid = splitFidUid(username);
        this.facilityId = fidUid[0];
        this.userName = fidUid[1];
        this.password = hashPass ? password : HashUtil.MD5(password);
    }
    
    private String[] splitFidUid(String username) {
        int pos = username.indexOf(IInfoModel.COMPOSITE_KEY_MAKER);
        String fid = username.substring(0, pos);
        String uid = username.substring(pos + 1);
        return new String[]{fid, uid};
    }
    
    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String uri) {
        // 手抜きｗ
        uri = uri.replace("http", "https").replace(":8080", ":8443");
        baseURI = uri;
    }
    
    public String getPath(String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseURI);
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path);
        return sb.toString();
    }
    
    public ClientRequest getClientRequest(String path, MultivaluedMap<String, String> qmap) {

        ClientRequest request = getClientRequest(path);
        if (qmap != null) {
            request.getQueryParameters().putAll(qmap);
        }
        return request;
    }
    
    public ClientRequest getClientRequest(String path) {
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        secureHttpClient(httpClient);
        
        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 30 * 1000);
        HttpConnectionParams.setSoTimeout(params, 0);
        ClientExecutor executor = new ApacheHttpClient4Executor(httpClient);
        
        ClientRequest request = new ClientRequest(getPath(path), executor);
        request.header(IInfoModel.FID, facilityId);
        request.header(IInfoModel.USER_NAME, userName);
        request.header(IInfoModel.PASSWORD, password);
        request.header(IInfoModel.CLIENT_UUID, clientUUID);
        
        return request;
    }
    
    // SSL証明書を受け入れる
    private void secureHttpClient(HttpClient httpClient) {
        
        if (sslSocketFactory == null) {
            return;
        }
        //register https protocol in httpclient's scheme registry
        SchemeRegistry sr = httpClient.getConnectionManager().getSchemeRegistry();
        sr.register(new Scheme("https", 443, sslSocketFactory));    // 8443?
    }
    
    private void setupOreOreSSL() throws NoSuchAlgorithmException, KeyManagementException {

        //Secure Protocol implementation.
        SSLContext ctx = SSLContext.getInstance("TLS");
        
        //Implementation of a trust manager for X509 certificates
        X509TrustManager manager = new X509TrustManager() {
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
        };

        X509HostnameVerifier verifier = new X509HostnameVerifier() {
            @Override
            public void verify(String string, SSLSocket ssls) throws IOException {
            }

            @Override
            public void verify(String string, X509Certificate xc) throws SSLException {
            }

            @Override
            public void verify(String string, String[] strings, String[] strings1) throws SSLException {
            }

            @Override
            public boolean verify(String string, SSLSession ssls) {
                return true;
            }
        };
        
        ctx.init(null, new TrustManager[]{manager}, null);
        sslSocketFactory = new SSLSocketFactory(ctx, verifier);
    }
}
