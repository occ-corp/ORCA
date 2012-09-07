package open.dolphin.rest;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import open.dolphin.infomodel.StateMsgModel;
import open.dolphin.mbean.ServletContextHolder;
import open.dolphin.session.StateServiceBean;

/**
 *
 * @author masuda
 */
@Path("chartState")
public class StateResource extends AbstractResource {
    
    private static final boolean debug = false;
    
    private static final int asyncTimeout = 60 * 1000 * 60; // 60 minutes
    
    private static final String CLIENT_UUID = "clientUUID";
    
    @Inject
    private StateServiceBean stateServiceBean;
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @Context
    private HttpServletRequest servletReq;
    

    @GET
    @Path("subscribe/{id}")
    public void listenChartState(@PathParam("id") String id) {

        String fid = getRemoteFacility(servletReq.getRemoteUser());
        String clientUUID = servletReq.getHeader("clientUUID");
        
        final AsyncContext ac = servletReq.startAsync();
        // timeoutを設定
        ac.setTimeout(asyncTimeout);
        // requestにfid, msgIdを記録しておく
        ac.getRequest().setAttribute("fid", fid);
        ac.getRequest().setAttribute("id", Integer.valueOf(id));
        ac.getRequest().setAttribute(CLIENT_UUID, clientUUID);
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
    @Path("state")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putChartState(String json) {
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());

        StateMsgModel msg = (StateMsgModel)
                getConverter().fromJson(json, StateMsgModel.class);
        
        // クライアントから送られてきたmsgにfidを設定
        msg.setFacilityId(fid);
        
        int cnt = stateServiceBean.updateChartState(msg);

        return String.valueOf(cnt);
    }
    
    @GET
    @Path("msgList/{param}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getStateMsgList(@PathParam("param") String param){
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        int currentId = Integer.valueOf(param);
        
        List<StateMsgModel> list = stateServiceBean.getStateMsgList(fid, currentId);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }
    
    // 参：きしだのはてな もっとJavaEE6っぽくcometチャットを実装する
    // http://d.hatena.ne.jp/nowokay/20110416/1302978207
    @GET
    @Path("currentId")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String getCurrentId() {
        // ac.dispatchならattributeにfidが設定されている
        String fid = (String) servletReq.getAttribute("fid");
        return (String) servletReq.getAttribute("currentId");
    }
    
    @GET
    @Path("initialId")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String getInitialId() {
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        int msgCounter = stateServiceBean.getMsgCounter(fid);
        return String.valueOf(msgCounter);
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
