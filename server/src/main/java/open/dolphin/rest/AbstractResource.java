package open.dolphin.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.JsonConverter;

/**
 * AbstractResource
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class AbstractResource {

    protected static final boolean DEBUG = false;
    
    protected static final String CAMMA = ",";
    //protected static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
    //protected static final SimpleDateFormat ISO_DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //protected static final SimpleDateFormat MML_Df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    private static final String CHARSET_UTF8 = "; charset=UTF-8";
    protected static final String MEDIATYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + CHARSET_UTF8;
    protected static final String MEDIATYPE_TEXT_UTF8 = MediaType.TEXT_PLAIN + CHARSET_UTF8;

    protected static final Logger logger = Logger.getLogger(AbstractResource.class.getName());

    protected Date parseDate(String source) {
        try {
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_DF_FORMAT);
            return frmt.parse(source);
            //return ISO_DF.parse(source);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    protected void debug(String msg) {
        logger.info(msg);
    }

    protected String getRemoteFacility(String remoteUser) {
        int index = remoteUser.indexOf(IInfoModel.COMPOSITE_KEY_MAKER);
        return remoteUser.substring(0, index);
    }

    protected String getFidPid(String remoteUser, String pid) {
        StringBuilder sb = new StringBuilder();
        sb.append(getRemoteFacility(remoteUser));
        sb.append(IInfoModel.COMPOSITE_KEY_MAKER);
        sb.append(pid);
        return sb.toString();
    }
    
    protected StreamingOutput getJsonOutStream(final Object obj) {
        StreamingOutput so = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                getConverter().toJson(obj, os);
            }
        };
        return so;
    }
    
    protected JsonConverter getConverter() {
        return JsonConverter.getInstance();
    }
}
