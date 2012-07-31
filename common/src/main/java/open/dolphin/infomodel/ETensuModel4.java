
package open.dolphin.infomodel;


/**
 * 入院基本料テーブルモデル
 * 
 * @author masuda, Masuda Naida
 */
public class ETensuModel4 {

    private String n_group;     // 加算グループごとに設定した番号
    private String srycd;       // 診療行為コード
    private String yukostymd;   // 有効開始日
    private String yukoedymd;   // 有効終了日
    private int kasan;          // 加算識別
    private String chgymd;      // 変更年月日
    
    // constructor
    public ETensuModel4() {
    }
    
    // getter
    public String getN_group() {
        return n_group;
    }
    
    public String getSrycd() {
        return srycd;
    }

    public String getYukostymd() {
        return yukostymd;
    }

    public String getYukoedymd() {
        return yukoedymd;
    }

    public int getKasan() {
        return kasan;
    }

    public String getChgymd() {
        return chgymd;
    }

    // setter
    public void setN_group(String n_group) {
        this.n_group = n_group;
    }

    public void setSrycd(String srycd) {
        this.srycd = srycd;
    }

    public void setYukostymd(String yukostymd) {
        this.yukostymd = yukostymd;
    }

    public void setYukoedymd(String yukoedymd) {
        this.yukoedymd = yukoedymd;
    }

    public void setKasan(int kasan) {
        this.kasan = kasan;
    }

    public void setChgymd(String chgymd) {
        this.chgymd = chgymd;
    }    
}
