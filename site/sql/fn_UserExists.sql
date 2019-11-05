DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_UserExists`(
	_userId int(11)
) RETURNS bit(1)
BEGIN
	if exists(select * from Users where user_id = _UserId) then
		return 1;
	end if;
	return 0;
END$$
DELIMITER ;
