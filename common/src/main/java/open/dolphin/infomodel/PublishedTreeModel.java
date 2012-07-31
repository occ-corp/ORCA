package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import javax.persistence.*;

/**
 * PublishedTreeModel
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
@Entity
@Table(name="d_published_tree")
public class PublishedTreeModel extends InfoModel implements IStampTreeModel {
    
    @Id
    private long id;
    
    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private UserModel user;
    
    // TreeSetの名称
    @Column(nullable=false)
    private String name;
    
    // OID or Public
    // OID の時は施設用
    @Column(nullable=false)
    private String publishType;
    
    // Treeのカテゴリ
    @Column(nullable=false)
    private String category;
    
    // 団体名等
    @Column(nullable=false)
    private String partyName;
    
    // URL
    @Column(nullable=false)
    private String url;
    
    // 説明
    @Column(nullable=false)
    private String description;
    
    // 公開した日
    @Column(nullable=false)
    @Temporal(value = TemporalType.DATE)
    private Date publishedDate;
    
    @Transient
    private String treeXml;
    
    //masuda Lobのサイズを拡張する
    //@Column(nullable=false)
    @Column(nullable=false, length=16777215)    // MEDIUMBLOB
    @Lob
    private byte[] treeBytes;
    
    // 更新した日
    @Column(nullable=false)
    @Temporal(value = TemporalType.DATE)
    private Date lastUpdated;
    
    @JsonIgnore
    @Transient
    private boolean imported;
    
//    @Version
//    private int version;
    
    public PublishedTreeModel() {
    }

    @Override
    public long getId() {
        return id;
    }
    
    @Override
    public void setId(long id) {
        this.id = id;
    }
    
    @Override
    public UserModel getUserModel() {
        return user;
    }
    
    @Override
    public void setUserModel(UserModel user) {
        this.user = user;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getPublishType() {
        return publishType;
    }
    

    @Override
    public void setPublishType(String publishType) {
        this.publishType = publishType;
    }
    
    @Override
    public String getCategory() {
        return category;
    }
    
    @Override
    public void setCategory(String category) {
        this.category = category;
    }
    
    @Override
    public String getPartyName() {
        return partyName;
    }
    
    @Override
    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }
    
    @Override
    public String getUrl() {
        return url;
    }
    
    @Override
    public void setUrl(String url) {
        this.url = url;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public Date getPublishedDate() {
        return publishedDate;
    }
    
    @Override
    public void setPublishedDate(Date publishedDate) {
        this.publishedDate = publishedDate;
    }
    
    @Override
    public byte[] getTreeBytes() {
        return treeBytes;
    }
    
    @Override
    public void setTreeBytes(byte[] treeBytes) {
        this.treeBytes = treeBytes;
    }
    
    @Override
    public String getTreeXml() {
        return treeXml;
    }
    
    @Override
    public void setTreeXml(String treeXml) {
        this.treeXml = treeXml;
    }
    
    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    @Override
    public void setLastUpdated(Date updatedDate) {
        this.lastUpdated = updatedDate;
    }
    
    public boolean isImported() {
        return imported;
    }
    
    public void setImported(boolean imported) {
        this.imported = imported;
    }
    
//    public int getVersion() {
//        return version;
//        return 0;
//    }
//    
//    public void setVersion(int version) {
//        //this.version = version;
//    }
    
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
        final PublishedTreeModel other = (PublishedTreeModel) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
