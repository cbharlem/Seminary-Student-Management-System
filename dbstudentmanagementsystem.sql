-- ============================================================
-- Student Management System - St. Francis de Sales Major Seminary
-- Database Schema
-- Designed for: IT2A Group 3
-- Based on: Project Scope Statement (March 7, 2026)
-- ============================================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";
SET NAMES utf8mb4;

-- ============================================================
-- DATABASE
-- ============================================================
CREATE DATABASE IF NOT EXISTS `dbstudentmanagementsystem`;
USE `dbstudentmanagementsystem`;

-- ============================================================
-- DROP TABLES (reverse order to respect foreign keys)
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `tblbackuplog`;
DROP TABLE IF EXISTS `tblalumni`;
DROP TABLE IF EXISTS `tblreports`;
DROP TABLE IF EXISTS `tblgrades`;
DROP TABLE IF EXISTS `tblenrollmentsubjects`;
DROP TABLE IF EXISTS `tblenrollment`;
DROP TABLE IF EXISTS `tblstudentsection`;
DROP TABLE IF EXISTS `tblschedule`;
DROP TABLE IF EXISTS `tblsection`;
DROP TABLE IF EXISTS `tblrooms`;
DROP TABLE IF EXISTS `tblinstructors`;
DROP TABLE IF EXISTS `tbldocuments`;
DROP TABLE IF EXISTS `tblstudents`;
DROP TABLE IF EXISTS `tblentranceexam`;
DROP TABLE IF EXISTS `tblonline_submissions`;
DROP TABLE IF EXISTS `tblapplications`;
DROP TABLE IF EXISTS `tblapplicants`;
DROP TABLE IF EXISTS `tblprerequisites`;
DROP TABLE IF EXISTS `tblcourses`;
DROP TABLE IF EXISTS `tblsemester`;
DROP TABLE IF EXISTS `tblschoolyear`;
DROP TABLE IF EXISTS `tblprogram`;
DROP TABLE IF EXISTS `tblusers`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- MODULE 7: USER ACCESS CONTROL
-- ============================================================

CREATE TABLE `tblusers` (
  `fldIndex`        INT(11) NOT NULL AUTO_INCREMENT,
  `fldUserID`       VARCHAR(30) NOT NULL UNIQUE,
  `fldUsername`     VARCHAR(50) NOT NULL UNIQUE,
  `fldPasswordHash` VARCHAR(255) NOT NULL,
  `fldRole`         ENUM('Registrar','Student') NOT NULL,
  `fldIsActive`     TINYINT(1) NOT NULL DEFAULT 1,
  `fldCreatedAt`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MODULE 3: CURRICULUM & PROGRAM MANAGEMENT
-- ============================================================

CREATE TABLE `tblprogram` (
  `fldIndex`       INT(11) NOT NULL AUTO_INCREMENT,
  `fldProgramID`   VARCHAR(30) NOT NULL UNIQUE,
  `fldProgramCode` VARCHAR(30) NOT NULL UNIQUE,
  `fldProgramName` VARCHAR(100) NOT NULL,
  `fldTotalUnits`  INT(11) NOT NULL,
  `fldDuration`    VARCHAR(20) NOT NULL COMMENT 'e.g. 4 years, 4-6 years',
  `fldIsActive`    TINYINT(1) NOT NULL DEFAULT 1,
  `fldCreatedAt`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblprogram` (`fldProgramID`,`fldProgramCode`,`fldProgramName`,`fldTotalUnits`,`fldDuration`,`fldIsActive`) VALUES
('PRG-1001','PHILO','Bachelor of Arts in Philosophy',177,'4 years',1),
('PRG-1002','THEO','Bachelor of Arts in Theology',130,'4-6 years',1);

-- ============================================================

CREATE TABLE `tblschoolyear` (
  `fldIndex`        INT(11) NOT NULL AUTO_INCREMENT,
  `fldSchoolYearID` VARCHAR(20) NOT NULL UNIQUE,
  `fldYearLabel`    VARCHAR(20) NOT NULL COMMENT 'e.g. 2025-2026',
  `fldIsActive`     TINYINT(1) NOT NULL DEFAULT 0,
  `fldCreatedAt`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblschoolyear` (`fldSchoolYearID`,`fldYearLabel`,`fldIsActive`) VALUES
('SY-2526','2025-2026',1);

-- ============================================================

CREATE TABLE `tblsemester` (
  `fldIndex`          INT(11) NOT NULL AUTO_INCREMENT,
  `fldSemesterID`     VARCHAR(20) NOT NULL UNIQUE,
  `fldSchoolYearIndex` INT(11) NOT NULL COMMENT 'FK to tblschoolyear.fldIndex',
  `fldSemesterNumber` TINYINT(1) NOT NULL COMMENT '1 = First, 2 = Second, 3 = Summer',
  `fldSemesterLabel`  VARCHAR(50) NOT NULL COMMENT 'e.g. First Semester 2025-2026',
  `fldStartDate`      DATE NOT NULL,
  `fldEndDate`        DATE NOT NULL,
  `fldIsActive`       TINYINT(1) NOT NULL DEFAULT 0,
  `fldCreatedAt`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldSchoolYearIndex`) REFERENCES `tblschoolyear`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblsemester` (`fldSemesterID`,`fldSchoolYearIndex`,`fldSemesterNumber`,`fldSemesterLabel`,`fldStartDate`,`fldEndDate`,`fldIsActive`) VALUES
('SEM-2526-1',1,1,'First Semester 2025-2026','2025-08-01','2025-12-31',0),
('SEM-2526-2',1,2,'Second Semester 2025-2026','2026-01-01','2026-05-31',1);

-- ============================================================

CREATE TABLE `tblcourses` (
  `fldIndex`          INT(11) NOT NULL AUTO_INCREMENT,
  `fldCourseID`       VARCHAR(30) NOT NULL UNIQUE,
  `fldCourseCode`     VARCHAR(30) NOT NULL UNIQUE,
  `fldCourseName`     VARCHAR(100) NOT NULL,
  `fldUnits`          INT(11) NOT NULL,
  `fldProgramIndex`   INT(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldYearLevel`      TINYINT(1) NOT NULL COMMENT '1 to 4 (or 6 for Theology)',
  `fldSemesterNumber` TINYINT(1) NOT NULL COMMENT '1 = First, 2 = Second',
  `fldIsActive`       TINYINT(1) NOT NULL DEFAULT 1,
  `fldCreatedAt`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- BA in Philosophy — Full Curriculum (CHED-approved, AY 2023-2024)
-- Program index 1 = BA Philosophy (PRG-1001)
-- Courses are ordered by year level then semester for readability.
INSERT INTO `tblcourses` (`fldCourseID`,`fldCourseCode`,`fldCourseName`,`fldUnits`,`fldProgramIndex`,`fldYearLevel`,`fldSemesterNumber`) VALUES
-- 1st Year, 1st Semester (index 1-9)
('CRS001','GE1','Understanding the Self',3,1,1,1),
('CRS002','GE2','Readings in the Philippine History',3,1,1,1),
('CRS003','GE3','Purposive Communication',3,1,1,1),
('CRS004','GE4','Science, Technology and Society',3,1,1,1),
('CRS005','PHILO1','Logic (Symbolic)',3,1,1,1),
('CRS006','THEO1','Creed',3,1,1,1),
('CRS007','LAT1','Latin 1',3,1,1,1),
('CRS008','PE1','Movement Competency Training',2,1,1,1),
('CRS009','CWTS1','Civic Welfare Training Service 1',3,1,1,1),
-- 1st Year, 2nd Semester (index 10-17)
('CRS010','GE5','The Contemporary World',3,1,1,2),
('CRS011','GE6','Mathematics in the Modern World',3,1,1,2),
('CRS012','PHILO2','Introduction to Philosophy (Ancient Philosophy)',3,1,1,2),
('CRS013','PHILO7','History of Oriental Philosophy 1',3,1,1,2),
('CRS014','THEO2','Sacraments and Liturgy',3,1,1,2),
('CRS015','LAT2','Latin 2',3,1,1,2),
('CRS016','PE2','Exercise-Based Fitness Activities',2,1,1,2),
('CRS017','CWTS2','Civic Welfare Training Service 2',3,1,1,2),
-- 2nd Year, 1st Semester (index 18-26)
('CRS018','RIZAL','Life and Works of Jose Rizal',3,1,2,1),
('CRS019','GE7','Art Appreciation/Aesthetics 1',3,1,2,1),
('CRS020','GE8','Great Books',3,1,2,1),
('CRS021','PHILO8','Philosophy of Man 1',3,1,2,1),
('CRS022','PHILO20','Seminar on Plato/Aristotle',3,1,2,1),
('CRS023','PHILO23','History of Oriental Philosophy 2',3,1,2,1),
('CRS024','THEO3','Morals of the Heart (Commandments)',3,1,2,1),
('CRS025','FEELEC1','Spanish',3,1,2,1),
('CRS026','PE3','Dance Activities',2,1,2,1),
-- 2nd Year, 2nd Semester (index 27-33)
('CRS027','FIL1','Pagbasa at Pagsulat Tungo sa Pananaliksik',3,1,2,2),
('CRS028','PHILO3','History of Western Philosophy 1 (Medieval)',3,1,2,2),
('CRS029','PHILO9','Philosophical Anthropology (Philosophy of Man 2)',3,1,2,2),
('CRS030','PHILO16','Epistemology',3,1,2,2),
('CRS031','PHILO21','Cosmology/Philosophy of Science and Technology',3,1,2,2),
('CRS032','GE9','Gender and Development',3,1,2,2),
('CRS033','PE4','Sports (Basketball)',2,1,2,2),
-- 3rd Year, 1st Semester (index 34-41)
('CRS034','PHILO4','History of Western Philosophy 2 (Modern and Contemporary)',3,1,3,1),
('CRS035','PHILO10','Social/Political Philosophy',3,1,3,1),
('CRS036','GE10','Ethics',3,1,3,1),
('CRS037','PHILO14','Metaphysics 1',3,1,3,1),
('CRS038','PHILO22','Seminar on Filipino Philosophy',3,1,3,1),
('CRS039','PHILO24','Art Appreciation/Aesthetics 2',3,1,3,1),
('CRS040','THEO4','Scriptures (Old and New Testament)',3,1,3,1),
('CRS041','MUSIC1','Fundamentals of Music 1',1,1,3,1),
-- 3rd Year, 2nd Semester (index 42-49)
('CRS042','PHILO5','Existentialism, Phenomenology, Hermeneutics, Post Modernism',3,1,3,2),
('CRS043','PHILO6','Modern Asia Thoughts',3,1,3,2),
('CRS044','PHILO12','Special Questions in Ethics',3,1,3,2),
('CRS045','PHILO15','Metaphysics 2',3,1,3,2),
('CRS046','PHILO18','Comparative Philosophy (East-West)',3,1,3,2),
('CRS047','PHILO28W','Philosophy Thesis Writing',3,1,3,2),
('CRS048','ELEC1','Philippine Sociology',3,1,3,2),
('CRS049','MUSIC2','Fundamentals of Music 2',1,1,3,2),
-- 4th Year, 1st Semester (index 50-56)
('CRS050','FIL2','Komunikasyon sa Akademikong Filipino',3,1,4,1),
('CRS051','GE11','Environmental Science',3,1,4,1),
('CRS052','PHILO25','Special Questions in Philosophy',3,1,4,1),
('CRS053','PHILO26','Seminar on Contemporary Philosophy',3,1,4,1),
('CRS054','ELEC2','World and Philippine Literature',3,1,4,1),
('CRS055','EDUC6','Assessment in Learning',3,1,4,1),
('CRS056','MUSIC3','Gregorian Chant',1,1,4,1),
-- 4th Year, 2nd Semester (index 57-63)
('CRS057','FIL3','Filipino sa Iba''t ibang Disiplina',3,1,4,2),
('CRS058','PHILO17','Theodicy/Philosophy of Religion',3,1,4,2),
('CRS059','PHILO19','Philosophy of Language',3,1,4,2),
('CRS060','PHILO28S','Philosophy Synthesis (Thesis Defense and Comprehensive Exam)',3,1,4,2),
('CRS061','ELEC3','Thomas Aquinas',3,1,4,2),
('CRS062','FEELEC2','St. Francis de Sales',3,1,4,2),
('CRS063','MUSIC4','Music in the Liturgy',1,1,4,2);

-- ============================================================

CREATE TABLE `tblprerequisites` (
  `fldIndex`              INT(11) NOT NULL AUTO_INCREMENT,
  `fldPrerequisiteID`     VARCHAR(30) NOT NULL UNIQUE,
  `fldCourseIndex`        INT(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex - course being taken',
  `fldPrerequisiteIndex`  INT(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex - course that must be passed first',
  `fldCreatedAt`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  UNIQUE KEY `uq_prereq` (`fldCourseIndex`,`fldPrerequisiteIndex`),
  FOREIGN KEY (`fldCourseIndex`)       REFERENCES `tblcourses`(`fldIndex`),
  FOREIGN KEY (`fldPrerequisiteIndex`) REFERENCES `tblcourses`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Prerequisites for BA Philosophy curriculum.
-- Course indices match the insertion order in tblcourses above (1-63).
-- Courses with "All Philosophy Subjects taken" as prereq (PHILO25, PHILO28W)
-- are enforced administratively and are not listed here.
INSERT INTO `tblprerequisites` (`fldPrerequisiteID`,`fldCourseIndex`,`fldPrerequisiteIndex`) VALUES
-- 1st Year 2nd Sem: prereqs from 1Y1S
('PRQ-001',12,5),   -- PHILO2 → PHILO1 (Logic)
('PRQ-002',13,5),   -- PHILO7 → PHILO1 (Logic)
('PRQ-003',14,6),   -- THEO2 → THEO1
('PRQ-004',15,7),   -- LAT2 → LAT1
('PRQ-005',16,8),   -- PE2 → PE1
('PRQ-006',17,9),   -- CWTS2 → CWTS1
-- 2nd Year 1st Sem: prereqs from 1Y2S
('PRQ-007',21,12),  -- PHILO8 → PHILO2
('PRQ-008',22,12),  -- PHILO20 → PHILO2
('PRQ-009',23,13),  -- PHILO23 → PHILO7
('PRQ-010',24,14),  -- THEO3 → THEO2
('PRQ-011',26,16),  -- PE3 → PE2
-- 2nd Year 2nd Sem: prereqs from 1Y2S and 2Y1S
('PRQ-012',28,12),  -- PHILO3 → PHILO2 (Ancient Philosophy precedes Medieval)
('PRQ-013',29,21),  -- PHILO9 → PHILO8
('PRQ-014',30,12),  -- PHILO16 → PHILO2
('PRQ-015',30,22),  -- PHILO16 → PHILO20
('PRQ-016',31,12),  -- PHILO21 → PHILO2
('PRQ-017',31,28),  -- PHILO21 → PHILO3
('PRQ-018',33,26),  -- PE4 → PE3
-- 3rd Year 1st Sem: prereqs from 2Y semesters
('PRQ-019',34,12),  -- PHILO4 → PHILO2
('PRQ-020',34,28),  -- PHILO4 → PHILO3
('PRQ-021',35,12),  -- PHILO10 → PHILO2
('PRQ-022',37,30),  -- PHILO14 → PHILO16 (Epistemology)
('PRQ-023',38,13),  -- PHILO22 → PHILO7
('PRQ-024',38,23),  -- PHILO22 → PHILO23
('PRQ-025',39,19),  -- PHILO24 → GE7 (Aesthetics 1)
('PRQ-026',40,24),  -- THEO4 → THEO3
-- 3rd Year 2nd Sem: prereqs from 3Y1S and earlier
('PRQ-027',42,34),  -- PHILO5 → PHILO4
('PRQ-028',43,13),  -- PHILO6 → PHILO7
('PRQ-029',43,23),  -- PHILO6 → PHILO23
('PRQ-030',43,34),  -- PHILO6 → PHILO4
('PRQ-031',44,36),  -- PHILO12 → GE10 (Ethics)
('PRQ-032',45,37),  -- PHILO15 → PHILO14
('PRQ-033',46,13),  -- PHILO18 → PHILO7
('PRQ-034',46,23),  -- PHILO18 → PHILO23
('PRQ-035',46,34),  -- PHILO18 → PHILO4
('PRQ-036',49,41),  -- MUSIC2 → MUSIC1
-- 4th Year 1st Sem
('PRQ-037',50,19),  -- FIL2 → GE7
('PRQ-038',53,34),  -- PHILO26 → PHILO4
('PRQ-039',56,49),  -- MUSIC3 → MUSIC2
-- 4th Year 2nd Sem
('PRQ-040',57,20),  -- FIL3 → GE8
('PRQ-041',58,45),  -- PHILO17 → PHILO15
('PRQ-042',59,42),  -- PHILO19 → PHILO5
('PRQ-043',60,47),  -- PHILO28S → PHILO28W
('PRQ-044',63,56);  -- MUSIC4 → MUSIC3

-- ============================================================
-- MODULE 1: ADMISSIONS & ENROLLMENT
-- ============================================================

CREATE TABLE `tblapplicants` (
  `fldIndex`              INT(11) NOT NULL AUTO_INCREMENT,
  `fldApplicantID`        VARCHAR(30) NOT NULL UNIQUE,
  `fldFirstName`          VARCHAR(50) NOT NULL,
  `fldMiddleName`         VARCHAR(50) DEFAULT NULL,
  `fldLastName`           VARCHAR(50) NOT NULL,
  `fldDateOfBirth`        DATE NOT NULL,
  `fldPlaceOfBirth`       VARCHAR(100) DEFAULT NULL,
  `fldGender`             ENUM('Male','Female','Other') DEFAULT NULL,
  `fldAddress`            VARCHAR(255) DEFAULT NULL,
  `fldContactNumber`      VARCHAR(20) DEFAULT NULL,
  `fldEmail`              VARCHAR(100) NOT NULL,
  `fldNationality`        VARCHAR(50) DEFAULT NULL,
  `fldReligion`           VARCHAR(50) DEFAULT NULL,
  `fldFatherName`         VARCHAR(100) DEFAULT NULL,
  `fldFatherOccupation`   VARCHAR(100) DEFAULT NULL,
  `fldMotherName`         VARCHAR(100) DEFAULT NULL,
  `fldMotherOccupation`   VARCHAR(100) DEFAULT NULL,
  `fldGuardianName`       VARCHAR(100) DEFAULT NULL,
  `fldGuardianContact`    VARCHAR(20) DEFAULT NULL,
  `fldLastSchoolAttended` VARCHAR(150) DEFAULT NULL,
  `fldLastSchoolYear`     VARCHAR(20) DEFAULT NULL,
  `fldLastYearLevel`      VARCHAR(50) DEFAULT NULL,
  `fldSeminaryLevel`      ENUM('Propaedeutic','College') DEFAULT NULL,
  `fldProgramIndex`       INT(11) DEFAULT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldCreatedAt`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblapplicants` (`fldApplicantID`,`fldFirstName`,`fldLastName`,`fldDateOfBirth`,`fldEmail`,`fldSeminaryLevel`,`fldProgramIndex`) VALUES
('P-1001','John','Cruz','2003-05-14','john.cruz@email.com','College',1),
('P-1002','Maria','Santos','2004-08-22','maria.santos@email.com','College',2),
('P-1003','Kevin','Reyes','2002-11-03','kevin.reyes@email.com','College',1),
('P-1004','Anna','Dela Cruz','2003-02-19','anna.delacruz@email.com','College',2),
('P-1005','Mark','Villanueva','2001-09-30','mark.v@email.com','Propaedeutic',1),
('P-1006','Julia','Mendoza','2004-06-07','julia.m@email.com','College',2),
('P-1007','Ryan','Garcia','2002-12-25','ryan.lopez@email.com','College',1),
('P-1008','Sofia','Garcia','2003-04-11','sofia.g@email.com','Propaedeutic',2),
('P-1009','Daniel','Flores','2001-10-05','daniel.f@email.com','College',1),
('P-1010','Bianca','Navarro','2004-01-28','bianca.n@email.com','College',2);

-- ============================================================

CREATE TABLE `tblapplications` (
  `fldIndex`              INT(11) NOT NULL AUTO_INCREMENT,
  `fldApplicationID`      VARCHAR(30) NOT NULL UNIQUE,
  `fldApplicantIndex`     INT(11) NOT NULL COMMENT 'FK to tblapplicants.fldIndex',
  `fldApplicationDate`    DATE NOT NULL,
  `fldSchoolYearIndex`    INT(11) NOT NULL COMMENT 'FK to tblschoolyear.fldIndex',
  `fldApplicationStatus`  ENUM(
    'Applied',
    'Interviewed',
    'AspiringConventionAttended',
    'Confirmed',
    'Enrolled',
    'Rejected',
    'Withdrawn'
  ) NOT NULL DEFAULT 'Applied',
  `fldInterviewDate`      DATE DEFAULT NULL,
  `fldConventionDate`     DATE DEFAULT NULL,
  `fldRejectionReason`    VARCHAR(255) DEFAULT NULL,
  `fldRemarks`            TEXT DEFAULT NULL,
  `fldCreatedAt`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldApplicantIndex`)  REFERENCES `tblapplicants`(`fldIndex`),
  FOREIGN KEY (`fldSchoolYearIndex`) REFERENCES `tblschoolyear`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblapplications` (`fldApplicationID`,`fldApplicantIndex`,`fldApplicationDate`,`fldSchoolYearIndex`,`fldApplicationStatus`) VALUES
('APP-1001',1,'2025-01-10',1,'Enrolled'),
('APP-1002',2,'2025-01-11',1,'Enrolled'),
('APP-1003',3,'2025-01-12',1,'Enrolled'),
('APP-1004',4,'2025-01-13',1,'Enrolled'),
('APP-1005',5,'2025-01-14',1,'Enrolled'),
('APP-1006',6,'2025-01-15',1,'Enrolled'),
('APP-1007',7,'2025-01-16',1,'Enrolled'),
('APP-1008',8,'2025-01-17',1,'Enrolled'),
('APP-1009',9,'2025-01-18',1,'Enrolled'),
('APP-1010',10,'2025-01-19',1,'Enrolled');

-- ============================================================

CREATE TABLE `tblentranceexam` (
  `fldIndex`          INT(11) NOT NULL AUTO_INCREMENT,
  `fldExamID`         VARCHAR(30) NOT NULL UNIQUE,
  `fldApplicantIndex` INT(11) NOT NULL COMMENT 'FK to tblapplicants.fldIndex',
  `fldExamDate`       DATE NOT NULL,
  `fldScore`          DECIMAL(5,2) DEFAULT NULL,
  `fldMaxScore`       DECIMAL(5,2) DEFAULT NULL,
  `fldResult`         ENUM('Passed','Failed','Pending') NOT NULL DEFAULT 'Pending',
  `fldRemarks`        TEXT DEFAULT NULL,
  `fldCreatedAt`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldApplicantIndex`) REFERENCES `tblapplicants`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MODULE 2: STUDENT RECORDS MANAGEMENT
-- ============================================================

CREATE TABLE `tblstudents` (
  `fldIndex`              INT(11) NOT NULL AUTO_INCREMENT,
  `fldStudentID`          VARCHAR(30) NOT NULL UNIQUE,
  `fldApplicationIndex`   INT(11) NOT NULL UNIQUE COMMENT 'FK to tblapplications.fldIndex',
  `fldUserIndex`          INT(11) DEFAULT NULL COMMENT 'FK to tblusers.fldIndex',
  `fldFirstName`          VARCHAR(50) NOT NULL,
  `fldMiddleName`         VARCHAR(50) DEFAULT NULL,
  `fldLastName`           VARCHAR(50) NOT NULL,
  `fldDateOfBirth`        DATE NOT NULL,
  `fldPlaceOfBirth`       VARCHAR(100) DEFAULT NULL,
  `fldGender`             ENUM('Male','Female','Other') DEFAULT NULL,
  `fldAddress`            VARCHAR(255) DEFAULT NULL,
  `fldContactNumber`      VARCHAR(20) DEFAULT NULL,
  `fldEmail`              VARCHAR(100) NOT NULL,
  `fldNationality`        VARCHAR(50) DEFAULT NULL,
  `fldReligion`           VARCHAR(50) DEFAULT NULL,
  `fldFatherName`         VARCHAR(100) DEFAULT NULL,
  `fldFatherOccupation`   VARCHAR(100) DEFAULT NULL,
  `fldMotherName`         VARCHAR(100) DEFAULT NULL,
  `fldMotherOccupation`   VARCHAR(100) DEFAULT NULL,
  `fldGuardianName`       VARCHAR(100) DEFAULT NULL,
  `fldGuardianContact`    VARCHAR(20) DEFAULT NULL,
  `fldBloodType`          VARCHAR(5) DEFAULT NULL,
  `fldMedicalConditions`  TEXT DEFAULT NULL,
  `fldAllergies`          TEXT DEFAULT NULL,
  `fldBaptismDate`        DATE DEFAULT NULL,
  `fldBaptismChurch`      VARCHAR(150) DEFAULT NULL,
  `fldConfirmationDate`   DATE DEFAULT NULL,
  `fldConfirmationChurch` VARCHAR(150) DEFAULT NULL,
  `fldParishPriest`       VARCHAR(100) DEFAULT NULL,
  `fldDiocese`            VARCHAR(100) DEFAULT NULL,
  `fldSeminaryLevel`      ENUM('Propaedeutic','College') NOT NULL,
  `fldCurrentYearLevel`   TINYINT(1) NOT NULL DEFAULT 1,
  `fldCurrentStatus`      ENUM('Active','Inactive','LOA','Dismissed','Graduated','Alumni') NOT NULL DEFAULT 'Active',
  `fldProgramIndex`       INT(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldCreatedAt`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldApplicationIndex`) REFERENCES `tblapplications`(`fldIndex`),
  FOREIGN KEY (`fldUserIndex`)        REFERENCES `tblusers`(`fldIndex`),
  FOREIGN KEY (`fldProgramIndex`)     REFERENCES `tblprogram`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblstudents` (`fldStudentID`,`fldApplicationIndex`,`fldFirstName`,`fldLastName`,`fldDateOfBirth`,`fldEmail`,`fldSeminaryLevel`,`fldCurrentYearLevel`,`fldCurrentStatus`,`fldProgramIndex`) VALUES
('S2026-001',1,'John','Cruz','2003-05-14','john.cruz@email.com','College',1,'Active',1),
('S2026-002',2,'Maria','Santos','2004-08-22','maria.santos@email.com','College',1,'Active',2),
('S2026-003',3,'Kevin','Reyes','2002-11-03','kevin.reyes@email.com','College',1,'Active',1),
('S2026-004',4,'Anna','Dela Cruz','2003-02-19','anna.delacruz@email.com','College',1,'Active',2),
('S2026-005',5,'Mark','Villanueva','2001-09-30','mark.v@email.com','Propaedeutic',1,'Active',1),
('S2026-006',6,'Julia','Mendoza','2004-06-07','julia.m@email.com','College',1,'Active',2),
('S2026-007',7,'Ryan','Garcia','2002-12-25','ryan.lopez@email.com','College',1,'Active',1),
('S2026-008',8,'Sofia','Garcia','2003-04-11','sofia.g@email.com','Propaedeutic',1,'Active',2),
('S2026-009',9,'Daniel','Flores','2001-10-05','daniel.f@email.com','College',1,'Active',1),
('S2026-010',10,'Bianca','Navarro','2004-01-28','bianca.n@email.com','College',1,'Active',2);

-- ============================================================

CREATE TABLE `tbldocuments` (
  `fldIndex`        INT(11) NOT NULL AUTO_INCREMENT,
  `fldDocumentID`   VARCHAR(30) NOT NULL UNIQUE,
  `fldStudentIndex` INT(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldDocumentType` ENUM(
    'BirthCertificate',
    'Form137',
    'Diploma',
    'BaptismalRecord',
    'ConfirmationRecord',
    'MarriageContractOfParents',
    'MedicalRecord',
    'DentalRecord',
    'ParishPriestRecommendation',
    'Other'
  ) NOT NULL,
  `fldFileName`     VARCHAR(255) NOT NULL,
  `fldFilePath`     VARCHAR(500) NOT NULL,
  `fldUploadedAt`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldRemarks`      VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldStudentIndex`) REFERENCES `tblstudents`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MODULE 4: CLASS & SCHEDULE MANAGEMENT
-- ============================================================

CREATE TABLE `tblinstructors` (
  `fldIndex`          INT(11) NOT NULL AUTO_INCREMENT,
  `fldInstructorID`   VARCHAR(30) NOT NULL UNIQUE,
  `fldFirstName`      VARCHAR(50) NOT NULL,
  `fldMiddleName`     VARCHAR(50) DEFAULT NULL,
  `fldLastName`       VARCHAR(50) NOT NULL,
  `fldEmail`          VARCHAR(100) DEFAULT NULL,
  `fldContactNumber`  VARCHAR(20) DEFAULT NULL,
  `fldSpecialization` VARCHAR(100) DEFAULT NULL,
  `fldIsActive`       TINYINT(1) NOT NULL DEFAULT 1,
  `fldCreatedAt`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================

CREATE TABLE `tblrooms` (
  `fldIndex`     INT(11) NOT NULL AUTO_INCREMENT,
  `fldRoomID`    VARCHAR(30) NOT NULL UNIQUE,
  `fldRoomName`  VARCHAR(50) NOT NULL,
  `fldBuilding`  VARCHAR(50) DEFAULT NULL,
  `fldCapacity`  INT(11) DEFAULT NULL,
  `fldIsActive`  TINYINT(1) NOT NULL DEFAULT 1,
  `fldCreatedAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================

CREATE TABLE `tblsection` (
  `fldIndex`        INT(11) NOT NULL AUTO_INCREMENT,
  `fldSectionID`    VARCHAR(30) NOT NULL UNIQUE,
  `fldSectionCode`  VARCHAR(30) NOT NULL,
  `fldSectionName`  VARCHAR(50) NOT NULL,
  `fldProgramIndex` INT(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldYearLevel`    TINYINT(1) NOT NULL,
  `fldSemesterIndex` INT(11) NOT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldCapacity`     INT(11) DEFAULT 40,
  `fldIsActive`     TINYINT(1) NOT NULL DEFAULT 1,
  `fldCreatedAt`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldProgramIndex`)  REFERENCES `tblprogram`(`fldIndex`),
  FOREIGN KEY (`fldSemesterIndex`) REFERENCES `tblsemester`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblsection` (`fldSectionID`,`fldSectionCode`,`fldSectionName`,`fldProgramIndex`,`fldYearLevel`,`fldSemesterIndex`) VALUES
('SEC-1001','PHIL1A','Philosophy Year 1 - Section A',1,1,2),
('SEC-1002','PHIL1B','Philosophy Year 1 - Section B',1,1,2),
('SEC-1003','THEO1A','Theology Year 1 - Section A',2,1,2),
('SEC-1004','THEO1B','Theology Year 1 - Section B',2,1,2);

-- ============================================================

CREATE TABLE `tblschedule` (
  `fldIndex`           INT(11) NOT NULL AUTO_INCREMENT,
  `fldScheduleID`      VARCHAR(30) NOT NULL UNIQUE,
  `fldSectionIndex`    INT(11) NOT NULL COMMENT 'FK to tblsection.fldIndex',
  `fldCourseIndex`     INT(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex',
  `fldInstructorIndex` INT(11) NOT NULL COMMENT 'FK to tblinstructors.fldIndex',
  `fldRoomIndex`       INT(11) NOT NULL COMMENT 'FK to tblrooms.fldIndex',
  `fldDayOfWeek`       ENUM('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday') NOT NULL,
  `fldTimeStart`       TIME NOT NULL,
  `fldTimeEnd`         TIME NOT NULL,
  `fldCreatedAt`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldSectionIndex`)    REFERENCES `tblsection`(`fldIndex`),
  FOREIGN KEY (`fldCourseIndex`)     REFERENCES `tblcourses`(`fldIndex`),
  FOREIGN KEY (`fldInstructorIndex`) REFERENCES `tblinstructors`(`fldIndex`),
  FOREIGN KEY (`fldRoomIndex`)       REFERENCES `tblrooms`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================

CREATE TABLE `tblstudentsection` (
  `fldIndex`            INT(11) NOT NULL AUTO_INCREMENT,
  `fldStudentSectionID` VARCHAR(30) NOT NULL UNIQUE,
  `fldStudentIndex`     INT(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldSectionIndex`     INT(11) NOT NULL COMMENT 'FK to tblsection.fldIndex',
  `fldSemesterIndex`    INT(11) NOT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldDateAssigned`     DATE NOT NULL,
  `fldCreatedAt`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  UNIQUE KEY `uq_student_section_sem` (`fldStudentIndex`,`fldSectionIndex`,`fldSemesterIndex`),
  FOREIGN KEY (`fldStudentIndex`)  REFERENCES `tblstudents`(`fldIndex`),
  FOREIGN KEY (`fldSectionIndex`)  REFERENCES `tblsection`(`fldIndex`),
  FOREIGN KEY (`fldSemesterIndex`) REFERENCES `tblsemester`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblstudentsection` (`fldStudentSectionID`,`fldStudentIndex`,`fldSectionIndex`,`fldSemesterIndex`,`fldDateAssigned`) VALUES
('SS-001',1,1,2,'2026-06-10'),
('SS-002',2,3,2,'2026-06-10'),
('SS-003',3,1,2,'2026-06-11'),
('SS-004',4,3,2,'2026-06-11'),
('SS-005',5,2,2,'2026-06-12'),
('SS-006',6,4,2,'2026-06-12'),
('SS-007',7,1,2,'2026-06-13'),
('SS-008',8,3,2,'2026-06-13'),
('SS-009',9,2,2,'2026-06-14'),
('SS-010',10,4,2,'2026-06-14');

-- ============================================================

CREATE TABLE `tblenrollment` (
  `fldIndex`            INT(11) NOT NULL AUTO_INCREMENT,
  `fldEnrollmentID`     VARCHAR(30) NOT NULL UNIQUE,
  `fldStudentIndex`     INT(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldProgramIndex`     INT(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldSemesterIndex`    INT(11) NOT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldYearLevel`        TINYINT(1) NOT NULL,
  `fldEnrollmentDate`   DATE NOT NULL,
  `fldEnrollmentStatus` ENUM('Enrolled','Dropped','LOA','Withdrawn') NOT NULL DEFAULT 'Enrolled',
  `fldCreatedAt`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  UNIQUE KEY `uq_enrollment` (`fldStudentIndex`,`fldSemesterIndex`),
  FOREIGN KEY (`fldStudentIndex`)  REFERENCES `tblstudents`(`fldIndex`),
  FOREIGN KEY (`fldProgramIndex`)  REFERENCES `tblprogram`(`fldIndex`),
  FOREIGN KEY (`fldSemesterIndex`) REFERENCES `tblsemester`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tblenrollment` (`fldEnrollmentID`,`fldStudentIndex`,`fldProgramIndex`,`fldSemesterIndex`,`fldYearLevel`,`fldEnrollmentDate`) VALUES
('ENR-001',1,1,2,1,'2026-06-10'),
('ENR-002',2,2,2,1,'2026-06-10'),
('ENR-003',3,1,2,1,'2026-06-11'),
('ENR-004',4,2,2,1,'2026-06-11'),
('ENR-005',5,1,2,1,'2026-06-12'),
('ENR-006',6,2,2,1,'2026-06-12'),
('ENR-007',7,1,2,1,'2026-06-13'),
('ENR-008',8,2,2,1,'2026-06-13'),
('ENR-009',9,1,2,1,'2026-06-14'),
('ENR-010',10,2,2,1,'2026-06-14');

-- ============================================================

CREATE TABLE `tblenrollmentsubjects` (
  `fldIndex`               INT(11) NOT NULL AUTO_INCREMENT,
  `fldEnrollmentSubjectID` VARCHAR(30) NOT NULL UNIQUE,
  `fldEnrollmentIndex`     INT(11) NOT NULL COMMENT 'FK to tblenrollment.fldIndex',
  `fldCourseIndex`         INT(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex',
  `fldScheduleIndex`       INT(11) DEFAULT NULL COMMENT 'FK to tblschedule.fldIndex',
  `fldStatus`              ENUM('Enrolled','Dropped','Completed','Failed','Incomplete') NOT NULL DEFAULT 'Enrolled',
  `fldCreatedAt`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  UNIQUE KEY `uq_enrollment_course` (`fldEnrollmentIndex`,`fldCourseIndex`),
  FOREIGN KEY (`fldEnrollmentIndex`) REFERENCES `tblenrollment`(`fldIndex`),
  FOREIGN KEY (`fldCourseIndex`)     REFERENCES `tblcourses`(`fldIndex`),
  FOREIGN KEY (`fldScheduleIndex`)   REFERENCES `tblschedule`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MODULE 5: GRADES MANAGEMENT
-- 5-point scale: 1.0 = highest, 5.0 = lowest, 3.0 = passing
-- ============================================================

CREATE TABLE `tblgrades` (
  `fldIndex`                    INT(11) NOT NULL AUTO_INCREMENT,
  `fldGradeID`                  VARCHAR(30) NOT NULL UNIQUE,
  `fldEnrollmentSubjectIndex`   INT(11) NOT NULL UNIQUE COMMENT 'FK to tblenrollmentsubjects.fldIndex',
  `fldStudentIndex`             INT(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldCourseIndex`              INT(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex',
  `fldSemesterIndex`            INT(11) NOT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldMidtermGrade`             DECIMAL(3,2) DEFAULT NULL COMMENT '1.0 to 5.0 scale',
  `fldFinalGrade`               DECIMAL(3,2) DEFAULT NULL COMMENT '1.0 to 5.0 scale',
  `fldFinalRating`              DECIMAL(3,2) DEFAULT NULL COMMENT 'Computed final grade',
  `fldGradeStatus`              ENUM('Passed','Failed','Incomplete','Dropped','NotYetGraded') NOT NULL DEFAULT 'NotYetGraded',
  `fldRemarks`                  VARCHAR(100) DEFAULT NULL,
  `fldEnteredByUserIndex`       INT(11) DEFAULT NULL COMMENT 'FK to tblusers.fldIndex - must be Registrar',
  `fldDateEntered`              DATETIME DEFAULT NULL,
  `fldLastModifiedAt`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldEnrollmentSubjectIndex`) REFERENCES `tblenrollmentsubjects`(`fldIndex`),
  FOREIGN KEY (`fldStudentIndex`)           REFERENCES `tblstudents`(`fldIndex`),
  FOREIGN KEY (`fldCourseIndex`)            REFERENCES `tblcourses`(`fldIndex`),
  FOREIGN KEY (`fldSemesterIndex`)          REFERENCES `tblsemester`(`fldIndex`),
  FOREIGN KEY (`fldEnteredByUserIndex`)     REFERENCES `tblusers`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MODULE 6: REPORTS & OUTPUTS
-- ============================================================

CREATE TABLE `tblreports` (
  `fldIndex`          INT(11) NOT NULL AUTO_INCREMENT,
  `fldReportID`       VARCHAR(30) NOT NULL UNIQUE,
  `fldReportType`     ENUM(
    'TranscriptOfRecords',
    'GradeCard',
    'SummaryOfGrades',
    'GradeCertificate',
    'EnrollmentStatistics',
    'CHEDReport',
    'Other'
  ) NOT NULL,
  `fldStudentIndex`   INT(11) DEFAULT NULL COMMENT 'FK to tblstudents.fldIndex - NULL for system-wide reports',
  `fldSemesterIndex`  INT(11) DEFAULT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldGeneratedByIndex` INT(11) NOT NULL COMMENT 'FK to tblusers.fldIndex',
  `fldGeneratedAt`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldFilePath`       VARCHAR(500) DEFAULT NULL,
  `fldExportFormat`   ENUM('PDF','XLSX') NOT NULL DEFAULT 'PDF',
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldStudentIndex`)     REFERENCES `tblstudents`(`fldIndex`),
  FOREIGN KEY (`fldSemesterIndex`)    REFERENCES `tblsemester`(`fldIndex`),
  FOREIGN KEY (`fldGeneratedByIndex`) REFERENCES `tblusers`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- ALUMNI RECORDS (permanent, no deletion policy)
-- ============================================================

CREATE TABLE `tblalumni` (
  `fldIndex`           INT(11) NOT NULL AUTO_INCREMENT,
  `fldAlumniID`        VARCHAR(30) NOT NULL UNIQUE,
  `fldStudentIndex`    INT(11) NOT NULL UNIQUE COMMENT 'FK to tblstudents.fldIndex',
  `fldGraduationDate`  DATE NOT NULL,
  `fldProgramIndex`    INT(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldYearGraduated`   VARCHAR(20) NOT NULL,
  `fldHonors`          VARCHAR(100) DEFAULT NULL,
  `fldCurrentMinistry` VARCHAR(255) DEFAULT NULL,
  `fldCurrentAddress`  VARCHAR(255) DEFAULT NULL,
  `fldNotes`           TEXT DEFAULT NULL,
  `fldCreatedAt`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldUpdatedAt`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldStudentIndex`) REFERENCES `tblstudents`(`fldIndex`),
  FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- SYSTEM: Database backup log
-- ============================================================

CREATE TABLE `tblbackuplog` (
  `fldIndex`           INT(11) NOT NULL AUTO_INCREMENT,
  `fldBackupID`        VARCHAR(30) NOT NULL UNIQUE,
  `fldBackupDate`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldBackupFilePath`  VARCHAR(500) NOT NULL,
  `fldBackupType`      ENUM('Manual','Scheduled') NOT NULL DEFAULT 'Manual',
  `fldPerformedByIndex` INT(11) NOT NULL COMMENT 'FK to tblusers.fldIndex',
  `fldNotes`           VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldPerformedByIndex`) REFERENCES `tblusers`(`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MODULE 14: ONLINE ADMISSION SUBMISSIONS
-- Students fill out apply.html; their data is stored here
-- pending registrar review before becoming official applicants.
-- ============================================================

CREATE TABLE `tblonline_submissions` (
  `fldIndex`              INT(11) NOT NULL AUTO_INCREMENT,
  `fldSubmissionID`       VARCHAR(30) NOT NULL UNIQUE,
  `fldFirstName`          VARCHAR(50) NOT NULL,
  `fldMiddleName`         VARCHAR(50) DEFAULT NULL,
  `fldLastName`           VARCHAR(50) NOT NULL,
  `fldDateOfBirth`        DATE NOT NULL,
  `fldPlaceOfBirth`       VARCHAR(100) DEFAULT NULL,
  `fldGender`             ENUM('Male','Female','Other') DEFAULT NULL,
  `fldAddress`            VARCHAR(255) DEFAULT NULL,
  `fldContactNumber`      VARCHAR(20) DEFAULT NULL,
  `fldEmail`              VARCHAR(100) NOT NULL,
  `fldNationality`        VARCHAR(50) DEFAULT NULL,
  `fldReligion`           VARCHAR(50) DEFAULT NULL,
  `fldFatherName`         VARCHAR(100) DEFAULT NULL,
  `fldFatherOccupation`   VARCHAR(100) DEFAULT NULL,
  `fldMotherName`         VARCHAR(100) DEFAULT NULL,
  `fldMotherOccupation`   VARCHAR(100) DEFAULT NULL,
  `fldGuardianName`       VARCHAR(100) DEFAULT NULL,
  `fldGuardianContact`    VARCHAR(20) DEFAULT NULL,
  `fldLastSchoolAttended` VARCHAR(150) DEFAULT NULL,
  `fldLastSchoolYear`     VARCHAR(20) DEFAULT NULL,
  `fldLastYearLevel`      VARCHAR(50) DEFAULT NULL,
  `fldSeminaryLevel`      ENUM('Propaedeutic','College') DEFAULT NULL,
  `fldProgramIndex`       INT(11) DEFAULT NULL,
  `fldStatus`             ENUM('Pending','Accepted','Rejected') NOT NULL DEFAULT 'Pending',
  `fldRejectionReason`    VARCHAR(255) DEFAULT NULL,
  `fldSubmittedAt`        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fldReviewedAt`         TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`fldIndex`),
  FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

COMMIT;

-- ============================================================
-- END OF SCHEMA
-- Tables: 23 total
--   1.  tblusers
--   2.  tblprogram
--   3.  tblschoolyear
--   4.  tblsemester
--   5.  tblcourses
--   6.  tblprerequisites
--   7.  tblapplicants
--   8.  tblapplications
--   9.  tblentranceexam
--   10. tblstudents
--   11. tbldocuments
--   12. tblinstructors
--   13. tblrooms
--   14. tblsection
--   15. tblschedule
--   16. tblstudentsection
--   17. tblenrollment
--   18. tblenrollmentsubjects
--   19. tblgrades
--   20. tblreports
--   21. tblalumni
--   22. tblbackuplog
--   23. tblonline_submissions
-- ============================================================
