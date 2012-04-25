BEGIN TRANSACTION;

CREATE TABLE Distances (distance_id INTEGER NOT NULL, distance_name TEXT NOT NULL, 
raid_id INTEGER NOT NULL, PRIMARY KEY (distance_id));

CREATE INDEX IDXDST_raid_id ON Distances (raid_id);

CREATE TABLE LevelPoints (levelpoint_id INTEGER NOT NULL, level_id INTEGER NOT NULL, 
pointtype_id INTEGER NOT NULL, PRIMARY KEY (levelpoint_id));

CREATE INDEX IDXLP_level_id ON LevelPoints (level_id);

CREATE TABLE Levels (level_id INTEGER NOT NULL, level_name TEXT NOT NULL, level_order INTEGER NOT NULL, distance_id INTEGER NOT NULL, 
level_starttype INTEGER NOT NULL, level_pointnames TEXT NOT NULL, level_pointpenalties TEXT NOT NULL, level_begtime TEXT, 
level_maxbegtime TEXT, level_minendtime TEXT, level_endtime TEXT, PRIMARY KEY (level_id));

CREATE INDEX IDXLV_distance_id ON Levels (distance_id);

CREATE TABLE Raids (raid_registrationenddate TEXT, raid_id INTEGER NOT NULL, raid_name TEXT NOT NULL, PRIMARY KEY (raid_id));

CREATE TABLE TeamUsers (teamuser_id INTEGER NOT NULL, user_id INTEGER NOT NULL, team_id INTEGER NOT NULL, 
teamuser_hide INTEGER, PRIMARY KEY (teamuser_id));

CREATE INDEX IDXTU_team_id ON TeamUsers (team_id);
CREATE INDEX IDXTU_user_id ON TeamUsers (user_id);

CREATE TABLE Teams (team_id INTEGER NOT NULL, team_name TEXT NOT NULL, distance_id INTEGER NOT NULL, 
team_num INTEGER NOT NULL, PRIMARY KEY (team_id));

CREATE TABLE Users (user_id INTEGER NOT NULL, user_name TEXT NOT NULL, 
user_birthyear INTEGER, PRIMARY KEY (user_id));

CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
INSERT INTO android_metadata VALUES('en_US');

CREATE TABLE LocalSequence (sequence_id INTEGER, sequence_value INTEGER);
INSERT INTO LocalSequence VALUES(1,1);

CREATE TABLE TeamLevelPoints (teamlevelpoint_date TEXT NOT NULL, user_id INTEGER NOT NULL, 
device_id INTEGER NOT NULL, levelpoint_id INTEGER NOT NULL, team_id INTEGER NOT NULL, teamlevelpoint_datetime TEXT NOT NULL, 
teamlevelpoint_points TEXT, teamlevelpoint_comment TEXT, PRIMARY KEY (user_id, levelpoint_id, team_id));

CREATE INDEX IDXTLP_accelerator_1 ON TeamLevelPoints (levelpoint_id, team_id);

CREATE TABLE TeamLevelDismiss (teamleveldismiss_date TEXT NOT NULL, user_id INTEGER NOT NULL, 
device_id INTEGER NOT NULL, levelpoint_id INTEGER NOT NULL, team_id INTEGER NOT NULL, teamuser_id INTEGER NOT NULL, 
PRIMARY KEY (user_id, levelpoint_id, team_id, teamuser_id));

CREATE INDEX IDXTLD_team_id ON TeamLevelDismiss (team_id);

COMMIT;

