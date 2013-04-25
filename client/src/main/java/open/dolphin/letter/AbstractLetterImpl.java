
package open.dolphin.letter;

import java.awt.print.PageFormat;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import open.dolphin.client.*;
import open.dolphin.delegater.LetterDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.LetterModule;
import open.dolphin.tr.TextComponentTransferHandler;

/**
 * レターのの抽象クラス
 * 
 * @author masuda, Masuda Naika
 */
public abstract class AbstractLetterImpl extends AbstractChartDocument implements Letter {
    
    protected LetterModule model;
    protected boolean listenerIsAdded;
    protected LetterStateMgr stateMgr;
    protected DocumentListener dl;
    
    protected abstract Panel2 getView();
    protected abstract String getFrameTitle();
    
    @Override
    public void setListeners() {

        dl = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                stateMgr.processDirtyEvent();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                stateMgr.processDirtyEvent();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                stateMgr.processDirtyEvent();
            }
        };
    }
    
    protected final void setComponentListeners(JTextComponent[] jcs) {
        
        for (JTextComponent jc : jcs) {
            jc.getDocument().addDocumentListener(dl);
            jc.addFocusListener(AutoKanjiListener.getInstance());
            jc.setTransferHandler(TextComponentTransferHandler.getInstance());
            jc.addMouseListener(CutCopyPasteAdapter.getInstance());
        }
    }
    
    @Override
    public final void save() {

        viewToModel();

        DBTask task = new DBTask<Boolean, Void>(getContext()) {

            @Override
            protected Boolean doInBackground() throws Exception {

                LetterDelegater ddl = LetterDelegater.getInstance();
                long result = ddl.saveOrUpdateLetter(model);
                model.setId(result);
                return true;
            }

            @Override
            protected void succeeded(Boolean result) {
                getContext().getDocumentHistory().getDocumentHistory();
                stateMgr.processSavedEvent();
            }
        };

        task.execute();
    }
     
    @Override
    public final void enter() {
        super.enter();
        if (stateMgr != null) {
            stateMgr.enter();
        }
    }

    @Override
    public final void stop() {
    }
    
    @Override
    public final boolean isDirty() {
        if (stateMgr != null) {
            return stateMgr.isDirtyState();
        } else {
            return super.isDirty();
        }
    }

    public final void modifyKarte() {
        stateMgr.processModifyKarteEvent();
    }
    
    @Override
    public final void print() {
        
        if (this.model == null) {
            return;
        }
        
        viewToModel();
        
        String frameTitle = getFrameTitle();

        StringBuilder sb = new StringBuilder();
        sb.append("PDFファイルを作成しますか?");

        int option = JOptionPane.showOptionDialog(
                getContext().getFrame(),
                sb.toString(),
                ClientContext.getFrameTitle(frameTitle),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"PDF作成", "フォーム印刷", "取消し"},
                "PDF作成");

        if (option == 0) {
            makePDF();
        } else if (option == 1) {
            PageFormat pageFormat = getContext().getContext().getPageFormat();
            String name = getContext().getPatient().getFullName();
            Panel2 panel = getView();
            panel.printPanel(pageFormat, 1, false, name, 0, true);
        }
    }
}
