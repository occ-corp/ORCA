package open.dolphin.delegater;

import com.sun.jersey.api.client.ClientResponse;
import java.util.List;
import open.dolphin.infomodel.AppointmentModel;

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
    private AppointmentDelegater(){
    }
    
    public int putAppointments(List<AppointmentModel> list) {

        String path = "appo/";
        String json = getConverter().toJson(list);

        ClientResponse response = getResource(path)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);
        
        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        
        debug(status, entityStr);

        return Integer.parseInt(entityStr);
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
