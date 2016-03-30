<?php
// +++++++++++ Показ/редактирование данных пользователя +++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// Выходим, если не администратор и не модератор
 if (!$Administrator and !$Moderator)
  {
    CMmb::setShortResult('Нет прав на экспорт', '');
    return;
   }
         
         
 
// Выводим javascrpit
?>

<!-- Выводим javascrpit -->
<script language = "JavaScript">


	function RecalcRaidResults()
	{ 
              document.AdminForm.action.value = "RecalcRaidResults";
              document.AdminForm.RaidId.value = document.FindTeamForm.RaidId.value; 
	      document.AdminForm.submit();
              return true;
	}

	function FindRaidErrors()
	{
              document.AdminForm.action.value = "FindRaidErrors";
              document.AdminForm.RaidId.value = document.FindTeamForm.RaidId.value;
	      document.AdminForm.submit();
              return true;
	}

	function ClearTables()
	{ 
		document.AdminForm.action.value = "ClearTables";
		document.AdminForm.submit();
	}

	function LoadRaidDataFile()
	{ 
		document.AdminForm.action.value = "LoadRaidDataFile";
		document.AdminForm.submit();
	}

	function JSON()
	{ 
		document.AdminForm.action.value = "JSON";
                document.AdminForm.RaidId.value = document.FindTeamForm.RaidId.value;
		document.AdminForm.submit();
	}


	function RecalcRaidRank()
	{ 
              document.AdminForm.action.value = "RecalcRaidRank";
              document.AdminForm.RaidId.value = document.FindTeamForm.RaidId.value; 
	      document.AdminForm.submit();
              return true;
	}

	function RecalcAllRaidsRank()
	{ 
              document.AdminForm.action.value = "RecalcAllRaidsRank";
              document.AdminForm.RaidId.value = 0; 
	      document.AdminForm.submit();
              return true;
	}


	// Функция отправки сообщения
	function SendMessageForAll()
	{ 
		document.SendMessageForAllForm.action.value = "SendMessageForAll";
		document.SendMessageForAllForm.submit();
              return true;
	}


</script>
<!-- Конец вывода javascrpit -->

<?php

		$TabIndex = 1;

         // выводим форму с данными пользователя
	 
	 print('<form  name = "AdminForm" enctype="multipart/form-data"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
         print('<input type = "hidden" name = "RaidId" value = "'.$RaidId.'">'."\r\n");
         print('<input type = "hidden" name = "action" value = "">'."\r\n");
         print('<input type="hidden" name="MAX_FILE_SIZE" value="1000000" />'."\r\n");
	 
         print('<table  border = "0" cellpadding = "0" cellspacing = "0" width = "100%">'."\r\n");


          print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><a href = "?action=PrintRaidTeams&RaidId='.$RaidId.'" target = "_blank">Список для печати</a></td></tr>'."\r\n");


          //print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><a href = "?action=JSON&sessionid='.$SessionId.'" target = "_blank">JSON dump</a></td></tr>'."\r\n");

	  print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><input type="button" style = "width:185px;" name="JSONdump" value="Получить дамп"
                          onclick = "javascript: JSON();"
                          tabindex = "'.(++$TabIndex).'"></td></tr>'."\r\n");



	  print('<tr><td style = "padding-top: 10px; padding-bottom: 10px;">Файл с данными:<br/><input  type="file" name="android" /> &nbsp;
                 <input type="button"  style = "width:185px;" name = "LoadRaidDataFileButton"  value="Загрузить"  onclick = "javascript: LoadRaidDataFile(); "  tabindex = "'.(++$TabIndex).'"  /></td></tr>'."\r\n");


	  print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><input type="button" style = "width:185px;" name="RecalcRaidResultsButton" value="Пересчитать результаты"
                          onclick = "javascript: RecalcRaidResults();"
                          tabindex = "'.(++$TabIndex).'"></td></tr>'."\r\n");


	  print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><input type="button" style = "width:185px;" name="FindRaidErrorsButton" value="Найти ошибки"
                          onclick = "javascript: FindRaidErrors();"
                          tabindex = "'.(++$TabIndex).'"></td></tr>'."\r\n");

	 print('</table></form>'."\r\n"); 
	 // Конец вывода формы с данными пользователя



 	print('<div style = "margin-top: 30px; margin-bottom: 10px; text-align: left">Рассылка для всех участников!</div>'."\r\n");
		print('<form  name = "SendMessageForAllForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
		print('<input type = "hidden" name = "action" value = "">'."\r\n");
	        print('<input type = "hidden" name = "RaidId" value = "'.$RaidId.'">'."\r\n");

		print("<br/>\r\n");

                 $DisabledText = '';
                $NewMessageSubject = 'Тема рассылки';
                $NewMessageText =  'Текст сообщения';
	//	print('<div align = "left" style = "padding-top: 5px;">'."\r\n");

		// Показываем выпадающий список типов ссылок
		print('<select name="SendForAllTypeId" tabindex="'.(++$TabIndex).'">'."\n");
			print('<option value="1" selected>Обычная (всем участникам выбранного ММБ, с учетом флага)</option>'."\n");
			print('<option value="2">Экстренная (всем участникам выбранного ММБ)</option>'."\n");
			print('<option value="3">Пользователям (всем пользователям сайта, с учетом флага)</option>'."\n");
		print('</select>'."\n");

		print('<div style = "margin-top: 30px; margin-bottom: 10px; text-align: left">'."\r\n");
		print('<input type="text" name="MessageSubject" size="40" value="'.$NewMessageSubject.'" tabindex = "'.(++$TabIndex).'"  '.$DisabledText.' '
		. CMmbUI::placeholder($NewMessageSubject) . ' title = "Тема рассылки">'."\r\n");
		print('</div>'."\r\n");

	//	print("</div>\r\n");

		print('<div class="team_res"><textarea name="MessageText"  rows="4" cols="50" tabindex = "'.(++$TabIndex).'"  '.$DisabledText.' '
		. CMmbUI::placeholder($NewMessageText) .' title = "Текст сообщения">'.$NewMessageText.'</textarea></div>'."\r\n");
    	        print("<br/>\r\n");
    	        print('<input type="button" onClick = "javascript: SendMessageForAll();;"  name="SendMessageForAllButton" value="Отправить" tabindex = "'.(++$TabIndex).'">'."\r\n");

	        print('</form>'."\r\n");



?>



