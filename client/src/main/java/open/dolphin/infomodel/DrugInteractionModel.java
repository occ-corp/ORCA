package open.dolphin.infomodel;

/**
 * 薬剤相互作用のモデル
 *
 * @author masuda, Masuda Naika
 */
public class DrugInteractionModel {

    String srycd1;
    String srycd2;
    String sskijo;
    String syojyoucd;


    public DrugInteractionModel(String srycd1, String srycd2, String sskijo, String syojyoucd){
        this.srycd1 = srycd1;
        this.srycd2 = srycd2;
        this.sskijo = sskijo;
        this.syojyoucd = syojyoucd;
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

}
