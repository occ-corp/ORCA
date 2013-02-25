package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import open.dolphin.infomodel.UserModel;
import open.dolphin.mbean.ServletContextHolder;
import open.dolphin.session.UserServiceBean;

/**
 * UserResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("rest/user")
public class UserResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private UserServiceBean userServiceBean;
    
    @Inject
    private ServletContextHolder contextHolder;
    

    public UserResource() {
    }

    @GET
    @Path("{userId}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getUser(@PathParam("userId") String userId) {

        UserModel result = userServiceBean.getUser(userId);
        
        String json = getConverter().toJson(result);
        debug(json);

        return json;
    }

    
    @GET
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getAllUser() {
        
        String fid = getRemoteFacility();
        debug(fid);

        List<UserModel> result = userServiceBean.getAllUser(fid);

        String json = getConverter().toJson(result);
        debug(json);

        return json;
    }


    @POST
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String postUser(String json) {

        String fid = getRemoteFacility();
        debug(fid);
        
        UserModel model = (UserModel)
                getConverter().fromJson(json, UserModel.class);
        model.getFacilityModel().setFacilityId(fid);

        int result = userServiceBean.addUser(model);
        String cntStr = String.valueOf(result);
        
        debug(cntStr);
        
        contextHolder.getUserMap().clear();

        return cntStr;
    }


    @PUT
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putUser(String json) {

        UserModel model = (UserModel)
                getConverter().fromJson(json, UserModel.class);
        
        int result = userServiceBean.updateUser(model);
        String cntStr = String.valueOf(result);
        
        debug(cntStr);
        
        contextHolder.getUserMap().clear();

        return cntStr;
    }


    @DELETE
    @Path("{userId}/")
    public void deleteUser(@PathParam("userId") String userId) {

        int result = userServiceBean.removeUser(userId);

        debug(String.valueOf(result));
        
        contextHolder.getUserMap().clear();
    }


    @PUT
    @Path("facility/")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putFacility(String json) {

        UserModel model = (UserModel)
                getConverter().fromJson(json, UserModel.class);

        int result = userServiceBean.updateFacility(model);
        String cntStr = String.valueOf(result);
        
        debug(cntStr);

        return cntStr;
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
