package open.dolphin.client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.text.Position;
import open.dolphin.infomodel.ModuleModel;

/**
 * KartePane に Component　として挿入されるスタンプを保持スルクラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class StampHolder extends AbstractComponentHolder implements ComponentHolder {

    private static final Color FOREGROUND = new Color(20, 20, 140);
    private static final Color BACKGROUND = new Color(0, 0, 0, 0); // 透明
    private static final Color SELECTED_BORDER = new Color(255, 0, 153);
    private static final Color NON_SELECTED_BORDER = new Color(0, 0, 0, 0); // 透明
    private static final Border nonSelectedBorder = BorderFactory.createLineBorder(NON_SELECTED_BORDER);
    private static final Border selectedBorder = BorderFactory.createLineBorder(SELECTED_BORDER);
    private static final Color LBL_COLOR = new Color(0xFF, 0xCE, 0xD9);
    
    private Color foreGround = FOREGROUND;
    private Color background = BACKGROUND;
    
    private ModuleModel stamp;
    private KartePane kartePane;
    
    private StampRenderingHints hints;
    private StampHolderFunction function;

    private Position start;
    //private Position end; //endPositionはstart+1で代用

    private boolean selected;

    // StampRenderingHintsのlineSpacingを0にすると、ラベルの上部に隙間ができてしまう
    //　見栄えが悪いので線を追加描画する
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Color c = g.getColor();
        g.setColor(LBL_COLOR);
        int w = getWidth() - 2;
        int h = g.getFontMetrics().getHeight() + 2;
        g.drawLine(1, 1, w, 1);
        g.drawLine(1, h, w, h);
        g.setColor(c);
    }
    
    public StampHolder(KartePane kartePane, ModuleModel stamp) {
        super();
        function = StampHolderFunction.getInstance();
        function.setDeleteAction(StampHolder.this);
        
        this.kartePane = kartePane;
        setHints(StampRenderingHints.getInstance());
        setForeground(foreGround);
        setBackground(background);
        // 非選択状態では透明のLineBorder
        setBorder(nonSelectedBorder);
        setStamp(stamp);
    }

    /**
     * Popupメニューを表示する。
     */
    @Override
    public void mabeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            StampHolder sh = (StampHolder) e.getComponent();
            function.setSelectedStampHolder(sh);
            function.showPopupMenu(e.getPoint());
        }
    }

    /**
     * このスタンプホルダのKartePaneを返す。
     */
    @Override
    public KartePane getKartePane() {
        return kartePane;
    }
    
    /**
     * スタンプホルダのコンテントタイプを返す。
     */
    @Override
    public int getContentType() {
        return ComponentHolder.TT_STAMP;
    }
    
    /**
     * このホルダのモデルを返す。
     * @return
     */
    public ModuleModel getStamp() {
        return stamp;
    }
    
    /**
     * このホルダのモデルを設定する。
     * @param stamp
     */
    public void setStamp(ModuleModel stamp) {
        if (this.stamp!=stamp) {
            this.stamp = stamp;
        }
        function.setMyText(this);
    }
    
    public StampRenderingHints getHints() {
        return hints;
    }
    
    public void setHints(StampRenderingHints hints) {
        this.hints = hints;
    }
    
    /**
     * 選択されているかどうかを返す。
     * @return 選択されている時 true
     */
    @Override
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * 選択属性を設定する。
     * @param selected 選択の時 true
     */
    @Override
    public void setSelected(boolean selected) {
//masuda^
        if (selected) {
            this.setBorder(selectedBorder);
            this.selected = true;
        } else {
            this.setBorder(nonSelectedBorder);
            this.selected = false;
        }
//masuda
    }
    
    /**
     * KartePane でこのスタンプがダブルクリックされた時コールされる。
     * StampEditor を開いてこのスタンプを編集する。
     */
    @Override
    public void edit() {
        function.setSelectedStampHolder(this);
        function.edit();
    }
    
    /**
     * エディタで編集した値を受け取り内容を表示する。
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        function.setSelectedStampHolder(this);
        ModuleModel[] newStamps = (ModuleModel[]) e.getNewValue();
        ModuleModel[] oldValue = (ModuleModel[]) e.getOldValue();
        function.setNewValue(newStamps, oldValue);
    }
    
    /**
     * スタンプの内容を置き換える。
     * @param newStamp
     */
    public void importStamp(ModuleModel newStamp) {
        setStamp(newStamp);
        kartePane.setDirty(true);
        kartePane.getTextPane().validate();
        kartePane.getTextPane().repaint();
    }
    
    /**
     * TextPane内での開始と終了ポジションを保存する。
     */
    @Override
    public void setEntry(Position start, Position end) {
        this.start = start;
        //this.end = end;
    }
    
    /**
     * 開始ポジションを返す。
     */
    @Override
    public int getStartPos() {
        return start.getOffset();
    }
    
    /**
     * 終了ポジションを返す。
     */
    @Override
    public int getEndPos() {
        //return end.getOffset();
        int ret = getStartPos() + 1;
        return ret;
    }
}
