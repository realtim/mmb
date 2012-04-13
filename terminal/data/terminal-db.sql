CREATE TABLE Distances (distance_id INTEGER, distance_name TEXT, raid_id INTEGER);

CREATE TABLE LevelPoints (levelpoint_id INTEGER, level_id INTEGER, pointtype_id INTEGER);

CREATE TABLE Levels (level_id INTEGER, level_name TEXT, level_order INTEGER, distance_id INTEGER, 
level_starttype INTEGER, level_pointnames TEXT, level_pointpenalties TEXT, level_begtime TEXT, 
level_maxbegtime TEXT, level_minendtime TEXT, level_endtime TEXT);

CREATE TABLE Raids (raid_registrationenddate TEXT, raid_id INTEGER, raid_name TEXT);

CREATE TABLE TeamUsers (teamuser_id INTEGER, user_id INTEGER, team_id INTEGER, teamuser_hide INTEGER);

CREATE TABLE Teams (team_id INTEGER, team_name TEXT, distance_id INTEGER, team_num INTEGER);

CREATE TABLE Users (user_id INTEGER, user_name TEXT, user_birthyear INTEGER);

