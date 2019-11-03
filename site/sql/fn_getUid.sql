DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_getUid`(_uid bigint(20) unsigned) RETURNS bigint(20) unsigned
BEGIN
	return IFNULL(_uid, UUID_short());
RETURN 1;
END$$
DELIMITER ;
