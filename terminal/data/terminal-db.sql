BEGIN TRANSACTION;
CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
INSERT INTO android_metadata VALUES('en_US');
CREATE TABLE input_data (_id integer PRIMARY KEY, lap_id integer, input_mode text, team_id integer, check_time text, taken_checkpoints text, dirty integer, user_id integer, creation_time text);
CREATE TABLE sqlite_sequence(name,seq);
CREATE TABLE withdraw (_id integer PRIMARY KEY, lap_id integer, team_id integer, participant_id integer, dirty integer, user_id integer, creation_time text);
COMMIT;
