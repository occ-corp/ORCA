package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Jackson関連
 * @author masuda, Masuda Naika
 */
public class JsonConverter {
    
    private static final String CAMMA = ",";
    private static final ObjectMapper objectMapper;
    private static final JsonConverter instance;
    private static final boolean debug = false;
    
    static {
        instance = new JsonConverter();
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        if (debug) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }
    
    private JsonConverter(){
    }
    
    public static JsonConverter getInstance() {
        return instance;
    }
    
    public String toJson(Object obj) {
        try {
            String json =objectMapper.writeValueAsString(obj);
            debug(json);
            return json;
        } catch (JsonGenerationException ex) {
            processException(ex);
        } catch (JsonMappingException ex) {
            processException(ex);
        } catch (IOException ex) {
            processException(ex);
        } catch (Exception ex) {
            processException(ex);
        }
        return null;
    }
    
    public Object fromJson(String json, Class clazz) {
        try {
            debug(json);
            return objectMapper.readValue(json, clazz);
        } catch (JsonParseException ex) {
            processException(ex);
        } catch (JsonMappingException ex) {
            processException(ex);
        } catch (IOException ex) {
            processException(ex);
        } catch (Exception ex) {
            processException(ex);
        }
        
        return null;
    }
    
    public Object fromJson(String json, TypeReference typeRef) {
        try {
            debug(json);
            return objectMapper.readValue(json, typeRef);
        } catch (JsonParseException ex) {
            processException(ex);
        } catch (JsonMappingException ex) {
            processException(ex);
        } catch (IOException ex) {
            processException(ex);
        } catch (Exception ex) {
            processException(ex);
        }
        return null;
    }
    
    private void processException(Exception ex) {
        ex.printStackTrace(System.err);
    }
    
    private void debug(String msg) {
        if (debug) {
            System.out.print(msg);
        }
    }
    
    public List<Long> toLongList(String params) {
        String[] strArray  = params.split(CAMMA);
        List<Long> ret = new ArrayList<Long>();
        for (String s : strArray) {
            ret.add(Long.valueOf(s));
        }
        return ret;
    }
    
    public List<String> toStrList(String params) {
        String[] strArray  = params.split(CAMMA);
        return Arrays.asList(strArray);
    }
    
    public String fromList(List list) {
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Iterator itr = list.iterator(); itr.hasNext();) {
            if (!first) {
                sb.append(CAMMA);
            } else {
                first = false;
            }
            sb.append(String.valueOf(itr.next()));
        }
        return sb.toString();
    }
}
