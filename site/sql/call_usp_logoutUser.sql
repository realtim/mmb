set  @executionUid = null;
set  @sessionUid = null;
CALL usp_loginUser(@executionUid, @sessionUid, 'leonidfishkis@gmail.com', '6ceb43331f5fb03ff0797c13f5c22594');
select @executionUid, @sessionUid;
call usp_viewLogs(@executionUid, null);