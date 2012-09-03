package open.dolphin.mbean;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import open.dolphin.infomodel.ChartStateMsgModel;
import open.dolphin.infomodel.PatientVisitModel;

/**
 *
 * @author masuda, Masuda Naika
 */
public class FacilityContext {

    private final List<ChartStateMsgModel> chartStateMsgList;
    private final List<PatientVisitModel> pvtList;

    public FacilityContext() {
        chartStateMsgList = new CopyOnWriteArrayList<ChartStateMsgModel>();
        pvtList = new CopyOnWriteArrayList<PatientVisitModel>();
    }
    
    public List<ChartStateMsgModel> getChartStateMsgList() {
        return chartStateMsgList;
    }
    
    public List<PatientVisitModel> getPvtList() {
        return pvtList;
    }
    
    
}

