set  @executionUid = null;
set  @sessionUid = null;
CALL usp_viewUsers(@executionUid, @sessionUid, null, null, 32, null);
select @executionUid, @sessionUid;
call usp_viewLogs(@executionUid, null);