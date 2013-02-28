package open.dolphin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.infomodel.DisconItemModel;

/**
 * 中止項目を扱うクラス
 *
 * @author masuda, Masuda Naika
 */
public final class DisconItems {

    private static final DisconItems instance;
    private static final CopyOnWriteArrayList<DisconItemModel> discontinuedItems;   // 中止項目のリスト
    private static final MasudaDelegater del;
    private static boolean changed;

    static {
        instance = new DisconItems();
        discontinuedItems = new CopyOnWriteArrayList<DisconItemModel>();
        del = MasudaDelegater.getInstance();
        changed = true;
        instance.loadDisconItems();
    }

    public static DisconItems getInstance() {
        return instance;
    }

    public void loadDisconItems(){
        
        if (!changed) {
            return;
        }
        
        discontinuedItems.clear();

        // データベースから中止項目を調べる
        try {
            List<DisconItemModel> list = del.getDisconItemModels();
            for (DisconItemModel model : list) {
                discontinuedItems.add(model);
            }
            changed = false;
        } catch (Exception ex) {
        }
    }

    public boolean isDiscon(String str){
        // 中止項目かどうかを調べる
        boolean b = false;
        for (DisconItemModel model : discontinuedItems){
            if (str.contains(model.getItemName())){
                b = true;
                break;
            }
        }
        return b;
    }

    // DisconItemPanelで使用
    public List<DisconItemModel> getDisconItemList() {
        return new ArrayList<DisconItemModel>(discontinuedItems);
    }

    public void addDisconItems(List<DisconItemModel> list) {

        for (DisconItemModel model : list) {
            if (model != null) {
                try {
                    del.addDisconItemModel(model);
                    discontinuedItems.add(model);
                    changed = true;
                } catch (Exception ex) {
                }
            }
        }
    }

    public void removeDisconItems(List<DisconItemModel> list) {
        
        for (DisconItemModel model : list) {
            if (model != null) {
                try {
                    del.removeDisconItemModel(model);
                    discontinuedItems.remove(model);
                    changed = true;
                } catch (Exception ex) {
                }
            }
        }
    }

    public void updateDisconItems(List<DisconItemModel> list) {
        
        for (DisconItemModel model : list) {
            if (model != null) {
                try {
                    del.updateDisconItemModel(model);
                    changed = true;
                } catch (Exception ex) {
                }
            }
        }
    }
}
