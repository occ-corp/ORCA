package open.dolphin.helper;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import open.dolphin.client.ClientContext;

/**
 * Window Menu をサポートするためのクラス。
 * Factory method で WindowMenu をもつ JFrame を生成する。
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class WindowSupport implements MenuListener {
    
    private static List<WindowSupport> allWindows;
    
    // JFrameとChartMediatorのマップ　フォーカス処理に使用
    private static Map<JFrame, Object> mediatorMap;
    
    private static final String WINDOW_MWNU_NAME = "ウインドウ";
    
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
    
    private static final ImageIcon icon = ClientContext.getClientContextStub().getImageIcon("dolphinIcon.png");
    
    static {
        allWindows = new ArrayList<WindowSupport>();
        mediatorMap = new HashMap<JFrame, Object>();
    }
    
    /**
     * WindowSupportを生成する。
     * @param title フレームタイトル
     * @return WindowSupport
     */
    public static WindowSupport create(String title) {
        
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
                frame.toFront();
            }
        };
        
        // インスタンスを生成する
        final WindowSupport ret
                = new WindowSupport(frame, menuBar, windowMenu, windowAction);
        
        // WindowEvent をこのクラスに通知しリストの管理を行う
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                WindowSupport.windowOpened(ret);
            }
            
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                WindowSupport.windowClosed(ret);
            }
        });
        
        // windowMenu にメニューリスナを設定しこのクラスで処理をする
        windowMenu.addMenuListener(ret);
        return ret;
    }
    
    public static Object getRelatedMediator(JFrame frame) {
        return mediatorMap.get(frame);
    }
    
    public static List<WindowSupport> getAllWindows() {
        return allWindows;
    }
    
    public static void windowOpened(WindowSupport opened) {
        // リストに追加する
        allWindows.add(opened);
        // mediatorMapに追加する
        mediatorMap.put(opened.getFrame(), opened.getMediator());
    }
    
    public static void windowClosed(WindowSupport closed) {
        // リストから削除する
        allWindows.remove(closed);
        // mediatorMapから削除する
        mediatorMap.remove(closed.getFrame());
    }
    
    public static boolean contains(WindowSupport toCheck) {
        return allWindows.contains(toCheck);
    }
    
    // プライベートコンストラクタ
    private WindowSupport(JFrame frame, JMenuBar menuBar, JMenu windowMenu,
            Action windowAction) {
        this.frame = frame;
        this.menuBar = menuBar;
        this.windowMenu = windowMenu;
        this.windowAction = windowAction;
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
}
