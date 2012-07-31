package open.dolphin.session;

import java.text.SimpleDateFormat;
import java.util.*;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.*;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
@Named
@Stateless
public class KarteServiceBean {

    //private static final String QUERY_PATIENT_BY_FIDPID = "from PatientModel p where p.facilityId=:fid and p.patientId=:pid";

    private static final String PATIENT_PK = "patientPk";
    private static final String KARTE_ID = "karteId";
    private static final String FROM_DATE = "fromDate";
    private static final String TO_DATE = "toDate";
    private static final String ID = "id";
    private static final String ENTITY = "entity";
    private static final String FID = "fid";
    private static final String PID = "pid";

    private static final String QUERY_KARTE 
            = "from KarteBean k where k.patient.id=:patientPk";
    private static final String QUERY_ALLERGY 
            = "from ObservationModel o where o.karte.id=:karteId and o.observation='Allergy'";
    private static final String QUERY_BODY_HEIGHT 
            = "from ObservationModel o where o.karte.id=:karteId and o.observation='PhysicalExam' and o.phenomenon='bodyHeight'";
    private static final String QUERY_BODY_WEIGHT 
            = "from ObservationModel o where o.karte.id=:karteId and o.observation='PhysicalExam' and o.phenomenon='bodyWeight'";
    private static final String QUERY_PATIENT_VISIT 
            = "from PatientVisitModel p where p.patient.id=:patientPk and p.pvtDate >= :fromDate";
    private static final String QUERY_DOC_INFO 
            = "from DocumentModel d where d.karte.id=:karteId and d.started >= :fromDate and (d.status='F' or d.status='T')";
    private static final String QUERY_PATIENT_MEMO 
            = "from PatientMemoModel p where p.karte.id=:karteId";

    private static final String QUERY_DOCUMENT_INCLUDE_MODIFIED 
            = "from DocumentModel d where d.karte.id=:karteId and d.started >= :fromDate and d.status !='D'";
    private static final String QUERY_DOCUMENT 
            = "from DocumentModel d where d.karte.id=:karteId and d.started >= :fromDate and (d.status='F' or d.status='T')";
    private static final String QUERY_DOCUMENT_BY_LINK_ID 
            = "from DocumentModel d where d.linkId=:id";

    private static final String QUERY_MODULE_BY_DOC_ID 
            = "from ModuleModel m where m.document.id=:id";
    private static final String QUERY_SCHEMA_BY_DOC_ID 
            = "from SchemaModel i where i.document.id=:id";
    private static final String QUERY_MODULE_BY_ENTITY 
            = "from ModuleModel m where m.karte.id=:karteId and m.moduleInfo.entity=:entity and m.started between :fromDate and :toDate and m.status='F'";
    private static final String QUERY_SCHEMA_BY_KARTE_ID 
            = "from SchemaModel i where i.karte.id =:karteId and i.started between :fromDate and :toDate and i.status='F'";

    private static final String QUERY_SCHEMA_BY_FACILITY_ID 
            = "from SchemaModel i where i.karte.patient.facilityId like :fid and i.extRef.sop is not null and i.status='F'";

    private static final String QUERY_DIAGNOSIS_BY_KARTE_DATE 
            = "from RegisteredDiagnosisModel r where r.karte.id=:karteId and r.started >= :fromDate";
    private static final String QUERY_DIAGNOSIS_BY_KARTE_DATE_ACTIVEONLY 
            = "from RegisteredDiagnosisModel r where r.karte.id=:karteId and r.started >= :fromDate and r.ended is NULL";
    private static final String QUERY_DIAGNOSIS_BY_KARTE 
            = "from RegisteredDiagnosisModel r where r.karte.id=:karteId";
    private static final String QUERY_DIAGNOSIS_BY_KARTE_ACTIVEONLY 
            = "from RegisteredDiagnosisModel r where r.karte.id=:karteId and r.ended is NULL";
    
    private static final String TOUTOU = "TOUTOU";
    private static final String TOUTOU_REPLY = "TOUTOU_REPLY";
    private static final String QUERY_LETTER_BY_KARTE_ID 
            = "from TouTouLetter f where f.karte.id=:karteId";
    private static final String QUERY_REPLY_BY_KARTE_ID 
            = "from TouTouReply f where f.karte.id=:karteId";
    private static final String QUERY_LETTER_BY_ID 
            = "from TouTouLetter t where t.id=:id";
    private static final String QUERY_REPLY_BY_ID 
            = "from TouTouReply t where t.id=:id";

    private static final String QUERY_APPO_BY_KARTE_ID_PERIOD
            = "from AppointmentModel a where a.karte.id = :karteId and a.date between :fromDate and :toDate";

    //private static final String QUERY_PVT_BY_ID = "from PatientVisitModel p where p.id=id";
    
    private static final String QUERY_PATIENT_BY_FID_PID 
            = "from PatientModel p where p.facilityId=:fid and p.patientId=:pid";

//masuda^
    private static final String QUERY_LASTDOC_DATE 
            = "select max(m.started) from DocumentModel m where m.karte.id = :karteId and (m.status = 'F' or m.status = 'T')";
    private static final String QUERY_APPOINTMENTS 
            = "from AppointmentModel a where a.karte.id = :karteId and a.started >= :fromDate";
    private static final String QUERY_DOCUMENT_INCLUDE_MODIFIED2 
            = "from DocumentModel d where d.karte.id=:karteId and d.started >= :fromDate";
//masuda$
    
    @PersistenceContext
    private EntityManager em;
    
    public KarteBean getKarte(String fid, String pid, Date fromDate) {
        
        try {
            // 患者レコードは FacilityId と patientId で複合キーになっている
            PatientModel patient
                = (PatientModel)em.createQuery(QUERY_PATIENT_BY_FID_PID)
                .setParameter(FID, fid)
                .setParameter(PID, pid)
                .getSingleResult();
            
//masuda    下のgetKarteにまとめる
            return getKarte(patient.getId(), fromDate);

        } catch (Exception e) {
        }
        
        return null;
    }

    /**
     * カルテの基礎的な情報をまとめて返す。
     * @param patientPk 患者の Database Primary Key
     * @param fromDate 各種エントリの検索開始日
     * @return 基礎的な情報をフェッチした KarteBean
     */
    public KarteBean getKarte(long patientPK, Date fromDate) {

        try {
            // 最初に患者のカルテを取得する
            @SuppressWarnings("unchecked")
            List<KarteBean> kartes = 
                    em.createQuery(QUERY_KARTE)
                    .setParameter(PATIENT_PK, patientPK)
                    .getResultList();
            KarteBean karte = kartes.get(0);

            // カルテの PK を得る
            long karteId = karte.getId();

            // アレルギーデータを取得する
            @SuppressWarnings("unchecked")
            List<ObservationModel> list1 = 
                    em.createQuery(QUERY_ALLERGY)
                    .setParameter(KARTE_ID, karteId)
                    .getResultList();
            if (!list1.isEmpty()) {
                List<AllergyModel> allergies = new ArrayList<AllergyModel>(list1.size());
                for (ObservationModel observation : list1) {
                    AllergyModel allergy = new AllergyModel();
                    allergy.setObservationId(observation.getId());
                    allergy.setFactor(observation.getPhenomenon());
                    allergy.setSeverity(observation.getCategoryValue());
                    allergy.setIdentifiedDate(observation.confirmDateAsString());
                    allergy.setMemo(observation.getMemo());
                    allergies.add(allergy);
                }
                karte.setAllergies(allergies);
            }

            // 身長データを取得する
            @SuppressWarnings("unchecked")
            List<ObservationModel> list2 = 
                    em.createQuery(QUERY_BODY_HEIGHT)
                    .setParameter(KARTE_ID, karteId)
                    .getResultList();
            if (!list2.isEmpty()) {
                List<PhysicalModel> physicals = new ArrayList<PhysicalModel>(list2.size());
                for (ObservationModel observation : list2) {
                    PhysicalModel physical = new PhysicalModel();
                    physical.setHeightId(observation.getId());
                    physical.setHeight(observation.getValue());
                    physical.setIdentifiedDate(observation.confirmDateAsString());
                    physical.setMemo(ModelUtils.getDateAsString(observation.getRecorded()));
                    physicals.add(physical);
                }
                karte.setHeights(physicals);
            }

            // 体重データを取得する
            @SuppressWarnings("unchecked")
            List<ObservationModel> list3 = 
                    em.createQuery(QUERY_BODY_WEIGHT)
                    .setParameter(KARTE_ID, karteId)
                    .getResultList();
            if (!list3.isEmpty()) {
                List<PhysicalModel> physicals = new ArrayList<PhysicalModel>(list3.size());
                for (ObservationModel observation : list3) {
                    PhysicalModel physical = new PhysicalModel();
                    physical.setWeightId(observation.getId());
                    physical.setWeight(observation.getValue());
                    physical.setIdentifiedDate(observation.confirmDateAsString());
                    physical.setMemo(ModelUtils.getDateAsString(observation.getRecorded()));
                    physicals.add(physical);
                }
                karte.setWeights(physicals);
            }

            // 直近の来院日エントリーを取得しカルテに設定する
            @SuppressWarnings("unchecked")
            List<PatientVisitModel> latestVisits = 
                    em.createQuery(QUERY_PATIENT_VISIT)
                    .setParameter(PATIENT_PK, patientPK)
                    .setParameter(FROM_DATE, ModelUtils.getDateAsString(fromDate))
                    .getResultList();

            if (!latestVisits.isEmpty()) {
                List<String> visits = new ArrayList<String>(latestVisits.size());
                for (PatientVisitModel bean : latestVisits) {
                    // 来院日のみを使用する
                    visits.add(bean.getPvtDate());
                }
                karte.setPatientVisits(visits);
            }

            // 文書履歴エントリーを取得しカルテに設定する
            @SuppressWarnings("unchecked")
            List<DocumentModel> documents = 
                    em.createQuery(QUERY_DOC_INFO)
                    .setParameter(KARTE_ID, karteId)
                    .setParameter(FROM_DATE, fromDate)
                    .getResultList();

            if (!documents.isEmpty()) {
                List<DocInfoModel> c = new ArrayList<DocInfoModel>(documents.size());
                for (DocumentModel docBean : documents) {
                    docBean.toDetuch();
                    c.add(docBean.getDocInfoModel());
                }
                karte.setDocInfoList(c);
            }

            // 患者Memoを取得する
            @SuppressWarnings("unchecked")
            List<PatientMemoModel> memo = 
                    em.createQuery(QUERY_PATIENT_MEMO)
                    .setParameter(KARTE_ID, karteId)
                    .getResultList();
            if (!memo.isEmpty()) {
                karte.setMemoList(memo);
            }
//masuda^
            // 最終文書日
            try {
                Date lastDocDate = (Date)
                        em.createQuery(QUERY_LASTDOC_DATE)
                        .setParameter(KARTE_ID, karteId)
                        .getSingleResult();
                karte.setLastDocDate(lastDocDate);
            } catch (NoResultException e) {
            }
            // 予約
            @SuppressWarnings("unchecked")
            List<AppointmentModel> appoList =
                    em.createQuery(QUERY_APPOINTMENTS)
                    .setParameter(KARTE_ID, karteId)
                    .setParameter(FROM_DATE, fromDate)
                    .getResultList();
            if (appoList != null && !appoList.isEmpty()) {
                karte.setAppointmentList(appoList);
            }
//masuda$
            return karte;

        } catch (NoResultException e) {
            // 患者登録の際にカルテも生成してある
        }

        return null;
    }

    /**
     * 文書履歴エントリを取得する。
     * @param karteId カルテId
     * @param fromDate 取得開始日
     * @param status ステータス
     * @return DocInfo のコレクション
     */
    @SuppressWarnings("unchecked")
    public List<DocInfoModel> getDocumentList(long karteId, Date fromDate, boolean includeModifid) {

        List<DocumentModel> documents = null;

        if (includeModifid) {
//masuda^
            //documents = (List<DocumentModel>)em.createQuery(QUERY_DOCUMENT_INCLUDE_MODIFIED)
            documents = 
                    em.createQuery(QUERY_DOCUMENT_INCLUDE_MODIFIED2)
                    .setParameter(KARTE_ID, karteId)
                    .setParameter(FROM_DATE, fromDate)
                    .getResultList();
//masuda
        } else {
            documents = 
                    em.createQuery(QUERY_DOCUMENT)
                    .setParameter(KARTE_ID, karteId)
                    .setParameter(FROM_DATE, fromDate)
                    .getResultList();
        }

        List<DocInfoModel> result = new ArrayList<DocInfoModel>();
        for (DocumentModel doc : documents) {
            // モデルからDocInfo へ必要なデータを移す
            // クライアントが DocInfo だけを利用するケースがあるため
            doc.toDetuch();
            result.add(doc.getDocInfoModel());
        }
        return result;
    }

    /**
     * 文書(DocumentModel Object)を取得する。
     * @param ids DocumentModel の pkコレクション
     * @return DocumentModelのコレクション
     */
//masuda^
/*
    public List<DocumentModel> getDocuments(List<Long> ids) {

        List<DocumentModel> ret = new ArrayList<DocumentModel>(3);

        // ループする
        for (Long id : ids) {

            // DocuentBean を取得する
            DocumentModel document = (DocumentModel) em.find(DocumentModel.class, id);

            // ModuleBean を取得する
            List modules = em.createQuery(QUERY_MODULE_BY_DOC_ID)
            .setParameter(ID, id)
            .getResultList();
            document.setModules(modules);

            // SchemaModel を取得する
            List images = em.createQuery(QUERY_SCHEMA_BY_DOC_ID)
            .setParameter(ID, id)
            .getResultList();
            document.setSchema(images);

            ret.add(document);
        }

        return ret;
    }
*/
    public List<DocumentModel> getDocuments(List<Long> ids) {

        //long t = System.currentTimeMillis();

        // まとめて query改
        @SuppressWarnings("unchecked")
        List<DocumentModel> documentList =
                em.createQuery("from DocumentModel m where m.id in (:ids)")
                .setParameter("ids", ids)
                .getResultList();
        @SuppressWarnings("unchecked")
        List<ModuleModel> moduleList =
                em.createQuery("from ModuleModel m where m.document.id in (:ids)")
                .setParameter("ids", ids)
                .getResultList();
        @SuppressWarnings("unchecked")
        List<SchemaModel> schemaList =
                em.createQuery("from SchemaModel m where m.document.id in (:ids)")
                .setParameter("ids", ids)
                .getResultList();

        HashMap<Long, DocumentModel> dmMap = new HashMap<Long, DocumentModel>();
        for (DocumentModel dm : documentList) {
            dmMap.put(dm.getId(), dm);
        }

        HashMap<Long, List<ModuleModel>> mmMap = new HashMap<Long, List<ModuleModel>>();
        for (ModuleModel mm : moduleList) {
            long id = mm.getDocumentModel().getId();
            List<ModuleModel> mmList = mmMap.get(id);
            if (mmList == null) {
                mmList = new ArrayList<ModuleModel>();
                mmMap.put(id, mmList);
            }
            mmList.add(mm);
        }

        HashMap<Long, List<SchemaModel>> smMap = new HashMap<Long, List<SchemaModel>>();
        for (SchemaModel sm : schemaList) {
            long id = sm.getDocumentModel().getId();
            List<SchemaModel> smList = smMap.get(id);
            if (smList == null) {
                smList = new ArrayList<SchemaModel>();
                smMap.put(id, smList);
            }
            smList.add(sm);
        }

        List<DocumentModel> ret = new ArrayList<DocumentModel>(documentList.size());
        for (long id : ids) {
            DocumentModel dm = dmMap.get(id);
            List<ModuleModel> mmList = mmMap.get(id);
            dm.setModules(mmList != null ? mmList : new ArrayList<ModuleModel>(0));
            List<SchemaModel> smList = smMap.get(id);
            dm.setSchema(smList != null ? smList : new ArrayList<SchemaModel>(0));
            ret.add(dm);
        }
        //System.out.println(System.currentTimeMillis() - t);
        return ret;
    }
//masuda$
    
    /**
     * ドキュメント DocumentModel オブジェクトを保存する。
     * @param karteId カルテId
     * @param document 追加するDocumentModel オブジェクト
     * @return 追加した数
     */
    public long addDocument(DocumentModel document) {

        // 永続化する
        em.persist(document);

        // ID
        long id = document.getId();

        // 修正版の処理を行う
        long parentPk = document.getDocInfoModel().getParentPk();

        if (parentPk != 0L) {

            // 適合終了日を新しい版の確定日にする
            Date ended = document.getConfirmed();

            // オリジナルを取得し 終了日と status = M を設定する
            DocumentModel old = em.find(DocumentModel.class, parentPk);
            old.setEnded(ended);
            old.setStatus(IInfoModel.STATUS_MODIFIED);
            
//masuda^   HibernateSearchのFulTextEntityManagerを用意。修正済みのものはインデックスから削除する
            final FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
            fullTextEntityManager.purge(DocumentModel.class, parentPk);
//masuda$
            
            // 関連するモジュールとイメージに同じ処理を実行する
            @SuppressWarnings("unchecked")
            List<ModuleModel> oldModules = 
                    em.createQuery(QUERY_MODULE_BY_DOC_ID)
                    .setParameter(ID, parentPk)
                    .getResultList();
            for (ModuleModel model : oldModules) {
                model.setEnded(ended);
                model.setStatus(IInfoModel.STATUS_MODIFIED);
            }

            @SuppressWarnings("unchecked")
            List<SchemaModel> oldImages = 
                    em.createQuery(QUERY_SCHEMA_BY_DOC_ID)
                    .setParameter(ID, parentPk)
                    .getResultList();
            for (SchemaModel model : oldImages) {
                model.setEnded(ended);
                model.setStatus(IInfoModel.STATUS_MODIFIED);
            }
        }
        
//masuda^   算定履歴を登録する
        registSanteiHistory(document);
//masuda$
        
        return id;
    }


    public long addDocumentAndUpdatePVTState(DocumentModel document, long pvtPK, int state) {

        // 永続化する
        em.persist(document);

        // ID
        long id = document.getId();

        // 修正版の処理を行う
        long parentPk = document.getDocInfoModel().getParentPk();

        if (parentPk != 0L) {

            // 適合終了日を新しい版の確定日にする
            Date ended = document.getConfirmed();

            // オリジナルを取得し 終了日と status = M を設定する
            DocumentModel old = em.find(DocumentModel.class, parentPk);
            old.setEnded(ended);
            old.setStatus(IInfoModel.STATUS_MODIFIED);

            // 関連するモジュールとイメージに同じ処理を実行する
            @SuppressWarnings("unchecked")
            List<ModuleModel> oldModules = 
                    em.createQuery(QUERY_MODULE_BY_DOC_ID)
                    .setParameter(ID, parentPk)
                    .getResultList();
            for(ModuleModel model : oldModules) {
                model.setEnded(ended);
                model.setStatus(IInfoModel.STATUS_MODIFIED);
            }

            @SuppressWarnings("unchecked")
            List<SchemaModel> oldImages = 
                    em.createQuery(QUERY_SCHEMA_BY_DOC_ID)
                    .setParameter(ID, parentPk)
                    .getResultList();
            for(SchemaModel model : oldImages) {
                model.setEnded(ended);
                model.setStatus(IInfoModel.STATUS_MODIFIED);
            }
        }
        
        try {
            // PVT 更新  state==2 || state == 4
            PatientVisitModel exist = em.find(PatientVisitModel.class, new Long(pvtPK));
            exist.setState(state);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }

        return id;
    }

    /**
     * ドキュメントを論理削除する。
     * @param pk 論理削除するドキュメントの primary key
     * @return 削除した件数
     */
    @SuppressWarnings("unchecked")
    public int deleteDocument(long id) {
        
//masuda^   オリジナルでは修正したり仮保存をした文書を削除できないので改変
/*
        //
        // 対象 Document を取得する
        //
        Date ended = new Date();
        DocumentModel delete = (DocumentModel) em.find(DocumentModel.class, id);

        //
        // 参照している場合は例外を投げる
        //
        if (delete.getLinkId() != 0L) {
            throw new CanNotDeleteException("他のドキュメントを参照しているため削除できません。");
        }

        //
        // 参照されている場合は例外を投げる
        //
        Collection refs = em.createQuery(QUERY_DOCUMENT_BY_LINK_ID)
        .setParameter(ID, id).getResultList();
        if (refs != null && refs.size() >0) {
            CanNotDeleteException ce = new CanNotDeleteException("他のドキュメントから参照されているため削除できません。");
            throw ce;
        }

        //
        // 単独レコードなので削除フラグをたてる
        //
        delete.setStatus(IInfoModel.STATUS_DELETE);
        delete.setEnded(ended);

        //
        // 関連するモジュールに同じ処理を行う
        //
        Collection deleteModules = em.createQuery(QUERY_MODULE_BY_DOC_ID)
        .setParameter(ID, id).getResultList();
        for (Iterator iter = deleteModules.iterator(); iter.hasNext(); ) {
            ModuleModel model = (ModuleModel) iter.next();
            model.setStatus(IInfoModel.STATUS_DELETE);
            model.setEnded(ended);
        }

        //
        // 関連する画像に同じ処理を行う
        //
        Collection deleteImages = em.createQuery(QUERY_SCHEMA_BY_DOC_ID)
        .setParameter(ID, id).getResultList();
        for (Iterator iter = deleteImages.iterator(); iter.hasNext(); ) {
            SchemaModel model = (SchemaModel) iter.next();
            model.setStatus(IInfoModel.STATUS_DELETE);
            model.setEnded(ended);
        }

        return 1;
*/
        // 対象 Document を取得する
        Date ended = new Date();
        List<DocumentModel> deleteList = new ArrayList<DocumentModel>();

        long docPk = id;
        // まずは親分文書を追加していく
        while (docPk != 0) {
            DocumentModel model = em.find(DocumentModel.class, docPk);
            deleteList.add(model);
            docPk = model.getLinkId();
        }
        
        // 次に子分文書を追加していく
        long linkId = id;
        List<DocumentModel> toDelete = null;
        while (toDelete != null && !toDelete.isEmpty()) {
            toDelete =
                    em.createQuery(QUERY_DOCUMENT_BY_LINK_ID)
                    .setParameter(ID, linkId)
                    .getResultList();
            for (DocumentModel model : toDelete) {
                deleteList.add(model);
                linkId = model.getId();
            }
        }

        for (DocumentModel delete : deleteList) {
            
            long delId = delete.getId();
            // 削除フラグをたてる
            delete.setStatus(IInfoModel.STATUS_DELETE);
            delete.setEnded(ended);

            // HibernateSearchのFulTextEntityManagerを用意。削除済みのものはインデックスから削除する
            final FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
            fullTextEntityManager.purge(DocumentModel.class, delId);

            // 関連するモジュールに同じ処理を行う
            @SuppressWarnings("unchecked")
            List<ModuleModel> deleteModules =
                    em.createQuery(QUERY_MODULE_BY_DOC_ID)
                    .setParameter(ID, delId)
                    .getResultList();
            for (ModuleModel model : deleteModules) {
                model.setStatus(IInfoModel.STATUS_DELETE);
                model.setEnded(ended);
            }

            // 関連する画像に同じ処理を行う
            @SuppressWarnings("unchecked")
            List<SchemaModel> deleteImages =
                    em.createQuery(QUERY_SCHEMA_BY_DOC_ID)
                    .setParameter(ID, delId)
                    .getResultList();
            for (SchemaModel model : deleteImages) {
                model.setStatus(IInfoModel.STATUS_DELETE);
                model.setEnded(ended);
            }

            // 削除されたものは算定履歴も削除する
            deleteSanteiHistory(delId);
        }
        
        return 1;
//masuda$
    }

    /**
     * ドキュメントのタイトルを変更する。
     * @param pk 変更するドキュメントの primary key
     * @return 変更した件数
     */
    public int updateTitle(long pk, String title) {
        DocumentModel update = em.find(DocumentModel.class, pk);
        update.getDocInfoModel().setTitle(title);
        return 1;
    }

    /**
     * ModuleModelエントリを取得する。
     * @param spec モジュール検索仕様
     * @return ModuleModelリストのリスト
     */
    
    public List<List<ModuleModel>> getModules(long karteId, String entity, List<Date> fromDate, List<Date> toDate) {

        // 抽出期間は別けられている
        int len = fromDate.size();
        List<List<ModuleModel>> ret = new ArrayList<List<ModuleModel>>(len);

        // 抽出期間セットの数だけ繰り返す
        for (int i = 0; i < len; i++) {

            @SuppressWarnings("unchecked")
            List<ModuleModel> modules = 
                    em.createQuery(QUERY_MODULE_BY_ENTITY)
                    .setParameter(KARTE_ID, karteId)
                    .setParameter(ENTITY, entity)
                    .setParameter(FROM_DATE, fromDate.get(i))
                    .setParameter(TO_DATE, toDate.get(i))
                    .getResultList();

            ret.add(modules);
        }

        return ret;
    }

    /**
     * SchemaModelエントリを取得する。
     * @param karteId カルテID
     * @param fromDate
     * @param toDate
     * @return SchemaModelエントリの配列
     */
    public List<List<SchemaModel>> getImages(long karteId, List<Date> fromDate, List<Date> toDate) {

        // 抽出期間は別けられている
        int len = fromDate.size();
        List<List<SchemaModel>> ret = new ArrayList<List<SchemaModel>>(len);

        // 抽出期間セットの数だけ繰り返す
        for (int i = 0; i < len; i++) {

            @SuppressWarnings("unchecked")
            List<SchemaModel> modules = 
                    em.createQuery(QUERY_SCHEMA_BY_KARTE_ID)
                    .setParameter(KARTE_ID, karteId)
                    .setParameter(FROM_DATE, fromDate.get(i))
                    .setParameter(TO_DATE, toDate.get(i))
                    .getResultList();

            ret.add(modules);
        }

        return ret;
    }

    /**
     * 画像を取得する。
     * @param id SchemaModel Id
     * @return SchemaModel
     */
    public SchemaModel getImage(long id) {
        SchemaModel image = em.find(SchemaModel.class, id);
        return image;
    }

    public List<SchemaModel> getS3Images(String fid, int firstResult, int maxResult) {

        @SuppressWarnings("unchecked")
        List<SchemaModel> ret = 
                em.createQuery(QUERY_SCHEMA_BY_FACILITY_ID)
                .setParameter(FID, fid+"%")
                .setFirstResult(firstResult)
                .setMaxResults(maxResult)
                .getResultList();
        
        return ret;
    }

    public void deleteS3Image(long pk) {
        SchemaModel target = em.find(SchemaModel.class, pk);
        target.getExtRefModel().setBucket(null);
        target.getExtRefModel().setSop(null);
        target.getExtRefModel().setUrl(null);
    }

    /**
     * 傷病名リストを取得する。
     * @param spec 検索仕様
     * @return 傷病名のリスト
     */
    @SuppressWarnings("unchecked")
    public List<RegisteredDiagnosisModel> getDiagnosis(long karteId, Date fromDate, boolean activeOnly) {

        List<RegisteredDiagnosisModel> ret = null;

        // 疾患開始日を指定している
        if (fromDate != null) {
            String query = activeOnly ? QUERY_DIAGNOSIS_BY_KARTE_DATE_ACTIVEONLY : QUERY_DIAGNOSIS_BY_KARTE_DATE;
            ret =  em.createQuery(query)
                    .setParameter(KARTE_ID, karteId)
                    .setParameter(FROM_DATE, fromDate)
                    .getResultList();
        } else {
            // 全期間の傷病名を得る
            String query = activeOnly ? QUERY_DIAGNOSIS_BY_KARTE_ACTIVEONLY : QUERY_DIAGNOSIS_BY_KARTE;
            ret = em.createQuery(query)
                    .setParameter(KARTE_ID, karteId)
                    .getResultList();
        }

        return ret;
    }

    /**
     * 傷病名を追加する。
     * @param addList 追加する傷病名のリスト
     * @return idのリスト
     */
    public List<Long> addDiagnosis(List<RegisteredDiagnosisModel> addList) {

        List<Long> ret = new ArrayList<Long>(addList.size());

        for (RegisteredDiagnosisModel bean : addList) {
            em.persist(bean);
            ret.add(new Long(bean.getId()));
        }

        return ret;
    }

    /**
     * 傷病名を更新する。
     * @param updateList
     * @return 更新数
     */
    public int updateDiagnosis(List<RegisteredDiagnosisModel> updateList) {

        int cnt = 0;

        for (RegisteredDiagnosisModel bean : updateList) {
            em.merge(bean);
            cnt++;
        }

        return cnt;
    }

    /**
     * 傷病名を削除する。
     * @param removeList 削除する傷病名のidリスト
     * @return 削除数
     */
    public int removeDiagnosis(List<Long> removeList) {

        int cnt = 0;

        for (Long id : removeList) {
            RegisteredDiagnosisModel bean = em.find(RegisteredDiagnosisModel.class, id);
            if (bean != null) {
                em.remove(bean);
                cnt++;
            }
        }

        return cnt;
    }

    /**
     * Observationを取得する。
     * @param spec 検索仕様
     * @return Observationのリスト
     */
    @SuppressWarnings("unchecked")
    public List<ObservationModel> getObservations(long karteId, String observation, String phenomenon, Date firstConfirmed) {

        List<ObservationModel> ret = null;

        if (observation != null) {
            if (firstConfirmed != null) {
                ret = em.createQuery("from ObservationModel o where o.karte.id=:karteId and o.observation=:observation and o.started >= :firstConfirmed")
                .setParameter(KARTE_ID, karteId)
                .setParameter("observation", observation)
                .setParameter("firstConfirmed", firstConfirmed)
                .getResultList();

            } else {
                ret = em.createQuery("from ObservationModel o where o.karte.id=:karteId and o.observation=:observation")
                .setParameter(KARTE_ID, karteId)
                .setParameter("observation", observation)
                .getResultList();
            }
        } else if (phenomenon != null) {
            if (firstConfirmed != null) {
                ret = em.createQuery("from ObservationModel o where o.karte.id=:karteId and o.phenomenon=:phenomenon and o.started >= :firstConfirmed")
                .setParameter(KARTE_ID, karteId)
                .setParameter("phenomenon", phenomenon)
                .setParameter("firstConfirmed", firstConfirmed)
                .getResultList();
            } else {
                ret = em.createQuery("from ObservationModel o where o.karte.id=:karteId and o.phenomenon=:phenomenon")
                .setParameter(KARTE_ID, karteId)
                .setParameter("phenomenon", phenomenon)
                .getResultList();
            }
        }
        return ret;
    }

    /**
     * Observationを追加する。
     * @param observations 追加するObservationのリスト
     * @return 追加したObservationのIdリスト
     */
    public List<Long> addObservations(List<ObservationModel> observations) {

        if (observations != null && observations.size() > 0) {

            List<Long> ret = new ArrayList<Long>(observations.size());

            for (ObservationModel model : observations) {
                em.persist(model);
                ret.add(new Long(model.getId()));
            }

            return ret;
        }
        return null;
    }

    /**
     * Observationを削除する。
     * @param observations 削除するObservationのリスト
     * @return 削除した数
     */
    public int removeObservations(List<Long> observations) {
        if (observations != null && observations.size() > 0) {
            int cnt = 0;
            for (Long id : observations) {
                ObservationModel model = em.find(ObservationModel.class, id);
                if (model != null) {
                    em.remove(model);
                    cnt++;
                }
            }
            return cnt;
        }
        return 0;
    }

    /**
     * 患者メモを更新する。
     * @param memo 更新するメモ
     */
    public int updatePatientMemo(PatientMemoModel memo) {

        int cnt = 0;

        if (memo.getId() == 0L) {
            em.persist(memo);
        } else {
            em.merge(memo);
        }
        cnt++;
        return cnt;
    }

    //--------------------------------------------------------------------------

    public List<List<AppointmentModel>> getAppointmentList(long karteId, List<Date> fromDate, List<Date> toDate) {

        // 抽出期間は別けられている
        int len = fromDate.size();
        List<List<AppointmentModel>> ret = new ArrayList<List<AppointmentModel>>(len);

        // 抽出期間セットの数だけ繰り返す
        for (int i = 0; i < len; i++) {

            @SuppressWarnings("unchecked")
            List<AppointmentModel> modules = 
                    em.createQuery(QUERY_APPO_BY_KARTE_ID_PERIOD)
                    .setParameter(KARTE_ID, karteId)
                    .setParameter(FROM_DATE, fromDate.get(i))
                    .setParameter(TO_DATE, toDate.get(i))
                    .getResultList();

            ret.add(modules);
        }

        return ret;
    }

    
//masuda^   算定情報登録
    private static final SimpleDateFormat frmt2 = new SimpleDateFormat("yyyyMMdd");
    
    //@Asynchronous
    private void registSanteiHistory(DocumentModel document) {
        
        List<ModuleModel> mmList = document.getModules();
        Date date = document.getStarted();
        
        // 保存するDocumentで電子点数表に関連のあるsrycdを取得する
        Set<String> srycdSet = new HashSet<String>();
        for (ModuleModel mm : mmList) {
            String entity = mm.getModuleInfoBean().getEntity();
            if (IInfoModel.MODULE_PROGRESS_COURSE.equals(entity)) {
                continue;
            }
            mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            if (cb == null) {
                continue;
            }
            for (ClaimItem ci : cb.getClaimItem()) {
                srycdSet.add(ci.getCode());
            }
        }
        // 空ならリターン
        if (srycdSet.isEmpty()) {
            return;
        }
        List<String> srycdList = getETenRelatedSrycdList(date, srycdSet);
        
        // 電子点数表に関連のあるものは算定履歴に登録する。
        for (ModuleModel mm : mmList) {
            String entity = mm.getModuleInfoBean().getEntity();
            if (IInfoModel.MODULE_PROGRESS_COURSE.equals(entity)) {
                continue;
            }
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            if (cb == null) {
                continue;
            }
            int bundleNumber = parseInt(cb.getBundleNumber());
            for (int i = 0; i < cb.getClaimItem().length; ++i) {
                ClaimItem ci = cb.getClaimItem()[i];
                if (!srycdList.contains(ci.getCode())) {
                    continue;
                }
                int claimNumber = parseInt(ci.getNumber());
                int count = bundleNumber * claimNumber;
                SanteiHistoryModel history = new SanteiHistoryModel();
                history.setSrycd(ci.getCode());
                history.setItemCount(count);
                history.setItemIndex(i);
                history.setModuleModel(mm);
                em.persist(history);
            }
        }
        
        // 修正されたものは削除する
        long parentPk = document.getDocInfoModel().getParentPk();
        if (parentPk != 0) {
            deleteSanteiHistory(parentPk);
        }
    }
    
    private int parseInt(String str) {

        int num = 1;
        try {
            num = Integer.valueOf(str);
        } catch (Exception e) {
        }
        return num;
    }
    
    // 指定されたidのDocumentModelに関連するSanteiHistoryModelを削除する
    //@Asynchronous
    private void deleteSanteiHistory(long docPk) {
        
        final String sql = "delete from SanteiHistoryModel s where s.moduleModel.id = :mId";

        DocumentModel document = em.find(DocumentModel.class, docPk);
        for (ModuleModel mm : document.getModules()) {
            long mId = mm.getId();
            em.createQuery(sql).setParameter("mId", mId).executeUpdate();
        }

    }
    
    @SuppressWarnings("unchecked")
    private List<String> getETenRelatedSrycdList(Date date, Collection<String> srycds) {

        final String sql1 = "select distinct e.srycd from ETensuModel1 e";
        final String sql2 = sql1 + " where e.yukostymd <= :date and :date <= e.yukoedymd";
        final String sql3 = sql2 + " and e.srycd in (:srycds)";
        
        List<String> list = null;
        if (date == null && srycds == null) {
            list = em.createQuery(sql1).getResultList();
        } else {
            String ymd = frmt2.format(date);
            if (srycds == null) {
                list = em.createQuery(sql2)
                        .setParameter("date", ymd)
                        .getResultList();
            } else {
                list = em.createQuery(sql3)
                        .setParameter("date", ymd)
                        .setParameter("srycds", srycds)
                        .getResultList();
            }
        }
        return list;
    }
//masuda$
}
