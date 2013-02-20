package open.dolphin.util;

import java.util.concurrent.ThreadFactory;

/**
 * NamedThreadFactory
 * @author masuda, Masuda Naika
 */
public class NamedThreadFactory implements ThreadFactory {
    
    private String name;
    private int number;
            
    public NamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name + String.valueOf(++number));
    }
}
