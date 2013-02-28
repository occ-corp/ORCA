package open.dolphin.delegater;

import open.dolphin.infomodel.ChartEventModel;
import org.jboss.resteasy.client.ClientResponse;

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
    
    public int putChartEvent(ChartEventModel evt) {
        
        try {
            String json = getConverter().toJson(evt);

            ClientResponse response = getClientRequest(PUT_EVENT_PATH)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .put(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
            if (status != HTTP200) {
                return -1;
            }
            
            return Integer.parseInt(enityStr);
            
        } catch (Exception ex) {
            return -1;
        }
    }

    public String subscribe() throws Exception {
        
        ClientResponse response = getClientRequest(SUBSCRIBE_PATH, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        String entityStr = (String) response.getEntity(String.class);
        
        return entityStr;
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
