<?php
// +++++++++++ Обработчик действий, связанных с результатом +++++++++++++++++++
// Предполагается, что он вызывается после обработчика teamaction,
// поэтому не проверяем сессию и прочее

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;


function teamAddError($error)
{
	global $view, $viewmode;
	CMmb::setError($error, $view, 'ReturnAfterErrorTlp');
	$viewmode = "AddTlp";
}

function teamEditError($error)
{
	global $view, $viewmode;
	CMmb::setError($error, $view, 'ReturnAfterErrorTlp');
	$viewmode = "EditTlp";
}

//echo 'action '.$action;

// =============== Изменение/добавление результатов команды ===================
if ($action == "ChangeTeamResult")
{
	//$view = "ViewTeamData";
	$viewmode = "";
	if ($TeamId <= 0) return;

	// Проверка возможности редактировать результаты
	if (!CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange))
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
			left outer join TeamLevels tl on l.level_id = tl.level_id and t.team_id = tl.team_id and tl.teamlevel_hide = 0
		where l.level_hide = 0  and t.team_id = $TeamId";
	$rs = MySqlQuery($sql);

	// ================ Цикл обработки данных по этапам
	$statustext = "";
	while ($Row = mysql_fetch_assoc($rs))
	{
		// По этому ключу потом определяем, есть ли уже строчка в TeamLevels или её нужно создать
		$TeamLevelId = $Row['teamlevel_id'];

		$levelPfx = 'Level'.$Row['level_id'];

		// Обрабатываем сход с этапа
		$TeamLevelProgress = mmb_validateInt($_POST, $levelPfx.'_progress', 0);

		// Вычисляем время выхода на этап
		$BegYear = mmb_validate($_POST, $levelPfx.'_begyear', '');
		$BegDate = mmb_validate($_POST, $levelPfx.'_begdate', '');
		$BegTime = mmb_validate($_POST, $levelPfx.'_begtime', '');
		$BegYDTs = CSql::timeString($BegYear, $BegDate, $BegTime);

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
					$statustext = $statustext."</br> старта '{$Row['level_name']}'";
				// Теперь не выходим, а ставим время NULL - вдруг пользователь сохранить хотел КП
				$BegYDTs = "NULL";
			}
		}

		// Вычисляем время финиша на этапе
		$EndYear = $_POST[$levelPfx.'_endyear'];
		$EndDate = $_POST[$levelPfx.'_enddate'];
		$EndTime = $_POST[$levelPfx.'_endtime'];
		$EndYDTs = CSql::timeString($EndYear, $EndDate, $EndTime);

		// Конвертируем в даты php для сравнения
		$EndYDT = strtotime(substr(trim($EndYDTs), 1, -1));
		$EndMinYDT = strtotime(substr(trim($Row['level_minendtime']), 1, -1));
		$EndMaxYDT = strtotime(substr(trim($Row['level_endtime']), 1, -1));
		// Проверка границ финиша
		if ($EndYDT < $EndMinYDT or $EndYDT > $EndMaxYDT)
		{
			$EndYDTs = "NULL";
			if ($TeamLevelProgress > 1)
				$statustext = $statustext."<br/> финиша '{$Row['level_name']}'";
		}

		// Ставим флаг "Дошла до конца этапа", если у нас есть корректное время финиша команды
		if (($EndYDTs <> "NULL") && !$TeamLevelProgress) $TeamLevelProgress = 2;

		// Получаем отметки о невзятых КП переводим его в строку
               // Штраф и результат считаем для всех этапов отдельно (после записи данных)
		$ArrLen = count(explode(',', $Row['level_pointnames']));
		//$Penalties = explode(',', $Row['level_pointpenalties']);
		$TeamLevelPoints = '';
		$PenaltyTime = 0;
		$points = array();
		for ($i = 0; $i < $ArrLen; $i++)
			$points[] = mmb_isOn($_POST, $levelPfx.'_chk'.$i);

		$TeamLevelPoints = implode(", ", $points);

		// Обрабатываем комментарии
		$Comment = $_POST[$levelPfx.'_comment'];
		if ($Comment == "") $Comment = "NULL"; else $Comment = "'" . mysql_real_escape_string($Comment) . "'";


		// Если есть запись в базе - изменяем, нет - вставляем
		if ($TeamLevelId > 0)
		{
			$sql = "update TeamLevels set
					teamlevel_begtime = $BegYDTs,
					teamlevel_endtime = $EndYDTs,
					teamlevel_points = '$TeamLevelPoints',
					teamlevel_progress = '$TeamLevelProgress',
					teamlevel_comment = $Comment,
					error_id = NULL
				where teamlevel_id = $TeamLevelId";
		}
		else
		{
			$sql = "insert into TeamLevels (team_id, level_id, teamlevel_begtime, teamlevel_endtime, teamlevel_points, teamlevel_comment, teamlevel_progress)
					values ($TeamId, {$Row['level_id']}, $BegYDTs, $EndYDTs, '$TeamLevelPoints', $Comment, $TeamLevelProgress)";
		}
		MySqlQuery($sql);
	}
	// Конец цикла по этапам
	mysql_free_result($rs);

	if (trim($statustext) <> "")
		$statustext = "Выход за допустимые границы: ".trim($statustext);

	// Пересчет врмени нахождения команды на этапах
	RecalcTeamLevelDuration($TeamId);
	// Пересчет штрафов 
	RecalcTeamLevelPenalty($TeamId);

        //  10/06/2014 если старцый ММБ. то не обновляем результат
	// Обновляем результат команды
	if (!$OldMmb) {
		RecalcTeamResult($TeamId);
	}

        //Если передали альтернативную страницу, на которую переходить (пока только одна возможность - на список команд)
	$view = $_POST['view'];
	if (empty($view)) $view = "ViewTeamData";


}
// ============ Добавить точку  =============
elseif ($action == 'AddTlp')
{
	//$view = "ViewLevelPoints";
	//$viewmode = "AddTlp";


	// Общая проверка возможности редактирования
//	if (!$Administrator && !$Moderator)

	if (!CRights::canEditPointResult($UserId, $RaidId))
	{
		CMmb::setMessage('Нет прав на ввод  результата для точки');
		return;
	}




	$pTeamId = $_POST['TeamId'];
	$pLevelPointId = $_POST['LevelPointId'];
        //$pPointName = $_POST['PointName'];

        $pTlpYear = $_POST['TlpYear'];
        $pTlpDate = $_POST['TlpDate'];
        $pTlpTime = $_POST['TlpTime'];
        $pTlpComment = $_POST['TlpComment'];
	$pErrorId = $_POST['ErrorId'];


         // тут по-хорошему нужны проверки
	if ($pTeamId <= 0 or $pLevelPointId <= 0)
	{
		teamAddError('Не определён ключ команды или ключ точки для результата.');
		//$viewmode = "EditTlp";
		return;
	}


	
        // год всегда пишем текущий. если надо - можно добавить поле для года

	$TlpYDTs = CSql::timeString($pTlpYear, $pTlpDate, $pTlpTime, false);

	// Если день и время пустые, то и год пустой считаем
	if ((int)$pTlpDate == 0 and (int)$pTlpTime == 0)
	{
		$TlpYDTs = "'0000-00-00 00:00:00'";
	}


                
	//echo 	$TlpYDTs;

        $sql = " select count(*) as countresult
	          from TeamLevelPoints
	          where team_id = $pTeamId
		        and levelpoint_id = $pLevelPointId";

	if  (((int) CSql::singleValue($sql, 'countresult')) > 0)
	{
		teamAddError('Результаты по точке уже есть.');
		return;
	}


        // потом добавить время макс. и мин.
	     
	$sql = "insert into TeamLevelPoints (team_id, levelpoint_id,
                device_id,
		teamlevelpoint_datetime, teamlevelpoint_comment, error_id)
		values ($pTeamId, $pLevelPointId, 1,
		        $TlpYDTs, '$pTlpComment', $pErrorId)";


	//  echo $sql;
	// При insert должен вернуться послений id - это реализовано в MySqlQuery
	$TeamLevelPointId = MySqlQuery($sql);

	if ($TeamLevelPointId <= 0)
	{
		teamAddError('Ошибка записи нового результата для точки.');
		return;
	}
	
/*
	 $statustext = CheckLevelPoints($DistanceId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }
*/

         RecalcTeamResultFromTeamLevelPoints(0,  $pTeamId);


	//$view = $_POST['view'];
	//if (empty($view)) $view = "ViewTeamData";

 }
 elseif ($action == "TlpInfo")  
 {
    // Действие вызывается кнопокй "Править" в таблице точек

	$view = $_POST['view'];
	if (empty($view)) $view = "ViewTeamData";
	$viewmode = "EditTlp";
 }
 // ============ Правка точки  =============
elseif ($action == 'ChangeTlp')
{
//	if (!$Administrator && !$Moderator)
	if (!CRights::canEditPointResult(UserId, $RaidId))
	{
		CMmb::setMessage('Нет прав на правку результата для точки');
		return;
	}

  

        $pTeamLevelPointId = $_POST['TeamLevelPointId'];


	if ($pTeamLevelPointId <= 0)
	{
		teamEditError('Не определён ключ результата для точки.');
		return;
	}



	$pLevelPointId = $_POST['LevelPointId'];
	$pTeamId = $_POST['TeamId'];

	$pTlpYear = $_POST['TlpYear'];
        $pTlpDate = $_POST['TlpDate'];
        $pTlpTime = $_POST['TlpTime'];
        $pTlpComment = $_POST['TlpComment'];
	$pErrorId = $_POST['ErrorId'];


	$TlpYDTs = CSql::timeString($pTlpYear, $pTlpDate, $pTlpTime, false);

	// Если день и время пустые, то и год пустой считаем
	if ((int)$pTlpDate == 0 and (int)$pTlpTime == 0)
	{
		$TlpYDTs = "'0000-00-00 00:00:00'";
	}

	
	if ($pTeamId <= 0 or $pLevelPointId <= 0)
	{
		teamEditError('Не определён ключ команды или ключ точки для результата.');
		return;
	}

	
	
	
	$sql = " select count(*) as countresult
	          from TeamLevelPoints
	          where team_id = $pTeamId
		        and levelpoint_id = $pLevelPointId
			and teamlevelpoint_id <> $pTeamLevelPointId";
                
	if  (CSql::singleValue($sql, 'countresult') > 0)
	{
		CMmb::setErrorSm('Результаты по точке уже есть.', 'ReturnAfterErrorTlp');
		return;
	}



	

		
        $sql = "update TeamLevelPoints  set levelpoint_id = $pLevelPointId
	                                ,team_id = $pTeamId
	                                ,error_id = $pErrorId
	                                ,teamlevelpoint_comment = '$pTlpComment'
	                                ,teamlevelpoint_datetime = $TlpYDTs
	        where teamlevelpoint_id = $pTeamLevelPointId";
			
	//echo $sql;
			
	 MySqlQuery($sql);
    

         RecalcTeamResultFromTeamLevelPoints(0,  $pTeamId);
//         RecalcTeamResultFromTeamLevelPoints(25, 0);
    
    /*   
	 $statustext = CheckLevelPoints($DistanceId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }
	*/	

}
// ============ Удаление точки  =============
elseif ($action == 'HideTlp')
{
//	if (!$Administrator && !$Moderator)
	if (!CRights::canEditPointResult($UserId, $RaidId))
	{
		CMmb::setMessage('Нет прав на правку результата для точки');
		return;
	}

        $pTeamLevelPointId = $_POST['TeamLevelPointId'];
        $pTeamId = $_POST['TeamId'];

	
	if ($pTeamId <= 0)
	{
		teamEditError('Не определён ключ команды.');
		return;
	}



	if ($pTeamLevelPointId <= 0)
	{
		teamEditError('Не определён ключ результата для точки.');
		return;
	}
	

        $sql = "delete from TeamLevelPoints where teamlevelpoint_id = $pTeamLevelPointId";
       
			
	 MySqlQuery($sql);


         RecalcTeamResultFromTeamLevelPoints(0,  $pTeamId);


/*
	 $statustext = CheckLevelPoints($DistanceId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }
*/

		

}
else
{
}

?>
