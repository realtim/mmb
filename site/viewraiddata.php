<?php
// +++++++++++ Показ/редактирование данных ММБ ++++++++++++++++++++++++++++


// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";

// ================ Добавляем новый ММБ ===================================
if ($viewmode == 'Add')
{

	// Если запрещено создавать ММБ - молча выходим, сообщение уже выведено в teamaction.php
	if (!$Administrator) return;

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$RaidName = $_POST['RaidName'];
		$RaidPeriod = $_POST['RaidPeriod'];
                $RaidRegistrationEndDate = $_POST['RaidRegistrationEndDate'];
                $RaidLogoLink = $_POST['RaidLogoLink'];
                $RaidRulesLink = $_POST['RaidRulesLink'];

	}
	else
	// Пробуем создать команду первый раз
	{

                $RaidName = 'Название ММБ';
		$RaidPeriod = 'Период проведения';
                $RaidRegistrationEndDate = 'Дата окончания регистрации (yyyy-mm-dd)';
                $RaidLogoLink = 'Ссылка на эмблему ММБ';
                $RaidRulesLink = 'Ссылка на положение о ММБ';

	}

	// Определяем следующее действие
	$NextActionName = 'AddRaid';
	// Действие на текстовом поле по клику
	$OnClickText = ' onClick="javascript:this.value = \'\';"';
	// Надпись на кнопке
	$SaveButtonText = 'Создать ММБ';

}

else
// ================ Редактируем/смотрим существующий ММБ =================
{
	if ($RaidId <= 0)
	{
		return;
	}

	$sql = "select r.raid_name, r.raid_period, r.raid_registrationenddate,
	               (CASE WHEN r.raid_registrationenddate is null THEN 1 ELSE 0 END) as raid_clearregistrationenddate,
		       r.raid_logolink, r.raid_ruleslink 
		from Raids r
		where r.raid_id = ".$RaidId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$RaidName = $_POST['RaidName'];
		$RaidPeriod = $_POST['RaidPeriod'];
                $RaidRegistrationEndDate = $_POST['RaidRegistrationEndDate'];
		$ClearRaidRegistrationEndDate = $_POST['ClearRaidRegistrationEndDate'];
		$RaidLogoLink = $_POST['RaidLogoLink'];
		$RaidRulesLink = $_POST['RaidRulesLink'];
		
/*
		$TeamNum = (int) $_POST['TeamNum'];
		$TeamName = str_replace( '"', '&quot;', $_POST['TeamName']);
		$DistanceId = $_POST['DistanceId'];
		$TeamUseGPS = ($_POST['TeamUseGPS'] == 'on' ? 1 : 0);
		$TeamMapsCount = (int)$_POST['TeamMapsCount'];
		$TeamGreenPeace = ($_POST['TeamGreenPeace'] == 'on' ? 1 : 0);
*/
	}
	else
	{

		$RaidName = $Row['raid_name'];
		$RaidPeriod = $Row['raid_period'];
		$RaidRegistrationEndDate = $Row['raid_registrationenddate'];
                $ClearRaidRegistrationEndDate = $Row['raid_clearregistrationenddate'];
		$RaidLogoLink = $Row['raid_logolink'];
		$RaidRulesLink = $Row['raid_ruleslink'];
	    

	}

	$NextActionName = 'RaidChangeData';
	$AllowEdit = 0;
	$OnClickText = '';
	$SaveButtonText = 'Сохранить данные ММБ';

}
// ================ Конец инициализации переменных ММБ =================

// Определяем права по редактированию команды
if ($Administrator)
{
	$AllowEdit = 1;
	$DisabledText = '';
	$OnSubmitFunction = 'return ValidateRaidDataForm();';
}
else
{
	$AllowEdit = 0;
	$DisabledText = ' disabled';
	$OnSubmitFunction = 'return false;';
}
// Определяем права по просмотру результатов
if ($Administrator)
{
	$AllowViewResults = 1;
}
else 
{
       $AllowViewResults = 0;
}


// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateRaidDataForm()
	{
		document.RaidDataForm.action.value = "<? echo $NextActionName; ?>";
		return true;
	}
	// Конец проверки правильности заполнения формы

	// Удалить ММБ
	function HideRaid()
	{
		document.RaidDataForm.action.value = 'HideRaid';
		document.RaidDataForm.submit();
	}

/*
	// Удалить пользователя
	function HideTeamUser(teamuserid)
	{
		document.TeamDataForm.HideTeamUserId.value = teamuserid;
		document.TeamDataForm.action.value = 'HideTeamUser';
		document.TeamDataForm.submit();
	}
*/
	// Функция отмены изменения
	function Cancel()
	{
		document.RaidDataForm.action.value = "CancelChangeRaidData";
		document.RaidDataForm.submit();
	}
/*
	// Посмотреть профиль пользователя
	function ViewUserInfo(userid)
	{
		document.TeamDataForm.UserId.value = userid;
		document.TeamDataForm.action.value = 'UserInfo';
		document.TeamDataForm.submit();
	}
*/
	// 
</script>

<?php
// Выводим начало формы с ММБ
print('<form name="RaidDataForm" action="'.$MyPHPScript.'" method="post" enctype="multipart/form-data" onSubmit="'.$OnSubmitFunction.'">'."\n");
print('<input type="hidden" name="sessionid" value="'.$SessionId.'">'."\n");
print('<input type="hidden" name="action" value="">'."\n");
print('<input type="hidden" name="view" value="'.(($viewmode == "Add") ? 'MainPage' : 'ViewRaidData').'">'."\n");
print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
print('<input type="hidden" name="UserId" value="0">'."\n\n");

print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");
$TabIndex = 0;

$DisabledText = '';

// ============ Кнопка удаления ММБ для тех, кто имеет право
/*
print('<tr><td class="input">'."\n");
if (($viewmode <> "Add") && ($AllowEdit == 1))
{
	print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить ММБ: '.trim($RaidName).'? \')) {HideRaid();}" name="HideRaidButton" value="Удалить ММБ" tabindex="'.(++$TabIndex).'">'."\n");
}

print('</td></tr>'."\n\n");
*/


// ============ Название ММБ
print('<tr><td class="input"><input type="text" name="RaidName" size="50" value="'.$RaidName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidName.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidName.'\';}"')
	.' title="Название ММБ"></td></tr>'."\n\n");

print('<tr><td class="input">'."\n");

// ============ Период ММБ
print('<tr><td class="input"><input type="text" name="RaidPeriod" size="50" value="'.$RaidPeriod.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidPeriod.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidPeriod.'\';}"')
	.' title="Период ММБ"></td></tr>'."\n\n");

print('<tr><td class="input">'."\n");


// ============ Дата окончания регистрации ММБ
print('<tr><td class="input"><input type="text" name="RaidRegistrationEndDate" size="50" value="'.$RaidRegistrationEndDate.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidRegistrationEndDate.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidRegistrationEndDate.'\';}"')
	.' title="Дата закрытия регистрации ММБ"></td></tr>'."\n\n");

// ============ Очистка даты окончания регистрации ММБ
print('<tr><td class = "input"><input type="checkbox" name="ClearRaidRegistrationEndDate" '.(($ClearRaidRegistrationEndDate == 1) ? 'checked="checked"' : '').' tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
	        title = "Дата окончания регистрации будет очищена" /> Убрать дату окончания регистрации</td></tr>'."\r\n");


// ============ Эмблема (ссылка и загрузка файла)
print('<tr><td class="input"><input type="text" name="RaidLogoLink" size="50" value="'.$RaidLogoLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidLogoLink.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidLogoLink.'\';}"')
	.' title="Ссылка на эмблему ММБ"></td></tr>'."\n\n");

print('<tr><td class = "input">Новый файл эмблемы: <input name="userfile[]" type="file" /></td></tr>'."\r\n");

// ============ Положение (ссылка и загрузка файла)
print('<tr><td class="input"><input type="text" name="RaidRulesLink" size="50" value="'.$RaidRulesLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidRulesLink.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidRulesLink.'\';}"')
	.' title="Ссылка на эмблему ММБ"></td></tr>'."\n\n");

print('<tr><td class = "input">Новый файл положения: <input name="userfile[]" type="file" /></td></tr>'."\r\n");
  




print('<tr><td class="input">'."\n");


// ================ Submit для формы ==========================================
if ($AllowEdit == 1)
{
	print('<tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateRaidDataForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
	print('<select name="CaseView" onChange="javascript:document.RaidDataForm.view.value = document.RaidDataForm.CaseView.value;" class="leftmargin" tabindex="'.(++$TabIndex).'">'."\n");
	print('<option value="ViewRaidData"'.(($viewmode <> "Add") ? ' selected' : '').'>и остаться на этой странице</option>'."\n");
	print('<option value="MainPage"'.(($viewmode == "Add") ? ' selected' : '').'>и перейти к странице марш-бросков</option>'."\n");
	print('</select>'."\n");
	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	print('</td></tr>'."\n\n");

}

print('</table></form>'."\n");
?>
