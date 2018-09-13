<?php
// +++++++++++ Обработчик действий, связанных с марш-бросокм +++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

function raidError($message)
{
	global $viewmode;
	$viewmode = "Edit";
	CMmb::setErrorSm($message);
}

// ============ Обработка возможности создания нового марш-броска команды =====================
if ($action == "RegisterNewRaid")
{
	CMmb::setViews('ViewRaidData', 'Add');

	// Проверка возможности создать сарш бросок
	if (!$Administrator)
	{
		CMmb::setMessage('Нет прав на создание марш-броска.');
		return;
	}
}
// ============ Информация о ММБ (форма) =============
elseif ($action == 'RaidInfo')
{
	if ($RaidId <= 0)
	{
		CMmb::setErrorMessage('Id ММБ не указан');
		return;
	}
	CMmb::setViews('ViewRaidData', '');
}
// ============ Изменение данных ММБ или запись нового ММБ (реакция на отправку формы) =============
elseif ($action == 'RaidChangeData' or $action == "AddRaid")
{
	CMmb::setViews('ViewRaidData', ($action == "AddRaid") ? 'Add' : '');
	// Общая проверка возможности редактирования
	if (!$Administrator)
	{
		CMmb::setMessage('Нет прав на ввод или правку марш-броска');
		return;
	}

	$pRaidName = $_POST['RaidName'];
	$pRaidPeriod = $_POST['RaidPeriod'];
	$pRaidRegistrationEndDate = $_POST['RaidRegistrationEndDate'];
	$pClearRaidRegistrationEndDate = mmb_isOn($_POST, 'ClearRaidRegistrationEndDate');
	//$pRaidLogoLink = $_POST['RaidLogoLink'];
	//$pRaidRulesLink = $_POST['RaidRulesLink'];
	$pRaidStartPointName = $_POST['RaidStartPointName'];
	//$pRaidStartLink = $_POST['RaidStartLink'];
	$pRaidFinishPointName = $_POST['RaidFinishPointName'];
	$pRaidCloseDate = $_POST['RaidCloseDate'];
	$pClearRaidCloseDate = mmb_isOn($_POST, 'ClearRaidCloseDate');
	//$pRaidZnLink = $_POST['RaidZnLink'];
        $pRaidDistancesCount = (int)$_POST['RaidDistancesCount'];
        $pRaidNoShowResult = mmb_isOn($_POST, 'RaidNoShowResult');
	$pRaidFilePrefix = $_POST['RaidFilePrefix'];
        $pRaidReadOnlyHoursBeforeStart = (int)$_POST['RaidReadOnlyHoursBeforeStart'];
        $pRaidMapPrice = (int)$_POST['RaidMapPrice'];
	$pRaidNoStartPrice = (int)$_POST['RaidNoStartPrice'];
	$pRaidTeamsLimit = (int)$_POST['RaidTeamsLimit'];

/*
        // Обрабатываем зхагрузку файла эмблемы
        if (!empty($_FILES['logofile']['name']) and ($_FILES['logofile']['size'] > 0))
	{
           if  (substr(trim($_FILES['logofile']['type']), 0, 6) != 'image/') 
	   {
			CMmb::setErrorSm('Недопустимый тип файла.');
			return;
	   }
            
	   $UploadFile = $MyStoreFileLink . basename($_FILES['logofile']['name']);

	   if (move_uploaded_file($_FILES['logofile']['tmp_name'], $UploadFile))
	   {
		// Успешно загрузили файл
		$pRaidLogoLink = $MyStoreHttpLink . basename($_FILES['logofile']['name']);
	   } else {
			CMmb::setErrorSm('Ошибка загрузки файла с эмблемой ММБ.');
			return;
           }
           // Конец проверки на успешность загрузки
	}
        // Конец проверки на указание в форме файла для загрузки эмблемы
	*/
	// Проверка на пустое название 
	if  (empty($pRaidName)) 
	{
		CMmb::setErrorSm('Пустое название ММБ.');
		return;
	}
        // Конец проверки на пустое название 
	
	// Проверка на число дистанций
	if  ($pRaidDistancesCount <= 0) 
	{
		CMmb::setErrorSm('Число дистанций должно быть положительным.');
		return;
	}
        // Конец проверки на число дистанций
	
	
	/*

        // Обрабатываем зхагрузку файла положения
        if (!empty($_FILES['rulesfile']['name']) and ($_FILES['rulesfile']['size'] > 0))
	{

           if  (substr(trim($_FILES['rulesfile']['type']), 0, 9) != 'text/html') 
	   {
			CMmb::setErrorSm('Недопустимый тип файла.');
			return;
	   }
            
	   $UploadFile = $MyStoreFileLink . basename($_FILES['rulesfile']['name']);

	   if (move_uploaded_file($_FILES['rulesfile']['tmp_name'], $UploadFile))
	   {
		// Успешно загрузили файл
		$pRaidRulesLink = $MyStoreHttpLink . basename($_FILES['rulesfile']['name']);
	   } else {
			CMmb::setErrorSm('Ошибка загрузки файла с положением ММБ.');
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
			where  trim(raid_name) = '$pRaidName'";

		if (CSql::singleValue($sql, 'resultcount') > 0)
		{
			CMmb::setErrorSm('Уже есть ММБ с таким названием.');
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
		$regEndDate = ($pClearRaidRegistrationEndDate == 1 or empty($pRaidRegistrationEndDate)) ? 'NULL' : "'$pRaidRegistrationEndDate'";
		$closeDate = ($pClearRaidCloseDate == 1 or empty($pRaidCloseDate)) ? 'NULL' : "'$pRaidCloseDate'";

                $sql = "insert into Raids (raid_name, raid_period, raid_registrationenddate, 
		                           raid_startpoint, raid_finishpoint, raid_closedate,
					   raid_noshowresult, raid_fileprefix,
					   raid_readonlyhoursbeforestart, raid_mapprice, 
					   raid_nostartprice, raid_teamslimit
					   )
			values (trim('$pRaidName'), trim('$pRaidPeriod'), $regEndDate
				, trim('$pRaidStartPointName'), trim('$pRaidFinishPointName')
				, $closeDate
				, $pRaidNoShowResult, trim('$pRaidFilePrefix')
				, $pRaidReadOnlyHoursBeforeStart, $pRaidMapPrice
				, $pRaidNoStartPrice, $pRaidTeamsLimit)";


		// При insert должен вернуться послений id - это реализовано в MySqlQuery

                //  echo $sql;

		$RaidId = MySqlQuery($sql);
		GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);

		if ($RaidId <= 0)
		{
			CMmb::setErrorSm('Ошибка записи нового ММБ.');
			return;
		} else {
		
		    // Добавляем дистанции
                    if  ($pRaidDistancesCount == 1)
		    {
				$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink, distance_hide) 
				        values ($RaidId, 'Общая', '','', 0)";
		    
				$rs = MySqlQuery($sql);
		    
		    } else {

			$AddDistanceCounter =  0;
			while ($AddDistanceCounter < $pRaidDistancesCount) 
			{
				$AddDistanceCounter++;
			
				$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink, distance_hide) 
				        values ($RaidId, 'Дистанция$AddDistanceCounter', '','', 0)";

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
			where distance_hide = 0 and raid_id = $RaidId";

		$NowDistancesCounter = CSql::singleValue($sql, 'resultcount');
		if ($NowDistancesCounter > $pRaidDistancesCount)
		{
			CMmb::setErrorSm('Дистанций не может быть меньше, чем уже создано.');
			return;

                } else {
                    // Добавляем дистанции
                    $AddDistanceCounter =  $NowDistancesCounter;
                    while ($AddDistanceCounter < $pRaidDistancesCount) 
		    {
			$AddDistanceCounter++;
			
			$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink, distance_hide) 
			        values ($RaidId, 'Дистанция$AddDistanceCounter', '','', 0)";

                        // echo $sql;
			$rs = MySqlQuery($sql);
		    }
		    // Конец добавления дистанций 
		}
                // Конец проверки на число дистанций


		$regEndDate = ($pClearRaidRegistrationEndDate == 1) ? 'NULL' : "'$pRaidRegistrationEndDate'";
		$closeDate = ($pClearRaidCloseDate == 1) ? 'NULL' : "'$pRaidCloseDate'";

		$sql = "update Raids set raid_name = trim('$pRaidName'),
				raid_period = trim('$pRaidPeriod'),
				raid_registrationenddate = $regEndDate
				, raid_startpoint =  trim('$pRaidStartPointName')
				, raid_finishpoint = trim('$pRaidFinishPointName')
				, raid_closedate = $closeDate
				, raid_noshowresult = $pRaidNoShowResult
				, raid_readonlyhoursbeforestart = $pRaidReadOnlyHoursBeforeStart
				, raid_mapprice = $pRaidMapPrice
				, raid_nostartprice = $pRaidNoStartPrice
				, raid_teamslimit = $pRaidTeamsLimit
				, raid_fileprefix = trim('$pRaidFilePrefix')
		        where raid_id = $RaidId";
	    
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
	CMmb::setViews('ViewRaidData', '');
	// Общая проверка возможности редактирования
	if (!$Administrator)
	{
		CMmb::setMessage('Нет прав на ввод или правку дистанций');
		return;
	}
        $pDistanceId = (int)$_POST['DistanceId'];

	// Проверка на ключ дистанции
	if  ($pDistanceId <= 0) 
	{
			CMmb::setErrorSm('Не найден ключ дистанции.', '' /*'ReturnAfterError' */);
			return;
	}
        // Конец проверки на пустое название 

        		
        $pDistanceName = $_POST['DistanceName'.$pDistanceId];

	// Проверка на пустое название 
	if (empty($pDistanceName)) 
	{
		CMmb::setErrorSm('Пустое название дистанции.', '' /*'ReturnAfterError' */);
		return;
	}
        // Конец проверки на пустое название 

        $pDistanceData = $_POST['DistanceData'.$pDistanceId];
	
	$sql = "update Distances set distance_name = trim('$pDistanceName'),
	 			     distance_data = trim('$pDistanceData')
		where distance_id = $pDistanceId";
	    
	// echo $sql;
	$rs = MySqlQuery($sql);
}
// ============ Удаление дистанции  =============
elseif ($action == 'HideDistance')
{
	if (!$Administrator)
	{
		CMmb::setMessage('Нет прав на удаление дистанции');
		return;
	}

        $pDistanceId = mmb_validateInt($_POST, 'DistanceId');

	if ($pDistanceId <= 0)
	{
		CMmb::setErrorSm('Не определён ключ дистанции.', '' /*'ReturnAfterError' */);
		return;
	}

	CMmb::setViews('ViewRaidData', '');
	
        // Проверяем, что нет точек на эту дистацнию
	$sql = "select count(*) as resultcount
		from LevelPoints lp
		where levelpoint_hide = 0 and distance_id = $pDistanceId";

	if (CSql::singleValue($sql, 'resultcount') > 0)
	{
		CMmb::setErrorSm('Уже есть точки на эту дистанцию.', '' /*"ReturnAfterError"*/);
		return;
        }

        // Проверяем, что нет команд на эту дистацнию
	$sql = "select count(*) as resultcount
		from Teams t
		where team_hide = 0 and distance_id = $pDistanceId";

	if (CSql::singleValue($sql, 'resultcount') > 0)
	{
		CMmb::setErrorSm('Уже есть команды на эту дистанцию.', '' /*'ReturnAfterError'*/);
		return;
        }
	 

	// 22.06.2015 Заменил на удаление дистанции
        $sql = "delete Distances where distance_id = $pDistanceId";
        //    $sql = "update Distances set distance_hide = 1
	//        where distance_id = ".$pDistanceId;   
	MySqlQuery($sql);


	$view = "ViewRaidData";
}
// ============ Загруженные файлы  =============
elseif ($action == 'ViewRaidFilesPage')
{
	if ($RaidId <= 0)
	{
		CMmb::setErrorMessage('Id ММБ не указан');
		return;
	}
	CMmb::setViews('ViewRaidFiles', 'Add');
}
// ============ Загрузка файда  =============
elseif ($action == 'AddRaidFile')
{
	CMmb::setViews('ViewRaidFiles', 'Add');


	// Общая проверка возможности редактирования
	if (!$Administrator and !$Moderator)
	{
		CMmb::setMessage('Нет прав на загрузку файла');
		return;
	}

	$pFileTypeId = mmb_validateInt($_POST, 'FileTypeId', -1);
	//$pLevelPointId = $_POST['LevelPointId'];
	$pRaidFileComment = $_POST['RaidFileComment'];


        // Обрабатываем загрузку файла
	// Проверка, что файл загрузился
        if (!empty($_FILES['raidfile']['name']) and ($_FILES['raidfile']['size'] > 0))
	{
		$pMimeType = trim($_FILES['raidfile']['type']);
/*
         Тут можно вставить проверки по типу звгружаемого файла
           if  (substr(trim($_FILES['raidfile']['type']), 0, 6) != 'image/') 
	   {
			CMmb::setErrorSm('Недопустимый тип файла.');
			return;
	   }
*/


		$sql = "select  raid_fileprefix
	              from Raids
	              where raid_id = $RaidId";

		$Prefix = trim(CSql::singleValue($sql, 'raid_fileprefix'));

 	        $pRaidFileName = trim(basename($_FILES['raidfile']['name']));

                if (strlen($Prefix) > 0 && substr($pRaidFileName, 0, strlen($Prefix)) <> $Prefix)
                        $pRaidFileName = $Prefix.$pRaidFileName;

	        $UploadFile = $MyStoreFileLink . $pRaidFileName;

	        if (move_uploaded_file($_FILES['raidfile']['tmp_name'], $UploadFile))
	        {
		// Успешно загрузили файл
			$pRaidFileLink = $MyStoreHttpLink . $pRaidFileName;
	        } else {
			CMmb::setErrorSm('Ошибка загрузки файла.');
			return;
                }
                // Конец проверки на успешность загрузки

                //Удаляем все ссылки на файл с таким же имененм
	        $sql = "update RaidFiles set raidfile_hide = 1
	                where raid_id = $RaidId and raidfile_name = '".trim($pRaidFileName)."'";
			
	        MySqlQuery($sql);

		// Пишем ссылку на файл в таблицу
		//  М.б. можно переделать на запись файла прямо в таблицу - это повышаетбезопасность, но
		// надо тогда писать собственное отображение файлов
		$sql = "insert into RaidFiles (raid_id, raidfile_mimetype, filetype_id,
			raidfile_uploaddt, raidfile_name, raidfile_comment, raidfile_hide)
			values ($RaidId, '$pMimeType', $pFileTypeId, NOW(), '$pRaidFileName','$pRaidFileComment', 0)";
	        // При insert должен вернуться послений id - это реализовано в MySqlQuery
	        $RaidFileId = MySqlQuery($sql);
	
	        if ($RaidFileId <= 0)
	        {
			CMmb::setErrorSm('Ошибка записи нового файла.');
			return;
	        }

		// делаем preview
	        $point = strrpos($pRaidFileName, '.');
	        if  ($point > 0 and (substr($pRaidFileName, $point) == '.png' or substr($pRaidFileName, $point) == '.gif')) 
	        {
		          $tumbImg = substr($pRaidFileName, 0, $point).'_tumb'.substr($pRaidFileName, $point);
			//	echo $point.' '.$ImageLink.' '.$tumbImg;
		          if (!is_file(trim($MyStoreFileLink).$tumbImg))
		          {
			//	echo '1111';
			     image_resize(trim($MyStoreFileLink).trim($pRaidFileName), trim($MyStoreFileLink).trim($tumbImg), 1000, 100, 0);
		          }
	        }
		// конец проверки на необходимость делать картинку preview


       }
       // Конец проверки, что файл был загружен	
 }
 elseif ($action == "RaidFileInfo")  
 {
       // Действие вызывается кнопокй "Править" в таблице файлов
	 CMmb::setViews('ViewRaidFiles', 'Edit');
 }
 // ============ Правка файла  =============
elseif ($action == 'RaidFileChange')
{
	if (!$Administrator and !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку файла');
		return;
	}

        $pRaidFileId = mmb_validateInt($_POST, 'RaidFileId');
	if ($pRaidFileId <= 0)
	{
		CMmb::setErrorSm('Не определён ключ файла.');
		return;
	}
	
	$pFileTypeId = mmb_validateInt($_POST, 'FileTypeId');
	//$pLevelPointId = $_POST['LevelPointId'];
	$pRaidFileComment = $_POST['RaidFileComment'];

        $sql = "update RaidFiles  set filetype_id = $pFileTypeId,
	                              raidfile_comment = '$pRaidFileComment'
	        where raidfile_id = $pRaidFileId";
			
	MySqlQuery($sql);
       
        // Не знаю, какой правильно режим поставить
	CMmb::setViews('ViewRaidFiles', 'Add');
}
// ============ Удаление  файла  =============
elseif ($action == 'HideFile')
{
	if (!$Administrator and !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку файла');
		return;
	}

        $pRaidFileId = mmb_validateInt($_POST, 'RaidFileId');
	if ($pRaidFileId <= 0)
	{
		CMmb::setErrorSm('Не определён ключ файла.');
		return;
	}

        $sql = "select  raidfile_name
	        from RaidFiles
	        where raidfile_id = $pRaidFileId";

        $UnlinkFile = $MyStoreFileLink.trim(CSql::singleValue($sql, 'raidfile_name'));

        if (file_exists($UnlinkFile))
	{
	        unlink($UnlinkFile);
	} 
       
        $sql = "update RaidFiles set raidfile_hide = 1
	        where raidfile_id = $pRaidFileId";
	MySqlQuery($sql);

        // Не знаю, какой правильно режим поставить
	CMmb::setViews('ViewRaidFiles', 'Add');
}
// ============ Точки сканирвания марш-броска  =============
elseif ($action == 'ViewScanPointsPage')
{
	if ($RaidId <= 0)
	{
		CMmb::setErrorMessage('Id ММБ не указан');
		return;
	}

	CMmb::setViews('ViewScanPoints', 'Add');
}
// ============ Добавить скан-точку  =============
elseif ($action == 'AddScanPoint')
{
	CMmb::setViews('ViewScanPoints', 'Add');

	// Общая проверка возможности редактирования
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на ввод скан-точки');
		return;
	}

        $pScanPointName = $_POST['ScanPointName'];

        // тут по-хорошему нужны проверки

	$sql = "select  MAX(scanpoint_order) as lastorder
	      from ScanPoints
	      where scanpoint_hide = 0 and raid_id = $RaidId";

	$LastOrder = (int)CSql::singleValue($sql, 'lastorder');

	if  (empty($pScanPointName) or trim($pScanPointName) == 'Название точки сканирования')
	{
		CMmb::setErrorSm('Не указано название скан-точки.');
		return;
	}

	$sql = " select count(*) as countresult
		  from ScanPoints
		  where scanpoint_hide = 0  and raid_id = $RaidId
		        and trim(scanpoint_name) = trim('$pScanPointName')";

	if  (((int) CSql::singleValue($sql, 'countresult')) > 0)
	{
		CMmb::setErrorSm('Повтор названия скан-точки.');
		return;
	}

        // потом добавить время макс. и мин.
	     
	$sql = "insert into ScanPoints (raid_id, scanpoint_name,
		scanpoint_order, scanpoint_hide)
		values ($RaidId, '$pScanPointName',  ".($LastOrder + 1).", 0)";
	// При insert должен вернуться послений id - это реализовано в MySqlQuery

        //    echo $sql;
		
	$ScanPointId = MySqlQuery($sql);

	if ($ScanPointId <= 0)
	{
		CMmb::setErrorSm('Ошибка записи новой скан-точки.');
		return;
	}
	

	$statustext = CheckScanPoints($RaidId);
	if (!empty($error))
	{
		$alert = 1;
	}
 }
 elseif ($action == "ScanPointInfo")  
 {
        // Действие вызывается кнопокй "Править" в таблице скан-точек
	 CMmb::setViews('ViewScanPoints', 'Edit');
 }
 // ============ Правка скан-точки  =============
elseif ($action == 'ScanPointChange')
{
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку скан-точки');
		return;
	}

	CMmb::setViews('ViewScanPoints', 'Add');

        $pScanPointId = mmb_validateInt($_POST, 'ScanPointId');
        $pScanPointName = $_POST['ScanPointName'];
	if ($pScanPointId <= 0)
	{
		raidError('Не определён ключ скан-точки.');
		return;
	}

	$sql = " select count(*) as countresult
		  from ScanPoints
		  where scanpoint_hide = 0  and raid_id = $RaidId
		        and scanpoint_id <> $pScanPointId
		        and trim(scanpoint_name)= trim('$pScanPointName')";

	if  (((int) CSql::singleValue($sql, 'countresult')) > 0)
	{
		raidError('Повтор названия.');
		return;
	}

        $sql = "update ScanPoints  set scanpoint_name = '$pScanPointName'
	        where scanpoint_id = $pScanPointId";
	//echo $sql;
	 MySqlQuery($sql);
       
	 $statustext = CheckScanPoints($RaidId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }
}
// ============ Удаление скан-точки  =============
elseif ($action == 'HideScanPoint')
{
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку скан-точки');
		return;
	}

	CMmb::setViews('ViewScanPoints', 'Add');

        $pScanPointId = mmb_validateInt($_POST, 'ScanPointId');
	if ($pScanPointId <= 0)
	{
		raidError('Не определён ключ скан-точки.');
		return;
	}
	
	$sql = "select  count(*) as lpcount
	      from LevelPoints
	      where scanpoint_id = $pScanPointId
	            and levelpoint_hide = 0" ;
	 
	if (CSql::singleValue($sql, 'lpcount') > 0)
	{
		raidError('Есть точки дистанции, которые ссылаются на эту скан-точку.');
		return;
	}
	
	$sql = "select  scanpoint_order
	      from ScanPoints
	      where scanpoint_id = $pScanPointId";

	$ScanOrder = CSql::singleValue($sql, 'scanpoint_order');

        $sql = "update ScanPoints set scanpoint_hide = 1, scanpoint_order = 0 
	        where scanpoint_id = $pScanPointId";
			
	MySqlQuery($sql);

		// сдвигаем все точки с большими порядоквыми номерами, чем текущая
        $sql = "update ScanPoints set scanpoint_order = scanpoint_order - 1
	        where scanpoint_order > $ScanOrder and raid_id = $RaidId";
			
	MySqlQuery($sql);


	$statustext = CheckScanPoints($RaidId);
	if (!empty($error))
	{
		$alert = 1;
	}

	CMmb::setViews('ViewScanPoints', 'Add');
}
// ============ Поднять скан-точку (уменьшить порядковый номер)  =============
elseif ($action == 'ScanPointOrderDown')
{
	CMmb::setViews('ViewScanPoints', 'Add');

	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку скан-точки');
		return;
	}

        $pScanPointId = mmb_validateInt($_POST, 'ScanPointId');
	if ($pScanPointId <= 0)
	{
		raidError('Не определён ключ скан-точки.');
		return;
	}
	

	$sql = "select  scanpoint_order
	      from ScanPoints
              where scanpoint_id = $pScanPointId";

	$ScanOrder = CSql::singleValue($sql, 'scanpoint_order');

	$sql = "select  scanpoint_id
	      from ScanPoints
	      where scanpoint_order < $ScanOrder
	            and raid_id = $RaidId
		    and scanpoint_hide = 0
	     order by scanpoint_order desc
	     LIMIT 0,1";
	 
	$MaxScanPointId = (int)CSql::singleValue($sql, 'scanpoint_id');
        if ($MaxScanPointId == 0)
	{
		raidError('Нельзя уменьшить порядковый номер.');
		return;
	}

        $sql = "update ScanPoints set scanpoint_order = scanpoint_order - 1
	        where scanpoint_id = $pScanPointId";
	MySqlQuery($sql);


        $sql = "update ScanPoints set scanpoint_order = scanpoint_order + 1
	        where scanpoint_id = $MaxScanPointId";
	MySqlQuery($sql);

	$statustext = CheckScanPoints($RaidId);
	if (!empty($error))
	{
		$alert = 1;
	}
}
// ============ Опустить скан-точку  (увеличить порядковый номер) =============
elseif ($action == 'ScanPointOrderUp')
{
	CMmb::setViews('ViewScanPoints', 'Add');

	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку скан-точки');
		return;
	}

        $pScanPointId = mmb_validateInt($_POST, 'ScanPointId');
	if ($pScanPointId <= 0)
	{
		raidError('Не определён ключ скан-точки.');
		return;
	}
	
	$sql = "select   scanpoint_order
	      from ScanPoints
	      where scanpoint_id = $pScanPointId";

	$ScanOrder = CSql::singleValue($sql, 'scanpoint_order');

	$sql = "select  scanpoint_id
	        from ScanPoints
	        where scanpoint_order > $ScanOrder
	              and raid_id = $RaidId
		      and scanpoint_hide = 0
	        order by scanpoint_order asc
	        LIMIT 0,1";
	
	     //   echo $sql;
	$MinScanPointId = (int)CSql::singleValue($sql, 'scanpoint_id');
        if ($MinScanPointId == 0)
	{
		raidError('Нельзя увеличить порядковый номер.');
		return;
	}
       
        $sql = "update ScanPoints set scanpoint_order = scanpoint_order + 1
	        where scanpoint_id = $pScanPointId";
			
	 MySqlQuery($sql);


        $sql = "update ScanPoints set scanpoint_order = scanpoint_order - 1
	        where scanpoint_id = $MinScanPointId";
			
	 MySqlQuery($sql);
	 
	 $statustext = CheckScanPoints($RaidId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }
	 

}
// ============ Точки дистанции  =============
elseif ($action == 'ViewLevelPointsPage')
{
	if ($RaidId <= 0)
	{
		CMmb::setErrorMessage('Id ММБ не указан');
		return;
	}

	$DistanceId = mmb_validateInt($_POST, 'DistanceId');
	// Есди дистанция не указана  - берём первую
        if ($DistanceId <= 0)
	{
		$sql = "select distance_id, distance_name from Distances where distance_hide = 0  and raid_id = $RaidId order by distance_id ";
	        $DistanceId = CSql::singleValue($sql, 'distance_id');
	}
	// Конец инициализации дистанции 

	CMmb::setViews('ViewLevelPoints', 'Add');
}
// ============ Добавить точку  =============
elseif ($action == 'AddLevelPoint')
{
	CMmb::setViews('ViewLevelPoints', 'Add');

	// Общая проверка возможности редактирования
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на ввод точки');
		return;
	}

	$pPointTypeId = mmb_validateInt($_POST, 'PointTypeId');
	$pDistanceId = mmb_validateInt($_POST, 'DistanceId');
        $pPointName = $_POST['PointName'];
        $pPointPenalty = mmb_validateInt($_POST, 'PointPenalty');

	$pScanPointId = mmb_validateInt($_POST, 'ScanPointId');
//	$pLevelId = $_POST['LevelId'];

	$MinYDTs = CSql::timeString2($_POST, 'MinYear', 'MinDate', 'MinTime');
	$MaxYDTs = CSql::timeString2($_POST, 'MaxYear', 'MaxDate', 'MaxTime');

         // тут по-хорошему нужны проверки

	$sql = "select  MAX(levelpoint_order) as lastorder, YEAR(NOW()) as nowyear
	      from LevelPoints
	      where levelpoint_hide = 0 and distance_id = $pDistanceId";

	$LastOrder = (int)CSql::singleValue($sql, 'lastorder');

	if  (empty($pPointName) or trim($pPointName) == 'Название КП')
	{
		CMmb::setErrorSm('Не указано название точки.');
		return;
	}


	$sql = " select count(*) as countresult
		  from LevelPoints
		  where levelpoint_hide = 0  and distance_id = $pDistanceId
	                and trim(levelpoint_name)= trim('$pPointName')";

	if  (((int) CSql::singleValue($sql, 'countresult')) > 0)
	{
		CMmb::setErrorSm('Повтор названия.');
		return;
	}


        // потом добавить время макс. и мин.
	     
	$sql = "insert into LevelPoints (distance_id, levelpoint_name, pointtype_id,
		levelpoint_penalty, levelpoint_order, levelpoint_hide,
		levelpoint_mindatetime, levelpoint_maxdatetime, scanpoint_id)
		values ($pDistanceId, '$pPointName', $pPointTypeId,
		        $pPointPenalty , ".($LastOrder + 1).", 0, $MinYDTs, $MaxYDTs, $pScanPointId)";

	// При insert должен вернуться послений id - это реализовано в MySqlQuery
	$LevelPointId = MySqlQuery($sql);
	if ($LevelPointId <= 0)
	{
		CMmb::setErrorSm('Ошибка записи новой точки.');
		return;
	}
	

	 $statustext = CheckLevelPoints($DistanceId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }


 }
 elseif ($action == "LevelPointInfo")  
 {
        // Действие вызывается кнопокй "Править" в таблице точек
	 CMmb::setViews('ViewLevelPoints', 'Edit');
 }
 // ============ Правка точки  =============
elseif ($action == 'LevelPointChange')
{
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку точки');
		return;
	}

  	CMmb::setViews('ViewLevelPoints', 'Add');


        $pLevelPointId = mmb_validateInt($_POST, 'LevelPointId');
	if ($pLevelPointId <= 0)
	{
		raidError('Не определён ключ точки.');
		return;
	}


        $sql = "select  YEAR(NOW()) as nowyear";
	$NowYear = (int)CSql::singleValue($sql, 'nowyear');     // вроде есть способы и попроще :)


	$pPointTypeId = mmb_validateInt($_POST, 'PointTypeId');
	$pDistanceId = mmb_validateInt($_POST, 'DistanceId');
        $pPointName = trim($_POST['PointName']);
        $pPointPenalty = mmb_validateInt($_POST, 'PointPenalty');

	$pScanPointId = $_POST['ScanPointId'];		// todo почему где-то оно -- int, а где-то -- строка
//	$pLevelId = $_POST['LevelId'];

	$MinYDTs = CSql::timeString2($_POST, 'MinYear', 'MinDate', 'MinTime');
	$MaxYDTs = CSql::timeString2($_POST, 'MaxYear', 'MaxDate', 'MaxTime');

	// тут надо поставить проверки

	$sql = " select count(*) as countresult
	  from LevelPoints
	  where levelpoint_hide = 0  and distance_id = $pDistanceId
	        and levelpoint_id <> $pLevelPointId
	        and trim(levelpoint_name)= trim('$pPointName')";
                
	if  (((int) CSql::singleValue($sql, 'countresult')) > 0)
	{
		raidError('Повтор названия.');
		return;
	}


//	                                ,level_id = '".$pLevelId."'
		
        $sql = "update LevelPoints  set pointtype_id = $pPointTypeId
	                                ,scanpoint_id = '$pScanPointId'
	                                ,levelpoint_name = '$pPointName'
	                                ,levelpoint_penalty = $pPointPenalty
	                                ,levelpoint_mindatetime = $MinYDTs
	                                ,levelpoint_maxdatetime = $MaxYDTs
	        where levelpoint_id = $pLevelPointId";
	//echo $sql;
	 MySqlQuery($sql);
       
	 $statustext = CheckLevelPoints($DistanceId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }
		

}
// ============ Удаление точки  =============
elseif ($action == 'HideLevelPoint')
{
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку точки');
		return;
	}

        $pLevelPointId = mmb_validateInt($_POST, 'LevelPointId');
	if ($pLevelPointId <= 0)
	{
		raidError('Не определён ключ точки.');
		return;
	}
	
	
	$sql = "select  count(*) as lpcount
	      from LevelPointDiscounts
	      where levelpoint_id = $pLevelPointId
	      	and levelpointdiscount_hide = 0";
	
	if (CSql::singleValue($sql, 'lpcount') > 0)
	{
		raidError('Есть группы амнистии, которые ссылаются на эту контрольную точку.');
		return;
	}

	$sql = "select  count(*) as lpcount
	      from TeamLevelPoints
	      where levelpoint_id = $pLevelPointId";
	
	if (CSql::singleValue($sql, 'lpcount') > 0)
	{
		raidError('Есть данные о прохождении команд, которые ссылаются на эту контрольную точку.');
		return;
	}
	
	$sql = "select  count(*) as lpcount
	      from TeamLevelDismiss
	      where levelpoint_id = $pLevelPointId";
	
	if (CSql::singleValue($sql, 'lpcount') > 0)
	{
		raidError('Есть данные о сходах участников команд, которые ссылаются на эту контрольную точку. ');
		return;
	}


	
	$sql = "select  distance_id, levelpoint_order
	      from LevelPoints
	      where levelpoint_id = $pLevelPointId";
	 
	$Row = CSql::singleRow($sql);
	$DistanceId = $Row['distance_id'];
	$LevelOrder = $Row['levelpoint_order'];


        //  19.06.2015 ПРобуем удалить точку физически

        $sql = "delete from LevelPoints where levelpoint_id = $pLevelPointId";
       
//        $sql = "update LevelPoints set levelpoint_hide = 1, levelpoint_order = 0 
//	        where levelpoint_id = $pLevelPointId";
			
	 MySqlQuery($sql);

	$sql = "select  count(*) as lpcount
	      from LevelPoints
	      where levelpoint_id = $pLevelPointId";
	
	if (CSql::singleValue($sql, 'lpcount') > 0)
	{
		raidError('Контрольная точка не удалена. ');
		return;
	} else {
		// сдвигаем все точки с большими порядоквыми номерами, чем текущая
		// с условием, что точка удалена (предыдущим запросом) - не сработало ограничение целостности
        	$sql = "update LevelPoints set levelpoint_order = levelpoint_order - 1
	        	where levelpoint_order > $LevelOrder and distance_id = $DistanceId";
		print $sql;	
	 	MySqlQuery($sql);
	}
	// конец проверки, что точка действительно удалилиась			
		

	 $statustext = CheckLevelPoints($DistanceId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }

	CMmb::setViews('ViewLevelPoints', 'Add');
}
// ============ Поднять точку (уменьшить порядковый номер)  =============
elseif ($action == 'LevelPointOrderDown')
{
	CMmb::setViews('ViewLevelPoints', 'Add');


	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку точки');
		return;
	}

        $pLevelPointId = mmb_validateInt($_POST, 'LevelPointId');
	if ($pLevelPointId <= 0)
	{
		raidError('Не определён ключ точки.');
		return;
	}
	

	$sql = "select  distance_id, levelpoint_order
	      from LevelPoints
	      where levelpoint_id = $pLevelPointId";

	$Row = CSql::singleRow($sql);
	$DistanceId = $Row['distance_id'];
	$LevelOrder = $Row['levelpoint_order'];


	$sql = "select  levelpoint_id, levelpoint_order
	      from LevelPoints
	      where levelpoint_order < $LevelOrder
	            and distance_id = $DistanceId
		    and levelpoint_hide = 0
	     order by levelpoint_order desc
	     LIMIT 0,1";
	
	$Row = CSql::singleRow($sql);
	$MaxLevelPointId = $Row['levelpoint_id'];
	$MaxLevelOrder = $Row['levelpoint_order'];

        if ($MaxLevelPointId == 0)
	{
		raidError('Нельзя уменьшить порядковый номер.');
		return;
	}
       
              
        $sql = "update LevelPoints set levelpoint_order = 0
	        where levelpoint_id = $pLevelPointId";
			
	 MySqlQuery($sql);


        $sql = "update LevelPoints set levelpoint_order = $MaxLevelOrder + 1
	        where levelpoint_id = $MaxLevelPointId";
			
	 MySqlQuery($sql);

	$sql = "update LevelPoints set levelpoint_order = $MaxLevelOrder
	        where levelpoint_id = $pLevelPointId";
			
	 MySqlQuery($sql);


	 $statustext = CheckLevelPoints($DistanceId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }
		

}
// ============ Опустить точку  (увеличить порядковый номер) =============
elseif ($action == 'LevelPointOrderUp')
{
	CMmb::setViews('ViewLevelPoints', 'Add');

	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку точки');
		return;
	}

        $pLevelPointId = mmb_validateInt($_POST, 'LevelPointId');
	if ($pLevelPointId <= 0)
	{
		raidError('Не определён ключ точки.');
		return;
	}
	
	$sql = "select  distance_id, levelpoint_order
	      from LevelPoints
	      where levelpoint_id = $pLevelPointId";
	 
	$Row = CSql::singleRow($sql);
	$DistanceId = $Row['distance_id'];
	$LevelOrder = $Row['levelpoint_order'];


	$sql = "select  levelpoint_id, levelpoint_order
	      from LevelPoints
	      where levelpoint_order > $LevelOrder
	            and distance_id = $DistanceId
		    and levelpoint_hide = 0
	     order by levelpoint_order asc
	     LIMIT 0,1";

	$Row = CSql::singleRow($sql);
	$MinLevelPointId = $Row['levelpoint_id'];
	$MinLevelOrder = $Row['levelpoint_order'];

	if ($MinLevelPointId == 0)
	{
		raidError('Нельзя увеличить порядковый номер.');
		return;
	}
       
        $sql = "update LevelPoints set levelpoint_order = 0
	        where levelpoint_id = $pLevelPointId";
			
	 MySqlQuery($sql);


        $sql = "update LevelPoints set levelpoint_order = $MinLevelOrder - 1
	        where levelpoint_id = $MinLevelPointId";
			
	 MySqlQuery($sql);
	
        $sql = "update LevelPoints set levelpoint_order = $MinLevelOrder
	        where levelpoint_id = $pLevelPointId";
			
	 MySqlQuery($sql);

	
	 $statustext = CheckLevelPoints($DistanceId);
	 if (!empty($error))
	 {
		$alert = 1;
	 }
}
// ============ просмотр интервалов амнистии  =============
elseif ($action == 'ViewLevelPointDiscountsPage')
{
	if ($RaidId <= 0)
	{
		CMmb::setErrorMessage('Id ММБ не указан');
		return;
	}
	
	// Есди дистанция не указана  - берём первую
	$DistanceId = mmb_validateInt($_POST, 'DistanceId');
        if ($DistanceId <= 0)
	{
		$sql = "select distance_id, distance_name from Distances where distance_hide = 0  and raid_id = $RaidId order by distance_id ";
	        $DistanceId = CSql::singleValue($sql, 'distance_id');
	}
	// Конец инициализации дистанции 
	CMmb::setViews('ViewLevelPointDiscounts', 'Add');
}
// ============  Добавить интервал амнистии  =============
elseif ($action == 'AddLevelPointDiscount')
{
	CMmb::setViews('ViewLevelPointDiscounts', 'Add');

	// Общая проверка возможности редактирования
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на ввод интервала');
		return;
	}

	$pDistanceId = (int)$_POST['DistanceId'];
        $pDiscountStart = (int)$_POST['DiscountStart'];
        $pDiscountFinish = (int)$_POST['DiscountFinish'];
        $pDiscountValue = (int)$_POST['DiscountValue'];
	$pLevelPointId = (int)$_POST['LevelPointId'];
                


	// тут по-хорошему нужны проверки
	if  (empty($pLevelPointId))
	{
		CMmb::setErrorSm('Не указана точка зачёта амнистии');
		return;
	}
      
	if  (empty($pDiscountValue) or ($pDiscountFinish < $pDiscountStart) or empty($pDiscountStart) or empty($pDiscountFinish))
	{
		CMmb::setErrorSm('Нулевая амнистия, пустое начало или конец; начало амнистии позже конца.');
		return;
	}

	$sql = " select count(*) as countresult
		  from LevelPointDiscounts
		  where levelpointdiscount_hide = 0  and distance_id = $pDistanceId
		        and (levelpointdiscount_start <= $pDiscountFinish and levelpointdiscount_finish >= $pDiscountStart)";
                
	if (((int)CSql::singleValue($sql, 'countresult')) > 0)
	{
		CMmb::setErrorSm('Интервал пересекается с существующим.');
		return;
	}

	$sql = " select count(*) as countresult
		  from LevelPoints
		  where levelpoint_hide = 0  and distance_id = $pDistanceId
		        and pointtype_id in (1,2,3,4)
		        and (levelpoint_order <= $pDiscountFinish
			and levelpoint_order >= $pDiscountStart)";

	if  (((int)CSql::singleValue($sql, 'countresult')) > 0)
	{
		CMmb::setErrorSm('Интервал содержит запрещённые для амнистии точки.');
		return;
	}

                
	// потом добавить время макс. и мин.
	     
	$sql = "insert into LevelPointDiscounts (distance_id, levelpointdiscount_start, levelpointdiscount_finish,
		levelpointdiscount_hide, levelpointdiscount_value, levelpoint_id)
		values ($pDistanceId, $pDiscountStart, $pDiscountFinish, 0, $pDiscountValue, $pLevelPointId)";
	// При insert должен вернуться послений id - это реализовано в MySqlQuery
	$LevelPointDiscountId = MySqlQuery($sql);

	if ($LevelPointDiscountId <= 0)
	{
		CMmb::setErrorSm('Ошибка записи новой точки.');
		return;
	}
	

 }
elseif ($action == "LevelPointDiscountInfo")
{
// Действие вызывается кнопокй "Править" в таблице интервалов
	CMmb::setViews('ViewLevelPointDiscounts', 'Edit');
}
 // ============ Правка интервала  =============
elseif ($action == 'LevelPointDiscountChange')
{
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку интервала');
		return;
	}

	CMmb::setViews('ViewLevelPointDiscounts', 'Add');
	
        $pLevelPointDiscountId = mmb_validateInt($_POST, 'LevelPointDiscountId');
	if ($pLevelPointDiscountId <= 0)
	{
		raidError('Не определён ключ интервала.');
		return;
	}

                

	$pDistanceId = (int)$_POST['DistanceId'];
	$pDiscountStart = (int)$_POST['DiscountStart'];
	$pDiscountFinish = (int)$_POST['DiscountFinish'];
	$pDiscountValue = (int)$_POST['DiscountValue'];
	$pLevelPointId = (int)$_POST['LevelPointId'];
                


         // тут по-хорошему нужны проверки
	if  (empty($pLevelPointId))
	{
		CMmb::setErrorSm('Не указана точка зачёта амнистии');
		return;
	}
     
     
	if  (empty($pDiscountValue) or ($pDiscountFinish < $pDiscountStart) or empty($pDiscountStart) or empty($pDiscountFinish))
	{
		raidError('Нулевая амнистия, пустое начало или конец; начало амнистии позже конца.');
		return;
	}

	$sql = " select count(*) as countresult
		 from LevelPointDiscounts
		 where levelpointdiscount_hide = 0  and distance_id = $pDistanceId
		        and  levelpointdiscount_id <> $pLevelPointDiscountId
		        and (levelpointdiscount_start <= $pDiscountFinish and levelpointdiscount_finish >= $pDiscountStart)";

	if  (((int)CSql::singleValue($sql, 'countresult')) > 0)
	{
		raidError('Интервал пересекается с существующим.');
		return;
	}

	$sql = " select count(*) as countresult
		  from LevelPoints
		  where levelpoint_hide = 0  and distance_id = $pDistanceId
		        and pointtype_id in (1,2,3,4)
		        and (levelpoint_order <= $pDiscountFinish
			and levelpoint_order >= $pDiscountStart)";

	if  (((int)CSql::singleValue($sql, 'countresult'))  > 0)
	{
		raidError('Интервал содержит запрещённые для амнистии точки.');
		return;
	}

	
		
        $sql = "update LevelPointDiscounts  set levelpointdiscount_value = $pDiscountValue
	                                ,levelpointdiscount_start = $pDiscountStart
	                                ,levelpointdiscount_finish = $pDiscountFinish
	                                ,levelpoint_id = $pLevelPointId
	       where 	levelpointdiscount_id = $pLevelPointDiscountId";
			
	//echo $sql;

	MySqlQuery($sql);

	$viewmode = "Add";
}
// ============ Удаление интервала  =============
elseif ($action == 'HideLevelPointDiscount')
{
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку интервала');
		return;
	}

        $pLevelPointDiscountId = mmb_validateInt($_POST, 'LevelPointDiscountId');
	if ($pLevelPointDiscountId <= 0)
	{
		raidError('Не определён ключ интервала.');
		return;
	}
	
	      
        $sql = "update LevelPointDiscounts set levelpointdiscount_hide = 1
	        where levelpointdiscount_id = $pLevelPointDiscountId";
			
	MySqlQuery($sql);

	CMmb::setViews('ViewLevelPointDiscounts', 'Add');
}
// ============ Пересоздание этапов =================================
elseif ($action == 'RecalculateLevels')
{
	if (!$Administrator && !$Moderator)
	{
		CMmb::setMessage('Нет прав на правку');
		return;
	}

	CMmb::setViews('ViewLevelPoints', 'Add');

 	 $pDistanceId = (int)$_POST['DistanceId'];

         if ($pDistanceId <= 0)
	 {
		CMmb::setErrorSm('Не определена дистанция.');
		return;
	 }
	 
	 $statustext = CheckLevelPoints($pDistanceId);
	 if (!empty($error))
	 {
		CMmb::setErrorSm("Нельзя создавать этапы: $statustext");
		return;
	 }

          // Нет проверки, что на одном этапе ровно одна амнистия


         // удаляем существующие этапы
         $sql = "update Levels set level_hide = 1 
	          where distance_id = $pDistanceId";
			
	 MySqlQuery($sql);

          // 2014-05-10 Исключилд старт и финиш из этапа	 
         // цикл по точкам
	 $sql = "select lp.levelpoint_id, pt.pointtype_id, pt.pointtype_name,  
	                lp.levelpoint_name, lp.levelpoint_penalty, 
	 	        lp.distance_id, lp.levelpoint_order,
		        lp.levelpoint_mindatetime, 
		        lp.levelpoint_maxdatetime, 
		        COALESCE(lpd.levelpointdiscount_value, 0) as levelpoint_discount
	 	 from LevelPoints lp
		      inner join PointTypes pt
		      on lp.pointtype_id = pt.pointtype_id
		      left outer join LevelPointDiscounts lpd
		      on lp.distance_id = lpd.distance_id
		         and lpd.levelpointdiscount_hide = 0
			 and lpd.levelpointdiscount_start <= lp.levelpoint_order
			 and lpd.levelpointdiscount_finish >= lp.levelpoint_order
		 where lp.levelpoint_hide = 0 and lp.distance_id = $pDistanceId
		 order by levelpoint_order";

	 
	 $Result = MySqlQuery($sql);

         $LevelId = 0;
         $LevelOrder = 0;
	 $LevelPoints = "";
	 $LevelPenalties  = "";
	 $LevelDiscountPoints = "";
	 $LevelDiscountValue = 0;
	  
 	 while ($Row = mysql_fetch_assoc($Result))
	 {


//               echo 'pt '.$Row['pointtype_id'];

               // 2014-05-10 Убрал старт и финиш из точек этапа 
               // Финиш или смена-карт - обновляем уже созданный уровень
               if (($Row['pointtype_id'] == 2 or $Row['pointtype_id'] == 4) and ($LevelId > 0))
	       {
		  $sqlPoint = "update LevelPoints set level_id = $LevelId
			       where levelpoint_id = {$Row['levelpoint_id']}";
			
		  MySqlQuery($sqlPoint);

  		  $sqlFinishLevel = "update Levels set   
                                                      level_minendtime = '{$Row['levelpoint_mindatetime']}'
						      ,level_endtime = '{$Row['levelpoint_maxdatetime']}'
						      ,level_pointnames = '".substr($LevelPoints, 1)."'		 
						      ,level_pointpenalties = '".substr($LevelPenalties, 1)."'		 
						      ,level_discountpoints = '".substr($LevelDiscountPoints, 1)."'		 
						      ,level_discount = $LevelDiscountValue
						      ,level_name = CONCAT(level_name, '".trim($Row['levelpoint_name'])."')		 
		 	            where level_id = $LevelId";
	          // echo $sqlFinishLevel;
	          MySqlQuery($sqlFinishLevel);
	       }       
               // Конец проверки на финиш или смену карт

               // Старт или смена-карт - записываем этап
	       // Для смены карт нужно не забыть, что точка привязывается к предыдущему этапу
               if ($Row['pointtype_id'] == 1 or $Row['pointtype_id'] == 4)
	       {

	         $LevelId = 0;
		 $LevelPoints = "";
		 $LevelPenalties  = "";
		 $LevelDiscountPoints = "";
		 $LevelDiscountValue = 0;
	         $LevelOrder++;

                 if ($Row['pointtype_id'] == 4)
		 {
		   $StartType = 3;
		 } else {
                    // Проверка на общий старт
		    if ($Row['levelpoint_mindatetime'] == $Row['levelpoint_maxdatetime']) 
		    {
		       $StartType = 2;
		    } else  {
 		       $StartType = 1;
		    }
                 }	         
		
		 $sqlStartLevel = "insert into Levels (distance_id, level_name, level_starttype,
		                                       level_hide, level_order, 
			                               level_begtime, level_maxbegtime)
			            values ($pDistanceId, '".trim($Row['levelpoint_name'])." - ', $StartType,
				              0, $LevelOrder,
				            '{$Row['levelpoint_mindatetime']}', '{$Row['levelpoint_maxdatetime']}')";
		
		 // При insert должен вернуться послений id - это реализовано в MySqlQuery
		 //echo $sqlStartLevel;
		 $LevelId = MySqlQuery($sqlStartLevel);

                 // Привязываем точку только в случае старта. 
		 // Для смены карт точка уже привязан к прредыдущему этапу
                 if ($Row['pointtype_id'] == 1)
		 {

		    $sqlPoint = "update LevelPoints set level_id = $LevelId
				       where levelpoint_id = {$Row['levelpoint_id']}";
			
		     MySqlQuery($sqlPoint);
		  }   
		  // Конец проверки на привязку точки старта

	       }       
               // Конец проверки на необходимость создать новый этап (старт или смену карт)
             
//	        echo  $Row['levelpoint_name'].'  '.$Row['pointtype_id']; 
	     
	       // Точка, кроме старта, финиша или смены карт 
	       if ($LevelId > 0 and $Row['pointtype_id'] <> 1  and $Row['pointtype_id'] <> 2 and $Row['pointtype_id'] <> 4)
	       {
	       
	          $sqlPoint = "update LevelPoints set level_id = $LevelId
			       where levelpoint_id = {$Row['levelpoint_id']}";
			
		  MySqlQuery($sqlPoint);

                  if ($Row['levelpoint_discount'] <> 0) 
		  {
		     if ($LevelDiscountValue == 0)
		     {
			$LevelDiscountValue = $Row['levelpoint_discount'];
                     }
	             $LevelDiscountPoints .= ",1";
		  } else {
                     $LevelDiscountPoints .= ",0";
		  }
	       
       	          $LevelPoints .= ",".$Row['levelpoint_name'];
		  $LevelPenalties .= ",".$Row['levelpoint_penalty'];
	       }       
               // Конец проверки, что точка не смена карт старт или финиш

	 }
         // Конец цикла 
	 mysql_free_result($Result);
}
// ============ Впечатления  =============
elseif ($action == 'ViewUsersLinksPage')
{
	if ($RaidId <= 0)
	{
		CMmb::setErrorMessage('Id ММБ не указан');
		return;
	}
	
//       	print '2';
	CMmb::setViews('ViewUsersLinks', '');
}
// ============ Значки  =============
elseif ($action == 'ViewAllBadgesPage')
{
	
  //     	print '1';
	CMmb::setViews('ViewAllBadges', '');
}

// ============ Никаких действий не требуется =================================
else
{
}

?>
