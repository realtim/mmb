<?php
// +++++++++++ Показ/редактирование данных команды ++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";

// ================ Добавляем новую команду ===================================
if ($viewmode == 'Add')
{
	if (($RaidId <= 0) || ($UserId <= 0))
	{
		CMmb::setErrorMessage('Для регистрации новой команды обязателен идентификатор пользователя и ММБ');
		return;
	}

	// Если запрещено создавать команду - молча выходим, сообщение уже выведено в teamaction.php
	//if (!CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange)) return;
	if (!CRights::canCreateTeam($UserId, $RaidId))
		return;


	$Sql = "select user_email from Users where user_id = $UserId";
	$UserEmail = CSql::singleValue($Sql, 'user_email');

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$TeamNum = (int) $_POST['TeamNum'];
		$TeamName = CMmbUi::toHtml($_POST['TeamName']);
		$DistanceId = $_POST['DistanceId'];
		$TeamUseGPS = mmb_isOn($_POST, 'TeamUseGPS');
		$TeamMapsCount = (int)$_POST['TeamMapsCount'];
		$TeamRegisterDt = 0;
		$TeamGreenPeace = mmb_isOn($_POST, 'TeamGreenPeace');
	}
	else
	// Пробуем создать команду первый раз
	{
		$TeamNum = 'Номер';
		$TeamName = 'Название команды';
		$DistanceId = 0;
		$TeamUseGPS = 0;
		$TeamMapsCount = 0;
		$TeamRegisterDt = 0;
		$TeamGreenPeace = 0;
	}

	// Определяем следующее действие
	$NextActionName = 'AddTeam';
	// Действие на текстовом поле по клику
	$OnClickText = ' onClick="javascript:this.value = \'\';"';
	// Надпись на кнопке
	$SaveButtonText = 'Создать команду';
	$UnionButtonText = 'Добавить в объединение';
}

else
// ================ Редактируем/смотрим существующую команду =================
{
	// Проверка нужна только для случая регистрация новой команды
	// Только тогда Id есть в переменной php, но нет в вызывающей форме
	if ($TeamId <= 0)
	{
		// Должна быть определена команда, которую смотрят
		return;
	}

	$sql = "select t.team_num, t.distance_id, t.team_usegps, t.team_name,
		t.team_mapscount, t.team_registerdt, team_waitdt,
		t.team_greenpeace,
		TIME_FORMAT(t.team_result, '%H:%i') as team_result,
		CASE WHEN DATE(t.team_registerdt) > r.raid_registrationenddate
			THEN 1
			ELSE 0
		END as team_late,
		d.distance_resultlink
		from Teams t
			inner join Distances d on t.distance_id = d.distance_id
			inner join Raids r on d.raid_id = r.raid_id
		where t.team_id = $TeamId";
	$Row = CSql::singleRow($sql);

	$TeamRegisterDt = $Row['team_registerdt'];
	$TeamResult = $Row['team_result'];
	$TeamWait = $Row['team_waitdt'];
	$TeamLate = (int)$Row['team_late'];
	$DistanceResultLink = $Row['distance_resultlink'];

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$TeamNum = (int) $_POST['TeamNum'];
		$TeamName = $_POST['TeamName'];
		$DistanceId = $_POST['DistanceId'];
		$TeamUseGPS = mmb_isOn($_POST, 'TeamUseGPS');
		$TeamMapsCount = (int)$_POST['TeamMapsCount'];
		$TeamGreenPeace = mmb_isOn($_POST, 'TeamGreenPeace');
	}
	else
	{
		$TeamNum = $Row['team_num'];
		$TeamName = $Row['team_name'];
		$DistanceId = $Row['distance_id'];
		$TeamUseGPS = $Row['team_usegps'];
		$TeamMapsCount = (int)$Row['team_mapscount'];
		$TeamGreenPeace = $Row['team_greenpeace'];
	}

	$TeamName = CMmbUi::toHtml($TeamName);

	$NextActionName = 'TeamChangeData';
	$AllowEdit = 0;
	$OnClickText = '';
	$SaveButtonText = 'Сохранить данные команды';
	$UnionButtonText = 'Добавить в объединение';
}
// ================ Конец инициализации переменных команды =================

// Определяем права по редактированию команды
if (($viewmode == "Add") || CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange))
{
	$AllowEdit = 1;
	$DisabledText = '';
	$OnSubmitFunction = 'return ValidateTeamDataForm();';
}
else
{
	$AllowEdit = 0;
	$DisabledText = ' disabled';
	$OnSubmitFunction = 'return false;';
}

// Определяем права по просмотру результатов
if (($viewmode <> "Add") && CanViewResults($Administrator, $Moderator, $RaidStage))
	$AllowViewResults = 1;
else $AllowViewResults = 0;

// Получаем параметры марш-броска
$sql = "select r.raid_name, r.raid_registrationenddate, raid_mapprice, raid_teamslimit,
	DATE_SUB(MIN(lp.levelpoint_mindatetime), INTERVAL COALESCE(r.raid_readonlyhoursbeforestart, 8) HOUR) as raid_editend
	from Raids r, Distances d, LevelPoints lp
	where r.raid_id = $RaidId and d.raid_id = r.raid_id and lp.distance_id = d.distance_id and lp.levelpoint_mindatetime is not NULL and lp.levelpoint_mindatetime > 0";
$Row = CSql::singleRow($sql);
$RaidName = $Row['raid_name'];
$RegistrationEnd = $Row['raid_registrationenddate'] . " 23:59";
$EditEnd = substr($Row['raid_editend'], 0, -3);
$MapPrice = $Row['raid_mapprice'];
$TeamsLimit = $Row['raid_teamslimit'];

// Получаем количество зарегистрированных команд
$sql = "select count(*) as teamscount from Raids r, Distances d, Teams t
	where r.raid_id = $RaidId and r.raid_id = d.raid_id and d.distance_id = t.distance_id
	and t.team_hide = 0 and t.team_outofrange = 0";
$TeamsCount = CSql::singleValue($sql, 'teamscount');

// Получаем количество команд в листе ожидания
$sql = "select count(*) as waitcount from Raids r, Distances d, Teams t
	where r.raid_id = $RaidId and r.raid_id = d.raid_id and d.distance_id = t.distance_id
	and t.team_hide = 0 and t.team_waitdt is not NULL";
$WaitCount = CSql::singleValue($sql, 'waitcount');

// 21.03.2014 Ищем ссылку на положение в загруженных файлах
$RaidRulesLink = CSql::raidFileLink($RaidId, 1, false);

// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateTeamDataForm()
	{
		document.TeamDataForm.action.value = "<? echo $NextActionName; ?>";
		return true;
	}

	// Удалить команду
	function HideTeam()
	{
		document.TeamDataForm.action.value = 'HideTeam';
		document.TeamDataForm.submit();
	}

	// Удалить пользователя
	function HideTeamUser(teamuserid)
	{
		document.TeamDataForm.HideTeamUserId.value = teamuserid;
		document.TeamDataForm.action.value = 'HideTeamUser';
		document.TeamDataForm.submit();
	}

	// Функция отмены редактирования команды
	function CancelEdit()
	{
		document.TeamDataForm.action.value = "CancelChangeTeamData";
		document.TeamDataForm.submit();
	}

	// Функция отмены создания команды
	function CancelAdd()
	{
		document.TeamDataForm.action.value = "ViewRaidTeams";
		document.TeamDataForm.submit();
	}

	// 25.11.2013 Для совместимости оставил
	// Указать этап схода пользователя
	function TeamUserOut(teamuserid, levelid)
	{
		document.TeamDataForm.HideTeamUserId.value = teamuserid;
		document.TeamDataForm.UserOutLevelId.value = levelid;
		document.TeamDataForm.action.value = 'TeamUserOut';
		document.TeamDataForm.submit();
	}

	// 25.11.2013 Новый вариант с точкой
	// Указать точку неявки участника
	function TeamUserNotInPoint(teamuserid, levelpointid)
	{
		document.TeamDataForm.HideTeamUserId.value = teamuserid;
		document.TeamDataForm.UserNotInLevelPointId.value = levelpointid;
		document.TeamDataForm.action.value = 'TeamUserNotInPoint';
		document.TeamDataForm.submit();
	}

	// Функция объединения команд
	function AddTeamInUnion()
	{
		document.TeamDataForm.action.value = "AddTeamInUnion";
		document.TeamDataForm.submit();
	}

	// Функция перевода команды в зачет
	function InviteTeam()
	{
		document.TeamDataForm.action.value = "InviteTeam";
		document.TeamDataForm.submit();
	}


</script>

<?php
// Выводим начало формы с командой
print('<form name="TeamDataForm" action="'.$MyPHPScript.'#'.$TeamNum.'" method="post" onSubmit="'.$OnSubmitFunction.'">'."\n");
print('<input type="hidden" name="action" value="">'."\n");
print('<input type="hidden" name="TeamId" value="'.$TeamId.'">'."\n");
print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
print('<input type="hidden" name="HideTeamUserId" value="0">'."\n");
print('<input type="hidden" name="UserOutLevelId" value="0">'."\n");
print('<input type="hidden" name="UserNotInLevelPointId" value="0">'."\n");
print('<input type="hidden" name="UserId" value="0">'."\n\n");
if (($viewmode == "Add") && !$Moderator && !$Administrator)
	// В новой команде, которую заводит не модератор/администратор, будет единственный участник - тот, который создал команду
	print('<input type="hidden" name="NewTeamUserEmail" size="50" value="'.$UserEmail.'" >'."\n");

// ============ Показываем шапку перед таблицей о том, что можно и что нельзя
if ($viewmode == "Add")
// Создание новой команды
{
	print('<strong>Создание новой команды на ММБ '.$RaidName.'</strong><br/><input type="hidden" name="TeamNum" value="0"><br/>'."\n");
	if ($RaidStage <= 1)
	// Регистрация открыта
	{
		print('Регистрация открыта до '.$RegistrationEnd.".\n");
/*
		if ($WaitCount || ($TeamsLimit && ($TeamsCount >= $TeamsLimit)))
		// Места закончились
		{
			print('Свободные места на ММБ закончились. Вы можете создать команду, участвующую <b>вне зачета</b>.<br/>'."\n");
			print('Такая команда не отмечается судьями на дистанции, не получает места в итоговом протоколе и имеет ряд других ограничений.'."\n");
			print('Подробнее смотрите в <a href="'.$RaidRulesLink.'">Положении</a>.<br/>'."\n");
			print('Свободное место появится, если одна из команд в зачете решит удалить себя.'."\n");
			if ($WaitCount) print('В листе претендентов на свободное место перед Вами будет команд: '.$WaitCount.".\n");
		}
		else
		// Места еще есть
		{
			print('Регистрация открыта до '.$RegistrationEnd.".\n");
			if ($TeamsLimit) print('Осталось мест '.($TeamsLimit - $TeamsCount).' из '.$TeamsLimit.".\n");
		}
		// Выводим только пока открыта регистрация, потом в свой состав смогут добавлять только команды вне зачета

*/
		print('<br/>Если Вы хотите участвовать в ММБ в составе другой команды, то не создавайте свою, а попросите участников той команды добавить Вас в ее состав.'."\n");
	}
	else if ($RaidStage <= 3)
	// Регистрация закрыта, марш-бросок не начался, можно создавать команды только вне зачета
	{
		print('Регистрация на ММБ закончилась '.$RegistrationEnd.'<br/>'."\n");


/*
		print('Регистрация на ММБ закончилась '.$RegistrationEnd.'. Сейчас Вы можете создать команду, участвующую <b>вне зачета</b>.<br/>'."\n");
		print('Такая команда не отмечается судьями на дистанции, не получает места в итоговом протоколе и имеет ряд других ограничений.<br/>'."\n");
		print('Подробнее смотрите в <a href="'.$RaidRulesLink.'">Положении</a>.'."\n");
*/
	}
	else
	// марш-бросок начался и возможно закончился, можно создавать команды только вне зачета
	{

/*
		print('Если Вы участвовали в ММБ вне зачета, то вы можете создать команду и самостоятельно ввести ее результаты на дистанции.'."\n");
		print('Результаты не будут проверяться судьями, а команда не получит места в итоговом протоколе'."\n");
*/
	}
	print('<br/>Если Вы хотите добавить других участников в свою команду, то Вы сможете это сделать после создания команды.'."\n");
}
else
// Редактирование / просмотр команды
{
	print('Команда N <b>'.$TeamNum.'</b> на ММБ '.$RaidName.' <input type="hidden" name="TeamNum" value="'.$TeamNum.'"><br/>'."\n");
	$RegisterDtFontColor = ($TeamLate == 1) ? '#BB0000' : '#000000';
	print('Зарегистрирована <span style="color: '.$RegisterDtFontColor.';">'.$TeamRegisterDt.'</span>'."\n\n");

	if ($TeamUser and $TeamOutOfRange)
	// Пользователь смотрит свою команду. Предупредим его, если команда вне зачета.
	{
		print('<br/>Ваша команда <b>ожидает приглашения</b>.<br/>'."\n");
//		print('<br/>Ваша команда зарегистрирована <b>вне зачета</b>.<br/>'."\n");
	//	print('Такая команда не отмечается судьями на дистанции, не получает места в итоговом протоколе и имеет ряд других ограничений.<br/>'."\n");
		print('Подробнее смотрите в <a href="'.$RaidRulesLink.'">Положении</a>.'."\n");
	}


/*
	if ($TeamsLimit and ($TeamWait <> '') and ($RaidStage <= 1))
	// Команда в листе ожидания и еще может попасть в зачет. Сообщим ее шансы.
	{
		$sql = "select count(*) as positioninwl from Raids r, Distances d, Teams t
			where r.raid_id = $RaidId and r.raid_id = d.raid_id and d.distance_id = t.distance_id
			and t.team_hide = 0 and t.team_waitdt is not NULL and t.team_waitdt <= '$TeamWait'";
		$PositionInWL = CSql::singleValue($sql, 'positioninwl');
		print('<br/>Команда находится в <b>листе ожидания</b> на '.$PositionInWL.' месте.<br/>'."\n");
		print('Первая команда из листа ожидания будет переведена в зачет, если одна из команд в зачете решит удалить себя до '.$RegistrationEnd.".<br/>\n");
	}
*/
	if ($TeamOutOfRange and $TeamsLimit and CanEditOutOfRange($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange))
	// Сообщаем администратору/модератору, стоит ли команду переводить в зачет
	{
		print('Всего зарегистрировано в зачете '.$TeamsCount.' команд из '.$TeamsLimit.".\n");
	}
}

print('<table border="0" cellpadding="2" cellspacing="0" style="padding-top: 10px;">'."\n\n");
$TabIndex = 0;

print('<tr><td class="input">'."\n");
// ============ Дистанция
print('Дистанция '."\n");

// 22-03-2016 Отключение через disabled для списка (select) приводит к тому, что перестает передаваться информация о дистанции
// поставил анализ

// 21.03.2016 Определяем, когда можно и когда нельзя менять дистанцию
// при вводе дистанцию можно менять всегда до закрытия протокола
// при правке дистанцию можно менять только до закрытия регистрации участнику
// и нельзя не участнику, если он не модератор  для команд в зачете
// для команд вне зачета до закрытия проткола
	if (
		($viewmode == 'Add' and CSql::raidStage($RaidId) < 7)
		or ($viewmode <> 'Add' and CRights::canEditTeam($UserId, $RaidId, $TeamId))
	   )
	{
		$DisabledDistance =  0;
	} else {
		$DisabledDistance = 1;
	}


if (!$DisabledDistance) {

	// Показываем выпадающий список дистанций
	print('<select name="DistanceId" class="leftmargin" tabindex="'.(++$TabIndex).'">'."\n");
	$sql = "select distance_id, distance_name from Distances where distance_hide = 0 and raid_id = $RaidId";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		$distanceselected = ($Row['distance_id'] == $DistanceId ? 'selected' : '');
		print('<option value="'.$Row['distance_id'].'" '.$distanceselected.' >'.$Row['distance_name']."</option>\n");
	}
	mysql_free_result($Result);
	print('</select>'."\n");

	
}  else {

	print('<input type="hidden" name="DistanceId" size="50" value="'.$DistanceId.'" tabindex="'.(++$TabIndex).'">'."\n");
	print('<select name="DistanceDisabledId" class="leftmargin" tabindex="'.(++$TabIndex).'" disabled>'."\n");
	$sql = "select distance_id, distance_name from Distances where distance_hide = 0 and raid_id = $RaidId";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		$distanceselected = ($Row['distance_id'] == $DistanceId ? 'selected' : '');
		print('<option value="'.$Row['distance_id'].'" '.$distanceselected.' >'.$Row['distance_name']."</option>\n");
	}
	mysql_free_result($Result);
	print('</select>'."\n");

}
// Конец проверки на блокировку выбора дистанции

print('</td></tr>'."\n\n");

// ============ Название команды
print('<tr><td class="input"><input type="text" name="TeamName" size="50" value="'.$TeamName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($TeamName))
	.' title="Название команды"></td></tr>'."\n\n");

print('<tr><td class="input">'."\n");




// ============ Вне зачета

if ($RaidId <=27) {
	// Если регистрация команды, то атрибут "вне зачёта" не может изменить даже администратор - этот параметр рассчитывается по времени
	// администратор может поменять флаг при правке
	if ($viewmode <> "Add" and CanEditOutOfRange($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange))
		$DisabledTextOutOfRange = '';
	else
		$DisabledTextOutOfRange = 'disabled';

	print('Вне зачета! <input type="checkbox" name="TeamOutOfRange" value="on"'.(($TeamOutOfRange == 1) ? ' checked="checked"' : '')
		.' tabindex="'.(++$TabIndex).'" '.$DisabledTextOutOfRange.' title="Команда вне зачета"/> &nbsp;'."\n");
} else {

 	if ($TeamOutOfRange) {
		print('Ожидает приглашения!&nbsp;'."\n");
 	}


	// 09/06/2016 Покащзываем кнопку активации
	if (CRights::canInviteTeam($UserId, $TeamId))
	{
		print('<input type="button" onClick="javascript: InviteTeam();" name="InviteTeamButton" value="Пригласить команду" tabindex="'.(++$TabIndex).'">'."\r\n");
	
	}	
}

print('</td></tr>'."\n\n");
print('<tr><td class="input">'."\n");

// ============ Использование GPS
print('GPS <input type="checkbox" name="TeamUseGPS" value="on"'.(($TeamUseGPS == 1) ? ' checked="checked"' : '')
	.' tabindex="'.(++$TabIndex).'"'.$DisabledText
	.' title="Отметьте, если команда использует для ориентирования GPS"/> &nbsp;'."\n");

// ============ Число карт
print('&nbsp; Комплектов карт <input type="text" name="TeamMapsCount" size="2" maxlength="2" value="'.$TeamMapsCount.'" tabindex="'.(++$TabIndex).'"'
	.$OnClickText.$DisabledText.' title="Число заказанных на команду комплектов карт">&nbsp;'."\n");

// ============ расчет стоимости
// для новых команд мы еще не знаем количество заказанных карт,
// а командам вне зачета мы ничего не обещаем и только сообщаем, почем они могут купить карты на старте при их наличии
if (($viewmode == "Add") || $TeamOutOfRange)
{
	$sql = "select r.raid_mapprice from Raids r where r.raid_id = $RaidId";
	$MapPrice = CSql::singleValue($sql, 'raid_mapprice');
	print('(стоимость одного комплекта '.$MapPrice.' руб.)'."\n");
}
else if ($TeamUser or $Administrator or $Moderator)
	// показываем стоимость карт только при просмотре своей команды или админам/модераторам
	print('К оплате на старте: <b>'.CalcualteTeamPayment($TeamId).'</b> руб. &nbsp;'."\n");

print('</td></tr>'."\n\n");

// ============ Нет сломанным унитазам!
print('<tr><td class="input">'."\n");
print('<a href="http://community.livejournal.com/_mmb_/2010/09/24/">Нет сломанным унитазам!</a> - прочитали и поддерживаем <input type="checkbox" name="TeamGreenPeace" value="on"'.(($TeamGreenPeace >= 1) ? ' checked="checked"' : '')
	.' tabindex="'.(++$TabIndex).'"'.$DisabledText.' title="Отметьте, если команда берёт повышенные экологические обязательства"/>'."\n");
print("</td></tr>\r\n");

// ============ Участники
// Их еще нет при создании команды
if ($viewmode <> "Add")
{
	print('<tr><td class="input" style="padding-top: 10px;">'."\n");

	// Если команда в зачете и пользователь смотрит свою команду, то напомним ему о возможности удалять участников и сообщим deadline
	// Правильность состава команд вне зачета нас не особо интересует
	if ($TeamUser and !$TeamOutOfRange and $AllowEdit)
		print('Если кто-то из участников не сможет участвовать в ММБ, удалите его до '.$EditEnd.".\n");

	$sql = "select tu.teamuser_id,
			tu.teamuser_notstartraidid,
			r.raid_nostartprice,
			CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name, u.user_birthyear, u.user_id, COALESCE(tld.levelpoint_id, 0) as levelpoint_id
		from TeamUsers tu
			inner join Users u
			on tu.user_id = u.user_id
			left outer join TeamLevelDismiss tld
			on tu.teamuser_id = tld.teamuser_id
			left outer join Raids r
			on tu.teamuser_notstartraidid = r.raid_id
		where tu.teamuser_hide = 0 and team_id = $TeamId";
	$Result = MySqlQuery($sql);

	while ($Row = mysql_fetch_assoc($Result))
	{
		$userName = CMmbUI::toHtml($Row['user_name']);
		print('<div style="margin-top: 5px;">'."\n");
		if ($AllowEdit)
		{
			print('<input type="button" style="margin-right: 15px;" onClick="javascript:if (confirm(\'Вы уверены, что хотите удалить участника: '.$userName.'? \')) { HideTeamUser('.$Row['teamuser_id'].'); }" name="HideTeamUserButton" tabindex="'.(++$TabIndex).'" value="Удалить">'."\n");
		}

		// Показываем только если можно смотреть результаты марш-броска
		// (так как тут есть список этапов)
		if ($AllowViewResults)
		{
			print('Неявка в: <select name="UserNotInPoint'.$Row['teamuser_id'].'" style="width: 100px; margin-right: 15px;" title="Точка, в которую не явился участник" onChange="javascript:if (confirm(\'Вы уверены, что хотите отметить неявку участника: '.$userName.'? \')) { TeamUserNotInPoint('.$Row['teamuser_id'].', this.value); }" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
			$sqllevelpoints = "select levelpoint_id, levelpoint_name from LevelPoints lp where lp.distance_id = $DistanceId order by levelpoint_order";
			$ResultLevelPoints = MySqlQuery($sqllevelpoints);
			$userlevelpointselected = ($Row['levelpoint_id'] == 0 ? ' selected' : '');
			print('<option value="0"'.$userlevelpointselected.'>-</option>'."\n");
			while ($RowLevelPoints = mysql_fetch_assoc($ResultLevelPoints))
			{
				$userlevelpointselected = ($RowLevelPoints['levelpoint_id'] == $Row['levelpoint_id'] ? 'selected' : '');
				print('<option value="'.$RowLevelPoints['levelpoint_id'].'"'.$userlevelpointselected.'>'.$RowLevelPoints['levelpoint_name']."</option>\n");
			}
			mysql_free_result($ResultLevelPoints);
			print('</select>'."\n");
		}

		// ФИО и год рождения участника
		print("<a href=\"?UserId={$Row['user_id']}\">$userName</a> {$Row['user_birthyear']}\n");

		// Отметка невыхода на старт в предыдущем ММБ
		if ($Row['teamuser_notstartraidid'] > 0)
			print(' <a title="Участник был заявлен, но не вышел на старт в прошлый раз" href="#comment">(?!)</a> ');

		print("</div>\n");
	}

	mysql_free_result($Result);
	print("</td></tr>\n");
}
// Закончили вывод списка участников

// ============ Новый участник
// Возможность добавлять участников заканчивается вместе с возможностью создавать команды
// Обычный пользователь может добавлять новых участников при редактировании своей команды
// Модератор/Администратор могут создавать новые команды с другим участником вместо себя
//if (($AllowEdit == 1) && CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange) &&
//	(($viewmode <> "Add") || $Moderator || $Administrator))


// 21.03.2016 Определяем, когда можно добавлять нового пользователя
// при добавлении команды можно только модераторам или администраторам
// при правке - в зависимости от типа команды

if (       ($viewmode <> 'Add' and CRights::canEditTeam($UserId, $RaidId, $TeamId))
    	or ($viewmode == 'Add' and (CSql::userAdmin($UserId) or CSql::userModerator($UserId, $RaidId)) and CSql::raidStage($RaidId) < 7)
    )
{
	print('<tr><td class="input" style="padding-top: 10px;">'."\n");

	// Предупредим команды в зачете о том, что они могут добавлять участников только до закрытия регистрации
	if ($TeamUser and !$TeamOutOfRange)
	{
		print('Добавление новых участников в команду разрешено до '.$RegistrationEnd.".<br/>\n");
		print('После этой даты они могут участвовать в ММБ только в виде самостоятельной команды вне зачета.<br/>'."\n");
	}

	print('<input type="text" name="NewTeamUserEmail" size="50" value="Email нового участника" tabindex="'.(++$TabIndex) .'"'
		. CMmbUI::placeholder('Email нового участника') . 'title="Укажите e-mail пользователя, которого Вы хотите добавить в команду. Пользователь может запретить добавлять себя в команду в настройках своей учетной записи.">'."\n");
	print("</td></tr>\n");
}

// 20/02/2014 Пользовательское соглашение
if (($viewmode == "Add") && ($AllowEdit == 1) )
{
	print('<tr><td class="input" style="padding-top: 10px; font-size: 80%;">'."\n");
	print('<b>Условия участия (выдержка из <a href="'.$RaidRulesLink.'">положения</a>):</b><br/>'."\n");

	// Ищем последнее пользовательское соглашение
	$ConfirmFile = trim($MyStoreHttpLink).CSql::raidFileName(null, 8, true); 

	$Fp = fopen($ConfirmFile, "r");
	while ((!feof($Fp)) && (!strpos(trim(fgets($Fp, 4096)),'body')));
	$NowStr = '';
	while ((!feof($Fp)) && (!strpos(trim($NowStr),'/body')))
	{
		print(trim($NowStr)."\r\n");
		$NowStr = fgets($Fp, 4096);
	}
	fclose($Fp);

	print("</td></tr>\r\n");

	print('<tr><td class="input">'."\n");
	print("<a href=\"$RaidRulesLink\">Полный текст положения</a><br/>\n");
	print('Прочитал и согласен с условиями участия в ММБ <input type="checkbox" name="Confirmation" value="on" tabindex="'.(++$TabIndex).'"'.$DisabledText.' title="Прочитал и согласен с условиями участия в ММБ"/>'."\n");
	print("</td></tr>\r\n");
}
// конец блока пользовательского соглашения

// ================ Submit для формы ==========================================
if ($AllowEdit == 1)
{
	print('<tr><td class="input" style="padding-top: 10px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateTeamDataForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
	print('<select name="view" class="leftmargin" tabindex="'.(++$TabIndex).'">'."\n");
	if ($viewmode == 'Add')
	{
		print('<option value="ViewTeamData" selected>и перейти к карточке команды</option>'."\n");
		print('<option value="ViewRaidTeams">и перейти к списку команд</option>'."\n");
		print('</select>'."\n");
		print('<input type="button" onClick="javascript: CancelAdd();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	}
	else
	{
		print('<option value="ViewTeamData">и остаться на этой странице</option>'."\n");
		print('<option value="ViewRaidTeams" selected>и перейти к списку команд</option>'."\n");
		print('</select>'."\n");
		print('<input type="button" onClick="javascript: CancelEdit();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	}
	print('</td></tr>'."\n\n");

	// Кнопка удаления всей команды для тех, кто имеет право
	if ($viewmode <> "Add")
	{
		print('<tr><td class="input" style="padding-top: 10px;">');
		// Попросим членов команды в зачете удалить свою команду, если они передумали участвовать
		// Команды вне зачета нас не интересуют
		if ($TeamUser and !$TeamOutOfRange)
		{
			print('Если Ваша команда не сможет участвовать в ММБ, пожалуйста, удалите ее до '.$EditEnd.".<br/>\n");
			// Напомним о тех, кто в листе ожидания
			if (($RaidStage <= 1) and $WaitCount)
				print('Если Вы успеете удалить свою команду до '.$RegistrationEnd.', то первая из '.$WaitCount.' команд в листе ожидания сможет участвовать в зачете благодаря Вам.<br/>'."\n");
		}
		print('<input type="button" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить команду: '.trim($TeamName).'? \')) {HideTeam();}" name="HideTeamButton" value="Удалить команду" tabindex="'.(++$TabIndex).'"> </td></tr>'."\n");
	}

	// для Администратора/Модератора добавляем кнопку "Объединить"
	if (($Administrator or $Moderator) and $viewmode <> 'Add' and $TeamOutOfRange == 0)
	{
		print('<tr><td class="input" style="padding-top: 10px;">'."\r\n");
		print('<input type="button" onClick="javascript: AddTeamInUnion();" name="UnionButton" value="'.$UnionButtonText.'" tabindex="'.(++$TabIndex).'">'."\r\n");
		print('</td></tr>'."\r\n");
	}
}

print("</table></form>\n");
?>
