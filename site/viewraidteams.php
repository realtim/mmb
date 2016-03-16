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
		while ($row = mysql_fetch_assoc($UserResult))
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

		mysql_free_result($UserResult);

	return $res;
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
        while ($row = mysql_fetch_assoc($sqlRes))
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

        mysql_free_result($sqlRes);

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
	while ($row = mysql_fetch_assoc($UserResult))
		$res[$row['team_id']] = array('names' => $row['notlevelpoint_name'],
		                              'distance' => $row['distance_id'],
					      'last' => $row['last_done']);

	mysql_free_result($UserResult);

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
				 COALESCE(r.raid_noshowresult, 0) as raid_noshowresult
			  from  Raids r
			  where r.raid_id = $RaidId"; 
            
		$Row = CSql::singleRow($sql);
                $RaidRegisterEndDt = $Row['raid_registrationenddate'];
                $RaidCloseDt = $Row['raid_closedate'];
                $RaidNoShowResult = $Row['raid_noshowresult'];
   
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

        $sql = "select distance_id, distance_name
                    from  Distances
                    where distance_hide = 0 and raid_id = $RaidId
                    order by distance_name";
		//echo 'sql '.$sql;
	$Result = MySqlQuery($sql);
	if (mysql_num_rows($Result) > 1)
	{
		print('<select name="DistanceId" style = "margin-left: 10px; margin-right: 5px;"
                               onchange="DistanceIdChange();"  tabindex="'.(++$TabIndex).'">'."\r\n");

                $selected  =  (0 == $distanceId) ? ' selected' : '';
		print("<option value=\"0\" $selected>дистанцию</option>\r\n");

	        while ($Row = mysql_fetch_assoc($Result))
		{
		  $selected = ($Row['distance_id'] == $distanceId) ? ' selected' : '';
		  print("<option value=\"{$Row['distance_id']}\" $selected>{$Row['distance_name']}</option>\r\n");
		}
		print("</select>\r\n");
	}
	mysql_free_result($Result);

/*
============================= точки ===============================
*/
// 06/11/2015 Закоментировал условие фильтрации по результатам, чтобы ускорить запрос
// вместо этого просто фильтруем по типам  - не показываем только КП
// если потом появятся новые типы точек - нужно проверить условие where

        $sql = "select lp.levelpoint_id, lp.levelpoint_name, d.distance_name,
        		tlp1.teamscount, tlp2.teamuserscount
                from  LevelPoints lp
 	  		inner join Distances d
			on lp.distance_id = d.distance_id
/*
                        inner join
				      (
				       select tlp.levelpoint_id
				       from TeamLevelPoints tlp
				            inner join Teams t on tlp.team_id = t.team_id
				            inner join Distances d on t.distance_id = d.distance_id
					where d.raid_id = $RaidId and $DistanceCondition
					      and  TIME_TO_SEC(COALESCE(tlp.teamlevelpoint_duration, 0)) <> 0
					group by tlp.levelpoint_id
					) a
					  on lp.levelpoint_id = a.levelpoint_id
*/			left outer join
			     (select tlp.levelpoint_id, count(t.team_id) as teamscount
			     from TeamLevelPoints tlp
			          inner join Teams t on t.team_id = tlp.team_id
				  inner join Distances d on t.distance_id = d.distance_id
			     where d.raid_id = $RaidId and $DistanceCondition
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
				group by tlp.levelpoint_id
			     	) tlp2
		     	on lp.levelpoint_id = tlp2.levelpoint_id					  
		where d.raid_id = $RaidId and $DistanceCondition and lp.pointtype_id <> 5
		order by lp.levelpoint_order ";

	//echo 'sql '.$sql;
	$Result = MySqlQuery($sql);
                
	print('<select name="LevelPointId" style = "margin-left: 10px; margin-right: 5px;" 
                       onchange = "LevelPointIdChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
        $levelpointselected =  (0 == $_REQUEST['LevelPointId'] ? 'selected' : '');
	print("<option value = '0' $levelpointselected>точку</option>\r\n");

	if (!isset($_REQUEST['LevelPointId'])) $_REQUEST['LevelPointId'] = "";

        while ($Row = mysql_fetch_assoc($Result))
	{
		$levelpointselected = ($Row['levelpoint_id'] == $_REQUEST['LevelPointId']  ? 'selected' : '');
		print("<option value = '{$Row['levelpoint_id']}' $levelpointselected>{$Row['distance_name']} {$Row['levelpoint_name']} ({$Row['teamscount']}/{$Row['teamuserscount']})</option>\r\n");
	}
	print('</select>'."\r\n");  
	mysql_free_result($Result);		

/*
======================  GPS  ====================
*/
	print('<select name="GPSFilter" style = "margin-left: 10px; margin-right: 5px;"
                       onchange = "GPSChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
	print('<option value="0" '. ($GpsFilter == 0 ? 'selected' : '') ." >не фильтровать по GPS</option>\r\n");
	print('<option value="1" '. ($GpsFilter == 1 ? 'selected' : '') ." >без GPS</option>\r\n");
	print('</select>'."\r\n");  

/*
=====================================
*/

    // Режим отображения результатов
    $ResultViewMode = mmb_validate($_REQUEST, 'ResultViewMode', '');

	print('</div>'."\r\n");
	print('<div align="left" style="margin-top:10px; margin-bottom:10px; font-size: 100%;">'."\r\n");
	print('<a style="font-size:80%; margin-right: 15px;" href="?files&RaidId='.$RaidId.'" title="Список файлов для выбранного выше ММБ">Файлы</a>'."\r\n");
	print('<a style="font-size:80%; margin-right: 15px;" href="javascript: JsonExport();">Json</a> '."\r\n");
	print('</div>'."\r\n");
	print("\r\n</form>\r\n");

    // Информация о дистанции(ях)
    print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");

    $sql = "select  d.distance_name, d.distance_data
            from Distances d
            where d.distance_hide = 0 and  d.raid_id = $RaidId";
				
    $Result = MySqlQuery($sql);

    // теперь цикл обработки данных по дистанциям
    while ($Row = mysql_fetch_assoc($Result))
    {
       	print('<tr><td width="100">'.$Row['distance_name'].'</td>
        <td width="300">'.$Row['distance_data']."</td>\r\n");

        // Если идёт регистрацию время окончания выделяем жирным
        $bStyle = $RaidStage == 1 ? 'style="font-weight: bold;"': '';
        print("<td $bStyle>Регистрация до: $RaidRegisterEndDt</td>\r\n");

	if (!empty($RaidCloseDt))
	{
                print("<td>Протокол закрыт: $RaidCloseDt</td>\r\n");
	}
	print("</tr>\r\n");

    }
		    
    // конец цикла по дистанциям
    mysql_free_result($Result);

    print("</table>\r\n");

	// ============ Вывод списка команд ===========================


	// Выводим список команд
	if  ($OrderType == 'Num')
	{

                  // Сортировка по номеру (в обратном порядке)
		$sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,
 			               t.team_mapscount,  d.distance_name, d.distance_id,
			       			COALESCE(t.team_outofrange, 0) as  team_outofrange
	        	from  Teams t
		    			inner join  Distances d on t.distance_id = d.distance_id
				where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
						and $DistanceCondition and $GpsCondition
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
					       COALESCE(t.team_donelevelpoint, COALESCE(lp.levelpoint_name, '')) as levelpoint_name,
					       COALESCE(t.team_comment, '') as team_comment /*,
					       COALESCE(t.team_skippedlevelpoint, '') as team_skippedlevelpoint */
					from  Teams t
							inner join  Distances d	on t.distance_id = d.distance_id
							inner join  TeamLevelPoints tlp	on t.team_id = tlp.team_id
       						inner join LevelPoints lp on tlp.levelpoint_id = lp.levelpoint_id
				  	where t.team_hide = 0 and $LevelCondition and $GpsCondition
				    order by distance_name, team_outofrange, team_progress desc, team_error asc, tlp.teamlevelpoint_result asc, team_num asc ";

		} else {

			$sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,
			               t.team_mapscount, t.team_maxlevelpointorderdone as team_progress,
			               COALESCE(t.team_minlevelpointorderwitherror, 0) as team_error,
					       d.distance_name, d.distance_id,
			               TIME_FORMAT(t.team_result, '%H:%i') as team_sresult,
					       COALESCE(t.team_outofrange, 0) as  team_outofrange,
					       COALESCE(t.team_donelevelpoint, COALESCE(lp.levelpoint_name, '')) as levelpoint_name,
				    	   COALESCE(t.team_comment, '') as team_comment /*,
						   COALESCE(t.team_skippedlevelpoint, '') as team_skippedlevelpoint */
					  from  Teams t
							inner join  Distances d	on t.distance_id = d.distance_id
							left outer join LevelPoints lp
							on lp.distance_id = t.distance_id
					   			and lp.levelpoint_order = t.team_maxlevelpointorderdone
				  	  where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
				  			and $DistanceCondition and $GpsCondition
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
			       			COALESCE(lp.levelpoint_name, '') as levelpoint_name,
			       			COALESCE(t.team_comment, '') as team_comment
			  		from  Teams t
							inner join
								(
								select tlp.team_id
				  				from TeamLevelPoints tlp
								where error_id is not null
				  				group by tlp.team_id
								) err
							on t.team_id = err.team_id
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
	
    $tdstyle = 'padding: 5px 0px 2px 5px;';
    $tdstyle = '';
    $thstyle = 'border-color: #000000; border-style: solid; border-width: 1px 1px 1px 1px; padding: 5px 0px 2px 5px;';
    $thstyle = '';



	print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");
	print('<tr class = "gray">'."\r\n");

	if ($OrderType == 'Num') {

			$ColumnWidth = 350;
			print('<td width = "50" style = "'.$thstyle.'">Номер</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Команда</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Участники</td>'."\r\n");  
		
	} elseif ($OrderType == 'Place') {

            $ColumnWidth = 350;
			print('<td width = "50" style = "'.$thstyle.'">Номер</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Команда</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Участники</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Точка финиша</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Результат</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Место</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Комментарий</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Не пройдены точки</td>'."\r\n");


	} elseif ($OrderType == 'Errors') {
	
	
            $ColumnWidth = 350;
			print('<td width = "50" style = "'.$thstyle.'">Номер</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Команда</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Участники</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Точка финиша</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Результат</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Место</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Комментарий</td>'."\r\n");  

		
	}
	
	print('</tr>'."\r\n");
	
	$TeamsCount = mysql_num_rows($Result);
    // Меняем логику отображения места
    // Было 1111233345  Стало 1111455589
    $TeamPlace = 0;
    $SamePlaceTeamCount = 1;
    $PredResult = '';
	$PredDistanceId = 0;
		
	while ($Row = mysql_fetch_assoc($Result))
	{
			$TrClass = ($TeamsCount%2 == 0) ? 'yellow': 'green';
			$TeamsCount--;

			$useGps = $Row['team_usegps'] == 1 ? 'gps, ' : '';
			$teamGP = $Row['team_greenpeace'] == 1 ? ', <a title="Нет сломанным унитазам!" href="#comment">ну!</a>' : '';
			$outOfRange = $Row['team_outofrange'] == 1 ? ', Вне зачета!' : '';

 			print('<tr class="'.$TrClass.'">
			       <td style="'.$tdstyle.'"><a name="'.$Row['team_num'].'"></a>'.$Row['team_num'].'</td>
			       <td style="'.$tdstyle.'"><a href="?TeamId='.$Row['team_id'].'&RaidId=' . $RaidId .'">'.
			          CMmbUI::toHtml($Row['team_name'])."</a> ($useGps{$Row['distance_name']}, {$Row['team_mapscount']}$teamGP$outOfRange)</td><td style=\"$tdstyle\">\r\n");


                        // Формируем колонку Участники			
			if (!isset($TeamMembers[$Row['team_id']]))
				die("</td></tr></table> no member records in team '{$Row['team_id']}'");

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
	mysql_free_result($Result);

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



