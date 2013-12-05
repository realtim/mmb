<?php
// +++++++++++ Загрузка файла/порказ списка файлов ++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";

// ================ Добавляем новый файл ===================================
if ($viewmode == 'Add')
{

	// Если запрещено создавать ММБ - молча выходим, сообщение уже выведено в teamaction.php
	if (!$Administrator) return;

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();

		//$LevelPointId = $_POST['LevelPointId'];
		$FileTypeId = $_POST['FileTypeId'];
                $RaidFileComment = $_POST['RaidFileComment'];
                

	}
	else
	// Пробуем создать команду первый раз
	{

		//$LevelPointId = 0;
		$FileTypeId = 0;
	        $RaidFileComment = 'Описание файла';
	    
	}

	// Определяем следующее действие
	$NextActionName = 'AddRaidFile';
	// Действие на текстовом поле по клику
	$OnClickText = ' onClick="javascript:this.value = \'\';"';
	// Надпись на кнопке
	$SaveButtonText = 'Загрузить файл';

        $pRaidFileId = 0;
	$RaidFileName = '';

}

else
// ================ Редактируем/смотрим существующий файл =================
{


        $pRaidFileId = $_POST['RaidFileId'];

	if ($pRaidFileId <= 0)
	{
		return;
	}

	$sql = "select rf.raidfile_id, ft.filetype_id, 
	               rf.raidfile_comment, rf.raidfile_name
		from RaidFiles rf
		     inner join FileTypes ft
		     on rf.filetype_id = ft.filetype_id
		where rf.raidfile_id = ".$pRaidFileId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result);

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$FileTypeId = $_POST['FileTypeId'];
//		$LevelPointId = $_POST['LevelPointId'];
                $RaidFileComment = $_POST['RaidFileComment'];
		$RaidFileName = '';
	}
	else
	{

		$FileTypeId = $Row['filetype_id'];
		//$LevelPointId = $Row['levelpoint_id'];
		$RaidFileComment = $Row['raidfile_comment'];
		$RaidFileName = $Row['raidfile_name'];
	}

	$NextActionName = 'RaidFileChange';
	$AllowEdit = 0;
	$OnClickText = '';
	$SaveButtonText = 'Сохранить изменения файла';

}
// ================ Конец инициализации переменных для загружаемого/загруженного файла =================

// Определяем права по редактированию 
if ($Administrator)
{
	$AllowEdit = 1;
	$DisabledText = '';
	$OnSubmitFunction = 'return ValidateRaidFileForm();';
}
else
{
	$AllowEdit = 0;
	$DisabledText = ' disabled';
	$OnSubmitFunction = 'return false;';
}
// Определяем права по просмотру
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
	function ValidateRaidFileForm()
	{
		document.RaidFileForm.action.value = "<? echo $NextActionName; ?>";
		return true;
	}
	// Конец проверки правильности заполнения формы

	// Удалить файл
	function HideFile()
	{
		document.RaidFileForm.action.value = 'HideFile';
		document.RaidFileForm.submit();
	}

	// Править файл
	function EditFile(raidfileid)
	{
		document.RaidFileForm.RaidFileId.value = raidfileid;
		document.RaidFileForm.action.value = 'RaidFileInfo';
		document.RaidFileForm.submit();
	}


	// Функция отмены изменения
	function Cancel()
	{
		document.RaidFileForm.action.value = "CancelChangeRaidFile";
		document.RaidFileForm.submit();
	}
	// 
</script>

<?php
// Выводим начало формы с файлом
print('<form name="RaidFileForm" action="'.$MyPHPScript.'" method="post" enctype="multipart/form-data" onSubmit="'.$OnSubmitFunction.'">'."\n");
print('<input type="hidden" name="sessionid" value="'.$SessionId.'">'."\n");
print('<input type="hidden" name="action" value="">'."\n");
print('<input type="hidden" name="view" value="ViewRaidFiles">'."\n");
print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
//print('<input type="hidden" name="UserId" value="0">'."\n\n");
//print('<input type="hidden" name="LevelPointId" value="'.$LevelPointId.'">'."\n");
print('<input type="hidden" name="RaidFileId" value="'.$pRaidFileId.'">'."\n\n");
print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");

$TabIndex = 0;
$DisabledText = '';

print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");


if ($viewmode == "Add") 
{	
	// ============ загрузка файла
	print('<tr><td class = "input">Новый файл для загрузки: <input name="raidfile" type="file" /></td></tr>'."\r\n");
} else {
	print('<tr><td class = "input"><b>'.$RaidFileName.'</b></td></tr>'."\r\n");

}


print('<tr><td class="input">'."\n");

	print('Тип файла</span>'."\n");
	// Показываем выпадающий список файлов
	print('<select name="FileTypeId" class="leftmargin" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
	$sql = "select filetype_id, filetype_name from FileTypes ";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		$filetypeselected = ($Row['filetype_id'] == $FileTypeId ? 'selected' : '');
		print('<option value="'.$Row['filetype_id'].'" '.$filetypeselected.' >'.$Row['filetype_name']."</option>\n");
	}
	mysql_free_result($Result);
	print('</select>'."\n");

        print('<tr><td class = "input"><input type="text" name="RaidFileComment" size="50" value="'.$RaidFileComment.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$RaidFileComment.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$RaidFileComment.'\';}"').'
                title = "Описание файла"></td></tr>'."\r\n");

/*
	print(' <span style="margin-left: 30px;"> &nbsp; Точка дистанции</span>'."\n");
	// Показываем выпадающий список точек
	print('<select name="LevelPointId" class="leftmargin" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
	$sql = "select lp.levelpoint_id, lp.levelpoint_name 
	        from LevelPoints lp 
		     inner join Levels l on lp.level_id = l.level_id 
		     inner join Distances d on l.distance_id = d.distance_id
		where  d.raid_id = ".$RaidId."
		order by  d.distance_id, lp.levelpoint_order";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		$levelpointselected = ($Row['levelpoint_id'] == $LevelPointId ? 'selected' : '');
		print('<option value="'.$Row['levelpoint_id'].'" '.$levelpointselected.' >'.$Row['levelpoint_name']."</option>\n");
	}
	mysql_free_result($Result);
	print('</select>'."\n");
*/


print('</td></tr>'."\n\n");


// ================ Submit для формы ==========================================
if ($AllowEdit == 1)
{
	print('<tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateRaidFileForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
//	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	

	// ============ Кнопка удаления файла
	if (($viewmode <> "Add") && ($AllowEdit == 1))
	{
	print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить файл: '.trim($RaidFileName).'? \')) {HideFile();}" name="HideFileButton" value="Удалить файл" tabindex="'.(++$TabIndex).'">'."\n");
	}

	print('</td></tr>'."\n\n");

}


print('</table>'."\n");


print('</br>'."\n");

	// Список уже загруженных файлов
	
	$sql = "select rf.raidfile_id, rf.raidfile_name, ft.filetype_id,
	               rf.raidfile_comment, ft.filetype_name
		from RaidFiles rf
		     inner join FileTypes ft
		     on rf.filetype_id = ft.filetype_id
		where rf.raid_id = ".$RaidId."
		      and rf.raidfile_hide = 0 ";
	$Result = MySqlQuery($sql);
	
	
	
	$tdstyle = 'padding: 5px 0px 2px 5px;';		
        $thstyle = 'padding: 5px 0px 0px 5px;';		


		print('<table border = "1" cellpadding = "0" cellspacing = "0" style = "font-size: 80%">'."\r\n");  

		print('<tr class = "gray">
		         <td width = "300" style = "'.$thstyle.'">Файл</td>
		         <td width = "100" style = "'.$thstyle.'">Тип</td>
		         <td width = "300" style = "'.$thstyle.'">Описание</td>
		         <td width = "100" style = "'.$thstyle.'">&nbsp;</td>
			 </tr>'."\r\n");
		
	        // Сканируем команды
		while ($Row = mysql_fetch_assoc($Result))
		{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
                     print('<tr>'."\r\n");
		     print('<td align = "left" style = "'.$tdstyle.'"><a target = "_blank" href = "'. trim($MyStoreHttpLink).trim($Row['raidfile_name']).'">'.$Row['raidfile_name'].'</a></td><td align = "left" style = "'.$tdstyle.'">'.$Row['filetype_name'].'</td><td align = "left" style = "'.$tdstyle.'">'.($Row['raidfile_comment'] == '' ? '&nbsp;' : $Row['raidfile_comment']).'</td><td align = "left" style = "'.$tdstyle.'">');
		     print('&nbsp; <input type="button" onClick="javascript: EditFile('.$Row['raidfile_id'].');" name="EditFileButton" value="Править" tabindex="'.(++$TabIndex).'">'."\n");
                     print('</td>'."\r\n");


                     //print('<td align = "center" style = "'.$tdstyle.'">'.$Row['unionstatus']."\r\n");
                                
		}	

		mysql_free_result($Result);
		print('</table>'."\r\n");
		
	print('</form>'."\r\n");

?>
