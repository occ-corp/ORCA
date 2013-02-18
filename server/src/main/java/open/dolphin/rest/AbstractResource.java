package open.dolphin.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
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

    private static final String CHARSET_UTF8 = "; charset=UTF-8";
    protected static final String MEDIATYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + CHARSET_UTF8;
    protected static final String MEDIATYPE_TEXT_UTF8 = MediaType.TEXT_PLAIN + CHARSET_UTF8;

    protected static final Logger logger = Logger.getLogger(AbstractResource.class.getName());

    @Context
    protected HttpServletRequest servletReq;
    

    protected Date parseDate(String source) {
        try {
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_DF_FORMAT);
            return frmt.parse(source);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    protected void debug(String msg) {
        logger.info(msg);
    }

    protected String getRemoteFacility() {
        return (String) servletReq.getAttribute(IInfoModel.FID);
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
