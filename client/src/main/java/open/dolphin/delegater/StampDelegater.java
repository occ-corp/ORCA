package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.infomodel.*;

/**
 * Stamp関連の Delegater クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class StampDelegater extends BusinessDelegater {

    private static final String UTF8 = "UTF-8";
    private static final String RES_STAMP_TREE = "stampTree/";
    private static final String RES_STAMP = "stamp/";
    
    private static final boolean debug = false;
    private static final StampDelegater instance;
    
    private Map<String, StampModel> stampCache;

    static {
        instance = new StampDelegater();
    }

    public static StampDelegater getInstance() {
        return instance;
    }

    private StampDelegater() {
        stampCache = new HashMap<String, StampModel>();
    }
    
    /**
     * StampTree を保存/更新する。
     * @param model 保存する StampTree
     * @return 保存個数
     */
    public long putTree(IStampTreeModel model) {
        try {
            model.setTreeBytes(model.getTreeXml().getBytes(UTF8)); // UTF-8 bytes
            //model.setTreeXml(null); DO NOT DO THIS!
        } catch (UnsupportedEncodingException ex) {
            logger.warn(ex.getMessage());
        }
        
        // こっちでキャストしておく
        StampTreeModel treeModel = (StampTreeModel) model;

        String json = getConverter().toJson(treeModel);

        // resource post
        String path = RES_STAMP_TREE;
        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)    
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        long pk = Long.parseLong(entityStr);
        return pk;
    }

    public List<IStampTreeModel> getTrees(long userPK) {
        
        String path = RES_STAMP_TREE + String.valueOf(userPK);
        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);
            
        if (status != HTTP200) {
            return null;
        }

        UserStampTreeModel ret = (UserStampTreeModel) getConverter()
                .fromJson(entityStr, UserStampTreeModel.class);
        
        List<IStampTreeModel> treeList = new ArrayList<IStampTreeModel>();
        List<IStampTreeModel> list = ret.getTreeList();
        
        for (IStampTreeModel model : list) {
            try {
                String treeXml = new String(model.getTreeBytes(), UTF8);
                model.setTreeXml(treeXml);
                model.setTreeBytes(null);
                treeList.add(model);
            } catch (UnsupportedEncodingException ex) {
                logger.warn(ex.getMessage());
            }
        }

        return treeList;
    }
    
    /**
     * 個人用のStampTreeを保存し公開する。
     * @param model 個人用のStampTreeで公開するもの
     * @return id
     */
    public long saveAndPublishTree(StampTreeModel model, byte[] publishBytes) {
        
        try {
            model.setTreeBytes(model.getTreeXml().getBytes(UTF8));
        } catch (UnsupportedEncodingException ex) {
            logger.warn(ex.getMessage());
        }

        PublishedTreeModel publishedModel = createPublishedTreeModel(model, publishBytes);
        
        // interfaceはmarshallingできない
        UserStampTreeModel treeModel = new UserStampTreeModel();
        treeModel.setStampTreeList(Collections.singletonList(model));
        treeModel.setPublishedList(Collections.singletonList(publishedModel));

        String json = getConverter().toJson(treeModel);

        String path = RES_STAMP_TREE + "published";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .post(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        
        debug(status, entityStr);
        
        return Long.valueOf(entityStr);
    }
    
    /**
     * 既に保存されている個人用のTreeを公開する。
     * @param model 既に保存されている個人用のTreeで公開するもの
     * @return 公開数
     */
    public int publishTree(StampTreeModel model, byte[] publishBytes) {
        
        // updatePublishedTreeといっしょ
        return updatePublishedTree(model, publishBytes);

    }
    
    /**
     * 公開されているTreeを更新する。
     * @param model 更新するTree
     * @return 更新数
     */
    public int updatePublishedTree(StampTreeModel model, byte[] publishBytes) {

      try {
            model.setTreeBytes(model.getTreeXml().getBytes(UTF8));
        } catch (UnsupportedEncodingException ex) {
            logger.warn(ex.getMessage());
        }

        PublishedTreeModel publishedModel = createPublishedTreeModel(model, publishBytes);
        
        // interfaceはunmarshallingできない
        UserStampTreeModel treeModel = new UserStampTreeModel();
        treeModel.setStampTreeList(Collections.singletonList(model));
        treeModel.setPublishedList(Collections.singletonList(publishedModel));

        String json = getConverter().toJson(treeModel);

        String path = RES_STAMP_TREE + "published";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        
        debug(status, entityStr);
        
        return Integer.valueOf(entityStr);
    }
    
    /**
     * 公開されているTreeを削除する。
     * @param id 削除するTreeのID
     * @return 削除数
     */
    public int cancelPublishedTree(StampTreeModel model) {
        
        try {
            model.setTreeBytes(model.getTreeXml().getBytes(UTF8));
        } catch (UnsupportedEncodingException ex) {
            logger.warn(ex.getMessage());
        }
        
        String json = getConverter().toJson(model);

        String path = RES_STAMP_TREE + "published/cancel/";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();

        debug(status, "put response");

        return 1;
    }
    
    public List<PublishedTreeModel> getPublishedTrees() {

        String path = RES_STAMP_TREE + "published";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        TypeReference typeRef = new TypeReference<List<PublishedTreeModel>>(){};
        List<PublishedTreeModel> ret = (List<PublishedTreeModel>)
                getConverter().fromJson(entityStr, typeRef);

        return ret;
    }

    // 個人用StampTreeから公開用StampTreeを生成する。
    // byte[] publishBytes は公開されるカテゴリのみを含むサブセットバイト
    private PublishedTreeModel createPublishedTreeModel(StampTreeModel model, byte[] publishBytes) {
        PublishedTreeModel publishedModel = new PublishedTreeModel();
        publishedModel.setId(model.getId());                            // pk
        publishedModel.setUserModel(model.getUserModel());              // UserModel
        publishedModel.setName(model.getName());                        // 名称
        publishedModel.setPublishType(model.getPublishType());          // 公開タイプ
        publishedModel.setCategory(model.getCategory());                // カテゴリ
        publishedModel.setPartyName(model.getPartyName());              // パーティー名
        publishedModel.setUrl(model.getUrl());                          // URL
        publishedModel.setDescription(model.getDescription());          // 説明
        publishedModel.setPublishedDate(model.getPublishedDate());      // 公開日
        publishedModel.setLastUpdated(model.getLastUpdated());          // 更新日
        publishedModel.setTreeBytes(publishBytes);                      // XML bytes
        return publishedModel;
    }


    //---------------------------------------------------------------------------

    public List<Long> subscribeTrees(List<SubscribedTreeModel> subscribeList) {
        
        String json = getConverter().toJson(subscribeList);

        String path = RES_STAMP_TREE + "subscribed";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)    
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        String[] pks = entityStr.split(",");
        List<Long> ret = new ArrayList<Long>(pks.length);
        for (String str : pks) {
            ret.add(Long.valueOf(str));
        }
        return ret;
    }
    
    
    public int unsubscribeTrees(List<SubscribedTreeModel> removeList) {

        String path = RES_STAMP_TREE +"subscribed";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (SubscribedTreeModel s : removeList) {
            if (!first) {
                sb.append(CAMMA);
            } else {
                first = false;
            }
            sb.append(String.valueOf(s.getTreeId()));
            sb.append(CAMMA);
            sb.append(String.valueOf(s.getUserModel().getId()));
        }

        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("ids", sb.toString());

        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_TEXT_UTF8)
                .delete(ClientResponse.class);

        int status = response.getStatus();

        debug(status, "delete response");

        return 1;
    }
    

    //---------------------------------------------------------------------------

    /**
     * Stampを保存する。
     * @param model StampModel
     * @return 保存件数
     */
    public List<String> putStamp(List<StampModel> list) {
        
        // キャッシュに登録する
        for (StampModel model : list) {
            stampCache.put(model.getId(), model);
        }
        
        String json = getConverter().toJson(list);
        String path = RES_STAMP + "list";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        String[] params = entityStr.split(",");
        List<String> ret = Arrays.asList(params);

        return ret;
    }
    
    /**
     * Stampを保存する。
     * @param model StampModel
     * @return 保存件数
     */
    public String putStamp(StampModel model) {
        
        // キャッシュに登録する
        stampCache.put(model.getId(), model);

        String json = getConverter().toJson(model);
        String path = RES_STAMP + "id";

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)    
                .type(MEDIATYPE_JSON_UTF8)
                .put(ClientResponse.class, json);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);
        
        debug(status, entityStr);

        return entityStr;
    }

    /**
     * Stampを置き換える。
     * @param model
     * @return
     * @throws Exception
     */
    public String replaceStamp(StampModel model) {
        
        // キャッシュを更新する
        stampCache.put(model.getId(), model);
        
        return putStamp(model);
    }
    
    /**
     * Stampを取得する。
     * @param stampId 取得する StampModel の id
     * @return StampModel
     */
    public StampModel getStamp(String stampId) {
        
        // StampModelのキャッシュを参照する
        StampModel ret = stampCache.get(stampId);
        if (ret != null) {
            return ret;
        }
        
        String path = RES_STAMP + "id/" +  stampId;

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
        ret = (StampModel)
                getConverter().fromJson(entityStr, StampModel.class);
        
        // キャッシュに登録する
        stampCache.put(stampId, ret);

        return ret;
    }
    
    /**
     * Stampを取得する。
     * @param stampId 取得する StampModel の id
     * @return StampModel
     */
    public List<StampModel> getStamps(List<ModuleInfoBean> list) {
        
        // キャッシュにあるか調べる
        List<ModuleInfoBean> infosToGet = new ArrayList<ModuleInfoBean>();
        for (ModuleInfoBean info : list) {
            if (!stampCache.containsKey(info.getStampId())) {
                infosToGet.add(info);
            }
        }

        String path = RES_STAMP + "list";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ModuleInfoBean info : infosToGet) {
            if (!first) {
                sb.append(CAMMA);
            } else {
                first = false;
            }
            sb.append(info.getStampId());
        }
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("ids", sb.toString());

        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }
        
       TypeReference typeRef = new TypeReference<List<StampModel>>(){};
       List<StampModel> smList = (List<StampModel>)
                getConverter().fromJson(entityStr, typeRef);
        
        // キャッシュに登録する
        for (StampModel sm : smList) {
            stampCache.put(sm.getId(), sm);
        }
        
        // キャッシュを参照してStampModel Listを返す
        List<StampModel> ret = new ArrayList<StampModel>();
        for (ModuleInfoBean info : list) {
            ret.add(stampCache.get(info.getStampId()));
        }
        
        return ret;
    }
    
    /**
     * Stampを削除する。
     * @param stampId 削除する StampModel の id
     * @return 削除件数
     */
    public int removeStamp(String stampId) {
        
        // キャッシュから削除する
        stampCache.remove(stampId);
        
        String path = RES_STAMP + "id/" + stampId;

        ClientResponse response = getResource(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .delete(ClientResponse.class);

        int status = response.getStatus();
        debug(status, "delete response");
 
        return 1;
    }
    
    /**
     * Stampを削除する。
     * @param stampId 削除する StampModel の id
     * @return 削除件数
     */
    public int removeStamps(List<String> ids) {
        
        // キャッシュから削除する
        for (String stampId : ids) {
            stampCache.remove(stampId);
        }
        
        String path = RES_STAMP + "list";
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
        qmap.add("ids", getConverter().fromList(ids));
        
        ClientResponse response = getResource(path, qmap)
                .accept(MEDIATYPE_TEXT_UTF8)
                .delete(ClientResponse.class);

        int status = response.getStatus();
        debug(status, "delete response");

        return ids.size();
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
