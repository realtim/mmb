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
	$pRaidLogoLink = $_POST['RaidLogoLink'];
	$pRaidRulesLink = $_POST['RaidRulesLink'];
	$pRaidStartPointName = $_POST['RaidStartPointName'];
	$pRaidStartLink = $_POST['RaidStartLink'];
	$pRaidFinishPointName = $_POST['RaidFinishPointName'];
	$pRaidCloseDate = $_POST['RaidCloseDate'];
	$pClearRaidCloseDate = (isset($_POST['ClearRaidCloseDate']) && ($_POST['ClearRaidCloseDate'] == 'on')) ? 1 : 0;
	$pRaidZnLink = $_POST['RaidZnLink'];
        $pRaidDistancesCount = (int)$_POST['RaidDistancesCount'];
        $pRaidNoShowResult = (isset($_POST['RaidNoShowResult']) && ($_POST['RaidNoShowResult'] == 'on')) ? 1 : 0;
	

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

		$sql = "insert into Raids (raid_name, raid_period, raid_registrationenddate, 
		                           raid_logolink, raid_ruleslink, raid_startpoint, 
					   raid_startlink, raid_finishpoint, raid_closedate,
					   raid_znlink
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
				$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink) 
				        values (".$RaidId.", 'Общая', '','')";
		    
				$rs = MySqlQuery($sql);
		    
		    } else {

			$AddDistanceCounter =  0;
			while ($AddDistanceCounter < $pRaidDistancesCount) 
			{
				$AddDistanceCounter++;
			
				$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink) 
				        values (".$RaidId.", 'Дистанция".$AddDistanceCounter."', '','')";

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
	// Изменения в уже существующей команде
	{


		// Проверяем, что текущее чимсло дистанций не больше, чем указано
		$sql = "select count(*) as resultcount
			from Distances 
			where  raid_id = ".$RaidId; 

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
			
			$sql = "insert into Distances (raid_id, distance_name, distance_data, distance_resultlink) 
			        values (".$RaidId.", 'Дистанция".$AddDistanceCounter."', '','')";

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
		$sql.=  " , raid_logolink = trim('".$pRaidLogoLink."') "; 
		$sql.=  " , raid_ruleslink = trim('".$pRaidRulesLink."') "; 
		$sql.=  " , raid_startpoint =  trim('".$pRaidStartPointName."') ";
		$sql.=  " , raid_startlink = trim('".$pRaidStartLink."') ";
		$sql.=  " , raid_finishpoint = trim('".$pRaidFinishPointName."') ";
                $sql.=  " , raid_closedate =  ";
					   
		if ($pClearRaidCloseDate == 1) 
		{	
			$sql.=  " NULL ";
		} else {
			$sql.=  " '".$pRaidCloseDate."'";
		}
	
		$sql.= ", raid_znlink = trim('".$pRaidZnLink."') ";
		$sql.= ", raid_noshowresult = ".$pRaidNoShowResult." ";
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
			$viewsubmode = "ReturnAfterError";
			return;
	}
        // Конец проверки на пустое название 

        		
        $pDistanceName = $_POST['DistanceName'.$pDistanceId];

	// Проверка на пустое название 
	if (empty($pDistanceName)) 
	{
			$statustext = 'Пустое название дистанции.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
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
// ============ Никаких действий не требуется =================================
else
{
}

?>
