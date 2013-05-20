package open.dolphin.helper;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import open.dolphin.client.ChartImpl;
import open.dolphin.client.ClientContext;
import open.dolphin.client.EditorFrame;

/**
 * Window Menu をサポートするためのクラス。
 * Factory method で WindowMenu をもつ JFrame を生成する。
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class WindowSupport implements MenuListener {
    
    private static final String WINDOW_MWNU_NAME = "ウインドウ";
    private static final ImageIcon icon = 
            ClientContext.getClientContextStub().getImageIcon("dolphinIcon.png");
    
    // WindowSupportのリスト　フォーカス処理にも使用
    private static final SortedSet<WindowSupport> allWindows;
    // allChartsはChartImplから移動
    private static final List<ChartImpl> allCharts;
    // allEditorFramesはEditorFrameから移動
    private static final List<EditorFrame> allEditorFrames;
    
    static {
        allWindows = new ConcurrentSkipListSet<>(new WindowComparator());
        allEditorFrames = new CopyOnWriteArrayList<>();
        allCharts = new CopyOnWriteArrayList<>();
    }
    
    // Window support が提供するスタッフ
    // フレーム
    private JFrame frame;
    
    // メニューバー
    private JMenuBar menuBar;
    
    // ウインドウメニュー
    private JMenu windowMenu;
    
    // Window Action
    private Action windowAction;
    
    // ChartMediator
    private Object mediator;
    
    // プライベートコンストラクタ
    private WindowSupport(JFrame frame, JMenuBar menuBar, JMenu windowMenu,
            Action windowAction) {
        this.frame = frame;
        this.menuBar = menuBar;
        this.windowMenu = windowMenu;
        this.windowAction = windowAction;
    }

    /**
     * WindowSupportを生成する。
     * @param title フレームタイトル
     * @return WindowSupport
     */
    public static WindowSupport create(String title) {
        return create(title, null);
    }
    
    public static WindowSupport create(String title, final ChartImpl chartImpl) {
        
        // フレームを生成する
        final JFrame frame = new JFrame(title);
        // dolphinアイコンをセット
        frame.setIconImage(icon.getImage());

        // メニューバーを生成する
        JMenuBar menuBar = new JMenuBar();
        
        // Window メニューを生成する
        JMenu windowMenu = new JMenu(WINDOW_MWNU_NAME);
        
        // メニューバーへWindow メニューを追加する
        menuBar.add(windowMenu);
        
        // フレームにメニューバーを設定する
        frame.setJMenuBar(menuBar);
        
        // Windowメニューのアクション
        // 選択されたらフレームを全面にする
        Action windowAction = new AbstractAction(title) {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setExtendedState(Frame.NORMAL);
                frame.toFront();
            }
        };
        
        // インスタンスを生成する
        final WindowSupport windowSupport
                = new WindowSupport(frame, menuBar, windowMenu, windowAction);

        // WindowEvent をこのクラスに通知しリストの管理を行う
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowOpened(WindowEvent e) {
                allWindows.add(windowSupport);
                if (chartImpl != null) {
                    allCharts.add(chartImpl);
                }
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                allWindows.remove(windowSupport);
                if (chartImpl != null) {
                    allCharts.remove(chartImpl);
                }
            }
        });
        
        // windowMenu にメニューリスナを設定しこのクラスで処理をする
        windowMenu.addMenuListener(windowSupport);
        
        return windowSupport;
    }
    
    public static Object getMediator(JFrame frame) {
        for (WindowSupport ws : allWindows) {
            if (ws.getFrame() == frame) {
                return ws.getMediator();
            }
        }
        return null;
    }
    
    public static List<EditorFrame> getAllEditorFrames() {
        return allEditorFrames;
    }

    public static List<ChartImpl> getAllCharts() {
        return allCharts;
    }
    
    public JFrame getFrame() {
        return frame;
    }
    
    public JMenuBar getMenuBar() {
        return menuBar;
    }
    
    public JMenu getWindowMenu() {
        return windowMenu;
    }
    
    public Action getWindowAction() {
        return windowAction;
    }
    
    // ChartMediator(MainWindowの場合はMediator)をセットする
    public void setMediator(Object mediator) {
        this.mediator = mediator;
    }
    public Object getMediator() {
        return mediator;
    }
    
    /**
     * ウインドウメニューが選択された場合、現在オープンしているウインドウのリストを使用し、
     * それらを選択するための MenuItem を追加する。
     */
    @Override
    public void menuSelected(MenuEvent e) {
        
        // 全てリムーブする
        JMenu wm = (JMenu) e.getSource();
        wm.removeAll();
        
        // リストから新規に生成する
        for (WindowSupport ws : allWindows) {
            Action action = ws.getWindowAction();
            wm.add(action);
        }
    }
    
    @Override
    public void menuDeselected(MenuEvent e) {
    }
    
    @Override
    public void menuCanceled(MenuEvent e) {
    }
    
    // タイトルに応じてソートするComparator
    private static class WindowComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            String title1 = ((WindowSupport) o1).getFrame().getTitle();
            String title2 = ((WindowSupport) o2).getFrame().getTitle();
            return (title1 == null) ? -1 : title1.compareTo(title2);
        }
    }
}
