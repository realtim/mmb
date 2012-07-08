<?php
// +++++++++++ Обработчик действий, связанных с результатом +++++++++++++++++++
// Предполагается, что он вызывается после обработчика teamaction,
// поэтому не проверяем сессию и прочее

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// =============== Показываем страницу администрирования ===================
if ($action == "ViewAdminPage")  {
    // Действие вызывается ссылкой Администрирование
   
	$view = "ViewAdminPage";
	$viewmode = "";

}
// =============== Пересчет результатов ММБ администратором ===================
elseif ($action == 'RecalcRaidResults')
{
	if ($RaidId <= 0)
	{
		$statustext = 'Марш-бросок не найден';
		$view = "";
		return;
	}

	if (!$Administrator) return;

	$sql = 'select team_id
		from Teams t
			inner join Distances d on t.distance_id = d.distance_id
		where t.team_hide = 0 and d.raid_id = '.$RaidId.' 
               order by team_id';
        
        $Result = MySqlQuery($sql);
	// Цикл по всем командам
	while ($Row = mysql_fetch_assoc($Result))
	{
		$RecalcTeamId = $Row['team_id'];
		RecalcTeamLevelDuration($RecalcTeamId);
		RecalcTeamLevelPenalty($RecalcTeamId);
		RecalcTeamResult($RecalcTeamId);
	}
	mysql_free_result($Result);

	$statustext = 'Результаты марш-броска пересчитаны';
	$view = "ViewAdminPage";
}

// =============== Поиск ошибок и обновление штрафного и общего времени =======
elseif ($action == 'FindRaidErrors')
{
	if ($RaidId <= 0)
	{
		$statustext = 'Марш-бросок не найден';
		$view = "";
		return;
	}
	if (!$Administrator && !$Moderator) return;

	$n_Errors = 0;
	$n_Warnings = 0;

	// Обрабатываем в цикле все дистанции марш-броска
        $sql = 'select distance_id, distance_name from Distances where raid_id = '.$RaidId.' order by distance_name';
	$DResult = MySqlQuery($sql);
        while ($Distance = mysql_fetch_assoc($DResult))
	{
		// Получаем список этапов дистанции и их параметры
		$sql = 'select * from Levels where distance_id = '.$Distance['distance_id'].' order by level_order ASC';
		$Result = MySqlQuery($sql);
		$nlevel = 1;
		unset($Levels);
	        while ($Row = mysql_fetch_assoc($Result))
		{
			foreach ($Row as $key => $value) $Levels[$key][$nlevel] = $value;
			$nlevel++;
		}
		mysql_free_result($Result);
		// Переводим все временные парамеры этапов в секунды
		foreach ($Levels['level_begtime'] as &$time) $time = strtotime($time);
		foreach ($Levels['level_maxbegtime'] as &$time) $time = strtotime($time);
		foreach ($Levels['level_minendtime'] as &$time) $time = strtotime($time);
		foreach ($Levels['level_endtime'] as &$time) $time = strtotime($time);

		// Проверяем в цикле все команды, записанные на эту дистанцию
		$sql = 'select team_id, team_progress, team_result from Teams where distance_id = '.$Distance['distance_id'].' and team_hide = 0 order by team_num ASC';
		$Result = MySqlQuery($sql);
	        while ($Team = mysql_fetch_assoc($Result))
		{
			$Error = ValidateTeam($Team, $Levels);
			if ($Error > 0) $n_Errors++;
			elseif ($Error < 0) $n_Warnings++;
		}
		mysql_free_result($Result);
	}
	mysql_free_result($DResult);

	$statustext = 'Найдено '.$n_Errors.' ошибок и '.$n_Warnings.' предупреждений';
	$view = "ViewAdminPage";
}
// =============== Никаких действий не требуется ==============================
else
{
}

// Сохранение флага ошибки в базе
function LogError($teamlevel_id, $error)
{
	$sql = 'update TeamLevels set error_id = '.$error.' where teamlevel_id = '.$teamlevel_id;
	$Result = MySqlQuery($sql);
	return($error);
}

// Проверка конкретной команды
function ValidateTeam($Team, $Levels)
{
	// Получаем список записей результатов из TeamLevels
	foreach ($Levels['level_id'] as $n => $level_id)
	{
		$sql = 'select * from TeamLevels where level_id = '.$level_id.' and team_id = '.$Team['team_id'].' and teamlevel_hide = 0';
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
		if ($begtime && $endtime && (($endtime - $begtime) > 23*3600)) return(LogError($teamlevel['teamlevel_id'], -2));
		// проверяем корректность прогресса на дистанции
		if ($teamlevel['teamlevel_begtime'] && ($teamlevel['teamlevel_progress'] == 0)) return(LogError($teamlevel['teamlevel_id'], 6));
		if ($endtime && ($teamlevel['teamlevel_progress'] <> 2)) return(LogError($teamlevel['teamlevel_id'], 7));
		if (!$endtime && ($teamlevel['teamlevel_progress'] == 2)) return(LogError($teamlevel['teamlevel_id'], 8));
		// проверяем длину списка КП и пересчитываем штраф
		$level_pointpenalties = explode(',', $Levels['level_pointpenalties'][$n]);
		$teamlevel_points = explode(',', $teamlevel['teamlevel_points']);
		if (count($teamlevel_points) <> count($level_pointpenalties)) return(LogError($teamlevel['teamlevel_id'], 9));
		$teamlevel_penalty = 0;
		foreach ($teamlevel_points as $npoint => $point)
		{
			if ((($point == "0") && ((int)$level_pointpenalties[$npoint] > 0)) || (($point == "1") && ((int)$level_pointpenalties[$npoint] < 0)))
				$teamlevel_penalty += (int)$level_pointpenalties[$npoint];
		}
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
	if ($team_result <> $Team['team_result']) die('Ошибка подсчета итогового времени у команды '.$Team['team_id'].': правильное='.$team_result.", в базе=".$Team['team_result']);
	if ($team_progress <> $Team['team_progress']) die('Ошибка подсчета итогового времени у команды '.$Team['team_id'].': правильное='.$team_result.", в базе=".$Team['team_result']);

	// Ошибок в результатах команды не обнаружено
	return(0);
}
?>
