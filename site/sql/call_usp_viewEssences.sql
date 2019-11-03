set  @executionUid = null;
set  @sessionUid = null;
CALL usp_viewEssences(@executionUid, @sessionUid);
select @executionUid, @sessionUid;
call usp_viewLogs(@executionUid, null);