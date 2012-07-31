package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.swing.ImageIcon;

/**
 * SchemaModel
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 *
 */
@Entity
@Table(name = "d_image")
public class SchemaModel extends KarteEntryBean {
    
    @Embedded
    private ExtRefModel extRef;
    
    @Lob
    @Column(nullable=false, length=16777215)    // MEDIUMBLOB
    private byte[] jpegByte;
    
    //@JsonBackReference
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="doc_id", nullable=false)
    private DocumentModel document;
    
    // Comaptible props
    @JsonIgnore
    @Transient
    private String fileName;
    
    @JsonIgnore
    @Transient
    private ImageIcon icon;
    
    @JsonIgnore
    @Transient
    private int imageNumber;

//    private String url;
//
//    private String sop;
    
    
    /** Creates new Schema */
    public SchemaModel() {
    }
    
    public ExtRefModel getExtRefModel() {
        return extRef;
    }
    
    public void setExtRefModel(ExtRefModel val) {
        extRef = val;
    }
    
    public DocumentModel getDocumentModel() {
        return document;
    }
    
    public void setDocumentModel(DocumentModel document) {
        this.document = document;
    }
    
    public byte[] getJpegByte() {
        return jpegByte;
    }
    
    public void setJpegByte(byte[] jpegByte) {
        this.jpegByte = jpegByte;
    }
    
    public ImageIcon getIcon() {
        return icon;
    }
    
    public void setIcon(ImageIcon val) {
        icon = val;
    }
    
    public int getImageNumber() {
        return imageNumber;
    }
    
    public void setImageNumber(int imageNumber) {
        this.imageNumber = imageNumber;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String val) {
        fileName = val;
    }
    
    public IInfoModel getModel() {
        return (IInfoModel)getExtRefModel();
    }
    
    public void setModel(IInfoModel val) {
        setExtRefModel((ExtRefModel)val);
    }
    
    /**
     * 確定日及びイメージ番号で比較する。
     * @param other
     * @return
     */
    @Override
    public int compareTo(Object other) {
        int result = super.compareTo(other);
        if (result == 0) {
            // primittive なので比較はOK
            int no1 = getImageNumber();
            int no2 = ((SchemaModel) other).getImageNumber();
            result = no1 - no2;
        }
        return result;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        SchemaModel ret = new SchemaModel();
        ret.setConfirmed(this.getConfirmed());
        ret.setEnded(this.getEnded());
        ret.setExtRefModel((ExtRefModel)this.getExtRefModel().clone());
        ret.setFileName(this.getFileName());
        ret.setFirstConfirmed(this.getConfirmed());
        ret.setImageNumber(this.getImageNumber());
        ret.setLinkId(this.getLinkId());
        ret.setLinkRelation(this.getLinkRelation());
        ret.setRecorded(this.getRecorded());
        ret.setStarted(this.getStarted());
        ret.setStatus(this.getStatus());

        if (this.getIcon()!=null) {
            ret.setIcon(new ImageIcon(this.getIcon().getImage()));
        }

        if (this.getJpegByte()!=null) {
            byte[] dest = new byte[this.getJpegByte().length];
            System.arraycopy(this.getJpegByte(), 0, dest, 0, this.getJpegByte().length);
            ret.setJpegByte(dest);
        }

        return ret;
    }
}
