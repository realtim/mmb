DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_canLogoutUser`(_sessionId varchar(36)) RETURNS bit(1)
BEGIN
	RETURN if(isnull(_sessionId), 0, if(trim(_sessionId) = '', 0, if(ifnull(fn_getUserId(_sessionId), 0) = 0, 0, 1)));
END$$
DELIMITER ;
