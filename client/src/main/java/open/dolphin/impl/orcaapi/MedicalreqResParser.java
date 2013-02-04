package open.dolphin.impl.orcaapi;

import org.jdom2.Document;


/**
 * MedicalreqResParser
 * 
 * @author masuda, Masuda Naika
 */
public class MedicalreqResParser extends AbstractOrcaApiParser {
    
    public MedicalreqResParser(Document doc) {
        super(doc);
    }
    
    public String getMedicalUid() {
        final String name = "Medical_Uid";
        return xml2 
                ? getElementText2(name) 
                : getElementText(name);
    }
}
