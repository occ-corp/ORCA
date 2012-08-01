
package open.dolphin.client;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.*;
import open.dolphin.infomodel.*;
import open.dolphin.order.StampEditor;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.tr.StampHolderTransferHandler;
import open.dolphin.util.ZenkakuUtils;

/**
 * StampHolderのメソッドをまとめてみた
 * @author masuda, Masuda Naika
 */
public class StampHolderFunction {
    
    private static final String MED_TEIKI = "定期";
    private static final String MED_RINJI = "臨時";
    private static final String IN_MED_HOUKATSU = "院内包括";
    
    private static StampHolderFunction instance;
    
    // アクション群
    private AbstractAction copyAsTextAction;
    private AbstractAction editAction;
    private AbstractAction changeBundleNumberAction;
    private AbstractAction changeInExMedAction;
    private AbstractAction renumberRpAction;
    private AbstractAction changeHokatsuAction;
    private AbstractAction labelPrintAction;
    private AbstractAction registRoutineMedAction;
    private AbstractAction deleteAction;
    
    private StampHolder selectedStampHolder;
    
    static {
        instance = new StampHolderFunction();
    }
    
    private StampHolderFunction() {
        setupActions();
    }
    
    public static StampHolderFunction getInstance() {
        return instance;
    }
    
    
    // 編集対象のスタンプホルダを設定する
    public void setSelectedStampHolder(StampHolder sh) {
        selectedStampHolder = sh;
    }
    
    // スタンプホルダにDELETEキーでのスタンプ削除アクションを登録する
    public void setDeleteAction(final StampHolder sh) {
        sh.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteStamp");
        sh.getActionMap().put("deleteStamp", deleteAction);
    }
    
    // アクションを設定する
    private void setupActions() {

        // スタンプ削除アクション、これはDELETEキーで
        deleteAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteStamp();
            }
        };
        
        // 以下popup menuのアクション
        copyAsTextAction = new AbstractAction("テキストとしてコピー") {

            @Override
            public void actionPerformed(ActionEvent e) {
                copyAsText();
            }
        };
            
        editAction = new AbstractAction("編集") {

            @Override
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        };
        
        changeBundleNumberAction = new AbstractAction("処方日数変更") {

            @Override
            public void actionPerformed(ActionEvent e) {
                changeBundleNumber();
            }
        };
        
        changeInExMedAction = new AbstractAction("院内⇔院外") {

            @Override
            public void actionPerformed(ActionEvent e) {
                changeInnaiIngai();
            }
        };
        
        renumberRpAction = new AbstractAction("処方番号変更") {

            @Override
            public void actionPerformed(ActionEvent e) {
                changeRpNumber();
            }
        };
        
        changeHokatsuAction = new AbstractAction("包括にする") {

            @Override
            public void actionPerformed(ActionEvent e) {
                changeToHokatsu();
            }
        };

        labelPrintAction = new AbstractAction("ラベル印刷") {

            @Override
            public void actionPerformed(ActionEvent e) {
                printStampLabel();
            }
        };
        registRoutineMedAction = new AbstractAction("薬歴登録") {

            @Override
            public void actionPerformed(ActionEvent e) {
                registRoutineMed();
            }
        };
    }
    
    // スタンプホルダにpopup menuを表示する
    public void showPopupMenu(Point p) {

        KartePane kartePane = selectedStampHolder.getKartePane();

        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);
        ChartMediator mediator = kartePane.getMediator();
        popup.add(mediator.getAction(GUIConst.ACTION_CUT));
        popup.add(mediator.getAction(GUIConst.ACTION_COPY));

        // copyAsText
        popup.add(copyAsTextAction);
        popup.add(mediator.getAction(GUIConst.ACTION_PASTE));

        // 編集可の時のみ edit
        boolean editable = kartePane.getTextPane().isEditable();
        if (editable) {
            popup.addSeparator();
            popup.add(editAction);
        }

        // 処方日数変更と番号変更とまとめて編集をpopupに追加

        // StampHolderが選択されていないまま右クリックあるいは
        // 選択したStampHolder上以外で右クリックしたときは、いったん選択解除し
        // thisを選択状態にする。

        if (!selectedStampHolder.isSelected()) {
            StampHolderTransferHandler.getInstance().stampHolderSingleSelection(selectedStampHolder);
        }

        if (editable && isAllMedicine()) {
            JMenuItem item = new JMenuItem(changeBundleNumberAction);
            popup.add(item);
            List<StampHolder> stampHolderList = getSelectedStampHolder();
            if (stampHolderList.size() > 1) {
                item = new JMenuItem(renumberRpAction);
                popup.add(item);
            }

            item = new JMenuItem(changeInExMedAction);
            popup.add(item);

            // 包括にする
            item = new JMenuItem(changeHokatsuAction);
            popup.add(item);
        }

        // 薬剤ラベル印刷
        String lblPrtAddress = Project.getString(MiscSettingPanel.LBLPRT_ADDRESS, null);
        if (lblPrtAddress != null && !"".equals(lblPrtAddress)) {
            if (kartePane.getTextPane().isEditable()) {
                JMenuItem item = new JMenuItem(labelPrintAction);
                popup.add(item);
            }
        }

        // KarteViewerのみ個人薬歴登録
        if (kartePane.getParent() == null && isAllMedicine()) {
            // RoutineMed
            JMenuItem item = new JMenuItem(registRoutineMedAction);
            popup.add(item);
        }

        popup.show((Component) selectedStampHolder, p.x, p.y);
    }
    
    // 編集したスタンプホルダを更新する
    public void setNewValue(ModuleModel[] newStamps, ModuleModel[] oldValue) {

        // 取り消しならnullが返ってくる
        if (newStamps == null || newStamps.length == 0){
            return;
        }
        // 処方スタンプ以外のとき
        ModuleModel first = newStamps[0];
        if (!(first.getModel() instanceof BundleMed)) {
            // スタンプを置き換える
            selectedStampHolder.importStamp(first);
            return;
        }

        // 以下処方スタンプの場合
        // 元の選択していたスタンプが１個で新たなのも１個ならスタンプを置き換える
        if (oldValue != null && oldValue.length == 1 && newStamps.length == 1) {
            selectedStampHolder.importStamp(newStamps[0]);
            return;
        }

        // 複数スタンプ編集なら編集もとのスタンプは削除とする
        List<StampHolder> stampHolderList = getSelectedStampHolder();
        for (StampHolder sh : stampHolderList) {
            sh.getKartePane().removeStamp(sh);
        }

        // そして新しいのを追加しておく
        KartePane kartePane = selectedStampHolder.getKartePane();
        kartePane.getTextPane().setCaretPosition(selectedStampHolder.getStartPos());
        for (ModuleModel newStamp : newStamps) {
            kartePane.stamp(newStamp);
        }
    }
    
    // アクション処理群
    // スタンプホルダを編集する editAction
    public void edit() {

        KartePane kartePane = selectedStampHolder.getKartePane();
        ModuleModel stamp = selectedStampHolder.getStamp();
        if (kartePane.getTextPane().isEditable()) {

            // 基本料スタンプなら基本料入力補助で編集 masuda
            String stampName = stamp.getModuleInfoBean().getStampName();
            if ("基本料(院内)".equals(stampName) || "基本料(院外)".equals(stampName)){
                MakeBaseChargeStamp mbcs = new MakeBaseChargeStamp();
                mbcs.enter2(selectedStampHolder);
                if (mbcs.isModified() == true) {
                    ModuleModel mm = mbcs.getBaseChargeStamp();
                    selectedStampHolder.importStamp(mm);
                    return;
                }
            }

            String category = stamp.getModuleInfoBean().getEntity();
            if ("medOrder".equals(category) && isAllMedicine()) {
                List<StampHolder> stampHolderList = getSelectedStampHolder();
                int s = stampHolderList.size();
                ModuleModel[] stamps = new ModuleModel[s];
                for (int i = 0; i < s; ++i) {
                    stamps[i] = stampHolderList.get(i).getStamp();
                }
                new StampEditor(stamps, selectedStampHolder, kartePane.getParent().getContext());
            } else {
                new StampEditor(new ModuleModel[]{stamp}, selectedStampHolder, kartePane.getParent().getContext());
            }
        }
    }
    
    // 選択中のスタンプホルダを削除する deleteAction
    private void deleteStamp() {
        StampHolderTransferHandler handler = StampHolderTransferHandler.getInstance();
        if (handler != null) {
            handler.deleteSelectedStampHolder();
        }
    }

    // テキストとしてコピーする copyAsTextAction
    private void copyAsText() {
        
        IInfoModel im = selectedStampHolder.getStamp().getModel();
        if (im instanceof BundleDolphin) {
            BundleDolphin bundle = (BundleDolphin) im;
            StringSelection ss = new StringSelection(bundle.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        }
    }

    // 選択している薬剤を院内包括にする changeHokatsuAction
    private void changeToHokatsu() {

        List<StampHolder> stampHolderList = getSelectedStampHolder();
        for (StampHolder sh : stampHolderList) {
            ModuleModel mm = sh.getStamp();
            boolean editable = sh.getKartePane().getTextPane().isEditable();
            if (mm.getModel() instanceof BundleMed && editable) {
                BundleMed bundle = (BundleMed) mm.getModel();
                bundle.setMemo(IN_MED_HOUKATSU);
                String clsCode = bundle.getClassCode();
                clsCode = clsCode.substring(0, 2) + "3";
                if (ClaimConst.RECEIPT_CODE_RINJI_HOKATSU.equals(clsCode)) {
                    clsCode = ClaimConst.RECEIPT_CODE_NAIYO_HOKATSU;
                }
                bundle.setClassCode(clsCode);
                setMyTextLater(sh);
                sh.getKartePane().setDirty(true);
            }
        }
    }

    // 選択している薬剤の院外・院内処方を変更 changeInExMedAction
    private void changeInnaiIngai() {

        BundleMed bundle = (BundleMed) selectedStampHolder.getStamp().getModel();
        String clsCode = bundle.getClassCode();
        boolean exMed = clsCode.endsWith("2") || "院外処方".equals(bundle.getMemo());
        exMed = !exMed; //反転する
        List<StampHolder> stampHolderList = getSelectedStampHolder();
        for (StampHolder sh : stampHolderList) {
            ModuleModel mm = sh.getStamp();
            boolean editable = sh.getKartePane().getTextPane().isEditable();
            if (mm.getModel() instanceof BundleMed && editable) {
                bundle = (BundleMed) mm.getModel();
                String memo = exMed ? "院外処方" : "院内処方";
                bundle.setMemo(memo);
                clsCode = clsCode.substring(0, 2) + (exMed ? "2" : "1");
                bundle.setClassCode(clsCode);
                setMyTextLater(sh);
                sh.getKartePane().setDirty(true);
            }
        }
    }

    // スタンプのラベル印刷を行う labelPrintAction
    private void printStampLabel() {

        List<StampHolder> stampHolderList = getSelectedStampHolder();
        PrintLabel pl = new PrintLabel();
        pl.enter2(stampHolderList);
    }

    // 処方スタンプの日数変更を行う changeBundleNumberAction
    private void changeBundleNumber() {

        String defaultNum = ((BundleMed) selectedStampHolder.getStamp().getModel()).getBundleNumber();

        ScreenTenKey stk = new ScreenTenKey();
        stk.setInput(defaultNum);
        String num = stk.enterDialog();

        if (num == null || num.replaceAll("\\d+", "").length() > 0) {
            return;
        }
        List<StampHolder> stampHolderList = getSelectedStampHolder();
        for (StampHolder sh : stampHolderList) {
            ModuleModel mm = sh.getStamp();
            boolean editable = sh.getKartePane().getTextPane().isEditable();
            if (mm.getModel() instanceof BundleMed && editable) {
                BundleMed bundle = (BundleMed) mm.getModel();
                // 外用剤は総量なのでbundle数は変更しない
                if (!(bundle.getClassCode()).startsWith("23")) {
                    bundle.setBundleNumber(num);
                }
                setMyTextLater(sh);
                sh.getKartePane().setDirty(true);
            }
        }
    }

    // 処方スタンプの処方番号振り直しを行う renumberRpAction
    private void changeRpNumber() {

        List<StampHolder> stampHolderList = getSelectedStampHolder();
        if (stampHolderList.isEmpty()) {
            return;
        }

        int teiki = 1;
        int rinji = 1;
        for (StampHolder sh : stampHolderList) {
            ModuleModel mm = sh.getStamp();
            boolean editable = sh.getKartePane().getTextPane().isEditable();
            if (mm.getModel() instanceof BundleMed && editable) {
                // deepCopyを使う。そうしないとスタンプ箱内のスタンプ名まで変わってしまう
                //ModuleInfoBean info = (ModuleInfoBean) BeanUtils.getClonedObject(mm.getModuleInfoBean());
                ModuleInfoBean old = mm.getModuleInfoBean();
                ModuleInfoBean info = new ModuleInfoBean();
                info.setASP(old.isASP());
                info.setEditable(old.isEditable());
                info.setEntity(old.getEntity());
                info.setStampId(old.getStampId());
                info.setStampMemo(old.getStampMemo());
                info.setStampName(old.getStampName());
                info.setStampNumber(old.getStampNumber());
                info.setStampRole(old.getStampRole());
                info.setTurnIn(old.isTurnIn());
                
                String stampName = info.getStampName();
                if (stampName.contains(MED_TEIKI)) {
                    String newStampName = MED_TEIKI + " - " + intToZenkaku(teiki);
                    info.setStampName(newStampName);
                    ++teiki;
                }
                if (stampName.contains(MED_RINJI)) {
                    String newStampName = MED_RINJI + " - " + intToZenkaku(rinji);
                    info.setStampName(newStampName);
                    ++rinji;
                }
                mm.setModuleInfoBean(info);
                setMyTextLater(sh);
                sh.getKartePane().setDirty(true);
            }
        }
    }

    // 個人薬歴に登録 registRoutineMedAction
    private void registRoutineMed() {
        
        List<StampHolder> stampHolderList = getSelectedStampHolder();
        if (stampHolderList == null || stampHolderList.isEmpty()) {
            return;
        }
        Frame parent = (Frame) selectedStampHolder.getTopLevelAncestor();
        RoutineMedDialog dialog = new RoutineMedDialog();
        dialog.showDialog(stampHolderList, parent);
    }
    
    private boolean isAllMedicine() {
        boolean allMedicine = true;
        List<StampHolder> stampHolderList = getSelectedStampHolder();
        for (StampHolder sh : stampHolderList) {
            String category = sh.getStamp().getModuleInfoBean().getEntity();
            if (!"medOrder".equals(category)) {
                allMedicine = false;
                break;
            }
        }
        return allMedicine;
    }

    private String intToZenkaku(int num) {
        String[] NumZenkaku = {"０", "１", "２", "３", "４", "５", "６", "７", "８", "９"};
        StringBuilder sb = new StringBuilder();
        String str = String.valueOf(num);
        for (int i = 0; i < str.length(); ++i) {
            int n = Integer.valueOf(str.substring(i, i + 1));
            sb.append(NumZenkaku[n]);
        }
        return sb.toString();
    }

    private List<StampHolder> getSelectedStampHolder() {
        return StampHolderTransferHandler.getInstance().getSelectedStampHolder();
    }
    
    /**
     * スタンプのHTML内容をセットする
     */
    public void setMyText(StampHolder sh) {

        if (sh.getStamp() == null) {
            return;
        }

        String text = sh.getHints().getHtmlText(sh.getStamp());
        sh.setText(ZenkakuUtils.toHalfNumber(text));

        // カルテペインへ展開された時広がるのを防ぐ
        sh.setMaximumSize(sh.getPreferredSize());
    }
    
    private void setMyTextLater(final StampHolder sh) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                setMyText(sh);
            }
        });
    }
}
