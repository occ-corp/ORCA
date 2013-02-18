package open.dolphin.delegater;

import com.sun.jersey.api.client.AsyncWebResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.util.HashUtil;

/**
 * JerseyClient
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class JerseyClient {

    private static final JerseyClient instance;
    private static final int TIMEOUT1 = 30;
    
    private String clientUUID;
    private String baseURI;
    private String userName;
    private String password;
    private String facilityId;
    
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
            String[] fidUid = splitFidUid(username);
            this.facilityId = fidUid[0];
            this.userName = fidUid[1];
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
                    .header(IInfoModel.FID, facilityId)
                    .header(IInfoModel.USER_NAME, userName)
                    .header(IInfoModel.PASSWORD, password);
        } else {
            return webResource.path(path)
                    .header(IInfoModel.FID, facilityId)
                    .header(IInfoModel.USER_NAME, userName)
                    .header(IInfoModel.PASSWORD, password);
        }
    }
    
    // pvt同期用のクライアント

    public AsyncWebResource.Builder getAsyncResource(String path) {
        return asyncResource.path(path)
                .header(IInfoModel.FID, facilityId)
                .header(IInfoModel.USER_NAME, userName)
                .header(IInfoModel.PASSWORD, password)
                .header(IInfoModel.CLIENT_UUID, clientUUID);
    }

    
    private String[] splitFidUid(String username) {
        int pos = username.indexOf(IInfoModel.COMPOSITE_KEY_MAKER);
        String fid = username.substring(0, pos);
        String uid = username.substring(pos + 1);
        return new String[]{fid, uid};
    }
}
