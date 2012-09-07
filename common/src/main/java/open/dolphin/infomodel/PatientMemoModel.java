package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

/**
 * MemoModel
 *
 * @author Minagawa, Kazushi
 */
@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
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
