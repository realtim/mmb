CREATE DATABASE  IF NOT EXISTS `datacollector` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `datacollector`;
-- MySQL dump 10.13  Distrib 5.6.17, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: datacollector
-- ------------------------------------------------------
-- Server version	5.6.21-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Distances`
--

DROP TABLE IF EXISTS `Distances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Distances` (
  `distance_id` int(11) NOT NULL,
  `raid_id` int(11) NOT NULL,
  `distance_name` varchar(50) NOT NULL,
  PRIMARY KEY (`distance_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Distances`
--

LOCK TABLES `Distances` WRITE;
/*!40000 ALTER TABLE `Distances` DISABLE KEYS */;
/*!40000 ALTER TABLE `Distances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `LevelPointDiscounts`
--

DROP TABLE IF EXISTS `LevelPointDiscounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LevelPointDiscounts` (
  `levelpointdiscount_id` int(11) NOT NULL,
  `distance_id` int(11) NOT NULL,
  `levelpointdiscount_value` int(11) NOT NULL,
  `levelpointdiscount_start` int(11) NOT NULL,
  `levelpointdiscount_finish` int(11) NOT NULL,
  PRIMARY KEY (`levelpointdiscount_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `LevelPointDiscounts`
--

LOCK TABLES `LevelPointDiscounts` WRITE;
/*!40000 ALTER TABLE `LevelPointDiscounts` DISABLE KEYS */;
/*!40000 ALTER TABLE `LevelPointDiscounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `LevelPoints`
--

DROP TABLE IF EXISTS `LevelPoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LevelPoints` (
  `levelpoint_id` int(11) NOT NULL,
  `pointtype_id` int(11) NOT NULL,
  `distance_id` int(11) NOT NULL,
  `levelpoint_order` int(11) NOT NULL,
  `levelpoint_name` varchar(50) NOT NULL,
  `levelpoint_penalty` int(11) DEFAULT NULL,
  `levelpoint_mindatetime` varchar(20) DEFAULT NULL,
  `levelpoint_maxdatetime` varchar(20) DEFAULT NULL,
  `scanpoint_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`levelpoint_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `LevelPoints`
--

LOCK TABLES `LevelPoints` WRITE;
/*!40000 ALTER TABLE `LevelPoints` DISABLE KEYS */;
/*!40000 ALTER TABLE `LevelPoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `MetaColumns`
--

DROP TABLE IF EXISTS `MetaColumns`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `MetaColumns` (
  `column_id` int(11) NOT NULL,
  `table_id` int(11) NOT NULL,
  `column_name` varchar(50) NOT NULL,
  `column_order` int(11) NOT NULL,
  `column_data_type` varchar(50) NOT NULL,
  `is_primary_key` int(11) NOT NULL,
  PRIMARY KEY (`column_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `MetaColumns`
--

LOCK TABLES `MetaColumns` WRITE;
/*!40000 ALTER TABLE `MetaColumns` DISABLE KEYS */;
INSERT INTO `MetaColumns` VALUES (1,1,'raid_id',1,'INTEGER',1),(2,1,'raid_name',2,'TEXT',0),(3,1,'raid_registrationenddate',3,'SHORT_DATE',0),(11,2,'distance_id',1,'INTEGER',1),(12,2,'raid_id',2,'INTEGER',0),(13,2,'distance_name',3,'TEXT',0),(21,3,'scanpoint_id',1,'INTEGER',1),(22,3,'raid_id',2,'INTEGER',0),(23,3,'scanpoint_name',3,'TEXT',0),(24,3,'scanpoint_order',4,'INTEGER',0),(31,4,'levelpoint_id',1,'INTEGER',1),(32,4,'pointtype_id',2,'INTEGER',0),(33,4,'distance_id',3,'INTEGER',0),(34,4,'levelpoint_name',4,'TEXT',0),(35,4,'levelpoint_order',5,'INTEGER',0),(36,4,'levelpoint_penalty',6,'INTEGER',0),(37,4,'levelpoint_mindatetime',7,'LONG_DATE',0),(38,4,'levelpoint_maxdatetime',8,'LONG_DATE',0),(39,4,'scanpoint_id',9,'INTEGER',0),(51,5,'team_id',1,'INTEGER',1),(52,5,'distance_id',2,'INTEGER',0),(53,5,'team_name',3,'TEXT',0),(54,5,'team_num',4,'INTEGER',0),(61,6,'user_id',1,'INTEGER',1),(62,6,'user_name',2,'TEXT',0),(63,6,'user_birthyear',3,'INTEGER',0),(81,7,'teamuser_id',1,'INTEGER',1),(82,7,'team_id',2,'INTEGER',0),(83,7,'user_id',3,'INTEGER',0),(84,7,'teamuser_hide',4,'INTEGER',0),(91,8,'user_id',1,'INTEGER',1),(92,8,'levelpoint_id',2,'INTEGER',1),(93,8,'team_id',3,'INTEGER',1),(94,8,'teamuser_id',4,'INTEGER',1),(95,8,'teamleveldismiss_date',5,'LONG_DATE',0),(96,8,'device_id',6,'INTEGER',0),(101,9,'user_id',1,'INTEGER',1),(102,9,'levelpoint_id',2,'INTEGER',1),(103,9,'team_id',3,'INTEGER',1),(104,9,'teamlevelpoint_date',4,'LONG_DATE',0),(105,9,'device_id',5,'INTEGER',0),(106,9,'teamlevelpoint_datetime',6,'LONG_DATE',0),(107,9,'teamlevelpoint_points',7,'TEXT',0),(108,9,'teamlevelpoint_comment',8,'TEXT',0),(131,11,'levelpointdiscount_id',1,'INTEGER',1),(132,11,'distance_id',2,'INTEGER',0),(133,11,'levelpointdiscount_value',3,'INTEGER',0),(134,11,'levelpointdiscount_start',4,'INTEGER',0),(135,11,'levelpointdiscount_finish',5,'INTEGER',0),(141,12,'user_id',1,'INTEGER',0),(142,12,'device_id',2,'INTEGER',0),(143,12,'logger_id',3,'INTEGER',1),(144,12,'scanpoint_id',4,'INTEGER',1),(145,12,'team_id',5,'INTEGER',1),(146,12,'rawloggerdata_date',6,'LONG_DATE',0),(151,13,'user_id',1,'INTEGER',1),(152,13,'device_id',2,'INTEGER',0),(153,13,'scanpoint_id',3,'INTEGER',1),(154,13,'team_id',4,'INTEGER',1),(155,13,'rawteamlevelpoints_points',5,'TEXT',0),(156,13,'rawteamlevelpoints_date',6,'LONG_DATE',0),(161,14,'user_id',1,'INTEGER',1),(162,14,'device_id',2,'INTEGER',0),(163,14,'scanpoint_id',3,'INTEGER',1),(164,14,'team_id',4,'INTEGER',1),(165,14,'teamuser_id',5,'INTEGER',1),(166,14,'rawteamleveldismiss_date',6,'LONG_DATE',0);
/*!40000 ALTER TABLE `MetaColumns` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `MetaTables`
--

DROP TABLE IF EXISTS `MetaTables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `MetaTables` (
  `table_id` int(11) NOT NULL,
  `table_name` varchar(50) NOT NULL,
  `update_date_column_name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`table_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `MetaTables`
--

LOCK TABLES `MetaTables` WRITE;
/*!40000 ALTER TABLE `MetaTables` DISABLE KEYS */;
INSERT INTO `MetaTables` VALUES (1,'Raids',NULL),(2,'Distances',NULL),(3,'ScanPoints',NULL),(4,'LevelPoints',NULL),(5,'Teams',NULL),(6,'Users',NULL),(7,'TeamUsers',NULL),(8,'TeamLevelDismiss',NULL),(9,'TeamLevelPoints',NULL),(11,'LevelPointDiscounts',NULL),(12,'RawLoggerData',NULL),(13,'RawTeamLevelPoints',NULL),(14,'RawTeamLevelDismiss',NULL);
/*!40000 ALTER TABLE `MetaTables` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Raids`
--

DROP TABLE IF EXISTS `Raids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Raids` (
  `raid_id` int(11) NOT NULL,
  `raid_registrationenddate` varchar(20) NOT NULL,
  `raid_name` varchar(50) NOT NULL,
  PRIMARY KEY (`raid_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Raids`
--

LOCK TABLES `Raids` WRITE;
/*!40000 ALTER TABLE `Raids` DISABLE KEYS */;
/*!40000 ALTER TABLE `Raids` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `RawLoggerData`
--

DROP TABLE IF EXISTS `RawLoggerData`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RawLoggerData` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `logger_id` int(11) NOT NULL,
  `scanpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `rawloggerdata_date` varchar(20) NOT NULL,
  PRIMARY KEY (`logger_id`,`scanpoint_id`,`team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RawLoggerData`
--

LOCK TABLES `RawLoggerData` WRITE;
/*!40000 ALTER TABLE `RawLoggerData` DISABLE KEYS */;
/*!40000 ALTER TABLE `RawLoggerData` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `RawTeamLevelDismiss`
--

DROP TABLE IF EXISTS `RawTeamLevelDismiss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RawTeamLevelDismiss` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `scanpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `teamuser_id` int(11) NOT NULL,
  `rawteamleveldismiss_date` varchar(20) NOT NULL,
  PRIMARY KEY (`user_id`,`scanpoint_id`,`team_id`,`teamuser_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RawTeamLevelDismiss`
--

LOCK TABLES `RawTeamLevelDismiss` WRITE;
/*!40000 ALTER TABLE `RawTeamLevelDismiss` DISABLE KEYS */;
/*!40000 ALTER TABLE `RawTeamLevelDismiss` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `RawTeamLevelPoints`
--

DROP TABLE IF EXISTS `RawTeamLevelPoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RawTeamLevelPoints` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `scanpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `rawteamlevelpoints_points` varchar(150) DEFAULT NULL,
  `rawteamlevelpoints_date` varchar(20) NOT NULL,
  PRIMARY KEY (`user_id`,`scanpoint_id`,`team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RawTeamLevelPoints`
--

LOCK TABLES `RawTeamLevelPoints` WRITE;
/*!40000 ALTER TABLE `RawTeamLevelPoints` DISABLE KEYS */;
/*!40000 ALTER TABLE `RawTeamLevelPoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ScanPoints`
--

DROP TABLE IF EXISTS `ScanPoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ScanPoints` (
  `scanpoint_id` int(11) NOT NULL,
  `raid_id` int(11) NOT NULL,
  `scanpoint_name` varchar(50) NOT NULL,
  `scanpoint_order` int(11) NOT NULL,
  PRIMARY KEY (`scanpoint_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ScanPoints`
--

LOCK TABLES `ScanPoints` WRITE;
/*!40000 ALTER TABLE `ScanPoints` DISABLE KEYS */;
/*!40000 ALTER TABLE `ScanPoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TeamLevelDismiss`
--

DROP TABLE IF EXISTS `TeamLevelDismiss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TeamLevelDismiss` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `levelpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `teamuser_id` int(11) NOT NULL,
  `teamleveldismiss_date` varchar(20) NOT NULL,
  PRIMARY KEY (`user_id`,`levelpoint_id`,`team_id`,`teamuser_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TeamLevelDismiss`
--

LOCK TABLES `TeamLevelDismiss` WRITE;
/*!40000 ALTER TABLE `TeamLevelDismiss` DISABLE KEYS */;
/*!40000 ALTER TABLE `TeamLevelDismiss` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TeamLevelPoints`
--

DROP TABLE IF EXISTS `TeamLevelPoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TeamLevelPoints` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `levelpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `teamlevelpoint_datetime` varchar(20) NOT NULL,
  `teamlevelpoint_points` varchar(150) DEFAULT NULL,
  `teamlevelpoint_comment` varchar(100) DEFAULT NULL,
  `teamlevelpoint_date` varchar(20) NOT NULL,
  PRIMARY KEY (`user_id`,`levelpoint_id`,`team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TeamLevelPoints`
--

LOCK TABLES `TeamLevelPoints` WRITE;
/*!40000 ALTER TABLE `TeamLevelPoints` DISABLE KEYS */;
/*!40000 ALTER TABLE `TeamLevelPoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TeamUsers`
--

DROP TABLE IF EXISTS `TeamUsers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TeamUsers` (
  `teamuser_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `teamuser_hide` int(11) NOT NULL,
  PRIMARY KEY (`teamuser_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TeamUsers`
--

LOCK TABLES `TeamUsers` WRITE;
/*!40000 ALTER TABLE `TeamUsers` DISABLE KEYS */;
/*!40000 ALTER TABLE `TeamUsers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Teams`
--

DROP TABLE IF EXISTS `Teams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Teams` (
  `team_id` int(11) NOT NULL,
  `distance_id` int(11) NOT NULL,
  `team_name` varchar(50) NOT NULL,
  `team_num` int(11) NOT NULL,
  PRIMARY KEY (`team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Teams`
--

LOCK TABLES `Teams` WRITE;
/*!40000 ALTER TABLE `Teams` DISABLE KEYS */;
/*!40000 ALTER TABLE `Teams` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Users`
--

DROP TABLE IF EXISTS `Users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Users` (
  `user_id` int(11) NOT NULL,
  `user_name` varchar(100) NOT NULL,
  `user_birthyear` int(11) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Users`
--

LOCK TABLES `Users` WRITE;
/*!40000 ALTER TABLE `Users` DISABLE KEYS */;
/*!40000 ALTER TABLE `Users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-04-16 16:43:17
