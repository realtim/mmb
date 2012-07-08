<?php
// +++++++++++ Показ/редактирование данных пользователя +++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

// Выходим, если не администратор 
if (!($Administrator)) return;

         

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
		document.AdminServiceForm.action.value = "ClearTables";
		document.AdminServiceForm.submit();
	}

</script>
<!-- Конец вывода javascrpit -->

<?php

         // выводим форму с данными пользователя
	 
	 print('<form  name = "AdminForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
         print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\r\n");
         print('<input type = "hidden" name = "RaidId" value = "'.$RaidId.'">'."\r\n");
         print('<input type = "hidden" name = "action" value = "">'."\r\n");

	 
         print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0" width = "300">'."\r\n");

	  print('<tr><td class = "input"><input type="button" style = "width:185px;" name="RecalcRaidResultsButton" value="Пересчитать результаты"
                          onclick = "javascript: RecalcRaidResults();"
                          tabindex = "101"></td></tr>'."\r\n");


	  print('<tr><td class = "input"><input type="button" style = "width:185px;" name="FindRaidErrorsButton" value="Найти ошибки"
                          onclick = "javascript: FindRaidErrors();"
                          tabindex = "102"></td></tr>'."\r\n");


       //  показываем кнопку "Очистить таблицы" 
	  // print('<input type="button" style = "width:185px; margin-top:10px;" name="ClearTablesButton" value="Очистить таблицы"
          //                onclick = "ClearTables();"
          //                tabindex = "402">'."\r\n");

         
         // Конец вывода кнопок

	 print('</table></form>'."\r\n"); 
	 // Конец вывода формы с данными пользователя
 

?>



