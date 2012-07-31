package open.dolphin.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 *
 * @author pns
 * @author modified by masuda, Masuda Naika
 */
public class FindAndView {

    private static final Color SELECTED_COLOR = new Color(255, 180, 66); //カーソルがある部分の色
    private static final String SELECTED_COLOR_HEX = "#FFB442";
    private static final Color FOUND_COLOR = new Color(243, 255, 15); //黄色っぽい色
    private static final String FOUND_COLOR_HEX = "#F3FF0F";
    //private static final Color SELECTED_BORDER = new Color(255, 0, 153); //stampHolder の選択色
    private String searchText;
    private JPanel scrollerPanel; // 検索対象の Panel (KarteDocumentViewer からもってくる
    private SimpleAttributeSet foundAttr = new SimpleAttributeSet(); // 見つかった
    private SimpleAttributeSet onCursorAttr = new SimpleAttributeSet(); // 現在いるところ
    private SimpleAttributeSet defaultAttr = new SimpleAttributeSet(); // もともとの背景色（panelに応じて変化）

    // StampHolder にマークするためのタグ
    private static final String FONT_END = "</font>";
    private static final String FONT_FOUND = "<font style=\"background-color:" + FOUND_COLOR_HEX + "\">";
    private static final String FONT_SELECTED = "<font style=\"background-color:" + SELECTED_COLOR_HEX + "\">";

    private List<FindDataModel> findDataList;
    private int row;    // findDataListの現在のrow

    private static final String lineSeparator = System.getProperty("line.separator");


    public FindAndView() {
        foundAttr.addAttribute(StyleConstants.Background, FOUND_COLOR);
        onCursorAttr.addAttribute(StyleConstants.Background, SELECTED_COLOR);
    }

    /**
     * findFirst 検索対象のパネルをスキャンして，検索結果を positions データベースに入れる
     * さらに最初に見つかった部分を表示する
     * @param text
     * @param soaIsOn
     * @param pIsOn
     * @param panel
     */
    @SuppressWarnings("unchecked")
    public void showFirst(String text, boolean soaIsOn, boolean pIsOn, JPanel panel) {

        searchText = text;
        scrollerPanel = panel;
        findDataList = new ArrayList<FindDataModel>();
        Component cp;

        cp = panel.getComponent(0);
        KartePanel kartePanel = (KartePanel) cp;
        defaultAttr.addAttribute(StyleConstants.Background, kartePanel.getSoaTextPane().getBackground());

        int kpCount = panel.getComponentCount(); // panel に組み込まれている kartePanel の数
        int kpHeight = 0; // 後でソートするために，scrollerPanel 上の y 座標を記録するとき使う

        // 前回検索のマーキング全部クリアする
        clearMarking(panel);
        int kpHeight1 = 0;

        for (int i = 0; i < kpCount; i++) {
            try {
                cp = panel.getComponent(i);

                kartePanel = (KartePanel) cp;
                JTextPane soaPane = kartePanel.getSoaTextPane();
                JTextPane pPane = kartePanel.getPTextPane();
                kpHeight1 = kartePanel.getHeight();

                // soa Pane の text 検索 --------------------------------------------
                if (soaIsOn) {
                    // Windows では改行コードを変換しないと位置がずれる!!
                    String str = soaPane.getText().replace(lineSeparator, "\n");
                    Pattern p = Pattern.compile(text);
                    Matcher m = p.matcher(str);

                    // もし見つかったら，マーキングと positions データベース登録
                    while (m.find()) {
                        //　見つかったテキストに，foundAttr をセットして，見つかった位置の y 座標をfindDataListに入れる
                        int pos = m.start();
                        int len = m.end() - pos;
                        setFoundAttr(soaPane, pos, len);
                        int y = kpHeight + soaPane.modelToView(pos).y;
                        findDataList.add(new FindDataModel(y, soaPane, pos, len, null));
                    }
                }

                // pPane の text, stamp 検索 ----------------------------------------
                if (pIsOn && pPane != null) {

                    // Windows では改行コードを変換しないと位置がずれる!!
                    String str = pPane.getText().replace(lineSeparator, "\n");
                    Pattern p = Pattern.compile(text);
                    Matcher m = p.matcher(str);
                    // もし見つかったら，マーキングと positions データベース登録
                    while (m.find()) {
                        //　見つかったテキストに，foundAttr をセットして，見つかった位置の y 座標をfindDataListに入れる
                        int pos = m.start();
                        int len = m.end() - pos;
                        setFoundAttr(pPane, pos, len);
                        int y = kpHeight + pPane.modelToView(pos).y;
                        findDataList.add(new FindDataModel(y, pPane, pos, len, null));
                    }

                    // 次に stamp 検索
                    KarteStyledDocument kd = (KarteStyledDocument) pPane.getStyledDocument();
                    List<StampHolder> list = kd.getStampHolders();
                    for (StampHolder sh : list) {
                        String stampText = sh.getText();
                        //StampHolderをgetTextしても、改行は\nなのでおｋ
                        //タグ除去してから調べることにしてみた。masuda
                        stampText = stampText.replaceAll("<.+?>", "");   //タグ除去
                        p = Pattern.compile(text);
                        m = p.matcher(stampText);
                        while (m.find()) {
                            setFoundAttr(sh);
                            int y;
                            y = kpHeight + pPane.modelToView(sh.getStartPos()).y;
                            findDataList.add(new FindDataModel(y, pPane, sh.getStartPos(), 0, sh));
                            break;
                        }
                    }
                }
            } catch (BadLocationException ex) {
                System.out.println(ex);
            }
            kpHeight = kpHeight + kpHeight1; // kartePanel の高さ分だけずらす
        }

        if (!findDataList.isEmpty()) {
            row = 0;
            Collections.sort(findDataList);
            FindDataModel model = findDataList.get(row);

            if (model.getStampHolder() == null){
                setOnCursorAttr(model.getPane(), model.getStartPos(), model.getLength());
            } else {
                setOnCursorAttr(model.getStampHolder());
            }
             scrollToCenter(panel, model.getPane(), model.getStartPos());
        } else {
            showNotFoundDialog("検索", "がみつかりません");
        }
    }

    /**
     * 検索結果データベース(positions)を元に見つかった部分を表示する
     * @param panel
     * @param next
     */
    private void show(JPanel panel, boolean next) {

        // 検索結果が１つだったらすぐリターン
        if (findDataList.size() <= 1) {
            showNotFoundDialog(next ? "次を検索" : "前を検索", "はこれだけです");
            return;
        }

        // scrollerPanel が変化していなければ次を探す。変化していたらクリア。
        if (panel == scrollerPanel) {

            // 次の検索の前に onCursorAttr を foundAttr に戻す
            FindDataModel model = findDataList.get(row);
            if (model.getStampHolder() == null) {
                setFoundAttr(model.getPane(), model.getStartPos(), model.getLength());
            } else {
                setFoundAttr(model.getStampHolder());
            }

            // findNext or findPrevious
            if (next) {
                row++;
                if (row == findDataList.size()) {
                    row = 0;
                    if (showConfirmDialog("文書の最後まで検索しました。最初からもう一度検索しますか？") == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
            } else {
                row --;
                if (row < 0) {
                    row = findDataList.size() - 1;
                    if (showConfirmDialog("文書の最初に戻りました。最後からもう一度検索しますか？") == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
            }

            // 次の見つかった部分を表示して onCursorAttr セット
            model = findDataList.get(row);
            if (model.getStampHolder() == null) {
                setOnCursorAttr(model.getPane(), model.getStartPos(), model.getLength());
            } else {
                setOnCursorAttr(model.getStampHolder());
            }
            scrollToCenter(panel, model.getPane(), model.getStartPos());

        } else {
            // scroller が変わってたら，マーキングを全てクリアする
            clearMarking(panel);
        }
    }

    /**
     * 検索文字列をハイライト表示するための関連メソッド
     * @param pane
     * @param pos
     */
    private void setOnCursorAttr(JTextPane pane, int pos, int len) {
        pane.getStyledDocument().setCharacterAttributes(pos, len, onCursorAttr, false);
    }

    private void setOnCursorAttr(StampHolder sh) {
        setAttr(sh, FONT_SELECTED);
    }

    private void setFoundAttr(JTextPane pane, int pos, int len) {
        pane.getStyledDocument().setCharacterAttributes(pos, len, foundAttr, false);
    }

    private void setFoundAttr(StampHolder sh) {
        setAttr(sh, FONT_FOUND);
    }

    private void setAttr(StampHolder sh, String fontTag) {
        removeAttr(sh);
        Pattern p, p1;
        Matcher m, m1;
        boolean flag;
        StringBuffer sb = new StringBuffer();
        p = Pattern.compile(searchText);
        p1 = Pattern.compile("<.+?>", Pattern.DOTALL);
        m = p.matcher(sh.getText());
        m1 = p1.matcher(sh.getText());

        //まずはタグの位置をArrayListに記録する masuda
        //int [] tagPosition = new int [2];
        List<int[]> tagPositionArray = new ArrayList<int[]>();
        while (m1.find()) {
            int[] tagPosition = {m1.start(), m1.end() - 1};
            tagPositionArray.add(tagPosition);
        }

        while (m.find()) {
            flag = true;
            for (int[] tagPosition : tagPositionArray) {
                //検索文字がタグの中かどうかを判定する masuda
                if (tagPosition[0] <= m.start() && m.start() <= tagPosition[1]) {
                    flag = false;
                    break;
                }
                if (tagPosition[0] <= m.end() - 1 && m.end() - 1 <= tagPosition[1]) {
                    flag = false;
                    break;
                }
            }

            //タグの外なら文字修飾を施す masuda
            if (flag) {
                m.appendReplacement(sb, fontTag + m.group() + FONT_END);
            }
        }
        m.appendTail(sb);
        sh.setText(sb.toString());
    }
/*
    private void removeAttr(JTextPane pane, int pos, int len) {
        pane.getStyledDocument().setCharacterAttributes(pos, len, defaultAttr, false);
    }
*/
    private void removeAttr(StampHolder sh) {
        sh.setText(sh.getText().replace(FONT_FOUND, "").replace(FONT_SELECTED, "").replace(FONT_END, ""));
    }

    public void showNext(JPanel panel) {
        show(panel, true);
    }

    public void showPrevious(JPanel panel) {
        show(panel, false);
    }

    /**
     * 検索してカーソルがある部分を画面の中央に表示する
     * @param panel
     * @param pane
     * @param pos
     */
    private void scrollToCenter(JPanel panel, JTextPane pane, int pos) {
        try {
            Rectangle r = pane.modelToView(pos);
            int h = panel.getParent().getBounds().height; // viewport の高さ
            r.y = r.y - h / 2;
            r.height = h;
            pane.scrollRectToVisible(r);
        } catch (BadLocationException ex) {
            System.out.println(ex);
        }
    }

    /**
     * マーキングを全てクリアする
     * @param panel
     */
    private void clearMarking(JPanel panel) {

        for (Component cp : panel.getComponents()) {

            KartePanel kartePanel = (KartePanel) cp;

            JTextPane soaPane = kartePanel.getSoaTextPane();
            soaPane.getStyledDocument().setCharacterAttributes(0, soaPane.getText().length(), defaultAttr, false);

            JTextPane pPane = kartePanel.getPTextPane();
            pPane.getStyledDocument().setCharacterAttributes(0, pPane.getText().length(), defaultAttr, false);
            // stamp
            KarteStyledDocument kd = (KarteStyledDocument) pPane.getStyledDocument();
            List<StampHolder> list = kd.getStampHolders();
            for (StampHolder sh : list) {
                removeAttr(sh);
            }
        }
    }

    private int showConfirmDialog(String message) {
        return JOptionPane.showConfirmDialog( scrollerPanel.getRootPane(),
                message,
                "",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
    }

    private void showNotFoundDialog(String title, String message) {
        JOptionPane.showMessageDialog(scrollerPanel.getRootPane(),
                "「" + searchText + "」" + message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    private static class FindDataModel implements Comparable {
        private JTextPane textPane;
        private int posY;
        private StampHolder stampHolder;
        private int startPos;
        private int length;

        private FindDataModel(int y, JTextPane pane, int pos, int len, StampHolder sh){
            posY = y;
            textPane = pane;
            startPos = pos;
            length = len;
            stampHolder = sh;
        }

        private JTextPane getPane(){
            return textPane;
        }
        private StampHolder getStampHolder() {
            return stampHolder;
        }
         private int getPosY() {
            return posY;
        }
        private int getLength() {
            return length;
        }
        private int getStartPos() {
            return startPos;
        }

        @Override
        public int compareTo(Object o) {
            int test = ((FindDataModel) o).getPosY();
            if (test == posY){
                return 0;
            } else if (test > posY){
                return -1;
            }
            return 1;
        }
    }

}
