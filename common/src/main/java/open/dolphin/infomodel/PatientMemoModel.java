package open.dolphin.infomodel;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

/**
 * MemoModel
 *
 * @author Minagawa, Kazushi
 *
 */
@Entity
@Table(name = "d_patient_memo")
public class PatientMemoModel extends KarteEntryBean {
    
    @Lob
    @Type(type="org.hibernate.type.StringClobType")
    private String memo;
    
    public PatientMemoModel() {
    }
    
    public String getMemo() {
        return memo;
    }
    
    public void setMemo(String memo) {
        this.memo = memo;
    }
}
