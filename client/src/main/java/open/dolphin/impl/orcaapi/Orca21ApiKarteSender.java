package open.dolphin.impl.orcaapi;

import java.util.*;
import open.dolphin.client.Chart;
import open.dolphin.client.IKarteSender;
import open.dolphin.client.KarteSenderResult;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.BeanUtils;
import open.dolphin.util.ZenkakuUtils;

/**
 * Orca21ApiKarteSender
 * 
 * @author masuda, Masuda Naika
 */
public class Orca21ApiKarteSender implements IKarteSender {

    // Context
    private Chart context;

    // DG UUID の変わりに保険情報モジュールを送信する
    //private PVTHealthInsuranceModel insuranceToApply;
    
//masuda^    ClaimItemの最大数
    private static final int maxClaimItemCount = 40;
//masuda$
    
    @Override
    public Chart getContext() {
        return context;
    }

    @Override
    public void setContext(Chart context) {
        this.context = context;
    }

/*
    @Override
    public void prepare(DocumentModel data) {
        if (data==null || (!data.getDocInfoModel().isSendClaim())) {
            return;
        }
        insuranceToApply = context.getHealthInsuranceToApply(data.getDocInfoModel().getHealthInsuranceGUID());
    }
*/
    
    @Override
    public KarteSenderResult send(DocumentModel sendModel) {
        
        if (sendModel == null 
                || !sendModel.getDocInfoModel().isSendClaim() 
                || context == null) {
            return new KarteSenderResult(KarteSenderResult.ORCA_API, KarteSenderResult.SKIPPED, null);
        }
        
        // ORCA API使用しない場合はリターン
        if (!Project.getBoolean(Project.USE_ORCA_API)) {
            return new KarteSenderResult(KarteSenderResult.ORCA_API, KarteSenderResult.SKIPPED, null);
        }
        
        PVTHealthInsuranceModel insuranceToApply
                = context.getHealthInsuranceToApply(sendModel.getDocInfoModel().getHealthInsuranceGUID());
        
        if (insuranceToApply == null) {
            return new KarteSenderResult(KarteSenderResult.ORCA_API, KarteSenderResult.SKIPPED, null);
        }

        DocInfoModel docInfo = sendModel.getDocInfoModel();
        // 入院カルテの場合はadmitFlagを立てる
        AdmissionModel admission = sendModel.getDocInfoModel().getAdmissionModel();
        boolean admissionFlg = (admission != null);
        List<ClaimBundle> cbList = getClaimBundleList(sendModel.getModules(), admissionFlg);
        
        MedicalModModel modModel = new MedicalModModel();
        modModel.setContext(context);
        modModel.setDepartmentCode(docInfo.getDepartmentCode());
        modModel.setPhysicianCode(Project.getString(Project.ORCA_STAFF_CODE));
        modModel.setPerformDate(docInfo.getFirstConfirmDate());
        modModel.setInsuranceModel(insuranceToApply);
        modModel.setClaimBundleList(cbList);
        modModel.setAdmissionFlg(admissionFlg);
        
        KarteSenderResult result = OrcaApiDelegater.getInstance().sendMedicalModModel(modModel);
        return result;
    }

    private List<ClaimBundle> getClaimBundleList(Collection<ModuleModel> modules_src, boolean admission){
        // 保存する KarteModel の全モジュールをチェックしClaimBundleならヘルパーに登録
        // Orcaで受信できないような大きなClaimBundleを分割する
        // 処方のコメント項目は分離して、別に".980"として送信する
        Collection<ModuleModel> modules = 
                (Collection<ModuleModel>) BeanUtils.deepCopy(modules_src);
        
        List<ClaimBundle> bundleList = new ArrayList<ClaimBundle>();
        List<ClaimItem> commentItem = new ArrayList<ClaimItem>();

        for (ModuleModel module : modules) {

            String entity = module.getModuleInfoBean().getEntity();
            
            // 処方箋コメントを分離
            if (IInfoModel.ENTITY_MED_ORDER.equals(entity)) {
                BundleMed bundle = (BundleMed) module.getModel();

                List<ClaimItem> nonCommentItem = new ArrayList<ClaimItem>();
                for (ClaimItem ci : bundle.getClaimItem()){
                    // 文字置換
                    String replaced = ZenkakuUtils.utf8Replace(ci.getName());
                    ci.setName(replaced);
                    
                    // それぞれの処方bundleをしらべる
                    boolean comment = ci.getCode().matches(ClaimConst.REGEXP_PRESCRIPTION_COMMENT);
                    // 先頭がアスタリスクならば.980に分離しない
                    comment &= (ci.getName() != null && !ci.getName().startsWith("*") && !ci.getName().startsWith("＊"));
                    if (comment) {
                        commentItem.add(ci);    // コメントコード
                    } else {
                        nonCommentItem.add(ci); // コメントじゃない
                    }
                }
                // コメントコードを抜き取った残りをbundleに登録しなおす
                if (!commentItem.isEmpty()) {
                    bundle.setClaimItem(nonCommentItem.toArray(new ClaimItem[0]));
                }
            }
            
            // 注射手技料なしの場合はClaim送信前に手技を抜く
            if (IInfoModel.ENTITY_INJECTION_ORDER.equals(entity)) {
                ClaimBundle bundle = (ClaimBundle) module.getModel();
                String clsCode = bundle.getClassCode();
                if (clsCode != null && clsCode.startsWith("3") && clsCode.endsWith("1")) {
                    List<ClaimItem> ciList = new ArrayList<ClaimItem>();
                    for (ClaimItem ci : bundle.getClaimItem()) {
                        // int ClaimConst.SYUGI = 0
                        if (!"0".equals(ci.getClassCode())) {
                            ciList.add(ci);
                        }
                    }
                    bundle.setClaimItem(ciList.toArray(new ClaimItem[0]));
                }
            }
            
            // 40を超えるClaimItemを持つClaimBundleは分割して登録する
            IInfoModel m = module.getModel();

            if (m instanceof ClaimBundle) {
                ClaimBundle bundle = (ClaimBundle) m;
                // 文字置換
                for (ClaimItem ci : bundle.getClaimItem()) {
                    String replaced = ZenkakuUtils.utf8Replace(ci.getName());
                    ci.setName(replaced);
                }

                // 入院の検体検査の場合は包括対象検査区分ごとに分類する
                // そうしないと項目によってはbundleNumberが不正になってしまう。
                // ORCAの「仕様」とのこと…
                List<ClaimBundle> cbList = new ArrayList<ClaimBundle>();
                if (admission && ClaimConst.RECEIPT_CODE_LABO.equals(bundle.getClassCode())) {
                    cbList.addAll(divideBundleByHokatsuKbn(bundle));
                } else {
                    cbList.add(bundle);
                }
                
                // ClaimItem数が40を超えないように分割する
                for (ClaimBundle cb1 : cbList) {
                    int count = cb1.getClaimItem().length;
                    if (count > maxClaimItemCount) {
                        bundleList.addAll(divideClaimBundle(cb1));
                    } else {
                        // 40以下なら今までどおり
                        bundleList.add(cb1);
                    }
                }
            }
        }

        // 抜き出したコメント項目は.980で別に送る。コメントが20超えることはないだろう。
        // レセには印刷されなくなる？
        if (!commentItem.isEmpty()) {
            ClaimBundle cb = new ClaimBundle();
            cb.setClassName(MMLTable.getClaimClassCodeName("980"));
            cb.setClassCode("980");                             // 処方箋備考のclass code
            cb.setClassCodeSystem(ClaimConst.CLASS_CODE_ID);    // "Claim007"
            cb.setClaimItem(commentItem.toArray(new ClaimItem[0]));
            bundleList.add(cb);
        }
        
        return bundleList;
    }

    private List<ClaimBundle> divideClaimBundle(ClaimBundle cb) {

        List<ClaimBundle> ret = new ArrayList<ClaimBundle>();

        ClaimItem[] array = cb.getClaimItem();
        int size = array.length;
        int index = 0;
        
        while (index < size) {
            ClaimBundle bundle = copyClaimBundle(cb);
            int indexTo = Math.min(index + maxClaimItemCount, size);
            ClaimItem[] ciArray = Arrays.copyOfRange(array, index, indexTo);
            bundle.setClaimItem(ciArray);
            ret.add(bundle);
            index = index + maxClaimItemCount;
        }

        return ret;
    }
    
    private ClaimBundle copyClaimBundle(ClaimBundle source) {
        ClaimBundle ret = new ClaimBundle();
        ret.setAdmin(source.getAdmin());
        ret.setAdminCode(source.getAdminCode());
        ret.setAdminCodeSystem(source.getAdminCodeSystem());
        ret.setAdminMemo(source.getAdminMemo());
        ret.setBundleNumber(source.getBundleNumber());
        ret.setClassCode(source.getClassCode());
        ret.setClassCodeSystem(source.getClassCodeSystem());
        ret.setClassName(source.getClassName());
        ret.setInsurance(source.getInsurance());
        ret.setMemo(source.getMemo());
        return ret;
    }
    
    // 包括対象検査区分分ごとに分類する
    private List<ClaimBundle> divideBundleByHokatsuKbn(ClaimBundle cb) {
        
        // srycdを列挙する
        List<String> srycds = new ArrayList<String>();
        for (ClaimItem ci : cb.getClaimItem()) {
            srycds.add(ci.getCode());
        }
        
        // 包括対象検査区分とのマップを取得する
        Map<String, Integer> kbnMap = SqlMiscDao.getInstance().getHokatsuKbnMap(srycds);
        
        // 各項目をグループ分けする
        Map<Integer, List<ClaimItem>> ciMap = new HashMap<Integer, List<ClaimItem>>();
        for (ClaimItem ci : cb.getClaimItem()) {
            Integer kbn = kbnMap.get(ci.getCode());
            List<ClaimItem> list = ciMap.get(kbn);
            if (list == null) {
                list = new ArrayList<ClaimItem>();
            }
            list.add(ci);
            ciMap.put(kbn, list);
        }
        
        // ClaimBundleに戻す
        List<ClaimBundle> ret = new ArrayList<ClaimBundle>();
        for (Iterator itr = ciMap.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            int houksnkbn = (Integer) entry.getKey();
            List<ClaimItem> ciList = (List<ClaimItem>) entry.getValue();
            // ＯＳＣに問い合わせたところ、下記の返答 2012/09/26
            // 「包括対象検査の対象でない検査は、検査毎に剤を分けていただくしか方法はありません」
            if (houksnkbn != 0) {
                ClaimBundle bundle = copyClaimBundle(cb);
                bundle.setClaimItem(ciList.toArray(new ClaimItem[0]));
                ret.add(bundle);
            } else {
                for (ClaimItem ci : ciList) {
                    ClaimBundle bundle = copyClaimBundle(cb);
                    bundle.setClaimItem(new ClaimItem[]{ci});
                    ret.add(bundle);
                }
            }
        }
        
        return ret;
    }
}
