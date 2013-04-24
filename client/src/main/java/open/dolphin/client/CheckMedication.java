package open.dolphin.client;

import java.awt.Toolkit;
import java.util.*;
import javax.swing.JOptionPane;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.infomodel.*;
import open.dolphin.util.ZenkakuUtils;

/**
 * KarteEditorで保存したとき呼ばれる
 * KartePane内の薬剤をリストアップしてOrcaの薬剤併用データベースで検索
 * 内服薬のみ。注射はなし。
 * 臨時処方の日数や長期処方制限、２錠／分３などの確認も行う
 * 
 * @author masuda, Masuda Naika
 */

public class CheckMedication {

    private HashMap<String, String> drugCodeNameMap;
    private List<ModuleModel> moduleList;
    private List<BundleMed> medList;         // 内服薬
    private List<BundleDolphin> bundleList;  // 注射も含む
    
    
    private static final int DAY_LIMIT = 14;

    
    public boolean checkStart(Chart context, List<ModuleModel> stamps) {
        
        moduleList = stamps;
        makeDrugList();

        String msg = checkMedication();
        if (msg != null && msg.length() != 0) {
            Toolkit.getDefaultToolkit().beep();
            String[] options = {"取消", "無視"};
            int val = JOptionPane.showOptionDialog(context.getFrame(), msg, "薬剤確認",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (val != 1) {
                // 取り消し
                return true;
            }
        }

        msg = checkInteraction();
        if (msg != null && msg.length() != 0) {
            Toolkit.getDefaultToolkit().beep();
            String[] options = {"取消", "無視"};
            int val = JOptionPane.showOptionDialog(context.getFrame(), msg, "薬剤併用警告",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            if (val != 1) {
                // 取り消し
                return true;
            }
        }
        // 採用薬チェックと登録
        checkUsingDrugs();
        return false;
    }


    private void makeDrugList() {
        
        drugCodeNameMap = new HashMap<String, String>();
        bundleList = new ArrayList<BundleDolphin>();
        medList = new ArrayList<BundleMed>();
        
        for (ModuleModel stamp : moduleList) {
            String entity = stamp.getModuleInfoBean().getEntity();
            if (IInfoModel.ENTITY_MED_ORDER.equals(entity) || IInfoModel.ENTITY_INJECTION_ORDER.equals(entity)) {
                BundleDolphin bundle = (BundleDolphin) stamp.getModel();
                bundleList.add(bundle);
                ClaimItem[] ci = bundle.getClaimItem();
                for (ClaimItem c : ci) {
                    if (ClaimConst.YAKUZAI == Integer.valueOf(c.getClassCode())) {
                        drugCodeNameMap.put(c.getCode(), c.getName());
                    }
                }
            }
            if (IInfoModel.ENTITY_MED_ORDER.equals(entity)) {
                BundleMed bundle = (BundleMed) stamp.getModel();
                medList.add(bundle);
            }
        }
    }

    private void checkUsingDrugs() {

        // 採用薬と中止項目を最新にする
        UsingDrugs.getInstance().loadUsingDrugs();
        DisconItems.getInstance().loadDisconItems();

        List<UsingDrugModel> newDrugs = new ArrayList<UsingDrugModel>();
        for (BundleDolphin bundle : bundleList) {
            String admin = bundle.getAdmin();
            if (admin != null && admin.startsWith("１日")) {
                int pos = admin.indexOf("回");
                admin = admin.substring("１日".length(), pos);
                admin = ZenkakuUtils.toHalfNumber(admin);
            } else {
                admin = null;
            }
            for (ClaimItem ci : bundle.getClaimItem()) {
                if (ClaimConst.YAKUZAI != Integer.valueOf(ci.getClassCode())) {
                    continue;
                }
                String srycd = ci.getCode();
                boolean inUse = UsingDrugs.getInstance().isInUse(srycd);
                if (!inUse && !DisconItems.getInstance().isDiscon(ci.getName())) {
                    // 登録されていない場合は追加する。中止項目は登録しない
                    UsingDrugModel model = new UsingDrugModel();
                    model.setValid(true);
                    model.setSrycd(Integer.parseInt(srycd));
                    model.setAdmin(admin);
                    model.setName(ci.getName());
                    model.setUsualDose(ci.getNumber());
                    newDrugs.add(model);
                }
            }
        }

        UsingDrugs.getInstance().addUsingDrugs(newDrugs);

    }

    private String checkMedication() {

        if (medList.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        for (BundleMed bundle : medList){
            String classCode = bundle.getClassCode();
            int days = Integer.valueOf(bundle.getBundleNumber());

            if (days > DAY_LIMIT) {
                if (classCode.startsWith("29")) {
                    sb.append(bundle.getItemNames());
                    sb.append(" は臨時処方なので１４日以内にしてください\n");
                }
                for (ClaimItem ci : bundle.getClaimItem()) {
                    UsingDrugModel udm = UsingDrugs.getInstance().getUsingDrugModel(ci.getCode());
                    if (udm != null && udm.getHasLimit()) {     // のつはる診療所　白坂先生のご指摘！
                        sb.append(ci.getName());
                        sb.append(" は長期処方制限があります。\n");
                    }
                }
            }

            String admin = bundle.getAdmin();
            int bunkatsu = 0;
            if (!classCode.startsWith("23") && admin.startsWith("１日")) {
                int posKai = admin.indexOf("回");
                try {
                    String numHankaku = ZenkakuUtils.toHalfNumber(admin.substring("１日".length(), posKai));
                    bunkatsu = Integer.valueOf(numHankaku);
                } catch (Exception e) {
                }
            }
            // 分割投与でなくて分１以外なら
            if (bunkatsu != 0 && bunkatsu != 1) {
                ClaimItem[] ci = bundle.getClaimItem();
                for (ClaimItem item : ci) {
                    String zaikei = item.getUnit();
                    if ("錠".equals(zaikei) || "カプセル".equals(zaikei)) {
                        float num = Float.valueOf(item.getNumber());
                        if (num % bunkatsu != 0) {
                            sb.append(item.getName());
                            sb.append("  ");
                            sb.append(num);
                            sb.append(zaikei);
                            sb.append("／分");
                            sb.append(bunkatsu);
                            sb.append(" は用量確認してください\n");
                        }
                    }
                }
            }
        }
        
        // 院内処方と院外処方の混在チェック
        boolean hasInMed = false;
        boolean hasExMed = false;
        for (BundleMed bundle : medList) {
            String classCode = bundle.getClassCode();
            String memo = bundle.getMemo();
            if ((classCode != null && classCode.endsWith("1")) 
                    || (memo != null && memo.contains("院内"))) {
                hasInMed = true;
                break;
            }
        }
        for (BundleMed bundle : medList) {
            String classCode = bundle.getClassCode();
            String memo = bundle.getMemo();
            if ((classCode != null && classCode.endsWith("2")) 
                    || (memo != null && memo.contains("院外"))) {
                hasExMed = true;
                break;
            }
        }
        if (hasInMed && hasExMed) {
            sb.append("院内処方と院外処方が混在しています。");
        }
        
        // 入院処方チェック
        // TO-DO
        
        return formatMsg(sb.toString());
    }

    private String checkInteraction() {

        // おのおのをsql投げるのではなくてまとめてsql投げるようにした。
        // パフォーマンス良くなるはず。

        int len = drugCodeNameMap.size();
        // 薬なかったらリターン
        if (len == 0){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Collection<String> codes = drugCodeNameMap.keySet();

        final SqlMiscDao dao = SqlMiscDao.getInstance();
        List<DrugInteractionModel> list = dao.checkInteraction(codes, codes);
        if (!dao.isNoError()) {
            return null;
        }
        if (list != null && !list.isEmpty()){
            for (DrugInteractionModel model : list){
                sb.append("＜併用禁忌＞ ");
                sb.append(drugCodeNameMap.get(model.getSrycd1()));
                sb.append(" と ");
                sb.append(drugCodeNameMap.get(model.getSrycd2()));
                sb.append("\n");
                sb.append(model.getSskijo());
                sb.append(" ");
                sb.append(model.getSyojyoucd());
                sb.append("\n");
            }
        }
        return formatMsg(sb.toString());
    }

    private String formatMsg(String str) {
        if (str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width:300px'><b>");
        sb.append(str.replace("\n", "<br>"));
        sb.append("</b></body></html>");
        
        return sb.toString();
    }

    // 処方切れの可能性のある患者を調べる
    public List<PatientModel> getShohougirePatient() throws Exception{

        final int searchPeriod = 40;
        final int yoyuu = 3;

        GregorianCalendar gc = new GregorianCalendar();
        Date toDate = gc.getTime();
        gc.add(GregorianCalendar.DATE, -searchPeriod);
        Date fromDate = gc.getTime();
        
        MasudaDelegater del = MasudaDelegater.getInstance();
        List<PatientModel> list = del.getOutOfMedPatient(fromDate, toDate, yoyuu);
        return list;
    }
}
