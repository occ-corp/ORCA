package open.dolphin.infomodel;

import java.io.Serializable;
import javax.persistence.*;

/**
 * 
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_room_model")
public class RoomModel implements Serializable {
    
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    private String ward;
    
    private String roomNumber;
    
    private Integer extraCharge;
    

    public void setId(long id) {
        this.id = id;
    }    
    
    public long getId() {
        return id;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }
    
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
    
    public void setExtraCharge(int extraCharge) {
        this.extraCharge = extraCharge;
    }
    
    public String getWard() {
        return ward;
    }
    
    public String getRoomNumber() {
        return roomNumber;
    }
    
    public int getExtraCharge() {
        return extraCharge == null ? 0 : extraCharge;
    }

}
