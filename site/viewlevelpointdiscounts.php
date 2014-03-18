<?php
// +++++++++++ Добавление /правка / показ интервалов амнистии ++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";


// Определяем права по редактированию 
if ($Administrator || $Moderator)
{
	$AllowEdit = 1;
	$DisabledText = '';
	$OnSubmitFunction = 'return ValidateLevelPointDiscountForm();';
}
else
{
	$AllowEdit = 0;
	$DisabledText = ' disabled';
	$OnSubmitFunction = 'return false;';
}
// Определяем права по просмотру
if ($Administrator || $Moderator)
{
	$AllowViewResults = 1;
}
else 
{
       $AllowViewResults = 0;
}


// ================ Форма для добавления новой  точки ===================================
if ($viewmode == 'Add')
{

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();

		$DistanceId = $_POST['DistanceId'];
                $DiscountValue = $_POST['DiscountValue'];
                $DiscountStart = $_POST['DiscountStart'];
                $DiscountFinish = $_POST['DiscountFinish'];
                

	}
	else
	// Инициализация перемнных для новой точки 
	{



                // дистанция уже должна быть инициализирована в raidactions!
                if (empty($DistanceId)) {return;}
		
                $DiscountValue = 0;
                $DiscountStart = 0;
                $DiscountFinish = 0;
    
	}

	// Определяем следующее действие
	$NextActionName = 'AddLevelPointDiscount';
	// Действие на текстовом поле по клику
	$OnClickText = ' onClick="javascript:this.value = \'\';"';
	// Надпись на кнопке
	$SaveButtonText = 'Добавить интервал амнистии';

        $pLevelPointDiscountId = 0;
}

else
// ================ Редактируем/смотрим существующую точку =================
{


        $pLevelPointDiscountId = $_POST['LevelPointDiscountId'];

	if ($pLevelPointDiscountId <= 0)
	{
		return;
	}

	$sql = "select lpd.levelpointdiscount_id, 
	               lpd.levelpointdiscount_value,  
	               lpd.levelpointdiscount_start,  
	               lpd.levelpointdiscount_finish,
		       lpd.distance_id  
		from LevelPointDiscounts lpd
		where lpd.levelpointdiscount_id = ".$pLevelPointDiscountId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();

		$DistanceId = $_POST['DistanceId'];
                $DiscountValue = $_POST['DiscountValue'];
                $DiscountStart = $_POST['DiscountStart'];
                $DiscountFinish = $_POST['DiscountFinish'];
	}
	else
	{

                $DiscountValue = $Row['levelpointdiscount_value'];
                $DiscountStart = $Row['levelpointdiscount_start'];
                $DiscountFinish = $Row['levelpointdiscount_finish'];
		$DistanceId = $Row['distance_id'];

 	}	

	$NextActionName = 'LevelPointDiscountChange';
	$OnClickText = '';
	$SaveButtonText = 'Сохранить изменения в амнистии';

}
// ================ Конец инициализации переменных для добавляемой/редактируемой точки =================


// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateLevelPointDiscountForm()
	{
		document.LevelPointDiscountForm.action.value = "<? echo $NextActionName; ?>";
		return true;
	}
	// Конец проверки правильности заполнения формы

	// Скрыть интервал
	function HideLevelPointDiscount()
	{
		document.LevelPointDiscountForm.action.value = 'HideLevelPointDiscount';
		document.LevelPointDiscountForm.submit();
	}

	// Править интервал
	function EditLevelPointDiscount(levelpointdiscountid)
	{
		document.LevelPointDiscountForm.LevelPointDiscountId.value = levelpointdiscountid;
		document.LevelPointDiscountForm.action.value = 'LevelPointDiscountInfo';
		document.LevelPointDiscountForm.submit();
	}


	// Функция отмены изменения
	function Cancel()
	{
		document.LevelPointDiscountForm.action.value = "CancelChangeLevelPointDiscount";
		document.LevelPointDiscountForm.submit();
	}
	// 
</script>



<?php

$TabIndex = 0;

// отдельная маленькая форма-список с дистанциями


	print('<form name="DistancesForm" action="'.$MyPHPScript.'" method="post" >'."\n");
	print('<input type="hidden" name="sessionid" value="'.$SessionId.'">'."\n");
	print('<input type="hidden" name="action" value="ViewLevelPointDiscountsPage">'."\n");
	print('<input type="hidden" name="view" value="ViewLevelPointDiscounts">'."\n");
	print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
	print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");
	print('<tr><td class="input">'."\n");
	print('Дистанция: </span>'."\n");
	// Показываем выпадающий список дистанций
	print('<select name="DistanceId" class="leftmargin" tabindex="'.(++$TabIndex).'"  onClick="javascript: submit();">'."\n");
	$sql = "select distance_id, distance_name from Distances where distance_hide = 0  and raid_id = ".$RaidId." order by distance_id ";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		$distanceselected = ($Row['distance_id'] == $DistanceId ? 'selected' : '');
		print('<option value="'.$Row['distance_id'].'" '.$distanceselected.' >'.$Row['distance_name']."</option>\n");
	}
	mysql_free_result($Result);
	print('</select>'."\n");
	print('</td></tr>'."\n\n");
	print('</table>'."\n");
	print('</form>'."\r\n");

        print('</br>'."\r\n");


if ($AllowEdit == 1)
{

	// Выводим начало формы с интервалом
	print('<form name="LevelPointDiscountForm" action="'.$MyPHPScript.'" method="post"  onSubmit="'.$OnSubmitFunction.'">'."\n");
	print('<input type="hidden" name="sessionid" value="'.$SessionId.'">'."\n");
	print('<input type="hidden" name="action" value="">'."\n");
	print('<input type="hidden" name="view" value="ViewLevelPointDiscounts">'."\n");
	print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
	print('<input type="hidden" name="DistanceId" value="'.$DistanceId.'">'."\n");
	//print('<input type="hidden" name="UserId" value="0">'."\n\n");
	//print('<input type="hidden" name="LevelPointId" value="'.$LevelPointId.'">'."\n");
	print('<input type="hidden" name="LevelPointDiscountId" value="'.$pLevelPointDiscountId.'">'."\n\n");
	print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");

	$DisabledText = '';

	print('<tr><td class="input">'."\n");
	
        print(' Амнистия (минуты) <input type="text" name="DiscountValue" size="5" value="'.$DiscountValue.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$DiscountValue.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$DiscountValue.'\';}"').'
                title = "Величина амнистии на интервале КП в минутах">'."\r\n");

        print(' на порядковые номера точек (КП) с <input type="text" name="DiscountStart" size="5" value="'.$DiscountStart.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$DiscountStart.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$DiscountStart.'\';}"').'
                title = "Порядковый номер первого КП в амнистии">'."\r\n");

        print(' по <input type="text" name="DiscountFinish" size="5" value="'.$DiscountFinish.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$DiscountFinish.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$DiscountFinish.'\';}"').'
                title = "Порядковый номер первого КП в амнистии">'."\r\n");

	print('</td></tr>'."\n\n");


	// ================ Submit для формы ==========================================
	print('</tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateLevelPointDiscountForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
//	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	

	// ============ Кнопка удаления точки
	if (($viewmode <> "Add") && ($AllowEdit == 1))
	{
	print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить интервал амнистии? \')) {HideLevelPointDiscount();}" name="HideLevelPointDiscountButton" value="Удалить интервал" tabindex="'.(++$TabIndex).'">'."\n");
	}

	print('</td></tr>'."\n\n");

	print('</table>'."\n");
	print('</form>'."\r\n");


}




print('</br>'."\n");



if (empty($DistanceId))
{
   return;
}

	// Список точек

	$sql = "select lpd.levelpointdiscount_id, 
	               lpd.levelpointdiscount_value,
		       lpd.levelpointdiscount_start,
		       lpd.levelpointdiscount_finish
		from LevelPointDiscounts lpd
		where lpd.levelpointdiscount_hide = 0 and lpd.distance_id = ".$DistanceId."
		order by levelpointdiscount_id";
	
	
	$Result = MySqlQuery($sql);
	
	$tdstyle = 'padding: 5px 0px 2px 5px;';		
        $thstyle = 'padding: 5px 0px 0px 5px;';		


		print('<table border = "1" cellpadding = "0" cellspacing = "0" style = "font-size: 80%">'."\r\n");  

		print('<tr class = "gray">
		         <td width = "150" style = "'.$thstyle.'">Амнистия (минуты)</td>
		         <td width = "150" style = "'.$thstyle.'">Порядковый номер с</td>
		         <td width = "150" style = "'.$thstyle.'">по</td>'."\r\n");

		if ($AllowEdit == 1)
		{
		       print('<td width = "100" style = "'.$thstyle.'">&nbsp;</td>'."\r\n");

		}
		
		print('</tr>'."\r\n");
		
	        // Сканируем команды
		while ($Row = mysql_fetch_assoc($Result))
		{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
                     print('<tr>'."\r\n");
		     print('<td align = "left" style = "'.$tdstyle.'">'.$Row['levelpointdiscount_value'].'</td>
		             <td align = "left" style = "'.$tdstyle.'">'.$Row['levelpointdiscount_start'].'</td>
		             <td align = "left" style = "'.$tdstyle.'">'.$Row['levelpointdiscount_finish'].'</td>');

  		     if ($AllowEdit == 1)
		     {
			     print('<td align = "left" style = "'.$tdstyle.'">');
			     print('&nbsp; <input type="button" onClick="javascript: EditLevelPointDiscount('.$Row['levelpointdiscount_id'].');" name="EditLevelPointDiscountButton" value="Править" tabindex="'.(++$TabIndex).'">'."\n");
			     print('</td>'."\r\n");
		     }		      
                                
		}	

		mysql_free_result($Result);
		print('</table>'."\r\n");
	

?>

