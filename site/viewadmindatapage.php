<?php
// +++++++++++ Показ/редактирование данных пользователя +++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// Выходим, если не администратор и не модератор
 if (!$Administrator and !$Moderator)
  {
    $statustext = 'Нет прав на экспорт';				     
    $view = "";
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


</script>
<!-- Конец вывода javascrpit -->

<?php

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
                          tabindex = "101"></td></tr>'."\r\n");



	  print('<tr><td style = "padding-top: 10px; padding-bottom: 10px;">Файл с данными:<br/><input  type="file" name="android" /> &nbsp;
                 <input type="button"  style = "width:185px;" name = "LoadRaidDataFileButton"  value="Загрузить"  onclick = "javascript: LoadRaidDataFile(); "  tabindex = "102"  /></td></tr>'."\r\n");


	  print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><input type="button" style = "width:185px;" name="RecalcRaidResultsButton" value="Пересчитать результаты"
                          onclick = "javascript: RecalcRaidResults();"
                          tabindex = "103"></td></tr>'."\r\n");


	  print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><input type="button" style = "width:185px;" name="FindRaidErrorsButton" value="Найти ошибки"
                          onclick = "javascript: FindRaidErrors();"
                          tabindex = "104"></td></tr>'."\r\n");

/*
Теперь рейтинг считается вместе с результатами
	  print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><input type="button" style = "width:185px;" name="RecalcRaidRankButton" value="Пересчитать рейтинг"
                          onclick = "javascript: RecalcRaidRank();"
                          tabindex = "105"></td></tr>'."\r\n");
*/

/*
	  print('<tr><td style = "padding-top: 5px; padding-bottom: 5px;"><input type="button" style = "width:270px;" name="RecalcAllRaidsResultsButton" value="Пересчитать рейтинг по всем ММБ"
                          onclick = "javascript: RecalcAllRaidsRank();"
                          tabindex = "106"></td></tr>'."\r\n");
*/
       //  показываем кнопку "Очистить таблицы" 
	  // print('<input type="button" style = "width:185px; margin-top:10px;" name="ClearTablesButton" value="Очистить таблицы"
          //                onclick = "ClearTables();"
          //                tabindex = "402">'."\r\n");

         
         // Конец вывода кнопок

	 print('</table></form>'."\r\n"); 
	 // Конец вывода формы с данными пользователя
/* 
	 print('</br>'."\r\n"); 
 	print('<form name = "LoadFileForm"  enctype="multipart/form-data" action="'.$MyPHPScript.'" method="POST">');
        print('<input type = "hidden" name = "RaidId" value = "'.$RaidId.'">'."\r\n");
        print('<input type = "hidden" name = "action" value = "LoadRaidDataFile">'."\r\n");
	print('<input type="hidden" name="MAX_FILE_SIZE" value="1000000" />');
	print('Файл с данными: <input name="android" type="file" /> &nbsp;');
	print('<input type="submit" value="Загрузить" />');
	print('</form>');
*/

?>



