package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import open.dolphin.infomodel.RoleModel;
import open.dolphin.infomodel.UserModel;
import open.dolphin.session.UserServiceBean;

/**
 * UserResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("user")
public class UserResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private UserServiceBean userServiceBean;
    
    @Context
    private HttpServletRequest servletReq;

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
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());
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

        String fid = getRemoteFacility(servletReq.getRemoteUser());
        debug(fid);
        
        UserModel model = (UserModel)
                getConverter().fromJson(json, UserModel.class);
        model.getFacilityModel().setFacilityId(fid);

        // 関係を構築する
        List<RoleModel> roles = model.getRoles();
        for (RoleModel role : roles) {
            role.setUserModel(model);
        }

        int result = userServiceBean.addUser(model);
        String cntStr = String.valueOf(result);
        
        debug(cntStr);
        
        UserCache.getInstance().getMap().clear();

        return cntStr;
    }


    @PUT
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putUser(String json) {

        UserModel model = (UserModel)
                getConverter().fromJson(json, UserModel.class);
        
        // 関係を構築する
        List<RoleModel> roles = model.getRoles();
        for (RoleModel role : roles) {
            role.setUserModel(model);
        }

        int result = userServiceBean.updateUser(model);
        String cntStr = String.valueOf(result);
        
        debug(cntStr);
        
        UserCache.getInstance().getMap().clear();

        return cntStr;
    }


    @DELETE
    @Path("{userId}/")
    public void deleteUser(@PathParam("userId") String userId) {

        int result = userServiceBean.removeUser(userId);

        debug(String.valueOf(result));
        
        UserCache.getInstance().getMap().clear();
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
