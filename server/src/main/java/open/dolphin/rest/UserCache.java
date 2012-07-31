
package open.dolphin.rest;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ユーザーのキャッシュ
 * @author masuda, Masuda Naika
 */
public class UserCache {
    
    private static UserCache instance;
    private ConcurrentHashMap<String, String> map;
    
    static {
        instance = new UserCache();
    }
    
    private UserCache(){
        map = new ConcurrentHashMap<String, String>();
    }
    
    public static UserCache getInstance() {
        return instance;
    }
    
    public ConcurrentHashMap<String, String> getMap() {
        return map;
    }
}
