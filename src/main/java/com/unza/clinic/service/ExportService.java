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
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
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

    public byte[] exportReferralPdf(ReferralRecord referral, Patient patient) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 42, 42, 46, 52);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new ReferralFooter());
        doc.open();

        Color brandGreen = new Color(18, 96, 50);
        Color brandGold = new Color(242, 169, 0);
        Color border = new Color(210, 218, 214);
        com.lowagie.text.Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        com.lowagie.text.Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(91, 99, 95));
        com.lowagie.text.Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, new Color(32, 38, 35));
        com.lowagie.text.Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.BLACK);
        com.lowagie.text.Font emphasisFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, brandGreen);

        addReferralBrandedHeader(doc, brandGreen, brandGold);

        PdfPTable referenceBar = new PdfPTable(3);
        referenceBar.setWidthPercentage(100);
        referenceBar.setWidths(new float[]{1.4f, 1f, 1f});
        referenceBar.setSpacingAfter(10f);
        addReferenceCell(referenceBar, "REFERRAL NUMBER", nvl(referral.getReferralId()), labelFont, emphasisFont, border);
        addReferenceCell(referenceBar, "DATE", nvl(referral.getDate()), labelFont, valueFont, border);
        addUrgencyCell(referenceBar, referral.getUrgency(), labelFont, brandGreen, brandGold);
        doc.add(referenceBar);

        addReferralSectionHeader(doc, "Patient Details", sectionFont, brandGreen);
        PdfPTable patientDetails = new PdfPTable(3);
        patientDetails.setWidthPercentage(100);
        patientDetails.setWidths(new float[]{1.25f, 1f, 1f});
        patientDetails.setSpacingAfter(10f);
        addDetailCell(patientDetails, "FULL NAME", referral.getPatientName(), labelFont, valueFont, border);
        addDetailCell(patientDetails, "PATIENT / CLINIC NUMBER", firstNonBlank(
                patient != null ? patient.getClinicNumber() : null, referral.getPatientId()), labelFont, valueFont, border);
        addDetailCell(patientDetails, "DATE OF BIRTH / AGE", patientDemographicAge(patient), labelFont, valueFont, border);
        addDetailCell(patientDetails, "GENDER", patient != null ? patient.getGender() : null, labelFont, valueFont, border);
        addDetailCell(patientDetails, "PHONE", patient != null ? patient.getPhone() : null, labelFont, valueFont, border);
        addDetailCell(patientDetails, "PATIENT TYPE", patient != null ? patient.getPatientType() : null, labelFont, valueFont, border);
        doc.add(patientDetails);

        addReferralSectionHeader(doc, "Referral Information", sectionFont, brandGreen);
        PdfPTable referralDetails = new PdfPTable(3);
        referralDetails.setWidthPercentage(100);
        referralDetails.setWidths(new float[]{1.25f, 1f, 1f});
        referralDetails.setSpacingAfter(10f);
        addDetailCell(referralDetails, "FROM DEPARTMENT / FACILITY", referral.getFromDept(), labelFont, valueFont, border);
        addDetailCell(referralDetails, "TO DEPARTMENT / SERVICE", referral.getToDept(), labelFont, valueFont, border);
        addDetailCell(referralDetails, "DESTINATION FACILITY", referral.getDestinationFacility(), labelFont, valueFont, border);
        addDetailCell(referralDetails, "REFERRING CLINICIAN", referral.getReferredBy(), labelFont, valueFont, border);
        addDetailCell(referralDetails, "CLINICIAN CONTACT", referral.getReferringClinicianContact(), labelFont, valueFont, border);
        addDetailCell(referralDetails, "STATUS", referral.getStatus(), labelFont, valueFont, border);
        doc.add(referralDetails);

        addReferralSectionHeader(doc, "Clinical Information", sectionFont, brandGreen);
        addNarrativeBlock(doc, "REASON FOR REFERRAL", referral.getReason(), labelFont, bodyFont, border);
        addNarrativeBlock(doc, "PROVISIONAL DIAGNOSIS", referral.getProvisionalDiagnosis(), labelFont, bodyFont, border);
        addNarrativeBlock(doc, "CLINICAL HISTORY AND SUMMARY", referral.getClinicalSummary(), labelFont, bodyFont, border);
        addNarrativeBlock(doc, "VITAL SIGNS / EXAMINATION FINDINGS", referral.getVitalSigns(), labelFont, bodyFont, border);
        addNarrativeBlock(doc, "INVESTIGATIONS AND RESULTS", referral.getInvestigations(), labelFont, bodyFont, border);
        addNarrativeBlock(doc, "TREATMENT GIVEN / CURRENT MANAGEMENT", referral.getTreatmentGiven(), labelFont, bodyFont, border);
        PdfPTable patientRisks = new PdfPTable(2);
        patientRisks.setWidthPercentage(100);
        patientRisks.setWidths(new float[]{1f, 1f});
        addDetailCell(patientRisks, "KNOWN ALLERGIES", patient != null ? patient.getAllergies() : null,
                labelFont, bodyFont, border);
        addDetailCell(patientRisks, "RELEVANT CONDITIONS", patient != null ? patient.getConditions() : null,
                labelFont, bodyFont, border);
        doc.add(patientRisks);
        addNarrativeBlock(doc, "ADDITIONAL NOTES", referral.getNotes(), labelFont, bodyFont, border);

        PdfPTable signature = new PdfPTable(2);
        signature.setWidthPercentage(100);
        signature.setWidths(new float[]{1f, 1f});
        signature.setSpacingBefore(16f);
        addSignatureCell(signature, "Clinician signature / stamp", border, labelFont);
        addSignatureCell(signature, "Date received / receiving clinician", border, labelFont);
        doc.add(signature);

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

    private void addReferralSectionHeader(Document doc, String title, com.lowagie.text.Font font, Color background)
            throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);
        PdfPCell cell = new PdfPCell(new Phrase(title.toUpperCase(), font));
        cell.setBackgroundColor(background);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4.5f);
        table.addCell(cell);
        doc.add(table);
    }

    private void addReferenceCell(PdfPTable table, String label, String value,
                                  com.lowagie.text.Font labelFont, com.lowagie.text.Font valueFont, Color border) {
        PdfPCell cell = stackedCell(label, printable(value), labelFont, valueFont, border);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addUrgencyCell(PdfPTable table, String urgency, com.lowagie.text.Font labelFont,
                                Color brandGreen, Color brandGold) {
        String normalized = printable(urgency).toUpperCase();
        Color background = switch (normalized) {
            case "EMERGENCY" -> new Color(178, 39, 45);
            case "URGENT" -> new Color(217, 119, 6);
            default -> brandGreen;
        };
        com.lowagie.text.Font urgencyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Color.WHITE);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(background);
        cell.setBorderColor(brandGold);
        cell.setBorderWidth(1f);
        cell.setPadding(7f);
        cell.addElement(new Paragraph("URGENCY", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE)));
        cell.addElement(new Paragraph(normalized, urgencyFont));
        table.addCell(cell);
    }

    private void addDetailCell(PdfPTable table, String label, String value,
                               com.lowagie.text.Font labelFont, com.lowagie.text.Font valueFont, Color border) {
        table.addCell(stackedCell(label, printable(value), labelFont, valueFont, border));
    }

    private PdfPCell stackedCell(String label, String value, com.lowagie.text.Font labelFont,
                                 com.lowagie.text.Font valueFont, Color border) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(border);
        cell.setBorderWidth(0.6f);
        cell.setPadding(6f);
        Paragraph labelParagraph = new Paragraph(label, labelFont);
        labelParagraph.setSpacingAfter(2f);
        cell.addElement(labelParagraph);
        cell.addElement(new Paragraph(value, valueFont));
        return cell;
    }

    private void addNarrativeBlock(Document doc, String label, String value,
                                   com.lowagie.text.Font labelFont, com.lowagie.text.Font bodyFont, Color border)
            throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSplitRows(true);
        PdfPCell cell = stackedCell(label, printable(value), labelFont, bodyFont, border);
        cell.setPadding(5f);
        table.addCell(cell);
        doc.add(table);
    }

    private void addReferralBrandedHeader(Document doc, Color brandGreen, Color brandGold)
            throws DocumentException {
        PdfPTable header = new PdfPTable(3);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{0.65f, 2.35f, 1.6f});
        header.setSpacingAfter(5f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPaddingRight(8f);
        try (InputStream logoStream = getClass().getResourceAsStream("/static/logo.png")) {
            if (logoStream != null) {
                Image logo = Image.getInstance(logoStream.readAllBytes());
                logo.scaleToFit(45f, 45f);
                logoCell.addElement(logo);
            }
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Could not load logo for referral PDF header", exception);
        }
        header.addCell(logoCell);

        PdfPCell clinicCell = new PdfPCell();
        clinicCell.setBorder(Rectangle.NO_BORDER);
        clinicCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        clinicCell.addElement(new Paragraph("UNZA Clinic",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f, brandGreen)));
        clinicCell.addElement(new Paragraph("University of Zambia Health Services",
                FontFactory.getFont(FontFactory.HELVETICA, 8f, new Color(91, 96, 93))));
        clinicCell.addElement(new Paragraph("Great East Road Campus, Lusaka, Zambia",
                FontFactory.getFont(FontFactory.HELVETICA, 8f, new Color(91, 96, 93))));
        header.addCell(clinicCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph title = new Paragraph("MEDICAL REFERRAL\nLETTER",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, new Color(53, 58, 55)));
        title.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(title);
        header.addCell(titleCell);
        doc.add(header);

        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingAfter(8f);
        PdfPCell ruleCell = new PdfPCell(new Phrase(" "));
        ruleCell.setBorderWidthTop(0f);
        ruleCell.setBorderWidthLeft(0f);
        ruleCell.setBorderWidthRight(0f);
        ruleCell.setBorderWidthBottom(2f);
        ruleCell.setBorderColorBottom(brandGold);
        ruleCell.setPadding(0f);
        rule.addCell(ruleCell);
        doc.add(rule);
    }

    private void addSignatureCell(PdfPTable table, String label, Color border, com.lowagie.text.Font labelFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(border);
        cell.setBorderWidthTop(0.8f);
        cell.setBorderWidthLeft(0f);
        cell.setBorderWidthRight(0f);
        cell.setBorderWidthBottom(0f);
        cell.setPaddingTop(7f);
        cell.setPaddingBottom(10f);
        cell.addElement(new Paragraph(label, labelFont));
        table.addCell(cell);
    }

    private String patientDemographicAge(Patient patient) {
        if (patient == null) return "Not recorded";
        String dob = nvl(patient.getDob()).trim();
        String age = patient.getAge() != null ? patient.getAge() + " years" : "";
        if (!dob.isEmpty() && !age.isEmpty()) return dob + " / " + age;
        return printable(firstNonBlank(dob, age));
    }

    private String printable(String value) {
        return value == null || value.trim().isEmpty() ? "Not provided" : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first.trim() : nvl(second).trim();
    }

    private static final class ReferralFooter extends PdfPageEventHelper {
        private final com.lowagie.text.Font footerFont =
                FontFactory.getFont(FontFactory.HELVETICA, 7f, new Color(100, 105, 102));

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("CONFIDENTIAL MEDICAL DOCUMENT - Handle in accordance with patient privacy requirements.", footerFont),
                    document.left(), 24f, 0f);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), footerFont),
                    document.right(), 24f, 0f);
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
