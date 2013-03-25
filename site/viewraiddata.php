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
		$RaidStartPointName = $_POST['RaidStartPointName'];
		$RaidStartLink = $_POST['RaidStartLink'];
		$RaidFinishPointName = $_POST['RaidFinishPointName'];
		$RaidCloseDate = $_POST['RaidCloseDate'];
		$ClearRaidCloseDate = (isset($_POST['ClearRaidCloseDate']) && ($_POST['ClearRaidCloseDate'] == 'on')) ? 1 : 0;
		$RaidZnLink = $_POST['RaidZnLink'];

	}
	else
	// Пробуем создать команду первый раз
	{

                $RaidName = 'Название ММБ';
		$RaidPeriod = 'Период проведения';
                $RaidRegistrationEndDate = 'Дата окончания регистрации (yyyy-mm-dd)';
                $RaidLogoLink = 'Ссылка на эмблему ММБ';
                $RaidRulesLink = 'Ссылка на положение о ММБ';
		$RaidStartPointName = 'Название старта';
		$RaidStartLink = 'Ссылка на информацию о старте';
		$RaidFinishPointName = 'Название финиша';
		$RaidCloseDate = 'Дата закрытия протокола';
		$RaidZnLink = 'Ссылка на значок ММБ';

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
		       r.raid_logolink, r.raid_ruleslink,  r.raid_startpoint, 
		       r.raid_startlink, r.raid_finishpoint, r.raid_closedate,
		       r.raid_znlink, 
      	               (CASE WHEN r.raid_closedate is null THEN 1 ELSE 0 END) as raid_clearclosedate
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
		$RaidStartPointName = $_POST['RaidStartPointName'];
		$RaidStartLink = $_POST['RaidStartLink'];
		$RaidFinishPointName = $_POST['RaidFinishPointName'];
		$RaidCloseDate = $_POST['RaidCloseDate'];
		$ClearRaidCloseDate = $_POST['ClearRaidCloseDate'];
		$RaidZnLink = $_POST['RaidZnLink'];

		
	}
	else
	{

		$RaidName = $Row['raid_name'];
		$RaidPeriod = $Row['raid_period'];
		$RaidRegistrationEndDate = $Row['raid_registrationenddate'];
                $ClearRaidRegistrationEndDate = $Row['raid_clearregistrationenddate'];
		$RaidLogoLink = $Row['raid_logolink'];
		$RaidRulesLink = $Row['raid_ruleslink'];
		$RaidStartPointName = $Row['raid_startpoint'];
		$RaidStartLink = $Row['raid_startlink'];
		$RaidFinishPointName = $Row['raid_finishpoint'];
		$RaidCloseDate = $Row['raid_closedate'];
		$ClearRaidCloseDate = $Row['raid_clearclosedate'];
		$RaidZnLink = $Row['raid_znlink'];
	    

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
print('<tr><td class="input">Название: <input type="text" name="RaidName" size="20" value="'.$RaidName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidName.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidName.'\';}"')
	.' title="Название ММБ"></td></tr>'."\n\n");

// ============ Эмблема (ссылка и загрузка файла)
print('<tr><td class="input">Ссылка на эмблему: <input type="text" name="RaidLogoLink" size="50" value="'.$RaidLogoLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidLogoLink.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidLogoLink.'\';}"')
	.' title="Ссылка на эмблему ММБ"></td></tr>'."\r\n");

print('<tr><td class = "input">Новый файл эмблемы для загрузки: <input name="logofile" type="file" /></td></tr>'."\r\n");


// ============ Период ММБ
print('<tr><td class="input">Период: <input type="text" name="RaidPeriod" size="30" value="'.$RaidPeriod.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidPeriod.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidPeriod.'\';}"')
	.' title="Период ММБ"></td></tr>'."\r\n");


// ============ Дата окончания регистрации ММБ
print('<tr><td class="input">Дата закрытия регистрации (гггг-мм-дд): <input type="text" name="RaidRegistrationEndDate" size="10" value="'.$RaidRegistrationEndDate.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidRegistrationEndDate.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidRegistrationEndDate.'\';}"')
	.' title="Дата закрытия регистрации ММБ">'."\r\n");

// ============ Очистка даты окончания регистрации ММБ
print('<input type="checkbox" name="ClearRaidRegistrationEndDate" '.(($ClearRaidRegistrationEndDate == 1) ? 'checked="checked"' : '').' tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
	        title = "Дата окончания регистрации будет очищена" /> Убрать</td></tr>'."\r\n");

// ============ Пояснение
print('<tr><td class="input">Пользователи видят ММБ в списке, если указана дата закрытия регистрации</td></tr>'."\r\n");
print('<tr><td class="input"><br/></td></tr>'."\r\n");


// ============ Положение (ссылка и загрузка файла)
print('<tr><td class="input">Ссылка на положение: <input type="text" name="RaidRulesLink" size="50" value="'.$RaidRulesLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidRulesLink.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidRulesLink.'\';}"')
	.' title="Ссылка на положение ММБ"></td></tr>'."\r\n");

print('<tr><td class = "input">Новый файл положения для загрузки: <input name="rulesfile" type="file" /></td></tr>'."\r\n");
  


// ============ Старт ММБ
print('<tr><td class="input">Название пункта старта: <input type="text" name="RaidStartPointName" size="20" value="'.$RaidStartPointName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidStartPointName.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidStartPointName.'\';}"')
	.' title="Название пункта старта ММБ"></td></tr>'."\n\n");

// ============ Информация о старте  (ссылка)
print('<tr><td class="input">Ссылка на информацию о старте: <input type="text" name="RaidStartLink" size="36" value="'.$RaidStartLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidStartLink.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidStartLink.'\';}"')
	.' title="Ссылка на информацию о старте ММБ"></td></tr>'."\r\n");

print('<tr><td class="input"><br/></td></tr>'."\r\n");
print('<tr><td class="input"><b>Заполняется после ММБ</b></td></tr>'."\r\n");

// ============ Финиш ММБ
print('<tr><td class="input">Название пункта финиша: <input type="text" name="RaidFinishPointName" size="40" value="'.$RaidFinishPointName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidFinishPointName.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidFinishPointName.'\';}"')
	.' title="Название пункта финиша ММБ"></td></tr>'."\n\n");


// ============ Дата закрытия протокола ММБ
print('<tr><td class="input">Дата закрытия протокола (гггг-мм-дд): <input type="text" name="RaidCloseDate" size="10" value="'.$RaidCloseDate.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidCloseDate.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidCloseDate.'\';}"')
	.' title="Дата закрытия протокола  ММБ">'."\r\n");

// ============ Очистка даты закрытия протокола ММБ
print('<input type="checkbox" name="ClearRaidCloseDate" '.(($ClearRaidCloseDate == 1) ? 'checked="checked"' : '').' tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
	        title = "Дата закрытия протокола будет очищена" /> Убрать</td></tr>'."\r\n");


// ============ Значок (ссылка и загрузка файла)
print('<tr><td class="input">Ссылка на значок: <input type="text" name="RaidZnLink" size="50" value="'.$RaidZnLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : ' onclick="javascript: if (trimBoth(this.value) == \''.$RaidZnLink.'\') {this.value=\'\';}"')
	.($viewmode <> 'Add' ? '' : ' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidZnLink.'\';}"')
	.' title="Ссылка на значок ММБ"></td></tr>'."\r\n");

print('<tr><td class = "input">Новый файл значка для загрузки: <input name="znfile" type="file" /></td></tr>'."\r\n");



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
