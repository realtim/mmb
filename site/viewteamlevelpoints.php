<?php
/**
 * +++++++++++ Показ/редактирование результатов команды +++++++++++++++++++++++
 */

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) {
    return;
}

// Считаем, что все переменные уже определены, если нет - выходим
if ($TeamId <= 0) {
    return;
}

if (!isset($viewmode)) {
    $viewmode = "";
}
if (!isset($viewsubmode)) {
    $viewsubmode = "";
}

//  TeamLevelPoint = Tlp

// Проверяем, можно ли показывать результаты в принципе
if (!CanViewResults($Administrator, $Moderator, $RaidStage)) {
    return;
}

// И запоминаем на будущее, можно ли их редактировать
if (CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)) {
    $AllowEditResult = 1;
    $DisabledResultText = '';
    $OnSubmitResultFunction = 'return ValidateTlpForm();';
} else {
    $AllowEditResult = 0;
    $DisabledResultText = ' readonly';
    $OnSubmitResultFunction = 'return false;';
}

// ================ Форма для добавления новой  точки ===================================
if ($viewmode === 'AddTlp' || $viewmode == '')
{
    // Если вернулись после ошибки переменные не нужно инициализировать
    if ($viewsubmode === "ReturnAfterErrorTlp") {
        ReverseClearArrays();

        $LevelPointId = $_POST['LevelPointId'];
        $TlpComment = $_POST['TlpComment'];
        $TlpYear = $_POST['TlpYear'];
        $TlpDate = $_POST['TlpDate'];
        $TlpTime = $_POST['TlpTime'];
        $PointName = '';
        $ErrorId = $_POST['ErrorId'];
    } else {// Инициализация переменных для новой точки

        // дистанция уже должна быть инициализирована в raidactions!
        if (empty($DistanceId)) {
            return;
        }
        if (empty($TeamId)) {
            return;
        }

        $LevelPointId = 0;
        $TlpComment = '';
        $TlpYear = '';
        $TlpDate = '';
        $TlpTime = '';
        $PointName = '';
        $ErrorId = 0;
    }

    // Определяем следующее действие
    $NextActionName = 'AddTlp';
    // Действие на текстовом поле по клику
    $OnClickText = ' onClick="javascript:this.value = \'\';"';
    // Надпись на кнопке
    $SaveButtonText = 'Добавить точку';

    $pTeamLevelPointId = 0;
} else {
// ================ Редактируем/смотрим существующую точку =================

    $pTeamLevelPointId = mmb_validateInt($_POST, 'TeamLevelPointId');

    if ($pTeamLevelPointId <= 0) {
        return;
    }

       // 19.06.2015 Добавил level_id чтобы проставить ввести точки по старым ММБ,
       // потом можно и нужно убрать

	$sql = "select tlp.levelpoint_id, 
	               lp.levelpoint_name as lp_name,
	               tlp.teamlevelpoint_comment as tlp_comment,
		       tlp.teamlevelpoint_duration, 
		       tlp.teamlevelpoint_penalty as tlp_penalty, 
		       tlp.teamlevelpoint_result as tlp_result, 
		       tlp.teamlevelpoint_datetime, 
		       COALESCE(tlp.error_id, 0) as error_id,
		       DATE_FORMAT(tlp.teamlevelpoint_datetime, '%Y') as tlp_syear,
		       DATE_FORMAT(tlp.teamlevelpoint_datetime, '%d%m') as tlp_sdate,
		       DATE_FORMAT(tlp.teamlevelpoint_datetime, '%H%i%s') as tlp_stime,
		       DATE_FORMAT(tlp.teamlevelpoint_duration, '%H%i%s') as tlp_duration
		from TeamLevelPoints tlp
		     inner join LevelPoints lp
		     on lp.levelpoint_id = tlp.levelpoint_id
		where tlp.teamlevelpoint_id = $pTeamLevelPointId";

	$Row = CSql::singleRow($sql);

    // Если вернулись после ошибки переменные не нужно инициализировать
    if ($viewsubmode === "ReturnAfterErrorTlp") {
        ReverseClearArrays();
        $LevelPointId = mmb_validateInt($_POST, 'LevelPointId');
        $TlpComment = $_POST['TlpComment'];
        $TlpYear = $_POST['TlpYear'];
        $TlpDate = $_POST['TlpDate'];
        $TlpTime = $_POST['TlpTime'];
        $PointName = '';
        $ErrorId = mmb_validateInt($_POST, 'ErrorId');
    } else {
        $LevelPointId = $Row['levelpoint_id'];
        $TlpComment = $Row['tlp_comment'];
        $TlpYear = $Row['tlp_syear'];
        $TlpDate = $Row['tlp_sdate'];
        $TlpTime = $Row['tlp_stime'];
        $PointName = $Row['lp_name'];
        $ErrorId = $Row['error_id'];
    }

	$NextActionName = 'ChangeTlp';
	$OnClickText = '';
	$SaveButtonText = 'Сохранить результат';
}

// В форме правки выводится только день и время, год считаем по дате регистрации ММБ и не выводим
if (empty($TlpYear) || (int)$TlpYear === 0) {

	$sql = "select YEAR(r.raid_registrationenddate) as raidyear
		from Raids r
	             inner join Distances d
		     on d.raid_id = r.raid_id
		where d.distance_id = $DistanceId";

	$TlpYear = CSql::singleValue($sql, 'raidyear');
}

// ================ Конец инициализации переменных для добавляемой/редактируемой точки =================


// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateTlpForm()
	{
		document.TlpForm.action.value = "<?php echo $NextActionName; ?>";
		return true;
	}
	// Конец проверки правильности заполнения формы

	// Скрыть точку
	function HideTlp()
	{
		document.TlpForm.action.value = 'HideTlp';
		document.TlpForm.submit();
	}
	
	// Править точку
	function EditTlp(teamlevelpointid)
	{
		document.TlpForm.TeamLevelPointId.value = teamlevelpointid;
		document.TlpForm.action.value = 'TlpInfo';
		document.TlpForm.submit();
	}
</script>


<?php

print('<br/>'."\n");

// переменная определена в модуле viewteamdat если нужно - можно здесь ещё один запрос сделать, как для общего результатам ниже
if ($TeamDismiss) {
    print("<b>Результаты.</b> Команда в полном составе не явилась на старт.\n");
    print('<br/>' . "\n");
} else {
	// ============ Общее время команды
	$sql = "select COALESCE(TIME_FORMAT(t.team_result, '%H:%i'), '00:00') as team_result from Teams t where t.team_id = $TeamId";
	$TeamResult = CSql::singleValue($sql, 'team_result');

	$TeamPlace = GetTeamPlace($TeamId);
	$TeamPlaceResult = "";
	if ($TeamResult === "00:00") {
        $TeamResult = "-";
    }
	if ($TeamPlace > 0) {
        $TeamPlaceResult = " Место <b>$TeamPlace</b>";
    }

	print("<b>Результаты.</b> Общее время с учетом штрафов и бонусов: <b title=\"Обновляется после сохранения результатов\">$TeamResult</b>$TeamPlaceResult\n");
}

print("<br/>\n");
print("<div style=\"margin-top: 10px; font-size: 80%;\">В таблице отмечаются только взятые (пройденные) точки, а совокупный штраф за пропущенные отображается в следующей (по номеру) взятой точке. Для точек с отсечкой времени в колонке 'Время' отражается разница с предыдущей (длительность прохождения этого участка). Для точек с амнистией штраф начисляется на указанную организаторами точку, обычно это следующая за 'облаком' точка с отсечкой времени.</div>\n");

if ($AllowEditResult === 1) {
    print("<br/>\n");
	// Выводим начало формы с точкой
	print('<form name="TlpForm" action="'.$MyPHPScript.'" method="post"  onSubmit="'.$OnSubmitResultFunction.'">'."\n");
	print('<input type="hidden" name="action" value="">'."\n");
	print('<input type="hidden" name="view" value="">'."\n");
	print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
	print('<input type="hidden" name="TeamId" value="'.$TeamId.'">'."\n");
	print('<input type="hidden" name="DistanceId" value="'.$DistanceId.'">'."\n");
	print('<input type="hidden" name="TeamLevelPointId" value="'.$pTeamLevelPointId.'">'."\n\n");
	print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");
	$DisabledText = '';

    // 19.06.2015 Добавил level_id чтобы проставить ввести точки по старым ММБ,
    // потом можно и нужно убрать

	print('<tr><td class="input">'."\n");
	print('Точка '."\n");
	print('<select name="LevelPointId" class="leftmargin" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
	$sql = "select levelpoint_id, levelpoint_name from LevelPoints where distance_id = ".$DistanceId." order by levelpoint_order ";
	$Result = MySqlQuery($sql);

	print('<option value="0">Не указана</option>'."\n");

    while ($Row = mysqli_fetch_assoc($Result)) {
        $levelpointselected = ($Row['levelpoint_id'] == $LevelPointId ? 'selected' : '');
        print('<option value="' . $Row['levelpoint_id'] . '" ' . $levelpointselected . ' >' . $Row['levelpoint_name'] . "</option>\n");
    }
	mysqli_free_result($Result);
	print('</select>'."\n");
	print("</td></tr>\r\n");
	print("<tr><td>\r\n");
	print("Дата (ддмм) и время (ччммсс) прохождения точки: \r\n");
        // Можно отключить правку написав  $DateReadOnly = 'readonly'
        $DateReadOnly = '';

	print('<input type="hidden" maxlength="4" name="TlpYear" size="3" value="'.$TlpYear.'" >'."\n");
	print('<input type="Text" maxlength="4" name="TlpDate" size="3" value="'.$TlpDate.'" tabindex="'.(++$TabIndex).'"'.$DateReadOnly.' title="ддмм - день месяц без разделителя" onclick="this.select();" onkeydown="if (event.keyCode == 13 && this.value.length == 4) {document.TlpForm.TlpTime.focus();}">'."\n");
	print('<input type="Text" maxlength="6" name="TlpTime" size="6" value="'.$TlpTime.'" tabindex="'.(++$TabIndex).'"'.' onclick="this.select();" title="ччммсс - часы минуты секунды без разделителя">'."\n");
	print('</td></tr>'."\r\n");
	print('<tr><td class="input">'."\n");
	print('Ошибка '."\n");
	print('<select name="ErrorId" class="leftmargin" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
	$sql = "select error_id, error_name from Errors order by error_id ";
	$Result = MySqlQuery($sql);

	while ($Row = mysqli_fetch_assoc($Result))
	{
		$errorselected = ($Row['error_id'] == $ErrorId ? 'selected' : '');
		print('<option value="'.$Row['error_id'].'" '.$errorselected.' >'.$Row['error_name']."</option>\n");
	}
	mysqli_free_result($Result);
	print('</select>'."\n");
	print('</td></tr>'."\r\n");
	print('<tr><td class="input">'."\n");
        print(' Комментарий <input type="text" name="TlpComment" size="20" value="'.$TlpComment.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.' '
                 .($viewmode !== 'Add' ? '' : CMmbUI::placeholder($TlpComment))
		 .'title="Комментарий">'."\r\n");
	print("</td></tr>\r\n");

	// ================ Submit для формы ==========================================
	print('<tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateTlpForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
//	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	

	// ============ Кнопка удаления точки
	if (($viewmode !== "Add") && !empty($viewmode)  && ($AllowEditResult === 1))
	{
	print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить точку: '.trim($PointName).'? \')) {HideTlp();}" name="HideTlpButton" value="Удалить точку" tabindex="'.(++$TabIndex).'">'."\n");
	}

	print("</td></tr>\r\n");
	print("</table>\n");
	print("</form>\r\n");
}

print("<br/>\n");
print("<table class=\"std\">\r\n");
print('<tr class="head">
                <td>Взятые кп</td>
                <td>Дата отметки</td>
                <td>Время отметки</td>
                <td>Ошибка</td>
		        <td>Комментарий</td>
		        <td>Сплит (время на отрезке)</td>
		        <td>Штраф, минуты</td>
		        <td>Время на дистанции</td>
	 '."\r\n");

if ($AllowEdit === 1) {
    print('<td>&nbsp;</td>' . "\r\n");
}
print("</tr>\r\n");

// Запрашиваем данные о точках. 
// можно добавить условие на то, что текущее время должно быть больше времени закрытия точки

/*
$sql = "select count(tlp.teamlevelpoint_id) as cnt
	from TeamLevelPoints tlp
 	where tlp.team_id = $TeamId and (tlp.teamlevelpoint_datetime is null or tlp.teamlevelpoint_datetime <= 0)";
$TeamNoTimePointsNumber = CSql::singleValue($sql, 'cnt');
*/

$sql = "select tlp.teamlevelpoint_id, lp.levelpoint_id, lp.levelpoint_name, 
	COALESCE(DATE_FORMAT(tlp.teamlevelpoint_datetime,'%d.%m.%y'), '-') as tlp_date,
	COALESCE(DATE_FORMAT(tlp.teamlevelpoint_datetime, '%H:%i'), '-') as tlp_time,
	tlp.teamlevelpoint_comment,
	tlp.error_id,
	IF(tlp.error_id = 0, '-', COALESCE(err.error_name, '')) as error_name,
	COALESCE(tlp.teamlevelpoint_duration, '-') as teamlevelpoint_duration,
	tlp.teamlevelpoint_penalty,
	tlp.teamlevelpoint_result
	from TeamLevelPoints tlp
  	     inner join LevelPoints lp on tlp.levelpoint_id = lp.levelpoint_id 
  	     inner join Distances d on d.distance_id = lp.distance_id 
	     left outer join Errors err
	     on tlp.error_id = err.error_id 
	where tlp.team_id = $TeamId
	order by tlp.teamlevelpoint_datetimeaftercorrection ASC, lp.levelpoint_order ASC
	";

/*
if ($TeamNoTimePointsNumber)
{	
	$sql = $sql." order by lp.levelpoint_order ASC ";
} else 
{
	$sql = $sql." order by tlp.teamlevelpoint_datetime ASC ";
}
*/

$Result = MySqlQuery($sql);

// ============ Цикл обработки данных по этапам ===============================
while ($Row = mysqli_fetch_assoc($Result))
{

	print("<tr>\r\n");
	print("<td>{$Row['levelpoint_name']}</td>
              <td>{$Row['tlp_date']}</td>
              <td>{$Row['tlp_time']}</td>
              <td>{$Row['error_name']}</td>
              <td>{$Row['teamlevelpoint_comment']}</td>
              <td>{$Row['teamlevelpoint_duration']}</td>
	      <td>{$Row['teamlevelpoint_penalty']}</td>
	      <td>{$Row['teamlevelpoint_result']}</td>");

  	if ($AllowEdit == 1)
	{
	     print('<td>');
	     print('&nbsp; <input type="button" onClick="javascript: EditTlp('.$Row['teamlevelpoint_id'].');" name="EditTlpButton" value="Править" tabindex="'.(++$TabIndex).'">'."\n");
	     print("</td>\r\n");
	}		      
               
	print("</tr>\r\n");
}
// ============ Конец цикла обработки данных по этапам ========================
mysqli_free_result($Result);


// Закрываем таблицу
//print('<tr><td colspan="5">*Наведите курсор на ячейку с КП, чтобы узнать штраф за его невзятие</td></tr>'."\n");
print("</table>\r\n");

 $sql = "    select lp.levelpoint_id
				, lp.levelpoint_name
				, lp.levelpoint_penalty
			from Distances d 
			inner join LevelPoints lp 
			on d.distance_id = lp.distance_id 
			left outer join (
			select lp1.levelpoint_id
			from  LevelPointDiscounts lpd 
				inner join LevelPoints lp1 
				on lp1.levelpoint_order >= lpd.levelpointdiscount_start
					and lp1.levelpoint_order <= lpd.levelpointdiscount_finish
					and lp1.distance_id = lpd.distance_id
			where lpd.distance_id = $DistanceId
				and COALESCE(lp1.levelpoint_hide, 0) = 0
				and COALESCE(lpd.levelpointdiscount_hide, 0) = 0
			) lpd1
			on lp.levelpoint_id = lpd1.levelpoint_id		
			left outer join TeamLevelPoints tlp 
			on tlp.levelpoint_id = lp.levelpoint_id 
			and  tlp.team_id = $TeamId
			where  d.distance_id = $DistanceId
			and tlp.levelpoint_id is null
			and lpd1.levelpoint_id	is null
			and COALESCE(lp.levelpoint_hide, 0) = 0
			order by lp.levelpoint_order ASC
	 ";
$Result = MySqlQuery($sql);
if (mysqli_num_rows($Result) > 0) {
    print("<br/>\r\n");
    print("<table class=\"std\">\r\n");
    print('<tr class="head">
                <td>Пропуск кп с собственным штрафом</td>
                <td>Штраф, минуты</td>
	 ' . "\r\n");

    while ($Row = mysqli_fetch_assoc($Result)) {
        print("<tr>\r\n");
        print("<td>{$Row['levelpoint_name']}</td>
			<td>{$Row['levelpoint_penalty']}</td>\r\n");
        print("</tr>\r\n");
    }
    print("</tr>\r\n");
    print("</table>\r\n");
}
mysqli_free_result($Result);

	$sql = "select lp.levelpoint_id, lp.levelpoint_name, 
				lp.levelpoint_penalty,
				lpd.levelpointdiscount_start,
				lpd.levelpointdiscount_finish,
				lpd.levelpointdiscount_value,
				lpd.levelpointdiscount_id
			from Distances d 
				inner join LevelPointDiscounts lpd 
				on d.distance_id = lpd.distance_id 
				inner join LevelPoints lp 
				on lp.levelpoint_order >= lpd.levelpointdiscount_start
					and lp.levelpoint_order <= lpd.levelpointdiscount_finish
					and lp.distance_id = lpd.distance_id
					and COALESCE(lp.levelpoint_hide, 0) = 0
				left outer join 
				(select lp1.levelpoint_order, lp1.levelpoint_id
				from TeamLevelPoints tlp 
					inner join LevelPoints lp1
					on lp1.levelpoint_id = tlp.levelpoint_id
				where tlp.team_id = $TeamId
				) tlplp
				on lp.levelpoint_id = tlplp.levelpoint_id 
			where  d.distance_id = $DistanceId 
				and tlplp.levelpoint_id  is null
				and COALESCE(lpd.levelpointdiscount_hide, 0) = 0
			order by lp.levelpoint_order ASC
			";

$Result = MySqlQuery($sql);
if (mysqli_num_rows($Result) > 0)
{
	print("<br/>\r\n");
	print("<table class=\"std\">\r\n");
	print('<tr class="head">
					<td>Номер кп</td>
					<td>Штраф, минуты</td>
					</tr>'."\r\n");
	
	$predDiscountId = 0;
	while ($Row = mysqli_fetch_assoc($Result))
	{
		if ($predDiscountId <> $Row['levelpointdiscount_id'])
		{
			$predDiscountId = $Row['levelpointdiscount_id'];
			print('<tr class="head">'."\r\n");
			print("<td>Облако {$Row['levelpointdiscount_start']} - {$Row['levelpointdiscount_finish']}</td>\r\n");
			print("<td>Амнистия, минуты {$Row['levelpointdiscount_value']}</td>\r\n");
			print("</tr>\r\n");
		}
		print("<tr>\r\n");
		print("<td>{$Row['levelpoint_name']}</td>
	   <td>{$Row['levelpoint_penalty']}</td>\r\n");
		print("</tr>\r\n");
	}
	print("</tr>\r\n");
	print("</table>\r\n");

}
mysqli_free_result($Result);

print("<br/>\r\n");

// Закрываем форму
print("</form>\n");
?>