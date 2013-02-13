package open.dolphin.setting;

import java.util.List;
import javax.swing.ProgressMonitor;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.SimpleWorker;

/**
 * 設定画面からもHibernateSearch初期化
 * 
 * @author masuda, Masuda Naika
 */
public class InitHibernateSearchIndex {
    
    private static final String FINISHED = "finished";
    private ProjectSettingDialog context;
    
    public void start(ProjectSettingDialog context) {
        this.context = context;
        
        IndexTaskWorker worker = new IndexTaskWorker();
        context.getBlockGlass().setText("インデックス作成は時間がかかります。");
        worker.execute();
    }
    
    private class IndexTaskWorker extends SimpleWorker<Void, String[]> {

        private ProgressMonitor progressMonitor;
        private final String message = "インデックス作成";
        private final String progressNote = "<html>索引を作成中<br>%d件中、%d％完了";
        private final String startingNote = "処理を開始します。";
        private final String initialNote = "<html><br>";

        @Override
        protected Void doInBackground() {

            context.getBlockGlass().block();
            // progress bar 設定
            progressMonitor = new ProgressMonitor(context.getDialog(), message, initialNote, 0, 100);
            progressMonitor.setMillisToDecideToPopup(0); // この処理は絶対時間がかかるので，すぐ出す
            progressMonitor.setMillisToPopup(0);
            progressMonitor.setProgress(0);

            // 索引作成開始
            MasudaDelegater dl = MasudaDelegater.getInstance();

            // maxResult毎にインデックス作成する
            final int maxResults = 200;
            long fromDocPk = 0;
            int page = 0;
            String ret = null;
            // progress bar 表示
            publish(new String[]{startingNote, "0"});
            
            while (!FINISHED.equals(ret)) {
                // キャンセルされた場合
                if (progressMonitor.isCanceled()) {
                    break;
                }
                ret = dl.makeDocumentModelIndex(fromDocPk, maxResults);
                if (!FINISHED.equals(ret)) {
                    String[] str = ret.split(",");
                    long totalModelCount = Long.valueOf(str[1]);
                    fromDocPk = Long.valueOf(str[0]);
                    page++;
                    // progress bar 表示
                    int ratio = (totalModelCount == 0)
                        ? 0 : (int) (100 * page * maxResults / totalModelCount);
                    String msg = String.format(progressNote, totalModelCount, ratio);
                    publish(new String[]{msg, String.valueOf(ratio)});
                }
            }
            return null;
        }

        @Override
        protected void process(List<String[]> chunks) {
            for (String[] chunk : chunks) {
                progressMonitor.setNote(chunk[0]);
                progressMonitor.setProgress(Integer.valueOf(chunk[1]));
            }
        }

        @Override
        protected void done() {
            context.getBlockGlass().setText("");
            progressMonitor.close();
            context.getBlockGlass().unblock();
        }

        @Override
        protected void failed(Throwable cause) {
            cause.printStackTrace(System.err);
            context.getBlockGlass().unblock();
        }
    }
}
