# Student Management System (SMS)
**St. Francis de Sales Major Seminary — IT2A Group 3**

A full-stack web application built with **HTML, CSS, JavaScript** (frontend) and **Java Spring Boot** (backend), connected to **MySQL** via phpMyAdmin.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML · CSS · JavaScript (Vanilla) |
| **Backend** | Java 17 · Spring Boot 3.2.3 |
| **Security** | Spring Security 6 (form login, BCrypt) |
| **Database** | MySQL 8 via phpMyAdmin (localhost:3306) |
| **ORM** | Spring Data JPA / Hibernate |
| **Build Tool** | Maven |

---

## How the Frontend and Backend Connect

```
Browser (HTML + CSS + JavaScript)
         ↕  fetch() / AJAX calls to /api/...
Spring Boot Backend (Java) on localhost:8080
         ↕  JPA / Hibernate queries
MySQL Database on localhost:3306 (phpMyAdmin)
```

The frontend files are plain HTML/CSS/JS stored in `static/`.
Spring Boot serves them automatically — no special configuration needed.
JavaScript uses `fetch()` to call Spring Boot's REST API endpoints,
which return JSON data that JavaScript then renders as HTML.

---

## Prerequisites

- Java 17+ (JDK) — download from https://adoptium.net
- Maven 3.8+ — download from https://maven.apache.org
- MySQL running locally via XAMPP / phpMyAdmin

---

## Setup Steps

### 1. Import the Database
Open phpMyAdmin → Create database `dbstudentmanagementsystem` → Import `dbstudentmanagementsystem.sql`

### 2. Create the Admin Account
In phpMyAdmin, run `seed_admin.sql` to create the default Registrar login:
- Username: `admin`
- Password: *(see `seed_admin.sql` — change immediately after first login)*

### 3. Configure Database Credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dbstudentmanagementsystem?useSSL=false&serverTimezone=Asia/Manila&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=        ← set your MySQL password here (blank for XAMPP default)
```

### 4. Run the Application
```bash
cd sms
mvn spring-boot:run
```

The first run takes 2–5 minutes — Maven downloads all dependencies automatically.

When you see this in the console, it's ready:
```
Started SmsApplication in X.XXX seconds
```

### 5. Open in Browser
Go to: **http://localhost:8080/login.html**

Login with the default admin credentials from `seed_admin.sql`. **Change the password immediately after first login.**

---

## Project Structure

```
sms/
├── pom.xml                                     ← Maven build file (dependencies)
├── seed_admin.sql                              ← SQL to create the first admin account
├── README.md
└── src/main/
    ├── java/com/seminary/sms/
    │   ├── SmsApplication.java                 ← Entry point (main method)
    │   ├── config/
    │   │   ├── SecurityConfig.java             ← Login, logout, role-based access
    │   │   └── StudentSecurity.java            ← Helper for @PreAuthorize checks
    │   ├── entity/                             ← 17 Java classes mapped to DB tables
    │   │   ├── User.java
    │   │   ├── Program.java
    │   │   ├── SchoolYear.java / Semester.java
    │   │   ├── Course.java / Prerequisite.java
    │   │   ├── Applicant.java / Application.java / EntranceExam.java
    │   │   ├── Student.java / Document.java / StudentSection.java
    │   │   ├── Instructor.java / Room.java
    │   │   ├── Section.java / Schedule.java
    │   │   ├── Enrollment.java / EnrollmentSubject.java
    │   │   ├── Grade.java
    │   │   └── Report.java / Alumni.java / BackupLog.java
    │   ├── repository/                         ← 17 Spring Data JPA interfaces (DB queries)
    │   ├── service/                            ← Business logic
    │   │   ├── StudentService.java             ← CRUD, account creation
    │   │   ├── EnrollmentService.java          ← Enrollment + prerequisite checking
    │   │   ├── GradeService.java               ← Grade entry + GWA computation
    │   │   ├── ScheduleService.java            ← Schedule + conflict detection
    │   │   ├── ApplicantService.java           ← Admissions pipeline
    │   │   └── AlumniService.java              ← Graduation + alumni management
    │   └── controller/                         ← REST API endpoints (/api/...)
    │       ├── MeController.java               ← GET /api/me (current user info)
    │       ├── PageController.java             ← Redirects browser to HTML files
    │       ├── DashboardController.java        ← GET /api/dashboard/stats
    │       ├── StudentApplicantControllers.java
    │       ├── EnrollmentGradeScheduleControllers.java
    │       ├── AdminControllers.java
    │       └── StudentMeAndBackupControllers.java
    └── resources/
        ├── application.properties              ← Database and server configuration
        └── static/                             ← FRONTEND FILES (HTML/CSS/JS)
            ├── login.html                      ← Login page
            ├── index.html                      ← Main SPA (all modules in one file)
            ├── css/
            │   └── style.css                   ← All styles
            └── js/
                ├── api.js                      ← fetch() helper, utility functions
                └── app.js                      ← All page logic, loaders, save handlers
```

---

## User Roles

| Role | Access |
|---|---|
| **Registrar** | Full access — all 15 modules |
| **Student** | My Grades · My Schedule · My Profile (read-only) |

---

## Modules (Registrar)

1. **Dashboard** — live stats, recent enrollments, program breakdown
2. **Applicants** — pipeline tracking, entrance exam recording
3. **Enrollment** — formal enrollment with section assignment
4. **Student Records** — full CRUD (no delete), Personal / Family / Religious / Grades tabs
5. **Alumni** — permanent records, graduate student action
6. **Curriculum** — PHILO & THEO programs, courses, prerequisite management
7. **Sections** — class sections per semester
8. **Scheduling** — weekly grid view, conflict detection (room + instructor)
9. **Grades** — 5-point scale entry, auto-compute final rating
10. **Instructors** — faculty CRUD
11. **Rooms** — facility CRUD
12. **Reports** — TOR, Grade Card, Summary, Certificate, Enrollment Stats, CHED
13. **User Accounts** — create, toggle active, reset password
14. **School Years** — semester management, set active semester
15. **Backup** — backup log, create/restore hooks

---

## Business Rules Implemented

- **Prerequisite enforcement** — blocks enrollment if prerequisite courses not passed
- **Grade auto-computation** — `finalRating = (midterm + final) / 2` · Passed if ≤ 3.0
- **No record deletion** — StudentService has no delete method (data retention policy)
- **Role-based access** — `@PreAuthorize` on every API endpoint
- **Conflict detection** — flags room and instructor double-booking on scheduling
- **Graduation flow** — moves student to Alumni status and creates Alumni record

---

## Grade Scale Reference

| Rating | Equivalent | Status |
|---|---|---|
| 1.00 | 97–100% | Passed (Excellent) |
| 1.25–1.50 | 91–96% | Passed |
| 1.75–2.00 | 85–90% | Passed |
| 2.25–2.50 | 79–84% | Passed |
| 2.75–3.00 | 75–78% | Passed (Minimum) |
| 5.00 | Below 75% | **Failed** |
| INC | — | Incomplete |

---

## Pending Implementations (Post-Demo)

| Feature | How to implement |
|---|---|
| PDF/XLSX Report generation | Add `iText` (PDF) or `Apache POI` (XLSX) Maven dependency + implement `ReportService` |
| mysqldump backup | Use `ProcessBuilder` in `BackupController.createBackup()` |
| Document file upload | Add `FileStorageService` with `MultipartFile` handling |


---

*IT2A Group 3 · St. Francis de Sales Major Seminary · AY 2025–2026*
