<?php
// +++++++++++ Добавление/правка/показ точек сканирвания ++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";


// Определяем права по редактированию 
if ($Administrator || $Moderator)
{
	$AllowEdit = 1;
	$DisabledText = '';
	$OnSubmitFunction = 'return ValidateScanPointForm();';
}
else
{
	$AllowEdit = 0;
	$DisabledText = ' disabled';
	$OnSubmitFunction = 'return false;';
}
// Определяем права по просмотру 
if ($Administrator || $Moderator )
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
                $ScanPointName = $_POST['ScanPointName'];
	}
	else
	// Инициализация перемнных для новой точки 
	{
                $ScanPointName = 'Название точки сканирования';
	}

	// Определяем следующее действие
	$NextActionName = 'AddScanPoint';
	// Действие на текстовом поле по клику
	$OnClickText = ' onClick="javascript:this.value = \'\';"';
	// Надпись на кнопке
	$SaveButtonText = 'Добавить точку сканирования';

        $pScanPointId = 0;
}

else
// ================ Редактируем/смотрим существующую точкусканирования  =================
{
        $pScanPointId = mmb_validateInt($_POST, 'ScanPointId');

	if ($pScanPointId <= 0)
	{
		return;
	}

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
                $ScanPointName = $_POST['ScanPointName'];

	}
	else
	{
		$sql = "select sp.scanpoint_id,
	               sp.scanpoint_name,
		       sp.scanpoint_order
		from ScanPoints sp
		where sp.scanpoint_id = $pScanPointId";

                $ScanPointName = CSql::singleValue($sql, 'scanpoint_name');
	}

	$NextActionName = 'ScanPointChange';
	$OnClickText = '';
	$SaveButtonText = 'Сохранить изменения точки сканирования';

}
// ================ Конец инициализации переменных для добавляемой/редактируемой точки сканирования =================


// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateScanPointForm()
	{
		document.ScanPointForm.action.value = "<? echo $NextActionName; ?>";
		return true;
	}
	// Конец проверки правильности заполнения формы

	// Скрыть точку
	function HideScanPoint()
	{
		document.ScanPointForm.action.value = 'HideScanPoint';
		document.ScanPointForm.submit();
	}

	// Поднять точку (уменьшить порядковый) номер
	function ScanPointUp()
	{
		document.ScanPointForm.action.value = 'ScanPointOrderDown';
		document.ScanPointForm.submit();
	}

	// Опустить точку (увеличить порядковый номер)
	function ScanPointDown()
	{
		document.ScanPointForm.action.value = 'ScanPointOrderUp';
		document.ScanPointForm.submit();
	}

	// Править точку сканирования
	function EditScanPoint(scanpointid)
	{
		document.ScanPointForm.ScanPointId.value = scanpointid;
		document.ScanPointForm.action.value = 'ScanPointInfo';
		document.ScanPointForm.submit();
	}

	// Функция отмены изменения
	function Cancel()
	{
		document.ScanPointForm.action.value = "CancelChangeScanPoint";
		document.ScanPointForm.submit();
	}
	// 
</script>



<?php

$TabIndex = 0;



if ($AllowEdit == 1)
{

	// Выводим начало формы с точкой
	print('<form name="ScanPointForm" action="'.$MyPHPScript.'" method="post"  onSubmit="'.$OnSubmitFunction.'">'."\n");
	print('<input type="hidden" name="action" value="">'."\n");
	print('<input type="hidden" name="view" value="ViewLevelPoints">'."\n");
	print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
	print('<input type="hidden" name="ScanPointId" value="'.$pScanPointId.'">'."\n\n");
	print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");

	$DisabledText = '';

	print('<tr><td class="input">'."\n");
        print('<input type="text" name="ScanPointName" size="40" value="'.$ScanPointName.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($ScanPointName))
                 .'title = "Название точки сканирования">'."\r\n");
	print('</td></tr>'."\n\n");

	// ================ Submit для формы ==========================================
	print('<tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateScanPointForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
	

	// ============ Кнопка удаления точки
	if (($viewmode <> "Add") && ($AllowEdit == 1))
	{
	print('&nbsp; <input type="button" style="margin-left: 20px;" onClick="javascript: {ScanPointUp();}" name="ScanPointUpButton" value="Поднять скан-точку" tabindex="'.(++$TabIndex).'">'."\n");
	print('&nbsp; <input type="button" style="margin-left: 20px;" onClick="javascript: {ScanPointDown();}" name="ScanPointDownButton" value="Опустить скан-точку" tabindex="'.(++$TabIndex).'">'."\n");
	print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить скан-точку: '.trim($ScanPointName).'? \')) {HideScanPoint();}" name="HideLevelPointButton" value="Удалить точку" tabindex="'.(++$TabIndex).'">'."\n");
	}
	print("</td></tr>\r\n");
	print("</table>\n");
	print("</form>\r\n");
 

}

print("<br/>\n");


if ($AllowViewResults == 1)
{
	// Список точек сканирования

	$sql = "select sp.scanpoint_id,
	               sp.scanpoint_name, 
		       sp.scanpoint_order
		from ScanPoints sp
		where sp.scanpoint_hide = 0 and sp.raid_id = $RaidId
		order by scanpoint_order";
	
	
	$Result = MySqlQuery($sql);
	

		print("<table class=\"std\">\r\n");
		print('<tr class="head gray">
		         <td width="50">N п/п</td>
		         <td width="200">Название</td>'."\r\n");

		if ($AllowEdit == 1)
		{
		       print('<td width="100">&nbsp;</td>'."\r\n");
		}
		
		print("</tr>\r\n");
		
	        // Сканируем команды
		while ($Row = mysql_fetch_assoc($Result))
		{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
                     print("<tr>\r\n");
		     print("<td>{$Row['scanpoint_order']}</td>
		             <td>{$Row['scanpoint_name']}</td>");

  		     if ($AllowEdit == 1)
		     {
			     print('<td>');
			     print('&nbsp; <input type="button" onClick="javascript: EditScanPoint('.$Row['scanpoint_id'].');" name="EditScanPointButton" value="Править" tabindex="'.(++$TabIndex).'">'."\n");
			     print("</td>\r\n");
		     }

		     print("</tr>\r\n");
		}

		mysql_free_result($Result);
		print("</table>\r\n");
	

               print("<br/>\n");
   }
   // Конец проверки прав на просмотр точек
?>

