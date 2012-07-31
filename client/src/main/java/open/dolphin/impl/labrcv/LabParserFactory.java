package open.dolphin.impl.labrcv;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 */
public class LabParserFactory {

    public static LabResultParser getParser(String key) {
        try {
            String clsName = null;
            if (key.toLowerCase().endsWith(".dat")) {
                clsName = "open.dolphin.impl.labrcv.NLabParser";

            } else if (key.toLowerCase().endsWith(".dat2")) {
                clsName = "open.dolphin.impl.labrcv.Dat2Parser";

            } else if (key.toLowerCase().endsWith(".hl7")) {
//masuda^
                //clsName = "open.dolphin.impl.falco.HL7Falco";
                clsName = "open.dolphin.impl.labrcv.Hl7Parser";
//masuda$

            } else if (key.toLowerCase().endsWith(".txt")) {
                clsName = "open.dolphin.impl.labrcv.WolfParser";
//masuda^
            } else if (key.toLowerCase().endsWith("xml")) {
                clsName = "open.dolphin.impl.labrcv.MMLParser";
//masuda$
            }
            
            LabResultParser ret = (LabResultParser) Class.forName(clsName).newInstance();
            return ret;
            
        } catch (InstantiationException ex) {
            ex.printStackTrace(System.err);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace(System.err);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }
}
