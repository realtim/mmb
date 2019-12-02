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
		$ClearRegistrationEndDateDate = mmb_isOn($_POST, 'ClearRaidRegistrationEndDateDate');
           //     $RaidLogoLink = $_POST['RaidLogoLink'];
           //     $RaidRulesLink = $_POST['RaidRulesLink'];
		$RaidStartPointName = $_POST['RaidStartPointName'];
	//	$RaidStartLink = $_POST['RaidStartLink'];
		$RaidFinishPointName = $_POST['RaidFinishPointName'];
		$RaidCloseDate = $_POST['RaidCloseDate'];
		$ClearRaidCloseDate = mmb_isOn($_POST, 'ClearRaidCloseDate');
	//	$RaidZnLink = $_POST['RaidZnLink'];
		$RaidDistancesCount = (int)$_POST['RaidDistancesCount'];
                $RaidNoShowResult = mmb_isOn($_POST, 'RaidNoShowResult');
                $RaidReadOnlyHoursBeforeStart = (int)$_POST['RaidReadOnlyHoursBeforeStart'];
		$RaidFilePrefix = $_POST['RaidFilePrefix'];
	        $RaidMapPrice = (int)$_POST['RaidMapPrice'];
	        $RaidNoStartPrice = (int)$_POST['RaidNoStartPrice'];
	        $RaidTeamsLimit = (int)$_POST['RaidTeamsLimit'];
		$RaidBluetoothPIN = $_POST['RaidBluetoothPIN'];
	}
	else
	// Пробуем создать команду первый раз
	{

                $RaidName = '';
		$RaidPeriod = '';
                $RaidRegistrationEndDate = '';
                $ClearRaidRegistrationEndDate = 0;
	//	$RaidLogoLink = '';
          //      $RaidRulesLink = '';
		$RaidStartPointName = '';
//		$RaidStartLink = '';
		$RaidFinishPointName = '';
		$RaidCloseDate = '';
//		$RaidZnLink = '';
                $ClearRaidCloseDate = 0;
		$RaidDistancesCount = 1;
                $RaidNoShowResult = 1; 
                $RaidReadOnlyHoursBeforeStart = 8;
                $RaidFilePrefix = '';
		$RaidMapPrice = 0;
		$RaidNoStartPrice = 0;
		$RaidTeamsLimit = 0;
		$RaidBluetoothPIN = '';
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
		       r.raid_startpoint, 
		       r.raid_finishpoint, r.raid_closedate,
		       COALESCE(r.raid_noshowresult, 0) as raid_noshowresult,
		       COALESCE(r.raid_readonlyhoursbeforestart, 8) as raid_readonlyhoursbeforestart,  
		       COALESCE(r.raid_mapprice, 8) as raid_mapprice,  COALESCE(r.raid_nostartprice, 8) as raid_nostartprice,  
		       r.raid_fileprefix,
      	               (CASE WHEN r.raid_closedate is null THEN 1 ELSE 0 END) as raid_clearclosedate,
		       (select count(*) from Distances where distance_hide = 0 and raid_id = $RaidId) as raid_distancescount,
		       r.raid_teamslimit,
		       r.raid_btpin
		from Raids r
		where r.raid_id = $RaidId";
	$Row = CSql::singleRow($sql);

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$RaidName = $_POST['RaidName'];
		$RaidPeriod = $_POST['RaidPeriod'];
                $RaidRegistrationEndDate = $_POST['RaidRegistrationEndDate'];
		$ClearRaidRegistrationEndDate = mmb_isOn($_POST, 'ClearRaidRegistrationEndDate');
	//	$RaidLogoLink = $_POST['RaidLogoLink'];
	//	$RaidRulesLink = $_POST['RaidRulesLink'];
		$RaidStartPointName = $_POST['RaidStartPointName'];
	//	$RaidStartLink = $_POST['RaidStartLink'];
		$RaidFinishPointName = $_POST['RaidFinishPointName'];
		$RaidCloseDate = $_POST['RaidCloseDate'];
		$ClearRaidCloseDate = mmb_isOn($_POST, 'ClearRaidCloseDate');     // а ничего, что он всюду сравнивается с on? !!!
	//	$RaidZnLink = $_POST['RaidZnLink'];
                //В отличие от остальных полей это - вычисляемое и после ошибки не возвращается
		$RaidDistancesCount = (int)$_POST['RaidDistancesCount'];
		$RaidNoShowResult = mmb_isOn($_POST, 'RaidNoShowResult');         // а ничего, что он всюду сравнивается с on? !!!
                $RaidReadOnlyHoursBeforeStart = (int)$_POST['RaidReadOnlyHoursBeforeStart'];
		$RaidFilePrefix = $_POST['RaidFilePrefix'];
                $RaidMapPrice = (int)$_POST['RaidMapPrice'];
                $RaidNoStartPrice = (int)$_POST['RaidNoStartPrice'];
                $RaidTeamsLimit = (int)$_POST['RaidTeamsLimit'];
                $RaidBluetoothPIN = $_POST['RaidBluetoothPIN'];
	}
	else
	{

		$RaidName = $Row['raid_name'];
		$RaidPeriod = $Row['raid_period'];
		$RaidRegistrationEndDate = $Row['raid_registrationenddate'];
                $ClearRaidRegistrationEndDate = $Row['raid_clearregistrationenddate'];
	//	$RaidLogoLink = $Row['raid_logolink'];
	//	$RaidRulesLink = $Row['raid_ruleslink'];
		$RaidStartPointName = $Row['raid_startpoint'];
	//	$RaidStartLink = $Row['raid_startlink'];
		$RaidFinishPointName = $Row['raid_finishpoint'];
		$RaidCloseDate = $Row['raid_closedate'];
		$ClearRaidCloseDate = $Row['raid_clearclosedate'];
	//	$RaidZnLink = $Row['raid_znlink'];
		$RaidDistancesCount = (int)$Row['raid_distancescount'];
	        $RaidNoShowResult = (int)$Row['raid_noshowresult'];
                $RaidReadOnlyHoursBeforeStart = (int)$Row['raid_readonlyhoursbeforestart'];
		$RaidFilePrefix = $Row['raid_fileprefix'];
                $RaidMapPrice = (int)$Row['raid_mapprice'];
                $RaidNoStartPrice = (int)$Row['raid_nostartprice'];
                $RaidTeamsLimit = (int)$Row['raid_teamslimit'];
                $RaidBluetoothPIN = $Row['raid_btpin'];
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

	// Функция отмены изменения
	function Cancel()
	{
		document.RaidDataForm.action.value = "CancelChangeRaidData";
		document.RaidDataForm.submit();
	}
	// 
</script>

<?php
// Выводим начало формы с ММБ
print('<form name="RaidDataForm" action="'.$MyPHPScript.'" method="post" enctype="multipart/form-data" onSubmit="'.$OnSubmitFunction.'">'."\n");
print('<input type="hidden" name="action" value="">'."\n");
print('<input type="hidden" name="view" value="ViewRaidData">'."\n");
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
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidName))
	.' title="Название ММБ"></td></tr>'."\n\n");

/*
// ============ Эмблема (ссылка и загрузка файла)
print('<tr><td class="input">Ссылка на эмблему: <input type="text" name="RaidLogoLink" size="50" value="'.$RaidLogoLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidLogoLink))
	.' title="Ссылка на эмблему ММБ"></td></tr>'."\r\n");

print('<tr><td class = "input">Новый файл эмблемы для загрузки: <input name="logofile" type="file" /></td></tr>'."\r\n");

*/
// ============ Период ММБ
print('<tr><td class="input">Период: <input type="text" name="RaidPeriod" size="20" value="'.$RaidPeriod.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidPeriod))
	.' title="Период ММБ"></td></tr>'."\r\n");

// ============ Префикс файлов прим загрузке 
print('<tr><td class="input">Префикс файлов: <input type="text" name="RaidFilePrefix" size="20" value="'.$RaidFilePrefix.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidFilePrefix))
	.' title="Префикс файлов"></td></tr>'."\r\n");


// ============ Число Дистанций
print('<tr><td class="input">Число дистанций <input type="text" name="RaidDistancesCount" size="1" maxlength="1" value="'.$RaidDistancesCount.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidDistancesCount))
	.' title="Число дистанций"> <i>Должно быть не меньше, чем уже создано.</i></td></tr>'."\r\n");

/*
// ============ Положение (ссылка и загрузка файла)
print('<tr><td class="input">Ссылка на положение: <input type="text" name="RaidRulesLink" size="50" value="'.$RaidRulesLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidRulesLink))
	.' title="Ссылка на положение ММБ"></td></tr>'."\r\n");

print('<tr><td class = "input">Новый файл положения для загрузки: <input name="rulesfile" type="file" /></td></tr>'."\r\n");
*/

// ============ Дата окончания регистрации ММБ
print('<tr><td class="input">Дата закрытия регистрации (гггг-мм-дд): <input type="text" name="RaidRegistrationEndDate" size="10" value="'.$RaidRegistrationEndDate.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidRegistrationEndDate))
	.' title="Дата закрытия регистрации ММБ">'."\r\n");

// ============ Очистка даты окончания регистрации ММБ
print('<input type="checkbox" name="ClearRaidRegistrationEndDate" '.(($ClearRaidRegistrationEndDate == 1) ? 'checked="checked"' : '').' tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
	        title = "Дата окончания регистрации будет очищена" /> Убрать</td></tr>'."\r\n");

// ============ Пояснение
print('<tr><td class="input"><i>Пользователи видят ММБ в списке, если указана дата закрытия регистрации.<br/>Команду можно зарегистрировать до 23:59 этой даты.</i></td></tr>'."\r\n");
print('<tr><td class="input"><br/></td></tr>'."\r\n");

// ============ Запрет на редактирование команд участникаи за ... часов до старта 
print('<tr><td class="input">Запрет правок участниками в часах до старта <input type="text" name="RaidReadOnlyHoursBeforeStart" size="2" maxlength="2" value="'.$RaidReadOnlyHoursBeforeStart.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidReadOnlyHoursBeforeStart))
	.' title="часов до старта"></td></tr>'."\r\n");
 


// ============ Старт ММБ
print('<tr><td class="input">Название пункта старта: <input type="text" name="RaidStartPointName" size="20" value="'.$RaidStartPointName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidStartPointName))
	.' title="Название пункта старта ММБ"></td></tr>'."\n\n");

// ============ Стоимость одного комплекта карт
print('<tr><td class="input">Стоимость одного комплекта карт (руб.) <input type="text" name="RaidMapPrice" size="4" maxlength="4" value="'.$RaidMapPrice.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidMapPrice))
	.' title="Стоимость одного комплекта карт (руб.)"></td></tr>'."\r\n");

// ============ Стоимость (штраф) с участника за неявку на старт на этот ММБ
print('<tr><td class="input">Стоимость неявки (руб.) <input type="text" name="RaidNoStartPrice" size="4" maxlength="4" value="'.$RaidNoStartPrice.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidNoStartPrice))
	.' title="Стоимость неявки (руб.)"></td></tr>'."\r\n");

// ============ Лимит команд
print('<tr><td class="input">Лимит команд <input type="text" name="RaidTeamsLimit" size="4" maxlength="4" value="'.$RaidTeamsLimit.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidTeamsLimit))
	.' title="Лимит команд"></td></tr>'."\r\n");

// ============ Bluetooth PIN-код судейских станций
print('<tr><td class="input">Sportiduino PIN-код <input type="text" name="RaidBluetoothPIN" size="4" maxlength="4" value="'.$RaidBluetoothPIN.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidBluetoothPIN))
	.' title="Sportiduino PIN-код"></td></tr>'."\r\n");

/*
// ============ Информация о старте  (ссылка)
print('<tr><td class="input">Ссылка на информацию о старте: <input type="text" name="RaidStartLink" size="36" value="'.$RaidStartLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidStartLink))
	.' title="Ссылка на информацию о старте ММБ"></td></tr>'."\r\n");
*/
print('<tr><td class="input"><br/></td></tr>'."\r\n");
print('<tr><td class="input"><b>Заполняется после ММБ</b></td></tr>'."\r\n");

// ============ Финиш ММБ
print('<tr><td class="input">Название пункта финиша: <input type="text" name="RaidFinishPointName" size="20" value="'.$RaidFinishPointName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidFinishPointName))
	.' title="Название пункта финиша ММБ"></td></tr>'."\n\n");


// ============ Отображение резудьтатов ММБ
print('<tr><td class="input">Не показывать результаты ММБ <input type="checkbox" name="RaidNoShowResult" '.(($RaidNoShowResult == 1) ? 'checked="checked"' : '').' tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
	        title = "Не показывать результаты ММБ" /></td></tr>'."\r\n");

// ============ Дата закрытия протокола ММБ
print('<tr><td class="input">Дата закрытия протокола (гггг-мм-дд): <input type="text" name="RaidCloseDate" size="10" value="'.$RaidCloseDate.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidCloseDate))
	.' title="Дата закрытия протокола  ММБ">'."\r\n");

// ============ Очистка даты закрытия протокола ММБ
print('<input type="checkbox" name="ClearRaidCloseDate" '.(($ClearRaidCloseDate == 1) ? 'checked="checked"' : '').' tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
	        title = "Дата закрытия протокола будет очищена" /> Убрать</td></tr>'."\r\n");

/*
// ============ Значок (ссылка и загрузка файла)
print('<tr><td class="input">Ссылка на значок: <input type="text" name="RaidZnLink" size="50" value="'.$RaidZnLink.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($RaidZnLink))
	.' title="Ссылка на значок ММБ"></td></tr>'."\r\n");

print('<tr><td class = "input">Новый файл значка для загрузки: <input name="znfile" type="file" /></td></tr>'."\r\n");
*/


print("<tr><td class=\"input\">\n");


// ================ Submit для формы ==========================================
if ($AllowEdit == 1)
{
	print('<tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateRaidDataForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	print("</td></tr>\r\n");

}

print("</table></form>\n");


// Если редактируем, то выводим дистанции и этапы

if ($viewmode != 'Add')
{

	print('<div style="margin-top: 35px; font-weight: bold; font-size: 80%;">Дистанции</div>'."\n");


	$sql = "select d.distance_id, d.distance_name, d.distance_data
		from Distances d
		where d.distance_hide = 0 and d.raid_id = $RaidId";
	$DistanceResult = MySqlQuery($sql);
	while ($RowDistance = mysql_fetch_assoc($DistanceResult))
	{

		print('<form name="DistanceDataForm'.$RowDistance['distance_id'].'" action="'.$MyPHPScript.'" method="post">'."\n");
		print('<input type="hidden" name="action" value="DistanceChangeData">'."\n");
		print('<input type="hidden" name="view" value="ViewRaidData">'."\n");
		print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
		print('<input type="hidden" name="UserId" value="0">'."\n");
		print('<input type="hidden" name="DistanceId" value="'.$RowDistance['distance_id'].'">'."\n");


		print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");
		$TabIndex = 0;
		$DisabledText = '';
		// ============ Название Дистанции
		print('<tr><td class="input">Название: <input type="text" name="DistanceName'.$RowDistance['distance_id'].'" size="20" value="'.$RowDistance['distance_name'].'" tabindex="'.(++$TabIndex).'">'."\r\n");
		print(' параметры: <input type="text" name="DistanceData'.$RowDistance['distance_id'].'" size="50" value="'.$RowDistance['distance_data'].'" tabindex="'.(++$TabIndex).'">'."\r\n");
		print('<input type="button" onClick="javascript: submit();" name="SaveDistance'.$RowDistance['distance_id'].'" value="Сохранить" tabindex="'.(++$TabIndex).'">'."\r\n");
		print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить дистанцию.: '.trim($RowDistance['distance_name']).'? \')) {  DistanceDataForm'.$RowDistance['distance_id'].'.action.value = \'HideDistance\';submit();}" name="HideFileButton'.$RowDistance['distance_id'].'" value="Удалить" tabindex="'.(++$TabIndex).'">'."\n");
		print("</td></tr>\r\n");
		print("</table></form>\r\n");
	}
	mysql_free_result($DistanceResult);
	print("<br/>\r\n");
	// Закончили вывод дистанций
}
// Конец проверки на редактирование ММБ

?>
