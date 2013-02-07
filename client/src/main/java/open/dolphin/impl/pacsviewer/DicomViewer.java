package open.dolphin.impl.pacsviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
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
    private DicomViewerPanel viewerPanel;
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
    
    public JRadioButton getZoomBtn() {
        return zoomBtn;
    }
    public JRadioButton getMoveBtn() {
        return moveBtn;
    }
    public JToggleButton getMeasureBtn() {
        return measureBtn;
    }
    
    private void exit() {
        
        // Frameを閉じるときにPreferrenceに保存する
        Project.setDouble(MiscSettingPanel.PACS_VIEWER_GAMMA, getSliderGamma());
        boolean b = showInfoCb.isSelected();
        Project.setBoolean(MiscSettingPanel.PACS_SHOW_IMAGEINFO, b);
        
        // memory leak?
        thumbnailTableModel.clear();
        thumbnailTableModel = null;
        frame = null;
        
        // ViewerPanelを始末
        viewerPanel = null;
        
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
        viewerPanel = new DicomViewerPanel(DicomViewer.this);
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
        viewerPanel.setGamma(d);

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
                viewerPanel.resetImage();
            }
        });
        showInfoCb.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean b = showInfoCb.isSelected();
                viewerPanel.setShowInfo(b);
            }
        });
        slider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                double d = getSliderGamma();
                sliderValue.setText(frmt1.format(d));
                viewerPanel.setGamma(d);
            }
        });
        gammaBtn.setSelected(true);
        gammaBtn.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                if (gammaBtn.isSelected()) {
                    double d = getSliderGamma();
                    sliderValue.setText(frmt1.format(d));
                    viewerPanel.setGamma(d);
                    slider.setEnabled(true);
                } else {
                    sliderValue.setText(frmt1.format(gammaDefault));
                    viewerPanel.setGamma(gammaDefault);
                    slider.setEnabled(false);
                }
            }
        });

        // イメージ表示パネル
        frame.add(viewerPanel, BorderLayout.CENTER);

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
        viewerPanel.setShowInfo(b);
        showInfoCb.setSelected(b);
        ComponentMemory cm = new ComponentMemory(frame, new Point(100, 100), frame.getPreferredSize(), DicomViewer.this);
        cm.setToPreferenceBounds();
    }

    private double getSliderGamma() {
        int pos = slider.getValue();
        return gammaStep * pos + gammaMin;
    }

    // 次の画像を表示
    public void nextImage() {
        if (index < thumbnailTableModel.getImageList().size() - 1) {
            ++index;
            setSelectedIndex();
        }
    }

    // 前の画像を表示
    public void prevImage() {
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
                BufferedImage buf = viewerPanel.getSubImage();
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
        for (int i = 0; i < columnCount; i++) {
            TableColumn column = tbl.getColumnModel().getColumn(i);
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
            try {
            DicomImageEntry entry = (DicomImageEntry) thumbnailTableModel.getValueAt(row, column);
            DicomObject object = entry.getDicomObject();
            boolean isCR = "CR".equals(object.getString(Tag.Modality));
            gammaBtn.setSelected(isCR);
                setStudyInfoLabel(object);
                BufferedImage image = ImageTool.getDicomImage(object);
                viewerPanel.setPixelSpacing(object.getDoubles(Tag.PixelSpacing));
                viewerPanel.setInfo(new DicomImageInfo(object));
                viewerPanel.setImage(image);
            } catch (Exception ex) {
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

}
