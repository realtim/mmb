DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_startSession`(
	_userId int(11)
) RETURNS varchar(36) CHARSET utf8
BEGIN
	declare sessionUid varchar(36);
    declare nowDT datetime;
    set sessionUid = fn_getUUid(null);
    set nowDT = now();
    
    if (ifnull(sessionUid, '') = '') then
		return null;
    end if ;
    
    if (not fn_UserExists(_userId)) then
		return null;
    end if;
           
    insert into Sessions
    (
		`session_id`
	,	`user_id`
	,	`connection_id`
	,	`session_status`
	,	`session_starttime`
	,	`session_updatetime`
    )
    values
    (
		sessionUid
	,	_userId
    ,	0
    ,	0
    ,	nowDT
	,	nowDT
    );
    
    return sessionUid;
END$$
DELIMITER ;
