package open.dolphin.mbean;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * JndiUtil
 * @author masuda, Masuda Naika
 */
public class JndiUtil {
    
    private static final String warName = "OpenDolphin-server-2.3";
    
    public static Object getJndiResource(Class cls) throws NamingException {
        StringBuilder sb = new StringBuilder();
        sb.append("java:global/");
        sb.append(warName).append("/");
        sb.append(cls.getSimpleName());
        InitialContext ic = new InitialContext();
        Object obj = ic.lookup(sb.toString());
        return obj;
    }
}
