DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `usp_addLog`(
	inout _executionUid bigint(20) unsigned
,	in _logLevel enum('critical','error','warning','info','debug','trace') 
,	in _userId int(11) 
,	in _logDuration int(11)
,	in _logOperation tinytext
,	in _logMessage text
,	in _sessionId nvarchar(50)
)
BEGIN
	set _executionUid = fn_getUid(_executionUid);
    
    INSERT INTO Logs
	(
	`logs_level`,
	`user_id`,
	`logs_duration`,
	`logs_operation`,
	`logs_message`,
	`logs_executionuid`,
	`session_id`
	)
	VALUES
	(	_logLevel
    ,	_userId 
    ,	_logDuration
    ,	_logOperation
    ,	_logMessage
    ,	_executionUid
    ,	_sessionId
	);
    
 END$$
DELIMITER ;
