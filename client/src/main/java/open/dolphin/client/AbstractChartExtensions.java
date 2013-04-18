package open.dolphin.client;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import open.dolphin.infomodel.AdmissionModel;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.tr.StampHolderTransferHandler;
import open.dolphin.util.NamedThreadFactory;

/**
 * AbstractChartExtensions
 * 
 * @author masuda, Masuda Naika
 */
public abstract class AbstractChartExtensions {
    
    protected JButton baseChargeBtn;
    protected JButton rpLabelBtn;
    
    private static final ImageIcon ICON_WIZ = ClientContext.getImageIconAlias("icon_wizard");
    private static final ImageIcon ICON_LBL = ClientContext.getImageIconAlias("icon_lbl_print");
    
    protected Chart context;
    // タイマー、ChartImplから移動
    private static final long DELAY = 10L;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> beeperHandle;
    private long statred;
    private long delay = DELAY;
    
    // 抽象メソッド
    public abstract JToolBar createToolBar();
    
    protected abstract Chart getContext();

    
    // 共通ボタンを追加。基本料入力とラベル印刷
    protected void addCommonBtn(JToolBar myToolBar) {

        // toolBarに基本料入力ボタンと処方ラベル印刷ボタンを追加
            baseChargeBtn = createButton();
            baseChargeBtn.setEnabled(false);
            baseChargeBtn.setIcon(ICON_WIZ);
            baseChargeBtn.setToolTipText("基本料スタンプを挿入します。");
            myToolBar.add(baseChargeBtn);
            baseChargeBtn.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    insertBaseChargeStamp();
                }
            });
        // toolBarにラベルプリンタのアドレスが設定されていればラベルプリンタのボタンを追加。
        // 空白なら不使用として非表示
        String lblPrtAddress = Project.getString(MiscSettingPanel.LBLPRT_ADDRESS, null);
        if (lblPrtAddress != null && !"".equals(lblPrtAddress)) {
            rpLabelBtn = createButton();
            rpLabelBtn.setEnabled(false);
            rpLabelBtn.setIcon(ICON_LBL);
            rpLabelBtn.setToolTipText("処方ラベルを印刷します。");
            myToolBar.add(rpLabelBtn);
            rpLabelBtn.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    printMedicineLabel();
                }
            });
        }
    }
    
    // 薬剤ラベルを印刷
    private void printMedicineLabel() {

        KarteEditor editor = context.getKarteEditor();
        if (editor == null) {
            return;
        }
        PrintLabel pl = new PrintLabel();
        pl.enter(editor.getPPane());
    }

    // 基本料入力
    private void insertBaseChargeStamp() {

        final KarteEditor editor = context.getKarteEditor();
        if (editor == null) {
            return;
        }
        
        // 基本料スタンプがすでにあればそれを編集する
        List<StampHolder> shList = editor.getPPane().getDocument().getStampHolders();
        for (StampHolder sh : shList) {
            String stampName = sh.getStamp().getModuleInfoBean().getStampName();
            if (MakeBaseChargeStamp.BCS_TITLE_IN.equals(stampName) 
                    || MakeBaseChargeStamp.BCS_TITLE_OUT.equals(stampName)){
                StampHolderTransferHandler.getInstance().stampHolderSingleSelection(sh);
                sh.edit();
                return;
            }
        }

        // 無い場合は新規に作成する
        MakeBaseChargeStamp mbcs = new MakeBaseChargeStamp();
        mbcs.enter(editor);
        if (mbcs.isModified()) {
            ModuleModel mm = mbcs.getBaseChargeStamp();
            editor.getPPane().getTextPane().setCaretPosition(0);
            editor.getPPane().stamp(mm);
        }
        mbcs = null;
    }
    
    // 診察時間タイマーと基本料入力・薬剤ラベル印刷ボタンをenableする。
    public void enableBtnTimer() {

        // 基本料・ラベル印刷ボタンをenableする。
        enableExtBtn(true);

        // timer 開始
        statred = System.currentTimeMillis();
        if (scheduler == null) {
            NamedThreadFactory factory = new NamedThreadFactory(getClass().getSimpleName());
            scheduler = Executors.newSingleThreadScheduledExecutor(factory);
        }
        final Runnable beeper = new Runnable() {

            @Override
            public void run() {
                long time = System.currentTimeMillis() - statred;
                time = time / 1000L;
                context.getStatusPanel().setTimeInfo(time);
            }
        };
        beeperHandle = scheduler.scheduleAtFixedRate(beeper, delay, delay, TimeUnit.SECONDS);
    }

    public void shutdownTimer() {
        if (beeperHandle != null) {
            beeperHandle.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
    
    // 基本料・ラベル印刷ボタンをenableする。
    public void enableExtBtn(boolean b) {

        KarteEditor editor = getContext().getKarteEditor();
        AdmissionModel admission = (editor == null)
                ? null 
                : editor.getModel().getDocInfoModel().getAdmissionModel();
        
        // 入院ならば基本料スタンプボタンは無効
        if (admission != null) {
            baseChargeBtn.setEnabled(false);
            baseChargeBtn.setVisible(false);
        } else {
            baseChargeBtn.setEnabled(b);
            baseChargeBtn.setVisible(b);
        }
        if (rpLabelBtn != null) {
            rpLabelBtn.setEnabled(b);
            rpLabelBtn.setVisible(b);
        }
    }
    
    // quaqua9でボタンボーダーが増えた？
    protected JButton createButton() {
        JButton btn = new JButton();
        btn.putClientProperty("Quaqua.Component.visualMargin", new Insets(0, 0, 0, 0));
        return btn;
    }
}
