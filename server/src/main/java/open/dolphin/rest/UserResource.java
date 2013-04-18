package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.UserModel;
import open.dolphin.mbean.ServletContextHolder;
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
    
    @Inject
    private ServletContextHolder contextHolder;
    

    public UserResource() {
    }

    @GET
    @Path("{userId}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getUser(@PathParam("userId") String userId) {

        UserModel result = userServiceBean.getUser(userId);

        StreamingOutput so = getJsonOutStream(result);
        
        return Response.ok(so).build();
    }

    
    @GET
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getAllUser() {
        
        String fid = getRemoteFacility();
        debug(fid);

        List<UserModel> result = userServiceBean.getAllUser(fid);
        
        StreamingOutput so = getJsonOutStream(result);
        
        return Response.ok(so).build();
    }


    @POST
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response postUser(String json) {

        String fid = getRemoteFacility();
        debug(fid);
        
        UserModel model = (UserModel)
                getConverter().fromJson(json, UserModel.class);
        model.getFacilityModel().setFacilityId(fid);

        int result = userServiceBean.addUser(model);
        String cntStr = String.valueOf(result);
        
        debug(cntStr);
        
        contextHolder.getUserMap().clear();

        return Response.ok(cntStr).build();
    }


    @PUT
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putUser(String json) {

        UserModel model = (UserModel)
                getConverter().fromJson(json, UserModel.class);
        
        int result = userServiceBean.updateUser(model);
        String cntStr = String.valueOf(result);
        
        debug(cntStr);
        
        contextHolder.getUserMap().clear();

        return Response.ok(cntStr).build();
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
    public Response putFacility(String json) {

        UserModel model = (UserModel)
                getConverter().fromJson(json, UserModel.class);

        int result = userServiceBean.updateFacility(model);
        String cntStr = String.valueOf(result);
        
        debug(cntStr);

        return Response.ok(cntStr).build();
    }
    
    @GET
    @Path("login/")
    public Response login(@QueryParam("fidUid") String fidUid, 
            @QueryParam("clientUUID") String clientUUID,
            @QueryParam("force") String forceStr) {
        
        boolean force = Boolean.parseBoolean(forceStr);
        String uuid = userServiceBean.login(fidUid, clientUUID, force);
        return Response.ok(uuid).build();
    }
    
    @GET
    @Path("logout/")
    public Response logout(@QueryParam("fidUid") String fidUid, 
            @QueryParam("clientUUID") String clientUUID) {
        
        String uuid = userServiceBean.logout(fidUid, clientUUID);
        return Response.ok(uuid).build();
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
