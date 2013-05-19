package open.dolphin.impl.claim;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import open.dolphin.client.*;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.infomodel.*;
import open.dolphin.message.ClaimHelper;
import open.dolphin.message.MessageBuilder;
import open.dolphin.project.Project;
import open.dolphin.util.BeanUtils;
import open.dolphin.util.ZenkakuUtils;
import org.apache.log4j.Level;

/**
 * Karte と Diagnosis の CLAIM を送る
 * KarteEditor の sendClaim を独立させた
 * DiagnosisDocument の CLAIM 送信部分もここにまとめた
 * @author pns
 */
public class ClaimSender implements IKarteSender {
    
    private static final String CLAIM = "CLAIM";

    // Context
    private Chart context;
    private DocumentModel sendModel;
    private PropertyChangeSupport boundSupport;

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
    public void setModel(DocumentModel sendModel) {
        this.sendModel = sendModel;
    }

    @Override
    public void addListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(KarteSenderResult.PROP_KARTE_SENDER_RESULT, listener);
    }
    
    @Override
    public void removeListeners() {
        if (boundSupport != null) {
            for (PropertyChangeListener listener : boundSupport.getPropertyChangeListeners()) {
                boundSupport.removePropertyChangeListener(KarteSenderResult.PROP_KARTE_SENDER_RESULT, listener);
            }
        }
    }

    @Override
    public void fireResult(KarteSenderResult result) {
        if (boundSupport != null) {
            boundSupport.firePropertyChange(KarteSenderResult.PROP_KARTE_SENDER_RESULT, null, result);
        }
    }

    /**
     * DocumentModel の CLAIM 送信を行う。
     */
    @Override
    public void send() {

        if (sendModel == null 
                || !sendModel.getDocInfoModel().isSendClaim()
                || context == null) {
            fireResult(new KarteSenderResult(CLAIM, KarteSenderResult.SKIPPED, null, this));
            return;
        }
        
        // ORCA API使用時はCLAIM送信しない
        if (Project.getBoolean(Project.USE_ORCA_API)) {
            fireResult(new KarteSenderResult(CLAIM, KarteSenderResult.SKIPPED, null, this));
            return;
        }
        
        // CLAIM 送信リスナ
        ClaimMessageListener claimListener = context.getCLAIMListener();

        // DG UUID の変わりに保険情報モジュールを送信する
        PVTHealthInsuranceModel insuranceToApply 
                = context.getHealthInsuranceToApply(sendModel.getDocInfoModel().getHealthInsuranceGUID());
        
        if (claimListener == null || insuranceToApply == null) {
            fireResult(new KarteSenderResult(CLAIM, KarteSenderResult.SKIPPED, null, this));
            return;
        }

        // ヘルパークラスを生成しVelocityが使用するためのパラメータを設定する
        ClaimHelper helper = new ClaimHelper();
        
//masuda^   入院カルテの場合はadmitFlagを立てる
        AdmissionModel admission = sendModel.getDocInfoModel().getAdmissionModel();
        if (admission != null) {
            helper.setAdmitFlag(true);
        }
        boolean b = Project.getBoolean(Project.CLAIM_01);
        helper.setUseDefalutDept(b);
//masuda$
        
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
        MessageBuilder mb = MessageBuilder.getInstance();
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
    private void registToHelper(ClaimHelper helper, Collection<ModuleModel> modules_src){
        // 保存する KarteModel の全モジュールをチェックしClaimBundleならヘルパーに登録
        // Orcaで受信できないような大きなClaimBundleを分割する
        // 処方のコメント項目は分離して、別に".980"として送信する

        // 気持ちが悪いので複製をつかう
        //byte[] bytes = BeanUtils.getXMLBytes(modules_src);
        //Collection<ModuleModel> modules 
        //        = (Collection<ModuleModel>) BeanUtils.xmlDecode(bytes);
        Collection<ModuleModel> modules = 
                (Collection<ModuleModel>) BeanUtils.deepCopy(modules_src);
        
        boolean admission = helper.getAdmitFlag();
        
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
            
            // 20を超えるClaimItemを持つClaimBundleは分割して登録する
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
                
                // ClaimItem数が20を超えないように分割する
                for (ClaimBundle cb1 : cbList) {
                    int count = cb1.getClaimItem().length;
                    if (count > maxClaimItemCount) {
                        for (ClaimBundle cb2 : divideClaimBundle(cb1)) {
                            helper.addClaimBundle(cb2);
                        }
                    } else {
                        // 20以下なら今までどおり
                        helper.addClaimBundle(cb1);
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
            helper.addClaimBundle(cb);
        }
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
        for (Map.Entry entry : ciMap.entrySet()) {
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
