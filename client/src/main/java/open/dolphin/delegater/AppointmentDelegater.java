package open.dolphin.delegater;

import java.util.List;
import open.dolphin.infomodel.AppointmentModel;
import org.jboss.resteasy.client.ClientResponse;

/**
 * AppointmentDelegater
 * 
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class AppointmentDelegater extends BusinessDelegater {
    
    private static final boolean debug = false;
    private static final AppointmentDelegater instance;

    static {
        instance = new AppointmentDelegater();
    }

    public static AppointmentDelegater getInstance() {
        return instance;
    }

    private AppointmentDelegater() {
    }

    public int putAppointments(List<AppointmentModel> list) throws Exception {

        String path = "appo/";
        String json = getConverter().toJson(list);

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .body(MEDIATYPE_JSON_UTF8, json)
                .put(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = (String) response.getEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        return Integer.parseInt(entityStr);
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
