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



        // Обрабатываем зхагрузку файла
        if (!empty($_FILES['userfile']['name'][0]) and ($_FILES['userfile']['size'][0] > 0))
	{
           if  (substr(trim($_FILES['userfile']['type'][0]), 0, 6) != 'image/') 
	   {
			$statustext = 'Недопустимый тип файла.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
	   }
            
	   $UploadFile = $MyStoreFileLink . basename($_FILES['userfile']['name'][0]);

	   if (move_uploaded_file($_FILES['userfile']['tmp_name'][0], $UploadFile))
	   {
		// Успешно загрузили файл
		$pRaidLogoLink = $MyStoreHttpLink . basename($_FILES['userfile']['name'][0]);
	   } else {
			$statustext = 'Ошибка загрузки файла с эмблемой ММБ.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
           }
           // Конец проверки на успешность загрузки
	}
        // Конец проверки на указание в форме файла для загрузки
	
	
		
	// Добавляем/изменяем марш-бросок в базе

if ($action == "AddRaid")
	// Новая команда
	{
		$sql = "insert into Raids (raid_name, raid_period, raid_registrationenddate, 
		                           raid_logolink, raid_ruleslink, raid_startpoint, 
					   raid_startlink, raid_finishpoint, raid_closedate,
					   raid_znlink
					   )
			values (trim('".$pRaidName."'), trim('".$pRaidPeriod."'),  ";
		
		if ($pClearRaidRegistrationEndDate == 1) 
		{	
			$sql.=  " NULL ";
		} else {
			$sql.=  " '".$pRaidRegistrationEndDate."'";
		}
		$sql.= ", trim('".$pRaidLogoLink."') ";
		$sql.= ", trim('".$pRaidRulesLink."') ";
		$sql.= ", trim('".$pRaidStartPointName."') ";
		$sql.= ", trim('".$pRaidStartLink."') ";
		$sql.= ", trim('".$pRaidFinishPointName."') ";

		if ($pClearRaidCloseDate == 1) 
		{	
			$sql.=  " NULL ";
		} else {
			$sql.=  " '".$pRaidCloseDate."'";
		}
	
		$sql.= ", trim('".$pRaidZnLink."') ";
		$sql.= ")";
		// При insert должен вернуться послений id - это реализовано в MySqlQuery

            //    echo $sql;

		$RaidId = MySqlQuery($sql);
		// Поменялся TeamId, заново определяем права доступа
//		GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage);
		if ($RaidId <= 0)
		{
			$statustext = 'Ошибка записи нового ММБ.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}


//		$sql = "insert into TeamUsers (team_id, user_id) values (".$TeamId.", ".$NewUserId.")";
//		MySqlQuery($sql);
		// Теперь нужно открыть на просмотр
		$viewmode = "";
	}
	else
	// Изменения в уже существующей команде
	{
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
// ============ Никаких действий не требуется =================================
else
{
}

?>
