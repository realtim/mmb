DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_getUUid`(_uid varchar(36)) RETURNS varchar(36) CHARSET utf8
BEGIN
	return IFNULL(_uid, UUID());
END$$
DELIMITER ;
