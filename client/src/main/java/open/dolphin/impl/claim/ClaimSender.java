package open.dolphin.impl.claim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import open.dolphin.client.*;
import open.dolphin.infomodel.*;
import open.dolphin.message.ClaimHelper;
import open.dolphin.message.MessageBuilder;
import open.dolphin.order.MMLTable;
import open.dolphin.project.Project;
import open.dolphin.util.ZenkakuUtils;
import org.apache.log4j.Level;

/**
 * Karte と Diagnosis の CLAIM を送る
 * KarteEditor の sendClaim を独立させた
 * DiagnosisDocument の CLAIM 送信部分もここにまとめた
 * @author pns
 */
public class ClaimSender implements IKarteSender {

    // Context
    private Chart context;

    // CLAIM 送信リスナ
    private ClaimMessageListener claimListener;

    // DG UUID の変わりに保険情報モジュールを送信する
    private PVTHealthInsuranceModel insuranceToApply;

    private boolean DEBUG;
    
//masuda^    ClaimItemの最大数
    private static final int maxClaimItemCount = 20;
//masuda$

    public ClaimSender() {
        DEBUG = (ClientContext.getBootLogger().getLevel()==Level.DEBUG);
    }

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
        claimListener  = context.getCLAIMListener();
    }

    /**
     * DocumentModel の CLAIM 送信を行う。
     */
    @Override
    public void send(DocumentModel sendModel) {

        if (sendModel==null ||
            sendModel.getDocInfoModel().isSendClaim()==false ||
            insuranceToApply==null ||
            claimListener==null) {
            return;
        }
        
        // ORCA API使用時はCLAIM送信しない
        if (Project.getBoolean(Project.USE_ORCA_API)) {
            return;
        }

        // ヘルパークラスを生成しVelocityが使用するためのパラメータを設定する
        ClaimHelper helper = new ClaimHelper();

        //DG ------
        //DocInfoModel docInfo = sendModel.getDocInfo();
        DocInfoModel docInfo = sendModel.getDocInfoModel();
        Collection<ModuleModel> modules = sendModel.getModules();
        //--------DG

        //DG ------------------------------------------
        // 過去日で送信するために firstConfirmDate へ変更
        //String confirmedStr = ModelUtils.getDateTimeAsString(docInfo.getConfirmDate());
        String confirmedStr = ModelUtils.getDateTimeAsString(docInfo.getFirstConfirmDate());
        //--------------------------------------------- DG
        helper.setConfirmDate(confirmedStr);
        debug(confirmedStr);

        String deptName = docInfo.getDepartmentName();
        String deptCode = docInfo.getDepartmentCode();
        String doctorName = docInfo.getAssignedDoctorName();
        if (doctorName == null) {
            doctorName = Project.getUserModel().getCommonName();
        }
        String doctorId = docInfo.getAssignedDoctorId();
        if (doctorId == null) {
            doctorId = Project.getUserModel().getOrcaId()!=null
                    ? Project.getUserModel().getOrcaId()
                    : Project.getUserModel().getUserId();
        }
        String jamriCode = docInfo.getJMARICode();
        if (jamriCode == null) {
            jamriCode = Project.getString(Project.JMARI_CODE);
        }
        if (DEBUG) {
            debug(deptName);
            debug(deptCode);
            debug(doctorName);
            debug(doctorId);
            debug(jamriCode);
        }
        helper.setCreatorDeptDesc(deptName);
        helper.setCreatorDept(deptCode);
        helper.setCreatorName(doctorName);
        helper.setCreatorId(doctorId);
        helper.setCreatorLicense(Project.getUserModel().getLicenseModel().getLicense());
        helper.setJmariCode(jamriCode);
        helper.setFacilityName(Project.getUserModel().getFacilityModel().getFacilityName());
        
        //DG -------------------------------------------
        //helper.setPatientId(sendModel.getKarte().getPatient().getPatientId());
        //helper.setPatientId(sendModel.getKarteBean().getPatientModel().getPatientId());
        helper.setPatientId(context.getPatient().getPatientId());
        //--------------------------------------------- DG
        helper.setGenerationPurpose(docInfo.getPurpose());
        helper.setDocId(docInfo.getDocId());
        helper.setHealthInsuranceGUID(docInfo.getHealthInsuranceGUID());
        helper.setHealthInsuranceClassCode(docInfo.getHealthInsurance());
        helper.setHealthInsuranceDesc(docInfo.getHealthInsuranceDesc());

        //DG -----------------------------------------------
        // 2010-11-10 UUIDの変わりに保険情報モジュールを送信する
        helper.setSelectedInsurance(insuranceToApply);
        //-------------------------------------------------- DG
        if (DEBUG) {
            debug(helper.getHealthInsuranceGUID());
            debug(helper.getHealthInsuranceClassCode());
            debug(helper.getHealthInsuranceDesc());
        }
//masuda^   ヘルパー登録を分離
        registToHelper(helper, modules);
/*
        // 保存する KarteModel の全モジュールをチェックし
        // それが ClaimBundle ならヘルパーへ追加する
        for (ModuleModel module : modules) {
            IInfoModel m = module.getModel();
            if (m instanceof ClaimBundle) {
                //DG-----------------------------------
                ClaimBundle bundle = (ClaimBundle) m;
                ClaimItem[] items = bundle.getClaimItem();
                if (items!=null && items.length>0) {
                    for (ClaimItem cl : items) {
                        cl.setName(ZenkakuUtils.utf8Replace(cl.getName()));
                    }
                }
                //-------------------------------------DG
                helper.addClaimBundle(bundle);
            }
        }
*/
//masuda$
        MessageBuilder mb = new MessageBuilder();
        String claimMessage = mb.build(helper);
        ClaimMessageEvent cvt = new ClaimMessageEvent(this);
        cvt.setClaimInstance(claimMessage);
        //DG ----------------------------------------------
        //cvt.setPatientId(sendModel.getKarte().getPatient().getPatientId());
        //cvt.setPatientName(sendModel.getKarte().getPatient().getFullName());
        //cvt.setPatientSex(sendModel.getKarte().getPatient().getGender());
        //cvt.setTitle(sendModel.getDocInfo().getTitle());
        cvt.setPatientId(context.getPatient().getPatientId());
        cvt.setPatientName(context.getPatient().getFullName());
        cvt.setPatientSex(context.getPatient().getGender());
        cvt.setTitle(sendModel.getDocInfoModel().getTitle());
        //---------------------------------------------- DG
        cvt.setConfirmDate(confirmedStr);

        // debug 出力を行う
        if (ClientContext.getClaimLogger() != null) {
            ClientContext.getClaimLogger().debug(cvt.getClaimInsutance());
        }

        claimListener.claimMessageEvent(cvt);
    }

    private void debug(String msg) {
        if (DEBUG) {
            ClientContext.getBootLogger().debug(msg);
        }
    }
    
//masuda^
    private void registToHelper(ClaimHelper helper, Collection<ModuleModel> modules){
        // 保存する KarteModel の全モジュールをチェックしClaimBundleならヘルパーに登録
        // Orcaで受信できないような大きなClaimBundleを分割する
        // 処方のコメント項目は分離して、別に".980"として送信する
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
                        helper.addClaimBundle(cb1);
                    }
                } else {
                    // 20以下なら今までどおり
                    helper.addClaimBundle(cb);
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
            helper.addClaimBundle(cb);
        }
    }

    private List<ClaimBundle> divideClaimBundle(ClaimBundle cb) {
        // Orcaで同時に受信できるClaimItem数が20に限られているので
        // 20を超えていたらClaimBundleを分割する masuda
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
//masuda$
}
