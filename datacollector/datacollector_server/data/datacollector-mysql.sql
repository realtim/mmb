CREATE TABLE `datacollector`.`Raids` (
  `raid_id` int(11) NOT NULL,
  `raid_registrationenddate` varchar(20) NOT NULL,
  `raid_name` varchar(50) NOT NULL,
  PRIMARY KEY (`raid_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`Distances` (
  `distance_id` int(11) NOT NULL,
  `raid_id` int(11) NOT NULL,
  `distance_name` varchar(50) NOT NULL,
  PRIMARY KEY (`distance_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`ScanPoints` (
  `scanpoint_id` int(11) NOT NULL,
  `raid_id` int(11) NOT NULL,
  `scanpoint_name` varchar(50) NOT NULL,
  `scanpoint_order` int(11) NOT NULL,
  PRIMARY KEY (`scanpoint_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`LevelPoints` (
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

CREATE TABLE `datacollector`.`LevelPointDiscounts` (
  `levelpointdiscount_id` int(11) NOT NULL,
  `distance_id` int(11) NOT NULL,
  `levelpointdiscount_value` int(11) NOT NULL,
  `levelpointdiscount_start` int(11) NOT NULL,
  `levelpointdiscount_finish` int(11) NOT NULL,
  PRIMARY KEY (`levelpointdiscount_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`Teams` (
  `team_id` int(11) NOT NULL,
  `distance_id` int(11) NOT NULL,
  `team_name` varchar(50) NOT NULL,
  `team_num` int(11) NOT NULL,
  PRIMARY KEY (`team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`Users` (
  `user_id` int(11) NOT NULL,
  `user_name` varchar(100) NOT NULL,
  `user_birthyear` int(11) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`TeamUsers` (
  `teamuser_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `teamuser_hide` int(11) NOT NULL,
  PRIMARY KEY (`teamuser_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`TeamLevelDismiss` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `levelpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `teamuser_id` int(11) NOT NULL,
  `teamleveldismiss_date` varchar(20) NOT NULL,
  PRIMARY KEY (`user_id`, `levelpoint_id`, `team_id`, `teamuser_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`TeamLevelPoints` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `levelpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `teamlevelpoint_datetime` varchar(20) NOT NULL,
  `teamlevelpoint_points` varchar(150) DEFAULT NULL,
  `teamlevelpoint_comment` varchar(100) DEFAULT NULL,
  `teamlevelpoint_date` varchar(20) NOT NULL,
  PRIMARY KEY (`user_id`, `levelpoint_id`, `team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`RawLoggerData` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,
  `logger_id` int(11) NOT NULL,
  `scanpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `rawloggerdata_date` varchar(20) NOT NULL,
  PRIMARY KEY (`logger_id`, `scanpoint_id`, `team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`RawTeamLevelPoints` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,  
  `scanpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `rawteamlevelpoints_points` varchar(150) DEFAULT NULL,
  `rawteamlevelpoints_date` varchar(20) NOT NULL,
  PRIMARY KEY (`user_id`, `scanpoint_id`, `team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`RawTeamLevelDismiss` (
  `user_id` int(11) NOT NULL,
  `device_id` int(11) NOT NULL,  
  `scanpoint_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `teamuser_id` int(11) NOT NULL,
  `rawteamleveldismiss_date` varchar(20) NOT NULL,
  PRIMARY KEY (`user_id`, `scanpoint_id`, `team_id`, `teamuser_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`MetaTables` (
  `table_id` int(11) NOT NULL,
  `table_name` varchar(50) NOT NULL,
  `update_date_column_name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`table_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `datacollector`.`MetaColumns` (
  `column_id` int(11) NOT NULL,
  `table_id` int(11) NOT NULL,
  `column_name` varchar(50) NOT NULL,
  `column_order` int(11) NOT NULL,
  `column_data_type` varchar(50) NOT NULL,
  `is_primary_key` int(11) NOT NULL,
  PRIMARY KEY (`column_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

INSERT INTO MetaTables VALUES(1,'Raids',NULL);
INSERT INTO MetaTables VALUES(2,'Distances',NULL);
INSERT INTO MetaTables VALUES(3,'ScanPoints',NULL);
INSERT INTO MetaTables VALUES(4,'LevelPoints',NULL);
INSERT INTO MetaTables VALUES(5,'Teams',NULL);
INSERT INTO MetaTables VALUES(6,'Users',NULL);
INSERT INTO MetaTables VALUES(7,'TeamUsers',NULL);
INSERT INTO MetaTables VALUES(8,'TeamLevelDismiss',NULL);
INSERT INTO MetaTables VALUES(9,'TeamLevelPoints',NULL);
INSERT INTO MetaTables VALUES(11,'LevelPointDiscounts',NULL);
INSERT INTO MetaTables VALUES(12,'RawLoggerData',NULL);
INSERT INTO MetaTables VALUES(13,'RawTeamLevelPoints',NULL);
INSERT INTO MetaTables VALUES(14,'RawTeamLevelDismiss',NULL);

/* raids */
INSERT INTO MetaColumns VALUES(1, 1, 'raid_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(2, 1, 'raid_name', 2, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(3, 1, 'raid_registrationenddate', 3, 'SHORT_DATE', 0);
/* distances */
INSERT INTO MetaColumns VALUES(11, 2, 'distance_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(12, 2, 'raid_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(13, 2, 'distance_name', 3, 'TEXT', 0);
/* scanpoints */
INSERT INTO MetaColumns VALUES(21, 3, 'scanpoint_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(22, 3, 'raid_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(23, 3, 'scanpoint_name', 3, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(24, 3, 'scanpoint_order', 4, 'INTEGER', 0);
/* levelpoints */
INSERT INTO MetaColumns VALUES(31, 4, 'levelpoint_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(32, 4, 'pointtype_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(33, 4, 'distance_id', 3, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(34, 4, 'levelpoint_name', 4, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(35, 4, 'levelpoint_order', 5, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(36, 4, 'levelpoint_penalty', 6, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(37, 4, 'levelpoint_mindatetime', 7, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(38, 4, 'levelpoint_maxdatetime', 8, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(39, 4, 'scanpoint_id', 9, 'INTEGER', 0);
/* teams */
INSERT INTO MetaColumns VALUES(51, 5, 'team_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(52, 5, 'distance_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(53, 5, 'team_name', 3, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(54, 5, 'team_num', 4, 'INTEGER', 0);
/* users */
INSERT INTO MetaColumns VALUES(61, 6, 'user_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(62, 6, 'user_name', 2, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(63, 6, 'user_birthyear', 3, 'INTEGER', 0);
/* teamusers */
INSERT INTO MetaColumns VALUES(81, 7, 'teamuser_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(82, 7, 'team_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(83, 7, 'user_id', 3, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(84, 7, 'teamuser_hide', 4, 'INTEGER', 0);
/* teamleveldismiss */
INSERT INTO MetaColumns VALUES(91, 8, 'user_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(92, 8, 'levelpoint_id', 2, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(93, 8, 'team_id', 3, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(94, 8, 'teamuser_id', 4, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(95, 8, 'teamleveldismiss_date', 5, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(96, 8, 'device_id', 6, 'INTEGER', 0);
/* teamlevelpoints */
INSERT INTO MetaColumns VALUES(101, 9, 'user_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(102, 9, 'levelpoint_id', 2, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(103, 9, 'team_id', 3, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(104, 9, 'teamlevelpoint_date', 4, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(105, 9, 'device_id', 5, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(106, 9, 'teamlevelpoint_datetime', 6, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(107, 9, 'teamlevelpoint_points', 7, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(108, 9, 'teamlevelpoint_comment', 8, 'TEXT', 0);
/* levelpointdiscounts */
INSERT INTO MetaColumns VALUES(131, 11, 'levelpointdiscount_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(132, 11, 'distance_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(133, 11, 'levelpointdiscount_value', 3, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(134, 11, 'levelpointdiscount_start', 4, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(135, 11, 'levelpointdiscount_finish', 5, 'INTEGER', 0);
/* rawloggerdata */
INSERT INTO MetaColumns VALUES(141, 12, 'user_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(142, 12, 'device_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(143, 12, 'logger_id', 3, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(144, 12, 'scanpoint_id', 4, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(145, 12, 'team_id', 5, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(146, 12, 'rawloggerdata_date', 6, 'LONG_DATE', 0);
/* rawteamlevelpoints */
INSERT INTO MetaColumns VALUES(151, 13, 'user_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(152, 13, 'device_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(153, 13, 'scanpoint_id', 3, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(154, 13, 'team_id', 4, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(155, 13, 'rawteamlevelpoints_points', 5, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(156, 13, 'rawteamlevelpoints_date', 6, 'LONG_DATE', 0);
/* rawteamleveldismiss */
INSERT INTO MetaColumns VALUES(161, 14, 'user_id', 1, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(162, 14, 'device_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(163, 14, 'scanpoint_id', 3, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(164, 14, 'team_id', 4, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(165, 14, 'teamuser_id', 5, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(166, 14, 'rawteamleveldismiss_date', 6, 'LONG_DATE', 0);