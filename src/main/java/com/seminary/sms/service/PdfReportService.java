package com.seminary.sms.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.core.io.ClassPathResource;
import com.seminary.sms.entity.*;
import com.seminary.sms.repository.ReportRepository;
import com.seminary.sms.repository.SemesterRepository;
import com.seminary.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final StudentService studentService;
    private final GradeService gradeService;
    private final EnrollmentService enrollmentService;
    private final SemesterRepository semesterRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    private static final DeviceRgb NAVY      = new DeviceRgb(13, 43, 99);
    private static final DeviceRgb LIGHT_BG  = new DeviceRgb(235, 240, 255);
    private static final String    SCHOOL    = "St. Francis de Sales Major Seminary";
    private static final String    ADDRESS   = "Marawoy, Lipa City, Batangas, Philippines";

    // ── GRADE CARD ───────────────────────────────────────────────────────────

    @Transactional
    public byte[] generateGradeCard(String studentId, String semesterId) {
        Student student = studentService.getById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        Semester semester = semesterRepository.findBySemesterId(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester not found: " + semesterId));
        List<Grade> grades = gradeService.getGradesByStudentAndSemester(studentId, semesterId);
        BigDecimal gwa = gradeService.computeGWA(studentId, semesterId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = buildDocument(out)) {
            addSchoolHeader(doc, "GRADE CARD");
            addStudentInfoTable(doc, student, semester);

            // Grades table
            Table table = buildGradeTable(
                new String[]{"Course Code", "Course Name", "Units", "Midterm", "Final", "Rating", "Remarks"},
                new float[]{90, 200, 45, 65, 65, 65, 90}
            );
            int totalUnits = 0;
            for (Grade g : grades) {
                table.addCell(bodyCell(g.getCourse().getCourseCode()));
                table.addCell(bodyCell(g.getCourse().getCourseName()));
                table.addCell(bodyCell(String.valueOf(g.getCourse().getUnits())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(bodyCell(fmt(g.getMidtermGrade())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(bodyCell(fmt(g.getFinalGrade())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(bodyCell(fmt(g.getFinalRating())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(bodyCell(g.getRemarks() != null ? g.getRemarks() : ""));
                if (g.getCourse() != null) totalUnits += g.getCourse().getUnits();
            }
            doc.add(table);

            // Footer summary
            addFooterSummary(doc, totalUnits, gwa);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Grade Card PDF", e);
        }
        byte[] pdf = out.toByteArray();
        logReport(Report.ReportType.GradeCard, student, semester);
        return pdf;
    }

    // ── TRANSCRIPT OF RECORDS ─────────────────────────────────────────────────

    @Transactional
    public byte[] generateTranscript(String studentId) {
        Student student = studentService.getById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(studentId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = buildDocument(out)) {
            addSchoolHeader(doc, "TRANSCRIPT OF RECORDS");

            // Student detail block (full)
            Table info = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            info.setMarginBottom(14);
            addInfoRow(info, "Student Name", student.getFullName());
            addInfoRow(info, "Student ID",   student.getStudentId());
            addInfoRow(info, "Program",      student.getProgram() != null ? student.getProgram().getProgramName() : "—");
            addInfoRow(info, "Date of Birth",student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : "—");
            addInfoRow(info, "Address",      student.getAddress() != null ? student.getAddress() : "—");
            addInfoRow(info, "Status",       student.getCurrentStatus().name());
            doc.add(info);

            int grandTotalUnits = 0;
            BigDecimal grandTotalWeighted = BigDecimal.ZERO;

            for (Enrollment enr : enrollments) {
                Semester sem = enr.getSemester();
                String semLabel = sem.getSemesterLabel() + " — " +
                        (sem.getSchoolYear() != null ? sem.getSchoolYear().getYearLabel() : "");

                Paragraph semHeader = new Paragraph(semLabel)
                        .setFont(getBold()).setFontSize(9)
                        .setFontColor(NAVY)
                        .setMarginTop(10).setMarginBottom(4);
                doc.add(semHeader);

                List<Grade> grades = gradeService.getGradesByStudentAndSemester(studentId, sem.getSemesterId());
                Table table = buildGradeTable(
                    new String[]{"Code", "Course Name", "Units", "Rating", "Remarks"},
                    new float[]{80, 240, 45, 65, 130}
                );
                int semUnits = 0;
                BigDecimal semWeighted = BigDecimal.ZERO;
                for (Grade g : grades) {
                    table.addCell(bodyCell(g.getCourse().getCourseCode()));
                    table.addCell(bodyCell(g.getCourse().getCourseName()));
                    int u = g.getCourse().getUnits();
                    table.addCell(bodyCell(String.valueOf(u)).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(bodyCell(fmt(g.getFinalRating())).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(bodyCell(g.getRemarks() != null ? g.getRemarks() : ""));
                    semUnits += u;
                    if (g.getFinalRating() != null)
                        semWeighted = semWeighted.add(g.getFinalRating().multiply(BigDecimal.valueOf(u)));
                }
                doc.add(table);

                BigDecimal semGwa = semUnits > 0
                        ? semWeighted.divide(BigDecimal.valueOf(semUnits), 2, RoundingMode.HALF_UP) : null;
                doc.add(new Paragraph("Semester GWA: " + fmt(semGwa) + "    Total Units: " + semUnits)
                        .setFontSize(8).setTextAlignment(TextAlignment.RIGHT).setMarginTop(2));

                grandTotalUnits    += semUnits;
                grandTotalWeighted  = grandTotalWeighted.add(semWeighted);
            }

            BigDecimal overallGwa = grandTotalUnits > 0
                    ? grandTotalWeighted.divide(BigDecimal.valueOf(grandTotalUnits), 2, RoundingMode.HALF_UP) : null;
            addFooterSummary(doc, grandTotalUnits, overallGwa);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Transcript PDF", e);
        }
        byte[] pdf = out.toByteArray();
        logReport(Report.ReportType.TranscriptOfRecords, student, null);
        return pdf;
    }

    // ── CHED REPORT ───────────────────────────────────────────────────────────

    @Transactional
    public byte[] generateCHEDReport(String semesterId) {
        Semester semester = semesterRepository.findBySemesterId(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester not found: " + semesterId));
        List<Enrollment> enrollments = enrollmentService.getBySemester(semesterId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = buildDocument(out)) {
            addSchoolHeader(doc, "CHED ENROLLMENT & COMPLIANCE REPORT");

            String semLabel = semester.getSemesterLabel() +
                    (semester.getSchoolYear() != null ? " — " + semester.getSchoolYear().getYearLabel() : "");
            doc.add(new Paragraph("Semester: " + semLabel)
                    .setFont(getBold()).setFontSize(9).setFontColor(NAVY).setMarginBottom(10));

            Table table = buildGradeTable(
                new String[]{"Student ID", "Student Name", "Program", "Year", "Subjects", "Units", "GWA", "Status"},
                new float[]{75, 150, 70, 35, 55, 45, 50, 60}
            );

            for (Enrollment enr : enrollments) {
                Student s = enr.getStudent();
                List<Grade> grades = gradeService.getGradesByStudentAndSemester(
                        s.getStudentId(), semesterId);
                int units = grades.stream()
                        .mapToInt(g -> g.getCourse() != null ? g.getCourse().getUnits() : 0).sum();
                BigDecimal gwa = gradeService.computeGWA(s.getStudentId(), semesterId);

                table.addCell(bodyCell(s.getStudentId()));
                table.addCell(bodyCell(s.getFullName()));
                table.addCell(bodyCell(enr.getProgram() != null ? enr.getProgram().getProgramCode() : "—"));
                table.addCell(bodyCell(String.valueOf(enr.getYearLevel())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(bodyCell(String.valueOf(grades.size())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(bodyCell(String.valueOf(units)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(bodyCell(fmt(gwa)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(bodyCell(enr.getEnrollmentStatus().name()));
            }
            doc.add(table);

            doc.add(new Paragraph("Total Enrolled: " + enrollments.size())
                    .setFont(getBold()).setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setMarginTop(8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CHED Report PDF", e);
        }
        byte[] pdf = out.toByteArray();
        logReport(Report.ReportType.CHEDReport, null, semester);
        return pdf;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void logReport(Report.ReportType type, Student student, Semester semester) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User generatedBy = (auth != null)
                ? userRepository.findByUsername(auth.getName()).orElse(null)
                : null;
        if (generatedBy == null) return;

        String reportId = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        Report report = Report.builder()
                .reportId(reportId)
                .reportType(type)
                .student(student)
                .semester(semester)
                .generatedBy(generatedBy)
                .exportFormat(Report.ExportFormat.PDF)
                .build();
        reportRepository.save(report);
    }

    private Document buildDocument(ByteArrayOutputStream out) throws Exception {
        PdfWriter  writer = new PdfWriter(out);
        PdfDocument pdf   = new PdfDocument(writer);
        Document   doc    = new Document(pdf, PageSize.A4.rotate() /* landscape for wide tables */);
        doc.setMargins(36, 36, 36, 36);
        return doc;
    }

    private void addSchoolHeader(Document doc, String reportTitle) throws Exception {
        // 3-column table: logo | school name + address | empty spacer (keeps text centered)
        Table header = new Table(UnitValue.createPercentArray(new float[]{12, 76, 12}))
                .useAllAvailableWidth().setMarginBottom(8);

        ClassPathResource logoRes = new ClassPathResource("static/images/seminaryLogo.png");
        ImageData imgData = ImageDataFactory.create(logoRes.getURL());
        Image logo = new Image(imgData).setWidth(54).setHeight(54);

        header.addCell(new Cell()
                .add(logo)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        header.addCell(new Cell()
                .add(new Paragraph(SCHOOL)
                        .setFont(getBold()).setFontSize(13).setFontColor(NAVY)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2))
                .add(new Paragraph(ADDRESS)
                        .setFont(getRegular()).setFontSize(8).setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        header.addCell(new Cell().setBorder(Border.NO_BORDER)); // spacer

        doc.add(header);

        doc.add(new Paragraph(reportTitle)
                .setFont(getBold()).setFontSize(11).setFontColor(NAVY)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderBottom(new SolidBorder(NAVY, 1.5f)).setPaddingBottom(6).setMarginBottom(12));
    }

    private void addStudentInfoTable(Document doc, Student student, Semester semester) throws Exception {
        Table info = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
        info.setMarginBottom(14);
        addInfoRow(info, "Student Name", student.getFullName());
        addInfoRow(info, "Student ID",   student.getStudentId());
        addInfoRow(info, "Program",      student.getProgram() != null ? student.getProgram().getProgramName() : "—");
        addInfoRow(info, "Year Level",   student.getCurrentYearLevel() != null ? student.getCurrentYearLevel() + "" : "—");
        addInfoRow(info, "Semester",     semester.getSemesterLabel());
        addInfoRow(info, "School Year",  semester.getSchoolYear() != null ? semester.getSchoolYear().getYearLabel() : "—");
        doc.add(info);
    }

    private void addInfoRow(Table table, String label, String value) throws Exception {
        Cell labelCell = new Cell().add(new Paragraph(label).setFont(getBold()).setFontSize(8))
                .setBorder(Border.NO_BORDER).setPadding(2);
        Cell valueCell = new Cell().add(new Paragraph(value != null ? value : "—").setFont(getRegular()).setFontSize(8))
                .setBorder(Border.NO_BORDER).setPadding(2);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Table buildGradeTable(String[] headers, float[] widths) throws Exception {
        Table table = new Table(UnitValue.createPointArray(widths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(6);
        for (String h : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(h).setFont(getBold()).setFontSize(8).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(NAVY)
                    .setPadding(4).setTextAlignment(TextAlignment.CENTER)
                    .setBorder(Border.NO_BORDER);
            table.addHeaderCell(cell);
        }
        return table;
    }

    private Cell bodyCell(String text) throws Exception {
        return new Cell()
                .add(new Paragraph(text != null ? text : "—").setFont(getRegular()).setFontSize(8))
                .setPadding(3).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER).setBorderBottom(new SolidBorder(LIGHT_BG, 0.5f));
    }

    private void addFooterSummary(Document doc, int totalUnits, BigDecimal gwa) throws Exception {
        Table footer = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
        footer.setMarginTop(10);
        addInfoRow(footer, "Total Units", String.valueOf(totalUnits));
        addInfoRow(footer, "GWA",         fmt(gwa));
        doc.add(footer);
    }

    private String fmt(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP).toPlainString() : "—";
    }

    private PdfFont getRegular() throws Exception {
        return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
    }

    private PdfFont getBold() throws Exception {
        return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
    }
}
