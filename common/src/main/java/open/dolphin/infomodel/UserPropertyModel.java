
package open.dolphin.infomodel;

import java.io.Serializable;
import javax.persistence.*;

/**
 * クライアントの設定保存項目
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_userProperty")
public class UserPropertyModel implements Serializable {
    
    private static final String[] commonKeys = {
        "baseURI", "facilityId", "jmariCode",
        "claimAddress", "claimPort", "CLAIM01", "claimHostName", "orcaUserId", "orcaUserPassword",
        "pvtOnServer", "fevOnServer", "fevSharePath",
        "pacsServerIp", "pacsServerPort", "pacsServerAE",
        "useSSL"
    };
    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    @Column(name = "c_key")
    private String key;
    
    @Column(name = "c_value", length = 16384)   // MySqlではLONGVARCHARになっちゃう…
    private String value;
    
    private String facilityId;
    
    private String userId;
    
    public UserPropertyModel() {
    }
    
    public long getId() {
        return id;
    }
    public String getKey() {
        return key;
    }
    public String getValue() {
        return value;
    }
    public String getFacilityId() {
        return facilityId;
    }
    public String getUserId() {
        return userId;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public void setValue(String value){
        this.value = value;
    }
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public boolean isFacilityCommon(String testKey) {
        for (String commonKey : commonKeys) {
            if (commonKey.equals(testKey)) {
                return true;
            }
        }
        return false;
    }
}
