-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 03, 2026 at 05:21 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `dbstudentmanagementsystem`
--

-- --------------------------------------------------------

--
-- Table structure for table `tblalumni`
--

CREATE TABLE `tblalumni` (
  `fldIndex` int(11) NOT NULL,
  `fldAlumniID` varchar(30) NOT NULL,
  `fldStudentIndex` int(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldGraduationDate` date NOT NULL,
  `fldProgramIndex` int(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldYearGraduated` varchar(20) NOT NULL,
  `fldHonors` varchar(100) DEFAULT NULL,
  `fldCurrentMinistry` varchar(255) DEFAULT NULL,
  `fldCurrentAddress` varchar(255) DEFAULT NULL,
  `fldNotes` text DEFAULT NULL,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tblapplicants`
--

CREATE TABLE `tblapplicants` (
  `fldIndex` int(11) NOT NULL,
  `fldApplicantID` varchar(30) NOT NULL,
  `fldFirstName` varchar(50) NOT NULL,
  `fldMiddleName` varchar(50) DEFAULT NULL,
  `fldLastName` varchar(50) NOT NULL,
  `fldDateOfBirth` date NOT NULL,
  `fldPlaceOfBirth` varchar(100) DEFAULT NULL,
  `fldGender` enum('Male') DEFAULT 'Male',
  `fldAddress` varchar(255) DEFAULT NULL,
  `fldContactNumber` varchar(20) DEFAULT NULL,
  `fldEmail` varchar(100) NOT NULL,
  `fldNationality` varchar(50) DEFAULT NULL,
  `fldReligion` varchar(50) DEFAULT NULL,
  `fldFatherName` varchar(100) DEFAULT NULL,
  `fldFatherOccupation` varchar(100) DEFAULT NULL,
  `fldMotherName` varchar(100) DEFAULT NULL,
  `fldMotherOccupation` varchar(100) DEFAULT NULL,
  `fldGuardianName` varchar(100) DEFAULT NULL,
  `fldGuardianContact` varchar(20) DEFAULT NULL,
  `fldLastSchoolAttended` varchar(150) DEFAULT NULL,
  `fldLastSchoolYear` varchar(20) DEFAULT NULL,
  `fldLastYearLevel` varchar(50) DEFAULT NULL,
  `fldSeminaryLevel` enum('Propaedeutic','College') DEFAULT NULL,
  `fldProgramIndex` int(11) DEFAULT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblapplicants`
--

INSERT INTO `tblapplicants` (`fldIndex`, `fldApplicantID`, `fldFirstName`, `fldMiddleName`, `fldLastName`, `fldDateOfBirth`, `fldPlaceOfBirth`, `fldGender`, `fldAddress`, `fldContactNumber`, `fldEmail`, `fldNationality`, `fldReligion`, `fldFatherName`, `fldFatherOccupation`, `fldMotherName`, `fldMotherOccupation`, `fldGuardianName`, `fldGuardianContact`, `fldLastSchoolAttended`, `fldLastSchoolYear`, `fldLastYearLevel`, `fldSeminaryLevel`, `fldProgramIndex`, `fldCreatedAt`, `fldUpdatedAt`) VALUES
(1, 'P-1001', 'Charles', 'Benedict', 'Sual', '2005-12-09', 'Lipa City', 'Male', 'Blk 7 Lot 19 San Carlos Homes P2, Barangay Balintawak, Lipa City, Batangas', '+639215353015', 'charlessual11@gmail.com', 'Filipino', 'Roman Catholic', 'Carnelito Sual Jr', 'Nurse', 'Katherine Sual', 'Financial Manager', '', '', 'De La Salle Lipa', '2022-2024', 'Grade 12', 'College', 1, '2026-04-27 09:37:37', '2026-04-27 09:38:22'),
(2, 'P-1002', 'Francis Angelo', 'Dichoso', 'Grantoza', '2005-06-18', 'San Pablo City', 'Male', 'Richwood Park Village, San Pablo City, Laguna', '09931270935', 'officialtck2005@gmail.com', 'Filipino', 'Roman Catholic', 'Geoffrey Grantoza', 'Safety Officer', 'Marsha Grantoza', 'Housewife', '', '', 'SPC', '2023-2024', 'Grade 12', 'College', 1, '2026-04-27 11:25:31', '2026-04-27 11:26:18'),
(3, 'P-1003', 'Justine Miko', '', 'Magsombol', '2007-08-05', 'Lipa City', 'Male', '', '', 'barunge.0805@gmail.com', '', 'Roman Catholic', '', '', '', '', '', '', '', '', '', 'College', 1, '2026-04-30 12:15:15', '2026-04-30 12:15:26'),
(4, 'P-1004', 'Giancarlo', '', 'Plenos', '2006-09-26', 'Sto Thomas', 'Male', '', '', 'giancarloplenos@gmail.com', '', '', '', '', '', '', '', '', '', '', '', 'College', 1, '2026-04-30 12:22:12', '2026-04-30 12:22:35');

-- --------------------------------------------------------

--
-- Table structure for table `tblapplications`
--

CREATE TABLE `tblapplications` (
  `fldIndex` int(11) NOT NULL,
  `fldApplicationID` varchar(30) NOT NULL,
  `fldApplicantIndex` int(11) NOT NULL COMMENT 'FK to tblapplicants.fldIndex',
  `fldApplicationDate` date NOT NULL,
  `fldSchoolYearIndex` int(11) NOT NULL COMMENT 'FK to tblschoolyear.fldIndex',
  `fldApplicationStatus` enum('Applied','Interviewed','AspiringConventionAttended','Confirmed','Admitted','Enrolled','Rejected','Withdrawn') NOT NULL DEFAULT 'Applied',
  `fldInterviewDate` date DEFAULT NULL,
  `fldConventionDate` date DEFAULT NULL,
  `fldRejectionReason` varchar(255) DEFAULT NULL,
  `fldRemarks` text DEFAULT NULL,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblapplications`
--

INSERT INTO `tblapplications` (`fldIndex`, `fldApplicationID`, `fldApplicantIndex`, `fldApplicationDate`, `fldSchoolYearIndex`, `fldApplicationStatus`, `fldInterviewDate`, `fldConventionDate`, `fldRejectionReason`, `fldRemarks`, `fldCreatedAt`, `fldUpdatedAt`) VALUES
(1, 'APP-1001', 1, '2026-04-27', 1, 'Enrolled', NULL, NULL, NULL, NULL, '2026-04-27 09:37:37', '2026-04-27 09:39:28'),
(2, 'APP-1002', 2, '2026-04-27', 1, 'Enrolled', NULL, NULL, NULL, NULL, '2026-04-27 11:25:31', '2026-04-27 11:28:16'),
(3, 'APP-1003', 3, '2026-04-30', 1, 'Enrolled', NULL, NULL, NULL, NULL, '2026-04-30 12:15:15', '2026-04-30 12:19:16'),
(4, 'APP-1004', 4, '2026-04-30', 1, 'Enrolled', NULL, NULL, NULL, NULL, '2026-04-30 12:22:12', '2026-04-30 12:22:46');

-- --------------------------------------------------------

--
-- Table structure for table `tblauditlog`
--

CREATE TABLE `tblauditlog` (
  `fldIndex` int(11) NOT NULL,
  `fldAction` varchar(30) NOT NULL,
  `fldDetail` varchar(500) DEFAULT NULL,
  `fldEntityType` varchar(50) NOT NULL,
  `fldIpAddress` varchar(50) DEFAULT NULL,
  `fldPerformedBy` varchar(50) NOT NULL,
  `fldRole` varchar(20) NOT NULL,
  `fldTimestamp` datetime(6) NOT NULL,
  `fldLogType` enum('AUDIT','SECURITY') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tblbackuplog`
--

CREATE TABLE `tblbackuplog` (
  `fldIndex` int(11) NOT NULL,
  `fldBackupID` varchar(30) NOT NULL,
  `fldBackupDate` datetime NOT NULL DEFAULT current_timestamp(),
  `fldBackupFilePath` varchar(500) NOT NULL,
  `fldBackupType` enum('Manual','Scheduled') NOT NULL DEFAULT 'Manual',
  `fldPerformedByIndex` int(11) NOT NULL COMMENT 'FK to tblusers.fldIndex',
  `fldNotes` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tblcourses`
--

CREATE TABLE `tblcourses` (
  `fldIndex` int(11) NOT NULL,
  `fldCourseID` varchar(30) NOT NULL,
  `fldCourseCode` varchar(30) NOT NULL,
  `fldCourseName` varchar(100) NOT NULL,
  `fldUnits` int(11) NOT NULL,
  `fldProgramIndex` int(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldYearLevel` tinyint(1) NOT NULL COMMENT '1 to 4 (or 6 for Theology)',
  `fldSemesterNumber` tinyint(1) NOT NULL COMMENT '1 = First, 2 = Second',
  `fldIsActive` tinyint(1) NOT NULL DEFAULT 1,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `fldCurriculumIndex` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblcourses`
--

INSERT INTO `tblcourses` (`fldIndex`, `fldCourseID`, `fldCourseCode`, `fldCourseName`, `fldUnits`, `fldProgramIndex`, `fldYearLevel`, `fldSemesterNumber`, `fldIsActive`, `fldCreatedAt`, `fldUpdatedAt`, `fldCurriculumIndex`) VALUES
(1, 'CRS001', 'GE1', 'Understanding the Self', 3, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(2, 'CRS002', 'GE2', 'Readings in the Philippine History', 3, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(3, 'CRS003', 'GE3', 'Purposive Communication', 3, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(4, 'CRS004', 'GE4', 'Science, Technology and Society', 3, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(5, 'CRS005', 'PHILO1', 'Logic (Symbolic)', 3, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(6, 'CRS006', 'THEO1', 'Creed', 3, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(7, 'CRS007', 'LAT1', 'Latin 1', 3, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(8, 'CRS008', 'PE1', 'Movement Competency Training', 2, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(9, 'CRS009', 'CWTS1', 'Civic Welfare Training Service 1', 3, 1, 1, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(10, 'CRS010', 'GE5', 'The Contemporary World', 3, 1, 1, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(11, 'CRS011', 'GE6', 'Mathematics in the Modern World', 3, 1, 1, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(12, 'CRS012', 'PHILO2', 'Introduction to Philosophy (Ancient Philosophy)', 3, 1, 1, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(13, 'CRS013', 'PHILO7', 'History of Oriental Philosophy 1', 3, 1, 1, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(14, 'CRS014', 'THEO2', 'Sacraments and Liturgy', 3, 1, 1, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(15, 'CRS015', 'LAT2', 'Latin 2', 3, 1, 1, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(16, 'CRS016', 'PE2', 'Exercise-Based Fitness Activities', 2, 1, 1, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(17, 'CRS017', 'CWTS2', 'Civic Welfare Training Service 2', 3, 1, 1, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(18, 'CRS018', 'RIZAL', 'Life and Works of Jose Rizal', 3, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(19, 'CRS019', 'GE7', 'Art Appreciation/Aesthetics 1', 3, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(20, 'CRS020', 'GE8', 'Great Books', 3, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(21, 'CRS021', 'PHILO8', 'Philosophy of Man 1', 3, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(22, 'CRS022', 'PHILO20', 'Seminar on Plato/Aristotle', 3, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(23, 'CRS023', 'PHILO23', 'History of Oriental Philosophy 2', 3, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(24, 'CRS024', 'THEO3', 'Morals of the Heart (Commandments)', 3, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(25, 'CRS025', 'FEELEC1', 'Spanish', 3, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(26, 'CRS026', 'PE3', 'Dance Activities', 2, 1, 2, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(27, 'CRS027', 'FIL1', 'Pagbasa at Pagsulat Tungo sa Pananaliksik', 3, 1, 2, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(28, 'CRS028', 'PHILO3', 'History of Western Philosophy 1 (Medieval)', 3, 1, 2, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(29, 'CRS029', 'PHILO9', 'Philosophical Anthropology (Philosophy of Man 2)', 3, 1, 2, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(30, 'CRS030', 'PHILO16', 'Epistemology', 3, 1, 2, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(31, 'CRS031', 'PHILO21', 'Cosmology/Philosophy of Science and Technology', 3, 1, 2, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(32, 'CRS032', 'GE9', 'Gender and Development', 3, 1, 2, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(33, 'CRS033', 'PE4', 'Sports (Basketball)', 2, 1, 2, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(34, 'CRS034', 'PHILO4', 'History of Western Philosophy 2 (Modern and Contemporary)', 3, 1, 3, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(35, 'CRS035', 'PHILO10', 'Social/Political Philosophy', 3, 1, 3, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(36, 'CRS036', 'GE10', 'Ethics', 3, 1, 3, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(37, 'CRS037', 'PHILO14', 'Metaphysics 1', 3, 1, 3, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(38, 'CRS038', 'PHILO22', 'Seminar on Filipino Philosophy', 3, 1, 3, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(39, 'CRS039', 'PHILO24', 'Art Appreciation/Aesthetics 2', 3, 1, 3, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(40, 'CRS040', 'THEO4', 'Scriptures (Old and New Testament)', 3, 1, 3, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(41, 'CRS041', 'MUSIC1', 'Fundamentals of Music 1', 1, 1, 3, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(42, 'CRS042', 'PHILO5', 'Existentialism, Phenomenology, Hermeneutics, Post Modernism', 3, 1, 3, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(43, 'CRS043', 'PHILO6', 'Modern Asia Thoughts', 3, 1, 3, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(44, 'CRS044', 'PHILO12', 'Special Questions in Ethics', 3, 1, 3, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(45, 'CRS045', 'PHILO15', 'Metaphysics 2', 3, 1, 3, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(46, 'CRS046', 'PHILO18', 'Comparative Philosophy (East-West)', 3, 1, 3, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(47, 'CRS047', 'PHILO28W', 'Philosophy Thesis Writing', 3, 1, 3, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(48, 'CRS048', 'ELEC1', 'Philippine Sociology', 3, 1, 3, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(49, 'CRS049', 'MUSIC2', 'Fundamentals of Music 2', 1, 1, 3, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(50, 'CRS050', 'FIL2', 'Komunikasyon sa Akademikong Filipino', 3, 1, 4, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(51, 'CRS051', 'GE11', 'Environmental Science', 3, 1, 4, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(52, 'CRS052', 'PHILO25', 'Special Questions in Philosophy', 3, 1, 4, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(53, 'CRS053', 'PHILO26', 'Seminar on Contemporary Philosophy', 3, 1, 4, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(54, 'CRS054', 'ELEC2', 'World and Philippine Literature', 3, 1, 4, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(55, 'CRS055', 'EDUC6', 'Assessment in Learning', 3, 1, 4, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(56, 'CRS056', 'MUSIC3', 'Gregorian Chant', 1, 1, 4, 1, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(57, 'CRS057', 'FIL3', 'Filipino sa Iba''t ibang Disiplina', 3, 1, 4, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(58, 'CRS058', 'PHILO17', 'Theodicy/Philosophy of Religion', 3, 1, 4, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(59, 'CRS059', 'PHILO19', 'Philosophy of Language', 3, 1, 4, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(60, 'CRS060', 'PHILO28S', 'Philosophy Synthesis (Thesis Defense and Comprehensive Exam)', 3, 1, 4, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(61, 'CRS061', 'ELEC3', 'Thomas Aquinas', 3, 1, 4, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(62, 'CRS062', 'FEELEC2', 'St. Francis de Sales', 3, 1, 4, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1),
(63, 'CRS063', 'MUSIC4', 'Music in the Liturgy', 1, 1, 4, 2, 1, '2026-04-22 23:47:37', '2026-04-28 11:04:33', 1);

-- --------------------------------------------------------

--
-- Table structure for table `tblcurriculum`
--

CREATE TABLE `tblcurriculum` (
  `fldIndex` int(11) NOT NULL,
  `fldCreatedAt` datetime(6) NOT NULL,
  `fldCurriculumID` varchar(30) NOT NULL,
  `fldIsActive` tinyint(4) NOT NULL,
  `fldLabel` varchar(60) NOT NULL,
  `fldProgramIndex` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblcurriculum`
--

INSERT INTO `tblcurriculum` (`fldIndex`, `fldCreatedAt`, `fldCurriculumID`, `fldIsActive`, `fldLabel`, `fldProgramIndex`) VALUES
(1, '2026-04-28 11:04:33.000000', 'CUR-001', 1, '2025-2026 Curriculum', 1),
(2, '2026-04-28 11:04:33.000000', 'CUR-002', 1, '2025-2026 Curriculum', 2);

-- --------------------------------------------------------

--
-- Table structure for table `tbldocuments`
--

CREATE TABLE `tbldocuments` (
  `fldIndex` int(11) NOT NULL,
  `fldDocumentID` varchar(30) NOT NULL,
  `fldStudentIndex` int(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldDocumentType` enum('BirthCertificate','Form137','Diploma','BaptismalRecord','ConfirmationRecord','MarriageContractOfParents','MedicalRecord','DentalRecord','ParishPriestRecommendation','Other') NOT NULL,
  `fldFileName` varchar(255) NOT NULL,
  `fldFilePath` varchar(500) NOT NULL,
  `fldUploadedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldRemarks` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tblenrollment`
--

CREATE TABLE `tblenrollment` (
  `fldIndex` int(11) NOT NULL,
  `fldEnrollmentID` varchar(30) NOT NULL,
  `fldStudentIndex` int(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldProgramIndex` int(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldSemesterIndex` int(11) NOT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldYearLevel` tinyint(1) NOT NULL,
  `fldEnrollmentDate` date NOT NULL,
  `fldEnrollmentStatus` enum('Enrolled','Dropped','LOA','Withdrawn') NOT NULL DEFAULT 'Enrolled',
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblenrollment`
--

INSERT INTO `tblenrollment` (`fldIndex`, `fldEnrollmentID`, `fldStudentIndex`, `fldProgramIndex`, `fldSemesterIndex`, `fldYearLevel`, `fldEnrollmentDate`, `fldEnrollmentStatus`, `fldCreatedAt`, `fldUpdatedAt`) VALUES
(1, 'ENR-001', 1, 1, 2, 1, '2026-04-27', 'Enrolled', '2026-04-27 09:39:28', '2026-04-27 09:39:28'),
(2, 'ENR-002', 2, 1, 2, 1, '2026-04-27', 'Enrolled', '2026-04-27 11:28:16', '2026-04-27 11:28:16'),
(3, 'ENR-003', 1, 1, 1, 1, '2026-04-27', 'Enrolled', '2026-04-27 22:54:05', '2026-04-27 22:54:05'),
(4, 'ENR-004', 2, 1, 1, 1, '2026-04-27', 'Enrolled', '2026-04-27 23:31:20', '2026-04-27 23:31:20'),
(7, 'ENR-005', 1, 1, 3, 2, '2026-04-28', 'Enrolled', '2026-04-28 13:46:46', '2026-04-28 13:46:46'),
(8, 'ENR-006', 1, 1, 5, 2, '2026-04-29', 'Enrolled', '2026-04-29 22:51:38', '2026-04-29 22:51:38'),
(9, 'ENR-007', 1, 1, 4, 3, '2026-04-29', 'Enrolled', '2026-04-29 23:10:07', '2026-04-29 23:10:07'),
(10, 'ENR-008', 2, 1, 4, 2, '2026-04-30', 'Enrolled', '2026-04-30 12:09:56', '2026-04-30 12:09:56'),
(11, 'ENR-009', 3, 1, 4, 1, '2026-04-30', 'Enrolled', '2026-04-30 12:19:16', '2026-04-30 12:19:16'),
(12, 'ENR-010', 4, 1, 4, 1, '2026-04-30', 'Enrolled', '2026-04-30 12:22:46', '2026-04-30 12:22:46');

-- --------------------------------------------------------

--
-- Table structure for table `tblenrollmentsubjects`
--

CREATE TABLE `tblenrollmentsubjects` (
  `fldIndex` int(11) NOT NULL,
  `fldEnrollmentSubjectID` varchar(30) NOT NULL,
  `fldEnrollmentIndex` int(11) NOT NULL COMMENT 'FK to tblenrollment.fldIndex',
  `fldCourseIndex` int(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex',
  `fldScheduleIndex` int(11) DEFAULT NULL COMMENT 'FK to tblschedule.fldIndex',
  `fldStatus` enum('Enrolled','Dropped','Completed','Failed','Incomplete') NOT NULL DEFAULT 'Enrolled',
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldOverrideReason` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblenrollmentsubjects`
--

INSERT INTO `tblenrollmentsubjects` (`fldIndex`, `fldEnrollmentSubjectID`, `fldEnrollmentIndex`, `fldCourseIndex`, `fldScheduleIndex`, `fldStatus`, `fldCreatedAt`, `fldOverrideReason`) VALUES
(10, 'ES-1777301696578', 3, 1, NULL, 'Completed', '2026-04-27 22:54:56', NULL),
(11, 'ES-1777301696585', 3, 2, NULL, 'Completed', '2026-04-27 22:54:56', NULL),
(12, 'ES-1777301696592', 3, 3, NULL, 'Completed', '2026-04-27 22:54:56', NULL),
(13, 'ES-1777301696599', 3, 4, NULL, 'Completed', '2026-04-27 22:54:56', NULL),
(14, 'ES-1777301696604', 3, 5, NULL, 'Failed', '2026-04-27 22:54:56', NULL),
(15, 'ES-1777301696609', 3, 6, NULL, 'Completed', '2026-04-27 22:54:56', NULL),
(16, 'ES-1777301696614', 3, 7, NULL, 'Completed', '2026-04-27 22:54:56', NULL),
(17, 'ES-1777301696620', 3, 8, NULL, 'Completed', '2026-04-27 22:54:56', NULL),
(18, 'ES-1777301696627', 3, 9, NULL, 'Completed', '2026-04-27 22:54:56', NULL),
(19, 'ES-1777302528301', 1, 10, NULL, 'Completed', '2026-04-27 23:08:48', NULL),
(20, 'ES-1777302528309', 1, 11, NULL, 'Completed', '2026-04-27 23:08:48', NULL),
(21, 'ES-1777302528315', 1, 14, NULL, 'Completed', '2026-04-27 23:08:48', NULL),
(22, 'ES-1777302528321', 1, 15, NULL, 'Completed', '2026-04-27 23:08:48', NULL),
(23, 'ES-1777302528329', 1, 16, NULL, 'Completed', '2026-04-27 23:08:48', NULL),
(24, 'ES-1777302528336', 1, 17, NULL, 'Completed', '2026-04-27 23:08:48', NULL),
(26, 'ES-1777303887031', 4, 1, NULL, 'Completed', '2026-04-27 23:31:27', NULL),
(27, 'ES-1777303887034', 4, 2, NULL, 'Completed', '2026-04-27 23:31:27', NULL),
(28, 'ES-1777303887039', 4, 3, NULL, 'Completed', '2026-04-27 23:31:27', NULL),
(29, 'ES-1777303887042', 4, 4, NULL, 'Completed', '2026-04-27 23:31:27', NULL),
(30, 'ES-1777303887046', 4, 5, NULL, 'Completed', '2026-04-27 23:31:27', NULL),
(31, 'ES-1777303887049', 4, 6, NULL, 'Failed', '2026-04-27 23:31:27', NULL),
(32, 'ES-1777303887052', 4, 7, NULL, 'Completed', '2026-04-27 23:31:27', NULL),
(33, 'ES-1777303887055', 4, 8, NULL, 'Completed', '2026-04-27 23:31:27', NULL),
(34, 'ES-1777303887058', 4, 9, NULL, 'Completed', '2026-04-27 23:31:27', NULL),
(36, 'ES-1777352146160', 2, 10, NULL, 'Completed', '2026-04-28 12:55:46', NULL),
(37, 'ES-1777352146163', 2, 11, NULL, 'Completed', '2026-04-28 12:55:46', NULL),
(38, 'ES-1777352146170', 2, 12, NULL, 'Completed', '2026-04-28 12:55:46', NULL),
(39, 'ES-1777352146176', 2, 13, NULL, 'Completed', '2026-04-28 12:55:46', NULL),
(40, 'ES-1777352146183', 2, 15, NULL, 'Completed', '2026-04-28 12:55:46', NULL),
(41, 'ES-1777352146190', 2, 16, NULL, 'Completed', '2026-04-28 12:55:46', NULL),
(42, 'ES-1777352146198', 2, 17, NULL, 'Completed', '2026-04-28 12:55:46', NULL),
(43, 'ES-1777352146202', 2, 6, NULL, 'Completed', '2026-04-28 12:55:46', NULL),
(44, 'ES-1777352233333', 2, 14, NULL, 'Completed', '2026-04-28 12:57:13', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `tblentranceexam`
--

CREATE TABLE `tblentranceexam` (
  `fldIndex` int(11) NOT NULL,
  `fldExamID` varchar(30) NOT NULL,
  `fldApplicantIndex` int(11) NOT NULL COMMENT 'FK to tblapplicants.fldIndex',
  `fldExamDate` date NOT NULL,
  `fldScore` decimal(5,2) DEFAULT NULL,
  `fldMaxScore` decimal(5,2) DEFAULT NULL,
  `fldResult` enum('Passed','Failed','Pending') NOT NULL DEFAULT 'Pending',
  `fldRemarks` text DEFAULT NULL,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tblgrades`
--

CREATE TABLE `tblgrades` (
  `fldIndex` int(11) NOT NULL,
  `fldGradeID` varchar(30) NOT NULL,
  `fldEnrollmentSubjectIndex` int(11) NOT NULL COMMENT 'FK to tblenrollmentsubjects.fldIndex',
  `fldStudentIndex` int(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldCourseIndex` int(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex',
  `fldSemesterIndex` int(11) NOT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldMidtermGrade` decimal(5,2) DEFAULT NULL COMMENT 'Computed: (CS x 60%) + (Exam x 40%)',
  `fldFinalGrade` decimal(5,2) DEFAULT NULL COMMENT 'Computed: (CS x 60%) + (Exam x 40%)',
  `fldFinalRating` decimal(5,2) DEFAULT NULL COMMENT 'Computed: (Midterm Grade + Final Grade) / 2',
  `fldMidtermClassStanding` decimal(5,2) DEFAULT NULL COMMENT 'Periodic Grade (Midterm) - entered by registrar',
  `fldMidtermExam` decimal(5,2) DEFAULT NULL COMMENT 'Exam score (Midterm) - entered by registrar',
  `fldFinalClassStanding` decimal(5,2) DEFAULT NULL COMMENT 'Periodic Grade (Final) - entered by registrar',
  `fldFinalExam` decimal(5,2) DEFAULT NULL COMMENT 'Exam score (Final) - entered by registrar',
  `fldGradeStatus` enum('Passed','Failed','Incomplete','Dropped','NotYetGraded') NOT NULL DEFAULT 'NotYetGraded',
  `fldRemarks` varchar(100) DEFAULT NULL,
  `fldEnteredByUserIndex` int(11) DEFAULT NULL COMMENT 'FK to tblusers.fldIndex - must be Registrar',
  `fldDateEntered` datetime DEFAULT NULL,
  `fldLastModifiedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblgrades`
--

INSERT INTO `tblgrades` (`fldIndex`, `fldGradeID`, `fldEnrollmentSubjectIndex`, `fldStudentIndex`, `fldCourseIndex`, `fldSemesterIndex`, `fldMidtermGrade`, `fldFinalGrade`, `fldFinalRating`, `fldMidtermClassStanding`, `fldMidtermExam`, `fldFinalClassStanding`, `fldFinalExam`, `fldGradeStatus`, `fldRemarks`, `fldEnteredByUserIndex`, `fldDateEntered`, `fldLastModifiedAt`) VALUES
(10, 'GRD-1777301696581', 10, 1, 1, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:06:18', '2026-04-27 23:06:18'),
(11, 'GRD-1777301696586', 11, 1, 2, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:06:23', '2026-04-27 23:06:23'),
(12, 'GRD-1777301696593', 12, 1, 3, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:06:27', '2026-04-27 23:06:27'),
(13, 'GRD-1777301696600', 13, 1, 4, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:06:36', '2026-04-27 23:06:36'),
(14, 'GRD-1777301696605', 14, 1, 5, 1, 3.40, 3.40, 3.40, 5.00, 1.00, 5.00, 1.00, 'Failed', '', 2, '2026-04-27 23:07:32', '2026-04-27 23:07:32'),
(15, 'GRD-1777301696610', 15, 1, 6, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:06:44', '2026-04-27 23:06:44'),
(16, 'GRD-1777301696615', 16, 1, 7, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:06:49', '2026-04-27 23:06:49'),
(17, 'GRD-1777301696621', 17, 1, 8, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:06:54', '2026-04-27 23:06:54'),
(18, 'GRD-1777301696628', 18, 1, 9, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:07:02', '2026-04-27 23:07:02'),
(19, 'GRD-1777302528304', 19, 1, 10, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:55:59', '2026-04-28 12:55:59'),
(20, 'GRD-1777302528309', 20, 1, 11, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:02', '2026-04-28 12:56:02'),
(21, 'GRD-1777302528316', 21, 1, 14, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:05', '2026-04-28 12:56:05'),
(22, 'GRD-1777302528322', 22, 1, 15, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:09', '2026-04-28 12:56:09'),
(23, 'GRD-1777302528330', 23, 1, 16, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:14', '2026-04-28 12:56:14'),
(24, 'GRD-1777302528338', 24, 1, 17, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:18', '2026-04-28 12:56:18'),
(26, 'GRD-1777303887032', 26, 2, 1, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:31:40', '2026-04-27 23:31:40'),
(27, 'GRD-1777303887035', 27, 2, 2, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:31:44', '2026-04-27 23:31:44'),
(28, 'GRD-1777303887040', 28, 2, 3, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:31:47', '2026-04-27 23:31:47'),
(29, 'GRD-1777303887042', 29, 2, 4, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:31:50', '2026-04-27 23:31:50'),
(30, 'GRD-1777303887047', 30, 2, 5, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:31:54', '2026-04-27 23:31:54'),
(31, 'GRD-1777303887050', 31, 2, 6, 1, 3.40, 3.40, 3.40, 5.00, 1.00, 5.00, 1.00, 'Failed', '', 2, '2026-04-27 23:35:26', '2026-04-27 23:35:26'),
(32, 'GRD-1777303887053', 32, 2, 7, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:32:01', '2026-04-27 23:32:01'),
(33, 'GRD-1777303887056', 33, 2, 8, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:32:05', '2026-04-27 23:32:05'),
(34, 'GRD-1777303887059', 34, 2, 9, 1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-27 23:32:08', '2026-04-27 23:32:08'),
(36, 'GRD-1777352146161', 36, 2, 10, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:26', '2026-04-28 12:56:26'),
(37, 'GRD-1777352146164', 37, 2, 11, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:29', '2026-04-28 12:56:29'),
(38, 'GRD-1777352146171', 38, 2, 12, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:32', '2026-04-28 12:56:32'),
(39, 'GRD-1777352146177', 39, 2, 13, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:36', '2026-04-28 12:56:36'),
(40, 'GRD-1777352146184', 40, 2, 15, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:39', '2026-04-28 12:56:39'),
(41, 'GRD-1777352146191', 41, 2, 16, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:43', '2026-04-28 12:56:43'),
(42, 'GRD-1777352146199', 42, 2, 17, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:46', '2026-04-28 12:56:46'),
(43, 'GRD-1777352146203', 43, 2, 6, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:56:50', '2026-04-28 12:56:50'),
(44, 'GRD-1777352233334', 44, 2, 14, 2, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 'Passed', '', 2, '2026-04-28 12:57:21', '2026-04-28 12:57:21');

-- --------------------------------------------------------

--
-- Table structure for table `tblinstructors`
--

CREATE TABLE `tblinstructors` (
  `fldIndex` int(11) NOT NULL,
  `fldInstructorID` varchar(30) NOT NULL,
  `fldFirstName` varchar(50) NOT NULL,
  `fldMiddleName` varchar(50) DEFAULT NULL,
  `fldLastName` varchar(50) NOT NULL,
  `fldEmail` varchar(100) DEFAULT NULL,
  `fldContactNumber` varchar(20) DEFAULT NULL,
  `fldSpecialization` varchar(100) DEFAULT NULL,
  `fldIsActive` tinyint(1) NOT NULL DEFAULT 1,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblinstructors`
--

INSERT INTO `tblinstructors` (`fldIndex`, `fldInstructorID`, `fldFirstName`, `fldMiddleName`, `fldLastName`, `fldEmail`, `fldContactNumber`, `fldSpecialization`, `fldIsActive`, `fldCreatedAt`, `fldUpdatedAt`) VALUES
(1, 'INS-001', 'Chris', '', 'Brown', '', '', '', 1, '2026-04-03 21:14:23', '2026-04-15 16:25:18');

-- --------------------------------------------------------

--
-- Table structure for table `tblonline_submissions`
--

CREATE TABLE `tblonline_submissions` (
  `fldIndex` int(11) NOT NULL,
  `fldSubmissionID` varchar(30) NOT NULL,
  `fldFirstName` varchar(50) NOT NULL,
  `fldMiddleName` varchar(50) DEFAULT NULL,
  `fldLastName` varchar(50) NOT NULL,
  `fldDateOfBirth` date NOT NULL,
  `fldPlaceOfBirth` varchar(100) DEFAULT NULL,
  `fldGender` enum('Male') DEFAULT 'Male',
  `fldAddress` varchar(255) DEFAULT NULL,
  `fldContactNumber` varchar(20) DEFAULT NULL,
  `fldEmail` varchar(100) NOT NULL,
  `fldNationality` varchar(50) DEFAULT NULL,
  `fldReligion` varchar(50) DEFAULT NULL,
  `fldFatherName` varchar(100) DEFAULT NULL,
  `fldFatherOccupation` varchar(100) DEFAULT NULL,
  `fldMotherName` varchar(100) DEFAULT NULL,
  `fldMotherOccupation` varchar(100) DEFAULT NULL,
  `fldGuardianName` varchar(100) DEFAULT NULL,
  `fldGuardianContact` varchar(20) DEFAULT NULL,
  `fldLastSchoolAttended` varchar(150) DEFAULT NULL,
  `fldLastSchoolYear` varchar(20) DEFAULT NULL,
  `fldLastYearLevel` varchar(50) DEFAULT NULL,
  `fldSeminaryLevel` enum('Propaedeutic','College') DEFAULT NULL,
  `fldProgramIndex` int(11) DEFAULT NULL,
  `fldStatus` enum('Pending','Accepted','Rejected') NOT NULL DEFAULT 'Pending',
  `fldRejectionReason` varchar(255) DEFAULT NULL,
  `fldSubmittedAt` timestamp NOT NULL DEFAULT current_timestamp(),
  `fldReviewedAt` timestamp NULL DEFAULT NULL,
  `fldBirthCertificate` varchar(255) DEFAULT NULL,
  `fldBaptismalCertificate` varchar(255) DEFAULT NULL,
  `fldConfirmationCertificate` varchar(255) DEFAULT NULL,
  `fldReportCard` varchar(255) DEFAULT NULL,
  `fldGoodMoral` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblonline_submissions`
--

INSERT INTO `tblonline_submissions` (`fldIndex`, `fldSubmissionID`, `fldFirstName`, `fldMiddleName`, `fldLastName`, `fldDateOfBirth`, `fldPlaceOfBirth`, `fldGender`, `fldAddress`, `fldContactNumber`, `fldEmail`, `fldNationality`, `fldReligion`, `fldFatherName`, `fldFatherOccupation`, `fldMotherName`, `fldMotherOccupation`, `fldGuardianName`, `fldGuardianContact`, `fldLastSchoolAttended`, `fldLastSchoolYear`, `fldLastYearLevel`, `fldSeminaryLevel`, `fldProgramIndex`, `fldStatus`, `fldRejectionReason`, `fldSubmittedAt`, `fldReviewedAt`, `fldBirthCertificate`, `fldBaptismalCertificate`, `fldConfirmationCertificate`, `fldReportCard`, `fldGoodMoral`) VALUES
(1, 'SUB-1001', 'Charles', 'Benedict', 'Sual', '2005-12-09', 'Lipa City', 'Male', 'Blk 7 Lot 19 San Carlos Homes P2, Barangay Balintawak, Lipa City, Batangas', '+639215353015', 'charlessual11@gmail.com', 'Filipino', 'Roman Catholic', 'Carnelito Sual Jr', 'Nurse', 'Katherine Sual', 'Financial Manager', NULL, NULL, 'De La Salle Lipa', '2022-2024', 'Grade 12', 'College', 1, 'Accepted', NULL, '2026-04-27 01:37:01', '2026-04-27 01:37:37', NULL, NULL, NULL, NULL, NULL),
(2, 'SUB-1002', 'Francis Angelo', 'Dichoso', 'Grantoza', '2005-06-18', 'San Pablo City', 'Male', 'Richwood Park Village, San Pablo City, Laguna', '09931270935', 'officialtck2005@gmail.com', 'Filipino', 'Roman Catholic', 'Geoffrey Grantoza', 'Safety Officer', 'Marsha Grantoza', 'Housewife', NULL, NULL, 'SPC', '2023-2024', 'Grade 12', 'College', 1, 'Accepted', NULL, '2026-04-27 03:23:20', '2026-04-27 03:25:31', NULL, NULL, NULL, NULL, NULL),
(3, 'SUB-1003', 'Justine Miko', NULL, 'Magsombol', '2007-08-05', 'Lipa City', 'Male', NULL, NULL, 'barunge.0805@gmail.com', NULL, 'Roman Catholic', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'College', 1, 'Accepted', NULL, '2026-04-30 04:14:50', '2026-04-30 04:15:15', NULL, NULL, NULL, NULL, NULL),
(4, 'SUB-1004', 'Giancarlo', NULL, 'Plenos', '2006-09-26', 'Sto Thomas', 'Male', NULL, NULL, 'giancarloplenos@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'College', 1, 'Accepted', NULL, '2026-04-30 04:21:45', '2026-04-30 04:22:12', NULL, NULL, NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `tblpassword_reset_tokens`
--

CREATE TABLE `tblpassword_reset_tokens` (
  `fldIndex` int(11) NOT NULL,
  `fldToken` varchar(64) NOT NULL,
  `fldUserIndex` int(11) NOT NULL,
  `fldExpiresAt` datetime NOT NULL,
  `fldUsed` tinyint(4) NOT NULL DEFAULT 0,
  `fldCreatedAt` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tblprerequisites`
--

CREATE TABLE `tblprerequisites` (
  `fldIndex` int(11) NOT NULL,
  `fldPrerequisiteID` varchar(30) NOT NULL,
  `fldCourseIndex` int(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex - course being taken',
  `fldPrerequisiteIndex` int(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex - course that must be passed first',
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblprerequisites`
--

INSERT INTO `tblprerequisites` (`fldIndex`, `fldPrerequisiteID`, `fldCourseIndex`, `fldPrerequisiteIndex`, `fldCreatedAt`) VALUES
(1, 'PRQ-001', 12, 5, '2026-04-22 23:47:37'),(2, 'PRQ-002', 13, 5, '2026-04-22 23:47:37'),(3, 'PRQ-003', 14, 6, '2026-04-22 23:47:37'),(4, 'PRQ-004', 15, 7, '2026-04-22 23:47:37'),(5, 'PRQ-005', 16, 8, '2026-04-22 23:47:37'),(6, 'PRQ-006', 17, 9, '2026-04-22 23:47:37'),(7, 'PRQ-007', 21, 12, '2026-04-22 23:47:37'),(8, 'PRQ-008', 22, 12, '2026-04-22 23:47:37'),(9, 'PRQ-009', 23, 13, '2026-04-22 23:47:37'),(10, 'PRQ-010', 24, 14, '2026-04-22 23:47:37'),
(11, 'PRQ-011', 26, 16, '2026-04-22 23:47:37'),(12, 'PRQ-012', 28, 12, '2026-04-22 23:47:37'),(13, 'PRQ-013', 29, 21, '2026-04-22 23:47:37'),(14, 'PRQ-014', 30, 12, '2026-04-22 23:47:37'),(15, 'PRQ-015', 30, 22, '2026-04-22 23:47:37'),(16, 'PRQ-016', 31, 12, '2026-04-22 23:47:37'),(17, 'PRQ-017', 31, 28, '2026-04-22 23:47:37'),(18, 'PRQ-018', 33, 26, '2026-04-22 23:47:37'),(19, 'PRQ-019', 34, 12, '2026-04-22 23:47:37'),(20, 'PRQ-020', 34, 28, '2026-04-22 23:47:37'),
(21, 'PRQ-021', 35, 12, '2026-04-22 23:47:37'),(22, 'PRQ-022', 37, 30, '2026-04-22 23:47:37'),(23, 'PRQ-023', 38, 13, '2026-04-22 23:47:37'),(24, 'PRQ-024', 38, 23, '2026-04-22 23:47:37'),(25, 'PRQ-025', 39, 19, '2026-04-22 23:47:37'),(26, 'PRQ-026', 40, 24, '2026-04-22 23:47:37'),(27, 'PRQ-027', 42, 34, '2026-04-22 23:47:37'),(28, 'PRQ-028', 43, 13, '2026-04-22 23:47:37'),(29, 'PRQ-029', 43, 23, '2026-04-22 23:47:37'),(30, 'PRQ-030', 43, 34, '2026-04-22 23:47:37'),
(31, 'PRQ-031', 44, 36, '2026-04-22 23:47:37'),(32, 'PRQ-032', 45, 37, '2026-04-22 23:47:37'),(33, 'PRQ-033', 46, 13, '2026-04-22 23:47:37'),(34, 'PRQ-034', 46, 23, '2026-04-22 23:47:37'),(35, 'PRQ-035', 46, 34, '2026-04-22 23:47:37'),(36, 'PRQ-036', 49, 41, '2026-04-22 23:47:37'),(37, 'PRQ-037', 50, 19, '2026-04-22 23:47:37'),(38, 'PRQ-038', 53, 34, '2026-04-22 23:47:37'),(39, 'PRQ-039', 56, 49, '2026-04-22 23:47:37'),(40, 'PRQ-040', 57, 20, '2026-04-22 23:47:37'),
(41, 'PRQ-041', 58, 45, '2026-04-22 23:47:37'),(42, 'PRQ-042', 59, 42, '2026-04-22 23:47:37'),(43, 'PRQ-043', 60, 47, '2026-04-22 23:47:37'),(44, 'PRQ-044', 63, 56, '2026-04-22 23:47:37');

-- --------------------------------------------------------

--
-- Table structure for table `tblprogram`
--

CREATE TABLE `tblprogram` (
  `fldIndex` int(11) NOT NULL,
  `fldProgramID` varchar(30) NOT NULL,
  `fldProgramCode` varchar(30) NOT NULL,
  `fldProgramName` varchar(100) NOT NULL,
  `fldTotalUnits` int(11) NOT NULL,
  `fldDuration` varchar(20) NOT NULL COMMENT 'e.g. 4 years, 4-6 years',
  `fldIsActive` tinyint(1) NOT NULL DEFAULT 1,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblprogram`
--

INSERT INTO `tblprogram` (`fldIndex`, `fldProgramID`, `fldProgramCode`, `fldProgramName`, `fldTotalUnits`, `fldDuration`, `fldIsActive`, `fldCreatedAt`, `fldUpdatedAt`) VALUES
(1, 'PRG-1001', 'PHILO', 'Bachelor of Arts in Philosophy', 120, '4 years', 1, '2026-03-29 17:08:45', '2026-03-29 17:08:45'),
(2, 'PRG-1002', 'THEO', 'Bachelor of Arts in Theology', 130, '4-6 years', 1, '2026-03-29 17:08:45', '2026-03-29 17:08:45');

-- --------------------------------------------------------

--
-- Table structure for table `tblreports`
--

CREATE TABLE `tblreports` (
  `fldIndex` int(11) NOT NULL,
  `fldReportID` varchar(30) NOT NULL,
  `fldReportType` enum('TranscriptOfRecords','GradeCard','SummaryOfGrades','GradeCertificate','EnrollmentStatistics','CHEDReport','Other') NOT NULL,
  `fldStudentIndex` int(11) DEFAULT NULL COMMENT 'FK to tblstudents.fldIndex - NULL for system-wide reports',
  `fldSemesterIndex` int(11) DEFAULT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldGeneratedByIndex` int(11) NOT NULL COMMENT 'FK to tblusers.fldIndex',
  `fldGeneratedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldFilePath` varchar(500) DEFAULT NULL,
  `fldExportFormat` enum('PDF','XLSX') NOT NULL DEFAULT 'PDF'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tblrooms`
--

CREATE TABLE `tblrooms` (
  `fldIndex` int(11) NOT NULL,
  `fldRoomID` varchar(30) NOT NULL,
  `fldRoomName` varchar(50) NOT NULL,
  `fldBuilding` varchar(50) DEFAULT NULL,
  `fldCapacity` int(11) DEFAULT NULL,
  `fldIsActive` tinyint(1) NOT NULL DEFAULT 1,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblrooms`
--

INSERT INTO `tblrooms` (`fldIndex`, `fldRoomID`, `fldRoomName`, `fldBuilding`, `fldCapacity`, `fldIsActive`, `fldCreatedAt`) VALUES
(1, 'RM-001', 'Study Room', 'Main Building', 30, 1, '2026-04-03 21:13:41');

-- --------------------------------------------------------

--
-- Table structure for table `tblschedule`
--

CREATE TABLE `tblschedule` (
  `fldIndex` int(11) NOT NULL,
  `fldScheduleID` varchar(30) NOT NULL,
  `fldSectionIndex` int(11) NOT NULL COMMENT 'FK to tblsection.fldIndex',
  `fldCourseIndex` int(11) NOT NULL COMMENT 'FK to tblcourses.fldIndex',
  `fldInstructorIndex` int(11) NOT NULL COMMENT 'FK to tblinstructors.fldIndex',
  `fldRoomIndex` int(11) NOT NULL COMMENT 'FK to tblrooms.fldIndex',
  `fldDayOfWeek` enum('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday') NOT NULL,
  `fldTimeStart` time NOT NULL,
  `fldTimeEnd` time NOT NULL,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblschedule`
--

INSERT INTO `tblschedule` (`fldIndex`, `fldScheduleID`, `fldSectionIndex`, `fldCourseIndex`, `fldInstructorIndex`, `fldRoomIndex`, `fldDayOfWeek`, `fldTimeStart`, `fldTimeEnd`, `fldCreatedAt`, `fldUpdatedAt`) VALUES
(2, 'SCH-1777212903144', 1, 1, 1, 1, 'Monday', '07:30:00', '09:00:00', '2026-04-26 22:15:03', '2026-04-26 22:15:03');

-- --------------------------------------------------------

--
-- Table structure for table `tblschoolyear`
--

CREATE TABLE `tblschoolyear` (
  `fldIndex` int(11) NOT NULL,
  `fldSchoolYearID` varchar(20) NOT NULL,
  `fldYearLabel` varchar(20) NOT NULL COMMENT 'e.g. 2025-2026',
  `fldIsActive` tinyint(1) NOT NULL DEFAULT 0,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblschoolyear`
--

INSERT INTO `tblschoolyear` (`fldIndex`, `fldSchoolYearID`, `fldYearLabel`, `fldIsActive`, `fldCreatedAt`) VALUES
(1, 'SY-2526', '2025-2026', 1, '2026-03-29 17:08:45'),
(2, 'SY-2627', '2026-2027', 0, '2026-04-28 13:23:35'),
(6, 'SY-2728', '2027-2028', 0, '2026-04-29 22:47:42');

-- --------------------------------------------------------

--
-- Table structure for table `tblsection`
--

CREATE TABLE `tblsection` (
  `fldIndex` int(11) NOT NULL,
  `fldSectionID` varchar(30) NOT NULL,
  `fldSectionCode` varchar(30) NOT NULL,
  `fldSectionName` varchar(50) NOT NULL,
  `fldProgramIndex` int(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldYearLevel` tinyint(1) NOT NULL,
  `fldSemesterIndex` int(11) NOT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldCapacity` int(11) DEFAULT 40,
  `fldIsActive` tinyint(1) NOT NULL DEFAULT 1,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblsection`
--

INSERT INTO `tblsection` (`fldIndex`, `fldSectionID`, `fldSectionCode`, `fldSectionName`, `fldProgramIndex`, `fldYearLevel`, `fldSemesterIndex`, `fldCapacity`, `fldIsActive`, `fldCreatedAt`, `fldUpdatedAt`) VALUES
(1, 'SEC-1001', 'PHIL1A', 'Philosophy Year 1 - Section A', 1, 1, 2, 40, 1, '2026-03-29 17:08:45', '2026-03-29 17:08:45'),
(2, 'SEC-1002', 'PHIL1B', 'Philosophy Year 1 - Section B', 1, 1, 2, 40, 1, '2026-03-29 17:08:45', '2026-03-29 17:08:45'),
(3, 'SEC-1003', 'THEO1A', 'Theology Year 1 - Section A', 2, 1, 2, 40, 1, '2026-03-29 17:08:45', '2026-03-29 17:08:45'),
(4, 'SEC-1004', 'THEO1B', 'Theology Year 1 - Section B', 2, 1, 2, 40, 1, '2026-03-29 17:08:45', '2026-03-29 17:08:45'),
(5, 'SEC-1005', 'PHIL1C', 'Philosophy Year 1 - Section C', 1, 1, 1, 1, 0, '2026-04-14 17:51:16', '2026-04-28 13:41:22'),
(6, 'SEC-1006', 'PHIL2A', 'Philosophy Year 2 - Section A', 1, 2, 3, 40, 1, '2026-04-28 13:41:49', '2026-04-28 13:41:49'),
(7, 'SEC-1007', 'PHIL3A', 'Philosopy Year 3 - Section A', 1, 3, 4, 40, 1, '2026-04-29 22:52:48', '2026-04-29 22:53:03');

-- --------------------------------------------------------

--
-- Table structure for table `tblsemester`
--

CREATE TABLE `tblsemester` (
  `fldIndex` int(11) NOT NULL,
  `fldSemesterID` varchar(20) NOT NULL,
  `fldSchoolYearIndex` int(11) NOT NULL COMMENT 'FK to tblschoolyear.fldIndex',
  `fldSemesterNumber` tinyint(1) NOT NULL COMMENT '1 = First, 2 = Second, 3 = Summer',
  `fldSemesterLabel` varchar(50) NOT NULL COMMENT 'e.g. First Semester 2025-2026',
  `fldStartDate` date NOT NULL,
  `fldEndDate` date NOT NULL,
  `fldIsActive` tinyint(1) NOT NULL DEFAULT 0,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldEnrollmentOpen` tinyint(4) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblsemester`
--

INSERT INTO `tblsemester` (`fldIndex`, `fldSemesterID`, `fldSchoolYearIndex`, `fldSemesterNumber`, `fldSemesterLabel`, `fldStartDate`, `fldEndDate`, `fldIsActive`, `fldCreatedAt`, `fldEnrollmentOpen`) VALUES
(1, 'SEM-2526-1', 1, 1, 'First Semester 2025-2026', '2025-08-01', '2025-12-31', 0, '2026-03-29 17:08:45', 1),
(2, 'SEM-2526-2', 1, 2, 'Second Semester 2025-2026', '2026-01-01', '2026-05-31', 0, '2026-03-29 17:08:45', 1),
(3, 'SEM-2627-1', 2, 1, 'First Semester 2026-2027', '2026-06-15', '2026-12-04', 0, '2026-04-28 13:23:35', 1),
(4, 'SEM-2728-1', 6, 1, 'First Semester 2027-2028', '2027-06-07', '2027-11-12', 1, '2026-04-29 22:47:42', 0),
(5, 'SEM-2627-2', 2, 2, 'Second Semester 2026-2027', '2027-01-04', '2027-05-14', 0, '2026-04-29 22:49:43', 1);

-- --------------------------------------------------------

--
-- Table structure for table `tblstudents`
--

CREATE TABLE `tblstudents` (
  `fldIndex` int(11) NOT NULL,
  `fldStudentID` varchar(30) NOT NULL,
  `fldApplicationIndex` int(11) NOT NULL COMMENT 'FK to tblapplications.fldIndex',
  `fldUserIndex` int(11) DEFAULT NULL COMMENT 'FK to tblusers.fldIndex',
  `fldFirstName` varchar(50) NOT NULL,
  `fldMiddleName` varchar(50) DEFAULT NULL,
  `fldLastName` varchar(50) NOT NULL,
  `fldDateOfBirth` date NOT NULL,
  `fldPlaceOfBirth` varchar(100) DEFAULT NULL,
  `fldGender` enum('Male') DEFAULT 'Male',
  `fldAddress` varchar(255) DEFAULT NULL,
  `fldContactNumber` varchar(20) DEFAULT NULL,
  `fldEmail` varchar(100) NOT NULL,
  `fldNationality` varchar(50) DEFAULT NULL,
  `fldReligion` varchar(50) DEFAULT NULL,
  `fldFatherName` varchar(100) DEFAULT NULL,
  `fldFatherOccupation` varchar(100) DEFAULT NULL,
  `fldMotherName` varchar(100) DEFAULT NULL,
  `fldMotherOccupation` varchar(100) DEFAULT NULL,
  `fldGuardianName` varchar(100) DEFAULT NULL,
  `fldGuardianContact` varchar(20) DEFAULT NULL,
  `fldBloodType` varchar(5) DEFAULT NULL,
  `fldMedicalConditions` text DEFAULT NULL,
  `fldAllergies` text DEFAULT NULL,
  `fldBaptismDate` date DEFAULT NULL,
  `fldBaptismChurch` varchar(150) DEFAULT NULL,
  `fldConfirmationDate` date DEFAULT NULL,
  `fldConfirmationChurch` varchar(150) DEFAULT NULL,
  `fldParishPriest` varchar(100) DEFAULT NULL,
  `fldDiocese` varchar(100) DEFAULT NULL,
  `fldSeminaryLevel` enum('Propaedeutic','College') NOT NULL,
  `fldCurrentYearLevel` tinyint(1) NOT NULL DEFAULT 1,
  `fldCurrentStatus` enum('Active','Inactive','LOA','Dismissed','Alumni') NOT NULL DEFAULT 'Active',
  `fldProgramIndex` int(11) NOT NULL COMMENT 'FK to tblprogram.fldIndex',
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblstudents`
--

INSERT INTO `tblstudents` (`fldIndex`, `fldStudentID`, `fldApplicationIndex`, `fldUserIndex`, `fldFirstName`, `fldMiddleName`, `fldLastName`, `fldDateOfBirth`, `fldPlaceOfBirth`, `fldGender`, `fldAddress`, `fldContactNumber`, `fldEmail`, `fldNationality`, `fldReligion`, `fldFatherName`, `fldFatherOccupation`, `fldMotherName`, `fldMotherOccupation`, `fldGuardianName`, `fldGuardianContact`, `fldBloodType`, `fldMedicalConditions`, `fldAllergies`, `fldBaptismDate`, `fldBaptismChurch`, `fldConfirmationDate`, `fldConfirmationChurch`, `fldParishPriest`, `fldDiocese`, `fldSeminaryLevel`, `fldCurrentYearLevel`, `fldCurrentStatus`, `fldProgramIndex`, `fldCreatedAt`, `fldUpdatedAt`) VALUES
(1, 'S2026-001', 1, 3, 'Charles', 'Benedict', 'Sual', '2005-12-09', NULL, NULL, 'Blk 7 Lot 19 San Carlos Homes P2, Barangay Balintawak, Lipa City, Batangas', '+639215353015', 'charlessual11@gmail.com', 'Filipino', 'Roman Catholic', 'Carnelito Sual Jr', 'Nurse', 'Katherine Sual', 'Financial Manager', '', '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'College', 3, 'Active', 1, '2026-04-27 09:39:28', '2026-04-30 22:06:58'),
(2, 'S2026-002', 2, 4, 'Francis Angelo', 'Dichoso', 'Grantoza', '2005-06-18', NULL, NULL, 'Richwood Park Village, San Pablo City, Laguna', '09931270935', 'officialtck2005@gmail.com', 'Filipino', 'Roman Catholic', 'Geoffrey Grantoza', 'Safety Officer', 'Marsha Grantoza', 'Housewife', '', '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'College', 2, 'Active', 1, '2026-04-27 11:28:16', '2026-04-30 22:06:58'),
(3, 'S2026-003', 3, 5, 'Justine Miko', '', 'Magsombol', '2007-08-05', NULL, NULL, '', '', 'barunge.0805@gmail.com', '', 'Roman Catholic', '', '', '', '', '', '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'College', 1, 'Active', 1, '2026-04-30 12:19:16', '2026-04-30 12:19:16'),
(4, 'S2026-004', 4, 6, 'Giancarlo', '', 'Plenos', '2006-09-26', NULL, NULL, '', '', 'giancarloplenos@gmail.com', '', '', '', '', '', '', '', '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'College', 1, 'Active', 1, '2026-04-30 12:22:46', '2026-04-30 12:22:46');

-- --------------------------------------------------------

--
-- Table structure for table `tblstudentsection`
--

CREATE TABLE `tblstudentsection` (
  `fldIndex` int(11) NOT NULL,
  `fldStudentSectionID` varchar(30) NOT NULL,
  `fldStudentIndex` int(11) NOT NULL COMMENT 'FK to tblstudents.fldIndex',
  `fldSectionIndex` int(11) NOT NULL COMMENT 'FK to tblsection.fldIndex',
  `fldSemesterIndex` int(11) NOT NULL COMMENT 'FK to tblsemester.fldIndex',
  `fldDateAssigned` date NOT NULL,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblstudentsection`
--

INSERT INTO `tblstudentsection` (`fldIndex`, `fldStudentSectionID`, `fldStudentIndex`, `fldSectionIndex`, `fldSemesterIndex`, `fldDateAssigned`, `fldCreatedAt`) VALUES
(1, 'SS-001', 1, 1, 2, '2026-04-27', '2026-04-27 09:39:28'),
(2, 'SS-002', 2, 1, 2, '2026-04-27', '2026-04-27 11:28:16'),
(3, 'SS-003', 1, 1, 1, '2026-04-27', '2026-04-27 22:54:05'),
(4, 'SS-004', 2, 1, 1, '2026-04-27', '2026-04-27 23:31:20'),
(5, 'SS-005', 1, 6, 3, '2026-04-28', '2026-04-28 13:46:46'),
(6, 'SS-006', 1, 6, 5, '2026-04-29', '2026-04-29 22:51:38'),
(7, 'SS-007', 1, 7, 4, '2026-04-29', '2026-04-29 23:10:07'),
(8, 'SS-008', 2, 6, 4, '2026-04-30', '2026-04-30 12:09:56'),
(9, 'SS-009', 3, 1, 4, '2026-04-30', '2026-04-30 12:19:16'),
(10, 'SS-010', 4, 1, 4, '2026-04-30', '2026-04-30 12:22:46');

-- --------------------------------------------------------

--
-- Table structure for table `tblusers`
--

CREATE TABLE `tblusers` (
  `fldIndex` int(11) NOT NULL,
  `fldUserID` varchar(30) NOT NULL,
  `fldUsername` varchar(50) NOT NULL,
  `fldPasswordHash` varchar(255) NOT NULL,
  `fldRole` enum('Registrar','Student','Admin') NOT NULL,
  `fldIsActive` tinyint(1) NOT NULL DEFAULT 1,
  `fldCreatedAt` datetime NOT NULL DEFAULT current_timestamp(),
  `fldUpdatedAt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `fldProfilePicture` longblob DEFAULT NULL,
  `fldProfilePictureType` varchar(50) DEFAULT NULL,
  `fldEmail` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tblusers`
--

INSERT INTO `tblusers` (`fldIndex`, `fldUserID`, `fldUsername`, `fldPasswordHash`, `fldRole`, `fldIsActive`, `fldCreatedAt`, `fldUpdatedAt`, `fldProfilePicture`, `fldProfilePictureType`, `fldEmail`) VALUES
(1, 'USR-1008', 'headofschool', '$2a$10$d4ZVUl6jEjzWDnFIOmLFkuTe.MbHtLmWA1p27DUBWqEpyrwKoKUGi', 'Admin', 1, '2026-04-26 14:13:16', '2026-04-28 13:12:11', NULL, NULL, NULL),
(2, 'USR-1001', 'Registrar', '$2a$10$d4ZVUl6jEjzWDnFIOmLFkuTe.MbHtLmWA1p27DUBWqEpyrwKoKUGi', 'Registrar', 1, '2026-03-30 10:23:52', '2026-04-26 14:26:35', NULL, NULL, NULL),
(3, 'USR-1003', 'S2026-001', '$2a$10$iffMXXm0I13sxPWcp3tAmObNGTDQRu1oTr3FnV100ZWlFOFN8mNE6', 'Student', 1, '2026-04-27 09:39:28', '2026-05-03 16:39:05', NULL, NULL, 'charlessual11@gmail.com'),
(4, 'USR-1004', 'Francis', '$2a$10$5rlbikGSk0jyV/cyOh2Hv.DDDqbctWY9TWeitCmCXEdqudINWbsry', 'Student', 1, '2026-04-27 11:28:16', '2026-05-03 14:35:16', NULL, NULL, 'officialtck2005@gmail.com'),
(5, 'USR-1005', 'S2026-003', '$2a$10$QziVrNvP2F/skn5wVOdG4.Y8me0IXU3xI/pypxVY7/anrX6IsaoBa', 'Student', 1, '2026-04-30 12:19:16', '2026-05-03 14:35:16', NULL, NULL, 'barunge.0805@gmail.com'),
(6, 'USR-1006', 'S2026-004', '$2a$10$kV6mw70DKE/1EuljriW5Juggz6H/WWeKHzur6NS5W4REdbt8xU..q', 'Student', 1, '2026-04-30 12:22:46', '2026-05-03 14:35:16', NULL, NULL, 'giancarloplenos@gmail.com');

--
-- Indexes for dumped tables
--

ALTER TABLE `tblalumni`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldAlumniID` (`fldAlumniID`),
  ADD UNIQUE KEY `fldStudentIndex` (`fldStudentIndex`),
  ADD KEY `fldProgramIndex` (`fldProgramIndex`);

ALTER TABLE `tblapplicants`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldApplicantID` (`fldApplicantID`),
  ADD KEY `fldProgramIndex` (`fldProgramIndex`);

ALTER TABLE `tblapplications`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldApplicationID` (`fldApplicationID`),
  ADD KEY `fldApplicantIndex` (`fldApplicantIndex`),
  ADD KEY `fldSchoolYearIndex` (`fldSchoolYearIndex`);

ALTER TABLE `tblauditlog`
  ADD PRIMARY KEY (`fldIndex`);

ALTER TABLE `tblbackuplog`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldBackupID` (`fldBackupID`),
  ADD KEY `fldPerformedByIndex` (`fldPerformedByIndex`);

ALTER TABLE `tblcourses`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldCourseID` (`fldCourseID`),
  ADD KEY `fldProgramIndex` (`fldProgramIndex`),
  ADD KEY `FKd05u48yonkqi330x0uq3am2hb` (`fldCurriculumIndex`);

ALTER TABLE `tblcurriculum`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `UKsmgn28k0qxq5juvrnwwc1pbo0` (`fldCurriculumID`),
  ADD KEY `FKd51mbs3e4jllhvsbsmsi61kxv` (`fldProgramIndex`);

ALTER TABLE `tbldocuments`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldDocumentID` (`fldDocumentID`),
  ADD KEY `fldStudentIndex` (`fldStudentIndex`);

ALTER TABLE `tblenrollment`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldEnrollmentID` (`fldEnrollmentID`),
  ADD UNIQUE KEY `uq_enrollment` (`fldStudentIndex`,`fldSemesterIndex`),
  ADD KEY `fldProgramIndex` (`fldProgramIndex`),
  ADD KEY `fldSemesterIndex` (`fldSemesterIndex`);

ALTER TABLE `tblenrollmentsubjects`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldEnrollmentSubjectID` (`fldEnrollmentSubjectID`),
  ADD UNIQUE KEY `uq_enrollment_course` (`fldEnrollmentIndex`,`fldCourseIndex`),
  ADD KEY `fldCourseIndex` (`fldCourseIndex`),
  ADD KEY `fldScheduleIndex` (`fldScheduleIndex`);

ALTER TABLE `tblentranceexam`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldExamID` (`fldExamID`),
  ADD KEY `fldApplicantIndex` (`fldApplicantIndex`);

ALTER TABLE `tblgrades`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldGradeID` (`fldGradeID`),
  ADD UNIQUE KEY `fldEnrollmentSubjectIndex` (`fldEnrollmentSubjectIndex`),
  ADD KEY `fldStudentIndex` (`fldStudentIndex`),
  ADD KEY `fldCourseIndex` (`fldCourseIndex`),
  ADD KEY `fldSemesterIndex` (`fldSemesterIndex`),
  ADD KEY `fldEnteredByUserIndex` (`fldEnteredByUserIndex`);

ALTER TABLE `tblinstructors`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldInstructorID` (`fldInstructorID`);

ALTER TABLE `tblonline_submissions`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldSubmissionID` (`fldSubmissionID`),
  ADD KEY `fldProgramIndex` (`fldProgramIndex`);

ALTER TABLE `tblpassword_reset_tokens`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldToken` (`fldToken`),
  ADD KEY `fldUserIndex` (`fldUserIndex`);

ALTER TABLE `tblprerequisites`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldPrerequisiteID` (`fldPrerequisiteID`),
  ADD UNIQUE KEY `uq_prereq` (`fldCourseIndex`,`fldPrerequisiteIndex`),
  ADD KEY `fldPrerequisiteIndex` (`fldPrerequisiteIndex`);

ALTER TABLE `tblprogram`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldProgramID` (`fldProgramID`),
  ADD UNIQUE KEY `fldProgramCode` (`fldProgramCode`);

ALTER TABLE `tblreports`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldReportID` (`fldReportID`),
  ADD KEY `fldStudentIndex` (`fldStudentIndex`),
  ADD KEY `fldSemesterIndex` (`fldSemesterIndex`),
  ADD KEY `fldGeneratedByIndex` (`fldGeneratedByIndex`);

ALTER TABLE `tblrooms`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldRoomID` (`fldRoomID`);

ALTER TABLE `tblschedule`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldScheduleID` (`fldScheduleID`),
  ADD KEY `fldSectionIndex` (`fldSectionIndex`),
  ADD KEY `fldCourseIndex` (`fldCourseIndex`),
  ADD KEY `fldInstructorIndex` (`fldInstructorIndex`),
  ADD KEY `fldRoomIndex` (`fldRoomIndex`);

ALTER TABLE `tblschoolyear`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldSchoolYearID` (`fldSchoolYearID`);

ALTER TABLE `tblsection`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldSectionID` (`fldSectionID`),
  ADD KEY `fldProgramIndex` (`fldProgramIndex`),
  ADD KEY `fldSemesterIndex` (`fldSemesterIndex`);

ALTER TABLE `tblsemester`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldSemesterID` (`fldSemesterID`),
  ADD KEY `fldSchoolYearIndex` (`fldSchoolYearIndex`);

ALTER TABLE `tblstudents`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldStudentID` (`fldStudentID`),
  ADD UNIQUE KEY `fldApplicationIndex` (`fldApplicationIndex`),
  ADD KEY `fldUserIndex` (`fldUserIndex`),
  ADD KEY `fldProgramIndex` (`fldProgramIndex`);

ALTER TABLE `tblstudentsection`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldStudentSectionID` (`fldStudentSectionID`),
  ADD UNIQUE KEY `uq_student_section_sem` (`fldStudentIndex`,`fldSectionIndex`,`fldSemesterIndex`),
  ADD KEY `fldSectionIndex` (`fldSectionIndex`),
  ADD KEY `fldSemesterIndex` (`fldSemesterIndex`);

ALTER TABLE `tblusers`
  ADD PRIMARY KEY (`fldIndex`),
  ADD UNIQUE KEY `fldUserID` (`fldUserID`),
  ADD UNIQUE KEY `fldUsername` (`fldUsername`);

--
-- AUTO_INCREMENT for dumped tables
--

ALTER TABLE `tblalumni` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;
ALTER TABLE `tblapplicants` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;
ALTER TABLE `tblapplications` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;
ALTER TABLE `tblauditlog` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=107;
ALTER TABLE `tblbackuplog` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT;
ALTER TABLE `tblcourses` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=64;
ALTER TABLE `tblcurriculum` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;
ALTER TABLE `tbldocuments` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT;
ALTER TABLE `tblenrollment` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;
ALTER TABLE `tblenrollmentsubjects` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=47;
ALTER TABLE `tblentranceexam` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT;
ALTER TABLE `tblgrades` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=47;
ALTER TABLE `tblinstructors` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;
ALTER TABLE `tblonline_submissions` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;
ALTER TABLE `tblpassword_reset_tokens` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;
ALTER TABLE `tblprerequisites` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=45;
ALTER TABLE `tblprogram` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;
ALTER TABLE `tblreports` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT;
ALTER TABLE `tblrooms` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;
ALTER TABLE `tblschedule` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;
ALTER TABLE `tblschoolyear` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;
ALTER TABLE `tblsection` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;
ALTER TABLE `tblsemester` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;
ALTER TABLE `tblstudents` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;
ALTER TABLE `tblstudentsection` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;
ALTER TABLE `tblusers` MODIFY `fldIndex` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- Constraints for dumped tables
--

ALTER TABLE `tblalumni`
  ADD CONSTRAINT `tblalumni_ibfk_1` FOREIGN KEY (`fldStudentIndex`) REFERENCES `tblstudents` (`fldIndex`),
  ADD CONSTRAINT `tblalumni_ibfk_2` FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`);

ALTER TABLE `tblapplicants`
  ADD CONSTRAINT `tblapplicants_ibfk_1` FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`);

ALTER TABLE `tblapplications`
  ADD CONSTRAINT `tblapplications_ibfk_1` FOREIGN KEY (`fldApplicantIndex`) REFERENCES `tblapplicants` (`fldIndex`),
  ADD CONSTRAINT `tblapplications_ibfk_2` FOREIGN KEY (`fldSchoolYearIndex`) REFERENCES `tblschoolyear` (`fldIndex`);

ALTER TABLE `tblbackuplog`
  ADD CONSTRAINT `tblbackuplog_ibfk_1` FOREIGN KEY (`fldPerformedByIndex`) REFERENCES `tblusers` (`fldIndex`);

ALTER TABLE `tblcourses`
  ADD CONSTRAINT `FKd05u48yonkqi330x0uq3am2hb` FOREIGN KEY (`fldCurriculumIndex`) REFERENCES `tblcurriculum` (`fldIndex`),
  ADD CONSTRAINT `tblcourses_ibfk_1` FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`);

ALTER TABLE `tblcurriculum`
  ADD CONSTRAINT `FKd51mbs3e4jllhvsbsmsi61kxv` FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`);

ALTER TABLE `tbldocuments`
  ADD CONSTRAINT `tbldocuments_ibfk_1` FOREIGN KEY (`fldStudentIndex`) REFERENCES `tblstudents` (`fldIndex`);

ALTER TABLE `tblenrollment`
  ADD CONSTRAINT `tblenrollment_ibfk_1` FOREIGN KEY (`fldStudentIndex`) REFERENCES `tblstudents` (`fldIndex`),
  ADD CONSTRAINT `tblenrollment_ibfk_2` FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`),
  ADD CONSTRAINT `tblenrollment_ibfk_3` FOREIGN KEY (`fldSemesterIndex`) REFERENCES `tblsemester` (`fldIndex`);

ALTER TABLE `tblenrollmentsubjects`
  ADD CONSTRAINT `tblenrollmentsubjects_ibfk_1` FOREIGN KEY (`fldEnrollmentIndex`) REFERENCES `tblenrollment` (`fldIndex`),
  ADD CONSTRAINT `tblenrollmentsubjects_ibfk_2` FOREIGN KEY (`fldCourseIndex`) REFERENCES `tblcourses` (`fldIndex`),
  ADD CONSTRAINT `tblenrollmentsubjects_ibfk_3` FOREIGN KEY (`fldScheduleIndex`) REFERENCES `tblschedule` (`fldIndex`);

ALTER TABLE `tblentranceexam`
  ADD CONSTRAINT `tblentranceexam_ibfk_1` FOREIGN KEY (`fldApplicantIndex`) REFERENCES `tblapplicants` (`fldIndex`);

ALTER TABLE `tblgrades`
  ADD CONSTRAINT `tblgrades_ibfk_1` FOREIGN KEY (`fldEnrollmentSubjectIndex`) REFERENCES `tblenrollmentsubjects` (`fldIndex`),
  ADD CONSTRAINT `tblgrades_ibfk_2` FOREIGN KEY (`fldStudentIndex`) REFERENCES `tblstudents` (`fldIndex`),
  ADD CONSTRAINT `tblgrades_ibfk_3` FOREIGN KEY (`fldCourseIndex`) REFERENCES `tblcourses` (`fldIndex`),
  ADD CONSTRAINT `tblgrades_ibfk_4` FOREIGN KEY (`fldSemesterIndex`) REFERENCES `tblsemester` (`fldIndex`),
  ADD CONSTRAINT `tblgrades_ibfk_5` FOREIGN KEY (`fldEnteredByUserIndex`) REFERENCES `tblusers` (`fldIndex`);

ALTER TABLE `tblonline_submissions`
  ADD CONSTRAINT `tblonline_submissions_ibfk_1` FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`);

ALTER TABLE `tblpassword_reset_tokens`
  ADD CONSTRAINT `tblpassword_reset_tokens_ibfk_1` FOREIGN KEY (`fldUserIndex`) REFERENCES `tblusers` (`fldIndex`);

ALTER TABLE `tblprerequisites`
  ADD CONSTRAINT `tblprerequisites_ibfk_1` FOREIGN KEY (`fldCourseIndex`) REFERENCES `tblcourses` (`fldIndex`),
  ADD CONSTRAINT `tblprerequisites_ibfk_2` FOREIGN KEY (`fldPrerequisiteIndex`) REFERENCES `tblcourses` (`fldIndex`);

ALTER TABLE `tblreports`
  ADD CONSTRAINT `tblreports_ibfk_1` FOREIGN KEY (`fldStudentIndex`) REFERENCES `tblstudents` (`fldIndex`),
  ADD CONSTRAINT `tblreports_ibfk_2` FOREIGN KEY (`fldSemesterIndex`) REFERENCES `tblsemester` (`fldIndex`),
  ADD CONSTRAINT `tblreports_ibfk_3` FOREIGN KEY (`fldGeneratedByIndex`) REFERENCES `tblusers` (`fldIndex`);

ALTER TABLE `tblschedule`
  ADD CONSTRAINT `tblschedule_ibfk_1` FOREIGN KEY (`fldSectionIndex`) REFERENCES `tblsection` (`fldIndex`),
  ADD CONSTRAINT `tblschedule_ibfk_2` FOREIGN KEY (`fldCourseIndex`) REFERENCES `tblcourses` (`fldIndex`),
  ADD CONSTRAINT `tblschedule_ibfk_3` FOREIGN KEY (`fldInstructorIndex`) REFERENCES `tblinstructors` (`fldIndex`),
  ADD CONSTRAINT `tblschedule_ibfk_4` FOREIGN KEY (`fldRoomIndex`) REFERENCES `tblrooms` (`fldIndex`);

ALTER TABLE `tblsection`
  ADD CONSTRAINT `tblsection_ibfk_1` FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`),
  ADD CONSTRAINT `tblsection_ibfk_2` FOREIGN KEY (`fldSemesterIndex`) REFERENCES `tblsemester` (`fldIndex`);

ALTER TABLE `tblsemester`
  ADD CONSTRAINT `tblsemester_ibfk_1` FOREIGN KEY (`fldSchoolYearIndex`) REFERENCES `tblschoolyear` (`fldIndex`);

ALTER TABLE `tblstudents`
  ADD CONSTRAINT `tblstudents_ibfk_1` FOREIGN KEY (`fldApplicationIndex`) REFERENCES `tblapplications` (`fldIndex`),
  ADD CONSTRAINT `tblstudents_ibfk_2` FOREIGN KEY (`fldUserIndex`) REFERENCES `tblusers` (`fldIndex`),
  ADD CONSTRAINT `tblstudents_ibfk_3` FOREIGN KEY (`fldProgramIndex`) REFERENCES `tblprogram` (`fldIndex`);

ALTER TABLE `tblstudentsection`
  ADD CONSTRAINT `tblstudentsection_ibfk_1` FOREIGN KEY (`fldStudentIndex`) REFERENCES `tblstudents` (`fldIndex`),
  ADD CONSTRAINT `tblstudentsection_ibfk_2` FOREIGN KEY (`fldSectionIndex`) REFERENCES `tblsection` (`fldIndex`),
  ADD CONSTRAINT `tblstudentsection_ibfk_3` FOREIGN KEY (`fldSemesterIndex`) REFERENCES `tblsemester` (`fldIndex`);

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
