package open.dolphin.rest;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.ChartEventModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.mbean.ServletContextHolder;
import open.dolphin.session.ChartEventServiceBean;

/**
 * ChartEventResource
 * @author masuda, Masuda Naika
 */
@Path("chartEvent")
public class ChartEventResource extends AbstractResource {
    
    private static final boolean debug = false;
    
    private static final int asyncTimeout = 60 * 1000 * 60; // 60 minutes
    
    public static final String DISPATCH_URL = "/openSource/chartEvent/dispatch";
    public static final String KEY_NAME = "chartEvent";
    
    @Inject
    private ChartEventServiceBean eventServiceBean;
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @GET
    @Path("subscribe")
    public void subscribe() {

        String fid = getRemoteFacility();
        String clientUUID = servletReq.getHeader(IInfoModel.CLIENT_UUID);
        
        final AsyncContext ac = servletReq.startAsync();
        // timeoutを設定
        ac.setTimeout(asyncTimeout);
        // requestにfid, clientUUIDを記録しておく
        ac.getRequest().setAttribute(IInfoModel.FID, fid);
        ac.getRequest().setAttribute(IInfoModel.CLIENT_UUID, clientUUID);
        contextHolder.addAsyncContext(ac);

        ac.addListener(new AsyncListener() {

            private void remove() {
                // JBOSS終了時にぬるぽ？
                try {
                    contextHolder.removeAsyncContext(ac);
                } catch (NullPointerException ex) {
                }
            }

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                remove();
                //System.out.println("ON TIMEOUT");
                //event.getThrowable().printStackTrace(System.out);
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                remove();
                //System.out.println("ON ERROR");
                //event.getThrowable().printStackTrace(System.out);
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
            }
        });
    }
    
    @PUT
    @Path("event")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putChartEvent(String json) {
        
        ChartEventModel msg = (ChartEventModel)
                getConverter().fromJson(json, ChartEventModel.class);

        int cnt = eventServiceBean.processChartEvent(msg);

        return String.valueOf(cnt);
    }
    
    // 参：きしだのはてな もっとJavaEE6っぽくcometチャットを実装する
    // http://d.hatena.ne.jp/nowokay/20110416/1302978207
    @GET
    @Path("dispatch")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response deliverChartEvent() {
        
        ChartEventModel msg = (ChartEventModel) servletReq.getAttribute(KEY_NAME);
        String json = getConverter().toJson(msg);
        return Response.ok(json).build();
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
