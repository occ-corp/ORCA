
package open.dolphin.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.spi.interception.MessageBodyWriterContext;
import org.jboss.resteasy.spi.interception.MessageBodyWriterInterceptor;

/**
 * UTF8MessageBodyWriterInterceptor
 * RESTEasy on Windowsで文字化けしないように
 * MeidaTypeにcharset=UTF-8を設定する
 * 
 * @author masuda, Masuda Naika
 */
@Provider
@ServerInterceptor
public class UTF8MessageBodyWriterInterceptor implements MessageBodyWriterInterceptor {

    @Override
    public void write(MessageBodyWriterContext context) throws IOException, WebApplicationException {
        
        MediaType oldMedia = context.getMediaType();
        // 書き換え不可のMapなのでコピーを作る
        Map<String, String> newMap = new HashMap<String, String>(oldMedia.getParameters());
        newMap.put("charset", "UTF-8");
        // 新たにMediaTypeを作成しセットする
        MediaType newMedia = new MediaType(oldMedia.getType(), oldMedia.getSubtype(), newMap);
        context.setMediaType(newMedia);
        
        context.proceed();
    }

}
