package open.dolphin.letter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import open.dolphin.client.ClientContext;
import open.dolphin.infomodel.UserModel;
import open.dolphin.project.Project;

/**
 * 紹介状の PDF メーカー。
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class Reply2PDFMaker extends AbstractPDFMaker {

    private static final String DOC_TITLE = "ご　報　告";
    //private static final String FILE_TITLE = "ご報告";
    
    // 文書名を返す
    @Override
    protected String getTitle() {
        return DOC_TITLE.replace(" ", "").replace("　", "");
    }
    
    // PDFに出力する
    @Override
    protected boolean makePDF(String filePath) {
        
        boolean result = false;
        marginLeft = 20;
        marginRight = 20;
        marginTop = 20;
        marginBottom = 30;
        titleFontSize = 10;

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

            // タイトル
            Paragraph para = new Paragraph(DOC_TITLE, titleFont);
            para.setAlignment(Element.ALIGN_CENTER);
            document.add(para);

            document.add(new Paragraph("　"));

            // 紹介元病院
            Paragraph para2;
            String hosp = model.getClientHospital();
            if (hosp == null || (hosp.equals(""))) {
                para2 = new Paragraph("　", bodyFont);
                para2.setAlignment(Element.ALIGN_LEFT);
                document.add(para2);
            } else {
                para2 = new Paragraph(hosp, bodyFont);
                para2.setAlignment(Element.ALIGN_LEFT);
                document.add(para2);
            }

            // 紹介元診療科
            String dept = model.getClientDept();
            if (dept == null || (dept.equals(""))) {
                para2 = new Paragraph("　", bodyFont);
                para2.setAlignment(Element.ALIGN_LEFT);
                document.add(para2);
            } else {
                if (!dept.endsWith("科")) {
                    dept += "科";
                }
                para2 = new Paragraph(dept, bodyFont);
                para2.setAlignment(Element.ALIGN_LEFT);
                document.add(para2);
            }

            // 紹介元医師
            StringBuilder sb = new StringBuilder();
            if (model.getClientDoctor()!=null) {
                sb.append(model.getClientDoctor());
                sb.append(" ");
            }
            sb.append("先生　");
            // title
            String title = Project.getString("letter.atesaki.title");
            if (title!=null && (!title.equals("無し"))) {
                sb.append(title);
            }
            para2 = new Paragraph(sb.toString(), bodyFont);
            para2.setAlignment(Element.ALIGN_LEFT);
            document.add(para2);

            document.add(new Paragraph("　"));

            // 拝啓
            para2 = new Paragraph("拝啓", bodyFont);
            para2.setAlignment(Element.ALIGN_LEFT);
            document.add(para2);

            // 挨拶
            para2 = new Paragraph("時下ますますご清祥の段、お慶び申し上げます。", bodyFont);
            para2.setAlignment(Element.ALIGN_LEFT);
            document.add(para2);

            // 患者受診
            String visitedDate = model.getItemValue(Reply2Impl.ITEM_VISITED_DATE);
            String informed = model.getTextValue(Reply2Impl.TEXT_INFORMED_CONTENT);

            sb = new StringBuilder();
            sb.append(model.getPatientName());
            sb.append(" 殿(生年月日: ");
            sb.append(getDateString(model.getPatientBirthday()));
            sb.append(" ").append(model.getPatientAge()).append("歳").append(")、");
            sb.append(getDateString(visitedDate));
            sb.append(" に受診されました。");
            para2 = new Paragraph(sb.toString(), bodyFont);
            para2.setAlignment(Element.ALIGN_LEFT);
            document.add(para2);

            sb = new StringBuilder();
            sb.append("下記ご報告させていただきます。");
            para2 = new Paragraph(sb.toString(), bodyFont);
            para2.setAlignment(Element.ALIGN_LEFT);
            document.add(para2);

            // 敬具
            para2 = new Paragraph("敬具", bodyFont);
            para2.setAlignment(Element.ALIGN_RIGHT);
            document.add(para2);

            document.add(new Paragraph("　"));

            // 所見等
            para2 = new Paragraph("所見等", bodyFont);
            para2.setAlignment(Element.ALIGN_LEFT);
            document.add(para2);

            // 内容
            Table lTable = new Table(1); //テーブル・オブジェクトの生成
            lTable.setPadding(2);
            lTable.setWidth(100);

            sb = new StringBuilder();
            sb.append(informed);
            sb.append("\n");
            sb.append("　");
            lTable.addCell(new Phrase(sb.toString(), bodyFont));
            document.add(lTable);

            document.add(new Paragraph("　"));
            //document.add(new Paragraph("　"));

            // 日付
            String dateStr = getDateString(model.getConfirmed());
            para = new Paragraph(dateStr, bodyFont);
            para.setAlignment(Element.ALIGN_RIGHT);
            document.add(para);

            // 病院の住所、電話
            if (model.getConsultantAddress()==null) {
                UserModel user = Project.getUserModel();
                String zipCode = user.getFacilityModel().getZipCode();
                String address = user.getFacilityModel().getAddress();
                sb = new StringBuilder();
                sb.append(zipCode);
                sb.append(" ");
                sb.append(address);
                para2 = new Paragraph(sb.toString(), bodyFont);
                para2.setAlignment(Element.ALIGN_RIGHT);
                document.add(para2);
            } else {
                sb = new StringBuilder();
                sb.append(model.getConsultantZipCode());
                sb.append(" ");
                sb.append(model.getConsultantAddress());
                para2 = new Paragraph(sb.toString(), bodyFont);
                para2.setAlignment(Element.ALIGN_RIGHT);
                document.add(para2);
            }
            
            if (model.getConsultantTelephone()==null) {
                sb = new StringBuilder();
                sb.append("電話　");
                sb.append(Project.getUserModel().getFacilityModel().getTelephone());
                para2 = new Paragraph(sb.toString(), bodyFont);
                para2.setAlignment(Element.ALIGN_RIGHT);
                document.add(para2);
            } else {
                sb = new StringBuilder();
                sb.append("電話　");
                sb.append(model.getConsultantTelephone());
                para2 = new Paragraph(sb.toString(), bodyFont);
                para2.setAlignment(Element.ALIGN_RIGHT);
                document.add(para2);
            }

            // 差出人病院
            para2 = new Paragraph(model.getConsultantHospital(), bodyFont);
            para2.setAlignment(Element.ALIGN_RIGHT);
            document.add(para2);

            // 差出人医師
            sb = new StringBuilder();
            sb.append(model.getConsultantDoctor());
            sb.append(" 印");
            para2 = new Paragraph(sb.toString(), bodyFont);
            para2.setAlignment(Element.ALIGN_RIGHT);
            document.add(para2);

            document.add(new Paragraph("　"));

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
