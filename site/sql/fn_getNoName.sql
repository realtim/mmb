DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_getNoName`(_userName varchar(64)) RETURNS varchar(64) CHARSET utf8
BEGIN
	RETURN 'Пользователь скрыл данные';
END$$
DELIMITER ;