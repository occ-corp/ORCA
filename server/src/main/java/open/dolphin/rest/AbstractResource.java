package open.dolphin.rest;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
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
    protected static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
    protected static final SimpleDateFormat ISO_DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    protected static final SimpleDateFormat MML_Df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    private static final String CHARSET_UTF8 = "; charset=UTF-8";
    protected static final String MEDIATYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + CHARSET_UTF8;
    protected static final String MEDIATYPE_TEXT_UTF8 = MediaType.TEXT_PLAIN + CHARSET_UTF8;

    protected static final Logger logger = Logger.getLogger(AbstractResource.class.getName());

    protected static Date parseDate(String source) {
        try {
            return ISO_DF.parse(source);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    protected void debug(String msg) {
        logger.info(msg);
    }

    protected static String getRemoteFacility(String remoteUser) {
        int index = remoteUser.indexOf(IInfoModel.COMPOSITE_KEY_MAKER);
        return remoteUser.substring(0, index);
    }

    protected static String getFidPid(String remoteUser, String pid) {
        StringBuilder sb = new StringBuilder();
        sb.append(getRemoteFacility(remoteUser));
        sb.append(IInfoModel.COMPOSITE_KEY_MAKER);
        sb.append(pid);
        return sb.toString();
    }
    
    
    
    protected JsonConverter getConverter() {
        return JsonConverter.getInstance();
    }

    protected List<Long> toLongList(String params) {
        String[] strArray  = params.split(CAMMA);
        List<Long> ret = new ArrayList<Long>();
        for (String s : strArray) {
            ret.add(Long.valueOf(s));
        }
        return ret;
    }
    
    protected List<String> toStrList(String params) {
        String[] strArray  = params.split(CAMMA);
        return Arrays.asList(strArray);
    }
    
    protected String fromList(List list) {
        
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
