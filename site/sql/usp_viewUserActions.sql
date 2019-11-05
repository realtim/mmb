DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `usp_viewUserActions`(
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
    call usp_addLog(_executionUid, 'debug', null, null, null, 'start usp_viewUserActions', _sessionUid);
   
	select EssenceAction_Code from EssenceActions where EssenceAction_Code = if(fn_SessionValid(_sessionUid), 'UserLogout','UserLogin')
    union
	select EssenceAction_Code from EssenceActions where EssenceAction_Code in ('UserAdd') and not fn_SessionValid(_sessionUid)
    union
    select EssenceAction_Code from EssenceActions where EssenceAction_Code in ('UserEdit','UserGetProfile') and fn_canEditUser(_sessionUid, _userId);
    
	call usp_addLog(_executionUid, 'debug', null, null, null, 'finish usp_viewUserActions', _sessionUid);
end$$
DELIMITER ;
