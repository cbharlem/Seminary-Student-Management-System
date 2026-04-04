# Database Quality Report
## Student Management System — St. Francis de Sales Major Seminary
### IT2A Group 3 | March 2026

---

## 1. NORMALIZATION ANALYSIS

### 1NF — First Normal Form
**Rule:** Every column must contain atomic (indivisible) values. No repeating groups. Every row must be unique.

| Table | Status | Notes |
|---|---|---|
| All 22 tables | ✅ PASS | Every column holds one value per row |
| tblstudents | ✅ PASS | Family, medical, religious fields are atomic |
| tblcourses | ✅ PASS | Each course is one row, no arrays or lists |
| tblprerequisites | ✅ PASS | Each prerequisite rule is a separate row |

**Verdict: All tables satisfy 1NF.**

---

### 2NF — Second Normal Form
**Rule:** Must satisfy 1NF. Every non-key column must depend on the ENTIRE primary key, not just part of it. (Only relevant for composite primary keys.)

All tables use a single-column `fldIndex` as the primary key. This means partial dependency is not possible by design — all non-key columns fully depend on the single `fldIndex`.

| Table | Status | Notes |
|---|---|---|
| All 22 tables | ✅ PASS | Single-column PK eliminates partial dependency entirely |

**Verdict: All tables satisfy 2NF.**

---

### 3NF — Third Normal Form
**Rule:** Must satisfy 2NF. No transitive dependencies — non-key columns must not depend on other non-key columns.

| Table | Status | Notes |
|---|---|---|
| tblstudents | ✅ PASS | All fields describe the student directly |
| tblcourses | ✅ PASS | All fields describe the course directly |
| tblenrollment | ✅ PASS | No field depends on another non-key field |
| tblgrades | ✅ PASS | fldFinalRating is computed but stored separately — acceptable for performance |
| tblsemester | ✅ PASS | fldSemesterLabel is derived from school year + semester number but stored for convenience — minor acceptable deviation |
| tblprogram | ✅ PASS | No transitive dependencies found |

**Minor Note:** `fldSemesterLabel` in `tblsemester` (e.g. "First Semester 2025-2026") could technically be derived from `fldSemesterNumber` and `fldYearLabel` in `tblschoolyear`. However, storing it is a practical decision to avoid complex string-building queries on every report. This is a widely accepted trade-off.

**Verdict: All tables satisfy 3NF.**

---

### BCNF — Boyce-Codd Normal Form
**Rule:** A stricter version of 3NF. For every functional dependency X → Y, X must be a superkey (a key that uniquely identifies the row).

| Table | Status | Notes |
|---|---|---|
| tblusers | ✅ PASS | fldUsername is UNIQUE — both fldIndex and fldUsername are superkeys |
| tblcourses | ✅ PASS | fldCourseCode is UNIQUE — both fldIndex and fldCourseCode are superkeys |
| tblprogram | ✅ PASS | fldProgramCode is UNIQUE — both are superkeys |
| tblprerequisites | ✅ PASS | Composite UNIQUE on (fldCourseIndex, fldPrerequisiteIndex) |
| tblstudentsection | ✅ PASS | Composite UNIQUE on (fldStudentIndex, fldSectionIndex, fldSemesterIndex) |
| tblenrollment | ✅ PASS | Composite UNIQUE on (fldStudentIndex, fldSemesterIndex) |
| tblenrollmentsubjects | ✅ PASS | Composite UNIQUE on (fldEnrollmentIndex, fldCourseIndex) |
| tblgrades | ✅ PASS | fldEnrollmentSubjectIndex is UNIQUE — one grade per subject enrollment |

**Verdict: All tables satisfy BCNF.**

---

### 4NF — Fourth Normal Form
**Rule:** Must satisfy BCNF. No multi-valued dependencies — a table should not store two or more independent multi-valued facts about the same entity.

| Table | Status | Notes |
|---|---|---|
| tblstudents | ✅ PASS | Personal, family, medical, religious info all describe ONE student — not independent facts |
| tblprerequisites | ✅ PASS | Each row is one specific prerequisite rule, not multiple independent facts |
| tblschedule | ✅ PASS | Each row ties one course + one instructor + one room + one time — all related facts |
| tblstudentsection | ✅ PASS | One student, one section, one semester per row |

**Potential area to watch:** `tblstudents` contains personal, family, medical, and religious background all in one table. In strict 4NF theory, these could be separated into sub-tables. However, for a system of this scale (seminary with ~100 users), keeping them together is a practical and widely accepted design decision that avoids unnecessary complexity.

**Verdict: All tables satisfy 4NF.**

---

## 2. REFERENTIAL INTEGRITY

**Rule:** Every foreign key must point to a valid, existing primary key. No orphaned records.

| Relationship | FK Column | References | Status |
|---|---|---|---|
| tblsemester → tblschoolyear | fldSchoolYearIndex | tblschoolyear.fldIndex | ✅ |
| tblcourses → tblprogram | fldProgramIndex | tblprogram.fldIndex | ✅ |
| tblprerequisites → tblcourses (x2) | fldCourseIndex, fldPrerequisiteIndex | tblcourses.fldIndex | ✅ |
| tblapplicants → tblprogram | fldProgramIndex | tblprogram.fldIndex | ✅ |
| tblapplications → tblapplicants | fldApplicantIndex | tblapplicants.fldIndex | ✅ |
| tblapplications → tblschoolyear | fldSchoolYearIndex | tblschoolyear.fldIndex | ✅ |
| tblentranceexam → tblapplicants | fldApplicantIndex | tblapplicants.fldIndex | ✅ |
| tblstudents → tblapplications | fldApplicationIndex | tblapplications.fldIndex | ✅ |
| tblstudents → tblusers | fldUserIndex | tblusers.fldIndex | ✅ |
| tblstudents → tblprogram | fldProgramIndex | tblprogram.fldIndex | ✅ |
| tbldocuments → tblstudents | fldStudentIndex | tblstudents.fldIndex | ✅ |
| tblsection → tblprogram | fldProgramIndex | tblprogram.fldIndex | ✅ |
| tblsection → tblsemester | fldSemesterIndex | tblsemester.fldIndex | ✅ |
| tblschedule → tblsection | fldSectionIndex | tblsection.fldIndex | ✅ |
| tblschedule → tblcourses | fldCourseIndex | tblcourses.fldIndex | ✅ |
| tblschedule → tblinstructors | fldInstructorIndex | tblinstructors.fldIndex | ✅ |
| tblschedule → tblrooms | fldRoomIndex | tblrooms.fldIndex | ✅ |
| tblstudentsection → tblstudents | fldStudentIndex | tblstudents.fldIndex | ✅ |
| tblstudentsection → tblsection | fldSectionIndex | tblsection.fldIndex | ✅ |
| tblstudentsection → tblsemester | fldSemesterIndex | tblsemester.fldIndex | ✅ |
| tblenrollment → tblstudents | fldStudentIndex | tblstudents.fldIndex | ✅ |
| tblenrollment → tblprogram | fldProgramIndex | tblprogram.fldIndex | ✅ |
| tblenrollment → tblsemester | fldSemesterIndex | tblsemester.fldIndex | ✅ |
| tblenrollmentsubjects → tblenrollment | fldEnrollmentIndex | tblenrollment.fldIndex | ✅ |
| tblenrollmentsubjects → tblcourses | fldCourseIndex | tblcourses.fldIndex | ✅ |
| tblenrollmentsubjects → tblschedule | fldScheduleIndex | tblschedule.fldIndex | ✅ |
| tblgrades → tblenrollmentsubjects | fldEnrollmentSubjectIndex | tblenrollmentsubjects.fldIndex | ✅ |
| tblgrades → tblstudents | fldStudentIndex | tblstudents.fldIndex | ✅ |
| tblgrades → tblcourses | fldCourseIndex | tblcourses.fldIndex | ✅ |
| tblgrades → tblsemester | fldSemesterIndex | tblsemester.fldIndex | ✅ |
| tblgrades → tblusers | fldEnteredByUserIndex | tblusers.fldIndex | ✅ |
| tblreports → tblstudents | fldStudentIndex | tblstudents.fldIndex | ✅ |
| tblreports → tblsemester | fldSemesterIndex | tblsemester.fldIndex | ✅ |
| tblreports → tblusers | fldGeneratedByIndex | tblusers.fldIndex | ✅ |
| tblalumni → tblstudents | fldStudentIndex | tblstudents.fldIndex | ✅ |
| tblalumni → tblprogram | fldProgramIndex | tblprogram.fldIndex | ✅ |
| tblbackuplog → tblusers | fldPerformedByIndex | tblusers.fldIndex | ✅ |

**Verdict: All 37 foreign key relationships are properly defined. Referential integrity is fully enforced.**

---

## 3. DATA CONSISTENCY

| Aspect | Status | Notes |
|---|---|---|
| Data Types | ✅ GOOD | INT for numbers, VARCHAR for IDs/names, DATE for dates, DECIMAL for grades, ENUM for fixed choices |
| Grade Scale | ✅ GOOD | DECIMAL(3,2) correctly stores 1.00 to 5.00 for the 5-point scale |
| ENUM Usage | ✅ GOOD | Used for fixed-choice fields: roles, statuses, document types, days of week |
| Timestamps | ✅ GOOD | fldCreatedAt and fldUpdatedAt present on all major tables |
| NULL Policy | ✅ GOOD | Optional fields are nullable; required fields are NOT NULL |
| Character Set | ✅ GOOD | utf8mb4 used throughout — supports Filipino special characters |

**Minor Recommendation:** `fldDuration` in `tblprogram` is stored as VARCHAR (e.g. "4 years"). Consider storing it as an INT (number of years) to make it easier to compute expected graduation dates programmatically.

---

## 4. NAMING CONVENTIONS

| Convention | Status | Notes |
|---|---|---|
| Table prefix `tbl` | ✅ CONSISTENT | All 22 tables use tbl prefix |
| Field prefix `fld` | ✅ CONSISTENT | All fields use fld prefix |
| PascalCase after prefix | ✅ CONSISTENT | e.g. fldStudentID, fldCreatedAt |
| Foreign key naming | ✅ CONSISTENT | All FK fields end in `Index` e.g. fldStudentIndex |
| ID field naming | ✅ CONSISTENT | All ID fields end in `ID` e.g. fldStudentID |
| Boolean fields | ✅ CONSISTENT | fldIsActive, fldIsActive use TINYINT(1) |

**Verdict: Naming conventions are clean and consistent throughout.**

---

## 5. REDUNDANCY CHECK

| Issue | Status | Notes |
|---|---|---|
| Duplicate student data | ✅ NONE | Student personal info stored only in tblstudents |
| Duplicate program data | ✅ NONE | Program info stored only in tblprogram |
| Overlapping enrollment tables | ✅ RESOLVED | tblenrollment (program-level) and tblenrollmentsubjects (subject-level) serve distinct purposes |
| fldStudentIndex in tblgrades | ⚠️ MINOR | fldStudentIndex in tblgrades can be derived through tblenrollmentsubjects → tblenrollment → tblstudents. Stored directly for query performance — acceptable trade-off |
| fldCourseIndex in tblgrades | ⚠️ MINOR | Same as above — derivable but stored for performance |

**Verdict: No significant redundancy. Minor denormalization in tblgrades is intentional and acceptable for performance.**

---

## 6. COMPLETENESS (vs. Scope Statement)

| Module | Required | Covered | Status |
|---|---|---|---|
| Admissions & Enrollment | Applicant tracking, exam results, status pipeline, classification | tblapplicants, tblapplications, tblentranceexam, tblenrollment | ✅ |
| Student Records | Full profile, document attachments, permanent retention | tblstudents, tbldocuments, tblalumni | ✅ |
| Curriculum & Programs | Programs, subjects, prerequisites | tblprogram, tblcourses, tblprerequisites | ✅ |
| Class & Scheduling | Sections, instructors, rooms, conflict detection | tblsection, tblschedule, tblinstructors, tblrooms | ✅ |
| Grades Management | 5-point scale, incomplete/failed tracking | tblgrades, tblenrollmentsubjects | ✅ |
| Reports & Outputs | Report audit log, PDF/Excel tracking | tblreports | ✅ |
| User Access Control | Registrar and Student roles, login | tblusers | ✅ |
| Alumni Records | Permanent archiving | tblalumni | ✅ |
| Backup & Restore | Backup logging | tblbackuplog | ✅ |

**Verdict: Database fully covers all 7 modules and supporting requirements from the scope statement.**

---

## 7. SCALABILITY

| Aspect | Status | Notes |
|---|---|---|
| INT(11) for fldIndex | ✅ GOOD | Supports up to ~2.1 billion rows per table — far beyond seminary needs |
| School year / semester structure | ✅ GOOD | New school years and semesters can be added without schema changes |
| Program expansion | ✅ GOOD | New programs can be added to tblprogram without affecting other tables |
| Multiple sections per semester | ✅ GOOD | tblsection supports unlimited sections per program per semester |
| Concurrent users | ✅ GOOD | InnoDB engine supports row-level locking — handles 100 concurrent users easily |
| Future modules | ✅ GOOD | Schema is modular — new tables can be added without breaking existing ones |

**Verdict: Database is well-structured for the seminary's current and future needs.**

---

## 8. SECURITY CONSIDERATIONS

| Aspect | Status | Notes |
|---|---|---|
| Password storage | ⚠️ REMINDER | fldPasswordHash must store BCrypt hashed passwords — never plain text |
| Role enforcement | ✅ GOOD | fldRole ENUM limits values to 'Registrar' and 'Student' only |
| Soft delete policy | ✅ GOOD | fldCurrentStatus in tblstudents prevents hard deletion per scope requirement |
| Audit trail | ✅ GOOD | fldCreatedAt and fldUpdatedAt on all major tables |
| Grade entry tracking | ✅ GOOD | fldEnteredByUserIndex and fldDateEntered in tblgrades tracks who entered grades |
| Report generation tracking | ✅ GOOD | tblreports logs who generated what and when |
| Backup logging | ✅ GOOD | tblbackuplog records all backup activities |

**Key Reminder:** Password hashing must be enforced at the Spring Boot backend level using BCryptPasswordEncoder. The database alone cannot enforce this.

---

## 9. SAMPLE DATA QUALITY

| Aspect | Status | Notes |
|---|---|---|
| 10 applicants | ✅ GOOD | Realistic Filipino names |
| 10 applications | ✅ GOOD | All linked correctly to applicants |
| 10 students | ✅ GOOD | All linked correctly to applications |
| 10 enrollments | ✅ GOOD | Properly distributed across programs and semesters |
| 10 section assignments | ✅ GOOD | Correctly assigned to existing sections |
| 2 programs | ✅ GOOD | Philosophy and Theology as per scope |
| 10 courses | ✅ GOOD | Distributed across both programs and year levels |
| 3 prerequisite rules | ✅ GOOD | Realistic examples provided |
| 4 sections | ✅ GOOD | Two per program for the current semester |
| School year & semesters | ✅ GOOD | 2025-2026 with First and Second semester |

**Minor Note:** No sample data for tblinstructors, tblrooms, and tblschedule since these depend on actual seminary faculty and facility data. These should be populated during the data migration phase.

---

## 10. OVERALL SUMMARY

| Category | Rating | Notes |
|---|---|---|
| 1NF | ✅ PASS | All tables atomic and unique |
| 2NF | ✅ PASS | No partial dependencies |
| 3NF | ✅ PASS | No transitive dependencies |
| BCNF | ✅ PASS | All determinants are superkeys |
| 4NF | ✅ PASS | No multi-valued dependencies |
| Referential Integrity | ✅ EXCELLENT | All 37 FK relationships enforced |
| Data Consistency | ✅ GOOD | Proper data types throughout |
| Naming Conventions | ✅ EXCELLENT | Fully consistent |
| Redundancy | ✅ MINIMAL | Only intentional performance denormalization |
| Completeness | ✅ EXCELLENT | All 7 scope modules covered |
| Scalability | ✅ GOOD | Designed to grow with the seminary |
| Security | ✅ GOOD | Pending BCrypt enforcement at backend level |
| Sample Data | ✅ GOOD | Realistic and properly linked |

### Final Verdict
The database is well-designed, fully normalized up to 4NF, properly structured for the seminary's needs, and aligned with all requirements in the Project Scope Statement. It is ready for backend development.
