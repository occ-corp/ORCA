
package open.dolphin.client;

import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingWorker;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.MMLDate;

/**
 * Brother QL-580Nで処方注射ラベルを印刷する
 * ウチはしばし紙カルテと併用なので役に立つかも？？
 * 62mmロール紙きめうち。半角文字の処理は甘い。
 * port9100にesc/pのデータをそのまま送るのでwindowsにドライバは必要ありません
 *
 * @author masuda, Masuda Naika
 */
public class PrintLabel {

    // QL-580Nの全角最大桁数(全角32dot fontで半角換算の桁数)
    private static final int maxColumn = 42;

    // esc/pコマンド関連
    private static final byte[] escpInitialize = {0x1b, 0x40};
    private static final byte[] escpCmdModeChange = {0x1b, 0x69, 0x61, 0x00};    // esc/pモード
    private static final byte[] escpKISO = {0x1c, 0x26, 0x12};
    private static final byte[] escpKOSI = {0x1c, 0x2e, 0x0f};
    private static final byte[] escpFF = {0x0c};
    // JISコードに変換時のKI/KO
    private static final byte[] KI = {0x1b, 0x24, 0x42};
    //private static final byte[] KO = {0x1b, 0x28, 0x42};

    private List<LineModel> lineData = new ArrayList<LineModel>();
    private List<ModuleModel> rpStamp = new ArrayList<ModuleModel>();
    private List<ModuleModel> exStamp = new ArrayList<ModuleModel>();
    private List<ModuleModel> otherStamp = new ArrayList<ModuleModel>();
    private List<ModuleModel> otherStamp2 = new ArrayList<ModuleModel>();
    private List<ModuleModel> injStamp = new ArrayList<ModuleModel>();
    private List<StampHolder> stampHolders = new ArrayList<StampHolder>();
    private KartePane kartePane = new KartePane();

    private String date;

    public PrintLabel() {
    }

    public void enter(KartePane kp) {
        // JISで送っていたが、毎文字にKI/KOを付加していたので、半角全角変更時
        // 適宜KI/KOを送信するように変更した。

        kartePane = kp;

        collectMedStampHolder();
        collectModuleModel();
        buildLineDataArray();
        sendRawPrintData();
    }

    public void enter2(List<StampHolder> al) {
        // スタンプホルダのpopupから実行した場合
        if (al.isEmpty()) {
            return;
        }

        kartePane = al.get(0).getKartePane();
        stampHolders = al;
        setDate();

        collectModuleModel();
        buildLineDataArray();
        sendRawPrintData();
    }

    private void sendRawPrintData() {
        String str = buildPrintString();
        byte[] rawData = makeRawData(str);
        sendData(rawData);
    }

    private void collectMedStampHolder() {

        KarteStyledDocument doc = (KarteStyledDocument) kartePane.getTextPane().getDocument();
        List<StampHolder> list = doc.getStampHolders();
        for (StampHolder sh : list) {
            String entity = sh.getStamp().getModuleInfoBean().getEntity();
            if ("medOrder".equals(entity) || "injectionOrder".equals(entity)) {
                stampHolders.add(sh);
            }
        }
        setDate();
    }

    private void setDate() {
        // ラベルの日付を、一個目のスタンプからDocInfoを調べて取得する。
        date = MMLDate.getDate();
        if (!stampHolders.isEmpty()) {
            Chart chart = stampHolders.get(0).getKartePane().getParent().getContext();
            KarteEditor editor = chart.getKarteEditor();
            if (editor.getModel().getDocInfoModel().getParentId() != null) {     // 新規作成でなかったら
                date = editor.getModel().getDocInfoModel().getFirstConfirmDateTrimTime();

            }
        }
    }


    private void collectModuleModel() {
        for (StampHolder sh : stampHolders) {
            ModuleModel stamp = sh.getStamp();
            String entity = stamp.getModuleInfoBean().getEntity();
            if ("medOrder".equals(entity)) {
                String rpName = stamp.getModuleInfoBean().getStampName();
                // 順番に印刷するために定期臨時注射それぞれのArrayListに登録
                if (rpName.indexOf("定期") != -1) {
                    rpStamp.add(stamp);
                } else if (rpName.indexOf("臨時") != -1) {
                    exStamp.add(stamp);
                } else {
                    otherStamp.add(stamp);
                }
            } else if ("injectionOrder".equals(entity)) {
                injStamp.add(stamp);
            } else {
                otherStamp2.add(stamp);
            }
        }
    }

    private void buildLineDataArray() {

        String name = kartePane.getParent().getContext().getPatient().getFullName() + "　様";
        // 印字桁数が限られているので削る
        if (date != null) {
            date = date.substring(2, date.length());
            date = date.replace("-", "");
        }
        // 一行目は患者名と処方日
        lineData.add(new LineModel(name, hankakuNumToZenkaku(date), "　"));

        // 定期処方・臨時処方・その他処方・注射の順で印刷データ作成
        for (ModuleModel mm : rpStamp) {
            addLineFromModule(mm);
        }
        for (ModuleModel mm : exStamp) {
            addLineFromModule(mm);
        }
        for (ModuleModel mm : otherStamp) {
            addLineFromModule(mm);
        }
        for (ModuleModel mm : injStamp) {
            addLineFromModule(mm);
        }
        for (ModuleModel mm : otherStamp2) {
            addLineFromModule(mm);
        }
    }

    private void addLineFromModule(ModuleModel stamp) {
        if ("medOrder".equals(stamp.getModuleInfoBean().getEntity())) {
            // 処方の場合の処理
            BundleMed bundle = (BundleMed) stamp.getModel();
            ClaimItem[] ci = bundle.getClaimItem();
            String rpName = stamp.getModuleInfoBean().getStampName();
            rpName = rpName.replace("定期 - ", "Ｒｐ");
            rpName = rpName.replace("臨時 - ", "Ｅｘ");

            lineData.add(new LineModel(rpName, "", "─"));
            for (ClaimItem c : ci) {
                String itemName = c.getName();
                String unit = c.getUnit();
                if ("カプセル".equals(unit)) {
                    unit = "Ｃ";
                }
                // 0085系のコメントはunitがnullなので""に置き換える。
                // 手技の場合もclassCodeが"0"なので置き換える
                if (unit == null || "0".equals(c.getClassCode())) {
                    unit = "";
                }
                String itemNumber = hankakuNumToZenkaku(c.getNumber()) + unit;
                lineData.add(new LineModel(itemName, itemNumber, "　"));
            }

            String admin = "【" + bundle.getAdmin() + "】";
            String bundleNumber = hankakuNumToZenkaku(bundle.getBundleNumber());

            // 頓用と外用なら「何回分」にする
            boolean tonyo = bundle.getClassCode().startsWith(ClaimConst.RECEIPT_CODE_TONYO.substring(0, 2));
            boolean gaiyo = bundle.getClassCode().startsWith(ClaimConst.RECEIPT_CODE_GAIYO.substring(0, 2));
            if (tonyo || gaiyo) {
                bundleNumber = bundleNumber + "回分";
            } else {
                bundleNumber = bundleNumber + "日分";
            }

            lineData.add(new LineModel(admin, bundleNumber, "　"));

            String adminMemo = bundle.getAdminMemo();
            if (adminMemo != null) {
                lineData.add(new LineModel(adminMemo, "", "　"));
            }

        } else if ("injectionOrder".equals(stamp.getModuleInfoBean().getEntity())) {
            BundleDolphin bundle = (BundleDolphin) stamp.getModel();
            lineData.add(new LineModel("注射", "", "─"));
            ClaimItem[] ci = bundle.getClaimItem();
            for (ClaimItem c : ci) {
                String itemName = c.getName();
                String unit = c.getUnit();
                if (unit != null) {
                    // 注射の薬剤はここで処理される
                    String itemNumber = hankakuNumToZenkaku(c.getNumber()) + unit;
                    lineData.add(new LineModel(itemName, itemNumber, "　"));
                } else {
                    // 注射の手技はここで処理される
                    itemName = "【" + itemName + "】";
                    lineData.add(new LineModel(itemName, "", "　"));
                }
            }
            // 入院注射、施行日
            String bundleNum = bundle.getBundleNumber();
            if (bundleNum.startsWith("*")) {
                String itemName =parseBundleNum(bundleNum);
                lineData.add(new LineModel(itemName, "", "　"));
            }
        } else {
            BundleDolphin bundle = (BundleDolphin) stamp.getModel();
            lineData.add(new LineModel("", "", "─"));
            ClaimItem[] ci = bundle.getClaimItem();
            for (ClaimItem c : ci) {
                String itemName = c.getName();
                String unit = c.getUnit();
                if (unit != null) {
                    String itemNumber = hankakuNumToZenkaku(c.getNumber()) + unit;
                    lineData.add(new LineModel(itemName, itemNumber, "　"));
                } else {
                    String num = c.getNumber();
                    String itemNumber = hankakuNumToZenkaku(num);
                    if (num != null) {
                        itemNumber = "×" + itemNumber;
                    }
                    lineData.add(new LineModel(itemName, itemNumber, "　"));
                }
            }
        }
    }
    
    private String parseBundleNum(String str) {
        
        int len = str.length();
        int pos = str.indexOf("/");
        StringBuilder sb = new StringBuilder();
        sb.append("回数：");
        sb.append(str.substring(0, pos));
        sb.append(" 実施日：");
        sb.append(str.substring(pos + 1, len));
        sb.append("日");

        return sb.toString();
    }

    private String buildPrintString() {

        StringBuilder sb = new StringBuilder();

        // 第２項目（数量）の桁数を基準に第１項目の桁数を決める
        for (LineModel model : lineData) {
            String item1 = model.getItemName();     // 薬剤名、患者名
            String item2 = model.getNumDate();     // 数量、日付
            String filler =model.getFiller();    // Filler

            boolean firstLine = true;
            int linePosition = 0;
            int item2Position = maxColumn - item2.getBytes().length - 1;

            for (int i = 0; i < item1.length(); ++i) {
                if (linePosition < item2Position - 2) {
                    String str = item1.substring(i, i + 1);
                    sb.append(str);
                    linePosition = linePosition + str.getBytes().length;
                }
                if (i == item1.length() - 1 || linePosition >= item2Position - 2) {
                    if ((linePosition & 1) == 1) {
                        sb.append(" ");             // 半角の調整
                        ++linePosition;
                    }
                    if (firstLine) {
                        firstLine = false;
                        // ここは項目区切りのSPCx2を含めてFillerで埋めるので(item2Position - 2 + 2)となる
                        while (linePosition < item2Position) {
                            sb.append(filler);
                            linePosition = linePosition + filler.getBytes().length;
                        }
                        sb.append(item2);
                    } else {
                        while (linePosition < maxColumn) {
                            sb.append(filler);
                            linePosition = linePosition + filler.getBytes().length;
                        }
                    }
                    sb.append("\n");
                    linePosition = 0;
                }
            }
        }
        //System.out.println(sb.toString());

        return sb.toString();
    }

    private String hankakuNumToZenkaku(String input) {
        if (input == null) {
            return "";
        }
        final String hankaku = "0123456789 ./";
        final String zenkaku = "０１２３４５６７８９　．／";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.length(); ++i) {
            int l = hankaku.indexOf(input.charAt(i));
            if (l != -1) {
                sb.append(zenkaku.charAt(l));
            } else {
                sb.append(input.charAt(i));
            }
        }
        String output = sb.toString();
        return output;
    }

    private byte[] makeRawData(String input) {

        ByteBuffer buf = ByteBuffer.allocate(10240);
        boolean kanjiMode = false;

        buf.put(escpInitialize);
        buf.put(escpCmdModeChange);
        buf.put(escpKOSI);

        for (int i = 0; i < input.length(); ++i) {
            String str = input.substring(i, i + 1);
            byte[] bytes = convertToJisBytes(str);
            if (bytes != null) {
                if (bytes.length > 4) {
                    byte[] ctrl = {bytes[0], bytes[1], bytes[2]};
                    if (Arrays.equals(ctrl, KI)) {
                        if (!kanjiMode) {
                            buf.put(escpKISO);
                        }
                        buf.put(bytes[3]);
                        buf.put(bytes[4]);
                        kanjiMode = true;
                    }
                } else if (bytes.length > 0){
                    if (kanjiMode) {
                        buf.put(escpKOSI);
                    }
                    buf.put(bytes[0]);
                    kanjiMode = false;
                }
            }
        }

        buf.put(escpKOSI);
        buf.put(escpFF);

        buf.flip();
        byte[] ret = new byte[buf.limit()];
        buf.get(ret);
        return ret;
    }

    private byte[] convertToJisBytes(String str) {
        byte[] bytes = null;
        try {
            // 全角マイナス等は自分でJISに変換
            if ("－".equals(str)) {                 // "－"
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x5d, 0x1b, 0x28, 0x42};
            } else if ("～".equals(str)) {          // "～"
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x41, 0x1b, 0x28, 0x42};
            } else if ("∥".equals(str)){           // "∥"
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x42, 0x1b, 0x28, 0x42};
            } else if ("￠".equals(str)){           // "￠"
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x71, 0x1b, 0x28, 0x42};
            } else if ("￡".equals(str)){           // "￡"
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x72, 0x1b, 0x28, 0x42};
            } else if ("￢".equals(str)){           // "￢"
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x22, 0x4c, 0x1b, 0x28, 0x42};
            } else {
                bytes = str.getBytes("JIS");
            }
        } catch (Exception e) {
        }
        return bytes;
    }

    private void sendData(final byte[] rawData) {
        // esc/pのraw dataをQL-580Nに転送する
        if (rawData == null) {
            return;
        }

        final String prtAddress = Project.getString(MiscSettingPanel.LBLPRT_ADDRESS, MiscSettingPanel.DEFAULT_LBLPRT_ADDRESS);
        final int prtPort = Project.getInt(MiscSettingPanel.LBLPRT_PORT, MiscSettingPanel.DEFAULT_LBLPRT_PORT);

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    // Socketを用意
                    Socket socket = new Socket(prtAddress, prtPort);
                    // 出力ストリームを取得
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    // 転送
                    out.write(rawData);
                    // ストリーム・ソケットを閉じる
                    out.close();
                    socket.close();
                    out = null;
                    socket = null;
                } catch (Exception e) {
                }
                return null;
            }
        };
        worker.execute();
    }


    private static class LineModel {

        private String itemName;
        private String numDate;
        private String filler;

        private LineModel(String itemName, String numDate, String filler) {
            this.itemName = itemName;
            this.numDate = numDate;
            this.filler = filler;
        }

        private String getItemName() {
            return itemName;
        }

        private String getNumDate() {
            return numDate;
        }

        private String getFiller() {
            return filler;
        }
    }
}
