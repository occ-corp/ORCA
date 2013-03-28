package open.dolphin.impl.orcaapi;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.project.Project;

/**
 * ORCA API用のJersey Client
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaApiClient {
    
    private static final OrcaApiClient instance;
    
    private Client client;
    private WebResource webResource;
    
    private static final int API_PORT = 8000;
    
    static {
        instance = new OrcaApiClient();
    }
    
    private OrcaApiClient() {
        client = Client.create();
        setup();
    }
    
    public static OrcaApiClient getInstance() {
        return instance;
    }
    
    final public void setup() {

        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(Project.getString(Project.CLAIM_ADDRESS));
        sb.append(":").append(String.valueOf(API_PORT));
        String uri = sb.toString();
        String username = Project.getString(Project.ORCA_USER_ID);
        String password = Project.getString(Project.ORCA_USER_PASSWORD);
        
        webResource = client.resource(uri);
        
        client.removeAllFilters();
        client.addFilter(new HTTPBasicAuthFilter(username, password));
    }

    // QueryParam付のWebResource
    public WebResource.Builder getResource(String path, MultivaluedMap<String, String> qmap) {

        if (qmap != null) {
            return webResource.path(path).queryParams(qmap).getRequestBuilder();
        } else {
            return webResource.path(path).getRequestBuilder();
        }
    }
}
