package open.dolphin.client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.text.*;

/**
 * 改行文字を表示するEditorKit
 *
 * @author masuda, Masuda Naika
 * http://terai.xrea.jp/Swing/ParagraphMark.html
 * http://abebas.sub.jp/java/JavaPrograming/01_Editor/011.html
 */
public class KartePanelEditorKit extends StyledEditorKit {

    private static final Color COLOR = Color.GRAY;
    private static final String CR = "↲";
    private static final String EOF = "◀";
    private static final int crMargin = 20;
    private boolean showCr;

    public KartePanelEditorKit(boolean showCr) {
        this.showCr = showCr;
    }

    @Override
    public ViewFactory getViewFactory() {
        if (showCr) {
            return new KartePanelEditorKit.VisibleCrViewFactory();
        } else {
            return new KartePanelEditorKit.InvisibleCrViewFactory();
        }
    }

    private class VisibleCrViewFactory implements ViewFactory {

        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new LabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new KartePanelEditorKit.MyParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new KartePanelEditorKit.MyComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
            return new LabelView(elem);
        }
    }

    private class InvisibleCrViewFactory implements ViewFactory {

        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new LabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new KartePanelEditorKit.MyComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
            return new LabelView(elem);
        }
    }

    private class MyComponentView extends ComponentView {

        private MyComponentView(Element elem) {
            super(elem);
        }

        // KartePane幅より広いスタンプの場合に直後の改行文字がwrapされないように
        // 厳密には正しくない
        @Override
        public float getPreferredSpan(int axis) {
            if (axis == View.X_AXIS && getComponent() instanceof StampHolder) {
                return getStampHolderSpanX();
                //return 0;
            }
            return super.getPreferredSpan(axis);
        }

        @Override
        public float getMaximumSpan(int axis) {

            if (axis == View.X_AXIS && getComponent() instanceof StampHolder) {
                return getStampHolderSpanX();
            }
            return super.getMaximumSpan(axis);
        }

        private float getStampHolderSpanX() {

            float span = super.getPreferredSpan(View.X_AXIS);
            int width = getComponent().getParent().getParent().getWidth() - crMargin;
            if (span > width && width > 0) {
                return width;
            }
            return span;
        }
    }

    private class MyParagraphView extends ParagraphView {

        private MyParagraphView(Element elem) {
            super(elem);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            super.paint(g, a);
            try {
                Shape paragraph = modelToView(getEndOffset(), a, Position.Bias.Backward);
                Rectangle r = (paragraph == null) ? a.getBounds() : paragraph.getBounds();
                int fontHeight = g.getFontMetrics().getHeight();
                Color old = g.getColor();
                g.setColor(COLOR);
                if (getEndOffset() != getDocument().getEndPosition().getOffset()) {
                    g.drawString(CR, r.x + 1, r.y + (r.height + fontHeight) / 2 - 2);
                } else {
                    g.drawString(EOF, r.x + 1, r.y + (r.height + fontHeight) / 2 - 2);
                }
                g.setColor(old);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
