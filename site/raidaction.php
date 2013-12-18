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
	if ($action == "AddRaidFile") $viewmode = "Add"; else $viewmode = "";
	$view = "ViewRaidFiles";
	// Общая проверка возможности редактирования
	if (!$Administrator)
	{
		$statustext = "Нет прав на загрузку файла";
		$alert = 0;
		return;
	}

	$pFileTypeId = $_POST['FileTypeId'];
	//$pLevelPointId = $_POST['LevelPointId'];
	$pRaidFileComment = $_POST['RaidFileComment'];



        // Обрабатываем зхагрузку файла 
        if (!empty($_FILES['raidfile']['name']) and ($_FILES['raidfile']['size'] > 0))
	{
             $pMimeType = trim($_FILES['raidfile']['type']);   
     /*      if  (substr(trim($_FILES['raidfile']['type']), 0, 6) != 'image/') 
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


           if ($action == "AddRaidFile")
	   // Новsый файл
	   {
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
		// Открываем опять с возможностью загрузить новый файл
		$viewmode = "Add";
	       }
	
	}
        // Конец проверки на указание в форме файла для загрузки 
	
 }
 elseif ($action == "RaidFileInfo")  
 {
    // Действие вызывается ссылкой под имененм пользователя
   
	$view = "ViewRaidFiles";
	$viewmode = "Edit";
 }
 // ============ Правка файла  =============
elseif ($action == 'RaidFileChange')
{
	if (!$Administrator)
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
	if (!$Administrator)
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
// ============ Никаких действий не требуется =================================
else
{
}

?>
