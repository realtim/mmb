<?php
// +++++++++++ Просмотр рейтинга пользователей ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript))
	return;


class CTeamPlaces
{
	protected $teamPlaces;
	protected $userDistance;

	function __construct()
	{
		$this->retrieveTeamPlaces();
		$this->retrieveUserDistance();
	}

	private function retrieveTeamPlaces()
	{
		$sql = "select team_id, distance_id, TIME_TO_SEC(COALESCE(t.team_result, 0)) as result
				from Teams  t
				where t.team_hide = 0
					and COALESCE(t.team_outofrange, 0) = 0
					and COALESCE(t.team_result, 0) > 0
					and COALESCE(t.team_minlevelpointorderwitherror, 0) = 0
				order by distance_id asc, result asc";

		$result = MySqlQuery($sql);

		$dist = null;
		$this->teamPlaces = array();
		$lastPlace = 0;
		$lastRes = 0;
		$skip = 0;
		while ($row = mysql_fetch_assoc($result))
		{
			if ($dist != $row['distance_id'])
			{
				$lastPlace = 0;
				$skip = 1;
				$lastRes = 0;
			}
			$dist = $row['distance_id'];

			if ($lastRes == $row['result'])
			{
				$skip ++;
			}
			else
			{
				$lastPlace += $skip;
				$skip = 1;
				$lastRes = $row['result'];
			}

			$this->teamPlaces[$row['team_id']] = $lastPlace;
		}
		mysql_free_result($result);
	}

	private function retrieveUserDistance()
	{
		// Показываем  список ММБ
		$sql = "select t.distance_id, t.team_name, t.team_id, t.team_outofrange,
		                    	         lp.levelpoint_name, tu.teamuser_rank, tu.user_id,
									     lp.levelpoint_id
				from Teams t
				inner join Distances d on d.distance_id = t.distance_id and d.distance_hide = 0
				inner join TeamUsers tu  on t.team_id = tu.team_id and tu.teamuser_hide = 0
				left outer join TeamLevelDismiss tld  on tu.teamuser_id = tld.teamuser_id
				left outer join LevelPoints lp  on tld.levelpoint_id = lp.levelpoint_id
				           where   t.team_hide = 0

			        order by tu.user_id asc";

		$result = MySqlQuery($sql);
		$this->userDistance = array();
		$lastUser = null;
		$last = array();
		while ($row = mysql_fetch_assoc($result))
		{
			$uid = $row['user_id'];
			if ($last !== $uid)
				$this->userDistance[$uid] = array();
			$last = $uid;

			$this->userDistance[$uid][$row['distance_id']] = array(
				'team_name' => $row['team_name'],
				'team_id' => $row['team_id'],
				'team_outofrange' => $row['team_outofrange'],
				'levelpoint_name' => $row['levelpoint_name'],
				'teamuser_rank' => $row['teamuser_rank'],
				'levelpoint_id' => $row['levelpoint_id']);
		}

		mysql_free_result($result);
	}

	function GetTeamPlace($teamId)
	{
		return isset($this->teamPlaces[$teamId]) ? $this->teamPlaces[$teamId] : 0;
	}

	function GetUserDistance($userId, $distanceId)
	{
		return isset($this->userDistance[$userId]) && isset($this->userDistance[$userId][$distanceId]) ? $this->userDistance[$userId][$distanceId] : null;
	}
}


	$TabIndex = 0;

	/*print('<form  name="RankUsersForm"  action="'.$MyPHPScript.'" method="post">'."\r\n");
	print('<input type="hidden" name="action" value="">'."\r\n");*/


	$ShowAllRaids = mmb_validate($_GET, 'rating', '') == 'all';

	$js = "window.location.search = '?rating' + (this.checked ? '=all' : ''); ";
	print('Отображать все марш-броски <input type="checkbox"  autocomplete="off" name="ShowAllRaids" '.($ShowAllRaids ? 'checked="checked"' : '').' tabindex="'.(++$TabIndex).'"
		title="Отображать все марш-броски" onchange="'. $js .'" />'."\r\n");


	//print('</form>'."\r\n");

	print('<br/><br/>'."\r\n");
	print('<div style="margin-top: 15px; max-width: 1500px;" align="left">Рейтинг по версии slazav: по всем ММБ суммируется отношение времени лидера к времени участника.
	       <br/>Для марш-бросков с несколькими дистанциями это отношение дополнительно умножается на отношение  длины текущей дистанции к максимальной  из длин дистанций.
	        Рейтинг участника марш-броска не рассчитывается в следующих случаях: 1) команда вне зачёта; 2) команда не финишировала; 3) участник сошёл с дистанции.
	        Для марш-бросков до 2012 года сход участников не отражён в данных - можно сообщать о неточностях на общий адрес или в сообщество (ЖЖ)
	       </div>'."\r\n");
	print('<br/>'."\r\n");


	// Возможно здесь нужно вызвать пересчёт рейтинга по всем ММБ и это не долго
	// RecalcTeamUsersRank(0);
	  

	$sql = "select tu.user_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name,  SUM(tu.teamuser_rank) as userrank,
	                   COUNT(tu.teamuser_id) as userrankcount,
			   0 as distance_id, '&nbsp;' as distance_name,  'Итог' as raid_name,
			   '&nbsp;' as team_num, '&nbsp;' as team_name
	            from  TeamUsers tu
		          inner join Users u
			  on u.user_id = tu.user_id
		          inner join Teams t
			  on t.team_id = tu.team_id
		          inner join Distances d
			  on t.distance_id = d.distance_id
		    where d.distance_hide = 0
			  and t.team_hide = 0
		          and  COALESCE(t.team_outofrange, 0) = 0
		          and  COALESCE(t.team_result, 0) > 0
			  and  COALESCE(tu.teamuser_rank, 0) > 0
		    group by tu.user_id
		    order by userrank DESC
		  ";


/*

		    ) a
		    inner join 
		    (
                    select tu.user_id, u.user_name,  tu.teamuser_rank as userrank, null as  userrankcount,
		           d.distance_id, d.distance_name, r.raid_name,
			   t.team_num, t.team_name
	            from  Raids r
		          inner join Distances d
			  on r.raid_id = d.distance_id 
		          left outer join Teams t
			  on t.distance_id = d.distance_id
			     and t.team_hide = 0 
		             and  COALESCE(t.team_outofrange, 0) = 0
		             and  COALESCE(t.team_result, 0) > 0
                          left outer join TeamUsers tu
			  on t.team_id = tu.team_id			   
		          left outer join Users u
			  on u.user_id = tu.user_id
		    where d.distance_hide = 0 
		    ) b
		    on a.user_id = b.user_id

*/	
	  	//echo 'sql '.$sql;
	$sqTime = 0;
	$gtp = 0;
	$t1 = microtime(true);
	$Result = MySqlQuery($sql);
	$t2 = microtime(true);

	if ($ShowAllRaids)
	{
		$sqlRaids = "select r.raid_id, r.raid_name, d.distance_name, d.distance_id from Raids r
			        inner join Distances d on r.raid_id = d.raid_id and d.distance_hide = 0
		                order by r.raid_id  desc, d.distance_id desc ";
		$t3 = microtime(true);
		$ResultRaids = MySqlQuery($sqlRaids);
		$t4 = microtime(true);
		$RowCount = mysql_num_rows($ResultRaids);
		$TableWidth =  $RowCount*100 + 550;

		$ctp = microtime(true);
		$teamPlaces = new CTeamPlaces();
		$ctp = microtime(true) - $ctp;

	} else {
		$TableWidth = 550;
	}

$t5 = microtime(true);
	print('<table class="std" width="'.$TableWidth.'" >'."\r\n");

	print('<tr class="gray head">
                 <td width="100">N строки</td>
                 <td width="350">Пользователь</td>
	         <td width="100" align="center">Рейтинг</td>'."\r\n");

	if ($ShowAllRaids)
	{
	        // Показываем  список ММБ
		$distances = array();
		while ($RowRaids = mysql_fetch_assoc($ResultRaids))
		{
	                print('<td width="100">'.$RowRaids['raid_name'].' '.$RowRaids['distance_name']."</td>\r\n");
			$distances[] = $RowRaids['distance_id'];
		}
		mysql_free_result($ResultRaids);
	}
	       
	print("</tr>\r\n");
 	
	
        $LineNum = 0;
        // Сканируем команды
	while ($Row = mysql_fetch_assoc($Result))
	{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
		$LineNum++;

		print("<tr>\r\n");
		print("<td>$LineNum</td>\r\n");
		print("<td><a href=\"?UserId={$Row['user_id']}\">" . CMmbUI::toHtml($Row['user_name']). "</a></td>\r\n");
		print("<td align=\"center\">{$Row['userrank']}</td>\r\n");

                if ($ShowAllRaids)
	        {
                        // Показываем  список ММБ
			/*$sqlRaids = "select r.raid_id, d.distance_id, a.team_name, a.team_id, a.team_outofrange,
			                     a.levelpoint_name, a.teamuser_rank,  a.levelpoint_id
			        from Raids r
				     inner join Distances d on r.raid_id = d.raid_id and d.distance_hide = 0
				     left outer join (select t.distance_id, t.team_name, t.team_id, t.team_outofrange,
		                    	         lp.levelpoint_name, tu.teamuser_rank,
									     lp.levelpoint_id
				                   from Teams t
			                           inner join TeamUsers tu  on t.team_id = tu.team_id and tu.teamuser_hide = 0 and tu.user_id = {$Row['user_id']}
			                           left outer join TeamLevelDismiss tld  on tu.teamuser_id = tld.teamuser_id
			                           left outer join LevelPoints lp  on tld.levelpoint_id = lp.levelpoint_id
							  	   where   t.team_hide = 0) a
	                             on d.distance_id = a.distance_id
			        order by r.raid_id  desc,  d.distance_id desc";

		        $t7 = microtime(true);
		        $ResultRaids = MySqlQuery($sqlRaids);
		        $sqTime += microtime(true) - $t7;*/


		        foreach($distances as $distanceId)
			{
				$RowRaids = $teamPlaces->GetUserDistance($Row['user_id'], $distanceId);
				if ($RowRaids !== null && !empty($RowRaids['team_name']))
				{
					$TeamPlace = $teamPlaces->GetTeamPlace($RowRaids['team_id']);

					$LevelPointId = $RowRaids['levelpoint_id'];

					// Есть место команды и нет схода участника
					$TeamPlaceResult = ($TeamPlace > 0 and $LevelPointId == 0) ? ", место $TeamPlace" : '';

					$TeamUserOff = "";
					// Есть место команды, но сход участника
				//	if ($TeamPlace > 0 and $LevelId > 0) $TeamUserOff = ", сход на этапе <b>".$RowRaids['level_name']."</b>";
				//	if ($TeamPlace > 0 and $LevelPointId > 0) $TeamUserOff = ", не явка в точку <b>".$RowRaids['levelpoint_name']."</b>";


					$TeamString = '<a href="?TeamId='.$RowRaids['team_id'].'">'.CMmbUI::toHtml($RowRaids['team_name']).'</a></br>'.$RowRaids['teamuser_rank'].$TeamPlaceResult.$TeamUserOff;
				} else {
					$TeamString = '&nbsp;';
				}

                                print("<td>$TeamString</td>\r\n");
			}
			//mysql_free_result($ResultRaids);
                }

                print("</tr>\r\n");
	}
        // Конец цикла по журналу объединений

	mysql_free_result($Result);

	print("</table>\r\n");

$t6 = microtime(true);

	$sqTime = 0;

	$add = $ShowAllRaids ? "запросы по годам: '$sqTime', teamPlaces: '$ctp', " : '';
	print("<div><small>Общее время: '" . ($t6-$t1) . "' запрос: '" . ($t2-$t1) . "', $add выборка-отрисовка: '" . ($t6-$t5 - $sqTime). '</small></div>');
?>
		
		<br/>
