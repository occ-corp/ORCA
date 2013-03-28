package open.dolphin.delegater;

import com.sun.jersey.api.client.ClientResponse;
import java.util.concurrent.Future;
import open.dolphin.infomodel.ChartEventModel;

/**
 * State変化関連のデレゲータ
 * @author masuda, Masuda Naika
 */
public class ChartEventDelegater extends BusinessDelegater {
    
    private static final String RES_CE = "chartEvent/";
    private static final String SUBSCRIBE_PATH = RES_CE + "subscribe";
    private static final String PUT_EVENT_PATH = RES_CE + "event";
    
    private static final boolean debug = false;
    private static final ChartEventDelegater instance;
    
    static {
        instance = new ChartEventDelegater();
    }
    
    private ChartEventDelegater() {
    }
    
    public static ChartEventDelegater getInstance() {
        return instance;
    }
    
    public int putChartEvent(ChartEventModel evt) throws Exception {
        
        String json = getConverter().toJson(evt);

        ClientResponse response = getClientRequest(PUT_EVENT_PATH, null)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = (String) response.getEntity(String.class);
        debug(status, enityStr);
        isHTTP200(status);

        return Integer.parseInt(enityStr);
    }

    public Future<ClientResponse> subscribe() throws Exception {
        
        Future<ClientResponse> future = getAsyncClientRequest(SUBSCRIBE_PATH)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        return future;
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
