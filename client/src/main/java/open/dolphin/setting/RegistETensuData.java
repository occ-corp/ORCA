
package open.dolphin.setting;

import java.util.List;
import javax.swing.ProgressMonitor;
import open.dolphin.dao.SqlETensuDao;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.infomodel.ETensuModel1;


/**
 * 電子点数表をORCAから取得して登録する 未使用
 * 
 * @author masuda, Masuda Naika
 */
public class RegistETensuData {

    private static final String FINISHED = "finished";
    
    private ProjectSettingDialog context;


    public void startRegist(ProjectSettingDialog context) {
        
        this.context = context;
        
        AddETensuModelTask task = new AddETensuModelTask();
        task.execute();
    }
    
    public void startInitSantei(ProjectSettingDialog context) {
         
        this.context = context;
        InitSanteiHistoryTask task = new InitSanteiHistoryTask();
        task.execute();
    }

    private class AddETensuModelTask extends SimpleWorker<Void, String[]> {
        
        private ProgressMonitor progressMonitor;
        private final String message = "電子点数表を登録しています。      ";
        private final String progressNote = "<html>全%d件中、%dから%d件まで、<br>追加%d件、更新%d件";
        private final String startingNote = "処理を開始します。";
        private final String initialNote = "<html><br>";
        
        @Override
        protected Void doInBackground() throws Exception {
            
            context.getBlockGlass().block();

            progressMonitor = new ProgressMonitor(context.getDialog(), message, initialNote, 0, 100);
            progressMonitor.setMillisToDecideToPopup(0); // この処理は絶対時間がかかるので，すぐ出す
            progressMonitor.setMillisToPopup(0);
            
            MasudaDelegater del = MasudaDelegater.getInstance();
            SqlETensuDao dao = SqlETensuDao.getInstance();
            
            final int limit = 200;
            long rowCount = dao.getETensu1RowCount();
            long offset = 0;
            // progress bar 表示
            publish(new String[]{startingNote, "0"});
            
            while (offset < rowCount) {
                // キャンセルされた場合
                if (progressMonitor.isCanceled()) {
                    break;
                }
                
                List<ETensuModel1> list = dao.getETensu1List(offset, limit);
                long toIndex = Math.min(offset + limit, rowCount) - 1;
                
                String[] params = del.updateETensu1Table(list).split(",");
                long added = Long.valueOf(params[0]);
                long updated = Long.valueOf(params[1]);
                offset += limit;
                // progress bar 表示
                String note = String.format(progressNote, rowCount, offset, toIndex, added, updated);
                int ratio = (int) (100 * offset / rowCount);
                publish(new String[]{note, String.valueOf(ratio)});
            }
            
            return null;
        }
        
        @Override
        protected void succeeded(Void result) {
            context.getBlockGlass().unblock();
            progressMonitor.close();
        }
        
        @Override
        protected void process(List<String[]> chunks) {
            for (String[] chunk : chunks) {
                progressMonitor.setNote(chunk[0]);
                progressMonitor.setProgress(Integer.valueOf(chunk[1]));
            }
        }
        
        @Override
        protected void failed(Throwable cause) {
            cause.printStackTrace(System.err);
            context.getBlockGlass().unblock();
        }
    }
    
    private class InitSanteiHistoryTask extends SimpleWorker<Void, String[]> {
        
        private ProgressMonitor progressMonitor;
        private final String message = "既存カルテからデータを抽出しています。  ";
        private final String progressNote = "<html>全%d件中、%dから%d件、<br>追加%d件、更新%d件、全%d件";
        private final String startingNote = "処理を開始します。";
        private final String initialNote = "<html><br>";
        
        @Override
        protected Void doInBackground() throws Exception {
            
            context.getBlockGlass().block();

            progressMonitor = new ProgressMonitor(context.getDialog(), message, initialNote, 0, 100);
            progressMonitor.setMillisToDecideToPopup(0); // この処理は絶対時間がかかるので，すぐ出す
            progressMonitor.setMillisToPopup(0);
            
            MasudaDelegater del = MasudaDelegater.getInstance();
            
            final int maxResults = 100;
            long totalModelCount = 0;
            long fromId = 0;
            int page = 0;
            long totalAdded = 0;
            String ret = null;
            // progress bar 表示
            publish(new String[]{startingNote, "0"});
            
            while (!FINISHED.equals(ret)) {
                // キャンセルされた場合
                if (progressMonitor.isCanceled()) {
                    context.getBlockGlass().unblock();
                    break;
                }

                ret = del.initSanteiHistory(fromId, maxResults);
                
                if (!FINISHED.equals(ret)) {
                    String[] str = ret.split(",");
                    fromId = Long.valueOf(str[0]);
                    totalModelCount = Long.valueOf(str[1]);
                    long added = Long.valueOf(str[2]);
                    long updated = Long.valueOf(str[3]);
                    totalAdded += added;
                    page++;
                    // progress bar 表示
                    int ratio = (totalModelCount == 0)
                        ? 0 : (int) (100 * page * maxResults / totalModelCount);
                    String note = String.format(progressNote, totalModelCount, fromId, maxResults, added, updated, totalAdded);
                    publish(new String[]{note, String.valueOf(ratio)});
                }
            }
            
            return null;
        }
        
        @Override
        protected void succeeded(Void result) {
            context.getBlockGlass().unblock();
            progressMonitor.close();
        }
        
        @Override
        protected void process(List<String[]> chunks) {
            for (String[] chunk : chunks) {
                progressMonitor.setNote(chunk[0]);
                progressMonitor.setProgress(Integer.valueOf(chunk[1]));
            }
        }
        
        @Override
        protected void failed(Throwable cause) {
            cause.printStackTrace(System.err);
            context.getBlockGlass().unblock();
        }
    }
}
