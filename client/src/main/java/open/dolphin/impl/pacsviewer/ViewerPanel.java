package open.dolphin.impl.pacsviewer;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.awt.image.RasterFormatException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import open.dolphin.util.ImageTool;

/**
 * 移動・拡大を可能にした画像表示用のパネル
 *
 * @author masuda, Masauda Naika
 */
public class ViewerPanel extends JPanel {

    private AffineTransform af;
    private BufferedImage image;
    private Point basePoint;
    private LookupOp lookupOp;
    private byte[] lut;
    private double scale;
    private int pow;
    private double gamma;
    private DicomImageInfo info;
    private boolean showInfo;
    private List<PointPair> measure;
    private double pixelSpacingX;
    private double pixelSpacingY;
    private static final int maxDepth = 256;
    private static final int maxWW = maxDepth - 1;
    private static final int minWW = 1;
    private static final int maxWL = maxDepth - 1;
    private static final int minWL = -maxWL;
    private static final int defaultWW = maxDepth - 1;
    private static final int defaultWL = maxDepth / 2 - 1;
    private int windowWidth = defaultWW;
    private int windowLevel = defaultWL;
    private final BasicStroke STROKE_DOTTED =
            new BasicStroke(1,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f,
            new float[]{5f},
            0.0f);
    private final BasicStroke STROKE_PLAIN = new BasicStroke();
    private final static DecimalFormat frmt = new DecimalFormat("0.0");
    
    private DicomViewer viewer;

    public ViewerPanel(DicomViewer viewer) {
        this.viewer = viewer;
        measure = new ArrayList<PointPair>();
        af = new AffineTransform();
        MyMouseAdapter adapter = new MyMouseAdapter();
        this.addMouseWheelListener(adapter);
        this.addMouseListener(adapter);
        this.addMouseMotionListener(adapter);
    }

    // AffineTransformとLookup Tableを適応した画像を表示
    @Override
    public void paintComponent(Graphics g) {
        if (image == null) {
            return;
        }
        Graphics2D g2D = (Graphics2D) g;
        g2D.drawImage(lookupOp.filter(image, null), af, this);
        if (!measure.isEmpty()) {
            drawMeasureLine(g2D);
        }
        if (showInfo) {
            drawStudyInfo(g2D, basePoint);
        }
    }

    // Graphics2Dにstudy informationを書き込む
    private void drawStudyInfo(Graphics2D g2D, Point base) {
        g2D.setColor(Color.LIGHT_GRAY);
        g2D.setFont(new Font("Dialog", Font.PLAIN, 20));
        FontMetrics fm = g2D.getFontMetrics();
        int fontHeight = fm.getHeight();
        int x = (int) base.getX() + 10;
        int y = (int) base.getY();
        g2D.drawString(info.getInstitutionName(), x, y += fontHeight);
        g2D.drawString(buildString(new String[]{
                    info.getPatientID(), " ",
                    info.getPatientName(), " ",
                    info.getPatientAgeSex()
                }), x, y += fontHeight);
        g2D.drawString(info.getStudyDate(), x, y += fontHeight);
        g2D.drawString(buildString(new String[]{
                    "Se:", info.getSeriesNumber(),
                    " Im:", info.getInstanceNumber()
                }), x, y += fontHeight);
        g2D.drawString(buildString(new String[]{
                    "W:", String.valueOf(windowWidth),
                    " L:", String.valueOf(windowLevel)
                }), x, y += fontHeight);
    }

    private String buildString(String[] msgs) {
        StringBuilder sb = new StringBuilder();
        for (String msg : msgs) {
            sb.append(msg);
        }
        return sb.toString();
    }

    // 計測線を描画する
    private void drawMeasureLine(Graphics2D g2D) {

        g2D.setColor(Color.MAGENTA);
        for (PointPair pair : measure) {
            Point2D s = af.transform(pair.getStartPoint(), null);
            Point2D e = af.transform(pair.getEndPoint(), null);
            // ２点間に波線を描画
            g2D.setStroke(STROKE_DOTTED);
            g2D.draw(new Line2D.Double(s, e));
            // 両端に十字を描画
            final int len = 3;
            g2D.setStroke(STROKE_PLAIN);
            g2D.draw(new Line2D.Double(s.getX() - len, s.getY(), s.getX() + len, s.getY()));
            g2D.draw(new Line2D.Double(s.getX(), s.getY() - len, s.getX(), s.getY() + len));
            g2D.draw(new Line2D.Double(e.getX() - len, e.getY(), e.getX() + len, e.getY()));
            g2D.draw(new Line2D.Double(e.getX(), e.getY() - len, e.getX(), e.getY() + len));
            // 中心付近に距離を描画
            g2D.drawString(pair.getDistance(),
                    (int) (s.getX() + e.getX()) / 2,
                    (int) (s.getY() + e.getY()) / 2 - 4);
        }
    }

    // このパネルにimageを設定する
    public void setImage(BufferedImage image) {
        this.image = image;
        resetImage2();
        repaint();
    }

    // 移動・拡大を初期値に戻し、計測を消去。
    public void resetImage() {
        windowWidth = defaultWW;
        windowLevel = defaultWL;
        setLUT();
        resetImage2();
    }

    // 移動・拡大を初期値に戻し、計測を消去。Window width/levelは変更しない
    public void resetImage2() {
        basePoint = new Point(0, 0);
        pow = 0;
        measure.clear();
        setScale();
        setAffineTransform();
        repaint();
    }

    // 画像情報を設定する
    public void setInfo(DicomImageInfo info) {
        this.info = info;
    }

    // 画像情報を表示するかのフラグ
    public void setShowInfo(boolean b) {
        showInfo = b;
        repaint();
    }

    // 画像のPixel spacingを設定する
    public void setPixelSpacing(double[] pixelSpacing) {
        if (pixelSpacing != null) {
            pixelSpacingX = pixelSpacing[0];
            pixelSpacingY = pixelSpacing[1];
        } else {
            pixelSpacingX = 0;
            pixelSpacingY = 0;
        }
    }
    // ガンマ係数を設定する

    public void setGamma(double d) {
        gamma = d;
        setLUT();
        repaint();
    }

    public double getGamma() {
        return gamma;
    }

    // 拡大率を設定する
    private void setScale() {
        double panelWidth = getWidth();
        double panelHeight = getHeight();
        // スケール計算
        double sx = (panelWidth / image.getWidth());
        double sy = (panelHeight / image.getHeight());
        scale = Math.min(sx, sy) * Math.pow(4, (double) pow / 10);
    }

    // 現在のbasePointとscaleに対応したAffineTransformを作成する
    private void setAffineTransform() {
        af.setToTranslation(basePoint.getX(), basePoint.getY());
        af.scale(scale, scale);
    }

    // Window Width/Levelとガンマ値に応じたLUTを作成する。
    private void setLUT() {
        lut = new byte[maxDepth];
        for (int i = 0; i < maxDepth; ++i) {
            double d1;
            if (i <= windowLevel - 0.5 - (windowWidth - 1) / 2) {
                d1 = minWW;
            } else if (i > windowLevel - 0.5 + (windowWidth - 1) / 2) {
                d1 = maxWW;
            } else {
                d1 = ((i - (windowLevel - 0.5)) / (windowWidth - 1) + 0.5) * (maxWW - minWW) + minWW;
            }
            double d2 = (maxDepth - 1) * Math.pow(d1 / (maxDepth - 1), 1 / gamma);
            lut[i] = (byte) d2;
        }
        lookupOp = new LookupOp(new ByteLookupTable(0, lut), null);
    }

    // コピー用のBufferedImageを作成する
    public BufferedImage getSubImage() {
        try {
            // このパネルの表示内容を取得
            BufferedImage buf = ImageTool.getImageFromComponent(this);
            // 表示されている画像の大きさ
            double imageWidth = image.getWidth() * scale;
            double imageHeight = image.getHeight() * scale;
            // パネルとの重なりを取得する
            Rectangle panelRect = new Rectangle(buf.getWidth(), buf.getHeight());
            Area panelArea = new Area(panelRect);
            Rectangle imgRect = new Rectangle((int) basePoint.getX(), (int) basePoint.getY(), (int) imageWidth, (int) imageHeight);
            Area imgArea = new Area(imgRect);
            imgArea.intersect(panelArea);
            // イメージを切り取る
            Rectangle sub = imgArea.getBounds();
            BufferedImage ret = buf.getSubimage(sub.x, sub.y, sub.width, sub.height);
            return ret;
        } catch (RasterFormatException e) {
        }
        return null;
    }

    // basePointを、拡大中心点を基準にした位置に移動させる
    private void setBasePosAfterZoom(Point2D zoomPoint, double oldScale) {
        double zoomX = zoomPoint.getX();
        double zoomY = zoomPoint.getY();
        double zoomRatio = scale / oldScale;
        AffineTransform tmp = new AffineTransform();
        tmp.setToTranslation(zoomX, zoomY);
        tmp.scale(zoomRatio, zoomRatio);
        tmp.translate(-zoomX, -zoomY);
        tmp.transform(basePoint, basePoint);
    }

    // ドラッグで移動、マウスホイールで画像選択・拡大縮小を実装するMouseAdapter
    private class MyMouseAdapter extends MouseAdapter {

        private int mouseButton;
        private Point oldBaseP;
        private Point startP;
        private Point endP;
        private double oldWW;
        private double oldWL;

        private MyMouseAdapter() {
            oldBaseP = new Point();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int count = e.getWheelRotation();
            if (viewer.getZoomBtn().isSelected()) {
                int newValue = pow + count;
                if (-10 < newValue && newValue < 10) {
                    pow = newValue;
                    // 現在の拡大率を保存
                    double oldScale = scale;
                    // 新しい拡大率を設定
                    setScale();
                    // マウスカーソルが中心になるようにbasePointを移動させる
                    setBasePosAfterZoom(e.getPoint(), oldScale);
                    setAffineTransform();
                    repaint();
                }
            } else {
                if (count > 0) {
                    viewer.nextImage();
                } else {
                    viewer.prevImage();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mouseButton = e.getButton();
            switch (mouseButton) {
                case MouseEvent.BUTTON1:
                    if (!viewer.getMeasureBtn().isSelected()) {
                        // 左ドラッグ開始位置を保存する
                        oldBaseP.setLocation(basePoint);
                        startP = e.getPoint();
                    } else {
                        startP = e.getPoint();
                        try {
                            af.inverseTransform(startP, startP);
                        } catch (NoninvertibleTransformException ex) {
                        }
                        measure.add(0, new PointPair(startP, startP));
                    }
                    break;
                case MouseEvent.BUTTON2:
                    // 画像選択と画像拡大の切り替え
                    boolean b = viewer.getZoomBtn().isSelected();
                    viewer.getZoomBtn().setSelected(!b);
                    viewer.getMoveBtn().setSelected(b);
                    break;
                case MouseEvent.BUTTON3:
                    // Window width / level
                    oldWW = windowWidth;
                    oldWL = windowLevel;
                    startP = e.getPoint();
                    break;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            switch (mouseButton) {
                case MouseEvent.BUTTON1:
                    if (!viewer.getMeasureBtn().isSelected()) {
                        // 左ドラッグの処理
                        basePoint.setLocation(
                                e.getX() - startP.getX() + oldBaseP.getX(),
                                e.getY() - startP.getY() + oldBaseP.getY());
                        setAffineTransform();
                    } else {
                        endP = e.getPoint();
                        try {
                            af.inverseTransform(endP, endP);
                        } catch (NoninvertibleTransformException ex) {
                        }
                        measure.set(0, new PointPair(startP, endP));
                    }
                    repaint();
                    break;
                case MouseEvent.BUTTON3:
                    // Window width / level
                    windowWidth = (int) (oldWW + e.getX() - startP.getX());
                    if (windowWidth < minWW) {
                        windowWidth = minWW;
                    } else if (windowWidth > maxWW) {
                        windowWidth = maxWW;
                    }
                    windowLevel = (int) (oldWL + e.getY() - startP.getY());
                    if (windowLevel < minWL) {
                        windowLevel = minWL;
                    } else if (windowLevel > maxWL) {
                        windowLevel = maxWL;
                    }
                    setLUT();
                    repaint();
                    break;
            }
        }
    }

    // 計測２点を記憶するクラス
    private class PointPair {

        private Point start;
        private Point end;

        private PointPair(Point start, Point end) {
            this.start = new Point(start);
            this.end = new Point(end);
        }

        private Point getStartPoint() {
            return start;
        }

        private Point getEndPoint() {
            return end;
        }

        private String getDistance() {
            if (pixelSpacingX == 0 || pixelSpacingY == 0) {
                Double d = Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
                return frmt.format(d) + "px";
            }
            Double d = Math.sqrt(Math.pow((start.x - end.x) * pixelSpacingX, 2) + Math.pow((start.y - end.y) * pixelSpacingY, 2));
            if (d > 10) {
                return frmt.format(d / 10) + "cm";
            } else {
                return frmt.format(d) + "mm";
            }
        }
    }
}
