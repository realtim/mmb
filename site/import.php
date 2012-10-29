<?php

// Проверка, как именно используем скрипт: из интерфейса или отдельно
if (isset($MyPHPScript) and $action == 'LoadRaidDataFile')
{
  if (!$Administrator && !$Moderator) return;

  $ConnectionId = mysql_connect($ServerName, $WebUserName, $WebUserPassword);
  if ($ConnectionId <= 0) die(mysql_error());
  // Устанавливаем временную зону
  mysql_query('set time_zone = \'+4:00\'', $ConnectionId);
  // Устанавливаем кодировку для взаимодействия
  mysql_query('set names \'utf8\'', $ConnectionId);
  // Выбираем БД ММБ
  if (mysql_select_db($DBName, $ConnectionId) == "") die(mysql_error());

}
else 
{


  // Общие настройки
  include("settings.php");
  // Библиотека функций
  include("functions.php");
  // Устанавливаем часовой пояс по умолчанию
  date_default_timezone_set("Europe/Moscow");
  // Подключаемся к базе
  $ConnectionId = mysql_connect($ServerName, $WebUserName, $WebUserPassword);
  if ($ConnectionId <= 0) die(mysql_error());
  // Устанавливаем временную зону
  mysql_query('set time_zone = \'+4:00\'', $ConnectionId);
  // Устанавливаем кодировку для взаимодействия
  mysql_query('set names \'utf8\'', $ConnectionId);
  // Выбираем БД ММБ
  if (mysql_select_db($DBName, $ConnectionId) == "") die(mysql_error());

  print('<html>');
  print('<head>');
  print('<title>Импорт данных с Android</title>');
  print('<link rel="Stylesheet" type="text/css"  href="styles/mmb.css" />');
  print('<meta http-equiv="Content-Type" content="text/html; charset=utf-8">');
  print('</head>');
  print('<body>');

  print('<form enctype="multipart/form-data" action="import.php" method="POST">');
  print('<input type="hidden" name="MAX_FILE_SIZE" value="1000000" />');
  print('Файл с данными: <input name="android" type="file" /> &nbsp;');
  print('<input type="submit" value="Загрузить" />');
  print('</form>');

}
// Конец проверки, как именно используем скрипт: из интерфейса или отдельно


// Обработка загруженного файла
if (isset($_FILES['android']))
{
	if ($_FILES["android"]["error"] > 0) die("Ошибка загрузки: ".$_FILES["android"]["error"]);
	echo "<br />Загружено ".$_FILES["android"]["size"] . " байт<br />";
	$lines = file($_FILES['android']['tmp_name'], FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);

	// ====== В первый цикл просто проверяем данные, ничего не записывая в базу
	$type = "";
	$password = "";
	foreach ($lines as $line_num => $line)
	{
		// Проверка существования пользователя, от имени которого загружаем
		if ($line_num == 0)
		{
			$sql = "select user_password from Users where user_id = ".mysql_real_escape_string($line);
			$Result = mysql_query($sql);
			if (!$Result) die("Автор файла данных отсутствует в базе");
			$Row = mysql_fetch_assoc($Result);
			if (isset($Row['user_password'])) $password = $Row['user_password'];
			mysql_free_result($Result);
			continue;
		}
		// Проверка пароля пользователя, от имени которого загружаем
		if ($line_num == 1)
		{
			if (!$password || ($line <> $password)) die("Пароль автора файла данных неправильный");
			continue;
		}
		// Смена типа данных
		if ($line == "---TeamLevelPoints")
		{
			$type = "TeamLevelPoints";
			continue;
		}
		else if ($line == "---TeamLevelDismiss")
		{
			$type = "TeamLevelDismiss";
			continue;
		}
		else if ($line == "end")
		{
			$type = "end";
			continue;
		}

		// Если оказались здесь - обрабатываем строчку с данными
		if (!$type) die("Не указан тип данных");
		if ($type == "end") die("Данные после строки 'end'");

		// Единая проверка валидности оператора, который ввел данные,
		// его планшета и контрольной точки, где происходил ввод
		if (($type == "TeamLevelDismiss") || ($type == "TeamLevelPoints"))
		{
			// Получаем переменные
			$values = explode(';', $line);
			if (count($values) < 6) die("Некорректное число параметров в строке #".$line_num." - ".$line);
			foreach ($values as &$value) $value = trim($value, '"');
			// Проверяем наличие в базе оператора данных
			$sql = "select user_id from Users where user_id = ".mysql_real_escape_string($values[0]);
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1) die("Несуществующий автор данных в строке #".$line_num." - ".$line);
			mysql_free_result($Result);
			// Проверяем наличие в базе устройства для ввода данных
			if ($type == "TeamLevelPoints") $device_id = $values[4]; else $device_id = $values[5];
			$sql = "select device_id from Devices where device_id = ".mysql_real_escape_string($device_id);
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1) die("Несуществующее устройство ввода данных в строке #".$line_num." - ".$line);
			mysql_free_result($Result);
			// Проверяем наличие активной точки в базе
			$sql = "select level_id, pointtype_id from LevelPoints where levelpoint_id = ".mysql_real_escape_string($values[1]);
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1) die("Несуществующая активная точка в строке #".$line_num." - ".$line);
			$Row = mysql_fetch_assoc($Result);
			$level_id = $Row['level_id'];
			$pointtype_id = $Row['pointtype_id'];
			if (($pointtype_id <> 1) && ($pointtype_id <> 2)) die("Неподдерживаемый тип активной точка в строке #".$line_num." - ".$line);
			mysql_free_result($Result);
			// Проверяем наличие команды в базе
			$sql = "select distance_id from Teams where team_id = ".mysql_real_escape_string($values[2]);
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1) die("Несуществующая команда в строке #".$line_num." - ".$line);
			$Row = mysql_fetch_assoc($Result);
			$distance_id = $Row['distance_id'];
			mysql_free_result($Result);
			// Проверяем, что команда с этой дистанции могла оказаться на этой точке
			$sql = "select level_begtime, level_maxbegtime, level_endtime, level_minendtime from Levels where level_id = ".$level_id." and distance_id = ".$distance_id;
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1) die("Несуществующая команда в строке #".$line_num." - ".$line);
			$Row = mysql_fetch_assoc($Result);
			if ($pointtype_id == 1)
			{
				$begtime = $Row['level_begtime'];
				$endtime = $Row['level_maxbegtime'];
			}
			elseif ($pointtype_id == 2)
			{
				$begtime = $Row['level_minendtime'];
				$endtime = $Row['level_endtime'];
			}
			mysql_free_result($Result);
		}
		else die("Неизвестный тип данных '".$type."'");

		// Данные о сходе участников на контрольной точке
		if ($type == "TeamLevelDismiss")
		{
			// Проверяем точное число параметров в строке
			if (count($values) <> 6) die("Некорректное число параметров в строке #".$line_num." - ".$line);
			// Проверяем, является ли сошедший участник членом команды
			$sql = "select teamuser_id from TeamUsers where team_id = ".mysql_real_escape_string($values[2])." and user_id = ".mysql_real_escape_string($values[3]);
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1) die("Сошедшего участника нет в его команде в строке #".$line_num." - ".$line);
			mysql_free_result($Result);
		}

		// Данные о командах на контрольной точке
		if ($type == "TeamLevelPoints")
		{
			// Проверяем точное число параметров в строке
			if (count($values) <> 8) die("Некорректное число параметров в строке #".$line_num." - ".$line);
			// Проверяем, что команда отметилась на контрольной точке в то время, когда точка была открыта
			$sql = "select * from Levels WHERE UNIX_TIMESTAMP('".$values[5]."') >= UNIX_TIMESTAMP('".$begtime."') and UNIX_TIMESTAMP('".$values[5]."') <= UNIX_TIMESTAMP('".$endtime."') LIMIT 1";
			$Result = mysql_query($sql);
			if (!$Result || !mysql_num_rows($Result)) die("Время прихода на контрольную точку не совпадает со временем ее работы в строке #".$line_num." - ".$line);
			mysql_free_result($Result);
			// Проверяем, что время регистрации результата >= времени прихода команды
			$sql = "select * from Levels WHERE UNIX_TIMESTAMP('".$values[3]."') >= UNIX_TIMESTAMP('".$values[5]."') LIMIT 1";
			$Result = mysql_query($sql);
			if (!$Result || !mysql_num_rows($Result)) die("Время регистрации результата меньше времени регистрируемого результата в строке #".$line_num." - ".$line);
			mysql_free_result($Result);
			// Получаем список полный КП этапа, на котором зарегистрирован результат
			$sql = "select level_pointnames from Levels where level_id = ".$level_id;
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1) die("Ошибка получения полного списка КП этапа в строке #".$line_num." - ".$line);
			$Row = mysql_fetch_assoc($Result);
			$full_points = explode(',', $Row['level_pointnames']);
			mysql_free_result($Result);
			// Сначала считаем, что все КП невзятые
			$bit_points = array();
			foreach ($full_points as $n => $val) $bit_points[$n] = 0;
			// Берем из результатов список взятых КП
			if ($values[6])	$visited_points = explode(',', $values[6]);
			else $visited_points = array();
			if (($pointtype_id <> 2) && count($visited_points)) die("взятые КП отмечены не на финише этапа в строке #".$line_num." - ".$line);
			// Помечаем их взятыми в битовом массиве
			foreach ($visited_points as $point)
			{
				$index = array_search($point, $full_points, true);
				if ($index === false) die("Несуществующее на этапе КП '".$point."' в строке #".$line_num." - ".$line);
				$bit_points[$index] = 1;
			}
			// Запоминаем сгеренированную строку для последующего сохранения в базе
			$teamlevelpoint_points[$line_num] = implode(",", $bit_points);
			if ($pointtype_id <> 2) $teamlevelpoint_points[$line_num] = "NULL";
		}
	}
	// Проверяем, что в конце файла был end
	if ($line <> "end") die("В конце файла отсутствует 'end'");

	// ====== Если добрались сюда - все данные корректные, можно сохранять в TeamLevelPoints
	echo "Проверка данных завершилась успешно<br \>";
	flush();

	// Повторно сканируем файл и берем из него данные с минимумом проверок
	$n_new = $n_updated = $n_unchanged = 0;
	$d_new = $d_updated = $d_unchanged = 0;
	$d_dismiss = $d_delete = 0;
	foreach ($lines as $line_num => $line)
	{
		// Логин и пароль уже не проверяем
		if (($line_num == 0) || ($line_num == 1)) continue;
		// Смена типа данных (но пока все равно поддерживается только TeamLevelPoints)
		if ($line == "---TeamLevelPoints")
		{
			$type = "TeamLevelPoints";
			continue;
		}
		else if ($line == "---TeamLevelDismiss")
		{
			$type = "TeamLevelDismiss";
			continue;
		}
		else if ($line == "end") continue;

		// Данные о сходе участников на контрольной точке
		if ($type == "TeamLevelDismiss")
		{
			// Получаем переменные
			$values = explode(';', $line);
			foreach ($values as &$value) $value = mysql_real_escape_string(trim($value, '"'));
			// Выясняем, есть ли уже такая запись
			$sql = "select teamleveldismiss_date, device_id from TeamLevelDismiss where user_id = ".$values[0].
				" and levelpoint_id = ".$values[1]." and team_id = ".$values[2]." and teamuser_id = ".$values[3];
			$Result = mysql_query($sql);
			$Old = mysql_fetch_assoc($Result);
			if (!$Old) $Record = "new";
			else
			{
				foreach ($Old as &$val) $val = mysql_real_escape_string($val);
				if (($Old['teamleveldismiss_date'] == $values[4]) &&
				    ($Old['device_id'] == $values[5]))
					$Record = "unchanged";
				else $Record = "updated";
			}
			mysql_free_result($Result);
			// Если записи раньше не было - вставляем ее в таблицу
			if ($Record == "new")
			{
				// новая запись о результате
				$d_new++;
				$sql = "insert into TeamLevelDismiss
					(teamleveldismiss_date, user_id, device_id, levelpoint_id, team_id, teamuser_id)
					values ('".$values[4]."', '".$values[0]."', '".$values[5]."', '".$values[1]."', '".$values[2]."', '".$values[3]."')";
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
			}
			elseif ($Record == "updated")
			{
				// измененная запись о результате
				$d_updated++;
				$sql = "update TeamLevelDismiss set
					teamleveldismiss_date = '".$values[4]."',
					device_id = ".$values[5]."
					where user_id = ".$values[0]." and levelpoint_id = ".$values[1].
					" and team_id = ".$values[2]." and teamuser_id = ".$values[3];
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
			}
			else
			{
				// дубль, игнорируем
				$d_unchanged++;
				continue;
			}
			// =============== Данные импортировали, теперь обновляем другие таблицы на основе импортированной записи
			// Выясняем level_id контрольной точки, на которой зарегистрирован сход
			$sql = "select level_id, levelpoint_order from LevelPoints where levelpoint_id = ".$values[1];
			$Result = mysql_query($sql);
			$Row = mysql_fetch_assoc($Result);
			$new_level_dismiss = $Row['level_id'];
			$new_levelpoint_order = $Row['levelpoint_order'];
			mysql_free_result($Result);
			// Получаем из базы текущую информацию о сходе участника
			$sql = "select teamuser_hide, level_id from TeamUsers where team_id = ".$values[2]." and user_id = ".$values[3];
			$Result = mysql_query($sql);
			$Row = mysql_fetch_assoc($Result);
			$old_level_dismiss = $Row['level_id'];
			$old_teamuser_hide = $Row['teamuser_hide'];
			mysql_free_result($Result);
			// Если он в базе сошел - выясняем порядковый номер контрольной точки, на которой он уже точно сошел
			if ($old_level_dismiss == '')
			{
				$old_level_dismiss = "NULL";
				$old_levelpoint_order = 9999;
			}
			else
			{
				$sql = "select levelpoint_order from LevelPoints where level_id = ".$old_level_dismiss." order by levelpoint_order desc";
				$Result = mysql_query($sql);
				$Row = mysql_fetch_assoc($Result);
				$old_levelpoint_order = $Row['levelpoint_order'];
			}
			mysql_free_result($Result);
			// Если участник сошел на старте 1 этапа, то его надо удалить из команды
			if ($new_levelpoint_order == 1)
			{
				$new_level_dismiss = "NULL";
				$new_teamuser_hide = 1;
			}
			else $new_teamuser_hide = $old_teamuser_hide;
			// Если в импортированных данных участник сошел раньше, чем в базе - обновляем базу
			if ((($new_level_dismiss <> $old_level_dismiss) || ($new_teamuser_hide <> $old_teamuser_hide)) && ($new_levelpoint_order < $old_levelpoint_order))
			{
				$sql = "update TeamUsers set
					teamuser_hide = ".$new_teamuser_hide.",
					level_id = ".$new_level_dismiss."
					where user_id = ".$values[3]." and team_id = ".$values[2];
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
				if ($new_levelpoint_order == 1) $d_delete++; else $d_dismiss++;
			}
		}

		// Данные о командах на контрольной точке
		if ($type == "TeamLevelPoints")
		{
			// Получаем переменные
			$values = explode(';', $line);
			foreach ($values as &$value) $value = mysql_real_escape_string(trim($value, '"'));
			if ($values[7] == "") $values[7] = "NULL";
			if ($values[7] != "NULL") $values[7] = "'".$values[7]."'";
			// Выясняем, есть ли уже такая запись
			$sql = "select teamlevelpoint_date, device_id, teamlevelpoint_datetime, teamlevelpoint_points, teamlevelpoint_comment from TeamLevelPoints where user_id = ".$values[0]." and levelpoint_id = ".$values[1]." and team_id = ".$values[2];
			$Result = mysql_query($sql);
			$Old = mysql_fetch_assoc($Result);
			if (!$Old) $Record = "new";
			else
			{
				foreach ($Old as &$val) $val = mysql_real_escape_string($val);
				if (!$Old['teamlevelpoint_comment']) $Old['teamlevelpoint_comment'] = "NULL"; else $Old['teamlevelpoint_comment'] = "'".$Old['teamlevelpoint_comment']."'";
				if (($Old['teamlevelpoint_date'] == $values[3]) &&
				    ($Old['device_id'] == $values[4]) &&
				    ($Old['teamlevelpoint_datetime'] == $values[5]) &&
				    ($Old['teamlevelpoint_points'] == $teamlevelpoint_points[$line_num]) &&
				    ($Old['teamlevelpoint_comment'] == $values[7]))
					$Record = "unchanged";
				else $Record = "updated";
			}
			mysql_free_result($Result);
			// Если записи раньше не было - вставляем ее в таблицу
			if ($Record == "new")
			{
				// новая запись о результате
				$n_new++;
				$sql = "insert into TeamLevelPoints
					(teamlevelpoint_date, user_id, device_id, levelpoint_id, team_id, teamlevelpoint_datetime, teamlevelpoint_points, teamlevelpoint_comment)
					values ('".$values[3]."', '".$values[0]."', '".$values[4]."', '".$values[1]."', '".$values[2]."', '".$values[5]."', '".$teamlevelpoint_points[$line_num]."', ".$values[7].")";
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
			}
			elseif ($Record == "updated")
			{
				// измененная запись о результате
				$n_updated++;
				$sql = "update TeamLevelPoints set
					teamlevelpoint_date = '".$values[3]."',
					device_id = ".$values[4].",
					teamlevelpoint_datetime = '".$values[5]."',
					teamlevelpoint_points = '".$teamlevelpoint_points[$line_num]."',
					teamlevelpoint_comment = ".$values[7]."
					where user_id = ".$values[0]." and levelpoint_id = ".$values[1]." and team_id = ".$values[2];
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
			}
			else
			{
				// дубль, игнорируем
				$n_unchanged++;
				continue;
			}

			// =============== Данные импортировали, теперь обновляем другие таблицы на основе импортированной записи
			// Выясняем level_id и pointtype_id для обновления TeamLevels
			$sql = "select level_id, pointtype_id from LevelPoints where levelpoint_id = ".$values[1];
			$Result = mysql_query($sql);
			$Row = mysql_fetch_assoc($Result);
			$level_id = $Row['level_id'];
			$pointtype_id = $Row['pointtype_id'];
			mysql_free_result($Result);
			// Смотрим, есть ли уже запись об этом этапе для этой команды
			// Если есть - инициализируем ей переменные
			$sql = "select * from TeamLevels where level_id = ".$level_id." and team_id = ".$values[2];
			$Result = mysql_query($sql);
			$Old = mysql_fetch_assoc($Result);
			mysql_free_result($Result);
			// Если результатов команды на этом этапе еще нет - инициализируем значениями по умолчанию
			if (!$Old)
			{
				$Old['teamlevel_id'] = "NULL";
				$Old['level_id'] = $level_id;
				$Old['team_id'] = $values[2];
				$Old['teamlevel_begtime'] = "NULL";
				$Old['teamlevel_endtime'] = "NULL";
				$Old['teamlevel_points'] = "NULL";
				$Old['teamlevel_comment'] = "";
				$Old['teamlevel_progress'] = "0";
				$Old['teamlevel_penalty'] = "NULL";
				$Old['error_id'] = "NULL";
				$Old['teamlevel_hide'] = "0";
				$insert = 1;
			}
			else
			{
				foreach ($Old as &$val)
					$val = mysql_real_escape_string($val);
				$insert = 0;
			}

			// Обновляем запись результатами из строки импорта
			if ($pointtype_id == 1)
			{
				// запись о выходе на старт
				$Old['teamlevel_begtime'] = $values[5];
				if ($Old['teamlevel_progress'] == "0") $Old['teamlevel_progress'] = "1";
			}
			elseif ($pointtype_id == 2)
			{
				// запись о приходе на финиш
				$Old['teamlevel_endtime'] = $values[5];
				$Old['teamlevel_points'] = $teamlevelpoint_points[$line_num];
				$Old['teamlevel_progress'] = "2";
			}
			if ($Old['teamlevel_comment'] == "") $Old['teamlevel_comment'] = $values[7];
			// Заново вычисляем штраф на этапе
			if ($pointtype_id == 2)
			{
				$sql = "select level_pointpenalties from Levels where level_id = ".$level_id;
				$Result = mysql_query($sql);
				if (!$Result || mysql_num_rows($Result) <> 1) die("Ошибка получения списка штрафов этапа в строке #".$line_num." - ".$line);
				$Row = mysql_fetch_assoc($Result);
				$Penalties = explode(',', $Row['level_pointpenalties']);
				mysql_free_result($Result);
				$PenaltyTime = 0;
				$Points = explode(',', $teamlevelpoint_points[$line_num]);
				foreach ($Points as $n => $point)
				{
					if ((($point == "0") && ((int)$Penalties[$n] > 0)) || (($point == "1") && ((int)$Penalties[$n] < 0)))
						$PenaltyTime += (int)$Penalties[$n];
				}
				$Old['teamlevel_penalty'] = $PenaltyTime;
			}
			// Добавляем/обновляем запись в таблице TeamLevels
			foreach ($Old as &$val)
			{
				 if ($val == "") $val = "NULL";
				 if (($val <> "NULL") && (substr($val, 0, 1) <> "'")) $val = "'".$val."'";
			}
			if ($insert)
				$sql = "insert into TeamLevels (teamlevel_id, level_id, team_id, teamlevel_begtime, teamlevel_endtime, teamlevel_points, teamlevel_comment, teamlevel_progress, teamlevel_penalty, error_id, teamlevel_hide)
					values (".$Old['teamlevel_id'].", ".$Old['level_id'].", ".$Old['team_id'].", ".$Old['teamlevel_begtime'].", ".$Old['teamlevel_endtime'].", ".$Old['teamlevel_points'].", ".$Old['teamlevel_comment'].", ".$Old['teamlevel_progress'].", ".$Old['teamlevel_penalty'].", ".$Old['error_id'].", ".$Old['teamlevel_hide'].")";
			else
				$sql = "update TeamLevels set
					teamlevel_begtime = ".$Old['teamlevel_begtime'].",
					teamlevel_endtime = ".$Old['teamlevel_endtime'].",
					teamlevel_points = ".$Old['teamlevel_points'].",
					teamlevel_comment = ".$Old['teamlevel_comment'].",
					teamlevel_progress = ".$Old['teamlevel_progress']."
					where level_id = ".$level_id." and team_id = ".$values[2];
			mysql_query($sql);
			if (mysql_error()) die($sql.": ".mysql_error());
			// Пересчитываем штрафы и общий прогресс команды
			RecalcTeamLevelDuration($values[2]);
			RecalcTeamLevelPenalty($values[2]);
			RecalcTeamResult($values[2]);
		}
	}
	echo "Команды: $n_new результатов добавлено, $n_updated изменено, $n_unchanged являются дубликатами<br />";
	echo "Данные о сходах: $d_new добавлено, $d_updated изменено, $d_unchanged являются дубликатами<br />";
	echo "У $d_dismiss участников изменена информация о сходе, $d_delete удалены из команд из-за неявки на старт<br />";
}


if (!isset($MyPHPScript) or $action <> 'LoadRaidDataFile')
{
   print('</body>');
   print('</html>');
}

?>
