DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_isSessionValid`(
	_sessionUid varchar(36)
) RETURNS bit(1)
BEGIN
	if exists(select * from Sessions where session_id = _sessionUid and session_status = 0)  then
		return 1;
    end if;
RETURN 0;
END$$
DELIMITER ;
