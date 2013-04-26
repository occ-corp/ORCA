package open.dolphin.infomodel;

/**
 * 薬剤相互作用のモデル
 *
 * @author masuda, Masuda Naika
 */
public class DrugInteractionModel {

    private String srycd1;
    private String srycd2;
    private String sskijo;
    private String syojyoucd;
    private String brandName1;   // 対応先発品名
    private String brandName2;

    public DrugInteractionModel(String srycd1, String srycd2, String sskijo, 
            String syojyoucd, String brandName1, String brandName2){
        this.srycd1 = srycd1;
        this.srycd2 = srycd2;
        this.sskijo = sskijo;
        this.syojyoucd = syojyoucd;
        this.brandName1 = brandName1;
        this.brandName2 = brandName2;
    }

    public String getSrycd1(){
        return srycd1;
    }
    public String getSrycd2(){
        return srycd2;
    }
    public String getSskijo(){
        return sskijo;
    }
    public String getSyojyoucd(){
        return syojyoucd;
    }
    public String getBrandname1() {
        return brandName1;
    }
    public String getBrandname2() {
        return brandName2;
    }
}
