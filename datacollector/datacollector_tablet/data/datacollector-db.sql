BEGIN TRANSACTION;

CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
INSERT INTO android_metadata VALUES('en_US');

CREATE TABLE Raids (raid_registrationenddate TEXT, raid_id INTEGER NOT NULL, raid_name TEXT NOT NULL, PRIMARY KEY (raid_id));

CREATE TABLE Distances (distance_id INTEGER NOT NULL, distance_name TEXT NOT NULL, 
raid_id INTEGER NOT NULL, PRIMARY KEY (distance_id));

CREATE INDEX IDXDST_raid_id ON Distances (raid_id);

CREATE TABLE ScanPoints (scanpoint_id INTEGER NOT NULL, raid_id INTEGER NOT NULL, scanpoint_name TEXT NOT NULL, 
scanpoint_order INTEGER NOT NULL, PRIMARY KEY (scanpoint_id));

CREATE INDEX IDXSCP_raid_id ON ScanPoints (raid_id);

CREATE TABLE LevelPoints (levelpoint_id INTEGER NOT NULL, pointtype_id INTEGER NOT NULL, distance_id INTEGER NOT NULL,
levelpoint_order INTEGER NOT NULL, levelpoint_name TEXT NOT NULL, levelpoint_penalty INTEGER, levelpoint_mindatetime TEXT, 
levelpoint_maxdatetime TEXT, scanpoint_id INTEGER, PRIMARY KEY (levelpoint_id));

CREATE INDEX IDXLP_distance_id ON LevelPoints (distance_id);
CREATE INDEX IDXLP_scanpoint_id ON LevelPoints (scanpoint_id);

CREATE TABLE LevelPointDiscounts (levelpointdiscount_id INTEGER NOT NULL, 
distance_id INTEGER NOT NULL, levelpointdiscount_value INTEGER, levelpointdiscount_start INTEGER, levelpointdiscount_finish INTEGER, 
PRIMARY KEY (levelpointdiscount_id));

CREATE INDEX IDXLPD_distance_id ON LevelPointDiscounts (distance_id);

CREATE TABLE Teams (team_id INTEGER NOT NULL, team_name TEXT NOT NULL, distance_id INTEGER NOT NULL, 
team_num INTEGER NOT NULL, PRIMARY KEY (team_id));

CREATE TABLE Users (user_id INTEGER NOT NULL, user_name TEXT NOT NULL, 
user_birthyear INTEGER, PRIMARY KEY (user_id));

CREATE TABLE TeamUsers (teamuser_id INTEGER NOT NULL, user_id INTEGER NOT NULL, team_id INTEGER NOT NULL, 
teamuser_hide INTEGER, PRIMARY KEY (teamuser_id));

CREATE INDEX IDXTU_team_id ON TeamUsers (team_id);
CREATE INDEX IDXTU_user_id ON TeamUsers (user_id);

-- rawloggerdata_date - время последнего изменения
-- scanned_date - время отметки на контрольной точке
CREATE TABLE RawLoggerData (user_id INTEGER NOT NULL, device_id INTEGER NOT NULL, logger_id INTEGER NOT NULL, 
scanpoint_id INTEGER NOT NULL, team_id INTEGER NOT NULL, rawloggerdata_date TEXT NOT NULL, scanned_date TEXT NOT NULL,
changed_manual INTEGER NOT NULL DEFAULT 0,
PRIMARY KEY (logger_id, scanpoint_id, team_id));

CREATE TABLE RawTeamLevelPoints (user_id INTEGER NOT NULL, device_id INTEGER NOT NULL, scanpoint_id INTEGER NOT NULL, 
team_id INTEGER NOT NULL, rawteamlevelpoints_points TEXT, rawteamlevelpoints_date TEXT NOT NULL,
PRIMARY KEY (user_id, scanpoint_id, team_id));

CREATE TABLE RawTeamLevelDismiss (user_id INTEGER NOT NULL, device_id INTEGER NOT NULL, scanpoint_id INTEGER NOT NULL,
team_id INTEGER NOT NULL, teamuser_id INTEGER NOT NULL, rawteamleveldismiss_date TEXT NOT NULL,
PRIMARY KEY (user_id, scanpoint_id, team_id, teamuser_id));


CREATE TABLE MetaTables (table_id INTEGER NOT NULL, table_name TEXT NOT NULL, PRIMARY KEY (table_id));

CREATE TABLE MetaColumns (column_id INTEGER NOT NULL, table_id INTEGER NOT NULL, column_name TEXT NOT NULL,
column_order INTEGER NOT NULL, column_data_type TEXT NOT NULL, 
is_primary_key INTEGER DEFAULT 0 NOT NULL, PRIMARY KEY (column_id));

CREATE INDEX IDXMCOL_table_id ON MetaColumns (table_id);



INSERT INTO MetaTables VALUES(1,'Raids');
INSERT INTO MetaTables VALUES(2,'Distances');
INSERT INTO MetaTables VALUES(3,'ScanPoints');
INSERT INTO MetaTables VALUES(4,'LevelPoints');
INSERT INTO MetaTables VALUES(5,'Teams');
INSERT INTO MetaTables VALUES(6,'Users');
INSERT INTO MetaTables VALUES(7,'TeamUsers');
INSERT INTO MetaTables VALUES(11,'LevelPointDiscounts');
INSERT INTO MetaTables VALUES(12,'RawLoggerData');
INSERT INTO MetaTables VALUES(13,'RawTeamLevelPoints');
INSERT INTO MetaTables VALUES(14,'RawTeamLevelDismiss');

/* raids */
INSERT INTO MetaColumns VALUES(1, 1, 'raid_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(2, 1, 'raid_name', 1, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(3, 1, 'raid_registrationenddate', 2, 'SHORT_DATE', 0);
/* distances */
INSERT INTO MetaColumns VALUES(11, 2, 'distance_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(12, 2, 'raid_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(13, 2, 'distance_name', 2, 'TEXT', 0);
/* scanpoints */
INSERT INTO MetaColumns VALUES(21, 3, 'scanpoint_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(22, 3, 'raid_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(23, 3, 'scanpoint_name', 2, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(24, 3, 'scanpoint_order', 3, 'INTEGER', 0);
/* levelpoints */
INSERT INTO MetaColumns VALUES(31, 4, 'levelpoint_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(32, 4, 'pointtype_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(33, 4, 'distance_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(34, 4, 'levelpoint_name', 3, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(35, 4, 'levelpoint_order', 4, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(36, 4, 'levelpoint_penalty', 5, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(37, 4, 'levelpoint_mindatetime', 6, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(38, 4, 'levelpoint_maxdatetime', 7, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(39, 4, 'scanpoint_id', 8, 'INTEGER', 0);
/* teams */
INSERT INTO MetaColumns VALUES(51, 5, 'team_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(52, 5, 'distance_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(53, 5, 'team_name', 2, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(54, 5, 'team_num', 3, 'INTEGER', 0);
/* users */
INSERT INTO MetaColumns VALUES(61, 6, 'user_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(62, 6, 'user_name', 1, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(63, 6, 'user_birthyear', 2, 'INTEGER', 0);
/* teamusers */
INSERT INTO MetaColumns VALUES(81, 7, 'teamuser_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(82, 7, 'team_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(83, 7, 'user_id', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(84, 7, 'teamuser_hide', 3, 'INTEGER', 0);
/* levelpointdiscounts */
INSERT INTO MetaColumns VALUES(131, 11, 'levelpointdiscount_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(132, 11, 'distance_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(133, 11, 'levelpointdiscount_value', 2, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(134, 11, 'levelpointdiscount_start', 3, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(135, 11, 'levelpointdiscount_finish', 4, 'INTEGER', 0);
/* rawloggerdata */
INSERT INTO MetaColumns VALUES(141, 12, 'user_id', 0, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(142, 12, 'device_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(143, 12, 'logger_id', 2, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(144, 12, 'scanpoint_id', 3, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(145, 12, 'team_id', 4, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(146, 12, 'rawloggerdata_date', 5, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(147, 12, 'scanned_date', 6, 'LONG_DATE', 0);
INSERT INTO MetaColumns VALUES(148, 12, 'changed_manual', 7, 'INTEGER', 0);
/* rawteamlevelpoints */
INSERT INTO MetaColumns VALUES(151, 13, 'user_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(152, 13, 'device_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(153, 13, 'scanpoint_id', 2, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(154, 13, 'team_id', 3, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(155, 13, 'rawteamlevelpoints_points', 4, 'TEXT', 0);
INSERT INTO MetaColumns VALUES(156, 13, 'rawteamlevelpoints_date', 5, 'LONG_DATE', 0);
/* rawteamleveldismiss */
INSERT INTO MetaColumns VALUES(161, 14, 'user_id', 0, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(162, 14, 'device_id', 1, 'INTEGER', 0);
INSERT INTO MetaColumns VALUES(163, 14, 'scanpoint_id', 2, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(164, 14, 'team_id', 3, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(165, 14, 'teamuser_id', 4, 'INTEGER', 1);
INSERT INTO MetaColumns VALUES(166, 14, 'rawteamleveldismiss_date', 5, 'LONG_DATE', 0);

COMMIT;

