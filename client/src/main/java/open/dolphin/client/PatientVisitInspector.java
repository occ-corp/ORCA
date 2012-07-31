package open.dolphin.client;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import open.dolphin.infomodel.AppointmentModel;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.SimpleDate;

/**
 *
 * @author Kazushi Minagawa.
 * 
 * Modified my masuda, Masuda Naika
 */
public class PatientVisitInspector {
    
    private CalendarCardPanel calendarCardPanel;

    private ChartImpl context;
    
//masuda^
    private static final String pvtEvent= ClientContext.getString("eventCode.pvt"); // PVT
    private static final HashMap<String, String> appoEventMap = new HashMap<String, String>();
    static {
        appoEventMap.put("再診", "EXAM_APPO2");
        appoEventMap.put("検体検査", "TEST2");
        appoEventMap.put("画像診断", "IMAGE2");
        appoEventMap.put("その他", "OTHER2");
    }

    public void setTitle() {
        calendarCardPanel.setBorder(BorderFactory.createTitledBorder("来院歴　桃⇒診察　橙⇒処方"));
    }
//masuda$
    
    /**
     * PatientVisitInspector を生成する。
     */
    public PatientVisitInspector(ChartImpl context) {
        this.context = context;
        initComponent();
        update();
    }

    /**
     * レイアウトパネルを返す。
     * @return レイアウトパネル
     */
    public JPanel getPanel() {
        return calendarCardPanel;
    }

    /**
     * GUIコンポーネントを初期化する。
     */
    private void initComponent() {
        calendarCardPanel = new CalendarCardPanel(ClientContext.getEventColorTable());

//masuda^
        //calendarCardPanel.setCalendarRange(new int[]{-12, 0});
        // カレンダーで選択した日付のカルテを開くためにPropertyChangeListenerを追加
        calendarCardPanel.setUpBtnVisible(true);    // PatientVisitInspectorでは３か月表示を可能にする
        calendarCardPanel.addPropertyChangeListener(CalendarCardPanel.PICKED_DATE, new PropertyChangeListener(){

            @Override
            @SuppressWarnings("unchecked")
            public void propertyChange(PropertyChangeEvent e) {
                // 来院歴のカレンダで選択するとその日付のカルテを開く masuda
                SimpleDate sd = (SimpleDate) e.getNewValue();
                String pickedDate = SimpleDate.simpleDateToMmldate(sd);
                DocumentHistory docHistory = context.getDocumentHistory();
                List<DocInfoModel> docInfo = context.getKarte().getDocInfoList();
                for (DocInfoModel dim : docInfo) {
                    String karteDate = dim.getFirstConfirmDateTrimTime();
                    if (karteDate.equals(pickedDate)) {
                        int index = docInfo.indexOf(dim);
                        JTable docHistoryTable = ((DocumentHistoryView) docHistory.getPanel()).getTable();
                        docHistoryTable.setRowSelectionInterval(index, index);
                        docHistoryTable.scrollRectToVisible(docHistoryTable.getCellRect(index, 0, true));
                        break;
                    }
                }
            }
        });
//masuda$
    }

    private void update() {
        
//masuda^   来院歴はPVTではなくて文書があるかどうかで判断する様に変更

        List<DocInfoModel> docInfo = context.getKarte().getDocInfoList();
        List<SimpleDate> simpleDates = new ArrayList<SimpleDate>();

        if (docInfo != null && !docInfo.isEmpty()) {
            GregorianCalendar gc = new GregorianCalendar();
            for (DocInfoModel dim : docInfo) {
                gc.setTime(dim.getFirstConfirmDate());
                SimpleDate sd = new SimpleDate(gc);
                //処方のある日は色をつけて表示する
                if (dim.isHasRp()) {
                    sd.setEventCode("RP");          // 橙
                    sd.setText("処方");
                } else {
                    // 文書履歴のみのとき
                    sd.setEventCode(pvtEvent);      // 桃
                    sd.setText("診察");
                }
                simpleDates.add(sd);
            }
        }

        // 予約も設定
        List<AppointmentModel> appoList = context.getKarte().getAppointmentList();
        if (appoList != null && !appoList.isEmpty()) {
            GregorianCalendar gc = new GregorianCalendar();
            for (AppointmentModel appo : appoList) {
                gc.setTime(appo.getDate());
                SimpleDate sd = new SimpleDate(gc);
                String name = appo.getName();
                sd.setEventCode(appoEventMap.get(name));
                sd.setText("予約：" + name);
                simpleDates.add(sd);
            }
        }

        // CalendarCardに通知する
        if (!simpleDates.isEmpty()) {
            calendarCardPanel.setMarkList(simpleDates);
        }
/*
        // 来院歴を取り出す
        //List<String> latestVisit = (List<String>) context.getKarte().getEntryCollection("visit");
        List<String> latestVisit = context.getKarte().getPatientVisits();

        // 来院歴
        if (latestVisit != null && latestVisit.size() > 0) {
            ArrayList<SimpleDate> simpleDates = new ArrayList<SimpleDate>(latestVisit.size());
            for (String pvtDate : latestVisit) {
                SimpleDate sd = SimpleDate.mmlDateToSimpleDate(pvtDate);
                sd.setEventCode(pvtEvent);
                simpleDates.add(sd);
            }
            // CardCalendarに通知する
            calendarCardPanel.setMarkList(simpleDates);
        }
*/
//masuda$        
    }
}
