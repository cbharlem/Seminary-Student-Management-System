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
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final StudentService studentService;
    private final GradeService gradeService;
    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSubjectRepository enrollmentSubjectRepository;
    private final GradeRepository gradeRepository;
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
        BigDecimal gwa = gradeService.computeGWA(studentId, semesterId);

        // Get all enrolled subjects for this semester (includes INC/DRP even without grade records)
        Optional<Enrollment> optEnr = enrollmentRepository
                .findByStudent_StudentIdAndSemester_SemesterId(studentId, semesterId);
        List<EnrollmentSubject> subjects = optEnr.isPresent()
                ? enrollmentSubjectRepository.findByEnrollment_Index(optEnr.get().getIndex())
                : new ArrayList<>();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = buildDocument(out)) {
            addSchoolHeader(doc, "GRADE CARD");
            addStudentInfoTable(doc, student, semester);

            Table table = buildGradeTable(
                new String[]{"Course Code", "Course Name", "Units", "Midterm", "Final", "Rating", "Remarks"},
                new float[]{90, 200, 45, 65, 65, 65, 90}
            );
            int totalUnits = 0;
            for (EnrollmentSubject es : subjects) {
                Optional<Grade> optGrade = gradeRepository.findByEnrollmentSubject_Index(es.getIndex());
                Grade g = optGrade.orElse(null);
                Course course = es.getCourse();
                table.addCell(bodyCell(course.getCourseCode()));
                table.addCell(bodyCell(course.getCourseName()));
                int u = course.getUnits();
                table.addCell(bodyCell(String.valueOf(u)).setTextAlignment(TextAlignment.CENTER));
                if (g != null) {
                    table.addCell(bodyCell(fmt(g.getMidtermGrade())).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(bodyCell(fmt(g.getFinalGrade())).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(bodyCell(fmt(g.getFinalRating())).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(bodyCell(gradeRemarks(g)));
                } else {
                    table.addCell(bodyCell("—").setTextAlignment(TextAlignment.CENTER));
                    table.addCell(bodyCell("—").setTextAlignment(TextAlignment.CENTER));
                    table.addCell(bodyCell("—").setTextAlignment(TextAlignment.CENTER));
                    table.addCell(bodyCell(es.getStatus() == EnrollmentSubject.SubjectStatus.Dropped ? "DRP" : "—"));
                }
                totalUnits += u;
            }
            doc.add(table);

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

            // Group enrollments by year level so GWA can be shown per year (Sem1+Sem2 combined)
            Map<Integer, List<Enrollment>> byYear = new LinkedHashMap<>();
            for (Enrollment enr : enrollments) {
                byYear.computeIfAbsent(enr.getYearLevel(), k -> new ArrayList<>()).add(enr);
            }

            int grandTotalUnits = 0;
            BigDecimal grandTotalWeighted = BigDecimal.ZERO;

            for (Map.Entry<Integer, List<Enrollment>> yearEntry : byYear.entrySet()) {
                Integer yl = yearEntry.getKey();

                doc.add(new Paragraph("YEAR " + yl)
                        .setFont(getBold()).setFontSize(10).setFontColor(NAVY)
                        .setMarginTop(14).setMarginBottom(2));

                int yearUnits = 0;
                BigDecimal yearWeighted = BigDecimal.ZERO;

                for (Enrollment enr : yearEntry.getValue()) {
                    Semester sem = enr.getSemester();
                    String semLabel = sem.getSemesterLabel() + " — " +
                            (sem.getSchoolYear() != null ? sem.getSchoolYear().getYearLabel() : "");

                    doc.add(new Paragraph(semLabel)
                            .setFont(getBold()).setFontSize(9).setFontColor(NAVY)
                            .setMarginTop(6).setMarginBottom(4));

                    // All enrolled subjects for this semester (includes INC/DRP)
                    List<EnrollmentSubject> subjects =
                            enrollmentSubjectRepository.findByEnrollment_Index(enr.getIndex());
                    Table table = buildGradeTable(
                        new String[]{"Code", "Course Name", "Units", "Rating", "Remarks"},
                        new float[]{80, 240, 45, 65, 130}
                    );
                    int semUnits = 0;
                    BigDecimal semWeighted = BigDecimal.ZERO;
                    for (EnrollmentSubject es : subjects) {
                        Optional<Grade> optGrade = gradeRepository.findByEnrollmentSubject_Index(es.getIndex());
                        Grade g = optGrade.orElse(null);
                        Course course = es.getCourse();
                        int u = course.getUnits();
                        table.addCell(bodyCell(course.getCourseCode()));
                        table.addCell(bodyCell(course.getCourseName()));
                        table.addCell(bodyCell(String.valueOf(u)).setTextAlignment(TextAlignment.CENTER));
                        if (g != null && g.getFinalRating() != null) {
                            table.addCell(bodyCell(fmt(g.getFinalRating())).setTextAlignment(TextAlignment.CENTER));
                            table.addCell(bodyCell(gradeRemarks(g)));
                            semWeighted = semWeighted.add(g.getFinalRating().multiply(BigDecimal.valueOf(u)));
                            semUnits += u;
                        } else if (g != null) {
                            table.addCell(bodyCell("—").setTextAlignment(TextAlignment.CENTER));
                            table.addCell(bodyCell(gradeRemarks(g)));
                        } else {
                            table.addCell(bodyCell("—").setTextAlignment(TextAlignment.CENTER));
                            table.addCell(bodyCell(es.getStatus() == EnrollmentSubject.SubjectStatus.Dropped ? "DRP" : "—"));
                        }
                    }
                    doc.add(table);

                    yearUnits     += semUnits;
                    yearWeighted   = yearWeighted.add(semWeighted);
                    grandTotalUnits    += semUnits;
                    grandTotalWeighted  = grandTotalWeighted.add(semWeighted);
                }

                BigDecimal yearGwa = yearUnits > 0
                        ? yearWeighted.divide(BigDecimal.valueOf(yearUnits), 2, RoundingMode.HALF_UP) : null;
                doc.add(new Paragraph("Year " + yl + " GWA: " + fmt(yearGwa) + "    Total Units: " + yearUnits)
                        .setFont(getBold()).setFontSize(9).setFontColor(NAVY)
                        .setTextAlignment(TextAlignment.RIGHT).setMarginTop(4).setMarginBottom(4));
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

    private String gradeRemarks(Grade g) {
        if (g.getRemarks() != null && !g.getRemarks().isBlank()) return g.getRemarks();
        if (g.getGradeStatus() == Grade.GradeStatus.Incomplete) return "INC";
        if (g.getGradeStatus() == Grade.GradeStatus.Dropped)    return "DRP";
        if (g.getGradeStatus() == Grade.GradeStatus.NotYetGraded) return "—";
        return "";
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
