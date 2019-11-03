DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `usp_getUser`(
	inout _executionUid bigint(20) unsigned
,	inout _sessionUid nvarchar(36)
,	in _userId int(11)
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
    call usp_addLog(_executionUid, 'debug', null, null, null, 'start usp_getUser', _sessionUid);
   
    if (fn_canEditUser(_sessionUid, _userId)) then 
		select 		
					user_id
				,	user_email
				,	user_name
                ,	user_birthyear
                ,	user_city
		from Users
        where user_id = _userId;    
    else
		call usp_addLog(_executionUid, 'info', null, null, 'check rights', 'You have no permission or record doesn''t exists', _sessionUid);
    end if;

	call usp_addLog(_executionUid, 'debug', null, null, null, 'finish usp_getUser', _sessionUid);
end$$
DELIMITER ;
