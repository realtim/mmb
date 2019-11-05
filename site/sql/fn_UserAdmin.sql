DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_UserAdmin`(
	_userId int(11)
) RETURNS bit(1)
BEGIN
	if exists(select * from Users where user_id = _UserId and user_admin = 1) then
		return 1;
	end if;
	return 0;
END$$
DELIMITER ;
