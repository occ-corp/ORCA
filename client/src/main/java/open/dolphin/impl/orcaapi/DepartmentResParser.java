package open.dolphin.impl.orcaapi;

import java.util.ArrayList;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;

/**
 * ORCA API46のDepartment_Informationをパースする
 * 
 * @author masuda, Masuda Naika
 */
public class DepartmentResParser extends AbstractOrcaApiParser {
    
    private enum ATTRIBUTE {
            Code, WholeName, Name1, Name2, Name3, Receipt_Code, other
    };

    
    public DepartmentResParser(Document doc) {
        super(doc);
    }
    
    public List<DepartmentInfo> getList() {
        return xml2 ? getList2() : getList1();
    }
    
    private List<DepartmentInfo> getList1() {
        
        List<DepartmentInfo> list = new ArrayList<DepartmentInfo>();
        Element arrayElm = doc.getRootElement().getChild(RECORD).getChild(RECORD).getChild(ARRAY);
        
        //String resName = arrayElm.getAttributeValue(NAME);
        for (Element elm : arrayElm.getChildren()) {
            for (Element elm1 : elm.getChildren()) {
                DepartmentInfo info = new DepartmentInfo();
                String name = elm1.getAttributeValue(NAME);
                String value = elm1.getText();
                ATTRIBUTE attr = getValue(name);
                switch(attr) {
                    case Code:
                        info.setCode(value);
                        break;
                    case WholeName:
                        info.setWholeName(value);
                        break;
                    case Name1:
                        info.setName1(value);
                        break;
                    case Name2:
                        info.setName2(value);
                        break;
                    case Name3:
                        info.setName3(value);
                        break;
                    case Receipt_Code:
                        info.setReceiptCode(value);
                        break;
                    default:
                        break;
                }
                
                String code = info.getCode();
                if (code != null && !code.isEmpty()) {
                    list.add(info);
                }
            }
        }
        if (list.isEmpty()) {
            list = null;
        }
        return list;
    }
    
    private List<DepartmentInfo> getList2() {

        Element record = doc.getRootElement().getChild("departmentres");
        if (record == null) {
            return null;
        }
        Element array = record.getChild("Department_Information");
        if (array == null) {
            return null;
        }

        List<DepartmentInfo> list = new ArrayList<DepartmentInfo>();
        for (Element elm : array.getChildren()) {
            DepartmentInfo info = new DepartmentInfo();
            String code = elm.getChildText("Code");
            info.setCode(code);
            info.setWholeName(elm.getChildText("WholeName"));
            info.setName1(elm.getChildText("Name1"));
            info.setName2(elm.getChildText("Name2"));
            info.setName3(elm.getChildText("Name3"));
            info.setReceiptCode(elm.getChildText("Receipt_Code"));
            if (code != null && !code.isEmpty()) {
                list.add(info);
            }

        }

        if (list.isEmpty()) {
            list = null;
        }
        return list;
    }
    
    private ATTRIBUTE getValue(String name) {
        ATTRIBUTE attr = ATTRIBUTE.other;
        try {
            attr = ATTRIBUTE.valueOf(name);
        } catch (IllegalArgumentException ex) {
        }
        return attr;
    }
}
