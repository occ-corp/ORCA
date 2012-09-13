package open.dolphin.client;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleModel;

/**
 * 入院患者の場合の保存時チェック
 *
 * @author masuda, Masuda Naika
 */
public class CheckAdmission {

    private static final Map<Integer, String> laboNgMap;

    static {
        laboNgMap = new HashMap<Integer, String>();
        laboNgMap.put(160177770, "外来迅速検体検査加算");
        laboNgMap.put(160095710, "静脈採血(B-V)");
        laboNgMap.put(160095810, "末梢採血(B-C)");
        laboNgMap.put(160101210, "動脈採血(B-A)");
    }
    
    private List<ModuleModel> moduleList;
    private List<ModuleModel> medList;
    private List<ModuleModel> injList;
    private List<ModuleModel> laboList;

    public boolean checkStart(Chart context, List<ModuleModel> stamps) {

        moduleList = stamps;

        prepareModuleList();

        String msg = checkModules();
        if (msg != null && msg.length() != 0) {
            Toolkit.getDefaultToolkit().beep();
            String[] options = {"取消", "無視"};
            int val = JOptionPane.showOptionDialog(context.getFrame(), msg, "入院スタンプ確認",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (val != 1) {
                // 取り消し
                return true;
            }
        }

        return false;
    }

    // ModuleModelを処方・注射・検査に分類する
    private void prepareModuleList() {

        medList = new ArrayList<ModuleModel>();
        injList = new ArrayList<ModuleModel>();
        laboList = new ArrayList<ModuleModel>();

        for (ModuleModel mm : moduleList) {

            String entity = mm.getModuleInfoBean().getEntity();
            if (IInfoModel.ENTITY_MED_ORDER.equals(entity)) {
                medList.add(mm);
            } else if (IInfoModel.ENTITY_INJECTION_ORDER.equals(entity)) {
                injList.add(mm);
            } else if (IInfoModel.ENTITY_LABO_TEST.equals(entity)) {
                laboList.add(mm);
            }
        }
    }

    private String checkModules() {
        StringBuilder sb = new StringBuilder();
        sb.append(checkMedication());
        sb.append(checkInjection());
        sb.append(checkLaboTest());
        return sb.toString();
    }

    private String checkMedication() {

        StringBuilder sb = new StringBuilder();
        
        // 臨時はダメ
        for (ModuleModel mm : medList) {
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            String classCode = cb.getClassCode();
            if (classCode != null && classCode.startsWith("29")) {
                sb.append("入院中は臨時処方は使えません。\n");
                break;
            }
        }
        // ７日超えるとダメ
        for (ModuleModel mm : medList) {
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            // 屯用以外
            int days;
            try {
                days = Integer.valueOf(cb.getBundleNumber());
            } catch (Exception ex) {
                continue;
            }
            if (!cb.getClassCode().startsWith("22") && days > 7) {
                sb.append("７日を超える処方があります。\n");
                break;
            }
        }

        return sb.toString();
    }

    private String checkInjection() {

        StringBuilder sb = new StringBuilder();

        for (ModuleModel mm : injList) {
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            // 手技料なしはダメ
            if (cb.getClassCode().endsWith("1")) {
                sb.append("入院中は注射手技料なしを設定しないでください。\n");
                break;
            }
        }
        return sb.toString();
    }

    private String checkLaboTest() {

        StringBuilder sb = new StringBuilder();

        for (ModuleModel mm : laboList) {
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            ClaimItem[] claimItems = cb.getClaimItem();
            for (ClaimItem ci : claimItems) {
                int srycd = Integer.valueOf(ci.getCode());
                String laboName = laboNgMap.get(srycd);
                if (laboName != null) {
                    sb.append("入院中は");
                    sb.append(laboName);
                    sb.append("を算定できません。\n");
                }
            }
        }
        return sb.toString();
    }
}
