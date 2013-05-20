<?php
// +++++++++++ Показ/редактирование результатов команды +++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// ============ Функция преобразования вывода данных
function ConvertTeamLevelPointsToHTML($LevelPointNames, $LevelPointPenalties, $TeamLevelPoints, $LevelId, $DisabledResultText)
{
	$Names = explode(',', $LevelPointNames);
	$Penalties = explode(',', $LevelPointPenalties);
	if (count($Names) <> count($Penalties))
	{
		print('Ошибка данных по КП марш-броска'."\n");
		return;
	}
	if (!empty($TeamLevelPoints))
	{
		$TeamPoints = explode(',', $TeamLevelPoints);
	}
	else $TeamPoints = array();
	if (!empty($TeamLevelPoints) && (count($Names) <> count($TeamPoints)))
	{
		print('Ошибка данных по КП команды'."\n");
		return;
	}

	print('&nbsp;Взяты КП:'."\n");
	print('<table style="text-align: center; font-size: 100%; border-style: solid; border-width: 1px; border-color: #000000;" cellspacing="0" cellpadding="1">'."\n");
	print('<tr>'."\n");

	// Проверяем, что не отмечены все checkbox
	if (!strstr($TeamLevelPoints, '0') && !empty($TeamLevelPoints)) $AllChecked = ' checked';
	else $AllChecked = '';

	// Прописываем javascript, который ставит или сбрасывает все checkbox-ы
	print('<td>'."\n");
	print('&nbsp;Все&nbsp;<br /><input type="checkbox" name="chkall" value="on"'.$AllChecked.$DisabledResultText.' OnClick="javascript: ');
	for ($i = 0; $i < count($Names); $i++)
		print('document.TeamResultDataForm.Level'.$LevelId.'_chk'.$i.'.checked = this.checked;');
	print('"></td>'."\n");

	for ($i = 0; $i < count($Names); $i++)
	{
		print('<td style="border-left-style: solid; border-left-width: 1px; border-left-color: #000000;" title="'.$Penalties[$i].'">'.$Names[$i].'<br />'."\n");
		$Checked = (isset($TeamPoints[$i]) && ($TeamPoints[$i] == 1)) ? ' checked' : '';
		print('<input type="checkbox" name="Level'.$LevelId.'_chk'.$i.'" value="on"'.$Checked.$DisabledResultText.'>'."\n");
		print('</td>'."\n");
	}

	print('</tr>'."\n");
	print('</table>'."\n");
	return;
}
// ============ Конец функции вывода данных по КП


// Считаем, что все переменные уже определны. если нет - выходим
if ($TeamId <= 0) return;

// Результаты не могут отображаться, если команда только вводится
if ($viewmode == 'Add') return;

// Проверяем, можно ли показывать результаты в принципе
if (!CanViewResults($Administrator, $Moderator, $RaidStage)) return;

// И запоминаем на будущее, можно ли их редактировать
if (CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange))
{
	$AllowEditResult = 1;
	$NextResultActionName = 'ChangeTeamResult';
	$DisabledResultText = '';
	$OnSubmitResultFunction = 'return ValidateTeamResultDataForm();';
}
else
{
	$AllowEditResult = 0;
	$NextResultActionName = '';
	$DisabledResultText = ' readonly';
	$OnSubmitResultFunction = 'return false;';
}
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateTeamResultDataForm()
	{
		document.TeamResultDataForm.action.value = "<? echo $NextResultActionName; ?>";
		return true;
	}

	// Функция отмены изменения
	function CancelResult()
	{
		document.TeamResultDataForm.action.value = "CancelChangeTeamResultData";
		document.TeamResultDataForm.submit();
	}

	// Функция выделения
	function SelectEntry(loElement)
	{
		loElement.select();
	}

	// Выделеить все checkbox-ы
	function CheckAll(oForm, chkName, checked)
	{
		for (var i = 0; i < oForm[chkName].length; i++)
			oForm[chkName][i].checked = checked;
	}
</script>
<?php

print('<br /><div><b><big>Результаты:</big></b></div>'."\n");

// Форма показа/редактироания результатов
print('<form name="TeamResultDataForm" action="'.$MyPHPScript.'" method="post" onSubmit="'.$OnSubmitResultFunction.'">'."\n");
print('<input type="hidden" name="sessionid" value="'.$SessionId.'">'."\n");
print('<input type="hidden" name="action" value="">'."\n");
print('<input type="hidden" name="view" value="'.(($viewmode == "Add") ? 'ViewRaidTeams' : 'ViewTeamData').'">'."\n");
print('<input type="hidden" name="TeamId" value="'.$TeamId.'">'."\n");
print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");

print('<table border="0" cellpadding="7" style="font-size: 80%">'."\n");

// ============ Общее время команды
$sql = "select TIME_FORMAT(t.team_result, '%H:%i') as team_result from Teams t where t.team_id = ".$TeamId;
$Result = MySqlQuery($sql);
$Row = mysql_fetch_assoc($Result);
$TeamResult = $Row['team_result'];
mysql_free_result($Result);
if ($TeamResult == "00:00") $TeamResult = "-";
print('<tr><td colspan="5">'."\n");
print('Общее время с учетом штрафов и бонусов: <b title="Обновляется после сохранения результатов">'.$TeamResult.'</b>'."\n");
print('</td></tr>'."\n\n");

// ============ Шапка таблицы
print('<tr class="gray">'."\n");
print('<td>Этап</td>'."\n");
print('<td>Параметры старта/финиша</td>'."\n");
print('<td style="text-align: center">Старт</td>'."\n");
print('<td style="text-align: center">Финиш</td>'."\n");
print('<td>Комментарий</td>'."\n");
print('</tr>'."\n\n");


// Выводим данные только, когда минимальное время начала этапа меньше или равно текущему
// Довольно своеорбазно определяем год, чтобы не вводить его каждый раз
$sql = "select l.level_id, l.level_name, l.level_pointnames, l.level_starttype,
	l.level_pointpenalties, l.level_order,
	DATE_FORMAT(l.level_begtime,    '%d.%m %H:%i') as level_sbegtime,
	DATE_FORMAT(l.level_maxbegtime, '%d.%m %H:%i') as level_smaxbegtime,
	DATE_FORMAT(l.level_minendtime, '%d.%m %H:%i') as level_sminendtime,
	DATE_FORMAT(l.level_endtime,    '%d.%m %H:%i') as level_sendtime,
	CASE WHEN COALESCE(YEAR(l.level_minendtime), YEAR(NOW())) = COALESCE(YEAR(l.level_endtime), YEAR(NOW()))
		THEN COALESCE(YEAR(l.level_minendtime), YEAR(NOW()))
		ELSE YEAR(NOW())
	END as level_sendyear,
	CASE WHEN COALESCE(YEAR(l.level_maxbegtime), YEAR(NOW())) = COALESCE(YEAR(l.level_begtime), YEAR(NOW()))
		THEN COALESCE(YEAR(l.level_maxbegtime), YEAR(NOW()))
		ELSE YEAR(NOW())
	END as level_sbegyear,
	l.level_begtime, l.level_maxbegtime, l.level_minendtime, l.level_endtime,
	tl.teamlevel_begtime, tl.teamlevel_endtime,
	DATE_FORMAT(tl.teamlevel_begtime, '%Y') as teamlevel_sbegyear,
	DATE_FORMAT(tl.teamlevel_begtime, '%d%m') as teamlevel_sbegdate,
	DATE_FORMAT(tl.teamlevel_begtime, '%H%i') as teamlevel_sbegtime,
	DATE_FORMAT(tl.teamlevel_endtime, '%Y') as teamlevel_sendyear,
	DATE_FORMAT(tl.teamlevel_endtime, '%d%m') as teamlevel_senddate,
	DATE_FORMAT(tl.teamlevel_endtime, '%H%i') as teamlevel_sendtime,
	tl.teamlevel_points, tl.teamlevel_penalty,
	tl.teamlevel_id, tl.teamlevel_comment, tl.teamlevel_progress
	from Teams t
		inner join Distances d on t.distance_id = d.distance_id
		inner join Levels l on d.distance_id = l.distance_id
		left outer join TeamLevels tl
			on l.level_id = tl.level_id and t.team_id = tl.team_id and tl.teamlevel_hide = 0
	where t.team_id = ".$TeamId;
// Пока убрал ограничение по выдаче этапов от времени просмотра
// $sql = $sql." and l.level_begtime <= now() ";
$sql = $sql." order by l.level_order ";
$Result = MySqlQuery($sql);


// ============ Цикл обработки данных по этапам ===============================
while ($Row = mysql_fetch_assoc($Result))
{
	// По этому ключу потом определяем, есть ли уже строчка в TeamLevels или её нужно создать
	$TeamLevelId = $Row['teamlevel_id'];
	$LevelStartType = $Row['level_starttype'];
	$LevelPointNames = $Row['level_pointnames'];
	$LevelPointPenalties = $Row['level_pointpenalties'];
	$TeamLevelPoints = ($TeamLevelId > 0) ? $Row['teamlevel_points'] : '&nbsp;';
	$TeamLevelComment = $Row['teamlevel_comment'];
	$TeamLevelProgress = $Row['teamlevel_progress'];

	// ============ Название этапа
	print('<tr><td><b>'.$Row['level_name'].'</b></td>'."\n");

	// ============ Параметры старта/финиша
	// Делаем оформление в зависимости от типа старта и соотношения гранчиных дат:
	// Есди даты границ совпадают - выводим только первую
	// Если есть даты старта и фнишиа и они совпадают - выодим только дату старта
	if (empty($LevelStartType)) $LevelStartType = 2;
	if ($LevelStartType == 1)
	{
		$LevelStartTypeText = 'По готовности (';
		if (substr(trim($Row['level_sbegtime']), 0, 5) == substr(trim($Row['level_smaxbegtime']), 0, 5))
			$LevelStartTypeText = $LevelStartTypeText.$Row['level_sbegtime'].' - '.substr(trim($Row['level_smaxbegtime']), 6);
		else
			$LevelStartTypeText = $LevelStartTypeText.$Row['level_sbegtime'].' - '.$Row['level_smaxbegtime'];
		$LevelStartTypeText = $LevelStartTypeText.')/(';

	}
	elseif ($LevelStartType == 2)
	{
		$LevelStartTypeText = 'Общий ('.$Row['level_sbegtime'];
		$LevelStartTypeText = $LevelStartTypeText.')/(';

	}
	elseif ($LevelStartType == 3)
	{
		$LevelStartTypeText = 'Во время финиша (';
	}
	// Дополняем рамками финиша
	// Проверяем на одинаковые даты
	if (substr(trim($Row['level_sminendtime']), 0, 5) == substr(trim($Row['level_sendtime']), 0, 5))
	{
		if (substr(trim($Row['level_sbegtime']), 0, 5) == substr(trim($Row['level_sendtime']), 0, 5))
			$LevelStartTypeText = $LevelStartTypeText.substr(trim($Row['level_sminendtime']), 6).' - '.substr(trim($Row['level_sendtime']), 6);
		else
			$LevelStartTypeText = $LevelStartTypeText.$Row['level_sminendtime'].' - '.substr(trim($Row['level_sendtime']), 6);
	}
	else
	{
		$LevelStartTypeText = $LevelStartTypeText.$Row['level_sminendtime'].' - '.$Row['level_sendtime'];
	}
	$LevelStartTypeText = $LevelStartTypeText.')';
	print('<td>'.$LevelStartTypeText.'</td>'."\n");

	// ============ Поля ввода для времени старта (text или hidden)
	print('<td style="text-align: center">');
	if ($LevelStartType == 1)
	{
		// год записываем из данных этапа (макс и мин время старта/финиша
		print("\n".'<input type="hidden" maxlength="4" name="Level'.$Row['level_id'].'_begyear" size="3" value="'.$Row['level_sbegyear'].'" >'."\n");
		// Если даты совпадают - отключаем поле даты с помощью readonly
		// Нельзя просто ставить disabled, т.к. в этом случае параметр не передается
		if (substr(trim($Row['level_sbegtime']), 0, 5) == substr(trim($Row['level_smaxbegtime']), 0, 5))
		{
			$TeamLevelBegDate = substr(trim($Row['level_sbegtime']), 0, 2).substr(trim($Row['level_sbegtime']), 3, 2);
			$BegDateReadOnly = 'readonly';
		}
		else
		{
			$TeamLevelBegDate = $Row['teamlevel_sbegdate'];
			$BegDateReadOnly = '';
		}
		print('<input type="Text" maxlength="4" name="Level'.$Row['level_id'].'_begdate" size="3" value="'.$TeamLevelBegDate.'" tabindex="'.(++$TabIndex).'"'.$DisabledResultText.' '.$BegDateReadOnly.' title="ддмм - день месяц без разделителя" onclick = "this.select();" onkeydown="if (event.keyCode == 13 && this.value.length == 4) {document.TeamResultDataForm.Level'.$Row['level_id'].'_begtime.focus();}">'."\n");
		print('<input type="Text" maxlength="4" name="Level'.$Row['level_id'].'_begtime" size="3" value="'.$Row['teamlevel_sbegtime'].'" tabindex="'.(++$TabIndex).'"'.$DisabledResultText.' onclick="this.select();" title="ччмм - часы минуты без разделителя">'."\n");
	}
	else
	{
		print('-');
	}
	print('</td>'."\n");

	// ============ Поля ввода для времени финиша (text или hidden)
	print('<td style="text-align: center">'."\n");
	print('<input type="hidden" name="Level'.$Row['level_id'].'_endyear" value="'.$Row['level_sendyear'].'">'."\n");
	// Если даты совпадают - отключаем поле даты с помощью readonly
	// Нельзя просто ставить disabled, т.к. в этом случае параметр не передается
	if (substr(trim($Row['level_sendtime']), 0, 5) == substr(trim($Row['level_sminendtime']), 0, 5))
	{
		$TeamLevelEndDate = substr(trim($Row['level_sendtime']), 0, 2).substr(trim($Row['level_sendtime']), 3, 2);
		$EndDateReadOnly = 'readonly';

	}
	else
	{
		$TeamLevelEndDate = $Row['teamlevel_senddate'];
		$EndDateReadOnly = '';
	}
	print('<input type="Text" maxlength="4" name="Level'.$Row['level_id'].'_enddate" size="3" value="'.$TeamLevelEndDate.'" tabindex="'.(++$TabIndex).'"'.$DisabledResultText.' '.$EndDateReadOnly.' title="ддмм - день месяц без разделителя" onclick="this.select();" onkeydown="if (event.keyCode == 13 && this.value.length == 4) {document.TeamResultDataForm.Level'.$Row['level_id'].'_endtime.focus();}">'."\n");
	print('<input type="Text" maxlength="4" name="Level'.$Row['level_id'].'_endtime" size="3" value="'.$Row['teamlevel_sendtime'].'" tabindex="'.(++$TabIndex).'"'.$DisabledResultText.' onclick="this.select();" title="ччмм - часы минуты без разделителя">'."\n");
	print('</td>'."\n");

	// ============ Комментарий
	print('<td>'."\n");
	print('<input type="text" name="Level'.$Row['level_id'].'_comment" size="15" value="'.$TeamLevelComment.'" tabindex="'.(++$TabIndex).'"'.$DisabledResultText.' onclick="this.select();" title="Комментарий к этапу">'."\n");
	print('</td></tr>'."\n\n");

	// ============ Следующая строка - сход команды с этапа
	print('<tr><td colspan="5" style="padding-top: 0px">'."\n");
	print('<select name="Level'.$Row['level_id'].'_progress" tabindex="'.(++$TabIndex).'"'.$DisabledResultText.'>'."\n");
	print('<option value="0"'.(($TeamLevelProgress == 0) ? ' selected' : '').'>Не вышла на этап</option>'."\n");
	print('<option value="1"'.(($TeamLevelProgress == 1) ? ' selected' : '').'>Сошла с этапа</option>'."\n");
	print('<option value="2"'.(($TeamLevelProgress == 2) ? ' selected' : '').'>Дошла до конца этапа</option>'."\n");
	print('</select>'."\n");
	print('</td></tr>'."\n");

	// ============ Следующая строка - взятые КП
	print('<tr><td colspan="5" style="padding-top: 0px; border-bottom-style: dotted; border-bottom-width: 1px; border-bottom-color: #000000;">'."\n");
	ConvertTeamLevelPointsToHTML($Row['level_pointnames'], $Row['level_pointpenalties'], $Row['teamlevel_points'], $Row['level_id'], $DisabledResultText);
	print('</td></tr>'."\n\n");
}
// ============ Конец цикла обработки данных по этапам ========================
mysql_free_result($Result);
// Закрываем таблицу
print('<tr><td colspan="5">*Наведите курсор на ячейку с КП, чтобы узнать штраф за его невзятие</td></tr>'."\n");
print('</table>'."\n\n");


// ============ Блок кнопок сохранения результатов ============================
if ($AllowEditResult == 1)
{
	print('<table class="menu" border="0" cellpadding="0" cellspacing="0">'."\n");
	print('<tr><td class="input" style="padding-top: 10px;">'."\n");
	$TabIndex++;
	print('<input type="button" onClick="javascript: if (ValidateTeamResultDataForm()) submit();" name="SaveChangeResultButton" value="Сохранить результаты" tabindex="'.$TabIndex.'">'."\n");
	$TabIndex++;
	print('<select name="CaseView" onChange="javascript:document.TeamResultDataForm.view.value = document.TeamResultDataForm.CaseView.value;" class="leftmargin" tabindex="'.$TabIndex.'">'."\n");
	print('<option value="ViewTeamData" selected >и остаться на этой странице'."\n");
	print('<option value="ViewRaidTeams" >и перейти к списку команд'."\n");
	print('</select>'."\n");
	$TabIndex++;
	print('<input type="button" onClick="javascript: CancelResult();" name="CancelButton" value="Отмена" tabindex="'.$TabIndex.'">'."\n");
	print('</td></tr>'."\n");
	print('</table>'."\n");
}

// Закрываем форму
print('</form>'."\n");
?>
