DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `usp_editUser`(
	inout _executionUid bigint(20) unsigned
,	inout _sessionUid nvarchar(36)
,	in _userId int(11)
,	in _userEmail varchar(60) 
,	in _userName varchar(100) 
,	in _userBirthYear int(11) 
,	in _userCity varchar(50)
)
begin
    declare userId int(11);
	declare errNo varchar(150);

    declare exit handler for sqlexception
	begin
		get diagnostics condition 1 
		@sqlErrorState = returned_sqlstate, @sqlErrorMessage = message_text;
        call usp_addLog(_executionUid, 'error', null, null, null, @sqlErrorMessage, _sessionUid);
		RESIGNAL; 
	end;

   
    set _executionUid  = fn_getUid(_executionUid);
    call usp_addLog(_executionUid, 'debug', null, null, null, 'start usp_editUser', _sessionUid);
   
    if (fn_canEditUser(_sessionUid, _userId)) then 
		update Users
			set 	user_email = coalesce(_userEmail, user_email)
				,	user_name = coalesce(_userName, user_name)
                ,	user_birthyear = coalesce(_userBirthYear, user_birthyear)
                ,	user_city = coalesce(_userCity, user_city)
		where user_id = _userId;    
    else
		call usp_addLog(_executionUid, 'info', _userId, 'check rights', 'You have no permission or record doesn''t exists', _sessionUid);
    end if;

	call usp_addLog(_executionUid, 'debug', null, null, null, 'finish usp_editUser', _sessionUid);
end$$
DELIMITER ;
