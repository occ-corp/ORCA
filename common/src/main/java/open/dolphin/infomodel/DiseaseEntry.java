package open.dolphin.infomodel;

import open.dolphin.infomodel.ModelUtils;

/**
 * DiseaseEntry
 * 
 * @author  Minagawa, Kazushi
 */
public final class DiseaseEntry extends MasterEntry {
	
    private String icdTen;

    /** Creates a new instance of DeseaseEntry */
    public DiseaseEntry() {
    }

    public String getIcdTen() {
        return icdTen;
    }

    public void setIcdTen(String val) {
        icdTen = val;
    }
    
    @Override
    public boolean isInUse() {
        if (disUseDate != null) {
            return refDate.compareTo(disUseDate) <= 0;
        }
        return false;
    }
    
//masuda^   特定疾患関連
    private int byoKanrenKbn;
    public void setByoKanrenKbn(int i){
        byoKanrenKbn = i;
    }
    public int getByoKanrenKbn(){
        return byoKanrenKbn;
    }
    public String getByoKanrenKbnStr() {
        return ModelUtils.getByoKanrenKbnStr(byoKanrenKbn);
    }
//masuda$
}
