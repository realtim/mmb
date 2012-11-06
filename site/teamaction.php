<?php
// +++++++++++ Обработчик действий, связанных с командой +++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// Для всех обработок, кроме ViewRaidTeams,
// требуем, чтобы пользователь вошёл в систему

// ============ Обработка возможности регистрации команды =====================
if ($action == "RegisterNewTeam")
{
	$view = "ViewTeamData";
	$viewmode = "Add";
	if ($RaidId <= 0)
	{
		$statustext = "Не указан ММБ (выберите из списка).";
		$alert = 0;
		return;
	}

	// Проверка возможности создать команду
	if (!CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage))
	{
		$statustext = "Регистрация на марш-бросок закрыта";
		$alert = 0;
		return;
	}

	// Проверка, что пользователь не включен в команду на выбранном ММБ
	$sql = "select t.team_num
		from TeamUsers tu
			inner join Teams t on tu.team_id = t.team_id
			inner join Distances d on t.distance_id = d.distance_id
		where d.raid_id = '.$RaidId.' and tu.teamuser_hide = 0 and tu.user_id = ".$UserId;
	$rs = MySqlQuery($sql);
	$row = mysql_fetch_assoc($rs);
	mysql_free_result($rs);
	$TeamNum = $row['team_num'];
	if ($TeamNum > 0)
	{
		$statustext = "Уже есть команда c Вашим участием (N ".$row['team_num'].")";
		$alert = 0;
		return;
	}
}

// ============ Изменение данных команды или запись новой команды =============
elseif ($action == 'TeamChangeData' or $action == "AddTeam")
{
	if ($action == "AddTeam") $viewmode = "Add"; else $viewmode = "";
	$view = "ViewTeamData";

	// пока валим всё в одну кучу - проверяем ниже
	$pDistanceId = (int)$_POST['DistanceId'];
	$pTeamNum = (int) $_POST['TeamNum'];
	$pTeamName = $_POST['TeamName'];
	$pTeamUseGPS = (isset($_POST['TeamUseGPS']) && ($_POST['TeamUseGPS'] == 'on')) ? 1 : 0;
	$pTeamMapsCount = (int)$_POST['TeamMapsCount'];
	$pTeamGreenPeace = (isset($_POST['TeamGreenPeace']) && ($_POST['TeamGreenPeace'] == 'on')) ? 1 : 0;
	$pNewTeamUserEmail = $_POST['NewTeamUserEmail'];
	if (!isset($_POST['TeamNotOnLevelId'])) $_POST['TeamNotOnLevelId'] = "";
	$pTeamNotOnLevelId = (int)$_POST['TeamNotOnLevelId'];

	if (($action <> "AddTeam") && ($TeamId <= 0))
	{
		$statustext = "Не найден идентификатор команды.";
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	if (empty($pDistanceId))
	{
		$statustext = "Не указана дистанция.";
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	if ($RaidId <= 0)
	{
		$statustext = "Не указан ММБ.";
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	if (trim($pTeamName) == '')
	{
		$statustext = "Не указано название.";
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	if (($pTeamMapsCount <= 0) || ($pTeamMapsCount > 15))
	{
		$statustext = "Не указано число карт или недопустимое число карт.";
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	// Проверяем, нет ли уже команды с таким названием
	$sql = "select count(*) as resultcount
		from Teams t
			inner join Distances d
			on t.distance_id = d.distance_id
		where d.raid_id = ".$RaidId." and trim(team_name) = '".$pTeamName."' and team_hide = 0 and team_id <> ".$TeamId;
	$rs = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($rs);
	mysql_free_result($rs);
	if ($Row['resultcount'] > 0)
	{
		$statustext = "Уже есть команда с таким названием.";
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}
	// Проверяем номер команды: если новая - 0 и такого номера не должно быть
	$sql = "select count(*) as resultcount
		from Teams t
			inner join Distances d
			on t.distance_id = d.distance_id
		where d.raid_id = '.$RaidId.' and team_num = '.$pTeamNum.' and team_hide = 0 and team_id <> ".$TeamId;
	$rs = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($rs);
	mysql_free_result($rs);
	if ($Row['resultcount'] > 0)
	{
		$statustext = "Уже есть команда с таким номером.";
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}

	if ($OldMmb and $pTeamNum <= 0)
	{
		$statustext = "Для ММБ до 2012 года нужно указывать номер команды.";
		$alert = 1;
		$viewsubmode = "ReturnAfterError";
		return;
	}

	// Проверяем email нового участника команды
	if (!empty($pNewTeamUserEmail) and trim($pNewTeamUserEmail) <> 'Email нового участника')
	{
		$sql = "select user_id, user_prohibitadd from Users where ltrim(COALESCE(user_password, '')) <> '' and user_hide = 0 and trim(user_email) = trim('".$pNewTeamUserEmail."')";
		$rs = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($rs);
		mysql_free_result($rs);
		$NewUserId = $Row['user_id'];
		$UserProhibitAdd = $Row['user_prohibitadd'];
		if (empty($NewUserId))
		{
			$statustext = 'Пользователь с таким email не найден.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
		// Проверка на запрет включения в команду
		if ($UserProhibitAdd and $NewUserId <> $UserId and !$Moderator)
		{
			$NewUserId = 0;
			$statustext = 'Пользователь запретил добавлять себя в команду другим пользователям.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
		// Проверка на наличие пользователя в другой команде
		$sql = "select count(*) as result
			from TeamUsers tu
				inner join Teams t on tu.team_id = t.team_id
				inner join Distances d on t.distance_id = d.distance_id
			where teamuser_hide = 0 and d.raid_id = ".$RaidId." and user_id = ".$NewUserId;
		$rs = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($rs);
		mysql_free_result($rs);
		if ($Row['result'] > 0)
		{
			$NewUserId = 0;
			$statustext = 'Пользователь с таким email уже включен в другую команду';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
		if (!CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage))
		{
			$NewUserId = 0;
			$statustext = 'Добавление новых участников закрыто';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
	}
	else
	{
		// Проверяем, что для новой команды передали email участника
		if ($action == "AddTeam")
		{
			$NewUserId = 0;
			$statustext = "Для новой команды должен быть указан email участника.";
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
		$NewUserId = 0;
	} // Конец проверки на корректную передачу email

	// Общая проверка возможности редактирования
	if (($viewmode == "Add") && !CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage))
	{
		$statustext = "Регистрация на марш-бросок закрыта";
		$alert = 0;
		return;
	}
	if (($viewmode <> "Add") && !CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage))
	{
		$statustext = "Изменения в команде запрещены";
		$alert = 0;
		return;
	}

	// Добавляем/изменяем команду в базе
	$TeamActionTextForEmail = "";
	if ($action == "AddTeam")
	// Новая команда
	{
		$sql = "insert into Teams (team_num, team_name, team_usegps, team_mapscount, distance_id,
			team_registerdt, team_greenpeace)
			values (";
		// Номер команды
		if ($OldMmb) $sql = $sql.$pTeamNum;
		else
		{
			$sql = $sql."(select COALESCE(MAX(t.team_num), 0) + 1
				from Teams t
					inner join Distances d on t.distance_id = d.distance_id
				where d.raid_id = ".$RaidId.")";
		}
		// Все остальное
		$sql = $sql.", '".$pTeamName."',".$pTeamUseGPS.",".$pTeamMapsCount.", ".$pDistanceId.",NOW(), "
			.$pTeamGreenPeace.")";
		// При insert должен вернуться послений id - это реализовано в MySqlQuery
		$TeamId = MySqlQuery($sql);
		// Поменялся TeamId, заново определяем права доступа
		GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage);
		if ($TeamId <= 0)
		{
			$statustext = 'Ошибка записи новой команды.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError";
			return;
		}
		$sql = "insert into TeamUsers (team_id, user_id) values (".$TeamId.", ".$NewUserId.")";
		MySqlQuery($sql);
		$TeamActionTextForEmail = "создана команда";
		$SendEmailToAllTeamUsers = 1;
		// Теперь нужно открыть на просмотр
		$viewmode = "";
	}
	else
	// Изменения в уже существующей команде
	{
		$TeamActionTextForEmail = "изменение данных команды";
		$SendEmailToAllTeamUsers = 0;
		$sql = "update Teams set team_name = trim('".$pTeamName."'),
				distance_id = ".$pDistanceId.",
				team_usegps = ".$pTeamUseGPS.",
				team_greenpeace = ".$pTeamGreenPeace.",
				team_mapscount = ".$pTeamMapsCount."
			where team_id = ".$TeamId;
		$rs = MySqlQuery($sql);

		// Если добавляли участника
		if ($NewUserId > 0)
		{
			$sql = "insert into TeamUsers (team_id, user_id) values (".$TeamId.", ".$NewUserId.")";
			MySqlQuery($sql);
			$sql = "select user_name from Users where user_id = ".$NewUserId;
			$Result = MySqlQuery($sql);
			$Row = mysql_fetch_assoc($Result);
			mysql_free_result($Result);
			$NewUserName = $Row['user_name'];
			$TeamActionTextForEmail = "добавлен участник ".$NewUserName;
		}
	}
	// Конец разных вариантов действий при создании и редактировании команды

	// Обновляем результат команды (реально нужно только при изменения этапа невыхода команды)
	if ($UserId > 0 and $TeamId > 0) RecalcTeamResult($TeamId);

	// Отправляем письмо всем участникам команды об изменениях
	// Кроме того, кто вносил изменения, если $SendEmailToAllTeamUsers <> 1
	if ($UserId > 0 and $TeamId > 0)
	{
		$sql = "select user_name from Users where user_id = ".$UserId;
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);
		$ChangeDataUserName = $Row['user_name'];
		$sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
			from Users u
				inner join TeamUsers tu on tu.user_id = u.user_id
				inner join Teams t on tu.team_id = t.team_id
				inner join Distances d on t.distance_id = d.distance_id
				inner join Raids r on d.raid_id = r.raid_id
			where tu.teamuser_hide = 0 and tu.team_id = ".$TeamId;
		if ($SendEmailToAllTeamUsers <> 1) $sql = $sql." and u.user_id <> ".$UserId;
		$sql = $sql." order by tu.teamuser_id asc";
		$Result = MySqlQuery($sql);
		while ($Row = mysql_fetch_assoc($Result))
		{
			// Формируем сообщение
			$Msg = "Уважаемый участник ".$Row['user_name']."!\n\n";
			$Msg = $Msg."Действие: ".$TeamActionTextForEmail.".\n";
			$Msg = $Msg."Команда N ".$Row['team_num'].", Дистанция: ".$Row['distance_name'].", ММБ: ".trim($Row['raid_name']).".\n";
			$Msg = $Msg."Автор изменений: ".$ChangeDataUserName.".\n";
			$Msg = $Msg."Вы можете увидеть результат на сайте и при необходимости внести свои изменения.\n\n";
			$Msg = $Msg."P.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
			// Отправляем письмо
			SendMail($Row['user_email'], $Msg, $Row['user_name']);
		}
		mysql_free_result($Result);
	}
	// Конец отправки писем

	// Если передали альтернативную страницу, на которую переходить (пока только одна возможность - на список команд)
	$view = $_POST['view'];
	if (empty($view)) $view = "ViewTeamData";
}

// ============ Информация о команде по номеру ================================
elseif ($action == 'FindTeam')
{
	if (isset($_REQUEST['TeamNum'])) $TeamNum = $_REQUEST['TeamNum']; else $TeamNum = "";
	if (($TeamNum == '') || ($TeamNum == 'Номер команды'))
	{
		$statustext = 'Не указан номер команды';
		$view = "";
		return;
	}
	$sql = "select team_id from Teams t
			inner join Distances d on t.distance_id = d.distance_id
		where d.raid_id = ".$RaidId." and t.team_hide = 0 and t.team_num = ".(int)$TeamNum;
	$rs = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($rs);
	mysql_free_result($rs);
	$TeamId = $Row['team_id'];
	// Поменялся TeamId, заново определяем права доступа
	GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage);
	if ($TeamId <= 0)
	{
		$statustext = 'Команда с номером '.(int)$TeamNum.' не найдена';
		$view = "";
		return;
	}
	$view = "ViewTeamData";
}

// ============ Информация о команде по Id ====================================
elseif ($action == 'TeamInfo')
{
	if ($TeamId <= 0)
	{
		$statustext = 'Id команды не указан';
		$alert = 1;
		return;
	}
	$view = "ViewTeamData";
	$viewmode = "";
}

// ============ Удаление участника команды ====================================
elseif ($action == 'HideTeamUser')
{
	$HideTeamUserId = $_POST['HideTeamUserId'];
	if ($HideTeamUserId <= 0)
	{
		$statustext = 'Участник не найден';
		$alert = 1;
		return;
	}
	if ($TeamId <= 0)
	{
		$statustext = 'Команда не найдена';
		$alert = 1;
		return;
	}
	if ($RaidId <= 0)
	{
		$statustext = 'Марш-бросок не найден';
		$alert = 1;
		return;
	}
	if ($SessionId <= 0)
	{
		$statustext = 'Сессия не найдена';
		$alert = 1;
		return;
	}

	// Проверка возможности редактировать команду
	if (!CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage))
	{
		$statustext = "Изменения в команде запрещены";
		$alert = 1;
		return;
	}

	// Смотрим, был ли это последний участник или нет
	$sql = "select count(*) as result from TeamUsers where teamuser_hide = 0 and team_id = ".$TeamId;
	$rs = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($rs);
	mysql_free_result($rs);
	$TeamUserCount = $Row['result'];
	if ($TeamUserCount > 1)
	// Кто-то ещё остается
	{
		$sql = "update TeamUsers set teamuser_hide = 1 where teamuser_id = ".$HideTeamUserId;
		$rs = MySqlQuery($sql);
		$view = "ViewTeamData";
	}
	else
	// Это был последний участник
	{
		$sql = "update TeamUsers set teamuser_hide = 1 where teamuser_id = ".$HideTeamUserId;
		$rs = MySqlQuery($sql);
		$sql = "update Teams set team_hide = 1 where team_id = ".$TeamId;
		$rs = MySqlQuery($sql);
		$view = "";
	}

	// Отправить письмо всем участникам команды об удалении
	// Кроме того, кто удалял
	if ($UserId > 0 and $TeamId > 0)
	{
		$sql = "select user_name from Users where user_id = ".$UserId;
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
		$ChangeDataUserName = $Row['user_name'];
		mysql_free_result($Result);
		$sql = "select user_name from Users u inner join TeamUsers tu on tu.user_id = u.user_id where tu.teamuser_id = ".$HideTeamUserId;
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
		$DelUserName = $Row['user_name'];
		mysql_free_result($Result);
		$sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
			from Users u
				inner join TeamUsers tu on tu.user_id = u.user_id
				inner join Teams t on tu.team_id = t.team_id
				inner join Distances d on t.distance_id = d.distance_id
				inner join Raids r on d.raid_id = r.raid_id
			where tu.teamuser_id = ".$HideTeamUserId." or (tu.teamuser_hide = 0 and tu.team_id = ".$TeamId." and u.user_id <> ".$UserId.")
			order by tu.teamuser_id asc";
		$Result = MySqlQuery($sql);

		if ($TeamUserCount > 1)
		// В команде еще осталось как минимум 2 участника
		{
			while ($Row = mysql_fetch_assoc($Result))
			{
				// Формируем сообщение
				if (trim($DelUserName) <> trim($Row['user_name']))
					$Msg = "Уважаемый участник ".$Row['user_name']."!\n\nИз Вашей команды (N ".$Row['team_num'].", Дистанция: ".trim($Row['distance_name']).", ММБ: ".trim($Row['raid_name']).") был удален участник: ".$DelUserName.".\nАвтор изменений: ".$ChangeDataUserName.".\nВы можете увидеть результат на сайте и при необходимости внести свои изменения.\n\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
				else
					$Msg = "Уважаемый участник ".$Row['user_name']."!\n\nВы были удалены из команды (N ".$Row['team_num'].", Дистанция: ".trim($Row['distance_name']).", ММБ: ".trim($Row['raid_name']).")\nАвтор изменений: ".$ChangeDataUserName.".\nВы можете увидеть результат на сайте и при необходимости внести свои изменения.\n\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
				// Отправляем письмо
				SendMail($Row['user_email'], $Msg, $Row['user_name']);
			}
		}
		elseif ($TeamUserCount == 1)
		{
			$Row = mysql_fetch_assoc($Result);
			$Msg = "Уважаемый участник ".$Row['user_name']."!\n\nВаша команда (N ".$Row['team_num'].", Дистанция: ".trim($Row['distance_name']).", ММБ: ".trim($Row['raid_name']).") была удалена.\nАвтор изменений: ".$ChangeDataUserName.".\nВы можете увидеть результат на сайте и при необходимости внести свои изменения.\n\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
			// Отправляем письмо
			SendMail($Row['user_email'], $Msg, $Row['user_name']);
		}
		mysql_free_result($Result);
	}
	// Конец отправки писем об удалении
}

// ============ Смена этапа схода участника команды ===========================
elseif ($action == 'TeamUserOut')
{
	$HideTeamUserId = $_POST['HideTeamUserId'];
	if ($HideTeamUserId <= 0)
	{
		$statustext = 'Участник не найден';
		$alert = 1;
		return;
	}
	// Здесь может быть 0 этап - значит, что участник нигде не сходил
	$LevelId = $_POST['UserOutLevelId'];
	if ($LevelId < 0)
	{
		$statustext = 'Этап не найден';
		$alert = 1;
		return;
	}
	if ($TeamId <= 0)
	{
		$statustext = 'Команда не найдена';
		$alert = 1;
		return;
	}
	if ($RaidId <= 0)
	{
		$statustext = 'Не найден ММБ.';
		$alert = 1;
		return;
	}
	if ($SessionId <= 0)
	{
		$statustext = 'Не найдена сессия.';
		$alert = 1;
		return;
	}

	// Проверка возможности редактировать результаты
	if (!CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage))
	{
		$statustext = 'Изменение результатов команды запрещено';
		$alert = 1;
		return;
	}

	$sql = "update TeamUsers set level_id = ".($LevelId > 0 ? $LevelId : 'null' )." where teamuser_id = ".$HideTeamUserId;
	$rs = MySqlQuery($sql);
	$view = "ViewTeamData";

	// Письмо об изменениях	всем, кроме автора изменений
	// !!! Сход относится к результатам на дистанции и об их изменений письма слать не надо
	$sql = "select user_name from Users where user_id = ".$UserId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);
	$ChangeDataUserName = $Row['user_name'];
	$sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
		from Users u
			inner join TeamUsers tu on tu.user_id = u.user_id
			inner join Teams t on tu.team_id = t.team_id
			inner join Distances d on t.distance_id = d.distance_id
			inner join Raids r on d.raid_id = r.raid_id
		where tu.teamuser_hide = 0 and tu.team_id = ".$TeamId." and u.user_id <> ".$UserId."
		order by tu.teamuser_id asc";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		// Формируем сообщение
		$Msg = "Уважаемый участник ".$Row['user_name']."!\n\n";
		$Msg = $Msg."Действие: изменение данных команды.\n";
		$Msg = $Msg."Команда N ".$Row['team_num'].", Дистанция: ".$Row['distance_name'].", ММБ: ".trim($Row['raid_name']).".\n";
		$Msg = $Msg."Автор изменений: ".$ChangeDataUserName.".\n";
		$Msg = $Msg."Вы можете увидеть результат на сайте и при необходимости внести свои изменения.\n\n";
		$Msg = $Msg."P.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
		// Отправляем письмо
		SendMail($Row['user_email'], $Msg, $Row['user_name']);
	}
	mysql_free_result($Result);
}

// ============ Обратимое удаление команды ====================================
elseif ($action == 'HideTeam')
{
	if ($TeamId <= 0)
	{
		$statustext = 'Команда не найдена';
		$alert = 1;
		return;
	}
	if ($RaidId <= 0)
	{
		$statustext = 'Марш-бросок не найден';
		$alert = 1;
		return;
	}
	if ($SessionId <= 0)
	{
		$statustext = 'Сессия не найдена';
		$alert = 1;
		return;
	}

	// Проверка возможности удалить команду
	if (!CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage))
	{
		$statustext = "Удаление команды запрещено";
		$alert = 1;
		return;
	}

	// Уведомление всем. в т.ч тому, кто удалял
	if (($UserId > 0) && ($TeamId > 0))
	{
		$Sql = "select user_name from Users where user_id = ".$UserId;
		$Result = MySqlQuery($Sql);
		$Row = mysql_fetch_assoc($Result);
		$ChangeDataUserName = $Row['user_name'];
		mysql_free_result($Result);
	}

	$sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
		from Users u
			inner join TeamUsers tu on tu.user_id = u.user_id
			inner join Teams t on tu.team_id = t.team_id
			inner join Distances d on t.distance_id = d.distance_id
			inner join Raids r on d.raid_id = r.raid_id
		where tu.teamuser_hide = 0 and tu.team_id = ".$TeamId."
		order by tu.teamuser_id asc";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		// Формируем сообщение
		$Msg = "Уважаемый участник ".$Row['user_name']."!\n\nВаша команда (N ".$Row['team_num'].", Дистанция: ".trim($Row['distance_name']).", ММБ: ".trim($Row['raid_name']).") была удалена.\nАвтор изменений: ".$ChangeDataUserName.".\n\n\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
		// Отправляем письмо
		SendMail($Row['user_email'], $Msg, $Row['user_name']);
	}
	mysql_free_result($Result);

	$sql = "update TeamUsers set teamuser_hide = 1 where team_id = ".$TeamId;
	$rs = MySqlQuery($sql);
	$sql = "update Teams set team_hide = 1 where team_id = ".$TeamId;
	$rs = MySqlQuery($sql);

	$view = "ViewRaidTeams";
}

// ============ Отмена изменений в команде ====================================
elseif ($action == "CancelChangeTeamData")
{
	$view = "ViewTeamData";
}

// ============ Действие вызывается ссылкой Отмена ============================
elseif ($action == "ViewRaidTeams")
{
	$view = "ViewRaidTeams";
}
// =============== Получение JSON экмпорта для жедающих анализировтаь протокол ===================
// Сначала поместил в административный, но там нет доступа, кроме администратора
elseif ($action == 'JsonExport')
{

	if (!isset($_REQUEST['RaidId'])) {$_REQUEST['RaidId'] = "";}

	$RaidId = $_REQUEST['RaidId'];


// Берём марш-бросок, который передан
// Если такого нет - берём последний
// Проверяем, что передали идентификатор ММБ


if (empty($RaidId))
{
  $sql = "select raid_id
 	  from Raids 
 	  order by raid_registrationenddate desc
	  LIMIT 0,1 ";

  $Result = MySqlQuery($sql);
  $Row = mysql_fetch_assoc($Result);
  $RaidId = $Row['raid_id'];
  mysql_free_result($Result);

}



// Сбор данных для дампа
$data = array();

// Raids: raid_id, raid_name, raid_registrationenddate
$Sql = "select raid_id, raid_name 
        from Raids 
	where raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Raids"][] = $Row; }
mysql_free_result($Result);

// Distances: distance_id, raid_id, distance_name
$Sql = "select distance_id, raid_id, distance_name 
        from Distances
	where raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Distances"][] = $Row; }
mysql_free_result($Result);

// Levels: level_id, distance_id, level_name, level_order, level_starttype, level_pointnames, level_pointpenalties, level_begtime, level_maxbegtime, level_minendtime, level_endtime
$Sql = "select level_id, l.distance_id, level_name, level_order, level_starttype, 
               level_pointnames, level_pointpenalties, level_begtime, level_maxbegtime,
	       level_minendtime, level_endtime, level_discountpoints, level_discount
        from Levels l 
	     inner join Distances d on l.distance_id = d.distance_id 
        where d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Levels"][] = $Row; }
mysql_free_result($Result);

// LevelPoints: levelpoint_id, level_id, pointtype_id
/*
$Sql = "select levelpoint_id, lp.level_id, pointtype_id from LevelPoints lp inner join Levels l on lp.level_id = l.level_id  inner join Distances d on l.distance_id = d.distance_id where d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["LevelPoints"][] = $Row; }
mysql_free_result($Result);
*/

// Teams: team_id, distance_id, team_name, team_num // *
$Sql = "select team_id, t.distance_id, team_name, team_num, team_usegps, team_greenpeace,
        level_id, team_progress, team_result 
        from Teams t 
	     inner join Distances d on t.distance_id = d.distance_id 
	where t.team_hide = 0  and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Teams"][] = $Row; }
mysql_free_result($Result);

// Users: user_id, user_name, user_birthyear // *
// Добавил олграничение - только по текущему ММБ
$Sql = "select u.user_id, u.user_name, u.user_birthyear 
        from Users u
	     inner join TeamUsers tu on u.user_id = tu.user_id
	     inner join Teams t on tu.team_id = t.team_id 
	     inner join Distances d on t.distance_id = d.distance_id 
	where u.user_hide = 0 and t.team_hide = 0 and tu.teamuser_hide = 0 and d.raid_id = ".$RaidId;

$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Users"][] = $Row; }
mysql_free_result($Result);

// TeamUsers: teamuser_id, team_id, user_id, teamuser_hide
$Sql = "select teamuser_id, tu.team_id, tu.user_id, tu.level_id 
        from TeamUsers tu 
	     inner join Teams t on tu.team_id = t.team_id 
	     inner join Distances d on t.distance_id = d.distance_id 
	where t.team_hide = 0 and tu.teamuser_hide = 0 and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamUsers"][] = $Row; }
mysql_free_result($Result);


// TeamLevels: 
$Sql = "select teamlevel_id, tl.team_id, tl.level_id, 
	       teamlevel_begtime, teamlevel_endtime,
	       teamlevel_points, teamlevel_comment,
	       teamlevel_progress, teamlevel_penalty,
	       error_id, teamlevel_duration 
        from TeamLevels tl 
	     inner join Teams t on tl.team_id = t.team_id 
	     inner join Distances d on t.distance_id = d.distance_id 
	where t.team_hide = 0 and tl.teamlevel_hide = 0 and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamUsers"][] = $Row; }
mysql_free_result($Result);


// TeamLevelDismiss: user_id, levelpoint_id, team_id, teamuser_id, teamleveldismiss_date, device_id
/*
$Sql = "select user_id, levelpoint_id, tld.team_id, teamuser_id, teamleveldismiss_date, device_id from TeamLevelDismiss tld inner join Teams t on tld.team_id = t.team_id inner join Distances d on t.distance_id = d.distance_id where t.team_hide = 0  and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamLevelDismiss"][] = $Row; }
mysql_free_result($Result);
*/
// TeamLevelPoints: user_id, levelpoint_id, team_id, teamlevelpoint_date, device_id, teamlevelpoint_datetime, teamlevelpoint_points, teamlevelpoint_comment
/*
$Sql = "select user_id, levelpoint_id, tlp.team_id, teamlevelpoint_date, device_id, teamlevelpoint_datetime, teamlevelpoint_points, teamlevelpoint_comment from TeamLevelPoints tlp inner join Teams t on tlp.team_id = t.team_id inner join Distances d on t.distance_id = d.distance_id where t.team_hide = 0  and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamLevelPoints"][] = $Row; }
mysql_free_result($Result);
*/
// Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
header("Content-Type: application/octet-stream");
header("Content-Disposition: attachment; filename=\"mmbdata.json\"");

// Вывод json
print json_encode( $data );




  // Можно не прерывать, но тогда нужно написать обработчик в index, чтобы не выводить дальше ничего
  die();
  return;
}
// ============ Никаких действий не требуется =================================
else
{
}

?>
