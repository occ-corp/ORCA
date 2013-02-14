package open.dolphin.impl.mml;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import open.dolphin.client.*;
import open.dolphin.infomodel.SchemaModel;
import open.dolphin.project.Project;
import org.apache.log4j.Logger;


/**
 * MML 送信サービス。
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public class SendMmlImpl implements MmlMessageListener {
    
    // CSGW への書き込みパス
    // CSGW = Client Side Gateway
    private String csgwPath;
    
    // MML Encoding
    private String encoding;
    
    private MainWindow context;
    
    private String name;
    
    private Logger logger;
    
    private static final String MML = "MML";
    
    /** Creates new SendMmlService */
    public SendMmlImpl() {
        logger = ClientContext.getMmlLogger();
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
        if (! directory.exists()) {
            if (directory.mkdirs()) {
                logger.debug("MMLファイル出力先のディレクトリを作成しました");
            } else {
                logger.warn("MMLファイル出力先のディレクトリを作成できません");
            }
        }
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void start() {
        
        // CSGW 書き込みパスを設定する
        setCSGWPath(Project.getCSGWPath());
        encoding = Project.getString(Project.MML_ENCODING);
    }
    
    @Override
    public void mmlMessageEvent(MmlMessageEvent mevt) {
        
        MMLSender sender = (MMLSender) mevt.getSource();

        try {
            String groupId = mevt.getGroupId();
            String instance = mevt.getMmlInstance();
            List<SchemaModel> schemas = mevt.getSchema();

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

        } catch (IOException e) {
            e.printStackTrace(System.err);
            String errMsg = e.getMessage();
            logger.warn(errMsg);
            sender.fireResult(new KarteSenderResult(MML, KarteSenderResult.ERROR, errMsg, sender));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            String errMsg = e.getMessage();
            logger.warn(errMsg);
            sender.fireResult(new KarteSenderResult(MML, KarteSenderResult.ERROR, errMsg, sender));
        }
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
}