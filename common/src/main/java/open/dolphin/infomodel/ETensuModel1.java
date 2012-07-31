package open.dolphin.infomodel;

import java.io.Serializable;
import javax.persistence.*;


/**
 * 補助マスターテーブルモデル
 *
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "tbl_etensu_1")
public class ETensuModel1 implements Serializable {

    //他の診療行為を包括するか否かを表す。包括する期間を表す。
    public static final int H_UNIT_NO_RELATION = 0;     //００：関連なし
    public static final int H_UNIT_PER_DAY = 1;         //０１：１日につき
    public static final int H_UNIT_SAME_MONTH = 2;      //０２：同一月内
    public static final int H_UNIT_SAME_TIME = 3;       //０３：同時
    public static final int H_UNIT_PER_WEEK = 4;        //０４：１週間につき
    public static final int H_UNIT_PRE_OPE_1WEEK = 5;   //０５：手術前１週間
    public static final int H_UNIT_PER_OPE = 6;         //０６：１手術につき
    // 関連の有無
    public static final int NOT_RELATED = 0;
    public static final int RELATED = 1;
    
    // TBL_ETENSU_1
    private String srycd;       // 診療行為コード
    private String yukostymd;   // 有効開始日
    private String yukoedymd;   // 有効終了日
    private int h_tani1;        // 包括被包括関連（包括単位１）
    private String h_group1;    // 包括被包括関連（グループ番号１）
    private int h_tani2;        // 包括被包括関連（包括単位２）
    private String h_group2;    // 包括被包括関連（グループ番号２）
    private int h_tani3;        // 包括被包括関連（包括単位３）
    private String h_group3;    // 包括被包括関連（グループ番号３）
    private int r_day;          // 背反関連識別（１日につき） =>3-1 table
    private int r_month;        // 背反関連識別（同一月内）=>3-2 table
    private int r_same;         // 背反関連識別（同時） =>3-3 table
    private int r_week;         // 背反関連識別（１週間につき） =>3-4 table
    private int n_group;        // 入院基本料識別 => 4 table
    private int c_kaisu;        // 算定回数関連 =>5 table
    private String chgymd;      // 変更年月日
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    // constructor
    public ETensuModel1() {
    }

    // getter
    public long getId() {
        return id;
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

    public int getH_tani1() {
        return h_tani1;
    }

    public String getH_group1() {
        return h_group1;
    }

    public int getH_tani2() {
        return h_tani2;
    }

    public String getH_group2() {
        return h_group2;
    }

    public int getH_tani3() {
        return h_tani3;
    }

    public String getH_group3() {
        return h_group3;
    }

    public int getR_day() {
        return r_day;
    }

    public int getR_month() {
        return r_month;
    }

    public int getR_same() {
        return r_same;
    }

    public int getR_week() {
        return r_week;
    }

    public int getN_group() {
        return n_group;
    }

    public int getC_kaisu() {
        return c_kaisu;
    }

    public String getChgymd() {
        return chgymd;
    }
    
    // setter
    public void setId(long id) {
        this.id = id;
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

    public void setH_tani1(int h_tani1) {
        this.h_tani1 = h_tani1;
    }

    public void setH_group1(String h_group1) {
        this.h_group1 = h_group1;
    }

    public void setH_tani2(int h_tani2) {
        this.h_tani2 = h_tani2;
    }

    public void setH_group2(String h_group2) {
        this.h_group2 = h_group2;
    }

    public void setH_tani3(int h_tani3) {
        this.h_tani3 = h_tani3;
    }

    public void setH_group3(String h_group3) {
        this.h_group3 = h_group3;
    }

    public void setR_day(int r_day) {
        this.r_day = r_day;
    }

    public void setR_month(int r_month) {
        this.r_month = r_month;
    }

    public void setR_same(int r_same) {
        this.r_same = r_same;
    }

    public void setR_week(int r_week) {
        this.r_week = r_week;
    }

    public void setN_group(int n_group) {
        this.n_group = n_group;
    }

    public void setC_kaisu(int c_kaisu) {
        this.c_kaisu = c_kaisu;
    }

    public void setChgymd(String chgymd) {
        this.chgymd = chgymd;
    }
}
