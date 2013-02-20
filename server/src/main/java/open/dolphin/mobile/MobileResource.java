package open.dolphin.mobile;

import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.session.ChartEventServiceBean;

/**
 * TEST
 * @author masuda, Masuda Naika
 */
@Path("mobile")
public class MobileResource {
    
    private static final String CHARSET_UTF8 = "; charset=UTF-8";
    private static final String MEDIATYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + CHARSET_UTF8;
    private static final String MEDIATYPE_TEXT_UTF8 = MediaType.TEXT_PLAIN + CHARSET_UTF8;
    private static final String MEDIATYPE_HTML_UTF8 = MediaType.TEXT_HTML + CHARSET_UTF8;
    
    @Context
    protected HttpServletRequest servletReq;
    
    @Inject
    private ChartEventServiceBean eventServiceBean;
    
    @GET
    @Path("/pvt")
    @Produces(MEDIATYPE_HTML_UTF8)
    public String getPvt() {
        String fid = (String) servletReq.getAttribute(IInfoModel.FID);
        List<PatientVisitModel> pvtList = eventServiceBean.getPvtList(fid);
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><BODY>");
        for (PatientVisitModel pvt : pvtList) {
            sb.append(pvt.getPvtDate()).append(" ");
            sb.append(pvt.getPatientName()).append("<BR>");
        }
        sb.append("</BODY></HTML>");
        return sb.toString();
    }
    
}
