package open.dolphin.session;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.*;

/**
 * PVTServiceBean
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
@Named
@Stateless
public class PVTServiceBean { //implements PVTServiceBeanLocal {

    private static final String QUERY_PATIENT_BY_FID_PID
            = "from PatientModel p where p.facilityId=:fid and p.patientId=:pid";
    private static final String QUERY_INSURANCE_BY_PATIENT_ID
            = "from HealthInsuranceModel h where h.patient.id=:id";
    //private static final String QUERY_KARTE_BY_PATIENT_ID
    //      = "from KarteBean k where k.patient.id=:id";
    private static final String QUERY_KARTE_ID_BY_PATIENT_ID
            = "select k.id from KarteBean k where k.patient.id = :id";
    private static final String QUERY_APPO_BY_KARTE_ID_DATE
            = "from AppointmentModel a where a.karte.id=:id and a.date=:date";
    private static final String FID = "fid";
    private static final String PID = "pid";
    private static final String ID = "id";
    private static final String DATE = "date";
    
    private static final String DEFAULT_FACILITY_OID = "1.3.6.1.4.1.9414.10.1";
    private static final String QUERY_PROPERTY_BY_KEY_AND_VALUE 
            = "from UserPropertyModel u where u.key = :key and u.value = :value";

    @PersistenceContext
    private EntityManager em;
    
    @Inject
    private PvtServiceMediator mediator;
    
    private static final Logger logger = Logger.getLogger(PVTServiceBean.class.getName());

    /**
     * 患者来院情報を登録する。
     * @param spec 来院情報を保持する DTO オブジェクト
     * @return 登録個数
     */
    public int addPvt(PatientVisitModel pvt) {

        // CLAIM 送信の場合 facilityID がデータベースに登録されているものと異なる場合がある
        // 施設IDを認証にパスしたユーザの施設IDに設定する。
        String fid = pvt.getFacilityId();
        // fidがない場合はjmariCodeから設定する
        if (fid == null || fid.isEmpty()) {
            String jmariCode = pvt.getJmariNumber();
            try {
                UserPropertyModel model = (UserPropertyModel) 
                        em.createQuery(QUERY_PROPERTY_BY_KEY_AND_VALUE)
                        .setParameter("key", "jmariCode")
                        .setParameter("value", jmariCode)
                        .getSingleResult();
                fid = model.getFacilityModel().getFacilityId();
            } catch (NoResultException ex) {
                fid = DEFAULT_FACILITY_OID;
            } catch (NonUniqueResultException ex) {
                fid = DEFAULT_FACILITY_OID;
            }
        }
        PatientModel patient = pvt.getPatientModel();
        pvt.setFacilityId(fid);
        patient.setFacilityId(fid);
        
        // 1.4との互換性のためdepartmentにも設定する
        StringBuilder sb = new StringBuilder();
        sb.append(pvt.getDeptName()).append(",");
        sb.append(pvt.getDeptCode()).append(",");
        sb.append(pvt.getDoctorName()).append(",");
        sb.append(pvt.getDoctorId()).append(",");
        sb.append(pvt.getJmariNumber()).append(",");
        pvt.setDepartment(sb.toString());

        // 既存の患者かどうか調べる
        try {
            // 既存の患者かどうか調べる。なければNoResultException
            PatientModel exist = (PatientModel) 
                    em.createQuery(QUERY_PATIENT_BY_FID_PID)
                    .setParameter(FID, fid)
                    .setParameter(PID, patient.getPatientId())
                    .getSingleResult();

            //-----------------------------
            // 健康保険情報を更新する
            //-----------------------------
            @SuppressWarnings("unchecked")
            List<HealthInsuranceModel> old =
                    em.createQuery(QUERY_INSURANCE_BY_PATIENT_ID)
                    .setParameter(ID, exist.getId())
                    .getResultList();
            
            // ORCAからpvtに乗ってやってきた保険情報を取得する。検索などからPVT登録したものには乗っかっていない
            List<HealthInsuranceModel> newOne = patient.getHealthInsurances();

            if (newOne != null && !newOne.isEmpty()) {
                // 現在の保険情報を削除する
                for (HealthInsuranceModel model : old) {
                    em.remove(model);
                }

                // 新しい健康保険情報を登録する
                for (HealthInsuranceModel model : newOne) {
                    model.setPatient(exist);
                    em.persist(model);
                }
                // 健康保険を新しいものに更新する
                exist.setHealthInsurances(newOne);
            } else {
                // pvtに保険情報が乗っかっていない場合は古いのを使う
                exist.setHealthInsurances(old);
            }
            
            // 名前を更新する 2007-04-12
            exist.setFamilyName(patient.getFamilyName());
            exist.setGivenName(patient.getGivenName());
            exist.setFullName(patient.getFullName());
            exist.setKanaFamilyName(patient.getKanaFamilyName());
            exist.setKanaGivenName(patient.getKanaGivenName());
            exist.setKanaName(patient.getKanaName());
            exist.setRomanFamilyName(patient.getRomanFamilyName());
            exist.setRomanGivenName(patient.getRomanGivenName());
            exist.setRomanName(patient.getRomanName());

            // 性別
            exist.setGender(patient.getGender());
            exist.setGenderDesc(patient.getGenderDesc());
            exist.setGenderCodeSys(patient.getGenderCodeSys());

            // Birthday
            exist.setBirthday(patient.getBirthday());

            // 住所、電話を更新する
            exist.setSimpleAddressModel(patient.getSimpleAddressModel());
            exist.setTelephone(patient.getTelephone());
            //exist.setMobilePhone(patient.getMobilePhone());

            // PatientModelを新しい情報に更新する
            em.merge(exist);
            // PatientVisit との関係を設定する
            pvt.setPatientModel(exist);

        } catch (NoResultException e) {
            // 新規患者であれば登録する
            // 患者属性は cascade=PERSIST で自動的に保存される
            em.persist(patient);

            // この患者のカルテを生成する
            KarteBean karte = new KarteBean();
            karte.setPatientModel(patient);
            karte.setCreated(new Date());
            em.persist(karte);
        }

        // ここからPVT登録処理
        
        // CLAIM の仕様により患者情報のみを登録し、来院情報はない場合がある
        // 来院情報を登録する。pvtDate == nullなら患者登録のみ
        if (pvt.getPvtDate() == null) {
            return 0;   // 追加０個、終了
        }

        // カルテの PK を得る
        long karteId = (Long)
                em.createQuery(QUERY_KARTE_ID_BY_PATIENT_ID)
                .setParameter(ID, pvt.getPatientModel().getId())
                .getSingleResult();
        // 予約を検索する
        @SuppressWarnings("unchecked")
        List<AppointmentModel> c =
                em.createQuery(QUERY_APPO_BY_KARTE_ID_DATE)
                .setParameter(ID, karteId)
                .setParameter(DATE, mediator.getToday().getTime())
                .getResultList();
        if (c != null && !c.isEmpty()) {
            AppointmentModel appo = c.get(0);
            pvt.setAppointment(appo.getName());
        }

        // 受付嬢にORCAの受付ボタンを連打されたとき用ｗ 復活！！
        List<PatientVisitModel> pvtList = mediator.getPvtListModel(fid).getPvtList();
        for (int i = 0; i < pvtList.size(); ++i) {
            PatientVisitModel test = pvtList.get(i);
            // pvt時刻が同じでキャンセルでないものは更新(merge)する
            if (test.getPvtDate().equals(pvt.getPvtDate()) 
                    && (test.getState() & (1<< PvtServiceMediator.BIT_CANCEL)) ==0) {
                pvt.setId(test.getId());    // pvtId, state, ownerUUID, byomeiCountは既存のものを使う
                pvt.setState(test.getState());
                pvt.setOwnerUUID(test.getOwnerUUID());
                pvt.setByomeiCount(test.getByomeiCount());
                pvt.setByomeiCountToday(test.getByomeiCountToday());
                // データベースを更新
                em.merge(pvt);
                // 新しいもので置き換える
                pvtList.set(i, pvt);
                // クライアントに通知
                PvtMessageModel msg = new PvtMessageModel(pvt);
                msg.setPatientVisitModel(pvt);
                mediator.notifyMerge(fid, msg);
                return 0;   // 追加０個
            }
        }
        // 同じ時刻のPVTがないならばPVTをデータベースに登録(persist)する
        mediator.setByomeiCount(karteId, pvt);   // 病名数をカウントする
        em.persist(pvt);
        // pvtListに追加
        pvtList.add(pvt);
        // クライアントに通知
        PvtMessageModel msg = new PvtMessageModel(pvt);
        msg.setPatientVisitModel(pvt);
        mediator.notifyAdd(fid, msg);
        
        return 1;   // 追加１個
    }

   
    /**
     * Pvtの情報を更新する
     * 所有者、state、病名数、メモ
     */
    public int updatePvtState(PvtMessageModel msg) {
        
        int ret = 0;

        long pvtId = msg.getPvtPk();
        int state = msg.getState();
        int byomeiCount = msg.getByomeiCount();
        int byomeiCountToday = msg.getByomeiCountToday();
        String memo = msg.getMemo();

        // データベースから該当PVTを取得
        PatientVisitModel exist = em.find(PatientVisitModel.class, pvtId);
        // WatingListから開いていないとexist = nullなので
        if (exist != null) {
            // データベースのpvtStateを更新
            exist.setState(state);
            exist.setByomeiCount(byomeiCount);
            exist.setByomeiCountToday(byomeiCountToday);
            exist.setMemo(memo);
        }

        // pvtListのpvtStateとbyomei countとオーナーを更新
        String fid = msg.getFacilityId();
        List<PatientVisitModel> pvtList = mediator.getPvtListModel(fid).getPvtList();

        for (PatientVisitModel model : pvtList) {
            if (model.getId() == pvtId) {
                model.setState(state);
                model.setByomeiCount(byomeiCount);
                model.setByomeiCountToday(byomeiCountToday);
                model.setMemo(memo);
                model.setOwnerUUID(msg.getOwnerUUID());
                // クライアントに通知
                mediator.notifyState(fid, msg);
                ret = 1;
                break;
            }
        }
        
        return ret;
    }
    
    /**
     * 受付情報を削除する。
     * @param PvtMessageModel msg
     * @return 削除件数
     */
    public int removePvt(PvtMessageModel msg) {

        long id = msg.getPvtPk();

        try {
            // データベースから削除
            PatientVisitModel exist = em.find(PatientVisitModel.class, id);
            // WatingListから開いていないとexist = nullなので。
            if (exist != null) {
                em.remove(exist);
            }
            // pvtListから削除
            String fid = msg.getFacilityId();
            List<PatientVisitModel> pvtList = mediator.getPvtListModel(fid).getPvtList();
            for (PatientVisitModel model : pvtList) {
                if (model.getId() == id) {
                    pvtList.remove(model);
                    break;
                }
            }

            // クライアントに通知
            mediator.notifyDelete(fid, msg);
            return 1;

        } catch (Exception e) {
        }
        return 0;
    }
    
    public PvtServiceMediator getPvtMediator() {
        return mediator;
    }
}
