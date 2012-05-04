<?php
// +++++++++++ Обработчик действий, связанных с результатом +++++++++++++++++++
// Предполагается, что он вызывается после обработчика teamaction,
// поэтому не проверяем сессию и прочее

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// =============== Изменение/добавление результатов команды ===================
if ($action == "ChangeTeamResult")
{
	$view = "ViewTeamData";
	$viewmode = "";
	if ($TeamId <= 0) return;

	$sql = "select r.raid_registrationenddate,
		t.team_moderatorconfirmresult
		from Raids r
			inner join Distances d on r.raid_id = d.raid_id
			inner join Teams t on d.distance_id = t.distance_id
		where t.team_id = ".$TeamId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);
	$ModeratorConfirmResult = $Row['team_moderatorconfirmresult'];

	// Проверка возможности редактировать результаты
	if (!CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage))
	{
		$statustext = 'Изменение результатов команды запрещено';
		return;
	}

	// Получаем информацию об этапах, которые могла проходить команда
	$sql = "select l.level_id, l.level_name, l.level_pointnames, l.level_starttype, l.level_pointpenalties,
		l.level_begtime, l.level_maxbegtime, l.level_minendtime, l.level_endtime,
		tl.teamlevel_begtime, tl.teamlevel_endtime,
		tl.teamlevel_points, tl.teamlevel_progress, tl.teamlevel_penalty,
		tl.teamlevel_id
		from Teams t
			inner join Distances d on t.distance_id = d.distance_id
			inner join Levels l on d.distance_id = l.distance_id
			left outer join Levels l1 on t.level_id = l1.level_id
			left outer join TeamLevels tl on l.level_id = tl.level_id and t.team_id = tl.team_id and tl.teamlevel_hide = 0
		where l.level_order < COALESCE(l1.level_order, l.level_order + 1) and t.team_id = ".$TeamId;
	$rs = MySqlQuery($sql);

	// ================ Цикл обработки данных по этапам
	$statustext = "";
	$TeamLevelProgressPrev = 2;
	while ($Row = mysql_fetch_assoc($rs))
	{
		// По этому ключу потом определяем, есть ли уже строчка в TeamLevels или её нужно создать
		$TeamLevelId = $Row['teamlevel_id'];

		// Обрабатываем сход с этапа
		$Index = 'Level'.$Row['level_id'].'_progress';
		if (isset($_POST[$Index])) $TeamLevelProgress = (int)$_POST[$Index]; else $TeamLevelProgress = 0;
		if ($TeamLevelProgressPrev <> 2) $TeamLevelProgress = 0;
		$TeamLevelProgressPrev = $TeamLevelProgress;

		// Вычисляем время выхода на этап
		$Index = 'Level'.$Row['level_id'].'_begyear';
		if (isset($_POST[$Index])) $BegYear = $_POST[$Index]; else $BegYear = "";
		$Index = 'Level'.$Row['level_id'].'_begdate';
		if (isset($_POST[$Index])) $BegDate = $_POST[$Index]; else $BegDate = "";
		$Index = 'Level'.$Row['level_id'].'_begtime';
		if (isset($_POST[$Index])) $BegTime = $_POST[$Index]; else $BegTime = "";
		$BegYDTs = "'".$BegYear."-".substr(trim($BegDate), -2)."-".substr(trim($BegDate), 0, 2)." ".substr(trim($BegTime), 0, 2).":".substr(trim($BegTime), -2).":00'";
		// Конвертируем в даты php для сравнения
		$BegYDT = strtotime(substr(trim($BegYDTs), 1, -1));
		$BegMinYDT = strtotime(substr(trim($Row['level_begtime']), 1, -1));
		$BegMaxYDT = strtotime(substr(trim($Row['level_maxbegtime']), 1, -1));
		// Обнуляем время старта, если он не в порядке готовности
		if ($Row['level_starttype'] <> 1)
		{
			$BegYDTs = "NULL";
		}
		else
		// Проверка границ старта
		{
			if ($BegYDT < $BegMinYDT or $BegYDT > $BegMaxYDT)
			{
				if ($TeamLevelProgress > 0)
					$statustext = $statustext."</br> старта '".$Row['level_name']."'";
				// Теперь не выходим, а ставим время NULL - вдруг пользовател сохранить хотел КП
				$BegYDTs = "NULL";
			}
		}

		// Вычисляем время финиша на этапе
		$Index = 'Level'.$Row['level_id'].'_endyear';
		$EndYear = $_POST[$Index];
		$Index = 'Level'.$Row['level_id'].'_enddate';
		$EndDate = $_POST[$Index];
		$Index = 'Level'.$Row['level_id'].'_endtime';
		$EndTime = $_POST[$Index];
		$EndYDTs = "'".$EndYear."-".substr(trim($EndDate), -2)."-".substr(trim($EndDate), 0, 2)." ".substr(trim($EndTime), 0, 2).":".substr(trim($EndTime), -2).":00'";
		// Конвертируем в даты php для сравнения
		$EndYDT = strtotime(substr(trim($EndYDTs), 1, -1));
		$EndMinYDT = strtotime(substr(trim($Row['level_minendtime']), 1, -1));
		$EndMaxYDT = strtotime(substr(trim($Row['level_endtime']), 1, -1));
		// Проверка границ финиша
		if ($EndYDT < $EndMinYDT or $EndYDT > $EndMaxYDT)
		{
			$EndYDTs = "NULL";
			if ($TeamLevelProgress > 1)
				$statustext = $statustext."</br> финиша '".$Row['level_name']."'";
		}

		// Получаем отметки о невзятых КП переводим его в строку и считаем штраф
		$ArrLen = count(explode(',', $Row['level_pointnames']));
		$Penalties = explode(',', $Row['level_pointpenalties']);
		$TeamLevelPoints = '';
		$PenaltyTime = 0;
		for ($i = 0; $i < $ArrLen; $i++)
		{
			$Index = 'Level'.$Row['level_id'].'_chk'.$i;
			if (isset($_POST[$Index])) $Point = $_POST[$Index]; else $Point = "";
			if ($Point == 'on') $TeamLevelPoints = $TeamLevelPoints.',1';
			else $TeamLevelPoints = $TeamLevelPoints.',0';
			// Считаем штраф
			if ((($Point <> 'on') && ((int)$Penalties[$i] > 0)) || (($Point == 'on') && ((int)$Penalties[$i] < 0)))
				$PenaltyTime += (int)$Penalties[$i];
		}
		$TeamLevelPoints = substr(trim($TeamLevelPoints), 1);

		// Обрабатываем комментарии
		$Index = 'Level'.$Row['level_id'].'_comment';
		$Comment = $_POST[$Index];
		if ($Comment == "") $Comment = "NULL"; else $Comment = "'" . mysql_real_escape_string($Comment) . "'";


		// Если есть запись в базе - изменяем, нет - вставляем
		if ($TeamLevelId > 0)
		{
			$sql = "update TeamLevels set
					teamlevel_begtime = ".$BegYDTs.",
					teamlevel_endtime = ".$EndYDTs.",
					teamlevel_penalty = ".$PenaltyTime.",
					teamlevel_points = '".$TeamLevelPoints."',
					teamlevel_progress = '".$TeamLevelProgress."',
					teamlevel_comment = ".$Comment."
				where teamlevel_id = ".$TeamLevelId."";
		}
		else
		{
			$sql = "insert into TeamLevels (team_id, level_id, teamlevel_begtime, teamlevel_endtime, teamlevel_penalty, teamlevel_points, teamlevel_comment, teamlevel_progress)
					values (".$TeamId.", ".$Row['level_id'].", ".$BegYDTs.", ".$EndYDTs.", ".$PenaltyTime.", '".$TeamLevelPoints."', ".$Comment."', ".$TeamLevelProgress.")";
		}
		MySqlQuery($sql);
	}
	// Конец цикла по этапам
	mysql_free_result($rs);

	if (trim($statustext) <> "")
		$statustext = "Выход за допустимые границы: ".trim($statustext);

	// Обновляем результат команды
	RecalcTeamResult($TeamId);
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
		RecalcTeamResult($RecalcTeamId);
	}
	mysql_free_result($Result);

	$statustext = 'Результаты марш-броска пересчитаны';
	$view = "";
}

// =============== Никаких действий не требуется ==============================
else
{
}
?>
