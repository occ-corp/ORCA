package open.dolphin.tr;

import java.io.Serializable;
import open.dolphin.infomodel.SchemaModel;

/**
 * SchemaList
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public class SchemaList implements Serializable {

    private SchemaModel[] schemaList;

    /** Creates new ImageList */
    public SchemaList() {
    }
    
    public SchemaList(SchemaModel[] schemaList) {
        this.schemaList = schemaList;
    }
    
    public SchemaModel[] getSchemaList() {
        return schemaList;
    }
    
    public void setSchemaList(SchemaModel[] schemaList) {
        this.schemaList = schemaList;
    }
}