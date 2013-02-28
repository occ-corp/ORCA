package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.UserModel;
import open.dolphin.project.Project;
import org.jboss.resteasy.client.ClientResponse;

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

        RESTEasyClient restEasy = RESTEasyClient.getInstance();
        String baseURI = Project.getBaseURI();
        restEasy.setBaseURI(baseURI);
        restEasy.setUpAuthentication(fidUid, password, false);
        
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
        String entityStr = (String) response.getEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        UserModel userModel = (UserModel) 
                getConverter().fromJson(entityStr, UserModel.class);

        return userModel;
    }
    
    public List<UserModel> getAllUser() throws Exception {
        
        String path = RES_USER;

        ClientResponse response = getClientRequest(path, null)
                   .accept(MEDIATYPE_JSON_UTF8)
                   .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = (String) response.getEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        TypeReference typeRef = new TypeReference<List<UserModel>>(){};
        List<UserModel> list  = (List<UserModel>) 
                getConverter().fromJson(entityStr, typeRef);

        return list;
    }
    
    public int addUser(UserModel userModel) throws Exception {

        String path = RES_USER;
        String json = getConverter().toJson(userModel);

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .body(MEDIATYPE_JSON_UTF8, json)
                .post(ClientResponse.class);

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
                .body(MEDIATYPE_JSON_UTF8, json)
                .put(ClientResponse.class);

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
                .body(MEDIATYPE_JSON_UTF8, json)
                .put(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = (String) response.getEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
