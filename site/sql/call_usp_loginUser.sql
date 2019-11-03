set  @executionUid = null;
set  @sessionUid = null;
call usp_loginUser(@executionUid, @sessionUid, 'leonidfishkis@gmail.com', '6ceb43331f5fb03ff0797c13f5c22594');
select @executionUid, @sessionUid;
call usp_viewLogs(@executionUid, null);
/*
'aeb99e9d-f43a-11e9-9666-ed80b7164828'
+/