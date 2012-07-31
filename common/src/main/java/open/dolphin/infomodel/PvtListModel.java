package open.dolphin.infomodel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * PvtListModel
 * 
 * @author masuda, Masuda Naika
 */
public class PvtListModel {
    
    private int nextId;
    
    @JsonDeserialize(contentAs=PatientVisitModel.class)
    private List<PatientVisitModel> pvtList;
    
    public PvtListModel() {
    }
    
    public void setNextId(int id) {
        nextId = id;
    }
    public int getNextId() {
        return nextId;
    }
    
    public void setPvtList(List<PatientVisitModel> list) {
        pvtList = list;
    }
    public List<PatientVisitModel> getPvtList() {
        return pvtList;
    }
}
