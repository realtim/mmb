<?php

// По идее, для всех действий должно передаваться SessionId через post
// Исключение действия UserLogin и переход по ссылке из письма - там стартует сессия прям на этой странице
// и передачи через форму не происходит
// м.б. стоит яано прописать для каких action м.б.пустая сессия
if (empty($SessionId))
{
	$SessionId = $_POST['sessionid'];
}

// Текущий пользователь
$NowUserId = GetSession($SessionId);

if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";

// ================ Добавляем новую команду ===================================
if ($viewmode == 'Add')
{
	$RaidId = $_REQUEST['RaidId'];
	if (empty($RaidId) or empty($NowUserId))
	{
		$statustext = 'Для регистрации новой команды обязателен идентификатор пользователя и ММБ';
		$alert = 1;
		return;
	}

	$Sql = "select user_email from Users where user_id = ".$NowUserId;
	$Result = MySqlQuery($Sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);
	$UserEmail = $Row['user_email'];

	// Новая команда
	$TeamId = 0;

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$TeamNum = (int) $_POST['TeamNum'];
		$TeamName = str_replace( '"', '&quot;', $_POST['TeamName']);
		$DistanceId = $_POST['DistanceId'];
		$TeamUseGPS = (isset($_POST['TeamUseGPS']) && ($_POST['TeamUseGPS'] == 'on')) ? 1 : 0;
		$TeamMapsCount = (int)$_POST['TeamMapsCount'];
		$TeamRegisterDt = 0;
		$TeamConfirmResult = ($_POST['TeamConfirmResult'] == 'on' ? 1 : 0);
		$ModeratorConfirmResult = ($_POST['ModeratorConfirmResult'] == 'on' ? 1 : 0);
		$TeamGreenPeace = (isset($_POST['TeamGreenPeace']) && ($_POST['TeamGreenPeace'] == 'on')) ? 1 : 0;
		$TeamNotOnLevelId = $_POST['TeamNotOnLevelId'];
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
		$TeamConfirmResult = 0;
		$ModeratorConfirmResult = 0;
		$TeamGreenPeace = 0;
		$TeamNotOnLevelId = 0;
	}

	$TeamUser = 0;

	// Определяем следующее действие
	$NextActionName = 'AddTeam';
	// Действие на текстовом поле по клику
	$OnClickText = ' onClick="javascript:this.value = \'\';"';
	// Надпись на кнопке
	$SaveButtonText = 'Зарегистрировать';
}

else

// ================ Редактируем/смотрим существующую команду =================
{
	// Проверка нужна только для случая регистрация новой команды
	// Только тогда Id есть в переменной php, но нет в вызывающей форме
	if (!isset($_REQUEST['TeamId'])) $_REQUEST['TeamId'] = "";
	if (empty($TeamId))
	{
		$TeamId = $_REQUEST['TeamId'];
	}

	if ($TeamId <= 0)
	{
		// Должна быть определена команда, которую смотрят
		return;
	}

	$sql = "select t.team_num, t.distance_id, t.team_usegps, t.team_name,
		t.team_mapscount, d.raid_id, t.team_registerdt,
		t.team_confirmresult, t.team_moderatorconfirmresult,
		t.team_greenpeace, t.level_id,
		TIME_FORMAT(t.team_result, '%H:%i') as team_result,
		CASE WHEN t.team_registerdt >= r.raid_registrationenddate
			THEN 1
			ELSE 0
		END as team_late,
		d.distance_resultlink
		from Teams t
			inner join Distances d on t.distance_id = d.distance_id
			inner join Raids r on d.raid_id = r.raid_id
		where t.team_id = ".$TeamId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);

	// Эти данные всегда берём из базы
	$RaidId = $Row['raid_id'];
	$TeamRegisterDt = $Row['team_registerdt'];
	$TeamResult = $Row['team_result'];
	$TeamLate = (int)$Row['team_late'];
	$DistanceResultLink = $Row['distance_resultlink'];

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$TeamNum = (int) $_POST['TeamNum'];
		$TeamName = str_replace( '"', '&quot;', $_POST['TeamName']);
		$DistanceId = $_POST['DistanceId'];
		$TeamUseGPS = ($_POST['TeamUseGPS'] == 'on' ? 1 : 0);
		$TeamMapsCount = (int)$_POST['TeamMapsCount'];
		$TeamConfirmResult = ($_POST['TeamConfirmResult'] == 'on' ? 1 : 0);
		$ModeratorConfirmResult = ($_POST['ModeratorConfirmResult'] == 'on' ? 1 : 0);
		$TeamGreenPeace = ($_POST['TeamGreenPeace'] == 'on' ? 1 : 0);
		$TeamNotOnLevelId = $_POST['TeamNotOnLevelId'];
	}
	else
	{
		$TeamNum = $Row['team_num'];
		$TeamName = str_replace( '"', '&quot;', $Row['team_name']);
		$DistanceId = $Row['distance_id'];
		$TeamUseGPS = $Row['team_usegps'];
		$TeamMapsCount = (int)$Row['team_mapscount'];
		$TeamConfirmResult = $Row['team_confirmresult'];
		$ModeratorConfirmResult = $Row['team_moderatorconfirmresult'];
		$TeamGreenPeace = $Row['team_greenpeace'];
		$TeamNotOnLevelId = $Row['level_id'];
	}

	if (CheckTeamUser($SessionId, $TeamId))	$TeamUser = 1;
	else $TeamUser = 0;

	$NextActionName = 'TeamChangeData';
	$AllowEdit = 0;
	$OnClickText = '';
	$SaveButtonText = 'Сохранить данные команды';
}
// ================ Конец инициализации переменных команды =================


// Определяем статус пользователя
// К этому моменту, что для новой команды, что для существующей
// уже известен ммб

// Получаем данные о временных ограничениях ММБ
// и о том, не является ли ММБ старым (проводился до 2012 года)
$sql = "select r.raid_resultpublicationdate, r.raid_registrationenddate,
	CASE WHEN r.raid_registrationenddate is not null and YEAR(r.raid_registrationenddate) <= 2011
		THEN 1
		ELSE 0
	END as oldmmb,
	CASE WHEN r.raid_registrationenddate is not null and r.raid_registrationenddate <= NOW()
		THEN 1
		ELSE 0
	END as showresultfield
	from Raids r
	where r.raid_id = ".$RaidId;
$Result = MySqlQuery($sql);
$Row = mysql_fetch_assoc($Result);
mysql_free_result($Result);
$RaidPublicationResultDate = $Row['raid_resultpublicationdate'];
$RaidRegistrationEndDate = $Row['raid_registrationenddate'];
$OldMmb = $Row['oldmmb'];
$RaidShowResultField = $Row['showresultfield'];

// Должна быть определена дата окончания регистрации
if (empty($RaidRegistrationEndDate))
{
	return;
}

// Является ли пользователь модератором данного ММБ
if (CheckModerator($SessionId, $RaidId)) $Moderator = 1;
else $Moderator = 0;

// !!! Нужно сделать запрет создавать/редактировать команду
// не модераторам после начала ММБ (кроме марш-бросков до 2012 года)

// Общее правило для возможности редактирования
if (($viewmode == "Add") || $Moderator || ($TeamUser && !$ModeratorConfirmResult))
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

// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateTeamDataForm()
	{
		document.TeamDataForm.action.value = "<? echo $NextActionName; ?>";
		return true;
	}
	// Конец проверки правильности заполнения формы

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

	// Функция отмены изменения
	function Cancel()
	{
		document.TeamDataForm.action.value = "CancelChangeTeamData";
		document.TeamDataForm.submit();
	}

	// Посмотреть профиль пользователя
	function ViewUserInfo(userid)
	{
		document.TeamDataForm.UserId.value = userid;
		document.TeamDataForm.action.value = 'UserInfo';
		document.TeamDataForm.submit();
	}

	// Указать этап схода пользователя
	function TeamUserOut(teamuserid, levelid)
	{
		document.TeamDataForm.HideTeamUserId.value = teamuserid;
		document.TeamDataForm.UserOutLevelId.value = levelid;
		document.TeamDataForm.action.value = 'TeamUserOut';
		document.TeamDataForm.submit();
	}
</script>

<?
// Выводим начало формы с командой
print('<form name="TeamDataForm" action="'.$MyPHPScript.'" method="post" onSubmit="'.$OnSubmitFunction.'">'."\n");
print('<input type="hidden" name="sessionid" value="'.$SessionId.'">'."\n");
print('<input type="hidden" name="action" value="">'."\n");
print('<input type="hidden" name="view" value="'.(($viewmode == "Add") ? 'ViewRaidTeams' : 'ViewTeamData').'">'."\n");
print('<input type="hidden" name="TeamId" value="'.$TeamId.'">'."\n");
print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
print('<input type="hidden" name="HideTeamUserId" value="0">'."\n");
print('<input type="hidden" name="UserOutLevelId" value="0">'."\n");
print('<input type="hidden" name="UserId" value="0">'."\n\n");

print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");
$TabIndex = 0;

print('<tr><td class="input">'."\n");

// ============ Номер команды
if ($viewmode=="Add")
// Добавляем новую команду
// Если старый ММБ - открываем редактирование номера, иначе номер не передаём
{
	if ($OldMmb == 1)
	{
		print('Команда N <input type="text" name="TeamNum" size="5" value="0" tabindex="'.(++$TabIndex).'" title="Для прошлых ММБ укажите номер команды">'."\n");
	}
	else
	{
		print('<b>Новая команда!</b> <input type="hidden" name="TeamNum" value="0">'."\n");
	}
}
else
// Уже существующая команда
{
	print('Команда N <b>'.$TeamNum.'</b> <input type="hidden" name="TeamNum" value="'.$TeamNum.'">'."\n");
}

// ============ Дистанция
if ($OldMmb == 1)
// Для старых ММБ выводим ссылки на старые статические результаты
{
	$sql = "select distance_resultlink, distance_name from Distances where raid_id = ".$RaidId;
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		print('&nbsp; Дистанция <a href="'.trim($Row['distance_resultlink']).'" target="_blank">' . $Row['distance_name'] . '</a> &nbsp; '."\n");
	}
	mysql_free_result($Result);
}
else
// Для новых ММБ перед выпадающим списком ничего не выводим
{
	print(' <span style="margin-left: 30px;"> &nbsp; Дистанция</span>'."\n");
}
// Показываем выпадающий список дистанций
print('<select name="DistanceId" class="leftmargin" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
$sql = "select distance_id, distance_name from Distances where raid_id = ".$RaidId;
$Result = MySqlQuery($sql);
while ($Row = mysql_fetch_assoc($Result))
{
	$distanceselected = ($Row['distance_id'] == $DistanceId ? 'selected' : '');
	print('<option value="'.$Row['distance_id'].'" '.$distanceselected.' >'.$Row['distance_name']."</option>\n");
}
mysql_free_result($Result);
print('</select>'."\n");

// ============ Кнопка удаления всей команды для тех, кто имеет право
if (($viewmode <> "Add") && ($AllowEdit == 1))
{
	print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить команду: '.trim($TeamName).'? \')) {HideTeam();}" name="HideTeamButton" value="Удалить команду" tabindex="'.(++$TabIndex).'">'."\n");
}

print('</td></tr>'."\n\n");

// ============ Дата регистрации команды
if ($viewmode <> "Add")
{
	if ($TeamLate == 1) $RegisterDtFontColor = '#BB0000';
	else $RegisterDtFontColor = '#000000';
	print('<tr><td class="input">Зарегистрирована: <span style="color: '.$RegisterDtFontColor.';">'.$TeamRegisterDt.'</span></td></tr>'."\n\n");
}
else
{
	print('<tr><td class="input">Время окончания регистрации: '.$RaidRegistrationEndDate.'</td></tr>'."\n\n");
}

// ============ Название команды
print('<tr><td class="input"><input type="text" name="TeamName" size="50" value="'.$TeamName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$TeamName.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$TeamName.'\';}"')
	.' title="Название команды"></td></tr>'."\n\n");

print('<tr><td class="input">'."\n");

// ============ Использование GPS
print('GPS <input type="checkbox" name="TeamUseGPS" value="on"'.(($TeamUseGPS == 1) ? ' checked="checked"' : '')
	.' tabindex="'.(++$TabIndex).'"'.$DisabledText
	.' title="Отметьте, если команда использует для ориентирования GPS"/> &nbsp;'."\n");

// ============ Число карт
print('&nbsp; Число карт <input type="text" name="TeamMapsCount" size="2" maxlength="2" value="'.$TeamMapsCount.'" tabindex="'.(++$TabIndex).'"'
	.$OnClickText.$DisabledText.' title="Число заказанных на команду карт на каждый из этапов"> &nbsp;'."\n");

// ============ Нет сломанным унитазам!
print('&nbsp; Нет <a href="http://community.livejournal.com/_mmb_/2010/09/24/" target="_blank">сломанным унитазам</a>! <input type="checkbox" name="TeamGreenPeace" value="on"'.(($TeamGreenPeace >= 1) ? ' checked="checked"' : '')
	.' tabindex="'.(++$TabIndex).'"'.$DisabledText.' title="Отметьте, если команда берёт повышенные экологические обязательства"/>'."\n");

print('</td></tr>'."\n\n");

// ============ Участники
print('<tr><td class="input">'."\n");

$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, tu.level_id, u.user_id
	from TeamUsers tu
		inner join Users u
		on tu.user_id = u.user_id
	where tu.teamuser_hide = 0 and team_id = ".$TeamId;
$Result = MySqlQuery($sql);

while ($Row = mysql_fetch_assoc($Result))
{
	print('<div style="margin-top: 5px;">'."\n");
	// Ссылку удалить ставим только в том случае, если работает модератор или участник команды
	// !!! Удаление нужно разрешать команде только до начала ММБ
	if ($Moderator or $TeamUser)
	{
		print('<input type="button" style="margin-right: 15px;" onClick="javascript:if (confirm(\'Вы уверены, что хотите удалить участника: '.$Row['user_name'].'? \')) { HideTeamUser('.$Row['teamuser_id'].'); }" name="HideTeamUserButton" tabindex="'.(++$TabIndex).'" value="Удалить">'."\n");
	}

	// Если текущая дата больше времени окончания регистрации - появляются поля схода
	// !!! Ввод схода нужно разрешать по тем же правилам, что и ввод результатов
	if (($viewmode <> "Add") && ($RaidShowResultField == 1))
	{
		// Список этапов, чтобы выбрать, на каком сошёл участник
		print('Сход: <select name="UserOut'.$Row['teamuser_id'].'" style="width: 100px; margin-right: 15px;" title="Этап, на котором сошёл участник" onChange="javascript:if (confirm(\'Вы уверены, что хотите отметить сход участника: '.$Row['user_name'].'? \')) { TeamUserOut('.$Row['teamuser_id'].', this.value); }" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
		$sqllevels = "select level_id, level_name from Levels where distance_id = ".$DistanceId." order by level_order";
		$ResultLevels = MySqlQuery($sqllevels);
		$userlevelselected = ($Row['level_id'] == 0 ? ' selected' : '');
		print('<option value="0"'.$userlevelselected.'>-</option>'."\n");
		while ($RowLevels = mysql_fetch_assoc($ResultLevels))
		{
			$userlevelselected = ($RowLevels['level_id'] == $Row['level_id'] ? 'selected' : '');
			print('<option value="'.$RowLevels['level_id'].'"'.$userlevelselected.'>'.$RowLevels['level_name']."</option>\n");
		}
		mysql_free_result($ResultLevels);
		print('</select>'."\n");
	}

	// ФИО и год рождения участника
	print('<a href="javascript:ViewUserInfo('.$Row['user_id'].');">'.$Row['user_name'].'</a> '.$Row['user_birthyear']."\n");
	print('</div>'."\n");
}
mysql_free_result($Result);
print('</td></tr>'."\n");
// Закончили вывод списка участников

// ============ Новый участник
if ($AllowEdit == 1)
{
	print('<tr><td class="input" style="padding-top: 10px;">'."\n");
	if (($viewmode == "Add") && !$Moderator)
	{
		// Новая команда и заводит не модератор
		print($UserEmail.'<input type="hidden" name="NewTeamUserEmail" size="50" value="'.$UserEmail.'" >'."\n");
	}
	else
	{
		print('<input type="text" name="NewTeamUserEmail" size="50" value="Email нового участника" tabindex="'.(++$TabIndex)
		.'" onclick="javascript: if (trimBoth(this.value) == \'Email нового участника\') {this.value=\'\';}" onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\'Email нового участника\';}" title="Укажите e-mail пользователя, которого Вы хотите добавить в команду. Пользователь может запретить добавлять себя в команду в настройках своей учетной записи.">'."\n");
	}
	print('</td></tr>'."\n");
}

// ================ Секция результатов ========================================

// !!! Ввод схода нужно разрешать по тем же правилам, что и ввод результатов
if (($AllowEdit == 1) && ($viewmode <> "Add") && ($RaidShowResultField == 1))
{
	// Список этапов, чтобы выбрать, на какой команда не вышла (по умолчанию считается, что вышла на всё)
	print('<tr><td style="padding-top: 15px;"><b>Результаты:</b></td></tr>'."\n");
	print('<tr><td class="input">Не вышла на этап: &nbsp; '."\n");
	print('<select name="TeamNotOnLevelId" style="width: 100px; margin-left: 10px;margin-right: 10px;" tabindex="'.(++$TabIndex).'"'.$DisabledText
		.' title="Будьте аккуратны: изменение этого поля влияет на число отображаемых ниже этапов для ввода данных.">'."\n");
	$sql = "select level_id, level_name from Levels where distance_id = ".$DistanceId." order by level_order";
	$Result = MySqlQuery($sql);
	$teamlevelselected = ($TeamNotOnLevelId == 0 ? ' selected' : '');
	print('<option value="0"'.$teamlevelselected.'>-</option>'."\n");
	while ($Row = mysql_fetch_assoc($Result))
	{
		$teamlevelselected = ($Row['level_id'] == $TeamNotOnLevelId ? ' selected' : '');
		print('<option value="'.$Row['level_id'].'"'.$teamlevelselected.'>'.$Row['level_name']."</option>\n");
	}
	mysql_free_result($Result);
	print('</select>'."\n");

	print(' &nbsp; Общее время: '.$TeamResult.'</td></tr>'."\n");

	print('<tr><td class="input"> Подтверждение: &nbsp;'."\n");

	// Подтверждение правильности результатов командой
	print('команды <input type="checkbox" name="TeamConfirmResult" value="on"'.(($TeamConfirmResult == 1) ? ' checked="checked"' : '')
		.' tabindex="'.(++$TabIndex).'"'.$DisabledText
		.' title="Отметьте, если команда проверила результаты и согласна с ними"/> &nbsp;'."\n");

	// Подтверждение правильности результатов модератором
	if ($Moderator) $ModeratorConfirmResultDisabledText = '';
	else $ModeratorConfirmResultDisabledText = ' disabled';
	print('модератора <input type="checkbox" name="ModeratorConfirmResult" value="on"'.(($ModeratorConfirmResult == 1) ? ' checked="checked"' : '')
		.' tabindex="'.(++$TabIndex).'"'.$ModeratorConfirmResultDisabledText
		.' title="Заполняется модератором после проверки результатов."/>'."\n");

	print('</td></tr>'."\n\n");
}
// Конец проверки на отображение секции Результатов


// ================ Submit для формы ==========================================
if ($AllowEdit == 1)
{
	print('<tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateTeamDataForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
	print('<select name="CaseView" onChange="javascript:document.TeamDataForm.view.value = document.TeamDataForm.CaseView.value;" class="leftmargin" tabindex="'.(++$TabIndex).'">'."\n");
	print('<option value="ViewTeamData"'.(($viewmode <> "Add") ? ' selected' : '').'>и остаться на этой странице</option>'."\n");
	print('<option value="ViewRaidTeams"'.(($viewmode == "Add") ? ' selected' : '').'>и перейти к списку команд</option>'."\n");
	print('</select>'."\n");
	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	print('</td></tr>'."\n\n");
}

print('</table></form>'."\n");
?>
