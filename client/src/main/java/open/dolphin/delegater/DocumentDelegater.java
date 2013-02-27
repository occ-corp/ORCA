package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.ImageEntry;
import open.dolphin.dto.DocumentSearchSpec;
import open.dolphin.dto.ImageSearchSpec;
import open.dolphin.dto.ModuleSearchSpec;
import open.dolphin.infomodel.*;
import open.dolphin.util.BeanUtils;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

/**
 * Session と Document の送受信を行う Delegater クラス。
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 * @author modified by katoh, Hashimoto iin
 */
public class  DocumentDelegater extends BusinessDelegater {

    private static final String TITLE_LETTER = "紹介状:";
    private static final String TITLE_REPLY = "返書:";
    private static final String TITLE_CERTIFICATE = "診断書";
    
    private static final boolean debug = false;
    private static final DocumentDelegater instance;

    static {
        instance = new DocumentDelegater();
    }

    public static DocumentDelegater getInstance() {
        return instance;
    }

    private DocumentDelegater() {
    }
    
    /**
     * 患者のカルテを取得する。
     * @param patientPk 患者PK
     * @param fromDate 履歴の検索開始日
     * @return カルテ
     */
    public KarteBean getKarte(long patientPK, Date fromDate) {
        
        try {
            String path = "karte/" + String.valueOf(patientPK);
            MultivaluedMap<String, String> qmap= new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(fromDate));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);

            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            KarteBean karte = (KarteBean)
                    getConverter().fromJson(entityStr, KarteBean.class);

            return karte;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    /**
     * Documentを保存する。
     * @param karteModel KarteModel
     * @return Result Code
     */
    public long postDocument(DocumentModel karteModel) {
        
        try {
            // 確定日、適合開始日、記録日、ステータスを
            // DocInfo から DocumentModel(KarteEntry) に移す
            karteModel.toPersist();
            
            String json = getConverter().toJson(karteModel);

            String path = "karte/document";
            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            
            debug(status, entityStr);

            if (status != HTTP200) {
                return -1;
            }

            return Long.parseLong(entityStr);
            
        } catch (Exception ex) {
            return -1;
        }
    }
    
    /**
     * Documentを検索して返す。
     * @param id DocumentID
     * @return DocumentValue
     */
    public List<DocumentModel> getDocuments(List<Long> ids) {
        
        try {
            String path = "karte/document";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("ids", getConverter().fromList(ids));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            //String entityStr = (String) response.getEntity(String.class);
            byte[] bytes = (byte[]) response.getEntity(byte[].class);

            //debug(status, entityStr);
            if (status != HTTP200) {
                return null;
            }

            TypeReference typeRef = new TypeReference<List<DocumentModel>>(){};
            List<DocumentModel> list = (List<DocumentModel>)
                    getConverter().fromGzippedJson(bytes, typeRef);

            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 文書履歴を検索して返す。
     * @param spec DocumentSearchSpec 検索仕様
     * @return DocInfoModel の Collection
     */
    public List<DocInfoModel> getDocumentList(DocumentSearchSpec spec) {

        if (spec.getDocType().startsWith("karte")) {
            return getKarteList(spec);

        } else if (spec.getDocType().equals(IInfoModel.DOCTYPE_LETTER)) {
            return getLetterList(spec);

        }

        return null;
    }
    
//katoh^
    private List<DocInfoModel> getKarteList(DocumentSearchSpec spec) {
        
        try {
            String path = "karte/docinfo/" + String.valueOf(spec.getKarteId());
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(spec.getFromDate()));
            qmap.add("toDate", toRestFormat(spec.getToDate()));
            qmap.add("includeModified", String.valueOf(spec.isIncludeModifid()));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);

            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<DocInfoModel>>(){};
            List<DocInfoModel> list = (List<DocInfoModel>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
//katoh$
    
    private List<DocInfoModel> getLetterList(DocumentSearchSpec spec) {
        
        try {
            String path = "odletter/list/" + String.valueOf(spec.getKarteId());

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);

            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<LetterModule>>(){};
            List<LetterModule> list = (List<LetterModule>)
                    getConverter().fromJson(entityStr, typeRef);

            List<DocInfoModel> ret = new ArrayList<DocInfoModel>();
            
            if (list != null && !list.isEmpty()) {
                for (LetterModule module : list) {
                    DocInfoModel docInfo = new DocInfoModel();
                    docInfo.setDocPk(module.getId());
                    docInfo.setDocType(IInfoModel.DOCTYPE_LETTER);
                    docInfo.setDocId(String.valueOf(module.getId()));
                    docInfo.setConfirmDate(module.getConfirmed());
                    docInfo.setFirstConfirmDate(module.getConfirmed());
                    StringBuilder sb = new StringBuilder();
                    if (module.getTitle()!=null) {
                        sb.append(module.getTitle());
                    } else if(module.getLetterType().equals(IInfoModel.CONSULTANT)) {
                        sb.append(TITLE_REPLY).append(module.getClientHospital());
                    } else if (module.getLetterType().equals(IInfoModel.CLIENT)) {
                        sb.append(TITLE_LETTER).append(module.getConsultantHospital());
                    } else if (module.getLetterType().equals(IInfoModel.MEDICAL_CERTIFICATE)) {
                        sb.append(TITLE_CERTIFICATE);
                    }
                    docInfo.setTitle(sb.toString());
                    docInfo.setHandleClass(module.getHandleClass());

                    ret.add(docInfo);
                }
            } else {
                System.err.println("parse no results");
            }

            return ret;
        } catch (Exception ex) {
            return null;
        }
    }
    
  
    /**
     * ドキュメントを論理削除する。
     * @param pk 論理削除するドキュメントの prmary key
     * @return 削除件数
     */
    public int deleteDocument(long pk) {
        
        try {
            String path = "karte/document/" + String.valueOf(pk);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .delete(ClientResponse.class);

            int status = response.getStatus();

            return 1;
        } catch (Exception ex) {
            return -1;
        }
    }
    
    /**
     * 文書履歴のタイトルを変更する。
     * @param pk Document の pk
     * @return 変更した件数
     */
    public int updateTitle(DocInfoModel docInfo) {
        
        try {
            String path = "karte/document/" + String.valueOf(docInfo.getDocPk());

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .body(MEDIATYPE_TEXT_UTF8, docInfo.getTitle())
                    .put(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);

            debug(status, entityStr);

            return Integer.parseInt(entityStr);
            
        } catch (Exception ex) {
            return -1;
        }
    }
    
    
    /**
     * Moduleを検索して返す。
     * @param spec ModuleSearchSpec 検索仕様
     * @return Module の Collection
     */
    public List<List<ModuleModel>> getModuleList(ModuleSearchSpec spec) {
        
        try {
            String path = "karte/modules/" + String.valueOf(spec.getKarteId());
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();

            Date[] froms = spec.getFromDate();
            Date[] tos = spec.getToDate();
            
            int len = froms.length;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i != 0) {
                    sb.append(CAMMA);
                }
                sb.append(toRestFormat(froms[i]));
            }
            qmap.add("froms", sb.toString());
            
            len = tos.length;
            sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i != 0) {
                    sb.append(CAMMA);
                }
                sb.append(toRestFormat(tos[i]));
            }
            qmap.add("tos", sb.toString());
            qmap.add("entity", spec.getEntity());
            
            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);

            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            TypeReference typeRef = new TypeReference<List<List<ModuleModel>>>(){};
            List<List<ModuleModel>> ret = (List<List<ModuleModel>>) 
                    getConverter().fromJson(entityStr, typeRef);
            
            for (List<ModuleModel> list : ret) {
                for (ModuleModel module : list) {
                    module.setModel((InfoModel)BeanUtils.xmlDecode(module.getBeanBytes()));
                }
            }
            return ret;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    /**
     * イメージを取得する。
     * @param id 画像のId
     * @return SchemaModel
     */
    public SchemaModel getImage(long id) {
        
        try {
            String path = "karte/image/" + String.valueOf(id);
     
            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);

            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            SchemaModel model = (SchemaModel) 
                    getConverter().fromJson(entityStr, SchemaModel.class);
            
            byte[] bytes = model.getJpegByte();
            ImageIcon icon = new ImageIcon(bytes);
            if (icon != null) {
                model.setIcon(icon);
            }
            
            return model;
            
        } catch (Exception ex) {
            return null;
        }
    }


    /**
     * Imageを検索して返す。
     * @param spec ImageSearchSpec 検索仕様
     * @return Imageリストのリスト
     */
    public List<List<ImageEntry>> getImageList(ImageSearchSpec spec) {
/*
        String path = "karte/images/" + String.valueOf(spec.getKarteId());
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();

        Date[] froms = spec.getFromDate();
        Date[] tos = spec.getToDate();
        
        int len = froms.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i != 0) {
                sb.append(CAMMA);
            }
            sb.append(REST_DATE_FRMT.format(froms[i]));
        }
        qmap.add("froms", sb.toString());
        
        len = tos.length;
        sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i != 0) {
                sb.append(CAMMA);
            }
            sb.append(REST_DATE_FRMT.format(tos[i]));
        }
        qmap.add("tos", sb.toString());
        
        ClientResponse response = getQueryResource(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        TypeReference typeRef = new TypeReference<List<List<SchemaModel>>>(){};

        // 検索結果
        List<List<SchemaModel>> result = (List<List<SchemaModel>>) 
                getConverter().fromJsonTypeRef(entityStr, typeRef);

        List<List<ImageEntry>> ret = new ArrayList<List<ImageEntry>>();
        for (List<SchemaModel> periodList : result) {

            // ImageEntry 用のリスト
            List<ImageEntry> el = new ArrayList<ImageEntry>();
            // 抽出期間をイテレートする
            for (SchemaModel model : periodList) {
                // シェーマモデルをエントリに変換しリストに加える
                ImageEntry entry = ImageTool.getImageEntryFromSchema(model, spec.getIconSize());
                el.add(entry);
            }
            // リターンリストへ追加する
            ret.add(el);

        }

        return ret;
*/
        return null;
    }

    //---------------------------------------------------------------------------
    
    public List<Long> putDiagnosis(List<RegisteredDiagnosisModel> list) {
        
        try {
            String path = "karte/diagnosis/";

            String json = getConverter().toJson(list);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);

            debug(status, entityStr);
            List<Long> ret = getConverter().toLongList(entityStr);

            return ret;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    public int updateDiagnosis(List<RegisteredDiagnosisModel> list) {
        
        try {
            String path = "karte/diagnosis/";
            
            String json = getConverter().toJson(list);

            ClientResponse response = getClientRequest(path, null)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .put(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            return Integer.parseInt(entityStr);
            
        } catch (Exception ex) {
            return -1;
        }
    }
    
    public int removeDiagnosis(List<Long> ids) {
        
        try {
            String path = "karte/diagnosis/";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("ids", getConverter().fromList(ids));

            ClientResponse response = getClientRequest(path, qmap)
                    .delete(ClientResponse.class);

            int status = response.getStatus();
            debug(status, "delete response");

            return ids.size();
            
        } catch (Exception ex) {
            return -1;
        }
    }
    
    /**
     * Diagnosisを検索して返す。
     * @param spec DiagnosisSearchSpec 検索仕様
     * @return DiagnosisModel の Collection
     */
    public List<RegisteredDiagnosisModel> getDiagnosisList(long karteId, Date fromDate, boolean activeOnly) {
        
        try {
            String path = "karte/diagnosis/" + String.valueOf(karteId);
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(fromDate));
            qmap.add("activeOnly", String.valueOf(activeOnly));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<RegisteredDiagnosisModel>>(){};
            List<RegisteredDiagnosisModel> list = (List<RegisteredDiagnosisModel>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }

    
    public List<Long> addObservations(List<ObservationModel> observations) {
        
        try {
            String path = "/karte/observations";
            
            String json = getConverter().toJson(observations);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            List<Long> list = getConverter().toLongList(entityStr);

            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    public int removeObservations(List<Long> ids) {
        
        try {
            String path = "/karte/observations";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("ids", getConverter().fromList(ids));

            ClientResponse response = getClientRequest(path, qmap)
                    .delete(ClientResponse.class);

            int status = response.getStatus();
            debug(status, "delete response");

            return ids.size();
            
        } catch (Exception ex) {
            return -1;
        }
    }

    //-------------------------------------------------------------------------
    
    public int updatePatientMemo(PatientMemoModel pm) {
        
        try {
            String path = "/karte/memo";
            
            String json = getConverter().toJson(pm);

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .put(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            return Integer.parseInt(entityStr);
            
        } catch (Exception ex) {
            return -1;
        }
    }

    //-------------------------------------------------------------------------
    
    public List<List<AppointmentModel>> getAppoinmentList(ModuleSearchSpec spec) {
        
        try {
            String path = "karte/appo/" + String.valueOf(spec.getKarteId());

            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            Date[] froms = spec.getFromDate();
            Date[] tos = spec.getToDate();
            
            int len = froms.length;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i != 0) {
                    sb.append(CAMMA);
                }
                sb.append(toRestFormat(froms[i]));
            }
            qmap.add("froms", sb.toString());
            
            len = tos.length;
            sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i != 0) {
                    sb.append(CAMMA);
                }
                sb.append(toRestFormat(tos[i]));
            }
            qmap.add("tos", sb.toString());

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<List<AppointmentModel>>>(){};
            List<List<AppointmentModel>> ret = (List<List<AppointmentModel>>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return ret;
        } catch (Exception ex) {
            return null;
        }
    }


    public void updatePVTState(long pvtPK, int state) {
        
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("pvt/");
            sb.append(pvtPK);
            sb.append(CAMMA);
            sb.append(state);
            String path = sb.toString();

            ClientResponse response = getClientRequest(path, null)
                        .accept(MEDIATYPE_TEXT_UTF8)
                        .put(ClientResponse.class);

            int status = response.getStatus();
            debug(status, "put response");
        } catch (Exception ex) {
        }
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}