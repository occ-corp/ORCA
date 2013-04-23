package open.dolphin.letter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import open.dolphin.client.ClientContext;
import open.dolphin.project.Project;
import open.dolphin.util.AgeCalculator;

/**
 * 紹介状の PDF メーカー。
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class LetterPDFMaker extends AbstractPDFMaker {

    private static final String DOC_TITLE = "診療情報提供書";
    private static final String GREETINGS = "下記の患者さんを紹介致します。ご高診の程宜しくお願い申し上げます。";
    private static final int ADDRESS_FONT_SIZE = 9;
    private static final int HOSPITAL_FONT_SIZE = 12;
    private static final int URL_FONT_SIZE = 8;
    
    // 文書名を返す
    @Override
    protected String getTitle() {
        return DOC_TITLE.replace(" ", "").replace("　", "");
    }
    
    // PDFに出力する
    @Override
    protected boolean makePDF(String filePath) {
        
        boolean result = false;
        marginLeft = LEFT_MARGIN;
        marginRight = RIGHT_MARGIN;
        marginTop = TOP_MARGIN;
        marginBottom = BOTTOM_MARGIN;
        titleFontSize = 14;
        bodyFontSize = 10;
        

        // 用紙サイズを設定
        Document document = new Document(PageSize.A4, marginLeft, marginRight, marginTop, marginBottom);
        
        try {
            
            // Open Document
            writer = PdfWriter.getInstance(document, new FileOutputStream(pathToPDF));
            document.open();

            // Font
            baseFont = getMinchoFont();
            titleFont = new Font(baseFont, getTitleFontSize());
            bodyFont = new Font(baseFont, getBodyFontSize());
            Font addressFont = new Font(baseFont, ADDRESS_FONT_SIZE);
            Font urlFont = new Font(Font.HELVETICA, URL_FONT_SIZE, Font.NORMAL);
            Font hospitalFont = new Font(baseFont, HOSPITAL_FONT_SIZE);
            
            // フッターに宣伝
            document.setFooter(getDolphinFooter());
            document.open();
            
            // タイトル
            Paragraph para = new Paragraph(DOC_TITLE, titleFont);
            para.setAlignment(Element.ALIGN_CENTER);
            document.add(para);

            // 日付
            String dateStr = getDateString(model.getConfirmed());
            //Locale locale = new Locale("ja","JP","JP"); 
            //SimpleDateFormat frmt = new SimpleDateFormat("GGGGy年M月d日", locale); 
            //String dateStr = frmt.format(model.getConfirmed()); 
            para = new Paragraph(dateStr, bodyFont);
            para.setAlignment(Element.ALIGN_RIGHT);
            document.add(para);

            document.add(new Paragraph("　"));

            // 紹介先病院
            Paragraph para2 = new Paragraph(model.getConsultantHospital(), hospitalFont);
            para2.setAlignment(Element.ALIGN_LEFT);
            document.add(para2);

            // 紹介先診療科
            para2 = new Paragraph(model.getConsultantDept(), bodyFont);
            para2.setAlignment(Element.ALIGN_LEFT);
            document.add(para2);

            // 紹介先医師
            StringBuilder sb = new StringBuilder();
            if (model.getConsultantDoctor()!= null) {
                sb.append(model.getConsultantDoctor());
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

            // 紹介元病院
            para2 = new Paragraph(model.getClientHospital(), hospitalFont);
            para2.setAlignment(Element.ALIGN_RIGHT);
            document.add(para2);

//            // 紹介元診療科
//            para2 = new Paragraph(model.getCl, bodyFont);
//            para2.setAlignment(Element.ALIGN_RIGHT);
//            document.add(para2);

            // 紹介元医師
            sb = new StringBuilder();
/*
            // masudaは使わないのでコメントアウト
            String department = model.getClientDept();
            if (department != null && !department.isEmpty()) {
                sb.append(department);
                sb.append("    ");
            }
*/
            sb.append(model.getClientDoctor());
            //sb.append(" 印");
            
            para2 = new Paragraph(sb.toString(), bodyFont);
            para2.setAlignment(Element.ALIGN_RIGHT);
            document.add(para2);

            // 紹介元住所    稲葉先生のリクエスト　郵便番号を含める
            sb = new StringBuilder();
            String clientZip = model.getClientZipCode();
            if (clientZip != null && !clientZip.isEmpty()) {
                sb.append("〒");
                sb.append(clientZip);
                sb.append(" ");
            }
            sb.append(model.getClientAddress());
            para2 = new Paragraph(sb.toString(), addressFont);
            para2.setAlignment(Element.ALIGN_RIGHT);
            document.add(para2);

            // 紹介元電話番号    稲葉先生のリクエスト　Faxを含める
            sb = new StringBuilder();
            sb.append("電話:");
            sb.append(model.getClientTelephone());
            String fax = model.getClientFax();
            if (fax != null && !fax.isEmpty()) {
                sb.append("  Fax:");
                sb.append(fax);
            }
            para2 = new Paragraph(sb.toString(), addressFont);
            para2.setAlignment(Element.ALIGN_RIGHT);
            document.add(para2);
            
            // Web address
            String url = Project.getUserModel().getFacilityModel().getUrl();
            if (url != null && !"".equals(url)) {
                para2 = new Paragraph(url, urlFont);
                para2.setAlignment(Element.ALIGN_RIGHT);
                document.add(para2);
            }

            document.add(new Paragraph("　"));
            //document.add(new Paragraph("　"));

            // 紹介挨拶
            if (Project.getBoolean("letter.greetings.include")) {
                para2 = new Paragraph(GREETINGS, bodyFont);
                para2.setAlignment(Element.ALIGN_CENTER);
                document.add(para2);
            }

            // 患者
            Table pTable = new Table(4);
            pTable.setPadding(2);
            int width[] = new int[]{20, 60, 10, 10};
            pTable.setWidths(width);
            pTable.setWidth(100);

            String birthday = AgeCalculator.toNengoKanji(model.getPatientBirthday());
            String sexStr = model.getPatientGender();
            pTable.addCell(new Phrase("患者氏名", bodyFont));
            
            // 稲葉先生のリクエスト　カナ氏名を含める。ルビは難しいので手抜き
            sb = new StringBuilder();
            sb.append(model.getPatientName());
            String kanaName = model.getPatientKana();
            if (kanaName != null && !kanaName.isEmpty()) {
                sb.append("  （");
                sb.append(kanaName);
                sb.append("）");
            }
            pTable.addCell(new Phrase(sb.toString(), bodyFont));
            pTable.addCell(new Phrase("性別", bodyFont));
            pTable.addCell(new Phrase(sexStr, bodyFont));
            pTable.addCell(new Phrase("生年月日", bodyFont));
            sb = new StringBuilder();
            sb.append(birthday);
            sb.append(" (");
            sb.append(model.getPatientAge().split("\\.")[0]);
            sb.append(" 歳)");
            Cell cell = new Cell(new Phrase(sb.toString(), bodyFont));
            cell.setColspan(3);
            pTable.addCell(cell);

            // 稲葉先生のリクエスト 患者住所と電話番号を含める
            pTable.addCell(new Phrase("住所・電話", bodyFont));
            sb = new StringBuilder();
            // LetterModelには住所などは含まれていないのでChartから取得する
            try {

                String zipCode = model.getPatientZipCode();
                if (zipCode != null && !"".equals(zipCode)) {
                    sb.append("〒");
                    sb.append(zipCode);
                    sb.append(" ");
                }

                String address = model.getPatientAddress();
                if (address != null && !"".equals(address)) {
                    address = address.replace(" ", "");
                    sb.append(address);
                }

                String telephone = model.getPatientTelephone();
                if (telephone != null && !"".equals(telephone)) {
                    sb.append(" TEL:");
                    sb.append(telephone);
                }

                cell = new Cell(new Phrase(sb.toString(), bodyFont));
                cell.setColspan(3);
                pTable.addCell(cell);

            } catch (NullPointerException e) {
            }
            document.add(pTable);

            // 紹介状内容
            String disease = model.getItemValue(LetterImpl.ITEM_DISEASE);
            String purpose = model.getItemValue(LetterImpl.ITEM_PURPOSE);
            String pastFamily = model.getTextValue(LetterImpl.TEXT_PAST_FAMILY);
            String clinicalCourse = model.getTextValue(LetterImpl.TEXT_CLINICAL_COURSE);
            String medication = model.getTextValue(LetterImpl.TEXT_MEDICATION);
            String remarks = model.getItemValue(LetterImpl.ITEM_REMARKS);

            Table lTable = new Table(2); //テーブル・オブジェクトの生成
            lTable.setPadding(2);
            width = new int[]{20, 80};
            lTable.setWidths(width); //各カラムの大きさを設定（パーセント）
            lTable.setWidth(100);

            lTable.addCell(new Phrase("傷病名", bodyFont));
            lTable.addCell(new Phrase(disease, bodyFont));

            lTable.addCell(new Phrase("紹介目的", bodyFont));
            lTable.addCell(new Phrase(purpose, bodyFont));

            sb = new StringBuilder();
            sb.append("既往歴").append("\n").append("家族歴");
            lTable.addCell(new Phrase(sb.toString(), bodyFont));
            cell = new Cell(new Phrase(pastFamily, bodyFont));
            lTable.addCell(cell);

            sb = new StringBuilder();
            sb.append("症状経過").append("\n").append("検査結果").append("\n").append("治療経過");
            lTable.addCell(new Phrase(sb.toString(), bodyFont));
            lTable.addCell(new Phrase(clinicalCourse, bodyFont));

            lTable.addCell(new Phrase("現在の処方", bodyFont));
            lTable.addCell(new Phrase(medication, bodyFont));

            lTable.addCell(new Phrase("備 考", bodyFont));
            lTable.addCell(new Phrase(remarks, bodyFont));

            document.add(lTable);

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
