-- MySQL dump 10.13  Distrib 9.5.0, for macos15.7 (arm64)
--
-- Host: localhost    Database: erpdb
-- ------------------------------------------------------
-- Server version	9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'a229cabe-caf7-11f0-986a-0a752d81007c:1-446';

--
-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `announcement_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `message` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`announcement_id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `announcements_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcements`
--

LOCK TABLES `announcements` WRITE;
/*!40000 ALTER TABLE `announcements` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `assignments`
--

DROP TABLE IF EXISTS `assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignments` (
  `assignment_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `due_date` date NOT NULL,
  `posted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `assignments_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assignments`
--

LOCK TABLES `assignments` WRITE;
/*!40000 ALTER TABLE `assignments` DISABLE KEYS */;
INSERT INTO `assignments` VALUES (1,1,'DSA Homework 3',NULL,'2025-11-30','2025-11-27 10:22:36'),(2,202,'OS Project Milestone',NULL,'2025-12-02','2025-11-27 10:22:36'),(3,303,'DM Worksheet 2',NULL,'2025-12-07','2025-11-27 10:22:36');
/*!40000 ALTER TABLE `assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance`
--

DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `attendance_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `user_id` int NOT NULL,
  `date` date NOT NULL,
  `status` enum('Present','Absent','Late') DEFAULT 'Present',
  PRIMARY KEY (`attendance_id`),
  KEY `course_id` (`course_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `attendance_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`),
  CONSTRAINT `attendance_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `students` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance`
--

LOCK TABLES `attendance` WRITE;
/*!40000 ALTER TABLE `attendance` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_defaults`
--

DROP TABLE IF EXISTS `course_defaults`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_defaults` (
  `course_id` int NOT NULL,
  `default_section_id` int DEFAULT NULL,
  PRIMARY KEY (`course_id`),
  KEY `fk_cd_section` (`default_section_id`),
  CONSTRAINT `fk_cd_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`),
  CONSTRAINT `fk_cd_section` FOREIGN KEY (`default_section_id`) REFERENCES `sections` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_defaults`
--

LOCK TABLES `course_defaults` WRITE;
/*!40000 ALTER TABLE `course_defaults` DISABLE KEYS */;
/*!40000 ALTER TABLE `course_defaults` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_grading_scheme`
--

DROP TABLE IF EXISTS `course_grading_scheme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_grading_scheme` (
  `course_id` int NOT NULL,
  `endsem_weight` decimal(5,2) NOT NULL,
  `midsem_weight` decimal(5,2) NOT NULL,
  `quiz_weight` decimal(5,2) NOT NULL,
  PRIMARY KEY (`course_id`),
  CONSTRAINT `fk_scheme_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_grading_scheme`
--

LOCK TABLES `course_grading_scheme` WRITE;
/*!40000 ALTER TABLE `course_grading_scheme` DISABLE KEYS */;
INSERT INTO `course_grading_scheme` VALUES (1,45.00,35.00,20.00),(202,45.00,35.00,20.00),(303,45.00,35.00,20.00),(304,45.00,35.00,20.00),(305,45.00,35.00,20.00);
/*!40000 ALTER TABLE `course_grading_scheme` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `course_id` int NOT NULL AUTO_INCREMENT,
  `course_name` varchar(100) NOT NULL DEFAULT 'Unknown',
  `instructor` varchar(100) NOT NULL DEFAULT 'TBA',
  `code` varchar(10) NOT NULL,
  `title` varchar(100) NOT NULL,
  `credits` int NOT NULL,
  `reg_deadline` date DEFAULT NULL,
  `drop_deadline` date DEFAULT NULL,
  PRIMARY KEY (`course_id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=308 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` VALUES (1,'Data Structures','Prof. A','CS101','Data Structures',4,'2025-12-04','2015-11-30'),(202,'Operating Systems','Prof. B','CS202','Operating Systems',4,'2025-12-04','2015-11-30'),(303,'Discrete Mathematics','Prof. C','MA103','Discrete Mathematics',3,'2025-12-04','2015-11-30'),(304,'Algorithms','Prof. D','CS204','Algorithms',4,'2025-12-07','2015-11-30'),(305,'Computer Networks','Prof. E','CS305','Computer Networks',4,'2025-12-07','2015-11-30');
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollments`
--

DROP TABLE IF EXISTS `enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollments` (
  `enrollment_id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `section_id` int NOT NULL,
  `status` varchar(20) DEFAULT 'REGISTERED',
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `student_id` (`student_id`,`section_id`),
  UNIQUE KEY `uq_enroll_student_section` (`student_id`,`section_id`),
  KEY `section_id` (`section_id`),
  CONSTRAINT `enrollments_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`),
  CONSTRAINT `enrollments_ibfk_2` FOREIGN KEY (`section_id`) REFERENCES `sections` (`section_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollments`
--

LOCK TABLES `enrollments` WRITE;
/*!40000 ALTER TABLE `enrollments` DISABLE KEYS */;
INSERT INTO `enrollments` VALUES (4,3,1,'REGISTERED'),(5,3,2,'REGISTERED'),(9,3,4,'REGISTERED'),(10,3,5,'REGISTERED'),(11,3,3,'REGISTERED');
/*!40000 ALTER TABLE `enrollments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fees`
--

DROP TABLE IF EXISTS `fees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fees` (
  `fee_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `semester` varchar(10) NOT NULL,
  `amount` int NOT NULL,
  `status` enum('PAID','UNPAID','PARTIAL') DEFAULT 'UNPAID',
  `due_date` date DEFAULT NULL,
  `paid_date` date DEFAULT NULL,
  PRIMARY KEY (`fee_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `fees_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `authdb`.`users_auth` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fees`
--

LOCK TABLES `fees` WRITE;
/*!40000 ALTER TABLE `fees` DISABLE KEYS */;
INSERT INTO `fees` VALUES (1,3,'2025S',180000,'PAID','2025-01-15','2025-01-10'),(2,3,'2025F',180000,'UNPAID','2025-08-10',NULL);
/*!40000 ALTER TABLE `fees` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `final_grades`
--

DROP TABLE IF EXISTS `final_grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `final_grades` (
  `fg_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `course_id` int NOT NULL,
  `semester` varchar(10) NOT NULL,
  `letter_grade` varchar(3) DEFAULT NULL,
  `numeric_grade` decimal(5,2) DEFAULT NULL,
  PRIMARY KEY (`fg_id`),
  KEY `user_id` (`user_id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `final_grades_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `authdb`.`users_auth` (`user_id`),
  CONSTRAINT `final_grades_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `final_grades`
--

LOCK TABLES `final_grades` WRITE;
/*!40000 ALTER TABLE `final_grades` DISABLE KEYS */;
INSERT INTO `final_grades` VALUES (4,1,1,'2025S',NULL,NULL),(5,1,202,'2025S',NULL,NULL),(6,1,305,'2025S',NULL,NULL),(7,1,303,'2025S',NULL,NULL),(11,3,1,'Monsoon','A+',97.63),(12,3,202,'Monsoon','B',67.25),(13,3,305,'Monsoon',NULL,NULL),(14,3,304,'Monsoon',NULL,NULL);
/*!40000 ALTER TABLE `final_grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grades`
--

DROP TABLE IF EXISTS `grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grades` (
  `grade_id` int NOT NULL AUTO_INCREMENT,
  `enrollment_id` int NOT NULL,
  `component` varchar(50) DEFAULT NULL,
  `score` decimal(5,2) DEFAULT NULL,
  `final_grade` decimal(5,2) DEFAULT NULL,
  `max_score` decimal(6,2) DEFAULT NULL,
  PRIMARY KEY (`grade_id`),
  UNIQUE KEY `unique_component_per_enrollment` (`enrollment_id`,`component`),
  KEY `enrollment_id` (`enrollment_id`),
  CONSTRAINT `fk_grades_reg` FOREIGN KEY (`enrollment_id`) REFERENCES `registrations` (`reg_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grades`
--

LOCK TABLES `grades` WRITE;
/*!40000 ALTER TABLE `grades` DISABLE KEYS */;
INSERT INTO `grades` VALUES (31,12,'Midsem',22.00,NULL,40.00),(32,12,'Endsem',48.00,NULL,60.00),(36,20,'Quiz',7.00,NULL,10.00),(37,20,'Midsem',28.00,NULL,40.00),(38,20,'Endsem',53.00,NULL,60.00),(40,12,'Quiz',6.00,NULL,10.00),(42,25,'Quiz',10.00,NULL,10.00),(43,25,'Midsem',39.00,NULL,40.00),(44,25,'Endsem',58.00,NULL,60.00);
/*!40000 ALTER TABLE `grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grading_components`
--

DROP TABLE IF EXISTS `grading_components`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grading_components` (
  `component_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `section_id` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `max_score` decimal(8,2) NOT NULL,
  `weight_percent` decimal(5,2) NOT NULL,
  PRIMARY KEY (`component_id`),
  UNIQUE KEY `uq_comp_course_section_name` (`course_id`,`section_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grading_components`
--

LOCK TABLES `grading_components` WRITE;
/*!40000 ALTER TABLE `grading_components` DISABLE KEYS */;
INSERT INTO `grading_components` VALUES (1,1,1,'Quiz',10.00,20.00),(3,1,1,'Midsem',40.00,35.00),(5,1,1,'Endsem',60.00,45.00),(7,202,2,'Quiz',10.00,20.00),(8,202,2,'Midsem',40.00,35.00),(9,202,2,'Endsem',60.00,45.00);
/*!40000 ALTER TABLE `grading_components` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grading_weights`
--

DROP TABLE IF EXISTS `grading_weights`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grading_weights` (
  `course_id` int NOT NULL,
  `weight_endsem` decimal(5,2) NOT NULL DEFAULT '0.45',
  `weight_midsem` decimal(5,2) NOT NULL DEFAULT '0.35',
  `weight_quiz` decimal(5,2) NOT NULL DEFAULT '0.20',
  PRIMARY KEY (`course_id`),
  CONSTRAINT `grading_weights_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grading_weights`
--

LOCK TABLES `grading_weights` WRITE;
/*!40000 ALTER TABLE `grading_weights` DISABLE KEYS */;
/*!40000 ALTER TABLE `grading_weights` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hostel_allocations`
--

DROP TABLE IF EXISTS `hostel_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hostel_allocations` (
  `allocation_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `room_no` varchar(20) DEFAULT NULL,
  `block` varchar(20) DEFAULT NULL,
  `status` enum('ALLOCATED','PENDING','NOT_ALLOCATED') DEFAULT 'PENDING',
  `check_in` date DEFAULT NULL,
  `check_out` date DEFAULT NULL,
  PRIMARY KEY (`allocation_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `hostel_allocations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `authdb`.`users_auth` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hostel_allocations`
--

LOCK TABLES `hostel_allocations` WRITE;
/*!40000 ALTER TABLE `hostel_allocations` DISABLE KEYS */;
INSERT INTO `hostel_allocations` VALUES (1,3,'B-312','Boys Block B','ALLOCATED','2025-01-08',NULL);
/*!40000 ALTER TABLE `hostel_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instructors`
--

DROP TABLE IF EXISTS `instructors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `instructors` (
  `user_id` int NOT NULL,
  `name` varchar(100) NOT NULL DEFAULT 'Unknown',
  `department` varchar(50) DEFAULT NULL,
  `email` varchar(120) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instructors`
--

LOCK TABLES `instructors` WRITE;
/*!40000 ALTER TABLE `instructors` DISABLE KEYS */;
INSERT INTO `instructors` VALUES (2,'Dr. Inst 1','Computer Science','dr.inst1@iiitd.ac.in','+911234567890');
/*!40000 ALTER TABLE `instructors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `materials`
--

DROP TABLE IF EXISTS `materials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `materials` (
  `material_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `file_path` varchar(500) NOT NULL,
  `upload_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`material_id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `materials_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `materials`
--

LOCK TABLES `materials` WRITE;
/*!40000 ALTER TABLE `materials` DISABLE KEYS */;
/*!40000 ALTER TABLE `materials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registrations`
--

DROP TABLE IF EXISTS `registrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registrations` (
  `reg_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int DEFAULT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`reg_id`),
  UNIQUE KEY `uq_reg_course_user` (`course_id`,`user_id`),
  KEY `fk_reg_user` (`user_id`),
  KEY `fk_reg_course` (`course_id`),
  CONSTRAINT `fk_reg_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reg_user` FOREIGN KEY (`user_id`) REFERENCES `students` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `registrations_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registrations`
--

LOCK TABLES `registrations` WRITE;
/*!40000 ALTER TABLE `registrations` DISABLE KEYS */;
INSERT INTO `registrations` VALUES (25,1,3),(12,202,3),(24,303,3),(20,304,3),(23,305,3);
/*!40000 ALTER TABLE `registrations` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_after_registration_insert` AFTER INSERT ON `registrations` FOR EACH ROW BEGIN
  INSERT IGNORE INTO enrollments (student_id, section_id)
  SELECT NEW.user_id,
         COALESCE(cd.default_section_id, smin.section_id)
  FROM (SELECT MIN(section_id) AS section_id FROM sections WHERE course_id = NEW.course_id) smin
  LEFT JOIN course_defaults cd ON cd.course_id = NEW.course_id
  WHERE COALESCE(cd.default_section_id, smin.section_id) IS NOT NULL;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `sections`
--

DROP TABLE IF EXISTS `sections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sections` (
  `section_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `instructor_id` int DEFAULT NULL,
  `day` varchar(10) DEFAULT NULL,
  `time` varchar(20) DEFAULT NULL,
  `room` varchar(20) DEFAULT NULL,
  `capacity` int DEFAULT NULL,
  `semester` varchar(20) DEFAULT NULL,
  `year` int DEFAULT NULL,
  PRIMARY KEY (`section_id`),
  KEY `course_id` (`course_id`),
  KEY `instructor_id` (`instructor_id`),
  CONSTRAINT `sections_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`),
  CONSTRAINT `sections_ibfk_2` FOREIGN KEY (`instructor_id`) REFERENCES `instructors` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sections`
--

LOCK TABLES `sections` WRITE;
/*!40000 ALTER TABLE `sections` DISABLE KEYS */;
INSERT INTO `sections` VALUES (1,1,2,'Mon','10:00','B101',40,'Spring',2025),(2,202,2,'Wed','14:00','B102',40,'Spring',2025),(3,303,NULL,'TBD','TBD','TBD',40,'TBD',2025),(4,304,NULL,'TBD','TBD','TBD',40,'TBD',2025),(5,305,NULL,'TBD','TBD','TBD',40,'TBD',2025);
/*!40000 ALTER TABLE `sections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `settings`
--

DROP TABLE IF EXISTS `settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `settings` (
  `key` varchar(50) NOT NULL,
  `value` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `settings`
--

LOCK TABLES `settings` WRITE;
/*!40000 ALTER TABLE `settings` DISABLE KEYS */;
INSERT INTO `settings` VALUES ('maintenance','false'),('maintenance_mode','OFF');
/*!40000 ALTER TABLE `settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `user_id` int NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `roll_no` varchar(20) NOT NULL,
  `program` varchar(50) DEFAULT NULL,
  `year` int DEFAULT NULL,
  `email` varchar(120) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `roll_no` (`roll_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (3,'Student One','ROLL123','CSAM',3,'stu1@iiitd.ac.in','9876543210'),(5,'Student Two','ROLL456','CSE',2,'amit206@iiitd.ac.in','9999999999');
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `submissions`
--

DROP TABLE IF EXISTS `submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submissions` (
  `submission_id` int NOT NULL AUTO_INCREMENT,
  `assignment_id` int NOT NULL,
  `user_id` int NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `submitted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `grade` varchar(10) DEFAULT NULL,
  `feedback` text,
  PRIMARY KEY (`submission_id`),
  KEY `assignment_id` (`assignment_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `submissions_ibfk_1` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`assignment_id`),
  CONSTRAINT `submissions_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `students` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `submissions`
--

LOCK TABLES `submissions` WRITE;
/*!40000 ALTER TABLE `submissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `submissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `timetable`
--

DROP TABLE IF EXISTS `timetable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `timetable` (
  `id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `day` enum('MON','TUE','WED','THU','FRI','SAT') DEFAULT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `room` varchar(20) DEFAULT 'TBD',
  `capacity` int NOT NULL DEFAULT '60',
  `semester` varchar(10) DEFAULT '2025S',
  `year` int DEFAULT '2025',
  PRIMARY KEY (`id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `timetable_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `timetable`
--

LOCK TABLES `timetable` WRITE;
/*!40000 ALTER TABLE `timetable` DISABLE KEYS */;
INSERT INTO `timetable` VALUES (1,1,'MON','09:00:00','10:00:00','LH101',60,'2025S',2025),(2,202,'MON','11:00:00','12:30:00','LH201',60,'2025S',2025),(3,303,'TUE','14:00:00','15:30:00','LH301',60,'2025S',2025),(4,304,'WED','09:00:00','10:00:00','LH102',60,'2025S',2025),(5,305,'THU','10:00:00','11:30:00','LH202',60,'2025S',2025);
/*!40000 ALTER TABLE `timetable` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-11 16:32:01
