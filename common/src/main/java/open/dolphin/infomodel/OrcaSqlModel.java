package open.dolphin.infomodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * サーバー経由ORCA SQLモデル
 * @author masuda, Masuda Naika
 */
public class OrcaSqlModel implements Serializable {
    
    private String url;
    private String sql;
    private boolean ps;
    private List<Integer> typeList;
    private List<String> paramList;
    private List<List<String>> valuesList;
    private String errorMsg;
    
    public OrcaSqlModel() {
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    public void setSql(String sql) {
        this.sql = sql;
    }
    public void setPreparedStatement(boolean b) {
        ps = b;
    }
    public void setValuesList(List<List<String>> list) {
        valuesList = list;
    }
    public void setTypeList(List<Integer> typeList) {
        this.typeList = typeList;
    }
    public void setParamList(List<String> paramList) {
        this.paramList = paramList;
    }
    public void setErrorMessage(String msg) {
        errorMsg = msg;
    }
    
    public String getUrl() {
        return url;
    }
    public String getSql() {
        return sql;
    }
    public boolean isPreparedStatement() {
        return ps;
    }
    public List<List<String>> getValuesList() {
        return valuesList;
    }
    public List<Integer> getTypeList() {
        return typeList;
    }
    public List<String> getParamList() {
        return paramList;
    }
    public String getErrorMessage() {
        return errorMsg;
    }
    
    public void addParameter(Integer dataType, String param) {
        if (typeList == null) {
            typeList = new ArrayList<Integer>();
        }
        if (paramList == null) {
            paramList = new ArrayList<String>();
        }
        typeList.add(dataType);
        paramList.add(param);
    }
}
