package open.dolphin.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import open.dolphin.infomodel.AppointmentModel;
import open.dolphin.session.AppoServiceBean;

/**
 * AppoResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("appo")
public class AppoResource extends AbstractResource {

    private boolean debug = false;
    
    @Inject
    private AppoServiceBean appoServiceBean;

    /** Creates a new instance of AppoResource */
    public AppoResource() {
    }

    @PUT
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putAppointments(String json) {

        TypeReference typeRef = new TypeReference<List<AppointmentModel>>(){};
        List<AppointmentModel> list = (List<AppointmentModel>) 
                getConverter().fromJsonTypeRef(json, typeRef);

        int count = appoServiceBean.putAppointments(list);
        
        String cntStr = String.valueOf(count);
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
