<?php
// +++++++++++ Обработчик действий, связанных с результатом +++++++++++++++++++
// Предполагается, что он вызывается после обработчика teamaction,
// поэтому не проверяем сессию и прочее

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

//echo 'action '.$action;

// =============== Изменение/добавление результатов команды ===================
if ($action == "ChangeTeamResult")
{
	//$view = "ViewTeamData";
	$viewmode = "";
	if ($TeamId <= 0) return;

	$sql = "select r.raid_registrationenddate
		from Raids r
			inner join Distances d on r.raid_id = d.raid_id
			inner join Teams t on d.distance_id = t.distance_id
		where t.team_id = ".$TeamId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);

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
		where l.level_hide = 0  and t.team_id = ".$TeamId;
	$rs = MySqlQuery($sql);

	// ================ Цикл обработки данных по этапам
	$statustext = "";
	while ($Row = mysql_fetch_assoc($rs))
	{
		// По этому ключу потом определяем, есть ли уже строчка в TeamLevels или её нужно создать
		$TeamLevelId = $Row['teamlevel_id'];

		// Обрабатываем сход с этапа
		$Index = 'Level'.$Row['level_id'].'_progress';
		if (isset($_POST[$Index])) $TeamLevelProgress = (int)$_POST[$Index]; else $TeamLevelProgress = 0;

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

		// Ставим флаг "Дошла до конца этапа", если у нас есть корректное время финиша команды
		if (($EndYDTs <> "NULL") && !$TeamLevelProgress) $TeamLevelProgress = 2;

		// Получаем отметки о невзятых КП переводим его в строку
               // Штраф и результат считаем для всех этапов отдельно (после записи данных)
		$ArrLen = count(explode(',', $Row['level_pointnames']));
		//$Penalties = explode(',', $Row['level_pointpenalties']);
		$TeamLevelPoints = '';
		$PenaltyTime = 0;
		for ($i = 0; $i < $ArrLen; $i++)
		{
			$Index = 'Level'.$Row['level_id'].'_chk'.$i;
			if (isset($_POST[$Index])) $Point = $_POST[$Index]; else $Point = "";
			if ($Point == 'on') $TeamLevelPoints = $TeamLevelPoints.',1';	else $TeamLevelPoints = $TeamLevelPoints.',0';
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
					teamlevel_points = '".$TeamLevelPoints."',
					teamlevel_progress = '".$TeamLevelProgress."',
					teamlevel_comment = ".$Comment.",
					error_id = NULL
				where teamlevel_id = ".$TeamLevelId."";
		}
		else
		{
			$sql = "insert into TeamLevels (team_id, level_id, teamlevel_begtime, teamlevel_endtime, teamlevel_points, teamlevel_comment, teamlevel_progress)
					values (".$TeamId.", ".$Row['level_id'].", ".$BegYDTs.", ".$EndYDTs.", '".$TeamLevelPoints."', ".$Comment.", ".$TeamLevelProgress.")";
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
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на ввод  результата для точки";
		$alert = 0;
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
		$statustext = 'Не определён ключ команды или ключ точки для результата.';
		$alert = 1;
		$viewsubmode = "ReturnAfterErrorTlp";
		$viewmode = "AddTlp";
		//$viewmode = "EditTlp";
		return;
	}


	
                // год всегда пишем текущий. если надо - можно добавить поле для года

	        $TlpYDTs = "'".$pTlpYear."-".substr(trim($pTlpDate), -2)."-".substr(trim($pTlpDate), 0, 2)." ".substr(trim($pTlpTime), 0, 2).":".substr(trim($pTlpTime), 2, 2).":".substr(trim($pTlpTime), -2)."'";
                
		//echo 	$TlpYDTs;

		 $sql = " select count(*) as countresult 
		          from TeamLevelPoints
		          where team_id = ".$pTeamId."
			        and levelpoint_id = ".$pLevelPointId; 
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $AlreadyExists = (int)$Row['countresult'];
		mysql_free_result($Result);

	   if  ($AlreadyExists > 0)
	   {
			$statustext = 'Результаты по точке уже есть.';
			$alert = 1;
			$viewsubmode = "ReturnAfterErrorTlp";
			$viewmode = "AddTlp";
			return;
           }


             // потом добавить время макс. и мин.
	     
		$sql = "insert into TeamLevelPoints (team_id, levelpoint_id, 
                        device_id,
			teamlevelpoint_datetime, teamlevelpoint_comment, error_id)
			values (".$pTeamId.", ".$pLevelPointId.", 1, 
			        ".$TlpYDTs.", '".$pTlpComment."', ".$pErrorId.")";


               //  echo $sql;
		// При insert должен вернуться послений id - это реализовано в MySqlQuery
		$TeamLevelPointId = MySqlQuery($sql);
		
		if ($TeamLevelPointId <= 0)
		{
			$statustext = 'Ошибка записи нового результата для точки.';
			$alert = 1;
			$viewsubmode = "ReturnAfterErrorTlp";
			$viewmode = "AddTlp";
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
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку результата для точки";
		$alert = 0;
		return;
	}

  

        $pTeamLevelPointId = $_POST['TeamLevelPointId'];


	if ($pTeamLevelPointId <= 0)
	{
		$statustext = 'Не определён ключ результата для точки.';
		$alert = 1;
		$viewsubmode = "ReturnAfterErrorTlp";
		$viewmode = "EditTlp";
		return;
	}



		$pLevelPointId = $_POST['LevelPointId'];
		$pTeamId = $_POST['TeamId'];
     
		$pTlpYear = $_POST['TlpYear'];
                $pTlpDate = $_POST['TlpDate'];
                $pTlpTime = $_POST['TlpTime'];
                $pTlpComment = $_POST['TlpComment'];
		$pErrorId = $_POST['ErrorId'];


		$TlpYDTs = "'".$pTlpYear."-".substr(trim($pTlpDate), -2)."-".substr(trim($pTlpDate), 0, 2)." ".substr(trim($pTlpTime), 0, 2).":".substr(trim($pTlpTime), 2, 2).":".substr(trim($pTlpTime), -2)."'";
         
	
	if ($pTeamId <= 0 or $pLevelPointId <= 0)
	{
		$statustext = 'Не определён ключ команды или ключ точки для результата.';
		$alert = 1;
		$viewsubmode = "ReturnAfterErrorTlp";
		$viewmode = "EditTlp";
		return;
	}

	
	
	
		 $sql = " select count(*) as countresult 
		          from TeamLevelPoints
		          where team_id = ".$pTeamId."
			        and levelpoint_id = ".$pLevelPointId." 
				and teamlevelpoint_id <> ".$pTeamLevelPointId;
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $AlreadyExists = (int)$Row['countresult'];
		mysql_free_result($Result);

	   if  ($AlreadyExists > 0)
	   {
			$statustext = 'Результаты по точке уже есть.';
			$alert = 1;
			$viewsubmode = "ReturnAfterErrorTlp";
			return;
           }



	

		
        $sql = "update TeamLevelPoints  set levelpoint_id = ".$pLevelPointId." 
	                                ,team_id = ".$pTeamId."
	                                ,error_id = ".$pErrorId."
	                                ,teamlevelpoint_comment = '".$pTlpComment."'
	                                ,teamlevelpoint_datetime = ".$TlpYDTs."
	        where teamlevelpoint_id = ".$pTeamLevelPointId;        
			
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
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку результата для точки";
		$alert = 0;
		return;
	}

        $pTeamLevelPointId = $_POST['TeamLevelPointId'];
        $pTeamId = $_POST['TeamId'];

	
	if ($pTeamId <= 0)
	{
		$statustext = 'Не определён ключ команды.';
		$alert = 1;
		$viewsubmode = "ReturnAfterErrorTlp";
		$viewmode = "EditTlp";
		return;
	}



	if ($pTeamLevelPointId <= 0)
	{
		$statustext = 'Не определён ключ результата для точки.';
		$alert = 1;
		$viewsubmode = "ReturnAfterErrorTlp";
		$viewmode = "EditTlp";
		return;
	}
	

        $sql = "delete from TeamLevelPoints where teamlevelpoint_id = ".$pTeamLevelPointId;        
       
			
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
