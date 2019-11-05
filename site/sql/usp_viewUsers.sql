DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `usp_viewUsers`(
	inout _executionUid bigint(20) unsigned
,	inout _sessionUid nvarchar(36)
,	in _userId int(11)
,	in _teamId int(11)
,	in _raidIdForDevelopers int(11)
,	in _userNamePattern varchar(64)
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
    call usp_addLog(_executionUid, 'debug', null, null, null, 'start usp_viewUsers', _sessionUid);
    
    if (_teamId is not null) then
 
		select  	Users.user_id
				,	case when ifnull(user_noshow, 0) = 1 
						 then fn_getNoName(Users.user_name) 
                         else Users.user_name 
					end
				,	Users.user_birthyear
				,	Users.user_city
		from Users
			inner join TeamUsers
			on Users.user_id = TeamUsers.user_id
		where ifnull(TeamUsers.teamuser_hide, 0) = 0
			and	TeamUsers.team_id = ifnull(_teamId, TeamUsers.team_id)
		order by 2;
	 
    elseif (_raidIdForDevelopers is not null) then 

		select  	Users.user_id
				,	case when ifnull(user_noshow, 0) = 1 
						 then fn_getNoName(Users.user_name) 
                         else Users.user_name 
					end
				,	Users.user_birthyear
				,	Users.user_city
		from Users
			inner join RaidDevelopers
			on Users.user_id = RaidDevelopers.user_id
		where ifnull(RaidDevelopers.raiddeveloper_hide, 0) = 0
			and	RaidDevelopers.raid_id = ifnull(_raidIdForDevelopers, RaidDevelopers.raid_id)
		order by 2;

    else
		select  	Users.user_id
				,	case when ifnull(user_noshow, 0) = 1 
						 then fn_getNoName(Users.user_name) 
                         else Users.user_name 
					end
				,	Users.user_birthyear
				,	Users.user_city
		from Users
		where 	Users.user_id = ifnull(_userId, Users.user_id)
			and	Users.user_name like ifnull(_userNamePattern, Users.user_name)
		order by 2;

    end if;
   	
 	call usp_addLog(_executionUid, 'debug', null, null, null, 'finish usp_lviewUsers', _sessionUid);
end$$
DELIMITER ;
