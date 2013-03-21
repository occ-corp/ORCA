package open.dolphin.toucha.model;

/**
 * DocumentModelS
 * @author masuda, Masuda Naika
 */
public class DocumentModelS {
    
    private long docPk;
    private String html;
    
    public DocumentModelS() {
    }
    
    public void setDocPk(long docPk) {
        this.docPk = docPk;
    }
    public void setHtml(String html) {
        this.html = html;
    }
    public long getDocPk() {
        return docPk;
    }
    public String getHtml() {
        return html;
    }
}
