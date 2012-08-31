
package open.dolphin.impl.orcaapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import open.dolphin.client.Chart;
import open.dolphin.client.IKarteSender;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
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
    private PVTHealthInsuranceModel insuranceToApply;
    
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

    @Override
    public void prepare(DocumentModel data) {
        if (data==null || (!data.getDocInfoModel().isSendClaim())) {
            return;
        }
        insuranceToApply = context.getHealthInsuranceToApply(data.getDocInfoModel().getHealthInsuranceGUID());
    }

    @Override
    public void send(DocumentModel sendModel) {
        
        if (sendModel == null
                || sendModel.getDocInfoModel().isSendClaim() == false
                || insuranceToApply == null) {
            return;
        }
        
        // ORCA API使用しない場合はリターン
        if (!Project.getBoolean(Project.USE_ORCA_API)) {
            return;
        }

        DocInfoModel docInfo = sendModel.getDocInfoModel();
        List<ClaimBundle> cbList = getClaimBundleList(sendModel.getModules());
        MedicalModModel modModel = new MedicalModModel();
        modModel.setContext(context);
        modModel.setDepartmentCode(docInfo.getDepartmentCode());
        modModel.setPhysicianCode(Project.getString(Project.ORCA_STAFF_CODE));
        modModel.setPerformDate(docInfo.getFirstConfirmDate());
        modModel.setInsuranceModel(insuranceToApply);
        modModel.setClaimBundleList(cbList);
        
        OrcaApi.getInstance().send(modModel);
    }

    private List<ClaimBundle> getClaimBundleList(Collection<ModuleModel> modules){
        // 保存する KarteModel の全モジュールをチェックしClaimBundleならヘルパーに登録
        // Orcaで受信できないような大きなClaimBundleを分割する
        // 処方のコメント項目は分離して、別に".980"として送信する
        
        List<ClaimBundle> bundleList = new ArrayList<ClaimBundle>();
        List<ClaimItem> commentItem = new ArrayList<ClaimItem>();

        for (ModuleModel module : modules) {

            // 処方箋コメントを分離
            if ("medOrder".equals(module.getModuleInfoBean().getEntity())) {
                BundleMed bundle = (BundleMed) module.getModel();

                List<ClaimItem> nonCommentItem = new ArrayList<ClaimItem>();
                for (ClaimItem ci : bundle.getClaimItem()){
                    // 文字置換
                    String replaced = ZenkakuUtils.utf8Replace(ci.getName());
                    ci.setName(replaced);
                    
                    // それぞれの処方bundleをしらべる
                    boolean comment = ci.getCode().matches(ClaimConst.REGEXP_PRESCRIPTION_COMMENT);
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

            // 20を超えるClaimItemを持つClaimBundleは分割して登録する
            IInfoModel m = module.getModel();

            if (m instanceof ClaimBundle) {
                ClaimBundle cb = (ClaimBundle) m;
                // 文字置換
                for (ClaimItem ci : cb.getClaimItem()) {
                    String replaced = ZenkakuUtils.utf8Replace(ci.getName());
                    ci.setName(replaced);
                }
                int count = cb.getClaimItem().length;
                if (count > maxClaimItemCount) {
                    for (ClaimBundle cb1 : divideClaimBundle(cb)){
                        bundleList.add(cb1);
                    }
                } else {
                    // 20以下なら今までどおり
                    bundleList.add(cb);
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
}
