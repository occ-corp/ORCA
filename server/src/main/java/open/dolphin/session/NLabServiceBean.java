package open.dolphin.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.*;

/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
@Stateless
public class NLabServiceBean {
    
    private static final String QUERY_MODULE_BY_MODULE_KEY 
            = "from NLaboModule m where m.moduleKey=:moduleKey";
    private static final String QUERY_MODULE_BY_PID_SAMPLEDATE_LABCODE 
            = "from NLaboModule m where m.patientId=:fidPid and m.sampleDate=:sampleDate and m.laboCenterCode=:laboCode";
    private static final String QUERY_MODULE_BY_FIDPID 
            = "from NLaboModule l where l.patientId=:fidPid order by l.sampleDate desc";
    private static final String QUERY_ITEM_BY_MID 
            = "from NLaboItem l where l.laboModule.id=:mid order by groupCode,parentCode,itemCode";
    private static final String QUERY_ITEM_BY_MID_ORDERBY_SORTKEY 
            = "from NLaboItem l where l.laboModule.id=:mid order by l.sortKey";
    private static final String QUERY_ITEM_BY_FIDPID_ITEMCODE 
            = "from NLaboItem l where l.patientId=:fidPid and l.itemCode=:itemCode order by l.sampleDate desc";

    private static final String QUERY_INSURANCE_BY_PATIENT_PK 
            = "from HealthInsuranceModel h where h.patient.id=:pk";
    
    private static final String PK = "pk";
    
    private static final String FIDPID = "fidPid";
    private static final String SAMPLEDATE = "sampleDate";
    private static final String LABOCODE = "laboCode";
    private static final String MODULEKEY = "moduleKey";
    private static final String MID = "mid";
    private static final String ITEM_CODE = "itemCode";
    private static final String WOLF = "WOLF";

    @PersistenceContext
    private EntityManager em;
    
    
    public List<PatientLiteModel> getConstrainedPatients(String fid, List<String>idList) {

        List<PatientLiteModel> ret = new ArrayList<PatientLiteModel>(idList.size());

        for (String pid : idList) {

            try {
                PatientModel patient = (PatientModel) em
                    .createQuery("from PatientModel p where p.facilityId=:fid and p.patientId=:pid")
                    .setParameter("fid", fid)
                    .setParameter("pid", pid)
                    .getSingleResult();
                
                ret.add(patient.patientAsLiteModel());
                
            } catch (NoResultException e) {
                PatientLiteModel dummy = new PatientLiteModel();
                dummy.setFullName("未登録");
                dummy.setKanaName("未登録");
                dummy.setGender("U");
                ret.add(dummy);
            }
        }

        return ret;
    }

    public PatientModel create(String fid, NLaboModule module) {

        String pid = module.getPatientId();

        // 施設IDと LaboModule の患者IDで 患者を取得する
        PatientModel patient = (PatientModel) em
                .createQuery("from PatientModel p where p.facilityId=:fid and p.patientId=:pid")
                .setParameter("fid", fid)
                .setParameter("pid", pid)
                .getSingleResult();


        //--------------------------------------------------------
        // 患者の健康保険を取得する
        setHealthInsurances(patient);
        //--------------------------------------------------------

        String fidPid = fid + ":" + pid;
        module.setPatientId(fidPid);

        // item の patientId を変更する
        Collection<NLaboItem> items = module.getItems();
        for (NLaboItem item : items) {
            item.setPatientId(fidPid);
        }

        //--------------------------------------------------------
        // patientId & 検体採取日 & ラボコード で key
        // これが一致しているモジュールは再報告として削除してから登録する。
        //--------------------------------------------------------
        String sampleDate = module.getSampleDate();
        String laboCode = module.getLaboCenterCode();
        String moduleKey = module.getModuleKey();
        if (moduleKey!=null) {
            StringBuilder sb = new StringBuilder();
            sb.append(pid).append(".").append(sampleDate).append(".").append(laboCode);
            String test = sb.toString();
            if (test.equals(moduleKey)) {
                sb = new StringBuilder();
                sb.append(fid);
                sb.append(":");
                sb.append(moduleKey);
                moduleKey = sb.toString();
                module.setModuleKey(moduleKey);
                System.err.println("corrected moduke key=" + module.getModuleKey());
            } 
        }

        NLaboModule exist = null;

        try {
            if (moduleKey!=null) {
                exist = (NLaboModule)
                        em.createQuery(QUERY_MODULE_BY_MODULE_KEY)
                        .setParameter(MODULEKEY, moduleKey)
                        .getSingleResult();
                System.err.println("module did exist");

            } else {
                exist = (NLaboModule)
                        em.createQuery(QUERY_MODULE_BY_PID_SAMPLEDATE_LABCODE)
                        .setParameter(FIDPID, fidPid)
                        .setParameter(SAMPLEDATE, sampleDate)
                        .setParameter(LABOCODE, laboCode)
                        .getSingleResult();
            }

        } catch (Exception e) {
            exist = null;
        }

        // Cascade.TYPE=ALL
        if (exist != null) {
            em.remove(exist);
            System.err.println("module did remove");
        }

        // 永続化する
        em.persist(module);

        return patient;
    }


    /**
     * ラボモジュールを検索する。
     * @param patientId     対象患者のID
     * @param firstResult   取得結果リストの最初の番号
     * @param maxResult     取得する件数の最大値
     * @return              ラボモジュールのリスト
     */
    public List<NLaboModule> getLaboTest(String fidPid, int firstResult, int maxResult) {

        //String fidPid = SessionHelper.getQualifiedPid(ctx, patientId);

        //
        // 検体採取日の降順で返す
        //
        List<NLaboModule> ret =
                em.createQuery(QUERY_MODULE_BY_FIDPID)
                .setParameter(FIDPID, fidPid)
                .setFirstResult(firstResult)
                .setMaxResults(maxResult)
                .getResultList();

        for (NLaboModule m : ret) {

            if (m.getReportFormat()!=null && m.getReportFormat().equals(WOLF)) {
                List<NLaboItem> items =
                        em.createQuery(QUERY_ITEM_BY_MID_ORDERBY_SORTKEY)
                        .setParameter(MID, m.getId())
                        .getResultList();
                m.setItems(items);

            } else {
                List<NLaboItem> items =
                        em.createQuery(QUERY_ITEM_BY_MID)
                        .setParameter(MID, m.getId())
                        .getResultList();
                m.setItems(items);
            }
        }
        
//masuda^   旧ラボをマージして返す
        ret.addAll(getMmlLaboModules(fidPid, firstResult, maxResult));
        Collections.sort(ret, Collections.reverseOrder(new SampleDateComparator()));
        ret = ret.subList(0, Math.min(ret.size(), maxResult));
//masuda$
        
        return ret;
    }


    /**
     * 指定された検査項目を検索する。
     * @param patientId     患者ID
     * @param firstResult   最初の結果
     * @param maxResult     戻す件数の最大値
     * @param itemCode      検索する検査項目コード
     * @return              検査項目コードが降順に格納されたリスト
     */
    public List<NLaboItem> getLaboTestItem(String fidPid, int firstResult, int maxResult, String itemCode) {

        //String fidPid = SessionHelper.getQualifiedPid(ctx, patientId);

        List<NLaboItem> ret =
                em.createQuery(QUERY_ITEM_BY_FIDPID_ITEMCODE)
                .setParameter(FIDPID, fidPid)
                .setParameter(ITEM_CODE, itemCode)
                .setFirstResult(firstResult)
                .setMaxResults(maxResult)
                .getResultList();

        return ret;
    }
    
    
//masuda^ 旧ラボ
    /* 上の
     * public List<NLaboItem> getLaboTestItem(String fidPid, int firstResult, int maxResult, String itemCode)
     * は現在使っていないようだ。旧ラボのコード書かなくて済んだ…
    */
    
    // MML形式のラボデータを登録する
    public PatientModel putLaboModule(String fid, LaboModuleValue laboModuleValue) {
        
        String patientId = laboModuleValue.getPatientId();
        
        try {
            // MMLファイルをパースした結果が登録される
            // 施設IDと LaboModule の患者IDで 患者を取得する
            PatientModel exist = (PatientModel)
                    em.createQuery("from PatientModel p where p.facilityId = :fid and p.patientId = :pid")
                    .setParameter("fid", fid)
                    .setParameter("pid", patientId)
                    .getSingleResult();

            // 患者のカルテを取得する
            KarteBean karte = (KarteBean)
                    em.createQuery("from KarteBean k where k.patient.id = :pk")
                    .setParameter("pk", exist.getId())
                    .getSingleResult();

            // laboModuleとカルテの関係を設定する
            laboModuleValue.setKarteBean(karte);

            // 永続化する
            em.persist(laboModuleValue);

            // 保険情報を設定する
            setHealthInsurances(exist);
            
            // PatientModelを返す
            return exist;

        } catch (Exception e) {
        }

        return null;
    }
    
    // MML形式のラボデータを取得する。後でマージする
    public List<NLaboModule> getMmlLaboModules(String fidPid, int firstResult, int maxResult) {
        
        String[] str = fidPid.split(":");
        String fid = str[0];
        String patientId = str[1];

        // 即時フェッチではない
        List<LaboModuleValue> modules =
                em.createQuery("from LaboModuleValue l where l.karte.patient.facilityId = :fid "
                + "and l.karte.patient.patientId = :ptId order by l.sampleTime desc")
                .setParameter("fid", fid)
                .setParameter("ptId", patientId)
                .setFirstResult(firstResult)
                .setMaxResults(maxResult)
                .getResultList();

        for (LaboModuleValue module : modules) {
            List<LaboSpecimenValue> specimens =
                    em.createQuery("from LaboSpecimenValue l where l.laboModule.id = :moduleId")
                    .setParameter("moduleId", module.getId())
                    .getResultList();
            module.setLaboSpecimens(specimens);

            for (LaboSpecimenValue specimen : specimens) {
                List<LaboItemValue> items =
                        em.createQuery("from LaboItemValue l where l.laboSpecimen.id = :specimenId")
                        .setParameter("specimenId", specimen.getId())
                        .getResultList();
                specimen.setLaboItems(items);
            }
        }
        
        // LaboModule -> NLaboModule
        List<NLaboModule> list = new ArrayList<NLaboModule>();
        for (LaboModuleValue module : modules) {
            NLaboModule nLaboModule = convert(fidPid, module);
            list.add(nLaboModule);
        }
        
        return list;
    }
    
    // 旧ラボ形式をを新ラボに変換する 表示に使う分だけ
    private NLaboModule convert(String patientId, LaboModuleValue labo13) {
        
        String laboCenterCode = labo13.getLaboratoryCenter();
        String sampleDate = labo13.getSampleTime().substring(0, "yyyy-MM-dd".length());

        List<NLaboItem> nLaboItems = new ArrayList<NLaboItem>();
        List<LaboSpecimenValue> specimens = labo13.getLaboSpecimens();
        
        for (LaboSpecimenValue specimen : specimens) {
            
            String specimenCode = specimen.getSpecimenCode();
            String specimenName = specimen.getSpecimenName();
            List<LaboItemValue> items = specimen.getLaboItems();
            
            for (LaboItemValue item : items) {
                NLaboItem nLaboItem = new NLaboItem();
                nLaboItem.setPatientId(patientId);
                nLaboItem.setLaboCode(laboCenterCode);
                nLaboItem.setGroupCode(specimenCode);   // specimenCodeで代用
                nLaboItem.setSpecimenCode(specimenCode);
                nLaboItem.setSpecimenName(specimenName);
                nLaboItem.setParentCode(item.getItemCode());
                nLaboItem.setItemCode(item.getItemCode());
                nLaboItem.setItemName(item.getItemName());
                nLaboItem.setValue(item.getItemValue());
                String flg = item.getNout();
                if (!"N".equals(flg)) {
                    nLaboItem.setAbnormalFlg(flg);
                }
                String low = item.getLow();
                String up = item.getUp();
                StringBuilder sb = new StringBuilder();
                if (low != null) {
                    sb.append(low);
                }
                if (low != null || up != null) {
                    sb.append("-");
                }
                if (up != null) {
                    sb.append(up);
                }
                nLaboItem.setNormalValue(sb.toString());
                nLaboItem.setUnit(item.getUnit());
                
                nLaboItems.add(nLaboItem);
            }
        }
        
        NLaboModule module = new NLaboModule();
        module.setPatientId(patientId);
        module.setSampleDate(sampleDate);
        module.setItems(nLaboItems);
        module.setReportFormat("MML");
        module.setId(labo13.getId());
        
        return module;
    }
    
    // 削除
    public int deleteMmlModule(long id) {
        
        LaboModuleValue labo = em.find(LaboModuleValue.class, id);
        if (labo != null) {
            em.remove(labo);
            return 1;
        }
        return 0;
    }
    
    public int deleteNlaboModule(long id) {
        
        NLaboModule labo = em.find(NLaboModule.class, id);
        if (labo != null) {
            em.remove(labo);
            return 1;
        }
        return 0;
    }
//masuda$
    
    private void setHealthInsurances(PatientModel pm) {
        if (pm != null) {
            pm.setHealthInsurances(null);
        }
    }
}
