package open.dolphin.letter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import open.dolphin.client.ClientContext;

/**
 * 診断書の PDF メーカー。
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class MedicalCertificatePDFMaker extends AbstractPDFMaker {

    private static final String DOC_TITLE = "診 断 書";
    // この文書のファイル名を作成
    
    // 文書名を返す
    @Override
    protected String getTitle() {
        return DOC_TITLE.replace(" ", "").replace("　", "");
    }
    
    // PDFに出力する
    @Override
    protected boolean makePDF(String filePath) {
        
        boolean result = false;
        marginLeft = 35;
        marginRight = 35;
        marginTop = 40;
        marginBottom = 40;
        titleFontSize = 18;

        // 用紙サイズを設定
        Document document = new Document(PageSize.A4, marginLeft, marginRight, marginTop, marginBottom);

        try {
            // Open Document
            writer = PdfWriter.getInstance(document, new FileOutputStream(pathToPDF));
            document.open();

            // Font
            baseFont = BaseFont.createFont(HEISEI_MIN_W3, UNIJIS_UCS2_HW_H, false);
            titleFont = new Font(baseFont, getTitleFontSize());
            bodyFont = new Font(baseFont, getBodyFontSize());

            //----------------------------------------
            // タイトル
            //----------------------------------------
            Paragraph para = new Paragraph(DOC_TITLE, titleFont);
            para.setAlignment(Element.ALIGN_CENTER);
            document.add(para);
            document.add(new Paragraph("　"));
            document.add(new Paragraph("　"));
            document.add(new Paragraph("　"));

            //----------------------------------------
            // 患者情報テーブル
            //----------------------------------------
            PdfPTable pTable = new PdfPTable(new float[]{20.0f, 60.0f, 10.0f, 10.0f});
            pTable.setWidthPercentage(100.0f);

            // 患者氏名
            PdfPCell cell;
            pTable.addCell(createNoBorderCell("氏　　名"));
            cell = createNoBorderCell(model.getPatientName());
            cell.setColspan(3);
            pTable.addCell(cell);

            // 生年月日 性別
            pTable.addCell(createNoBorderCell("生年月日"));
            pTable.addCell(createNoBorderCell(getDateString(model.getPatientBirthday())));
            pTable.addCell(createNoBorderCell("性別"));
            pTable.addCell(createNoBorderCell(model.getPatientGender()));

            // 住所
            pTable.addCell(createNoBorderCell("住　　所"));
            cell = createNoBorderCell(model.getPatientAddress());
            cell.setColspan(3);
            pTable.addCell(cell);

            // 傷病名
            String disease = model.getItemValue(MedicalCertificateImpl.ITEM_DISEASE);
            pTable.addCell(createNoBorderCell("傷 病 名"));
            cell = createNoBorderCell(disease);
            cell.setColspan(3);
            pTable.addCell(cell);

            document.add(pTable);
            document.add(new Paragraph("　"));

            //------------------------------------------
            // コンテントテーブル
            //------------------------------------------
            pTable = new PdfPTable(new float[]{1.0f});
            pTable.setWidthPercentage(100.0f);
            String informed = model.getTextValue(MedicalCertificateImpl.TEXT_INFORMED_CONTENT);
            cell = createNoBorderCell(informed);
            cell.setFixedHeight(250.0f);            // Cell 高
            cell.setLeading(0f, 1.5f);              // x 1.5 font height
            pTable.addCell(cell);
            document.add(pTable);
            document.add(new Paragraph("　"));

            //------------------------------------------
            // 署名テーブル
            //------------------------------------------
            // 日付
            pTable = new PdfPTable(new float[]{1.0f});
            pTable.setWidthPercentage(100.0f);

            // 上記の通り診断する
            pTable.addCell(createNoBorderCell("上記の通り診断する。"));
            String dateStr = getDateString(model.getConfirmed());
            pTable.addCell(createNoBorderCell(dateStr));
            
            // 住所 BaseFont.getWidthPoint
            String zipCode = model.getConsultantZipCode();
            String address = model.getConsultantAddress();
//            float zipLen = baseFont.getWidthPoint(zipCode, 12.0f);
//            float addressLen = baseFont.getWidthPoint(address, 12.0f);
//            float padlen = addressLen-zipLen;
//            sb = new StringBuilder();
//            while (true) {
//                sb.append("　");
//                if (baseFont.getWidthPoint(sb.toString(), 12.0f)>=padlen) {
//                    break;
//                }
//            }
//            String space = sb.toString();
            StringBuilder sb = new StringBuilder();
            sb.append("〒").append(zipCode);
            cell = createNoBorderCell(sb.toString());
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pTable.addCell(cell);

            sb = new StringBuilder();
            sb.append(address);
            cell = createNoBorderCell(sb.toString());
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pTable.addCell(cell);

            // 病院名
            cell = createNoBorderCell(model.getConsultantHospital());
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pTable.addCell(cell);

            // 電話番号
            cell = createNoBorderCell(model.getConsultantTelephone());
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pTable.addCell(cell);

            // 医師
            sb = new StringBuilder();
            sb.append("医 師　").append(model.getConsultantDoctor()).append("   印");
            cell = createNoBorderCell(sb.toString());
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pTable.addCell(cell);
            document.add(pTable);

            document.close();
            
            result = true;

        } catch (IOException ex) {
            ClientContext.getBootLogger().warn(ex);
            throw new RuntimeException(ERROR_IO);
        } catch (DocumentException ex) {
            ClientContext.getBootLogger().warn(ex);
            throw new RuntimeException(ERROR_PDF);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
        return result;
    }
}
