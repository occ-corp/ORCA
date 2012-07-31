package open.dolphin.rest;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import open.dolphin.session.UserServiceBean;

/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
@WebFilter(urlPatterns = {"/openSource/*"}, asyncSupported = true)
public class LogFilter implements Filter {
    
    private static final boolean WARN = false;
    private static final boolean INFO = true;

    private static final Logger logger = Logger.getLogger(LogFilter.class.getName());

    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";
    private static final String UNAUTHORIZED_USER = "Unauthorized user: ";

    @Inject
    private UserServiceBean userService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String userName = req.getHeader(USER_NAME);
        String password = req.getHeader(PASSWORD);
        //System.err.println(userName);
        //System.err.println(password);
        
        ConcurrentHashMap<String, String> userMap = UserCache.getInstance().getMap();
        boolean authentication = password.equals(userMap.get(userName));
        
        if (!authentication) {
            authentication = userService.authenticate(userName, password);
            if (!authentication) {
                HttpServletResponse res = (HttpServletResponse) response;
                StringBuilder sbd = new StringBuilder();
                sbd.append(UNAUTHORIZED_USER);
                sbd.append(userName).append(": ").append(req.getRequestURI());
                String msg = sbd.toString();
                warn(msg);
                res.sendError(401);
                return;
            } else {
                userMap.put(userName, password);
            }
        }

        BlockWrapper wrapper = new BlockWrapper(req);
        wrapper.setRemoteUser(userName);

        StringBuilder sb = new StringBuilder();
        sb.append(wrapper.getRemoteAddr()).append(" ");
        sb.append(wrapper.getShortUser()).append(" ");
        sb.append(wrapper.getMethod()).append(" ");
        String query = wrapper.getQueryString();
        if (query != null && !query.isEmpty()) {
            sb.append(" ").append(query);
        }
        String msg = sb.toString();
        info(msg);

        chain.doFilter(wrapper, response);
    }

    @Override
    public void destroy() {
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
}
