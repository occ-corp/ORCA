package open.dolphin.client;

import java.awt.Color;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;

/**
 * StampRenderingHints
 *
 * @author Minagawa, Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class StampRenderingHints {

    private int fontSize = 12;
    private Color foreground;
    private Color background = Color.WHITE;
    private Color labelColor;
    private int border = 0;
    private int cellSpacing = 1;    //masuda 0 -> 1 to avoid unexpected line wrap
    private int cellPadding = 0;    //masuda 3 -> 0 to make slim

 //masuda^
    private static final String TR_S = "<TR>";
    private static final String TR_E = "</TR>";
    private static final String TD_S = "<TD>";
    private static final String TD_E = "</TD>";
    private static final String TD_NOWRAP = "<TD NOWRAP>";
    private static final String TD_NOWRAP_ALIGN_R = "<TD NOWRAP ALIGN=\"RIGHT\">";
    private static final String TD_NOWRAP_COLSPAN2= "<TD NOWRAP COLSPAN=\"2\">";
    private static final String TD_NOWRAP_COLSPAN2_ALIGN_R= "<TD NOWRAP COLSPAN=\"2\" ALIGN=\"RIGHT\">";
    private static final String TD_NOWRAP_COLSPAN3 = "<TD NOWRAP COLSPAN=\"3\">";
    private static final String TD_COLSPAN2 = "<TD COLSPAN=\"2\">";
    private static final String TD_COLSPAN3 = "<TD COLSPAN=\"3\">";
    private static final String END_OF_HTML = "</TABLE></BODY></HTML>";
    
    private static final StampRenderingHints instance;

    static {
        instance = new StampRenderingHints();
        int cp = Project.getInt("stampHolderCellPadding", 0);
        instance.setCellPadding(cp);
    }

    private StampRenderingHints() {
    }

    public static StampRenderingHints getInstance() {
        return instance;
    }
    
    public String getRegExpCommnentCode() {
        return ClaimConst.REGEXP_COMMENT_MED;
    }
    
    // velocityを使わずにレンダリングする。速度うｐを目指したが大したことなかったorz
    public String getHtmlText(ModuleModel stamp) {

        // entityを取得
        String entity =stamp.getModuleInfoBean().getEntity();
        
        // entityに応じてHTMLを作成
        if (IInfoModel.ENTITY_MED_ORDER.equals(entity)) {
            return getBundleMedHtml(stamp);
        } else if (IInfoModel.ENTITY_LABO_TEST.equals(entity) && Project.getBoolean("laboFold", true)) {
            return getLaboFoldHtml(stamp);
        } else {
            return getBundleDolphinHtml(stamp);
        }
    }
    
    private boolean isNewStamp(ModuleModel stamp) {
        String stampName = stamp.getModuleInfoBean().getStampName();
        return "新規スタンプ".equals(stampName) 
                || "エディタから発行...".equals(stampName) 
                || "チェックシート".equals(stampName);
    }
    
    private String getStartOfHtml() {
        
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><BODY><TT>");
        sb.append("<FONT SIZE=\"").append(getFontSize()).append("\" ");
        sb.append("COLOR=\"").append(getBackgroundAs16String()).append("\">");
        sb.append("<TABLE BORDER=\"").append(getBorder()).append("\" ");
        sb.append("CELLSPACING=\"").append(getCellSpacing()).append("\" ");
        sb.append("CELLPADDING=\"").append(getCellPadding()).append("\">");
        return sb.toString();
    }
    
    // 処方スタンプのhtmlを取得
    private String getBundleMedHtml(ModuleModel stamp) {
        
        BundleMed model = (BundleMed) stamp.getModel();
        StringBuilder sb = new StringBuilder();
        
        // 全体書式
        sb.append(getStartOfHtml());
        
        // タイトル
        sb.append("<TR BGCOLOR=\"").append(getLabelColorAs16String()).append("\">");
        if (isNewStamp(stamp)) {
            sb.append(TD_NOWRAP).append("RP) ").append(TD_E);
        } else {
            sb.append(TD_NOWRAP).append("RP) ").append(stamp.getModuleInfoBean().getStampName()).append(TD_E);
        }
        sb.append(TD_NOWRAP_COLSPAN2_ALIGN_R);
        sb.append(model.getMemo().replace("処方", "")).append("/").append(model.getClassCode()).append(TD_E);
        sb.append(TR_E);
        
        // 項目
        for (ClaimItem ci : model.getClaimItem()) {
            sb.append(TR_S);
            // コメントコードなら"・"と"x"は表示しない
            if (ci.getCode().matches(ClaimConst.REGEXP_COMMENT_MED)) {
                sb.append(TD_COLSPAN3).append(ci.getName()).append(TD_E);
            } else {
                sb.append(TD_S).append("・").append(ci.getName()).append(TD_E);
                sb.append(TD_NOWRAP_ALIGN_R).append(" x ").append(ci.getNumber()).append(TD_E);
                String unit = ci.getUnit();
                if (unit != null && !unit.isEmpty()) {
                    sb.append(TD_NOWRAP).append(" ").append(ci.getUnit().replace("カプセル", "Ｃ")).append(TD_E);
                } else {
                    sb.append(TD_NOWRAP).append(" ").append(TD_E);
                }
            }
            sb.append(TR_E);
        }
        
        // 用法
        sb.append(TR_S);
        sb.append(TD_COLSPAN3).append(model.getAdminDisplayString()).append(TD_E);
        sb.append(TR_E);
        
        // 用法メモ
        String memo = model.getAdminMemo();
        if (memo != null && !memo.isEmpty()) {
            sb.append(TR_S);
            sb.append(TD_COLSPAN3).append(memo).append(TD_E);
            sb.append(TR_E);
        }
        
        // End of StampHolder
        sb.append(END_OF_HTML);
        
        return sb.toString();
    }
    
    // 折り返しラボのhtmlを取得する
    private String getLaboFoldHtml(ModuleModel stamp) {

        BundleDolphin model = (BundleDolphin) stamp.getModel();
        StringBuilder sb = new StringBuilder();

        // 全体書式
        sb.append(getStartOfHtml());
        
        // タイトル
        sb.append("<TR BGCOLOR=\"").append(getLabelColorAs16String()).append("\">");
        if (isNewStamp(stamp)) {
            sb.append(TD_NOWRAP).append(model.getOrderName()).append(TD_E);
        } else {
            sb.append(TD_NOWRAP).append(model.getOrderName());
            sb.append("(").append(stamp.getModuleInfoBean().getStampName()).append(")").append(TD_E);
        }
        sb.append(TD_NOWRAP_COLSPAN2_ALIGN_R).append(model.getClassCode()).append(TD_E);
        sb.append(TR_E);

        // 項目
        sb.append(TR_S);
        sb.append(TD_COLSPAN3).append("・").append(model.getItemNames()).append(TD_E);
        sb.append(TR_E);

        // メモ
        String memo = model.getMemo();
        if (memo != null && !memo.isEmpty()) {
            sb.append(TR_S);
            sb.append(TD_COLSPAN3).append(memo).append(TD_E);
            sb.append(TR_E);
        }
        
        // バンドル数量
        String bundleNum = model.getBundleNumber();
        // 入院対応
        if (bundleNum != null && bundleNum.startsWith("/")) {
            sb.append(TR_S);
            sb.append(TD_COLSPAN3);
            sb.append("・施行日：").append(bundleNum.substring(1)).append("日");
            sb.append(TD_E);
            sb.append(TR_E);
        } else if (bundleNum != null && !"1".equals(bundleNum)) {
            sb.append(TR_S);
            sb.append(TD_S).append("・回数").append(TD_E);
            sb.append(TD_NOWRAP_ALIGN_R).append(" x ").append(bundleNum).append(TD_E);
            sb.append(TD_NOWRAP).append(" 回").append(TD_E);
            sb.append(TR_E);
        }
        
        // End of StampHolder
        sb.append(END_OF_HTML);
        
        return sb.toString();
    }
    
    // その他スタンプのhtmlを取得する
    private String getBundleDolphinHtml(ModuleModel stamp) {
        
        BundleDolphin model = (BundleDolphin) stamp.getModel();
        StringBuilder sb = new StringBuilder();
        
        // 全体書式
        sb.append(getStartOfHtml());
        
        // タイトル
        sb.append("<TR BGCOLOR=\"").append(getLabelColorAs16String()).append("\">");
        if (isNewStamp(stamp)) {
            sb.append(TD_NOWRAP).append(model.getOrderName()).append(TD_E);
        } else {
            sb.append(TD_NOWRAP).append(model.getOrderName());
            sb.append("(").append(stamp.getModuleInfoBean().getStampName()).append(")").append(TD_E);
        }
        sb.append(TD_NOWRAP_COLSPAN2_ALIGN_R).append(model.getClassCode()).append(TD_E);
        sb.append(TR_E);
        
        // 項目
        for (ClaimItem ci : model.getClaimItem()) {
            sb.append(TR_S);
            String num = ci.getNumber();
            if (num != null && !num.isEmpty()) {
                sb.append(TD_S).append("・").append(ci.getName()).append(TD_E);
                sb.append(TD_NOWRAP_ALIGN_R).append(" x ").append(ci.getNumber()).append(TD_E);
                String unit = ci.getUnit();
                if (unit != null && !unit.isEmpty()) {
                    sb.append(TD_NOWRAP).append(" ").append(ci.getUnit()).append(TD_E);
                } else {
                    sb.append(TD_NOWRAP).append(" ").append(TD_E);
                }
            } else {
                sb.append(TD_COLSPAN3).append("・").append(ci.getName()).append(TD_E);
            }
            sb.append(TR_E);
        }
        
        // メモ
        String memo = model.getMemo();
        if (memo != null && !memo.isEmpty()) {
            sb.append(TR_S);
            sb.append(TD_COLSPAN3).append(memo).append(TD_E);
            sb.append(TR_E);
        }
        
        // バンドル数量
        String bundleNum = model.getBundleNumber();
        if (bundleNum != null && bundleNum.startsWith("/")) {
            sb.append(TR_S);
            sb.append(TD_COLSPAN3);
            sb.append("・施行日：").append(bundleNum.substring(1)).append("日");
            sb.append(TD_E);
            sb.append(TR_E);
        } else  if (bundleNum != null && !"1".equals(bundleNum)) {
            sb.append(TR_S);
            sb.append(TD_S).append("・回数").append(TD_E);
            sb.append(TD_NOWRAP_ALIGN_R).append(" x ").append(bundleNum).append(TD_E);
            sb.append(TD_NOWRAP).append(" 回").append(TD_E);
            sb.append(TR_E);
        }
        
        // End of StampHolder
        sb.append(END_OF_HTML);
        
        return sb.toString();
    }
//masuda$

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public Color getForeground() {
        return foreground;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(Color labelColor) {
        this.labelColor = labelColor;
    }

    public int getBorder() {
        return border;
    }

    public void setBorder(int border) {
        this.border = border;
    }

    public int getCellPadding() {
        return cellPadding;
    }

    public void setCellPadding(int cellPadding) {
        this.cellPadding = cellPadding;
    }

    public int getCellSpacing() {
        return cellSpacing;
    }

    public void setCellSpacing(int cellSpacing) {
        this.cellSpacing = cellSpacing;
    }

    public String getForegroundAs16String() {
        if (getForeground() == null) {
            return "#000C9C";
        } else {
            int r = getForeground().getRed();
            int g = getForeground().getGreen();
            int b = getForeground().getBlue();
            StringBuilder sb = new StringBuilder();
            sb.append("#");
            sb.append(Integer.toHexString(r));
            sb.append(Integer.toHexString(g));
            sb.append(Integer.toHexString(b));
            return sb.toString();
        }
    }

    public String getBackgroundAs16String() {
        if (getBackground() == null) {
            return "#FFFFFF";
        } else {
            int r = getBackground().getRed();
            int g = getBackground().getGreen();
            int b = getBackground().getBlue();
            StringBuilder sb = new StringBuilder();
            sb.append("#");
            sb.append(Integer.toHexString(r));
            sb.append(Integer.toHexString(g));
            sb.append(Integer.toHexString(b));
            return sb.toString();
        }
    }

    public String getLabelColorAs16String() {
        if (getLabelColor() == null) {
            return "#FFCED9";
        } else {
            int r = getLabelColor().getRed();
            int g = getLabelColor().getGreen();
            int b = getLabelColor().getBlue();
            StringBuilder sb = new StringBuilder();
            sb.append("#");
            sb.append(Integer.toHexString(r));
            sb.append(Integer.toHexString(g));
            sb.append(Integer.toHexString(b));
            return sb.toString();
        }
    }
}
