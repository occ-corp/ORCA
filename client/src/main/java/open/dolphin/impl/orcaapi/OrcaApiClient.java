package open.dolphin.impl.orcaapi;

import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.project.Project;
import open.dolphin.util.Base64Utils;
import org.jboss.resteasy.client.ClientRequest;

/**
 * ORCA API用のRESTEasyClient
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaApiClient {
    
    private static final OrcaApiClient instance;
    
    private static final int API_PORT = 8000;
    
    static {
        instance = new OrcaApiClient();
    }
    
    private OrcaApiClient() {
    }
    
    public static OrcaApiClient getInstance() {
        return instance;
    }
    
    private String getPath(String path) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(Project.getString(Project.CLAIM_ADDRESS));
        sb.append(":").append(String.valueOf(API_PORT));
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path);
        
        return sb.toString();
    }
    
    private String getAuthorizationHeader() {
        
        String username = Project.getString(Project.ORCA_USER_ID);
        String password = Project.getString(Project.ORCA_USER_PASSWORD);
        
        StringBuilder sb = new StringBuilder();
        sb.append(username).append(":").append(password);
        String base64 = Base64Utils.getBase64(sb.toString());
        sb = new StringBuilder();
        sb.append("Basic ");
        sb.append(base64);
        
        return sb.toString();
    }

    public ClientRequest getClientRequest(String path, MultivaluedMap<String, String> qmap) {
        ClientRequest request = new ClientRequest(getPath(path));
        request.header("Authorization", getAuthorizationHeader());
        if (qmap != null) {
            request.getQueryParameters().putAll(qmap);
        }
        return request;
    }
}
