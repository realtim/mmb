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
		while ($row = mysqli_fetch_assoc($result))
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
		mysqli_free_result($result);
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
		$last = array();
		while ($row = mysqli_fetch_assoc($result))
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

		mysqli_free_result($result);
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
         
         
         $sql = " select MAX(r.raid_id) as maxraidid
	         from Raids r 
	         ";
	$maxRaidId = CSql::singleValue($sql, 'maxraidid');
         
        $raidStage = CSql::raidStage($maxRaidId);
        
        $ShowAllRaids = 0;
        if ($raidStage > 6) {

		$ShowAllRaids = mmb_validate($_GET, 'rating', '') == 'all';

		$js = "window.location.search = '?rating' + (this.checked ? '=all' : ''); ";
		print('Отображать все марш-броски (долгая загрузка) <input type="checkbox"  autocomplete="off" name="ShowAllRaids" '.($ShowAllRaids ? 'checked="checked"' : '').' tabindex="'.(++$TabIndex).'"
			title="Отображать все марш-броски" onchange="'. $js .'" />'."\r\n");
        	
        	
        } 
        


	//print('</form>'."\r\n");

	print('<br/><br/>'."\r\n");
	print('<div style="margin-top: 15px; max-width: 1500px;" align="left">Рейтинг по версии slazav: по всем ММБ суммируется отношение времени лидера к времени участника.
	       <br/>Для марш-бросков с несколькими дистанциями это отношение дополнительно умножается на отношение  длины текущей дистанции к максимальной  из длин дистанций.
	        Рейтинг участника марш-броска не рассчитывается в следующих случаях: 1) команда вне зачёта; 2) команда не финишировала; 3) участник сошёл с дистанции.
	        Для марш-бросков до 2012 года сход участников не отражён в данных - можно сообщать о неточностях на общий адрес или в сообщество (ЖЖ)
	        <br/>R6 считается с уценкой каждого предыдущего ММБ на 0.9: последний марш-бросок берётся с весом 1, следующий 0.9, затем 0.9*0.9 и так далее.
	        Рейтинг пересчитывается при пересчете результатов очередного марш-броска.  
	        Знак !! ставится,  если не было участия ни в одном из 8 последних марш-бросков; если участник не вышел на старт или был дисквалифицирован на том марш-броске, где он последний раз участвовал.
	       </div>'."\r\n");
	print('<br/>'."\r\n");

/*


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

*/

	$sql = "select u.user_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name,  
	         COALESCE(u.user_r6, 0.00) as userrank,
	         CASE WHEN COALESCE(u.user_noinvitation, 0) = 1 THEN '!!' ELSE '&nbsp;' END as noinvitation,
	         COALESCE(u.user_rank, 0.00) as slazavrank,
	         0 as userrankcount,
		 0 as distance_id, '&nbsp;' as distance_name,  'Итог' as raid_name,
		 '&nbsp;' as team_num, '&nbsp;' as team_name
	         from  Users u
		 where COALESCE(u.user_hide, 0) = 0
		       and COALESCE(u.user_r6, 0.00) > 0
		 order by user_r6 DESC
		";



CMmbLogger::addRecord('rank query: ');
	$Result = MySqlQuery($sql);

	if ($ShowAllRaids)
	{
		$sqlRaids = "select r.raid_id, r.raid_name, d.distance_name, d.distance_id from Raids r
			        inner join Distances d on r.raid_id = d.raid_id and d.distance_hide = 0
		                order by r.raid_id  desc, d.distance_id desc ";
		$ResultRaids = MySqlQuery($sqlRaids);
		$RowCount = mysqli_num_rows($ResultRaids);
		$TableWidth =  $RowCount*100 + 660;

		$ctp = microtime(true);
		$teamPlaces = new CTeamPlaces();
		CMmbLogger::addInterval('team-places', $ctp);

	} else {
		$TableWidth = 660;
	}

$t5 = microtime(true);
	print('<table class="std" width="'.$TableWidth.'" >'."\r\n");

	print('<tr class="gray head">
                 <td width="100">N строки</td>
                 <td width="350">Пользователь</td>
	         <td width="100" align="center">R6</td>
	         <td width="10" align="center">!!</td>
	         <td width="100" align="center">Рейтинг slazav</td>'."\r\n");

	if ($ShowAllRaids)
	{
	        // Показываем  список ММБ
		$distances = array();
		while ($RowRaids = mysqli_fetch_assoc($ResultRaids))
		{
	                print('<td width="100">'.$RowRaids['raid_name'].' '.$RowRaids['distance_name']."</td>\r\n");
			$distances[] = $RowRaids['distance_id'];
		}
		mysqli_free_result($ResultRaids);
	}
	       
	print("</tr>\r\n");
 	
	
        $LineNum = 0;
        // Сканируем команды
	while ($Row = mysqli_fetch_assoc($Result))
	{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
		$LineNum++;

		print("<tr>\r\n");
		print("<td>$LineNum</td>\r\n");
		print("<td><a href=\"?UserId={$Row['user_id']}\">" . CMmbUI::toHtml($Row['user_name']). "</a></td>\r\n");
		print("<td align=\"center\">{$Row['userrank']}</td><td align=\"center\">{$Row['noinvitation']}</td><td align=\"center\">{$Row['slazavrank']}</td>\r\n");

                if ($ShowAllRaids)
	        {
                        // Показываем  список ММБ
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
                }

                print("</tr>\r\n");
	}
        // Конец цикла по журналу объединений

	mysqli_free_result($Result);

	print("</table>\r\n");

CMmbLogger::addInterval('выборка-отрисовка',  $t5);
?>
		
		<br/>
