package open.dolphin.session;

import java.text.SimpleDateFormat;
import java.util.*;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import open.dolphin.infomodel.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;

/**
 * MasudaServiceBean
 * @author masuda, Masuda Naika
 */
public class MasudaServiceBean extends AbstractServiceBean {

    private static final String FINISHED = "finished";
    
    
    // 定期処方
    @SuppressWarnings("unchecked")
    public List<RoutineMedModel> getRoutineMedModels(long karteId, int firstResult, int maxResults) {
        
        final String sql = "from RoutineMedModel r where r.karteId = :kId and r.bookmark = :bookmark";
        
        // bookmarkなしのものは指定した数だけ取得する
        List<RoutineMedModel> list1 =
                em.createQuery(sql)
                .setParameter("kId", karteId)
                .setParameter("bookmark", false)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
        
        // bookmarkありのものはすべて取得する
        List<RoutineMedModel> list2 =
                em.createQuery(sql)
                .setParameter("kId", karteId)
                .setParameter("bookmark", true)
                .getResultList();
        
        // マージする
        for (RoutineMedModel model : list2) {
            if (!list1.contains(model)) {
                list1.add(model);
            }
        }
        
        // PostgresでFetchType.EAGERにすると
        // org.hibernate.HibernateException: cannot simultaneously fetch multiple bags
        // それゆえFetchType.LAZYにしたので… トホホ MySQLだとOKなんだよぅ
        for (RoutineMedModel model : list1) {
            fetchModuleModelList(model);
        }

        Collections.sort(list1);
        
        return list1;
    }
    
    public RoutineMedModel getRoutineMedModel(long id) {
        RoutineMedModel model = em.find(RoutineMedModel.class, id);
        fetchModuleModelList(model);
        return model;
    }
    
    private void fetchModuleModelList(RoutineMedModel model) {

        final String sql = "from ModuleModel m where m.id in (:ids)";
        
        List<ModuleModel> mmListTemp = model.getModuleList();
        List<Long> idList = new ArrayList<Long>();
        for (ModuleModel mm : mmListTemp) {
            idList.add(mm.getId());
        }
        @SuppressWarnings("unchecked")
        List<ModuleModel> mmList =
                em.createQuery(sql)
                .setParameter("ids", idList)
                .getResultList();
        model.setModuleList(mmList);
    }

    public long removeRoutineMedModel(long id) {
        RoutineMedModel model = em.find(RoutineMedModel.class, id);
        if (model != null) {
            em.remove(model);
            return model.getId();
        }
        return -1;
    }
    
    public long addRoutineMedModel(RoutineMedModel model) {
        em.persist(model);
        return model.getId();
    }
    
    public long updateRoutineMedModel(RoutineMedModel model) {
        em.merge(model);
        return model.getId();
    }
    
    // 採用薬
    public List<UsingDrugModel> getUsingDrugModels(String fid) {

        final String sql1 = "from UsingDrugModel u where u.facilityId = :fid";

        @SuppressWarnings("unchecked")
        List<UsingDrugModel> list =
                em.createQuery(sql1)
                .setParameter("fid", fid)
                .getResultList();

        return list;
    }

    public long addUsingDrugModel(UsingDrugModel model) {
        model.setCreated(new Date());
        em.persist(model);
        return model.getId();
    }

    public long removeUsingDrugModel(long id) {
        // 分離オブジェクトは remove に渡せないので対象を検索する
        UsingDrugModel target = em.find(UsingDrugModel.class, id);
        if (target != null) {
            em.remove(target);
            return target.getId();
        }
        return -1;
    }

    public long updateUsingDrugModel(UsingDrugModel model) {
        model.setCreated(new Date());
        em.merge(model);
        return model.getId();
    }

    // 中止項目
    public List<DisconItemModel> getDisconItems(String fid) {

        final String sql1 = "from DisconItemModel d where d.facilityId = :fid";

        @SuppressWarnings("unchecked")
        List<DisconItemModel> list =
                em.createQuery(sql1)
                .setParameter("fid", fid)
                .getResultList();

        return list;
    }

    public long addDisconItem(DisconItemModel model) {
        em.persist(model);
        return model.getId();
    }

    public long removeDisconItem(long id) {
        // 分離オブジェクトは remove に渡せないので対象を検索する
        DisconItemModel target = em.find(DisconItemModel.class, id);
        if (target != null) {
            em.remove(target);
            return target.getId();
        }
        return -1;
    }

    public long updateDisconItem(DisconItemModel model) {
        em.merge(model);
        return model.getId();
    }


    // 指定したEntityのModuleModleを一括取得
    @SuppressWarnings("unchecked")
    public List<ModuleModel> getModulesEntitySearch(String fid, long karteId, Date fromDate, Date toDate, List<String> entities) {
        
        // 指定したentityのModuleModelを返す
        List<ModuleModel> ret;
        
        if (karteId != 0){
            final String sql = "from ModuleModel m where m.karte.id = :karteId " +
                    "and m.started between :fromDate and :toDate and m.status='F' " +
                    "and m.moduleInfo.entity in (:entities)";

            ret = em.createQuery(sql)
                    .setParameter("karteId", karteId)
                    .setParameter("fromDate", fromDate)
                    .setParameter("toDate", toDate)
                    .setParameter("entities", entities)
                    .getResultList();
        } else {
            // karteIdが指定されていなかったら、施設の指定期間のすべて患者のModuleModelを返す
            long fPk = getFacilityPk(fid);
            final String sql = "from ModuleModel m " +
                    "where m.started between :fromDate and :toDate " +
                    "and m.status='F' " +
                    "and m.moduleInfo.entity in (:entities)" +
                    "and m.creator.facility.id = :fPk";

            ret = em.createQuery(sql)
                    .setParameter("fromDate", fromDate)
                    .setParameter("toDate", toDate)
                    .setParameter("entities",entities)
                    .setParameter("fPk", fPk)
                    .getResultList();
        }

        return ret;
    }

    // FEV-70関連
    public PatientVisitModel getLastPvtInThisMonth(String fid, String ptId) {

        final SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        GregorianCalendar gc = new GregorianCalendar();
        int year = gc.get(GregorianCalendar.YEAR);
        int month = gc.get(GregorianCalendar.MONTH);
        gc.clear();
        gc.set(year, month, 1);
        String fromDate = frmt.format(gc.getTime());
        String toDate = frmt.format(new Date());
        final String sql = "from PatientVisitModel p " +
                "where p.facilityId = :fid and p.patient.patientId = :ptId " +
                "and p.pvtDate >= :fromDate and p.pvtDate < :toDate order by p.pvtDate desc";
        PatientVisitModel result = null;
        try {
            result = (PatientVisitModel)
                    em.createQuery(sql)
                    .setParameter("fid", fid)
                    .setParameter("ptId", ptId)
                    .setParameter("fromDate", fromDate)
                    .setParameter("toDate", toDate)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
        }
        return result;
    }

    // 指定されたdocPkのDocInfoModelを返す
    public List<DocInfoModel> getDocumentList(List<Long> docPkList) {

        if (docPkList == null || docPkList.isEmpty()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<DocumentModel> documents =
                em.createQuery("from DocumentModel d where d.id in (:docPkList)")
                .setParameter("docPkList", docPkList)
                .getResultList();

        List<DocInfoModel> result = new ArrayList<DocInfoModel>();
        for (DocumentModel docBean : documents) {
            // モデルからDocInfo へ必要なデータを移す
            // クライアントが DocInfo だけを利用するケースがあるため
            docBean.toDetuch();
            result.add(docBean.getDocInfoModel());
        }
        return result;
    }

    // DocumentModelのHiberbate search用インデックスを作成する
    public String makeDocumentModelIndex(String fid, long fromDocPk, int maxResults) {

        long fPk = getFacilityPk(fid);
        if (fPk == 0) {
            System.out.println("Hibernate Search: Illegal facility id.");
            return FINISHED;
        }

        final String fromSql = "from DocumentModel m where m.status = 'F' and m.creator.facility.id = :fPk and m.id > :fromPk";
        final FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

        // fromPk == 0の場合、まずはインデックスをクリアする
        if (fromDocPk == 0) {
            // これはサーバーに複数施設が同居してるとよくない
            //fullTextEntityManager.purgeAll(DocumentModel.class);
            purgeIndex(fPk);
        }

        // 総DocumentModel数を取得。進捗表示に使用
        long modelCount = (Long)
                em.createQuery("select count(m) " + fromSql)
                .setParameter("fPk", fPk)
                .setParameter("fromPk", 0L)
                .getSingleResult();

        // idがfromPkより大きいDocumentModelをmaxResultsずつ取得
        @SuppressWarnings("unchecked")
        List<DocumentModel> models =
                em.createQuery(fromSql)
                .setParameter("fPk", fPk)
                .setParameter("fromPk", fromDocPk)
                .setMaxResults(maxResults)
                .getResultList();

        // 該当なしならnullを返して終了
        if (models == null || models.isEmpty()) {
            System.out.println("Hibernate Search: indexing task done.");
            return FINISHED;
        }
        // サーバーでの進捗状況表示
        //long fromId = models.get(0).getId();
        long toId = models.get(models.size() - 1).getId();
        //System.out.println("Hibernate Search: indexing from " + fromId + " to " + toId);

        // DocumentModelのインデックスを作成
        for (DocumentModel dm : models) {
            fullTextEntityManager.index(dm);
        }

        // 返り値は、最後のDocPk:総DocumentModel数
        return String.format("%d,%d", toId, modelCount);
    }

    // 施設のHibernage Searchインデックスを消去する
    private void purgeIndex(long fPk) {
        //System.out.println("Hibernate Search: purging indexes.");
        final String sql = "select m.id from DocumentModel m where m.creator.facility.id = :fPk";
        final FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        @SuppressWarnings("unchecked")
        List<Long> pkList =
                em.createQuery(sql).setParameter("fPk", fPk).getResultList();
        for (long pk : pkList) {
            fullTextEntityManager.purge(DocumentModel.class, pk);
        }
    }

    // Hibernate searchを利用して全文検索する
    @SuppressWarnings("unchecked")
    public List<PatientModel> getKarteFullTextSearch(String fid, long karteId, String text) {

        long fPk = getFacilityPk(fid);
        if (fPk == 0) {
            return null;
        }

        HashSet<PatientModel> patientModelSet = new HashSet<PatientModel>();
        HashSet<Long> karteIdSet = new HashSet<Long>();

        // karteId == 0なら全患者から検索。PatientMemoModelも検索する
        if (karteId == 0) {
            List<Long> memoResult = getKarteOfMemo(fid, text);
            if (memoResult != null) {
                karteIdSet.addAll(memoResult);
            }
        }

        // DocumentModelを検索
        final FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        
        final Analyzer analyzer = fullTextEntityManager.getSearchFactory().getAnalyzer(DocumentModel.class);
        final org.apache.lucene.util.Version ver = org.apache.lucene.util.Version.LUCENE_34;
        QueryParser parser =
                new QueryParser(ver, "modules.beanBytes", analyzer);
        // http://lucene.jugem.jp/?eid=403
        parser.setAutoGeneratePhraseQueries(true);
        
        try {
            org.apache.lucene.search.Query luceneQuery = parser.parse(text);
        FullTextQuery fullTextQuery =
                fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentModel.class);

        if (karteId != 0) {
            // karteIdでフィルタリング
            fullTextQuery.enableFullTextFilter("karteId").setParameter("karteId", karteId);
        } else {
            // facilityIdでフィルタリング
            fullTextQuery.enableFullTextFilter("facilityPk").setParameter("facilityPk", fPk);
        }

        // DocumentModelを取得
        List<DocumentModel> result = fullTextQuery.getResultList();
        // karteIdとDocIdの対応マップを作成
        HashMap<Long, List<Long>> karteIdDocIdMap = new HashMap<Long, List<Long>>();

        for (DocumentModel dm : result) {
            long kid = dm.getKarteBean().getId();
            List<Long> docIdList = karteIdDocIdMap.get(kid);
            if (docIdList == null) {
                docIdList = new ArrayList<Long>();
            }
            docIdList.add(dm.getId());
            karteIdDocIdMap.put(kid, docIdList);
        }

        // karteIdに対応するPatientModelを取得する
        karteIdSet.addAll(karteIdDocIdMap.keySet());

        for (long kid : karteIdSet) {
            KarteBean karte = em.find(KarteBean.class, kid);
            long patientId = karte.getPatient().getId();
            PatientModel pm = em.find(PatientModel.class, patientId);
            // PatientModelに検索語とDocIdを設定する。
            pm.setSearchText(text);
            List<Long> docIdList = karteIdDocIdMap.get(kid);
            if (docIdList != null) {
                HashSet<Long> pkSet = new HashSet<Long>();
                pkSet.addAll(docIdList);
                if (pm.getDocPkList() != null) {
                    pkSet.addAll(pm.getDocPkList());
                }
                pm.setDocPkList(new ArrayList(pkSet));
            }
            patientModelSet.add(pm);
        }
        } catch (ParseException ex) {
        }

        // 保険情報をとPvtDateを設定する
        List<PatientModel> ret = new ArrayList<PatientModel>(patientModelSet);
        setInsuranceAndPvtDate(fid, ret);

        return ret;
    }

    private long getFacilityPk(String fid) {

        try {
            long facilityPk = (Long)
                    em.createQuery("select f.id from FacilityModel f where f.facilityId = :fid")
                    .setParameter("fid", fid)
                    .getSingleResult();
            return facilityPk;
        } catch (NoResultException e) {
        }
        return 0;
    }

    // Memo検索
    private List<Long> getKarteOfMemo(String fid, String text) {

        final String sql = "select p.karte.id from PatientMemoModel p " +
                "where p.memo like :memo " +
                "and p.creator.facility.facilityId = :fid";
        @SuppressWarnings("unchecked")
        List<Long> karteIdList =
                em.createQuery(sql)
                .setParameter("memo", "%" + text + "%")
                .setParameter("fid", fid)
                .getResultList();
        return karteIdList;
    }

    // 保険情報をとPvtDateを設定する
    private void setInsuranceAndPvtDate(String fid, List<PatientModel> list) {

        final int CANCEL_PVT = 1 << 6;  // BIT_CANCEL = 6;
        final String sqlPvt = "from PatientVisitModel p where p.facilityId = :fid " +
                "and p.patient.id = :patientPk and p.status != :status order by p.pvtDate desc";

        for (PatientModel pm : list) {
            // 患者の健康保険を取得する
            setHealthInsurances(pm);

            try {
                PatientVisitModel pvt = (PatientVisitModel)
                        em.createQuery(sqlPvt)
                        .setParameter("fid", fid)
                        .setParameter("patientPk", pm.getId())
                        .setParameter("status", CANCEL_PVT)
                        .setMaxResults(1)
                        .getSingleResult();
                pm.setPvtDate(pvt.getPvtDate());
            } catch (NoResultException e) {
            }
        }
    }

    // grep方式の全文検索
    @SuppressWarnings("unchecked")
    public SearchResultModel getSearchResult(String fid, String searchText, long fromModuleId, int maxResult, boolean progressCourseOnly) {

        final String fromSql = "from ModuleModel m where m.status = 'F' and m.creator.facility.facilityId = :fid";
        final String progressCourse = " and m.moduleInfo.entity = '" + IInfoModel.MODULE_PROGRESS_COURSE + "'";

        String sql1 = fromSql + " and m.id > :fromId";
        String sql2 = "select count(m) " + fromSql;

        if (progressCourseOnly) {
            sql1 = sql1 + progressCourse;
            sql2 = sql2 + progressCourse;
        }

        HashSet<PatientModel> patientModelSet = new HashSet<PatientModel>();
        HashSet<Long> karteIdSet = new HashSet<Long>();

        // ModuleModelを取得
        List<ModuleModel> modules =
                em.createQuery(sql1)
                .setParameter("fromId", fromModuleId)
                .setParameter("fid", fid)
                .setMaxResults(maxResult)
                .getResultList();

        if (modules.isEmpty()) {
            // 該当なしならnullを返す
            return null;
        }
        long toId = modules.get(modules.size() - 1).getId();

        // 検索語を含むkarteIdとDocIdの対応マップを作成
        HashMap<Long, List<Long>> karteIdDocIdMap = new HashMap<Long, List<Long>>();
        for (ModuleModel mm : modules) {
            // テキスト抽出
            IInfoModel im = (IInfoModel) ModelUtils.xmlDecode(mm.getBeanBytes());
            mm.setModel(im);
            String text;
            if (im instanceof ProgressCourse) {
                String xml = ((ProgressCourse) im).getFreeText();
                text = ModelUtils.extractText(xml);
            } else {
                text = im.toString();
            }
            // 検索語を含むかどうか
            if (text.contains(searchText)) {
                long docId = mm.getDocumentModel().getId();
                long karteId = mm.getKarteBean().getId();
                List<Long> docIdList = karteIdDocIdMap.get(karteId);
                if (docIdList == null) {
                    docIdList = new ArrayList<Long>();
                }
                docIdList.add(docId);
                karteIdDocIdMap.put(karteId, docIdList);
            }
        }

        // karteIdに対応するPatientModelを取得する
        karteIdSet.addAll(karteIdDocIdMap.keySet());
        for (long kid : karteIdSet) {
            KarteBean karte = em.find(KarteBean.class, kid);
            long patientId = karte.getPatient().getId();
            PatientModel pm = em.find(PatientModel.class, patientId);
            pm.setSearchText(searchText);
            List<Long> docIdList = karteIdDocIdMap.get(kid);

            if (docIdList != null) {
                HashSet<Long> pkSet = new HashSet<Long>();
                pkSet.addAll(docIdList);
                if (pm.getDocPkList() != null) {
                    pkSet.addAll(pm.getDocPkList());
                }
                pm.setDocPkList(new ArrayList(pkSet));
            }

            patientModelSet.add(pm);
        }

        // 保険情報をとPvtDateを設定する
        List<PatientModel> list = new ArrayList<PatientModel>(patientModelSet);
        setInsuranceAndPvtDate(fid, list);

        // 総モジュール数を取得、進捗具合に利用する
        long modelCount = (Long)
                em.createQuery(sql2)
                .setParameter("fid", fid)
                .getSingleResult();
        // 結果を返す
        SearchResultModel ret = new SearchResultModel(toId, modelCount, list);
        return ret;
    }
    
    // 通信量を減らすためサーバー側で検査履歴を調べる
    public List<ExamHistoryModel> getExamHistory(String fid, long karteId, Date fromDate, Date toDate) {

        final List<String> entities = new ArrayList<String>();
        entities.add(IInfoModel.ENTITY_RADIOLOGY_ORDER);
        entities.add(IInfoModel.ENTITY_PHYSIOLOGY_ORDER);
        entities.add(IInfoModel.ENTITY_LABO_TEST);

        List<ModuleModel> models = getModulesEntitySearch(fid, karteId, fromDate, toDate, entities);
        if (models == null) {
            return null;
        }
        // HashMapに登録しておく
        HashMap<Long, ExamHistoryModel> examMap = new HashMap<Long, ExamHistoryModel>();

        for (ModuleModel mm : models) {
            mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
            long docPk = mm.getDocumentModel().getId();
            ExamHistoryModel eh = examMap.get(docPk);
            if (eh == null) {
                eh = new ExamHistoryModel();
            }
            // 検査があるもののみ登録していく
            boolean hasExam = eh.putModuleModel(mm);
            if (hasExam) {
                examMap.put(docPk, eh);
            }
        }
        
        return new ArrayList<ExamHistoryModel>(examMap.values());
    }
    
    // 通信量を減らすためにサーバー側で処方切れを調べる
    public List<PatientModel> getOutOfMedStockPatient(String fid, Date fromDate, Date toDate, int yoyuu) {
        
        final long karteId = 0;
        final List<String> entities = Collections.singletonList(IInfoModel.ENTITY_MED_ORDER);
        List<ModuleModel> mmList = getModulesEntitySearch(fid, karteId, fromDate, toDate, entities);
        if (mmList == null) {
            return null;
        }

        // ModuleModelを患者毎に分類
        HashMap<PatientModel, List<ModuleModel>> pmmmMap = new HashMap<PatientModel, List<ModuleModel>>();
        for (ModuleModel mm : mmList){
            // いつもデコード忘れるｗ
            mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
            PatientModel pModel = mm.getKarteBean().getPatient();
            List<ModuleModel> list = pmmmMap.get(pModel);
            if (list == null){
                list = new ArrayList<ModuleModel>();
            }
            list.add(mm);
            pmmmMap.put(pModel, list);
        }
        // mmListは用なし。メモリ食いそうなのでnullにしてみるが、効果は？
        mmList = null;
        // 患者毎に処方切れかどうか調べる
        List<PatientModel> ret = new ArrayList<PatientModel>();

        for (Iterator itr = pmmmMap.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            PatientModel model = (PatientModel) entry.getKey();
            @SuppressWarnings("unchecked")
            List<ModuleModel> list = (List<ModuleModel>) entry.getValue();
            // 処方日と処方日数を列挙
            HashMap<Date, Integer> dateNumberMap = new HashMap<Date, Integer>();
            for (ModuleModel mm : list){
                // 処方日を取得
                Date date = mm.getDocumentModel().getStarted();
                // 処方日数を更新、外用や頓用の判断は省略ｗ
                int oldNumber = dateNumberMap.get(date) == null ? 0 : dateNumberMap.get(date);
                int newNumber = Integer.valueOf(((BundleMed) mm.getModel()).getBundleNumber());
                if (oldNumber < newNumber) {
                    dateNumberMap.put(date, newNumber);
                }
            }
            // 処方切れかどうかを判断
            Date oldestDate = new Date();
            Date lastDate = ModelUtils.AD1800;
            int totalBundleNumber = 0;
            for (Iterator itr1 = dateNumberMap.entrySet().iterator(); itr1.hasNext();) {
                Map.Entry entry1 = (Map.Entry) itr1.next();
                Date date = (Date) entry1.getKey();
                int bundleNumber = (Integer) entry1.getValue();
                if (date.before(oldestDate)) {
                    oldestDate = date;
                }
                if (date.after(lastDate)) {
                    lastDate = date;
                }
                totalBundleNumber = totalBundleNumber + bundleNumber;
            }
            GregorianCalendar now = new GregorianCalendar();
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(oldestDate);
            gc.add(GregorianCalendar.DATE, totalBundleNumber + yoyuu);
            // 処方切れの可能性があればPatientModelを追加
            if (now.after(gc)) {
                String pvtDate = ModelUtils.getDateTimeAsString(lastDate);
                model.setPvtDate(pvtDate);
                ret.add(model);

                // 患者の健康保険を取得する
                //setHealthInsurances(model);
            }
        }

        return ret;
    }
    
    public List<InFacilityLaboItem> getInFacilityLaboItemList(String fid) {
        final String sql = "from InFacilityLaboItem i where i.laboCode = :fid";
        @SuppressWarnings("unchecked")
        List<InFacilityLaboItem> list = 
                em.createQuery(sql).setParameter("fid", fid).getResultList();
        return list;
    }
    
    public long updateInFacilityLaboItem(String fid, List<InFacilityLaboItem> newList) {
        
        List<InFacilityLaboItem> oldList = getInFacilityLaboItemList(fid);
        // 削除されたものを探すして削除する
        for(InFacilityLaboItem oldItem : oldList) {
            boolean found = false;
            for (InFacilityLaboItem newItem : newList) {
                if (oldItem.getId() == newItem.getId()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                long pk = oldItem.getId();
                InFacilityLaboItem toDelete = em.find(InFacilityLaboItem.class, pk);
                em.remove(toDelete);
            }
        }
        // 変更されたものはmerge, 追加されたものはpersistする
        long added = 0;
        for (InFacilityLaboItem newItem : newList) {
            //newItem.setItemValue(null); // 検査値は消す
            long pk = newItem.getId();
            if (pk == 0) {
                em.persist(newItem);
                added++;
            } else {
                em.merge(newItem);
            }
        }
        return added;
    }
    
    
    // 電子点数表　未使用
    public String updateETensu1Table(List<ETensuModel1> list) {
        
        final String sql1 = "from ETensuModel1 e where e.srycd = :srycd and e.yukostymd = :yukostymd";
        int added = 0;
        int updated = 0;
        
        for (ETensuModel1 model : list) {
            try {
                ETensuModel1 exist = (ETensuModel1) 
                        em.createQuery(sql1)
                        .setParameter("srycd", model.getSrycd())
                        .setParameter("yukostymd", model.getYukostymd())
                        .getSingleResult();
                // 既存の上書き
                model.setId(exist.getId());
                em.merge(model);
                updated++;
            } catch (NoResultException e) {
                // 新規
                em.persist(model);
                added++;
            }
        }
        return String.format("%d,%d", added, updated);
    }
    
    private List<String> getETenRelatedSrycdList(Collection<String> srycds) {

        final String sql1 = "select distinct e.srycd from ETensuModel1 e where e.srycd in (:srycds)";

        if (srycds == null || srycds.isEmpty()) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        List<String> list =
                em.createQuery(sql1)
                .setParameter("srycds", srycds)
                .getResultList();
        
        return list;
    }

    public String initSanteiHistory(String fid, long fromId, int maxResults) {
        
        final String sql1 = "from ModuleModel m "
                + "where m.moduleInfo.entity <> '" + IInfoModel.MODULE_PROGRESS_COURSE + "' "
                + "and m.status = 'F' and m.creator.facility.facilityId = :fid";
        final String sql2 = "select count(m) " + sql1;
        final String sql3 = sql1 + " and m.id > :fromId";
        final String sql4 = "from SanteiHistoryModel s "
                + "where s.moduleModel.id = :mid and s.srycd = :srycd "
                + "and s.itemIndex = :index";
        
        // 総数を取得する
        long totalCount = (Long) em.createQuery(sql2)
                .setParameter("fid", fid)
                .getSingleResult();
        // 0件ならFINESHEDを返して終了
        if (totalCount == 0) {
            return FINISHED;
        }
        
        // fromIdからModuleModelを取得する
        @SuppressWarnings("unchecked")
        List<ModuleModel> mmList =
                em.createQuery(sql3)
                .setParameter("fid", fid)
                .setParameter("fromId", fromId)
                .setMaxResults(maxResults)
                .getResultList();
        
        // 該当なしならFINESHEDを返して終了
        if (mmList == null || mmList.isEmpty()) {
            return FINISHED;
        }
        
        long addedCount = 0;
        long updatedCount = 0;
        long toId = mmList.get(mmList.size() - 1).getId();
        
        // まずはsrycdをリストアップ
        Set<String> srycds = new HashSet<String>();
        for (ModuleModel mm : mmList) {
            mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            if (cb == null) {
                continue;
            }
            for (ClaimItem ci : cb.getClaimItem()) {
                String srycd = ci.getCode();
                if (srycd != null) {
                    srycds.add(srycd);
                }
            }
        }

        // syrcdsのうち電子点数表に関連するものを取得
        List<String> srycdList = getETenRelatedSrycdList(srycds);
        // 該当なしならリターン
        if (srycdList == null || srycdList.isEmpty()) {
            return String.format("%d,%d,%d,%d", toId, totalCount, addedCount, updatedCount);
        }
        
        // 各々のModuleModelを調べる
        for (ModuleModel mm : mmList) {
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
                try {
                    SanteiHistoryModel exist = (SanteiHistoryModel)
                            em.createQuery(sql4)
                            .setParameter("mid", mm.getId())
                            .setParameter("srycd", ci.getCode())
                            .setParameter("index", i)
                            .getSingleResult();
                    exist.setItemCount(count);
                    em.merge(exist);
                    updatedCount++;
                } catch (NoResultException e) {
                    SanteiHistoryModel history = new SanteiHistoryModel();
                    history.setSrycd(ci.getCode());
                    history.setItemCount(count);
                    history.setItemIndex(i);
                    history.setModuleModel(mm);
                    em.persist(history);
                    addedCount++;
                }
            }
        }
        
        return String.format("%d,%d,%d,%d", toId, totalCount, addedCount, updatedCount);
    }
    
    private int parseInt(String str) {

        int num = 1;
        try {
            num = Integer.valueOf(str);
        } catch (Exception e) {
        }
        return num;
    }
    
    @SuppressWarnings("unchecked")
    public List<SanteiHistoryModel> getSanteiHistory(long karteId, Date fromDate, Date toDate, List<String> srycds) {
        
        List<SanteiHistoryModel> list;
        
        if (srycds == null) {
            final String sql = "from SanteiHistoryModel s where s.moduleModel.karte.id = :kId "
                    + "and :fromDate <= s.moduleModel.started and s.moduleModel.started < :toDate";

            list = (List<SanteiHistoryModel>) 
                    em.createQuery(sql)
                    .setParameter("kId", karteId)
                    .setParameter("fromDate", fromDate)
                    .setParameter("toDate", toDate)
                    .getResultList();
        } else {
            final String sql = "from SanteiHistoryModel s where s.moduleModel.karte.id = :kId "
                    + "and :fromDate <= s.moduleModel.started and s.moduleModel.started < :toDate "
                    + "and s.srycd in (:srycds)";
            
            list = (List<SanteiHistoryModel>) 
                    em.createQuery(sql)
                    .setParameter("kId", karteId)
                    .setParameter("fromDate", fromDate)
                    .setParameter("toDate", toDate)
                    .setParameter("srycds", srycds)
                    .getResultList();
        }
        
        // SanteiHistoryModelに算定日と名前を設定する
        for (SanteiHistoryModel shm : list) {
            ModuleModel mm = shm.getModuleModel();
            mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
            ClaimBundle cb = (ClaimBundle) mm.getModel();
            shm.setItemName(cb.getClaimItem()[shm.getItemIndex()].getName());
            shm.setSanteiDate(mm.getStarted());
        }
        return list;
    }
    
    public String getSanteiCount(long karteId, Date fromDate, Date toDate, List<String> srycds) {

        Map<String, Integer> map = new HashMap<String, Integer>();
        for (String srycd : srycds) {
            map.put(srycd, 0);
        }
        
        List<SanteiHistoryModel> list =getSanteiHistory(karteId, fromDate, toDate, srycds);
        for (SanteiHistoryModel shm : list) {
            String srycd = shm.getSrycd();
            int count = map.get(srycd) + 1;
            map.put(srycd, count);
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            String srycd = (String) entry.getKey();
            String num = String.valueOf(entry.getValue());
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(srycd).append(",").append(num);
        }
        return sb.toString();
    }
    
    @SuppressWarnings("unchecked")
    public List<List<RpModel>> getRpModelList(long karteId, Date fromDate, Date toDate, boolean lastOnly) {
        
        final String yakuzaiClassCode = "2";    // 薬剤のclaim class code
        
        final String sql1 = 
                "from DocumentModel d where d.karte.id=:karteId "
                + "and d.started >= :fromDate and d.started < :toDate "
                + "and d.docInfo.hasRp = true and d.status='F' "
                + "order by d.started desc";
        final String sql2 =
                "from ModuleModel m where m.document.id = :docPk "
                + "and m.moduleInfo.entity = '" + IInfoModel.ENTITY_MED_ORDER + "'";
        
        List<List<RpModel>> ret = new ArrayList<List<RpModel>>();
        
        List<DocumentModel> docList;
        
        if (lastOnly) {
            docList = em.createQuery(sql1)
                    .setParameter("karteId", karteId)
                    .setParameter("fromDate", fromDate)
                    .setParameter("toDate", toDate)
                    .setMaxResults(1)
                    .getResultList();
        } else {
            docList = em.createQuery(sql1)
                    .setParameter("karteId", karteId)
                    .setParameter("fromDate", fromDate)
                    .setParameter("toDate", toDate)
                    .getResultList();
        }

        for (DocumentModel doc : docList) {

            @SuppressWarnings("unchecked")
            List<ModuleModel> mmList =
                    em.createQuery(sql2).setParameter("docPk", doc.getId()).getResultList();
            if (mmList.isEmpty()) {
                continue;
            }
            
            List<RpModel> rpModelList = new ArrayList<RpModel>();
            for (ModuleModel mm : mmList) {
                mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
                BundleMed bm = (BundleMed) mm.getModel();
                String rpDay = bm.getBundleNumber();
                String adminSrycd = bm.getAdminCode();
                Date rpDate = mm.getDocumentModel().getStarted();
                for (ClaimItem ci : bm.getClaimItem()) {
                    if (!yakuzaiClassCode.equals(ci.getClassCode())){
                        continue;
                    }
                    // 薬剤なら
                    String drugSrycd = ci.getCode();
                    String drugName = ci.getName();
                    String rpNumber = ci.getNumber();
                    RpModel rpModel = new RpModel(drugSrycd, drugName, adminSrycd, rpNumber, rpDay, rpDate);
                    rpModelList.add(rpModel);
                }
            }
            ret.add(rpModelList);
        }

        return ret;
    }
    
    // UserPropertyを保存する
    public int postUserProperties(List<UserPropertyModel> list) {
    
        final String sql1 = "from UserPropertyModel u where u.key = :key and u.facilityId = :fid and u.userId = :uid";
        final String sql2 = "delete " + sql1;
        
        for (UserPropertyModel model : list) {
            try {
                // 既存のpropertyを取得する
                UserPropertyModel exist = (UserPropertyModel) 
                        em.createQuery(sql1)
                        .setParameter("key", model.getKey())
                        .setParameter("fid", model.getFacilityId())
                        .setParameter("uid", model.getUserId())
                        .getSingleResult();
                // あれば更新
                model.setId(exist.getId());
                em.merge(model);
            } catch (NoResultException ex) {
                // なければ追加
                em.persist(model);
            } catch (NonUniqueResultException ex) {
                // ダブってたら一旦削除してから追加
                int num = em.createQuery(sql2)
                        .setParameter("key", model.getKey())
                        .setParameter("fid", model.getFacilityId())
                        .setParameter("uid", model.getUserId())
                        .executeUpdate();
                System.out.println("delete :" + num);
                em.persist(model);
            }
        }
        return list.size();
    }
    
    // UserPropertyを取得する
    public List<UserPropertyModel> getUserProperties(String userId) {
        
        int pos = userId.indexOf(":");
        String fid = userId.substring(0, pos);
        String userIdAsLocal = userId.substring(pos + 1);
        
        final String sql = "from UserPropertyModel u where u.facilityId = :fid and (u.userId = :fid or u.userId = :uid)";
        
        List<UserPropertyModel> list = (List<UserPropertyModel>)
                em.createQuery(sql)
                .setParameter("fid", fid)
                .setParameter("uid", userIdAsLocal)
                .getResultList();

        return list;
    }
    
    // サーバーで利用する施設共通プロパティー(userId = facilityId)を取得
    public Map<String, String> getUserPropertyMap(String fid) {
        
        final String sql = "from UserPropertyModel u where u.userId = :fid";
        List<UserPropertyModel> list = (List<UserPropertyModel>)
                em.createQuery(sql)
                .setParameter("fid", fid)
                .getResultList();
        
        Map<String, String> propMap = new HashMap<String, String>();
        for (UserPropertyModel model : list) {
            propMap.put(model.getKey(), model.getValue());
        }
        return propMap;
    }
    
    // JMARI番号からfidを探す
    public String getFidFromJmari(String jmariCode) {

        String fid = IInfoModel.DEFAULT_FACILITY_OID;
        try {
            UserPropertyModel model = (UserPropertyModel) 
                    em.createQuery("from UserPropertyModel u where u.key = :key and u.value = :value")
                    .setParameter("key", "jmariCode")
                    .setParameter("value", jmariCode)
                    .getSingleResult();
            fid = model.getFacilityId();
        } catch (NoResultException ex) {
        } catch (NonUniqueResultException ex) {
        }
        return fid;
    }
    
    // サーバーでPVT server socketを開くかどうか
    public boolean usePvtServletServer() {
        
        long c = (Long) 
                em.createQuery("select count(*) from UserPropertyModel u where u.key = :key and u.value = :value")
                .setParameter("key", "pvtOnServer")
                .setParameter("value", String.valueOf(true))
                .getSingleResult();
        return c > 0;
    }

    // 入院モデルを取得する
    public List<AdmissionModel> getAdmissionList(String fid, String patientId) {

        final String sql =
                "from AdmissionModel a where a.patient.patientId = :ptId and a.patient.facilityId = :fid "
                + "order by a.id desc";

        // 既存の入院モデルを取得する
        List<AdmissionModel> list = (List<AdmissionModel>) 
                em.createQuery(sql)
                .setParameter("ptId", patientId)
                .setParameter("fid", fid)
                .getResultList();
        
        return list;
    }
    
    // 入院モデルを更新する
    public int updateAdmissionModels(List<AdmissionModel> list) {
        
        int cnt = 0;
        
        for (AdmissionModel model : list) {
            try {
                em.merge(model);
                cnt++;
            } catch (Exception ex) {
            }
        }
        return cnt;
    }
    
    // 入院モデルを削除する。使うな危険？
    public int deleteAdmissionModels(List<Long> ids) {
        
        int cnt = 0;
        
        for (long id : ids) {
            try {
                // 関連するDocumentModelを取得
                List<DocumentModel> docList = (List<DocumentModel>)
                        em.createQuery("from DocumentModel d where d.admission.id = :id")
                        .setParameter("id", id)
                        .getResultList();
                // それぞれのDocumentModelのAdmissionModelを設定解除する
                for (DocumentModel docModel : docList) {
                    docModel.getDocInfoModel().setAdmissionModel(null);
                }
                // AdmissionModelを削除する
                AdmissionModel exist = em.find(AdmissionModel.class, id);
                em.remove(exist);
                cnt++;
            } catch (Exception ex) {
            }
        }
        
        return cnt;
    }

    // 現時点で過去日になった仮保存カルテを取得する
    @SuppressWarnings("unchecked")
    public List<PatientModel> getTempDocumentPatients(String fid, Date fromDate) {

        final String sql = "from DocumentModel d where d.status='T' "
                + "and d.started <= :fromDate and d.karte.patient.facilityId = :fid";

        List<DocumentModel> documents =
                em.createQuery(sql)
                .setParameter("fid", fid)
                .setParameter("fromDate", fromDate)
                .getResultList();

        Set<PatientModel> set = new HashSet<PatientModel>();

        for (DocumentModel doc : documents) {
            PatientModel pm = doc.getKarteBean().getPatientModel();
            set.add(pm);
        }
        
        // 患者の健康保険を取得する。忘れがちｗ
        if (!set.isEmpty()) {
            for (PatientModel patient : set) {
                //setHealthInsurances(patient);
            }
        }

        return new ArrayList<PatientModel>(set);
    }
}
