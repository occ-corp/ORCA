
package open.dolphin.infomodel;

/**
 * 背反関連テーブルモデル
 * 
 * @author masuda, Mauda Naika
 */
public class ETensuModel3 {
    
    public static final int R_DAY   = 1;
    public static final int R_MONTH = 2;
    public static final int R_SAME  = 3;
    public static final int R_WEEK  = 4;
    
    public static final int HAIHAN_1 = 1;   //１：診療行為コード１を算定する。
    public static final int HAIHAN_2 = 2;   //２：診療行為コード２を算定する。
    public static final int HAIHAN_3 = 3;   //３：何れか一方を算定する。
    
    //背反条件に特別な条件があるか否かを表す。
    public static final int NOT_CONDITIONAL = 0;    //０；条件なし
    public static final int CONDITIONAL = 1;        //１：条件あり
    
    private String srycd1;      // 診療行為コード１
    private String srycd2;      // 診療行為コード２
    private String yukostymd;   // 有効開始日
    private String yukoedymd;   // 有効終了日
    private int haihan;         // 背反区分
    private int tokurei;        // 特定条件
    private String chgymd;      // 変更年月日
    
    // ETensuModel1の背反識別
    private int r_haihan;
    
    // constructor
    public ETensuModel3() {
    }
    
    // getter
    public String getSrycd1() {
        return srycd1;
    }

    public String getSrycd2() {
        return srycd2;
    }
    
    public String getYukostymd() {
        return yukostymd;
    }

    public String getYukoedymd() {
        return yukoedymd;
    }

    public int getHaihan() {
        return haihan;
    }

    public int getTokurei() {
        return tokurei;
    }

    public String getChgymd() {
        return chgymd;
    }
    
    public int getR_haihan() {
        return r_haihan;
    }

    // setter
    public void setSrycd1(String srycd1) {
        this.srycd1 = srycd1;
    }

    public void setSrycd2(String srycd2) {
        this.srycd2 = srycd2;
    }

    public void setYukostymd(String yukostymd) {
        this.yukostymd = yukostymd;
    }

    public void setYukoedymd(String yukoedymd) {
        this.yukoedymd = yukoedymd;
    }

    public void setHaihan(int haihan) {
        this.haihan = haihan;
    }

    public void setTokurei(int tokurei) {
        this.tokurei = tokurei;
    }

    public void setChgymd(String chgymd) {
        this.chgymd = chgymd;
    }
    
    public void setR_haihan(int r_haihan) {
        this.r_haihan = r_haihan;
    }

}
