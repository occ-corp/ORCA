package open.dolphin.impl.pacsviewer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import open.dolphin.client.ClientContext;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.DicomImageEntry;
import open.dolphin.util.ImageTool;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * DicomViewer.java
 *
 * PACSから取得した画像を閲覧してみる
 *
 * @author masuda, Masuda Naika
 */
public class DicomViewer {

    private JFrame frame;
    private ImagePanel imagePanel;
    private JTable thumbnailTable;
    private JScrollPane thumbnailScrollPane;
    private JButton resetBtn;
    private JButton copyBtn;
    private JCheckBox showInfoCb;
    private JRadioButton moveBtn;
    private JRadioButton zoomBtn;
    private JToggleButton measureBtn;
    private JToggleButton gammaBtn;
    private JLabel studyInfoLbl;
    private JLabel statusLbl;
    private JSlider slider;
    private JLabel sliderValue;
    private ExecutorService exec;
    private ThumbnailTableModel thumbnailTableModel;
    private static final int MAX_IMAGE_SIZE = 120;
    private static final int CELL_WIDTH_MARGIN = 20;
    private static final int CELL_HEIGHT_MARGIN = 20;
    private int cellWidth = MAX_IMAGE_SIZE + CELL_WIDTH_MARGIN;
    private int cellHeight = MAX_IMAGE_SIZE + CELL_HEIGHT_MARGIN;
    private int columnCount = 1;
    private int index = 0;
    private final static DecimalFormat frmt = new DecimalFormat("0.0");
    private final static DecimalFormat frmt1 = new DecimalFormat("0.00");
    private final static double gammaStep = 0.01;
    private final static double gammaMin = 0.5;
    private final static double gammaMax = 2.0;
    private final static double gammaDefault = 1.0;

    public DicomViewer() {
        initComponents();
        exec = Executors.newSingleThreadExecutor();
    }
    
    private void exit() {
        
        // Frameを閉じるときにPreferrenceに保存する
        Project.setDouble(MiscSettingPanel.PACS_VIEWER_GAMMA, getSliderGamma());
        boolean b = showInfoCb.isSelected();
        Project.setBoolean(MiscSettingPanel.PACS_SHOW_IMAGEINFO, b);
        
        thumbnailTableModel = null;
        frame = null;
        
        try {
            exec.shutdown();
            if (!exec.awaitTermination(10, TimeUnit.MILLISECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException ex) {
            exec.shutdownNow();
        } catch (NullPointerException ex) {
        }
        exec = null;
    }
    
    private void initComponents() {

        frame = new JFrame();
        ClientContext.setDolphinIcon(frame);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                exit();
            }
        });
        
        String title = ClientContext.getFrameTitle("Dicom Viewer, Masuda Naika");
        frame.setTitle(title);
        frame.setPreferredSize(new Dimension(640, 480));
        imagePanel = new ImagePanel();
        thumbnailTable = new JTable();
        frame.setLayout(new BorderLayout());

        measureBtn = new JToggleButton("計測");
        resetBtn = new JButton("リセット");
        copyBtn = new JButton("コピー");
        moveBtn = new JRadioButton("前後画像");
        zoomBtn = new JRadioButton("ズーム");
        showInfoCb = new JCheckBox("画像情報");
        statusLbl = new JLabel("OpenDolphin 1.4m");
        studyInfoLbl = new JLabel("Study Info.");
        gammaBtn = new JToggleButton("γ");
        // ガンマ係数スライダの設定
        double d = Project.getDouble(MiscSettingPanel.PACS_VIEWER_GAMMA, MiscSettingPanel.DEFAULT_PACS_GAMMA);
        int sliderMax = (int) ((gammaMax - gammaMin) / gammaStep);
        slider = new JSlider(0, sliderMax);
        JLabel lblSliderLeft = new JLabel(frmt.format(gammaMin));
        JLabel lblSliderRight = new JLabel(frmt.format(gammaMax));
        sliderValue = new JLabel(frmt1.format(d));
        int pos = (int) ((d - gammaMin) / gammaStep);
        slider.setValue(pos);
        imagePanel.setGamma(d);

        // ボタンのパネル
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        //panel.add(new JLabel(ClientContext.getImageIcon("dcm4che.gif")));
        panel.add(gammaBtn);
        panel.add(lblSliderLeft);
        panel.add(slider);
        panel.add(lblSliderRight);
        panel.add(sliderValue);
        panel.add(measureBtn);
        panel.add(copyBtn);
        panel.add(resetBtn);
        panel.add(showInfoCb);
        ButtonGroup group = new ButtonGroup();
        group.add(moveBtn);
        group.add(zoomBtn);
        moveBtn.setSelected(true);
        panel.add(new JLabel(" ホイール:"));
        panel.add(moveBtn);
        panel.add(zoomBtn);

        frame.add(panel, BorderLayout.NORTH);
        // ボタン類の設定
        copyBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                copyImage();
            }
        });
        resetBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                imagePanel.resetImage();
            }
        });
        showInfoCb.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean b = showInfoCb.isSelected();
                imagePanel.setShowInfo(b);
            }
        });
        slider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                double d = getSliderGamma();
                sliderValue.setText(frmt1.format(d));
                imagePanel.setGamma(d);
            }
        });
        gammaBtn.setSelected(true);
        gammaBtn.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                if (gammaBtn.isSelected()) {
                    double d = getSliderGamma();
                    sliderValue.setText(frmt1.format(d));
                    imagePanel.setGamma(d);
                    slider.setEnabled(true);
                } else {
                    sliderValue.setText(frmt1.format(gammaDefault));
                    imagePanel.setGamma(gammaDefault);
                    slider.setEnabled(false);
                }
            }
        });

        // イメージ表示パネル
        frame.add(imagePanel, BorderLayout.CENTER);

        // サムネイルパネル
        thumbnailTableModel = new ThumbnailTableModel(columnCount);
        thumbnailTable.setModel(thumbnailTableModel);
        thumbnailTable.setTableHeader(null);
        prepareTable(thumbnailTable);
        // サムネイルはScrollPaneに入れる
        thumbnailScrollPane = new JScrollPane(thumbnailTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        int pWidth = (int) thumbnailTable.getPreferredSize().getWidth() + 20;
        int pHeight = (int) thumbnailTable.getPreferredSize().getHeight();
        thumbnailScrollPane.setPreferredSize(new Dimension(pWidth, pHeight));
        frame.add(thumbnailScrollPane, BorderLayout.WEST);

        // 情報パネル
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(studyInfoLbl);
        panel.add(statusLbl);
        frame.add(panel, BorderLayout.SOUTH);

        boolean b = Project.getBoolean(MiscSettingPanel.PACS_SHOW_IMAGEINFO, MiscSettingPanel.DEFAULT_PACS_SHOW_IMAGEINFO);
        imagePanel.setShowInfo(b);
        showInfoCb.setSelected(b);
        ComponentMemory cm = new ComponentMemory(frame, new Point(100, 100), frame.getPreferredSize(), DicomViewer.this);
        cm.setToPreferenceBounds();
    }

    private double getSliderGamma() {
        int pos = slider.getValue();
        return gammaStep * pos + gammaMin;
    }

    // 次の画像を表示
    private void nextImage() {
        if (index < thumbnailTableModel.getImageList().size() - 1) {
            ++index;
            setSelectedIndex();
        }
    }

    // 前の画像を表示
    private void prevImage() {
        if (index > 0) {
            --index;
            setSelectedIndex();
        }
    }

    // クリップボードに画像をコピー
    private void copyImage() {
        SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                BufferedImage buf = imagePanel.getSubImage();
                if (buf != null) {
                    ImageTool.copyToClipboard(buf);
                }
                return null;
            }
        };
        worker.execute();
    }

    // サムネイルで選択した画像を表示させる
    private void setSelectedIndex() {
        final int row = index / columnCount;
        final int col = index % columnCount;
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                thumbnailTable.setRowSelectionInterval(row, row);
                thumbnailTable.setColumnSelectionInterval(col, col);
                thumbnailTable.scrollRectToVisible(thumbnailTable.getCellRect(row, col, true));
            }
        });
    }

    // 入口
    public void enter(List<DicomImageEntry> list) {

        for (DicomImageEntry entry : list) {
            thumbnailTableModel.addImage(entry);
        }
        index = 0;
        frame.setVisible(true);
        showSelectedImage();
    }

    // imageTableにレンダラー等を設定する
    private void prepareTable(final JTable tbl) {

        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.setCellSelectionEnabled(true);
        tbl.setRowSelectionAllowed(true);
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // columnCountは１だけど。
        TableColumn column = null;
        for (int i = 0; i < columnCount; i++) {
            column = tbl.getColumnModel().getColumn(i);
            column.setPreferredWidth(cellWidth);
        }
        tbl.setRowHeight(cellHeight);

        // サムネイルテーブルのレンダラーを設定
        ThumbnailTableRenderer imageRenderer = new ThumbnailTableRenderer();
        imageRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tbl.setDefaultRenderer(java.lang.Object.class, imageRenderer);

        // サムネイルが選択されるとその画像を表示する
        ListSelectionModel m = tbl.getSelectionModel();
        m.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    int row = thumbnailTable.getSelectedRow();
                    int col = thumbnailTable.getSelectedColumn();
                    index = col + columnCount * row;
                    showSelectedImage();
                }
            }
        });
    }

    // 選択中の画像を設定する
    private void showSelectedImage() {

        exec.execute(new ShowSelectedImageTask(index));
    }

    // 選択中の画像をimagePanelに設定するタスク
    private class ShowSelectedImageTask implements Runnable {

        private int imageIndex;

        private ShowSelectedImageTask(final int index) {
            this.imageIndex = index;
        }

        @Override
        public void run() {

            int row = imageIndex % columnCount;
            int column = imageIndex / columnCount;
            DicomImageEntry entry = (DicomImageEntry) thumbnailTableModel.getValueAt(row, column);
            DicomObject object = entry.getDicomObject();
            boolean isCR = "CR".equals(object.getString(Tag.Modality));
            gammaBtn.setSelected(isCR);

            try {
                setStudyInfoLabel(object);
                BufferedImage image = ImageTool.getDicomImage(object);
                imagePanel.setPixelSpacing(object.getDoubles(Tag.PixelSpacing));
                imagePanel.setInfo(new DicomImageInfo(object));
                imagePanel.setImage(image);
            } catch (IOException ex) {
            }
        }
    }

    // study informationを表示する
    private void setStudyInfoLabel(DicomObject object) {
        StringBuilder sb = new StringBuilder();
        sb.append(object.getString(Tag.PatientName));
        sb.append(" / ");
        sb.append(object.getString(Tag.StudyDate));
        sb.append(" / ");
        sb.append(object.getString(Tag.SOPInstanceUID));
        statusLbl.setText(sb.toString());
    }

    // 移動・拡大を可能にした画像表示用のパネル
    private class ImagePanel extends JPanel {

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

        private ImagePanel() {
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
        private void setImage(BufferedImage image) {
            this.image = image;
            resetImage2();
            repaint();
        }

        // 移動・拡大を初期値に戻し、計測を消去。
        private void resetImage() {
            windowWidth = defaultWW;
            windowLevel = defaultWL;
            setLUT();
            resetImage2();
        }

        // 移動・拡大を初期値に戻し、計測を消去。Window width/levelは変更しない
        private void resetImage2() {
            basePoint = new Point(0, 0);
            pow = 0;
            measure.clear();
            setScale();
            setAffineTransform();
            repaint();
        }

        // 画像情報を設定する
        private void setInfo(DicomImageInfo info) {
            this.info = info;
        }

        // 画像情報を表示するかのフラグ
        private void setShowInfo(boolean b) {
            showInfo = b;
            repaint();
        }

        // 画像のPixel spacingを設定する
        private void setPixelSpacing(double[] pixelSpacing) {
            if (pixelSpacing != null) {
                pixelSpacingX = pixelSpacing[0];
                pixelSpacingY = pixelSpacing[1];
            } else {
                pixelSpacingX = 0;
                pixelSpacingY = 0;
            }
        }
        // ガンマ係数を設定する
        private void setGamma(double d){
            gamma = d;
            setLUT();
            repaint();
        }
        private double getGamma() {
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
        private BufferedImage getSubImage() {
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
                oldBaseP= new Point();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int count = e.getWheelRotation();
                if (zoomBtn.isSelected()) {
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
                        nextImage();
                    } else {
                        prevImage();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mouseButton = e.getButton();
                switch (mouseButton) {
                    case MouseEvent.BUTTON1:
                        if (!measureBtn.isSelected()) {
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
                        boolean b = zoomBtn.isSelected();
                        zoomBtn.setSelected(!b);
                        moveBtn.setSelected(b);
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
                        if (!measureBtn.isSelected()) {
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

    // 画像情報のクラス
    private static class DicomImageInfo {

        private String institutionName;
        private String patientID;
        private String patientName;
        private String studyDate;
        private String seriesNumber;
        private String instanceNumber;
        private String patientAgeSex;

        private DicomImageInfo(DicomObject obj){
            institutionName = nz(obj.getString(Tag.InstitutionName));
            patientID = nz(obj.getString(Tag.PatientID));
            patientAgeSex = nz(obj.getString(Tag.PatientAge)) + " " + nz(obj.getString(Tag.PatientSex));
            patientName = nz(obj.getString(Tag.PatientName)).replace("^", " ");
            studyDate = nz(obj.getString(Tag.StudyDate));
            seriesNumber = nz(obj.getString(Tag.SeriesNumber));
            instanceNumber = nz(obj.getString(Tag.InstanceNumber));
        }
        private String nz(String str) {
            return (str == null) ? "" : str;
        }
        private String getInstitutionName() {
            return institutionName;
        }
        private String getPatientID() {
            return patientID;
        }
        private String getPatientName() {
            return patientName;
        }
        private String getStudyDate() {
            return studyDate;
        }
        private String getSeriesNumber() {
            return seriesNumber;
        }
        private String getInstanceNumber() {
            return instanceNumber;
        }
        private String getPatientAgeSex() {
            return patientAgeSex;
        }
    }
}
