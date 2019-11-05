DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `usp_loginUser`(
	inout _executionUid bigint(20) unsigned
,	inout _sessionUid nvarchar(36)
,	in _userEmail varchar(60)
,	in _userPassword varchar(36)
)
begin
    declare userId int(11);
    declare userPassword varchar(36);
    declare resultsNumber int(11);
	declare errNo varchar(150);

    declare exit handler for sqlexception
	begin
		get diagnostics condition 1 
		@sqlErrorState = returned_sqlstate, @sqlErrorMessage = message_text;
        call usp_addLog(_executionUid, 'error', null, null, null, @sqlErrorMessage, _sessionUid);
		RESIGNAL; 
	end;

    /*
		if login correct - start new session
    */
   
    set _executionUid  = fn_getUid(_executionUid);
    call usp_addLog(_executionUid, 'debug', null, null, null, 'start usp_loginUser', _sessionUid);
   
    if (fn_canLoginUser(_sessionUid)) then
		set userId = null;
		set userPassword = null;
		
		select  	max(user_id)
				,	max(user_password)
				,	count(*)
		into userId, userPassword, resultsNumber 
		from Users
		where trim(lower(user_email)) = trim(lower(_userEmail))
		limit 1;
		
		if (resultsNumber = 1 and userPassword  = _userPassword) then
			set _sessionUid =  fn_startSession(userId);
		end if;
    end if;
	call usp_addLog(_executionUid, 'debug', null, null, null, 'finish usp_loginUser', _sessionUid);
end$$
DELIMITER ;
