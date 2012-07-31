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


/**
 * Jackson関連
 * @author masuda, Masuda Naika
 */
public class JsonConverter {
    
    private static final ObjectMapper objectMapper;
    private static final JsonConverter instance;
    static {
        instance = new JsonConverter();
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        //objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
      }
    
    private JsonConverter(){
    }
    
    public static JsonConverter getInstance() {
        return instance;
    }
    
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonGenerationException ex) {
            debug(ex);
        } catch (JsonMappingException ex) {
            debug(ex);
        } catch (IOException ex) {
            debug(ex);
        }
        return null;
    }
    
    public Object fromJson(String json, Class clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonParseException ex) {
            debug(ex);
        } catch (JsonMappingException ex) {
            debug(ex);
        } catch (IOException ex) {
            debug(ex);
        }
        return null;
    }
    
    public Object fromJsonTypeRef(String json, TypeReference typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonParseException ex) {
            debug(ex);
        } catch (JsonMappingException ex) {
            debug(ex);
        } catch (IOException ex) {
            debug(ex);
        }
        return null;
    }
    
    private void debug(Exception ex) {
        ex.printStackTrace(System.err);
    }

}
