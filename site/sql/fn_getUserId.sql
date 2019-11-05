DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_getUserId`(_sessionUid varchar(36)) RETURNS int(11)
BEGIN
	RETURN (select user_id
			from Sessions
			where session_id = _sessionUid
			and session_status = 0
			limit 0,1
			);
END$$
DELIMITER ;
