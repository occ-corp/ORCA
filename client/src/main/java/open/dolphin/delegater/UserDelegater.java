package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import java.util.List;
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
    private UserDelegater(){
    }
    
    public UserModel login(String fid, String uid, String password) {

        StringBuilder sb = new StringBuilder();
        sb.append(fid);
        sb.append(IInfoModel.COMPOSITE_KEY_MAKER);
        sb.append(uid);
        String fidUid = sb.toString();

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
    
    public UserModel getUser(String userPK) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(RES_USER);
        sb.append(userPK);
        String path = sb.toString();

        ClientResponse response = getResource(path)
                   .accept(MEDIATYPE_JSON_UTF8)
                   .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        UserModel userModel = (UserModel) 
                getConverter().fromJson(entityStr, UserModel.class);

        return userModel;
    }
    
    public List<UserModel> getAllUser() {

        String path = RES_USER;

        ClientResponse response = getResource(path)
                   .accept(MEDIATYPE_JSON_UTF8)
                   .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<UserModel>>(){};
        List<UserModel> list  = (List<UserModel>) 
                getConverter().fromJsonTypeRef(entityStr, typeRef);
        
        return list;
    }
    
    public int addUser(UserModel userModel) {

        String path = RES_USER;
        String json = getConverter().toJson(userModel);

        ClientResponse response = getResource(path)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        int cnt = Integer.parseInt(entityStr);
        
        return cnt;
    }
    
    public int updateUser(UserModel userModel) {
        
        String path = RES_USER;
        String json = getConverter().toJson(userModel);

        ClientResponse response = getResource(path)
                .accept(MEDIATYPE_TEXT_UTF8)   
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        int cnt = Integer.parseInt(entityStr);
        
        return cnt;
    }
    
    public int deleteUser(String uid) {

        String path = RES_USER + uid;

        ClientResponse response = getResource(path)
                .accept(MEDIATYPE_TEXT_UTF8)
                .delete(ClientResponse.class);

        int status = response.getStatus();
        
        debug(status, "delete response");

        return 1;
    }
    
    public int updateFacility(UserModel userModel) {
        
        String path = RES_USER + "facility";
        
        String json = getConverter().toJson(userModel);

        ClientResponse response = getResource(path)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

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
