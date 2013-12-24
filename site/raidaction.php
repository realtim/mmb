<?php
// +++++++++++ Обработчик действий, связанных с марш-бросокм +++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// ============ Обработка возможности создания нового марш-броска команды =====================
if ($action == "RegisterNewRaid")
{
	$view = "ViewRaidData";
	$viewmode = "Add";

	// Проверка возможности создать сарш бросок
	if (!$Administrator)
	{
		$statustext = "Нет прав на создание марш-броска.";
		$alert = 0;
		return;
	}


}
// ============ Информация о ММБ (форма) =============
elseif ($action == 'RaidInfo')
{
	if ($RaidId <= 0)
	{
		$statustext = 'Id ММБ не указан';
		$alert = 1;
		return;
	}
	$view = "ViewRaidData";
	$viewmode = "";
}
// ============ Изменение данных ММБ или запись нового ММБ (реакция на отправку формы) =============
elseif ($action == 'RaidChangeData' or $action == "AddRaid")
{
	if ($action == "AddRaid") $viewmode = "Add"; else $viewmode = "";
	$view = "ViewRaidData";
	// Общая проверка возможности редактирования
	if (!$Administrator)
	{
		$statustext = "Нет прав на ввод или правку марш-броска";
		$alert = 0;
		return;
	}

	$pRaidName = $_POST['RaidName'];
	$pRaidPeriod = $_POST['RaidPeriod'];
	$pRaidRegistrationEndDate = $_POST['RaidRegistrationEndDate'];
	$pClearRaidRegistrationEndDate = (isset($_POST['ClearRaidRegistrationEndDate']) && ($_POST['ClearRaidRegistrationEndDate'] == 'on')) ? 1 : 0;
	//$pRaidLogoLink = $_POST['RaidLogoLink'];
	//$pRaidRulesLink = $_POST['RaidRulesLink'];
	$pRaidStartPointName = $_POST['RaidStartPointName'];
	//$pRaidStartLink = $_POST['RaidStartLink'];
	$pRaidFinishPointName = $_POST['RaidFinishPointName'];
	$pRaidCloseDate = $_POST['RaidCloseDate'];
	$pClearRaidCloseDate = (isset($_POST['ClearRaidCloseDate']) && ($_POST['ClearRaidCloseDate'] == 'on')) ? 1 : 0;
	//$pRaidZnLink = $_POST['RaidZnLink'];
        $pRaidDistancesCount = (int)$_POST['RaidDistancesCount'];
        $pRaidNoShowResult = (isset($_POST['RaidNoShowResult']) && ($_POST['RaidNoShowResult'] == 'on')) ? 1 : 0;
	$pRaidFilePrefix = $_POST['RaidFilePrefix'];
        $pRaidReadOnlyHoursBeforeStart = (int)$_POST['RaidReadOnlyHoursBeforeStart'];

/*
        // Обрабатываем зхагрузку файла эмблемы
        if (!empty($_FILES['logofile']['name']) and ($_FILES['logofile']['size'] > 0))
	{
           if  (substr(trim($_FILES['logofile']['type']), 0, 6) != 'image/') 
	   {
			$statustext = 'Недопустимый тип файла.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
	   }
            
	   $UploadFile = $MyStoreFileLink . basename($_FILES['logofile']['name']);

	   if (move_uploaded_file($_FILES['logofile']['tmp_name'], $UploadFile))
	   {
		// Успешно загрузили файл
		$pRaidLogoLink = $MyStoreHttpLink . basename($_FILES['logofile']['name']);
	   } else {
			$statustext = 'Ошибка загрузки файла с эмблемой ММБ.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }
           // Конец проверки на успешность загрузки
	}
        // Конец проверки на указание в форме файла для загрузки эмблемы
	*/
	// Проверка на пустое название 
	if  (empty($pRaidName)) 
	{
			$statustext = 'Пустое название ММБ.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
	}
        // Конец проверки на пустое название 
	
	// Проверка на число дистанций
	if  ($pRaidDistancesCount <= 0) 
	{
			$statustext = 'Число дистанций длолжно быть положительным.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
	}
        // Конец проверки на число дистанций
	
/*

        // Обрабатываем зхагрузку файла положения
        if (!empty($_FILES['rulesfile']['name']) and ($_FILES['rulesfile']['size'] > 0))
	{

           if  (substr(trim($_FILES['rulesfile']['type']), 0, 9) != 'text/html') 
	   {
			$statustext = 'Недопустимый тип файла.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
	   }
            
	   $UploadFile = $MyStoreFileLink . basename($_FILES['rulesfile']['name']);

	   if (move_uploaded_file($_FILES['rulesfile']['tmp_name'], $UploadFile))
	   {
		// Успешно загрузили файл
		$pRaidRulesLink = $MyStoreHttpLink . basename($_FILES['rulesfile']['name']);
	   } else {
			$statustext = 'Ошибка загрузки файла с положением ММБ.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }
           // Конец проверки на успешность загрузки
	}
        // Конец проверки на указание в форме файла для загрузки положэние

	
*/
		
	// Добавляем/изменяем марш-бросок в базе

	if ($action == "AddRaid")
	// Новый ММБ
	{


		// Проверяем, нет ли уже ММБ с таким названием
		$sql = "select count(*) as resultcount
			from Raids r
			where  trim(raid_name) = '".$pRaidName."'";

		$rs = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($rs);
		mysql_free_result($rs);
		if ($Row['resultcount'] > 0)
		{
			$statustext = "Уже есть ММБ с таким названием.";
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
                // Конец проверки на  повтор имени

/*
		$sql = "insert into Raids (raid_name, raid_period, raid_registrationenddate, 
		                           raid_logolink, raid_ruleslink, raid_startpoint, 
					   raid_startlink, raid_finishpoint, raid_closedate,
					   raid_znlink, raid_noshowresult, raid_fileprefix,
					   raid_readonlyhoursbeforestart
					   )
			values (trim('".$pRaidName."'), trim('".$pRaidPeriod."')  ";
		
		if ($pClearRaidRegistrationEndDate == 1 or empty($pRaidRegistrationEndDate)) 
		{	
			$sql.=  ", NULL ";
		} else {
			$sql.=  ", '".$pRaidRegistrationEndDate."'";
		}
		$sql.= ", trim('".$pRaidLogoLink."') ";
		$sql.= ", trim('".$pRaidRulesLink."') ";
		$sql.= ", trim('".$pRaidStartPointName."') ";
		$sql.= ", trim('".$pRaidStartLink."') ";
		$sql.= ", trim('".$pRaidFinishPointName."') ";

		if ($pClearRaidCloseDate == 1 or empty($pRaidCloseDate)) 
		{	
			$sql.=  ", NULL ";
		} else {
			$sql.=  ", '".$pRaidCloseDate."'";
		}
	
		$sql.= ", trim('".$pRaidZnLink."') ";
		$sql.= ", ".$pRaidNoShowResult;
		$sql.= ", trim('".$pRaidFilePrefix."') " ;
		$sql.= ", ".$pRaidReadOnlyHoursBeforeStart;
		$sql.= ")";


*/


                $sql = "insert into Raids (raid_name, raid_period, raid_registrationenddate, 
		                           raid_startpoint, raid_finishpoint, raid_closedate,
					   raid_noshowresult, raid_fileprefix,
					   raid_readonlyhoursbeforestart
					   )
			values (trim('".$pRaidName."'), trim('".$pRaidPeriod."')  ";
		
		if ($pClearRaidRegistrationEndDate == 1 or empty($pRaidRegistrationEndDate)) 
		{	
			$sql.=  ", NULL ";
		} else {
			$sql.=  ", '".$pRaidRegistrationEndDate."'";
		}
		$sql.= ", trim('".$pRaidStartPointName."') ";
		$sql.= ", trim('".$pRaidFinishPointName."') ";

		if ($pClearRaidCloseDate == 1 or empty($pRaidCloseDate)) 
		{	
			$sql.=  ", NULL ";
		} else {
			$sql.=  ", '".$pRaidCloseDate."'";
		}
	
		$sql.= ", ".$pRaidNoShowResult;
		$sql.= ", trim('".$pRaidFilePrefix."') " ;
		$sql.= ", ".$pRaidReadOnlyHoursBeforeStart;
		$sql.= ")";


		// При insert должен вернуться послений id - это реализовано в MySqlQuery

              //  echo $sql;

		$RaidId = MySqlQuery($sql);
		GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);

		if ($RaidId <= 0)
		{
			$statustext = 'Ошибка записи нового ММБ.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		} else {
		
		    // Добавляем дистанции
                    if  ($pRaidDistancesCount == 1)
		    {
				$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink, distance_hide) 
				        values (".$RaidId.", 'Общая', '','', 0)";
		    
				$rs = MySqlQuery($sql);
		    
		    } else {

			$AddDistanceCounter =  0;
			while ($AddDistanceCounter < $pRaidDistancesCount) 
			{
				$AddDistanceCounter++;
			
				$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink, distance_hide) 
				        values (".$RaidId.", 'Дистанция".$AddDistanceCounter."', '','', 0)";

				// echo $sql;
				$rs = MySqlQuery($sql);
		    
		         }
                     }
		    // Конец добавления дистанций 
		}
                // Конец проверки успешной записи ММБ

//		$sql = "insert into TeamUsers (team_id, user_id) values (".$TeamId.", ".$NewUserId.")";
//		MySqlQuery($sql);
		// Теперь нужно открыть на просмотр
		$viewmode = "";
	}
	else
	// Изменения в уже существующем ММБ
	{


		// Проверяем, что текущее чимсло дистанций не больше, чем указано
		$sql = "select count(*) as resultcount
			from Distances d
			where distance_hide = 0 and raid_id = ".$RaidId; 

		$rs = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($rs);
		mysql_free_result($rs);
		$NowDistancesCounter = $Row['resultcount'];
		if ($NowDistancesCounter > $pRaidDistancesCount)
		{
			$statustext = "Дистанций не может быть меньше, чем уже создано.";
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;

                } else {
                    // Добавляем дистанции
                    $AddDistanceCounter =  $NowDistancesCounter;
                    while ($AddDistanceCounter < $pRaidDistancesCount) 
		    {
			$AddDistanceCounter++;
			
			$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink, distance_hide) 
			        values (".$RaidId.", 'Дистанция".$AddDistanceCounter."', '','', 0)";

                        // echo $sql;
			$rs = MySqlQuery($sql);
		    
		    }
		    // Конец добавления дистанций 
		}
                // Конец проверки на число дистанций



		$sql = "update Raids set raid_name = trim('".$pRaidName."'),
				raid_period = trim('".$pRaidPeriod."'), 
				raid_registrationenddate = ";
				
		if ($pClearRaidRegistrationEndDate == 1) 
		{	
			$sql.=  " NULL ";
		} else {
			$sql.=  "'".$pRaidRegistrationEndDate."'";
		}
//		$sql.=  " , raid_logolink = trim('".$pRaidLogoLink."') "; 
//		$sql.=  " , raid_ruleslink = trim('".$pRaidRulesLink."') "; 
		$sql.=  " , raid_startpoint =  trim('".$pRaidStartPointName."') ";
//		$sql.=  " , raid_startlink = trim('".$pRaidStartLink."') ";
		$sql.=  " , raid_finishpoint = trim('".$pRaidFinishPointName."') ";
                $sql.=  " , raid_closedate =  ";
					   
		if ($pClearRaidCloseDate == 1) 
		{	
			$sql.=  " NULL ";
		} else {
			$sql.=  " '".$pRaidCloseDate."'";
		}
	
//		$sql.= ", raid_znlink = trim('".$pRaidZnLink."') ";
		$sql.= ", raid_noshowresult = ".$pRaidNoShowResult." ";
		$sql.= ", raid_readonlyhoursbeforestart = ".$pRaidReadOnlyHoursBeforeStart." ";
		$sql.= ", raid_fileprefix = trim('".$pRaidFilePrefix."') ";
		$sql.=  " where raid_id = ".$RaidId; 
	    
	 //       echo $sql;

			
		$rs = MySqlQuery($sql);
	}
	// Конец разных вариантов действий при создании и редактировании ММБ


	// Если передали альтернативную страницу, на которую переходить (пока только одна возможность - на список команд)
	$view = $_POST['view'];
	if (empty($view)) $view = "ViewRaidData";
}
// ============ Отмена изменений в марш-броске ====================================
elseif ($action == "CancelChangeRaidData")
{
	$view = "ViewRaidData";
}
// ============ Измененеия в дистанции ====================================
elseif ($action == 'DistanceChangeData')
{
	$viewmode = "";
	$view = "ViewRaidData";
	// Общая проверка возможности редактирования
	if (!$Administrator)
	{
		$statustext = "Нет прав на ввод или правку дистанций";
		$alert = 0;
		return;
	}
        $pDistanceId = (int)$_POST['DistanceId'];

	// Проверка на ключ дистанции
	if  ($pDistanceId <= 0) 
	{
			$statustext = 'Не найден ключ дистанции.';
			$alert = 1;
		//	$viewsubmode = "ReturnAfterError";
			return;
	}
        // Конец проверки на пустое название 

        		
        $pDistanceName = $_POST['DistanceName'.$pDistanceId];

	// Проверка на пустое название 
	if (empty($pDistanceName)) 
	{
			$statustext = 'Пустое название дистанции.';
			$alert = 1;
		//	$viewsubmode = "ReturnAfterError";
			return;
	}
        // Конец проверки на пустое название 

        $pDistanceData = $_POST['DistanceData'.$pDistanceId];
	
	$sql = "update Distances set distance_name = trim('".$pDistanceName."'),
	 			     distance_data = trim('".$pDistanceData."')
		where distance_id = ".$pDistanceId; 
	    
	// echo $sql;
	$rs = MySqlQuery($sql);


}
// ============ Удаление дистанции  =============
elseif ($action == 'HideDistance')
{

	if (!$Administrator)
	{
		$statustext = "Нет прав на удаление дистанции";
		$alert = 0;
		return;
	}

        $pDistanceId = $_POST['DistanceId'];


	if ($pDistanceId <= 0)
	{
		$statustext = 'Не определён ключ дистанции.';
		$alert = 1;
//		$viewsubmode = "ReturnAfterError";
		return;
	}
	

	$viewmode = "";
	$view = "ViewRaidData";
	
	        // Проверяем, что нет точек на эту дистацнию
		$sql = "select count(*) as resultcount
			from LevelPoints lp
			where levelpoint_hide = 0 and distance_id = ".$pDistanceId;       

		$rs = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($rs);
		mysql_free_result($rs);
		$NowLevelPointsCounter = $Row['resultcount'];
		if ($NowLevelPointsCounter > 0)
		{
			$statustext = "Уже есть точки на эту дистанцию.";
			$alert = 1;
//			$viewsubmode = "ReturnAfterError";
			return;
                }

	        // Проверяем, что нет команд на эту дистацнию
		$sql = "select count(*) as resultcount
			from Teams t
			where team_hide = 0 and distance_id = ".$pDistanceId;       

		$rs = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($rs);
		mysql_free_result($rs);
		$NowTeamsCounter = $Row['resultcount'];
		if ($NowTeamsCounter > 0)
		{
			$statustext = "Уже есть команды на эту дистанцию.";
			$alert = 1;
//			$viewsubmode = "ReturnAfterError";
			return;
                }
	 

       
        $sql = "update Distances set distance_hide = 1
	        where distance_id = ".$pDistanceId;   
			
	MySqlQuery($sql);


	$view = "ViewRaidData";

}
// ============ Загруженные файлы  =============
elseif ($action == 'ViewRaidFilesPage')
{
	if ($RaidId <= 0)
	{
		$statustext = 'Id ММБ не указан';
		$alert = 1;
		return;
	}
	$view = "ViewRaidFiles";
	$viewmode = "Add";
}
// ============ Загрузка файда  =============
elseif ($action == 'AddRaidFile')
{

	$view = "ViewRaidFiles";
	$viewmode = "Add";


	// Общая проверка возможности редактирования
	if (!$Administrator and !$Moderator)
	{
		$statustext = "Нет прав на загрузку файла";
		$alert = 0;
		return;
	}

	$pFileTypeId = $_POST['FileTypeId'];
	//$pLevelPointId = $_POST['LevelPointId'];
	$pRaidFileComment = $_POST['RaidFileComment'];



        // Обрабатываем зхагрузку файла 
	// Проверка, что файл загрузился
        if (!empty($_FILES['raidfile']['name']) and ($_FILES['raidfile']['size'] > 0))
	{
             $pMimeType = trim($_FILES['raidfile']['type']);   
     /*
         Тут можно вставить проверки по типу звгружаемого файла
           if  (substr(trim($_FILES['raidfile']['type']), 0, 6) != 'image/') 
	   {
			$statustext = 'Недопустимый тип файла.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
	   }
       */     


	      $sql = "select  raid_fileprefix
	              from Raids
	              where raid_id = ".$RaidId;        
	 
		$Result = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);

		$Prefix = trim($Row['raid_fileprefix']);

 	   $pRaidFileName = trim(basename($_FILES['raidfile']['name']));

           if (strlen($Prefix) > 0 && substr($pRaidFileName, 0, strlen($Prefix)) <> $Prefix)
 	   {
                $pRaidFileName = $Prefix.$pRaidFileName;
	   }

	   $UploadFile = $MyStoreFileLink . $pRaidFileName;

	   if (move_uploaded_file($_FILES['raidfile']['tmp_name'], $UploadFile))
	   {
		// Успешно загрузили файл
		$pRaidFileLink = $MyStoreHttpLink . $pRaidFileName;
	   } else {
			$statustext = 'Ошибка загрузки файла.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }
           // Конец проверки на успешность загрузки

           // Пишем ссылку на файл в таблицу
	   //  М.б. можно переделать на запись файла прямо в таблицу - это повышаетбезопасность, но
	   // надо тогда писать собственное отображение файлов 
    	   $sql = "insert into RaidFiles (raid_id, raidfile_mimetype, filetype_id, 
			raidfile_uploaddt, raidfile_name, raidfile_comment, raidfile_hide)
			values (".$RaidId.", '".$pMimeType."', ".$pFileTypeId.", NOW(), '".$pRaidFileName."','".$pRaidFileComment."', 0)";
	   // При insert должен вернуться послений id - это реализовано в MySqlQuery
	   $RaidFileId = MySqlQuery($sql);
	
	   if ($RaidFileId <= 0)
	   {
			$statustext = 'Ошибка записи нового файла.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
	   }

       }
       // Конец проверки, что файл был загружен	
	
 }
 elseif ($action == "RaidFileInfo")  
 {
       // Действие вызывается кнопокй "Править" в таблице файлов

   
	$view = "ViewRaidFiles";
	$viewmode = "Edit";
 }
 // ============ Правка файла  =============
elseif ($action == 'RaidFileChange')
{
	if (!$Administrator and !$Moderator)
	{
		$statustext = "Нет прав на правку файла";
		$alert = 0;
		return;
	}


        $pRaidFileId = $_POST['RaidFileId'];

	if ($pRaidFileId <= 0)
	{
		$statustext = 'Не определён ключ файла.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	
	$pFileTypeId = $_POST['FileTypeId'];
	//$pLevelPointId = $_POST['LevelPointId'];
	$pRaidFileComment = $_POST['RaidFileComment'];

		
        $sql = "update RaidFiles  set filetype_id = ".$pFileTypeId.", 
	                              raidfile_comment = '".$pRaidFileComment."'
	        where raidfile_id = ".$pRaidFileId;        
			
	 MySqlQuery($sql);
       
         // Не знаю, какой правильно режим поставить
  	 $view = "ViewRaidFiles";
	 $viewmode = "Add";
		

}
// ============ Удаление  файла  =============
elseif ($action == 'HideFile')
{
	if (!$Administrator and !$Moderator)
	{
		$statustext = "Нет прав на правку файла";
		$alert = 0;
		return;
	}

        $pRaidFileId = $_POST['RaidFileId'];


	if ($pRaidFileId <= 0)
	{
		$statustext = 'Не определён ключ файла.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	

		
        $sql = "select  raidfile_name
	        from RaidFiles
	        where raidfile_id = ".$pRaidFileId;        
	 
       	$Result = MySqlQuery($sql);  
	$Row = mysql_fetch_assoc($Result);
        mysql_free_result($Result);

        $UnlinkFile = $MyStoreFileLink.trim($Row['raidfile_name']);

        if (file_exists($UnlinkFile))
	{
	  unlink($UnlinkFile);
	} 
       
        $sql = "update RaidFiles set raidfile_hide = 1
	        where raidfile_id = ".$pRaidFileId;        
			
	 MySqlQuery($sql);



         // Не знаю, какой правильно режим поставить
  	 $view = "ViewRaidFiles";
	 $viewmode = "Add";
		

}
// ============ Точки дистанции  =============
elseif ($action == 'ViewLevelPointsPage')
{
	if ($RaidId <= 0)
	{
		$statustext = 'Id ММБ не указан';
		$alert = 1;
		return;
	}
	
	// Есди дистанция не указана  - берём первую
        if (empty($_POST['DistanceId']))
	{

		$sql = "select distance_id, distance_name from Distances where distance_hide = 0  and raid_id = ".$RaidId." order by distance_id ";
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $DistanceId = $Row['distance_id'];
		mysql_free_result($Result);
	    
	} else {
	  $DistanceId =	$_POST['DistanceId'];
        }
	// Конец инициализации дистанции 
       	
	$view = "ViewLevelPoints";
	$viewmode = "Add";
}
// ============ Добавить точку  =============
elseif ($action == 'AddLevelPoint')
{


	$view = "ViewLevelPoints";
	$viewmode = "Add";


	// Общая проверка возможности редактирования
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на ввод точки";
		$alert = 0;
		return;
	}

		$pPointTypeId = $_POST['PointTypeId'];
		$pDistanceId = $_POST['DistanceId'];
                $pPointName = $_POST['PointName'];
                $pPointPenalty = $_POST['PointPenalty'];
		
	        $pLevelPointMinYear = $_POST['MinYear'];
                $pLevelPointMinDate = $_POST['MinDate'];
                $pLevelPointMinTime = $_POST['MinTime'];
                $pLevelPointMaxYear = $_POST['MaxYear'];
                $pLevelPointMaxDate = $_POST['MaxDate'];
                $pLevelPointMaxTime = $_POST['MaxTime'];


         // тут по-хорошему нужны проверки

	      $sql = "select  MAX(levelpoint_order) as lastorder, YEAR(NOW()) as nowyear
	              from LevelPoints
	              where levelpoint_hide = 0 and distance_id = ".$pDistanceId;        
	 
		$Result = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);

		$LastOrder = (int)$Row['lastorder'];
		$NowYear = (int)$Row['nowyear'];

	   if  (empty($pPointName) or trim($pPointName) == 'Название КП')
	   {
			$statustext = 'Не указано название точки.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }

                // год всегда пишем текущий. если надо - можно добавить поле для года

	        $MinYDTs = "'".$NowYear."-".substr(trim($pLevelPointMinDate), -2)."-".substr(trim($pLevelPointMinDate), 0, 2)." ".substr(trim($pLevelPointMinTime), 0, 2).":".substr(trim($pLevelPointMinTime), -2).":00'";
		$MinYDT = strtotime(substr(trim($MinYDTs), 1, -1));
	        $MaxYDTs = "'".$NowYear."-".substr(trim($pLevelPointMaxDate), -2)."-".substr(trim($pLevelPointMaxDate), 0, 2)." ".substr(trim($pLevelPointMaxTime), 0, 2).":".substr(trim($pLevelPointMaxTime), -2).":00'";
		$MaxYDT = strtotime(substr(trim($MaxYDTs), 1, -1));
	

		 $sql = " select count(*) as countresult 
		          from LevelPoints
		          where levelpoint_hide = 0  and distance_id = ".$pDistanceId."
			        and trim(levelpoint_name)= trim(".$pPointName.")"; 
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $AlreadyExists = (int)$Row['countresult'];
		mysql_free_result($Result);

	   if  ($AlreadyExists > 0)
	   {
			$statustext = 'Повтор названия.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }


             // потом добавить время макс. и мин.
	     
		$sql = "insert into LevelPoints (distance_id, levelpoint_name, pointtype_id, 
			levelpoint_penalty, levelpoint_order, levelpoint_hide, 
			levelpoint_mindatetime, levelpoint_maxdatetime )
			values (".$pDistanceId.", '".$pPointName."', ".$pPointTypeId.",
			        ".$pPointPenalty." , ".($LastOrder + 1).", 0, ".$MinYDTs.", ".$MaxYDTs.")";
		// При insert должен вернуться послений id - это реализовано в MySqlQuery
		$LevelPointId = MySqlQuery($sql);
		
		if ($LevelPointId <= 0)
		{
			$statustext = 'Ошибка записи новой точки.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
	

 }
 elseif ($action == "LevelPointInfo")  
 {
    // Действие вызывается кнопокй "Править" в таблице точек
   
	$view = "ViewLevelPoints";
	$viewmode = "Edit";
 }
 // ============ Правка точки  =============
elseif ($action == 'LevelPointChange')
{
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку точки";
		$alert = 0;
		return;
	}

  	 $view = "ViewLevelPoints";
	 $viewmode = "Add";


        $pLevelPointId = $_POST['LevelPointId'];


	if ($pLevelPointId <= 0)
	{
		$statustext = 'Не определён ключ точки.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}



     


                $sql = "select  YEAR(NOW()) as nowyear";        
	 
		$Result = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);
		$NowYear = (int)$Row['nowyear'];


		$pPointTypeId = $_POST['PointTypeId'];
		$pDistanceId = $_POST['DistanceId'];
                $pPointName = trim($_POST['PointName']);
                $pPointPenalty = $_POST['PointPenalty'];


		$pLevelPointMinYear = $_POST['MinYear'];
                $pLevelPointMinDate = $_POST['MinDate'];
                $pLevelPointMinTime = $_POST['MinTime'];
                $pLevelPointMaxYear = $_POST['MaxYear'];
                $pLevelPointMaxDate = $_POST['MaxDate'];
                $pLevelPointMaxTime = $_POST['MaxTime'];


        // тут надо поставить проверки
      // год всегда пишем текущий. если надо - можно добавить поле для года

	
		$MinYDTs = "'".$NowYear."-".substr(trim($pLevelPointMinDate), -2)."-".substr(trim($pLevelPointMinDate), 0, 2)." ".substr(trim($pLevelPointMinTime), 0, 2).":".substr(trim($pLevelPointMinTime), -2).":00'";
		$MinYDT = strtotime(substr(trim($MinYDTs), 1, -1));
	        $MaxYDTs = "'".$NowYear."-".substr(trim($pLevelPointMaxDate), -2)."-".substr(trim($pLevelPointMaxDate), 0, 2)." ".substr(trim($pLevelPointMaxTime), 0, 2).":".substr(trim($pLevelPointMaxTime), -2).":00'";
		$MaxYDT = strtotime(substr(trim($MaxYDTs), 1, -1));
	


		 $sql = " select count(*) as countresult 
		          from LevelPoints
		          where levelpoint_hide = 0  and distance_id = ".$pDistanceId."
                                and levelpoint_id <> ".$pLevelPointId."
			        and trim(levelpoint_name)= trim(".$pPointName.")"; 
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $AlreadyExists = (int)$Row['countresult'];
		mysql_free_result($Result);

	   if  ($AlreadyExists > 0)
	   {
			$statustext = 'Повтор названия.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }


		
        $sql = "update LevelPoints  set pointtype_id = ".$pPointTypeId." 
	                                ,levelpoint_name = '".$pPointName."'
	                                ,levelpoint_penalty = ".$pPointPenalty."
	                                ,levelpoint_mindatetime = ".$MinYDTs."
	                                ,levelpoint_maxdatetime = ".$MaxYDTs."
	        where levelpoint_id = ".$pLevelPointId;        
			
	//echo $sql;
			
	 MySqlQuery($sql);
       
		

}
// ============ Удаление точки  =============
elseif ($action == 'HideLevelPoint')
{
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку точки";
		$alert = 0;
		return;
	}

        $pLevelPointId = $_POST['LevelPointId'];


	if ($pLevelPointId <= 0)
	{
		$statustext = 'Не определён ключ точки.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	
	      $sql = "select  distance_id, levelpoint_order
	              from LevelPoints
	              where levelpoint_id = ".$pLevelPointId;        
	 
		$Result = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);

		$DistanceId = $Row['distance_id'];
		$LevelOrder = $Row['levelpoint_order'];


       
        $sql = "update LevelPoints set levelpoint_hide = 1, levelpoint_order = 0 
	        where levelpoint_id = ".$pLevelPointId;        
			
	 MySqlQuery($sql);

		// сдвигаем все точки с большими порядоквыми номерами, чем текущая
        $sql = "update LevelPoints set levelpoint_order = levelpoint_order - 1
	        where levelpoint_order > ".$LevelOrder. " and distance_id = ".$DistanceId;        
			
	 MySqlQuery($sql);


	$view = "ViewLevelPoints";
	$viewmode = "Add";
		

}
// ============ Поднять точку (уменьшить порядковый номер)  =============
elseif ($action == 'LevelPointOrderDown')
{


	$view = "ViewLevelPoints";
	$viewmode = "Add";


	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку точки";
		$alert = 0;
		return;
	}

        $pLevelPointId = $_POST['LevelPointId'];


	if ($pLevelPointId <= 0)
	{
		$statustext = 'Не определён ключ точки.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	

	      $sql = "select  distance_id, levelpoint_order
	              from LevelPoints
	              where levelpoint_id = ".$pLevelPointId;        
	 
		$Result = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);

		$DistanceId = $Row['distance_id'];
		$LevelOrder = $Row['levelpoint_order'];


	      $sql = "select  levelpoint_id
	              from LevelPoints
	              where levelpoint_order < ".$LevelOrder."
		            and distance_id = ".$DistanceId."
			    and levelpoint_hide = 0
		     order by levelpoint_order desc
		     LIMIT 0,1";
	 
		$Result = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);

		$MaxLevelPointId = (int)$Row['levelpoint_id'];

        if ($MaxLevelPointId == 0)
	{
		$statustext = 'Нельзя уменьшить порядковый номер.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	
	}
       
              
        $sql = "update LevelPoints set levelpoint_order = levelpoint_order - 1
	        where levelpoint_id = ".$pLevelPointId;        
			
	 MySqlQuery($sql);


        $sql = "update LevelPoints set levelpoint_order = levelpoint_order + 1
	        where levelpoint_id = ".$MaxLevelPointId;        
			
	 MySqlQuery($sql);


		

}
// ============ Опустить точку  (увеличить порядковый номер) =============
elseif ($action == 'LevelPointOrderUp')
{

	$view = "ViewLevelPoints";
	$viewmode = "Add";


	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку точки";
		$alert = 0;
		return;
	}

        $pLevelPointId = $_POST['LevelPointId'];


	if ($pLevelPointId <= 0)
	{
		$statustext = 'Не определён ключ точки.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	
	      $sql = "select  distance_id, levelpoint_order
	              from LevelPoints
	              where levelpoint_id = ".$pLevelPointId;        
	 
		$Result = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);

		$DistanceId = $Row['distance_id'];
		$LevelOrder = $Row['levelpoint_order'];


		$sql = "select  levelpoint_id
	              from LevelPoints
	              where levelpoint_order > ".$LevelOrder."
		            and distance_id = ".$DistanceId."
			    and levelpoint_hide = 0
		     order by levelpoint_order asc
		     LIMIT 0,1";
	
	      
		$Result = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);

		$MinLevelPointId = (int)$Row['levelpoint_id'];


        if ($MinLevelPointId == 0)
	{
		$statustext = 'Нельзя увеличить порядковый номер.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	
	}
       
        $sql = "update LevelPoints set levelpoint_order = levelpoint_order + 1
	        where levelpoint_id = ".$pLevelPointId;        
			
	 MySqlQuery($sql);


        $sql = "update LevelPoints set levelpoint_order = levelpoint_order - 1
	        where levelpoint_id = ".$MinLevelPointId;        
			
	 MySqlQuery($sql);

}
// ============ просмотр интервалов амнистии  =============
elseif ($action == 'ViewLevelPointDiscountsPage')
{
	if ($RaidId <= 0)
	{
		$statustext = 'Id ММБ не указан';
		$alert = 1;
		return;
	}
	
	// Есди дистанция не указана  - берём первую
        if (empty($_POST['DistanceId']))
	{

		$sql = "select distance_id, distance_name from Distances where distance_hide = 0  and raid_id = ".$RaidId." order by distance_id ";
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $DistanceId = $Row['distance_id'];
		mysql_free_result($Result);
	    
	} else {
	  $DistanceId =	$_POST['DistanceId'];
        }
	// Конец инициализации дистанции 
	$view = "ViewLevelPointDiscounts";
	$viewmode = "Add";
}
// ============  Добавить интервал амнистии  =============
elseif ($action == 'AddLevelPointDiscount')
{
	$view = "ViewLevelPointDiscounts";
	$viewmode = "Add";


	// Общая проверка возможности редактирования
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на ввод интервала";
		$alert = 0;
		return;
	}

		$pDistanceId = (int)$_POST['DistanceId'];
                $pDiscountStart = (int)$_POST['DiscountStart'];
                $pDiscountFinish = (int)$_POST['DiscountFinish'];
                $pDiscountValue = (int)$_POST['DiscountValue'];
                


         // тут по-хорошему нужны проверки
      
	   if  (empty($pDiscountValue) or ($pDiscountFinish < $pDiscountStart) or empty($pDiscountStart) or empty($pDiscountFinish))
	   {
			$statustext = 'Нулевая амнистия, пустое начало или конец; начало амнистии позже конца.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }

      	         $sql = " select count(*) as countresult 
		          from LevelPointDiscounts
		          where levelpointdiscount_hide = 0  and distance_id = ".$pDistanceId."
			        and (levelpointdiscount_start <= ".$pDiscountFinish." and levelpointdiscount_finish >= ".$pDiscountStart.")"; 
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $AlreadyExists = (int)$Row['countresult'];
		mysql_free_result($Result);

	   if  ($AlreadyExists > 0)
	   {
			$statustext = 'Интервал пересекается с существующим.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }

      	         $sql = " select count(*) as countresult 
		          from LevelPoints
		          where levelpoint_hide = 0  and distance_id = ".$pDistanceId."
			        and pointtype_id in (1,2,4) 
			        and (levelpoint_order <= ".$pDiscountFinish." and levelpoint_order >= ".$pDiscountStart.")";
				 
;
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $ForbiddenPointExists = (int)$Row['countresult'];
		mysql_free_result($Result);

	   if  ($ForbiddenPointExists > 0)
	   {
			$statustext = 'Интервал содержит запрещённые для амнистии точки.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }

                

             // потом добавить время макс. и мин.
	     
		$sql = "insert into LevelPointDiscounts (distance_id, levelpointdiscount_start, levelpointdiscount_finish, 
			levelpointdiscount_hide, levelpointdiscount_value)
			values (".$pDistanceId.", ".$pDiscountStart.", ".$pDiscountFinish.", 0, ".$pDiscountValue.")";
		// При insert должен вернуться послений id - это реализовано в MySqlQuery
		$LevelPointDiscountId = MySqlQuery($sql);
		
		if ($LevelPointDiscountId <= 0)
		{
			$statustext = 'Ошибка записи новой точки.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
	

 }
 elseif ($action == "LevelPointDiscountInfo")  
 {
    // Действие вызывается кнопокй "Править" в таблице интервалов
   
	$view = "ViewLevelPointDiscounts";
	$viewmode = "Edit";
 }
 // ============ Правка интервала  =============
elseif ($action == 'LevelPointDiscountChange')
{
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку интервала";
		$alert = 0;
		return;
	}

  	 $view = "ViewLevelPointDiscounts";
	 $viewmode = "Add";
	
        $pLevelPointDiscountId = $_POST['LevelPointDiscountId'];


	if ($pLevelPointDiscountId <= 0)
	{
		$statustext = 'Не определён ключ интервала.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}

                

		$pDistanceId = (int)$_POST['DistanceId'];
                $pDiscountStart = (int)$_POST['DiscountStart'];
                $pDiscountFinish = (int)$_POST['DiscountFinish'];
                $pDiscountValue = (int)$_POST['DiscountValue'];
           
        // тут надо поставить проверки

	   if  (empty($pDiscountValue) or ($pDiscountFinish < $pDiscountStart) or empty($pDiscountStart) or empty($pDiscountFinish))
	   {
			$statustext = 'Нулевая амнистия, пустое начало или конец; начало амнистии позже конца.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }

      	         $sql = " select count(*) as countresult 
		          from LevelPointDiscounts
		          where levelpointdiscount_hide = 0  and distance_id = ".$pDistanceId."
                                and  levelpointdiscount_id <> ".$pLevelPointDiscountId."
			        and (levelpointdiscount_start <= ".$pDiscountFinish." and levelpointdiscount_finish >= ".$pDiscountStart.")"; 
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $AlreadyExists = (int)$Row['countresult'];
		mysql_free_result($Result);

	   if  ($AlreadyExists > 0)
	   {
			$statustext = 'Интервал пересекается с существующим.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }

      	         $sql = " select count(*) as countresult 
		          from LevelPoints
		          where levelpoint_hide = 0  and distance_id = ".$pDistanceId."
			        and pointtype_id in (1,2,4) 
			        and (levelpoint_order <= ".$pDiscountFinish." and levelpoint_order >= ".$pDiscountStart.")";
				 
;
                
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
	        $ForbiddenPointExists = (int)$Row['countresult'];
		mysql_free_result($Result);

	   if  ($ForbiddenPointExists > 0)
	   {
			$statustext = 'Интервал содержит запрещённые для амнистии точки.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }

	
		
        $sql = "update LevelPointDiscounts  set levelpointdiscount_value = ".$pDiscountValue."
	                                ,levelpointdiscount_start = ".$pDiscountStart."
	                                ,levelpointdiscount_finish = ".$pDiscountFinish."
	       where 	levelpointdiscount_id = ".$pLevelPointDiscountId;        
			
	//echo $sql;
			
	 MySqlQuery($sql);
       
      	

}
// ============ Удаление интервала  =============
elseif ($action == 'HideLevelPointDiscount')
{
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку интервала";
		$alert = 0;
		return;
	}

        $pLevelPointDiscountId = $_POST['LevelPointDiscountId'];


	if ($pLevelPointDiscountId <= 0)
	{
		$statustext = 'Не определён ключ интервала.';
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	
	      

       
        $sql = "update LevelPointDiscounts set levelpointdiscount_hide = 1 
	        where levelpointdiscount_id = ".$pLevelPointDiscountId;        
			
	 MySqlQuery($sql);

	$view = "ViewLevelPointDiscounts";
	$viewmode = "Add";
		

}
// ============ Пересоздание этапов =================================
elseif ($action == 'RecalculateLevels')
{
	if (!$Administrator && !$Moderator)
	{
		$statustext = "Нет прав на правку";
		$alert = 0;
		return;
	}

  	 $view = "ViewLevelPoints";
	 $viewmode = "Add";
	 
	 
	 echo 'Пока не сделано.';
}
// ============ Никаких действий не требуется =================================
else
{
}

?>
