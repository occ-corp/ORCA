package open.dolphin.impl.mml;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import open.dolphin.client.ClientContext;
import open.dolphin.client.MainWindow;
import open.dolphin.client.MmlMessageEvent;
import open.dolphin.client.MmlMessageListener;
import open.dolphin.infomodel.SchemaModel;
import open.dolphin.project.Project;
import org.apache.log4j.Logger;


/**
 * MML 送信サービス。
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika こりゃ失敗ｗ
 */
public class SendMmlImpl implements MmlMessageListener {
    
    // CSGW への書き込みパス
    // CSGW = Client Side Gateway
    private String csgwPath;
    
    // MML Encoding
    private String encoding;
    
    // Work Queue
    private final List<MmlMessageEvent> queue;
    private ExecutorService exec;


    private MainWindow context;
    
    private String name;
    
    private Logger logger;

    
    /**
     * Creates new SendMmlService
     */
    public SendMmlImpl() {
        queue = new LinkedList<MmlMessageEvent>();
        exec = Executors.newSingleThreadExecutor();
        logger = ClientContext.getMmlLogger();
        encoding = "UTF-8";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public MainWindow getContext() {
        return context;
    }

    @Override
    public void setContext(MainWindow context) {
        this.context = context;
    }

    @Override
    public String getCSGWPath() {
        return csgwPath;
    }

    @Override
    public void setCSGWPath(String val) {
        csgwPath = val;
        File directory = new File(csgwPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                logger.debug("MMLファイル出力先のディレクトリを作成しました");
            } else {
                logger.warn("MMLファイル出力先のディレクトリを作成できません");
            }
        }
    }

    @Override
    public synchronized void stop() {
        shutdownExecutor();
        logDump();
    }

    @Override
    public void start() {

        // CSGW 書き込みパスを設定する
        setCSGWPath(Project.getCSGWPath());

        // 送信スレッドを開始する
        if (exec != null) {
            shutdownExecutor();
        }
        exec = Executors.newSingleThreadExecutor();

        logger.info("Send MML statered with CSGW = " + getCSGWPath());
    }

    @Override
    public synchronized void mmlMessageEvent(MmlMessageEvent e) {
        queue.add(e);
        processQueue();
    }

    private void processQueue() {
        
        if (queue == null || queue.isEmpty()) {
            return;
        }
        
        List<Callable<MmlMessageEvent>> taskList = new ArrayList<Callable<MmlMessageEvent>>();
        for (MmlMessageEvent mmlEvent : queue) {
            Callable task = new MmlOutputTask(mmlEvent);
            taskList.add(task);
        }
        try {
            List<Future<MmlMessageEvent>> futures = exec.invokeAll(taskList);
            for (Future<MmlMessageEvent> future : futures) {
                // 成功したらqueueから除去
                MmlMessageEvent result;
                try {
                    result = future.get();
                    queue.remove(result);
                } catch (ExecutionException e) {
                    e.printStackTrace(System.err);
                    logger.warn(e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
            logger.warn(e.getMessage());
        }
    }

    private void logDump() {

        for (MmlMessageEvent event : queue) {
            logger.warn(event.getMmlInstance());
        }
        queue.clear();
    }

    private void shutdownExecutor() {

        try {
            exec.shutdown();
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException ex) {
            exec.shutdownNow();
        } catch (NullPointerException ex) {
        }
        exec = null;
    }

    private String getCSGWPathname(String fileName, String ext) {
        StringBuilder buf = new StringBuilder();
        buf.append(csgwPath);
        buf.append(File.separator);
        buf.append(fileName);
        buf.append(".");
        buf.append(ext);
        return buf.toString();
    }

    private class MmlOutputTask implements Callable {

        private MmlMessageEvent mmlEvent;

        private MmlOutputTask(MmlMessageEvent mmlEvent) {
            this.mmlEvent = mmlEvent;
        }

        @Override
        public MmlMessageEvent call() throws Exception {

            // MML パッケージを取得
            //getLogger().debug("MMLファイルをコンシュームしました");
            String groupId = mmlEvent.getGroupId();
            String instance = mmlEvent.getMmlInstance();
            List<SchemaModel> schemas = mmlEvent.getSchema();

            // ファイル名を生成する
            String dest = getCSGWPathname(groupId, "xml");
            String temp = getCSGWPathname(groupId, "xml.tmp");
            File f = new File(temp);

            // インスタンスをUTF8で書き込む
            BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(f));
            byte[] bytes = instance.getBytes(encoding);
            writer.write(bytes);
            writer.flush();
            writer.close();

            // 書き込み終了後にリネームする (.tmp -> .xml)
            f.renameTo(new File(dest));
            logger.debug("MMLファイルを書き込みました");

            // 画像を送信する
            if (schemas != null) {
                for (SchemaModel schema : schemas) {
                    dest = csgwPath + File.separator + schema.getExtRefModel().getHref();
                    temp = dest + ".tmp";
                    f = new File(temp);
                    writer = new BufferedOutputStream(new FileOutputStream(f));
                    writer.write(schema.getJpegByte());
                    writer.flush();
                    writer.close();

                    // Renameする
                    f.renameTo(new File(dest));
                    logger.debug("画像ファイルを書き込みました");
                }
            }
            return mmlEvent;
        }
    }
}