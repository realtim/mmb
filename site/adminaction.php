<?php
// +++++++++++ Обработчик действий, связанных с результатом +++++++++++++++++++
// Предполагается, что он вызывается после обработчика teamaction,
// поэтому не проверяем сессию и прочее

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// =============== Показываем страницу администрирования ===================
if ($action == "ViewAdminDataPage")  {
	// Действие вызывается ссылкой Администрирование

	CMmb::setViews('ViewAdminDataPage', '');
}
// =============== Печать карточек ===================
elseif ($action == 'PrintRaidTeams')
{

// Проверяем, что передали идентификатор ММБ
if (!isset($_REQUEST['RaidId'])) $_REQUEST['RaidId'] = "";
$RaidId = $_REQUEST['RaidId'];
if (empty($RaidId))
{
	CMmb::setShortResult('Марш-бросок не найден', '');
	return;
}


  print('Дистанция;Номера карточек<br />'."\n");
  $sql = "select t.team_num, d.distance_name
	  from Teams t
	       inner join Distances d on t.distance_id = d.distance_id
	  where d.distance_hide = 0 and t.team_hide = 0 and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId
	  order by d.distance_name, team_num asc";

  $Result = MySqlQuery($sql);

  $PredDistance = "";
  $CardsArr = "";
  while ($Row = mysql_fetch_assoc($Result))
  {
	if ($Row['distance_name'] <> $PredDistance)
	{
		if ($PredDistance <> "")
		// записываем накопленное
		{
			print("$CardsArr<br />\n");
		}
		$PredDistance = $Row['distance_name'];
		$CardsArr = $PredDistance.';'.$Row['team_num'];
	}
	else
	// копим
	{
		$CardsArr = $CardsArr.','.$Row['team_num'];
	}
  }
  mysql_free_result($Result);

  // записываем накопленное
  print($CardsArr.'<br />'."\n");
  print('====<br />'."\n");


  print('Дистанция;Номер;GPS;Название;Участники;Карты;Сумма<br />'."\n");
  $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name,
	  t.team_mapscount, d.distance_name, d.distance_id
	  from Teams t
		inner join Distances d on t.distance_id = d.distance_id
	  where d.distance_hide = 0 and t.team_hide = 0  and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId
	  order by d.distance_name, team_num asc";

  $Result = MySqlQuery($sql);

  while ($Row = mysql_fetch_assoc($Result))
  {
	$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, u.user_id
		from TeamUsers tu
			inner join Users u on tu.user_id = u.user_id
		where tu.teamuser_hide = 0   and team_id = {$Row['team_id']}
		order by tu.teamuser_id asc";
	$UserResult = MySqlQuery($sql);

	$First = 1;
	while ($UserRow = mysql_fetch_assoc($UserResult))
	{
		if ($First == 1)
		{
			print($Row['distance_name'].';'.$Row['team_num'].';'.($Row['team_usegps'] == 1 ? '+' : '').';'.$Row['team_name'].';'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';'.$Row['team_mapscount'].';'.CalcualteTeamPayment($Row['team_id']).'<br />'."\n");
			$First = 0;
		}
		else
		{
			print(';;;;'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';<br />'."\n");
		}
	}
  
	mysql_free_result($UserResult);
  }

  mysql_free_result($Result);

  print("<br/>\n");

  // Можно не прерывать, но тогда нужно написать обработчик в index, чтобы не выводить дальше ничего
  die();
  return;
}
// =============== Генерация списка карточек в файл  ===================
elseif ($action == 'RaidCardsExport')
{

	if ($RaidId <= 0)
	{
		CMmb::setShortResult('Марш-бросок не найден', '');
		return;
	}

	// рассылать всем может только администратор
	if (!$Administrator) return;

        CMmb::setViews('ViewAdminDataPage', '');


	// Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
	header('Content-Type: text/plain; charset=windows-1251');
	header('Content-Disposition: attachment; filename=raidcards.txt');

	// create a file pointer connected to the output stream
	$output = fopen('php://output', 'w');


	fwrite($output, iconv('UTF-8', 'Windows-1251', 'Дистанция;Номера карточек')."\n");
  
  	$sql = "select t.team_num, d.distance_name
		  from Teams t
	       		inner join Distances d on t.distance_id = d.distance_id
		  where d.distance_hide = 0 and t.team_hide = 0 and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId
		  order by d.distance_name, team_num asc";

  	$Result = MySqlQuery($sql);

  	$PredDistance = "";
  	$CardsArr = "";
  	while ($Row = mysql_fetch_assoc($Result))
  	{
		if ($Row['distance_name'] <> $PredDistance)
		{
			if ($PredDistance <> "")
			// записываем накопленное
			{
				fwrite($output, iconv('UTF-8', 'Windows-1251', $CardsArr)."\n");
			}
			$PredDistance = $Row['distance_name'];
			$CardsArr = $PredDistance.';'.$Row['team_num'];
		}
		else
		// копим
		{
			$CardsArr = $CardsArr.','.$Row['team_num'];
		}
  	}
  	mysql_free_result($Result);

  	// записываем накопленное
  	

  	
//  	fwrite($output, $CardsArr."\n");
	fwrite($output, iconv('UTF-8', 'Windows-1251', $CardsArr)."\n");
//  	fwrite($output, '===='."\n");
	fwrite($output, iconv('UTF-8', 'Windows-1251', '====')."\n");
  //	fwrite($output, 'Дистанция;Номер;GPS;Название;Участники;Карты;Сумма'."\n");
	fwrite($output, iconv('UTF-8', 'Windows-1251', 'Дистанция;Номер;GPS;Название;Участники;Карты;Сумма')."\n");

	  $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name,
		  t.team_mapscount, d.distance_name, d.distance_id
	  		from Teams t
			inner join Distances d on t.distance_id = d.distance_id
		  where d.distance_hide = 0 and t.team_hide = 0  and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId
	  	  order by d.distance_name, team_num asc";

  	$Result = MySqlQuery($sql);

  	while ($Row = mysql_fetch_assoc($Result))
  	{
		$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, u.user_id
			from TeamUsers tu
				inner join Users u on tu.user_id = u.user_id
			where tu.teamuser_hide = 0   and team_id = {$Row['team_id']}
			order by tu.teamuser_id asc";
		$UserResult = MySqlQuery($sql);

		$First = 1;
		while ($UserRow = mysql_fetch_assoc($UserResult))
		{
			if ($First == 1)
			{
				$strtowrite = $Row['distance_name'].';'.$Row['team_num'].';'.($Row['team_usegps'] == 1 ? '+' : '').';'.$Row['team_name'].';'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';'.$Row['team_mapscount'].';'.CalcualteTeamPayment($Row['team_id']);
				fwrite($output, iconv('UTF-8', 'Windows-1251', $strtowrite)."\n");
				$First = 0;
			}
			else
			{
				$strtowrite = ';;;;'.$UserRow['user_name'].' '.$UserRow['user_birthyear'];
				fwrite($output, iconv('UTF-8', 'Windows-1251', $strtowrite)."\n");
			}
		}
  
		mysql_free_result($UserResult);
  	}

  	mysql_free_result($Result);

 	fclose($output);

 	die();
 	return;
}
// =============== Получение дампа ===================
elseif ($action == 'JSON')
{

 if (!$Administrator and !$Moderator)
  {
	  CMmb::setShortResult('Нет прав на экспорт', '');
    return;
   }

   include("json.php");
  
  // Можно не прерывать, но тогда нужно написать обработчик в index, чтобы не выводить дальше ничего
  die();
  return;
}
// =============== Загрузка файла с планшета =======
elseif ($action == 'LoadRaidDataFile')
{
	
        $statustext = ''; 

        // Пока разрешил и модератору         
	if (!$Administrator && !$Moderator) return;

        include('import.php');
       

//	$statustext = $statustext.'</br>'.$n_new.' результатов добавлено, '.$n_updated.' изменено, '.$n_unchanged.' являются дубликатами';
	$view = "ViewAdminDataPage";
}
// =============== Пересчет результатов ММБ администратором ===================
elseif ($action == 'RecalcRaidResults')
{
	if ($RaidId <= 0)
	{
		CMmb::setShortResult('Марш-бросок не найден', '');
		return;
	}

	if (!$Administrator && !$Moderator) return;

	RecalcTeamResultFromTeamLevelPoints($RaidId, 0);
/*
	$sql = 'select team_id
		from Teams t
			inner join Distances d on t.distance_id = d.distance_id
		where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = '.$RaidId.' 
               order by team_id';
        
        $Result = MySqlQuery($sql);
	// Цикл по всем командам
	while ($Row = mysql_fetch_assoc($Result))
	{
		$RecalcTeamId = $Row['team_id'];
		RecalcTeamLevelDuration($RecalcTeamId);
		RecalcTeamLevelPenalty($RecalcTeamId);
		//  10/06/2014 если старцый ММБ. то не обновляем результат
		// Обновляем результат команды
		if (!$OldMmb) {
			RecalcTeamResult($RecalcTeamId);
		}	
	}
	mysql_free_result($Result);

*/
	CMmb::setShortResult('Результаты марш-броска пересчитаны', 'ViewAdminDataPage');
}
// =============== Поиск ошибок и обновление штрафного и общего времени =======
elseif ($action == 'FindRaidErrors')
{
	if ($RaidId <= 0)
	{
		CMmb::setShortResult('Марш-бросок не найден', '');
		return;
	}
	if (!$Administrator && !$Moderator) return;

	$total_errors = FindErrors($RaidId, 0);

	$view = "ViewAdminDataPage";
}
// =============== Показываем страницу модераторов ===================
if ($action == "ViewAdminModeratorsPage")  {
	// Действие вызывается ссылкой Модераторы

	CMmb::setViews('ViewAdminModeratorsPage', '');
}

// =============== Показываем страницу объединения команд ===================
if ($action == "ViewAdminUnionPage")  {
	// Действие вызывается ссылкой Объединение

	CMmb::setViews('ViewAdminUnionPage', '');
}
// =============== Показываем страницу объединения пользователей ===================
if ($action == "ViewUserUnionPage")  {
        // Действие вызывается ссылкой Объединение

	CMmb::setViews('ViewUserUnionPage', '');
}
// =============== Показываем страницу рейтинга пользователей ===================
if ($action == "ViewRankPage")  {
        // Действие вызывается ссылкой Объединение

	CMmb::setViews('ViewRankPage', '');
}
// =============== Показываем страницу логов ===================
else if ($action == "viewLogs")  {
	// todo добавить проверку прав!!!
	CMmb::setViews('viewLogs', '');
}
// =============== Пересчет рейтинга для ММБ ===================
elseif ($action == 'RecalcRaidRank')
{
	if ($RaidId <= 0)
	{
		CMmb::setShortResult('Марш-бросок не найден', '');
		return;
	}

	if (!$Administrator && !$Moderator) return;

	
	$Result = 0;
	$Result =  RecalcTeamUsersRank($RaidId); 

	CMmb::setShortResult('Рейтинг участников марш-броска пересчитан, найдено '.$total_errors.' ошибок', 'ViewAdminDataPage');
}
// =============== Рассылка всем участникам ММБ ===================
elseif ($action == 'SendMessageForAll')
{
	if ($RaidId <= 0)
	{
		CMmb::setShortResult('Марш-бросок не найден', '');
		return;
	}

	// рассылать всем может только администратор
	if (!$Administrator) return;

        CMmb::setViews('ViewAdminDataPage', '');

             $pText = $_POST['MessageText'];
             $pSubject = $_POST['MessageSubject'];
             $pSendType = (int)$_POST['SendForAllTypeId'];

	     if (empty($pSubject) or trim($pSubject) == 'Тема рассылки')
	     {
		CMmb::setError('Укажите тему сообщения.', $view, '');
                return; 
	     }


	     if (empty($pText) or trim($pText) == 'Текст сообщения')
	     {
		CMmb::setError('Укажите текст сообщения.', $view, '');
                return; 
	     }

	     if (empty($pSendType) or $pSendType == 0)
	     {
		CMmb::setError('Укажите тип рассылки.', $view, '');
                return; 
	     }

	$Result = 0;

	$Result = SendMailForAll($RaidId, $pSubject, $pText, $pSendType);
     
        if ($Result == 1)
        {
		CMmb::setShortResult('Рассылка запущена', 'ViewAdminDataPage');
        } else {
        	CMmb::setError('Ошибка при отправке рассылки.', $view, '');
                return; 
        }
}
// =============== Генерация списка участников  ===================
elseif ($action == 'RaidTeamUsersExport')
{

	if ($RaidId <= 0)
	{
		CMmb::setShortResult('Марш-бросок не найден', '');
		return;
	}

	// рассылать всем может только администратор
	if (!$Administrator) return;

        CMmb::setViews('ViewAdminDataPage', '');


	$Sql = "select u.user_name, user_birthyear, 
			COALESCE(u.user_city, '') as user_city,
			COALESCE(u.user_phone, '') as user_phone,
		        t.team_num, t.team_name, t.team_outofrange  
		from Teams t 
			inner join Distances d on t.distance_id = d.distance_id
			inner join TeamUsers tu on tu.team_id = t.team_id
			inner join Users u on tu.user_id = u.user_id
		where t.team_hide = 0 
			and tu.teamuser_hide = 0
			and d.raid_id = $RaidId
		order by user_name
		";
	
	$Result = MySqlQuery($Sql);

	// Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
	header('Content-Type: text/plain; charset=windows-1251');
	header('Content-Disposition: attachment; filename=raidteamusers.txt');

	// create a file pointer connected to the output stream
	$output = fopen('php://output', 'w');

	while ( ( $Row = mysql_fetch_assoc($Result) ) )
	{  
		$strtowrite = trim($Row['user_name']).';'.$Row['user_birthyear'].';'.$Row['user_city'].';'.
				$Row['user_phone'].';'.$Row['team_num'].';'.trim($Row['team_name']).';'.
				$Row['team_outofrange'];
		fwrite($output, iconv('UTF-8', 'Windows-1251', $strtowrite)."\n"); 
		//fputcsv($output, $Row, ';');
	}
	mysql_free_result($Result);

 	fclose($output);
 	die();
 	return;

 	
}
// =============== Никаких действий не требуется ==============================
else
{
}

// Сохранение флага ошибки в базе
function LogError($teamlevel_id, $error)
{
	$sql = "update TeamLevels set error_id = $error where teamlevel_id = $teamlevel_id";
	$Result = MySqlQuery($sql);
	return($error);
}

// Проверка конкретной команды
function ValidateTeam($Team, $Levels)
{
	// Получаем список записей результатов из TeamLevels
	foreach ($Levels['level_id'] as $n => $level_id)
	{
		$sql = "select * from TeamLevels where level_id = $level_id and team_id = {$Team['team_id']} and teamlevel_hide = 0";
		$Result = MySqlQuery($sql);
		if (mysql_num_rows($Result) > 1) die('Несколько записей на один этап для команды '.$Team['team_id']);
	        $Row = mysql_fetch_assoc($Result);
		if ($Row) $TeamLevels[$n] = $Row;
		mysql_free_result($Result);
	}
	// Проверяем все этапы, о которых есть записи в таблицах
	$team_result = 0;
	$team_progress = 0;
	$finished = 1;
	if (isset($TeamLevels)) foreach ($TeamLevels as $n => $teamlevel)
	{
		$begtime = strtotime($teamlevel['teamlevel_begtime']);
		$endtime = strtotime($teamlevel['teamlevel_endtime']);
		// проверяем абсолютную корректность времени старта и финиша
		if ($begtime && (($begtime < $Levels['level_begtime'][$n]) || ($begtime > $Levels['level_maxbegtime'][$n]))) return(LogError($teamlevel['teamlevel_id'], 1));
		if (!$begtime && ($Levels['level_starttype'][$n] == 1)) return(LogError($teamlevel['teamlevel_id'], 2));
		if ($begtime && (($Levels['level_starttype'][$n] == 2) || ($Levels['level_starttype'][$n] == 3))) return(LogError($teamlevel['teamlevel_id'], 3));
		if ($endtime && (($endtime < $Levels['level_minendtime'][$n]) || ($endtime > $Levels['level_endtime'][$n]))) return(LogError($teamlevel['teamlevel_id'], 4));
		// вычисляем время старта, если он общий или в момент финиша на пред.этапе
		if ($Levels['level_starttype'][$n] == 2) $begtime = $Levels['level_begtime'][$n];
		if (($Levels['level_starttype'][$n] == 3) && isset($TeamLevels[$n - 1])) $begtime = strtotime($TeamLevels[$n - 1]['teamlevel_endtime']);
		// сравниваем время старта и финиша
		if ($begtime && $endtime && ($begtime >= $endtime)) return(LogError($teamlevel['teamlevel_id'], 5));
		if ($begtime && $endtime && (($endtime - $begtime) < 3*3600)) return(LogError($teamlevel['teamlevel_id'], -1));
		/* if ($begtime && $endtime && (($endtime - $begtime) > 23*3600)) return(LogError($teamlevel['teamlevel_id'], -2)); */
		// проверяем корректность прогресса на дистанции
		if ($teamlevel['teamlevel_begtime'] && ($teamlevel['teamlevel_progress'] == 0)) return(LogError($teamlevel['teamlevel_id'], 6));
		if ($endtime && ($teamlevel['teamlevel_progress'] <> 2)) return(LogError($teamlevel['teamlevel_id'], 7));
		if (!$endtime && ($teamlevel['teamlevel_progress'] == 2)) return(LogError($teamlevel['teamlevel_id'], 8));
		// проверяем наличие времени финиша и списка КП у финишировавшей команды
		if (($teamlevel['teamlevel_endtime'] == "") && !(strpos($teamlevel['teamlevel_points'], "1") === false)) return(LogError($teamlevel['teamlevel_id'], 12));
		if (($teamlevel['teamlevel_endtime'] != "") && ($teamlevel['teamlevel_points'] == "")) return(LogError($teamlevel['teamlevel_id'], 13));
		// проверяем длину списка КП и пересчитываем штраф
		$level_pointpenalties = explode(',', $Levels['level_pointpenalties'][$n]);
		$level_discountpoints = explode(',', $Levels['level_discountpoints'][$n]);
		if ($teamlevel['teamlevel_points'] == "")
		{
			unset($teamlevel_points);
			foreach ($level_pointpenalties as $penalty)
				$teamlevel_points[] = "0";
		}
		else
			$teamlevel_points = explode(',', $teamlevel['teamlevel_points']);
		if (count($teamlevel_points) <> count($level_pointpenalties)) return(LogError($teamlevel['teamlevel_id'], 9));
		$teamlevel_penalty = 0;
		$teamlevel_selectpenalty = 0;
		foreach ($teamlevel_points as $npoint => $point)
		{

                        if (empty($level_pointpenalties[$npoint]))                      
			{
			   $NowLevelPointPenalty = 0;
			} else {
			   $NowLevelPointPenalty = (int)$level_pointpenalties[$npoint];
			}
     
			if ((($point == "0") && ($NowLevelPointPenalty > 0)) || (($point == "1") && ($NowLevelPointPenalty < 0)))
			{
				if (!empty($level_discountpoints[$npoint]))
					$teamlevel_selectpenalty += $NowLevelPointPenalty;
				else
					$teamlevel_penalty += $NowLevelPointPenalty;
			}
		}
		if ($Levels['level_discount'][$n])
		{
			$teamlevel_selectpenalty -= $Levels['level_discount'][$n];
			if ($teamlevel_selectpenalty < 0) $teamlevel_selectpenalty = 0;
		}
		$teamlevel_penalty += $teamlevel_selectpenalty;
		if ($teamlevel_penalty <> $teamlevel['teamlevel_penalty']) return(LogError($teamlevel['teamlevel_id'], 10));
		// пока считаем, что ошибок на этапе нет
		LogError($teamlevel['teamlevel_id'], 0);
		// добавляем результаты этапа к общему результату
		if ($begtime && $endtime) $team_result += ($endtime - $begtime) / 60;
		$team_result += $teamlevel_penalty;
		$team_progress += (int)$teamlevel['teamlevel_progress'];
		if ($teamlevel['teamlevel_progress'] <> 2) $finished = 0;
	}
	// Считаем, что на отсутствующие в базе записи о прохождении этапов команда не выходила
	foreach ($Levels['level_id'] as $n => $level_id)
		if (!isset($TeamLevels[$n]))
		{
			$TeamLevels[$n]['teamlevel_progress'] = 0;
			$finished = 0;
		}
	// Смотрим, чтобы после схода команда опять не появлялась на дистанции
	foreach ($TeamLevels as $n => $teamlevel)
		if ($n > 1)
		{
			if ($teamlevel['teamlevel_progress'] > $TeamLevels[$n - 1]['teamlevel_progress']) return(LogError($teamlevel['teamlevel_id'], 11));
			if (($teamlevel['teamlevel_progress'] == 1) && ($TeamLevels[$n - 1]['teamlevel_progress'] == 1)) return(LogError($teamlevel['teamlevel_id'], 11));
		}
	// Сверяем итоговые прогресс и результат команды
	if (!$finished) $team_result = "";
	else $team_result = sprintf("%d:%02d:00", $team_result / 60, $team_result % 60);
	if ($team_result <> $Team['team_result']) echo "Ошибка подсчета итогового времени у команды {$Team['team_id']}: правильное=$team_result, в базе={$Team['team_result']}<br/>";
	if ($team_progress <> $Team['team_progress']) echo "Ошибка подсчета степени продвижения по дистанции у команды {$Team['team_id']}: правильное=$team_result, в базе={$Team['team_result']}<br/>";

	// Ошибок в результатах команды не обнаружено
	return(0);
}
?>
