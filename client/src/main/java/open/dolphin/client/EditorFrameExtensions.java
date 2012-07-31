
package open.dolphin.client;

import javax.swing.JToolBar;

/**
 * EditorFrameExtensions
 * 
 * @author masuda, Masuda Naika
 */
public class EditorFrameExtensions extends AbstractChartExtensions{
    
    public EditorFrameExtensions(Chart context) {
        this.context = context;
    }

    @Override
    public JToolBar createToolBar() {
        JToolBar myToolBar = new JToolBar();

        // 共通ボタンを追加
        addCommonBtn(myToolBar);
        return myToolBar;
    }

    @Override
    protected EditorFrame getContext() {
        return (EditorFrame) context;
    }

}
