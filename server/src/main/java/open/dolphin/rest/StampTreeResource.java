package open.dolphin.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.*;
import open.dolphin.session.StampServiceBean;

/**
 * StampTreeResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("stampTree")
public class StampTreeResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private StampServiceBean stampServiceBean;
    

    public StampTreeResource() {
    }

    @GET
    @Path("{userPK}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getStampTree(@PathParam("userPK") String userPK) {

        UserStampTreeModel result = stampServiceBean.getTrees(Long.parseLong(userPK));
        //String json = getConverter().toJson(result);
        //debug(json);
        //return json;
        
        StreamingOutput so = getJsonOutStream(result);
        
        return Response.ok(so).build();
    }

    @PUT
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putTree(String json) {

        StampTreeModel model = (StampTreeModel) 
                getConverter().fromJson(json, StampTreeModel.class);

        long pk = stampServiceBean.putTree((StampTreeModel) model);
        String pkStr = String.valueOf(pk);
        
        debug(pkStr);

        return Response.ok(pkStr).build();
    }

    @POST
    @Path("published")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response postPublishedTree(String json) {
        
        UserStampTreeModel treeModel = (UserStampTreeModel)
                getConverter().fromJson(json, UserStampTreeModel.class);

        List<IStampTreeModel> list = treeModel.getTreeList();

        long pk = stampServiceBean.saveAndPublishTree(list);
        String pkStr = String.valueOf(pk);
        
        debug(pkStr);

        return Response.ok(pkStr).build();
    }

    @PUT
    @Path("published")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putPublishedTree(String json) {

        UserStampTreeModel treeModel = (UserStampTreeModel)
                getConverter().fromJson(json, UserStampTreeModel.class);

        List<IStampTreeModel> list = treeModel.getTreeList();

        int cnt = stampServiceBean.updatePublishedTree(list);
        String cntStr = String.valueOf(cnt);
        
        debug(cntStr);

        return Response.ok(cntStr).build();
    }

    @PUT
    @Path("published/cancel")
    @Consumes(MEDIATYPE_JSON_UTF8)
    public void cancelPublishedTree(String json) {

        StampTreeModel model = (StampTreeModel) 
                getConverter().fromJson(json, StampTreeModel.class);
        
        int cnt = stampServiceBean.cancelPublishedTree(model);

        String cntStr = String.valueOf(cnt);
        
        debug(cntStr);
    }


    @GET
    @Path("published")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPublishedTrees() {

        String fid = getRemoteFacility();
        List<PublishedTreeModel> list = stampServiceBean.getPublishedTrees(fid);

        //String json = getConverter().toJson(list);
        //debug(json);
        //return json;
        
        StreamingOutput so = getJsonOutStream(list);
        
        return Response.ok(so).build();
    }


    @PUT
    @Path("subscribed")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response subscribeTrees(String json) {
        
        TypeReference typeRef = new TypeReference<List<SubscribedTreeModel>>(){};
        List<SubscribedTreeModel> list = (List<SubscribedTreeModel>)
                getConverter().fromJson(json, typeRef);

        List<Long> result = stampServiceBean.subscribeTrees(list);

        String pks = getConverter().fromList(result);
        
        debug(pks);

        return Response.ok(pks).build();
    }


    @DELETE
    @Path("subscribed")
    public void unsubscribeTrees(@QueryParam("ids") String ids) {

        List<Long> list = getConverter().toLongList(ids);

        int cnt = stampServiceBean.unsubscribeTrees(list);
        
        String cntStr = String.valueOf(cnt);
        debug(cntStr);
    }


    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
