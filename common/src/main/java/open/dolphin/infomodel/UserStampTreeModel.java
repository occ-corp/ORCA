
package open.dolphin.infomodel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.List;

/**
 * interfaceはjsonで送りにくいので…
 * @author masuda, Masuda Naika
 */
public class UserStampTreeModel {
    
    @JsonDeserialize(contentAs=StampTreeModel.class)
    private List<StampTreeModel> stampTreeList;
    
    @JsonDeserialize(contentAs=PublishedTreeModel.class)
    private List<PublishedTreeModel> publishedList;
    
    
    public UserStampTreeModel() {
    }
    
    public void setStampTreeList(List<StampTreeModel> list) {
        stampTreeList = list;
    }
    
    public void setPublishedList(List<PublishedTreeModel> list) {
        publishedList = list;
    }
    
    public List<StampTreeModel> getStampTreeList() {
        return stampTreeList;
    }
    
    public List<PublishedTreeModel> getPublishedList() {
        return publishedList;
    }
    
    public List<IStampTreeModel> getTreeList() {
        List<IStampTreeModel> ret = new ArrayList<IStampTreeModel>();
        if (stampTreeList != null) {
            ret.addAll(stampTreeList);
        }
        if (publishedList != null) {
            ret.addAll(publishedList);
        }
        return ret;
    }
}
