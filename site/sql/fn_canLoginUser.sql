DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_canLoginUser`(_sessionId varchar(50)) RETURNS bit(1)
BEGIN
	RETURN if(isnull(_sessionId), 1, if(trim(_sessionId) = '', 1, if(ifnull(fn_getUserId(_sessionId), 0) = 0, 1,0)));
END$$
DELIMITER ;
