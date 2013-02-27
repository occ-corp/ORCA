package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import open.dolphin.infomodel.LetterModule;
import org.jboss.resteasy.client.ClientResponse;

/**
 * 紹介状用のデリゲータークラス。
 * @author Kazushi Minagawa.
 * @author modified by masuda, Masuda Naika
 */
public class LetterDelegater extends BusinessDelegater {

    private static final String PATH_FOR_LETTER = "odletter/letter/";
    private static final String PATH_FOR_LETTER_LIST = "odletter/list/";
    
    private static final boolean debug = false;
    private static final LetterDelegater instance;

    static {
        instance = new LetterDelegater();
    }

    public static LetterDelegater getInstance() {
        return instance;
    }

    private LetterDelegater() {
    }
    
    public long saveOrUpdateLetter(LetterModule model) {
        
        try {
            String json = getConverter().toJson(model);

            if (json == null) {
                return 0L;
            }
            
            String path = PATH_FOR_LETTER;

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)    
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .put(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            long pk = Long.parseLong(entityStr);
            return pk;
            
        } catch (Exception ex) {
            return -1;
        }
    }

    public LetterModule getLetter(long letterPk) {
        
        try {
            String path = PATH_FOR_LETTER + String.valueOf(letterPk);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            LetterModule ret = (LetterModule)
                    getConverter().fromJson(entityStr, LetterModule.class);
            
            return ret;
            
        } catch (Exception ex) {
            return null;
        }
    }


    public List<LetterModule> getLetterList(long kartePk) {
        
        try {
            String path = PATH_FOR_LETTER_LIST + String.valueOf(kartePk);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<LetterModule>>(){};
            List<LetterModule> ret = (List<LetterModule>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return ret;
            
        } catch (Exception ex) {
            return null;
        }
    }


    public void delete(long pk) {
        
        try {
            String path = PATH_FOR_LETTER + String.valueOf(pk);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .delete(ClientResponse.class);

            int status = response.getStatus();
            debug(status, "delete response");
            
        } catch (Exception ex) {
        }
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
