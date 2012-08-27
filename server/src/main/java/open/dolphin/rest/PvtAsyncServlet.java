package open.dolphin.rest;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.session.PvtServiceMediator;

/**
 * AsyncServertを分離
 * @author masuda, Masuda Naika
 */
@WebServlet(urlPatterns = {"/openSource/pvt2/subscribe"}, asyncSupported = true)
public class PvtAsyncServlet extends HttpServlet {

    @Inject
    private PvtServiceMediator pvtServiceMediator;
    
    @Override
    protected void doGet(HttpServletRequest servletReq, HttpServletResponse servletRes) 
            throws ServletException, IOException {
        
        // JBOSS終了時にぬるぽ
        if (pvtServiceMediator == null) {
            return;
        }
        
        final AsyncContext ac = servletReq.startAsync();
        final String fid = getRemoteFacility(servletReq.getRemoteUser());
        pvtServiceMediator.subscribePvtTopic(ac, fid);
        
        ac.addListener(new AsyncListener() {
            
            private void remove() {
                pvtServiceMediator.removeAsyncContext(fid, ac);
            }

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
            }
            
            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                //System.out.println("ON TIMEOUT");
                //event.getThrowable().printStackTrace(System.out);
                remove();
            }
            
            @Override
            public void onError(AsyncEvent event) throws IOException {
                //System.out.println("ON ERROR");
                //event.getThrowable().printStackTrace(System.out);
                remove();
            }
            
            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
            }
        });
    }
    
    private String getRemoteFacility(String remoteUser) {
        int index = remoteUser.indexOf(IInfoModel.COMPOSITE_KEY_MAKER);
        return remoteUser.substring(0, index);
    }
    
}
