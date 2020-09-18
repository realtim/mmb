<?php
// +++++++++++ Загрузка файла/порказ списка файлов ++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";



print("<br/>\n");

	// Список уже загруженных файлов
	
	$sql = "select rf.raid_id, rf.raidfile_id, rf.raidfile_name
		from RaidFiles rf
		where rf.filetype_id = 3
		      and rf.raidfile_hide = 0 
		order by raid_id DESC";
	$Result = MySqlQuery($sql);
	

	print("<table class=\"std\">\r\n");

        // Сканируем
	while ($Row = mysqli_fetch_assoc($Result))
	{
             print("<tr>\r\n");
	     print('<td>');
	     print('&nbsp;<img src="'.trim($MyStoreHttpLink).trim($Row['raidfile_name']).'" height="512" width="512">'."\n");
	     print("</td>\r\n");
             print("</tr>\r\n");
	}

	mysqli_free_result($Result);
	print("</table>\r\n");
?>
