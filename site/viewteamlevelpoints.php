<?php
// +++++++++++ Показ/редактирование результатов команды +++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// Считаем, что все переменные уже определны. если нет - выходим
if ($TeamId <= 0) return;




if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";

/*
echo 'mode '.$viewmode;
echo 'submode '.$viewsubmode;
*/

//  TeamLevelPoint = Tlp

// Проверяем, можно ли показывать результаты в принципе
if (!CanViewResults($Administrator, $Moderator, $RaidStage)) return;

// И запоминаем на будущее, можно ли их редактировать
if (CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange))
{
	$AllowEditResult = 1;
	$DisabledResultText = '';
	$OnSubmitResultFunction = 'return ValidateTlpForm();';
}
else
{
	$AllowEditResult = 0;
	$DisabledResultText = ' readonly';
	$OnSubmitResultFunction = 'return false;';
}



// ================ Форма для добавления новой  точки ===================================
if ($viewmode == 'AddTlp' or $viewmode == '')
{

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterErrorTlp")
	{
		ReverseClearArrays();

		$LevelPointId = $_POST['LevelPointId'];
                $TlpComment = $_POST['TlpComment'];
                $TlpYear = $_POST['TlpYear'];
                $TlpDate = $_POST['TlpDate'];
                $TlpTime = $_POST['TlpTime'];
		$PointName = '';


	}
	else
	// Инициализация перемнных для новой точки 
	{

                // дистанция уже должна быть инициализирована в raidactions!
                if (empty($DistanceId)) {return;}
                if (empty($TeamId)) {return;}
		
	        $LevelPointId = 0;
                $TlpComment = '';
                $TlpYear = '';
                $TlpDate = '';
                $TlpTime = '';
		$PointName = '';
	    
	}

	// Определяем следующее действие
	$NextActionName = 'AddTlp';
	// Действие на текстовом поле по клику
	$OnClickText = ' onClick="javascript:this.value = \'\';"';
	// Надпись на кнопке
	$SaveButtonText = 'Добавить точку';

        $pTeamLevelPointId = 0;
}

else
// ================ Редактируем/смотрим существующую точку =================
{

 
        $pTeamLevelPointId = $_POST['TeamLevelPointId'];

	if ($pTeamLevelPointId <= 0)
	{
		return;
	}

       // 19.06.2015 Добавил level_id чтобы проставить ввести точки по старым ММБ,
       // потом можно и нужно убрать

	$sql = "select tlp.levelpoint_id, 
	               lp.levelpoint_name as lp_name,
	               tlp.teamlevelpoint_comment as tlp_comment,
		       tlp.teamlevelpoint_duration, 
		       tlp.teamlevelpoint_penalty as tlp_penalty, 
		       tlp.teamlevelpoint_datetime, 
		       DATE_FORMAT(tlp.teamlevelpoint_datetime, '%Y') as tlp_syear,
		       DATE_FORMAT(tlp.teamlevelpoint_datetime, '%d%m') as tlp_sdate,
		       DATE_FORMAT(tlp.teamlevelpoint_datetime, '%H%i%s') as tlp_stime,
		       DATE_FORMAT(tlp.teamlevelpoint_duration, '%H%i%s') as tlp_duration
		from TeamLevelPoints tlp
		     inner join LevelPoints lp
		     on lp.levelpoint_id = tlp.levelpoint_id
		where tlp.teamlevelpoint_id = ".$pTeamLevelPointId;

        

	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterErrorTlp")
	{
		ReverseClearArrays();
		$LevelPointId = $_POST['LevelPointId'];
                $TlpComment = $_POST['TlpComment'];
                $TlpYear = $_POST['TlpYear'];
                $TlpDate = $_POST['TlpDate'];
                $TlpTime = $_POST['TlpTime'];
		$PointName = '';

	}
	else
	{



		$LevelPointId = $Row['levelpoint_id'];
                $TlpComment = $Row['tlp_comment'];
                $TlpYear = $Row['tlp_syear'];
                $TlpDate = $Row['tlp_sdate'];
                $TlpTime = $Row['tlp_stime'];
		$PointName = $Row['lp_name'];


	}	

	$NextActionName = 'ChangeTlp';
	$OnClickText = '';
	$SaveButtonText = 'Сохранить результат';

}


if (empty($TlpYear) or $TlpYear = '0000') {
   $TlpYear = date('Y');
//   echo $TlpYear;
}

// ================ Конец инициализации переменных для добавляемой/редактируемой точки =================


// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateTlpForm()
	{
		document.TlpForm.action.value = "<? echo $NextActionName; ?>";
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


	// 
</script>


<?php

print('<br/>'."\n");


// ============ Общее время команды
$sql = "select TIME_FORMAT(t.team_result, '%H:%i') as team_result from Teams t where t.team_id = ".$TeamId;
$Result = MySqlQuery($sql);
$Row = mysql_fetch_assoc($Result);
$TeamResult = $Row['team_result'];
mysql_free_result($Result);

$TeamPlace = GetTeamPlace($TeamId);
$TeamPlaceResult = "";
if ($TeamResult == "00:00") $TeamResult = "-";
if ($TeamPlace > 0) $TeamPlaceResult = " Место <b>".$TeamPlace."</b>";



$tdstyle = 'padding: 5px 0px 2px 5px;';		
$thstyle = 'padding: 5px 0px 0px 5px;';		

print('<b>Результаты.</b> Общее время с учетом штрафов и бонусов: <b title="Обновляется после сохранения результатов">'.$TeamResult.'</b>'.$TeamPlaceResult."\n");

print('<br/>'."\n");



if ($AllowEditResult == 1)
{

        print('<br/>'."\n");

	// Выводим начало формы с точкой
	print('<form name="TlpForm" action="'.$MyPHPScript.'" method="post"  onSubmit="'.$OnSubmitResultFunction.'">'."\n");
	print('<input type="hidden" name="sessionid" value="'.$SessionId.'">'."\n");
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

	while ($Row = mysql_fetch_assoc($Result))
	{
		$levelpointselected = ($Row['levelpoint_id'] == $LevelPointId ? 'selected' : '');
		print('<option value="'.$Row['levelpoint_id'].'" '.$levelpointselected.' >'.$Row['levelpoint_name']."</option>\n");
	}
	mysql_free_result($Result);
	print('</select>'."\n");
	print('</td></tr>'."\r\n");
	print('<tr><td>'."\r\n");
	print('Дата (ддмм) и время (ччммсс) прохождания точки: '."\r\n");
        // Можно отключить правку написав  $DateReadOnly = 'readonly'
        $DateReadOnly = '';

	print('<input type="hidden" maxlength="4" name="TlpYear" size="3" value="'.$TlpYear.'" >'."\n");
	print('<input type="Text" maxlength="4" name="TlpDate" size="3" value="'.$TlpDate.'" tabindex="'.(++$TabIndex).'"'.$DateReadOnly.' title="ддмм - день месяц без разделителя" onclick="this.select();" onkeydown="if (event.keyCode == 13 && this.value.length == 4) {document.TlpForm.TlpTime.focus();}">'."\n");
	print('<input type="Text" maxlength="6" name="TlpTime" size="6" value="'.$TlpTime.'" tabindex="'.(++$TabIndex).'"'.' onclick="this.select();" title="ччммсс - часы минуты секунды без разделителя">'."\n");
	print('</td></tr>'."\r\n");
	print('<tr><td class="input">'."\n");
        print(' Комментарий <input type="text" name="TlpComment" size="20" value="'.$TlpComment.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$TlpComment.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$TlpComment.'\';}"').'
                title = "Комментарий">'."\r\n");
	print('</td></tr>'."\n\n");


	// ================ Submit для формы ==========================================
	print('<tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateTlpForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
//	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	

	// ============ Кнопка удаления точки
	if (($viewmode <> "Add") && !empty($viewmode)  && ($AllowEditResult == 1))
	{
	print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить точку: '.trim($PointName).'? \')) {HideTlp();}" name="HideTlpButton" value="Удалить точку" tabindex="'.(++$TabIndex).'">'."\n");
	}

	print('</td></tr>'."\n\n");
	print('</table>'."\n");
	print('</form>'."\r\n");


}


print('<br/>'."\n");


print('<table border = "1" cellpadding = "0" cellspacing = "0" style = "font-size: 80%">'."\r\n");  


print('<tr class = "gray">
                         <td width = "100" style = "'.$thstyle.'">Точка</td>
                         <td width = "100" style = "'.$thstyle.'">Время</td>
		         <td width = "150" style = "'.$thstyle.'">Комментарий</td>
		         <td width = "200" style = "'.$thstyle.'">Результат (время)</td>
		         <td width = "150" style = "'.$thstyle.'">Штраф (минуты)</td>
		         '."\r\n");

		if ($AllowEdit == 1)
		{
		       print('<td width = "100" style = "'.$thstyle.'">&nbsp;</td>'."\r\n");

		}
		
	
		print('</tr>'."\r\n");

// Запрашиваем данные о точках. 
// можно добавить условие на то, что текущее время должно быть больше времени закрытия точки

$sql = "select tlp.teamlevelpoint_id, lp.levelpoint_id, lp.levelpoint_name, 
	DATE_FORMAT(tlp.teamlevelpoint_datetime,    '%d.%m %H:%i') as tlp_datetime,
	tlp.teamlevelpoint_comment,
	tlp.teamlevelpoint_duration,
	tlp.teamlevelpoint_penalty
	from TeamLevelPoints tlp
  	     inner join LevelPoints lp on tlp.levelpoint_id = lp.levelpoint_id 
  	     inner join Distances d on d.distance_id = lp.distance_id 
	where tlp.team_id = ".$TeamId;

//$sql = $sql." and COALESCE(lp.levelpoint_endtime, now()) <= now() ";
$sql = $sql." order by lp.levelpoint_order ASC ";

//echo  $sql;

$Result = MySqlQuery($sql);


// ============ Цикл обработки данных по этапам ===============================
while ($Row = mysql_fetch_assoc($Result))
{



       print('<tr>'."\r\n");
       print('<td align = "left" style = "'.$tdstyle.'">'.$Row['levelpoint_name'].'</td>
              <td align = "left" style = "'.$tdstyle.'">'.$Row['tlp_datetime'].'</td>
              <td align = "left" style = "'.$tdstyle.'">'.$Row['teamlevelpoint_comment'].'</td>
              <td align = "left" style = "'.$tdstyle.'">'.$Row['teamlevelpoint_duration'].'</td>
	      <td align = "left" style = "'.$tdstyle.'">'.$Row['teamlevelpoint_penalty'].'</td>
	    ');

  	if ($AllowEdit == 1)
	{
	     print('<td align = "left" style = "'.$tdstyle.'">');
	     print('&nbsp; <input type="button" onClick="javascript: EditTlp('.$Row['teamlevelpoint_id'].');" name="EditTlpButton" value="Править" tabindex="'.(++$TabIndex).'">'."\n");
	     print('</td>'."\r\n");
	}		      
               

	print('</tr>'."\r\n");

}
// ============ Конец цикла обработки данных по этапам ========================
mysql_free_result($Result);


// Закрываем таблицу
//print('<tr><td colspan="5">*Наведите курсор на ячейку с КП, чтобы узнать штраф за его невзятие</td></tr>'."\n");
print('</table>'."\r\n");
print('<br/>'."\r\n");


// Закрываем форму
print('</form>'."\n");
?>
