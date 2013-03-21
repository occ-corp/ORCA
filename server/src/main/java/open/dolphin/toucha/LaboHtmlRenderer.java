package open.dolphin.toucha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import open.dolphin.infomodel.NLaboItem;
import open.dolphin.infomodel.NLaboModule;
import open.dolphin.infomodel.SampleDateComparator;

/**
 * LaboHtmlRenderer
 *
 * @author masuda, Masuda Naika
 */
public class LaboHtmlRenderer {

    private static final String[] TR_BG = {"<tr>", "<tr bgcolor=\"aliceblue\">"};
    private static LaboHtmlRenderer instance;

    static {
        instance = new LaboHtmlRenderer();
    }

    private LaboHtmlRenderer() {
    }

    public static LaboHtmlRenderer getInstance() {
        return instance;
    }

    public String render(List<NLaboModule> modules) {
        
        if (modules.isEmpty()) {
            return "No labo data.";
        }
        // 検体採取日の降順なので昇順にソートする
        Collections.sort(modules, new SampleDateComparator());
        
        List<String> header = getHeader(modules);
        List<LabTestRowObject> rowList = getRowList(modules);
        
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        // ヘッダ
        sb.append("<tr bgcolor=\"lightgrey\">");
        for (String value : header) {
            sb.append("<th>").append(value).append("</th>");
        }
        sb.append("</tr>");
        
        for (int row = 0; row < rowList.size(); ++row) {
            LabTestRowObject rowObj = rowList.get(row);

            // 項目名
            String specimenName = rowObj.getSpecimenName();
            if (specimenName != null) {
                sb.append("<tr bgcolor=\"gold\">");
                for (int i = 0; i < modules.size() + 1; ++i) {
                    // 項目グループ
                    sb.append("<td>").append(specimenName).append("</td>");
                }
            } else {
                sb.append(TR_BG[row & 1]);
                sb.append("<td>").append(rowObj.getItemName()).append("</td>");
                List<LabTestValueObject> values = rowObj.getValues();
                if (values != null) {
                    for (LabTestValueObject value : values) {
                        // 項目
                        sb.append("<td>");
                        if (value != null) {
                            String out = value.getOut();
                            if ("H".equals(out)) {
                                sb.append("<font color=\"red\">");
                                sb.append(value.getValue()).append("</font>");
                            } else if ("L".equals(out)) {
                                sb.append("<font color=\"blue\">");
                                sb.append(value.getValue()).append("</font>");
                            } else {
                                sb.append(value.getValue());
                            }
                           }
                        sb.append("</td>");
                    }
                }
            }
            sb.append("</tr>");
        }

        sb.append("</table>");
        return sb.toString();
    }
    
    private List<String> getHeader(List<NLaboModule> modules) {
        List<String> header = new ArrayList<String>();
        header.add("項目");
        for (NLaboModule module : modules) {
            header.add(module.getSampleDate().substring(2));
        }
        return header;
    }

    private List<LabTestRowObject> getRowList(List<NLaboModule> modules) {

        List<LabTestRowObject> bloodExams = new ArrayList<LabTestRowObject>();
        List<LabTestRowObject> urineExams = new ArrayList<LabTestRowObject>();
        List<LabTestRowObject> otherExams = new ArrayList<LabTestRowObject>();

        int moduleIndex = 0;

        for (NLaboModule module : modules) {

            for (NLaboItem item : module.getItems()) {

                // 検体名を取得する
                String specimenName = item.getSpecimenName();
                // 検体で分類してリストを選択する
                List<LabTestRowObject> rowObjectList = null;
                if (specimenName != null) {     // null check 橋本先生のご指摘
                    if (specimenName.contains("血")) {
                        rowObjectList = bloodExams;
                    } else if (specimenName.contains("尿") || specimenName.contains("便")) {
                        rowObjectList = urineExams;
                    } else {
                        rowObjectList = otherExams;
                    }
                } else {
                    rowObjectList = otherExams;
                }

                boolean found = false;

                for (LabTestRowObject rowObject : rowObjectList) {
                    if (item.getItemCode().equals(rowObject.getItemCode())) {
                        found = true;
                        LabTestValueObject value = new LabTestValueObject();
                        value.setSampleDate(module.getSampleDate());
                        value.setValue(item.getValue());
                        value.setOut(item.getAbnormalFlg());
                        value.setComment1(item.getComment1());
                        value.setComment2(item.getComment2());
                        rowObject.addLabTestValueObjectAt(moduleIndex, value);
                        rowObject.setNormalValue(item.getNormalValue());    // 基準値記録漏れ対策
                        break;
                    }
                }

                if (!found) {
                    LabTestRowObject row = new LabTestRowObject();
                    row.setLabCode(item.getLaboCode());
                    row.setGroupCode(item.getGroupCode());
                    row.setParentCode(item.getParentCode());
                    row.setItemCode(item.getItemCode());
                    row.setItemName(item.getItemName());
                    row.setUnit(item.getUnit());
                    row.setNormalValue(item.getNormalValue());
                    //
                    LabTestValueObject value = new LabTestValueObject();
                    value.setSampleDate(module.getSampleDate());
                    value.setValue(item.getValue());
                    value.setOut(item.getAbnormalFlg());
                    value.setComment1(item.getComment1());
                    value.setComment2(item.getComment2());
                    row.addLabTestValueObjectAt(moduleIndex, value);
                    //
                    rowObjectList.add(row);
                }
            }

            moduleIndex++;
        }

        List<LabTestRowObject> ret = new ArrayList<LabTestRowObject>();

        if (!bloodExams.isEmpty()) {
            Collections.sort(bloodExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("血液検査");
            bloodExams.add(0, specimen);
            ret.addAll(bloodExams);
        }
        if (!urineExams.isEmpty()) {
            Collections.sort(urineExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("尿・便");
            urineExams.add(0, specimen);
            ret.addAll(urineExams);
        }
        if (!otherExams.isEmpty()) {
            Collections.sort(otherExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("その他");
            otherExams.add(0, specimen);
            ret.addAll(otherExams);
        }
        
        return ret;
    }
}
