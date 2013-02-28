
package open.dolphin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.infomodel.UsingDrugModel;

/**
 * 採用薬を扱うクラス
 *
 * @author masuda, Masuda Naika
 */
public class UsingDrugs {

    private static final UsingDrugs instance;
    private static final ConcurrentHashMap<Integer, UsingDrugModel> usingDrugMap;     // 採用薬のHashMap
    private static final MasudaDelegater del;
    private static boolean changed;

    static {
        instance = new UsingDrugs();
        usingDrugMap = new ConcurrentHashMap<Integer, UsingDrugModel>();
        del = MasudaDelegater.getInstance();
        changed = true;
        instance.loadUsingDrugs();
    }

    public static UsingDrugs getInstance() {
        return instance;
    }

    public void loadUsingDrugs() {
        
        if (!changed) {
            return;
        }

        usingDrugMap.clear();

        // データベースから採用薬を取得
        try {
            List<UsingDrugModel> list = del.getUsingDrugModels();
            for (UsingDrugModel model : list) {
                usingDrugMap.put(model.getSrycd(), model);
            }

            changed = false;
        } catch (Exception ex) {
        }
    }

    // UsingDrugPanelから使用
    public List<UsingDrugModel> getUsingDrugModelList() {
        return new ArrayList<UsingDrugModel>(usingDrugMap.values());
    }

    // RpEditorで使用
    public UsingDrugModel getUsingDrugModel(String srycd){
        return usingDrugMap.get(Integer.valueOf(srycd));
    }

    // BaseEditor, RpEditor, CheckMedicationで使用
    public boolean isInUse(String srycd) {
        return usingDrugMap.containsKey(Integer.valueOf(srycd));
    }

    public void addUsingDrugs(List<UsingDrugModel> list) {
        
        for (UsingDrugModel model : list) {
            if (model != null) {
                try {
                    del.addUsingDrugModel(model);
                    usingDrugMap.put(model.getSrycd(), model);
                    changed = true;
                } catch (Exception ex) {
                }
            }
        }
     }

    public void removeUsingDrugs(List<UsingDrugModel> list) {
        
        for (UsingDrugModel model : list) {
            if (model != null) {
                try {
                    del.removeUsingDrugModel(model);
                    usingDrugMap.remove(model.getSrycd());
                    changed = true;
                } catch (Exception ex) {
                }
            }
        }
    }
            

    public void updateUsingDrugs(List<UsingDrugModel> list) {
        
        for (UsingDrugModel model : list) {
            if (model != null) {
                try {
                    del.updateUsingDrugModel(model);
                    changed = true;
                } catch (Exception ex) {
                }
            }
        }
    }
}
