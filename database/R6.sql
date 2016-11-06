//  27 - ключ ММБ



select 
u.user_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN 'Пользователь скрыл свои данные' ELSE u.user_name END as user_name,  
	         COALESCE(a.r6, 0.00) as userrank
 from  Users u
		inner join 
		(select tu.user_id, SUM(COALESCE(tu.teamuser_rank, 0.00)) as rank, 
			SUM(COALESCE(tu.teamuser_rank, 0.00) * POW(0.9, 27 - d.raid_id)) as r6
	        from TeamUsers tu 
			inner join Teams t
			on tu.team_id = t.team_id	
			inner join Distances d
	        	on t.distance_id = d.distance_id
			left outer join 
			(
		 	select tld.teamuser_id,  MIN(lp.levelpoint_order) as minorder
		 	from TeamLevelDismiss tld
			 	inner join LevelPoints lp
			 	on tld.levelpoint_id = lp.levelpoint_id
			 group by tld.teamuser_id
                        ) c
			on tu.teamuser_id = c.teamuser_id
		where d.distance_hide = 0 
		       and tu.teamuser_hide = 0
		       and t.team_hide = 0 
		       and  COALESCE(t.team_outofrange, 0) = 0
		       and  COALESCE(t.team_result, 0) > 0
		       and COALESCE(t.team_minlevelpointorderwitherror, 0) = 0
		       and  COALESCE(c.minorder, 0) = 0
		       and  d.raid_id <= 27
		group by tu.user_id
 		) a
		on a.user_id = u.user_id
		