package open.dolphin.order;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import open.dolphin.client.Chart;
import open.dolphin.client.ClientContext;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;


/**
 * Stamp 編集用の外枠を提供する Dialog.
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class StampEditor implements PropertyChangeListener {

    private AbstractStampEditor editor;
    private JDialog dialog;

    
    public StampEditor(ModuleModel[] stamps, final PropertyChangeListener listener, Chart chart) {


        String entity = stamps[0].getModuleInfoBean().getEntity();

        if (entity.equals(IInfoModel.ENTITY_MED_ORDER)) {
            // RP
            editor = new RpEditor(entity);

        } else if (entity.equals(IInfoModel.ENTITY_RADIOLOGY_ORDER)) {
            // Injection
            editor = new RadEditor(entity);

        } else if (entity.equals(IInfoModel.ENTITY_INJECTION_ORDER)) {
            // Rad
            editor = new InjectionEditor(entity);

        } else if (entity.equals("text")) {
            // テキストスタンプエディタ
            editor = new TextStampEditor(entity);
        } else if (entity.equals(IInfoModel.ENTITY_INSTRACTION_CHARGE_ORDER)) {
            // 指導
            editor = new InstractionEditor(entity);
        } else {
            // others, ex. physiology. most simple and basic editor
            editor = new BaseEditor(entity);
        }

//masuda^   editorにChartを設定する
        editor.setContext(chart);
//masuda$
        
        editor.addPropertyChangeListener(AbstractStampEditor.VALUE_PROP, listener);
        editor.addPropertyChangeListener(AbstractStampEditor.EDIT_END_PROP, StampEditor.this);
        editor.setValue(stamps);

        dialog = new JDialog(new JFrame(), true);
//masuda^    アイコン設定
        ClientContext.setDolphinIcon(dialog);
//masuda$
        dialog.setTitle(editor.getOrderName());
        dialog.getContentPane().add(editor.getView(), BorderLayout.CENTER);
        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                editor.setFocusOnSearchTextFld();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                dialog.setVisible(false);
            }
        });

        dialog.pack();
//masuda^   エディタごとにウィンドウサイズを記憶させる
        //ComponentMemory cm = new ComponentMemory(dialog, new Point(200, 100), dialog.getPreferredSize(), this);
        ComponentMemory cm = new ComponentMemory(dialog, new Point(200, 100), dialog.getPreferredSize(), editor);
//masuda$
        cm.setToPreferenceBounds();

        dialog.setVisible(true);
    }

    public StampEditor(RegisteredDiagnosisModel[] models, PropertyChangeListener listener, Window lock) {

        editor = new DiseaseEditor();
        editor.addPropertyChangeListener(AbstractStampEditor.VALUE_PROP, listener);
        editor.addPropertyChangeListener(AbstractStampEditor.EDIT_END_PROP, StampEditor.this);

        dialog = new JDialog((Frame) lock, true);
//masuda^    アイコン設定
        ClientContext.setDolphinIcon(dialog);
//masuda$
        dialog.setTitle(editor.getOrderName());
        dialog.getContentPane().add(editor.getView(), BorderLayout.CENTER);
        editor.setValue(models);     // 編集する病名をセット
        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                editor.setFocusOnSearchTextFld();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                dialog.setVisible(false);
            }
        });

        dialog.pack();
//masuda^   エディタごとにウィンドウサイズを記憶させる
        ComponentMemory cm = new ComponentMemory(dialog, new Point(200, 100), dialog.getPreferredSize(), editor);
        cm.setToPreferenceBounds();

        dialog.setVisible(true);
    }
    
    public StampEditor(String entity, final PropertyChangeListener listener, final Window lock) {

        Runnable r = new Runnable() {

            @Override
            public void run() {

                editor = new DiseaseEditor();
                editor.addPropertyChangeListener(AbstractStampEditor.VALUE_PROP, listener);
                editor.addPropertyChangeListener(AbstractStampEditor.EDIT_END_PROP, StampEditor.this);

                dialog = new JDialog((Frame) lock, true);
//masuda^    アイコン設定
                ClientContext.setDolphinIcon(dialog);
//masuda$
                dialog.setTitle(editor.getOrderName());
                dialog.getContentPane().add(editor.getView(), BorderLayout.CENTER);
                dialog.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowOpened(WindowEvent e) {
                        editor.setFocusOnSearchTextFld();
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        dialog.dispose();
                        dialog.setVisible(false);
                    }
                });

                dialog.pack();
//masuda^   エディタごとにウィンドウサイズを記憶させる
                //ComponentMemory cm = new ComponentMemory(dialog, new Point(200,100), dialog.getPreferredSize(), this);
                ComponentMemory cm = new ComponentMemory(dialog, new Point(200, 100), dialog.getPreferredSize(), editor);
//masuda$
                cm.setToPreferenceBounds();

                dialog.setVisible(true);
            }
        };

        SwingUtilities.invokeLater(r);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getPropertyName().equals(AbstractStampEditor.EDIT_END_PROP)) {
            Boolean b = (Boolean) evt.getNewValue();
            if (b.booleanValue()) {
                dialog.dispose();
                dialog.setVisible(false);
            }
        }
    }
}