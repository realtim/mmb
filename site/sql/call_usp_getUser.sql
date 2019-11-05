set  @executionUid = null;
set  @sessionUid = 'aeb99e9d-f43a-11e9-9666-ed80b7164828';
CALL usp_getUser(@executionUid, @sessionUid, 19);
select @executionUid, @sessionUid;
call usp_viewLogs(@executionUid, null);