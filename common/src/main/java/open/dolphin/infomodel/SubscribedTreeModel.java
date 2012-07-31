package open.dolphin.infomodel;

import javax.persistence.*;

/**
 * StampTreeXML のホルダクラス。
 * ユーザがインポートしているTreeクラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
@Entity
@Table(name="d_subscribed_tree")
public class SubscribedTreeModel extends InfoModel {
    
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    //@ManyToOne(fetch = FetchType.LAZY)
    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private UserModel user;
    
    @Column(nullable=false)
    private long treeId;
    
    public SubscribedTreeModel() {
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getTreeId() {
        return treeId;
    }
    
    public void setTreeId(long treeId) {
        this.treeId = treeId;
    }
    
    public UserModel getUserModel() {
        return user;
    }
    
    public void setUserModel(UserModel user) {
        this.user = user;
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (id ^ (id >>> 32));
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SubscribedTreeModel other = (SubscribedTreeModel) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
