package open.dolphin.delegater;

import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.util.HashUtil;
import org.jboss.resteasy.client.ClientRequest;

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
    
    private static final RESTEasyClient instance;
    
    static {
        instance = new RESTEasyClient();
    }

    private RESTEasyClient() {
        clientUUID = Dolphin.getInstance().getClientUUID();
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
    
    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String uri) {
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
        ClientRequest request = new ClientRequest(getPath(path));
        request.header(IInfoModel.FID, facilityId);
        request.header(IInfoModel.USER_NAME, userName);
        request.header(IInfoModel.PASSWORD, password);
        request.header(IInfoModel.CLIENT_UUID, clientUUID);
        if (qmap != null) {
            request.getQueryParameters().putAll(qmap);
        }
        return request;
    }
 
    private String[] splitFidUid(String username) {
        int pos = username.indexOf(IInfoModel.COMPOSITE_KEY_MAKER);
        String fid = username.substring(0, pos);
        String uid = username.substring(pos + 1);
        return new String[]{fid, uid};
    }
}
