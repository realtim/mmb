DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_canEditUser`(_sessionUid nvarchar(36), _userId int(11)) RETURNS bit(1)
begin
	declare UserId int(11);
    if (not fn_SessionValid(_sessionUid)) then
		return 0;
    end if;
	set UserId = fn_getUserId(_sessionUid);
    if (UserId = _userId or fn_UserAdmin(UserId)) then
		return 1;
    end if;
	return 0;
end$$
DELIMITER ;
