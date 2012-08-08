package open.dolphin.order;

import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import open.dolphin.client.ClientContext;
import open.dolphin.client.Dolphin;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.infomodel.*;
import open.dolphin.order.LaboTestPanelView.LaboCheckBox;
import open.dolphin.stampbox.StampBoxPlugin;
import open.dolphin.util.BeanUtils;
import open.dolphin.util.MMLDate;

/**
 * 一覧表から検査を入力するためのパネル
 * BaseEditor.javaから呼ばれる
 *
 * @author masuda, Masuda Naika, Re programmed on 2011/07/30
 */
public class LaboTestPanel {

    private List<MasterItem> masterItemList;
    // パネルにない検査項目
    private List<MasterItem> otherItems;
    // 検体検査スタンプの情報
    private HashMap<String, ModuleInfoBean> stampMap;

    private JDialog dialog;
    private BaseEditor editor;
    private LaboTestPanelView view;
    private List<LaboCheckBox> checkBoxList;
    private boolean isModified = false;

    // 点数マスタの静的マップ、一度取得したものは再利用する
    private static HashMap<Integer, TensuMaster> tensuMasterMap;

    private static final DecimalFormat srycdFrmt = new DecimalFormat("000000000");
    private static final String FROM_EDITOR_STAMP_NAME = "エディタから";
    private static final String DEFAULT_COMBO_ITEM = "-----";
    private static final int SRYCD_GAIRAI_RAPID = 160177770;

    public LaboTestPanel(BaseEditor editor) {

        this.editor = editor;
        otherItems = new ArrayList<MasterItem>();
        masterItemList = new ArrayList<MasterItem>();
        stampMap = new HashMap<String, ModuleInfoBean>();
        view = new LaboTestPanelView();
        checkBoxList = view.getCheckBoxList();
        configureButtonAction();
        prepareCmbStamp();
    }

    public List<MasterItem> getMasterItemList() {
        return new ArrayList<MasterItem>(masterItemList);
    }

    public void setMasterItemList(List<MasterItem> list) {
        masterItemList = new ArrayList<MasterItem>(list);
    }

    public boolean isModified() {
        return isModified;
    }

    public void enter() {

        if (tensuMasterMap != null) {
            // すでにTensuMaster取得済みのとき
            prepareCheckBox();
        } else {
            // はじめてのとき
            prepareMasterItemMap();
        }

        isModified = false;
        checkupLaboCheckBox();
        showDialog();
    }

    private void showDialog() {

        // dialogを作成
        dialog = new JDialog((Frame) null, true);
        ClientContext.setDolphinIcon(dialog);
        dialog.setContentPane(view);
        dialog.pack();

        // dialogのタイトル・サイズなどを設定
        String title = ClientContext.getFrameTitle("検査エディタ") + ", Masuda Naika";
        dialog.setTitle(title);
        ComponentMemory cm = new ComponentMemory(dialog, new Point(100, 100), view.getPreferredSize(), LaboTestPanel.this);
        //dialog.setSize(dialog.getPreferredSize());
        //dialog.setResizable(false);
        cm.setToPreferenceBounds();

        // 展開ボタンにフォーカス
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                view.getBtnTenkai().requestFocusInWindow();
            }
        });

        dialog.setVisible(true);
    }

    private void exit() {
        isModified = true;
        collectSelectedLaboTest();
        closePanel();
    }

    private void closePanel() {
        dialog.setVisible(false);
        dialog.dispose();
    }

    private void checkupLaboCheckBox() {

        // setTableに登録されている項目を調べてCheckBoxをセットする
        otherItems.clear();
        clearAllCheckBox();

        for (MasterItem mItem : masterItemList) {
            int code = Integer.valueOf(mItem.getCode());
            boolean found = false;
            for (LaboCheckBox cb : checkBoxList) {
                if (cb.getSrycd() == code) {
                    cb.setSelected(true);
                    found = true;
                    break;
                }
            }
            if (!found) {
                otherItems.add(mItem);
            }
        }
        view.getLblOtherItem().setVisible(!otherItems.isEmpty());
    }

    private void clearAllCheckBox() {
        // チェックボックスをすべてクリア
        for (LaboCheckBox cb : checkBoxList) {
            cb.setSelected(false);
        }
    }

    private void collectSelectedLaboTest() {

        // チェックされているCheckBoxを調べてselectedLaboTestを作成
        masterItemList.clear();
        int cnt = 0;
        MasterItem rapid = null;
        
        for (LaboCheckBox cb : checkBoxList) {
            if (cb.isSelected()) {
                TensuMaster tm = cb.getTensuMaster();
                if (tm != null) {
                    MasterItem mItem = new MasterItem();
                    mItem.setName(tm.getName());
                    mItem.setCode(tm.getSrycd());
                    mItem.setClassCode(ClaimConst.SYUGI);   // 11/08/04追加
                    mItem.setClaimClassCode(tm.getSrysyukbn());
                    mItem.setUnit(tm.getTaniname());
                    if (Integer.valueOf(tm.getSrycd()) == SRYCD_GAIRAI_RAPID) {
                        rapid = mItem;
                    } else {
                        masterItemList.add(mItem);
                        cnt++;
                    }
                }
            }
        }
        // パネルにない項目を戻す
        if (!otherItems.isEmpty()) {
            masterItemList.addAll(otherItems);
            for (MasterItem mi : otherItems) {
                // コメントは除外
                if (mi.getCode().startsWith("16")) {
                    cnt++;
                }
            }
        }
        
        // 外来迅速検査の数量を設定しリストに追加する
        if (rapid != null && cnt > 0) {
            String num = String.valueOf(Math.min(cnt, 5));
            rapid.setNumber(num);
            masterItemList.add(rapid);
        }
    }

    private void configureButtonAction() {

        view.getBtnTenkai().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });
        view.getBtnCancel().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                closePanel();
            }
        });
        view.getBtnClearAll().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                clearAllCheckBox();
            }
        });
        view.getCmbStamp().addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    importStamp((String) view.getCmbStamp().getSelectedItem());
                }
            }
        });
    }

    private void importStamp(String name) {

        final ModuleInfoBean stampInfo = stampMap.get(name);
        if (stampInfo == null) {
            return;
        }
        if (stampInfo.isSerialized()) {

            final StampDelegater sdl = StampDelegater.getInstance();

            SimpleWorker task = new SimpleWorker<StampModel, Void>() {

                @Override
                protected StampModel doInBackground() throws Exception {
                    StampModel getStamp = sdl.getStamp(stampInfo.getStampId());
                    return getStamp;
                }

                @Override
                public void succeeded(StampModel result) {
                    if (result != null) {
                        masterItemList.clear();
                        ClaimBundle bundle = (ClaimBundle) BeanUtils.xmlDecode(result.getStampBytes());
                        ClaimItem[] ci = bundle.getClaimItem();
                        for (ClaimItem item : ci) {
                            masterItemList.add(editor.claimToMasterItem(item));
                        }
                        checkupLaboCheckBox();
                    }
                }
            };
            task.execute();
        }
    }

    private void prepareCmbStamp() {

        StampBoxPlugin stampBox = Dolphin.getInstance().getStampBox();
        view.getCmbStamp().removeAllItems();
        view.getCmbStamp().addItem(DEFAULT_COMBO_ITEM);
        List<ModuleInfoBean> list = stampBox.getAllStamps(IInfoModel.ENTITY_LABO_TEST);
        for (ModuleInfoBean info : list) {
            String name = info.getStampName();
            if (name != null && !name.startsWith(FROM_EDITOR_STAMP_NAME)) {
                view.getCmbStamp().addItem(name);
                stampMap.put(name, info);
            }
        }
    }

    private void prepareCheckBox() {
        
        int todayInt = MMLDate.getTodayInt();

        for (LaboCheckBox cb : checkBoxList) {
            // CheckBoxにTensuMasterを登録する
            TensuMaster tm = tensuMasterMap.get(cb.getSrycd());
            cb.setTensuMaster(tm);
            // TensuMasterが登録されていて有効期限がＯＫなCheckBoxのみenableする
            if (tm != null 
                    && todayInt >= Integer.valueOf(tm.getYukostymd())
                    && todayInt <= Integer.valueOf(tm.getYukoedymd())) {
                cb.setEnabled(true);
            } else {
                cb.setEnabled(false);
            }
        }

        // 情報ラベル表示
        view.getLblMishutoku().setVisible(tensuMasterMap == null);

    }

    private void prepareMasterItemMap() {

        // TensuMasterをORCAから取得してmasterItemMapに登録する
        final SqlMiscDao dao = SqlMiscDao.getInstance();
        final SwingWorker worker = new SwingWorker<List<TensuMaster>, Void>() {

            @Override
            protected List<TensuMaster> doInBackground() throws Exception {

                List<String> srycdList = new ArrayList<String>();
                for (LaboCheckBox cb : checkBoxList) {
                    srycdList.add(srycdFrmt.format(cb.getSrycd()));
                }
                // データベースで検査項目コードに一致するTensuMasterをまとめて取得する。
                List<TensuMaster> tmResult = dao.getTensuMasterList(srycdList);
                if (!dao.isNoError()) {
                    throw new Exception(dao.getErrorMessage());
                }
                return tmResult;
            }

            @Override
            protected void done() {
                try {
                    List<TensuMaster> tmResult = get();
                    // 取得したTensuMasterをtensuMasterMapに登録する。
                    tensuMasterMap = new HashMap<Integer, TensuMaster>();
                    for (TensuMaster tm : tmResult) {
                        tensuMasterMap.put(Integer.valueOf(tm.getSrycd()), tm);
                    }
                    prepareCheckBox();
                } catch (Exception ex) {
                    closePanel();
                }
            }
        };

        // 別スレッドで処理
        worker.execute();
    }
}
