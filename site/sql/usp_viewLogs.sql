DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `usp_viewLogs`(
	in _executionUid bigint(20) unsigned
,   in _sessionId nvarchar(50)
)
begin
	select * 
    from Logs
    where ifnull(logs_executionuid, 0) = ifnull(_executionUid, ifnull(logs_executionuid, 0))
		and ifnull(session_id, '') = ifnull(_sessionId, ifnull(session_id, ''));
end$$
DELIMITER ;
