package open.dolphin.infomodel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * PvtListModel
 * 
 * @author masuda, Masuda Naika
 */
public class PvtListModel {
    
    private int currentId;
    
    @JsonDeserialize(contentAs=PatientVisitModel.class)
    private List<PatientVisitModel> pvtList;
    
    public PvtListModel() {
    }
    
    public void setCurrentId(int id) {
        currentId = id;
    }
    public int getCurrentId() {
        return currentId;
    }
    
    public void setPvtList(List<PatientVisitModel> list) {
        pvtList = list;
    }
    public List<PatientVisitModel> getPvtList() {
        return pvtList;
    }
}
