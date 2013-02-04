package open.dolphin.impl.orcaapi;

import java.util.ArrayList;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;

/**
 * ORCA API46のPhysician_Informationをパースする
 * 
 * @author masuda, Mausda Naika
 */
public class PhysicianResParser extends AbstractOrcaApiParser {
    
    private enum ATTRIBUTE {
            Code, WholeName, WholeName_inKana, Physician_Permission_Id, 
            Drug_Permission_Id, Department_Code1, Department_Code2, 
            Department_Code3, Department_Code4, Department_Code5,
            other
    };
    
    public PhysicianResParser(Document doc) {
        super(doc);
    }
    
    public List<PhysicianInfo> getList() {
        return xml2 ? getList2() : getList1();
    }
    
    private List<PhysicianInfo> getList1() {
        
        List<PhysicianInfo> list = new ArrayList<PhysicianInfo>();
        Element arrayElm = doc.getRootElement().getChild(RECORD).getChild(RECORD).getChild(ARRAY);
        
        //String resName = arrayElm.getAttributeValue(NAME);
        for (Element elm : arrayElm.getChildren()) {
            for (Element elm1 : elm.getChildren()) {
                PhysicianInfo info = new PhysicianInfo();
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
                    case WholeName_inKana:
                        info.setWholeNameInKana(value);
                        break;
                    case Physician_Permission_Id:
                        info.setPhysicianPermissionId(value);
                        break;
                    case Drug_Permission_Id:
                        info.setDrugPermissionId(value);
                        break;
                    case Department_Code1:
                        info.setDepartmentCode1(value);
                        break;
                    case Department_Code2:
                        info.setDepartmentCode2(value);
                        break;
                    case Department_Code3:
                        info.setDepartmentCode3(value);
                        break;
                    case Department_Code4:
                        info.setDepartmentCode4(value);
                        break;
                    case Department_Code5:
                        info.setDepartmentCode5(value);
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
    
    private List<PhysicianInfo> getList2() {

        Element record = doc.getRootElement().getChild("physicianres");
        if (record == null) {
            return null;
        }
        Element array = record.getChild("Physician_Information");
        if (array == null) {
            return null;
        }

        List<PhysicianInfo> list = new ArrayList<PhysicianInfo>();
        for (Element elm : array.getChildren()) {
            PhysicianInfo info = new PhysicianInfo();
            String code = elm.getChildText("Code");
            info.setCode(code);
            info.setWholeName(elm.getChildText("WholeName"));
            info.setWholeNameInKana(elm.getChildText("WholeName_inKana"));
            info.setPhysicianPermissionId(elm.getChildText("Physician_Permission_Id"));
            info.setDrugPermissionId(elm.getChildText("Drug_Permission_Id"));
            info.setDepartmentCode1(elm.getChildText("Department_Code1"));
            info.setDepartmentCode2(elm.getChildText("Department_Code2"));
            info.setDepartmentCode3(elm.getChildText("Department_Code3"));
            info.setDepartmentCode4(elm.getChildText("Department_Code4"));
            info.setDepartmentCode5(elm.getChildText("Department_Code5"));
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
