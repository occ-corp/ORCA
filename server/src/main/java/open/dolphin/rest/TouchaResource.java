package open.dolphin.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;


/**
 *
 * @author masuda, Masuda Naika
 */
@Path("toucha")
public class TouchaResource extends AbstractResource {
    
    private static final boolean debug = false;
    
   
    @GET
    @Path("hello/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response helloDolphin() {
        return Response.ok("Hello Dolphin").build();
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
