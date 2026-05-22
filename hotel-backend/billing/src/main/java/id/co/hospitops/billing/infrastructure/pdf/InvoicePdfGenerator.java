package id.co.hospitops.billing.infrastructure.pdf;

import id.co.hospitops.billing.domain.model.Invoice;
import id.co.hospitops.billing.domain.port.out.ReservationDetailPort;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class InvoicePdfGenerator {

    private static final DeviceRgb GOLD = new DeviceRgb(201, 168, 76);
    private static final DeviceRgb DARK = new DeviceRgb(42, 42, 42);
    private static final DeviceRgb MUTED = new DeviceRgb(136, 136, 136);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public byte[] generate(Invoice invoice,
                           ReservationDetailPort.ReservationDetail detail) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(50, 50, 50, 50);

            PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont regular = PdfFontFactory.createFont("Helvetica");

            // Header
            Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);
            header.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("HospitOps").setFont(bold).setFontSize(26).setFontColor(GOLD))
                    .add(new Paragraph("Hotel Management System")
                            .setFont(regular).setFontSize(10).setFontColor(MUTED)));
            header.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("INVOICE").setFont(bold).setFontSize(20)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph(invoice.getInvoiceNumber())
                            .setFont(regular).setFontSize(11).setFontColor(MUTED)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph(invoice.getPaymentStatus().name()).setFont(bold).setFontSize(10)
                            .setFontColor(invoice.getPaymentStatus().name().equals("PAID")
                                    ? new DeviceRgb(39, 174, 96) : new DeviceRgb(192, 57, 43))
                            .setTextAlignment(TextAlignment.RIGHT)));
            doc.add(header);
            doc.add(new Paragraph("\n"));

            // Guest + Stay
            Table info = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);
            info.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("BILLED TO").setFont(bold).setFontSize(9)
                            .setFontColor(MUTED).setCharacterSpacing(1.5f))
                    .add(new Paragraph(detail.guestFullName()).setFont(bold).setFontSize(13))
                    .add(new Paragraph(nvl(detail.guestIdNumber())).setFont(regular)
                            .setFontSize(11).setFontColor(MUTED))
                    .add(new Paragraph(nvl(detail.guestPhone())).setFont(regular)
                            .setFontSize(11).setFontColor(MUTED)));
            info.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("STAY DETAILS").setFont(bold).setFontSize(9)
                            .setFontColor(MUTED).setCharacterSpacing(1.5f))
                    .add(new Paragraph("Room: " + detail.roomNumber() + " · " + detail.roomTypeName())
                            .setFont(regular).setFontSize(11))
                    .add(new Paragraph("Check-In: " + detail.checkInDate().format(FMT))
                            .setFont(regular).setFontSize(11))
                    .add(new Paragraph("Check-Out: " + detail.checkOutDate().format(FMT))
                            .setFont(regular).setFontSize(11))
                    .add(new Paragraph("Nights: " + detail.nights())
                            .setFont(regular).setFontSize(11)));
            doc.add(info);
            doc.add(new Paragraph("\n"));

            // Line items
            Table items = new Table(UnitValue.createPercentArray(new float[]{5, 1, 2, 2}))
                    .setWidth(UnitValue.createPercentValue(100));
            for (String h : new String[]{"Description", "Qty", "Unit Price", "Total"}) {
                items.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFont(bold).setFontSize(9).setFontColor(MUTED))
                        .setBorder(Border.NO_BORDER)
                        .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                                DARK, 0.5f))
                        .setPadding(6));
            }
            for (var item : invoice.getItems()) {
                addRow(items, item.description(), String.valueOf(item.quantity()),
                        formatRp(item.unitPrice().amount()),
                        formatRp(item.totalPrice().amount()), regular);
            }
            doc.add(items);
            doc.add(new Paragraph("\n"));

            // Totals
            Table totals = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);
            totals.addCell(new Cell(4, 1).setBorder(Border.NO_BORDER));
            addTotal(totals, "Subtotal", formatRp(invoice.getSubtotal().amount()), false, regular, bold);
            addTotal(totals, "Tax (11%)", formatRp(invoice.getTaxAmount().amount()), false, regular, bold);
            if (!invoice.getDiscountAmount().isZero())
                addTotal(totals, "Discount", "- " + formatRp(invoice.getDiscountAmount().amount()), false, regular, bold);
            addTotal(totals, "TOTAL", formatRp(invoice.getTotalAmount().amount()), true, regular, bold);
            doc.add(totals);

            doc.add(new Paragraph("\n\n"));
            doc.add(new Paragraph(
                    "Thank you for staying with us. We look forward to welcoming you again.")
                    .setFont(regular).setFontSize(9).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addRow(Table t, String desc, String qty, String unit, String total, PdfFont f) {
        var b = new com.itextpdf.layout.borders.SolidBorder(DARK, 0.5f);
        t.addCell(new Cell().add(new Paragraph(desc).setFont(f).setFontSize(11))
                .setBorder(Border.NO_BORDER).setBorderBottom(b).setPadding(6));
        t.addCell(new Cell().add(new Paragraph(qty).setFont(f).setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER).setBorderBottom(b).setPadding(6));
        t.addCell(new Cell().add(new Paragraph(unit).setFont(f).setFontSize(11)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setBorderBottom(b).setPadding(6));
        t.addCell(new Cell().add(new Paragraph(total).setFont(f).setFontSize(11)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setBorderBottom(b).setPadding(6));
    }

    private void addTotal(Table t, String label, String value, boolean isBold,
                          PdfFont regular, PdfFont bold) {
        var border = isBold
                ? new com.itextpdf.layout.borders.SolidBorder(DARK, 1)
                : Border.NO_BORDER;
        t.addCell(new Cell().add(new Paragraph(label)
                        .setFont(isBold ? bold : regular).setFontSize(isBold ? 13 : 11)
                        .setFontColor(isBold ? com.itextpdf.kernel.colors.ColorConstants.WHITE : MUTED))
                .setBorder(Border.NO_BORDER).setBorderTop(border).setPadding(isBold ? 8 : 4));
        t.addCell(new Cell().add(new Paragraph(value)
                        .setFont(isBold ? bold : regular).setFontSize(isBold ? 13 : 11)
                        .setFontColor(isBold ? GOLD : com.itextpdf.kernel.colors.ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setBorderTop(border).setPadding(isBold ? 8 : 4));
    }

    private String formatRp(java.math.BigDecimal v) {
        return "Rp " + String.format("%,.0f", v);
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }
}
