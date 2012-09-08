package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import open.dolphin.client.Dolphin;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.BeanUtils;

/**
 * State変化関連のデレゲータ
 * @author masuda, Masuda Naika
 */
public class StateDelegater extends BusinessDelegater {
    
    private static final String RES_CS = "stateRes/";
    private static final String SUBSCRIBE_PATH = RES_CS + "subscribe/";
    
    private static final boolean debug = false;
    private static final StateDelegater instance;
    
    private String fid;
    private String clientUUID;

    static {
        instance = new StateDelegater();
    }
    
    private StateDelegater() {
        fid = Project.getFacilityId();
        clientUUID = Dolphin.getInstance().getClientUUID();
    }
    
    public static StateDelegater getInstance() {
        return instance;
    }
    
    public int putStateMsgModel(StateMsgModel msg) {
        
        msg.setFacilityId(fid);
        msg.setIssuerUUID(clientUUID);
        
        StringBuilder sb = new StringBuilder();
        sb.append(RES_CS);
        sb.append("state");
        String path = sb.toString();

        String json = getConverter().toJson(msg);

        ClientResponse response = getResource(path, null)
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
    
    public String subscribe() throws Exception {
        
        // できるだけ時間をとらないようにデシリアライズは後回しにする
        String json = JerseyClient.getInstance()
                .getAsyncResource(SUBSCRIBE_PATH)
                .accept(MEDIATYPE_TEXT_UTF8)
                .get(String.class);
        
        return json;
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
