package open.dolphin.mobile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.session.UserServiceBean;

/**
 * DigestFilterテスト
 * @author masuda, Masuda Naika
 */
@WebFilter(urlPatterns = {"/mobile/*"})
public class DigestFilter implements Filter {
    
    private static final boolean WARN = false;
    private static final boolean INFO = true;
    private static final Logger logger = Logger.getLogger(DigestFilter.class.getSimpleName());
    
    private static final String COLON = ":";
    private static final String EQUALS = "=";
    private static final String CAMMA = ",";

    private static final String UNAUTHORIZED_USER = "Unauthorized user: ";
    private static final String AUTHORIZATION = "Authorization";

    private static final String privateKey = "OpenDolphinMobile";
    private static final String dolphinRealm = "OpenDolphin";
    
    @Inject
    private UserServiceBean userService;
    
    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void destroy() {
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        
        Map<String, String> digestMap = getDigestMap(req);
        
        if (digestMap == null) {
            requestChallenge(response);
            return;
        }

        String username = digestMap.get("username");
        String[] fidPass = userService.getFidAndPassword(username);
        
        if (fidPass == null) {
            requestChallenge(response);
            return;
        }
        
        String fid = fidPass[0];
        String password = fidPass[1];
        digestMap.put("password", password);

        String clientResponse = digestMap.get("response");
        String serverResponse = createServerResponse(digestMap);
        boolean authenticated = clientResponse.equals(serverResponse);

        if (!authenticated) {
            StringBuilder sb = new StringBuilder();
            sb.append(UNAUTHORIZED_USER);
            sb.append(username).append(": ").append(req.getRequestURI());
            String msg = sb.toString();
            info(msg);
            requestChallenge(response);
            return;
        }
        
        req.setAttribute(IInfoModel.FID, fid);

        chain.doFilter(req, response);
    }
    
    private void requestChallenge(ServletResponse response) throws IOException {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Digest realm=\"").append(dolphinRealm).append("\",");
        sb.append("qop=\"auth,auth-int\",");
        sb.append("nonce=\"").append(getNonce()).append("\",");
        sb.append("opaque=\"").append(getOpaque()).append("\"");
        String header = sb.toString();

        HttpServletResponse res = (HttpServletResponse) response;
        res.setHeader("WWW-Authenticate", header);
        res.sendError(401);
    }
    
    private void warn(String msg) {
        if (WARN) {
            logger.warning(msg);
        }
    }

    private void info(String msg) {
        if (INFO) {
            logger.info(msg);
        }
    }
    
    private Map<String, String> getDigestMap(HttpServletRequest req) {

        String header = req.getHeader(AUTHORIZATION);

        if (header == null) {
            return null;
        }
        
        Map<String, String> map = new HashMap<String, String>();

        String[] attrs = header.substring("Digest ".length()).split(CAMMA);
        for (String attr : attrs) {
            int pos = attr.indexOf(EQUALS);
            String name = attr.substring(0, pos).trim();
            String value = attr.substring(pos + 1).trim().replace("\"", "");
            map.put(name, value);
        }
        
        String httpmethod = req.getMethod();
        map.put("httpmethod", httpmethod);

        if ("auth-int".equals(map.get("qop"))) {
            String entitybody = readEntityBody(req);
            map.put("entitybody", entitybody);
        }
        
        return map;
    }
    
    private String createServerResponse(Map<String, String> map) {

        String qop = map.get("qop");
        String username = map.get("username");
        String password = map.get("password");
        String nonce = map.get("nonce");
        String cnonce = map.get("cnonce");
        String realm = map.get("realm");
        String httpmethod = map.get("httpmethod");
        String uri = map.get("uri");
        String nc = map.get("nc");
        
        StringBuilder sb = new StringBuilder();
        
        if ("MD5-sess".equals(map.get("algorithm"))) {
            sb.append(username).append(COLON);
            sb.append(realm).append(COLON);
            sb.append(password);
            String urpMD5 = getMD5Hex(sb.toString());
            sb = new StringBuilder();
            sb.append(urpMD5).append(COLON);
            sb.append(nonce).append(COLON);
            sb.append(cnonce);
        } else {
            sb.append(username).append(COLON);
            sb.append(realm).append(COLON);
            sb.append(password);
        }
        String ha1 = getMD5Hex(sb.toString());

        sb = new StringBuilder();
        if ("auth-int".equals(qop)) {
            String entitybody = map.get("entitybody");
            sb.append(httpmethod).append(COLON);
            sb.append(uri).append(COLON);
            sb.append(getMD5Hex(entitybody));
        } else {
            sb.append(httpmethod).append(COLON);
            sb.append(uri);
        }
        String ha2 = getMD5Hex(sb.toString());

        sb = new StringBuilder();
        if ("auth".equals(qop) || "auth-int".equals(qop)) {
            sb.append(ha1).append(COLON);
            sb.append(nonce).append(COLON);
            sb.append(nc).append(COLON);
            sb.append(cnonce).append(COLON);
            sb.append(qop).append(COLON);
            sb.append(ha2);
        } else {
            sb.append(ha1).append(COLON);
            sb.append(nonce).append(COLON);
            sb.append(ha2);
        }
        String ret = getMD5Hex(sb.toString());

        return ret;
    }

    private String getMD5Hex(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            byte[] bytes = md5.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(Integer.toHexString((b & 0xF0) >> 4));
                sb.append(Integer.toHexString(b & 0xF));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }
    
    private String getNonce() {
        long t = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(t)).append(":");   // time-stamp
        sb.append(String.valueOf(t)).append(":");   // ETag
        sb.append(privateKey);
        return getMD5Hex(sb.toString());
    }
    
    private String getOpaque() {
        Random r = new Random(System.currentTimeMillis());
        long l = r.nextLong();
        return getMD5Hex(String.valueOf(l));
    }
    
    private String readEntityBody(HttpServletRequest request) {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        ReadableByteChannel channel = null;
        try {
            channel = Channels.newChannel(request.getInputStream());
            ByteBuffer buf = ByteBuffer.allocate(512);
            while (true) {
                buf.clear();
                int readLen = channel.read(buf);
                if (readLen == -1) {
                    break;
                }
                buf.flip();
                bos.write(buf.array(), 0, readLen);
            }
            bos.flush();
            
            String body = new String(baos.toByteArray());
            return body;
            
        } catch (IOException ex) {
            return "";
        } finally {
            try {
                channel.close();
            } catch (IOException ex) {
            }
        }
    }
}
