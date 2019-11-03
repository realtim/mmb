DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `usp_logoutUser`(
	inout _executionUid bigint(20) unsigned
,	inout _sessionUid varchar(36)
)
begin
    declare exit handler for sqlexception
	begin
		get diagnostics condition 1 
		@sqlErrorState = returned_sqlstate, @sqlErrorMessage = message_text;
        call usp_addLog(_executionUid, 'error', null, null, null, @sqlErrorMessage, _sessionUid);
		RESIGNAL; 
	end;

	set _executionUid  = fn_getUid(_executionUid);

    call usp_addLog(_executionUid, 'debug', null, null, null, 'start usp_logoutUser', _sessionUid);
 
	if (fn_isSessionValid(_sessionUid)) then
		update Sessions 
			set session_status = 3 
		where session_id = _sessionUid;
        
        set _sessionUid = null;
    end if;
	call usp_addLog(_executionUid, 'debug', null, null, null, 'finish usp_logoutUser', _sessionUid);
end$$
DELIMITER ;
