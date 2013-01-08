package open.dolphin.impl.mml;

import java.io.*;
import open.dolphin.client.*;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.message.MMLHelper;
import open.dolphin.project.Project;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 */
public class MMLSender implements IKarteSender {
    
    private Chart context;
    private static final String MML = "MML";
    //private MmlMessageListener mmlListener;

    @Override
    public Chart getContext() {
        return context;
    }

    @Override
    public void setContext(Chart context) {
        this.context = context;
    }

/*
    @Override
    public void prepare(DocumentModel data) {
        if (data.getDocInfoModel().isSendMml()) {
            mmlListener = context.getMMLListener();
        }
    }
*/
    
    @Override
    public KarteSenderResult send(DocumentModel model) {

        if (!model.getDocInfoModel().isSendMml() || context == null) {
            return new KarteSenderResult(MML, KarteSenderResult.SKIPPED, null);
        }

        MmlMessageListener mmlListener = context.getMMLListener();
        if (mmlListener == null) {
            return new KarteSenderResult(MML, KarteSenderResult.SKIPPED, null);
        }
        
        // MML Message を生成する
        MMLHelper mb = new MMLHelper();
        mb.setDocument(model);
        mb.setUser(Project.getUserModel());
        mb.setPatientId(context.getPatient().getPatientId());
        mb.buildText();

        try {
            VelocityContext vct = ClientContext.getVelocityContext();
            vct.put("mmlHelper", mb);

            // このスタンプのテンプレートファイルを得る
            String templateFile = "mml2.3Helper.vm";

            // Merge する
            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);
            InputStream instream = ClientContext.getTemplateAsStream(templateFile);
//masuda^   UTF-8に
            //BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "SHIFT_JIS"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
//masuda$
            Velocity.evaluate(vct, bw, "mml", reader);
            bw.flush();
            bw.close();
            reader.close();
            String mml = sw.toString();
            //System.out.println(mml);

            // debug出力を行う
            if (ClientContext.getMmlLogger() != null) {
                ClientContext.getMmlLogger().debug(mml);
            }

            MmlMessageEvent mevt = new MmlMessageEvent(this);
            mevt.setGroupId(mb.getDocId());
            mevt.setMmlInstance(mml);
            if (mb.getSchema() != null) {
                mevt.setSchema(mb.getSchema());
            }
            mmlListener.mmlMessageEvent(mevt);

            if (Project.getBoolean(Project.JOIN_AREA_NETWORK)) {
            // TODO
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
            return new KarteSenderResult(MML, KarteSenderResult.ERROR, e.getMessage());
        }
        return new KarteSenderResult(MML, KarteSenderResult.NO_ERROR, null);
    }

}
