package open.dolphin.rest;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.infomodel.PvtListModel;
import open.dolphin.infomodel.PvtMessageModel;
import open.dolphin.mbean.AsyncContextHolder;
import open.dolphin.session.PVTServiceBean;
import open.dolphin.session.PvtServiceMediator;

/**
 * PVTResource2
 *
 * @author masuda, Masuda Naika
 */

@Path("pvt2")
public class PVTResource2 extends AbstractResource {
    
    private static final int asyncTimeout = 60 * 1000 * 60; // 60 minutes

    private static final boolean debug = false;
    
    @Inject
    private PVTServiceBean pvtServiceBean;
    
    @Inject
    private PvtServiceMediator pvtServiceMediator;
    
    @Inject
    private AsyncContextHolder contextHolder;
    
    @Context
    private HttpServletRequest servletReq;

    public PVTResource2() {
    }

    @POST
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String postPvt(String json) {

        PatientVisitModel model = (PatientVisitModel)
                getConverter().fromJson(json, PatientVisitModel.class);

        // 関係構築
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        model.setFacilityId(fid);
        //model.getPatientModel().setFacilityId(fid);

        Collection<HealthInsuranceModel> c = model.getPatientModel().getHealthInsurances();
        if (c!= null && c.size() > 0) {
            for (HealthInsuranceModel hm : c) {
                hm.setPatient(model.getPatientModel());
            }
        }

        int result = pvtServiceBean.addPvt(model);
        String cntStr = String.valueOf(result);
        debug(cntStr);

        return cntStr;
    }
    
    @PUT
    @Path("state")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String putPvtState(String json) {
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());

        PvtMessageModel msg = (PvtMessageModel)
                getConverter().fromJson(json, PvtMessageModel.class);
        
        // クライアントから送られてきたmsgにfidを設定
        msg.setFacilityId(fid);
        
        int cnt = pvtServiceBean.updatePvtState(msg);

        return String.valueOf(cnt);
    }

    @DELETE
    @Path("{pvtPK}")
    public void deletePvt(@PathParam("pvtPK") String pkStr) {

        long pvtPK = Long.parseLong(pkStr);
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        
        // msgを作成
        PvtMessageModel msg = new PvtMessageModel();
        msg.setPvtPk(pvtPK);
        msg.setFacilityId(fid);

        int cnt = pvtServiceBean.removePvt(msg);

        debug(String.valueOf(cnt));
    }
    
    @GET
    @Path("subscribe")
    public void subscribePvtTopic() {

        String fid = getRemoteFacility(servletReq.getRemoteUser());
        final AsyncContext ac = servletReq.startAsync();
        // timeoutを設定
        ac.setTimeout(asyncTimeout);
        // requestにfidを記録しておく
        ac.getRequest().setAttribute("fid", fid);
        contextHolder.addAsyncContext(ac);
        //System.out.println("AsyncContextHolder size = " + contextHolder.getAsyncContextList().size());

        ac.addListener(new AsyncListener() {

            private void remove() {
                contextHolder.removeAsyncContext(ac);
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
    
    @GET
    @Path("pvtListModel")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getPvtListModel() {
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        PvtListModel model = pvtServiceMediator.getPvtListModel(fid);
        
        String json = getConverter().toJson(model);
        debug(json);
        
        return json;
    }
    
    @GET
    @Path("pvtMessage/{param}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public String getPvtMessageList(@PathParam("param") String param){
        
        String fid = getRemoteFacility(servletReq.getRemoteUser());
        int from = Integer.valueOf(param);
        
        List<PvtMessageModel> list = pvtServiceMediator.getPvtMessageList(fid, from);

        String json = getConverter().toJson(list);
        debug(json);
        
        return json;
    }
    
    // 参：きしだのはてな もっとJavaEE6っぽくcometチャットを実装する
    // http://d.hatena.ne.jp/nowokay/20110416/1302978207
    @GET
    @Path("nextId")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public String pushNextId() {
        String nextId = (String) servletReq.getAttribute("nextId");
        return nextId;
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
