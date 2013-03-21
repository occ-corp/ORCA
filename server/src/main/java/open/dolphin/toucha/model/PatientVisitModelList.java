package open.dolphin.toucha.model;

import java.util.List;

/**
 * PatientVisitModelList
 * @author masuda, Masuda Naika
 */
public class PatientVisitModelList {
    
    private String pvtDate;
    private List<PatientVisitModelS> pvtList;
    
    public PatientVisitModelList() {
    }
    
    public void setPvtDate(String pvtDate) {
        this.pvtDate = pvtDate;
    }
    public void setPvtList(List<PatientVisitModelS> list) {
        pvtList = list;
    }
    public String getPvtDate() {
        return pvtDate;
    }
    public List<PatientVisitModelS> getPvtList() {
        return pvtList;
    }
}
