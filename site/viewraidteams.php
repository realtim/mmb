<?php
// +++++++++++ Показ списка команд марш-броска ++++++++++++++++++++++++++++++++

	// Выходим, если файл был запрошен напрямую, а не через include
	if (!isset($MyPHPScript)) return;
?>

<script language = "JavaScript">

    // Смена сортировки
	function OrderTypeChange()
	{ 
	    document.RaidTeamsForm.action.value = "ViewRaidTeams";
	    document.RaidTeamsForm.submit();
	  
	}

	// Фильтр по дистанции
	function DistanceIdChange()
	{ 
		document.RaidTeamsForm.action.value = "ViewRaidTeams";
		document.RaidTeamsForm.submit();
	}

	// Фильтр по точке
	function LevelPointIdChange()
	{ 
    		document.RaidTeamsForm.action.value = "ViewRaidTeams";
		document.RaidTeamsForm.submit();
	}

	// Фильтр по GPS
	function GPSChange()
	{ 
    		document.RaidTeamsForm.action.value = "ViewRaidTeams";
		document.RaidTeamsForm.submit();
    	}

	
	// Фильтр по полу
	function SexChange()
	{ 
    		document.RaidTeamsForm.action.value = "ViewRaidTeams";
		document.RaidTeamsForm.submit();
    	}

	function AgeChange()
	{ 
    		document.RaidTeamsForm.action.value = "ViewRaidTeams";
		document.RaidTeamsForm.submit();
    	}

	function UsersCountChange()
	{ 
    		document.RaidTeamsForm.action.value = "ViewRaidTeams";
		document.RaidTeamsForm.submit();
    	}
	
	
	// Формат вывода результатов
	function ResultViewModeChange()
	{ 
    	document.RaidTeamsForm.action.value = "ViewRaidTeams";
    	document.RaidTeamsForm.OrderType.value = "Place";
    	document.RaidTeamsForm.submit();
	}

	// Выгрузка данных для анализа
	function JsonExport()
	{ 
		document.RaidTeamsForm.action.value = "JsonExport";
    	document.RaidTeamsForm.submit();
	}

</script>
<!-- Конец вывода javascrpit -->



<?php


	// выделяем в списках пропущенных КП интервалы. Закладываемся, что список точек отсортирован и без повторов.
	function normalizeSkippedString($str)
	{
		return empty($str) ? '&nbsp;' :  normalizeSkipped(explode(',', $str));
	}

	// выделяем в списках пропущенных КП интервалы. Закладываемся, что список точек отсортирован и без повторов.
    function normalizeSkipped($skipped)
    {
        if ($skipped == null)
                return '&nbsp;';

		$delim = "&nbsp;&#8209;&nbsp;"; // non-breaking hyphen with non-breaking spaces

        $len = count($skipped);
        if ($len < 2)
                return implode(", ", $skipped);

		$start = 0;
		$last = $skipped[0];
		$ints = array();
		for ($i = 1; $i < $len; $i++)
		{
			if ($skipped[$i] == $last + 1)
			{
				$last = $skipped[$i];
				continue;
			}

			if ($start < $i - 2)    // полноценный интервал
				$ints[] = "{$skipped[$start]}$delim$last";
			else
			{
				$ints[] = $skipped[$start];
				if ($start < $i - 1)
					$ints[] = $last;
			}

			$start = $i;
			$last = $skipped[$i];
		}

		if ($start < $len - 2)
			$ints[] = "{$skipped[$start]}$delim$last";
		else
		{
			$ints[] = $skipped[$start];
			if ($start < $len - 1)
				$ints[] = $last;
		}

		return implode(", ", $ints);
    }


	//Получить массив всех участников
    function GetAllTeamMembers($raidId, $distanceId = null)
    {

		global $Anonimus;
		$distanceCond = (empty($distanceId) || $distanceId === 0) ? 'true' : "d.distance_id = $distanceId";

		$sql = "select tu.teamuser_id,
						CASE WHEN COALESCE(u.user_noshow, 0) = 1
							 THEN '$Anonimus'
							 ELSE u.user_name
						END as user_name, u.user_birthyear, u.user_city,
				       u.user_id, tld.levelpoint_id, lp.levelpoint_name,
					   tu.teamuser_notstartraidid, t.team_id as team_id
				from  TeamUsers tu
				      inner join Teams t on t.team_id = tu.team_id
				      inner join Distances d on t.distance_id = d.distance_id
					  inner join Users u  on tu.user_id = u.user_id
		              left outer join TeamLevelDismiss tld  on tu.teamuser_id = tld.teamuser_id
                      left outer join LevelPoints lp  on tld.levelpoint_id = lp.levelpoint_id
				where tu.teamuser_hide = 0 and d.raid_id = $raidId and $distanceCond
				order by team_id desc, user_name asc";

		$UserResult = MySqlQuery($sql);

		$last = null;
		$res = array();
		while ($row = mysqli_fetch_assoc($UserResult))
		{
			$teamId = $row['team_id'];
			if ($teamId !== $last)
				$res[$teamId] = array();
			$last = $teamId;

			$res[$teamId][] = array(
				'user_id' => $row['user_id'],
				'user_name' => $row['user_name'],
				'user_birthyear' => $row['user_birthyear'],
				'user_city' => $row['user_city'],
				'levelpoint_id' => $row['levelpoint_id'],
				'levelpoint_name' => $row['levelpoint_name'],
				'teamuser_notstartraidid' => $row['teamuser_notstartraidid']);
		}

		mysqli_free_result($UserResult);

	return $res;
    }


    // Получить список команд, ожидающих приглашения, которые имеют приглашения, но не спешат их активировать
    function GetTeamsWithUnusedInvitation($raidId)
    {
		$sql = "SELECT TeamUsers.team_id FROM Invitations, InvitationDeliveries, TeamUsers, Teams, Distances
				WHERE TeamUsers.team_id = Teams.team_id AND Teams.distance_id = Distances.distance_id AND Distances.raid_id = $raidId AND team_hide = 0 AND teamuser_hide = 0
				AND Teams.invitation_id IS NULL AND  Invitations.user_id = TeamUsers.user_id AND InvitationDeliveries.raid_id = Distances.raid_id AND Invitations.invitationdelivery_id = InvitationDeliveries.invitationdelivery_id
				AND Invitations.invitation_begindt <= NOW() AND Invitations.invitation_enddt >= NOW()
			GROUP BY TeamUsers.team_id";
		$TeamResult = MySqlQuery($sql);

		$forgetful = array();
		while ($row = mysqli_fetch_assoc($TeamResult))
			array_push($forgetful, $row['team_id']);
		mysqli_free_result($TeamResult);
		return $forgetful;
    }

	//Эта функция не нужна, если используется поле team_skippedlevelpoint
    function GetDistancePoints($raidId, $checkPointId)
    {
        $sql = "select lp.levelpoint_name, lp.levelpoint_id, lp.levelpoint_order, lp.distance_id
                        from LevelPoints lp
                        inner join Distances d on d.distance_id = lp.distance_id

                        where d.distance_hide = 0 and d.raid_id = $raidId
                        order by d.distance_id, lp.levelpoint_order asc";

        $res = array();
        $dist = null;
        $skipTail = false;

        $sqlRes = MySqlQuery($sql);
        while ($row = mysqli_fetch_assoc($sqlRes))
        {
                if ($dist != $row['distance_id'])
                {
                        $dist = $row['distance_id'];
                        $res[$dist] = array();
                        $skipTail = false;
                }
                if ($skipTail)
                        continue;

                $res[$dist][] = array('order' => $row['levelpoint_order'],
                                      'name' => $row['levelpoint_name']);
		if ($row['levelpoint_id'] == $checkPointId)
			$skipTail = true;
        }

        mysqli_free_result($sqlRes);

        return $res;
    }

	//Эта функция не нужна, если используется поле team_skippedlevelpoint
    function InvertPointList($full, $visitedNames, $lastPointOrder)
    {
        $nameHash = array();
        foreach(explode(',', $visitedNames) as $skipped)
                $nameHash[$skipped] = true;

        $skippedList = array();
        foreach($full as $point)
        {
                if (!isset($nameHash[$point['name']]))
                        $skippedList[] = $point['name'];

                if ($point['order'] == $lastPointOrder)
                        break;
        }

        return normalizeSkipped($skippedList);
    }

	//Эта функция не нужна, если используется поле team_skippedlevelpoint
    function GetAllSkippedPoints($raidId, $checkPointId)
    {
        $sql = "select t.team_id, t.distance_id, GROUP_CONCAT(lp.levelpoint_name ORDER BY lp.levelpoint_order, ' ') as notlevelpoint_name,
			COALESCE(t.team_maxlevelpointorderdone, 0) as last_done
			from  Teams t
				inner join  Distances d
				  on t.distance_id = d.distance_id
				join LevelPoints lp
                                  on t.distance_id = lp.distance_id
					and  COALESCE(t.team_maxlevelpointorderdone, 0) >= lp.levelpoint_order

				inner join TeamLevelPoints tlp
					on lp.levelpoint_id = tlp.levelpoint_id
					and t.team_id = tlp.team_id
		        where 	 d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $raidId
			group by t.team_id, t.distance_id";

	$time = microtime(true);
	$UserResult = MySqlQuery($sql);
	$time = microtime(true) - $time;

	$res = array();
	while ($row = mysqli_fetch_assoc($UserResult))
		$res[$row['team_id']] = array('names' => $row['notlevelpoint_name'],
		                              'distance' => $row['distance_id'],
					      'last' => $row['last_done']);

	mysqli_free_result($UserResult);

	$distanceLists = GetDistancePoints($raidId, $checkPointId);

	$result = array();
	foreach($res as $tid => $rec)
		if (isset($distanceLists[$rec['distance']]))
			$result[$tid] = InvertPointList($distanceLists[$rec['distance']], $rec['names'], $rec['last']);
		else
			die("</td></tr></table>distance points list doesn't have distance id = '{$rec['distance']}'");

	$result['__time__'] = $time;       // хак, чтобы вернуть время, за которое отработал запрос

	return $result;
    }


function ShowDistanceHeader($RaidId, $DistanceId, $DistanceName, $DistanceData, $lottery_count, $colspan)
{
	$DistanceTeams = "t.distance_id = $DistanceId AND t.team_hide = 0";

	$sql = "SELECT COUNT(team_id) AS inrangecount FROM Teams t WHERE $DistanceTeams AND t.team_outofrange = 0";
	$teamInRangeCount =  CSql::singleValue($sql, 'inrangecount');
	$sql = "SELECT COUNT(team_id) AS outofrangecount FROM Teams t WHERE $DistanceTeams AND t.team_outofrange = 1";
	$teamOutOfRangeCount =  CSql::singleValue($sql, 'outofrangecount');

	$sql = "SELECT COALESCE(SUM(COALESCE(team_mapscount, 0)), 0) AS inrangecount FROM Teams t WHERE $DistanceTeams AND t.team_outofrange = 0";
	$mapsInRangeCount =  CSql::singleValue($sql, 'inrangecount');
	$sql = "SELECT COALESCE(SUM(COALESCE(team_mapscount, 0)), 0) AS outofrangecount FROM Teams t WHERE $DistanceTeams AND t.team_outofrange = 1";
	$mapsOutOfRangeCount =  CSql::singleValue($sql, 'outofrangecount');

	$sql = "SELECT COUNT(tu.teamuser_id) AS inrangecount FROM Teams t INNER JOIN TeamUsers tu ON t.team_id = tu.team_id WHERE $DistanceTeams AND tu.teamuser_hide = 0 AND t.team_outofrange = 0";
	$teamUserInRangeCount =  CSql::singleValue($sql, 'inrangecount');
	$sql = "SELECT COUNT(tu.teamuser_id) AS outofrangecount FROM Teams t INNER JOIN TeamUsers tu ON t.team_id = tu.team_id WHERE $DistanceTeams AND tu.teamuser_hide = 0 AND t.team_outofrange = 1";
	$teamUserOutOfRangeCount =  CSql::singleValue($sql, 'outofrangecount');

	$distanceDescription = $DistanceName;
	if ($DistanceData) $distanceDescription .= ', ' . $DistanceData;
	print("<tr class=yellow>\n <td colspan=2>Дистанция $distanceDescription</td>\n");

	// для марш-бросков без приглашений оставляем старый вариант вывода
	if ($RaidId < 28)
	{
		print(" <td colspan=$colspan>Команд <span title=\"в зачете\">$teamInRangeCount</span>/<span title=\"вне зачета\">$teamOutOfRangeCount</span>,".
			" карт <span title=\"у команд в зачете\">$mapsInRangeCount</span>/<span title=\"у команд вне зачета\">$mapsOutOfRangeCount</span>,".
			" участников <span title=\"в командах в зачете\">$teamUserInRangeCount</span>/<span title=\"в командах вне зачета\">$teamUserOutOfRangeCount</span></td>\n</tr>\n");
		return;
	}

	// получаем количество команд с приглашениями каждого типа
	$invited = array();
	for ($inv_type = 1; $inv_type <= 3; $inv_type++)
	{
		$sql = "SELECT COUNT(*) AS invited_count FROM
			(SELECT DISTINCT t.team_id FROM Teams t
				INNER JOIN Invitations inv ON inv.invitation_id = t.invitation_id
				INNER JOIN TeamUsers tu ON tu.team_id = t.team_id AND tu.teamuser_hide = 0
				INNER JOIN InvitationDeliveries invd ON invd.invitationdelivery_id = inv.invitationdelivery_id
				WHERE $DistanceTeams AND invd.invitationdelivery_type = $inv_type) AS TeamsInvited";
		$invited[$inv_type] =  CSql::singleValue($sql, 'invited_count');
	}
	// отдельно считаем количество команд, приглашенных по рейтингу своими участниками
	$sql = "SELECT COUNT(*) AS invited_count FROM
		(SELECT DISTINCT t.team_id FROM Teams t
			INNER JOIN Invitations inv ON inv.invitation_id = t.invitation_id
			INNER JOIN TeamUsers tu ON tu.team_id = t.team_id AND tu.teamuser_hide = 0
			INNER JOIN InvitationDeliveries invd ON invd.invitationdelivery_id = inv.invitationdelivery_id
			WHERE $DistanceTeams AND invd.invitationdelivery_type = 1 AND tu.user_id = inv.user_id) AS TeamsInvited";
	$invited_self =  CSql::singleValue($sql, 'invited_count');

	// выводим статистику
	print(" <td colspan=$colspan>Участвует команд <span title=\"всего команд с приглашениями\">$teamInRangeCount</span> ".
		"(<span title=\"пригласившие сами себя\">$invited_self</span>".
		"/<span title=\"приглашенные другими участниками\">".($invited[1] - $invited_self)."</span>".
		"/<span title=\"выгравшие в лотерею\">".$invited[2]."</span>".
		"/<span title=\"с приглашениями, выданными вручную\">".$invited[3]."</span>), ");
	/*if ($lottery_count)
	{
		print("участников <span title=\"в приглашенных командах\">$teamUserInRangeCount</span>, карт <span title=\"в приглашенных командах\">$mapsInRangeCount</span>");
	}
	else*/
	{
		print("ожидают приглашения <span title=\"всего команд без приглашений\">$teamOutOfRangeCount</span>, ");
		print("участников <span title=\"в приглашенных командах\">$teamUserInRangeCount</span>/<span title=\"в командах, ожидающих приглашение\">$teamUserOutOfRangeCount</span>, ");
		print("карт <span title=\"в приглашенных командах\">$mapsInRangeCount</span>/<span title=\"в командах, ожидающих приглашение\">$mapsOutOfRangeCount</span></td>\n</tr>\n");
	}
}


    // Проверяем, что передали  идентификатор ММБ
    if ($RaidId <= 0)
	{
		    CMmb::setMessage('Не указан ММБ');
		    return;
	}


?>	
         <form  name = "RaidTeamsForm"  action = "<? echo $MyPHPScript; ?>" method = "post">
         <input type = "hidden" name = "action" value = "ViewRaidTeams">
         <input type = "hidden" name = "TeamId" value = "0">
         <input type = "hidden" name = "UserId" value = "0">
         <input type = "hidden" name = "RaidId" value = "<? echo $RaidId; ?>">

<?

	$t1 = microtime(true);

	$TabIndex = 0;
	$DisabledText = '';

        //Определяем локальные переменные-флаги показа результатов и этапов
        $CanViewResults = CanViewResults($Administrator, $Moderator, $RaidStage);
        $CanViewLevels = CanViewLevels($Administrator, $Moderator, $RaidStage);


        // Разбираемся с сортировкой
        $OrderType = mmb_validate($_REQUEST, 'OrderType', '');
	if (($OrderType == 'Errors') && !$Administrator && !$Moderator) $OrderType = '';


		$sql = "select raid_registrationenddate,  raid_closedate,
				 COALESCE(r.raid_noshowresult, 0) as raid_noshowresult, raid_readonlyhoursbeforestart
			  from  Raids r
			  where r.raid_id = $RaidId"; 
            
		$Row = CSql::singleRow($sql);
                $RaidRegisterEndDt = $Row['raid_registrationenddate'];
                $RaidCloseDt = $Row['raid_closedate'];
                $RaidNoShowResult = $Row['raid_noshowresult'];
		$RaidReadonlyHoursBeforeStart = $Row['raid_readonlyhoursbeforestart'];
   
        // 03/05/2014 Исправил порядок сортировки - раньше независимо от установленного  $OrderType могло сбрасываться
        // если порядок не задан смотрим на соотношение временени публикации и текущего
        if (empty($OrderType))
	{
  	      $OrderType = $CanViewResults ? "Place" : "Num";
	}
	// Конец разбора сортировки по умолчанию
	   
// region часть над таблицей
          print('<div align="left" style="font-size: 80%;">'."\r\n");

          if ($CanViewResults || $Administrator || $Moderator)    // будет больше 1 пункта
          {
		print("Сортировать по \r\n");
		print('<select name="OrderType" style="margin-left: 10px; margin-right: 20px;"
	                       onchange="OrderTypeChange();"  tabindex="'.(++$TabIndex).'" '."$DisabledText>\r\n");
		print('<option value="Num" '.($OrderType == 'Num' ? 'selected' :'').">убыванию номера</option>\r\n");

	        //Сортировку по месту показыаем только после окончания ММБ, если не стоит флаг "Не показывать результаты"
		// Администраторам и модераторам флаг не мешает
		if ($CanViewResults)
		    print('<option value="Place" '.($OrderType == 'Place' ? 'selected' :'').">возрастанию места</option>\r\n");

		if ($Administrator || $Moderator)
		    print('<option value="Errors" '.($OrderType == 'Errors' ? 'selected' :'').">наличию ошибок</option>\r\n");

		print('</select>'."\r\n");
	 }


	print("Фильтровать: \r\n"); 

	$distanceId = mmb_validate($_REQUEST, 'DistanceId', '');
	$DistanceCondition = empty($distanceId) ? 'true' : "d.distance_id = $distanceId";

        $GpsFilter = (mmb_validateInt($_REQUEST, 'GPSFilter', 0)) == 1 ? 1 : 0;
        $GpsCondition = $GpsFilter ? "t.team_usegps = 0" : "true";

        $SexFilter = (mmb_validateInt($_REQUEST, 'SexFilter', 0));
        
	if ($SexFilter == 0)
	{
		$SexCondition = "true";
	} elseif ($SexFilter == 1) {
		$SexCondition = "t.team_maxsex = 1 and t.team_minsex = 1";
	} elseif ($SexFilter == 2) {
		$SexCondition = "t.team_maxsex = 2 and t.team_minsex = 2";
	} elseif ($SexFilter == 3) {
		$SexCondition = "t.team_maxsex = 2 and t.team_minsex = 1";
	} 
		 
        $AgeFilter = (mmb_validateInt($_REQUEST, 'AgeFilter', 0));
        
	if ($AgeFilter == 0)
	{
		$AgeCondition = "true";
	} elseif ($AgeFilter == 1) {
		$AgeCondition = "t.team_maxage < 23";
	} elseif ($AgeFilter == 2) {
		$AgeCondition = "t.team_maxage < 40 and t.team_minage >= 23";
	} elseif ($AgeFilter == 3) {
		$AgeCondition = "t.team_minage >= 40";
	} elseif ($AgeFilter == 4) {
		$AgeCondition = "t.team_minage >= 55";
	} 

        $UsersCountFilter = (mmb_validateInt($_REQUEST, 'UsersCountFilter', 0));
        
	if ($UsersCountFilter == 0)
	{
		$UsersCountCondition = "true";
	} elseif ($UsersCountFilter == 1) {
		$UsersCountCondition = "t.team_userscount = 1";
	} elseif ($UsersCountFilter == 2) {
		$UsersCountCondition = "t.team_userscount = 2";
	} elseif ($UsersCountFilter == 3) {
		$UsersCountCondition = "t.team_userscount > 2";
	} 
		 
		 
        $sql = "select distance_id, distance_name
                    from  Distances
                    where distance_hide = 0 and raid_id = $RaidId
                    order by distance_name";
		//echo 'sql '.$sql;
	$Result = MySqlQuery($sql);
	if (mysqli_num_rows($Result) > 1)
	{
		print('<select name="DistanceId" style = "margin-left: 10px; margin-right: 5px;"
                               onchange="DistanceIdChange();"  tabindex="'.(++$TabIndex).'">'."\r\n");

                $selected  =  (0 == $distanceId) ? ' selected' : '';
		print("<option value=\"0\" $selected>дистанцию</option>\r\n");

	        while ($Row = mysqli_fetch_assoc($Result))
		{
		  $selected = ($Row['distance_id'] == $distanceId) ? ' selected' : '';
		  print("<option value=\"{$Row['distance_id']}\" $selected>{$Row['distance_name']}</option>\r\n");
		}
		print("</select>\r\n");
	}
	mysqli_free_result($Result);

/*
============================= точки ===============================
*/
// 06/11/2015 Закоментировал условие фильтрации по результатам, чтобы ускорить запрос
// вместо этого просто фильтруем по типам  - не показываем только КП
// если потом появятся новые типы точек - нужно проверить условие where

	if (CRights::canShowPointsFilter($RaidId))
	{

	        $sql = "select lp.levelpoint_id, lp.levelpoint_name, d.distance_name,
        		tlp1.teamscount, tlp2.teamuserscount
        	        from  LevelPoints lp
				left outer join ScanPoints sp
				on lp.scanpoint_id = sp.scanpoint_id
 	  			inner join Distances d
				on lp.distance_id = d.distance_id
				left outer join
				     (select tlp.levelpoint_id, count(t.team_id) as teamscount
				     from TeamLevelPoints tlp
				          inner join Teams t on t.team_id = tlp.team_id
					  inner join Distances d on t.distance_id = d.distance_id
				     where d.raid_id = $RaidId and $DistanceCondition
			     		and t.team_hide = 0
			     		and t.team_outofrange = 0
				     group by tlp.levelpoint_id
				     ) tlp1
			     	on lp.levelpoint_id = tlp1.levelpoint_id
			     	left outer join
			     		(select tlp.levelpoint_id, count(tu.teamuser_id) as teamuserscount
			     		from TeamLevelPoints tlp
				     		inner join Teams t on t.team_id = tlp.team_id
					        inner join TeamUsers tu on t.team_id = tu.team_id
						inner join Distances d on t.distance_id = d.distance_id
				        where d.raid_id = $RaidId and $DistanceCondition
				     		and t.team_hide = 0
				     		and t.team_outofrange = 0
				     		and tu.teamuser_hide = 0
					group by tlp.levelpoint_id
				     	) tlp2
			     	on lp.levelpoint_id = tlp2.levelpoint_id					  
			where d.raid_id = $RaidId and $DistanceCondition and lp.pointtype_id <> 5  
			     and (lp.pointtype_id <> 3 or sp.scanpoint_id is not null)
			order by lp.levelpoint_order ";

		//echo 'sql '.$sql;
		$Result = MySqlQuery($sql);
                
		print('<select name="LevelPointId" style = "margin-left: 10px; margin-right: 5px;" 
                       onchange = "LevelPointIdChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
        	$levelpointselected =  (0 == $_REQUEST['LevelPointId'] ? 'selected' : '');
		print("<option value = '0' $levelpointselected>не фильтровать точку</option>\r\n");

		if (!isset($_REQUEST['LevelPointId'])) $_REQUEST['LevelPointId'] = "";

	        while ($Row = mysqli_fetch_assoc($Result))
		{
			$levelpointselected = ($Row['levelpoint_id'] == $_REQUEST['LevelPointId']  ? 'selected' : '');
			print("<option value = '{$Row['levelpoint_id']}' $levelpointselected>{$Row['distance_name']} {$Row['levelpoint_name']} ({$Row['teamscount']}/{$Row['teamuserscount']})</option>\r\n");
		}
		print('</select>'."\r\n");  
		mysqli_free_result($Result);		

	} else {
         
		print('<input type = "hidden" name = "LevelPointId" value = "0">'."\r\n");  

	}

/*
======================  GPS  ====================
*/
	print('<select name="GPSFilter" style = "margin-left: 10px; margin-right: 5px;"
                       onchange = "GPSChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
	print('<option value="0" '. ($GpsFilter == 0 ? 'selected' : '') ." >не фильтровать GPS</option>\r\n");
	print('<option value="1" '. ($GpsFilter == 1 ? 'selected' : '') ." >без GPS</option>\r\n");
	print('</select>'."\r\n");  

		 
/*
======================  Пол  ====================
*/
	print('<select name="SexFilter" style = "margin-left: 10px; margin-right: 5px;"
                       onchange = "SexChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
	print('<option value="0" '. ($SexFilter == 0 ? 'selected' : '') ." >не фильтровать пол</option>\r\n");
	print('<option value="1" '. ($SexFilter == 1 ? 'selected' : '') ." >Ж</option>\r\n");
	print('<option value="2" '. ($SexFilter == 2 ? 'selected' : '') ." >М</option>\r\n");
	print('<option value="3" '. ($SexFilter == 3 ? 'selected' : '') ." >ЖМ</option>\r\n");
	print('</select>'."\r\n");  
		 
/*
======================  Возраст  ====================
*/
	print('<select name="AgeFilter" style = "margin-left: 10px; margin-right: 5px;"
                       onchange = "AgeChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
	print('<option value="0" '. ($AgeFilter == 0 ? 'selected' : '') ." >не фильтровать возраст</option>\r\n");
	print('<option value="1" '. ($AgeFilter == 1 ? 'selected' : '') ." >22 и меньше</option>\r\n");
	print('<option value="2" '. ($AgeFilter == 2 ? 'selected' : '') ." >23-39</option>\r\n");
	print('<option value="3" '. ($AgeFilter == 3 ? 'selected' : '') ." >40 и больше</option>\r\n");
	print('<option value="4" '. ($AgeFilter == 4 ? 'selected' : '') ." >55 и больше</option>\r\n");
	print('</select>'."\r\n");  
		 

/*
======================  Число  ====================
*/
	print('<select name="UsersCountFilter" style = "margin-left: 10px; margin-right: 5px;"
                       onchange = "UsersCountChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
	print('<option value="0" '. ($UsersCountFilter == 0 ? 'selected' : '') ." >не фильтровать число участников</option>\r\n");
	print('<option value="1" '. ($UsersCountFilter == 1 ? 'selected' : '') ." >1</option>\r\n");
	print('<option value="2" '. ($UsersCountFilter == 2 ? 'selected' : '') ." >2</option>\r\n");
	print('<option value="3" '. ($UsersCountFilter == 3 ? 'selected' : '') ." >много</option>\r\n");
	print('</select>'."\r\n");  
		 

		 
/*
=====================================
*/

    // Режим отображения результатов
    $ResultViewMode = mmb_validate($_REQUEST, 'ResultViewMode', '');

	print('</div>'."\r\n");
	print('<div align="left" style="margin-top:10px; margin-bottom:10px; font-size: 100%;">'."\r\n");

	$ReglamentLink = CSql::raidFileLink($RaidId, 1, true);
	if (!empty($ReglamentLink))
	{
		print('<a style="font-size:80%; margin-right: 15px;" href="'.$ReglamentLink.'" title="Основные правила ММБ" target = "_blank">Положение</a>'."\r\n");
	}
	if ($UserId and $RaidId and CRights::canCreateTeam($UserId, $RaidId))
	{
		print('<a  style="font-size:80%; margin-right: 15px;"  href="javascript:NewTeam();" title="Регистрация команды на ММБ">Заявить команду</a>'."\r\n");
	}
	$teamId = CSql::userTeamId($UserId, $RaidId);
	if ($teamId)
	{
		$sql = "select COALESCE(t.team_num, 0) as team_num
			from  Teams t
			where t.team_hide = 0 and t.team_id = $teamId";
		$teamNum = (int)CSql::singleValue($sql, 'team_num');

		if ($teamNum)
		{
			print("<a style=\"font-size:80%; margin-right: 15px;\" href=\"#$teamNum\" title=\"Переход к строке Вашей команды\">Моя команда</a>\r\n");
		}
	}
	$StartLink = CSql::raidFileLink($RaidId, 10, true);
	if (!empty($StartLink))
	{
		print('<a style="font-size:80%; margin-right: 15px;" href="'.$StartLink.'" title="Информация о месте и порядке старта ММБ" target = "_blank">Старт</a>'."\r\n");
	}
	print('<a style="font-size:80%; margin-right: 15px;" href="?links&RaidId='.$RaidId.'" title="Страница впечатлений: отчеты, фотографии, треки...">Впечатления</a>'."\r\n");
	print('<a style="font-size:80%; margin-right: 15px;" href="?files&RaidId='.$RaidId.'" title="Все материалы ММБ: положение, карты, легенды...">Материалы</a>'."\r\n");
	print('<a style="font-size:80%; margin-right: 15px;" href="javascript: JsonExport();">Json</a> '."\r\n");


	// можем / нужно ли показывать карты?
	$mapQuery = "select raidfile_name, raidfile_comment
	                from RaidFiles
	                        where raid_id = $RaidId 
	                        	and filetype_id = 4 
	                        	and substr(lower(raidfile_name), -4)  in ('.png','.gif','.jpg','jpeg')
	                        	and raidfile_hide = 0
	     			order by raidfile_id asc";

	$canShowMaps = CRights::canShowImages($RaidId) && CSql::rowCount($mapQuery) > 0;

	$showMapImages = mmb_validateInt($_GET, 'showMap', '0');
	if ($canShowMaps)
	{
		if ($showMapImages == 1)
		{
			print('<a style="font-size:80%; margin-right: 15px;" href="?protocol&RaidId='.$RaidId.'&showMap=0" title="Не отображать карты в протоколе - можно смотреть на странцие материалов">Скрыть карты</a> '."\r\n");
		} else {
			print('<a style="font-size:80%; margin-right: 15px;" href="?protocol&RaidId='.$RaidId.'&showMap=1" title="Отображать карты на этой странице - время загрзуки может заметно вырасти!">Показать карты</a> '."\r\n");
		}
	}

	print('</div>'."\r\n");

	// собственно вывод карт
	if ($canShowMaps and $showMapImages)
	{
		print('<div align="left" style="margin-top:10px; margin-bottom:10px; font-size: 100%;">'."\r\n");

		$Result = MySqlQuery($mapQuery);
		while ($Row = mysqli_fetch_assoc($Result))
		{
			$ImageLink = $Row['raidfile_name'];
			$ImageComment = $Row['raidfile_comment'];

			$point = strrpos($ImageLink, '.');
			if  ($point <= 0)
				continue;

			$tumbImg = substr($ImageLink, 0, $point).'_tumb'.substr($ImageLink, $point);
		//	echo $point.' '.$ImageLink.' '.$tumbImg;
			if (!is_file(trim($MyStoreFileLink).$tumbImg))
			{
		//	echo '1111';
				image_resize(trim($MyStoreFileLink).trim($ImageLink), trim($MyStoreFileLink).trim($tumbImg), 1000, 100, 0);
			}
			print('<a style="margin-right: 15px;" href="'.trim($MyStoreHttpLink).trim($ImageLink).'" title="'.trim($ImageComment).'" target = "_blank"><img src = "'.trim($MyStoreHttpLink).trim($tumbImg).'"  alt = "'.trim($ImageComment).'" height = "100"></a>'."\r\n");
		}
  		mysqli_free_result($Result);
		print('</div>'."\r\n");
	}

	print("\r\n</form>\r\n");


       // Заголовок общей таблицы
       $tdstyle = ' style="padding: 5px 0px 2px 5px;"';
       $tdstyle = '';
       $thstyle = ' style="border-color: #000000; border-style: solid; border-width: 1px 1px 1px 1px; padding: 5px 0px 2px 5px"';
       $thstyle = '';
       $colspan = 1;
       if ($OrderType == 'Num') $colspan = 1;
       elseif ($OrderType == 'Place') $colspan = 6;
       elseif ($OrderType == 'Errors') $colspan = 5;

       print("<table border=0 cellpadding=10 style=\"font-size: 80%\">\n");

       // определяем, проводилась ли лотерея, чтобы сообщить о том, что новые команды создавать не стоит
       $sql = "SELECT COUNT(invitationdelivery_id) AS lottery_count FROM InvitationDeliveries invd WHERE invd.raid_id = $RaidId AND invd.invitationdelivery_type = 2";
       $lottery_count =  CSql::singleValue($sql, 'lottery_count');

       // Общая информация о марш-броске
       print("<tr class=green><td colspan=".($colspan + 2).">");
       if (!empty($RaidCloseDt))
           print("Протокол закрыт $RaidCloseDt");
       else
       {
           // определяем дату, до которой активны приглашения по рейтингу (if any)
           $sql = "SELECT MAX(inv.invitation_enddt) AS maxinvdt FROM InvitationDeliveries invd INNER JOIN Invitations inv ON invd.invitationdelivery_id = inv.invitationdelivery_id WHERE invd.raid_id = $RaidId AND invd.invitationdelivery_type = 1";
           $maxinvdt =  CSql::singleValue($sql, 'maxinvdt', false);

           // выводим текущий статус марш-броска
           if ($maxinvdt) $message = "Заявка команд до $maxinvdt"; else $message = "";
           if ($lottery_count) $message = "Заявка новых команд закрыта, добавление участников в существующие команды до $RaidRegisterEndDt";
           if ($RaidStage == 2) $message = "Добавление команд и участников уже невозможны, удаление закрывается за $RaidReadonlyHoursBeforeStart часов до старта";
           if ($RaidStage == 3) $message = "Любые измения в командах уже невозможны, ждем вас на старте";
           if (($RaidStage == 4) || ($RaidStage == 5)) $message = "Результаты марш-броска обычно появляются через сутки после закрытия финиша";
           if ($RaidStage > 5) $message = "Об ошибках в результатах сообщайте в соответствующей теме в ЖЖ";
           print($message);
       }
       print("</td></tr>\n");

      // Информация о дистанции(ях)
      $sql = "SELECT d.distance_name, d.distance_data, d.distance_id FROM Distances d WHERE d.distance_hide = 0 AND d.raid_id = $RaidId";
      $Result = MySqlQuery($sql);
      while ($Row = mysqli_fetch_assoc($Result))
          ShowDistanceHeader($RaidId, $Row['distance_id'], $Row['distance_name'], $Row['distance_data'], $lottery_count, $colspan);
      mysqli_free_result($Result);


	// ============ Вывод списка команд ===========================


	// Выводим список команд
	if  ($OrderType == 'Num')
	{

                  // Сортировка по номеру (в обратном порядке)
		$sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,
 			               t.team_mapscount,  d.distance_name, d.distance_id,
			       			COALESCE(t.team_outofrange, 0) as  team_outofrange,
						COALESCE(t.team_dismiss, 0) as  team_dismiss
	        	from  Teams t
		    			inner join  Distances d on t.distance_id = d.distance_id
				where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
						and $DistanceCondition and $GpsCondition and $SexCondition and $AgeCondition and $UsersCountCondition
				order by team_num desc";

	}
	elseif ($OrderType == 'Place') {
		// Сортировка по месту требует более хитрого запроса
		$levelPointId = mmb_validate($_REQUEST, 'LevelPointId', '');

		if (!empty($levelPointId)) {
			$LevelCondition = "tlp.levelpoint_id = $levelPointId";

			$sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,
			               t.team_mapscount, lp.levelpoint_order as team_progress,
			               CASE WHEN COALESCE(t.team_minlevelpointorderwitherror, 0) > lp.levelpoint_order THEN 0 ELSE COALESCE(t.team_minlevelpointorderwitherror, 0) END as team_error,
					       d.distance_name, d.distance_id,
	                       TIME_FORMAT(tlp.teamlevelpoint_result, '%H:%i') as team_sresult,
					       COALESCE(t.team_outofrange, 0) as  team_outofrange,
					       COALESCE(t.team_dismiss, 0) as  team_dismiss,
					       COALESCE(t.team_donelevelpoint, COALESCE(lp.levelpoint_name, '')) as levelpoint_name,
					       COALESCE(t.team_comment, '') as team_comment /*,
					       COALESCE(t.team_skippedlevelpoint, '') as team_skippedlevelpoint */
					from  Teams t
							inner join  Distances d	on t.distance_id = d.distance_id
							inner join  TeamLevelPoints tlp	on t.team_id = tlp.team_id
       						inner join LevelPoints lp on tlp.levelpoint_id = lp.levelpoint_id
				  	where t.team_hide = 0 and $LevelCondition and $GpsCondition and $SexCondition and $AgeCondition and $UsersCountCondition
				    order by distance_name, team_outofrange, team_progress desc, team_error asc, tlp.teamlevelpoint_result asc, team_num asc ";

		} else {

			$sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,
			               t.team_mapscount, t.team_maxlevelpointorderdone as team_progress,
			               COALESCE(t.team_minlevelpointorderwitherror, 0) as team_error,
					       d.distance_name, d.distance_id,
			               TIME_FORMAT(t.team_result, '%H:%i') as team_sresult,
					       COALESCE(t.team_outofrange, 0) as  team_outofrange,
					       COALESCE(t.team_dismiss, 0) as  team_dismiss,
					       COALESCE(t.team_donelevelpoint, COALESCE(lp.levelpoint_name, '')) as levelpoint_name,
				    	   COALESCE(t.team_comment, '') as team_comment /*,
						   COALESCE(t.team_skippedlevelpoint, '') as team_skippedlevelpoint */
					  from  Teams t
							inner join  Distances d	on t.distance_id = d.distance_id
							left outer join LevelPoints lp
							on lp.distance_id = t.distance_id
					   			and lp.levelpoint_order = t.team_maxlevelpointorderdone
				  	  where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
				  			and $DistanceCondition and $GpsCondition and $SexCondition and $AgeCondition and $UsersCountCondition
			          order by distance_name, team_outofrange, team_progress desc, team_error asc, team_result asc, team_num asc ";


		}
			$skpd = microtime(true);
				$skippedPoints = GetAllSkippedPoints($RaidId, $levelPointId);
			  $skpd = microtime(true) - $skpd;
				$skpd0 = $skippedPoints['__time__'];
			//$skpd0 = 0;

		// Конец проверки, задана ли точка фильтрации
	}
	elseif ($OrderType == 'Errors')
	{

       	// Не знаю, как будет реализовано
	    $sql = "select  t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,
			                t.team_mapscount, COALESCE(t.team_maxlevelpointorderdone, 0) as team_progress,
			                COALESCE(t.team_minlevelpointorderwitherror, 0) as team_error,
					        d.distance_name, d.distance_id,
                            TIME_FORMAT(t.team_result, '%H:%i') as team_sresult,
							COALESCE(t.team_outofrange, 0) as  team_outofrange,
							COALESCE(t.team_dismiss, 0) as  team_dismiss,
			       			COALESCE(lp.levelpoint_name, '') as levelpoint_name,
			       			CONCAT_WS(' Комментарий: ', COALESCE(errt.errcomment, ''), COALESCE(t.team_comment, '')) as team_comment
			  		from  Teams t
							inner join
								(
								select tlp.team_id,
								GROUP_CONCAT(CONCAT_WS(' - ', lp.levelpoint_name, err.error_name) ORDER BY lp.levelpoint_order, ' ') as errcomment
				  				from TeamLevelPoints tlp
				  				     inner join LevelPoints lp
				  				     on tlp.levelpoint_id = lp.levelpoint_id
				  				     inner join Errors err
				  				     on tlp.error_id = err.error_id
								where COALESCE(tlp.error_id, 0) <> 0
				  				group by tlp.team_id
								) errt
							on t.team_id = errt.team_id
							inner join  Distances d
							on t.distance_id = d.distance_id
							left outer join LevelPoints lp
							on lp.distance_id = t.distance_id
				   				and lp.levelpoint_order = t.team_maxlevelpointorderdone
			  		where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
			  				and $DistanceCondition
			        order by distance_name, team_outofrange, team_progress desc, team_error asc, team_result asc, team_num asc ";

	}

   	$prep = microtime(true);
	$Result = MySqlQuery($sql);
    	$t2 = microtime(true);

    	$TeamMembers  = GetAllTeamMembers($RaidId, $distanceId);
    	$allUsers = microtime(true) - $t2;
    
        if ($Administrator) $forgetful = GetTeamsWithUnusedInvitation($RaidId);
    	
	
	print('<tr class = "gray">'."\r\n");

	if ($OrderType == 'Num') {

		$ColumnWidth = 350;
		$ColumnSmallWidth = 50;
			print('<td width = "'.$ColumnSmallWidth.'"'.$thstyle.'>Номер</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Команда</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Участники</td>'."\r\n");
		
	} elseif ($OrderType == 'Place') {

        	$ColumnWidth = 350;
		$ColumnSmallWidth = 50;
			print('<td width = "'.$ColumnSmallWidth.'"'.$thstyle.'>Номер</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Команда</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Участники</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Отсечки времени</td>'."\r\n");
                        print('<td width = "'.$ColumnSmallWidth.'"'.$thstyle.'>Результат</td>'."\r\n");
                        print('<td width = "'.$ColumnSmallWidth.'"'.$thstyle.'>Место</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Комментарий</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Не пройдены точки</td>'."\r\n");


	} elseif ($OrderType == 'Errors') {
	
	
            $ColumnWidth = 350;
	    $ColumnSmallWidth = 50;

			print('<td width = "'.$ColumnSmallWidth.'"'.$thstyle.'>Номер</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Команда</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Участники</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Отсечки времени</td>'."\r\n");
                        print('<td width = "'.$ColumnSmallWidth.'"'.$thstyle.'>Результат</td>'."\r\n");
                        print('<td width = "'.$ColumnSmallWidth.'"'.$thstyle.'>Место</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'"'.$thstyle.'>Комментарий</td>'."\r\n");

		
	}
	
	print('</tr>'."\r\n");
	
	$TeamsCount = mysqli_num_rows($Result);
    // Меняем логику отображения места
    // Было 1111233345  Стало 1111455589
    $TeamPlace = 0;
    $SamePlaceTeamCount = 1;
    $PredResult = '';
	$PredDistanceId = 0;
		
	while ($Row = mysqli_fetch_assoc($Result))
	{
			$TrClass = ($TeamsCount%2 == 0) ? 'yellow': 'green';
			$TeamsCount--;

			$useGps = $Row['team_usegps'] == 1 ? 'gps, ' : '';
			$teamGP = $Row['team_greenpeace'] == 1 ? ', <a title="Нет сломанным унитазам!" href="#comment">ну!</a>' : '';
			$outOfRange = $Row['team_outofrange'] == 1 ? ($RaidId > 27 ? 'Ожидает приглашения!' : 'Вне зачета!') : '';
			$teamDismiss = $Row['team_dismiss'] == 1 ? ': Не явилась!' : '';

			if ($Administrator && $outOfRange && in_array($Row['team_id'], $forgetful)) $outOfRange = '<span style="color:red">' . $outOfRange . '</span>';
			if ($outOfRange) $outOfRange = ', ' . $outOfRange;

 			print('<tr class="'.$TrClass.'">
			       <td'.$tdstyle.'><a name="'.$Row['team_num'].'"></a>'.$Row['team_num'].'</td>
			       <td'.$tdstyle.'><a href="?TeamId='.$Row['team_id'].'&RaidId=' . $RaidId .'">'.
			          CMmbUI::toHtml($Row['team_name'])."</a> ($useGps{$Row['distance_name']}, {$Row['team_mapscount']}$teamGP$outOfRange) $teamDismiss</td><td$tdstyle>\r\n");


                        // Формируем колонку Участники			
			if (!isset($TeamMembers[$Row['team_id']]))
			{
				print('<div class= "input">no member records in team '.$Row['team_id'].'</div>'."\r\n");
				//	die("</td></tr></table> no member records in team '{$Row['team_id']}'");
			} else {
		
				foreach($TeamMembers[$Row['team_id']] as $UserRow)
				{
					print('<div class= "input"><a href="?UserId='.$UserRow['user_id'].'&RaidId=' . $RaidId . '">'.CMmbUI::toHtml($UserRow['user_name']).'</a> '.$UserRow['user_birthyear'].' '.CMmbUI::toHtml($UserRow['user_city'])."\r\n");

					// Отметка невыхода на старт в предыдущем ММБ
					if ($UserRow['teamuser_notstartraidid'] > 0)
						print(' <a title="Участник был заявлен, но не вышел на старт в прошлый раз" href="#comment">(?!)</a> ');

					// Неявку участников показываем, если загружены результаты
					if ($CanViewResults)
					{
						if ($UserRow['levelpoint_name'] <> '')
						print("<i>Не явился(-ась) в: {$UserRow['levelpoint_name']}</i>\r\n");
					}
					print('</div>'."\r\n");
				}
			}
			// Конец формирования колонки Участники
			print("</td>\r\n");


            // Сортировка "по месту"
			if ($OrderType == 'Place')   
			{
			    print("<td>{$Row['levelpoint_name']}</td>\r\n");
			    print("<td>{$Row['team_sresult']}</td>\r\n");

                // Формируем место
			    print("<td>\r\n");
                // Сбрасываем при смене дистанции
			    if ($Row['distance_id'] <> $PredDistanceId)
                {
			        $TeamPlace = 0;
					$SamePlaceTeamCount = 1;
					$PredResult = '';
					$PredDistanceId = $Row['distance_id'];
                }
			    
                if ($Row['team_sresult'] == '00:00' or $Row['team_sresult'] == '' or $Row['team_outofrange'] == 1 or $Row['team_error'] > 0)
                {
                    print('&nbsp;');
                }
				elseif($Row['team_sresult'] <>  $PredResult)
				{
                    $TeamPlace = $TeamPlace + $SamePlaceTeamCount;
                    print($TeamPlace);
			        $PredResult = $Row['team_sresult'];
			        $SamePlaceTeamCount = 1;
                }
				else
				{
					$SamePlaceTeamCount++;
                    print($TeamPlace);
			        $PredResult = $Row['team_sresult'];
                }

			    print("</td>\r\n");
			    print("<td>\r\n");
			    print($Row['team_comment']);
			    print("</td>\r\n");

			    $skipped = isset($skippedPoints[$Row['team_id']]) ? $skippedPoints[$Row['team_id']] : '&nbsp;';
			//	$skipped = isset($Row['team_skippedlevelpoint']) ? normalizeSkippedString($Row['team_skippedlevelpoint']) : '&nbsp;';

			    print("<td>$skipped</td>\r\n");

			}  elseif ($OrderType == 'Errors') {

			    print("<td>{$Row['levelpoint_name']}</td>\r\n");
			    print("<td>{$Row['team_sresult']}</td>\r\n");
			    print("<td>&nbsp;</td>\r\n");
			    print("<td>\r\n");
			    print($Row['team_comment']);
			    print("</td>\r\n");
			}
			// Конец проверки на вывод с сортировкой по месту
			print("</tr>\r\n");
	}
	mysqli_free_result($Result);

	print("</table>\r\n");

	$t3 = microtime(true);

	if (empty($skpd))
			$skpd = 0;
	if (empty($skpd0))
			$skpd0 = 0;

		print("<div style=\"display: none;\"><small>Общее время: '" . ($t3-$t1) . "' подготовка: '" . ($prep - $t1 - $skpd) . "', запрос: '" . ($t2-$prep) . "' get skipped: $skpd, core: $skpd0 выборка-отрисовка: " . ($t3-$t2 - $allUsers). '\'</small></div>');

?>
	
<br/>
<div id="comment" align="justify" style="font-size: 80%;">
	Примечания: 1) Команды, взявшие на себя повышенные экологические обязательства, отмечаются знаком <b>ну!</b><br/>
	2) Участники, которые не вышли на старт, и при этом не удалили свою заявку до окончания регистрации, при следующей заявке отмечаются знаком <b>(?!)</b><br/>
</div>


<br/>



