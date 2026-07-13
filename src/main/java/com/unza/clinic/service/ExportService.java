package com.unza.clinic.service;

// OpenPDF imports (fully qualify only the types that clash with Apache POI)
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Chunk;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// Apache POI imports — explicit to avoid ambiguity with OpenPDF
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row;   // POI Row — NOT com.lowagie.text.Row
import org.apache.poi.ss.usermodel.Cell;  // POI Cell — NOT com.lowagie.text.Cell
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.unza.clinic.model.*;
import com.unza.clinic.repository.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ExportService {

    private static final Logger LOG = Logger.getLogger(ExportService.class.getName());

    private final PatientRepository patientRepo;
    private final BillingRepository billingRepo;
    private final AttendanceRepository attendanceRepo;
    private final DrugRepository drugRepo;
    private final LabTestRepository labTestRepo;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ExportService(PatientRepository patientRepo, BillingRepository billingRepo,
                          AttendanceRepository attendanceRepo, DrugRepository drugRepo,
                          LabTestRepository labTestRepo) {
        this.patientRepo = patientRepo;
        this.billingRepo = billingRepo;
        this.attendanceRepo = attendanceRepo;
        this.drugRepo = drugRepo;
        this.labTestRepo = labTestRepo;
    }

    // ================================================================
    // Excel exports
    // ================================================================

    public byte[] exportPatientsExcel() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Patients");
            String[] headers = {"Clinic Number", "Name", "Type", "Age", "Gender", "Phone", "Email", "Blood Group", "Insurance", "Status"};
            createHeaderRow(wb, sheet, headers);
            int rowNum = 1;
            for (Patient p : patientRepo.findAll()) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(nvl(p.getClinicNumber()));
                r.createCell(1).setCellValue(nvl(p.getName()));
                r.createCell(2).setCellValue(nvl(p.getPatientType()));
                r.createCell(3).setCellValue(p.getAge() != null ? p.getAge() : 0);
                r.createCell(4).setCellValue(nvl(p.getGender()));
                r.createCell(5).setCellValue(nvl(p.getPhone()));
                r.createCell(6).setCellValue(nvl(p.getEmail()));
                r.createCell(7).setCellValue(nvl(p.getBloodGroup()));
                r.createCell(8).setCellValue(nvl(p.getInsurance()));
                r.createCell(9).setCellValue(nvl(p.getStatus()));
            }
            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }

    public byte[] exportBillingExcel(String fromDate, String toDate) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Billing");
            String[] headers = {"Invoice ID", "Patient Name", "Subtotal", "Tax", "Total", "Status", "Payment Method", "Due Date", "Paid Date"};
            createHeaderRow(wb, sheet, headers);
            int rowNum = 1;
            for (BillingInvoice inv : billingRepo.findAll()) {
                if (!inDateRange(inv.getDueDate(), fromDate, toDate)) continue;
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(nvl(inv.getInvoiceId()));
                r.createCell(1).setCellValue(nvl(inv.getPatientName()));
                r.createCell(2).setCellValue(inv.getSubtotal() != null ? inv.getSubtotal() : 0);
                r.createCell(3).setCellValue(inv.getTax() != null ? inv.getTax() : 0);
                r.createCell(4).setCellValue(inv.getTotal() != null ? inv.getTotal() : 0);
                r.createCell(5).setCellValue(nvl(inv.getStatus()));
                r.createCell(6).setCellValue(nvl(inv.getPaymentMethod()));
                r.createCell(7).setCellValue(nvl(inv.getDueDate()));
                r.createCell(8).setCellValue(nvl(inv.getPaidDate()));
            }
            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }

    public byte[] exportInventoryExcel() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Drug Inventory");
            String[] headers = {"Drug ID", "Name", "Category", "Type", "Batch", "Stock", "Reorder Level", "Unit", "Expiry", "Location", "Status"};
            createHeaderRow(wb, sheet, headers);
            int rowNum = 1;
            for (Drug d : drugRepo.findAll()) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(nvl(d.getDrugId()));
                r.createCell(1).setCellValue(nvl(d.getName()));
                r.createCell(2).setCellValue(nvl(d.getCategory()));
                r.createCell(3).setCellValue(nvl(d.getDrugType()));
                r.createCell(4).setCellValue(nvl(d.getBatchNumber()));
                r.createCell(5).setCellValue(d.getStock() != null ? d.getStock() : 0);
                r.createCell(6).setCellValue(d.getReorderLevel() != null ? d.getReorderLevel() : 0);
                r.createCell(7).setCellValue(nvl(d.getUnit()));
                r.createCell(8).setCellValue(nvl(d.getExpiry()));
                r.createCell(9).setCellValue(nvl(d.getStorageLocation()));
                r.createCell(10).setCellValue(nvl(d.getStatus()));
            }
            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }

    public byte[] exportLabResultsExcel(String fromDate, String toDate) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Lab Results");
            String[] headers = {"Test ID", "Patient Name", "Test", "Category", "Requested By", "Date", "Status", "Results", "Abnormal", "Approved By"};
            createHeaderRow(wb, sheet, headers);
            int rowNum = 1;
            for (LabTest t : labTestRepo.findAll()) {
                if (!inDateRange(t.getDate(), fromDate, toDate)) continue;
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(nvl(t.getTestId()));
                r.createCell(1).setCellValue(nvl(t.getPatientName()));
                r.createCell(2).setCellValue(nvl(t.getTest()));
                r.createCell(3).setCellValue(nvl(t.getCategory()));
                r.createCell(4).setCellValue(nvl(t.getRequestedBy()));
                r.createCell(5).setCellValue(nvl(t.getDate()));
                r.createCell(6).setCellValue(nvl(t.getStatus()));
                r.createCell(7).setCellValue(nvl(t.getResults()));
                r.createCell(8).setCellValue(nvl(t.getAbnormalFlag()));
                r.createCell(9).setCellValue(nvl(t.getApprovedBy()));
            }
            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // ================================================================
    // PDF exports — use com.lowagie.text.Font explicitly to avoid clash
    // ================================================================

    private void addBrandedHeader(Document doc, String docTitle) throws DocumentException {
        com.lowagie.text.Font clinicFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(22, 100, 29));
        com.lowagie.text.Font subFont     = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 100, 100));
        com.lowagie.text.Font docTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.DARK_GRAY);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 3f});
        header.setSpacingAfter(6f);

        // Logo cell
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setPaddingRight(8f);
        try (InputStream logoStream = getClass().getResourceAsStream("/static/logo.png")) {
            if (logoStream != null) {
                Image logo = Image.getInstance(logoStream.readAllBytes());
                logo.scaleToFit(50, 50);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not load logo for PDF header", e);
        }
        header.addCell(logoCell);

        // Clinic info cell
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(0);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        infoCell.addElement(new Paragraph("UNZA Clinic", clinicFont));
        infoCell.addElement(new Paragraph("University of Zambia Health Services", subFont));
        infoCell.addElement(new Paragraph("Great East Road Campus, Lusaka, Zambia", subFont));
        header.addCell(infoCell);

        doc.add(header);

        // Separator line via a thin coloured table row
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell ruleCell = new PdfPCell(new Phrase(" "));
        ruleCell.setBorderWidthBottom(1.5f);
        ruleCell.setBorderColorBottom(new Color(22, 100, 29));
        ruleCell.setBorderWidthTop(0); ruleCell.setBorderWidthLeft(0); ruleCell.setBorderWidthRight(0);
        ruleCell.setPaddingBottom(4f);
        rule.addCell(ruleCell);
        doc.add(rule);
        doc.add(Chunk.NEWLINE);

        Paragraph titlePara = new Paragraph(docTitle, docTitleFont);
        titlePara.setAlignment(Element.ALIGN_LEFT);
        doc.add(titlePara);
        doc.add(Chunk.NEWLINE);
    }

    public byte[] exportBillingInvoicePdf(BillingInvoice inv) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        com.lowagie.text.Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        com.lowagie.text.Font normalFont  = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        com.lowagie.text.Font totalFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 80, 140));

        addBrandedHeader(doc, "Billing Invoice");

        doc.add(new Paragraph("Invoice ID: " + nvl(inv.getInvoiceId()), sectionFont));
        doc.add(new Paragraph("Patient: " + nvl(inv.getPatientName()), normalFont));
        doc.add(new Paragraph("Status: " + nvl(inv.getStatus()), normalFont));
        doc.add(new Paragraph("Due Date: " + nvl(inv.getDueDate()), normalFont));
        if (inv.getPaidDate() != null) doc.add(new Paragraph("Paid Date: " + inv.getPaidDate(), normalFont));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Services / Items", sectionFont));
        doc.add(Chunk.NEWLINE);

        if (inv.getItems() != null) {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            addPdfTableHeader(table, sectionFont, "Item", "Details");
            String[] lines = inv.getItems().split("\n");
            for (String line : lines) {
                if (!line.isBlank()) addPdfTableRow(table, normalFont, line.trim(), "");
            }
            doc.add(table);
            doc.add(Chunk.NEWLINE);
        }

        doc.add(new Paragraph(String.format("Subtotal: ZMW %.2f", inv.getSubtotal() != null ? inv.getSubtotal() : 0), normalFont));
        doc.add(new Paragraph(String.format("Tax: ZMW %.2f", inv.getTax() != null ? inv.getTax() : 0), normalFont));
        doc.add(new Paragraph(String.format("TOTAL: ZMW %.2f", inv.getTotal() != null ? inv.getTotal() : 0), totalFont));

        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Generated: " + LocalDateTime.now().format(DT),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
        doc.close();
        return out.toByteArray();
    }

    public byte[] exportPatientSummaryPdf(Patient patient, List<LabTest> labTests,
                                           List<BillingInvoice> invoices) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        com.lowagie.text.Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        com.lowagie.text.Font normalFont  = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        addBrandedHeader(doc, "Patient Summary");

        doc.add(new Paragraph("Patient Details", sectionFont));
        doc.add(new Paragraph("Name: " + nvl(patient.getName()), normalFont));
        doc.add(new Paragraph("Clinic #: " + nvl(patient.getClinicNumber()), normalFont));
        doc.add(new Paragraph("Type: " + nvl(patient.getPatientType()), normalFont));
        doc.add(new Paragraph("Gender: " + nvl(patient.getGender()) + "   Age: " + (patient.getAge() != null ? patient.getAge() : ""), normalFont));
        doc.add(new Paragraph("Blood Group: " + nvl(patient.getBloodGroup()), normalFont));
        doc.add(new Paragraph("Phone: " + nvl(patient.getPhone()), normalFont));
        doc.add(new Paragraph("Allergies: " + nvl(patient.getAllergies()), normalFont));
        doc.add(new Paragraph("Conditions: " + nvl(patient.getConditions()), normalFont));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Recent Lab Results", sectionFont));
        if (labTests.isEmpty()) {
            doc.add(new Paragraph("No lab results on record.", normalFont));
        } else {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            addPdfTableHeader(table, sectionFont, "Test", "Date", "Status", "Results", "Flag");
            for (LabTest t : labTests) {
                addPdfTableRow(table, normalFont, nvl(t.getTest()), nvl(t.getDate()),
                        nvl(t.getStatus()), nvl(t.getResults()), nvl(t.getAbnormalFlag()));
            }
            doc.add(table);
        }
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Billing History", sectionFont));
        if (invoices.isEmpty()) {
            doc.add(new Paragraph("No billing records.", normalFont));
        } else {
            PdfPTable bt = new PdfPTable(4);
            bt.setWidthPercentage(100);
            addPdfTableHeader(bt, sectionFont, "Invoice", "Total (ZMW)", "Status", "Date");
            for (BillingInvoice inv : invoices) {
                addPdfTableRow(bt, normalFont,
                        nvl(inv.getInvoiceId()),
                        String.format("%.2f", inv.getTotal() != null ? inv.getTotal() : 0),
                        nvl(inv.getStatus()),
                        nvl(inv.getDueDate()));
            }
            doc.add(bt);
        }

        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Generated: " + LocalDateTime.now().format(DT),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
        doc.close();
        return out.toByteArray();
    }

    // ================================================================
    // Helpers
    // ================================================================

    private void createHeaderRow(Workbook wb, Sheet sheet, String... headers) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // Use fully-qualified POI Font to avoid clash with com.lowagie.text.Font
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) sheet.autoSizeColumn(i);
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private void addPdfTableHeader(PdfPTable table, com.lowagie.text.Font font, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(new Color(200, 220, 245));
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private void addPdfTableRow(PdfPTable table, com.lowagie.text.Font font, String... values) {
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v != null ? v : "", font));
            cell.setPadding(3);
            table.addCell(cell);
        }
    }

    private boolean inDateRange(String date, String from, String to) {
        if (date == null) return true;
        if (from != null && !from.isBlank() && date.compareTo(from) < 0) return false;
        if (to   != null && !to.isBlank()   && date.compareTo(to)   > 0) return false;
        return true;
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
