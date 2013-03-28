package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import java.io.InputStream;
import java.util.List;
import open.dolphin.infomodel.LetterModule;

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
    
    public long saveOrUpdateLetter(LetterModule model) throws Exception {

        String json = getConverter().toJson(model);

        if (json == null) {
            return 0L;
        }

        String path = PATH_FOR_LETTER;

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)    
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = (String) response.getEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        long pk = Long.parseLong(entityStr);
        return pk;
    }

    public LetterModule getLetter(long letterPk) throws Exception {
        
        String path = PATH_FOR_LETTER + String.valueOf(letterPk);

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        //String entityStr = (String) response.getEntity(String.class);
        //debug(status, entityStr);
        isHTTP200(status);
        InputStream is = response.getEntityInputStream();

        LetterModule ret = (LetterModule)
                getConverter().fromJson(is, LetterModule.class);

        return ret;
    }


    public List<LetterModule> getLetterList(long kartePk) throws Exception {
        
        String path = PATH_FOR_LETTER_LIST + String.valueOf(kartePk);

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        //String entityStr = (String) response.getEntity(String.class);
        //debug(status, entityStr);
        isHTTP200(status);
        InputStream is = response.getEntityInputStream();

        TypeReference typeRef = new TypeReference<List<LetterModule>>(){};
        List<LetterModule> ret = (List<LetterModule>)
                getConverter().fromJson(is, typeRef);

        return ret;
    }


    public void delete(long pk) throws Exception {

        String path = PATH_FOR_LETTER + String.valueOf(pk);

        ClientResponse response = getClientRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .delete(ClientResponse.class);

        int status = response.getStatus();
        debug(status, "delete response");
        isHTTP200(status);
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
