set  @executionUid = null;
set  @sessionUid = 'aeb99e9d-f43a-11e9-9666-ed80b7164828';
CALL usp_editUser(@executionUid, @sessionUid, 30, 'ttt@ttt.com', 'Казанцев Владимир', 1986, 'Железнодорожный');
select @executionUid, @sessionUid;
call usp_viewLogs(@executionUid, null);
