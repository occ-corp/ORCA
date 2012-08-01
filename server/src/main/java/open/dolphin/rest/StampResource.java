package open.dolphin.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import open.dolphin.infomodel.StampModel;
import open.dolphin.session.StampServiceBean;

/**
 * StampResource
 *
 * @author kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("stamp")
public class StampResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private StampServiceBean stampServiceBean;

    public StampResource() {
    }

    @GET
    @Path("id/{param}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getStamp(@PathParam("param") String param) {
        
        StampModel stamp = stampServiceBean.getStamp(param);
        String json = getConverter().toJson(stamp);
        debug(json);
        return json;
    }

    @GET
    @Path("list")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getStamps(@QueryParam("param") String ids) {

        List<String> list = toStrList(ids);

        List<StampModel> result = stampServiceBean.getStamp(list);

        String json = getConverter().toJson(result);
        debug(json);
        return json;
    }

    @PUT
    @Path("id")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putStamp(String json) {

        StampModel model = (StampModel)
                getConverter().fromJson(json, StampModel.class);

        String ret = stampServiceBean.putStamp(model);
        
        debug(ret);

        return ret;
    }

    @PUT
    @Path("list")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putStamps(String json) {
        
        TypeReference typeRef = new TypeReference<List<StampModel>>(){};
        List<StampModel> list = (List<StampModel>)
                getConverter().fromJsonTypeRef(json, typeRef);

        List<String> ret = stampServiceBean.putStamp(list);
        
        String retText = fromStrList(ret);
        
        debug(retText);

        return retText;
    }


    @DELETE
    @Path("id")
    public void deleteStamp(@QueryParam("param") String param) {

        int cnt = stampServiceBean.removeStamp(param);

        debug(String.valueOf(cnt));
    }
    

    @DELETE
    @Path("list")
    public void deleteStamps(@QueryParam("ids") String ids) {

        List<String> list = toStrList(ids);

        int cnt = stampServiceBean.removeStamp(list);

        debug(String.valueOf(cnt));
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
