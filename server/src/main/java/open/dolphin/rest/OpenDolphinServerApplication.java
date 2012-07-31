package open.dolphin.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * ApplicationPathアノテーションで、JAX-RSのルートディレクトリを指定
 * @author masuda, Masuda Naika
 */
@ApplicationPath("openSource")
public class OpenDolphinServerApplication extends Application {
}
