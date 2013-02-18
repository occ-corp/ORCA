package open.dolphin.rest;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import open.dolphin.infomodel.LetterModule;
import open.dolphin.session.LetterServiceBean;

/**
 * LetterResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
@Path("rest/odletter")
public class LetterResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private LetterServiceBean letterServiceBean;

    public LetterResource() {
    }

    @PUT
    @Path("letter")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putLetter(String json) {

        LetterModule model = (LetterModule)
                getConverter().fromJson(json, LetterModule.class);

        Long pk = letterServiceBean.saveOrUpdateLetter(model);

        String pkStr = String.valueOf(pk);
        debug(pkStr);

        return pkStr;
    }

    @GET
    @Path("list/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getLetterList(@PathParam("karteId") Long karteId) {

        List<LetterModule> list = letterServiceBean.getLetterList(karteId);
        
        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }

    @GET
    @Path("letter/{pk}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getLetter(@PathParam("pk") Long pk) {

        LetterModule result = letterServiceBean.getLetter(pk);
        
        String json = getConverter().toJson(result);
        debug(json);
        
        return json;
    }

    @DELETE
    @Path("letter/{pk}/")
    public void delete(@PathParam("pk") Long pk) {

        letterServiceBean.delete(pk);
    }


    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
