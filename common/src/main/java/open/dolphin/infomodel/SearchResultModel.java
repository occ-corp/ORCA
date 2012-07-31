package open.dolphin.infomodel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * 全文検索結果を返すモデル
 *
 * @author masuda, Masuda Naika
 */
public class SearchResultModel {

    private long totalCount;
    private long docPk;

    @JsonDeserialize(contentAs=PatientModel.class)
    private List<PatientModel> list;

    public SearchResultModel() {
    }

    public SearchResultModel(long docPk, long totalCount, List<PatientModel> list) {
        this.docPk = docPk;
        this.totalCount = totalCount;
        this.list = list;
    }

    public void setDocPk(long docPk) {
        this.docPk = docPk;
    }

    public long getDocPk() {
        return docPk;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setResultList(List<PatientModel> list) {
        this.list = list;
    }

    public List<PatientModel> getResultList() {
        return list;
    }
}
