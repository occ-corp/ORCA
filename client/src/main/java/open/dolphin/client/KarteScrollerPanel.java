package open.dolphin.client;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;

/**
 * KarteScrollerPanel
 * KarteDocumentViewer用のpanel
 * 現状では横並びではSkipScrollもPageScrollも同じ
 *
 * @author masuda, Masuda Naika
 */
public class KarteScrollerPanel extends JPanel {

    // アニメーションスクロールをするMouseWheelAdapter
    private SkipScrollWheelAdapter scrollAdapter;
    // アニメーションスクロールのタイマー
    private javax.swing.Timer scrollTimer;
    // アニメーションスクロールのActionListener
    private AnimateScrollAction scrollAction;
    private static final int DIV = 10;  // Animated scrollの分割数
    private static final int INTERVAL = 5; // Animated scrollの実行間隔msec

    private static final String PAGE_UP = DefaultEditorKit.pageUpAction;
    private static final String PAGE_DOWN = DefaultEditorKit.pageDownAction;
    private static final String LEFT_ARROW = DefaultEditorKit.backwardAction;
    private static final String RIGHT_ARROW = DefaultEditorKit.forwardAction;
    private static final String UP_ARROW = DefaultEditorKit.upAction;
    private static final String DOWN_ARROW = DefaultEditorKit.downAction;

    private KarteDocumentViewer docViewer;

    private boolean vsc;
    private int mode;

    public KarteScrollerPanel(KarteDocumentViewer docViewer) {

        this.docViewer = docViewer;
        vsc = docViewer.isVsc();
        mode =  Project.getInt(MiscSettingPanel.KARTE_SCROLL_TYPE, MiscSettingPanel.DEFAULT_KARTE_SCROLL);

        // WheeleAdapterを設定する。
        scrollAdapter = new SkipScrollWheelAdapter();
        this.addMouseWheelListener(scrollAdapter);

        // アニメーションスクロールを設定
        scrollAction = new AnimateScrollAction();
        scrollTimer = new javax.swing.Timer(INTERVAL, scrollAction);
        // KarteScrollerPanelにキー移動アクションを設定する
        setKeyAction(KarteScrollerPanel.this);
    }

    private JScrollPane getScrollPane() {
        return docViewer.getScrollPane();
    }

    // キー移動アクションを設定する
    private void setKeyAction(JComponent comp) {

        InputMap imap = comp.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap amap = comp.getActionMap();

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), PAGE_UP);
        amap.put(PAGE_UP, new PageUpAction());
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), PAGE_DOWN);
        amap.put(PAGE_DOWN, new PageDownAction());
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), LEFT_ARROW);
        amap.put(LEFT_ARROW, new LeftArrowAction());
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), RIGHT_ARROW);
        amap.put(RIGHT_ARROW, new RightArrowAction());
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), UP_ARROW);
        amap.put(UP_ARROW, new UpArrowAction());
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), DOWN_ARROW);
        amap.put(DOWN_ARROW, new DownArrowAction());
    }

    public void dispose() {
        scrollTimer.stop();
        scrollTimer = null;
        scrollAdapter = null;
        // memory leak?
        removeAll();
    }

    // スクロールはKarteViewer毎にするMouseWheelListener
    private class SkipScrollWheelAdapter extends MouseAdapter {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {

            if (getScrollPane() == null) {
                return;
            }

            if (vsc) {
                boolean downward = e.getWheelRotation() > 0;
                switch (mode) {
                    case MiscSettingPanel.DEFAULT_KARTE_SCROLL:
                        unitScrollV(downward);
                        break;
                    case MiscSettingPanel.SKIP_KARTE_SCROLL:
                        skipScrollV(downward);
                        break;
                    case MiscSettingPanel.PAGE_KARTE_SCROLL:
                        pageScrollV(downward);
                        break;
                }
            } else {
                boolean rightward = e.getWheelRotation() > 0;
                switch (mode) {
                    case MiscSettingPanel.DEFAULT_KARTE_SCROLL:
                        unitScrollH(rightward);
                        break;
                    case MiscSettingPanel.SKIP_KARTE_SCROLL:
                        skipScrollH(rightward);
                        break;
                    case MiscSettingPanel.PAGE_KARTE_SCROLL:
                        pageScrollH(rightward);
                        break;
                }
            }
        }
    }

    // スクロールを滑らかに移動するアクション
    private class AnimateScrollAction implements ActionListener {

        private Point destPoint;
        private Point curPoint;
        private int i;

        private void moveTo(Point p) {

            JScrollPane scroller = getScrollPane();

            if (scroller == null) {
                return;
            }

            if (scrollTimer.isRunning() && p.equals(destPoint)) {
                return;
            }

            scrollTimer.stop();
            destPoint = p;
            curPoint = scroller.getViewport().getViewPosition();
            i = 0;
            scrollTimer.start();
        }

        private Point getDestPoint() {
            return destPoint;
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            JScrollPane scroller = getScrollPane();

            if (scroller == null) {
                scrollTimer.stop();
            }

            if (i == DIV) {
                // 指定回数繰り返したらタイマーをストップする
                scrollTimer.stop();
                scroller.getViewport().setViewPosition(destPoint);
            } else {
                // 底の変換なんてちょー久しぶりｗ
                int y = (int) (curPoint.y + (destPoint.y - curPoint.y) * Math.log(++i) / Math.log(DIV));
                int x = (int) (curPoint.x + (destPoint.x - curPoint.x) * Math.log(++i) / Math.log(DIV));
                scroller.getViewport().setViewPosition(new Point(x, y));
            }
        }
    }

    private class PageUpAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (vsc) {
                pageScrollV(false);
            } else {
                pageScrollH(false);
            }
        }
    }

    private class PageDownAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (vsc) {
                pageScrollV(true);
            } else {
                pageScrollH(true);
            }
        }
    }

    private class UpArrowAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (vsc) {
                if (mode == MiscSettingPanel.SKIP_KARTE_SCROLL) {
                    skipScrollV(false);
                } else {
                    unitScrollV(false);
                }
            } else {
                unitScrollV(false);
            }
        }
    }

    private class DownArrowAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (vsc) {
                if (mode == MiscSettingPanel.SKIP_KARTE_SCROLL) {
                    skipScrollV(true);
                } else {
                    unitScrollV(true);
                }
            } else {
                unitScrollV(true);
            }
        }
    }

    private class LeftArrowAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (vsc) {
                unitScrollH(false);
            } else {
                if (mode == MiscSettingPanel.SKIP_KARTE_SCROLL) {
                    skipScrollH(false);
                } else {
                    unitScrollH(false);
                }
            }
        }
    }

    private class RightArrowAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (vsc) {
                unitScrollH(true);
            } else {
                if (mode == MiscSettingPanel.SKIP_KARTE_SCROLL) {
                    skipScrollH(true);
                } else {
                    unitScrollH(true);
                }
            }
        }
    }

    // 現在のViewPortの位置を取得。timerがrunningならばdestPoint
    private Point getViewportPoint() {
        boolean timerRunning = scrollTimer.isRunning();

        Point p = timerRunning
                ? scrollAction.getDestPoint()
                : getScrollPane().getViewport().getViewPosition();
        return p;
    }

    // 縦並びのページスクロール
    private void pageScrollV(boolean downward) {

        JScrollPane scroller = getScrollPane();

        if (scroller == null) {
            return;
        }

        Point destPoint = getViewportPoint();
        destPoint.x = 0;

        int height = scroller.getViewport().getHeight();

        if (downward) {
            destPoint.y = Math.min(destPoint.y + height, this.getHeight());
        } else {
            destPoint.y = Math.max(destPoint.y - height, 0);
        }

        scrollAction.moveTo(destPoint);
    }

    // 横並びのページスクロール
    private void pageScrollH(boolean rightward) {

        JScrollPane scroller = getScrollPane();

        if (scroller == null) {
            return;
        }

        Point destPoint = getViewportPoint();
        destPoint.y = 0;

        int widht = scroller.getViewport().getWidth();

        if (rightward) {
            destPoint.x = Math.min(destPoint.x + widht, this.getWidth());
        } else {
            destPoint.x = Math.max(destPoint.x - widht, 0);
        }

        scrollAction.moveTo(destPoint);
    }

    // 縦並びのスキップスクロール
    private void skipScrollV(boolean downward) {

        JScrollPane scroller = getScrollPane();

        if (scroller == null) {
            return;
        }

        boolean timerRunning = scrollTimer.isRunning();
        Point p = getViewportPoint();

        // currentLocのComponentを取得する。
        Component comp = this.getComponentAt(p);
        // どのKarteViewerか調べる
        List<KarteViewer> viewerList = docViewer.getViewerList();
        KarteViewer viewer = null;
        for (KarteViewer kv : viewerList) {
            if (kv.getUI() == comp) {
                viewer = kv;
                break;
            }
        }
        if (viewer == null) {
            return; 
        }

        Rectangle rect = viewer.getUI().getBounds();
        int index = viewer.getIndex();

        // 下向きスクロールで、今のカルテの最下部が欠けて表示されているときは、unitScrollする
        Point top = rect.getLocation();
        Point bottom = new Point(top.x, top.y + rect.height - 1);

        if (downward && !scroller.getViewport().getViewRect().contains(bottom)) {
            unitScrollV(downward);
            return;
        }

        // 次に表示すべきKarteViewerのindexを得る
        if (downward) {
            index = Math.min(index + 1, viewerList.size() - 1);
        } else if (timerRunning || scroller.getViewport().getViewRect().contains(top)) {
            // Timerがrunningか、上向きでかつ今のカルテの先頭が表示されているなら前のKarteViewer
            // 表示されていないならば今のカルテの先頭から表示、indexは変更なし。
            index = Math.max(index - 1, 0);
        }

        // 次に表示すべきKarteViewerを取得
        KarteViewer kvNext = viewerList.get(index);
        // KarteViewerの位置を取得
        Point destPoint = kvNext.getUI().getLocation();

        // 次のKarteViewerの位置にViewportを移動
        scrollAction.moveTo(destPoint);
    }

    // 横並びのスキップスクロール。現状ページスクロールと同じ動作
    private void skipScrollH(boolean rightward) {

        JScrollPane scroller = getScrollPane();

        if (scroller == null) {
            return;
        }

        boolean timerRunning = scrollTimer.isRunning();
        Point p = getViewportPoint();

        // currentLocのComponentを取得する。
        Component comp = this.getComponentAt(p);
        // どのKarteViewerか調べる
        List<KarteViewer> viewerList = docViewer.getViewerList();
        KarteViewer viewer = null;
        for (KarteViewer kv : viewerList) {
            if (kv.getUI() == comp) {
                viewer = kv;
                break;
            }
        }
        if (viewer == null) {
            return;
        }

        int index = viewer.getIndex();
        Rectangle rect = viewer.getUI().getBounds();
        
        // 右向きスクロールで、今のカルテの最右部が欠けて表示されているときは、unitScrollする
        Point left = rect.getLocation();
        left.y = p.y;
        Point right = new Point(left.x + rect.width - 1, left.y);

        if (rightward && !scroller.getViewport().getViewRect().contains(right)) {
            unitScrollH(rightward);
            return;
        }

        // 次に表示すべきKarteViewerのindexを得る
        if (rightward) {
            index = Math.min(index + 1, viewerList.size() - 1);
        } else if (timerRunning || scroller.getViewport().getViewRect().contains(left)) {
            // Timerがrunningか、左向きでかつ今のカルテの先頭が表示されているなら前のKarteViewer
            // 表示されていないならば今のカルテの先頭から表示、indexは変更なし。
            index = Math.max(index - 1, 0);
        }

        // 次に表示すべきKarteViewerを取得
        KarteViewer kvNext = viewerList.get(index);
        // KarteViewerの位置を取得
        Point destPoint = kvNext.getUI().getLocation();

        // 次のKarteViewerの位置にViewportを移動
        scrollAction.moveTo(destPoint);
    }

    // 既定ユニット単位の縦スクロール
    private void unitScrollV(boolean downward) {

        JScrollPane scroller = getScrollPane();

        if (scroller == null) {
            return;
        }
        
        int unit = scroller.getVerticalScrollBar().getUnitIncrement();
        Point p = scroller.getViewport().getViewPosition();
        if (downward) {
            p.y = Math.min(p.y + unit, this.getHeight());
        } else {
            p.y = Math.max(p.y - unit, 0);
        }
        scroller.getViewport().setViewPosition(p);
    }

    // 既定ユニット単位の横スクロール
    private void unitScrollH(boolean rightward) {

        JScrollPane scroller = getScrollPane();

        if (scroller == null) {
            return;
        }

        int unit = scroller.getVerticalScrollBar().getUnitIncrement();
        Point p = scroller.getViewport().getViewPosition();
        if (rightward) {
            p.x = Math.min(p.x + unit, this.getWidth());
        } else {
            p.x = Math.max(p.x - unit, 0);
        }
        scroller.getViewport().setViewPosition(p);
    }

}
