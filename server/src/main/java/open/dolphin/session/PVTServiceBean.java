package open.dolphin.session;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.*;
import open.dolphin.mbean.ServletContextHolder;

/**
 * PVTServiceBean
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
@Stateless
public class PVTServiceBean implements IServiceBean {
    
    private static final String QUERY_PATIENT_BY_FID_PID
            = "from PatientModel p where p.facilityId=:fid and p.patientId=:pid";
    private static final String QUERY_KARTE_ID_BY_PATIENT_ID
            = "select k.id from KarteBean k where k.patient.id = :id";
    private static final String QUERY_APPO_BY_KARTE_ID_DATE
            = "from AppointmentModel a where a.karte.id=:id and a.date=:date";

    @Inject
    private ChartEventServiceBean eventServiceBean;
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @PersistenceContext
    private EntityManager em;
    
    //private static final Logger logger = Logger.getLogger(PVTServiceBean.class.getName());

    /**
     * 患者来院情報を登録する。
     * @param spec 来院情報を保持する DTO オブジェクト
     * @return 登録個数
     */
    public int addPvt(PatientVisitModel pvt) {

        // CLAIM 送信の場合 facilityID がデータベースに登録されているものと異なる場合がある
        // 施設IDを認証にパスしたユーザの施設IDに設定する。
        String fid = pvt.getFacilityId();
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
            List<HealthInsuranceModel> old = getHealthInsurances(exist.getId());
            
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
                .setParameter(DATE, contextHolder.getToday().getTime())
                .getResultList();
        if (c != null && !c.isEmpty()) {
            AppointmentModel appo = c.get(0);
            pvt.setAppointment(appo.getName());
        }

        // 受付嬢にORCAの受付ボタンを連打されたとき用ｗ 復活！！
        List<PatientVisitModel> pvtList = eventServiceBean.getPvtList(fid);
        for (int i = 0; i < pvtList.size(); ++i) {
            PatientVisitModel test = pvtList.get(i);
            // pvt時刻が同じでキャンセルでないものは更新(merge)する
            if (test.getPvtDate().equals(pvt.getPvtDate()) 
                    && (test.getState() & (1<< PatientVisitModel.BIT_CANCEL)) ==0) {
                pvt.setId(test.getId());    // pvtId, state, ownerUUID, byomeiCountは既存のものを使う
                pvt.setState(test.getState());
                pvt.getPatientModel().setOwnerUUID(test.getPatientModel().getOwnerUUID());
                pvt.setByomeiCount(test.getByomeiCount());
                pvt.setByomeiCountToday(test.getByomeiCountToday());
                // データベースを更新
                em.merge(pvt);
                // 新しいもので置き換える
                pvtList.set(i, pvt);
                // クライアントに通知
                String uuid = contextHolder.getServerUUID();
                ChartEventModel msg = new ChartEventModel(uuid);
                msg.setParamFromPvt(pvt);
                msg.setPatientVisitModel(pvt);
                msg.setEventType(ChartEventModel.EVENT.PVT_MERGE);
                eventServiceBean.notifyEvent(msg);
                return 0;   // 追加０個
            }
        }
        // 同じ時刻のPVTがないならばPVTをデータベースに登録(persist)する
        eventServiceBean.setByomeiCount(karteId, pvt);   // 病名数をカウントする
        em.persist(pvt);
        // pvtListに追加
        pvtList.add(pvt);
        // クライアントに通知
        String uuid = contextHolder.getServerUUID();
        ChartEventModel msg = new ChartEventModel(uuid);
        msg.setParamFromPvt(pvt);
        msg.setPatientVisitModel(pvt);
        msg.setEventType(ChartEventModel.EVENT.PVT_ADD);
        eventServiceBean.notifyEvent(msg);
        
        return 1;   // 追加１個
    }

    /**
     * 受付情報を削除する。
     * @param pvtPk, fid
     * @return 削除件数
     */
    public int removePvt(long id, String fid) {
        
        try {
            // データベースから削除
            PatientVisitModel exist = em.find(PatientVisitModel.class, id);
            // WatingListから開いていないとexist = nullなので。
            if (exist != null) {
                em.remove(exist);
            }

            // pvtListから削除
            List<PatientVisitModel> pvtList = eventServiceBean.getPvtList(fid);
            PatientVisitModel toRemove = null;
            for (PatientVisitModel model : pvtList) {
                if (model.getId() == id) {
                    toRemove = model;
                    break;
                }
            }
            if (toRemove != null) {
                pvtList.remove(toRemove);
                return 1;
            }
        } catch (Exception e) {
        }
        return 0;
    }
    
    
    private void setHealthInsurances(Collection<PatientModel> list) {
        if (list != null && !list.isEmpty()) {
            for (PatientModel pm : list) {
                setHealthInsurances(pm);
            }
        }
    }
    
    private void setHealthInsurances(PatientModel pm) {
        if (pm != null) {
            List<HealthInsuranceModel> ins = getHealthInsurances(pm.getId());
            pm.setHealthInsurances(ins);
        }
    }

    private List<HealthInsuranceModel> getHealthInsurances(long pk) {
        
        List<HealthInsuranceModel> ins =
                em.createQuery(QUERY_INSURANCE_BY_PATIENT_PK)
                .setParameter(PK, pk)
                .getResultList();
        return ins;
    }
}
