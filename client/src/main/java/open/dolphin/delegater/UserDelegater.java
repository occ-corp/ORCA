package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.UserModel;
import open.dolphin.project.Project;

/**
 * User 関連の Business Delegater　クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class UserDelegater extends BusinessDelegater {

    private static final String RES_USER = "user/";
    
    private static final boolean debug = false;
    private static final UserDelegater instance;

    static {
        instance = new UserDelegater();
    }

    public static UserDelegater getInstance() {
        return instance;
    }

    private UserDelegater() {
    }
    
    public UserModel login(String fid, String uid, String password) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append(fid);
        sb.append(IInfoModel.COMPOSITE_KEY_MAKER);
        sb.append(uid);
        String fidUid = sb.toString();

        //RESTEasyClient restEasy = RESTEasyClient.getInstance();
        //String baseURI = Project.getBaseURI();
        //restEasy.setBaseURI(baseURI);
        //restEasy.setUpAuthentication(fidUid, password, false);
        JerseyClient jersey = JerseyClient.getInstance();
        String baseURI = Project.getBaseURI();
        jersey.setBaseURI(baseURI);
        jersey.setUpAuthentication(fidUid, password, false);
        
        if (DEBUG) {
            System.out.println(baseURI);
            System.out.println(fidUid);
            System.out.println(password);
        }

        return getUser(fidUid);
    }
    
    public UserModel getUser(String userPK) throws Exception {
        
        StringBuilder sb = new StringBuilder();
        sb.append(RES_USER);
        sb.append(userPK);
        String path = sb.toString();

        ClientResponse response = getClientRequest(path, null)
                   .accept(MEDIATYPE_JSON_UTF8)
                   .get(ClientResponse.class);

        int status = response.getStatus();
        //String entityStr = (String) response.getEntity(String.class);
        //debug(status, entityStr);
        isHTTP200(status);
        InputStream is = response.getEntityInputStream();

        UserModel userModel = (UserModel) 
                getConverter().fromJson(is, UserModel.class);

        return userModel;
    }
    
    public List<UserModel> getAllUser() throws Exception {
        
        String path = RES_USER;

        ClientResponse response = getClientRequest(path, null)
                   .accept(MEDIATYPE_JSON_UTF8)
                   .get(ClientResponse.class);

        int status = response.getStatus();
        //String entityStr = (String) response.getEntity(String.class);
        //debug(status, entityStr);
        isHTTP200(status);
        InputStream is = response.getEntityInputStream();

        TypeReference typeRef = new TypeReference<List<UserModel>>(){};
        List<UserModel> list  = (List<UserModel>) 
                getConverter().fromJson(is, typeRef);

        return list;
    }
    
    public int addUser(UserModel userModel) throws Exception {

        String path = RES_USER;
        String json = getConverter().toJson(userModel);

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = (String) response.getEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);
        
        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public int updateUser(UserModel userModel) throws Exception {

        String path = RES_USER;
        String json = getConverter().toJson(userModel);

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)   
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = (String) response.getEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public int deleteUser(String uid) throws Exception {
        
        String path = RES_USER + uid;

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .delete(ClientResponse.class);

        int status = response.getStatus();
        debug(status, "delete response");
        isHTTP200(status);

        return 1;
    }
    
    public int updateFacility(UserModel userModel) throws Exception {

        String path = RES_USER + "facility";

        String json = getConverter().toJson(userModel);

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = (String) response.getEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public String login(String fidUid, String clientUUID, boolean force) throws Exception {
        
        String path = RES_USER + "login";
        
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fidUid", fidUid);
        qmap.add("clientUUID", clientUUID);
        qmap.add("force", String.valueOf(force));
        
        ClientResponse response = getClientRequest(path, qmap)
                .accept(MEDIATYPE_TEXT_UTF8)
                .get(ClientResponse.class);
        int status = response.getStatus();
        String currentUUID = (String) response.getEntity(String.class);
        debug(status, currentUUID);
        isHTTP200(status);
        
        return currentUUID;
    }
    
    public String logout(String fidUid, String clientUUID) throws Exception {
        
        String path = RES_USER + "logout";
        
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("fidUid", fidUid);
        qmap.add("clientUUID", clientUUID);
        
        ClientResponse response = getClientRequest(path, qmap)
                .accept(MEDIATYPE_TEXT_UTF8)
                .get(ClientResponse.class);
        int status = response.getStatus();
        String oldUUID = (String) response.getEntity(String.class);
        debug(status, oldUUID);
        isHTTP200(status);
        
        return oldUUID;
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
