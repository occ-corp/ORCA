package open.dolphin.delegater;

import com.sun.jersey.api.client.ClientResponse;
import java.util.concurrent.Future;
import open.dolphin.infomodel.ChartEventModel;
import open.dolphin.project.Project;

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
    
    private String fid;

    static {
        instance = new ChartEventDelegater();
    }
    
    private ChartEventDelegater() {
        fid = Project.getFacilityId();
    }
    
    public static ChartEventDelegater getInstance() {
        return instance;
    }
    
    public int putChartEvent(ChartEventModel evt) {

        String json = getConverter().toJson(evt);

        ClientResponse response = getResource(PUT_EVENT_PATH, null)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String enityStr = response.getEntity(String.class);
        debug(status, enityStr);
        
        if (status != HTTP200) {
            return -1;
        }
        
        return Integer.parseInt(enityStr);
    }

    public Future<String> subscribe() throws Exception {
        
        // できるだけ時間をとらないようにデシリアライズは後回しにする
        // 処理もれが心配
        Future<String> future = JerseyClient.getInstance()
                .getAsyncResource(SUBSCRIBE_PATH)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(String.class);
        return future;
    }
/*
    public String subscribe() throws Exception {
        
        // できるだけ時間をとらないようにデシリアライズは後回しにする
        // 処理もれが心配
        String json = JerseyClient.getInstance()
                .getAsyncResource(SUBSCRIBE_PATH)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(String.class);
        return json;
    }
*/
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
